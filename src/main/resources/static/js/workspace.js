(() => {
  'use strict';
  const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const number = v => new Intl.NumberFormat(ariaCopy.locale()).format(Number(v || 0));
  async function mountDemoCatalogue(ctx){
    const root=document.getElementById('demo-catalogue-settings');if(!root)return;
    try{
      const state=await ctx.request('/demo-catalogue');if(!root.isConnected||!state.available)return;
      root.hidden=false;
      ariaCopy.setProperty(root,"innerHTML",ariaCopy.template("template.50353b6e77f391",[number(state.references),number(state.imported)]));
      root.querySelector('button').onclick=()=>ctx.form(ariaCopy.text("text.baff1a347a18d9"),ariaCopy.template("template.18e59be7530500",[ariaCopy.original(esc(state.note),true)]),async f=>{
        if(!f.has('confirmed'))throw new Error(ariaCopy.errorText(ariaCopy.text("text.d1c81475f43ad7")));
        const result=await ctx.request('/demo-catalogue','POST',{confirmed:true});
        return ariaCopy.template("template.aa3cb5d080fb6f",[result.created.length,result.preserved.length]);
      });
    }catch(error){if(root.isConnected){root.hidden=false;ariaCopy.setProperty(root,"innerHTML",ariaCopy.text("text.971d1810cf387f"));}}
  }
  window.ariaWorkspace={mount:ctx=>ariaDashboard.mount(ctx),mountDemoCatalogue};
})();
