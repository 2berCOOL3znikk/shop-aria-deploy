'use strict';
// Operator-only, server-local provisioning. No setup material is available through an HTTP download.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const {execFileSync,spawn}=require('node:child_process');
const root=path.resolve(__dirname,'../..');process.chdir(root);
const area=path.join(root,'data/onboarding'),db='aria_onboarding_shop',role=db,port=8089,origin='http://127.0.0.1:8089';
const pg='D:/PostgreSQL/bin',cfgPath=path.join(area,'workspace-config.enc'),pidPath=path.join(area,'workspace.pid'),jar=path.join(area,'workspace.jar');
if(fs.realpathSync(area).toLowerCase()!==area.toLowerCase())throw new Error('Private setup area must be an actual local directory, not a redirected path');
const read=p=>JSON.parse(fs.readFileSync(p,'utf8').replace(/^\uFEFF/,''));
const registry=read(path.join(area,'registry-config.json')),cluster=read(path.join(root,'data/preview/config.json'));
const sha=v=>crypto.createHash('sha256').update(v).digest('hex'),literal=v=>{if(String(v).includes('\0'))throw new Error('Invalid setup text');return "'"+String(v).replace(/'/g,"''")+"'";};
function seal(value){const iv=crypto.randomBytes(12),cipher=crypto.createCipheriv('aes-256-gcm',Buffer.from(registry.encryptionKey,'base64'),iv),bytes=Buffer.concat([cipher.update(value,'utf8'),cipher.final(),cipher.getAuthTag()]);return iv.toString('base64')+':'+bytes.toString('base64');}
function open(value){const [iv,data]=value.split(':'),bytes=Buffer.from(data,'base64'),cipher=crypto.createDecipheriv('aes-256-gcm',Buffer.from(registry.encryptionKey,'base64'),Buffer.from(iv,'base64'));cipher.setAuthTag(bytes.subarray(-16));return Buffer.concat([cipher.update(bytes.subarray(0,-16)),cipher.final()]).toString('utf8');}
function save(cfg){const temporary=cfgPath+'.tmp';fs.writeFileSync(temporary,seal(JSON.stringify(cfg)),{mode:0o600});fs.renameSync(temporary,cfgPath);}
function sql(query,database,admin=false){
 const env={...process.env,PGHOST:'127.0.0.1',PGPORT:'55434',PGUSER:admin?'aria_preview':role,PGPASSWORD:admin?cluster.databasePassword:config.databasePassword,PGCONNECT_TIMEOUT:'5'};
 try{return execFileSync(path.join(pg,'psql.exe'),['-w','-XAt','-v','ON_ERROR_STOP=1','-d',database],{input:query,encoding:'utf8',env,stdio:['pipe','pipe','pipe'],windowsHide:true}).trim();}catch{throw new Error('Local database operation refused; no credentials or query details are printed');}
}
let config;
async function ready(){try{const response=await fetch(origin+'/actuator/health',{signal:AbortSignal.timeout(1500),redirect:'error'});return response.ok&&(await response.json()).status==='UP';}catch{return false;}}
async function pause(){await new Promise(resolve=>setTimeout(resolve,750));}
function ownedProcess(){
 if(!fs.existsSync(pidPath))return null;const pid=Number(fs.readFileSync(pidPath,'utf8'));if(!Number.isSafeInteger(pid)||pid<1)throw new Error('Invalid owned process marker');
 let value;try{value=execFileSync('powershell.exe',['-NoProfile','-NonInteractive','-Command',`Get-CimInstance Win32_Process -Filter 'ProcessId=${pid}' | Select-Object ProcessId,CommandLine | ConvertTo-Json -Compress`],{encoding:'utf8',windowsHide:true}).trim();}catch{throw new Error('Cannot confirm local process ownership');}
 if(!value)return null;const process=JSON.parse(value);if(!process.CommandLine.includes(jar)||!process.CommandLine.includes('--server.port=8089')||!process.CommandLine.includes('/'+db))throw new Error('PID belongs to another process; preserved it');return process.ProcessId;
}
async function stopOwned(){const pid=ownedProcess();if(!pid)return;process.kill(pid);for(let n=0;n<20;n++){try{process.kill(pid,0);}catch{return;}await pause();}throw new Error('Owned process did not stop');}
async function start(initial){
 if(await ready())throw new Error('Port 8089 is occupied; no process stopped');
 const source=new URL('file:///'+path.join(root,'src/main/resources/static/').replace(/\\/g,'/')).href;
 const args=['-Duser.timezone=Asia/Qyzylorda','-Xms32m','-Xmx192m','-XX:+UseSerialGC','-XX:ActiveProcessorCount=2','-jar',jar,'--server.address=127.0.0.1','--server.port=8089','--spring.profiles.active=postgres,free','--spring.datasource.url=jdbc:postgresql://127.0.0.1:55434/'+db,'--spring.datasource.username='+role,'--aria.organization.key='+(initial?'demo':config.binding),'--aria.company-signup.enabled=false','--aria.company-activation.enabled='+(!initial),'--spring.jpa.hibernate.ddl-auto=validate','--spring.flyway.enabled=true','--spring.h2.console.enabled=false','--aria.seed-demo-users=false','--aria.seed-demo-catalog=false','--aria.reset-data-on-start=false','--aria.demo-market-enabled=false','--aria.intelligence.external-enabled=false','--aria.manager.enabled=false','--aria.manager.activity-enabled=false','--aria.manager.schedule-enabled=false','--aria.smart-pricing.schedule-enabled=false','--aria.store-sync.worker-enabled=false','--spring.web.resources.static-locations='+source+',classpath:/static/'];
 const env={...process.env,SPRING_DATASOURCE_USERNAME:role,SPRING_DATASOURCE_PASSWORD:config.databasePassword,ARIA_DATABASE_URL:'postgresql://127.0.0.1:55434/'+db,ARIA_ADMIN_EMAIL:config.email,ARIA_ADMIN_PASSWORD:config.bootstrapPassword,ARIA_JWT_SECRET:config.jwt,ARIA_INTEGRATION_ENCRYPTION_KEY:config.integrationKey,ARIA_WORKSPACE_REQUEST_ID:config.requestId,ARIA_WORKSPACE_ACCESS_HASH:config.accessHash,ARIA_WORKSPACE_PROBE_KEY:open(config.probeCipher),ARIA_WORKSPACE_DEADLINE:config.deadline};
 // Inherit no external AI key or unrelated database URL. Secrets are environment-only, never CLI arguments.
 delete env.DATABASE_URL;delete env.ARIA_OPENROUTER_API_KEY;
 const out=fs.openSync(path.join(area,'workspace.out.log'),'w'),err=fs.openSync(path.join(area,'workspace.err.log'),'w');
 const child=spawn(path.join(process.env.USERPROFILE,'.jdks/ms-21.0.12.1/bin/java.exe'),args,{cwd:root,env,detached:true,windowsHide:true,stdio:['ignore',out,err]});fs.closeSync(out);fs.closeSync(err);child.unref();fs.writeFileSync(pidPath,String(child.pid));
 for(let n=0;n<80;n++){if(await ready())return;try{process.kill(child.pid,0);}catch{throw new Error('Workspace did not start; inspect the private local logs');}await pause();}throw new Error('Workspace startup timed out');
}
(async()=>{
 const args=process.argv.slice(2),id=args.find(value=>/^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/.test(value));if(!id||args.some(a=>a!==id&&a!=='--restart'))throw new Error('Usage: node scripts/onboarding/workspace-local.cjs <application-uuid> [--restart]');
 if(cluster.databasePort!==55434||path.dirname(cfgPath)!==area)throw new Error('Only the owned local cluster and private configuration area are supported');
 // Registry credentials may read only the separate operator database, never create databases.
 const requestQuery='SELECT row_to_json(r) FROM (SELECT id,company_name,owner_name,owner_email,access_hash,status,workspace_binding,setup_secret_cipher,activation_deadline FROM company_registration_requests WHERE id='+literal(id)+') r;';
 let row;try{row=JSON.parse(execFileSync(path.join(pg,'psql.exe'),['-w','-XAt','-v','ON_ERROR_STOP=1','-d','aria_registration_local'],{input:requestQuery,encoding:'utf8',windowsHide:true,stdio:['pipe','pipe','pipe'],env:{...process.env,PGHOST:'127.0.0.1',PGPORT:'55434',PGUSER:'aria_registration_local',PGPASSWORD:registry.databasePassword,PGCONNECT_TIMEOUT:'5'}}).trim());}catch{throw new Error('Prepared application not found in the local operator database');}
 if(!['PREPARING','READY','ACTIVE'].includes(row.status)||!row.setup_secret_cipher||!row.workspace_binding?.match(/^business-[a-f0-9]{32}$/)||!row.access_hash?.match(/^[a-f0-9]{64}$/))throw new Error('Operator preparation is required');
 const fingerprint=sha(row.setup_secret_cipher),secret=open(row.setup_secret_cipher);if(!/^[a-f0-9]{64}$/.test(secret))throw new Error('Invalid encrypted local proof');
 if(fs.existsSync(cfgPath)){
  config=JSON.parse(open(fs.readFileSync(cfgPath,'utf8')));if(config.requestId!==id||config.binding!==row.workspace_binding||config.fingerprint!==fingerprint||config.accessHash!==row.access_hash||config.email!==row.owner_email||config.deadline!==new Date(row.activation_deadline).toISOString())throw new Error('Another application owns this local workspace; preserved it');
 }else{
  if(row.status!=='PREPARING'||Date.parse(row.activation_deadline)<=Date.now())throw new Error('A current prepared application is required');
  config={format:'aria-local-workspace-v1',requestId:id,binding:row.workspace_binding,email:row.owner_email,accessHash:row.access_hash,deadline:new Date(row.activation_deadline).toISOString(),probeCipher:row.setup_secret_cipher,fingerprint,databasePassword:crypto.randomBytes(24).toString('hex'),bootstrapPassword:crypto.randomBytes(32).toString('base64url'),jwt:crypto.randomBytes(48).toString('hex'),integrationKey:crypto.randomBytes(32).toString('base64'),createdDatabase:false};
  // Refuse a pre-existing role or database without our private encrypted ownership record.
  if(sql("SELECT EXISTS(SELECT 1 FROM pg_database WHERE datname='aria_onboarding_shop') OR EXISTS(SELECT 1 FROM pg_roles WHERE rolname='aria_onboarding_shop');",'postgres',true)!=='f')throw new Error('Local workspace database/role already exists; preserved it');
  save(config);
 }
 const running=ownedProcess();if(running&&!args.includes('--restart')){if(!await ready())throw new Error('Owned workspace is still starting');console.log('Local workspace already running: '+origin+'/company-activate.html');return;}if(running)await stopOwned();
 if(!config.createdDatabase){
  if(sql("SELECT EXISTS(SELECT 1 FROM pg_database WHERE datname='aria_onboarding_shop') OR EXISTS(SELECT 1 FROM pg_roles WHERE rolname='aria_onboarding_shop');",'postgres',true)!=='f')throw new Error('Partial creation needs operator review; no existing database or role was changed');
  sql('CREATE ROLE aria_onboarding_shop LOGIN PASSWORD '+literal(config.databasePassword)+' NOSUPERUSER NOCREATEDB NOCREATEROLE;','postgres',true);
  sql('CREATE DATABASE aria_onboarding_shop OWNER aria_onboarding_shop;','postgres',true);sql('REVOKE CONNECT ON DATABASE aria_onboarding_shop FROM PUBLIC;','postgres',true);config.createdDatabase=true;save(config);
 }
 fs.copyFileSync(path.join(root,'target/aria-backend-0.0.1-SNAPSHOT.jar'),jar);
 const schema=sql("SELECT to_regclass('public.workspace_activation') IS NOT NULL;",db);
 if(schema!=='t'){await start(true);await stopOwned();}
 const marker=sql('SELECT count(*) FROM workspace_activation;',db);
 if(marker==='0'){
  if(row.status!=='PREPARING')throw new Error('Missing activation marker; cannot reuse an existing workspace');
  const bind=sql('SELECT organization_key FROM saas_organization WHERE id=1;',db);if(bind!=='demo')throw new Error('Another company owns the database; preserved it');
  // A single transaction binds ONLY our new empty database, updates the prepared owner, and installs the marker.
  sql(`BEGIN;
   DO $$ BEGIN IF (SELECT count(*) FROM app_users)<>1 OR EXISTS(SELECT 1 FROM products) OR EXISTS(SELECT 1 FROM orders) OR EXISTS(SELECT 1 FROM connected_stores) OR EXISTS(SELECT 1 FROM company_registration_requests) THEN RAISE EXCEPTION 'Workspace must be new and empty'; END IF; END $$;
   UPDATE saas_organization SET organization_key=${literal(config.binding)},name=${literal(row.company_name)},demonstration=false,impact_enabled=false WHERE id=1 AND organization_key='demo';
   UPDATE app_users SET full_name=${literal(row.owner_name)} WHERE lower(email)=${literal(config.email)} AND role='ADMIN';
   INSERT INTO workspace_activation(id,request_id,binding,owner_user_id,owner_email_hash,access_hash,probe_hash,deadline) SELECT 1,${literal(id)},${literal(config.binding)},id,${literal(sha(config.email))},${literal(config.accessHash)},${literal(sha(secret))},${literal(config.deadline)}::timestamptz FROM app_users WHERE lower(email)=${literal(config.email)} AND role='ADMIN';
   DO $$ BEGIN IF NOT EXISTS(SELECT 1 FROM workspace_activation) THEN RAISE EXCEPTION 'Owner was not prepared'; END IF; END $$;COMMIT;`,db);
 }
 const actual=JSON.parse(sql('SELECT row_to_json(a) FROM workspace_activation a WHERE id=1;',db));
 if(actual.request_id!==id||actual.binding!==config.binding||actual.access_hash!==config.accessHash||actual.probe_hash!==sha(secret)||actual.owner_email_hash!==sha(config.email)||new Date(actual.deadline).toISOString()!==config.deadline)throw new Error('Activation marker differs; preserved the database');
 await start(false);console.log('Separate local workspace started: '+origin+'/company-activate.html. Confirm readiness in the operator page; the owner chooses their own password.');
})().catch(error=>{console.error(error.message);process.exitCode=1;});
