'use strict';
// Client guard contract only; native Java authentication remains authoritative.
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const source=fs.readFileSync('src/main/resources/static/js/workspace-affinity.js','utf8');
const jwt=key=>'e30.'+Buffer.from(JSON.stringify({iss:'aria:'+key,organization:key})).toString('base64url')+'.not-a-real-signature';
function fixture(page='/control.html'){
 const values=new Map([['aria_token',jwt('business-a')]]),events={},calls=[],navigations=[];let resolve;
 const response=new Promise(r=>resolve=r),context={URL,Headers,Request,DOMException,atob,localStorage:{getItem:k=>values.get(k)||null,setItem:(k,v)=>values.set(k,v)},location:{pathname:page,href:'https://aria.test'+page,origin:'https://aria.test',replace:url=>navigations.push(url)},document:{body:{replaceChildren(){}}},addEventListener:(name,fn)=>events[name]=fn,fetch:async(input,options)=>{calls.push({input,options});return response;}};context.window=context;vm.runInNewContext(source,context);
 return {context,values,calls,navigations,events,resolve};
}
(async()=>{
 const first=fixture();const pending=first.context.fetch('/api/admin/control/products',{headers:{Authorization:'Bearer '+jwt('business-a')}});first.resolve({status:200});await pending;assert.equal(first.calls[0].options.headers.get('X-ARIA-Workspace'),'business-a');
 first.values.set('aria_token',jwt('business-b'));await assert.rejects(()=>first.context.fetch('/api/admin/control/products',{method:'POST',headers:{Authorization:'Bearer '+jwt('business-b')}}),/Workspace changed/);assert.equal(first.calls.length,1);assert.equal(first.navigations.length,1);
 const inflight=fixture();const old=inflight.context.fetch('/api/admin/control/products',{headers:{Authorization:'Bearer '+jwt('business-a')}});inflight.values.set('aria_token',jwt('business-b'));inflight.resolve({status:200});await assert.rejects(()=>old,/Workspace changed/);
 const event=fixture();event.events.storage({key:'aria_token',newValue:jwt('business-b')});assert.equal(event.navigations.length,1);
 const login=fixture('/login.html');login.values.set('aria_token',jwt('business-b'));const signIn=login.context.fetch('/api/auth/session',{headers:{Authorization:'Bearer '+jwt('business-b')}});login.resolve({status:200});await signIn;assert.equal(login.navigations.length,0);
 console.log(JSON.stringify({passed:true,checks:5,contractOnly:true,provesServerAuthentication:false}));
})().catch(e=>{console.error(e.message);process.exitCode=1;});
