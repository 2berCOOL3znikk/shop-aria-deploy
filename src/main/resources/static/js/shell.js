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
      footer.innerHTML = '<div class="shell-footer-columns"><section><h2 data-shell-footer="project"></h2><a href="/about.html" data-shell-footer="about"></a><a href="/about.html#flow" data-shell-footer="how"></a><a href="/privacy.html" data-shell-footer="privacy"></a></section><section><h2 data-shell-footer="support"></h2><a href="/support.html" data-shell-footer="contact"></a><a href="/support.html#faq" data-shell-footer="faq"></a><a href="mailto:support@aria.shop?subject=ARIA%20Shop" data-shell-footer="write"></a></section><section><h2 data-shell-footer="mission"></h2><a href="/social.html" data-shell-footer="funds"></a></section></div><p class="shell-copyright">© 2026 ARIA · Smart Retail & Social Impact · Казахстан</p>';
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
