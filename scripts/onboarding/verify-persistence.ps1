$ErrorActionPreference='Stop'
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Set-Location -LiteralPath $root
$cfg=Get-Content -LiteralPath data/onboarding/registry-config.json -Raw -Encoding UTF8 | ConvertFrom-Json
$env:PGHOST='127.0.0.1';$env:PGPORT='55434';$env:PGUSER='aria_registration_local';$env:PGPASSWORD=$cfg.databasePassword
$pg='D:/PostgreSQL/bin/psql.exe'
function Read-Value([string]$sql){$value=& $pg -w -XAt -d aria_registration_local -v ON_ERROR_STOP=1 -c $sql;if($LASTEXITCODE -ne 0){throw 'Registry check failed'};return ($value -join '').Trim()}
if((Read-Value 'SELECT organization_key FROM saas_organization WHERE id=1') -ne 'aria-registration'){throw 'Unexpected company binding'}
$snapshot='SELECT md5(coalesce(json_agg(r ORDER BY id)::text, ''[]'')) FROM company_registration_requests r'
$before=Read-Value $snapshot;$count=Read-Value 'SELECT count(*) FROM company_registration_requests';$users=Read-Value 'SELECT count(*) FROM app_users'
$previous=[int](Get-Content -LiteralPath data/onboarding/registry.pid)
& (Join-Path $PSScriptRoot 'start-local.ps1') -SkipBuild -Restart
if($LASTEXITCODE -ne 0){throw 'Registry restart failed'}
$after=Read-Value $snapshot
if($before -ne $after -or (Read-Value 'SELECT count(*) FROM app_users') -ne $users){throw 'Application data or operator accounts differ after restart'}
@{passed=$true;previousProcess=$previous;newProcess=[int](Get-Content -LiteralPath data/onboarding/registry.pid);applications=[int]$count;applicationsPersisted=$true;operatorAccountsUnchanged=$true;productionChanged=$false} | ConvertTo-Json | Set-Content -LiteralPath docs/onboarding/evidence/persistence.json -Encoding UTF8
Write-Output 'PASS: company applications, reviews and operator account survived the isolated JVM restart'
