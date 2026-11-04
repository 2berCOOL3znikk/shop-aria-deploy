'use strict';
// Read-only browser verification against the isolated loopback preview.
const fs=require('node:fs'),assert=require('node:assert/strict');
const {chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||'playwright');
(async()=>{
 const cfg=JSON.parse(fs.readFileSync('data/preview/config.json','utf8'));
 const base='http://127.0.0.1:'+cfg.port;
 const browser=await chromium.launch({channel:'msedge',headless:true});
 const report={themes:[],errors:[]};
 try{
  const context=await browser.newContext({viewport:{width:1440,height:1000},reducedMotion:'reduce'}),page=await context.newPage();
  page.on('pageerror',e=>report.errors.push(e.message));
  await page.goto(base+'/login.html');await page.locator('#login-email').fill(cfg.email);await page.locator('#login-password').fill(cfg.password);
  await page.locator('#login-form [type=submit]').click();await page.waitForURL('**/control.html');await page.waitForSelector('.ws-ai-card');
  const token=await page.evaluate(()=>localStorage.getItem('aria_token'));
  const get=async path=>{const r=await context.request.get(base+'/api/admin/control'+path,{headers:{Authorization:'Bearer '+token}});assert.equal(r.status(),200);return r.json();};
  const listing=await get('/smart-pricing?view=ALL&size=100');const selected=listing.content.find(d=>d.source==='AI');assert.ok(selected,'A genuine saved AI decision is required');
  const decision=await get('/smart-pricing/'+selected.id);assert.equal(decision.source,'AI');
  report.ai={decisionId:decision.id,source:decision.source,model:decision.explanation?.model};
  await page.goto(base+'/control.html?decision='+decision.id+'#smart');await page.waitForSelector('.price-row');assert.equal(await page.locator('.price-row').count(),1);await page.locator('.price-row [data-smart=detail]').click();await page.waitForSelector('#control-dialog .smart-card');
  assert.equal(await page.locator('.smart-card').count(),1);
  assert.match(await page.locator('.smart-card').innerText(),new RegExp('решение #'+decision.id));
  await page.locator('#control-close').click();await page.locator('[data-smart=all]').click();await page.waitForFunction(()=>!new URLSearchParams(location.search).has('decision'));
  await page.goto(base+'/control.html#manager');await page.waitForSelector('#cc-overview');
  const facts=await get('/workspace/overview'),center=await get('/manager/center');const values=await page.locator('.cc-kpis strong').allTextContents();assert.deepEqual(values.map(Number),[facts.today.productsAnalyzed,facts.today.aiDecisions,facts.today.priceApplied,Number(facts.today.purchaseDrafts)+Number(facts.today.socialDrafts),center.openSignals]);assert.ok(facts.today.aiDecisions>0);if(facts.organization.demonstration)assert.ok((await page.locator('.cc-hero').innerText()).includes('Учебная компания'));report.dailyFacts=facts.today;report.persistedAiCountersVerified=true;
  await page.route('**/api/admin/control/workspace/overview',route=>route.fulfill({status:503,contentType:'application/json',body:'{}'}));await page.reload();await page.waitForSelector('.cc-kpis');assert.equal(await page.locator('.cc-kpis strong').nth(1).innerText(),'—');await page.unroute('**/api/admin/control/workspace/overview');await page.locator('#control-refresh').click();await page.waitForFunction(()=>document.querySelectorAll('.cc-kpis strong')[1]?.textContent!=='—');
  for(const width of [1440,768,390])for(const theme of ['light','dark']){await page.setViewportSize({width,height:width===390?844:1000});

   await page.evaluate(t=>ariaTheme.applyTheme(t),theme);
   const contrast=await page.locator('[data-manager=settings]').first().evaluate(el=>{
    const s=getComputedStyle(el),rgb=v=>(v.match(/[\d.]+/g)||[]).slice(0,3).map(Number);
    const luminance=c=>c.map(v=>{v/=255;return v<=.04045?v/12.92:((v+.055)/1.055)**2.4;}).reduce((a,v,i)=>a+v*[.2126,.7152,.0722][i],0);
    let node=el,background=s.backgroundColor;while((background==='transparent'||background==='rgba(0, 0, 0, 0)')&&node.parentElement){node=node.parentElement;background=getComputedStyle(node).backgroundColor;}const a=luminance(rgb(s.color)),b=luminance(rgb(background));return (Math.max(a,b)+.05)/(Math.min(a,b)+.05);
   });assert.ok(contrast>=4.5,theme+' control text contrast '+contrast);
   report.themes.push({width,theme,controlTextContrast:contrast});
   await page.screenshot({path:'target/e2e/center-final-'+width+'-'+theme+'.png',animations:'disabled'});
  }
  await page.setViewportSize({width:390,height:844});assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));await page.screenshot({path:'target/e2e/center-final-mobile.png'});
  await page.goto(base+'/index.html');await page.waitForSelector('.product-card');await page.locator('.aria-shell-burger').click();await page.locator('#theme-toggle-btn').click();await page.screenshot({path:'target/e2e/catalog-final-mobile.png'});
  assert.deepEqual(report.errors,[]);report.passed=true;console.log('PASS: persisted AI counters, explicit unavailable state, contrast and responsive control center');
 }finally{fs.writeFileSync('target/e2e/control-center-report.json',JSON.stringify(report,null,2));await browser.close();}
})().catch(e=>{console.error(e);process.exit(1);});
