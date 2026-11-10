'use strict';
// User-invoked preview: decrypt local educational credentials in memory and sign in through the real UI.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
process.chdir(path.resolve(__dirname,'../..'));
const cfg=JSON.parse(fs.readFileSync('data/onboarding/registry-config.json','utf8').replace(/^\uFEFF/,''));
function open(value){const [iv,raw]=value.split(':'),data=Buffer.from(raw,'base64'),cipher=crypto.createDecipheriv('aes-256-gcm',Buffer.from(cfg.encryptionKey,'base64'),Buffer.from(iv,'base64'));cipher.setAuthTag(data.subarray(-16));return Buffer.concat([cipher.update(data.subarray(0,-16)),cipher.final()]).toString('utf8');}
(async()=>{
 const {chromium}=require(process.env.ARIA_PLAYWRIGHT_MODULE||path.join(process.env.USERPROFILE,'.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright'));
 const workspace=JSON.parse(open(fs.readFileSync('data/onboarding/workspace-config.enc','utf8'))),password=open(fs.readFileSync('data/onboarding/e2e-owner-password.enc','utf8'));
 const browser=await chromium.launch({channel:'msedge',headless:false}),context=await browser.newContext(),page=await context.newPage();
 await page.goto('http://127.0.0.1:8089/login.html?next=%2Fcontrol.html%23stores');await page.locator('#login-email').fill(workspace.email);await page.locator('#login-password').fill(password);await page.locator('#login-form [type=submit]').click();await page.waitForURL('**/control.html#stores');
 console.log('Учебный кабинет открыт. Пароль и ключи не выводятся. Закройте окно браузера для завершения.');await new Promise(resolve=>browser.once('disconnected',resolve));
})().catch(()=>{console.error('Не удалось открыть локальный учебный кабинет. Запустите локальные процессы по инструкции docs/onboarding/README_RU.md.');process.exitCode=1;});
