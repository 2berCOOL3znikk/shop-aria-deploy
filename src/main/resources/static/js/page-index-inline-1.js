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
  if (!response.ok) throw new Error(ariaCopy.errorText(ariaCopy.text("text.1133eccf802e0a")));
  const data = await response.json();
  state.favoriteIds = new Set((data.ids || []).map(Number));
}

window.toggleFavorite = async function (productId, button) {
  if (!ariaAuth.requireLogin()) return;
  try {
    const response = await ariaAuth.authFetch("/api/favorites/" + encodeURIComponent(productId), { method: "POST" });
    if (!response.ok) throw new Error(ariaCopy.errorText(ariaCopy.text("text.5cb21968d2c5b7")));
    const data = await response.json();
    const isFavorite = !!data.isFavorite;
    if (isFavorite) state.favoriteIds.add(Number(productId));
    else state.favoriteIds.delete(Number(productId));
    if (button) {
      ariaCopy.setProperty(button,"textContent",isFavorite ? "♥" : "♡");
      button.classList.toggle("is-active", isFavorite);
      button.setAttribute("aria-pressed", String(isFavorite));
      ariaCopy.setAttribute(button,"aria-label",isFavorite ? ariaCopy.text("text.7658dd5450ed58") : ariaCopy.text("text.2a6b5f7278c0d6"));
      button.classList.add("pop");
      setTimeout(() => button.classList.remove("pop"), 340);
    }
    window.showToast(isFavorite ? ariaCopy.text("text.6527a13f1e2fec") : ariaCopy.text("text.b076284d2dfae1"));
  } catch (error) {
    window.showToast(error.message || ariaCopy.text("text.5e6d97672cf9b9"), "error");
  }
};

window.addToCart = async function (productId) {
  if (!ariaAuth.requireLogin()) return;
  try {
    if (await window.ariaCart.add(productId)) window.showToast(ariaCopy.text("text.ef754ea83995e0"));
  } catch (error) {
    window.showToast(error.message || ariaCopy.text("text.75085e97cda03f"), "error");
  }
};

function updateCategoryChips() {
  const container = document.getElementById("category-chips");
  if (!container) return;
  const categories = [""].concat(Array.from(state.allCategories).sort((a, b) => a.localeCompare(b, "ru")));
  const selected = new URLSearchParams(location.search).get("category") || "";
  const active = categories.includes(selected) ? selected : "";
  ariaCopy.setProperty(container,"innerHTML",categories.map(category =>
    '<button type="button" class="category-chip' + (category === active ? " active" : "") + '" aria-pressed="' + (category === active) + '" data-category="' + escapeHtml(category) + '">' +
    (category ? escapeHtml(category) : ariaCopy.text("text.98157c47a1bdb8")) + "</button>"
  ).join(""));
  container.querySelectorAll(".category-chip").forEach(chip => {
    chip.addEventListener("click", () => {
      container.querySelectorAll(".category-chip").forEach(item => { item.classList.remove("active"); item.setAttribute("aria-pressed", "false"); });
      chip.classList.add("active");
      chip.setAttribute("aria-pressed", "true");
      if (typeof window.filterAndSort === "function") window.filterAndSort();
    });
  });
}

async function initCatalogPage() {
  if (window.ariaTheme) ariaTheme.initTheme();
  if (window.ariaI18n) ariaI18n.initI18n();

  try {
    // An unavailable optional account feature must not hide the public catalogue.
    const accountLoads = await Promise.allSettled([loadFavorites(), loadCartCount()]);
    if (accountLoads.some(result => result.status === "rejected")) window.showToast(ariaCopy.text("text.0eecb5abc70336"), "error");
    const response = await fetch("/api/products/store");
    if (!response.ok) throw new Error(ariaCopy.errorText(ariaCopy.text("text.cac774f7b48a95")));
    state.products = await response.json();
    window.ariaCart.register(state.products, true);
    state.products.forEach(product => {
      if (product.category) state.allCategories.add(product.category);
    });
    updateCategoryChips();
    if (typeof window.filterAndSort === "function") window.filterAndSort();
    const sort = document.getElementById("sort");
    if (sort) sort.addEventListener("change", () => window.filterAndSort && window.filterAndSort());
  } catch (error) {
    const grid = document.getElementById("products");
    if (grid) ariaCopy.setProperty(grid,"innerHTML",ariaCopy.text("text.15973fdd73c335") + escapeHtml(error.message) + ariaCopy.text("text.19febf9e216781"));
    window.showToast(error.message || ariaCopy.text("text.cac774f7b48a95"), "error");
  }
}

document.addEventListener("DOMContentLoaded", initCatalogPage, { once: true });
