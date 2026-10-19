let productId = null;

    function escapeHtml(value) {
        return String(value ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
    }

    async function loadCartCount() {
        if (typeof window.refreshCartBadge === 'function') await window.refreshCartBadge();
    }

    async function addToCart(id) {
        if (!ariaAuth.requireLogin()) return;
        try {
            const response = await ariaAuth.authFetch('/api/cart/add?productId=' + encodeURIComponent(id) + '&qty=1', { method: 'POST' });
            if (!response.ok) throw new Error('Не удалось добавить товар в корзину');
            await loadCartCount();
            window.showToast('Товар добавлен в корзину');
        } catch(e) { window.showToast(e.message || 'Ошибка добавления', 'error'); }
    }

    async function toggleFavorite(id, btn) {
        if (!ariaAuth.requireLogin()) return;
        try {
            const res = await ariaAuth.authFetch('/api/favorites/' + encodeURIComponent(id), { method: 'POST' });
            if (!res.ok) throw new Error('Не удалось обновить избранное');
            const data = await res.json();
            if (data.isFavorite) {
                btn.textContent = '❤️ В избранном';
                window.showToast('Добавлено в избранное');
            } else {
                btn.textContent = '🤍 В избранное';
                window.showToast('Удалено из избранного');
            }
        } catch(e) { window.showToast(e.message || 'Ошибка', 'error'); }
    }

    async function loadReviews() {
        try {
            const res = await fetch('/api/reviews/product/' + encodeURIComponent(productId));
            if (!res.ok) throw new Error('Не удалось загрузить отзывы');
            const reviews = await res.json();
            const list = document.getElementById('reviews-list');
            if (!Array.isArray(reviews) || reviews.length === 0) {
                list.innerHTML = '<p style="color:var(--text-muted);padding:20px;text-align:center;" data-i18n="product.noReviews">Пока нет отзывов. Будьте первым!</p>';
                return;
            }
            list.innerHTML = reviews.map(r =>
                '<div class="glass-card" style="padding:16px;margin-bottom:10px;">' +
                '<div style="display:flex;justify-content:space-between;margin-bottom:8px;">' +
                '<strong>' + escapeHtml(r.authorName) + '</strong>' +
                '<span style="color:var(--accent-orange);">' + '★'.repeat(r.rating) + '☆'.repeat(5 - r.rating) + '</span>' +
                '</div>' +
                '<p style="margin:0;color:var(--text-secondary);line-height:1.6;">' + escapeHtml(r.comment) + '</p>' +
                '<div style="font-size:12px;color:var(--text-muted);margin-top:8px;">' + new Date(r.createdAt).toLocaleString('ru-RU') + '</div>' +
                '</div>'
            ).join('');
        } catch(e) {
            document.getElementById('reviews-list').innerHTML = '<p style="color:var(--text-muted);">Не удалось загрузить отзывы</p>';
        }
    }

    async function submitReview() {
        if (!productId || document.getElementById('reviews').hidden) return;
        const author = document.getElementById('review-author').value || 'Аноним';
        const comment = document.getElementById('review-comment').value;
        const rating = parseInt(document.getElementById('review-rating').value);
        if (!comment.trim()) { window.showToast(ariaI18n.t('product.reviewError'), 'error'); return; }
        try {
            const res = await fetch('/api/reviews', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ productId, author, comment, rating })
            });
            if (!res.ok) throw new Error('Не удалось сохранить отзыв');
            window.showToast(ariaI18n.t('toast.reviewAdded'));
            document.getElementById('review-comment').value = '';
            loadReviews();
        } catch(e) { window.showToast(ariaI18n.t('toast.error'), 'error'); }
    }

    async function loadProduct() {
        const params = new URLSearchParams(location.search);
        productId = params.get('id');
        if (!productId) { location.href = '/index.html'; return; }
        const reviews = document.getElementById('reviews');
        if (reviews) reviews.hidden = true;

        try {
            const res = await fetch('/api/products/' + encodeURIComponent(productId));
            if (!res.ok) throw Object.assign(new Error('Товар не найден'), { status: res.status });
            const data = await res.json();
            const p = data.product;
            const d = data.details;
            if (!p || !p.id) throw Object.assign(new Error('Товар не найден'), { status: 404 });

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
            const statusLabels = { 'ACTIVE': 'В продаже', 'DISCOUNTED': 'Специальная цена' };
            const statusText = statusLabels[p.status] || p.status;

            document.getElementById('product').innerHTML =
                '<div class="glass-card" style="padding:0;overflow:hidden;">' +
                CatalogImages.imageTag(displayProduct, { className: "product-main-image", alt: p.name, style: "width:100%;height:400px;object-fit:cover;", creditTarget: "product-image-credit" }) +
                '<small id="product-image-credit" class="image-credit" hidden>Изображение: ' + CatalogImages.creditFor(displayProduct) + '</small>' +
                '</div>' +
                '<div>' +
                '<h1 style="margin-bottom:10px;">' + escapeHtml(p.name) + '</h1>' +
                '<div style="color:var(--text-muted);margin-bottom:20px;">Код: ' + escapeHtml(p.sku) + ' · ' + escapeHtml(p.category || '') + '</div>' +
                '<section class="product-description"><h2>Описание товара</h2><p>' + escapeHtml(d && d.descriptionLong ? d.descriptionLong : 'Подробное описание пока не добавлено.') + '</p></section>' +
                '<div class="product-detail-price">' + ariaMoney.format(displayPrice) + oldPrice + '</div>' +

                // Характеристики
                (d ? '<div style="margin-bottom:20px;padding:15px;background:var(--bg-surface);border-radius:10px;">' +
                    '<h3 style="margin-bottom:12px;font-size:16px;">Характеристики</h3>' +
                    (d.brand ? '<div style="margin-bottom:8px;"><b>Бренд:</b> ' + escapeHtml(d.brand) + '</div>' : '') +
                    (d.color ? '<div style="margin-bottom:8px;"><b>Цвет:</b> ' + escapeHtml(d.color) + '</div>' : '') +
                    (d.size ? '<div style="margin-bottom:8px;"><b>Размер:</b> ' + escapeHtml(d.size) + '</div>' : '') +
                    (d.weightKg ? '<div style="margin-bottom:8px;"><b>Вес:</b> ' + d.weightKg + ' кг</div>' : '') +
                    '</div>' : '') +

                // Информация о товаре
                '<div style="margin-bottom:20px;padding:15px;background:var(--bg-surface);border-radius:10px;">' +
                '<h3 style="margin-bottom:12px;font-size:16px;">Информация</h3>' +
                '<div style="margin-bottom:8px;"><b>В наличии:</b> ' + p.quantityOnHand + ' шт.</div>' +
                (p.expiryDate ? '<div style="margin-bottom:8px;"><b>Срок годности:</b> ' + new Date(p.expiryDate).toLocaleDateString('ru-RU') + '</div>' : '') +
                '<div><b>Статус:</b> ' + statusText + '</div>' +
                '</div>' +

                // Кнопки
                '<div style="display:flex;gap:10px;">' +
                '<button class="btn-add-cart" onclick="addToCart(' + p.id + ')" style="flex:1;">Добавить в корзину</button>' +
                '<button class="btn-outline" onclick="toggleFavorite(' + p.id + ',this)">' + (isFav ? '❤️ В избранном' : '🤍 В избранное') + '</button>' +
                '</div>' +
                '</div>';

            if (reviews) reviews.hidden = false;
            loadReviews();
        } catch(e) {
            if (reviews) reviews.hidden = true;
            document.getElementById('similar-products')?.setAttribute('hidden', 'hidden');
            document.getElementById('product').innerHTML = '<section class="product-not-found glass-card"><span aria-hidden="true">⌕</span><h1>' + (e.status === 404 ? 'Товар не найден' : 'Не удалось загрузить товар') + '</h1><p>Проверьте ссылку или выберите товар в каталоге.</p><a class="btn-glow" href="/index.html">Вернуться в каталог</a></section>';
        }
    }

    ariaTheme.initTheme();
    ariaI18n.initI18n();
    loadCartCount();
    loadProduct();
