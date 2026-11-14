'use strict';
// Isolated browser contract checks. No remote WooCommerce, credentials, database or business E2E.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||path.join(process.env.USERPROFILE,'.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright'));
process.chdir(path.resolve(__dirname,'../..'));
(async()=>{const browser=await chromium.launch({channel:'msedge',headless:true});try{
 const page=await browser.newPage({viewport:{width:390,height:844}});let checks=0;
 for(const lang of ['ru','en']){
  await page.setContent('<main><section data-operations="orders"><header class="ox-heading"><div><h2>Native</h2><p>Description</p></div></header><div data-private-retail><input id="control-search"><select id="control-filter"></select></div></section><div data-order-pagination></div><dialog id="details"></dialog></main>');
  await page.evaluate(lang=>{window.ariaCopy={language:lang,locale:()=>lang==='en'?'en-KZ':'ru-KZ',original:value=>value};window.contractRequests=[];window.xssExecuted=false;window.contractStores=[{id:11,platform:'WOOCOMMERCE',name:'First store',testConnection:true},{id:12,platform:'WOOCOMMERCE',name:'USD store'},{id:13,platform:'WOOCOMMERCE',name:'Unavailable store'}];},lang);
  await page.addScriptTag({path:'src/main/resources/static/js/operations-experience.js'});await page.addScriptTag({path:'src/main/resources/static/js/connected-orders.js'});
  await page.evaluate(async()=>{
   const ctx={request:async route=>{contractRequests.push(route);if(route==='/api/auth/capabilities')return {publicStorefront:false};if(route==='/stores')return {stores:contractStores};if(route.startsWith('/stores/13/'))throw Error('Unavailable test source');if(route.endsWith('/catalog'))return [{externalId:7,name:'<img src=x onerror="xssExecuted=true">'}];return [{id:1,externalId:'101',status:'completed',currency:route.includes('/12/')?'USD':'KZT',total:'1500.00',refundedTotal:'200.00',createdAtUtc:'2026-11-01T10:00:00',observedAt:'2026-11-14T08:00:00',lines:[{productId:'7',quantity:2,total:'1500.00'}]}];},show:(title,html)=>{document.querySelector('#details').innerHTML=html;document.querySelector('#details').showModal();}};
   await ariaConnectedOrders.mount(ctx);
  });
  assert.equal(await page.locator('[data-external-order]').count(),2);assert.equal(await page.locator('[data-private-retail]').isVisible(),false);assert.equal(await page.locator('[data-order-pagination]').isVisible(),false);
  assert.equal(await page.locator('[role=alert]').count(),1);assert.equal(await page.locator('[data-external-order] img').count(),0);assert.equal(await page.evaluate(()=>xssExecuted),false);
  const cardText=await page.locator('[data-external-order="12-1"]').innerText();assert.match(cardText,/\$/);assert.ok(!cardText.includes('₸'));
  assert.ok((await page.locator('[data-external-order="11-1"]').innerText()).includes(lang==='en'?'Completed in store':'Завершён в магазине'));
  await page.locator('#external-order-search').fill('USD');assert.equal(await page.locator('[data-external-order]').count(),1);await page.locator('#external-order-search').fill('');await page.locator('#external-order-source').selectOption('11');assert.equal(await page.locator('[data-external-order]').count(),1);
  await page.locator('[data-external-order-open]').click();assert.ok((await page.locator('#details').innerText()).includes(lang==='en'?'Refunded':'Возвращено'));assert.ok((await page.locator('#details').innerText()).includes(lang==='en'?'Snapshot received':'Данные получены'));assert.equal(await page.locator('#details img').count(),0);await page.evaluate(()=>document.querySelector('#details').close());
  await page.evaluate(async()=>{contractStores=[];await ariaConnectedOrders.mount({request:async route=>route==='/stores'?{stores:[]}:{publicStorefront:false}});});
  assert.equal(await page.locator('.ox-connected-orders').last().locator('a[href="#stores"]').count(),1);assert.equal(await page.locator('a[href*="register"]').count(),0);
  checks+=9;
 }
 console.log(JSON.stringify({passed:true,checks,contractOnly:true,remoteWooCommerce:false}));
 }finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1;});
