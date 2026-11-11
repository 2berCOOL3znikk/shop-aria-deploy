param([switch]$SkipBuild,[switch]$Restart)
$ErrorActionPreference='Stop'
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Set-Location -LiteralPath $root
$area=Join-Path $root 'data/onboarding';$pg='D:/PostgreSQL/bin';$port=8088;$database='aria_registration_local'
New-Item -ItemType Directory -Path $area -Force | Out-Null
& icacls.exe $area /inheritance:r /grant:r "$([System.Security.Principal.WindowsIdentity]::GetCurrent().Name):(OI)(CI)(F)" | Out-Null
if($LASTEXITCODE -ne 0){throw 'Cannot restrict application/config directory'}
$configPath=Join-Path $area 'registry-config.json'
if(!(Test-Path -LiteralPath $configPath)){
 $bytes=New-Object byte[] 32;[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
 @{email='operator@registration.aria.local';password=('ARIA-Desk-'+[guid]::NewGuid().ToString('N'));jwt=([guid]::NewGuid().ToString('N')+[guid]::NewGuid().ToString('N'));databasePassword=[guid]::NewGuid().ToString('N');encryptionKey=[Convert]::ToBase64String($bytes)} | ConvertTo-Json | Set-Content -LiteralPath $configPath -Encoding UTF8
}
$cfg=Get-Content -LiteralPath $configPath -Raw -Encoding UTF8 | ConvertFrom-Json
$pidPath=Join-Path $area 'registry.pid';$jar=Join-Path $area 'registry.jar'
if(Test-Path -LiteralPath $pidPath){
 $owned=Get-CimInstance Win32_Process -Filter "ProcessId=$([int](Get-Content -LiteralPath $pidPath))"
 if($owned){
  if(!$owned.CommandLine.Contains($jar) -or !$owned.CommandLine.Contains('--server.port=8088') -or !$owned.CommandLine.Contains('/aria_registration_local')){throw 'PID belongs to another process; preserved it'}
  if(!$Restart){Write-Output 'Registration preview already running: http://127.0.0.1:8088/company-start.html';exit 0}
  Stop-Process -Id $owned.ProcessId;Wait-Process -Id $owned.ProcessId -ErrorAction SilentlyContinue
 }
}
if(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue){throw 'Port 8088 occupied; no process stopped'}
$env:JAVA_HOME=Join-Path $env:USERPROFILE '.jdks/ms-21.0.12.1'
if(!$SkipBuild){& ./mvnw.cmd -q -DskipTests package;if($LASTEXITCODE -ne 0){throw 'Build failed'}}
$preview=Get-Content -LiteralPath data/preview/config.json -Raw -Encoding UTF8 | ConvertFrom-Json
if($preview.databasePort -ne 55434){throw 'Only the existing owned local PostgreSQL cluster is supported'}
$cluster=(Resolve-Path -LiteralPath (Join-Path $root 'data/preview/postgres')).Path
if($cluster -ne (Join-Path $root 'data\preview\postgres') -or !(Test-Path -LiteralPath (Join-Path $cluster 'PG_VERSION'))){throw 'Unexpected cluster; preserved it'}
& "$pg/pg_ctl.exe" -D $cluster status *> $null
if($LASTEXITCODE -ne 0){
 if(Get-NetTCPConnection -LocalPort 55434 -State Listen -ErrorAction SilentlyContinue){throw 'Database port occupied; no process stopped'}
 $argsList=@('-D',('"'+$cluster+'"'),'-o','"-p 55434 -h 127.0.0.1"','-l',('"'+(Join-Path $area 'postgres-start.log')+'"'),'-w','start')
 # ShellExecute detaches inherited command pipes. Redirecting pg_ctl here can keep a caller
 # (Node, IntelliJ or ARIA.cmd) waiting after readiness because the server inherits its handles.
 # PostgreSQL already writes its actual server output to the explicit -l file above.
 $helper=Start-Process -FilePath "$pg/pg_ctl.exe" -ArgumentList $argsList -Verb Open -WindowStyle Hidden -PassThru
 if(!$helper.WaitForExit(30000)){throw 'PostgreSQL start timed out'}
 & "$pg/pg_ctl.exe" -D $cluster status *> $null;if($LASTEXITCODE -ne 0){throw 'Owned PostgreSQL could not start'}
}
$env:PGHOST='127.0.0.1';$env:PGPORT='55434';$env:PGUSER='aria_preview';$env:PGPASSWORD=$preview.databasePassword;$env:PGCONNECT_TIMEOUT='5'
$exists=& "$pg/psql.exe" -w -d postgres -XAt -c "SELECT 1 FROM pg_roles WHERE rolname='aria_registration_local'"
if($LASTEXITCODE -ne 0){throw 'Cannot read owned cluster'}
if(($exists -join '').Trim() -ne '1'){
 if($cfg.databasePassword -notmatch '^[a-f0-9]{32}$'){throw 'Unexpected generated database credential'}
 & "$pg/psql.exe" -w -d postgres -X -v ON_ERROR_STOP=1 -c "CREATE ROLE aria_registration_local LOGIN PASSWORD '$($cfg.databasePassword)' NOSUPERUSER NOCREATEDB NOCREATEROLE" | Out-Null
 if($LASTEXITCODE -ne 0){throw 'Cannot create isolated role'}
}
$exists=& "$pg/psql.exe" -w -d postgres -XAt -c "SELECT 1 FROM pg_database WHERE datname='aria_registration_local'"
if(($exists -join '').Trim() -ne '1'){
 & "$pg/createdb.exe" -w -O aria_registration_local aria_registration_local;if($LASTEXITCODE -ne 0){throw 'Cannot create isolated database'}
 & "$pg/psql.exe" -w -d postgres -X -v ON_ERROR_STOP=1 -c 'REVOKE CONNECT ON DATABASE aria_registration_local FROM PUBLIC' | Out-Null
}
$hasSchema=& "$pg/psql.exe" -w -d $database -XAt -c "SELECT to_regclass('public.saas_organization') IS NOT NULL"
$bootstrap=(($hasSchema -join '').Trim() -ne 't')
Copy-Item -LiteralPath target/aria-backend-0.0.1-SNAPSHOT.jar -Destination $jar -Force
$env:SPRING_DATASOURCE_USERNAME='aria_registration_local';$env:SPRING_DATASOURCE_PASSWORD=$cfg.databasePassword
$env:ARIA_DATABASE_URL='postgresql://127.0.0.1:55434/aria_registration_local'
$env:ARIA_ADMIN_EMAIL=$cfg.email;$env:ARIA_ADMIN_PASSWORD=$cfg.password;$env:ARIA_JWT_SECRET=$cfg.jwt;$env:ARIA_INTEGRATION_ENCRYPTION_KEY=$cfg.encryptionKey
$source=(Resolve-Path -LiteralPath src/main/resources/static).Path+[IO.Path]::DirectorySeparatorChar
function Start-Desk([bool]$initial){
 $binding=if($initial){'demo'}else{'aria-registration'};$enabled=if($initial){'false'}else{'true'}
 $arguments=@('-Duser.timezone=Asia/Qyzylorda','-Xms32m','-Xmx192m','-XX:+UseSerialGC','-XX:ActiveProcessorCount=2','-jar',('"'+$jar+'"'),'--server.address=127.0.0.1','--server.port=8088','--spring.profiles.active=postgres,free,local-onboarding','--spring.datasource.url=jdbc:postgresql://127.0.0.1:55434/aria_registration_local','--spring.datasource.username=aria_registration_local',('--aria.organization.key='+$binding),('--aria.company-signup.enabled='+$enabled),'--spring.jpa.hibernate.ddl-auto=validate','--spring.flyway.enabled=true','--spring.h2.console.enabled=false','--aria.seed-demo-users=false','--aria.seed-demo-catalog=false','--aria.reset-data-on-start=false','--aria.demo-market-enabled=false','--aria.intelligence.external-enabled=false','--aria.manager.enabled=false','--aria.manager.activity-enabled=false','--aria.manager.schedule-enabled=false','--aria.store-sync.worker-enabled=false',('--spring.web.resources.static-locations='+([Uri]$source).AbsoluteUri+',classpath:/static/'))
 $p=Start-Process -FilePath (Join-Path $env:JAVA_HOME 'bin/java.exe') -ArgumentList $arguments -WorkingDirectory $root -WindowStyle Hidden -RedirectStandardOutput (Join-Path $area 'registry.out.log') -RedirectStandardError (Join-Path $area 'registry.err.log') -PassThru
 $p.Id | Set-Content -LiteralPath $pidPath
 for($attempt=0;$attempt -lt 50;$attempt++){
  try{if((Invoke-RestMethod -Uri http://127.0.0.1:8088/actuator/health -TimeoutSec 2).status -eq 'UP'){return $p}}catch{}
  if($p.HasExited){throw 'Registration preview stopped; see data/onboarding/registry.out.log'};Start-Sleep -Seconds 1
 }
 throw 'Registration preview startup timed out'
}
if($bootstrap){
 $initial=Start-Desk $true
 $owned=Get-CimInstance Win32_Process -Filter "ProcessId=$($initial.Id)"
 if(!$owned.CommandLine.Contains($jar) -or !$owned.CommandLine.Contains('/aria_registration_local')){throw 'Bootstrap process changed; preserved it'}
 Stop-Process -Id $initial.Id;Wait-Process -Id $initial.Id -ErrorAction SilentlyContinue
}
$binding=& "$pg/psql.exe" -w -d $database -XAt -c 'SELECT organization_key FROM saas_organization WHERE id=1'
if(($binding -join '').Trim() -eq 'demo'){
 # This exact new operator database may be bound only while all business/request tables are empty.
 $sql=@'
DO $$ BEGIN
 IF EXISTS(SELECT 1 FROM products) OR EXISTS(SELECT 1 FROM orders) OR EXISTS(SELECT 1 FROM connected_stores) OR EXISTS(SELECT 1 FROM company_registration_requests) THEN RAISE EXCEPTION 'Refusing to relabel a populated company'; END IF;
END $$;
UPDATE saas_organization SET organization_key='aria-registration', name='ARIA · company registration', demonstration=false WHERE id=1 AND organization_key='demo';
'@
 $sql | & "$pg/psql.exe" -w -d $database -X -v ON_ERROR_STOP=1 | Out-Null
 if($LASTEXITCODE -ne 0){throw 'Operator database binding failed'}
}elseif(($binding -join '').Trim() -ne 'aria-registration'){throw 'Another company owns this database; preserved it'}
$running=Start-Desk $false
@('ISOLATED COMPANY APPLICATION DESK','Application: http://127.0.0.1:8088/company-start.html','Operator: http://127.0.0.1:8088/company-requests.html',('Email: '+$cfg.email),('Password: '+$cfg.password),'A company application does not create a tenant workspace. No production or existing company was changed.') | Set-Content -LiteralPath (Join-Path $area 'registry-access.txt') -Encoding UTF8
Write-Output 'Company application preview: http://127.0.0.1:8088/company-start.html; private operator access: data/onboarding/registry-access.txt'
