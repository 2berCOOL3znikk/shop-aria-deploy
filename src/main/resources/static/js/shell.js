(function () {
  "use strict";
  const labels = {
    ru: { catalog: "Каталог", social: "Соц. полка", favorites: "♡ Избранное", cart: "Корзина", profile: "Профиль", admin: "Управление", org: "Кабинет организации", support: "Поддержка", login: "Войти", logout: "Выйти", project: "Проект", about: "О проекте", how: "Как это работает", privacy: "Конфиденциальность", contact: "Связаться", faq: "FAQ", telegram: "Telegram", mission: "Соц. миссия", funds: "Помощь фондам и приютам" },
    en: { catalog: "Catalog", social: "Social shelf", favorites: "♡ Favorites", cart: "Cart", profile: "Profile", admin: "Management", org: "Organization cabinet", support: "Support", login: "Sign in", logout: "Sign out", project: "Project", about: "About", how: "How it works", privacy: "Privacy", contact: "Contact", faq: "FAQ", telegram: "Telegram", mission: "Social mission", funds: "Support charities and shelters" },
    kk: { catalog: "Каталог", social: "Әлеуметтік сөре", favorites: "♡ Таңдаулылар", cart: "Себет", profile: "Профиль", admin: "Басқару", org: "Ұйым кабинеті", support: "Қолдау", login: "Кіру", logout: "Шығу", project: "Жоба", about: "Жоба туралы", how: "Қалай жұмыс істейді", privacy: "Құпиялылық", contact: "Байланыс", faq: "FAQ", telegram: "Telegram", mission: "Әлеуметтік миссия", funds: "Қорлар мен панажайларға көмек" }
  };
  const langCode = () => window.ariaI18n && window.ariaI18n.getLang ? window.ariaI18n.getLang() : "ru";
  const t = key => (labels[langCode()] || labels.ru)[key] || labels.ru[key] || key;
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
    if (activeNav) activeNav.querySelectorAll("[data-shell-label]").forEach(el => el.textContent = t(el.dataset.shellLabel));
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
    brand.innerHTML = '<span class="aria-brand-mark">◆</span><span class="gradient-text">ARIA</span><span class="aria-brand-shop">Shop</span>';
    nav.appendChild(brand);
    const links = document.createElement("div");
    links.className = "nav-links aria-shell-links";
    links.id = "aria-shell-links";
    const role = window.ariaAuth && window.ariaAuth.getRole ? window.ariaAuth.getRole() : null;
    [makeLink("/index.html", t("catalog"), "catalog"), makeLink("/social.html", t("social"), "social"), makeLink("/favorites.html", t("favorites"), "favorites"), makeLink("/cart.html", t("cart"), "cart"), makeLink("/profile.html", t("profile"), "profile")].forEach(link => links.appendChild(link));
    if (role === "ORG") links.appendChild(makeLink("/social.html#claims-section", t("org"), "org"));
    if (role === "ADMIN") links.appendChild(makeLink("/admin.html", t("admin"), "admin"));
    links.appendChild(makeLink("/support.html", t("support"), "support"));
    nav.appendChild(links);
    const actions = document.createElement("div");
    actions.className = "aria-shell-actions";
    const theme = document.createElement("button");
    theme.type = "button"; theme.id = "theme-toggle-btn"; theme.className = "shell-icon-button"; theme.setAttribute("aria-label", "Сменить тему");
    theme.addEventListener("click", () => window.ariaTheme && window.ariaTheme.toggleTheme());
    actions.appendChild(theme);
    const lang = document.createElement("button");
    lang.type = "button"; lang.id = "lang-toggle-btn"; lang.className = "shell-lang-button";
    lang.addEventListener("click", () => { if (window.ariaI18n) window.ariaI18n.cycleLang(); refreshLanguage(); });
    actions.appendChild(lang);
    const user = document.createElement("div");
    user.id = "auth-nav"; user.className = "aria-shell-user";
    if (window.ariaAuth && window.ariaAuth.isLoggedIn()) {
      const email = document.createElement("span"); email.className = "shell-user-email"; email.textContent = window.ariaAuth.getUserEmail() || ""; user.appendChild(email);
      const logout = document.createElement("button"); logout.type = "button"; logout.className = "shell-user-button"; logout.textContent = t("logout"); logout.addEventListener("click", () => window.ariaAuth.logout()); user.appendChild(logout);
    } else user.appendChild(makeLink("/login.html", t("login"), "login"));
    actions.appendChild(user); nav.appendChild(actions);
    const burger = document.createElement("button");
    burger.type = "button"; burger.className = "aria-shell-burger"; burger.setAttribute("aria-label", "Открыть меню"); burger.setAttribute("aria-expanded", "false"); burger.innerHTML = "<span></span><span></span><span></span>";
    burger.addEventListener("click", () => { const open = nav.classList.toggle("menu-open"); burger.setAttribute("aria-expanded", String(open)); });
    nav.insertBefore(burger, links);
    if (oldNav) oldNav.remove();
    document.body.insertBefore(nav, document.body.firstChild);
    const toastBox = document.getElementById("toast-container") || document.createElement("div");
    toastBox.id = "toast-container"; toastBox.classList.add("toast-container");
    toastBox.setAttribute("aria-live", "polite"); toastBox.setAttribute("aria-atomic", "false");
    if (!toastBox.isConnected) document.body.appendChild(toastBox);
    document.querySelectorAll("body > footer:not(#aria-shell-footer)").forEach(el => el.remove());
    let footer = document.getElementById("aria-shell-footer");
    if (!footer) {
      footer = document.createElement("footer"); footer.id = "aria-shell-footer"; footer.className = "aria-shell-footer";
      footer.innerHTML = '<div class="shell-footer-columns"><section><h2 data-shell-footer="project"></h2><a href="/about.html" data-shell-footer="about"></a><a href="/about.html#flow" data-shell-footer="how"></a><a href="/privacy.html" data-shell-footer="privacy"></a></section><section><h2 data-shell-footer="support"></h2><a href="/support.html" data-shell-footer="contact"></a><a href="/support.html#faq" data-shell-footer="faq"></a><a href="https://t.me/aria_shop_kz" target="_blank" rel="noopener noreferrer" data-shell-footer="telegram"></a></section><section><h2 data-shell-footer="mission"></h2><a href="/social.html" data-shell-footer="funds"></a></section></div><p class="shell-copyright">© 2026 ARIA · Smart Retail &amp; Social Impact · Казахстан</p>';
      document.body.appendChild(footer);
    }
    footer.querySelectorAll("[data-shell-footer]").forEach(el => el.textContent = t(el.dataset.shellFooter));
    links.querySelectorAll("[data-shell-label]").forEach(el => el.textContent = t(el.dataset.shellLabel));
    theme.textContent = window.ariaTheme && window.ariaTheme.getTheme() === "dark" ? "☀" : "🌙";
    theme.title = window.ariaTheme && window.ariaTheme.getTheme() === "dark" ? "Светлая тема" : "Тёмная тема";
    lang.textContent = (langCode() || "ru").toUpperCase();
  }
  window.ariaShell = { render, refreshLanguage };
  window.showToast = window.showToast || function (message, type = "success") {
    const container = document.getElementById("toast-container") || document.body.appendChild(Object.assign(document.createElement("div"), { id: "toast-container", className: "toast-container" }));
    const toast = document.createElement("div"); toast.className = "toast " + (type === "error" ? "error" : "success"); toast.textContent = String(message ?? ""); container.appendChild(toast);
    window.setTimeout(() => toast.remove(), 3500);
  };
  document.addEventListener("DOMContentLoaded", render);
})();