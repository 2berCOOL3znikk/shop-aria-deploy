(function () {
  "use strict";

  const commons = "https://upload.wikimedia.org/wikipedia/commons";
  const thumb = (hash, file, width) => commons + "/thumb/" + hash + "/" + file + "/" + width + "px-" + file;
  const entries = [
    { key: "coffee", match: ["кофе"], category: "Продукты", file: "Coffee_Bean.jpg", url: thumb("b/bc", "Coffee_Bean.jpg", 960), page: "https://commons.wikimedia.org/wiki/File:Coffee_Bean.jpg", author: "Kettlechips1", license: "общественное достояние" },
    { key: "tea", match: ["чай"], category: "Продукты", file: "Loose_chamomile_tea.jpg", url: thumb("5/57", "Loose_chamomile_tea.jpg", 960), page: "https://commons.wikimedia.org/wiki/File:Loose_chamomile_tea.jpg", author: "Ehedaya", license: "общественное достояние" },
    { key: "chocolate", match: ["шоколад"], category: "Продукты", file: "Chocolate_bar.png", url: thumb("8/8b", "Chocolate_bar.png", 960), page: "https://commons.wikimedia.org/wiki/File:Chocolate_bar.png", author: "Debora Cartegena / CDC", license: "общественное достояние США" },
    { key: "pasta", match: ["паста", "спагетти"], category: "Продукты", file: "Drying_Spaghetti_in_Gragnano.jpg", url: commons + "/e/e0/Drying_Spaghetti_in_Gragnano.jpg", page: "https://commons.wikimedia.org/wiki/File:Drying_Spaghetti_in_Gragnano.jpg", author: "Pasta Faella", license: "CC BY-SA 2.0" },
    { key: "honey", match: ["мёд", "мед"], category: "Продукты", file: "HoneyJar.JPG", url: commons + "/c/cd/HoneyJar.JPG", page: "https://commons.wikimedia.org/wiki/File:HoneyJar.JPG", author: "Vicki Nunn", license: "общественное достояние" },
    { key: "towel", match: ["полотенце", "плед"], category: "Дом/текстиль", file: "Towel.jpg", url: thumb("c/c3", "Towel.jpg", 960), page: "https://commons.wikimedia.org/wiki/File:Towel.jpg", author: "More paper", license: "общественное достояние" },
    { key: "vase", match: ["ваза"], category: "Дом/текстиль", file: "Porcelain_vase.jpg", url: thumb("d/da", "Porcelain_vase.jpg", 960), page: "https://commons.wikimedia.org/wiki/File:Porcelain_vase.jpg", author: "R Ch Q", license: "CC BY-SA 4.0" },
    { key: "pillow", match: ["подушка"], category: "Дом/текстиль", file: "Ccc_pillow.jpg", url: commons + "/3/3e/Ccc_pillow.jpg", page: "https://commons.wikimedia.org/wiki/File:Ccc_pillow.jpg", author: "U.S. federal government / CCC", license: "общественное достояние США" },
    { key: "charger", match: ["зарядное", "зарядка", "usb-c", "гаджет", "аксессуар", "адаптер"], category: "Электроника", file: "Plug-in_USB_charger_-_case_removed-8159.jpg", url: thumb("2/2c", "Plug-in_USB_charger_-_case_removed-8159.jpg", 960), page: "https://commons.wikimedia.org/wiki/File:Plug-in_USB_charger_-_case_removed-8159.jpg", author: "Raimond Spekking", license: "CC BY-SA 4.0" },
    { key: "headphones", match: ["наушник"], category: "Электроника", file: "Earphones,_940074_005.jpg", url: thumb("1/1e", "Earphones%2C_940074_005.jpg", 960), page: "https://commons.wikimedia.org/wiki/File:Earphones,_940074_005.jpg", author: "Auckland Museum Tāmaki Paenga Hira", license: "CC BY 4.0" },
    { key: "dairy", match: ["йогурт", "молоко", "сыр"], category: "Продукты", file: "Milk-bottle.jpg", url: commons + "/0/01/Milk-bottle.jpg", page: "https://commons.wikimedia.org/wiki/File:Milk-bottle.jpg", author: "FiveRings", license: "CC BY 3.0" },
    { key: "gift", match: ["подарочный", "подарочн", "дегустационный", "дегустационн", "набор", "сет"], category: "Промо", file: "Wrapped_fruit_basket.jpg", url: thumb("5/5f", "Wrapped_fruit_basket.jpg", 960), page: "https://commons.wikimedia.org/wiki/File:Wrapped_fruit_basket.jpg", author: "Matthew Hoelscher", license: "CC BY-SA 2.0" }
  ];

  const normalize = value => String(value || "").toLocaleLowerCase("ru").replace(/ё/g, "е");
  const escapeAttr = value => String(value ?? "").replace(/[&<>"']/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);

  function resolve(product) {
    const name = normalize(product && (product.name || product.productName));
    const category = normalize(product && product.category);
    const found = entries.find(entry => entry.match.some(part => name.includes(normalize(part))));
    if (found) return found;
    if (category.includes("электрон") || category.includes("техник")) return entries.find(entry => entry.key === "charger");
    if (category.includes("дом") || category.includes("текстил")) return entries.find(entry => entry.key === "towel");
    if (category.includes("промо") || category.includes("подар")) return entries.find(entry => entry.key === "gift");
    return entries.find(entry => entry.key === "coffee");
  }

  function localPhoto(product) {
    const url = String(product && product.imageUrl || "");
    return /^\/api\/products\/\d+\/photo(?:[?#].*)?$/.test(url) ? url : "";
  }

  function fallback(category, name) {
    const kind = normalize(category);
    const seed = normalize(name || category);
    const hash = seed.split("").reduce((sum, char) => (sum * 31 + char.charCodeAt(0)) >>> 0, 7);
    const colors = ["#d9e9df", "#e9e1d4", "#dce6ed", "#eee2d7"];
    let art;
    if (kind.includes("электрон") || kind.includes("техник")) {
      art = '<rect x="66" y="44" width="74" height="86" rx="28" fill="#415e72"/><rect x="79" y="57" width="48" height="60" rx="20" fill="#a8c0ce"/><path d="M83 128v10m40-10v10" stroke="#415e72" stroke-width="9" stroke-linecap="round"/>';
    } else if (kind.includes("дом") || kind.includes("текстил")) {
      art = '<path d="M71 106c0-30 16-54 32-54s32 24 32 54l10 19H61z" fill="#bd8e65"/><path d="M85 80q18 14 36 0M78 105q24 13 50 0" fill="none" stroke="#f2ddc4" stroke-width="5"/>';
    } else if (kind.includes("промо") || kind.includes("подар")) {
      art = '<path d="M54 74h92v64H54z" fill="#7e9c8a"/><path d="M47 66h106v21H47z" fill="#ead5ae"/><path d="M96 66v72M97 65c-28-22-44 1-19 5m19-5c28-22 44 1 19 5" fill="none" stroke="#ead5ae" stroke-width="8"/>';
    } else {
      art = '<path d="M55 78h100l-10 59H65z" fill="#bb8958"/><path d="M66 91h78l-8 38H73z" fill="#efd9b5"/><path d="M82 77c3-32 45-32 48 0" fill="none" stroke="#8e6343" stroke-width="8"/><circle cx="88" cy="104" r="8" fill="#c97959"/><circle cx="113" cy="110" r="9" fill="#91a976"/><circle cx="128" cy="99" r="7" fill="#d9ae62"/>';
    }
    const svg = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 180"><rect width="200" height="180" rx="20" fill="' + colors[hash % colors.length] + '"/><ellipse cx="100" cy="146" rx="59" ry="8" fill="#60766b" opacity=".18"/>' + art + "</svg>";
    return "data:image/svg+xml;charset=UTF-8," + encodeURIComponent(svg);
  }

  function urlFor(product) {
    return localPhoto(product) || resolve(product).url;
  }

  function imageTag(product, options) {
    options = options || {};
    const name = product && (product.name || product.productName) || "Товар";
    const category = product && product.category || "";
    const css = options.className ? ' class="' + escapeAttr(options.className) + '"' : "";
    const style = options.style ? ' style="' + escapeAttr(options.style) + '"' : "";
    const alt = options.alt === undefined ? name : options.alt;
    const creditTarget = options.creditTarget ? ' data-credit-target="' + escapeAttr(options.creditTarget) + '"' : "";
    const remote = localPhoto(product) ? "false" : "true";
    return '<img src="' + escapeAttr(urlFor(product)) + '" data-cat="' + escapeAttr(category) + '" data-name="' + escapeAttr(name) + '" data-commons="' + remote + '"' + creditTarget + ' alt="' + escapeAttr(alt) + '" loading="lazy" decoding="async" onload="CatalogImages.attach(this)" onerror="CatalogImages.onError(this)"' + css + style + ">";
  }

  function creditFor(product) {
    const source = resolve(product);
    const licenseLinks = {
      "CC BY-SA 2.0": "https://creativecommons.org/licenses/by-sa/2.0/",
      "CC BY-SA 4.0": "https://creativecommons.org/licenses/by-sa/4.0/",
      "CC BY 4.0": "https://creativecommons.org/licenses/by/4.0/",
      "CC BY 3.0": "https://creativecommons.org/licenses/by/3.0/",
      "общественное достояние": "https://creativecommons.org/publicdomain/mark/1.0/",
      "общественное достояние США": "https://commons.wikimedia.org/wiki/Commons:Licensing"
    };
    const title = source.file.replace(/_/g, " ").replace(/%2C/gi, ",");
    return '<a href="' + source.page + '" target="_blank" rel="noopener noreferrer">Wikimedia Commons: ' + escapeAttr(title) + "</a> · автор: " + escapeAttr(source.author) + ' · лицензия <a href="' + licenseLinks[source.license] + '" target="_blank" rel="noopener noreferrer">' + escapeAttr(source.license) + "</a>";
  }

  function toggleCredit(image, visible) {
    const target = image.dataset.creditTarget && document.getElementById(image.dataset.creditTarget);
    if (target) target.hidden = !visible;
  }

  function onError(image) {
    if (!image || image.dataset.fallbackApplied === "true") return;
    image.dataset.fallbackApplied = "true";
    image.onerror = null;
    image.removeAttribute("onerror");
    toggleCredit(image, false);
    image.src = fallback(image.dataset.cat, image.dataset.name);
  }

  function attach(image) {
    if (!image) return;
    if (!image.dataset.catalogImageAttached) {
      image.dataset.catalogImageAttached = "true";
      image.onerror = () => onError(image);
    }
    toggleCredit(image, image.dataset.commons === "true" && image.dataset.fallbackApplied !== "true" && image.naturalWidth > 0);
  }

  window.CatalogImages = { urlFor, fallback, attach, onError, imageTag, creditFor };
})();