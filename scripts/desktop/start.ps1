param([switch]$NoBrowser,[switch]$SkipBuild,[switch]$Preview)
$ErrorActionPreference='Stop'
# Keep module configuration unchanged outside this process, including when launched by PS7 or IntelliJ.
$builtins=Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/Modules'
if($PSVersionTable.PSEdition -eq 'Desktop'){$env:PSModulePath=$builtins+';'+$env:PSModulePath}
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Set-Location -LiteralPath $root
$area=Join-Path $root 'data/desktop';New-Item -ItemType Directory -Path $area -Force | Out-Null
$stage='Подготовка'
$startupMutex=$null;$startupLock=$false
$previousConsoleEncoding=$null
try{$previousConsoleEncoding=[Console]::OutputEncoding;[Console]::OutputEncoding=New-Object Text.UTF8Encoding($false)}catch{}
function Owned-Process([string]$PidFile,[string]$Jar,[int]$Port,[string]$Database){
 if(!(Test-Path -LiteralPath $PidFile)){return $null}
 $pidValue=[int](Get-Content -LiteralPath $PidFile)
 $process=Get-CimInstance Win32_Process -Filter "ProcessId=$pidValue"
 if(!$process){return $null}
 if(!$process.CommandLine.Contains($Jar) -or !$process.CommandLine.Contains("--server.port=$Port") -or !$process.CommandLine.Contains('/'+$Database)){throw 'ARIA: В сохранённом адресе работает другой процесс. Его настройки сохранены.'}
 return $process
}
function Healthy([int]$Port){try{return (Invoke-RestMethod -Uri "http://127.0.0.1:$Port/actuator/health" -TimeoutSec 3).status -eq 'UP'}catch{return $false}}
function Needs-Update([string]$Jar,[object]$Process){if(!$Process){return $false};if(!(Test-Path -LiteralPath $Jar)){throw 'ARIA: Не найдена сборка работающего экземпляра.'};return (Get-FileHash -LiteralPath $Jar -Algorithm SHA256).Hash -ne $script:jarHash}
function Start-IsolatedService([string]$Name,[bool]$Restart){
 $operation=[guid]::NewGuid().ToString('N');$runner=Join-Path $PSScriptRoot 'service.ps1'
 $arguments=@('-NoProfile','-ExecutionPolicy','Bypass','-File',('"'+$runner+'"'),'-Service',$Name,'-Operation',$operation,'-NodePath',('"'+$script:desktopNode+'"'));if($Restart){$arguments+='-Restart'}
 # ShellExecute provides a separate console/handle set. The service process must not inherit
 # the launcher's command pipes, including when ARIA starts from IntelliJ or a parent Node task.
 $service=Start-Process -FilePath powershell.exe -ArgumentList $arguments -WorkingDirectory $root -Verb Open -WindowStyle Hidden -PassThru
 if(!$service.WaitForExit(120000)){throw ('ARIA: Служба '+$Name+' не подтвердила готовность. Данные сохранены.')}
 $receipt=Join-Path $area ($operation+'.json')
 if(!(Test-Path -LiteralPath $receipt) -or !(Get-Content -LiteralPath $receipt -Raw -Encoding UTF8 | ConvertFrom-Json).success){throw ('ARIA: Не удалось запустить '+$Name+'. Подробности — data/desktop/'+$Name+'-'+$operation+'.log')}
}
try{
 $identitySha=[Security.Cryptography.SHA256]::Create();$identityHash=[BitConverter]::ToString($identitySha.ComputeHash([Text.Encoding]::UTF8.GetBytes($root.ToLowerInvariant()))).Replace('-','').Substring(0,20);$identitySha.Dispose()
 $startupMutex=New-Object System.Threading.Mutex($false,('Local\ARIA_Startup_'+$identityHash))
 try{$startupLock=$startupMutex.WaitOne(0)}catch [System.Threading.AbandonedMutexException]{$startupLock=$true}
 if(!$startupLock){Write-Output 'ARIA уже запускается. Ожидаю готовности...';try{$startupLock=$startupMutex.WaitOne(180000)}catch [System.Threading.AbandonedMutexException]{$startupLock=$true}}
 if(!$startupLock){throw 'ARIA: Предыдущий запуск ещё не завершён. Дождитесь его результата.'}
 & icacls.exe $area /inheritance:r /grant:r "$([System.Security.Principal.WindowsIdentity]::GetCurrent().Name):(OI)(CI)(F)" | Out-Null
 if($LASTEXITCODE -ne 0){throw 'ARIA: Не удалось закрыть доступ к локальным настройкам.'}
 $stage='Проверка зависимостей';Write-Output 'ARIA: проверяю запуск...'
 $nodeCommand=Get-Command node.exe -ErrorAction SilentlyContinue
 if($nodeCommand){$node=$nodeCommand.Source}else{
  $nodeCandidates=@((Join-Path $env:USERPROFILE '.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node.exe'),(Join-Path $env:ProgramFiles 'nodejs/node.exe'))
  $node=$nodeCandidates | Where-Object {Test-Path -LiteralPath $_} | Select-Object -First 1
 }
 if(!$node){throw 'ARIA: Не найден Node.js. Установите бесплатную LTS-версию с nodejs.org и снова откройте ARIA. Вводить команды Node не требуется.'}
 $script:desktopNode=$node
 $nodeVersion=& $node --version
 if($LASTEXITCODE -ne 0 -or $nodeVersion -notmatch '^v(\d+)\.' -or [int]$Matches[1] -lt 20){throw 'ARIA: Нужен Node.js 20 или новее. После обновления снова откройте ARIA.'}
 $jdkCandidates=@();if($env:JAVA_HOME){$jdkCandidates+=$env:JAVA_HOME};$jdkCandidates+=(Join-Path $env:USERPROFILE '.jdks/ms-21.0.12.1')
 if(Test-Path -LiteralPath (Join-Path $env:USERPROFILE '.jdks')){$jdkCandidates+=Get-ChildItem -LiteralPath (Join-Path $env:USERPROFILE '.jdks') -Directory | Select-Object -ExpandProperty FullName}
 $jdk=$null
 foreach($candidate in ($jdkCandidates | Select-Object -Unique)){
  if(!(Test-Path -LiteralPath (Join-Path $candidate 'bin/java.exe'))){continue}
  $oldPreference=$ErrorActionPreference;$ErrorActionPreference='Continue'
  $version=& (Join-Path $candidate 'bin/java.exe') -version 2>&1 | Out-String
  $ErrorActionPreference=$oldPreference
  if($version -match 'version "21[.\"]'){$jdk=$candidate;break}
 }
 if(!$jdk){throw 'ARIA: Нужен Java JDK 21. Установите его через IntelliJ и повторите запуск.'}
 $env:JAVA_HOME=$jdk
 if(!(Test-Path -LiteralPath 'D:/PostgreSQL/bin/psql.exe') -or !(Test-Path -LiteralPath 'data/preview/config.json')){throw 'ARIA: Локальный PostgreSQL ещё не настроен. Существующие базы не изменены; требуется первоначальная установка среды.'}
 $stage='Сборка приложения'
 $revision='development';$git=Get-Command git.exe -ErrorAction SilentlyContinue
 if($git){$revision=(& git rev-parse HEAD).Trim();if($LASTEXITCODE -ne 0){$revision='development'};if((& git status --porcelain -- src pom.xml).Count -gt 0){$revision+='-dirty'}}
 $files=Get-ChildItem -LiteralPath src -Recurse -File | Where-Object {$_.FullName -notmatch '[\\/]test[\\/]'} | Sort-Object FullName
 $fingerprintText=(@($files | ForEach-Object {($_.FullName.Substring($root.Length))+':'+(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash})+('pom:'+(Get-FileHash -LiteralPath pom.xml -Algorithm SHA256).Hash)) -join "`n"
 $sha=[Security.Cryptography.SHA256]::Create();$fingerprint=[BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($fingerprintText))).Replace('-','');$sha.Dispose()
 $jar=Join-Path $root 'target/aria-backend-0.0.1-SNAPSHOT.jar';$buildPath=Join-Path $area 'build.json';$previous=$null
 if(Test-Path -LiteralPath $buildPath){try{$previous=Get-Content -LiteralPath $buildPath -Raw -Encoding UTF8 | ConvertFrom-Json}catch{}}
 $fresh=$previous -and (Test-Path -LiteralPath $jar) -and $previous.fingerprint -eq $fingerprint -and $previous.revision -eq $revision -and $previous.jarHash -eq (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash
 if(!$fresh -and !$SkipBuild){
  Write-Output 'ARIA: собираю актуальную версию...';$oldPreference=$ErrorActionPreference;$ErrorActionPreference='Continue'
  & ./mvnw.cmd -q -DskipTests "-Daria.build.revision=$revision" package *> (Join-Path $area 'build.log');$result=$LASTEXITCODE;$ErrorActionPreference=$oldPreference
  if($result -ne 0){throw 'ARIA: Сборка не завершилась. Подробности находятся в data/desktop/build.log.'}
 }
 if(!(Test-Path -LiteralPath $jar)){throw 'ARIA: Не найдена готовая сборка. Повторите обычный запуск без пропуска сборки.'}
 $script:jarHash=(Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash
 if(!$SkipBuild){@{fingerprint=$fingerprint;revision=$revision;jarHash=$script:jarHash;builtAt=[DateTime]::UtcNow.ToString('o')} | ConvertTo-Json | Set-Content -LiteralPath $buildPath -Encoding UTF8}
 # The IDE's original module may have no Maven/source roots. A tiny JDK-only bootstrap JAR
 # runs independently of that module and invokes exactly this launcher, never a demo profile.
 $desktopJar=Join-Path $area 'launcher.jar'
 if(!(Test-Path -LiteralPath $desktopJar)){
  & (Join-Path $jdk 'bin/jar.exe') --create --file $desktopJar --main-class com.aria.AriaDesktopLauncher -C target/classes com/aria/AriaDesktopLauncher.class
  if($LASTEXITCODE -ne 0){throw 'ARIA: Не удалось подготовить запуск из IntelliJ.'}
 }
 $stage='Запуск портала';Write-Output 'ARIA: запускаю кабинет и проверяю соединение...'
 $deskJar=Join-Path $root 'data/onboarding/registry.jar';$desk=Owned-Process (Join-Path $root 'data/onboarding/registry.pid') $deskJar 8088 'aria_registration_local'
 Start-IsolatedService 'registration' ((Needs-Update $deskJar $desk) -or ($desk -and !(Healthy 8088)))
 if(!(Healthy 8088)){throw 'ARIA: Портал компаний не подтвердил готовность. Данные сохранены.'}
 $entry='http://127.0.0.1:8088/start.html'
 if(Test-Path -LiteralPath data/onboarding/workspace-config.enc){
  $stage='Запуск компании';$workspaceJar=Join-Path $root 'data/onboarding/workspace.jar';$workspace=Owned-Process (Join-Path $root 'data/onboarding/workspace.pid') $workspaceJar 8089 'aria_onboarding_shop'
  Start-IsolatedService 'workspace' ((Needs-Update $workspaceJar $workspace) -or ($workspace -and (!$workspace.CommandLine.Contains('local-desktop') -or !(Healthy 8089))))
  if(!(Healthy 8089)){throw 'ARIA: Кабинет компании не подтвердил готовность. Данные сохранены.'}
  $entry='http://127.0.0.1:8089/start.html'
 }
 $stage='Запуск витрины';$demoJar=Join-Path $root 'data/localization-preview/app.jar';$demo=Owned-Process (Join-Path $root 'data/localization-preview/app.pid') $demoJar 8087 'aria_locale_demo'
 Start-IsolatedService 'storefront' ((Needs-Update $demoJar $demo) -or ($demo -and !(Healthy 8087)))
 if(!(Healthy 8087)){throw 'ARIA: Учебная витрина не подтвердила готовность. Данные сохранены.'}
 @{entry=$entry;revision=$revision;jarHash=$script:jarHash;readyAt=[DateTime]::UtcNow.ToString('o');registrationReady=$true;workspaceReady=($entry -like '*8089*');storefrontReady=$true;productionChanged=$false} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $area 'ready.json') -Encoding UTF8
 # The outer workspace is an IDE container; the versioned launcher lives at the Git root.
 $parent=Split-Path $root -Parent
 if((Test-Path -LiteralPath (Join-Path $parent '.idea')) -and (Split-Path $root -Leaf) -eq 'backend'){
  $shortcut=Join-Path $parent 'ARIA.cmd';if(!(Test-Path -LiteralPath $shortcut)){[IO.File]::WriteAllText($shortcut,"@echo off`r`ncall `"%~dp0backend\ARIA.cmd`" %*`r`n",[Text.Encoding]::ASCII)}
  $previewShortcut=Join-Path $parent 'ARIA-preview.cmd';if(!(Test-Path -LiteralPath $previewShortcut)){[IO.File]::WriteAllText($previewShortcut,"@echo off`r`ncall `"%~dp0backend\ARIA-preview.cmd`" %*`r`n",[Text.Encoding]::ASCII)}
  $runDirectory=Join-Path $parent '.run';New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null
  $runFile=Join-Path $runDirectory 'ARIA.run.xml';$canInstall=!(Test-Path -LiteralPath $runFile)
  if(!$canInstall){$existingRun=Get-Content -LiteralPath $runFile -Raw -Encoding UTF8;$canInstall=$existingRun.Contains('name="ARIA — запуск"') -and $existingRun.Contains('value="com.aria.AriaDesktopLauncher"')}
  if($canInstall){$runTemplate=Get-Content -LiteralPath '.run/ARIA.run.xml' -Raw -Encoding UTF8;$runTemplate=$runTemplate.Replace('$PROJECT_DIR$/data/','$PROJECT_DIR$/backend/data/');[IO.File]::WriteAllText($runFile,$runTemplate,[Text.Encoding]::UTF8)}
 }
 if(!$NoBrowser -and $Preview){
  $previewReceipt=Join-Path $area ('preview-'+[guid]::NewGuid().ToString('N')+'.json')
  $previewScript=Join-Path $root 'scripts/onboarding/open-owner-preview.cjs'
  Start-Process -FilePath $node -ArgumentList @(('"'+$previewScript+'"'),'--home',('"--ready-file='+$previewReceipt+'"')) -WorkingDirectory $root -Verb Open -WindowStyle Hidden | Out-Null
  $deadline=[DateTime]::UtcNow.AddSeconds(30)
  while(!(Test-Path -LiteralPath $previewReceipt) -and [DateTime]::UtcNow -lt $deadline){Start-Sleep -Milliseconds 250}
  if(!(Test-Path -LiteralPath $previewReceipt) -or !(Get-Content -LiteralPath $previewReceipt -Raw -Encoding UTF8 | ConvertFrom-Json).opened){throw 'ARIA: Не удалось открыть учебный кабинет. Обычный вход доступен на стартовой странице.'}
 }elseif(!$NoBrowser){Start-Process $entry}
 Write-Output ('ARIA готова: '+$entry)
 exit 0
}catch{
 ($stage+"`n"+$_.Exception.Message) | Set-Content -LiteralPath (Join-Path $area 'start.log') -Encoding UTF8
 $message=if($_.Exception.Message.StartsWith('ARIA:')){$_.Exception.Message}else{'Не удалось завершить этап: '+$stage+'. Данные сохранены. Подробности — data/desktop/start.log.'}
 Write-Output $message
 if(!$NoBrowser){try{Add-Type -AssemblyName System.Windows.Forms;[Windows.Forms.MessageBox]::Show($message,'ARIA') | Out-Null}catch{}}
 exit 1
}finally{
 if($startupMutex){if($startupLock){try{$startupMutex.ReleaseMutex()}catch{}};$startupMutex.Dispose()}
 if($previousConsoleEncoding){try{[Console]::OutputEncoding=$previousConsoleEncoding}catch{}}
}
