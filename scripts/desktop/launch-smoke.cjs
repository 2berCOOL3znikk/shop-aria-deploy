'use strict';
// Cold application/DB start and idempotent repeat on the exact owned educational environments only.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),{execFileSync}=require('node:child_process');
process.chdir(path.resolve(__dirname,'../..'));const root=process.cwd(),pg='D:/PostgreSQL/bin',cfg=JSON.parse(fs.readFileSync('data/preview/config.json','utf8').replace(/^\uFEFF/,''));
if(cfg.databasePort!==55434)throw new Error('Only the owned local cluster is supported');
const env={...process.env,PGHOST:'127.0.0.1',PGPORT:'55434',PGUSER:'aria_preview',PGPASSWORD:cfg.databasePassword,PGCONNECT_TIMEOUT:'5',PGOPTIONS:'-c timezone=UTC -c datestyle=ISO,YMD'};
function sql(database,query){try{return execFileSync(path.join(pg,'psql.exe'),['-w','-XAt','-d',database],{input:query,encoding:'utf8',env,windowsHide:true,stdio:['pipe','pipe','pipe']}).trim();}catch{throw new Error('Cannot inspect local launch state');}}
const databases=['aria_registration_local','aria_onboarding_shop','aria_locale_demo'],tables=['saas_organization','app_users','company_registration_requests','workspace_activation','products','orders','connected_stores','audit_events'];
function snapshot(){const value={};for(const db of databases)for(const table of tables)value[db+'.'+table]=JSON.parse(sql(db,`SELECT COALESCE(json_agg(v ORDER BY v.id)::text,'[]') FROM (SELECT * FROM ${table}) v;`));return value;}
function unchanged(before,after){
 const changes=[];let appended=0;
 for(const [table,rows] of Object.entries(before)){
  const present=new Map(after[table].map(row=>[String(row.id),row]));
  if(!table.endsWith('.audit_events')&&after[table].length!==rows.length)changes.push(table+': row count');
  for(const row of rows){const found=present.get(String(row.id));if(!found){changes.push(table+': missing row');continue;}for(const key of Object.keys(row))if(JSON.stringify(row[key])!==JSON.stringify(found[key]))changes.push(table+': '+key);}
  if(table.endsWith('.audit_events'))appended+=after[table].length-rows.length;
 }
 if(changes.length)throw new Error('Source records changed: '+[...new Set(changes)].join(', '));return appended;
}
const instances=[{area:'data/onboarding',pid:'registry.pid',jar:'registry.jar',port:8088,db:databases[0]},{area:'data/onboarding',pid:'workspace.pid',jar:'workspace.jar',port:8089,db:databases[1]},{area:'data/localization-preview',pid:'app.pid',jar:'app.jar',port:8087,db:databases[2]}];
function pids(){return instances.map(v=>({port:v.port,pid:Number(fs.readFileSync(path.join(v.area,v.pid),'utf8'))}));}
function ps(source){try{return execFileSync('powershell.exe',['-NoProfile','-NonInteractive','-Command',source],{encoding:'utf8',windowsHide:true,stdio:['ignore','pipe','pipe']});}catch{throw new Error('Owned process check/stop failed; no unrelated process was stopped');}}
function launch(){try{return execFileSync('cmd.exe',['/d','/c','ARIA.cmd -NoBrowser'],{encoding:'utf8',windowsHide:true,timeout:240000,stdio:['ignore','pipe','pipe']});}catch{throw new Error('The common Windows launcher failed; inspect private data/desktop logs');}}
const proof={productionChanged:false,sourceBusinessRecordsChanged:false,checks:[]},lockFile=path.join(root,'data/desktop/launch-smoke.lock');let lock;
(async()=>{
 if(fs.existsSync(lockFile)){const owner=JSON.parse(fs.readFileSync(lockFile,'utf8'));let alive=true;try{process.kill(owner.pid,0);}catch{alive=false;}if(alive)throw new Error('Another launch check is active');fs.unlinkSync(lockFile);}
 lock=fs.openSync(lockFile,'wx');fs.writeFileSync(lock,JSON.stringify({pid:process.pid}));
 const before=snapshot();const old=pids();
 for(const item of instances){const pid=Number(fs.readFileSync(path.join(item.area,item.pid),'utf8')),jar=path.join(root,item.area,item.jar);if(!Number.isSafeInteger(pid)||pid<1)throw new Error('Invalid owned PID');const literal=v=>"'"+v.replace(/'/g,"''")+"'";
  ps(`$p=Get-CimInstance Win32_Process -Filter 'ProcessId=${pid}'; if(!$p -or !$p.CommandLine.Contains(${literal(jar)}) -or !$p.CommandLine.Contains('--server.port=${item.port}') -or !$p.CommandLine.Contains('/${item.db}')){throw 'Not owned'};Stop-Process -Id ${pid};Wait-Process -Id ${pid} -ErrorAction SilentlyContinue`);
 }
 const clients=Number(sql('postgres',"SELECT count(*) FROM pg_stat_activity WHERE backend_type='client backend' AND pid<>pg_backend_pid();"));
 if(clients!==0)throw new Error('Other database clients are active; cluster preserved');
 const cluster=path.join(root,'data/preview/postgres');if(fs.realpathSync(cluster).toLowerCase()!==cluster.toLowerCase()||!fs.existsSync(path.join(cluster,'PG_VERSION')))throw new Error('Unexpected owned cluster path');
 execFileSync(path.join(pg,'pg_ctl.exe'),['-D',cluster,'-w','stop'],{windowsHide:true,stdio:['ignore','pipe','pipe']});proof.databaseInitiallyStopped=true;
 const started=Date.now();launch();proof.coldStartSeconds=Number(((Date.now()-started)/1000).toFixed(2));const ready=JSON.parse(fs.readFileSync('data/desktop/ready.json','utf8').replace(/^\uFEFF/,''));if(!ready.workspaceReady||!ready.registrationReady||!ready.storefrontReady||ready.entry!=='http://127.0.0.1:8089/start.html')throw new Error('The common start did not confirm all required applications');proof.auditRowsAppended=unchanged(before,snapshot());proof.checks.push('one ARIA.cmd starts the stopped owned PostgreSQL cluster and three applications; business records and all earlier audit rows retained');
 const current=pids(),repeatStarted=Date.now();launch();proof.repeatStartSeconds=Number(((Date.now()-repeatStarted)/1000).toFixed(2));if(JSON.stringify(pids())!==JSON.stringify(current))throw new Error('Unchanged applications were unnecessarily restarted');proof.auditRowsAppended=unchanged(before,snapshot());proof.checks.push('repeat launch reuses the same healthy process IDs and retained records');
 const app=await (await fetch(ready.entry.replace('/start.html','/api/application'),{redirect:'error'})).json();if(app.workspaceType!=='BUSINESS'||!app.desktop||app.build.revision!==ready.revision)throw new Error('Browser server identity does not match the launcher build');proof.checks.push('actual server build and business identity match the launcher');proof.entry=ready.entry;proof.revision=ready.revision;proof.before=old;proof.after=current;proof.passed=true;
})().catch(error=>{proof.failure=error.message;console.error(error.message);process.exitCode=1;}).finally(()=>{if(lock!==undefined){fs.closeSync(lock);fs.unlinkSync(lockFile);}proof.completedAt=new Date().toISOString();fs.writeFileSync('docs/aria4/evidence/launch-smoke.json',JSON.stringify(proof,null,2)+'\n');if(proof.passed)console.log('PASS: cold and repeated one-action start; correct company/build; business records and earlier audit rows retained');});
