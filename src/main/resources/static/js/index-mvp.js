(function () {
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
    if (!grid) return;
    if (!products.length) {
      grid.innerHTML = '<div class="catalog-empty" style="grid-column:1/-1"><span aria-hidden="true">⌕</span><h3>Ничего не найдено</h3><p>Попробуйте изменить запрос или выбрать другую категорию.</p></div>';
      document.getElementById("catalog-more")?.remove();
      return;
    }
    const visible = products.slice(0, pageSize);
    grid.innerHTML = visible.map(p => {
      const expiry = expiryInfo(p.expiryDate);
      const price = p.displayPrice ?? p.unitPrice;
      const basePrice = Number(p.unitPrice);
      const currentPrice = Number(price);
      const oldPrice = Number.isFinite(basePrice) && Number.isFinite(currentPrice) && currentPrice < basePrice
        ? '<del class="product-card-old-price">' + money(basePrice) + '</del>'
        : '';
      const favorite = state.favoriteIds.has(p.id);
      return '<article class="product-card aria-product-card" data-product-card="' + encodeURIComponent(p.id) + '">' +
        '<a class="aria-product-art" href="/product.html?id=' + encodeURIComponent(p.id) + '">' + CatalogImages.imageTag(p, { className: "product-card-image", alt: p.name }) + '</a>' +
        '<div class="product-card-content"><div class="product-card-category">' + escape(p.category || "Без категории") + '</div>' +
        '<div class="aria-rating" data-rating-product="' + encodeURIComponent(p.id) + '" hidden></div>' +
        '<a class="product-card-name" href="/product.html?id=' + encodeURIComponent(p.id) + '">' + escape(p.name) + '</a>' +
        '<div class="product-card-stock">В наличии: ' + Number(p.quantityOnHand || 0) + ' шт.</div>' +
        (expiry ? '<span class="expiry-pill ' + expiry.cls + '">' + expiry.label + '</span>' : '') +
        '<div class="product-card-price">' + money(price) + oldPrice + '</div>' +
        '<div class="product-card-actions"><button class="btn-add-cart" onclick="addToCart(' + Number(p.id) + ')">В корзину</button>' +
        '<button class="aria-favorite-button" aria-label="Избранное" onclick="toggleFavorite(' + Number(p.id) + ',this)">' + (favorite ? "♥" : "♡") + '</button></div></div></article>';
    }).join("");
    visible.forEach(product => fetch("/api/reviews/product/" + encodeURIComponent(product.id)).then(r => r.ok ? r.json() : []).then(reviews => {
      const validReviews = Array.isArray(reviews) ? reviews.filter(review => Number(review.rating) >= 1 && Number(review.rating) <= 5) : [];
      if (!validReviews.length) return;
      const avg = validReviews.reduce((sum, review) => sum + Number(review.rating), 0) / validReviews.length;
      const rating = grid.querySelector('[data-rating-product="' + product.id + '"]');
      if (!rating) return;
      rating.setAttribute("aria-label", "Рейтинг " + avg.toFixed(1) + " из 5");
      rating.innerHTML = '<span aria-hidden="true">' + "★".repeat(Math.round(avg)) + "☆".repeat(5 - Math.round(avg)) + '</span><small>' + avg.toFixed(1).replace(".", ",") + " · " + validReviews.length + " отзывов</small>";
      rating.hidden = false;
    }).catch(() => {}));
    let more = document.getElementById("catalog-more");
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
    if (sort === "price-asc") filtered.sort((a, b) => Number(a.displayPrice ?? a.unitPrice) - Number(b.displayPrice ?? b.unitPrice));
    else if (sort === "price-desc") filtered.sort((a, b) => Number(b.displayPrice ?? b.unitPrice) - Number(a.displayPrice ?? a.unitPrice));
    else if (sort === "expiry-asc") filtered.sort((a, b) => (a.expiryDate || "9999-12-31").localeCompare(b.expiryDate || "9999-12-31"));
    else if (sort === "popular") filtered.sort((a, b) => Number(b.popularityRank || 0) - Number(a.popularityRank || 0));
    pageSize = 12;
    window.renderProducts(filtered);
  };

  function hero() {
    const intro = document.querySelector(".catalog-intro");
    if (!intro) return;
    intro.className = "aria-catalog-hero glass-card";
    intro.innerHTML = '<div class="aria-hero-copy"><span class="product-card-category">ARIA · Казахстан</span><h1>ARIA — магазин, который не выбрасывает еду</h1><p>Помогаем магазинам сокращать списания и направлять подходящие товары людям через проверенные организации.</p><div class="aria-hero-actions"><a class="btn-glow" href="#products">В каталог</a><a class="btn-outline" href="/about.html">Как это работает</a><a class="btn-outline" href="/support.html#feedback-form">Написать нам</a></div></div><div class="aria-hero-illustration" aria-hidden="true"><svg viewBox="0 0 280 220" role="img"><rect x="34" y="36" width="212" height="156" rx="30" fill="var(--bg-elevated)"/><path d="M69 89h142l-13 78H82z" fill="#9a7658"/><path d="M77 99h126l-10 58H87z" fill="#e6c99b"/><circle cx="113" cy="112" r="16" fill="#b86649"/><circle cx="148" cy="122" r="18" fill="#819b6e"/><circle cx="177" cy="109" r="13" fill="#d7a84d"/><path d="M136 97c3-28 33-29 37 0" fill="none" stroke="#758e69" stroke-width="7" stroke-linecap="round"/></svg></div>';
  }

  function updateImpact() {
    fetch("/api/products/impact/positions").then(response => response.ok ? response.json() : Promise.reject()).then(data => {
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
    if (labels[0]) labels[0].textContent = "товаров спасено от списания";
    if (labels[1]) labels[1].textContent = "товаров в соц-разделе";
    if (labels[2]) labels[2].textContent = "товаров в обработке ARIA";
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
