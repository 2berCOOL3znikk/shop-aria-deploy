(function () {
  "use strict";

  const GENERIC_IMAGE = "/images/product-placeholder.svg";
  const escapeAttr = value => String(value ?? "").replace(/[&<>"']/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);

  function getSkuImage(product) {
    // An internal SKU is not evidence that a bundled photo depicts this product.
    // Only the explicitly assigned image may be used; owner uploads are preserved.
    return "";
  }

  function getLocalProductImage(product) {
    let value = String(product && product.imageUrl || "").trim();
    if (/^\/images\/product-(?:generic|food|home|gifts|electronics)\.svg$/.test(value)) value=GENERIC_IMAGE;
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
    const label = escapeAttr(String(name || category || ariaCopy.text("text.4fb40f52fb5a8d")).slice(0, 54));
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
    const name = product && (product.name || product.productName) || ariaCopy.text("text.4fb40f52fb5a8d");
    const category = product && product.category || "";
    const candidates = imageCandidates(product);
    const css = options.className ? ' class="' + escapeAttr(options.className) + '"' : "";
    const style = options.style ? ' style="' + escapeAttr(options.style) + '"' : "";
    const creditTarget = options.creditTarget ? ' data-credit-target="' + escapeAttr(options.creditTarget) + '"' : "";
    const alt = options.alt === undefined ? name : options.alt;
    const creditHtml=options.creditTarget?' data-credit-html="'+escapeAttr(creditFor(product))+'"':'';
    return '<img data-image-candidates="' + escapeAttr(JSON.stringify(candidates)) + '" data-image-index="0" data-cat="' + escapeAttr(category) + '" data-name="' + escapeAttr(name) + '"' + creditTarget + creditHtml + ' alt="' + escapeAttr(alt) + '" loading="lazy" decoding="async" onload="CatalogImages.attach(this)" onerror="CatalogImages.onError(this)"' + css + style + ' src="' + escapeAttr(candidates[0]) + '">';
  }

  function creditFor(product) {
    const explicit=getLocalProductImage(product);
    if (!explicit || explicit.endsWith('.svg')) return ariaCopy.text("text.998f7708d2b867");
    const photo=product?.provenance?.photo;
    if(photo&&explicit===photo.path){
      const link=(value,label)=>{try{const url=new URL(value);return url.protocol==='https:'?'<a href="'+escapeAttr(url.href)+'" target="_blank" rel="noopener noreferrer">'+escapeAttr(label)+'</a>':escapeAttr(label);}catch{return escapeAttr(label);}};
      return ariaCopy.text("text.6ff0e370203e73")+escapeAttr(photo.author)+' · '+link(photo.sourceUrl,ariaCopy.text("text.29a33ded420dd0"))+' · '+link(photo.licenseUrl,photo.license)+'. '+escapeAttr(photo.note);
    }
    return ariaCopy.text("text.fd558d9aefc37d");
  }

  function toggleCredit(image, visible) {
    const target = image.dataset.creditTarget && document.getElementById(image.dataset.creditTarget);
    if (target) {
      target.hidden = !visible;
      if(visible&&image.dataset.creditHtml)ariaCopy.setProperty(target,"innerHTML",Number(image.dataset.imageIndex||0)===0?image.dataset.creditHtml:ariaCopy.text("text.7c4eec284fda76"));
    }
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
    if(image.naturalWidth<2||image.naturalHeight<2){onError(image);return;}
    image.onerror = () => onError(image);
    toggleCredit(image, image.naturalWidth > 0);
  }

  window.CatalogImages = { getSkuImage, getLocalProductImage, imageCandidates, urlFor, fallback, attach, onError, imageTag, creditFor };
  window.getProductImageUrl = urlFor;
})();
