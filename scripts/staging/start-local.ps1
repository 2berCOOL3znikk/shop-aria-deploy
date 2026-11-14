param([switch]$SkipBuild)
$ErrorActionPreference='Stop'
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Set-Location -LiteralPath $root
$area=Join-Path $root 'data/staging';$configPath=Join-Path $area 'config.json'
$c=Get-Content -LiteralPath $configPath -Raw -Encoding UTF8 | ConvertFrom-Json
if($c.projectId -ne 'dawn-dew-85661116' -or $c.host -ne 'ep-weathered-art-ba8o4nc5.c-8.eu-central-1.aws.neon.tech' -or $c.dbName -ne 'aria_staging' -or $c.dbUser -ne 'aria_staging_owner'){throw 'Only the independently created staging project is allowed'}
$jdk=Join-Path $env:USERPROFILE '.jdks/ms-21.0.12.1';$env:JAVA_HOME=$jdk
if(!$SkipBuild){& ./mvnw.cmd -q -DskipTests package;if($LASTEXITCODE -ne 0){throw 'Build failed'}}
$pg='D:/PostgreSQL/bin';$env:PGHOST=$c.host;$env:PGPORT='5432';$env:PGDATABASE=$c.dbName;$env:PGUSER=$c.dbUser;$env:PGPASSWORD=$c.dbPassword;$env:PGSSLMODE='require';$env:PGCONNECT_TIMEOUT='10'
$jar=Join-Path $area 'aria.jar';$pidFile=Join-Path $area 'aria.pid'
function Stop-OwnedStaging {
 if(Test-Path -LiteralPath $pidFile){$saved=[int](Get-Content -LiteralPath $pidFile);$process=Get-CimInstance Win32_Process -Filter "ProcessId=$saved";if($process){if(!$process.CommandLine.Contains($jar) -or !$process.CommandLine.Contains('--server.port=8091') -or !$process.CommandLine.Contains($c.host)){throw 'Saved process is not this staging application; preserved it'};Stop-Process -Id $process.ProcessId;Wait-Process -Id $process.ProcessId -ErrorAction SilentlyContinue}}
}
function Start-OwnedStaging([bool]$Bootstrap) {
 if(Get-NetTCPConnection -LocalPort 8091 -State Listen -ErrorAction SilentlyContinue){throw 'Port 8091 occupied; no other process stopped'}
 $env:ARIA_ADMIN_EMAIL=$c.email;$env:ARIA_ADMIN_PASSWORD=$c.password;$env:ARIA_JWT_SECRET=$c.jwt;$env:ARIA_INTEGRATION_ENCRYPTION_KEY=$c.encryptionKey
 $env:SPRING_DATASOURCE_USERNAME=$c.dbUser;$env:SPRING_DATASOURCE_PASSWORD=$c.dbPassword;$env:ARIA_STAGING_DATABASE_PASSWORD=$c.dbPassword
 $env:ARIA_SEED_DEMO_USERS='false';$env:ARIA_SEED_DEMO_CATALOG='false';$env:ARIA_DEMO_MARKET_ENABLED='false';$env:ARIA_RESET_DATA_ON_START='false';$env:ARIA_INTELLIGENCE_EXTERNAL_ENABLED='false'
 $profiles=if($Bootstrap){'postgres,production,free'}else{'postgres,production,free,staging'};$binding=if($Bootstrap){'demo'}else{'aria-v4-staging'}
 $static=(Resolve-Path -LiteralPath 'src/main/resources/static').Path+[IO.Path]::DirectorySeparatorChar
 $argsList=@('-Duser.timezone=Asia/Qyzylorda','-Xms64m','-Xmx256m','-XX:+UseSerialGC','-XX:ActiveProcessorCount=2','-jar',('"'+$jar+'"'),'--server.address=127.0.0.1','--server.port=8091',('--spring.profiles.active='+$profiles),('--spring.datasource.url=jdbc:'+$c.databaseUrl),('--aria.organization.key='+$binding),'--aria.manager.enabled=false','--aria.manager.schedule-enabled=false','--aria.manager.activity-enabled=false','--aria.store-sync.worker-enabled=false','--aria.company-signup.enabled=false',('--spring.web.resources.static-locations='+([Uri]$static).AbsoluteUri+',classpath:/static/'))
 $process=Start-Process -FilePath (Join-Path $jdk 'bin/java.exe') -ArgumentList $argsList -WorkingDirectory $root -WindowStyle Hidden -RedirectStandardOutput (Join-Path $area 'app.out.log') -RedirectStandardError (Join-Path $area 'app.err.log') -PassThru
 $process.Id | Set-Content -LiteralPath $pidFile
 for($i=0;$i -lt 60;$i++){try{if((Invoke-RestMethod -Uri 'http://127.0.0.1:8091/actuator/health' -TimeoutSec 2).status -eq 'UP'){return}}catch{};if($process.HasExited){throw 'Staging bootstrap stopped; see private data/staging/app.out.log'};Start-Sleep -Milliseconds 1000}
 throw 'Staging did not become ready; data preserved'
}
Stop-OwnedStaging
Copy-Item -LiteralPath target/aria-backend-0.0.1-SNAPSHOT.jar -Destination $jar -Force
$hasSchema=& "$pg/psql.exe" -X -w -At -c "select to_regclass('public.saas_organization') is not null"
if($LASTEXITCODE -ne 0){throw 'Cannot inspect the new staging database'}
if(($hasSchema -join '').Trim() -eq 'f'){Start-OwnedStaging $true;Stop-OwnedStaging}
$binding=& "$pg/psql.exe" -X -w -At -c 'select organization_key from saas_organization where id=1'
if($LASTEXITCODE -ne 0){throw 'Cannot inspect staging binding'}
if(($binding -join '').Trim() -eq 'demo'){
 $sql=@'
BEGIN;
DO $$ BEGIN
 IF current_database()<>'aria_staging' OR EXISTS(SELECT 1 FROM products) OR EXISTS(SELECT 1 FROM orders) OR EXISTS(SELECT 1 FROM connected_stores) THEN RAISE EXCEPTION 'Refusing to relabel a populated or foreign database'; END IF;
END $$;
UPDATE saas_organization SET organization_key='aria-v4-staging',name='ARIA 4.0 - Staging',demonstration=false WHERE id=1 AND organization_key='demo';
COMMIT;
'@
 $sql | & "$pg/psql.exe" -X -w -v ON_ERROR_STOP=1 | Out-Null
 if($LASTEXITCODE -ne 0){throw 'Staging binding was not changed'}
}elseif(($binding -join '').Trim() -ne 'aria-v4-staging'){throw 'Database belongs to another company; preserved it'}
Start-OwnedStaging $false
@('ISOLATED ARIA STAGING',('Email: '+$c.email),('Initial password: '+$c.password),'Never share this file. No production data or AI keys were copied.') | Set-Content -LiteralPath (Join-Path $area 'access.txt') -Encoding UTF8
Write-Output 'Staging schema and owner verified locally: http://127.0.0.1:8091/start.html'
