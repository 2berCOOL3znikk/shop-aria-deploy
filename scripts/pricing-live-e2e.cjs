'use strict';
// Explicit live test, recommendation-only: never applies a price or enables automatic AI.
const fs=require('node:fs'),assert=require('node:assert/strict');
const requireAI=process.env.ARIA_E2E_EXPECT_AI!=='false';
const {chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||'playwright');
if(process.env.ARIA_E2E_REAL_AI!=='true')throw Error('Set ARIA_E2E_REAL_AI=true to explicitly enable this live preview check');
(async()=>{
 const cfg=JSON.parse(fs.readFileSync('data/preview/config.json','utf8')),base='http://127.0.0.1:'+cfg.port,dir='target/e2e/aria3';
 const b=await chromium.launch({channel:'msedge',headless:true}),report={realOpenRouter:true,checks:[],errors:[]};let context,token,original;
 const body=s=>Object.fromEntries(['version','mode','maximumDiscount','cooldownHours','allowLowConfidence','allowRestore','allowedCategories','externalAiEnabled','scheduledAiEnabled','aiDailyBudget','publicAssistantAiEnabled','publicAiDailyBudget'].map(k=>[k,s[k]]));
 const api=async(path,method='GET',data)=>{const r=await context.request.fetch(base+'/api/admin/control'+path,{method,headers:{Authorization:'Bearer '+token},data});assert.equal(r.status(),200);return r.json();};
 try{
  context=await b.newContext({viewport:{width:1440,height:1000}});const p=await context.newPage();p.on('pageerror',e=>report.errors.push(e.message));
  await p.goto(base+'/login.html');await p.locator('#login-email').fill(cfg.email);await p.locator('#login-password').fill(cfg.password);await p.locator('#login-form [type=submit]').click();await p.waitForURL('**/control.html');await p.waitForSelector('.ws-ai-card');token=await p.evaluate(()=>localStorage.getItem('aria_token'));
  original=await api('/smart-pricing/settings');assert.equal(original.mode,'RECOMMEND','Live check requires preview recommendation mode');
  let chosen;for(let page=0;page<10;page++){const listing=await api('/smart-pricing?page='+page+'&size=100');chosen=listing.content.find(d=>d.status==='PENDING'&&d.report.facts.quantity>0&&d.report.scenarios.length);if(chosen||page+1>=listing.totalPages)break;}assert.ok(chosen,'No eligible saved recommendation in the preview');
  report.syntheticFixture=/\d{10}/.test(chosen.report.facts.name);const before=(await api('/products/'+chosen.productId)).product;
  await p.goto(base+'/control.html?decision='+chosen.id+'#smart');await p.waitForSelector('.smart-card');
  if(!original.externalAiEnabled){await p.locator('[data-smart=settings]').click();await p.locator('#control-dialog [name=externalAiEnabled]').check();await p.locator('#control-dialog [type=submit]').click();await p.waitForFunction(()=>!document.getElementById('control-dialog').open);await p.waitForSelector('.smart-card');}
  const result=p.waitForResponse(r=>r.url().endsWith('/smart-pricing/'+chosen.id+'/ai-decide')&&r.request().method()==='POST');await p.locator('[data-smart=ai-decide][data-id="'+chosen.id+'"]').click();const r=await result;const answer=await r.json();assert.equal(r.status(),200,JSON.stringify(answer));const realAI=answer.source==='AI';report.realAiDecisionVerified=realAI;report.actualRequestSent=true;if(requireAI)assert.equal(answer.source,'AI',JSON.stringify({source:answer.source,note:answer.explanation?.note}));else assert.ok(['AI','ENGINE'].includes(answer.source));assert.equal(answer.explanation.mode,realAI?'AI':'ENGINE');if(!realAI){report.blockedReason=answer.explanation.note;report.aiScenarioPassed=false;}
  await p.waitForFunction(label=>document.querySelector('.smart-card')?.textContent.includes(label),realAI?'Решение ARIA AI':'Автоматический расчёт');await p.screenshot({path:dir+(realAI?'/pricing-real-ai-1440.png':'/pricing-provider-unavailable-1440.png'),fullPage:true});
  const after=(await api('/products/'+chosen.productId)).product;assert.equal(after.unitPrice,before.unitPrice);assert.equal(after.discountPercent,before.discountPercent);report.decision={id:answer.id,source:answer.source,strategy:answer.strategy,model:answer.explanation.model};
  await p.goto(base+'/control.html#manager');await p.waitForSelector('.ai-center');await p.waitForFunction(label=>document.getElementById('cc-decisions')?.textContent.includes(label),realAI?'Решение ARIA AI':'ИИ временно недоступен');assert.equal(await p.locator('.cc-sections button').count(),7);
  for(const width of [1440,390]){await p.setViewportSize({width,height:900});assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));await p.screenshot({path:dir+(realAI?'/center-real-ai-':'/center-provider-unavailable-')+width+'.png',fullPage:true});}
  await p.locator('[data-manager=settings]').first().click();await p.waitForSelector('.ai-setup-step');assert.equal(await p.locator('.ai-setup-step').count(),3);await p.locator('#control-close').click();
  assert.deepEqual(report.errors,[]);report.checks.push(realAI?'Actual UI→AI Decision Service→OpenRouter→persisted AI recommendation; prices unchanged in RECOMMEND; real decision visible in pricing and AI Control Center':'Actual OpenRouter request failed; UI honestly shows engine calculation and provider unavailability; price unchanged, no fictional AI result');report.availabilityCheckPassed=true;report.passed=realAI;
 }catch(e){report.failure=e.stack;throw e;}
 finally{if(original&&context&&token){try{const current=await api('/smart-pricing/settings');if(current.externalAiEnabled!==original.externalAiEnabled)await api('/smart-pricing/settings','PUT',{...body(original),version:current.version});}catch(e){report.restoreError=e.message;report.passed=false;process.exitCode=1;}}fs.mkdirSync(dir,{recursive:true});fs.writeFileSync(dir+'/pricing-live-report.json',JSON.stringify(report,null,2));await b.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
