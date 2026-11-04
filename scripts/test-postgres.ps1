param([string]$Tests='', [string]$PostgresBin='D:\PostgreSQL\bin')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $root
if($Tests -and $Tests -notmatch '^[A-Za-z0-9_,*#]+$'){throw 'Use a comma-separated list of test classes or Class#method'}
$cfg=Get-Content -LiteralPath data/preview/config.json -Raw | ConvertFrom-Json
if($cfg.databasePort -ne 55434){throw 'Only the owned local preview cluster on 55434 is supported'}
New-Item -ItemType Directory -Path target -Force | Out-Null
# Controller integration tests commit HTTP requests. A reused database is not a clean test fixture.
$testDatabase='aria_checks_'+[guid]::NewGuid().ToString('N').Substring(0,12)
$env:PGHOST='127.0.0.1';$env:PGPORT='55434';$env:PGUSER='aria_preview';$env:PGPASSWORD=$cfg.databasePassword
& (Join-Path $PostgresBin 'createdb.exe') -T template0 -E UTF8 $testDatabase
if($LASTEXITCODE -ne 0){throw 'Could not create isolated test database; no existing database was changed'}
$env:JAVA_HOME=Join-Path $env:USERPROFILE '.jdks/ms-21.0.12.1'
$env:ARIA_TEST_DATABASE_URL='jdbc:postgresql://127.0.0.1:55434/'+$testDatabase
$env:ARIA_TEST_DATABASE_USER='aria_preview';$env:ARIA_TEST_DATABASE_PASSWORD=$cfg.databasePassword
$env:ARIA_TEST_DATABASE_DRIVER='org.postgresql.Driver';$env:ARIA_TEST_FLYWAY='true';$env:ARIA_TEST_SCHEMA_MODE='validate'
$metadata=@{database=$testDatabase;startedAt=[DateTime]::UtcNow.ToString('o');tests=$Tests;log='target/experience-full-tests.log';productionChanged=$false}
$metadata | ConvertTo-Json | Set-Content -LiteralPath target/postgres-test-run.json -Encoding UTF8
$arguments=@('test');if($Tests){$arguments+=('-Dtest='+$Tests)}
$priorPreference=$ErrorActionPreference
try{
 # Windows PowerShell 5 wraps native stderr warnings as ErrorRecords. Judge Maven by its exit code.
 $ErrorActionPreference='Continue'
 & (Join-Path $root 'mvnw.cmd') @arguments *> target/experience-full-tests.log
 $result=$LASTEXITCODE
}finally{$ErrorActionPreference=$priorPreference}
$metadata.exitCode=$result;$metadata.completedAt=[DateTime]::UtcNow.ToString('o')
$metadata | ConvertTo-Json | Set-Content -LiteralPath target/postgres-test-run.json -Encoding UTF8
Get-Content -LiteralPath target/experience-full-tests.log | Select-Object -Last 14
exit $result
