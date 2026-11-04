'use strict';
// Real external inference is opt-in and only ever uses the isolated loopback preview.
const fs=require('node:fs'),assert=require('node:assert/strict');
const {chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||'playwright');
const dir='target/e2e/aria3',real=process.env.ARIA_E2E_REAL_AI==='true',verify=process.argv.includes('--verify-persistence');
(async()=>{
  const cfg=JSON.parse(fs.readFileSync('data/preview/config.json','utf8')),base='http://127.0.0.1:'+cfg.port;
  const browser=await chromium.launch({channel:'msedge',headless:true}),report={realOpenRouter:real,checks:[],errors:[]};
  fs.mkdirSync(dir,{recursive:true});let original,ctx,token;
  const api=async(path,method='GET',body)=>{const r=await ctx.request.fetch(base+'/api/admin/control'+path,{method,headers:{Authorization:'Bearer '+token},data:body});const value=await r.json();assert.equal(r.status(),200,JSON.stringify(value));return value;};
  try {
    ctx=await browser.newContext({viewport:{width:1440,height:1000}});const p=await ctx.newPage();p.on('pageerror',e=>report.errors.push(e.message));p.on('dialog',d=>d.accept());
    await p.goto(base+'/login.html');await p.locator('#login-email').fill(cfg.email);await p.locator('#login-password').fill(cfg.password);await p.locator('#login-form [type=submit]').click();await p.waitForURL('**/control.html');await p.waitForSelector('.ws-ai-card');token=await p.evaluate(()=>localStorage.getItem('aria_token'));
    if(verify){
      const manual=JSON.parse(fs.readFileSync(dir+'/manual-product-state.json','utf8'));
      const item=await api('/products/'+manual.id);
      assert.equal(item.product.sku,manual.sku);assert.equal(item.product.name,manual.name);assert.equal(item.product.quantityOnHand,0);
      report.manualPersistenceVerified=true;
      const aiFile=dir+'/created-product-state.json';
      if(fs.existsSync(aiFile)){
        const saved=JSON.parse(fs.readFileSync(aiFile,'utf8'));
        if(saved.safetyContract==='BOUND_TEXT_V1'){
          const aiItem=await api('/products/'+saved.id),origin=await api('/products/'+saved.id+'/ai-provenance');
          assert.equal(aiItem.product.sku,saved.sku);assert.equal(aiItem.product.name,saved.name);assert.equal(origin.model,saved.model);assert.equal(origin.verificationStatus,'OWNER_REVIEWED_TEXT');assert.equal(origin.productId,saved.id);report.aiPersistenceVerified=true;
        }else{report.aiPersistenceVerified=false;report.note='Earlier AI attempt before the bounded text contract is not reused as evidence of the final live scenario';}
      }
      report.checks.push('Persisted manual product and automatic SKU survive an actual Java process restart; AI evidence is reported separately');report.passed=true;return;
    }
    original=await api('/products/ai-settings');
    const begin=async()=>{await p.locator('.ws-page-heading [data-action=product-new]').click();await p.waitForSelector('#product-start');};
    await begin();await p.locator('#product-start input[name=name]').fill('Товар для внутреннего учёта');await p.locator('#product-manual').click();await p.locator('#control-dialog [name=unitPrice]').fill('750');
    assert.equal(await p.locator('#control-dialog input[name=sku]').count(),0);await p.locator('#control-dialog [type=submit]').click();await p.waitForFunction(()=>!document.getElementById('control-dialog').open);await p.waitForSelector('.ws-ai-card');
    const manual=(await api('/products')).filter(x=>x.name==='Товар для внутреннего учёта').sort((a,b)=>a.id-b.id).at(-1);assert.match(manual.sku,/^ARIA-AUTO-\d+$/);assert.equal(manual.quantityOnHand,0);fs.writeFileSync(dir+'/manual-product-state.json',JSON.stringify({id:manual.id,sku:manual.sku,name:manual.name},null,2));report.checks.push('Manual two-step creation needs no AI consent or hand-written SKU; stock stays zero until receiving a lot');
    if(real){
      assert.equal(original.configured,true,'A configured free OpenRouter model is required for this live check');
      await begin();await p.locator('#product-start input[name=name]').fill('Шоколад молочный');
      if(!original.textConsentEnabled){await p.locator('#product-start [type=submit]').click();assert.match(await p.locator('#product-start-error').innerText(),/Разрешите/);await p.locator('#product-start [name=consent]').check();}
      // This one deliberately simulated outage verifies honest error UX; it is not an AI result.
      await p.route('**/api/admin/control/products/ai-drafts',route=>route.fulfill({status:503,contentType:'application/json',body:JSON.stringify({message:'ИИ временно недоступен — проверка обработки ошибки'})}));
      await p.locator('#product-start [type=submit]').click();await p.waitForFunction(()=>document.getElementById('product-start-error')?.textContent.includes('проверка обработки ошибки'));
      assert.equal(await p.locator('.pc-source').count(),0);await p.unroute('**/api/admin/control/products/ai-drafts');
      const liveReply=p.waitForResponse(r=>r.url().endsWith('/products/ai-drafts')&&r.request().method()==='POST');await p.locator('#product-start [type=submit]').click();const liveResponse=await liveReply;const liveBody=await liveResponse.json();if(liveResponse.status()!==200){report.providerError=liveBody.message||liveBody.error;await p.screenshot({path:dir+'/product-ai-unavailable.png'});}assert.equal(liveResponse.status(),200,JSON.stringify({message:report.providerError}));await p.waitForSelector('.pc-source');
      assert.match(await p.locator('.pc-source').innerText(),/Решение ARIA AI/);assert.match(await p.locator('.pc-source').innerText(),/не проверенные характеристики/);
      const safeText=await p.locator('#control-dialog [name=description]').inputValue();assert.doesNotMatch(safeText,/какао|сахар|молока|\d|изготовлен|производитель/i);const draftName=await p.locator('#control-dialog [name=name]').inputValue();assert.doesNotMatch(draftName,/\d{10,}/);
      await p.screenshot({path:dir+'/product-ai-draft-1440.png',fullPage:true});await p.setViewportSize({width:390,height:844});assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));await p.screenshot({path:dir+'/product-ai-draft-390.png',fullPage:true});
      await p.locator('#control-dialog [name=unitPrice]').fill('990');await p.locator('#control-dialog [name=confirmed]').check();await p.locator('#control-dialog [type=submit]').click();await p.waitForFunction(()=>!document.getElementById('control-dialog').open);await p.waitForSelector('.ws-ai-card');
      const saved=(await api('/products')).filter(x=>x.name===draftName).sort((a,b)=>a.id-b.id).at(-1),origin=await api('/products/'+saved.id+'/ai-provenance');
      assert.match(saved.sku,/^ARIA-AUTO-\d+$/);assert.equal(saved.quantityOnHand,0);assert.equal(origin.source,'AI');assert.equal(origin.verificationStatus,'OWNER_REVIEWED_TEXT');assert.ok(origin.confirmedAt);assert.ok(origin.model);assert.notEqual(origin.model,'ENGINE');
      report.model=origin.model;report.product={id:saved.id,sku:saved.sku,name:saved.name,model:origin.model,safetyContract:'BOUND_TEXT_V1'};fs.writeFileSync(dir+'/created-product-state.json',JSON.stringify(report.product,null,2));
      const button=p.locator('#control-nav [data-id=products]');if(!await button.isVisible())await p.locator('#workspace-menu').click();await button.click();await p.waitForFunction(()=>document.getElementById('control-content').getAttribute('aria-busy')==='false');
      await p.locator('#control-search').fill(saved.sku);await p.waitForSelector('[data-action=product-edit][data-id="'+saved.id+'"]');await p.locator('[data-action=product-edit][data-id="'+saved.id+'"]').click();await p.waitForSelector('.pc-source');assert.match(await p.locator('.pc-source').innerText(),/Текст подготовлен ARIA AI/);await p.locator('#control-close').click();
      report.checks.push('Actual browser→Java→OpenRouter response shown in review; explicit text consent, no invented fact fields, owner confirmation, unique SKU, persisted source/model and edit provenance; no provider mock used for successful generation');
      report.checks.push('Separately simulated provider outage never displays a generated AI result; owner can retry or continue manually');
    }
    assert.deepEqual(report.errors,[]);report.passed=true;
  } catch(error){report.failure=error.stack;throw error;}
  finally {
    // Keep the owner's original separate text consent and budget. Does not touch pricing consents.
    if(original&&ctx&&token){try{const current=await api('/products/ai-settings');if(current.textConsentEnabled!==original.textConsentEnabled||current.dailyBudget!==original.dailyBudget)await api('/products/ai-settings','PUT',{revision:current.revision,textConsentEnabled:original.textConsentEnabled,dailyBudget:original.dailyBudget});}catch(error){report.restoreError=error.message;report.passed=false;process.exitCode=1;}}
    fs.writeFileSync(dir+(verify?'/product-persistence-report.json':real?'/product-creation-report.json':'/product-manual-report.json'),JSON.stringify(report,null,2));await browser.close();
  }
})().catch(error=>{console.error(error);process.exitCode=1;});
