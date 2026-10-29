param([string]$PostgresBin='D:\PostgreSQL\bin',[string]$JavaHome=(Join-Path $env:USERPROFILE '.jdks/ms-21.0.12.1'))
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $root
$preview=Get-Content data/preview/config.json -Raw|ConvertFrom-Json
if($preview.databasePort -ne 55434){throw 'Only the owned local preview cluster 55434 is supported'}
$stamp=[DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$out=Join-Path $root "data/b2b-isolation-$stamp"
New-Item -ItemType Directory -Path $out|Out-Null
$sourceJar=(Resolve-Path target/aria-backend-0.0.1-SNAPSHOT.jar).Path
$jar=Join-Path $out 'app.jar'
Copy-Item -LiteralPath $sourceJar -Destination $jar
$owned=@();$companies=@();$report=@{passed=$false;checks=@()}
function Sql([string]$database,[string]$statement){
 $env:PGHOST='127.0.0.1';$env:PGPORT='55434';$env:PGUSER='aria_preview';$env:PGPASSWORD=$preview.databasePassword
 $statement | & (Join-Path $PostgresBin 'psql.exe') -X -v ON_ERROR_STOP=1 -d $database *> (Join-Path $out 'sql.log')
 if($LASTEXITCODE -ne 0){throw 'Isolated database operation failed; see local sql.log'}
}
function StartCompany($c,[string]$binding){
 if(Get-NetTCPConnection -LocalPort $c.port -State Listen -ErrorAction SilentlyContinue){throw 'Test port is occupied; no unrelated process will be stopped'}
 $env:SPRING_DATASOURCE_PASSWORD=$c.password;$env:SPRING_DATASOURCE_USERNAME=$c.database
 $env:ARIA_JWT_SECRET=$preview.jwt # Deliberately identical: issuer must still isolate tokens.
 $env:ARIA_ADMIN_EMAIL='same-owner@b2b.test';$env:ARIA_ADMIN_PASSWORD=$preview.password
 $env:ARIA_ORGANIZATION_KEY=$binding
 $arguments=@('-Duser.timezone=Asia/Qyzylorda','-Xms32m','-Xmx192m','-XX:+UseSerialGC','-XX:ActiveProcessorCount=2','-XX:TieredStopAtLevel=1','-jar',('"'+$jar+'"'),"--server.port=$($c.port)",'--server.address=127.0.0.1',"--spring.datasource.url=jdbc:postgresql://127.0.0.1:55434/$($c.database)",'--spring.profiles.active=postgres,production,free','--aria.intelligence.external-enabled=false','--aria.smart-pricing.schedule-enabled=false','--aria.seed-demo-users=false','--aria.seed-demo-catalog=false')
 $p=Start-Process -FilePath (Join-Path $JavaHome 'bin/java.exe') -ArgumentList $arguments -WorkingDirectory $root -WindowStyle Hidden -RedirectStandardOutput (Join-Path $out "$($c.key)-$binding.log") -RedirectStandardError (Join-Path $out "$($c.key)-$binding.err.log") -PassThru
 $script:owned+= $p
 for($attempt=0;$attempt -lt 90;$attempt++){
  if($p.HasExited){throw 'Isolated application startup failed; inspect local logs'}
  try{if((Invoke-RestMethod "http://127.0.0.1:$($c.port)/actuator/health" -TimeoutSec 2).status -eq 'UP'){return $p}}catch{}
  Start-Sleep -Milliseconds 500
 }
 throw 'Isolated startup timeout'
}
function Api($c,[string]$path,[string]$method='GET',$body=$null,[string]$token=''){
 $args=@{Uri="http://127.0.0.1:$($c.port)$path";Method=$method;ContentType='application/json'}
 if($token){$args.Headers=@{Authorization="Bearer $token"}}
 if($null -ne $body){$args.Body=($body|ConvertTo-Json -Depth 10 -Compress)}
 try { Invoke-RestMethod @args } catch {
  if($_.ErrorDetails.Message){Write-Output ("API failure: "+$method+" "+$path+" "+$_.ErrorDetails.Message)}
  throw
 }
}
try{
 foreach($i in 0,1){
  $key=if($i -eq 0){'company-a'}else{'company-b'}
  $db="b2b_$($stamp)_$i";$pw=[guid]::NewGuid().ToString('N')
  $c=@{key=$key;database=$db;password=$pw;port=(8096+$i)};$companies+=$c
  Sql aria_preview "CREATE ROLE $db LOGIN PASSWORD '$pw' NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT;"
  Sql aria_preview "CREATE DATABASE $db OWNER $db;"
  Sql aria_preview "REVOKE ALL ON DATABASE $db FROM PUBLIC;"
  $p=StartCompany $c 'demo'
  Stop-Process -Id $p.Id;Wait-Process -Id $p.Id -ErrorAction SilentlyContinue
  # Only these fresh empty test databases are provisioned. Never relabel a real/demo business database.
  Sql $db "DO `$`$ BEGIN IF EXISTS(SELECT 1 FROM products) OR EXISTS(SELECT 1 FROM orders) THEN RAISE EXCEPTION 'Not empty'; END IF; END `$`$; UPDATE saas_organization SET organization_key='$key',name='$key',demonstration=false WHERE id=1;"
  $null=StartCompany $c $key
 }
 foreach($c in $companies){
  $login=Api $c '/api/auth/login' 'POST' @{email='same-owner@b2b.test';password=$preview.password}
  $c.token=$login.token
  $current=Api $c '/api/admin/control/organization' 'GET' $null $c.token
  if($current.binding -ne $c.key){throw 'Incorrect organization metadata'}
  foreach($privatePath in '/api/products/store','/api/social/impact','/api/admin/control/organization'){
   $denied=$false
   try{$null=Api $c $privatePath}catch{if([int]$_.Exception.Response.StatusCode -eq 401){$denied=$true}else{throw}}
   if(!$denied){throw 'Anonymous request accessed company business data'}
  }
  $null=Api $c '/api/admin/control/products' 'POST' @{sku='SAME-SKU';name=$c.key;category='Test';unitPrice=1000;unit='pcs';lowStockThreshold=0} $c.token
 }
 foreach($i in 0,1){
  $c=$companies[$i];$other=$companies[1-$i]
  $products=@(Api $c '/api/admin/control/products' 'GET' $null $c.token)
  if($products.Count -ne 1 -or $products[0].name -ne $c.key){throw 'Product data crossed boundary'}
  foreach($path in '/api/admin/control/products','/api/admin/control/manager','/api/admin/control/organization'){
   $denied=$false
   try{$null=Api $c $path 'GET' $null $other.token}catch{if([int]$_.Exception.Response.StatusCode -eq 401){$denied=$true}else{throw}}
   if(!$denied){throw 'Foreign organization token was accepted'}
  }
  $env:PGUSER=$c.database;$env:PGPASSWORD=$c.password
  $denialLog=Join-Path $out "cross-db-$i.err.log"
  $probe=Start-Process -FilePath (Join-Path $PostgresBin 'psql.exe') -ArgumentList @('-X','-d',$other.database,'-c','"select 1"') -WindowStyle Hidden -Wait -PassThru -RedirectStandardOutput (Join-Path $out "cross-db-$i.log") -RedirectStandardError $denialLog
  if($probe.ExitCode -eq 0 -or (Get-Content -LiteralPath $denialLog -Raw) -notmatch 'permission denied for database'){throw 'Database isolation not established'}
 }
 $report.checks=@('Two real PostgreSQL databases, distinct restricted logins, V1-V19 and Hibernate validate','Two real Spring Boot processes, same owner email and signing key','Same SKU stays independent; foreign tokens rejected by product/manager/organization APIs','Database login cannot connect to the other company database; no external AI requests')
 $report.passed=$true
 Write-Output 'PASS: two-company database/process/API isolation'
}finally{
 foreach($p in $owned){if(!$p.HasExited){Stop-Process -Id $p.Id -ErrorAction SilentlyContinue}}
 $report|ConvertTo-Json -Depth 8|Set-Content -LiteralPath (Join-Path $out 'report.json') -Encoding UTF8
 Write-Output "Report: $out\report.json"
}
