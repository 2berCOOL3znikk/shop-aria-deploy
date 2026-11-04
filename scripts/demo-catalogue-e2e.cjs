'use strict';
const fs=require('node:fs'),assert=require('node:assert/strict'),crypto=require('node:crypto');
const {chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||'playwright');
const output='target/e2e/storefront';

async function capture(page,path,fullPage=true){
  await page.locator('img').evaluateAll(async images=>{images.forEach(img=>img.loading='eager');await Promise.all(images.map(img=>img.decode().catch(()=>{})));});
  await page.waitForFunction(()=>[...document.images].every(img=>img.hidden||(img.complete&&img.naturalWidth>1)));
  await page.screenshot({path,fullPage});
}

async function main(){
  const cfg=JSON.parse(fs.readFileSync('data/preview/config.json','utf8').replace(/^\uFEFF/,''));
  assert.ok(Number.isInteger(cfg.port)&&cfg.port>=1024&&cfg.port<=65535);
  const base='http://127.0.0.1:'+cfg.port,report={checks:[],errors:[],externalAiCalled:false};
  const verifyPersistence=process.argv.includes('--verify-persistence');
  fs.mkdirSync(output,{recursive:true});
  const browser=await chromium.launch({channel:'msedge',headless:true});let owner,token,product,originalPercent;
  const call=async(context,path,method='GET',data,auth)=>{
    const r=await context.request.fetch(base+path,{method,data,headers:auth?{Authorization:'Bearer '+auth}:{}});
    const result=await r.json().catch(()=>({}));assert.equal(r.status(),200,path+' '+r.status()+' '+JSON.stringify(result));return result;
  };
  try{
    owner=await browser.newContext({viewport:{width:1440,height:1000},reducedMotion:'reduce'});
    token=(await call(owner,'/api/auth/login','POST',{email:cfg.email,password:cfg.password})).token;
    await owner.addInitScript(({base,token})=>{if(location.origin===base){localStorage.setItem('aria_token',token);localStorage.setItem('aria_role','ADMIN');}},{base,token});
    const p=await owner.newPage();p.on('pageerror',e=>report.errors.push(e.message));
    if(verifyPersistence){
      const snapshot=JSON.parse(fs.readFileSync('target/storefront-persistence.json','utf8'));
      const saved=(await call(owner,'/api/products/'+snapshot.productId)).product;
      assert.equal(saved.sku,snapshot.sku);assert.equal(saved.quantityOnHand,snapshot.expectedQuantity);assert.equal(saved.provenance.demonstration,true);assert.ok(saved.provenance.facts.length);
      const orders=await call(owner,'/api/admin/control/orders','GET',undefined,token);assert.equal(orders.find(o=>o.id===snapshot.orderId).status,'SHIPPED');
      await p.goto(base+'/index.html');await p.waitForSelector('.product-card');await capture(p,output+'/verified-catalogue-1440.png');
      await p.goto(base+'/product.html?id='+saved.id);await p.waitForSelector('#product-sources');await capture(p,output+'/verified-product-1440.png');
      const products=await call(owner,'/api/products/store'),can=products.find(row=>row.sku==='ARIA-DEMO-016');assert.ok(can);await p.goto(base+'/product.html?id='+can.id);await p.waitForSelector('#product-sources');await p.locator('#product-sources summary').click();await capture(p,output+'/licensed-photo-product-1440.png');
      await p.setViewportSize({width:390,height:844});await p.goto(base+'/index.html');await p.waitForSelector('.product-card');await capture(p,output+'/verified-catalogue-390.png');
      report.checks.push('After an actual preview restart: product identity, source reference, consumed stock and SHIPPED order retained');report.productId=saved.id;report.quantity=saved.quantityOnHand;report.orderId=snapshot.orderId;report.passed=true;
      console.log('Demo catalogue persistence passed after restart');return;
    }
    const anonymous=await browser.newContext();const denied=await anonymous.request.post(base+'/api/admin/control/demo-catalogue',{data:{confirmed:true}});assert.equal(denied.status(),401);await anonymous.close();
    await p.goto(base+'/control.html#settings');await p.waitForSelector('#demo-catalogue-settings:not([hidden])');
    await p.locator('#demo-catalogue-settings button').click();await p.locator('[name=confirmed]').check();
    await capture(p,output+'/catalogue-import-confirmation.png');
    const imported=p.waitForResponse(r=>r.url()===base+'/api/admin/control/demo-catalogue'&&r.request().method()==='POST');
    await p.locator('#control-dialog [type=submit]').click();const importedReply=await imported;assert.equal(importedReply.status(),200);const importResult=await importedReply.json();
    await p.waitForFunction(()=>!document.getElementById('control-dialog').open);
    assert.equal(importResult.created.length+importResult.preserved.length,16);
    const publicProducts=await call(owner,'/api/products/store');const catalogue=publicProducts.filter(p=>/^ARIA-DEMO-\d{3}$/.test(p.sku));assert.equal(catalogue.length,16);
    assert.ok(catalogue.every(p=>p.provenance.demonstration===true&&!p.name.match(/\d{13}/)&&p.unitCost===undefined&&p.minimumPrice===undefined));
    assert.equal(catalogue.filter(p=>p.provenance.verification==='PRIMARY_SOURCE').length,15);
    assert.equal(catalogue.filter(p=>p.provenance.photo).length,1);
    product=catalogue.find(p=>p.sku==='ARIA-DEMO-001');assert.ok(product.quantityOnHand>0);originalPercent=product.discountPercent;
    const stockBefore=product.quantityOnHand;
    await p.goto(base+'/index.html');await p.waitForSelector('#storefront-demonstration:not([hidden])');await p.waitForSelector('[data-product-card="'+product.id+'"]');
    await capture(p,output+'/verified-catalogue-1440.png');
    await p.goto(base+'/product.html?id='+product.id);await p.waitForSelector('#product-sources');assert.match(await p.locator('#product-sources').innerText(),/100 г/);assert.equal(await p.locator('#product-sources a').first().getAttribute('href').then(u=>new URL(u).hostname),'www.rakhat.kz');
    await capture(p,output+'/verified-product-1440.png');
    const can=catalogue.find(p=>p.sku==='ARIA-DEMO-016');await p.goto(base+'/product.html?id='+can.id);await p.waitForSelector('#product-sources');
    await p.waitForFunction(()=>document.querySelector('.product-main-image')?.complete&&document.querySelector('.product-main-image')?.naturalWidth>100);
    assert.match(await p.locator('#product-image-credit').innerText(),/AhmedMX in.*CC0 1.0/s);await p.locator('#product-sources summary').click();assert.equal(await p.locator('#product-sources').innerText().then(t=>t.includes('415 г')),true); // Explicit limitation: not the 24×415g case.
    const canData=(await call(owner,'/api/products/'+can.id)).product;assert.equal(canData.size,null);
    await capture(p,output+'/licensed-photo-product-1440.png');
    report.checks.push('Owner UI imports 16 attributed demo products; anonymous import denied; provenance, uncertain facts and CC0 photo shown');

    await call(owner,'/api/admin/control/pricing/'+product.id,'PUT',{percent:10,reason:'Учебная сквозная проверка цены, витрины и заказа'},token);
    const discounted=(await call(owner,'/api/products/'+product.id)).product;assert.equal(discounted.discountPercent,10);assert.equal(Number(discounted.displayPrice),Math.round(Number(product.unitPrice)*90)/100);
    const buyer=await browser.newContext({viewport:{width:390,height:844},reducedMotion:'reduce'});buyer.on('page',page=>page.on('pageerror',e=>report.errors.push(e.message)));
    const challenge=await call(buyer,'/api/auth/captcha'),numbers=challenge.question.match(/\d+/g).map(Number),suffix=crypto.randomUUID();
    const registered=await call(buyer,'/api/auth/register','POST',{email:'storefront-'+suffix+'@qa.invalid',password:'Store-QA-'+suffix,fullName:'Учебный покупатель',website:'',captchaId:challenge.id,captchaAnswer:String(numbers[0]+numbers[1])});
    assert.equal((await buyer.request.post(base+'/api/admin/control/demo-catalogue',{headers:{Authorization:'Bearer '+registered.token},data:{confirmed:true}})).status(),403);
    await buyer.addInitScript(({base,token})=>{if(location.origin===base){localStorage.setItem('aria_token',token);localStorage.setItem('aria_role','USER');}},{base,token:registered.token});
    const shop=await buyer.newPage();await shop.goto(base+'/product.html?id='+product.id);await shop.waitForFunction(()=>window.ariaCart?.isLoaded());
    await shop.locator('[data-cart-control="'+product.id+'"] [data-cart-op=add]').click();await shop.waitForFunction(id=>window.ariaCart.quantity(id)===1&&!window.ariaCart.isBusy(),product.id);
    await shop.goto(base+'/cart.html');await shop.waitForSelector('#delivery-address');await shop.locator('#delivery-address').fill('Учебный адрес ARIA — реальной доставки нет');
    const checkout=shop.waitForResponse(r=>r.url()===base+'/api/orders/checkout'&&r.request().method()==='POST');await shop.locator('#cart-total [onclick="checkout()"]').click();const orderResponse=await checkout;assert.equal(orderResponse.status(),200);const order=await orderResponse.json();
    assert.equal(Number(order.totalAmount),Number(discounted.displayPrice));assert.equal(order.items[0].quantity,1);assert.equal(order.status,'PENDING');
    assert.equal((await call(owner,'/api/products/'+product.id)).product.quantityOnHand,stockBefore-1);
    // Complete the owner action through the real order UI.
    await p.goto(base+'/control.html#orders');await p.waitForSelector('[data-action="order-state"][data-id="'+order.id+'"][data-state="SHIPPED"]');await p.locator('[data-action="order-state"][data-id="'+order.id+'"][data-state="SHIPPED"]').click();
    await p.locator('#control-dialog [name=reason]').fill('Учебная проверка: физической доставки и оплаты нет');await p.locator('#control-dialog [type=submit]').click();await p.waitForFunction(()=>!document.getElementById('control-dialog').open);
    const ownOrders=await call(buyer,'/api/orders','GET',undefined,registered.token);assert.equal(ownOrders.find(o=>o.id===order.id).status,'SHIPPED');
    const repeat=await call(owner,'/api/admin/control/demo-catalogue','POST',{confirmed:true},token);assert.equal(repeat.created.length,0);assert.equal(repeat.preserved.length,16);assert.equal((await call(owner,'/api/products/'+product.id)).product.quantityOnHand,stockBefore-1);
    await shop.goto(base+'/index.html');await shop.waitForSelector('[data-product-card="'+product.id+'"]');await capture(shop,output+'/verified-catalogue-390.png');
    report.checks.push('Native price update → mobile product/cart → real demo order → FEFO stock consumption → owner UI shipment; repeat import does not replenish stock');
    fs.writeFileSync('target/storefront-persistence.json',JSON.stringify({productId:product.id,sku:product.sku,expectedQuantity:stockBefore-1,orderId:order.id,ownerTokenNotStored:true},null,2));
    report.createdOnImport=importResult.created.length;report.preservedOnImport=importResult.preserved.length;report.orderId=order.id;report.stockBefore=stockBefore;report.stockAfter=stockBefore-1;
    assert.deepEqual(report.errors,[]);report.passed=true;
  }catch(error){report.failure=error.message;throw error;}
  finally{
    if(owner&&token&&product&&originalPercent!==undefined)try{await call(owner,'/api/admin/control/pricing/'+product.id,'PUT',{percent:originalPercent,reason:'Завершение учебной проверки: восстановлена исходная скидка'},token);}catch(error){report.restoreError=error.message;report.passed=false;process.exitCode=1;}
    fs.mkdirSync(output,{recursive:true});fs.writeFileSync(output+(verifyPersistence?'/demo-catalogue-persistence.json':'/demo-catalogue-report.json'),JSON.stringify(report,null,2));await browser.close();
  }
  console.log('Demo catalogue and native checkout E2E passed');
}
main().catch(error=>{console.error(error.message);process.exitCode=1;});
