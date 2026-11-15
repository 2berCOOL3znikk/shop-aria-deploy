// Page affinity is an additional UI guard. Real authentication remains in the isolated Java service.
(()=>{'use strict';
 const binding=token=>{try{const p=String(token||'').split('.');if(p.length!==3)return null;const x=JSON.parse(atob(p[1].replace(/-/g,'+').replace(/_/g,'/')));return typeof x.organization==='string'&&x.iss==='aria:'+x.organization?x.organization:null;}catch{return null;}};
 const token=()=>localStorage.getItem('aria_token'),publicPage=/(?:^|\/)(login|cloud-signup|company-start|company-activate|employee-join)\.html$/.test(location.pathname);const session=token();let context=binding(session),leaving=false;
 // A different user in the same company must never inherit this page's private UI.
 // Do not clear the replacement session: another tab has just signed in successfully.
 function changed(){if(leaving)return;leaving=true;if(document.body)document.body.replaceChildren();location.replace('/login.html?session-changed=1');}
 function check(value){if(publicPage)return;if(value!==session||token()!==session){changed();throw new DOMException('Workspace changed','AbortError');}}
 const original=window.fetch.bind(window);window.fetch=async(input,options={})=>{
  const url=new URL(typeof input==='string'||input instanceof URL?input:input.url,location.href);const headers=new Headers(input instanceof Request?input.headers:undefined);new Headers(options.headers).forEach((v,k)=>headers.set(k,v));const authorization=headers.get('Authorization');
  const guarded=url.origin===location.origin&&url.pathname.startsWith('/api/')&&authorization?.startsWith('Bearer '),bearer=guarded?authorization.slice(7):null;
  if(guarded){check(bearer);const key=binding(bearer);if(key)headers.set('X-ARIA-Workspace',key);}
  const result=await original(input,{...options,headers});if(guarded)check(bearer);return result;
 };
 addEventListener('storage',e=>{if(!publicPage&&(e.key==='aria_token'&&e.newValue!==session||e.key===null&&token()!==session))changed();});
 addEventListener('pageshow',()=>{if(!publicPage&&token()!==session)changed();});
 addEventListener('focus',()=>{if(!publicPage&&token()!==session)changed();});
 window.ariaWorkspaceAffinity={binding,context:()=>context};
})();
