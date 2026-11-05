(function () {
  const escape = value => String(value ?? "").replace(/[&<>"']/g, ch => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch]);
  async function load() {
    const grid = document.getElementById("favorites-grid");
    if (!ariaAuth.requireLogin()) return;
    try {
      const response = await ariaAuth.authFetch("/api/favorites");
      if (!response.ok) throw new Error(ariaCopy.errorText(ariaCopy.text("text.1133eccf802e0a")));
      const items = await response.json();
      if (!items.length) {
        ariaCopy.setProperty(grid,"innerHTML",ariaCopy.text("text.a32ae14996799b"));
        return;
      }
      window.ariaCart.register(items);
      ariaCopy.setProperty(grid,"innerHTML",items.map(p => '<article class="product-card favorite-card"><a href="/product.html?id=' + encodeURIComponent(p.id) + '">' + CatalogImages.imageTag(p, { className: "product-card-image", alt: p.name }) + '</a><div class="product-card-content"><span class="product-card-category">' + escape(p.category || ariaCopy.text("text.c1952d18056a9d")) + '</span><a class="product-card-name" href="/product.html?id=' + encodeURIComponent(p.id) + '">' + escape(p.name) + '</a><div class="product-card-price">' + formatKZT(p.displayPrice ?? p.unitPrice) + '</div><div class="product-card-actions">' + window.ariaCart.control(p) + '<button class="aria-favorite-button" onclick="removeFavorite(' + Number(p.id) + ariaCopy.text("text.bcbd786f9c04ce")).join(""));
    } catch (error) {
      ariaCopy.setProperty(grid,"innerHTML",ariaCopy.text("text.d4555c54a51617") + escape(error.message) + '</div>');
    }
  }
  window.addFavoriteToCart = async id => {
    if (!ariaAuth.requireLogin()) return;
    try {
      if (await window.ariaCart.add(id)) showToast(ariaCopy.text("text.ef754ea83995e0"));
    } catch (error) { showToast(error.message, "error"); }
  };
  window.removeFavorite = async id => {
    try {
      const response = await ariaAuth.authFetch("/api/favorites/" + encodeURIComponent(id), { method: "DELETE" });
      if (!response.ok) throw new Error(ariaCopy.errorText(ariaCopy.text("text.b2c7b58e76b8be")));
      showToast(ariaCopy.text("text.b076284d2dfae1"));
      load();
    } catch (error) { showToast(error.message, "error"); }
  };
  document.addEventListener("DOMContentLoaded", load);
})();
