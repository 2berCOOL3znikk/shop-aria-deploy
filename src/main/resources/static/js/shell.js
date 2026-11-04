(function () {
  "use strict";

  const labels = {
    ru: { write: "Написать нам", catalog: "Каталог", social: "Соц. полка", favorites: "♡ Избранное", cart: "Корзина", profile: "Профиль", admin: "Управление", org: "Кабинет организации", support: "Поддержка", login: "Войти", logout: "Выйти", project: "Проект", about: "О проекте", how: "Как это работает", privacy: "Конфиденциальность", contact: "Связаться", faq: "FAQ", telegram: "Telegram", mission: "Соц. миссия", funds: "Помощь фондам и приютам" },
    en: { write: "Write to us", catalog: "Catalog", social: "Social shelf", favorites: "♡ Favorites", cart: "Cart", profile: "Profile", admin: "Management", org: "Organization cabinet", support: "Support", login: "Sign in", logout: "Sign out", project: "Project", about: "About", how: "How it works", privacy: "Privacy", contact: "Contact", faq: "FAQ", telegram: "Telegram", mission: "Social mission", funds: "Support charities and shelters" },
    kk: { write: "Бізге жазыңыз", catalog: "Каталог", social: "Әлеуметтік сөре", favorites: "♡ Таңдаулылар", cart: "Себет", profile: "Профиль", admin: "Басқару", org: "Ұйым кабинеті", support: "Қолдау", login: "Кіру", logout: "Шығу", project: "Жоба", about: "Жоба туралы", how: "Қалай жұмыс істейді", privacy: "Құпиялылық", contact: "Байланыс", faq: "FAQ", telegram: "Telegram", mission: "Әлеуметтік миссия", funds: "Қорлар мен панажайларға көмек" }
  };

  const langCode = () => window.ariaI18n && window.ariaI18n.getLang ? window.ariaI18n.getLang() : "ru";
  const t = key => (labels[langCode()] || labels.ru)[key] || labels.ru[key] || key;
  const setNavLabel = link => {
    const badge = link.querySelector(".shell-cart-count");
    link.textContent = t(link.dataset.shellLabel);
    if (badge) link.appendChild(badge);
  };

  function makeLink(href, text, key) {
    const a = document.createElement("a");
    a.href = href;
    a.textContent = text;
    if (key) a.dataset.shellLabel = key;
    if (location.pathname.toLowerCase().endsWith(href.split("#")[0].toLowerCase())) a.classList.add("active");
    return a;
  }

  function refreshLanguage() {
    const activeNav = document.getElementById("aria-shell-links");
    const footer = document.getElementById("aria-shell-footer");
    if (activeNav) activeNav.querySelectorAll("[data-shell-label]").forEach(setNavLabel);
    if (footer) footer.querySelectorAll("[data-shell-footer]").forEach(el => el.textContent = t(el.dataset.shellFooter));
    const languageButton = document.getElementById("lang-toggle-btn");
    if (languageButton) languageButton.textContent = (langCode() || "ru").toUpperCase();
  }

  function render() {
    if (!document.body || document.body.dataset.ariaShell === "rendered") return;
    document.body.dataset.ariaShell = "rendered";

    const oldNav = document.querySelector("body > nav.nav-glass");
    const nav = document.createElement("nav");
    nav.className = "nav-glass aria-shell";
    nav.setAttribute("aria-label", "ARIA");

    const brand = document.createElement("a");
    brand.href = "/index.html";
    brand.className = "logo-new";
    brand.innerHTML = '<span class="aria-brand-mark">◆</span> <span class="gradient-text">ARIA</span> <span class="aria-brand-shop">Shop</span>';
    nav.appendChild(brand);

    const links = document.createElement("div");
    links.className = "nav-links aria-shell-links";
    links.id = "aria-shell-links";

    const role = window.ariaAuth && window.ariaAuth.getRole ? window.ariaAuth.getRole() : null;
    const cartLink = makeLink("/cart.html", t("cart"), "cart");
    const cartBadge = document.createElement("span");
    cartBadge.id = "cart-count";
    cartBadge.className = "shell-cart-count";
    cartBadge.textContent = "0";
    cartBadge.hidden = true;
    cartBadge.setAttribute("aria-label", "Товаров в корзине: 0");
    cartLink.appendChild(cartBadge);

    [makeLink("/index.html", t("catalog"), "catalog"),
      makeLink("/social.html", t("social"), "social"),
      makeLink("/favorites.html", t("favorites"), "favorites"),
      cartLink,
      makeLink("/profile.html", t("profile"), "profile")].forEach(link => links.appendChild(link));

    if (role === "ORG") links.appendChild(makeLink("/social.html#claims-section", t("org"), "org"));
    if (role === "ADMIN") links.appendChild(makeLink("/control.html", t("admin"), "admin"));
    links.appendChild(makeLink("/support.html", t("support"), "support"));

    nav.appendChild(links);

    const actions = document.createElement("div");
    actions.className = "aria-shell-actions";

    const theme = document.createElement("button");
    theme.type = "button";
    theme.id = "theme-toggle-btn";
    theme.className = "shell-icon-button";
    theme.setAttribute("aria-label", "Сменить тему");
    theme.addEventListener("click", () => window.ariaTheme && window.ariaTheme.toggleTheme());
    actions.appendChild(theme);

    const lang = document.createElement("button");
    lang.type = "button";
    lang.id = "lang-toggle-btn";
    lang.className = "shell-lang-button";
    lang.addEventListener("click", () => {
      if (window.ariaI18n) window.ariaI18n.cycleLang();
      refreshLanguage();
    });
    actions.appendChild(lang);

    const user = document.createElement("div");
    user.id = "auth-nav";
    user.className = "aria-shell-user";

    if (window.ariaAuth && window.ariaAuth.isLoggedIn()) {
      const email = document.createElement("span");
      email.className = "shell-user-email";
      email.textContent = window.ariaAuth.getUserEmail() || "";
      user.appendChild(email);

      const logout = document.createElement("button");
      logout.type = "button";
      logout.className = "shell-user-button";
      logout.textContent = t("logout");
      logout.addEventListener("click", () => window.ariaAuth.logout());
      user.appendChild(logout);
    } else {
      user.appendChild(makeLink("/login.html", t("login"), "login"));
    }

    actions.appendChild(user);
    nav.appendChild(actions);

    const burger = document.createElement("button");
    burger.type = "button";
    burger.className = "aria-shell-burger";
    burger.setAttribute("aria-label", "Открыть меню");
    burger.setAttribute("aria-expanded", "false");
    burger.innerHTML = "<span></span><span></span><span></span>";
    burger.addEventListener("click", () => {
      const open = nav.classList.toggle("menu-open");
      burger.setAttribute("aria-expanded", String(open));
    });
    nav.insertBefore(burger, links);

    if (oldNav) oldNav.remove();
    document.body.insertBefore(nav, document.body.firstChild);

    const toastBox = document.getElementById("toast-container") || document.createElement("div");
    toastBox.id = "toast-container";
    toastBox.classList.add("toast-container");
    toastBox.setAttribute("aria-live", "polite");
    toastBox.setAttribute("aria-atomic", "false");
    if (!toastBox.isConnected) document.body.appendChild(toastBox);

    document.querySelectorAll("body > footer:not(#aria-shell-footer)").forEach(el => el.remove());

    let footer = document.getElementById("aria-shell-footer");
    if (!footer) {
      footer = document.createElement("footer");
      footer.id = "aria-shell-footer";
      footer.className = "aria-shell-footer";
      footer.innerHTML = '<div class="shell-footer-columns"><section><h2 data-shell-footer="project"></h2><a href="/about.html" data-shell-footer="about"></a><a href="/about.html#flow" data-shell-footer="how"></a><a href="/privacy.html" data-shell-footer="privacy"></a></section><section><h2 data-shell-footer="support"></h2><a href="/support.html" data-shell-footer="contact"></a><a href="/support.html#faq" data-shell-footer="faq"></a><a href="/support.html#feedback-form" data-shell-footer="write"></a></section><section><h2 data-shell-footer="mission"></h2><a href="/social.html" data-shell-footer="funds"></a></section></div><p class="shell-copyright">© 2026 ARIA · Smart Retail & Social Impact · Казахстан</p>';
      document.body.appendChild(footer);
    }

    footer.querySelectorAll("[data-shell-footer]").forEach(el => el.textContent = t(el.dataset.shellFooter));
    links.querySelectorAll("[data-shell-label]").forEach(setNavLabel);

    theme.textContent = window.ariaTheme && window.ariaTheme.getTheme() === "dark" ? "☀" : "🌙";
    theme.title = window.ariaTheme && window.ariaTheme.getTheme() === "dark" ? "Светлая тема" : "Тёмная тема";
    lang.textContent = (langCode() || "ru").toUpperCase();
    window.refreshCartBadge();
  }

  window.refreshCartBadge = async function () {
    if (window.ariaCart) {
      await window.ariaCart.refresh().catch(() => {});
      return window.ariaCart.snapshot().length;
    }
    const badge = document.getElementById("cart-count");
    if (!badge) return 0;
    if (!window.ariaAuth || !window.ariaAuth.isLoggedIn()) {
      badge.textContent = "0";
      badge.setAttribute("aria-label", "Товаров в корзине: 0");
      badge.hidden = true;
      return 0;
    }
    try {
      const response = await window.ariaAuth.authFetch("/api/cart/total");
      if (!response.ok) throw new Error("Не удалось обновить корзину");
      const data = await response.json();
      const count = Math.max(0, Number(data.itemCount) || 0);
      badge.textContent = count > 99 ? "99+" : String(count);
      badge.setAttribute("aria-label", "Товаров в корзине: " + count);
      badge.hidden = count === 0;
      return count;
    } catch (_) {
      return Number(badge.textContent) || 0;
    }
  };

  // === НОВЫЙ SHOWTOAST С РАЗЛИЧИЕМ ТИПОВ И ПЛАВНЫМИ АНИМАЦИЯМИ ===
  window.showToast = function (message, type) {
    message = String(message ?? '');

    // Автоматически определяем тип по тексту сообщения
    if (!type || type === 'success') {
      if (/удал|убран|removed|dismiss|снят|отмен|очищ/i.test(message)) type = 'remove';
      else if (/ошибк|error|не удалось|запрещ|доступ|403|401|500|пуст/i.test(message)) type = 'error';
      else if (/важно|вниман|warning|осторож|критич|срочн/i.test(message)) type = 'warning';
      else if (/инфо|info|справк|обнов/i.test(message)) type = 'info';
      else type = 'success';
    }

    const ICONS = { success: "✓", info: "i", warning: "!", error: "✕", remove: "−" };
    const container = document.getElementById('toast-container') ||
        document.body.appendChild(Object.assign(document.createElement('div'), { id: 'toast-container', className: 'toast-container' }));

    const el = document.createElement('div');
    el.className = 'toast ' + type;
    el.innerHTML = '<span class="toast-icon">' + (ICONS[type] || '•') + '</span><span class="toast-message"></span>';
    el.querySelector('.toast-message').textContent = message;
    container.appendChild(el);

    requestAnimationFrame(() => el.classList.add('show'));           // плавный вход

    let killed = false;
    const kill = () => {
      if (killed) return;
      killed = true;
      el.classList.remove('show');
      el.classList.add('out');                                     // плавный уход
      setTimeout(() => el.remove(), 340);
    };

    let timer = setTimeout(kill, 5200);                              // ~5 секунд — успеваешь прочитать
    el.addEventListener('mouseenter', () => clearTimeout(timer));   // пауза под курсором
    el.addEventListener('mouseleave', () => { timer = setTimeout(kill, 1400); });

    return el;
  };

  // === ЕДИНАЯ ФУНКЦИЯ ДЛЯ ЛАЙКОВ (работает на всех страницах) ===
  window.bindFavoriteButton = function (btn, productId, isFav) {
    btn.classList.toggle('is-active', !!isFav);
    btn.innerHTML = isFav ? '♥' : '♡';
    btn.onclick = async function () {
      if (!window.ariaAuth || !window.ariaAuth.requireLogin()) return;
      try {
        const res = await window.ariaAuth.authFetch('/api/favorites/' + productId, { method: 'POST' });
        const data = await res.json();
        const nowFav = !!data.isFavorite;
        btn.classList.toggle('is-active', nowFav);
        btn.innerHTML = nowFav ? '♥' : '♡';
        btn.classList.add('pop');
        setTimeout(() => btn.classList.remove('pop'), 340);
        window.showToast(nowFav ? 'Добавлено в избранное' : 'Удалено из избранного');
      } catch(e) { window.showToast('Ошибка', 'error'); }
    };
  };

    window.ariaShell = { render, refreshLanguage };
  window.ariaShell.confirm = function (message) {
    return new Promise(resolve => {
      const overlay = document.createElement("div");
      overlay.className = "aria-confirm-backdrop";
      overlay.innerHTML = '<section class="aria-confirm-dialog glass-card" role="dialog" aria-modal="true" aria-labelledby="aria-confirm-title"><h2 id="aria-confirm-title">Подтвердите действие</h2><p class="aria-confirm-message"></p><div class="aria-confirm-actions"><button type="button" class="btn-outline" data-choice="cancel">Отмена</button><button type="button" class="btn-glow" data-choice="confirm">Подтвердить</button></div></section>';
      overlay.querySelector(".aria-confirm-message").textContent = String(message ?? "");
      const finish = choice => { overlay.remove(); document.removeEventListener("keydown", onKeydown); resolve(choice); };
      const onKeydown = event => { if (event.key === "Escape") finish(false); };
      overlay.addEventListener("click", event => { if (event.target === overlay) finish(false); });
      overlay.querySelector('[data-choice="cancel"]').addEventListener("click", () => finish(false));
      overlay.querySelector('[data-choice="confirm"]').addEventListener("click", () => finish(true));
      document.addEventListener("keydown", onKeydown);
      document.body.appendChild(overlay);
      overlay.querySelector('[data-choice="confirm"]').focus();
    });
  };
  window.alert = message => window.showToast(String(message ?? ""), "error");
  document.addEventListener("DOMContentLoaded", render);
})();

