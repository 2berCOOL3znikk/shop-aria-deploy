'use strict';
// Browser component contract with explicit fixtures. This is not a merchant E2E.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {chromium}=require(path.join(process.env.USERPROFILE,'.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright'));
const root=path.resolve('src/main/resources/static'),origin='https://aria-ux.invalid';
const report={contractOnly:true,checks:[],errors:[]};let browser;
const token='e30.'+Buffer.from(JSON.stringify({iss:'aria:fixture',organization:'fixture',sub:'fixture-owner'})).toString('base64url')+'.fixture';
async function open({publicStorefront=false,role=null,broken=false,lang='ru'}={}){
 const context=await browser.newContext({serviceWorkers:'block'}),requests=[];
 await context.addInitScript(({token,role,lang})=>{localStorage.setItem('aria_lang',lang);if(role){localStorage.setItem('aria_token',token);localStorage.setItem('aria_role',role);localStorage.setItem('aria_email','fixture@example.invalid');}},{token,role,lang});
 const page=await context.newPage();page.setDefaultTimeout(7000);page.on('pageerror',e=>report.errors.push(e.message));
 await page.route(origin+'/**',async route=>{
  const request=route.request(),url=new URL(request.url());if(url.pathname.startsWith('/api/')){
   requests.push({path:url.pathname,authenticated:request.headers().authorization==='Bearer '+token});
   const values={'/api/auth/capabilities':{publicStorefront,buyerRegistration:publicStorefront},'/api/auth/session':{role},'/api/products/store':broken?{message:'Fixture unavailable'}:[{id:1,name:'Contract fixture',sku:'FIXTURE',category:'Fixture',unitPrice:100,displayPrice:100,quantityOnHand:3,unit:'pcs'}],'/api/products/impact/positions':{},'/api/products/store-info':{demonstration:true},'/api/cart':[], '/api/cart/total':{},'/api/favorites/ids':{ids:[]}};
   return route.fulfill({status:broken&&url.pathname==='/api/products/store'?503:200,contentType:'application/json',body:JSON.stringify(values[url.pathname]||[])});
  }
  const file=path.resolve(root,'.'+url.pathname);if(file.startsWith(root+path.sep)&&fs.existsSync(file)&&fs.statSync(file).isFile())return route.fulfill({path:file});return route.fulfill({status:404,body:''});
 });
 await page.goto(origin+'/index.html');await page.evaluate(()=>window.ariaStorefront.ready);return{context,page,requests};
}
(async()=>{try{
 browser=await chromium.launch({channel:'msedge',headless:true});
 let f=await open();await f.page.getByRole('heading',{name:'Витрина компании закрыта'}).waitFor();assert.equal(f.requests.some(r=>r.path==='/api/products/store'),false);assert.equal(await f.page.getByRole('link',{name:'Войти в ARIA'}).count(),1);report.checks.push('Anonymous visitors get a private-store explanation without requesting the company catalogue');await f.context.close();
 f=await open({role:'ORG'});await f.page.getByRole('heading',{name:'Витрина компании закрыта'}).waitFor();assert.equal(f.requests.some(r=>r.path==='/api/products/store'),false);report.checks.push('A partner role cannot enter the owner catalogue preview');await f.context.close();
 f=await open({role:'ADMIN'});await f.page.getByRole('link',{name:'Contract fixture',exact:true}).first().waitFor();for(const endpoint of ['/api/products/store','/api/products/store-info','/api/products/impact/positions'])assert.equal(f.requests.find(r=>r.path===endpoint)?.authenticated,true);assert.equal(await f.page.getByRole('link',{name:'Вернуться в кабинет'}).getAttribute('href'),'/control.html#products');assert.equal(await f.page.locator('[data-open-assistant]').isVisible(),false);report.checks.push('Verified owner preview authenticates all catalogue reads, offers a return route and hides unavailable AI');await f.context.close();
 f=await open({publicStorefront:true,broken:true});await f.page.getByText('Каталог временно недоступен',{exact:true}).waitFor();assert.equal(await f.page.locator('#products').getAttribute('aria-busy'),'false');assert.equal(await f.page.locator('#catalog-result-count').innerText(),'');report.checks.push('A failed catalogue read leaves neither a spinner nor a stale loading count');await f.context.close();
 f=await open({publicStorefront:true,lang:'en'});await f.page.getByRole('link',{name:'Contract fixture',exact:true}).first().waitFor();assert.equal(f.requests.find(r=>r.path==='/api/products/store')?.authenticated,false);assert.equal(await f.page.locator('.storefront-preview-note').count(),0);report.checks.push('The public demo still loads anonymously in English');await f.context.close();
 assert.deepEqual(report.errors,[]);report.passed=true;
 }catch(e){report.failure=e.message;process.exitCode=1;}finally{await browser?.close();report.checkedAt=new Date().toISOString();fs.mkdirSync('docs/ux/evidence',{recursive:true});fs.writeFileSync('docs/ux/evidence/storefront-contract.json',JSON.stringify(report,null,2)+'\n');console.log(JSON.stringify(report));}})();
