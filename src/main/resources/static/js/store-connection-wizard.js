(() => {
 'use strict';
 const t=(ru,en)=>ariaCopy.language==='en'?en:ru;
 const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const steps=[ariaCopy.text("text.18f13216456872"),ariaCopy.text("text.da82e80563d75a"),ariaCopy.text("text.a6a60f97b0163e"),ariaCopy.text("text.e56a412d12852f"),ariaCopy.text("text.d0ef3540b088bb"),'ARIA AI'];
 function open(ctx,data,existing){
  const {request,show,formField,notify}=ctx;let store=existing,platform=existing?.platform||'WOOCOMMERCE',test=existing?.testConnection||false,name='',url='',step=existing?(existing.platform==='ARIA_DEMO'?4:!existing.hasCredentials?3:existing.status!=='CONNECTED'?4:!existing.lastSync?5:6):1,busy=false;
  let preflight=null,preflightLoading=false;
  const dialog=document.getElementById('control-dialog');if(store?.archived){notify(ariaCopy.text("text.d2331d31fc46da"),true);return;}
  const close=()=>{ctx.clearDirty();dialog.querySelectorAll('#store-connect-wizard input[type=password]').forEach(input=>input.value='');dialog.close();window.ariaStores.mount(ctx);};
  dialog.addEventListener('close',()=>{dialog.querySelectorAll('#store-connect-wizard input[type=password]').forEach(input=>input.value='');},{once:true});
  function render(){
   if(!document.getElementById('stores-root'))return;
   let body='',primary=ariaCopy.text("text.a42aee58d60a7d"),allow=true;
   if(step===1)body=ariaCopy.template("template.92e3c96cbc5b4b",[platform==='WOOCOMMERCE'?'checked':'',data.organization.demonstration&&!data.stores.some(s=>s.platform==='ARIA_DEMO'&&!s.archived&&s.status!=='DISCONNECTED')?ariaCopy.template("template.a908ea9f877936",[platform==='ARIA_DEMO'?'checked':'']):'',test?'checked':'']);
   if(step===2)body=formField(ariaCopy.text("text.da75e2ce9c5e07"),'name','text',name,{required:true,attrs:'maxlength="200"'})+(platform==='WOOCOMMERCE'?formField(ariaCopy.text("text.561603441f8a18"),'url','url',url,{required:true,attrs:'maxlength="500" placeholder="https://shop.example"',help:ariaCopy.text("text.59c680b20bd97b")}):ariaCopy.text("text.641ce11efaf3e8"));
   if(step===3){allow=platform==='ARIA_DEMO'||Boolean(preflight?.readyForCredentials);primary=platform==='ARIA_DEMO'?ariaCopy.text("text.a42aee58d60a7d"):ariaCopy.text("text.fbfdee4f7c07dd");body=platform==='ARIA_DEMO'?ariaCopy.text("text.b63c6c4739b009"):preflight?.readyForCredentials?ariaCopy.template("template.d48d85c6c42c10",['',formField(ariaCopy.text("text.87ef0336743030"),'key','password','',{required:true,attrs:'autocomplete="new-password" pattern="ck_[a-zA-Z0-9]{20,100}" maxlength="103"'}),formField(ariaCopy.text("text.e724d875bf8810"),'secret','password','',{required:true,attrs:'autocomplete="new-password" pattern="cs_[a-zA-Z0-9]{20,100}" maxlength="103"'})]):`<div class="connect-result" data-connection-preflight><span class="exp-status">${preflight?t('Нужно подготовить подключение','Connection preparation needed'):t('Проверяю готовность…','Checking readiness…')}</span><p>${preflight?.status==='STORAGE_UNAVAILABLE'?t('Хранилище доступов ещё не подготовлено. Обратитесь к администратору ARIA. Ключи сейчас не нужны.','Secure credential storage is not ready. Contact your ARIA administrator. Keys are not needed yet.'):preflight?.status==='ADDRESS_REVIEW_REQUIRED'?t('Адрес магазина должен пройти проверку у администратора ARIA. Передайте ему адрес ниже; доступы пока не вводите.','Your store address needs approval from your ARIA administrator. Share the address below; do not enter credentials yet.'):preflight?t('Не удалось проверить готовность. Повторите попытку — доступы не запрашиваются.','Readiness could not be checked. Retry; credentials are not requested.'):t('Проверяем разрешение адреса и защищённое хранение. Магазин ещё не считается подключённым.','Checking address permission and secure storage. The store is not connected yet.')}</p><p data-merchant-content>${esc(store.url)}</p>${preflight?`<button type="button" class="control-small" id="connect-recheck">${t('Проверить снова','Check again')}</button>`:''}</div>`;}

   if(step===4){const ready=store.status==='CONNECTED';primary=ready?ariaCopy.text("text.91b00cb3a0f7f9"):ariaCopy.text("text.93275ac8c51758");body=`<div class="connect-result"><span class="exp-status">${ready?ariaCopy.text("text.71f328e84291b1"):ariaCopy.text("text.f348d7ff4eb4fb")}</span><p>${platform==='ARIA_DEMO'?ariaCopy.text("text.e5a2b109df05d6"):ready?ariaCopy.text("text.88e41260299860"):ariaCopy.text("text.e198c6b4a51f3f")}</p></div>${store.lastError?`<p class="control-form-error">${esc(store.lastError)}</p>`:''}`;}
   if(step===5){const imported=Boolean(store.lastSync);primary=imported?ariaCopy.text("text.84b072ddf1dfd7"):ariaCopy.text("text.5dcd62f36c577f");body=`<div class="connect-result"><span class="exp-status">${imported?ariaCopy.text("text.684b480f1ce444"):ariaCopy.text("text.7831cff2e8b65c")}</span><p>${imported?ariaCopy.template("template.34adbbe6485cea",[Number(store.products),esc(store.catalogCurrency||ariaCopy.text("text.62beb8232c0b64"))]):ariaCopy.text("text.799d1c8237d374")}</p></div>${platform==='WOOCOMMERCE'?ariaCopy.text("text.dcc31ad8d75070"):''}${store.lastError?`<p class="control-form-error">${esc(store.lastError)}</p>`:''}`;}
   if(step===6){primary=ariaCopy.text("text.ef05d57959cfde");body=platform==='ARIA_DEMO'?ariaCopy.template("template.758554630347d5",[]):ariaCopy.template("template.635ecf579b5df1",[]);}
   show(ariaCopy.text("text.0e4f7119b6794c"),ariaCopy.template("template.789262a5ccf271",[steps.map((label,index)=>`<li ${step===index+1?'aria-current="step"':''} class="${step>index+1?'done':''}"><span>${index+1}</span><small>${label}</small></li>`).join(''),step,store?' · '+esc(store.name):'',steps[step-1],body,step===2||step===4&&platform==='WOOCOMMERCE'||step===5?ariaCopy.text("text.77b9a4bee67192"):'',!allow?'disabled':'',primary]));
   const panel=document.getElementById('store-connect-wizard'),form=panel.querySelector('form');let changed=false;
   panel.querySelector('#connect-recheck')?.addEventListener('click',()=>{preflight=null;render();});
   if(step===3&&platform==='WOOCOMMERCE'&&!preflight&&!preflightLoading){preflightLoading=true;const storeId=store.id;request('/stores/preflight','POST',{url:store.url}).then(result=>{preflight=result;}).catch(()=>{preflight={status:'UNAVAILABLE'};}).finally(()=>{preflightLoading=false;if(step===3&&store?.id===storeId&&dialog.open&&panel.isConnected)render();});}

   form.oninput=()=>{changed=true;ctx.markDirty();};
   const leave=()=>{if(changed&&!confirm(ariaCopy.text("text.aff03d34d6b2b6")))return false;ctx.clearDirty();return true;};
   panel.querySelector('#connect-later').onclick=()=>{if(!busy&&leave())close();};
   panel.querySelector('#connect-back')?.addEventListener('click',()=>{if(busy||!leave())return;step=step===4?3:step-1;render();});
   panel.querySelector('#connect-ai-link')?.addEventListener('click',()=>{ctx.clearDirty();dialog.close();});
   form.onsubmit=async event=>{
    event.preventDefault();if(busy)return;busy=true;const submit=form.querySelector('[type=submit]');submit.disabled=true;ariaCopy.setProperty(panel.querySelector('#connect-error'),"textContent",'');
    try{
     const values=new FormData(form);
     if(step===1){platform=values.get('platform');test=platform==='WOOCOMMERCE'&&values.has('testConnection');step=2;}
     else if(step===2){name=String(values.get('name')).trim();url=String(values.get('url')||'').trim();store=await request('/stores','POST',{name,platform,url,testConnection:test});preflight=null;step=3;await window.ariaStores.mount(ctx);}
     else if(step===3){if(platform==='WOOCOMMERCE')store=await request('/stores/'+store.id+'/credentials','PUT',{version:store.version,key:String(values.get('key')).trim(),secret:String(values.get('secret')).trim()});form.querySelectorAll('input[type=password]').forEach(i=>i.value='');step=4;await window.ariaStores.mount(ctx);}
     else if(step===4){if(store.status!=='CONNECTED')await operation('CHECK',panel);else step=5;}
     else if(step===5){if(!store.lastSync)await operation('SYNC',panel);else step=6;}
     else{close();notify(platform==='ARIA_DEMO'?ariaCopy.text("text.377899c39b561c"):ariaCopy.text("text.3be65ae6f5d081"));return;}
     changed=false;ctx.clearDirty();if(panel.isConnected)render();
    }catch(error){if(panel.isConnected)ariaCopy.setProperty(panel.querySelector('#connect-error'),"textContent",error.message);}finally{busy=false;if(submit.isConnected)submit.disabled=!allow;}
   };
  }
  async function operation(operation,panel){
   const task=await request('/stores/'+store.id+'/tasks','POST',{version:store.version,operation});ctx.clearDirty();ariaCopy.setProperty(panel.querySelector('#connect-progress-message'),"textContent",ariaCopy.text("text.f66cd8cf8a063b"));const deadline=Date.now()+60000;
   while(panel.isConnected&&dialog.open&&Date.now()<deadline){await new Promise(resolve=>setTimeout(resolve,1000));if(!panel.isConnected||!dialog.open)return;const latest=await request('/stores');store=latest.stores.find(s=>s.id===store.id);if(!store)throw new Error(ariaCopy.errorText(ariaCopy.text("text.e31f73344484f9")));const state=store.tasks.find(t=>t.operation===operation&&t.generation>=task.generation);if(state?.state==='DONE'){step=operation==='CHECK'?5:6;await window.ariaStores.mount(ctx);return;}if(state&&['FAILED','RETRY','CANCELLED'].includes(state.state)){await window.ariaStores.mount(ctx);throw new Error(ariaCopy.errorText(state.lastError||store.lastError||ariaCopy.text("text.7092dbad69634c")));}}
   if(panel.isConnected)throw new Error(ariaCopy.errorText(ariaCopy.text("text.8c5db1aa4535f5")));
  }
  render();
 }
 window.ariaStoreConnectionWizard={open};
})();
