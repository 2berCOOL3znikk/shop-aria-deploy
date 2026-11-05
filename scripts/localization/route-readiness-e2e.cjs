'use strict';
// Delay an actual authenticated GET in the browser; no fabricated metrics or business writes.
const fs=require('node:fs'),assert=require('node:assert/strict'),{chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||'playwright');
const cfg=JSON.parse(fs.readFileSync('data/woocommerce/aria-config.json','utf8').replace(/^\uFEFF/,'')),base='http://127.0.0.1:8085',baseline=process.argv.includes('--baseline');
const proof={environment:'isolated woo-e2e',baseline,productionChanged:false,businessWrites:0,aiRequested:false,checks:[],errors:[]};
const deferred=()=>{let resolve;return {promise:new Promise(done=>resolve=done),resolve:()=>resolve()};};
(async()=>{const browser=await chromium.launch({channel:'msedge',headless:true});try{
 for(const language of baseline?['en']:['ru','en']){
  const context=await browser.newContext({viewport:{width:390,height:1000}});await context.addInitScript(language=>localStorage.setItem('aria_lang',language),language);const page=await context.newPage();page.on('pageerror',error=>proof.errors.push(error.message));
  await page.goto(base+'/login.html');await page.locator('#login-email').fill(cfg.email);await page.locator('#login-password').fill(cfg.password);await page.locator('#login-form [type=submit]').click();await page.waitForURL('**/control.html');await page.locator('.ws-metrics').waitFor();
  let release=deferred(),entered=deferred();
  await page.route('**/api/admin/control/workspace/overview',async route=>{const actual=await route.fetch();assert.equal(actual.status(),200);entered.resolve();await release.promise;await route.fulfill({response:actual});});
  try{
   await page.goto(base+'/control.html#workspace');await entered.promise;await page.waitForTimeout(120);
   const whileHeld=await page.locator('#control-content').getAttribute('aria-busy');
   if(baseline){assert.equal(whileHeld,'false');proof.prematurelyReportedReady=true;proof.baselineReproduced=true;await page.screenshot({path:'docs/platform/evidence/dashboard-loading-before-en-390.png'});}
   else assert.equal(whileHeld,'true','Workspace must remain busy while its real GET is held');
   release.resolve();await page.locator('.ws-metrics').waitFor();await page.waitForFunction(()=>document.getElementById('control-content').getAttribute('aria-busy')==='false');
   if(!baseline){
    await page.evaluate(()=>location.hash='stores');await page.locator('#stores-search').waitFor();await page.waitForFunction(()=>document.getElementById('control-content').getAttribute('aria-busy')==='false');
    release=deferred();entered=deferred();await page.evaluate(()=>location.hash='workspace');await entered.promise;
    assert.equal(await page.locator('#control-content').getAttribute('aria-busy'),'true');
    // The mobile navigation is closed by route changes; hash navigation follows the same router.
    await page.evaluate(()=>location.hash='stores');await page.locator('#stores-search').waitFor();await page.waitForFunction(()=>document.getElementById('control-content').getAttribute('aria-busy')==='false');
    release.resolve();await page.waitForTimeout(180);assert.equal(await page.locator('#stores-search').count(),1);assert.equal(await page.locator('.ws-metrics').count(),0);assert.equal(await page.locator('#control-content').getAttribute('aria-busy'),'false');
    await page.unroute('**/api/admin/control/workspace/overview');await page.evaluate(()=>location.hash='workspace');await page.waitForFunction(()=>document.getElementById('control-content').getAttribute('aria-busy')==='false');await page.locator('.ws-metrics').waitFor();
    await page.screenshot({path:'docs/platform/evidence/dashboard-ready-'+language+'-390.png'});proof.checks.push({language,busyUntilActualResponse:true,rapidNavigationKeepsCurrentSection:true,actualDashboardRendered:true});
   }
  }finally{release.resolve();await context.close();}
 }
 assert.deepEqual(proof.errors,[]);if(!baseline)proof.passed=true;
}catch(error){proof.failure=error.stack;throw error;}finally{proof.completedAt=new Date().toISOString();fs.writeFileSync('docs/platform/evidence/route-readiness-'+(baseline?'before':'after')+'.json',JSON.stringify(proof,null,2));await browser.close();}})().catch(error=>{console.error(error.stack);process.exitCode=1;});
