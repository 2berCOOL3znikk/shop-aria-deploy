param([switch]$SkipBuild,[switch]$Restart)
$ErrorActionPreference='Stop'
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Set-Location -LiteralPath $root
$area=Join-Path $root 'data/woocommerce'
if(!(Test-Path -LiteralPath (Join-Path $area 'fixtures.json'))){throw 'Run scripts/woocommerce/start-local.ps1 first'}
$jdk=Join-Path $env:USERPROFILE '.jdks/ms-21.0.12.1'
$env:JAVA_HOME=$jdk
$cfgPath=Join-Path $area 'aria-config.json'
if(!(Test-Path -LiteralPath $cfgPath)){
 $bytes=New-Object byte[] 32;[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
 @{email='owner@woo-e2e.aria.local';password=('ARIA-Woo-'+[guid]::NewGuid().ToString('N'));jwt=([guid]::NewGuid().ToString('N')+[guid]::NewGuid().ToString('N'));databasePassword=[guid]::NewGuid().ToString('N');encryptionKey=[Convert]::ToBase64String($bytes)} | ConvertTo-Json | Set-Content -LiteralPath $cfgPath -Encoding UTF8
}
$cfg=Get-Content -LiteralPath $cfgPath -Raw | ConvertFrom-Json
$pidFile=Join-Path $area 'aria.pid';$jar=Join-Path $area 'aria.jar'
if(Test-Path -LiteralPath $pidFile){$p=Get-CimInstance Win32_Process -Filter "ProcessId=$([int](Get-Content -LiteralPath $pidFile))";if($p){if(!$p.CommandLine.Contains($jar) -or !$p.CommandLine.Contains('--server.port=8085') -or !$p.CommandLine.Contains('/aria_woo_e2e')){throw 'Saved process is not this isolated WooCommerce application; preserved it'};if(!$Restart){Write-Output 'Isolated ARIA already running on http://127.0.0.1:8085';exit 0};Stop-Process -Id $p.ProcessId;Wait-Process -Id $p.ProcessId -ErrorAction SilentlyContinue}}
if(Get-NetTCPConnection -LocalPort 8085 -State Listen -ErrorAction SilentlyContinue){throw 'Port 8085 occupied; no process stopped'}
if(!$SkipBuild){& ./mvnw.cmd -q -DskipTests package;if($LASTEXITCODE -ne 0){throw 'ARIA build failed'}}
$pg='D:/PostgreSQL/bin';$preview=Get-Content -LiteralPath data/preview/config.json -Raw | ConvertFrom-Json
if($preview.databasePort -ne 55434){throw 'Only the owned local PostgreSQL cluster is supported'}
$env:PGHOST='127.0.0.1';$env:PGPORT='55434';$env:PGUSER='aria_preview';$env:PGPASSWORD=$preview.databasePassword
$role=& "$pg/psql.exe" -w -d postgres -XAt -c "SELECT 1 FROM pg_roles WHERE rolname='aria_woo_e2e'"
if($LASTEXITCODE -ne 0){throw 'Start the existing local PostgreSQL preview first'}
if(($role -join '').Trim() -ne '1'){
 if($cfg.databasePassword -notmatch '^[a-f0-9]{32}$'){throw 'Unexpected generated database credential'}
 $sql="CREATE ROLE aria_woo_e2e LOGIN PASSWORD '$($cfg.databasePassword)' NOSUPERUSER NOCREATEDB NOCREATEROLE"
 $sql | & "$pg/psql.exe" -w -d postgres -X -v ON_ERROR_STOP=1 | Out-Null
 if($LASTEXITCODE -ne 0){throw 'Could not create isolated database role'}
}
$exists=& "$pg/psql.exe" -w -d postgres -XAt -c "SELECT 1 FROM pg_database WHERE datname='aria_woo_e2e'"
if(($exists -join '').Trim() -ne '1'){& "$pg/createdb.exe" -w -O aria_woo_e2e aria_woo_e2e;if($LASTEXITCODE -ne 0){throw 'Could not create isolated ARIA database'}; & "$pg/psql.exe" -w -d postgres -X -c 'REVOKE CONNECT ON DATABASE aria_woo_e2e FROM PUBLIC' | Out-Null}
$hasOrganization=& "$pg/psql.exe" -w -d aria_woo_e2e -XAt -c "SELECT to_regclass('public.saas_organization') IS NOT NULL"
$bootstrap=(($hasOrganization -join '').Trim() -ne 't')
if(!$bootstrap){
 $binding=& "$pg/psql.exe" -w -d aria_woo_e2e -XAt -c 'SELECT organization_key FROM saas_organization WHERE id=1'
 if(($binding -join '').Trim() -eq 'demo'){
  # Only this exact empty, newly-created local E2E database can be provisioned.
  $sql="DO `$`$ BEGIN IF EXISTS(SELECT 1 FROM products) OR EXISTS(SELECT 1 FROM orders) OR EXISTS(SELECT 1 FROM connected_stores) THEN RAISE EXCEPTION 'Cannot relabel a populated database'; END IF; END `$`$; UPDATE saas_organization SET organization_key='woo-e2e', name='ARIA WooCommerce isolated test',demonstration=false WHERE id=1 AND organization_key='demo';"
  $sql | & "$pg/psql.exe" -w -d aria_woo_e2e -X -v ON_ERROR_STOP=1 | Out-Null
  if($LASTEXITCODE -ne 0){throw 'Refusing to change a populated company database'}
 }elseif(($binding -join '').Trim() -ne 'woo-e2e'){throw 'Database belongs to another company; preserved it'}
}
$trust=Join-Path $area 'aria-truststore';if(!(Test-Path -LiteralPath $trust)){Copy-Item -LiteralPath (Join-Path $jdk 'lib/security/cacerts') -Destination $trust; $ErrorActionPreference='Continue'; & (Join-Path $jdk 'bin/keytool.exe') -importcert -noprompt -alias aria-isolated-woo -file (Join-Path $area 'tls-cert.pem') -keystore $trust -storepass changeit 2>$null;$ErrorActionPreference='Stop';if($LASTEXITCODE -ne 0){throw 'Cannot add isolated certificate to this test JVM trust store'}}
Copy-Item -LiteralPath 'target/aria-backend-0.0.1-SNAPSHOT.jar' -Destination $jar -Force
$env:SPRING_DATASOURCE_USERNAME='aria_woo_e2e';$env:SPRING_DATASOURCE_PASSWORD=$cfg.databasePassword
$env:ARIA_ADMIN_EMAIL=$cfg.email;$env:ARIA_ADMIN_PASSWORD=$cfg.password;$env:ARIA_JWT_SECRET=$cfg.jwt;$env:ARIA_ORGANIZATION_KEY=if($bootstrap){'demo'}else{'woo-e2e'};$env:ARIA_INTEGRATION_ENCRYPTION_KEY=$cfg.encryptionKey
$env:ARIA_SEED_DEMO_USERS='false';$env:ARIA_SEED_DEMO_CATALOG='false';$env:ARIA_RESET_DATA_ON_START='false';$env:ARIA_DEMO_MARKET_ENABLED='false'
$source=(Resolve-Path -LiteralPath 'src/main/resources/static').Path+[System.IO.Path]::DirectorySeparatorChar
$argsList=@('-Duser.timezone=Asia/Qyzylorda','-Xms64m','-Xmx256m','-XX:+UseSerialGC','-XX:ActiveProcessorCount=2',('-Djavax.net.ssl.trustStore="'+$trust+'"'),'-Djavax.net.ssl.trustStorePassword=changeit','-jar',('"'+$jar+'"'),'--server.address=127.0.0.1','--server.port=8085','--spring.profiles.active=postgres,free,local-woo','--spring.datasource.url=jdbc:postgresql://127.0.0.1:55434/aria_woo_e2e','--spring.datasource.username=aria_woo_e2e','--spring.jpa.hibernate.ddl-auto=validate','--spring.flyway.enabled=true','--spring.h2.console.enabled=false','--aria.smart-pricing.schedule-enabled=false','--aria.manager.schedule-enabled=false',('--spring.web.resources.static-locations='+([Uri]$source).AbsoluteUri+',classpath:/static/'))
$p=Start-Process -FilePath (Join-Path $jdk 'bin/java.exe') -ArgumentList $argsList -WorkingDirectory $root -WindowStyle Hidden -RedirectStandardOutput (Join-Path $area 'aria.out.log') -RedirectStandardError (Join-Path $area 'aria.err.log') -PassThru
$p.Id | Set-Content -LiteralPath $pidFile
$ready=$false
for($i=0;$i -lt 50;$i++){try{if((Invoke-RestMethod -Uri 'http://127.0.0.1:8085/actuator/health' -TimeoutSec 2).status -eq 'UP'){$ready=$true;break}}catch{};if($p.HasExited){throw 'Isolated ARIA stopped; see data/woocommerce/aria.out.log'};Start-Sleep -Seconds 1}
if(!$ready){throw 'Isolated ARIA not ready; see data/woocommerce/aria.out.log'}
if($bootstrap){Stop-Process -Id $p.Id;Wait-Process -Id $p.Id -ErrorAction SilentlyContinue;& $PSCommandPath -SkipBuild;exit $LASTEXITCODE}
@('ISOLATED ARIA + REAL LOCAL WOOCOMMERCE','URL: http://127.0.0.1:8085/control.html',('Email: '+$cfg.email),('Password: '+$cfg.password),'All catalogue data is educational. No production data copied.') | Set-Content -LiteralPath (Join-Path $area 'aria-access.txt') -Encoding UTF8
Write-Output 'Isolated ARIA ready: http://127.0.0.1:8085/control.html. Credentials: data/woocommerce/aria-access.txt'
