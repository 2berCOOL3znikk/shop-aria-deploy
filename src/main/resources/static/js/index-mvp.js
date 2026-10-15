(function () {
  const svgUri = (category, id) => {
    const text = String(category || "").toLowerCase();
    const hash = String(id || category || "").split("").reduce((sum, char) => (sum * 31 + char.charCodeAt(0)) >>> 0, 7);
    const tint = ["#d9e9df", "#e9e1d4", "#dce6ed", "#eee2d7"][hash % 4];
    let art;
    if (text.includes("электрон") || text.includes("техник")) art = '<rect x="66" y="44" width="74" height="86" rx="28" fill="#415e72"/><rect x="79" y="57" width="48" height="60" rx="20" fill="#a8c0ce"/><path d="M83 128v10m40-10v10" stroke="#415e72" stroke-width="9" stroke-linecap="round"/>';
    else if (text.includes("дом") || text.includes("текстил")) art = '<path d="M71 106c0-30 16-54 32-54s32 24 32 54l10 19H61z" fill="#bd8e65"/><path d="M85 80q18 14 36 0M78 105q24 13 50 0" fill="none" stroke="#f2ddc4" stroke-width="5"/>';
    else if (text.includes("промо") || text.includes("подар")) art = '<path d="M54 74h92v64H54z" fill="#7e9c8a"/><path d="M47 66h106v21H47z" fill="#ead5ae"/><path d="M96 66v72M97 65c-28-22-44 1-19 5m19-5c28-22 44 1 19 5" fill="none" stroke="#ead5ae" stroke-width="8"/>';
    else art = '<path d="M55 78h100l-10 59H65z" fill="#bb8958"/><path d="M66 91h78l-8 38H73z" fill="#efd9b5"/><path d="M82 77c3-32 45-32 48 0" fill="none" stroke="#8e6343" stroke-width="8"/><circle cx="88" cy="104" r="8" fill="#c97959"/><circle cx="113" cy="110" r="9" fill="#91a976"/><circle cx="128" cy="99" r="7" fill="#d9ae62"/>';
    const svg = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 180"><rect width="200" height="180" rx="20" fill="' + tint + '"/><ellipse cx="100" cy="146" rx="59" ry="8" fill="#60766b" opacity=".18"/>' + art + '</svg>';
    return "data:image/svg+xml;charset=UTF-8," + encodeURIComponent(svg);
  };

  const expiryInfo = date => {
    if (!date) return null;
    const days = Math.ceil((new Date(date + "T23:59:59") - new Date()) / 86400000);
    if (days < 0) return { label: "Просрочен", cls: "expiry-expired" };
    if (days <= 7) return { label: "Срок ≤7 дней", cls: "expiry-critical" };
    if (days <= 30) return { label: "Срок ≤30 дней", cls: "expiry-warning" };
    return { label: "Срок в норме", cls: "expiry-good" };
  };
  const escape = value => String(value ?? "").replace(/[&<>"']/g, ch => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch]);
  const money = value => window.formatKZT ? formatKZT(value) : (window.ariaMoney ? ariaMoney.format(value) : value + " ₸");
  let pageSize = 12;
  let currentList = [];

  window.renderProducts = function (products) {
    currentList = products.slice();
    const grid = document.getElementById("products");
    if (!products.length) {
      grid.innerHTML = '<div class="catalog-empty" style="grid-column:1/-1"><span aria-hidden="true">⌕</span><h3>Ничего не найдено</h3><p>Попробуйте изменить запрос или выбрать другую категорию.</p></div>';
      document.getElementById("catalog-more")?.remove();
      return;
    }
    const visible = products.slice(0, pageSize);
    grid.innerHTML = visible.map(p => {
      const expiry = expiryInfo(p.expiryDate);
      const discount = Number(p.discountPercent || 0) || Number(p.marketDealPercent || 0);
      const price = p.displayPrice ?? p.unitPrice;
      const favorite = state.favoriteIds.has(p.id);
      return '<article class="product-card aria-product-card">' +
        (discount > 0 ? '<span class="discount-badge">−' + Math.round(discount) + '%</span>' : '') +
        '<a class="aria-product-art" href="/product.html?id=' + encodeURIComponent(p.id) + '"><img src="' + svgUri(p.category, p.id) + '" alt="" class="product-card-image"></a>' +
        '<div class="product-card-content"><div class="product-card-category">' + escape(p.category || "Без категории") + '</div>' +
        '<div class="aria-rating" aria-label="Рейтинг 4,5 из 5" data-rating-product="' + encodeURIComponent(p.id) + '"><span>★★★★★</span><small>4,5 · отзывы покупателей</small></div>' +
        '<a class="product-card-name" href="/product.html?id=' + encodeURIComponent(p.id) + '">' + escape(p.name) + '</a>' +
        '<div class="product-card-stock">В наличии: ' + Number(p.quantityOnHand || 0) + ' шт.</div>' +
        (expiry ? '<span class="expiry-pill ' + expiry.cls + '">' + expiry.label + '</span>' : '') +
        '<div class="product-card-price">' + money(price) + (discount > 0 ? ' <span class="aria-old-price">' + money(p.unitPrice) + '</span>' : '') + '</div>' +
        '<div class="product-card-actions"><button class="btn-add-cart" onclick="addToCart(' + Number(p.id) + ')">В корзину</button>' +
        '<button class="aria-favorite-button" aria-label="Избранное" onclick="toggleFavorite(' + Number(p.id) + ',this)">' + (favorite ? "♥" : "♡") + '</button></div></div></article>';
    }).join("");
    visible.forEach(product => fetch("/api/reviews/product/" + encodeURIComponent(product.id)).then(r => r.ok ? r.json() : []).then(reviews => {
      if (!Array.isArray(reviews) || !reviews.length) return;
      const avg = reviews.reduce((sum, review) => sum + Number(review.rating || 0), 0) / reviews.length;
      const rating = grid.querySelector('[data-rating-product="' + product.id + '"]');
      if (!rating) return;
      rating.setAttribute("aria-label", "Рейтинг " + avg.toFixed(1) + " из 5");
      rating.querySelector("small").textContent = avg.toFixed(1).replace(".", ",") + " · " + reviews.length + " отзывов";
    }).catch(() => {}));    let more = document.getElementById("catalog-more");
    if (!more) {
      more = document.createElement("button");
      more.id = "catalog-more"; more.className = "btn-outline catalog-more";
      grid.insertAdjacentElement("afterend", more);
      more.addEventListener("click", () => { pageSize += 12; window.renderProducts(currentList); });
    }
    more.hidden = products.length <= pageSize;
    more.textContent = "Показать ещё";
  };

  window.filterAndSort = function () {
    const input = document.getElementById("search");
    const active = document.querySelector("#category-chips .active");
    const category = active ? active.dataset.category : "";
    const query = (input ? input.value : "").trim().toLocaleLowerCase("ru");
    const sort = document.getElementById("sort")?.value || "default";
    let filtered = state.products.filter(p => {
      const text = [p.name, p.sku, p.description, p.category].join(" ").toLocaleLowerCase("ru");
      return (!query || text.includes(query)) && (!category || p.category === category);
    });
    if (sort === "price-asc") filtered.sort((a, b) => Number(a.unitPrice) - Number(b.unitPrice));
    else if (sort === "price-desc") filtered.sort((a, b) => Number(b.unitPrice) - Number(a.unitPrice));
    else if (sort === "expiry-asc") filtered.sort((a, b) => (a.expiryDate || "9999-12-31").localeCompare(b.expiryDate || "9999-12-31"));
    else if (sort === "popular") filtered.sort((a, b) => Number(b.popularityRank || 0) - Number(a.popularityRank || 0));
    pageSize = 12;
    window.renderProducts(filtered);
  };

  function hero() {
    const intro = document.querySelector(".catalog-intro");
    if (!intro) return;
    intro.className = "aria-catalog-hero glass-card";
    intro.innerHTML = '<div class="aria-hero-copy"><span class="product-card-category">ARIA · Казахстан</span><h1>ARIA — магазин, который не выбрасывает еду</h1><p>Помогаем магазинам сокращать списания и направлять подходящие товары людям через проверенные организации.</p><div class="aria-hero-actions"><a class="btn-glow" href="#catalog">В каталог</a><a class="btn-outline" href="/about.html">Как это работает</a></div><small>₸ · демо по курсу ФНБ на 14.10.2026</small></div><div class="aria-hero-illustration" aria-hidden="true"><svg viewBox="0 0 280 220" role="img"><rect x="34" y="36" width="212" height="156" rx="30" fill="var(--bg-elevated)"/><path d="M69 89h142l-13 78H82z" fill="#9a7658"/><path d="M77 99h126l-10 58H87z" fill="#e6c99b"/><circle cx="113" cy="112" r="16" fill="#b86649"/><circle cx="148" cy="122" r="18" fill="#819b6e"/><circle cx="177" cy="109" r="13" fill="#d7a84d"/><path d="M136 97c3-28 33-29 37 0" fill="none" stroke="#758e69" stroke-width="7" stroke-linecap="round"/></svg></div>';
    document.querySelector("main.container")?.setAttribute("id", "catalog");
  }

  function updateImpact() {
    fetch("/api/products/impact").then(response => response.ok ? response.json() : Promise.reject()).then(data => {
      [["impact-saved", data.savedCount], ["impact-social", data.socialCount], ["impact-active", data.processingCount]].forEach(([id, value]) => {
        const node = document.getElementById(id);
        if (!node) return;
        const target = Number(value || 0), start = Number(node.textContent) || 0;
        const duration = 700, began = performance.now();
        const tick = now => {
          const progress = Math.min(1, (now - began) / duration);
          node.textContent = String(Math.round(start + (target - start) * progress));
          if (progress < 1) requestAnimationFrame(tick);
        };
        requestAnimationFrame(tick);
      });
    }).catch(() => {});
  }

  window.updateImpactStats = updateImpact;
  document.addEventListener("DOMContentLoaded", () => {
    hero();
    const labels = document.querySelectorAll(".impact-stat-label");
    if (labels[1]) labels[1].textContent = "В соц-разделе";
    if (labels[2]) labels[2].textContent = "В обработке ARIA";
    const sort = document.getElementById("sort");
    if (sort && !sort.querySelector('[value="expiry-asc"]')) sort.add(new Option("Срок годности: сначала ближайший", "expiry-asc"));
    const search = document.getElementById("search");
    let timer;
    if (search) search.addEventListener("input", event => {
      event.stopImmediatePropagation();
      clearTimeout(timer);
      timer = setTimeout(window.filterAndSort, 300);
    }, true);
    updateImpact();
  });
})();