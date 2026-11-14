'use strict';
// Read-only screenshots of the explicitly isolated staging; no setup, imports or consent changes.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||path.join(process.env.USERPROFILE,'.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright'));
process.chdir(path.resolve(__dirname,'../..'));const cfg=JSON.parse(fs.readFileSync('data/staging/config.json','utf8').replace(/^\uFEFF/,'')),base=process.argv[2]||'http://127.0.0.1:8091',label=process.argv[3]||'after-local',out='docs/aria4/evidence/operations';
assert.ok(['http://127.0.0.1:8091','https://aria-v4-staging-2ber.onrender.com'].includes(base));assert.match(label,/^[a-z-]+$/);assert.equal(cfg.projectId,'dawn-dew-85661116');fs.mkdirSync(out,{recursive:true});
(async()=>{const browser=await chromium.launch({channel:'msedge',headless:true});const report={base,label,readOnly:true,views:[],errors:[],completed:false};try{
 const ctx=await browser.newContext({viewport:{width:1440,height:1000},serviceWorkers:'block'}),p=await ctx.newPage();p.setDefaultTimeout(60000);p.on('pageerror',e=>report.errors.push(e.message));
 const identity=await ctx.request.get(base+'/api/application',{timeout:120000});const info=await identity.json();assert.equal(info.environment,'STAGING');report.revision=info.build.revision;
 await p.goto(base+'/login.html');await p.locator('#login-email').fill(cfg.email);await p.locator('#login-password').fill(cfg.password);await p.locator('#login-form [type=submit]').click();await p.waitForURL('**/control.html');
 for(const route of ['lots','purchases','suppliers','orders'])for(const width of [1440,390]){
  await p.setViewportSize({width,height:width===390?844:1000});await p.goto(base+'/control.html#'+route);await p.reload();await p.locator('#control-content[aria-busy=false]').waitFor();if(!label.startsWith('before'))await p.locator('[data-operations]').waitFor();assert.equal(await p.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1),false);
  await p.screenshot({path:out+'/'+label+'-'+route+'-'+width+'.png',fullPage:true});
  if(!label.startsWith('before')&&route==='lots'&&width===390){await p.locator('.ox-menu summary').first().click();const menu=p.locator('.ox-menu[open]>div');const bounds=await menu.boundingBox();assert.ok(bounds.x>=0&&bounds.x+bounds.width<=width);await p.keyboard.press('Escape');assert.equal(await p.locator('.ox-menu[open]').count(),0);}
  if(!label.startsWith('before')&&route==='purchases'){assert.ok((await p.locator('#purchase-search').boundingBox()).width>180);await p.locator('#purchase-search').fill('missing-review-query');assert.equal(await p.locator('[data-purchase-row]').count(),0);await p.locator('[data-purchase=clear]').click();await p.locator('[data-purchase-row]').waitFor();}
  report.views.push({route,width});
 }
 assert.deepEqual(report.errors,[]);report.completed=true;
 }catch(e){report.failure=e.message;process.exitCode=1;}finally{await browser.close();fs.writeFileSync(out+'/'+label+'-result.json',JSON.stringify(report,null,2));console.log(JSON.stringify(report));}})();
