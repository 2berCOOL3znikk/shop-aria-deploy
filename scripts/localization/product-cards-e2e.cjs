'use strict';
// Read-only checks against the separate educational storefront. No model, price or stock writes.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||'playwright');
const base='http://127.0.0.1:8087',out='docs/localization/evidence';
(async()=>{const browser=await chromium.launch({channel:'msedge',headless:true}),report={pages:[],errors:[],writesPerformed:false,aiRequested:false};try{
 const get=async path=>{const r=await fetch(base+path);assert.equal(r.status,200);return r.json();};
 assert.equal((await get('/api/auth/capabilities')).publicStorefront,true);
 const products=await get('/api/products/store'),cases=[
  {kind:'food',product:products.find(v=>v.sku==='ARIA-DEMO-004')},
  {kind:'household',product:products.find(v=>v.sku==='ARIA-DEMO-010')},
  {kind:'textile',product:products.find(v=>v.name==='Учебное полотенце')}
 ];assert.ok(cases.every(c=>c.product),'Run the isolated public fixture setup first');
 for(const lang of ['ru','en']){
  const context=await browser.newContext({viewport:{width:1440,height:1000},reducedMotion:'reduce'});await context.addInitScript(l=>{localStorage.setItem('aria_lang',l);localStorage.setItem('aria_theme','light');},lang);const page=await context.newPage();page.on('pageerror',e=>report.errors.push(e.message));
  for(const width of [1440,390]){await page.setViewportSize({width,height:1000});for(const item of cases){
   const data=await get('/api/products/'+item.product.id);await page.goto(base+'/product.html?id='+item.product.id);await page.waitForSelector('#product-sources');assert.equal(await page.locator('#product h1').innerText(),data.product.name);
   const d=data.details,expected=[d?.brand,d?.color,d?.size,Number(d?.weightKg)>0?d.weightKg:null].filter(v=>v!=null&&String(v).trim()!=='');
   assert.equal(await page.locator('#product-attributes tbody tr').count(),expected.length);
   if(item.kind==='textile'){assert.equal(await page.locator('#product-attributes').count(),0);assert.equal(await page.locator('#product').getByText(lang==='en'?'Expiry date:':'Срок годности:',{exact:true}).count(),0);}
   else {assert.equal(await page.locator('#product-attributes th').getByText(lang==='en'?'Pack size':'Упаковка',{exact:true}).count(),1);assert.match(await page.locator('#product-attributes').innerText(),lang==='en'?/Provided by the store/:/Указаны магазином/);assert.equal(await page.locator('#product-sources .storefront-facts>div').count(),data.product.provenance.facts.length);if(lang==='ru')assert.deepEqual(await page.locator('#product-sources .storefront-facts dt').allTextContents(),data.product.provenance.facts.map(f=>f.label));if(lang==='en')assert.ok((await page.locator('#product-sources .storefront-facts dt').allTextContents()).every(s=>!/[А-Яа-яЁё]/.test(s)));}
   assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));
   const before=JSON.stringify(data);assert.equal(JSON.stringify(await get('/api/products/'+item.product.id)),before,'Presentation must not change product facts');
   if(width===390||item.kind==='food')await page.screenshot({path:path.join(out,'card-'+item.kind+'-'+lang+'-'+width+'.png'),fullPage:true});
   report.pages.push({language:lang,width,category:item.kind,id:item.product.id,storeAttributes:expected.length,referenceFacts:data.product.provenance?.facts?.length||0});
  }}
  if(lang==='en'){
   await page.setViewportSize({width:1440,height:1000});await page.goto(base+'/product.html?id='+cases[0].product.id);await page.waitForSelector('#product-attributes');await page.locator('#theme-toggle-btn').click();assert.equal(await page.locator('html').getAttribute('data-theme'),'dark');await page.setViewportSize({width:390,height:1000});assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));await page.screenshot({path:path.join(out,'card-food-en-390-dark.png'),fullPage:true});report.pages.push({language:lang,width:390,theme:'dark',category:'food',id:cases[0].product.id});
  }
  await context.close();
 }assert.deepEqual(report.errors,[]);report.passed=true;
}catch(error){report.failure=error.stack;throw error;}finally{fs.writeFileSync(path.join(out,'product-cards.json'),JSON.stringify(report,null,2));await browser.close();}})().catch(e=>{console.error(e.stack);process.exitCode=1;});
