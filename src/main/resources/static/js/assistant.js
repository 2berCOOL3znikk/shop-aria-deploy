(() => {
  'use strict';
  function init() {
    if(document.getElementById('aria-assistant'))return;
    const launcher=document.createElement('button');launcher.id='assistant-launcher';launcher.type='button';launcher.className='assistant-launcher';launcher.setAttribute('aria-label','Открыть помощника ARIA');launcher.setAttribute('aria-expanded','false');launcher.setAttribute('aria-controls','aria-assistant');launcher.innerHTML='<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m12 2 2.7 7.3L22 12l-7.3 2.7L12 22l-2.7-7.3L2 12l7.3-2.7Z"/></svg>';
    const panel=document.createElement('section');panel.id='aria-assistant';panel.hidden=true;panel.setAttribute('role','dialog');panel.setAttribute('aria-modal','false');panel.setAttribute('aria-labelledby','assistant-title');
    panel.innerHTML='<header class="assistant-header"><span class="assistant-avatar" aria-hidden="true">✳</span><div><h2 id="assistant-title">ARIA</h2><p>Ваш помощник по магазину</p></div><button type="button" id="assistant-clear" aria-label="Очистить беседу" title="Очистить беседу">↺</button><button type="button" id="assistant-close" aria-label="Закрыть помощника">×</button></header><div class="assistant-transcript" id="assistant-transcript" role="log" aria-label="Беседа с помощником"><div class="assistant-welcome"><span>Покупать проще вместе.</span><h3>Что найдём для вас?</h3><p>Помогу выбрать из доступного каталога, сравнить товары и найти скидки.</p><div class="assistant-examples"><button type="button" data-query="Найди товары до 2000 ₸">До 2 000 ₸</button><button type="button" data-query="Что сейчас со скидкой?">Со скидкой</button><button type="button" data-query="Посоветуй недорогие продукты">На каждый день</button></div></div></div><p id="assistant-selection" hidden></p><div class="assistant-compose"><form id="assistant-form"><label class="sr-only" for="assistant-query">Сообщение помощнику</label><textarea id="assistant-query" maxlength="500" rows="2" required placeholder="Название товара или ваш вопрос…"></textarea><button type="submit" aria-label="Отправить сообщение" title="Отправить">↑</button></form><label id="assistant-consent-label"><input type="checkbox" id="assistant-consent" disabled><span>Использовать внешний AI <small>Передаются сообщение и публичные карточки товаров. Не указывайте личные сведения.</small></span></label><p id="assistant-status" role="status" aria-live="polite">Проверяем доступность AI…</p></div>';
    document.body.append(panel,launcher);
    const form=panel.querySelector('#assistant-form'),input=panel.querySelector('#assistant-query'),transcript=panel.querySelector('#assistant-transcript'),status=panel.querySelector('#assistant-status'),consent=panel.querySelector('#assistant-consent'),clear=panel.querySelector('#assistant-clear');
    let busy=false,externalAvailable=false,returnFocus=launcher;const comparison=new Map();
    function open(trigger=launcher){returnFocus=trigger;panel.hidden=false;launcher.setAttribute('aria-expanded','true');input.focus({preventScroll:true});}
    function close(){panel.hidden=true;launcher.setAttribute('aria-expanded','false');if(returnFocus?.isConnected)returnFocus.focus({preventScroll:true});}
    launcher.onclick=()=>panel.hidden?open():close();panel.querySelector('#assistant-close').onclick=close;
    document.addEventListener('keydown',event=>{if(event.key==='Escape'&&!panel.hidden){event.preventDefault();close();}});
    document.addEventListener('click',event=>{const trigger=event.target.closest('[data-open-assistant],a[href="#aria-assistant"]');if(trigger){event.preventDefault();open(trigger);}});
    function selection(){const n=panel.querySelector('#assistant-selection');n.hidden=comparison.size===0;n.replaceChildren();if(!comparison.size)return;n.append(document.createTextNode('Для сравнения: '+comparison.size+' из 5 · '));const button=document.createElement('button');button.type='button';button.textContent='Сравнить';button.disabled=busy;button.onclick=()=>{input.value='Сравни эти товары';form.requestSubmit();};n.append(button);}
    function message(text,kind){const node=document.createElement('article');node.className='assistant-message assistant-message-'+kind;const p=document.createElement('p');p.textContent=text;node.append(p);transcript.append(node);while(transcript.children.length>24)transcript.firstElementChild.remove();return node;}
    function productsIn(node,products){
      if(!Array.isArray(products)||!products.length)return;
      const list=document.createElement('div');list.className='assistant-products';
      for(const product of products){
        const card=document.createElement('article');card.className='assistant-product';
        const link=document.createElement('a');link.className='assistant-product-link';link.href='/product.html?id='+encodeURIComponent(product.id);
        if(window.CatalogImages){const image=document.createElement('img');image.src=CatalogImages.urlFor(product);image.alt='';image.loading='lazy';image.addEventListener('error',()=>{image.hidden=true;},{once:true});link.append(image);}
        const copy=document.createElement('span'),name=document.createElement('strong'),price=document.createElement('span');name.textContent=product.name;price.textContent=window.ariaMoney?ariaMoney.format(product.displayPrice):new Intl.NumberFormat('ru-KZ',{style:'currency',currency:'KZT'}).format(product.displayPrice);price.className='assistant-product-price';copy.append(name,price);link.append(copy);
        const label=document.createElement('label');label.className='assistant-compare';const checkbox=document.createElement('input');checkbox.type='checkbox';checkbox.checked=comparison.has(Number(product.id));checkbox.dataset.compareId=String(product.id);checkbox.onchange=()=>{const id=Number(product.id);if(checkbox.checked){if(comparison.size>=5){checkbox.checked=false;status.textContent='Можно сравнить до пяти товаров.';return;}comparison.set(id,product.name);}else comparison.delete(id);panel.querySelectorAll('[data-compare-id="'+id+'"]').forEach(c=>c.checked=checkbox.checked);selection();};label.append(checkbox,document.createTextNode('Сравнить'));card.append(link,label);list.append(card);
      }node.append(list);
    }
    fetch('/api/assistant/status').then(r=>r.ok?r.json():Promise.reject()).then(data=>{externalAvailable=data.externalAiAvailable===true;consent.disabled=!externalAvailable;status.textContent=externalAvailable?'По умолчанию работает подбор по каталогу. Внешний AI — только с вашим согласием.':'Внешний AI не подключён. Подбор работает по текущему каталогу.';}).catch(()=>{status.textContent='Статус AI недоступен. Можно попробовать подбор по каталогу.';});
    panel.querySelectorAll('[data-query]').forEach(button=>button.onclick=()=>{if(busy)return;input.value=button.dataset.query;form.requestSubmit();});
    clear.onclick=()=>{if(busy)return;transcript.replaceChildren();comparison.clear();selection();message('Беседа очищена. Что найдём в каталоге?','answer');input.value='';input.focus();};
    form.onsubmit=async event=>{
      event.preventDefault();const query=input.value.trim();if(busy||!query)return;
      busy=true;form.querySelector('[type=submit]').disabled=true;clear.disabled=true;input.value='';selection();
      message(query,'user');const reply=message('Проверяем каталог…','answer');reply.setAttribute('aria-busy','true');status.textContent='Подбираем доступные товары…';
      const useExternal=externalAvailable&&consent.checked;
      const abort=new AbortController(),timeout=setTimeout(()=>abort.abort(),45000);
      try{
        const response=await fetch('/api/assistant/query',{method:'POST',headers:{'Content-Type':'application/json'},signal:abort.signal,body:JSON.stringify({query,productIds:/сравн/i.test(query)?[...comparison.keys()]:[],externalAiConsent:useExternal})});
        const data=await response.json().catch(()=>({}));if(!response.ok)throw new Error(data.message||'Не удалось получить ответ. Попробуйте позже.');
        reply.replaceChildren();const source=document.createElement('span');source.className='assistant-source';source.textContent=data.source==='AI'?'Ответ ARIA AI':useExternal&&/недоступ|лимит|выключен/i.test(data.note||'')?'AI временно недоступен · подбор по каталогу':'Подбор по каталогу';reply.append(source);
        const text=document.createElement('p');text.textContent=data.text||'Уточните запрос.';reply.append(text);productsIn(reply,data.products);
        for(const item of data.links||[]){if(!['/profile.html','/support.html','/index.html'].includes(item.href))continue;const link=document.createElement('a');link.href=item.href;link.className='assistant-store-link';link.textContent=item.label;reply.append(link);}
        if(data.note){const note=document.createElement('p');note.className='assistant-note';note.textContent=data.note;reply.append(note);}
        status.textContent=data.source==='AI'?'Модель участвовала в подборе.':'Ответ подготовлен по данным каталога.';
      }catch(error){reply.replaceChildren();const text=document.createElement('p');text.textContent=error.name==='AbortError'?'Ответ задерживается. Повторите запрос чуть позже.':error.message;reply.append(text);reply.classList.add('assistant-message-error');status.textContent='Ответ не получен. Можно повторить запрос.';if(!input.value)input.value=query;}
      finally{clearTimeout(timeout);reply.removeAttribute('aria-busy');busy=false;form.querySelector('[type=submit]').disabled=false;clear.disabled=false;selection();reply.scrollIntoView({block:'nearest',behavior:'auto'});}
    };
    input.addEventListener('keydown',event=>{if(event.key==='Enter'&&!event.shiftKey&&!event.isComposing){event.preventDefault();form.requestSubmit();}});
    if(location.hash==='#aria-assistant')open();
    window.ariaAssistant={open,close};
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();