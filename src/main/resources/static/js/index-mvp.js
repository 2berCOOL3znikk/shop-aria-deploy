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
    return (p.provenance?.demonstration ? 'Учебный срок: ' : 'До ') + date.toLocaleDateString('ru-KZ', {day:'numeric',month:'short'});
  }
  function loadRating(id) {
    if (!reviewCache.has(id)) reviewCache.set(id, fetch('/api/reviews/product/' + encodeURIComponent(id)).then(r => r.ok ? r.json() : []).catch(() => []));
    reviewCache.get(id).then(reviews => {
      const valid = Array.isArray(reviews) ? reviews.filter(r => Number(r.rating)>=1 && Number(r.rating)<=5) : [];
      const node = document.querySelector('[data-rating-product="'+Number(id)+'"]');
      if (!node || !valid.length) return;
      const avg = valid.reduce((sum,r) => sum + Number(r.rating),0)/valid.length;
      node.setAttribute('aria-label','Рейтинг '+avg.toFixed(1)+' из 5, отзывов: '+valid.length);
      node.innerHTML = '<span aria-hidden="true">★</span> '+avg.toFixed(1).replace('.',',')+' <small>('+valid.length+')</small>';
      node.hidden=false;
    });
  }
  window.renderProducts = function(products) {
    currentList=products.slice();
    const grid=document.getElementById('products'); if(!grid)return;
    grid.setAttribute('aria-busy','false');
    const count=document.getElementById('catalog-result-count');if(count)count.textContent='Найдено: '+products.length;
    document.getElementById('catalog-more')?.remove();
    if(!products.length){grid.innerHTML='<div class="catalog-empty"><span aria-hidden="true">⌕</span><h3>Ничего не найдено</h3><p>Измените запрос, категорию или диапазон цен.</p><button type="button" class="btn-outline" data-reset-catalog>Сбросить фильтры</button></div>';return;}
    const visible=products.slice(0,pageSize);
    grid.innerHTML=visible.map(p=>{
      const price=p.displayPrice??p.unitPrice, reduced=Number(price)<Number(p.unitPrice), expiry=expiryInfo(p);
      const favorite=state.favoriteIds.has(Number(p.id));
      return '<article class="product-card aria-product-card" data-product-card="'+Number(p.id)+'"><a class="aria-product-art" href="/product.html?id='+Number(p.id)+'">'+CatalogImages.imageTag(p,{className:'product-card-image',alt:p.name})+'</a><div class="product-card-content"><div class="product-card-category">'+esc(p.category||'Без категории')+'</div><div class="aria-rating" data-rating-product="'+Number(p.id)+'" hidden></div><a class="product-card-name" href="/product.html?id='+Number(p.id)+'">'+esc(p.name)+'</a><div class="product-card-stock">В наличии: '+Number(p.quantityOnHand||0)+' '+esc(p.unit||'шт')+'</div>'+(expiry?'<span class="expiry-pill">'+esc(expiry)+'</span>':'')+'<div class="product-card-price">'+money(price)+(reduced?'<del class="product-card-old-price">'+money(p.unitPrice)+'</del>':'')+'</div><div class="product-card-actions">'+window.ariaCart.control(p)+'<button type="button" class="aria-favorite-button'+(favorite?' is-active':'')+'" aria-label="'+(favorite?'Удалить из избранного':'Добавить в избранное')+'" aria-pressed="'+favorite+'" onclick="toggleFavorite('+Number(p.id)+',this)">'+(favorite?'♥':'♡')+'</button></div></div></article>';
    }).join('');
    visible.forEach(p=>loadRating(p.id));
    if(products.length>pageSize){const more=document.createElement('button');more.id='catalog-more';more.className='btn-outline catalog-more';more.type='button';more.textContent='Показать ещё · '+Math.min(12,products.length-pageSize);more.onclick=()=>{pageSize+=12;window.renderProducts(currentList);};grid.after(more);}
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
    if(error){error.hidden=!invalid;error.textContent=invalid?'Проверьте диапазон: цена «От» не должна превышать «До», значения — не ниже нуля.':'';}
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
      [['impact-saved',data.savedCount],['impact-social',data.socialCount],['impact-active',data.processingCount]].forEach(([id,value])=>{const n=document.getElementById(id);if(n)n.textContent=Number.isFinite(Number(value))?String(Number(value)):'—';});
    }).catch(()=>{for(const id of ['impact-saved','impact-social','impact-active']){const n=document.getElementById(id);if(n){n.textContent='—';n.title='Данные пока недоступны';}}});
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
