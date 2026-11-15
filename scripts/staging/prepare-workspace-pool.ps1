param([switch]$Apply)
$ErrorActionPreference='Stop'
$script:preparationStep='configuration validation'
trap {[Console]::Error.WriteLine("Stopped at $script:preparationStep; no credential values logged");exit 1}
$workspaceRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
Set-Location -LiteralPath $workspaceRoot
$privateRoot=(Resolve-Path -LiteralPath 'data/staging').Path
if($privateRoot -ne (Join-Path $workspaceRoot 'data/staging')){throw 'Unexpected private directory'}
foreach($accessRule in (Get-Acl -LiteralPath $privateRoot).Access){
 $accessSid=$accessRule.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value
 if($accessRule.AccessControlType -eq 'Allow' -and $accessSid -in @('S-1-1-0','S-1-5-11','S-1-5-32-545')){throw 'The private directory must not be readable by other users'}
}
$targets=@(
 @{label='a';project='royal-darkness-00535090';name='ARIA SaaS Slot A'},
 @{label='b';project='late-flower-36533773';name='ARIA SaaS Slot B'}
)
function Random-Bytes([int]$length){$bytes=New-Object byte[] $length;$rng=[Security.Cryptography.RandomNumberGenerator]::Create();try{$rng.GetBytes($bytes)}finally{$rng.Dispose()};return ,$bytes}
function Random-Hex(){return ([BitConverter]::ToString((Random-Bytes 32))).Replace('-','').ToLowerInvariant()}
function Random-Url([int]$length){return [Convert]::ToBase64String((Random-Bytes $length)).TrimEnd('=').Replace('+','-').Replace('/','_')}
function Save-Private($value,[string]$file){[IO.File]::WriteAllText($file,($value|ConvertTo-Json -Depth 10))}
function Database-Call($connection,[string]$sql,[string]$step){
 $script:preparationStep=$step
 $start=New-Object Diagnostics.ProcessStartInfo
 $start.FileName='D:\PostgreSQL\bin\psql.exe';$start.Arguments='-X -q -v ON_ERROR_STOP=1 -A -t'
 $start.UseShellExecute=$false;$start.CreateNoWindow=$true
 $start.RedirectStandardInput=$true;$start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true
 foreach($entry in @{PGHOST=$connection.host;PGPORT='5432';PGDATABASE=$connection.database;PGUSER=$connection.user;PGPASSWORD=$connection.password;PGSSLMODE='require';PGCONNECT_TIMEOUT='20'}.GetEnumerator()){$start.EnvironmentVariables[$entry.Key]=[string]$entry.Value}
 $process=New-Object Diagnostics.Process;$process.StartInfo=$start
 try{
  if(!$process.Start()){throw 'Could not start the database client'}
  $output=$process.StandardOutput.ReadToEndAsync();$diagnostics=$process.StandardError.ReadToEndAsync()
  $process.StandardInput.WriteLine($sql);$process.StandardInput.Close()
  if(!$process.WaitForExit(45000)){$process.Kill();throw "Timeout at $step; no credentials logged"}
  $safeExit=$process.ExitCode;$text=$output.GetAwaiter().GetResult();$null=$diagnostics.GetAwaiter().GetResult()
  if($safeExit -ne 0){throw "Database preparation failed at $step (exit $safeExit); no credentials logged"}
  return $text.Trim()
 }finally{$process.Dispose()}
}
foreach($target in $targets){
 $label=$target.label;$metadata=(Get-Content -Raw -LiteralPath (Join-Path $privateRoot "p13-slot-$label-project.json")|ConvertFrom-Json).project
 $script:preparationStep="slot $label configuration validation"
 if($metadata.id -ne $target.project -or $metadata.name -ne $target.name){throw 'The approved project identity does not match'}
 $configPath=Join-Path $privateRoot "p13-slot-$label-config.json"
 & git check-ignore --quiet -- "data/staging/p13-slot-$label-config.json"
 if($LASTEXITCODE -ne 0){throw 'Private settings must be excluded from Git'}
 if(!$Apply){Write-Output "Plan: prepare only the existing empty Free project $($target.name); no change made";continue}
 if(Test-Path -LiteralPath $configPath){$slot=Get-Content -Raw -LiteralPath $configPath|ConvertFrom-Json}
 else{
  # Capture the provider connection only in memory. Never write it to a shell argument or log.
  $adminConnection=@(& .\data\tools\neon\node_modules\.bin\neon.cmd connection-string --project-id $target.project --database-name neondb --role-name neondb_owner --ssl require 2>$null)
  if($LASTEXITCODE -ne 0){throw 'Could not read the new project connection'}
  $adminUrl=($adminConnection -join "`n").Trim();$adminUri=$null;if(![Uri]::TryCreate($adminUrl,[UriKind]::Absolute,[ref]$adminUri)){throw 'Invalid provider connection'}
  if($adminUri.Scheme -notin @('postgres','postgresql') -or $adminUri.Host -notmatch '^ep-[a-z0-9-]+(?:\.[a-z0-9-]+)?\.eu-central-1\.aws\.neon\.tech$' -or $adminUri.Host -eq 'ep-weathered-art-ba8o4nc5.c-8.eu-central-1.aws.neon.tech' -or $adminUri.AbsolutePath -ne '/neondb'){throw 'Unexpected new-project database boundary'}
  $id=[Guid]::NewGuid();$suffix=$id.ToString('N')
  $slot=[pscustomobject]@{slotId=$id.ToString();projectId=$target.project;name="aria-company-$label-2ber-stg";url="https://aria-company-$label-2ber-stg.onrender.com";binding="business-$suffix";database="aria_slot_$suffix";dbUser="aria_slot_owner_$suffix";dbHost=$adminUri.Host;dbPassword=(Random-Url 32);bootstrapKey=(Random-Hex);admissionCode=(Random-Hex);jwt=(Random-Url 64);encryptionKey=[Convert]::ToBase64String((Random-Bytes 32));adminEmail="unclaimed-$suffix@workspace.invalid";adminPassword=(Random-Url 32);adminDatabaseUrl=$adminUrl;databasePrepared=$false;createdAt=[DateTime]::UtcNow.ToString('o')}
  Save-Private $slot $configPath
 }
 $suffix=$slot.binding -replace '^business-',''
 if($slot.projectId -ne $target.project -or $slot.binding -notmatch '^business-[a-f0-9]{32}$' -or $slot.database -ne "aria_slot_$suffix" -or $slot.dbUser -ne "aria_slot_owner_$suffix" -or $slot.url -ne "https://aria-company-$label-2ber-stg.onrender.com" -or $slot.dbPassword -notmatch '^[A-Za-z0-9_-]{40,}$' -or $slot.bootstrapKey -notmatch '^[a-f0-9]{64}$' -or $slot.admissionCode -notmatch '^[a-f0-9]{64}$'){throw 'Existing private settings have an unexpected boundary; nothing reset'}
 $adminUri=$null;if(![Uri]::TryCreate($slot.adminDatabaseUrl,[UriKind]::Absolute,[ref]$adminUri)){throw 'Invalid saved provider connection'};$credentials=$adminUri.UserInfo.Split(':',2)
 if($adminUri.Host -ne $slot.dbHost -or $adminUri.AbsolutePath -ne '/neondb' -or $credentials.Length -ne 2){throw 'Provider connection mismatch'}
 $admin=@{host=$slot.dbHost;database='neondb';user=[Uri]::UnescapeDataString($credentials[0]);password=[Uri]::UnescapeDataString($credentials[1])}
 if((Database-Call $admin "select count(*) from information_schema.tables where table_schema='public' and table_type='BASE TABLE';" 'empty provider database') -ne '0'){throw 'The provider default database is not empty; stop without changing it'}
 $role=$slot.dbUser;$database=$slot.database
 $hasRole=Database-Call $admin "select exists(select 1 from pg_roles where rolname='$role');" 'role lookup'
 if($hasRole -ne 't'){$null=Database-Call $admin "CREATE ROLE $role LOGIN PASSWORD '$($slot.dbPassword)' NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS;" 'new limited role'}
 $hasDatabase=Database-Call $admin "select exists(select 1 from pg_database where datname='$database');" 'database lookup'
 if($hasDatabase -ne 't'){
  # PG17 creates a role with ADMIN but not SET for its creator. Only the provider
  # administrator temporarily selects the NEW limited role to assign ownership.
  # The application role receives no membership or elevated attribute.
  $couldSetRole=(Database-Call $admin "select pg_has_role(current_user,'$role','SET');" 'provider ownership capability') -eq 't'
  if(!$couldSetRole){$null=Database-Call $admin "GRANT $role TO neondb_owner WITH SET TRUE;" 'temporary new-role ownership assignment'}
  try{$null=Database-Call $admin "CREATE DATABASE $database OWNER $role;" 'new dedicated database'}
  finally{if(!$couldSetRole){$null=Database-Call $admin "GRANT $role TO neondb_owner WITH SET FALSE;" 'restore provider membership setting'}}
 }
 $null=Database-Call $admin "REVOKE ALL ON DATABASE $database FROM PUBLIC;" 'database public access'
 $tenant=@{host=$slot.dbHost;database=$database;user=$role;password=$slot.dbPassword}
 $null=Database-Call $tenant 'REVOKE ALL ON SCHEMA public FROM PUBLIC;' 'schema public access'
 $proof=Database-Call $tenant "select json_build_object('unprivileged',not r.rolsuper and not r.rolcreatedb and not r.rolcreaterole and not r.rolbypassrls,'noRoleMembership',not exists(select 1 from pg_auth_members where member=r.oid),'ownsDatabase',exists(select 1 from pg_database where datname=current_database() and datdba=r.oid),'empty',not exists(select 1 from information_schema.tables where table_schema='public' and table_type='BASE TABLE')) from pg_roles r where rolname=current_user;" 'limited role proof'
 $verified=$proof|ConvertFrom-Json
 if(!$verified.unprivileged -or !$verified.noRoleMembership -or !$verified.ownsDatabase){throw 'The new role did not confirm isolation'}
 if(!$slot.databasePrepared -and !$verified.empty){throw 'The unprepared dedicated database is not empty; stop without modifying its data'}
 $slot.databasePrepared=$true;Save-Private $slot $configPath
 [pscustomobject]@{slot=$label;prepared=$true;unprivileged=$verified.unprivileged;noRoleMembership=$verified.noRoleMembership;ownsDatabase=$verified.ownsDatabase;empty=$verified.empty}|ConvertTo-Json -Compress
}
