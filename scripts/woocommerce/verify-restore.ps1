$ErrorActionPreference='Stop'
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Set-Location -LiteralPath $root
$area=Join-Path $root 'data/woocommerce';$pg='D:/PostgreSQL/bin';$jdk=Join-Path $env:USERPROFILE '.jdks/ms-21.0.12.1'
$cfg=Get-Content -LiteralPath (Join-Path $area 'aria-config.json') -Raw | ConvertFrom-Json
$preview=Get-Content -LiteralPath 'data/preview/config.json' -Raw | ConvertFrom-Json
if($preview.databasePort -ne 55434){throw 'Only the owned isolated PostgreSQL cluster is supported'}
$env:PGHOST='127.0.0.1';$env:PGPORT='55434';$env:PGUSER='aria_woo_e2e';$env:PGPASSWORD=$cfg.databasePassword
$binding=& "$pg/psql.exe" -w -XAt -d aria_woo_e2e -c 'SELECT organization_key FROM saas_organization WHERE id=1'
if($LASTEXITCODE -ne 0 -or ($binding -join '').Trim() -ne 'woo-e2e'){throw 'Source is not the owned WooCommerce E2E company'}
$dump=Join-Path $area 'verified-backup.dump'
& "$pg/pg_dump.exe" -w -Fc -f $dump aria_woo_e2e
if($LASTEXITCODE -ne 0){throw 'Isolated backup failed'}
& icacls.exe $dump /inheritance:r /grant:r "$([System.Security.Principal.WindowsIdentity]::GetCurrent().Name):(F)" | Out-Null
if($LASTEXITCODE -ne 0){throw 'Cannot restrict backup access'}
$restored='aria_woo_restore_'+[guid]::NewGuid().ToString('N').Substring(0,12)
$env:PGUSER='aria_preview';$env:PGPASSWORD=$preview.databasePassword
& "$pg/createdb.exe" -w -T template0 -E UTF8 -O aria_woo_e2e $restored
if($LASTEXITCODE -ne 0){throw 'Cannot create fresh restore target'}
& "$pg/psql.exe" -w -X -d postgres -c "REVOKE CONNECT ON DATABASE $restored FROM PUBLIC" | Out-Null
$env:PGUSER='aria_woo_e2e';$env:PGPASSWORD=$cfg.databasePassword
& "$pg/pg_restore.exe" -w --exit-on-error --single-transaction --no-owner --no-acl -d $restored $dump *> (Join-Path $area 'restore.log')
if($LASTEXITCODE -ne 0){throw 'Restore failed; source was not changed'}
$query=@'
SELECT json_build_object(
 'migrationVersion',(SELECT max(version::integer) FROM flyway_schema_history WHERE success AND version IS NOT NULL),
 'binding',(SELECT organization_key FROM saas_organization WHERE id=1),
 'stores',(SELECT count(*) FROM connected_stores),
 'products',(SELECT count(*) FROM store_snapshots),
 'orders',(SELECT count(*) FROM store_order_snapshots),
 'ownerFacts',(SELECT count(*) FROM store_product_policies),
 'decisions',(SELECT count(*) FROM store_pricing_decisions),
 'aiDecisions',(SELECT count(*) FROM store_pricing_decisions WHERE source='AI'),
 'verifiedPriceChanges',(SELECT count(*) FROM store_price_changes WHERE status='APPLIED'),
 'retainedEncryptedKeys',(SELECT count(*) FROM external_integrations WHERE encrypted_credentials IS NOT NULL),
 'pendingSyncs',(SELECT count(*) FROM store_sync_tasks WHERE state IN ('QUEUED','RUNNING','RETRY')),
 'webhookReceipts',(SELECT count(*) FROM store_webhook_receipts)
);
'@
function Read-State([string]$Database){$raw=$query | & "$pg/psql.exe" -w -XAt -v ON_ERROR_STOP=1 -d $Database;if($LASTEXITCODE -ne 0){throw 'Cannot read restored state'};return $raw | ConvertFrom-Json}
$source=Read-State 'aria_woo_e2e';$copy=Read-State $restored
foreach($name in $source.PSObject.Properties.Name){if($source.$name -ne $copy.$name){throw ('Restore mismatch: '+$name)}}
if($copy.migrationVersion -ne 32 -or $copy.verifiedPriceChanges -lt 2){throw 'Expected verified V32 price-write workflow was not restored'}
if(Get-NetTCPConnection -LocalPort 8086 -State Listen -ErrorAction SilentlyContinue){throw 'Port 8086 occupied; preserved the existing process'}
$env:SPRING_DATASOURCE_USERNAME='aria_woo_e2e';$env:SPRING_DATASOURCE_PASSWORD=$cfg.databasePassword;$env:ARIA_JWT_SECRET=$cfg.jwt;$env:ARIA_ADMIN_EMAIL=$cfg.email;$env:ARIA_ADMIN_PASSWORD=$cfg.password;$env:ARIA_ORGANIZATION_KEY='woo-e2e';$env:ARIA_INTEGRATION_ENCRYPTION_KEY=$cfg.encryptionKey
$jar=Join-Path $area 'aria.jar'
$argsList=@('-Xms32m','-Xmx192m','-XX:+UseSerialGC','-XX:ActiveProcessorCount=2','-jar',('"'+$jar+'"'),'--server.port=8086','--server.address=127.0.0.1','--spring.profiles.active=postgres,free',('--spring.datasource.url=jdbc:postgresql://127.0.0.1:55434/'+$restored),'--spring.datasource.username=aria_woo_e2e','--aria.intelligence.external-enabled=false','--aria.connected-pricing.schedule-enabled=false','--aria.store-sync.worker-enabled=false','--aria.smart-pricing.schedule-enabled=false','--aria.manager.enabled=false','--aria.manager.schedule-enabled=false','--aria.seed-demo-users=false','--aria.seed-demo-catalog=false')
$p=Start-Process -FilePath (Join-Path $jdk 'bin/java.exe') -ArgumentList $argsList -WorkingDirectory $root -WindowStyle Hidden -RedirectStandardOutput (Join-Path $area 'restored-app.log') -RedirectStandardError (Join-Path $area 'restored-app.err.log') -PassThru
try{
 $ready=$false
 for($i=0;$i -lt 60;$i++){try{if((Invoke-RestMethod -Uri 'http://127.0.0.1:8086/actuator/health' -TimeoutSec 2).status -eq 'UP'){$ready=$true;break}}catch{};if($p.HasExited){throw 'Restored application stopped'};Start-Sleep -Seconds 1}
 if(!$ready){throw 'Restored application did not start'}
 $login=Invoke-RestMethod -Uri 'http://127.0.0.1:8086/api/auth/login' -Method Post -ContentType 'application/json' -Body (@{email=$cfg.email;password=$cfg.password}|ConvertTo-Json -Compress)
 $headers=@{Authorization=('Bearer '+$login.token)}
 $stores=Invoke-RestMethod -Uri 'http://127.0.0.1:8086/api/admin/control/stores' -Headers $headers
 if(@($stores.stores).Count -ne $copy.stores){throw 'Restored API does not match the backup'}
 $center=Invoke-RestMethod -Uri 'http://127.0.0.1:8086/api/admin/control/manager/center' -Headers $headers
 if(@($center.connectedChanges).Count -lt 2){throw 'Restored price history is unavailable'}
 if($copy.aiDecisions -gt 0 -and !@($center.connectedDecisions | Where-Object source -eq 'AI').Count){throw 'Restored AI decision is unavailable'}
 $report=@{passed=$true;source='isolated local WooCommerce company';target=$restored;counts=$copy;applicationStarted=$true;ownerLoginVerified=$true;historyReadFromApi=$true;externalAiCalled=$false;productionChanged=$false;snapshotSha256=(Get-FileHash -LiteralPath $dump -Algorithm SHA256).Hash;completedAt=[DateTime]::UtcNow.ToString('o')}
 $report | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath 'docs/connected-commerce/evidence/restore-v32.json' -Encoding UTF8
 Write-Output 'PASS: V1-V32 restored into a fresh database; application starts, owner signs in, external catalogue and verified price history survive'
}finally{
 $owned=Get-CimInstance Win32_Process -Filter "ProcessId=$($p.Id)"
 if($owned -and $owned.CommandLine.Contains($jar) -and $owned.CommandLine.Contains('--server.port=8086')){Stop-Process -Id $p.Id}
}
