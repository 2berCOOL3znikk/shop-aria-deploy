param([Parameter(Mandatory=$true)][ValidateSet('registration','workspace','storefront','woocommerce')][string]$Service,[Parameter(Mandatory=$true)][ValidatePattern('^[a-f0-9]{32}$')][string]$Operation,[string]$NodePath,[switch]$Restart)
$ErrorActionPreference='Stop'
if($PSVersionTable.PSEdition -eq 'Desktop'){$env:PSModulePath=(Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/Modules')+';'+$env:PSModulePath}
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent;Set-Location -LiteralPath $root
$area=Join-Path $root 'data/desktop';$log=Join-Path $area ($Service+'-'+$Operation+'.log');$result=Join-Path $area ($Operation+'.json')
try{
 $options=@{SkipBuild=$true};if($Restart){$options.Restart=$true}
 if($Service -eq 'registration'){& ./scripts/onboarding/start-local.ps1 @options *> $log}
 elseif($Service -eq 'storefront'){& ./scripts/localization/start-public-preview.ps1 @options *> $log}
 elseif($Service -eq 'woocommerce'){
  if(!(Test-Path -LiteralPath data/woocommerce/fixtures.json)){throw 'The existing isolated WooCommerce installation is required'}
  & ./scripts/woocommerce/start-local.ps1 *> $log;if($LASTEXITCODE -ne 0){throw 'Isolated WooCommerce start failed'}
  # The approved teaching company only. No background model calls or price writes on launch.
  if(Test-Path -LiteralPath .env.openrouter.local){foreach($line in [IO.File]::ReadAllLines((Join-Path $root '.env.openrouter.local'))){if($line -match '^\s*(ARIA_OPENROUTER_API_KEY|ARIA_OPENROUTER_MODEL)\s*=(.*)$'){$key=$Matches[1];$value=$Matches[2].Trim().Trim('"').Trim("'");if($key -eq 'ARIA_OPENROUTER_MODEL' -and $value -ne 'openrouter/free' -and !$value.EndsWith(':free')){throw 'Only a free model can be used in this teaching preview'};[Environment]::SetEnvironmentVariable($key,$value,'Process')}}}
  $env:ARIA_MANAGER_ENABLED='false';$env:ARIA_MANAGER_ACTIVITY_ENABLED='false';$env:ARIA_MANAGER_SCHEDULE_ENABLED='false';$env:ARIA_CONNECTED_PRICING_SCHEDULE_ENABLED='false';$env:ARIA_STORE_SYNC_WORKER_ENABLED='false';$env:ARIA_INTELLIGENCE_FREE_ONLY='true'
  & ./scripts/woocommerce/start-aria.ps1 -SkipBuild -Restart *>> $log
 }
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
