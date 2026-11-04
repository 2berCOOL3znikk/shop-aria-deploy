'use strict';
// Real local preview only. Fixtures are archived; owner consents and automation are never changed.
const fs=require('node:fs'),assert=require('node:assert/strict'),crypto=require('node:crypto');
const {chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||'playwright');
async function main(){
  const cfg=JSON.parse(fs.readFileSync('data/preview/config.json','utf8').replace(/^\uFEFF/,''));
  const base='http://127.0.0.1:'+cfg.port,dir='target/e2e/product-experience';fs.mkdirSync(dir,{recursive:true});
  const report={checks:[],errors:[],screens:[],externalAiCalled:false};const browser=await chromium.launch({channel:'msedge',headless:true});let context,token,product,lot;
  const call=async(path,method='GET',data)=>{const r=await context.request.fetch(base+'/api/admin/control'+path,{method,data,headers:{Authorization:'Bearer '+token}});const out=await r.json();assert.equal(r.status(),200,path+' '+JSON.stringify(out));return out;};
  try{
    context=await browser.newContext({viewport:{width:1440,height:1000},reducedMotion:'reduce'});const p=await context.newPage();p.on('pageerror',e=>report.errors.push(e.message));
    const login=await context.request.post(base+'/api/auth/login',{data:{email:cfg.email,password:cfg.password}});assert.equal(login.status(),200);token=(await login.json()).token;
    await context.addInitScript(({base,token})=>{if(location.origin===base){localStorage.setItem('aria_token',token);localStorage.setItem('aria_role','ADMIN');localStorage.setItem('aria_lang','ru');}},{base,token});
    const original=await call('/smart-pricing/settings');let openingWrites=0;
    p.on('request',r=>{if(r.method()==='POST'&&/\/smart-pricing.*analyze/.test(r.url()))openingWrites++;});
    await p.goto(base+'/control.html#smart');await p.waitForSelector('.price-table');assert.equal(openingWrites,0,'Opening pricing must not analyze or apply prices');
    for(const width of [1440,768,390])for(const theme of ['light','dark']){
      await p.setViewportSize({width,height:width===390?844:1000});await p.evaluate(t=>{if(ariaTheme.getTheme()!==t)ariaTheme.toggleTheme();},theme);
      assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'Overflow '+width);
      const values=await p.evaluate(()=>({width:innerWidth,buttons:[...document.querySelectorAll('main button')].filter(b=>b.offsetWidth&&!b.disabled).length,characters:document.querySelector('main').innerText.length}));assert.equal(values.width,width);report.screens.push({width,theme,...values});
      await p.screenshot({path:dir+'/pricing-after-'+width+'-'+theme+'.png',fullPage:true});
    }
    await p.setViewportSize({width:1440,height:1000});await p.evaluate(()=>{if(ariaTheme.getTheme()!=='light')ariaTheme.toggleTheme();});
    const input={sku:'QA-PRICE-'+crypto.randomUUID().slice(0,12),name:'Учебный товар для проверки цен',category:'Проверки интерфейса',unitPrice:1000,brand:null,description:'Изолированная проверка интерфейса, не реальные продажи.',imageUrl:null,barcode:null,unit:'шт',lowStockThreshold:0,minimumPrice:650};
    product=await call('/products','POST',input);report.fixture={id:product.id,sku:product.sku};const today=new Date().toLocaleDateString('en-CA',{timeZone:'Asia/Qyzylorda'}),expiry=new Date(Date.now()+90*86400000).toISOString().slice(0,10);
    lot=await call('/lots','POST',{productId:product.id,lotNumber:'QA-PRICE',receivedAt:today,expiryDate:expiry,quantity:12,unitCost:600});report.fixture.lotId=lot.id;
    let d=await call('/smart-pricing/products/'+product.id+'/analyze','POST');
    const search=async value=>{await p.locator('#price-query').fill(value);const loaded=p.waitForResponse(r=>r.request().method()==='GET'&&new URL(r.url()).searchParams.get('query')===value);await p.locator('#price-search [type=submit]').click();await loaded;await p.waitForFunction(id=>document.querySelectorAll('.price-row').length===1&&document.querySelector('.price-row')?.dataset.decision===String(id),d.id);};
    await search(input.sku);assert.equal(await p.locator('.price-row').count(),1);assert.match(await p.locator('.price-row').innerText(),/Цена без изменений/);assert.match(await p.locator('.price-row').innerText(),/недостаточно продаж/);assert.equal(await p.locator('.price-row button').count(),1);
    assert.equal(await p.locator('.price-row [data-smart=apply]').count(),0);report.checks.push('Read-only opening, one latest row per product, honest KEEP with zero sales and one visible action');
    await p.locator('.price-row [data-smart=detail]').click();await p.waitForSelector('#control-dialog[open] .smart-card');assert.equal(await p.locator('#control-dialog details').getAttribute('open'),null);
    await p.locator('#control-dialog [data-smart=edit]').click();await p.locator('#control-dialog [name=percent]').fill('10');await p.locator('#control-dialog [name=reason]').fill('Учебная проверка ручного подтверждения цены');await p.locator('#control-dialog [type=submit]').click();
    await p.waitForFunction(()=>!document.getElementById('control-dialog').open);await p.waitForSelector('.price-row');
    let fresh=await call('/products/'+product.id);assert.equal(fresh.product.discountPercent,10);assert.equal(fresh.product.quantityOnHand,12);
    const publicProduct=await context.request.get(base+'/api/products/'+product.id);assert.equal((await publicProduct.json()).product.discountPercent,10);assert.match(await p.locator('.price-row').innerText(),/900/);
    await p.locator('.price-row [data-smart=detail]').click();await p.locator('#control-dialog [data-smart=rollback]').click();await p.locator('#control-dialog [name=reason]').fill('Учебная проверка безопасного отката');await p.locator('#control-dialog [type=submit]').click();await p.waitForFunction(()=>!document.getElementById('control-dialog').open);
    fresh=await call('/products/'+product.id);assert.equal(fresh.product.discountPercent,0);assert.equal((await call('/smart-pricing/products/'+product.id+'/history')).length,2);report.checks.push('Actual confirmation dialog → safe Java apply → public product price → audit/history → rollback, without stock movement');
    d=await call('/smart-pricing/products/'+product.id+'/analyze','POST');await call('/products/'+product.id,'PUT',{...input,name:'Понятное название владельца',unitPrice:1100});
    await p.reload();await p.waitForSelector('#price-query');await search(input.sku);assert.match(await p.locator('.price-row').innerText(),/Понятное название владельца/);assert.match(await p.locator('.price-row').innerText(),/1[\s\u00a0\u202f]?100/);assert.match(await p.locator('.price-row').innerText(),/Нужен новый расчёт/);assert.equal(await p.locator('.price-row [data-smart=reanalyze]').count(),1);
    const refreshed=p.waitForResponse(r=>r.url().endsWith('/products/'+product.id+'/analyze')&&r.request().method()==='POST');await p.locator('.price-row [data-smart=reanalyze]').click();d=await (await refreshed).json();await p.waitForSelector('.price-row[data-decision="'+d.id+'"]');
    await p.locator('[data-smart=filter][data-id=UNCHANGED]').click();await p.waitForSelector('.price-row[data-decision="'+d.id+'"]');assert.equal(await p.locator('.price-row').count(),1);
    await p.locator('.price-row [data-smart=detail]').click();await p.locator('#control-dialog [data-smart=reject]').click();await p.locator('#control-dialog [name=reason]').fill('Учебная проверка отклонения');await p.locator('#control-dialog [type=submit]').click();await p.waitForFunction(()=>!document.getElementById('control-dialog').open);
    assert.equal((await call('/smart-pricing/'+d.id)).status,'REJECTED');report.checks.push('Current owner name/current price, stale calculation guard, refresh, filters and actual rejection');
    if(process.env.ARIA_E2E_REAL_AI==='true'&&original.externalAiEnabled){
      // A rejected recommendation stays rejected while facts are identical. New facts produce a new decision.
      await call('/products/'+product.id,'PUT',{...input,name:'Понятное название владельца',unitPrice:1150});
      d=await call('/smart-pricing/products/'+product.id+'/analyze','POST');await p.locator('[data-smart=filter][data-id=ALL]').click();await p.waitForSelector('#price-query');await search(input.sku);
      await p.locator('.price-row [data-smart=detail]').click();const [response]=await Promise.all([p.waitForResponse(r=>r.url().endsWith('/smart-pricing/'+d.id+'/ai-decide')&&r.request().method()==='POST',{timeout:120000}),p.locator('#control-dialog [data-smart=ai-decide]').click()]);
      const answer=await response.json();report.ai={decisionId:answer.id,source:answer.source,model:answer.explanation?.model,note:answer.aiLastSkippedNote||answer.explanation?.note};report.externalAiCalled=Boolean((await call('/smart-pricing/ai-status')).lastAttempt);report.realAiDecisionVerified=answer.source==='AI';
      await p.waitForFunction(()=>!document.getElementById('control-dialog').open);await p.waitForFunction(label=>document.querySelector('.price-row')?.textContent.includes(label),answer.source==='AI'?'Решение ARIA AI':'Автоматический расчёт');
      assert.equal((await call('/products/'+product.id)).product.discountPercent,0);await p.screenshot({path:dir+'/pricing-live-ai-result.png',fullPage:true});report.checks.push(answer.source==='AI'?'Real UI → OpenRouter → validated AI decision visible; no automatic permission was enabled':'Real provider availability checked; engine result remains explicitly labelled, no AI success claimed');
    }
    const permissionKeys=['mode','maximumDiscount','cooldownHours','allowLowConfidence','allowRestore','allowedCategories','externalAiEnabled','scheduledAiEnabled','aiDailyBudget','publicAssistantAiEnabled','publicAiDailyBudget'];const afterSettings=await call('/smart-pricing/settings');for(const key of permissionKeys)assert.deepEqual(afterSettings[key],original[key],'Owner permission changed: '+key);
    const broken=await browser.newContext({viewport:{width:390,height:844}});await broken.addInitScript(token=>{localStorage.setItem('aria_token',token);localStorage.setItem('aria_role','ADMIN');},token);
    await broken.route('**/api/admin/control/smart-pricing?*',r=>r.fulfill({status:503,contentType:'application/json',body:'{"message":"Проверка временной недоступности"}'}));const errorPage=await broken.newPage();await errorPage.goto(base+'/control.html#smart');await errorPage.waitForSelector('#price-retry');assert.match(await errorPage.locator('#smart-pricing-root').innerText(),/Проверка временной недоступности/);await broken.unroute('**/api/admin/control/smart-pricing?*');await errorPage.locator('#price-retry').click();await errorPage.waitForSelector('.price-table');await broken.close();report.checks.push('Explicit injected 503 shows a useful error and working Retry; no successful response is simulated');
    assert.deepEqual(report.errors,[]);report.passed=true;
  }catch(e){report.failure=e.stack;throw e;}finally{
    if(product&&token)try{if(lot)await call('/lots/'+lot.id,'DELETE',{reason:'Завершена изолированная проверка интерфейса цен; учебный остаток удалён из продаж'});await call('/products/'+product.id,'DELETE');report.fixtureArchived=true;}catch(e){report.cleanupError=e.message;report.passed=false;process.exitCode=1;}
    fs.writeFileSync(dir+'/pricing-workspace-report.json',JSON.stringify(report,null,2));await browser.close();
  }
}
main().catch(e=>{console.error(e);process.exitCode=1;});
