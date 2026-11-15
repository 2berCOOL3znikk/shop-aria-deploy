(()=>{'use strict';
 const t=(ru,en)=>ariaCopy.language==='en'?en:ru,esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 function collect(rows){const values=[];for(const row of rows||[]){if(typeof row.category==='string')values.push(row.category);for(const value of Array.isArray(row.categories)?row.categories:[])values.push(typeof value==='string'?value:value?.name);}
  return [...new Set(values.filter(v=>typeof v==='string'&&v.trim()&&v.trim()!=='*').map(v=>v.trim()))].sort((a,b)=>a.localeCompare(b,ariaCopy.locale()));
 }
 function render({rows=[],selected=[],name='categories',native=false,label=null,hint=null}={}){
  const key=v=>native?String(v).trim().toLowerCase():String(v).trim(),available=collect(rows),options=new Map(available.map(v=>[key(v),{label:v,saved:false}]));
  const chosen=new Set(selected.map(key));for(const value of chosen)if(!options.has(value))options.set(value,{label:value,saved:true});
  return `<fieldset class="category-picker wide ax-wide" data-category-picker="${esc(name)}"><legend>${label==null?t('Категории для автоматических скидок','Categories for automatic price changes'):esc(label)}</legend><p>${hint==null?t('Выберите из каталога. Без выбора автоматические изменения запрещены.','Choose from your catalogue. No selection means no automatic price changes.'):esc(hint)}</p><div class="category-picker-options">${[...options].map(([value,item])=>`<label><input type="checkbox" name="${esc(name)}" value="${esc(value)}" ${chosen.has(value)?'checked':''}><span data-merchant-content>${esc(item.label)}${item.saved?`<small>${t('Сохранено ранее · сейчас нет в каталоге','Previously saved · not in the current catalogue')}</small>`:''}</span></label>`).join('')||`<p>${t('В каталоге пока нет категорий. Сначала добавьте или импортируйте товары.','No catalogue categories yet. Add or import products first.')}</p>`}</div></fieldset>`;
 }
 function read(form,name='categories'){return [...new Set(form.getAll(name).map(String).map(v=>v.trim()).filter(Boolean))];}
 const style=document.createElement('link');style.rel='stylesheet';style.href='/css/category-picker.css';document.head.append(style);
 window.ariaCategoryPicker={collect,render,read};
})();
