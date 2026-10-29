param([string]$PostgresBin='D:\PostgreSQL\bin')
$ErrorActionPreference='Stop'
$preview=Join-Path (Split-Path $PSScriptRoot -Parent) 'data\preview'
$pidFile=Join-Path $preview 'app.pid'
if(Test-Path -LiteralPath $pidFile){
    $previewPid=[int](Get-Content -LiteralPath $pidFile)
    $process=Get-CimInstance Win32_Process -Filter "ProcessId=$previewPid"
    if($process){if(!$process.CommandLine.Contains((Join-Path $preview 'app.jar'))){throw 'PID belongs to another process; nothing stopped'};Stop-Process -Id $previewPid;Wait-Process -Id $previewPid -ErrorAction SilentlyContinue}
    Remove-Item -LiteralPath $pidFile
}
$cluster=Join-Path $preview 'postgres'
if(Test-Path -LiteralPath (Join-Path $cluster 'PG_VERSION')){
    & (Join-Path $PostgresBin 'pg_ctl.exe') -D $cluster status *> $null
    if($LASTEXITCODE -eq 0){& (Join-Path $PostgresBin 'pg_ctl.exe') -D $cluster -w stop;if($LASTEXITCODE -ne 0){throw 'Cannot stop preview cluster'}}
}
Write-Output 'Only the local preview was stopped. Its database and uploaded photos are preserved.'
