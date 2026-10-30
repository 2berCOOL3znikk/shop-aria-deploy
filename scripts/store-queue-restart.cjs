'use strict';
// Local preview only. Two phases with a real application restart between them.
const fs=require('node:fs'),assert=require('node:assert/strict');
(async()=>{
 const cfg=JSON.parse(fs.readFileSync('data/preview/config.json','utf8')),base='http://127.0.0.1:'+cfg.port;
 const login=await fetch(base+'/api/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email:cfg.email,password:cfg.password})});assert.equal(login.status,200);const {token}=await login.json();
 const api=async(path,method='GET',data)=>{const r=await fetch(base+'/api/admin/control'+path,{method,headers:{Authorization:'Bearer '+token,'Content-Type':'application/json'},body:data?JSON.stringify(data):undefined});assert.ok(r.ok,path+' '+r.status);return r.json();};
 const path='target/e2e/store-queue-restart-state.json';fs.mkdirSync('target/e2e',{recursive:true});
 if(process.argv[2]==='prepare'){
  const available=(await api('/stores')).stores;const existing=available.findLast(s=>s.url==='https://queue.example.invalid'&&s.status!=='DISCONNECTED');
  const store=existing||await api('/stores','POST',{name:'Проверка сохранения запроса '+Date.now(),platform:'WOOCOMMERCE',url:'https://queue.example.invalid'});
  const job=await api('/stores/'+store.id+'/tasks','POST',{version:store.version,operation:'CHECK'});
  let saved;
  for(let n=0;n<40;n++){saved=(await api('/stores/'+store.id+'/tasks')).find(t=>t.id===job.id);if(saved.state==='RETRY')break;await new Promise(r=>setTimeout(r,250));}
  assert.equal(saved.state,'RETRY');fs.writeFileSync(path,JSON.stringify({storeId:store.id,taskId:job.id,attempts:saved.attempts,dueAt:saved.dueAt}));
  console.log('PASS prepare: a real retry is committed in the database');
 }else if(process.argv[2]==='verify'){
  const before=JSON.parse(fs.readFileSync(path,'utf8'));const job=(await api('/stores/'+before.storeId+'/tasks')).find(t=>t.id===before.taskId);
  assert.ok(job);assert.ok(['RETRY','RUNNING','FAILED'].includes(job.state));assert.ok(job.attempts>=before.attempts);assert.ok(job.attempts<=3);
  const store=(await api('/stores')).stores.find(s=>s.id===before.storeId);await api('/stores/'+store.id+'/disconnect','POST',{version:store.version});
  const cancelled=(await api('/stores/'+store.id+'/tasks')).find(t=>t.id===job.id);assert.ok(['CANCELLED','FAILED'].includes(cancelled.state));
  fs.writeFileSync('target/e2e/store-queue-restart-report.json',JSON.stringify({passed:true,taskId:job.id,stateBeforeRestart:'RETRY',stateAfterRestart:job.state,attemptsAfterRestart:job.attempts,afterDisconnect:cancelled.state},null,2));
  console.log('PASS verify: the same request survived restart and disconnect revoked pending work');
 }else throw new Error('Use prepare or verify');
})().catch(e=>{console.error(e.message);process.exit(1);});
