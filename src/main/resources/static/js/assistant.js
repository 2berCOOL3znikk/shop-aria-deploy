(() => {
  'use strict';
  function init() {
    if(document.getElementById('aria-assistant'))return;
    const launcher=document.createElement('button');launcher.id='assistant-launcher';launcher.type='button';launcher.className='assistant-launcher';ariaCopy.setAttribute(launcher,'aria-label',ariaCopy.text("text.ccb90eec06df88"));launcher.setAttribute('aria-expanded','false');launcher.setAttribute('aria-controls','aria-assistant');ariaCopy.setProperty(launcher,"innerHTML",'<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m12 2 2.7 7.3L22 12l-7.3 2.7L12 22l-2.7-7.3L2 12l7.3-2.7Z"/></svg>');
    const panel=document.createElement('section');panel.id='aria-assistant';panel.hidden=true;panel.setAttribute('role','dialog');panel.setAttribute('aria-modal','false');panel.setAttribute('aria-labelledby','assistant-title');
    ariaCopy.setProperty(panel,"innerHTML",ariaCopy.text("text.7f4d9f46ff097e"));
    document.body.append(panel,launcher);
    const form=panel.querySelector('#assistant-form'),input=panel.querySelector('#assistant-query'),transcript=panel.querySelector('#assistant-transcript'),status=panel.querySelector('#assistant-status'),consent=panel.querySelector('#assistant-consent'),clear=panel.querySelector('#assistant-clear');
    let busy=false,externalAvailable=false,returnFocus=launcher;const comparison=new Map();
    function open(trigger=launcher){returnFocus=trigger;panel.hidden=false;launcher.setAttribute('aria-expanded','true');input.focus({preventScroll:true});}
    function close(){panel.hidden=true;launcher.setAttribute('aria-expanded','false');if(returnFocus?.isConnected)returnFocus.focus({preventScroll:true});}
    launcher.onclick=()=>panel.hidden?open():close();panel.querySelector('#assistant-close').onclick=close;
    document.addEventListener('keydown',event=>{if(event.key==='Escape'&&!panel.hidden){event.preventDefault();close();}});
    document.addEventListener('click',event=>{const trigger=event.target.closest('[data-open-assistant],a[href="#aria-assistant"]');if(trigger){event.preventDefault();open(trigger);}});
    function selection(){const n=panel.querySelector('#assistant-selection');n.hidden=comparison.size===0;n.replaceChildren();if(!comparison.size)return;n.append(document.createTextNode(ariaCopy.text("text.c1a7d9908c4db8")+comparison.size+ariaCopy.text("text.67623484e47290")));const button=document.createElement('button');button.type='button';ariaCopy.setProperty(button,"textContent",ariaCopy.text("text.83b70468cd6063"));button.disabled=busy;button.onclick=()=>{input.value=ariaCopy.text("text.7e8efe32080a50");form.requestSubmit();};n.append(button);}
    function message(text,kind){const node=document.createElement('article');node.className='assistant-message assistant-message-'+kind;const p=document.createElement('p');ariaCopy.setProperty(p,"textContent",text);node.append(p);transcript.append(node);while(transcript.children.length>24)transcript.firstElementChild.remove();return node;}
    function productsIn(node,products){
      if(!Array.isArray(products)||!products.length)return;
      const list=document.createElement('div');list.className='assistant-products';
      for(const product of products){
        const card=document.createElement('article');card.className='assistant-product';
        const link=document.createElement('a');link.className='assistant-product-link';link.href='/product.html?id='+encodeURIComponent(product.id);
        if(window.CatalogImages){const image=document.createElement('img');image.src=CatalogImages.urlFor(product);ariaCopy.setProperty(image,"alt",'');image.loading='lazy';image.addEventListener('error',()=>{image.hidden=true;},{once:true});link.append(image);}
        const copy=document.createElement('span'),name=document.createElement('strong'),price=document.createElement('span');ariaCopy.setProperty(name,"textContent",product.name);ariaCopy.setProperty(price,"textContent",window.ariaMoney?ariaMoney.format(product.displayPrice):new Intl.NumberFormat(ariaCopy.locale(),{style:'currency',currency:'KZT'}).format(product.displayPrice));price.className='assistant-product-price';copy.append(name,price);link.append(copy);
        const label=document.createElement('label');label.className='assistant-compare';const checkbox=document.createElement('input');checkbox.type='checkbox';checkbox.checked=comparison.has(Number(product.id));checkbox.dataset.compareId=String(product.id);checkbox.onchange=()=>{const id=Number(product.id);if(checkbox.checked){if(comparison.size>=5){checkbox.checked=false;ariaCopy.setProperty(status,"textContent",ariaCopy.text("text.473e3a4c176a3f"));return;}comparison.set(id,product.name);}else comparison.delete(id);panel.querySelectorAll('[data-compare-id="'+id+'"]').forEach(c=>c.checked=checkbox.checked);selection();};label.append(checkbox,document.createTextNode(ariaCopy.text("text.83b70468cd6063")));card.append(link,label);list.append(card);
      }node.append(list);
    }
    fetch('/api/assistant/status',{headers:{'Accept-Language':ariaCopy.language}}).then(r=>r.ok?r.json():Promise.reject()).then(data=>{externalAvailable=data.externalAiAvailable===true;consent.disabled=!externalAvailable;ariaCopy.setProperty(status,"textContent",externalAvailable?ariaCopy.text("text.cde3a6ef441891"):ariaCopy.text("text.7329c03d4e784a"));}).catch(()=>{ariaCopy.setProperty(status,"textContent",ariaCopy.text("text.5d98dcbd5385dc"));});
    panel.querySelectorAll('[data-query]').forEach(button=>button.onclick=()=>{if(busy)return;input.value=button.dataset.query;form.requestSubmit();});
    clear.onclick=()=>{if(busy)return;transcript.replaceChildren();comparison.clear();selection();message(ariaCopy.text("text.a917f38bad8541"),'answer');input.value='';input.focus();};
    form.onsubmit=async event=>{
      event.preventDefault();const query=input.value.trim();if(busy||!query)return;
      busy=true;form.querySelector('[type=submit]').disabled=true;clear.disabled=true;input.value='';selection();
      message(query,'user');const reply=message(ariaCopy.text("text.e182dddaf4e594"),'answer');reply.setAttribute('aria-busy','true');ariaCopy.setProperty(status,"textContent",ariaCopy.text("text.834d911cbe323d"));
      const useExternal=externalAvailable&&consent.checked;
      const abort=new AbortController(),timeout=setTimeout(()=>abort.abort(),45000);
      try{
        const response=await fetch('/api/assistant/query',{method:'POST',headers:{'Content-Type':'application/json','Accept-Language':ariaCopy.language},signal:abort.signal,body:JSON.stringify({query,productIds:/сравн|compar/i.test(query)?[...comparison.keys()]:[],externalAiConsent:useExternal})});
        const data=await response.json().catch(()=>({}));if(!response.ok)throw new Error(ariaCopy.errorText(data.message||ariaCopy.text("text.2ed6086921c515")));
        reply.replaceChildren();const source=document.createElement('span');source.className='assistant-source';ariaCopy.setProperty(source,"textContent",data.source==='AI'?ariaCopy.text("text.dcf9d19582ebf2"):useExternal&&/недоступ|лимит|выключен|unavailable|limit|off/i.test(data.note||'')?ariaCopy.text("text.be7ab2a266291a"):ariaCopy.text("text.c15f6f93e2c456"));reply.append(source);
        const text=document.createElement('p');ariaCopy.setProperty(text,"textContent",data.text||ariaCopy.text("text.ffeaf149161ecd"));reply.append(text);productsIn(reply,data.products);
        for(const item of data.links||[]){if(!['/profile.html','/support.html','/index.html'].includes(item.href))continue;const link=document.createElement('a');link.href=item.href;link.className='assistant-store-link';ariaCopy.setProperty(link,"textContent",item.label);reply.append(link);}
        if(data.note){const note=document.createElement('p');note.className='assistant-note';ariaCopy.setProperty(note,"textContent",data.note);reply.append(note);}
        ariaCopy.setProperty(status,"textContent",data.source==='AI'?ariaCopy.text("text.945fa5e7247283"):ariaCopy.text("text.4eeab102444228"));
      }catch(error){reply.replaceChildren();const text=document.createElement('p');ariaCopy.setProperty(text,"textContent",error.name==='AbortError'?ariaCopy.text("text.6fa787d462e188"):error.message);reply.append(text);reply.classList.add('assistant-message-error');ariaCopy.setProperty(status,"textContent",ariaCopy.text("text.9ea3ae91e3fb72"));if(!input.value)input.value=query;}
      finally{clearTimeout(timeout);reply.removeAttribute('aria-busy');busy=false;form.querySelector('[type=submit]').disabled=false;clear.disabled=false;selection();reply.scrollIntoView({block:'nearest',behavior:'auto'});}
    };
    input.addEventListener('keydown',event=>{if(event.key==='Enter'&&!event.shiftKey&&!event.isComposing){event.preventDefault();form.requestSubmit();}});
    if(location.hash==='#aria-assistant')open();
    window.ariaAssistant={open,close};
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();