let productId = null;
let reviewBusy = false;

    function escapeHtml(value) {
        return String(value ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
    }

    async function loadCartCount() {
        if (typeof window.refreshCartBadge === 'function') await window.refreshCartBadge();
    }

    async function addToCart(id) {
        if (!ariaAuth.requireLogin()) return;
        try {
            if (await window.ariaCart.add(id)) window.showToast(ariaCopy.text("text.ef754ea83995e0"));
        } catch(e) { window.showToast(e.message || ariaCopy.text("text.75085e97cda03f"), 'error'); }
    }

    async function toggleFavorite(id, btn) {
        if (!ariaAuth.requireLogin()) return;
        try {
            const res = await ariaAuth.authFetch('/api/favorites/' + encodeURIComponent(id), { method: 'POST' });
            if (!res.ok) throw new Error(ariaCopy.errorText(ariaCopy.text("text.5cb21968d2c5b7")));
            const data = await res.json();
            if (data.isFavorite) {
                ariaCopy.setProperty(btn,"textContent",ariaCopy.text("text.6a5df732885c30"));
                btn.setAttribute('aria-pressed','true');
                window.showToast(ariaCopy.text("text.6527a13f1e2fec"));
            } else {
                ariaCopy.setProperty(btn,"textContent",ariaCopy.text("text.d6e7b0dcd513ff"));
                btn.setAttribute('aria-pressed','false');
                window.showToast(ariaCopy.text("text.b076284d2dfae1"));
            }
        } catch(e) { window.showToast(e.message || ariaCopy.text("text.bf22dfeeba8b85"), 'error'); }
    }

    async function loadReviews() {
        try {
            const res = await fetch('/api/reviews/product/' + encodeURIComponent(productId));
            if (!res.ok) throw new Error(ariaCopy.errorText(ariaCopy.text("text.c43d620ffc2337")));
            const reviews = await res.json();
            const list = document.getElementById('reviews-list');
            if (!Array.isArray(reviews) || reviews.length === 0) {
                ariaCopy.setProperty(list,"innerHTML",ariaCopy.text("text.d42a258b1ed552"));
                return;
            }
            ariaCopy.setProperty(list,"innerHTML",reviews.map(r =>
                '<div class="glass-card" style="padding:16px;margin-bottom:10px;">' +
                '<div style="display:flex;justify-content:space-between;margin-bottom:8px;">' +
                '<strong>' + escapeHtml(r.authorName) + '</strong>' +
                '<span style="color:var(--accent-orange);">' + '★'.repeat(r.rating) + '☆'.repeat(5 - r.rating) + '</span>' +
                '</div>' +
                '<p style="margin:0;color:var(--text-secondary);line-height:1.6;">' + escapeHtml(r.comment) + '</p>' +
                '<div style="font-size:12px;color:var(--text-muted);margin-top:8px;">' + new Date(r.createdAt).toLocaleString(ariaCopy.locale()) + '</div>' +
                '</div>'
            ).join(''));
        } catch(e) {
            ariaCopy.setProperty(document.getElementById('reviews-list'),"innerHTML",ariaCopy.text("text.6d9b9806f6162a"));
        }
    }

    async function submitReview() {
        if (reviewBusy || !productId || document.getElementById('reviews').hidden || !ariaAuth.requireLogin()) return;
        const author = document.getElementById('review-author').value || ariaCopy.text("text.1f4d00a3bfebed");
        const comment = document.getElementById('review-comment').value;
        const rating = parseInt(document.getElementById('review-rating').value);
        if (!comment.trim()) { window.showToast(ariaI18n.t('product.reviewError'), 'error'); return; }
        reviewBusy=true;
        const submit=document.getElementById('review-submit');if(submit)submit.disabled=true;
        try {
            const res = await ariaAuth.authFetch('/api/reviews', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ productId, author, comment, rating })
            });
            if (!res.ok) throw new Error(ariaCopy.errorText(ariaCopy.text("text.feba70ca3028a4")));
            window.showToast(ariaI18n.t('toast.reviewAdded'));
            document.getElementById('review-comment').value = '';
            loadReviews();
        } catch(e) { window.showToast(ariaI18n.t('toast.error'), 'error'); }
        finally { reviewBusy=false;if(submit)submit.disabled=false; }
    }

    async function loadProduct() {
        const params = new URLSearchParams(location.search);
        productId = params.get('id');
        if (!productId) { location.href = '/index.html'; return; }
        const reviews = document.getElementById('reviews');
        if (reviews) reviews.hidden = true;

        try {
            const res = await fetch('/api/products/' + encodeURIComponent(productId));
            if (!res.ok) throw Object.assign(new Error(ariaCopy.errorText(ariaCopy.text("text.25db4330ae7389"))), { status: res.status });
            const data = await res.json();
            const p = data.product;
            const d = data.details;
            if (!p || !p.id) throw Object.assign(new Error(ariaCopy.errorText(ariaCopy.text("text.25db4330ae7389"))), { status: 404 });
            window.ariaCart.register([p]);

            let isFav = false;
            if (ariaAuth.isLoggedIn()) {
                const favRes = await ariaAuth.authFetch('/api/favorites/check/' + productId);
                if (favRes.ok) isFav = (await favRes.json()).isFavorite;
            }
            const displayProduct = Object.assign({}, p, { imageUrl: p.imageUrl || (d && d.imageUrl) || "" });
            const displayPrice = Number(p.displayPrice ?? p.unitPrice);
            const unitPrice = Number(p.unitPrice);
            const oldPrice = Number.isFinite(displayPrice) && Number.isFinite(unitPrice) && displayPrice < unitPrice
                ? '<del class="product-detail-old-price">' + ariaMoney.format(unitPrice) + '</del>'
                : '';
            // Статус на русском
            const statusLabels = { 'ACTIVE': ariaCopy.text("text.2ad1287736b3d1"), 'DISCOUNTED': ariaCopy.text("text.d20b78b59fb4a9") };
            const statusText = statusLabels[p.status] || p.status;

            ariaCopy.setProperty(document.getElementById('product'),"innerHTML",'<div class="glass-card" style="padding:0;overflow:hidden;">' +
                CatalogImages.imageTag(displayProduct, { className: "product-main-image", alt: p.name, style: "width:100%;height:400px;object-fit:cover;", creditTarget: "product-image-credit" }) +
                ariaCopy.text("text.9d09c570a8fcdb") + CatalogImages.creditFor(displayProduct) + '</small>' +
                '</div>' +
                '<div>' +
                '<h1 style="margin-bottom:10px;">' + escapeHtml(p.name) + '</h1>' +
                ariaCopy.text("text.696158010744ad") + escapeHtml(p.sku) + ' · ' + escapeHtml(p.category || '') + '</div>' +
                ariaCopy.text("text.3b25cfb6a3146a") + escapeHtml(d && d.descriptionLong ? d.descriptionLong : ariaCopy.text("text.6cfd4fc43a52c3")) + '</p></section>' +
                '<div class="product-detail-price">' + ariaMoney.format(displayPrice) + oldPrice + '</div>' +

                // Характеристики
                (d ? '<div style="margin-bottom:20px;padding:15px;background:var(--bg-surface);border-radius:10px;">' +
                    ariaCopy.text("text.d501d021acfd29") +
                    (d.brand ? ariaCopy.text("text.0a621b4cbdc085") + escapeHtml(d.brand) + '</div>' : '') +
                    (d.color ? ariaCopy.text("text.2e16adf792c02f") + escapeHtml(d.color) + '</div>' : '') +
                    (d.size ? ariaCopy.text("text.dfe2277e3ad111") + escapeHtml(d.size) + '</div>' : '') +
                    (d.weightKg ? ariaCopy.text("text.9bdfba85e185a9") + d.weightKg + ariaCopy.text("text.7dc6e399f30608") : '') +
                    '</div>' : '') +

                // Информация о товаре
                '<div style="margin-bottom:20px;padding:15px;background:var(--bg-surface);border-radius:10px;">' +
                ariaCopy.text("text.bf4654328c1093") +
                ariaCopy.text("text.a7a915c4e3eb1d") + p.quantityOnHand + ariaCopy.text("text.b467c5a9493076") +
                (p.expiryDate ? '<div style="margin-bottom:8px;"><b>' + (p.provenance?.demonstration ? ariaCopy.text("text.d779ade0b65a43") : ariaCopy.text("text.1b706a30d3f8dd")) + '</b> ' + new Date(p.expiryDate + 'T12:00:00').toLocaleDateString(ariaCopy.locale()) + '</div>' : '') +
                ariaCopy.text("text.10e12b90ee1701") + statusText + '</div>' +
                '</div>' +

                // Кнопки
                '<div style="display:flex;gap:10px;">' +
                window.ariaCart.control(p) +
                '<button type="button" class="btn-outline" aria-pressed="' + isFav + '" onclick="toggleFavorite(' + p.id + ',this)">' + (isFav ? ariaCopy.text("text.6a5df732885c30") : ariaCopy.text("text.d6e7b0dcd513ff")) + '</button>' +
                '</div>' +
                '</div>');

            ariaCopy.setProperty(document,"title",p.name + ' — ARIA Shop');
            window.dispatchEvent(new CustomEvent('aria:product-loaded', {detail: p}));
            if (reviews) reviews.hidden = false;
            loadReviews();
        } catch(e) {
            if (reviews) reviews.hidden = true;
            document.getElementById('similar-products')?.setAttribute('hidden', 'hidden');
            ariaCopy.setProperty(document.getElementById('product'),"innerHTML",'<section class="product-not-found glass-card"><span aria-hidden="true">⌕</span><h1>' + (e.status === 404 ? ariaCopy.text("text.25db4330ae7389") : ariaCopy.text("text.8fe1e80b485e72")) + ariaCopy.text("text.026ae45a12874a"));
        }
    }

    ariaTheme.initTheme();
    ariaI18n.initI18n();
    loadCartCount();
    loadProduct();
