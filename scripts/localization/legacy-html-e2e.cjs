'use strict';
// Harmless DOM marker, no data extraction or outside requests. Only a new, zero-stock educational fixture.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||'playwright');
const base='http://127.0.0.1:8085',out='docs/platform/evidence',baseline=process.argv.includes('--baseline');fs.mkdirSync(out,{recursive:true});
(async()=>{const browser=await chromium.launch({channel:'msedge',headless:true}),ctx=await browser.newContext({viewport:{width:1440,height:1000}}),page=await ctx.newPage(),cfg=JSON.parse(fs.readFileSync('data/woocommerce/aria-config.json','utf8').replace(/^\uFEFF/,'')),report={baseline,productionChanged:false,harmlessMarkerOnly:true,cleanupCompleted:false};let token,product;
 const api=async(resource,method='GET',data)=>{const r=await ctx.request.fetch(base+'/api/admin/control'+resource,{method,headers:{Authorization:'Bearer '+token},...(data?{data}:{})});const body=await r.json();assert.equal(r.status(),200,resource);return body;};
 try{
  await page.goto(base+'/login.html');await page.locator('#login-email').fill(cfg.email);await page.locator('#login-password').fill(cfg.password);await page.locator('#login-form [type=submit]').click();await page.waitForURL('**/control.html');token=await page.evaluate(()=>localStorage.getItem('aria_token'));assert.equal((await api('/organization')).binding,'woo-e2e');
  const marker='<img src="/educational-missing-image" onerror="window.__ariaHtmlProbe=1">';
  product=await api('/products','POST',{name:'Учебная проверка '+marker,category:'Учебная '+marker,unitPrice:100,minimumPrice:100,unit:'pcs',lowStockThreshold:0});
  await page.goto(base+'/admin.html');await page.waitForFunction(id=>[...document.querySelectorAll('#aria-table-body tr')].some(row=>row.textContent.includes('ARIA-AUTO-'+id)),Number(product.sku.replace('ARIA-AUTO-','')));await page.waitForTimeout(150);
  report.legacyAdminMarkerExecuted=await page.evaluate(()=>window.__ariaHtmlProbe===1);report.fixtureStock=product.quantityOnHand;
  assert.equal(report.legacyAdminMarkerExecuted,baseline,baseline?'The suspected baseline issue was not reproduced':'Stored product text executed as HTML');
  if(!baseline){assert.equal(await page.locator('#aria-table-body img[src="/educational-missing-image"]').count(),0);assert.ok((await page.locator('#aria-table-body').innerText()).includes(marker));await page.goto(base+'/control.html#products');await page.waitForFunction(()=>document.getElementById('control-content')?.getAttribute('aria-busy')==='false');assert.equal(await page.evaluate(()=>window.__ariaHtmlProbe===1),false);assert.ok((await page.locator('#control-content').innerText()).includes(marker));}
  report.passed=true;
 }catch(error){report.failure=error.stack;throw error;}finally{
  if(product&&token){await api('/products/'+product.id,'PUT',{sku:product.sku,name:'Учебная карточка проверки безопасности',category:'Учебные проверки',unitPrice:100,minimumPrice:100,unit:'pcs',lowStockThreshold:0});await api('/products/'+product.id,'DELETE');report.cleanupCompleted=true;}
  fs.writeFileSync(path.join(out,baseline?'html-baseline.json':'html-fixed.json'),JSON.stringify(report,null,2));await browser.close();
 }
})().catch(e=>{console.error(e.stack);process.exitCode=1;});
