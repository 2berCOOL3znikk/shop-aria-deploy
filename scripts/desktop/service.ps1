param([Parameter(Mandatory=$true)][ValidateSet('registration','workspace','storefront')][string]$Service,[Parameter(Mandatory=$true)][ValidatePattern('^[a-f0-9]{32}$')][string]$Operation,[string]$NodePath,[switch]$Restart)
$ErrorActionPreference='Stop'
if($PSVersionTable.PSEdition -eq 'Desktop'){$env:PSModulePath=(Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/Modules')+';'+$env:PSModulePath}
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent;Set-Location -LiteralPath $root
$area=Join-Path $root 'data/desktop';$log=Join-Path $area ($Service+'-'+$Operation+'.log');$result=Join-Path $area ($Operation+'.json')
try{
 $options=@{SkipBuild=$true};if($Restart){$options.Restart=$true}
 if($Service -eq 'registration'){& ./scripts/onboarding/start-local.ps1 @options *> $log}
 elseif($Service -eq 'storefront'){& ./scripts/localization/start-public-preview.ps1 @options *> $log}
 else{
  if(!$NodePath -or !(Test-Path -LiteralPath $NodePath)){throw 'Node executable not found'}
  $arguments=@('scripts/onboarding/workspace-local.cjs','--current','--desktop');if($Restart){$arguments+='--restart'}
  & $NodePath @arguments *> $log
 }
 if($LASTEXITCODE -ne 0){throw 'Service start failed'}
 @{success=$true;service=$Service;operation=$Operation} | ConvertTo-Json | Set-Content -LiteralPath $result -Encoding UTF8
 exit 0
}catch{
 $_.Exception.Message | Add-Content -LiteralPath $log -Encoding UTF8
 @{success=$false;service=$Service;operation=$Operation} | ConvertTo-Json | Set-Content -LiteralPath $result -Encoding UTF8
 exit 1
}
