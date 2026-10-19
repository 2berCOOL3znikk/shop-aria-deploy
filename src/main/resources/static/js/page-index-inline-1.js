const state = { products: [], favoriteIds: new Set(), cartCount: 0, allCategories: new Set() };

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, ch => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch]);
}

async function loadCartCount() {
  if (!window.ariaAuth || !ariaAuth.isLoggedIn()) {
    state.cartCount = 0;
    return 0;
  }
  if (typeof window.refreshCartBadge !== "function") return state.cartCount;
  state.cartCount = await window.refreshCartBadge();
  return state.cartCount;
}

async function loadFavorites() {
  if (!window.ariaAuth || !ariaAuth.isLoggedIn()) {
    state.favoriteIds.clear();
    return;
  }
  const response = await ariaAuth.authFetch("/api/favorites/ids");
  if (!response.ok) throw new Error("Не удалось загрузить избранное");
  const data = await response.json();
  state.favoriteIds = new Set((data.ids || []).map(Number));
}

window.toggleFavorite = async function (productId, button) {
  if (!ariaAuth.requireLogin()) return;
  try {
    const response = await ariaAuth.authFetch("/api/favorites/" + encodeURIComponent(productId), { method: "POST" });
    if (!response.ok) throw new Error("Не удалось обновить избранное");
    const data = await response.json();
    const isFavorite = !!data.isFavorite;
    if (isFavorite) state.favoriteIds.add(Number(productId));
    else state.favoriteIds.delete(Number(productId));
    if (button) {
      button.textContent = isFavorite ? "♥" : "♡";
      button.classList.toggle("is-active", isFavorite);
      button.classList.add("pop");
      setTimeout(() => button.classList.remove("pop"), 340);
    }
    window.showToast(isFavorite ? "Добавлено в избранное" : "Удалено из избранного");
  } catch (error) {
    window.showToast(error.message || "Ошибка избранного", "error");
  }
};

window.addToCart = async function (productId) {
  if (!ariaAuth.requireLogin()) return;
  try {
    const response = await ariaAuth.authFetch("/api/cart/add?productId=" + encodeURIComponent(productId) + "&qty=1", { method: "POST" });
    if (!response.ok) throw new Error("Не удалось добавить товар в корзину");
    await loadCartCount();
    window.showToast("Товар добавлен в корзину");
  } catch (error) {
    window.showToast(error.message || "Ошибка добавления", "error");
  }
};

function updateCategoryChips() {
  const container = document.getElementById("category-chips");
  if (!container) return;
  const categories = [""].concat(Array.from(state.allCategories).sort((a, b) => a.localeCompare(b, "ru")));
  container.innerHTML = categories.map(category =>
    '<button class="category-chip' + (category === "" ? " active" : "") + '" data-category="' + escapeHtml(category) + '">' +
    (category ? escapeHtml(category) : "Все категории") + "</button>"
  ).join("");
  container.querySelectorAll(".category-chip").forEach(chip => {
    chip.addEventListener("click", () => {
      container.querySelectorAll(".category-chip").forEach(item => item.classList.remove("active"));
      chip.classList.add("active");
      if (typeof window.filterAndSort === "function") window.filterAndSort();
    });
  });
}

async function initCatalogPage() {
  if (window.ariaTheme) ariaTheme.initTheme();
  if (window.ariaI18n) ariaI18n.initI18n();

  try {
    await Promise.all([loadFavorites(), loadCartCount()]);
    const response = await fetch("/api/products/store");
    if (!response.ok) throw new Error("Не удалось загрузить каталог");
    state.products = await response.json();
    state.products.forEach(product => {
      if (product.category) state.allCategories.add(product.category);
    });
    updateCategoryChips();
    if (typeof window.filterAndSort === "function") window.filterAndSort();
    const sort = document.getElementById("sort");
    if (sort) sort.addEventListener("change", () => window.filterAndSort && window.filterAndSort());
  } catch (error) {
    const grid = document.getElementById("products");
    if (grid) grid.innerHTML = '<div class="catalog-empty" style="grid-column:1/-1"><h3>Каталог временно недоступен</h3><p>' + escapeHtml(error.message) + '</p><button class="btn-outline" type="button" onclick="location.reload()">Повторить</button></div>';
    window.showToast(error.message || "Не удалось загрузить каталог", "error");
  }
}

document.addEventListener("DOMContentLoaded", initCatalogPage, { once: true });