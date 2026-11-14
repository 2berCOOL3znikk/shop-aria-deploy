(() => {
 'use strict';
 async function open(ctx,id){
  const d=await ctx.request('/products/'+id+'/attributes'),textile=/текстил|одеж|textile|clothing|apparel/i.test(d.category||''),t=(ru,en)=>ariaCopy.language==='en'?en:ru;
  ctx.form(ariaCopy.text("text.4449aae9be2d69")+' · '+d.name,'<p class="control-help wide">'+ariaCopy.text("text.ab0ce5fc66b90c")+'</p>'+(textile?ctx.formField(ariaCopy.text("text.1f6d3746b8b8b9"),'color','text',d.color||'',{attrs:'maxlength="50"'}):'')+ctx.formField(textile?t('Размер','Size'):t('Упаковка с этикетки','Pack as labelled'),'size','text',d.size||'',{attrs:'maxlength="50"'})+ctx.formField(ariaCopy.text("text.e4b270e5a0a255"),'weightKg','number',d.weightKg??'',{attrs:'min="0.01" max="999999.99" step="0.01"'})+ctx.formField(ariaCopy.text("text.d373e048684d19"),'reason','text','',{required:true,wide:true,attrs:'maxlength="700"',help:ariaCopy.text("text.3222b13acadecf")})+'<p class="control-help wide">'+ariaCopy.text("text.508c4a67b0f363")+'</p><label class="pc-consent wide"><input name="confirmed" type="checkbox" required><span>'+ariaCopy.text("text.ea8c11dcdfaad5")+'</span></label>',async f=>{
   await ctx.request('/products/'+id+'/attributes','PUT',{revision:d.revision,color:textile?f.get('color')||null:d.color,size:f.get('size')||null,weightKg:f.get('weightKg')||null,confirmed:f.has('confirmed'),reason:f.get('reason')});return ariaCopy.text("text.eef6886bd7b2d6");
  });
 }
 window.ariaProductAttributes={open};
})();
