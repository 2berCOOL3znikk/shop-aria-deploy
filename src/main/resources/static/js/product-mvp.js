(function () {
  const escape = value => String(value ?? "").replace(/[&<>"']/g, ch => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch]);
    const addBreadcrumbs = name => {
    if (document.getElementById("product-breadcrumbs")) return;
    const node = document.createElement("nav");
    node.id = "product-breadcrumbs";
    node.className = "product-breadcrumbs";
    ariaCopy.setAttribute(node,"aria-label",ariaCopy.text("text.e638fc3afbee12"));
    ariaCopy.setProperty(node,"innerHTML",ariaCopy.text("text.18a2950704f6ec") + escape(name) + '</span>');
    document.getElementById("product").before(node);
    document.querySelector('main.container > a[href="/index.html"]')?.remove();
  };
  const addShare = () => {
    const info = document.querySelector("#product > div:nth-child(2)");
    if (!info || info.querySelector(".telegram-share")) return;
    const name = info.querySelector("h1")?.textContent || document.title;
    const link = document.createElement("a");
    link.className = "btn-outline telegram-share";
    ariaCopy.setProperty(link,"textContent",ariaCopy.text("text.9ed933587b8505"));
    link.href = "https://t.me/share/url?url=" + encodeURIComponent(location.href) + "&text=" + encodeURIComponent(name);
    link.target = "_blank";
    link.rel = "noopener noreferrer";
    info.appendChild(link);
  };
  async function loadSimilar(current) {
    let section = document.getElementById("similar-products");
    if (!section) {
      section = document.createElement("section");
      section.id = "similar-products";
      section.className = "similar-section";
      ariaCopy.setProperty(section,"innerHTML",ariaCopy.text("text.045989eb7a84b2"));
      document.querySelector("main.container").appendChild(section);
    }
    try {
      const response = await fetch("/api/products/store");
      if (!response.ok) return;
      const products = await response.json();
      const matches = products.filter(p => String(p.category || "") === String(current.category || "") && String(p.id) !== String(current.id)).slice(0, 8);
      const row = document.getElementById("similar-row");
      ariaCopy.setProperty(row,"innerHTML",matches.length ? matches.map(p => '<a class="similar-card glass-card" href="/product.html?id=' + encodeURIComponent(p.id) + '">' + CatalogImages.imageTag(p, { alt: p.name }) + '<span class="product-card-category">' + escape(p.category || "") + '</span><strong>' + escape(p.name) + '</strong><span>' + formatKZT(p.displayPrice ?? p.unitPrice) + '</span></a>').join("") : ariaCopy.text("text.4a6d184350ddd9"));
    } catch (_) {}
  }
  function enhance() {
    const product = document.getElementById("product");
    const title = product?.querySelector("h1");
    if (!title) return;
    addBreadcrumbs(title.textContent);
    addShare();
  }
  const target = document.getElementById("product");
  if (target) new MutationObserver(enhance).observe(target, { childList: true, subtree: true });
  function renderEvidence(product){
    const info=document.querySelector('#product > div:nth-child(2)');if(!info)return;
    const provenance=product.provenance||{};
    if(provenance.demonstration){const note=document.createElement('aside');note.className='storefront-demo-label';ariaCopy.setProperty(note,"textContent",ariaCopy.text("text.a7bafefc5aa744"));info.prepend(note);}
    const section=document.createElement('section');section.className='storefront-source';section.id='product-sources';
    const safeLink=(url,label)=>{try{const value=new URL(url);return value.protocol==='https:'?'<a href="'+escape(value.href)+'" target="_blank" rel="noopener noreferrer">'+escape(label)+'</a>':escape(label);}catch{return escape(label);}};
    const facts=Array.isArray(provenance.facts)?provenance.facts:[];
    if(facts.length){
      ariaCopy.setProperty(section,"innerHTML",'<h2>'+ (provenance.verification==='PACKAGE_PHOTO'?ariaCopy.text("text.8927030ab92286"):ariaCopy.text("text.260bca7afc39c1"))+ariaCopy.text("text.addb27b104b888")+facts.map(f=>'<div><dt>'+escape(ariaCopy.systemMessage(f.label))+'</dt><dd>'+escape(f.value)+'<small>'+safeLink(f.sourceUrl,ariaCopy.text("text.ef1f448895e3b8"))+' · '+escape(f.checkedAt)+'</small></dd></div>').join('')+'</dl>'+(provenance.limitations?.length?ariaCopy.text("text.0192fa10af8f56")+provenance.limitations.map(value=>'<li>'+escape(value)+'</li>').join('')+'</ul></details>':''));
    }else ariaCopy.setProperty(section,"innerHTML",ariaCopy.text("text.45b275b61d2cbb")+(provenance.verification==='OWNER_EDITED'?ariaCopy.text("text.0a8b50f06b0576"):ariaCopy.text("text.9cb6ffd377abfc"))+'</p>');
    info.append(section);
  }
  window.addEventListener("aria:product-loaded", event => {renderEvidence(event.detail);loadSimilar(event.detail);});
})();
