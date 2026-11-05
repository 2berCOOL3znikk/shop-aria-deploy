(function () {
  "use strict";

  const labels = {
    ru: { write: ariaCopy.text("text.b0f097f65241d8"), catalog: ariaCopy.text("text.d0ef3540b088bb"), social: ariaCopy.text("text.fd3069dfffcedf"), favorites: ariaCopy.text("text.68bbb1fc7579d6"), cart: ariaCopy.text("text.ec1808413ddcb0"), profile: ariaCopy.text("text.359497aff53e7d"), admin: ariaCopy.text("text.a4bba03c40fd1c"), org: ariaCopy.text("text.ed3b79e5d8058d"), support: ariaCopy.text("text.f0012086d52ed6"), login: ariaCopy.text("text.8fc9ab2eeb38ad"), logout: ariaCopy.text("text.5690c2e63b9da5"), project: ariaCopy.text("text.f45edf00685108"), about: ariaCopy.text("text.d09828b0005f32"), how: ariaCopy.text("text.b9f1fffef424d9"), privacy: ariaCopy.text("text.6d5c24967752a4"), contact: ariaCopy.text("text.2d4a75604eb904"), faq: "FAQ", telegram: "Telegram", mission: ariaCopy.text("text.66dc94f279a9f5"), funds: ariaCopy.text("text.5b2f6be0aeb139") },
    en: { write: "Write to us", catalog: "Catalog", social: "Social shelf", favorites: "♡ Favorites", cart: "Cart", profile: "Profile", admin: "Management", org: "Organization cabinet", support: "Support", login: "Sign in", logout: "Sign out", project: "Project", about: "About", how: "How it works", privacy: "Privacy", contact: "Contact", faq: "FAQ", telegram: "Telegram", mission: "Social mission", funds: "Support charities and shelters" },
    kk: { write: ariaCopy.text("text.d032982716d295"), catalog: ariaCopy.text("text.d0ef3540b088bb"), social: ariaCopy.text("text.4ec16e28f8e933"), favorites: ariaCopy.text("text.fdded270c5cf21"), cart: ariaCopy.text("text.f83105734fa566"), profile: ariaCopy.text("text.359497aff53e7d"), admin: ariaCopy.text("text.2ac2567b71b108"), org: ariaCopy.text("text.95bc07194ecbf4"), support: ariaCopy.text("text.f1fbb7c7a7279b"), login: ariaCopy.text("text.80342ff67646f8"), logout: ariaCopy.text("text.ec602d5330c3bb"), project: ariaCopy.text("text.5ece2380f3f854"), about: ariaCopy.text("text.ffab419e7e53ff"), how: ariaCopy.text("text.aa13a67d7d4bf9"), privacy: ariaCopy.text("text.632d909c3b56b6"), contact: ariaCopy.text("text.2619e6c33c9ead"), faq: "FAQ", telegram: "Telegram", mission: ariaCopy.text("text.ab4c7796f23459"), funds: ariaCopy.text("text.d6484b5ed09773") }
  };

  const langCode = () => window.ariaI18n && window.ariaI18n.getLang ? window.ariaI18n.getLang() : "ru";
  const t = key => (labels[langCode()] || labels.ru)[key] || labels.ru[key] || key;
  const setNavLabel = link => {
    const badge = link.querySelector(".shell-cart-count");
    ariaCopy.setProperty(link,"textContent",t(link.dataset.shellLabel));
    if (badge) link.appendChild(badge);
  };

  function makeLink(href, text, key) {
    const a = document.createElement("a");
    a.href = href;
    ariaCopy.setProperty(a,"textContent",text);
    if (key) a.dataset.shellLabel = key;
    if (location.pathname.toLowerCase().endsWith(href.split("#")[0].toLowerCase())) a.classList.add("active");
    return a;
  }

  function refreshLanguage() {
    const activeNav = document.getElementById("aria-shell-links");
    const footer = document.getElementById("aria-shell-footer");
    if (activeNav) activeNav.querySelectorAll("[data-shell-label]").forEach(setNavLabel);
    if (footer) footer.querySelectorAll("[data-shell-footer]").forEach(el => ariaCopy.setProperty(el,"textContent",t(el.dataset.shellFooter)));
    const languageButton = document.getElementById("lang-toggle-btn");
    if (languageButton) ariaCopy.setProperty(languageButton,"textContent",(langCode() || "ru").toUpperCase());
  }

  function render() {
    if (!document.body || document.body.dataset.ariaShell === "rendered") return;
    document.body.dataset.ariaShell = "rendered";

    const oldNav = document.querySelector("body > nav.nav-glass");
    const nav = document.createElement("nav");
    nav.className = "nav-glass aria-shell";
    ariaCopy.setAttribute(nav,"aria-label","ARIA");

    const brand = document.createElement("a");
    brand.href = "/index.html";
    brand.className = "logo-new";
    ariaCopy.setProperty(brand,"innerHTML",'<span class="aria-brand-mark">◆</span> <span class="gradient-text">ARIA</span> <span class="aria-brand-shop">Shop</span>');
    nav.appendChild(brand);

    const links = document.createElement("div");
    links.className = "nav-links aria-shell-links";
    links.id = "aria-shell-links";

    const role = window.ariaAuth && window.ariaAuth.getRole ? window.ariaAuth.getRole() : null;
    const cartLink = makeLink("/cart.html", t("cart"), "cart");
    const cartBadge = document.createElement("span");
    cartBadge.id = "cart-count";
    cartBadge.className = "shell-cart-count";
    ariaCopy.setProperty(cartBadge,"textContent","0");
    cartBadge.hidden = true;
    ariaCopy.setAttribute(cartBadge,"aria-label",ariaCopy.text("text.c660f1dac8ad2f"));
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
    ariaCopy.setAttribute(theme,"aria-label",ariaCopy.text("text.207dc60b4b0633"));
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
      ariaCopy.setProperty(email,"textContent",window.ariaAuth.getUserEmail() || "");
      user.appendChild(email);

      const logout = document.createElement("button");
      logout.type = "button";
      logout.className = "shell-user-button";
      ariaCopy.setProperty(logout,"textContent",t("logout"));
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
    ariaCopy.setAttribute(burger,"aria-label",ariaCopy.text("text.b2bbe988578044"));
    burger.setAttribute("aria-expanded", "false");
    ariaCopy.setProperty(burger,"innerHTML","<span></span><span></span><span></span>");
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
      ariaCopy.setProperty(footer,"innerHTML",ariaCopy.text("text.641128e13e3f4d"));
      document.body.appendChild(footer);
    }

    footer.querySelectorAll("[data-shell-footer]").forEach(el => ariaCopy.setProperty(el,"textContent",t(el.dataset.shellFooter)));
    links.querySelectorAll("[data-shell-label]").forEach(setNavLabel);

    ariaCopy.setProperty(theme,"textContent",window.ariaTheme && window.ariaTheme.getTheme() === "dark" ? "☀" : "🌙");
    ariaCopy.setProperty(theme,"title",window.ariaTheme && window.ariaTheme.getTheme() === "dark" ? ariaCopy.text("text.46bf625a0f170f") : ariaCopy.text("text.46fd0410f99d68"));
    ariaCopy.setProperty(lang,"textContent",(langCode() || "ru").toUpperCase());
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
      ariaCopy.setProperty(badge,"textContent","0");
      ariaCopy.setAttribute(badge,"aria-label",ariaCopy.text("text.c660f1dac8ad2f"));
      badge.hidden = true;
      return 0;
    }
    try {
      const response = await window.ariaAuth.authFetch("/api/cart/total");
      if (!response.ok) throw new Error(ariaCopy.errorText(ariaCopy.text("text.160d98c161571f")));
      const data = await response.json();
      const count = Math.max(0, Number(data.itemCount) || 0);
      ariaCopy.setProperty(badge,"textContent",count > 99 ? "99+" : String(count));
      ariaCopy.setAttribute(badge,"aria-label",ariaCopy.text("text.12ffa5b136e2c6") + count);
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
    ariaCopy.setProperty(el,"innerHTML",'<span class="toast-icon">' + (ICONS[type] || '•') + '</span><span class="toast-message"></span>');
    ariaCopy.setProperty(el.querySelector('.toast-message'),"textContent",message);
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
    ariaCopy.setProperty(btn,"innerHTML",isFav ? '♥' : '♡');
    btn.onclick = async function () {
      if (!window.ariaAuth || !window.ariaAuth.requireLogin()) return;
      try {
        const res = await window.ariaAuth.authFetch('/api/favorites/' + productId, { method: 'POST' });
        const data = await res.json();
        const nowFav = !!data.isFavorite;
        btn.classList.toggle('is-active', nowFav);
        ariaCopy.setProperty(btn,"innerHTML",nowFav ? '♥' : '♡');
        btn.classList.add('pop');
        setTimeout(() => btn.classList.remove('pop'), 340);
        window.showToast(nowFav ? ariaCopy.text("text.6527a13f1e2fec") : ariaCopy.text("text.b076284d2dfae1"));
      } catch(e) { window.showToast(ariaCopy.text("text.bf22dfeeba8b85"), 'error'); }
    };
  };

    window.ariaShell = { render, refreshLanguage };
  window.ariaShell.confirm = function (message) {
    return new Promise(resolve => {
      const overlay = document.createElement("div");
      overlay.className = "aria-confirm-backdrop";
      ariaCopy.setProperty(overlay,"innerHTML",ariaCopy.text("text.2271740a753084"));
      ariaCopy.setProperty(overlay.querySelector(".aria-confirm-message"),"textContent",String(message ?? ""));
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
    if(readError&&!loaded)return ariaCopy.template("template.4864eae7e9606b",[id,busy?'disabled':'']);
    if(!count)return `<button type="button" class="btn-add-cart" data-cart-op="add" data-cart-product="${id}" ${disabled||limit===0?'disabled':''}>${waiting?ariaCopy.text("text.db491ef5033fe0"):limit===0?ariaCopy.text("text.bd374f46503b94"):ariaCopy.text("text.4a252fb127a9de")}</button>`;
    return ariaCopy.template("template.fbba7487dc2cac",[id,disabled?'disabled':'',count,id,disabled||limit==null||count>=limit?'disabled':'',limit!=null&&count>limit?ariaCopy.template("template.5379263dfe0a8f",[limit]):ariaCopy.text("text.dc59224d0c7273")]);
  }
  function paint() {
    document.querySelectorAll('[data-cart-control]').forEach(node=>{
      const id=Number(node.dataset.cartControl);node.dataset.cartQuantity=String(quantity(id));node.setAttribute('aria-busy',String(busy));ariaCopy.setProperty(node,"innerHTML",controlsInner(id));
    });
    const badge=document.getElementById('cart-count');
    if(badge){const count=rows.length;ariaCopy.setProperty(badge,"textContent",count>99?'99+':String(count));badge.hidden=!count;ariaCopy.setAttribute(badge,'aria-label',ariaCopy.text("text.1ba9830420bb44")+count);}
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
    if(!reply.ok)throw new Error(ariaCopy.errorText(data.message||data.error||ariaCopy.text("text.518e47d808c15a")));
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
        if(!Array.isArray(data))throw new Error(ariaCopy.errorText(ariaCopy.text("text.29acb7589c028c")));
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
    if(!data.product)throw new Error(ariaCopy.errorText(ariaCopy.text("text.16069caeb4cfb1")));
    stocks.set(id,Math.max(0,Number(data.product.quantityOnHand)||0));
  }
  async function loadStocks() {
    const products=await response(fetch('/api/products/store'));
    if(!Array.isArray(products))throw new Error(ariaCopy.errorText(ariaCopy.text("text.8b528f40a705c3")));
    register(products,true);
  }
  async function mutate(productId,kind,value=0) {
    if(!window.ariaAuth?.requireLogin()||busy)return false;
    const actor=token();
    const id=Number(productId);if(!Number.isSafeInteger(id)||id<1)throw new Error(ariaCopy.errorText(ariaCopy.text("text.70f52edc0c60a0")));
    busy=true;emit();
    const operation=async()=>{
      if(token()!==actor)throw new Error(ariaCopy.errorText(ariaCopy.text("text.42b84698224205")));
      await Promise.all([refresh({force:true}),kind==='remove'?Promise.resolve():refreshStock(id)]);
      if(token()!==actor)throw new Error(ariaCopy.errorText(ariaCopy.text("text.42b84698224205")));
      const current=rows.find(row=>Number(row.productId)===id),count=quantity(id);
      if(kind==='add'&&count>0)return true; // The add button means "put in cart", never a blind second +1.
      let target=kind==='remove'?0:kind==='add'?1:count+value;
      const limit=stock(id);
      if(kind==='adjust'&&value<0&&limit!=null)target=Math.min(target,limit);
      target=Math.max(0,target);
      if(limit!=null&&target>limit)throw new Error(ariaCopy.errorText(ariaCopy.text("text.d18dbd27a02e84")+limit+ariaCopy.text("text.297cac49ffa570")));
      if(target===count)return true;
      if(!target){
        // Delete all observed duplicate rows too, should legacy data contain them.
        for(const item of rows.filter(row=>Number(row.productId)===id))await response(window.ariaAuth.authFetch('/api/cart/'+item.id,{method:'DELETE'}));
      }else if(current){
        if(rows.filter(row=>Number(row.productId)===id).length>1)throw new Error(ariaCopy.errorText(ariaCopy.text("text.c05ef70e6d81e3")));
        await response(window.ariaAuth.authFetch('/api/cart/'+current.id+'?qty='+target,{method:'PUT'}));
      }else await response(window.ariaAuth.authFetch('/api/cart/add?productId='+id+'&qty='+target,{method:'POST'}));
      await refresh({force:true});channel?.postMessage('changed');return true;
    };
    try{return await (navigator.locks?.request?navigator.locks.request('aria-cart-write',operation):operation());}
    catch(error){await refresh({force:true}).catch(()=>{});throw error;}
    finally{busy=false;emit();if(remotePending){remotePending=false;refresh().catch(()=>{});}}
  }
  const add=id=>mutate(id,'add');
  const adjust=(id,delta)=>{if(![-1,1].includes(delta))return Promise.reject(new Error(ariaCopy.errorText(ariaCopy.text("text.2d63e8c6b0fd85"))));return mutate(id,'adjust',delta);};
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
