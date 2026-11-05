param([string]$PythonExe='',[string]$NodeExe='')
$ErrorActionPreference='Stop'
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$area=Join-Path $root 'data/woocommerce'
New-Item -ItemType Directory -Path $area -Force | Out-Null
& icacls.exe $area /inheritance:r /grant:r "$([System.Security.Principal.WindowsIdentity]::GetCurrent().Name):(OI)(CI)(F)" | Out-Null
if($LASTEXITCODE -ne 0){throw 'Cannot protect isolated credentials'}
if(!$PythonExe){$PythonExe=Join-Path $env:USERPROFILE '.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe'}
if(!$NodeExe){$NodeExe=(Get-Command node -ErrorAction Stop).Source}
if(!(Test-Path -LiteralPath (Join-Path $area 'runtime.json'))){& $PythonExe (Join-Path $PSScriptRoot 'download-runtime.py');if($LASTEXITCODE -ne 0){throw 'Runtime download failed'}}
$configPath=Join-Path $area 'config.json'
if(!(Test-Path -LiteralPath $configPath)){@{databasePassword=[guid]::NewGuid().ToString('N');ownerPassword=('ARIA-Woo-'+[guid]::NewGuid().ToString('N'))} | ConvertTo-Json | Set-Content -LiteralPath $configPath -Encoding UTF8}
$cfg=Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
$php=Join-Path $area 'php/php.exe'
$phpIni=Join-Path $area 'php/php.ini'
if(!(Test-Path -LiteralPath $phpIni)){@('extension_dir="ext"','extension=mysqli','extension=curl','extension=mbstring','extension=openssl','extension=zip','extension=gd','memory_limit=512M','max_execution_time=90','display_errors=Off','log_errors=On',('error_log="'+(Join-Path $area 'php-errors.log').Replace('\','/')+'"')) | Set-Content -LiteralPath $phpIni -Encoding ascii}
$mdb=Get-ChildItem -LiteralPath (Join-Path $area 'mariadb') -Directory | Select-Object -First 1
$db=Join-Path $area 'database'
if(!(Test-Path -LiteralPath (Join-Path $db 'mysql'))){& (Join-Path $mdb.FullName 'bin/mariadb-install-db.exe') "--datadir=$db" "--password=$($cfg.databasePassword)" --port=53389; if($LASTEXITCODE -ne 0){throw 'Isolated MariaDB initialization failed'}}
function Start-Owned([string]$Name,[string]$Exe,[string[]]$ArgsList,[int]$Port){
 $pidPath=Join-Path $area ($Name+'.pid')
 if(Test-Path -LiteralPath $pidPath){$p=Get-CimInstance Win32_Process -Filter "ProcessId=$([int](Get-Content -LiteralPath $pidPath))";if($p -and $p.ExecutablePath -eq $Exe -and ($p.CommandLine.Contains($area) -or ($Name -eq 'https' -and $p.CommandLine.Contains((Join-Path $PSScriptRoot 'https-proxy.cjs'))))){return}}
 if(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue){throw "Port $Port occupied; no existing process stopped"}
 $p=Start-Process -FilePath $Exe -ArgumentList $ArgsList -WorkingDirectory (Split-Path $Exe -Parent) -WindowStyle Hidden -RedirectStandardOutput (Join-Path $area ($Name+'.out.log')) -RedirectStandardError (Join-Path $area ($Name+'.err.log')) -PassThru
 $p.Id | Set-Content -LiteralPath $pidPath
}
Start-Owned 'database' (Join-Path $mdb.FullName 'bin/mariadbd.exe') @('--no-defaults',('--datadir="'+$db+'"'),'--bind-address=127.0.0.1','--port=53389','--max-connections=20','--innodb-buffer-pool-size=64M','--skip-name-resolve','--console') 53389
$env:MYSQL_PWD=$cfg.databasePassword
$dbReady=$false
for($i=0;$i -lt 20;$i++){& (Join-Path $mdb.FullName 'bin/mariadb-admin.exe') --skip-ssl --protocol=tcp --host=127.0.0.1 --port=53389 --user=root ping 2>$null | Out-Null;if($LASTEXITCODE -eq 0){$dbReady=$true;break};Start-Sleep -Milliseconds 500}
if(!$dbReady){throw 'Isolated MariaDB not ready; check data/woocommerce logs'}
& (Join-Path $mdb.FullName 'bin/mariadb.exe') --skip-ssl --protocol=tcp --host=127.0.0.1 --port=53389 --user=root --execute='CREATE DATABASE IF NOT EXISTS aria_woo_e2e CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci'
Remove-Item Env:MYSQL_PWD
if($LASTEXITCODE -ne 0){throw 'Cannot create isolated WordPress database'}
if(!(Test-Path -LiteralPath (Join-Path $area 'fixtures.json'))){
 & $php (Join-Path $PSScriptRoot 'install-site.php');if($LASTEXITCODE -ne 0){throw 'WordPress bootstrap failed'}
 & $php (Join-Path $PSScriptRoot 'install-site.php');if($LASTEXITCODE -ne 0){throw 'WooCommerce bootstrap failed'}
}
if(!(Test-Path -LiteralPath (Join-Path $area 'fixtures.json'))){throw 'WooCommerce installation did not create actual fixture products and REST credentials'}
# Node's built-in crypto creates a key; OpenSSL signs only the isolated certificate.
$openssl=Join-Path $area 'php/extras/ssl/openssl.exe'
if(!(Test-Path -LiteralPath (Join-Path $area 'tls-cert.pem'))){
 $gitOpenSsl='C:/Program Files/Git/usr/bin/openssl.exe'
 if(!(Test-Path -LiteralPath $gitOpenSsl)){throw 'OpenSSL is required to create the isolated local certificate'}
 $ErrorActionPreference='Continue'
 & $gitOpenSsl req -x509 -newkey rsa:2048 -nodes -keyout (Join-Path $area 'tls-key.pem') -out (Join-Path $area 'tls-cert.pem') -days 365 -subj '/CN=ARIA isolated WooCommerce' -addext 'subjectAltName=IP:127.0.0.1' 2>$null
 $ErrorActionPreference='Stop'
 if($LASTEXITCODE -ne 0){throw 'Local certificate creation failed'}
}
Start-Owned 'php' $php @('-S','127.0.0.1:9089','-t',('"'+(Join-Path $area 'site/wordpress')+'"'),('"'+(Join-Path $PSScriptRoot 'router.php')+'"')) 9089
Start-Owned 'https' $NodeExe @(('"'+(Join-Path $PSScriptRoot 'https-proxy.cjs')+'"')) 8449
@('EDUCATIONAL ISOLATED WOO ONLY','Shop: https://127.0.0.1:8449','Admin: https://127.0.0.1:8449/wp-admin/','Username: aria_owner',('Password: '+$cfg.ownerPassword),'REST credentials: data/woocommerce/fixtures.json. Never commit these files.','Local certificate is not installed in Windows. Browser warning applies only to this test shop.') | Set-Content -LiteralPath (Join-Path $area 'access.txt') -Encoding UTF8
Write-Output 'Isolated real WooCommerce ready: https://127.0.0.1:8449. Login details: data/woocommerce/access.txt'
