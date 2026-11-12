'use strict';
// User-invoked preview: decrypt local educational credentials in memory and sign in through the real UI.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
process.chdir(path.resolve(__dirname,'../..'));
const cfg=JSON.parse(fs.readFileSync('data/onboarding/registry-config.json','utf8').replace(/^\uFEFF/,''));
function open(value){const [iv,raw]=value.split(':'),data=Buffer.from(raw,'base64'),cipher=crypto.createDecipheriv('aes-256-gcm',Buffer.from(cfg.encryptionKey,'base64'),Buffer.from(iv,'base64'));cipher.setAuthTag(data.subarray(-16));return Buffer.concat([cipher.update(data.subarray(0,-16)),cipher.final()]).toString('utf8');}
let previewBrowser;
const connected=process.argv.includes('--woo'),host=connected?'http://127.0.0.1:8085':'http://127.0.0.1:8089',home=process.argv.includes('--home'),readyArg=process.argv.find(value=>value.startsWith('--ready-file='));
const readyFile=readyArg&&path.resolve(readyArg.slice('--ready-file='.length));
if(readyFile&&(path.dirname(readyFile)!==path.resolve('data/desktop')||!/^preview-[a-f0-9]{32}\.json$/.test(path.basename(readyFile))))throw new Error('Invalid preview receipt location');
const receipt=opened=>{if(readyFile)fs.writeFileSync(readyFile,JSON.stringify({opened,url:opened?host+'/control.html#'+(connected?'manager':home?'workspace':'stores'):null}));};
(async()=>{
 const {chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||path.join(process.env.USERPROFILE,'.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright'));
 const workspace=connected?JSON.parse(fs.readFileSync('data/woocommerce/aria-config.json','utf8').replace(/^\uFEFF/,'')):JSON.parse(open(fs.readFileSync('data/onboarding/workspace-config.enc','utf8'))),password=connected?workspace.password:open(fs.readFileSync('data/onboarding/e2e-owner-password.enc','utf8'));
 const browser=await chromium.launch({channel:'msedge',headless:false});previewBrowser=browser;const context=await browser.newContext(),page=await context.newPage();
 const route=connected?'manager':home?'workspace':'stores';await page.goto(host+'/login.html?next='+encodeURIComponent('/control.html#'+route));await page.locator('#login-email').fill(workspace.email);await page.locator('#login-password').fill(password);await page.locator('#login-form [type=submit]').click();await page.waitForURL('**/control.html#'+route);await page.locator('#control-content[aria-busy="false"]').waitFor({timeout:30000});receipt(true);
 console.log('Учебный кабинет открыт. Пароль и ключи не выводятся. Закройте окно браузера для завершения.');await new Promise(resolve=>browser.once('disconnected',resolve));
})().catch(async()=>{receipt(false);if(previewBrowser?.isConnected())await previewBrowser.close();console.error('Не удалось открыть локальный учебный кабинет. Запустите локальные процессы по инструкции docs/onboarding/README_RU.md.');process.exitCode=1;});
