'use strict';
// Isolated renderer contract. Synthetic replies only; no charity verification or transfer is performed.
const path=require('node:path'),assert=require('node:assert/strict');
const {chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||path.join(process.env.USERPROFILE,'.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright'));
(async()=>{const browser=await chromium.launch({channel:'msedge',headless:true});try{let checks=0;
 for(const status of ['NOT_SUBMITTED','PENDING_VERIFICATION','VERIFIED','REJECTED']){
  const ctx=await browser.newContext({viewport:{width:390,height:844},serviceWorkers:'block'});await ctx.addInitScript(()=>{localStorage.setItem('aria_token','contract-only-not-a-real-token');localStorage.setItem('aria_role','ORG');});
  await ctx.route('**/api/**',async route=>{const p=new URL(route.request().url()).pathname;const body=p==='/api/auth/capabilities'?{publicStorefront:false,buyerRegistration:false}:p==='/api/organizations/verification'?{status}:p==='/api/social/offers'?[{id:1,productName:'Contract-only offer',quantityAvailable:1,validUntil:'2030-01-01',status:'OPEN'}]:p==='/api/social/my-claims'?[{id:2,productName:'Contract-only history',status:'TRANSFERRED',claimedQuantity:1}]:p==='/api/products/store-info'?{name:'Contract-only company',demonstration:true}:{};await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(body)});});
  await ctx.route(/\/js\/(nav|pwa|assistant)\.js$/,r=>r.fulfill({status:200,contentType:'application/javascript',body:''}));const page=await ctx.newPage();await page.goto('http://127.0.0.1:8091/social.html');await page.locator('.claim-row').waitFor();assert.equal(await page.locator('#claims-section').isVisible(),true);assert.equal(await page.locator('[data-claim]').count(),status==='VERIFIED'?1:0);assert.equal(await page.locator('#partner-message').isVisible(),false);checks+=3;await ctx.close();
 }
 console.log(JSON.stringify({passed:true,checks,contractOnly:true,realTransfer:false}));
 }finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1;});