// One in-memory view of the existing server cart. No cart data is stored in localStorage.
(function () {
  'use strict';
  const esc=value=>String(value??'').replace(/[&<>"']/g,ch=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
  let rows=[],loaded=false,busy=false,reading=null,ownerToken=null,readError='',remotePending=false,catalogLoaded=false;
  const stocks=new Map();
  const channel=typeof BroadcastChannel==='function'?new BroadcastChannel('aria-cart-updates'):null;
  const token=()=>window.ariaAuth?.getToken()||null;
  const snapshot=()=>rows.map(row=>({...row}));
  const quantity=id=>rows.filter(row=>Number(row.productId)===Number(id)).reduce((sum,row)=>sum+Number(row.quantity||0),0);
  const stock=id=>stocks.has(Number(id))?stocks.get(Number(id)):null;
  function controlsInner(id) {
    const count=quantity(id),limit=stock(id),waiting=!!token()&&!loaded,disabled=busy||waiting;
    if(readError&&!loaded)return `<button type="button" class="btn-add-cart" data-cart-op="retry" data-cart-product="${id}" ${busy?'disabled':''}>Обновить корзину</button>`;
    if(!count)return `<button type="button" class="btn-add-cart" data-cart-op="add" data-cart-product="${id}" ${disabled||limit===0?'disabled':''}>${waiting?'Загрузка…':limit===0?'Нет в наличии':'В корзину'}</button>`;
    return `<div class="cart-stepper" role="group" aria-label="Количество товара в корзине"><button type="button" class="btn-icon" data-cart-op="decrease" data-cart-product="${id}" aria-label="Уменьшить количество" ${disabled?'disabled':''}>−</button><output aria-label="Количество" aria-live="polite">${count}</output><button type="button" class="btn-icon" data-cart-op="increase" data-cart-product="${id}" aria-label="Увеличить количество" ${disabled||limit==null||count>=limit?'disabled':''}>+</button></div>${limit!=null&&count>limit?`<small class="cart-stock-note">Доступно ${limit}. Уменьшите количество.</small>`:'<small class="cart-control-note">В корзине</small>'}`;
  }
  function paint() {
    document.querySelectorAll('[data-cart-control]').forEach(node=>{
      const id=Number(node.dataset.cartControl);node.dataset.cartQuantity=String(quantity(id));node.setAttribute('aria-busy',String(busy));node.innerHTML=controlsInner(id);
    });
    const badge=document.getElementById('cart-count');
    if(badge){const count=rows.length;badge.textContent=count>99?'99+':String(count);badge.hidden=!count;badge.setAttribute('aria-label','Позиций в корзине: '+count);}
  }
  function emit(){paint();window.dispatchEvent(new CustomEvent('aria:cart-changed',{detail:{items:snapshot(),busy,loaded}}));}
  function control(product) {
    const id=Number(product.id??product.productId);
    if(!stocks.has(id)&&product.quantityOnHand!=null)stocks.set(id,Math.max(0,Number(product.quantityOnHand)||0));
    return `<div class="cart-quantity-control" data-cart-control="${id}" data-cart-quantity="${quantity(id)}" aria-busy="${busy}">${controlsInner(id)}</div>`;
  }
  function register(products,complete=false) {
    if(complete){stocks.clear();catalogLoaded=true;}
    products.forEach(product=>stocks.set(Number(product.id),Math.max(0,Number(product.quantityOnHand)||0)));
    if(complete)rows.forEach(row=>{if(!stocks.has(Number(row.productId)))stocks.set(Number(row.productId),0);});
    paint();
  }
  async function response(request) {
    const reply=await request;
    const data=await reply.json().catch(()=>({}));
    if(!reply.ok)throw new Error(data.message||data.error||'Не удалось обновить корзину. Проверьте соединение.');
    return data;
  }
  async function refresh(options={}) {
    const current=token();
    if(current!==ownerToken){rows=[];loaded=false;readError='';ownerToken=current;}
    if(!current){rows=[];loaded=true;readError='';emit();return snapshot();}
    if(busy&&!options.force)return snapshot();
    if(reading)return reading;
    reading=(async()=>{
      try {
        const data=await response(window.ariaAuth.authFetch('/api/cart'));
        if(!Array.isArray(data))throw new Error('Не удалось загрузить корзину.');
        // Ignore a response from a previous account if the session changed while reading.
        if(token()!==current)return snapshot();
        rows=data;if(catalogLoaded)rows.forEach(row=>{if(!stocks.has(Number(row.productId)))stocks.set(Number(row.productId),0);});loaded=true;readError='';emit();return snapshot();
      } catch(error){readError=error.message;emit();throw error;}
      finally{reading=null;}
    })();
    return reading;
  }
  async function refreshStock(id) {
    const reply=await fetch('/api/products/'+encodeURIComponent(id));
    if(reply.status===404){stocks.set(id,0);return;}
    const data=await response(Promise.resolve(reply));
    if(!data.product)throw new Error('Не удалось проверить наличие товара.');
    stocks.set(id,Math.max(0,Number(data.product.quantityOnHand)||0));
  }
  async function loadStocks() {
    const products=await response(fetch('/api/products/store'));
    if(!Array.isArray(products))throw new Error('Не удалось проверить остатки.');
    register(products,true);
  }
  async function mutate(productId,kind,value=0) {
    if(!window.ariaAuth?.requireLogin()||busy)return false;
    const actor=token();
    const id=Number(productId);if(!Number.isSafeInteger(id)||id<1)throw new Error('Товар не найден.');
    busy=true;emit();
    const operation=async()=>{
      if(token()!==actor)throw new Error('Учётная запись изменилась. Откройте корзину заново.');
      await Promise.all([refresh({force:true}),kind==='remove'?Promise.resolve():refreshStock(id)]);
      if(token()!==actor)throw new Error('Учётная запись изменилась. Откройте корзину заново.');
      const current=rows.find(row=>Number(row.productId)===id),count=quantity(id);
      if(kind==='add'&&count>0)return true; // The add button means "put in cart", never a blind second +1.
      let target=kind==='remove'?0:kind==='add'?1:count+value;
      const limit=stock(id);
      if(kind==='adjust'&&value<0&&limit!=null)target=Math.min(target,limit);
      target=Math.max(0,target);
      if(limit!=null&&target>limit)throw new Error('Сейчас доступно '+limit+' шт.');
      if(target===count)return true;
      if(!target){
        // Delete all observed duplicate rows too, should legacy data contain them.
        for(const item of rows.filter(row=>Number(row.productId)===id))await response(window.ariaAuth.authFetch('/api/cart/'+item.id,{method:'DELETE'}));
      }else if(current){
        if(rows.filter(row=>Number(row.productId)===id).length>1)throw new Error('Обновите корзину или удалите эту позицию перед повтором.');
        await response(window.ariaAuth.authFetch('/api/cart/'+current.id+'?qty='+target,{method:'PUT'}));
      }else await response(window.ariaAuth.authFetch('/api/cart/add?productId='+id+'&qty='+target,{method:'POST'}));
      await refresh({force:true});channel?.postMessage('changed');return true;
    };
    try{return await (navigator.locks?.request?navigator.locks.request('aria-cart-write',operation):operation());}
    catch(error){await refresh({force:true}).catch(()=>{});throw error;}
    finally{busy=false;emit();if(remotePending){remotePending=false;refresh().catch(()=>{});}}
  }
  const add=id=>mutate(id,'add');
  const adjust=(id,delta)=>{if(![-1,1].includes(delta))return Promise.reject(new Error('Выберите количество кнопками + и −.'));return mutate(id,'adjust',delta);};
  const remove=id=>mutate(id,'remove');
  function remoteRefresh(){if(busy){remotePending=true;return;}refresh().catch(()=>{});}
  if(channel)channel.onmessage=event=>{if(event.data==='changed')remoteRefresh();};
  document.addEventListener('click',async event=>{
    const button=event.target.closest('[data-cart-op]');if(!button||button.disabled)return;
    try{if(button.dataset.cartOp==='retry')await refresh();else if(button.dataset.cartOp==='add')await add(button.dataset.cartProduct);else await adjust(button.dataset.cartProduct,button.dataset.cartOp==='increase'?1:-1);}
    catch(error){window.showToast?.(error.message,'error');}
  });
  window.addEventListener('focus',()=>{if(document.querySelector('[data-cart-control],#cart-items'))remoteRefresh();});
  window.addEventListener('aria:order-recorded',async()=>{await refresh().catch(()=>{});channel?.postMessage('changed');});
  window.addEventListener('pageshow',event=>{if(event.persisted)remoteRefresh();});
  const style=document.createElement('link');style.rel='stylesheet';style.href='/css/cart-controls.css';document.head.append(style);
  window.ariaCart={snapshot,quantity,stock,control,register,refresh,loadStocks,add,adjust,remove,isBusy:()=>busy,isLoaded:()=>loaded,paint};
})();
