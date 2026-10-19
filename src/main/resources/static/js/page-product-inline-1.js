let productId = null;

    function escapeHtml(value) {
        return String(value ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
    }

    function showToast(msg, type = 'success') {
        const container = document.getElementById('toast-container');
        const toast = document.createElement('div');
        toast.className = 'toast ' + type;
        const icon = type === 'error' ? '❌' : type === 'warning' ? '⚠️' : '✅';
        toast.innerHTML = '<span class="toast-icon">' + icon + '</span><span class="toast-message">' + msg + '</span>';
        container.appendChild(toast);
        setTimeout(() => toast.remove(), 3500);
    }

    function updateAuthNav() {
        const authNav = document.getElementById('auth-nav');
        if (ariaAuth.isLoggedIn()) {
            authNav.innerHTML = '<span style="font-size:14px;color:var(--text-muted);margin-right:15px;">' + ariaAuth.getUserEmail() + '</span>' +
                '<button onclick="ariaAuth.logout()" style="background:transparent;border:1px solid var(--border-color);color:var(--text-primary);padding:6px 12px;border-radius:6px;cursor:pointer;font-size:13px;">Выйти</button>';
        } else {
            authNav.innerHTML = '<a href="/login.html" style="color:var(--accent-blue);text-decoration:none;font-size:14px;">Войти</a>';
        }
    }

    async function loadCartCount() {
        if (!ariaAuth.isLoggedIn()) return;
        try {
            const res = await ariaAuth.authFetch('/api/cart/total');
            const data = await res.json();
            document.getElementById('cart-count').textContent = data.itemCount || 0;
        } catch(e) {}
    }

    async function addToCart(id) {
        if (!ariaAuth.requireLogin()) return;
        try {
            await ariaAuth.authFetch('/api/cart/add?productId=' + id + '&qty=1', { method: 'POST' });
            await loadCartCount();
            showToast('Товар добавлен в корзину');
        } catch(e) { showToast('Ошибка добавления', 'error'); }
    }

    async function toggleFavorite(id, btn) {
        if (!ariaAuth.requireLogin()) return;
        try {
            const res = await ariaAuth.authFetch('/api/favorites/' + id, { method: 'POST' });
            const data = await res.json();
            if (data.success) {
                btn.textContent = '❤️ В избранном';
                showToast('Добавлено в избранное');
            } else {
                btn.textContent = '🤍 В избранное';
                showToast('Удалено из избранного');
            }
        } catch(e) { showToast('Ошибка', 'error'); }
    }

    async function loadReviews() {
        try {
            const res = await fetch('/api/reviews/product/' + productId);
            const reviews = await res.json();
            const list = document.getElementById('reviews-list');
            if (reviews.length === 0) {
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
        const author = document.getElementById('review-author').value || 'Аноним';
        const comment = document.getElementById('review-comment').value;
        const rating = parseInt(document.getElementById('review-rating').value);
        if (!comment) { showToast(ariaI18n.t('product.reviewError'), 'error'); return; }
        try {
            const res = await fetch('/api/reviews', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ productId, author, comment, rating })
            });
            if (!res.ok) throw new Error('Не удалось сохранить отзыв');
            showToast(ariaI18n.t('toast.reviewAdded'));
            document.getElementById('review-comment').value = '';
            loadReviews();
        } catch(e) { showToast(ariaI18n.t('toast.error'), 'error'); }
    }

    async function loadProduct() {
        const params = new URLSearchParams(location.search);
        productId = params.get('id');
        if (!productId) { location.href = '/index.html'; return; }

        try {
            const res = await fetch('/api/products/' + productId);
            const data = await res.json();
            const p = data.product;
            const d = data.details;

            let isFav = false;
            if (ariaAuth.isLoggedIn()) {
                const favRes = await ariaAuth.authFetch('/api/favorites/check/' + productId);
                if (favRes.ok) isFav = (await favRes.json()).isFavorite;
            }
            const displayProduct = Object.assign({}, p, { imageUrl: p.imageUrl || (d && d.imageUrl) || "" });
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
                '<div style="font-size:32px;font-weight:700;color:var(--accent-blue);margin-bottom:20px;">' + ariaMoney.format(p.displayPrice ?? p.unitPrice) + '</div>' +

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
                '<button class="btn-glow" onclick="addToCart(' + p.id + ')" style="flex:1;">Добавить в корзину</button>' +
                '<button class="btn-outline" onclick="toggleFavorite(' + p.id + ',this)">' + (isFav ? '❤️ В избранном' : '🤍 В избранное') + '</button>' +
                '</div>' +
                '</div>';

            loadReviews();
        } catch(e) {
            document.getElementById('product').innerHTML = '<p style="grid-column:1/-1;text-align:center;color:var(--accent-red);padding:60px;">Товар не найден</p>';
        }
    }

    updateAuthNav();
    ariaTheme.initTheme();
    ariaI18n.initI18n();
    loadCartCount();
    loadProduct();
