'use strict';
// One explicit real free-route check on an active educational product. Never enable consent or automation.
const fs=require('node:fs'),assert=require('node:assert/strict');const {chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||'playwright');
if(process.env.ARIA_E2E_REAL_AI!=='true')throw Error('Set ARIA_E2E_REAL_AI=true for an explicit live preview check');
(async()=>{
 const cfg=JSON.parse(fs.readFileSync('data/preview/config.json','utf8'));assert.equal(cfg.port,8084);const base='http://127.0.0.1:'+cfg.port,dir='target/e2e/product-experience';fs.mkdirSync(dir,{recursive:true});
 const b=await chromium.launch({channel:'msedge',headless:true}),report={actualRequestSent:false,errors:[]};let c,token;
 const api=async(path,method='GET',data)=>{const r=await c.request.fetch(base+'/api/admin/control'+path,{method,data,headers:{Authorization:'Bearer '+token}}),out=await r.json();assert.equal(r.status(),200,path+' '+JSON.stringify(out));return out;};
 try{
  c=await b.newContext({viewport:{width:1440,height:1000},reducedMotion:'reduce'});const login=await c.request.post(base+'/api/auth/login',{data:{email:cfg.email,password:cfg.password}});token=(await login.json()).token;
  const binding=await api('/organization');assert.equal(binding.binding,'demo');assert.equal(binding.organization.demonstration,true);const settings=await api('/smart-pricing/settings');assert.equal(settings.externalAiEnabled,true,'Owner consent must already be enabled');
  const listing=await api('/smart-pricing?view=ALL&size=100'),allowed=settings.allowedCategories.map(s=>s.trim().toLowerCase());
  const candidate=listing.content.find(d=>d.status==='PENDING'&&d.report?.facts?.sku?.startsWith('ARIA-DEMO-')&&d.live?.quantity>0&&(settings.mode!=='AUTOPILOT'||!allowed.includes(d.live.category.trim().toLowerCase())));assert.ok(candidate,'Need an active educational product outside automatic price permissions');
  const before=(await api('/products/'+candidate.productId)).product;let d=await api('/smart-pricing/products/'+candidate.productId+'/analyze','POST');assert.equal(d.status,'PENDING');
  await c.addInitScript(token=>{localStorage.setItem('aria_token',token);localStorage.setItem('aria_role','ADMIN');},token);const p=await c.newPage();p.on('pageerror',e=>report.errors.push(e.message));await p.goto(base+'/control.html?decision='+d.id+'#smart');await p.waitForSelector('.price-row');
  if(d.source!=='AI'){
   const status=await api('/smart-pricing/ai-status');assert.equal(status.configured,true);assert.ok(settings.aiBudgetUsed<settings.aiDailyBudget,'Existing daily budget is exhausted; never reset it');
   await p.locator('.price-row [data-smart=detail]').click();const [response]=await Promise.all([p.waitForResponse(r=>r.url().endsWith('/smart-pricing/'+d.id+'/ai-decide')&&r.request().method()==='POST',{timeout:120000}),p.locator('#control-dialog [data-smart=ai-decide]').click()]);assert.equal(response.status(),200);d=await response.json();report.actualRequestSent=Boolean((await api('/smart-pricing/ai-status')).lastAttempt);
  }else report.reusedPersistedDecision=true;
  const after=(await api('/products/'+candidate.productId)).product;assert.equal(after.unitPrice,before.unitPrice);assert.equal(after.discountPercent,before.discountPercent);assert.equal(after.quantityOnHand,before.quantityOnHand);
  report.decision={id:d.id,productId:d.productId,sku:before.sku,source:d.source,strategy:d.strategy,percent:d.selectedPercent,model:d.explanation?.model,completedAt:d.aiCompletedAt,note:d.aiLastSkippedNote||d.explanation?.note};report.realAiDecisionVerified=d.source==='AI'&&d.explanation?.mode==='AI';
  await p.goto(base+'/control.html#smart');await p.waitForFunction(({id,label})=>document.querySelector('[data-decision="'+id+'"]')?.textContent.includes(label),{id:d.id,label:d.source==='AI'?'Решение ARIA AI':'Автоматический расчёт'});
  await p.screenshot({path:dir+'/pricing-active-ai-1440.png',animations:'disabled'});await p.setViewportSize({width:390,height:844});await p.screenshot({path:dir+'/pricing-active-ai-390.png',animations:'disabled'});
  await p.goto(base+'/control.html#manager');await p.waitForSelector('.ai-center');if(d.source==='AI'){const link=p.locator('#cc-decisions a[href="/control.html?decision='+d.id+'#smart"]');await link.waitFor();assert.ok((await link.locator('xpath=..').innerText()).includes('Решение ARIA AI'));}
  await p.screenshot({path:dir+'/center-active-ai-390.png',animations:'disabled'});const current=await api('/smart-pricing/settings');for(const key of ['mode','maximumDiscount','cooldownHours','allowLowConfidence','allowRestore','allowedCategories','externalAiEnabled','scheduledAiEnabled','aiDailyBudget','publicAssistantAiEnabled','publicAiDailyBudget'])assert.deepEqual(current[key],settings[key]);
  assert.deepEqual(report.errors,[]);report.priceAndStockUnchanged=true;report.visibleInPricing=true;report.visibleInCenter=d.source==='AI';report.passed=report.realAiDecisionVerified;
 }catch(e){report.failure=e.stack;throw e;}finally{fs.writeFileSync(dir+'/active-pricing-ai-proof.json',JSON.stringify(report,null,2));await b.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
