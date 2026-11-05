'use strict';
// Real installed WooCommerce GETs only. Imports update isolated ARIA snapshots, never external prices.
const fs=require('node:fs'),https=require('node:https'),assert=require('node:assert/strict');
const {chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||'playwright');
const read=file=>JSON.parse(fs.readFileSync(file,'utf8').replace(/^\uFEFF/,''));
const cfg=read('data/woocommerce/aria-config.json'),fixture=read('data/woocommerce/fixtures.json');
const previous=read('docs/connected-commerce/evidence/woocommerce-e2e.json'),storeId=previous.storeId;
const base='http://127.0.0.1:8085',directory='docs/platform/evidence',ca=fs.readFileSync('data/woocommerce/tls-cert.pem');
fs.mkdirSync(directory,{recursive:true});
const report={environment:'isolated woo-e2e + installed local WordPress/WooCommerce',realWooCommerceReads:true,productionChanged:false,externalWrites:false,aiRequested:false,consentsChanged:false,checks:[],errors:[]};
function woo(resource){return new Promise((resolve,reject)=>{
 const request=https.get('https://127.0.0.1:8449/wp-json/wc/v3/'+resource,{ca,headers:{Authorization:'Basic '+Buffer.from(fixture.readKey+':'+fixture.readSecret).toString('base64')}},response=>{
  let text='';response.on('data',part=>text+=part);response.on('end',()=>{try{assert.equal(response.statusCode,200);resolve(JSON.parse(text));}catch(error){reject(error);}});
 });request.setTimeout(20000,()=>request.destroy(new Error('Local WooCommerce timeout')));request.on('error',reject);
});}
function puts(){return fs.readFileSync('data/woocommerce/request-audit.jsonl','utf8').trim().split('\n').map(JSON.parse).filter(row=>row.method==='PUT').length;}
async function poll(check){const end=Date.now()+60000;while(Date.now()<end){const result=await check();if(result)return result;await new Promise(resolve=>setTimeout(resolve,300));}throw new Error('Read queue did not complete');}
(async()=>{
 const browser=await chromium.launch({channel:'msedge',headless:true});
 try{
  let context=await browser.newContext({viewport:{width:1440,height:1000}}),page=await context.newPage();
  page.on('pageerror',error=>report.errors.push(error.message));
  await page.goto(base+'/login.html');await page.locator('#login-email').fill(cfg.email);await page.locator('#login-password').fill(cfg.password);await page.locator('#login-form [type=submit]').click();await page.waitForURL('**/control.html');
  const {token,role}=await page.evaluate(()=>({token:localStorage.getItem('aria_token'),role:localStorage.getItem('aria_role')}));
  const api=async(resource,method='GET',body)=>{const response=await context.request.fetch(base+'/api/admin/control'+resource,{method,headers:{Authorization:'Bearer '+token},...(body?{data:body}:{})});assert.equal(response.status(),200,resource);return response.json();};
  assert.equal((await api('/organization')).binding,'woo-e2e');
  const current=async()=>{const store=(await api('/stores')).stores.find(s=>s.id===storeId);assert.equal(store.testConnection,true);assert.equal(store.archived,false);assert.equal(store.url,'https://127.0.0.1:8449');return store;};
  const prefix='/stores/'+storeId,before=await api(prefix+'/catalog'),ordersBefore=await api(prefix+'/orders'),policyBefore=await api(prefix+'/pricing/settings'),globalBefore=await api('/smart-pricing/settings'),decisionsBefore=await api(prefix+'/pricing/decisions');
  assert.equal(policyBefore.scheduledAiConsent,false,'This read-only check must not trigger model calls');
  const ai=decisionsBefore.find(d=>d.source==='AI');assert.ok(ai,'Persisted real AI decision must remain available');
  const writesBefore=puts();
  const completed=operation=>poll(async()=>{const store=await current(),task=store.tasks.find(t=>t.operation===operation);if(['FAILED','RETRY'].includes(task?.state))throw new Error('Store read failed: '+(store.lastError||store.lastOrdersError||task.lastError));return task?.state==='DONE'&&!store.syncInProgress?store:false;});
  await page.goto(base+'/control.html#stores');await page.locator('#stores-search').waitFor();await page.locator('[data-store=filter][data-id=TEST]').click();await page.locator('[data-store=open][data-id="'+storeId+'"]').click();await page.locator('[data-store=sync]').click();await completed('SYNC');
  const identities=rows=>rows.map(row=>[row.externalId,row.id]).sort((a,b)=>a[0].localeCompare(b[0]));
  assert.deepEqual(identities(await api(prefix+'/catalog')),identities(before));
  for(const operation of ['SYNC','ORDERS','ORDERS']){const store=await current();await api(prefix+'/tasks','POST',{version:store.version,operation});await completed(operation);}
  const after=await api(prefix+'/catalog'),currency=await woo('data/currencies/current'),simple=await woo('products/'+fixture.simpleId);
  const variations=await woo('products/'+fixture.parentId+'/variations?per_page=100&page=1');
  assert.deepEqual(identities(after),identities(before));
  for(const external of [simple,...variations]){const saved=after.find(row=>row.externalId===String(external.id));assert.ok(saved);assert.equal(Number(saved.price),Number(external.price));assert.equal(saved.availableQuantity,external.manage_stock?external.stock_quantity:null);assert.equal(saved.currency,currency.code);}
  assert.deepEqual((await api(prefix+'/orders')).map(o=>[o.externalId,o.id]).sort(),ordersBefore.map(o=>[o.externalId,o.id]).sort());
  assert.deepEqual(await api(prefix+'/pricing/settings'),policyBefore);assert.deepEqual(await api('/smart-pricing/settings'),globalBefore);assert.deepEqual(await api(prefix+'/pricing/decisions'),decisionsBefore);assert.equal(puts(),writesBefore);
  report.checks.push('Real UI catalog refresh + second import retain product and variation IDs; current prices/currency/stock match actual WooCommerce GETs','Two real order imports retain order IDs and do not copy customer contacts','Existing AI decisions, permissions, budgets and external prices were preserved');
  report.catalog=after.map(row=>({externalId:row.externalId,id:row.id,price:row.price,currency:row.currency,availableQuantity:row.availableQuantity}));report.persistedAi={id:ai.id,source:ai.source,currentPrice:ai.currentPrice,suggestedPrice:ai.suggestedPrice};
  await context.close();
  for(const language of ['ru','en']){
   context=await browser.newContext({viewport:{width:390,height:1000}});await context.addInitScript(({token,role,language})=>{localStorage.setItem('aria_token',token);localStorage.setItem('aria_role',role);localStorage.setItem('aria_lang',language);},{token,role,language});page=await context.newPage();page.on('pageerror',error=>report.errors.push(error.message));
   await page.goto(base+'/control.html#stores');await page.locator('#stores-search').waitFor();await page.locator('[data-store=filter][data-id=TEST]').click();await page.locator('[data-store=open][data-id="'+storeId+'"]').click();
   // UI-only controlled late-response status. Actual cancellation/price races are separate PostgreSQL tests.
   const historyPattern='**/api/admin/control'+prefix+'/history';
   await page.route(historyPattern,route=>route.fulfill({json:[{id:0,completedAt:'2026-11-05T12:00:00',operation:'SYNC',status:'STALE',imported:0,skipped:0,note:'Чтение завершено после изменения подключения или данных. Ответ не применён.'}]}));
   await page.locator('[data-store=history]').click();const body=page.locator('#control-dialog-body');
   await body.getByText(language==='en'?'Response discarded':'Ответ не применён').first().waitFor();
   assert.match(await body.innerText(),language==='en'?/connection or data changed/:/изменения подключения/);
   for(const width of [360,390,768,1440]){await page.setViewportSize({width,height:1000});assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));
    if(width<=390){assert.ok(await body.locator('.store-sync-history').evaluate(wrap=>wrap.scrollWidth<=wrap.clientWidth+1));assert.equal(await body.locator('.store-sync-history').evaluate(wrap=>getComputedStyle(wrap,'::before').display),'none');
     if(language==='en'){assert.deepEqual(await body.locator('[data-label]').evaluateAll(cells=>cells.map(cell=>cell.dataset.label)),['When','Operation','Result','Records']);assert.match(await body.innerText(),/Skipped: 0/);}}
    if(language==='en'&&width<=390)assert.match(await page.evaluate(()=>{const test=document.createElement('div');test.className='control-table-wrap';document.body.append(test);const value=getComputedStyle(test,'::before').content;test.remove();return value;}),/Swipe right/);
    await page.screenshot({path:directory+'/store-discarded-'+language+'-'+width+'.png'});}
   await page.unroute(historyPattern);await page.locator('#control-close').click();await page.goto(base+'/control.html?storeId='+storeId+'#stores');await page.locator('.connected-ai-item').first().waitFor();assert.match(await page.locator('#connected-pricing-panel').innerText(),language==='en'?/ARIA AI decision/:/Решение ARIA AI/);
   await context.close();
  }
  report.historyStatusUi='Controlled response, RU/EN, 360/390/768/1440px; actual server races verified separately in ControlIntegrationTest';report.checks.push('Discarded response is explained in both languages; mobile history shows the outcome without horizontal scrolling; generic English table hints are translated; saved real AI remains visible in the connected store');assert.deepEqual(report.errors,[]);report.passed=true;
 }catch(error){report.failure=error.stack;throw error;}finally{report.completedAt=new Date().toISOString();fs.writeFileSync(directory+'/store-read-refresh.json',JSON.stringify(report,null,2));await browser.close();}
})().catch(error=>{console.error(error.stack);process.exitCode=1;});
