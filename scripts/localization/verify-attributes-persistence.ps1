$ErrorActionPreference='Stop'
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Set-Location -LiteralPath $root
$area=Join-Path $root 'data/localization-preview';$jar=Join-Path $area 'app.jar';$pidPath=Join-Path $area 'app.pid'
$proof=Get-Content -LiteralPath docs/localization/evidence/product-attributes.json -Raw -Encoding UTF8 | ConvertFrom-Json
if(!$proof.passed){throw 'Complete the educational attribute UI check first'}
$expected=$proof.scopes | Where-Object {$_.binding -eq 'demo'} | Select-Object -First 1
if(!$expected -or [long]$expected.productId -le 0){throw 'Missing public educational fixture'}
$previousId=[int](Get-Content -LiteralPath $pidPath)
$owned=Get-CimInstance Win32_Process -Filter "ProcessId=$previousId"
if(!$owned -or !$owned.CommandLine.Contains($jar) -or !$owned.CommandLine.Contains('--server.port=8087') -or !$owned.CommandLine.Contains('jdbc:postgresql://127.0.0.1:55434/aria_locale_demo')){throw 'This is not the owned public educational process; preserved it'}
$url='http://127.0.0.1:8087/api/products/'+[long]$expected.productId
function Read-Product([string]$uri){
 # Windows PowerShell 5 assumes a legacy encoding for JSON without a charset.
 $response=Invoke-WebRequest -Uri $uri -UseBasicParsing
 $response.RawContentStream.Position=0
 $reader=[IO.StreamReader]::new($response.RawContentStream,[Text.Encoding]::UTF8)
 try{return ($reader.ReadToEnd() | ConvertFrom-Json)}finally{$reader.Dispose()}
}
$before=Read-Product $url
Stop-Process -Id $previousId
Wait-Process -Id $previousId -ErrorAction SilentlyContinue
& (Join-Path $PSScriptRoot 'start-public-preview.ps1') -SkipBuild
if($LASTEXITCODE -ne 0){throw 'Public preview restart failed'}
$after=Read-Product $url
if($after.details.color -ne $expected.color -or $after.details.size -ne $expected.size -or [decimal]$after.details.weightKg -ne [decimal]$expected.weightKg){throw 'Persisted attributes differ after restart'}
if($after.product.unitPrice -ne $before.product.unitPrice -or $after.product.quantityOnHand -ne $before.product.quantityOnHand -or $after.product.sku -ne $before.product.sku){throw 'Price, stock or identity changed during restart'}
@{passed=$true;previousProcess=$previousId;newProcess=[int](Get-Content -LiteralPath $pidPath);productId=[long]$expected.productId;attributesPersisted=$true;priceAndStockPreserved=$true;productWritesPerformed=$false;externalAiEnabled=$false;productionChanged=$false} | ConvertTo-Json | Set-Content -LiteralPath docs/localization/evidence/attributes-persistence.json -Encoding UTF8
Write-Output 'PASS: educational product attributes survived an actual JVM restart; price, stock and SKU unchanged'
