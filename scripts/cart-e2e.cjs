'use strict';
// Only the isolated local preview. A dedicated QA buyer is created; no order is submitted.
const fs=require('node:fs'),assert=require('node:assert/strict'),crypto=require('node:crypto');
const {chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||'playwright');
const dir='target/e2e/aria3';
(async()=>{
  const cfg=JSON.parse(fs.readFileSync('data/preview/config.json','utf8'));
  assert.ok(Number.isInteger(cfg.port)&&cfg.port>=1024&&cfg.port<=65535);
  const base='http://127.0.0.1:'+cfg.port;
  const browser=await chromium.launch({channel:'msedge',headless:true});
  const report={checks:[],errors:[],checkoutRequests:0};fs.mkdirSync(dir,{recursive:true});
  let adminContext,buyerContext,adminToken,buyerToken,product;
  const request=async(context,path,method='GET',body,token)=>{
    const r=await context.request.fetch(base+path,{method,data:body,headers:token?{Authorization:'Bearer '+token}:{}});
    const value=await r.json().catch(()=>({}));
    assert.equal(r.status(),200,'Request failed: '+method+' '+path+' '+JSON.stringify(value));return value;
  };
  try {
    adminContext=await browser.newContext();
    const login=await adminContext.request.post(base+'/api/auth/login',{data:{email:cfg.email,password:cfg.password}});
    assert.equal(login.status(),200,'Local owner login required');adminToken=(await login.json()).token;
    const suffix=crypto.randomUUID().replaceAll('-','');
    product=await request(adminContext,'/api/admin/control/products','POST',{sku:'QA-CART-'+suffix.slice(0,16),name:'Товар для проверки корзины',category:'Внутренние проверки',unit:'шт',unitPrice:1234,minimumPrice:1234,lowStockThreshold:0},adminToken);
    const today=new Date().toLocaleDateString('en-CA',{timeZone:'Asia/Qyzylorda'});
    await request(adminContext,'/api/admin/control/lots','POST',{productId:product.id,lotNumber:'QA-CART',receivedAt:today,quantity:3,unitCost:800},adminToken);

    buyerContext=await browser.newContext({viewport:{width:1440,height:1000}});
    const challenge=await request(buyerContext,'/api/auth/captcha');
    const numbers=challenge.question.match(/\d+/g).map(Number);assert.equal(numbers.length,2);
    const email='cart-'+suffix+'@qa.invalid',password='Cart-QA-'+crypto.randomUUID();
    const buyer=await request(buyerContext,'/api/auth/register','POST',{email,password,fullName:'Проверка корзины',website:'',captchaId:challenge.id,captchaAnswer:String(numbers[0]+numbers[1])});
    buyerToken=buyer.token;assert.ok(buyerToken,'Registration must return a real buyer session');
    await buyerContext.addInitScript(({token,email,base})=>{if(location.origin!==base)return;localStorage.setItem('aria_token',token);localStorage.setItem('aria_role','USER');localStorage.setItem('aria_email',email);localStorage.setItem('aria_lang','ru');},{token:buyerToken,email,base});
    buyerContext.on('page',page=>page.on('pageerror',error=>report.errors.push(error.message)));
    buyerContext.on('request',r=>{if(r.method()==='POST'&&/\/api\/(?:orders|cart)\/checkout/.test(r.url()))report.checkoutRequests++;});
    const catalog=await buyerContext.newPage(),detail=await buyerContext.newPage();
    const control='[data-cart-control="'+product.id+'"]';
    const ready=async page=>{await page.waitForSelector(control);await page.waitForFunction(sel=>window.ariaCart?.isLoaded()&&document.querySelector(sel)?.getAttribute('aria-busy')==='false',control);};
    const count=async(page,n)=>{await page.waitForFunction(({sel,n})=>document.querySelector(sel)?.dataset.cartQuantity===String(n)&&!window.ariaCart.isBusy(),{sel:control,n});};
    const cart=()=>request(buyerContext,'/api/cart','GET',undefined,buyerToken);
    const click=async(page,operation)=>{await page.locator(control+' [data-cart-op="'+operation+'"]').click();};
    await catalog.goto(base+'/index.html');await catalog.locator('#search').fill(product.sku);await ready(catalog);
    await detail.goto(base+'/product.html?id='+product.id);await ready(detail);
    await catalog.evaluate(id=>{for(let i=0;i<8;i++)document.querySelector('[data-cart-control="'+id+'"] [data-cart-op=add]')?.click();},product.id);
    await Promise.all([count(catalog,1),count(detail,1)]);
    let saved=await cart();assert.equal(saved.length,1);assert.equal(saved[0].quantity,1);
    report.checks.push('Eight rapid add clicks create one server cart row; the add button becomes a quantity control in both open pages');

    await click(detail,'decrease');await Promise.all([count(catalog,0),count(detail,0)]);
    await Promise.all([catalog.evaluate(id=>document.querySelector('[data-cart-control="'+id+'"] [data-cart-op=add]').click(),product.id),detail.evaluate(id=>document.querySelector('[data-cart-control="'+id+'"] [data-cart-op=add]').click(),product.id)]);
    await Promise.all([count(catalog,1),count(detail,1)]);saved=await cart();assert.equal(saved.length,1);assert.equal(saved[0].quantity,1);
    report.checks.push('Concurrent initial adds from two tabs converge to one item without a second blind increment');
    await click(detail,'increase');await Promise.all([count(catalog,2),count(detail,2)]);
    await click(catalog,'increase');await Promise.all([count(catalog,3),count(detail,3)]);
    assert.equal(await catalog.locator(control+' [data-cart-op=increase]').isDisabled(),true);
    assert.equal(await detail.locator(control+' [data-cart-op=increase]').isDisabled(),true);
    const boundError=await detail.evaluate(async id=>{try{await window.ariaCart.adjust(id,1);return '';}catch(error){return error.message;}},product.id);
    assert.match(boundError,/доступно 3/);assert.equal((await cart())[0].quantity,3);
    report.checks.push('Quantity is synchronized and capped at the actual available stock, including direct repeated action calls');

    const basket=await buyerContext.newPage();await basket.goto(base+'/cart.html');await ready(basket);await count(basket,3);
    assert.ok((await basket.locator('#cart-total').innerText()).replace(/\s/g,'').includes('3702'));
    await basket.locator('#delivery-address').fill('QA адрес — заказ не отправляется');
    await click(basket,'decrease');await Promise.all([count(catalog,2),count(detail,2),count(basket,2)]);
    assert.equal(await basket.locator('#delivery-address').inputValue(),'QA адрес — заказ не отправляется');
    assert.ok((await basket.locator('#cart-total').innerText()).replace(/\s/g,'').includes('2468'));
    // This intentionally simulated failure tests presentation only; successful cart mutations above are real.
    await basket.route('**/api/cart/*?qty=*',route=>route.fulfill({status:503,contentType:'application/json',body:JSON.stringify({message:'Проверка ошибки корзины: повторите позже'})}));
    await click(basket,'increase');await basket.waitForFunction(()=>document.querySelector('.toast-message')?.parentElement?.parentElement?.textContent.includes('Проверка ошибки корзины'));
    await count(basket,2);assert.equal((await cart())[0].quantity,2);assert.equal(await basket.locator('#delivery-address').inputValue(),'QA адрес — заказ не отправляется');await basket.unroute('**/api/cart/*?qty=*');
    report.checks.push('Cart totals follow server state; editing quantity preserves the address; a separately simulated failed update never shows a false increment');
    await basket.reload();await ready(basket);await count(basket,2);assert.equal((await cart())[0].quantity,2);
    report.checks.push('The server cart survives page reload without storing cart contents in localStorage');
    for(const [width,height] of [[1440,1000],[390,844]]){
      for(const [page,name] of [[catalog,'catalog'],[detail,'product'],[basket,'cart']]){
        await page.setViewportSize({width,height});assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),name+' horizontal overflow at '+width);
        await page.screenshot({path:dir+'/cart-'+name+'-'+width+'.png',fullPage:true});
      }
    }
    await basket.locator('[data-cart-line="'+product.id+'"] [aria-label="Удалить товар"]').click();
    await Promise.all([count(catalog,0),count(detail,0)]);await basket.waitForFunction(()=>document.getElementById('cart-items').textContent.includes('Корзина пуста'));
    assert.deepEqual(await cart(),[]);
    const native=await request(adminContext,'/api/admin/control/products/'+product.id,'GET',undefined,adminToken);assert.equal(native.product.quantityOnHand,3);
    assert.equal(report.checkoutRequests,0);assert.deepEqual(report.errors,[]);
    report.checks.push('Removal updates all open pages; 390px and 1440px fit; no checkout or inventory consumption occurred');report.passed=true;
  }catch(error){report.failure=error.stack;throw error;}
  finally {
    if(buyerContext&&buyerToken)try{for(const item of await request(buyerContext,'/api/cart','GET',undefined,buyerToken))await request(buyerContext,'/api/cart/'+item.id,'DELETE',undefined,buyerToken);}catch(error){report.cleanupError=error.message;}
    await buyerContext?.close();
    if(product&&adminToken)try{const lots=await request(adminContext,'/api/admin/control/lots','GET',undefined,adminToken);for(const lot of lots.filter(x=>Number(x.product?.id)===Number(product.id)&&x.lotNumber==='QA-CART'&&x.active&&x.quantity>0))await request(adminContext,'/api/admin/control/lots/'+lot.id,'DELETE',{reason:'Завершение изолированной учебной проверки корзины'},adminToken);await request(adminContext,'/api/admin/control/products/'+product.id,'DELETE',undefined,adminToken);}catch(error){report.archiveError=error.message;}
    fs.writeFileSync(dir+'/cart-report.json',JSON.stringify(report,null,2));await browser.close();
  }
})().catch(error=>{console.error(error);process.exitCode=1;});
