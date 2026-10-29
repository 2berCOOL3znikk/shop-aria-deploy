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
  const context=await browser.newContext({viewport:{width:1440,height:1000}}),page=await context.newPage();
  page.on('pageerror',e=>report.errors.push(e.message));
  await page.goto(base+'/login.html');await page.locator('#login-email').fill(cfg.email);await page.locator('#login-password').fill(cfg.password);
  await page.locator('#login-form [type=submit]').click();await page.waitForURL('**/control.html');await page.waitForSelector('#cc-overview');
  const token=await page.evaluate(()=>localStorage.getItem('aria_token'));
  const get=async path=>{const r=await context.request.get(base+'/api/admin/control'+path,{headers:{Authorization:'Bearer '+token}});assert.equal(r.status(),200);return r.json();};
  const previous=JSON.parse(fs.readFileSync('target/e2e/platform-report.json','utf8'));
  const selected=previous.manager.result.aiResults.find(d=>d.source==='AI');assert.ok(selected,'A genuine autonomous AI result is required');
  const decision=await get('/smart-pricing/'+selected.decisionId);assert.equal(decision.source,'AI');
  report.ai={decisionId:decision.id,source:decision.source,explanation:decision.explanation};
  await page.goto(base+'/control.html?decision='+decision.id+'#smart');await page.waitForSelector('.smart-card');
  assert.equal(await page.locator('.smart-card').count(),1);
  assert.match(await page.locator('.smart-card').innerText(),new RegExp('решение #'+decision.id));
  await page.locator('[data-smart=all]').click();await page.waitForFunction(()=>!new URLSearchParams(location.search).has('decision'));
  await page.goto(base+'/control.html#manager');await page.waitForSelector('#cc-overview');
  for(const theme of ['light','dark']){
   await page.evaluate(t=>ariaTheme.applyTheme(t),theme);
   const contrast=await page.locator('[data-manager=settings]').first().evaluate(el=>{
    const s=getComputedStyle(el),rgb=v=>(v.match(/[\d.]+/g)||[]).slice(0,3).map(Number);
    const luminance=c=>c.map(v=>{v/=255;return v<=.04045?v/12.92:((v+.055)/1.055)**2.4;}).reduce((a,v,i)=>a+v*[.2126,.7152,.0722][i],0);
    const a=luminance(rgb(s.color)),b=luminance(rgb(s.backgroundColor));return (Math.max(a,b)+.05)/(Math.min(a,b)+.05);
   });assert.ok(contrast>=4.5,theme+' control text contrast '+contrast);
   report.themes.push({theme,controlTextContrast:contrast});
   await page.screenshot({path:'target/e2e/center-final-'+theme+'.png'});
  }
  await page.setViewportSize({width:390,height:844});assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));await page.screenshot({path:'target/e2e/center-final-mobile.png'});
  await page.goto(base+'/index.html');await page.waitForSelector('.product-card');await page.locator('.aria-shell-burger').click();await page.locator('#theme-toggle-btn').click();await page.screenshot({path:'target/e2e/catalog-final-mobile.png'});
  assert.deepEqual(report.errors,[]);report.passed=true;console.log(JSON.stringify(report,null,2));
 }finally{fs.writeFileSync('target/e2e/control-center-report.json',JSON.stringify(report,null,2));await browser.close();}
})().catch(e=>{console.error(e);process.exit(1);});
