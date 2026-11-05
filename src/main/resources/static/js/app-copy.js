(() => {'use strict';
 const dictionary=window.ARIA_COPY_DICTIONARY||{};
 const bySource=new Map(Object.values(dictionary).map(entry=>[entry[0],entry[1]]));
 const language=localStorage.getItem('aria_lang')==='en'?'en':'ru';
 if(!['ru','en'].includes(localStorage.getItem('aria_lang')))localStorage.setItem('aria_lang',language);
 document.documentElement.lang=language;
 const capabilities=fetch('/api/auth/capabilities',{cache:'no-store'}).then(r=>r.ok?r.json():null).catch(()=>null);
 function text(key){const entry=dictionary[key];return entry?entry[language==='en'?1:0]:'—';}
 function template(key,values){return text(key).replace(/§(\d+)§/g,(_,i)=>String(values[Number(i)]));}
 function systemMessage(value){value=String(value??'');if(language==='ru')return value;if(bySource.has(value))return bySource.get(value);
  const patterns=[[/^Просроченных партий: (\d+)\. Они исключены из доступного остатка\.$/,'Expired batches: $1. They are excluded from available stock.'],[/^Партий с критическим сроком: (\d+)\. Проверьте возможность скидки или передачи\.$/,'Batches close to expiry: $1. Review a discount or a suitable handover.'],[/^Товаров на минимуме или ниже: (\d+)\. Проверьте потребность в пополнении\.$/,'Products at or below minimum stock: $1. Review replenishment needs.']];for(const [pattern,replacement] of patterns)if(pattern.test(value))return value.replace(pattern,replacement);return value;
 }
 function errorText(value,status){value=systemMessage(value);const messages={401:['Войдите в аккаунт заново.','Please sign in again.'],403:['У вашей учётной записи нет права на это действие.','Your account cannot perform this action.'],409:['Данные изменились. Обновите страницу и проверьте значения.','Data changed. Refresh the page and review the values.'],429:['Достигнут лимит запросов. Попробуйте позже.','The request limit was reached. Try again later.'],503:['Сервис временно недоступен. Повторите позже.','The service is temporarily unavailable. Try again later.']};if(status&&messages[status]&&(language==='en'&&/[А-Яа-яЁё]/.test(value)||!value||/^(error|forbidden|unauthorized|conflict|bad request)$/i.test(value)))return messages[status][language==='en'?1:0];if(language==='ru'||!/[А-Яа-яЁё]/.test(value))return value;return 'Could not complete this operation. Review the form, refresh the data and try again.';}
 function validationMessage(value){value=String(value??'');if(/must not be (?:null|blank|empty)/i.test(value))return language==='en'?'Complete this field':'Заполните поле';if(/must be true/i.test(value))return language==='en'?'Your confirmation is required':'Требуется ваше подтверждение';if(/must match/i.test(value))return language==='en'?'Check the format':'Проверьте формат';if(language==='en'&&/[А-Яа-яЁё]/.test(value))return bySource.get(value)||'Check this value';if(language==='ru'&&/[a-z]{3}/i.test(value)&&!/[А-Яа-яЁё]/.test(value))return 'Проверьте значение, формат и ограничения поля';return value;}
 function original(escaped,label=false){escaped=String(escaped??'');if(!/[А-Яа-яЁё]/.test(escaped))return escaped;return '<span data-original-content lang="ru">'+escaped+'</span>'+(label&&language==='en'?'<small class="control-help original-language-label">Original text (Russian)</small>':'');}
 function captcha(value){if(language==='ru')return value;const match=String(value??'').match(/(\d+)\s*([+\-×*])\s*(\d+)/);return match?'What is '+match[1]+' '+match[2]+' '+match[3]+'?':systemMessage(value);}
 function setProperty(target,property,value){if(['textContent','innerText','innerHTML'].includes(property))target.removeAttribute?.('data-copy');if(target===document&&property==='title')document.querySelector('title')?.removeAttribute('data-copy');target.removeAttribute?.('data-copy-'+(property==='ariaLabel'?'aria-label':property));target[property]=value;return value;}
 function setAttribute(target,attribute,value){target.removeAttribute('data-copy-'+attribute);target.setAttribute(attribute,value);}
 function applyStatic(root=document){
  root.querySelectorAll('[data-copy]').forEach(el=>el.textContent=text(el.dataset.copy));
  for(const attribute of ['aria-label','placeholder','title','alt','data-query'])root.querySelectorAll('[data-copy-'+attribute+']').forEach(el=>el.setAttribute(attribute,text(el.getAttribute('data-copy-'+attribute))));
  root.querySelectorAll('[data-copy-language]').forEach(el=>{el.textContent=language==='en'?'EN → RU':'RU → EN';el.setAttribute('aria-label',language==='en'?'Switch to Russian':'Switch to English');});
  if(language==='en'){const manifest=document.querySelector('link[rel="manifest"]');if(manifest)manifest.href='/manifest-en.webmanifest';}
 }
 function setLanguage(next){
  next=next==='en'?'en':'ru';if(next===language)return;
  const changed=[...document.querySelectorAll('form input,form textarea,form select')].some(el=>el.type!=='hidden'&&(el.type==='checkbox'||el.type==='radio'?el.checked!==el.defaultChecked:el.tagName==='SELECT'?[...el.options].some(option=>option.selected!==option.defaultSelected):el.value!==el.defaultValue));
  if(changed&&!confirm(language==='en'?'Change language and discard unsaved form entries?':'Сменить язык без сохранения введённых данных?'))return;
  localStorage.setItem('aria_lang',next);location.reload();
 }
 window.ariaCopy={language,locale:()=>language==='en'?'en-KZ':'ru-KZ',text,template,errorText,validationMessage,systemMessage,original,captcha,applyStatic,setLanguage,setProperty,setAttribute,capabilities};
 document.addEventListener('click',event=>{if(event.target.closest('[data-copy-language]'))setLanguage(language==='en'?'ru':'en');});
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>applyStatic(),{once:true});else applyStatic();
 const updateAccessLinks=()=>capabilities.then(value=>{if(value?.publicStorefront===false)document.querySelectorAll('[data-public-store-link]').forEach(el=>el.remove());});
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',updateAccessLinks,{once:true});else updateAccessLinks();
})();
