(function () {
  'use strict';
  const esc = value => String(value ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
  const money = value => window.ariaMoney.format(value);
  const reviewCache = new Map();
  let pageSize = 12, currentList = [];
  const queryParams = () => new URLSearchParams(location.search);
  const numberField = id => { const node = document.getElementById(id); return node && node.value !== '' ? Number(node.value) : null; };

  function expiryInfo(p) {
    if (!p.expiryDate) return '';
    const date = new Date(p.expiryDate + 'T12:00:00');
    if (!Number.isFinite(date.getTime())) return '';
    return (p.provenance?.demonstration ? ariaCopy.text("text.44cab2c96f5fff") : ariaCopy.text("text.5a347316dd3625")) + date.toLocaleDateString(ariaCopy.locale(), {day:'numeric',month:'short'});
  }
  function loadRating(id) {
    if (!reviewCache.has(id)) reviewCache.set(id, fetch('/api/reviews/product/' + encodeURIComponent(id)).then(r => r.ok ? r.json() : []).catch(() => []));
    reviewCache.get(id).then(reviews => {
      const valid = Array.isArray(reviews) ? reviews.filter(r => Number(r.rating)>=1 && Number(r.rating)<=5) : [];
      const node = document.querySelector('[data-rating-product="'+Number(id)+'"]');
      if (!node || !valid.length) return;
      const avg = valid.reduce((sum,r) => sum + Number(r.rating),0)/valid.length;
      ariaCopy.setAttribute(node,'aria-label',ariaCopy.text("text.07a3cb939d930d")+avg.toFixed(1)+ariaCopy.text("text.4ea929ff8e18ef")+valid.length);
      ariaCopy.setProperty(node,"innerHTML",'<span aria-hidden="true">★</span> '+avg.toFixed(1).replace('.',',')+' <small>('+valid.length+')</small>');
      node.hidden=false;
    });
  }
  window.renderProducts = function(products) {
    currentList=products.slice();
    const grid=document.getElementById('products'); if(!grid)return;
    grid.setAttribute('aria-busy','false');
    const count=document.getElementById('catalog-result-count');if(count)ariaCopy.setProperty(count,"textContent",ariaCopy.text("text.1bee7400e81b03")+products.length);
    document.getElementById('catalog-more')?.remove();
    if(!products.length){ariaCopy.setProperty(grid,"innerHTML",ariaCopy.text("text.797df1f059efa1"));return;}
    const visible=products.slice(0,pageSize);
    ariaCopy.setProperty(grid,"innerHTML",visible.map(p=>{
      const price=p.displayPrice??p.unitPrice, reduced=Number(price)<Number(p.unitPrice), expiry=expiryInfo(p);
      const favorite=state.favoriteIds.has(Number(p.id));
      return '<article class="product-card aria-product-card" data-product-card="'+Number(p.id)+'"><a class="aria-product-art" href="/product.html?id='+Number(p.id)+'">'+CatalogImages.imageTag(p,{className:'product-card-image',alt:p.name})+'</a><div class="product-card-content"><div class="product-card-category">'+esc(p.category||ariaCopy.text("text.c1952d18056a9d"))+'</div><div class="aria-rating" data-rating-product="'+Number(p.id)+'" hidden></div><a class="product-card-name" href="/product.html?id='+Number(p.id)+'">'+esc(p.name)+ariaCopy.text("text.188b8bca754dd5")+Number(p.quantityOnHand||0)+' '+esc(ariaCopy.systemMessage(p.unit||ariaCopy.text("text.a2954e5e0db4a7")))+'</div>'+(expiry?'<span class="expiry-pill">'+esc(expiry)+'</span>':'')+'<div class="product-card-price">'+money(price)+(reduced?'<del class="product-card-old-price">'+money(p.unitPrice)+'</del>':'')+'</div><div class="product-card-actions">'+window.ariaCart.control(p)+'<button type="button" class="aria-favorite-button'+(favorite?' is-active':'')+'" aria-label="'+(favorite?ariaCopy.text("text.7658dd5450ed58"):ariaCopy.text("text.2a6b5f7278c0d6"))+'" aria-pressed="'+favorite+'" onclick="toggleFavorite('+Number(p.id)+',this)">'+(favorite?'♥':'♡')+'</button></div></div></article>';
    }).join(''));
    visible.forEach(p=>loadRating(p.id));
    if(products.length>pageSize){const more=document.createElement('button');more.id='catalog-more';more.className='btn-outline catalog-more';more.type='button';ariaCopy.setProperty(more,"textContent",ariaCopy.text("text.2b95a0709b6351")+Math.min(12,products.length-pageSize));more.onclick=()=>{pageSize+=12;window.renderProducts(currentList);};grid.after(more);}
  };

  function persistFilters(category) {
    const params=new URLSearchParams();
    const values={q:document.getElementById('search')?.value.trim(),category,sort:document.getElementById('sort')?.value,min:document.getElementById('price-min')?.value,max:document.getElementById('price-max')?.value,sale:document.getElementById('discount-only')?.checked?'1':''};
    Object.entries(values).forEach(([key,value])=>{if(value && !(key==='sort'&&value==='default'))params.set(key,value);});
    const query=params.toString();history.replaceState(null,'',location.pathname+(query?'?'+query:'')+location.hash);
  }
  window.filterAndSort = function() {
    const query=(document.getElementById('search')?.value||'').trim().toLocaleLowerCase('ru').replaceAll('ё','е');
    const category=document.querySelector('#category-chips .active')?.dataset.category||'';
    const sort=document.getElementById('sort')?.value||'default', min=numberField('price-min'), max=numberField('price-max'), sale=document.getElementById('discount-only')?.checked;
    const error=document.getElementById('catalog-filter-error');
    const invalid=(min!==null&&(!Number.isFinite(min)||min<0))||(max!==null&&(!Number.isFinite(max)||max<0))||(min!==null&&max!==null&&min>max);
    if(error){error.hidden=!invalid;ariaCopy.setProperty(error,"textContent",invalid?ariaCopy.text("text.44fc16d6d7816e"):'');}
    persistFilters(category);
    let filtered=invalid?[]:state.products.filter(p=>{
      const text=[p.name,p.sku,p.description,p.category,p.brand].join(' ').toLocaleLowerCase('ru').replaceAll('ё','е'), price=Number(p.displayPrice??p.unitPrice);
      return(!query||text.includes(query))&&(!category||p.category===category)&&(min===null||price>=min)&&(max===null||price<=max)&&(!sale||Number(p.discountPercent)>0);
    });
    if(sort==='price-asc')filtered.sort((a,b)=>Number(a.displayPrice??a.unitPrice)-Number(b.displayPrice??b.unitPrice));
    else if(sort==='price-desc')filtered.sort((a,b)=>Number(b.displayPrice??b.unitPrice)-Number(a.displayPrice??a.unitPrice));
    else if(sort==='expiry-asc')filtered.sort((a,b)=>(a.expiryDate||'9999-12-31').localeCompare(b.expiryDate||'9999-12-31'));
    else if(sort==='popular')filtered.sort((a,b)=>Number(b.popularityRank||0)-Number(a.popularityRank||0));
    else filtered.sort((a,b)=>(a.provenance?.photo?-1:a.provenance?.catalogueOrder??999999)-(b.provenance?.photo?-1:b.provenance?.catalogueOrder??999999)||a.name.localeCompare(b.name,'ru'));
    pageSize=12;window.renderProducts(filtered);
  };
  function resetFilters(){
    for(const id of ['search','price-min','price-max']){const node=document.getElementById(id);if(node)node.value='';}
    const sale=document.getElementById('discount-only');if(sale)sale.checked=false;
    document.getElementById('sort').value='default';
    document.querySelectorAll('#category-chips button').forEach(b=>{const active=!b.dataset.category;b.classList.toggle('active',active);b.setAttribute('aria-pressed',String(active));});
    window.filterAndSort();document.getElementById('search')?.focus({preventScroll:true});
  }
  window.updateImpactStats=function(){
    fetch('/api/products/impact/positions').then(r=>r.ok?r.json():Promise.reject()).then(data=>{
      [['impact-saved',data.savedCount],['impact-social',data.socialCount],['impact-active',data.processingCount]].forEach(([id,value])=>{const n=document.getElementById(id);if(n)ariaCopy.setProperty(n,"textContent",Number.isFinite(Number(value))?String(Number(value)):'—');});
    }).catch(()=>{for(const id of ['impact-saved','impact-social','impact-active']){const n=document.getElementById(id);if(n){ariaCopy.setProperty(n,"textContent",'—');ariaCopy.setProperty(n,"title",ariaCopy.text("text.ed05d91511d964"));}}});
  };
  document.addEventListener('DOMContentLoaded',()=>{
    const params=queryParams();
    for(const [id,key]of[['search','q'],['price-min','min'],['price-max','max']]){const n=document.getElementById(id);if(n)n.value=(params.get(key)||'').slice(0,id==='search'?200:20);}
    const sort=document.getElementById('sort');if(sort&&[...sort.options].some(o=>o.value===params.get('sort')))sort.value=params.get('sort');
    const sale=document.getElementById('discount-only');if(sale)sale.checked=params.get('sale')==='1';
    let timer;for(const id of ['search','price-min','price-max'])document.getElementById(id)?.addEventListener('input',()=>{clearTimeout(timer);timer=setTimeout(window.filterAndSort,200);});
    sale?.addEventListener('change',window.filterAndSort);document.getElementById('catalog-reset')?.addEventListener('click',resetFilters);
    document.addEventListener('click',event=>{if(event.target.closest('[data-reset-catalog]'))resetFilters();});
    window.updateImpactStats();
    fetch('/api/products/store-info').then(r=>r.ok?r.json():Promise.reject()).then(info=>{window.ariaStoreInfo=info;const banner=document.getElementById('storefront-demonstration');if(banner)banner.hidden=!info.demonstration;}).catch(()=>{});
  });
})();
