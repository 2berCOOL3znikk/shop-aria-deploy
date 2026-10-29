param(
    [Parameter(Mandatory=$true)][string]$OutputFile,
    [string]$PgDump='pg_dump'
)
$ErrorActionPreference='Stop'
foreach($key in @('PGHOST','PGDATABASE','PGUSER')) {
    if([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($key))) { throw "Set $key before creating a backup" }
}
if((Test-Path -LiteralPath $OutputFile) -or (Test-Path -LiteralPath ($OutputFile+'.sha256'))){throw 'The output file or its checksum already exists; choose a new backup filename'}
& $PgDump -w --format=custom --no-owner --no-privileges --file $OutputFile
if($LASTEXITCODE -ne 0){throw 'pg_dump failed; do not treat the output as a valid backup'}
if(!(Test-Path -LiteralPath $OutputFile) -or (Get-Item -LiteralPath $OutputFile).Length -eq 0){throw 'The backup is missing or empty'}
$digest=Get-FileHash -LiteralPath $OutputFile -Algorithm SHA256
$digest.Hash | Set-Content -LiteralPath ($OutputFile+'.sha256') -Encoding ascii
Write-Output 'Backup and SHA256 created. Store them in protected durable storage and test a restore into a separate database.'
