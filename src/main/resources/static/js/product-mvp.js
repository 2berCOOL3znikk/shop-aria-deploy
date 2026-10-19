(function () {
  const escape = value => String(value ?? "").replace(/[&<>"']/g, ch => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch]);
    const addBreadcrumbs = name => {
    if (document.getElementById("product-breadcrumbs")) return;
    const node = document.createElement("nav");
    node.id = "product-breadcrumbs";
    node.className = "product-breadcrumbs";
    node.setAttribute("aria-label", "Навигация");
    node.innerHTML = '<a href="/index.html">Главная</a><span aria-hidden="true">/</span><a href="/index.html">Каталог</a><span aria-hidden="true">/</span><span aria-current="page">' + escape(name) + '</span>';
    document.getElementById("product").before(node);
  };
  const makeSpecTable = () => {
    const heading = [...document.querySelectorAll("#product h3")].find(el => el.textContent.includes("Характеристики"));
    if (!heading || heading.parentElement.querySelector("table")) return;
    const box = heading.parentElement;
    const rows = [...box.querySelectorAll(":scope > div")].map(row => {
      const label = row.querySelector("b")?.textContent.replace(":", "").trim();
      const value = row.textContent.replace(row.querySelector("b")?.textContent || "", "").trim();
      return label && value ? [label, value] : null;
    }).filter(Boolean);
    if (!rows.length) return;
    const table = document.createElement("table");
    table.className = "product-spec-table";
    table.innerHTML = rows.map(([label, value]) => "<tr><th>" + escape(label) + "</th><td>" + escape(value) + "</td></tr>").join("");
    box.querySelectorAll(":scope > div").forEach(row => row.remove());
    box.appendChild(table);
  };
  const addShare = () => {
    const info = document.querySelector("#product > div:nth-child(2)");
    if (!info || info.querySelector(".telegram-share")) return;
    const name = info.querySelector("h1")?.textContent || document.title;
    const link = document.createElement("a");
    link.className = "btn-outline telegram-share";
    link.textContent = "Поделиться в Telegram";
    link.href = "https://t.me/share/url?url=" + encodeURIComponent(location.href) + "&text=" + encodeURIComponent(name);
    link.target = "_blank";
    link.rel = "noopener noreferrer";
    info.appendChild(link);
  };
  async function loadSimilar(current) {
    let section = document.getElementById("similar-products");
    if (!section) {
      section = document.createElement("section");
      section.id = "similar-products";
      section.className = "similar-section";
      section.innerHTML = '<h2>Похожие товары</h2><div class="similar-row" id="similar-row"></div>';
      document.querySelector("main.container").appendChild(section);
    }
    try {
      const response = await fetch("/api/products/store");
      if (!response.ok) return;
      const products = await response.json();
      const matches = products.filter(p => String(p.category || "") === String(current.category || "") && String(p.id) !== String(current.id)).slice(0, 8);
      const row = document.getElementById("similar-row");
      row.innerHTML = matches.length ? matches.map(p => '<a class="similar-card glass-card" href="/product.html?id=' + encodeURIComponent(p.id) + '">' + CatalogImages.imageTag(p, { alt: p.name }) + '<span class="product-card-category">' + escape(p.category || "") + '</span><strong>' + escape(p.name) + '</strong><span>' + formatKZT(p.displayPrice ?? p.unitPrice) + '</span></a>').join("") : '<p class="admin-note">В этой категории пока нет других товаров.</p>';
    } catch (_) {}
  }
  function enhance() {
    const product = document.getElementById("product");
    const title = product?.querySelector("h1");
    if (!title) return;
    addBreadcrumbs(title.textContent);
    makeSpecTable();
    addShare();
  }
  const target = document.getElementById("product");
  if (target) new MutationObserver(enhance).observe(target, { childList: true, subtree: true });
  document.addEventListener("DOMContentLoaded", async () => {
    const id = new URLSearchParams(location.search).get("id");
    if (!id) return;
    try {
      const response = await fetch("/api/products/" + encodeURIComponent(id));
      if (!response.ok) return;
      const current = (await response.json()).product;
      await loadSimilar(current);
    } catch (_) {}
  });
})();