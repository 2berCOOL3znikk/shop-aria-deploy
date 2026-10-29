param([int]$Port=8084,[int]$DatabasePort=55434,[string]$PostgresBin='D:\PostgreSQL\bin',[string]$JavaHome=$env:JAVA_HOME)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $root
if($Port -lt 1024 -or $Port -gt 65535 -or $DatabasePort -lt 1024 -or $DatabasePort -gt 65535 -or $Port -eq $DatabasePort){throw 'Choose two distinct non-reserved ports'}
if(!(Test-Path -LiteralPath (Join-Path $PostgresBin 'pg_ctl.exe'))){throw 'PostgreSQL tools not found. Pass -PostgresBin with the installed bin folder.'}
function Test-PreviewJava21([string]$JdkPath){
    $priorPreference=$ErrorActionPreference
    try{
        # java -version writes its successful response to stderr; Windows PowerShell wraps it as ErrorRecord.
        $ErrorActionPreference='Continue'
        $versionText=(& (Join-Path $JdkPath 'bin/java.exe') -version 2>&1 | Out-String)
        return $versionText -match 'version "21[.\"]'
    }finally{$ErrorActionPreference=$priorPreference}
}
if($JavaHome -and (Test-Path -LiteralPath (Join-Path $JavaHome 'bin/java.exe'))){
    if(!(Test-PreviewJava21 $JavaHome)){
        if($PSBoundParameters.ContainsKey('JavaHome')){throw 'The supplied JavaHome is not Java 21'}
        $JavaHome=$null
    }
}
if(!$JavaHome){$jdk=Get-ChildItem -LiteralPath (Join-Path $env:USERPROFILE '.jdks') -Directory | Where-Object {$_.Name -match '21' -and (Test-Path -LiteralPath (Join-Path $_.FullName 'bin/java.exe'))} | Select-Object -First 1; if($jdk){$JavaHome=$jdk.FullName}}
if(!$JavaHome -or !(Test-Path -LiteralPath (Join-Path $JavaHome 'bin/java.exe'))){throw 'Java 21 required. Pass -JavaHome with the JDK 21 folder.'}
if(!(Test-PreviewJava21 $JavaHome)){throw 'Java 21 required; selected JDK has a different version'}
$env:JAVA_HOME=$JavaHome
$preview=Join-Path $root 'data\preview'
New-Item -ItemType Directory -Path $preview -Force | Out-Null
$configPath=Join-Path $preview 'config.json'
if(!(Test-Path -LiteralPath $configPath)){
    $config=@{email='owner@preview.aria.local';password=('ARIA-'+[guid]::NewGuid().ToString('N'));jwt=([guid]::NewGuid().ToString('N')+[guid]::NewGuid().ToString('N'));databasePassword=[guid]::NewGuid().ToString('N');port=$Port;databasePort=$DatabasePort}
    $config | ConvertTo-Json | Set-Content -LiteralPath $configPath -Encoding UTF8
    & icacls.exe $configPath /inheritance:r /grant:r "$([System.Security.Principal.WindowsIdentity]::GetCurrent().Name):(F)" | Out-Null
    if($LASTEXITCODE -ne 0){throw 'Cannot restrict preview credential file permissions'}
}
$config=Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
if($config.port -ne $Port -or $config.databasePort -ne $DatabasePort){throw 'Preview already configured for different ports; pass its original Port/DatabasePort. Existing data was preserved.'}
$pidFile=Join-Path $preview 'app.pid'
if(Test-Path -LiteralPath $pidFile){$existing=Get-CimInstance Win32_Process -Filter "ProcessId=$([int](Get-Content -LiteralPath $pidFile))";if($existing -and $existing.CommandLine.Contains((Join-Path $preview 'app.jar')) -and $existing.CommandLine.Contains("--server.port=$Port")){Write-Output "Preview already running: http://127.0.0.1:$Port/index.html";exit 0}}
if(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue){throw "Port $Port is occupied. No existing process was stopped."}
# Build before starting PostgreSQL; never overwrite a JAR used by a running application.
& (Join-Path $root 'mvnw.cmd') -q -DskipTests package
if($LASTEXITCODE -ne 0){throw 'Build failed'}
$cluster=Join-Path $preview 'postgres'
$env:PGHOST='127.0.0.1';$env:PGPORT=[string]$DatabasePort;$env:PGUSER='aria_preview';$env:PGPASSWORD=$config.databasePassword;$env:PGCONNECT_TIMEOUT='5'
if(!(Test-Path -LiteralPath (Join-Path $cluster 'PG_VERSION'))){
    $pwfile=Join-Path $preview 'pg-init-password.tmp'
    try{$config.databasePassword | Set-Content -LiteralPath $pwfile -Encoding ascii; & (Join-Path $PostgresBin 'initdb.exe') -D $cluster --username=aria_preview --auth=scram-sha-256 --encoding=UTF8 --locale=C --pwfile=$pwfile; if($LASTEXITCODE -ne 0){throw 'Preview database initialization failed'}}
    finally{if(Test-Path -LiteralPath $pwfile){Remove-Item -LiteralPath $pwfile}}
}
& (Join-Path $PostgresBin 'pg_ctl.exe') -D $cluster status *> $null
if($LASTEXITCODE -ne 0){
    if(Get-NetTCPConnection -LocalPort $DatabasePort -State Listen -ErrorAction SilentlyContinue){throw 'Preview database port occupied; nothing changed'}
    # Separate handles: a PostgreSQL child must not keep a redirected PowerShell pipeline open.
    $pgArgs=@('-D',('"'+$cluster+'"'),'-o',('"-p '+$DatabasePort+' -h 127.0.0.1"'),'-l',('"'+(Join-Path $preview 'postgres.log')+'"'),'-w','start')
    $pgControl=Start-Process -FilePath (Join-Path $PostgresBin 'pg_ctl.exe') -ArgumentList $pgArgs -WindowStyle Hidden -RedirectStandardOutput (Join-Path $preview 'pgctl.log') -RedirectStandardError (Join-Path $preview 'pgctl.err.log') -PassThru
    if(!$pgControl.WaitForExit(30000)){throw 'Preview PostgreSQL startup timed out; see data/preview/pgctl.err.log'}
    # Windows PowerShell 5 can return a null ExitCode from Start-Process; verify the actual server.
    & (Join-Path $PostgresBin 'pg_ctl.exe') -D $cluster status *> $null
    if($LASTEXITCODE -ne 0){throw 'Preview PostgreSQL did not start; see data/preview/pgctl.err.log'}
}
$exists=& (Join-Path $PostgresBin 'psql.exe') -w -h 127.0.0.1 -p $DatabasePort -U aria_preview -X -A -t --dbname postgres --command "SELECT 1 FROM pg_database WHERE datname='aria_preview'"
if($LASTEXITCODE -ne 0){throw 'Cannot inspect preview cluster'}
if(($exists -join '').Trim() -ne '1'){& (Join-Path $PostgresBin 'createdb.exe') -w -h 127.0.0.1 -p $DatabasePort -U aria_preview aria_preview;if($LASTEXITCODE -ne 0){throw 'Cannot create preview database'}}
Copy-Item -LiteralPath (Join-Path $root 'target\aria-backend-0.0.1-SNAPSHOT.jar') -Destination (Join-Path $preview 'app.jar') -Force
$env:ARIA_DATABASE_URL="postgresql://127.0.0.1:$DatabasePort/aria_preview"
$env:SPRING_DATASOURCE_USERNAME='aria_preview';$env:SPRING_DATASOURCE_PASSWORD=$config.databasePassword
$env:SPRING_PROFILES_ACTIVE='postgres,production,free'
$env:ARIA_ADMIN_EMAIL=$config.email;$env:ARIA_ADMIN_PASSWORD=$config.password;$env:ARIA_JWT_SECRET=$config.jwt
$env:ARIA_SEED_DEMO_USERS='false';$env:ARIA_SEED_DEMO_CATALOG='false';$env:ARIA_DEMO_MARKET_ENABLED='false';$env:ARIA_RESET_DATA_ON_START='false'
$jar=Join-Path $preview 'app.jar'
$args=@('-Duser.timezone=Asia/Qyzylorda','-Xms64m','-Xmx256m','-XX:+UseSerialGC','-XX:ActiveProcessorCount=2','-XX:TieredStopAtLevel=1','-jar',('"'+$jar+'"'),"--server.port=$Port",'--server.address=127.0.0.1',"--spring.datasource.url=jdbc:postgresql://127.0.0.1:$DatabasePort/aria_preview",'--spring.datasource.username=aria_preview','--spring.profiles.active=postgres,production,free','--aria.smart-pricing.schedule-enabled=false')
$process=Start-Process -FilePath (Join-Path $JavaHome 'bin\java.exe') -ArgumentList $args -WorkingDirectory $root -WindowStyle Hidden -RedirectStandardOutput (Join-Path $preview 'app.log') -RedirectStandardError (Join-Path $preview 'app.err.log') -PassThru
$process.Id | Set-Content -LiteralPath $pidFile
$ready=$false
for($attempt=0;$attempt -lt 60;$attempt++){
    try{if((Invoke-RestMethod -Uri "http://127.0.0.1:$Port/actuator/health" -TimeoutSec 2).status -eq 'UP'){$ready=$true;break}}catch{}
    if($process.HasExited){throw 'Preview stopped during startup. See data/preview/app.log and app.err.log.'}
    Start-Sleep -Seconds 1
}
if(!$ready){throw 'Preview readiness timeout. Check data/preview/app.log; no other service was changed.'}
$access=Join-Path $preview 'access.txt'
@("LOCAL PREVIEW ONLY - no production data","URL: http://127.0.0.1:$Port/index.html","Admin: http://127.0.0.1:$Port/control.html","Email: $($config.email)","Initial password: $($config.password)","Password changes in the profile are persistent; this initial password is not reapplied.") | Set-Content -LiteralPath $access -Encoding UTF8
& icacls.exe $access /inheritance:r /grant:r "$([System.Security.Principal.WindowsIdentity]::GetCurrent().Name):(F)" | Out-Null
Write-Output "Preview ready: http://127.0.0.1:$Port/index.html"
Write-Output 'Login details: backend/data/preview/access.txt. AI requires consent in Smart Pricing settings; existing settings are preserved. This server is accessible only on this computer.'
