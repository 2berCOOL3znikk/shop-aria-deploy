param([ValidateSet('php','https','aria')][string]$Component)
$ErrorActionPreference='Stop'
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$area=Join-Path $root 'data/woocommerce'
$file=Join-Path $area ($Component+'.pid')
if(!(Test-Path -LiteralPath $file)){exit 0}
$ownedPid=[int](Get-Content -LiteralPath $file)
$p=Get-CimInstance Win32_Process -Filter "ProcessId=$ownedPid"
if(!$p){exit 0}
$valid=if($Component -eq 'https'){$p.CommandLine.Contains((Join-Path $PSScriptRoot 'https-proxy.cjs'))}elseif($Component -eq 'aria'){$p.CommandLine.Contains((Join-Path $area 'aria.jar')) -and $p.CommandLine.Contains('--server.port=8085')}else{$p.CommandLine.Contains((Join-Path $area 'site/wordpress')) -and $p.CommandLine.Contains('127.0.0.1:9089')}
if(!$valid){throw 'PID is not the owned isolated component; preserved it'}
Stop-Process -Id $ownedPid
Wait-Process -Id $ownedPid -ErrorAction SilentlyContinue
Write-Output ('Stopped isolated '+$Component)
