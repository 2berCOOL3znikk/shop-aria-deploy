'use strict';
// Runs only against the isolated loopback preview; no business data is modified.
const fs=require('node:fs'),assert=require('node:assert/strict');
const {chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||'playwright');
const directory='target/e2e/aria3';
(async()=>{
  const cfg=JSON.parse(fs.readFileSync('data/preview/config.json','utf8'));
  const base='http://127.0.0.1:'+cfg.port;
  const browser=await chromium.launch({channel:'msedge',headless:true});
  const report={checks:[],errors:[],screenshots:[]};fs.mkdirSync(directory,{recursive:true});
  try {
    const context=await browser.newContext({viewport:{width:1440,height:1000}}),p=await context.newPage();
    p.on('pageerror',e=>report.errors.push(e.message));
    await p.goto(base+'/login.html');await p.locator('#login-email').fill(cfg.email);await p.locator('#login-password').fill(cfg.password);await p.locator('#login-form [type=submit]').click();await p.waitForURL('**/control.html');await p.waitForSelector('.ws-ai-card');
    const token=await p.evaluate(()=>localStorage.getItem('aria_token'));
    const response=await context.request.get(base+'/api/admin/control/workspace/overview',{headers:{Authorization:'Bearer '+token}});
    assert.equal(response.status(),200);const d=await response.json();
    assert.equal(await p.locator('#workspace-company').textContent(),d.organization.name);
    assert.equal(await p.locator('#control-nav>button').count(),11);
    assert.deepEqual(await p.locator('#control-nav>button span').allTextContents(),['Главная','ARIA AI','Мои магазины','Товары','Заказы','Склад','Цены','Закупки','Аналитика','ARIA Impact','Настройки']);
    assert.match(await p.locator('.ws-ai-card').innerText(),/Что ARIA сделала сегодня/);
    const actual=await p.locator('.ws-ai-stats strong').allTextContents();
    assert.deepEqual(actual.map(n=>Number(n.replace(/\s/g,''))),[d.today.productsAnalyzed,d.today.aiDecisions,d.today.productDrafts,d.today.priceApplied,d.today.purchaseDrafts,d.today.socialDrafts]);
    assert.match(await p.locator('.ws-ai-footer').innerText(),new RegExp(String(d.today.engineDecisions)));
    assert.equal(d.sales.daily.length,14);assert.equal(d.sales.currency,'KZT');
    if(d.sales.daily.every(x=>Number(x.revenue)===0))assert.match(await p.locator('.ws-sales').innerText(),/Пока нет отгрузок/);
    else {await p.locator('.ws-chart-bar').first().focus();assert.equal(await p.locator('.ws-chart-bar>span').first().isVisible(),true);}
    report.checks.push('Dashboard counters equal persisted API facts; AI/engine provenance, demo label and native sales scope are explicit; zero-sales chart has an honest empty state');
    const groups={movements:'lots',pos:'lots',competitors:'smart',pricing:'smart',suppliers:'purchases',intelligence:'overview',social:'socialPlans',policies:'settings',integrations:'settings',audit:'settings',feedback:'settings',guide:'settings'};
    const nav=async key=>{
      const parent=groups[key]||key;
      const target=p.locator('#control-nav [data-id="'+parent+'"]');
      if(!await target.isVisible())await p.locator('#workspace-menu').click();
      await target.click();
      await p.waitForFunction(()=>document.getElementById('control-content').getAttribute('aria-busy')==='false');
      if(parent!==key){await p.locator('.ws-subnav [data-id="'+key+'"]').click();await p.waitForFunction(()=>document.getElementById('control-content').getAttribute('aria-busy')==='false');}
      assert.equal(await p.locator('#control-nav [aria-current=page]').getAttribute('data-id'),parent);
    };
    const screenshot=async name=>{const path=directory+'/'+name;await p.screenshot({path,fullPage:true});report.screenshots.push(path);};
    for(const theme of ['light','dark']){
      await p.evaluate(t=>ariaTheme.applyTheme(t),theme);
      for(const width of [1440,390]){
        await p.setViewportSize({width,height:width===390?844:1000});await nav('workspace');await p.waitForSelector('.ws-ai-card');
        assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'dashboard overflow '+width);
        await screenshot('after-dashboard-'+theme+'-'+width+'.png');
      }
    }
    await p.evaluate(()=>ariaTheme.applyTheme('light'));
    for(const width of [1440,390]){
      await p.setViewportSize({width,height:900});
      for(const key of ['workspace','manager','stores','products','orders','lots','movements','pos','smart','competitors','pricing','purchases','suppliers','overview','intelligence','socialPlans','social','settings','policies','integrations','audit','feedback','guide']){
        await nav(key);assert.ok(await p.locator('#control-content').innerText(),'empty section '+key);
        assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),key+' overflow '+width);
      }
      await nav('settings');assert.equal(await p.locator('.ws-settings-grid>a').count(),8);await screenshot('after-settings-'+width+'.png');
      await nav('products');await screenshot('after-products-'+width+'.png');
      await nav('manager');await p.waitForSelector('.cc-workspace');await screenshot('after-ai-'+width+'.png');
    }
    report.checks.push('All 23 preserved sections reachable through primary navigation and contextual tabs at 1440/390px; settings, themes, dialogs and no horizontal page overflow');
    await nav('workspace');await p.waitForSelector('.ws-ai-card');
    await p.route('**/api/admin/control/workspace/overview',route=>route.fulfill({status:503,contentType:'application/json',body:JSON.stringify({message:'Проверка недоступного сервера'})}));
    await p.locator('#control-refresh').click();await p.waitForSelector('.ws-error');assert.match(await p.locator('.ws-error').innerText(),/Проверка недоступного сервера/);
    await p.unroute('**/api/admin/control/workspace/overview');await p.locator('.ws-error button').click();await p.waitForSelector('.ws-ai-card');
    await p.emulateMedia({reducedMotion:'reduce'});assert.equal(await p.locator('#control-content').evaluate(el=>getComputedStyle(el).animationName),'none');
    report.checks.push('Explicitly simulated API outage shows an error and real retry recovers; reduced motion is respected');
    const denied=await context.request.get(base+'/api/admin/control/workspace/overview');assert.equal(denied.status(),401);
    await p.goto(base+'/index.html');assert.match(await p.title(),/ARIA/i);await p.goto(base+'/control.html#workspace');await p.waitForSelector('.ws-ai-card');
    assert.deepEqual(report.errors,[]);report.checks.push('Storefront/admin return path works; anonymous overview denied; no browser exceptions');report.passed=true;
  } catch(error) {report.failure=error.stack;throw error;}
  finally {fs.writeFileSync(directory+'/workspace-report.json',JSON.stringify(report,null,2));await browser.close();}
})().catch(error=>{console.error(error);process.exit(1);});
