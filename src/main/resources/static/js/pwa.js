(() => {
 'use strict';if(!('serviceWorker' in navigator)||!window.isSecureContext)return;
 let installPrompt=null,registration=null,reloading=false,requestedUpdate=false;
 const notify=text=>{if(typeof window.showToast==='function')window.showToast(text);else{const node=document.getElementById('aria-app-status');if(node)ariaCopy.setProperty(node,"textContent",text);}};
 function controls(){if(document.getElementById('aria-app-controls'))return;const host=document.querySelector('.control-sidebar-footer')||document.getElementById('aria-shell-footer')||document.querySelector('main');if(!host)return;const box=document.createElement('div');box.id='aria-app-controls';ariaCopy.setProperty(box,"innerHTML",ariaCopy.text("text.c8e3eb4b0e7150"));host.appendChild(box);
  box.querySelector('#aria-install').onclick=async()=>{if(installPrompt){await installPrompt.prompt();await installPrompt.userChoice;installPrompt=null;}else notify(ariaCopy.text("text.12962a5208c372"));};
  box.querySelector('#aria-notifications').onclick=async()=>{if(!('Notification' in window)){notify(ariaCopy.text("text.5dac98e93149ce"));return;}const permission=await Notification.requestPermission();notify(permission==='granted'?ariaCopy.text("text.7e0b1f1335e674"):ariaCopy.text("text.2929186658a883"));};
  box.querySelector('#aria-app-update').onclick=()=>{if(registration?.waiting&&confirm(ariaCopy.text("text.f02943096cae22"))){requestedUpdate=true;registration.waiting.postMessage({type:'ACTIVATE_UPDATE'});}};
 }
 window.addEventListener('beforeinstallprompt',event=>{event.preventDefault();installPrompt=event;controls();});
 navigator.serviceWorker.addEventListener('controllerchange',()=>{if(!requestedUpdate||reloading||!navigator.serviceWorker.controller)return;reloading=true;location.reload();});
 window.addEventListener('aria:manager-alert',event=>{if(window.ariaAuth?.getRole()==='ADMIN'&&'Notification' in window&&Notification.permission==='granted'&&event.detail?.count>0){const key='aria_manager_notice';if(sessionStorage.getItem(key)!==String(event.detail.count)){showNotification(ariaCopy.text("text.90e406e8ebcfdf"),{body:ariaCopy.text("text.9152007ee0f582"),tag:'aria-manager'});sessionStorage.setItem(key,String(event.detail.count));}}});
 window.addEventListener('aria:order-recorded',()=>{if('Notification' in window&&Notification.permission==='granted')showNotification(ariaCopy.text("text.888f9f9fcf9741"),{body:ariaCopy.text("text.ee765473bc6a39"),tag:'aria-order'});});
 function showNotification(title,options){if(registration)registration.showNotification(title,options).catch(()=>{});}
 function init(){controls();navigator.serviceWorker.register('/sw.js',{scope:'/',updateViaCache:'none'}).then(r=>{registration=r;const update=()=>{const button=document.getElementById('aria-app-update');if(button)button.hidden=!r.waiting;};update();r.addEventListener('updatefound',()=>{r.installing?.addEventListener('statechange',update);});}).catch(()=>notify(ariaCopy.text("text.80a3d8e1e41e62")));}
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();
