let items = [];

    async function loadCartCount() {
        if (typeof window.refreshCartBadge === 'function') await window.refreshCartBadge();
    }

    async function loadCart() {
        if (!ariaAuth.requireLogin()) return;
        try {
            const res = await ariaAuth.authFetch('/api/cart');
            items = await res.json();
            render();
            await loadCartCount();
        } catch(e) {
            window.showToast('Ошибка загрузки корзины', 'error');
        }
    }

    function render() {
        const box = document.getElementById('cart-items');
        if (items.length === 0) {
            box.innerHTML = '<p style="text-align:center;color:var(--text-muted);padding:40px;"><span data-i18n="cart.empty">Корзина пуста. </span><a href="/index.html" style="color:var(--accent-blue);" data-i18n="cart.emptyLink">Перейти в каталог</a></p>';
            document.getElementById('cart-total').innerHTML = '';
            return;
        }
        box.innerHTML = items.map(it =>
            '<div class="glass-card" style="display:flex;align-items:center;gap:20px;padding:16px;margin-bottom:15px;">' +
            CatalogImages.imageTag({ productName: it.productName, productSku: it.productSku, category: it.category, imageUrl: it.imageUrl }, { className: "cart-item-image", alt: it.productName, style: "width:80px;height:80px;object-fit:cover;border-radius:8px;" }) +
            '<div style="flex:1;">' +
            '<h3 style="margin:0 0 5px 0;">' + it.productName + '</h3>' +
            '<div style="color:var(--text-muted);font-size:12px;">SKU: ' + it.productSku + '</div>' +
            '<div style="color:var(--accent-blue);font-weight:700;margin-top:5px;">' + ariaMoney.format(it.priceAtAddTime) + '</div>' +
            '</div>' +
            '<div style="display:flex;align-items:center;gap:10px;">' +
            '<button class="btn-icon" onclick="changeQty(' + it.id + ',' + (it.quantity - 1) + ')" style="background:var(--bg-surface);border:1px solid var(--border-color);color:var(--text-primary);">−</button>' +
            '<span style="min-width:30px;text-align:center;">' + it.quantity + '</span>' +
            '<button class="btn-icon" onclick="changeQty(' + it.id + ',' + (it.quantity + 1) + ')" style="background:var(--bg-surface);border:1px solid var(--border-color);color:var(--text-primary);">+</button>' +
            '</div>' +
            '<div style="font-weight:700;min-width:100px;text-align:right;">' + ariaMoney.format(it.priceAtAddTime * it.quantity) + '</div>' +
            '<button class="btn-icon" onclick="removeItem(' + it.id + ')" style="background:transparent;border:none;color:var(--accent-red);cursor:pointer;font-size:20px;">✕</button>' +
            '</div>'
        ).join('');

        const total = items.reduce((sum, it) => sum + it.priceAtAddTime * it.quantity, 0);
        document.getElementById('cart-total').innerHTML =
            '<label class="cart-address-label" for="delivery-address">Адрес доставки</label><input id="delivery-address" type="text" autocomplete="street-address" value="г. Кызылорда, Казахстан" aria-label="Адрес доставки">' +
            '<div style="display:flex;justify-content:space-between;align-items:center;">' +
            '<div><b style="font-size:20px;"><span data-i18n="cart.total">Итого:</span> ' + ariaMoney.format(total) + '</b></div>' +
            '<button class="btn-glow" onclick="checkout()" data-i18n="cart.checkout">Оформить заказ</button>' +
            '</div>';
    }

    async function changeQty(id, newQty) {
        try {
            const res = await ariaAuth.authFetch('/api/cart/' + id + '?qty=' + newQty, {method:'PUT'});
            const data = await res.json();
            if (!res.ok) throw new Error(data.message || data.error || 'Не удалось изменить количество');
            await loadCart();
        } catch(e) { window.showToast(e.message, 'error'); }
    }

    async function removeItem(id) {
        items = items.filter(i => i.id !== id);
        render();
        try {
            await ariaAuth.authFetch('/api/cart/' + id, { method: 'DELETE' });
            await loadCartCount();
            window.showToast('Товар удалён');
        } catch(e) { window.showToast('Ошибка', 'error'); }
    }

    async function checkout() {
        const address = document.getElementById('delivery-address')?.value || '';
        if (!address.trim()) { window.showToast('Укажите адрес доставки', 'error'); return; }
        try {
            const res = await ariaAuth.authFetch('/api/orders/checkout', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ address: address.trim() })
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Не удалось оформить заказ');
            window.showToast('Заказ #' + data.id + ' принят. Статус: ' + data.status);
            items = [];
            render();
            await loadCartCount();
        } catch(e) { window.showToast('Ошибка оформления', 'error'); }
    }

    ariaTheme.initTheme();
    ariaI18n.initI18n();
    loadCart();
