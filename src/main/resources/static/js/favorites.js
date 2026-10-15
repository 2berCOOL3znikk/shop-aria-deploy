(function () {
  const escape = value => String(value ?? "").replace(/[&<>"']/g, ch => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch]);
  const art = product => {
    const category = String(product.category || "").toLowerCase();
    const hue = category.includes("электрон") ? "#dbe6ed" : category.includes("дом") ? "#ebe2d6" : category.includes("промо") ? "#e1eadd" : "#efe4d4";
    const svg = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 150"><rect width="200" height="150" rx="18" fill="' + hue + '"/><path d="M58 61h84l-9 56H67z" fill="#9a7658"/><path d="M66 70h68l-7 38H73z" fill="#ead3ac"/><circle cx="91" cy="85" r="10" fill="#bb6f50"/><circle cx="115" cy="91" r="11" fill="#849c70"/></svg>';
    return "data:image/svg+xml;charset=UTF-8," + encodeURIComponent(svg);
  };
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
      grid.innerHTML = items.map(p => '<article class="product-card favorite-card"><a href="/product.html?id=' + encodeURIComponent(p.id) + '"><img class="product-card-image" src="' + art(p) + '" alt=""></a><div class="product-card-content"><span class="product-card-category">' + escape(p.category || "Без категории") + '</span><a class="product-card-name" href="/product.html?id=' + encodeURIComponent(p.id) + '">' + escape(p.name) + '</a><div class="product-card-price">' + formatKZT(p.unitPrice) + '</div><div class="product-card-actions"><button class="btn-add-cart" onclick="addFavoriteToCart(' + Number(p.id) + ')">В корзину</button><button class="aria-favorite-button" onclick="removeFavorite(' + Number(p.id) + ')" aria-label="Удалить из избранного">♥</button></div></div></article>').join("");
    } catch (error) {
      grid.innerHTML = '<div class="empty-state glass-card" style="grid-column:1/-1">Не удалось загрузить избранное. ' + escape(error.message) + '</div>';
    }
  }
  window.addFavoriteToCart = async id => {
    if (!ariaAuth.requireLogin()) return;
    try {
      const response = await ariaAuth.authFetch("/api/cart/add?productId=" + encodeURIComponent(id) + "&qty=1", { method: "POST" });
      if (!response.ok) throw new Error("Не удалось добавить товар");
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