'use strict';
const fs=require('node:fs');
const assert=require('node:assert/strict');
const {chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||'playwright');

async function capture(page,path,fullPage=true){
  await page.locator('img').evaluateAll(async images=>{images.forEach(img=>img.loading='eager');await Promise.all(images.map(img=>img.decode().catch(()=>{})));});
  await page.waitForFunction(()=>[...document.images].every(img=>img.hidden||(img.complete&&img.naturalWidth>1)));
  await page.screenshot({path,fullPage});
}

async function main(){
  const cfg=JSON.parse(fs.readFileSync('data/preview/config.json','utf8').replace(/^\uFEFF/,''));
  const base='http://127.0.0.1:'+cfg.port;
  assert.equal(new URL(base).hostname,'127.0.0.1');
  const output='target/e2e/storefront';fs.mkdirSync(output,{recursive:true});
  const report={checks:[],pageErrors:[],externalAiCalled:false};
  const browser=await chromium.launch({channel:'msedge',headless:true});
  try{
    const context=await browser.newContext({viewport:{width:1440,height:1000},reducedMotion:'reduce'});
    const page=await context.newPage();page.on('pageerror',e=>report.pageErrors.push(e.message));
    const api=await context.request.get(base+'/api/products/store');assert.equal(api.status(),200);
    const products=await api.json();assert.ok(products.length>0,'Preview needs at least one sellable product');
    for(const asset of ['css/storefront.css','js/index-mvp.js','js/assistant.js','js/catalog-images.js','js/product-mvp.js']){const response=await context.request.get(base+'/'+asset);assert.equal(response.status(),200);assert.equal(await response.text(),fs.readFileSync('src/main/resources/static/'+asset,'utf8'),'Preview is stale: '+asset);}
    const privateFile=await context.request.get(base+'/js/%2e%2e/%2e%2e/.env.openrouter.local');assert.ok([400,401,403,404].includes(privateFile.status()),'Public resource traversal must be denied');
    const price=p=>Number(p.displayPrice??p.unitPrice);
    const first=products.slice().sort((a,b)=>price(a)-price(b))[0];
    const reviewReads=new Map();page.on('request',r=>{const u=new URL(r.url());if(u.pathname.startsWith('/api/reviews/product/'))reviewReads.set(u.pathname,(reviewReads.get(u.pathname)||0)+1);});
    await page.goto(base+'/index.html');await page.waitForSelector('.product-card');
    assert.equal(await page.locator('#catalog-title').textContent(),'На каждый день.');
    await page.locator('#search').fill(first.sku);
    await page.waitForFunction(id=>document.querySelectorAll('.product-card').length===1&&document.querySelector('[data-product-card="'+id+'"]'),first.id);
    assert.equal(await page.locator('.product-card-name').textContent(),first.name);
    await page.reload();await page.waitForSelector('[data-product-card="'+first.id+'"]');
    assert.equal(await page.locator('#search').inputValue(),first.sku);
    await page.locator('#catalog-reset').click();await page.waitForFunction(()=>document.getElementById('search').value==='');
    await page.locator('#price-max').fill(String(price(first)));
    await page.waitForFunction(()=>new URLSearchParams(location.search).has('max'));
    for(const id of await page.locator('[data-product-card]').evaluateAll(nodes=>nodes.map(n=>Number(n.dataset.productCard))))assert.ok(price(products.find(p=>p.id===id))<=price(first));
    await page.locator('#price-min').fill(String(price(first)+1));await page.waitForSelector('#catalog-filter-error:not([hidden])');
    assert.equal(await page.locator('.product-card').count(),0);
    await page.locator('[data-reset-catalog]').click();await page.waitForSelector('.product-card');
    await page.locator('#sort').selectOption('price-desc');
    await page.waitForFunction(()=>new URLSearchParams(location.search).get('sort')==='price-desc');
    const prices=await page.locator('[data-product-card]').evaluateAll(nodes=>nodes.map(n=>Number(n.dataset.productCard)));
    for(let i=1;i<prices.length;i++)assert.ok(price(products.find(p=>p.id===prices[i-1]))>=price(products.find(p=>p.id===prices[i])));
    await page.locator('#discount-only').check();
    await page.waitForFunction(()=>new URLSearchParams(location.search).get('sale')==='1');
    for(const id of await page.locator('[data-product-card]').evaluateAll(nodes=>nodes.map(n=>Number(n.dataset.productCard))))assert.ok(Number(products.find(p=>p.id===id).discountPercent)>0);
    await page.locator('#catalog-reset').click();await page.waitForSelector('.product-card');
    const category=products.find(p=>p.category)?.category;
    if(category){await page.getByRole('button',{name:category,exact:true}).click();await page.waitForFunction(cat=>new URLSearchParams(location.search).get('category')===cat,category);await page.reload();await page.waitForSelector('.product-card');assert.equal(await page.locator('.category-chip.active').getAttribute('data-category'),category);}
    report.checks.push('Real catalogue search, category, price range, invalid range, discount filter, sorting and reloadable URLs');
    // A full page reload legitimately reloads ratings. Within one page, filters do not refetch them.
    reviewReads.clear();await page.locator('#catalog-reset').click();await page.waitForSelector('.product-card');
    await page.locator('#search').fill(first.sku);await page.waitForFunction(()=>document.querySelectorAll('.product-card').length===1);
    await page.locator('#catalog-reset').click();await page.waitForSelector('.product-card');
    assert.ok([...reviewReads.values()].every(n=>n<=1));
    report.checks.push('Ratings are reused while filtering instead of being requested for every render');

    await page.locator('[data-open-assistant]').click();await page.waitForSelector('#aria-assistant:not([hidden])');
    assert.equal(await page.locator('#assistant-consent').isChecked(),false);
    assert.equal(await page.locator('#assistant-query').evaluate(n=>n===document.activeElement),true);
    const send=async text=>{await page.locator('#assistant-query').fill(text);const reply=page.waitForResponse(r=>r.url()===base+'/api/assistant/query'&&r.request().method()==='POST');await page.locator('#assistant-form [type=submit]').click();const response=await reply;assert.equal(response.status(),200);const body=await response.json();assert.equal(body.source,'ENGINE');await page.waitForFunction(()=>!document.querySelector('#assistant-form [type=submit]').disabled);return body;};
    const budget=Math.ceil(Math.max(...products.map(price)));
    const answer=await send('Найди товары до '+budget+' ₸');assert.ok(answer.products.length>0);
    const ids=new Set(products.map(p=>p.id));for(const p of answer.products){assert.ok(ids.has(p.id));assert.ok(price(p)<=budget);assert.equal(p.unitCost,undefined);}
    assert.match(await page.locator('.assistant-source').last().textContent(),/Подбор по каталогу/);
    await page.locator('.assistant-compare input').first().check();await page.locator('#assistant-selection button').click();
    await page.waitForFunction(()=>!document.querySelector('#assistant-form [type=submit]').disabled);
    await page.waitForSelector('.assistant-message-answer .assistant-product');
    const offTopic=await send('Напиши Python код сортировки');assert.equal(offTopic.intent,'OUT_OF_SCOPE');assert.deepEqual(offTopic.products,[]);
    const orders=await send('Где мой заказ?');assert.equal(orders.intent,'ORDERS_HELP');assert.equal(orders.links[0].href,'/profile.html');
    await page.route('**/api/assistant/query',route=>route.fulfill({status:503,contentType:'application/json',body:JSON.stringify({message:'Проверка ошибки связи: повторите запрос'})}));
    await page.locator('#assistant-query').fill('Шоколад');await page.locator('#assistant-form [type=submit]').click();await page.waitForSelector('.assistant-message-error');assert.match(await page.locator('.assistant-message-error').innerText(),/Проверка ошибки связи/);await page.unroute('**/api/assistant/query');
    await page.keyboard.press('Escape');assert.equal(await page.locator('#aria-assistant').isVisible(),false);
    report.checks.push('Floating chat: explicit unchecked external consent, real ENGINE results, comparison, topic guard, private-order link, explicit 503 handling, Escape/focus');

    for(const width of [1440,390])for(const theme of ['light','dark']){
      await page.setViewportSize({width,height:width===390?844:1000});
      await page.goto(base+'/index.html');await page.waitForSelector('.product-card');await page.evaluate(t=>ariaTheme.applyTheme(t),theme);
      assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'Catalogue overflow at '+width);
      await capture(page,output+'/after-catalog-'+theme+'-'+width+'.png');
      await page.locator('#assistant-launcher').click();
      const rect=await page.locator('#aria-assistant').boundingBox();assert.ok(rect.x>=0&&rect.x+rect.width<=width+1&&rect.y>=0);
      await capture(page,output+'/assistant-'+theme+'-'+width+'.png');
      await page.locator('#assistant-close').click();
      await page.goto(base+'/product.html?id='+first.id);await page.waitForSelector('#product h1');
      assert.equal(await page.locator('#product h1').textContent(),first.name);
      assert.equal(await page.locator('#assistant-launcher').count(),1);
      assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'Product overflow at '+width);
      await capture(page,output+'/product-'+theme+'-'+width+'.png');
      await page.goto(base+'/about.html');assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));
    }
    report.checks.push('Catalogue, product and chat fit 390/1440 px in light/dark themes; source product title preserved');
    // An optional account read failure must not hide catalogue products.
    const login=await context.request.post(base+'/api/auth/login',{data:{email:cfg.email,password:cfg.password}});assert.equal(login.status(),200);const {token}=await login.json();
    await context.addInitScript(({base,token})=>{if(location.origin===base){localStorage.setItem('aria_token',token);localStorage.setItem('aria_role','ADMIN');}},{base,token});
    await page.route('**/api/favorites/ids',route=>route.fulfill({status:503,contentType:'application/json',body:'{}'}));
    await page.goto(base+'/index.html');await page.waitForSelector('.product-card');assert.equal(await page.locator('a[href="/control.html"]').count(),1);await page.unroute('**/api/favorites/ids');
    report.checks.push('A failed optional favourites request does not hide the public catalogue; owner can return to administration');
    assert.deepEqual(report.pageErrors,[]);report.productCount=products.length;report.browser=await browser.version();report.passed=true;
  }catch(error){report.failure=error.message;throw error;}
  finally{fs.writeFileSync(output+'/storefront-report.json',JSON.stringify(report,null,2));await browser.close();}
  console.log('Storefront E2E passed ('+report.checks.length+' scenario groups; no external AI call)');
}
main().catch(error=>{console.error(error.message);process.exitCode=1;});
