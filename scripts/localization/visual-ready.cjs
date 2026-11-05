'use strict';
// Read-only screenshot checks. Wait for real cards and visible images, not a loading screen.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||'playwright');
const directory='docs/localization/evidence',base='http://127.0.0.1:8087';
(async()=>{const browser=await chromium.launch({channel:'msedge',headless:true}),report={checks:[],errors:[],writesPerformed:false,aiRequested:false};try{
 const response=await fetch(base+'/api/auth/capabilities');assert.equal(response.status,200);assert.equal((await response.json()).publicStorefront,true);
 for(const lang of ['ru','en']){
  const ctx=await browser.newContext({viewport:{width:1440,height:1000},reducedMotion:'reduce'});await ctx.addInitScript(l=>{localStorage.setItem('aria_lang',l);localStorage.setItem('aria_theme','light');},lang);const p=await ctx.newPage();p.on('pageerror',e=>report.errors.push(e.message));
  for(const width of [1440,390]){
   await p.setViewportSize({width,height:1000});await p.goto(base+'/index.html');await p.waitForSelector('#products [data-product-card]');await p.waitForFunction(()=>document.getElementById('products').getAttribute('aria-busy')==='false');
   await p.locator('#products').evaluate(async root=>{await Promise.all([...root.querySelectorAll('img')].map(img=>img.complete?null:new Promise(resolve=>{img.addEventListener('load',resolve,{once:true});img.addEventListener('error',resolve,{once:true});setTimeout(resolve,5000);})));});
   const state=await p.locator('#products').evaluate(root=>({cards:root.querySelectorAll('[data-product-card]').length,images:[...root.querySelectorAll('img')].filter(img=>img.complete&&img.naturalWidth>0).length,loading:!!root.querySelector('.catalog-loading')}));
   assert.ok(state.cards>0);assert.ok(state.images>0);assert.equal(state.loading,false);assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));
   await p.screenshot({path:path.join(directory,'catalogue-'+lang+'-'+width+'.png'),fullPage:true});report.checks.push({language:lang,width,...state});
  }await ctx.close();
 }assert.deepEqual(report.errors,[]);report.passed=true;
}catch(error){report.failure=error.stack;throw error;}finally{fs.writeFileSync(path.join(directory,'visual-ready.json'),JSON.stringify(report,null,2));await browser.close();}})().catch(e=>{console.error(e.stack);process.exitCode=1;});
