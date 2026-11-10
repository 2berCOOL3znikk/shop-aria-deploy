'use strict';
// Real backup/restore drill for the TWO owned onboarding databases. Never restores over a source.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),net=require('node:net');
const {execFileSync,spawn}=require('node:child_process');
process.chdir(path.resolve(__dirname,'../..'));const root=process.cwd(),area=path.join(root,'data/onboarding'),pg='D:/PostgreSQL/bin';
if(fs.realpathSync(area).toLowerCase()!==area.toLowerCase())throw new Error('Private backup area must not be redirected');
const json=p=>JSON.parse(fs.readFileSync(p,'utf8').replace(/^\uFEFF/,'')),registry=json(path.join(area,'registry-config.json')),cluster=json('data/preview/config.json');
if(cluster.databasePort!==55434)throw new Error('Only the owned local cluster is supported');
function open(value){const [iv,raw]=value.split(':'),bytes=Buffer.from(raw,'base64'),cipher=crypto.createDecipheriv('aes-256-gcm',Buffer.from(registry.encryptionKey,'base64'),Buffer.from(iv,'base64'));cipher.setAuthTag(bytes.subarray(-16));return Buffer.concat([cipher.update(bytes.subarray(0,-16)),cipher.final()]).toString('utf8');}
const workspace=JSON.parse(open(fs.readFileSync(path.join(area,'workspace-config.enc'),'utf8'))),ownerPassword=open(fs.readFileSync(path.join(area,'e2e-owner-password.enc'),'utf8'));
if(!/^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/.test(workspace.requestId)||!/^business-[a-f0-9]{32}$/.test(workspace.binding))throw new Error('Invalid private workspace binding');
const stamp=crypto.randomBytes(6).toString('hex'),directory=path.join(area,'recovery',stamp);fs.mkdirSync(directory,{recursive:true});
const digest=v=>crypto.createHash('sha256').update(v).digest('hex');
const admin={user:'aria_preview',password:cluster.databasePassword},desk={user:'aria_registration_local',password:registry.databasePassword},shop={user:'aria_onboarding_shop',password:workspace.databasePassword};
const env=(auth,database)=>({...process.env,PGHOST:'127.0.0.1',PGPORT:'55434',PGDATABASE:database,PGUSER:auth.user,PGPASSWORD:auth.password,PGCONNECT_TIMEOUT:'5'});
function sql(query,auth,database){try{return execFileSync(path.join(pg,'psql.exe'),['-w','-XAt','-v','ON_ERROR_STOP=1','-d',database],{input:query,encoding:'utf8',env:env(auth,database),windowsHide:true,stdio:['pipe','pipe','pipe']}).trim();}catch{throw new Error('Recovery database operation failed; private query details are not printed');}}
function shell(script,args,auth,database,success=true){
 // A PS7 parent can pass a PS7-only module path into Windows PowerShell 5. Add its own built-ins
 // in this CHILD environment only; preserve the user's actual configuration and module paths.
 const environment={...env(auth,database),PSModulePath:path.join(process.env.SystemRoot,'System32/WindowsPowerShell/v1.0/Modules')+';'+(process.env.PSModulePath||'')};
 try{execFileSync('powershell.exe',['-NoProfile','-NonInteractive','-ExecutionPolicy','Bypass','-File',script,...args],{encoding:'utf8',env:environment,windowsHide:true,stdio:['ignore','pipe','pipe']});if(!success)throw new Error('Unsafe restore was unexpectedly accepted');return true;}catch(error){if(!success&&error.status!==undefined)return false;fs.writeFileSync(path.join(directory,'command-error.log'),String(error.stderr||error.message));throw new Error(success?'Backup/restore command failed; inspect the private drill area':'Restore safety check did not reject the request');}
}
const tables=['saas_organization','app_users','company_registration_requests','workspace_activation','audit_events','products','orders','connected_stores','external_integrations'];
function snapshot(auth,database){
 const hashes={};for(const table of tables){const raw=sql(`SELECT COALESCE(json_agg(v ORDER BY v.id)::text,'[]') FROM (SELECT * FROM ${table}) v;`,auth,database);hashes[table]=digest(raw);}
 const version=Number(sql('SELECT max(version::integer) FROM flyway_schema_history WHERE success;',auth,database));if(version!==34)throw new Error('This drill requires the reviewed V34 schema');
 return {version,hashes};
}
function identical(a,b){if(JSON.stringify(a)!==JSON.stringify(b))throw new Error('Restored records differ; source remains untouched');}
async function free(port){await new Promise((resolve,reject)=>{const server=net.createServer();server.once('error',()=>reject(new Error('Recovery port occupied; no process stopped')));server.listen(port,'127.0.0.1',()=>server.close(resolve));});}
async function call(port,url,method='GET',body,headers={}){const r=await fetch('http://127.0.0.1:'+port+url,{method,headers:{...headers,...(body?{'Content-Type':'application/json'}:{})},...(body?{body:JSON.stringify(body)}:{}),redirect:'error',signal:AbortSignal.timeout(5000)});if(!r.ok)throw new Error('Restored API check failed with status '+r.status);return r.json();}
const delay=ms=>new Promise(resolve=>setTimeout(resolve,ms));
async function running(kind,auth,database,check){
 const isDesk=kind==='desk',port=isDesk?8091:8090;await free(port);
 const jar=path.join(directory,'restore.jar');if(!fs.existsSync(jar))fs.copyFileSync('target/aria-backend-0.0.1-SNAPSHOT.jar',jar);
 const binding=isDesk?'aria-registration':workspace.binding,email=isDesk?registry.email:workspace.email;
 const args=['-Duser.timezone=Asia/Qyzylorda','-Xms32m','-Xmx192m','-XX:+UseSerialGC','-XX:ActiveProcessorCount=2','-jar',jar,'--server.address=127.0.0.1','--server.port='+port,'--spring.profiles.active=postgres,free','--spring.datasource.url=jdbc:postgresql://127.0.0.1:55434/'+database,'--spring.datasource.username='+auth.user,'--aria.organization.key='+binding,'--aria.company-signup.enabled='+isDesk,'--aria.company-activation.enabled='+(!isDesk),'--spring.jpa.hibernate.ddl-auto=validate','--spring.flyway.enabled=true','--spring.h2.console.enabled=false','--aria.seed-demo-users=false','--aria.seed-demo-catalog=false','--aria.reset-data-on-start=false','--aria.demo-market-enabled=false','--aria.intelligence.external-enabled=false','--aria.manager.enabled=false','--aria.manager.activity-enabled=false','--aria.manager.schedule-enabled=false','--aria.smart-pricing.schedule-enabled=false','--aria.connected-pricing.schedule-enabled=false','--aria.store-sync.worker-enabled=false'];
 const environment={...process.env,...env(auth,database),SPRING_DATASOURCE_USERNAME:auth.user,SPRING_DATASOURCE_PASSWORD:auth.password,ARIA_DATABASE_URL:'postgresql://127.0.0.1:55434/'+database,ARIA_JWT_SECRET:crypto.randomBytes(48).toString('hex'),ARIA_ADMIN_EMAIL:email,ARIA_ADMIN_PASSWORD:isDesk?registry.password:workspace.bootstrapPassword,ARIA_INTEGRATION_ENCRYPTION_KEY:isDesk?registry.encryptionKey:workspace.integrationKey,ARIA_WORKSPACE_REQUEST_ID:workspace.requestId,ARIA_WORKSPACE_ACCESS_HASH:workspace.accessHash,ARIA_WORKSPACE_PROBE_KEY:open(workspace.probeCipher),ARIA_WORKSPACE_DEADLINE:workspace.deadline};delete environment.DATABASE_URL;delete environment.ARIA_OPENROUTER_API_KEY;
 const out=fs.openSync(path.join(directory,kind+'.out.log'),'w'),err=fs.openSync(path.join(directory,kind+'.err.log'),'w');
 const child=spawn(path.join(process.env.USERPROFILE,'.jdks/ms-21.0.12.1/bin/java.exe'),args,{cwd:root,env:environment,windowsHide:true,stdio:['ignore',out,err]});fs.closeSync(out);fs.closeSync(err);
 const exited=()=>child.exitCode!==null||child.signalCode!==null;
 try{let ready=false;for(let n=0;n<80;n++){if(exited())throw new Error('Restored process stopped; see private logs');try{ready=(await call(port,'/actuator/health')).status==='UP';}catch{}if(ready)break;await delay(750);}if(!ready)throw new Error('Restored process start timed out');await check(port,email,environment.ARIA_WORKSPACE_PROBE_KEY);}finally{if(!exited())child.kill();for(let n=0;n<40&&!exited();n++)await delay(250);if(!exited())throw new Error('Owned recovery process did not stop');}
}
async function browserCheck(kind,port,email,password){
 const {chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||path.join(process.env.USERPROFILE,'.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright')),browser=await chromium.launch({channel:'msedge',headless:true});
 try{const context=await browser.newContext({viewport:{width:1440,height:1000}}),page=await context.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message));const origin='http://127.0.0.1:'+port;
  const destination=kind==='desk'?'/company-requests.html':'/control.html#stores';await page.goto(origin+'/login.html?next='+encodeURIComponent(destination));await page.locator('#login-email').fill(email);await page.locator('#login-password').fill(password);await page.locator('#login-form [type=submit]').click();await page.waitForURL(origin+destination);
  await page.waitForFunction(kind=>document.getElementById(kind==='desk'?'signup-operator':'stores-root')?.getAttribute('aria-busy')==='false',kind);
  if(kind==='desk'){await page.locator('[data-open="'+workspace.requestId+'"]').click();await page.locator('#signup-verify-workspace').waitFor();}
  for(const width of [1440,390]){await page.setViewportSize({width,height:1000});if(!await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1))throw new Error('Restored UI overflow');const file='restored-'+kind+'-ru-'+width+'.png';await page.screenshot({path:'docs/onboarding/evidence/'+file});proof.screenshots.push(file);}
  if(errors.length)throw new Error('Restored UI JavaScript error');
 }finally{await browser.close();}
}
const proof={productionChanged:false,sourceDatabasesModified:false,restoredApplicationsStopped:false,externalAiRequests:0,checks:[],copies:[],screenshots:[]};
(async()=>{
 if(sql('SELECT organization_key FROM saas_organization WHERE id=1;',desk,'aria_registration_local')!=='aria-registration'||sql('SELECT organization_key FROM saas_organization WHERE id=1;',shop,'aria_onboarding_shop')!==workspace.binding)throw new Error('Source bindings do not match the owned educational environments');
 const originalDesk=snapshot(desk,'aria_registration_local'),originalShop=snapshot(shop,'aria_onboarding_shop');
 for(const [kind,auth,source,original] of [['desk',desk,'aria_registration_local',originalDesk],['shop',shop,'aria_onboarding_shop',originalShop]]){
  const target='aria_onboard_restore_'+kind+'_'+stamp,newRole=target,password=crypto.randomBytes(24).toString('hex'),restored={user:newRole,password},dump=path.join(directory,kind+'.dump');
  shell('scripts/backup-postgres.ps1',['-OutputFile',dump,'-PgDump',path.join(pg,'pg_dump.exe')],auth,source);
  // This role/database name is generated here and never supplied by the caller. No DROP, --clean, or reuse.
  if(sql(`SELECT EXISTS(SELECT 1 FROM pg_roles WHERE rolname='${newRole}') OR EXISTS(SELECT 1 FROM pg_database WHERE datname='${target}');`,admin,'postgres')!=='f')throw new Error('Recovery target exists; preserved it');
  sql(`CREATE ROLE ${newRole} LOGIN PASSWORD '${password}' NOSUPERUSER NOCREATEDB NOCREATEROLE;`,admin,'postgres');sql(`CREATE DATABASE ${target} OWNER ${newRole} TEMPLATE template0 ENCODING 'UTF8';`,admin,'postgres');sql(`REVOKE CONNECT ON DATABASE ${target} FROM PUBLIC;`,admin,'postgres');
  const args=['-BackupFile',dump,'-SourceDatabase',source,'-TargetDatabase',target,'-PgRestore',path.join(pg,'pg_restore.exe'),'-Psql',path.join(pg,'psql.exe')];
  const bad=path.join(directory,kind+'-bad-checksum.dump');fs.copyFileSync(dump,bad);fs.writeFileSync(bad+'.sha256','0'.repeat(64));const badArgs=[...args];badArgs[1]=bad;shell('scripts/restore-postgres.ps1',badArgs,restored,target,false);proof.checks.push(kind+': checksum mismatch refused before restore');
  shell('scripts/restore-postgres.ps1',args,restored,target);identical(snapshot(restored,target),original);shell('scripts/restore-postgres.ps1',args,restored,target,false);identical(snapshot(restored,target),original);proof.checks.push(kind+': actual full dump restored; repeated restore into populated target refused without changes');
  if(sql(`SELECT rolsuper OR rolcreatedb OR rolcreaterole FROM pg_roles WHERE rolname=current_user;`,restored,target)!=='f'||sql(`SELECT has_database_privilege(current_user,'${source}','CONNECT');`,restored,target)!=='f')throw new Error('Recovery role isolation failed');
  await running(kind,restored,target,async(port,email,key)=>{
   const login=await call(port,'/api/auth/login','POST',{email,password:kind==='desk'?registry.password:ownerPassword}),headers={Authorization:'Bearer '+login.token};
   if(kind==='desk'){
    const list=await call(port,'/api/admin/company-signup/requests','GET',null,headers);if(list.length!==3||list.find(row=>row.id===workspace.requestId)?.status!=='ACTIVE')throw new Error('Restored operator workflow differs');
    const [id,token]=fs.readFileSync(path.join(area,'e2e-receipt-ru.txt'),'utf8').trim().split('.'),status=await call(port,'/api/company-signup/requests/'+id,'GET',null,{'X-ARIA-Signup-Token':token});if(status.status!=='ACTIVE')throw new Error('Restored receipt does not open its request');
    const cipher=sql(`SELECT setup_secret_cipher FROM company_registration_requests WHERE id='${workspace.requestId}';`,restored,target);if(open(cipher)!==key)throw new Error('Restored private setup cannot be decrypted with the separately retained key');
   }else{
    const stores=await call(port,'/api/admin/control/stores','GET',null,headers);if(stores.stores.length!==0)throw new Error('Restored empty catalogue differs');
    const nonce=crypto.randomBytes(16).toString('hex'),message=['aria-workspace-request-v1',workspace.requestId,nonce].join('\n'),signature=crypto.createHmac('sha256',key).update(message).digest('hex'),status=await call(port,'/api/company-activation/attest','POST',{requestId:workspace.requestId,nonce},{'X-ARIA-Provision-Proof':signature});if(!status.claimedAt||status.nonce!==nonce||status.binding!==workspace.binding)throw new Error('Restored activation proof differs');
    const canonical=['aria-workspace-v1',workspace.requestId,workspace.binding,digest(email),nonce,new Date(status.expiresAt).toISOString().replace('.000Z','Z'),new Date(status.claimedAt).toISOString().replace('.000Z','Z')].join('\n');if(crypto.createHmac('sha256',key).update(canonical).digest('hex')!==status.signature)throw new Error('Restored signature invalid');
   }
   await browserCheck(kind,port,email,kind==='desk'?registry.password:ownerPassword);
  });proof.checks.push(kind+': restored application boots under restricted owner; actual login and business API verified');
  identical(snapshot(restored,target),original);proof.copies.push({kind,database:target,migrationVersion:original.version,recordDigestsMatch:true,roleRestricted:true,sourceConnectionDenied:true,backupSha256:digest(fs.readFileSync(dump))});
 }
 identical(snapshot(desk,'aria_registration_local'),originalDesk);identical(snapshot(shop,'aria_onboarding_shop'),originalShop);proof.checks.push('source record digests unchanged before and after drill');proof.restoredApplicationsStopped=true;proof.passed=true;
})().catch(error=>{proof.failure=error.message;console.error(error.message);process.exitCode=1;}).finally(()=>{proof.completedAt=new Date().toISOString();fs.writeFileSync('docs/onboarding/evidence/recovery-v34.json',JSON.stringify(proof,null,2)+'\n');if(proof.passed)console.log('PASS: two actual backups restored into fresh restricted databases; owner activation and login survive; sources untouched');});
