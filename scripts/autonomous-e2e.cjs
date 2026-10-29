'use strict';
// Only the password-protected, isolated local preview. Never a production target.
const fs=require('node:fs'),assert=require('node:assert/strict');
const {chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||'playwright');
const cfg=JSON.parse(fs.readFileSync('data/preview/config.json','utf8').replace(/^\uFEFF/,''));
const base='http://127.0.0.1:'+cfg.port,phase=process.argv[2]||'verify',fixtureFile='target/e2e/autonomous-persistence.json';
const date=n=>new Date(Date.now()+n*86400000).toLocaleDateString('en-CA',{timeZone:'Asia/Qyzylorda'});
fs.mkdirSync('target/e2e',{recursive:true});
(async()=>{
 const browser=await chromium.launch({channel:'msedge',headless:true}),report={phase,at:new Date().toISOString(),checks:[],errors:[]};
 try{
  const ctx=await browser.newContext({viewport:{width:1440,height:1000}}),p=await ctx.newPage();p.on('pageerror',e=>report.errors.push(e.message));p.on('dialog',d=>d.accept());
  await p.goto(base+'/login.html');await p.locator('#login-email').fill(cfg.email);await p.locator('#login-password').fill(cfg.password);await p.locator('#login-form [type=submit]').click();await p.waitForURL('**/control.html');await p.waitForSelector('#cc-overview');
  const token=await p.evaluate(()=>localStorage.getItem('aria_token'));
  const api=async(method,path,data,auth=token)=>{const r=await ctx.request.fetch(base+path,{method,data,headers:auth?{Authorization:'Bearer '+auth}:{}});assert.ok(r.ok(),path+' '+r.status()+' '+await r.text());return r.json();};
  const control='/api/admin/control';
  if(phase==='prepare'){
   await api('POST',control+'/manager/stop');const stamp=Date.now();
   const product=await api('POST',control+'/products',{sku:'RESTART-'+stamp,name:'Синтетическая партия для проверки восстановления '+stamp,category:'Продукты',unit:'шт',unitPrice:1400,minimumPrice:900,lowStockThreshold:2});
   const lot=await api('POST',control+'/lots',{productId:product.id,lotNumber:'RESTART-'+stamp,quantity:3,unitCost:700,receivedAt:date(0),expiryDate:date(5)});
   const state=await api('GET',control+'/manager'),task=state.queue.tasks.find(t=>t.productId===product.id);assert.equal(state.settings.mode,'OFF');assert.equal(task.status,'QUEUED');
   fs.writeFileSync(fixtureFile,JSON.stringify({productId:product.id,lotId:lot.id,taskId:task.id,generation:task.generation,preparedAt:new Date().toISOString()},null,2));report.checks.push('Real stock mutation persisted a single pending task while policy OFF prevented processing');
  }else{
   const fixture=JSON.parse(fs.readFileSync(fixtureFile,'utf8')),before=await api('GET',control+'/manager'),saved=before.queue.tasks.find(t=>t.id===fixture.taskId);
   assert.equal(before.settings.mode,'OFF');assert.ok(saved,'Pending task survived application/PostgreSQL restart');assert.equal(saved.productId,fixture.productId);assert.equal(saved.generation,fixture.generation);assert.equal(saved.status,'QUEUED');
   report.checks.push('Pending queue row and event generation survived a real application/PostgreSQL restart');
   const save=async()=>{await p.locator('#control-dialog [type=submit]').click();await p.waitForFunction(()=>!document.getElementById('control-dialog').open);await p.waitForFunction(()=>document.getElementById('control-content').getAttribute('aria-busy')==='false');};
   await p.locator('[data-manager=settings]').first().click();await p.locator('[name=mode]').selectOption('ADVISOR');await p.locator('[name=automaticAiConsent]').uncheck();await p.locator('[name=purchaseDrafts]').uncheck();await p.locator('[name=socialDrafts]').uncheck();await save();
   await api('GET','/api/products/store',null,null);
   let state,execution;const deadline=Date.now()+100000;
   while(Date.now()<deadline){state=await api('GET',control+'/manager');execution=state.queue.feed.find(e=>e.productId===fixture.productId&&e.status==='DONE');if(execution)break;await new Promise(r=>setTimeout(r,1500));}
   assert.ok(execution,'Normal activity must finish the persisted task');assert.equal(execution.source,'ENGINE DECISION');assert.equal(execution.priceApplied,false);assert.equal((await api('GET',control+'/products/'+fixture.productId)).product.discountPercent,0);
   report.checks.push('Activity-driven catch-up completed the persisted task without AI consent, price changes or stock consumption');
   const tab=async id=>{await p.locator('#control-nav [data-id="'+id+'"]').click();await p.waitForFunction(()=>document.getElementById('control-content').getAttribute('aria-busy')==='false');};
   await tab('lots');await p.locator('[data-action=lot-handling][data-id="'+fixture.lotId+'"]').click();await p.locator('[name=requirements]').fill('Синтетические сведения для проверки интерфейса; не реальные измерения');await p.locator('[name=status]').selectOption('REJECTED');await p.locator('[name=informationConfirmed]').check();await p.locator('[name=reason]').fill('Проверка блокировки непригодной партии на тестовых данных');await save();
   assert.equal((await api('GET',control+'/products/'+fixture.productId)).product.quantityOnHand,0);
   await p.locator('[data-action=lot-handling][data-id="'+fixture.lotId+'"]').click();await p.locator('[name=status]').selectOption('CONFIRMED');await p.locator('[name=informationConfirmed]').check();await p.locator('[name=reason]').fill('Завершение синтетической проверки; восстановлены тестовые условия');await save();assert.equal((await api('GET',control+'/products/'+fixture.productId)).product.quantityOnHand,3);
   const after=await api('GET',control+'/manager'),tasks=after.queue.tasks.filter(t=>t.productId===fixture.productId);assert.equal(tasks.length,1);assert.ok(tasks[0].generation>saved.generation);
   report.checks.push('UI handling evidence blocked and restored actual availability; new events revised one durable task instead of duplicating it');
   await tab('manager');await p.waitForSelector('#cc-activity .cc-feed li');const center=await api('GET',control+'/manager/center');assert.ok(Number(after.queue.today.products)>0);assert.ok(center.insights);assert.equal(center.decisions.length<=8,true);
   await p.evaluate(()=>ariaTheme.applyTheme('light'));await p.screenshot({path:'target/e2e/control-center-light.png'});await p.evaluate(()=>ariaTheme.applyTheme('dark'));await p.screenshot({path:'target/e2e/control-center-dark.png'});await p.setViewportSize({width:390,height:844});assert.equal(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true);await p.locator('#cc-activity').scrollIntoViewIfNeeded();await p.screenshot({path:'target/e2e/control-center-mobile.png'});
   await p.goto(base+'/index.html');await p.waitForSelector('.aria-shell-burger');assert.equal(await p.locator('#aria-shell-links').isVisible(),false);await p.locator('.aria-shell-burger').click();assert.equal(await p.locator('#aria-shell-links').isVisible(),true);await p.locator('.aria-shell-burger').click();await p.waitForSelector('.product-card');assert.equal(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true);await p.screenshot({path:'target/e2e/catalog-mobile.png'});
   const manifest=await(await ctx.request.get(base+'/manifest.webmanifest')).json();for(const size of [192,512]){const icon=manifest.icons.find(i=>i.sizes===size+'x'+size);assert.ok(icon);const dimensions=await p.evaluate(src=>new Promise((resolve,reject)=>{const im=new Image();im.onload=()=>resolve([im.naturalWidth,im.naturalHeight]);im.onerror=reject;im.src=src;}),icon.src);assert.deepEqual(dimensions,[size,size]);}
   await p.evaluate(async()=>{await navigator.serviceWorker.ready;});const cache=await p.evaluate(async()=>{const all=[];for(const key of await caches.keys())for(const r of await(await caches.open(key)).keys())all.push(new URL(r.url).pathname);return all;});assert.deepEqual(cache.sort(),['/images/aria-app.svg','/offline.html']);
   report.checks.push('Control Center light/dark/mobile, collapsed mobile navigation, real PWA icons and no private API/HTML in service-worker cache');
   for(const path of ['/manager','/manager/center','/purchases','/pos','/lots'])assert.equal((await ctx.request.get(base+control+path)).status(),401);
   await api('POST',control+'/manager/stop');const stopped=await api('GET',control+'/manager');assert.equal(stopped.settings.mode,'OFF');assert.equal(stopped.pricing.externalAiEnabled,false);assert.equal(stopped.pricing.publicAssistantAiEnabled,false);report.queue={taskId:fixture.taskId,generation:tasks[0].generation,source:execution.source};
  }
  assert.deepEqual(report.errors,[]);report.passed=true;console.log(JSON.stringify(report,null,2));
 }catch(e){report.failure=e.stack;throw e;}finally{fs.writeFileSync('target/e2e/autonomous-'+phase+'-report.json',JSON.stringify(report,null,2));await browser.close();}
})().catch(e=>{console.error(e.message);process.exit(1);});
