param([switch]$NoBrowser)
$ErrorActionPreference='Stop'
if($PSVersionTable.PSEdition -eq 'Desktop'){$env:PSModulePath=(Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/Modules')+';'+$env:PSModulePath}
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent;Set-Location -LiteralPath $root
$area=Join-Path $root 'data/desktop';$operation=[guid]::NewGuid().ToString('N');$node=(Get-Command node.exe).Source
$runner=Join-Path $PSScriptRoot 'service.ps1';$arguments=@('-NoProfile','-ExecutionPolicy','Bypass','-File',('"'+$runner+'"'),'-Service','woocommerce','-Operation',$operation)
$process=Start-Process -FilePath powershell.exe -ArgumentList $arguments -WorkingDirectory $root -Verb Open -WindowStyle Hidden -PassThru
if(!$process.WaitForExit(180000)){throw 'Isolated preview is not ready. Data preserved.'}
$receipt=Join-Path $area ($operation+'.json')
if(!(Test-Path -LiteralPath $receipt) -or !(Get-Content -LiteralPath $receipt -Raw -Encoding UTF8 | ConvertFrom-Json).success){throw ('See private log data/desktop/woocommerce-'+$operation+'.log')}
if(!$NoBrowser){$ready=Join-Path $area ('preview-'+[guid]::NewGuid().ToString('N')+'.json');$script=Join-Path $root 'scripts/onboarding/open-owner-preview.cjs';Start-Process -FilePath $node -ArgumentList @(('"'+$script+'"'),'--woo',('"--ready-file='+$ready+'"')) -WorkingDirectory $root -Verb Open -WindowStyle Hidden | Out-Null;$limit=[DateTime]::UtcNow.AddSeconds(30);while(!(Test-Path -LiteralPath $ready) -and [DateTime]::UtcNow -lt $limit){Start-Sleep -Milliseconds 250};if(!(Test-Path -LiteralPath $ready) -or !(Get-Content -LiteralPath $ready -Raw -Encoding UTF8 | ConvertFrom-Json).opened){throw 'The teaching application is ready, but browser sign-in was not confirmed'}}
Write-Output 'Isolated WooCommerce teaching workspace: http://127.0.0.1:8085/control.html#manager. No automatic AI request or price change on launch.'
