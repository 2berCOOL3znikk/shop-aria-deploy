(() => {
  'use strict';
  const {t,lang}=ariaSocialCopy,esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  let verificationStatus='NOT_SUBMITTED',offers=[],claims=[],offersError='',claimsError='',loading=false;
  const pending=new Set();
  const date=v=>v?new Date(v+'T12:00:00').toLocaleDateString(lang()==='en'?'en-GB':'ru-KZ'):t('notSpecified');
  const errorText=(status,fallback)=>t(({401:'unauthorized',403:'forbidden',409:'conflict',429:'unavailable',503:'unavailable'})[status]||fallback);
  async function request(url,options={}){let r;try{r=await ariaAuth.authFetch(url,options);}catch(_){throw new Error(t('unavailable'));}const body=await r.json().catch(()=>({}));if(!r.ok)throw new Error(errorText(r.status,'requestError'));return body;}
  function paintCopy(){
    document.querySelectorAll('[data-social-key]').forEach(el=>el.textContent=t(el.dataset.socialKey));
    document.title=t('pageTitle');
    const admin=ariaAuth.getRole()==='ADMIN',org=ariaAuth.getRole()==='ORG',cta=document.getElementById('social-access-link');
    cta.hidden=org;cta.href=admin?'/control.html#socialPlans':'/login.html';cta.textContent=t(admin?'manage':'signIn');
    document.getElementById('partner-message').textContent=t(org?verificationStatus:'access');
    document.getElementById('social-help').textContent=t(admin?'adminHelp':org&&verificationStatus==='VERIFIED'?'verifiedHelp':'unverifiedHelp');
    const status=document.getElementById('org-verification-status');if(status)status.textContent=t(verificationStatus);
  }
  function ensureVerificationPanel(){
    if(ariaAuth.getRole()!=='ORG'||document.getElementById('org-verification-panel'))return;
    const panel=document.createElement('section');panel.id='org-verification-panel';panel.className='glass-card org-verification-panel';panel.setAttribute('data-i18n-skip','');
    panel.innerHTML=`<h2 data-social-key="verification">${t('verification')}</h2><p id="org-verification-status" role="status"></p><form id="org-verification-form"><label for="org-verification-name" data-social-key="orgName">${t('orgName')}</label><input id="org-verification-name" name="organizationName" required maxlength="160" autocomplete="organization"><label for="org-registration-number" data-social-key="registration">${t('registration')}</label><input id="org-registration-number" name="registrationNumber" required maxlength="100"><p class="social-error" role="alert"></p><button class="btn-glow" type="submit" data-social-key="submit">${t('submit')}</button></form>`;
    document.querySelector('.social-hero').after(panel);
    panel.querySelector('form').onsubmit=async event=>{
      event.preventDefault();const form=event.currentTarget,button=form.querySelector('[type=submit]');if(button.disabled)return;button.disabled=true;button.textContent=t('sending');form.querySelector('[role=alert]').textContent='';
      try{await request('/api/organizations/verification',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({organizationName:form.organizationName.value.trim(),registrationNumber:form.registrationNumber.value.trim()})});showToast(t('submitted'));await loadVerification();paintOffers();}catch(error){form.querySelector('[role=alert]').textContent=error.message;}finally{button.disabled=false;button.textContent=t('submit');}
    };
  }
  async function loadVerification(){
    if(ariaAuth.getRole()!=='ORG'){paintCopy();return;}
    ensureVerificationPanel();try{const result=await request('/api/organizations/verification');verificationStatus=['NOT_SUBMITTED','PENDING_VERIFICATION','VERIFIED','REJECTED'].includes(result.status)?result.status:'UNKNOWN';if(result.organizationName)document.getElementById('org-verification-name').value=result.organizationName;}catch(_){verificationStatus='UNKNOWN';}
    document.getElementById('org-verification-form').hidden=['PENDING_VERIFICATION','VERIFIED','UNKNOWN'].includes(verificationStatus);document.getElementById('org-verification-panel').hidden=verificationStatus==='VERIFIED';paintCopy();
  }
  function offerCard(offer){
    const urgent=offer.expiryDate&&Math.ceil((new Date(offer.expiryDate+'T23:59:59+05:00')-new Date())/86400000)<=3,org=ariaAuth.getRole()==='ORG';
    const action=org&&verificationStatus==='VERIFIED'?`<button class="btn-glow" type="button" data-claim="${offer.id}" ${pending.has(offer.id)?'disabled':''}>${t(pending.has(offer.id)?'claiming':'claim')} · ${Number(offer.quantityAvailable)} ${t('units')}</button>`:`<p class="partner-access-note">${t(org?'onlyVerified':'access')}</p>`;
    return `<article class="offer-card">${CatalogImages.imageTag({productName:offer.productName,productSku:offer.productSku,imageUrl:offer.imageUrl},{alt:offer.productName})}<div class="offer-body"><div class="product-card-category">${t(urgent?'urgent':'offer')}</div><h3 class="offer-title" data-merchant-content>${esc(offer.productName)}</h3>${lang()==='en'&&/[А-Яа-яЁё]/.test(offer.productName)?`<small class="social-original">${t('original')}</small>`:''}<dl class="offer-meta"><div><dt>${t('quantity')}</dt><dd>${Number(offer.quantityAvailable)} ${t('units')}</dd></div><div><dt>${t('offerUntil')}</dt><dd>${date(offer.validUntil)}</dd></div><div><dt>${t('expiry')}</dt><dd>${date(offer.expiryDate)}</dd></div></dl>${offer.ariaReason?`<details class="offer-reason"><summary>${t('reason')}${lang()==='en'&&/[А-Яа-яЁё]/.test(offer.ariaReason)?' · '+t('original'):''}</summary><p data-merchant-content>${esc(offer.ariaReason)}</p></details>`:''}<div class="offer-price">${t('free')}</div>${action}</div></article>`;
  }
  function paintOffers(){
    const grid=document.getElementById('grid');grid.setAttribute('aria-busy',String(loading));
    grid.innerHTML=loading?`<p role="status">${t('loading')}</p>`:offersError?`<div class="empty-state glass-card" role="alert"><h3>${t(offersError)}</h3><button type="button" class="btn-outline" data-social-retry>${t('refresh')}</button></div>`:offers.length?offers.map(offerCard).join(''):`<div class="empty-state glass-card"><span class="social-empty-mark" aria-hidden="true">↗</span><h3>${t('empty')}</h3><p>${t('emptyNote')}</p></div>`;
    document.getElementById('social-offer-count').textContent=loading||offersError?'':String(offers.length);
    grid.querySelector('[data-social-retry]')?.addEventListener('click',loadOffers);
  }
  async function loadOffers(){if(loading)return;loading=true;offersError='';paintOffers();try{const response=await (ariaAuth.getToken()?ariaAuth.authFetch('/api/social/offers'):fetch('/api/social/offers'));if(!response.ok)throw new Error();offers=await response.json();offers.sort((a,b)=>(a.expiryDate||'9999-12-31').localeCompare(b.expiryDate||'9999-12-31'));}catch(_){offersError='offersError';}finally{loading=false;paintOffers();}}
  function paintClaims(){
    const section=document.getElementById('claims-section');section.hidden=ariaAuth.getRole()!=='ORG'||verificationStatus!=='VERIFIED';if(section.hidden)return;
    const heading=document.getElementById('social-offers-heading');if(heading&&section.nextElementSibling!==heading)heading.before(section);
    document.getElementById('claims-list').innerHTML=claimsError?`<p role="alert">${t('claimsError')}</p>`:claims.length?claims.map(c=>`<article class="claim-row"><div><strong data-merchant-content>${esc(c.productName)}</strong><p>${t(c.status==='TRANSFERRED'?'transferred':'reserved')} · ${Number(c.claimedQuantity||0)} ${t('units')}</p></div><button class="btn-outline" type="button" data-act="${c.id}">${t('act')}</button></article>`).join(''):`<p>${t('claimsEmpty')}</p>`;
  }
  async function loadClaims(){if(ariaAuth.getRole()!=='ORG'||verificationStatus!=='VERIFIED'){claims=[];paintClaims();return;}try{claims=await request('/api/social/my-claims');claimsError='';}catch(_){claimsError='claimsError';}paintClaims();}
  async function claimOffer(id){if(ariaAuth.getRole()!=='ORG'||verificationStatus!=='VERIFIED'||pending.has(id))return;pending.add(id);paintOffers();try{await request('/api/organizations/offers/'+id+'/claim',{method:'POST'});showToast(t('claimed'));await loadOffers();await loadClaims();}catch(error){showToast(error.message,'error');}finally{pending.delete(id);paintOffers();}}
  async function downloadClaimAct(id){const b=document.querySelector(`[data-act="${id}"]`);if(b?.disabled)return;if(b)b.disabled=true;try{const r=await ariaAuth.authFetch('/api/social/my-claims/'+id+'/act');if(!r.ok)throw new Error(t('actError'));const url=URL.createObjectURL(await r.blob()),a=document.createElement('a');a.href=url;a.download='aria-social-act-'+id+'.html';a.click();setTimeout(()=>URL.revokeObjectURL(url),30000);}catch(_){showToast(t('actError'),'error');}finally{if(b)b.disabled=false;}}
  window.claimOffer=claimOffer;window.downloadClaimAct=downloadClaimAct;window.loadSocial=loadOffers;window.loadClaims=loadClaims;window.renderOffer=offerCard;
  window.addEventListener('aria:language-changed',()=>{paintCopy();paintOffers();paintClaims();});
  document.addEventListener('DOMContentLoaded',async()=>{
    paintCopy();document.getElementById('social-refresh').onclick=async()=>{const b=document.getElementById('social-refresh');b.disabled=true;try{await loadVerification();await loadOffers();await loadClaims();}finally{b.disabled=false;}};
    document.getElementById('grid').onclick=e=>{const b=e.target.closest('[data-claim]');if(b&&!b.disabled)claimOffer(Number(b.dataset.claim));};document.getElementById('claims-list').onclick=e=>{const b=e.target.closest('[data-act]');if(b&&!b.disabled)downloadClaimAct(Number(b.dataset.act));};
    await loadVerification();await loadOffers();await loadClaims();
    try{const response=await (ariaAuth.getToken()?ariaAuth.authFetch('/api/products/store-info'):fetch('/api/products/store-info'));if(!response.ok)throw Error();const info=await response.json();document.getElementById('social-demo-note').hidden=!info.demonstration;}catch(_){}
  });
})();
