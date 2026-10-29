param(
    [Parameter(Mandatory=$true)][string]$BackupFile,
    [Parameter(Mandatory=$true)][string]$SourceDatabase,
    [Parameter(Mandatory=$true)][string]$TargetDatabase,
    [string]$PgRestore='pg_restore',
    [string]$Psql='psql'
)
$ErrorActionPreference='Stop'
foreach($key in @('PGHOST','PGUSER')) {
    if([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($key))) { throw "Set $key first" }
}
if($TargetDatabase -notmatch '^[a-zA-Z][a-zA-Z0-9_]{0,62}$' -or $SourceDatabase -eq $TargetDatabase) {
    throw 'Choose a distinct, explicitly named empty target database. Source and target must differ.'
}
if(!(Test-Path -LiteralPath $BackupFile) -or !(Test-Path -LiteralPath ($BackupFile+'.sha256'))) { throw 'Backup or SHA256 file missing' }
$expected=(Get-Content -LiteralPath ($BackupFile+'.sha256') -Raw).Trim()
if($expected -notmatch '^[a-fA-F0-9]{64}$' -or (Get-FileHash -LiteralPath $BackupFile -Algorithm SHA256).Hash -ne $expected) { throw 'Backup SHA256 mismatch; no restore attempted' }
# Refuse any user relations, in every non-system schema. Never use --clean.
$count=& $Psql -w -X -A -t --set ON_ERROR_STOP=1 --dbname $TargetDatabase --command "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname NOT IN ('pg_catalog','information_schema') AND n.nspname NOT LIKE 'pg_toast%' AND c.relkind IN ('r','p','v','m','S','f');"
if($LASTEXITCODE -ne 0){throw 'Cannot inspect target database'}
if(($count -join '').Trim() -ne '0'){throw 'Target database is not empty; no restore attempted'}
& $PgRestore -w --exit-on-error --single-transaction --no-owner --no-privileges --dbname $TargetDatabase $BackupFile
if($LASTEXITCODE -ne 0){throw 'Restore failed; the transaction was rolled back'}
Write-Output 'Restore completed in the separate database. Validate migrations, record counts, stock, order amounts and photos before using it.'
