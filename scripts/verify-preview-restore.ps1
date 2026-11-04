param([string]$PostgresBin='D:\PostgreSQL\bin')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $root
$cfg=Get-Content -LiteralPath data/preview/config.json -Raw | ConvertFrom-Json
if($cfg.port -ne 8084 -or $cfg.databasePort -ne 55434){throw 'Only the separate local preview is supported'}
$env:PGHOST='127.0.0.1';$env:PGPORT='55434';$env:PGUSER='aria_preview';$env:PGPASSWORD=$cfg.databasePassword
$snapshot=Join-Path $root 'data/preview/restore-verification.dump'
& (Join-Path $PostgresBin 'pg_dump.exe') -Fc -f $snapshot aria_preview
if($LASTEXITCODE -ne 0){throw 'Preview dump failed'}
$identity=[System.Security.Principal.WindowsIdentity]::GetCurrent().Name
& icacls.exe $snapshot /inheritance:r /grant:r ($identity+':(F)') *> $null
if($LASTEXITCODE -ne 0){throw 'Cannot restrict snapshot access'}
$restoredDatabase='aria_restore_'+[guid]::NewGuid().ToString('N').Substring(0,12)
& (Join-Path $PostgresBin 'createdb.exe') -T template0 -E UTF8 $restoredDatabase
if($LASTEXITCODE -ne 0){throw 'Cannot create a fresh restore database'}
& (Join-Path $PostgresBin 'pg_restore.exe') --exit-on-error --single-transaction --no-owner --no-acl -d $restoredDatabase $snapshot *> target/preview-restore.log
if($LASTEXITCODE -ne 0){throw 'Restore failed; preview and production are unchanged'}
$query=@'
SELECT json_build_object(
 'migrationVersion',(SELECT max(version::integer) FROM flyway_schema_history WHERE success AND version IS NOT NULL),
 'products',(SELECT count(*) FROM products),
 'orders',(SELECT count(*) FROM orders),
 'stores',(SELECT count(*) FROM connected_stores),
 'archivedStores',(SELECT count(*) FROM connected_stores WHERE archived),
 'currentStores',(SELECT count(*) FROM connected_stores WHERE NOT archived AND NOT test_connection),
 'aiDecisions',(SELECT count(*) FROM pricing_decisions WHERE source='AI'),
 'demoProductQuantity',(SELECT quantity_on_hand FROM products WHERE sku='ARIA-DEMO-001'),
 'activeJobsForArchivedStores',(SELECT count(*) FROM store_sync_tasks t JOIN connected_stores s ON s.id=t.store_id WHERE s.archived AND t.state IN ('QUEUED','RUNNING','RETRY'))
);
'@
function Read-Snapshot([string]$Database){
 $result=$query | & (Join-Path $PostgresBin 'psql.exe') -X -v ON_ERROR_STOP=1 -d $Database -At
 if($LASTEXITCODE -ne 0){throw 'Verification query failed'}
 return ($result | ConvertFrom-Json)
}
$original=Read-Snapshot 'aria_preview';$restored=Read-Snapshot $restoredDatabase
foreach($key in $original.PSObject.Properties.Name){if($original.$key -ne $restored.$key){throw ('Restored value differs: '+$key)}}
if($restored.migrationVersion -ne 29 -or $restored.activeJobsForArchivedStores -ne 0){throw 'Unexpected schema or archive state'}
$report=@{passed=$true;source='owned loopback preview';restoredDatabase=$restoredDatabase;snapshotSha256=(Get-FileHash -LiteralPath $snapshot -Algorithm SHA256).Hash;counts=$restored;productionChanged=$false;completedAt=[DateTime]::UtcNow.ToString('o')}
New-Item -ItemType Directory -Path target/e2e/product-experience -Force | Out-Null
$report | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath target/e2e/product-experience/preview-restore-report.json -Encoding UTF8
Write-Output 'PASS: preview backup restored into a fresh database; schema, records, stock and archive state match'
