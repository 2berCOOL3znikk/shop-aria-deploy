(function () {
  const escape = value => String(value ?? "").replace(/[&<>"']/g, ch => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch]);
  async function load() {
    const grid = document.getElementById("favorites-grid");
    if (!ariaAuth.requireLogin()) return;
    try {
      const response = await ariaAuth.authFetch("/api/favorites");
      if (!response.ok) throw new Error("Не удалось загрузить избранное");
      const items = await response.json();
      if (!items.length) {
        grid.innerHTML = '<div class="empty-state glass-card" style="grid-column:1/-1"><span aria-hidden="true">♡</span><h2>Пока ничего не сохранено</h2><p>Добавьте понравившиеся товары, чтобы быстро найти их позже.</p><a href="/index.html" class="btn-glow">Перейти в каталог</a></div>';
        return;
      }
      grid.innerHTML = items.map(p => '<article class="product-card favorite-card"><a href="/product.html?id=' + encodeURIComponent(p.id) + '">' + CatalogImages.imageTag(p, { className: "product-card-image", alt: p.name }) + '</a><div class="product-card-content"><span class="product-card-category">' + escape(p.category || "Без категории") + '</span><a class="product-card-name" href="/product.html?id=' + encodeURIComponent(p.id) + '">' + escape(p.name) + '</a><div class="product-card-price">' + formatKZT(p.displayPrice ?? p.unitPrice) + '</div><div class="product-card-actions"><button class="btn-add-cart" onclick="addFavoriteToCart(' + Number(p.id) + ')">В корзину</button><button class="aria-favorite-button" onclick="removeFavorite(' + Number(p.id) + ')" aria-label="Удалить из избранного">♥</button></div></div></article>').join("");
    } catch (error) {
      grid.innerHTML = '<div class="empty-state glass-card" style="grid-column:1/-1">Не удалось загрузить избранное. ' + escape(error.message) + '</div>';
    }
  }
  window.addFavoriteToCart = async id => {
    if (!ariaAuth.requireLogin()) return;
    try {
      const response = await ariaAuth.authFetch("/api/cart/add?productId=" + encodeURIComponent(id) + "&qty=1", { method: "POST" });
      if (!response.ok) throw new Error("Не удалось добавить товар");
      if (typeof window.refreshCartBadge === "function") await window.refreshCartBadge();
      showToast("Товар добавлен в корзину");
    } catch (error) { showToast(error.message, "error"); }
  };
  window.removeFavorite = async id => {
    try {
      const response = await ariaAuth.authFetch("/api/favorites/" + encodeURIComponent(id), { method: "DELETE" });
      if (!response.ok) throw new Error("Не удалось удалить товар");
      showToast("Удалено из избранного");
      load();
    } catch (error) { showToast(error.message, "error"); }
  };
  document.addEventListener("DOMContentLoaded", load);
})();
