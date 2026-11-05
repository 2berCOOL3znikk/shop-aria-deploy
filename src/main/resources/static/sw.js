'use strict';
const CACHE='aria-public-shell-v2';
const SHELL=['/offline.html','/images/aria-app.svg','/js/app-copy.js','/js/app-copy-data.js'];
self.addEventListener('install',event=>{event.waitUntil(caches.open(CACHE).then(cache=>cache.addAll(SHELL)));});
self.addEventListener('activate',event=>{event.waitUntil((async()=>{for(const key of await caches.keys())if(key.startsWith('aria-public-shell-')&&key!==CACHE)await caches.delete(key);await self.clients.claim();})());});
self.addEventListener('message',event=>{if(event.data?.type==='ACTIVATE_UPDATE')self.skipWaiting();});
self.addEventListener('fetch',event=>{
 const request=event.request,url=new URL(request.url);
 if(request.method!=='GET'||url.origin!==self.location.origin||url.pathname.startsWith('/api/')||url.pathname.startsWith('/actuator/'))return;
 if(request.mode==='navigate')event.respondWith(fetch(request).catch(()=>caches.match('/offline.html')));
 else if(SHELL.includes(url.pathname))event.respondWith(fetch(request).catch(()=>caches.match(url.pathname)));
 // Only fixed public language assets are cached. Never cache API, login/profile/admin
 // HTML, catalogue data, other scripts or submitted forms.
});
