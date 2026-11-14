'use strict';
// Read-only check of the public isolated staging. No fixtures, consent, keys or stock writes.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||path.join(process.env.USERPROFILE,'.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright'));
process.chdir(path.resolve(__dirname,'../..'));
const read=p=>JSON.parse(fs.readFileSync(p,'utf8').replace(/^\uFEFF/,'')),cfg=read('data/staging/config.json'),recipient=read('data/staging/p9-review.json'),fixture=read('data/staging/p11-review.json');
const base='https://aria-v4-staging-2ber.onrender.com',out='docs/aria4/evidence/partner-photo',expected=process.argv[2]||'fdba9788688c1c6619cc3df3d1d11be923ed9180';
assert.equal(cfg.url,base);assert.equal(cfg.projectId,'dawn-dew-85661116');assert.equal(fixture.completed,true);
const report={base,readOnly:true,productionChanged:false,checks:[],views:0,errors:[],completed:false};
fs.mkdirSync(out,{recursive:true});
(async()=>{let browser;try{
 browser=await chromium.launch({channel:'msedge',headless:true});
 const owner=await browser.newContext({viewport:{width:1440,height:1000},serviceWorkers:'block'}),p=await owner.newPage();p.setDefaultTimeout(60000);p.on('pageerror',e=>report.errors.push(e.message));
 const identity=await owner.request.get(base+'/api/application',{timeout:120000});assert.equal(identity.status(),200);const app=await identity.json();assert.equal(app.environment,'STAGING');assert.equal(app.desktop,false);assert.equal(app.build.revision,expected);report.revision=app.build.revision;
 const login=async(page,email,password)=>{await page.goto(base+'/login.html');await page.locator('#login-email').fill(email);await page.locator('#login-password').fill(password);await page.locator('#login-form [type=submit]').click();await page.waitForURL(url=>url.pathname!=='/login.html');return page.evaluate(()=>localStorage.getItem('aria_token'));};
 const token=await login(p,cfg.email,cfg.password),headers={Authorization:'Bearer '+token},get=async route=>{const r=await owner.request.get(base+'/api/admin/control'+route,{headers});assert.equal(r.status(),200,route);return r.json();};
 const members=(await get('/team')).members;assert.ok(members.filter(m=>m.email.endsWith('@example.test')).length>=2);assert.ok(members.filter(m=>m.email.endsWith('@example.test')).every(m=>m.active===false));
 const products=await get('/products');assert.equal(products.find(x=>x.sku==='ARIA-REVIEW-P7-001').quantityOnHand,12);assert.equal(products.find(x=>x.id===fixture.productId).archived,true);
 const manager=await get('/manager');assert.equal(manager.ai.configured,false);assert.equal(manager.pricing.externalAiEnabled,false);assert.equal((await get('/stores')).stores.filter(s=>!s.archived).length,0);
 report.checks.push('Exact deployed revision and staging identity confirmed; synthetic staff disabled, review product archived, original review stock remains 12, no connected credentials or external AI consent');
 // This check deliberately has no invitation, acceptance, membership or warehouse write branch.
 for(const [lang,theme]of [['ru','light'],['en','dark']])for(const width of [1440,390]){
  await p.evaluate(({lang,theme})=>{localStorage.setItem('aria_lang',lang);localStorage.setItem('aria_theme',theme);},{lang,theme});await p.setViewportSize({width,height:width===390?844:1000});await p.goto(base+'/control.html#team');await p.reload();await p.locator('#control-content[aria-busy=false]').waitFor();await p.locator('[data-team-member]').first().waitFor();assert.equal(await p.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1),false);await p.screenshot({path:'docs/aria4/evidence/team/online-owner-'+lang+'-'+theme+'-'+width+'.png',fullPage:true});report.views++;
 }
 report.checks.push('Owner team screen checked at 390 and 1440px, RU/light and EN/dark, without any management or stock writes');
 const url=base+'/api/social/offers/'+fixture.offerId+'/photo',image=await owner.request.get(url,{headers});assert.equal(image.status(),200);assert.match(image.headers()['content-type'],/^image\/png/);assert.match(image.headers()['cache-control'],/no-store/);assert.equal((await owner.request.get(url)).status(),401);
 report.checks.push('Owner can inspect the retained control image; response is private and not cached; anonymous access is rejected');
 const partner=await browser.newContext({viewport:{width:390,height:844},serviceWorkers:'block'}),s=await partner.newPage();s.setDefaultTimeout(60000);s.on('pageerror',e=>report.errors.push(e.message));const partnerToken=await login(s,recipient.partnerEmail,recipient.partnerPassword),partnerHeaders={Authorization:'Bearer '+partnerToken};
 assert.equal((await partner.request.get(url,{headers:partnerHeaders})).status(),404);assert.equal((await partner.request.get(base+'/api/products/'+fixture.productId+'/photo',{headers:partnerHeaders})).status(),403);assert.equal((await partner.request.get(base+'/api/admin/control/team',{headers:partnerHeaders})).status(),403);
 const claims=await partner.request.get(base+'/api/social/my-claims',{headers:partnerHeaders});assert.equal(claims.status(),200);assert.deepEqual(await claims.json(),[]);
 report.checks.push('Unverified partner cannot view a cancelled offer image, private catalogue or staff; own history API remains available and contains no fabricated transfer');
 for(const [lang,theme]of [['ru','light'],['en','dark']])for(const width of [1440,390]){
  await s.evaluate(({lang,theme})=>{localStorage.setItem('aria_lang',lang);localStorage.setItem('aria_theme',theme);},{lang,theme});await s.setViewportSize({width,height:width===390?844:1000});await s.goto(base+'/social.html');const loaded=s.waitForResponse(r=>r.url()===base+'/api/products/store-info'&&r.request().method()==='GET');await s.reload();await loaded;await s.locator('#grid[aria-busy=false] .empty-state').waitFor();assert.equal(await s.locator('#grid [role=alert]').count(),0);await s.locator('#org-verification-panel').waitFor();await s.waitForFunction(()=>document.getElementById('org-verification-status')?.textContent.trim());assert.equal(await s.locator('[data-claim]').count(),0);assert.equal(await s.locator('#assistant-launcher:visible').count(),0);assert.equal(await s.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1),false);await s.screenshot({path:out+'/online-recipient-'+lang+'-'+theme+'-'+width+'.png',fullPage:true});report.views++;
 }
 assert.deepEqual(report.errors,[]);report.completed=true;
}catch(e){report.failure=e.message;process.exitCode=1;}finally{if(browser)await browser.close();report.checkedAt=new Date().toISOString();fs.writeFileSync(out+'/online-result.json',JSON.stringify(report,null,2));console.log(JSON.stringify(report));}})();
