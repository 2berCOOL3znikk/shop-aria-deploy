param([switch]$SkipBuild,[switch]$Restart)
$ErrorActionPreference='Stop'
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Set-Location -LiteralPath $root
$area=Join-Path $root 'data/localization-preview'
New-Item -ItemType Directory -Path $area -Force | Out-Null
& icacls.exe $area /inheritance:r /grant:r "$([System.Security.Principal.WindowsIdentity]::GetCurrent().Name):(OI)(CI)F" | Out-Null
if($LASTEXITCODE -ne 0){throw 'Cannot restrict local test credentials'}
$cfgPath=Join-Path $area 'config.json'
if(!(Test-Path -LiteralPath $cfgPath)){
 @{email='owner@localization.aria.local';password=('ARIA-'+[guid]::NewGuid().ToString('N'));jwt=([guid]::NewGuid().ToString('N')+[guid]::NewGuid().ToString('N'));databasePassword=[guid]::NewGuid().ToString('N')} | ConvertTo-Json | Set-Content -LiteralPath $cfgPath -Encoding UTF8
}
$cfg=Get-Content -LiteralPath $cfgPath -Raw | ConvertFrom-Json
$jar=Join-Path $area 'app.jar';$pidFile=Join-Path $area 'app.pid'
if(Test-Path -LiteralPath $pidFile){$p=Get-CimInstance Win32_Process -Filter "ProcessId=$([int](Get-Content -LiteralPath $pidFile))";if($p){if(!$p.CommandLine.Contains($jar) -or !$p.CommandLine.Contains('--server.port=8087') -or !$p.CommandLine.Contains('/aria_locale_demo')){throw 'PID belongs to another process; preserved it'};if(!$Restart){Write-Output 'Separate public test storefront already running on 8087';exit 0};Stop-Process -Id $p.ProcessId;Wait-Process -Id $p.ProcessId -ErrorAction SilentlyContinue}}
if(Get-NetTCPConnection -LocalPort 8087 -State Listen -ErrorAction SilentlyContinue){throw 'Port 8087 occupied; preserved the existing process'}
$env:JAVA_HOME=Join-Path $env:USERPROFILE '.jdks/ms-21.0.12.1'
if(!$SkipBuild){& ./mvnw.cmd -q -DskipTests package;if($LASTEXITCODE -ne 0){throw 'Build failed'}}
$preview=Get-Content -LiteralPath data/preview/config.json -Raw | ConvertFrom-Json
if($preview.databasePort -ne 55434){throw 'Only the owned local PostgreSQL cluster is supported'}
$pg='D:/PostgreSQL/bin';$env:PGHOST='127.0.0.1';$env:PGPORT='55434';$env:PGUSER='aria_preview';$env:PGPASSWORD=$preview.databasePassword
$role=& "$pg/psql.exe" -w -d postgres -XAt -c "SELECT 1 FROM pg_roles WHERE rolname='aria_locale_demo'"
if($LASTEXITCODE -ne 0){throw 'Local PostgreSQL is not available'}
if(($role -join '').Trim() -ne '1'){
 if($cfg.databasePassword -notmatch '^[a-f0-9]{32}$'){throw 'Unexpected local credential format'}
 & "$pg/psql.exe" -w -d postgres -X -v ON_ERROR_STOP=1 -c "CREATE ROLE aria_locale_demo LOGIN PASSWORD '$($cfg.databasePassword)' NOSUPERUSER NOCREATEDB NOCREATEROLE" | Out-Null
 if($LASTEXITCODE -ne 0){throw 'Cannot create the isolated role'}
}
$exists=& "$pg/psql.exe" -w -d postgres -XAt -c "SELECT 1 FROM pg_database WHERE datname='aria_locale_demo'"
if(($exists -join '').Trim() -ne '1'){& "$pg/createdb.exe" -w -O aria_locale_demo aria_locale_demo;if($LASTEXITCODE -ne 0){throw 'Cannot create test database'};& "$pg/psql.exe" -w -d postgres -X -c 'REVOKE CONNECT ON DATABASE aria_locale_demo FROM PUBLIC' | Out-Null}
Copy-Item -LiteralPath target/aria-backend-0.0.1-SNAPSHOT.jar -Destination $jar -Force
$env:SPRING_DATASOURCE_USERNAME='aria_locale_demo';$env:SPRING_DATASOURCE_PASSWORD=$cfg.databasePassword;$env:ARIA_ORGANIZATION_KEY='demo'
$env:ARIA_ADMIN_EMAIL=$cfg.email;$env:ARIA_ADMIN_PASSWORD=$cfg.password;$env:ARIA_JWT_SECRET=$cfg.jwt
$env:ARIA_SEED_DEMO_USERS='false';$env:ARIA_SEED_DEMO_CATALOG='false';$env:ARIA_RESET_DATA_ON_START='false';$env:ARIA_DEMO_MARKET_ENABLED='false'
$source=(Resolve-Path -LiteralPath src/main/resources/static).Path+[IO.Path]::DirectorySeparatorChar
$arguments=@('-Duser.timezone=Asia/Qyzylorda','-Xms32m','-Xmx192m','-XX:+UseSerialGC','-XX:ActiveProcessorCount=2','-jar',('"'+$jar+'"'),'--server.address=127.0.0.1','--server.port=8087','--spring.profiles.active=postgres,free','--spring.datasource.url=jdbc:postgresql://127.0.0.1:55434/aria_locale_demo','--spring.datasource.username=aria_locale_demo','--spring.jpa.hibernate.ddl-auto=validate','--spring.flyway.enabled=true','--aria.organization.key=demo','--aria.demo-catalog-import-enabled=true','--aria.intelligence.external-enabled=false','--aria.smart-pricing.schedule-enabled=false','--aria.manager.schedule-enabled=false','--aria.manager.activity-enabled=false','--aria.store-sync.worker-enabled=false',('--spring.web.resources.static-locations='+([Uri]$source).AbsoluteUri+',classpath:/static/'))
$p=Start-Process -FilePath (Join-Path $env:JAVA_HOME 'bin/java.exe') -ArgumentList $arguments -WorkingDirectory $root -WindowStyle Hidden -RedirectStandardOutput (Join-Path $area 'app.out.log') -RedirectStandardError (Join-Path $area 'app.err.log') -PassThru
$p.Id | Set-Content -LiteralPath $pidFile
$ready=$false
for($i=0;$i -lt 50;$i++){try{if((Invoke-RestMethod -Uri http://127.0.0.1:8087/actuator/health -TimeoutSec 2).status -eq 'UP'){$ready=$true;break}}catch{};if($p.HasExited){throw 'Public test storefront stopped; see data/localization-preview/app.out.log'};Start-Sleep -Seconds 1}
if(!$ready){throw 'Public test storefront startup timed out'}
@('SEPARATE PUBLIC EDUCATIONAL STOREFRONT','URL: http://127.0.0.1:8087/index.html',('Email: '+$cfg.email),('Password: '+$cfg.password),'External AI disabled; no production data copied.') | Set-Content -LiteralPath (Join-Path $area 'access.txt') -Encoding UTF8
Write-Output 'Public educational storefront ready on http://127.0.0.1:8087; private access file: data/localization-preview/access.txt'
