'use strict';
// Synthetic, explicitly local preview only. Never run mutations against Render/Neon.
const {chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||'playwright');
const fs=require('node:fs'),assert=require('node:assert/strict');
const cfg=JSON.parse(fs.readFileSync('data/preview/config.json','utf8').replace(/^\uFEFF/,''));
const base='http://127.0.0.1:'+cfg.port,stamp=Date.now();
const date=n=>new Date(Date.now()+n*86400000).toLocaleDateString('en-CA',{timeZone:'Asia/Qyzylorda'});
const report={at:new Date().toISOString(),base,checks:[],tabs:[],errors:[]};
fs.mkdirSync('target/e2e',{recursive:true});
(async()=>{
 const browser=await chromium.launch({channel:'msedge',headless:true});
 try{
  const ctx=await browser.newContext({viewport:{width:1440,height:1000}}),p=await ctx.newPage();
  p.on('pageerror',e=>report.errors.push(e.message));p.on('dialog',d=>d.accept());
  await p.goto(base+'/login.html');await p.locator('#login-email').fill(cfg.email);await p.locator('#login-password').fill(cfg.password);await p.locator('#login-form [type=submit]').click();await p.waitForURL('**/control.html');
  const token=await p.evaluate(()=>localStorage.getItem('aria_token'));
  const api=async(method,path,data,auth=token)=>{const r=await ctx.request.fetch(base+path,{method,data,headers:auth?{Authorization:'Bearer '+auth}:{}});assert.ok(r.ok(),path+' '+r.status()+' '+await r.text());return r.json();};
  const control='/api/admin/control';
  const tab=async key=>{await p.locator('#control-nav [data-id="'+key+'"]').click();await p.waitForFunction(()=>document.getElementById('control-content').getAttribute('aria-busy')==='false');};
  const save=async()=>{await p.locator('#control-dialog [type=submit]').click();await p.waitForFunction(()=>!document.getElementById('control-dialog').open);await p.waitForFunction(()=>document.getElementById('control-content').getAttribute('aria-busy')==='false');};
  const initial=await api('POST',control+'/manager/stop');const before=initial.runs[0]?.id||0;
  await tab('suppliers');await p.locator('[data-action=supplier-new]').click();await p.locator('[name=name]').fill('Поставщик платформы '+stamp);await save();
  const supplier=(await api('GET',control+'/suppliers')).find(s=>s.name.endsWith(stamp));
  await tab('products');await p.locator('[data-action=product-new]').click();await p.locator('[name=sku]').fill('PLATFORM-'+stamp);await p.locator('[name=name]').fill('Товар кассы и закупки '+stamp);await p.locator('[name=barcode]').fill('POS-'+stamp);await p.locator('[name=unitPrice]').fill('1500');await p.locator('[name=minimumPrice]').fill('950');await p.locator('[name=lowStockThreshold]').fill('6');await save();
  const product=(await api('GET',control+'/products')).find(x=>x.sku==='PLATFORM-'+stamp);
  await p.locator('[data-action=lot-new][data-id="'+product.id+'"]').click();await p.locator('[name=lotNumber]').fill('PLATFORM-FIRST-'+stamp);await p.locator('[name=quantity]').fill('4');await p.locator('[name=unitCost]').fill('800');await p.locator('[name=expiryDate]').fill(date(15));await save();
  await tab('purchases');await p.locator('[data-purchase=quote]').first().click();await p.locator('[name=productId]').selectOption(String(product.id));await p.locator('[name=supplierId]').selectOption(String(supplier.id));await p.locator('[name=unitCost]').fill('800');await p.locator('[name=leadDays]').fill('2');await p.locator('[name=validUntil]').fill(date(7));await p.locator('[name=informationConfirmed]').check();await save();
  const quote=(await api('GET',control+'/purchases')).quotes.find(q=>q.productId===product.id);
  await p.locator('[data-purchase=draft]').click();await p.locator('[name=quoteId]').selectOption(String(quote.id));await p.locator('[name=quantity]').fill('4');await p.locator('[name=reason]').fill('Синтетическая проверка платформы');await save();
  let purchase=(await api('GET',control+'/purchases')).requests.find(r=>r.productId===product.id);assert.equal(purchase.status,'DRAFT');
  await p.locator('[data-purchase=approve][data-id="'+purchase.id+'"]').click();await p.locator('[name=reason]').fill('Проверены актуальные условия');await save();
  await p.locator('[data-purchase=receive][data-id="'+purchase.id+'"]').click();await p.locator('[name=quantity]').fill('2');await p.locator('[name=lotNumber]').fill('PLATFORM-RECEIVE-'+stamp);await p.locator('[name=expiryDate]').fill(date(25));await save();
  purchase=(await api('GET',control+'/purchases')).requests.find(r=>r.id===purchase.id);assert.equal(purchase.status,'PART_RECEIVED');assert.equal((await api('GET',control+'/products/'+product.id)).product.quantityOnHand,6);
  report.checks.push('UI: supplier, product, real lot, verified quote, purchase draft, approval and partial receiving');
  await tab('pos');await p.locator('#pos-code').fill('POS-'+stamp);await p.locator('#pos-scan-form [type=submit]').click();await p.locator('[data-pos=plus][data-id="'+product.id+'"]').click();
  let saleCalls=0;await p.route('**/api/admin/control/pos/sales',async route=>{saleCalls++;await new Promise(r=>setTimeout(r,300));await route.continue();});
  const saleResponse=p.waitForResponse(r=>r.url().endsWith('/pos/sales'));await p.locator('[data-pos=sale]').dblclick({delay:20});const sale=await(await saleResponse).json();assert.ok(sale.receipt.id);assert.equal(saleCalls,1);await p.unroute('**/api/admin/control/pos/sales');
  await p.waitForSelector('[data-pos=return][data-id="'+sale.receipt.id+'"]');assert.equal((await api('GET',control+'/products/'+product.id)).product.quantityOnHand,4);
  await p.locator('[data-pos=return][data-id="'+sale.receipt.id+'"]').click();await p.locator('[name=reason]').fill('Синтетический возврат; карантин, не в продажу');await save();
  assert.equal((await api('GET',control+'/products/'+product.id)).product.quantityOnHand,4);
  await p.locator('#pos-code').fill('POS-'+stamp);await p.locator('#pos-scan-form [type=submit]').click();
  let lostReceipt;await p.route('**/api/admin/control/pos/sales',async route=>{lostReceipt=await(await route.fetch()).json();await route.abort('failed');},{times:1});
  await p.locator('[data-pos=sale]').click();await p.waitForFunction(()=>document.getElementById('pos-root').textContent.includes('Неподтверждённая попытка'));
  await p.waitForFunction(()=>!document.querySelector('[data-pos=sale]').disabled);await p.locator('[data-pos=sale]').click();
  await p.waitForSelector('[data-pos=return][data-id="'+lostReceipt.receipt.id+'"]');assert.equal((await api('GET',control+'/products/'+product.id)).product.quantityOnHand,3);
  assert.equal((await api('GET',control+'/pos')).filter(x=>x.receipt.id===lostReceipt.receipt.id).length,1);
  await p.locator('[data-pos=camera]').click();await p.waitForFunction(()=>/скан|браузер|код/i.test(document.getElementById('control-message').textContent+document.getElementById('pos-camera-status').textContent));
  await tab('movements');await p.locator('#movement-product').selectOption(String(product.id));await p.waitForFunction(()=>document.getElementById('movements-root').textContent.includes('Карантин возврата'));assert.match(await p.locator('#movements-root').innerText(),/Продажа POS/);
  const ledger=await api('GET',control+'/movements?productId='+product.id);assert.equal(ledger.movements.content.find(m=>m.type==='RETURN_QUARANTINE').quarantineUnits,2);assert.equal(ledger.writtenOffUnits30,0);
  await tab('integrations');await p.waitForSelector('#integrations-root h4');assert.equal(await p.locator('#integrations-root h4').count(),6);assert.match(await p.locator('#integrations-root').innerText(),/Poster POS · не подключён/);
  const mapping=await api('POST',control+'/integrations/poster/prepare',{sale:{requestKey:'mapping-'+stamp,lines:[{productId:product.id,quantity:1,expectedPrice:1500}]},productIds:{[product.id]:'125'}});assert.equal(mapping.sent,false);assert.equal(mapping.fiscal,false);assert.equal((await api('GET',control+'/products/'+product.id)).product.quantityOnHand,3);
  const autoProduct=await api('POST',control+'/products',{sku:'AUTO-PO-'+stamp,name:'Проверка автономной закупки '+stamp,category:'Продукты',unitPrice:1500,unit:'шт',lowStockThreshold:2});
  await api('POST',control+'/purchases/quotes',{productId:autoProduct.id,supplierId:supplier.id,unitCost:800,minimumQuantity:1,leadDays:2,quotedOn:date(0),validUntil:date(7),informationConfirmed:true});
  report.checks.push('UI: barcode POS, single sale on double click, FEFO stock, quarantine return, movements, camera fallback and honest disconnected integrations');
  // Enable private consent through its actual visible form. No key is put in the browser.
  await tab('smart');await p.locator('[data-smart=settings]').click();await p.locator('[name=externalAiEnabled]').check();await p.locator('[name=aiDailyBudget]').fill('10');await save();
  await tab('manager');await p.locator('[data-manager=settings]').click();await p.locator('[name=mode]').selectOption('ADVISOR');await p.locator('[name=automaticAiConsent]').check();await p.locator('[name=purchaseDrafts]').check();await p.locator('[name=dailyDraftBudget]').fill('100000');await save();
  // A normal storefront read wakes the activity-driven cycle. No manual /manager/run here.
  await p.goto(base+'/index.html');await p.waitForSelector('#aria-install');
  const started=Date.now();let state,run;
  while(Date.now()-started<150000){
   await api('GET','/api/products/store',null,null);state=await api('GET',control+'/manager');run=state.runs.find(r=>r.id>before&&r.status!=='RUNNING');if(run)break;await new Promise(r=>setTimeout(r,3000));
  }
  assert.ok(run,'Ordinary store activity must trigger a complete manager cycle');const result=JSON.parse(run.resultJson);assert.ok(result.scanned>0);assert.ok(result.aiAttempts<=1);assert.equal(result.aiAppliedDecisions,0);report.manager={mode:run.mode,status:run.status,result};
  assert.ok(state.actions.some(a=>a.productId===product.id&&a.type==='REORDER'));assert.equal((await api('GET',control+'/products/'+product.id)).product.quantityOnHand,3);
  const automaticPurchase=(await api('GET',control+'/purchases')).requests.find(r=>r.productId===autoProduct.id);assert.equal(automaticPurchase.status,'DRAFT');assert.equal(automaticPurchase.managerCreated,true);assert.equal((await api('GET',control+'/products/'+autoProduct.id)).product.quantityOnHand,0);
  report.checks.push('Activity-triggered persisted ADVISOR cycle, real risks, bounded AI and no automatic price/stock changes');
  // AI is never silently credited for ENGINE fallback. Record genuine source and fail an explicit AI audit if absent.
  report.genuineManagerAi=result.aiResults?.some(a=>a.source==='AI')||false;
  await p.goto(base+'/control.html#manager');await p.waitForSelector('[data-manager=settings]');await p.locator('[data-manager=analytics][data-id="'+product.id+'"]').first().click();await p.waitForSelector('#control-dialog svg');assert.match(await p.locator('#control-dialog').innerText(),/недостаточно данных|INSUFFICIENT_HISTORY/);await p.locator('#control-close').click();
  await p.locator('[data-manager=ack]').first().click();await p.waitForFunction(()=>document.getElementById('manager-root').textContent.includes('Прочитано'));
  for(const viewport of [{width:1440,height:1000},{width:390,height:844}]){
   await p.setViewportSize(viewport);
   const keys=await p.locator('#control-nav [data-id]').evaluateAll(nodes=>nodes.map(n=>n.dataset.id));
   for(const key of keys){await tab(key);await p.waitForTimeout(100);assert.equal(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true,key+' overflow '+viewport.width);report.tabs.push(key+'@'+viewport.width);}
  }
  await tab('manager');await p.locator('[data-manager=stop]').click();await p.waitForFunction(()=>document.getElementById('manager-root').textContent.includes('Отключён политикой владельца'));const stopped=await api('GET',control+'/manager');assert.equal(stopped.settings.mode,'OFF');assert.equal(stopped.pricing.externalAiEnabled,false);assert.equal(stopped.pricing.publicAssistantAiEnabled,false);
  const manifest=await(await ctx.request.get(base+'/manifest.webmanifest')).json();assert.equal(manifest.display,'standalone');assert.ok(manifest.icons.some(i=>i.sizes==='192x192'));assert.ok(manifest.icons.some(i=>i.sizes==='512x512'));
  await p.evaluate(async()=>{await navigator.serviceWorker.ready;});
  const cached=await p.evaluate(async()=>{const urls=[];for(const key of await caches.keys())for(const r of await(await caches.open(key)).keys())urls.push(new URL(r.url).pathname);return urls;});assert.deepEqual(cached.sort(),['/images/aria-app.svg','/offline.html']);
  await ctx.setOffline(true);await p.goto(base+'/control.html');assert.match(await p.locator('body').innerText(),/соединени|сети/i);assert.equal(await p.evaluate(async()=>fetch('/api/admin/control/products').then(()=>true).catch(()=>false)),false);await ctx.setOffline(false);
  for(const path of ['/manager','/purchases','/pos','/movements','/integrations','/social-proposals/staff']){const r=await ctx.request.get(base+control+path);assert.equal(r.status(),401);}
  report.checks.push('All admin sections at desktop/mobile widths, emergency consent revocation, PWA manifest, public-only cache, offline page and API isolation');
  await p.reload();await p.goto(base+'/control.html#manager');await p.waitForSelector('[data-manager=settings]');await p.screenshot({path:'target/e2e/platform-manager-mobile.png',fullPage:true});await p.setViewportSize({width:1440,height:1000});await p.screenshot({path:'target/e2e/platform-manager-desktop.png',fullPage:true});
  assert.deepEqual(report.errors,[]);report.passed=true;console.log(JSON.stringify(report,null,2));
 }catch(error){report.failure=error.stack;throw error;}finally{fs.mkdirSync('target/e2e',{recursive:true});fs.writeFileSync('target/e2e/platform-report.json',JSON.stringify(report,null,2));await browser.close();}
})().catch(error=>{console.error(error.message);process.exit(1);});
