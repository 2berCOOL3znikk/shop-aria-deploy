(function () {
  "use strict";

  const GENERIC_IMAGE = "/images/product-generic.svg";
  const escapeAttr = value => String(value ?? "").replace(/[&<>"']/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);

  function getSkuImage(product) {
    const sku = String(product && (product.sku || product.productSku) || "").trim();
    const bundled = new Set(['SKU-1001','SKU-1002','SKU-1003','SKU-1004','SKU-1005','SKU-2001','SKU-2002','SKU-2003','SKU-2004','SKU-3001','SKU-3002','SKU-3003','SKU-4002','SKU-4003','SKU-5001','SKU-5002']);
    return bundled.has(sku) ? "/images/products/" + encodeURIComponent(sku) + ".jpg" : "";
  }

  function getLocalProductImage(product) {
    const value = String(product && product.imageUrl || "").trim();
    if (!value || /[\\\s"'<>]/.test(value)) return "";
    try {
      const parsed = new URL(value, window.location.origin);
      if(parsed.origin===window.location.origin) return parsed.pathname+parsed.search+parsed.hash;
      return parsed.protocol==='https:' ? parsed.href : '';
    } catch (_) {
      return "";
    }
  }

  function fallback(category, name) {
    const label = escapeAttr(String(name || category || "Товар").slice(0, 54));
    const svg = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 480"><rect width="640" height="480" rx="30" fill="#e8eef5"/><path d="M210 180h220l-24 175H234z" fill="#55708d"/><path d="M230 198h180l-17 130H247z" fill="#d7e3ee"/><path d="M265 180c4-65 106-65 110 0" fill="none" stroke="#55708d" stroke-width="18" stroke-linecap="round"/><text x="320" y="415" text-anchor="middle" font-family="Segoe UI,Arial,sans-serif" font-size="22" fill="#4b5d70">' + label + "</text></svg>";
    return "data:image/svg+xml;charset=UTF-8," + encodeURIComponent(svg);
  }

  function imageCandidates(product) {
    const explicit=getLocalProductImage(product);
    return Array.from(new Set([explicit && !explicit.endsWith('.svg') ? explicit : '', getSkuImage(product), explicit, GENERIC_IMAGE].filter(Boolean)));
  }

  function urlFor(product) {
    return imageCandidates(product)[0] || GENERIC_IMAGE;
  }

  function imageTag(product, options) {
    options = options || {};
    const name = product && (product.name || product.productName) || "Товар";
    const category = product && product.category || "";
    const candidates = imageCandidates(product);
    const css = options.className ? ' class="' + escapeAttr(options.className) + '"' : "";
    const style = options.style ? ' style="' + escapeAttr(options.style) + '"' : "";
    const creditTarget = options.creditTarget ? ' data-credit-target="' + escapeAttr(options.creditTarget) + '"' : "";
    const alt = options.alt === undefined ? name : options.alt;
    return '<img data-image-candidates="' + escapeAttr(JSON.stringify(candidates)) + '" data-image-index="0" data-cat="' + escapeAttr(category) + '" data-name="' + escapeAttr(name) + '"' + creditTarget + ' alt="' + escapeAttr(alt) + '" loading="lazy" decoding="async" onload="CatalogImages.attach(this)" onerror="CatalogImages.onError(this)"' + css + style + ' src="' + escapeAttr(candidates[0]) + '">';
  }

  function creditFor(product) {
    const sku = String(product && (product.sku || product.productSku) || "").trim();
    return sku ? "Локальное фото товара " + escapeAttr(sku) + ".jpg" : "Локальное фото товара";
  }

  function toggleCredit(image, visible) {
    const target = image.dataset.creditTarget && document.getElementById(image.dataset.creditTarget);
    if (target) target.hidden = !visible;
  }

  function onError(image) {
    if (!image) return;
    let candidates = [];
    try { candidates = JSON.parse(image.dataset.imageCandidates || "[]"); } catch (_) {}
    const nextIndex = Number(image.dataset.imageIndex || 0) + 1;
    if (nextIndex < candidates.length) {
      image.dataset.imageIndex = String(nextIndex);
      image.src = candidates[nextIndex];
      return;
    }
    if (image.dataset.inlineFallbackApplied === "true") return;
    image.dataset.inlineFallbackApplied = "true";
    image.onerror = null;
    image.removeAttribute("onerror");
    toggleCredit(image, false);
    image.src = fallback(image.dataset.cat, image.dataset.name);
  }

  function attach(image) {
    if (!image) return;
    image.onerror = () => onError(image);
    toggleCredit(image, image.naturalWidth > 0);
  }

  window.CatalogImages = { getSkuImage, getLocalProductImage, imageCandidates, urlFor, fallback, attach, onError, imageTag, creditFor };
  window.getProductImageUrl = urlFor;
})();
