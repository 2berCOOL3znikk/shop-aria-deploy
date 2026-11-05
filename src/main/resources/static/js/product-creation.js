(() => {
  'use strict';
  const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  async function settings(ctx) {
    const s=await ctx.request('/products/ai-settings');
    ctx.form(ariaCopy.text("text.475ec45404ce06"),ariaCopy.template("template.ab09057271b15b",[s.textConsentEnabled?'checked':'',ctx.formField(ariaCopy.text("text.80f198dd699ed6"),'dailyBudget','number',s.dailyBudget,{required:true,attrs:'min="1" max="20" step="1"'}),s.budgetUsed,s.dailyBudget]),f=>ctx.request('/products/ai-settings','PUT',{revision:s.revision,textConsentEnabled:f.has('textConsentEnabled'),dailyBudget:Number(f.get('dailyBudget'))}));
  }
  async function open(ctx) {
    const s=await ctx.request('/products/ai-settings');
    ctx.show(ariaCopy.text("text.cc37c8e93871a0"),ariaCopy.template("template.e563a5fbb1e601",[!s.textConsentEnabled?ariaCopy.template("template.ad141628a7b599",[]):ariaCopy.text("text.17f00394d5a126"),!s.configured||s.remaining<1?'disabled':'',s.configured?(s.remaining<1?ariaCopy.text("text.01640ec3be632a"):ariaCopy.text("text.bcdcaebd2c1ccb")+s.remaining):ariaCopy.text("text.7701de05b867d8")]));
    const root=document.getElementById('product-start');
    root.addEventListener('input',()=>ctx.markDirty?.());
    root.querySelector('input[name=name]').focus();
    root.querySelector('#product-ai-settings').onclick=()=>settings(ctx).catch(e=>ctx.notify(e.message,true));
    const review=(draft,ai)=>{
      ctx.clearDirty?.();
      const category=draft.category||ariaCopy.text("text.cf928bcc8d5fe1");
      const content=ariaCopy.template("template.bf70c533777eef",[ai?ariaCopy.template("template.13deddbf0f96a2",[esc(draft.model)]):ariaCopy.text("text.fc0db20225f6a8"),ctx.formField(ariaCopy.text("text.fba97a9aac68ca"),'name','text',draft.name,{required:true,wide:true,attrs:'maxlength="300"'}),ctx.formField(ariaCopy.text("text.41f780fdee89f8"),'category','text',category,{required:true,wide:true,attrs:'maxlength="120" list="product-ai-categories"'}),(s.categories||[ariaCopy.text("text.cf928bcc8d5fe1"),'Дом/текстиль',ariaCopy.text("text.b892b914019fe8"),ariaCopy.text("text.0564023ef49fb8")]).map(v=>`<option value="${esc(v)}"></option>`).join(''),ai?ariaCopy.text("text.1ad80af7f47ed2"):ariaCopy.text("text.b3680f2cba5f35"),esc(draft.description||''),draft.questions?.length?ariaCopy.template("template.4936c419005914",[draft.questions.length,draft.questions.map(q=>`<li>${esc(q)}</li>`).join('')]):'',ctx.formField(ariaCopy.text("text.d5916715681c89"),'unitPrice','number','',{required:true,attrs:'min="0.01" max="9999999999.99" step="0.01"'}),ctx.formField(ariaCopy.text("text.a96102d053732d"),'unit','text',ariaCopy.text("text.a2954e5e0db4a7"),{required:true,attrs:'maxlength="20"'}),ctx.formField(ariaCopy.text("text.e7aefd3d50da92"),'minimumPrice','number','',{attrs:'min="0.01" max="9999999999.99" step="0.01"'}),ctx.formField(ariaCopy.text("text.662851fd8d1577"),'lowStockThreshold','number',5,{attrs:'min="0" max="1000000" step="1"'}),ctx.formField(ariaCopy.text("text.46ccac30adf779"),'brand','text','',{attrs:'maxlength="100"',help:ariaCopy.text("text.81eba2267aa487")}),ctx.formField(ariaCopy.text("text.42bd5305f75c1e"),'barcode','text','',{attrs:'maxlength="64"',help:ariaCopy.text("text.f40b9725517771")}),ctx.formField(ariaCopy.text("text.a2248f393691f9"),'photo','file','',{wide:true,attrs:'accept="image/jpeg,image/png"',help:ariaCopy.text("text.5d25f2cf27e1cb")}),ai?ariaCopy.text("text.ba22e2871f89fb"):'']);
      ctx.form(ariaCopy.text("text.e6e38f86335588"),content,async f=>{
        const photo=f.get('photo');
        if(photo?.size>5*1024*1024)throw new Error(ariaCopy.errorText(ariaCopy.text("text.7907e6c4b94135")));
        if(photo?.size&&!['image/jpeg','image/png'].includes(photo.type))throw new Error(ariaCopy.errorText(ariaCopy.text("text.0d5b38e671b61c")));
        const product={name:f.get('name'),category:f.get('category'),description:f.get('description'),unitPrice:f.get('unitPrice'),unit:f.get('unit'),minimumPrice:f.get('minimumPrice')||null,lowStockThreshold:Number(f.get('lowStockThreshold')||5),brand:f.get('brand')||null,barcode:f.get('barcode')||null,imageUrl:null};
        const saved=await ctx.request(ai?'/products/ai-drafts/'+draft.id+'/confirm':'/products','POST',ai?{confirmed:f.has('confirmed'),product}:product);
        let photoNote='';
        if(photo?.size){try{const body=new FormData();body.append('photo',photo);const r=await ariaAuth.authFetch('/api/admin/products/'+saved.id+'/photo',{method:'POST',body});if(!r.ok)throw new Error();}catch{photoNote=ariaCopy.text("text.db0dc310cf26a9");}}
        return ariaCopy.text("text.b9e11085b8194f")+saved.name+ariaCopy.text("text.24b5df062a15f8")+saved.sku+ariaCopy.text("text.455549dffe7d0c")+photoNote;
      });
      const submit=document.querySelector('#control-dialog [type=submit]');if(submit)ariaCopy.setProperty(submit,"textContent",ariaCopy.text("text.924fd40910ec31"));
      ctx.markDirty?.();
    };
    root.querySelector('#product-manual').onclick=()=>{if(root.reportValidity())review({name:root.elements.name.value.trim()},false);};
    root.onsubmit=async e=>{
      e.preventDefault();if(!s.textConsentEnabled&&!root.elements.consent.checked){ariaCopy.setProperty(root.querySelector('[role=alert]'),"textContent",ariaCopy.text("text.49c0044b8d30bb"));return;}
      const submit=root.querySelector('[type=submit]');submit.disabled=true;root.querySelector('#product-manual').disabled=true;ariaCopy.setProperty(root.querySelector('[role=alert]'),"textContent",'');ariaCopy.setProperty(submit,"textContent",ariaCopy.text("text.03fa734b634476"));
      try {
        if(!s.textConsentEnabled){const updated=await ctx.request('/products/ai-settings','PUT',{revision:s.revision,textConsentEnabled:true,dailyBudget:s.dailyBudget});Object.assign(s,updated);}
        const draft=await ctx.request('/products/ai-drafts','POST',{name:root.elements.name.value.trim()});
        if(root.isConnected&&document.getElementById('control-dialog').open)review(draft,true);
      } catch(error) {if(root.isConnected){ariaCopy.setProperty(root.querySelector('[role=alert]'),"textContent",error.message);submit.disabled=false;root.querySelector('#product-manual').disabled=false;ariaCopy.setProperty(submit,"textContent",ariaCopy.text("text.c4c0ce487e6b6a"));}}
    };
  }
  window.ariaProductCreation={open,settings};
})();
