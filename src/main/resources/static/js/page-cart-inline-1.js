function ariaEscape(value){return String(value??'').replace(/[&<>\"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;',"'":'&#39;'}[c]));}
let items = [];
let cartBusy = false;
let deliveryAddress = '';

    async function cartResponse(response) {
        const data = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(data.message || data.error || 'Сервис временно недоступен. Повторите попытку.');
        return data;
    }

    async function cartAction(action) {
        if (cartBusy) return;
        cartBusy = true;
        deliveryAddress = document.getElementById('delivery-address')?.value ?? deliveryAddress;
        const buttons = [...document.querySelectorAll('#cart-items button, #cart-total button')];
        buttons.forEach(button => button.disabled = true);
        try { await action(); }
        catch (error) { window.showToast(error.message || 'Не удалось связаться с сервером. Проверьте результат перед повтором.', 'error'); }
        finally { cartBusy = false; buttons.forEach(button => button.disabled = false); }
    }

    async function loadCartCount() {
        if (typeof window.refreshCartBadge === 'function') await window.refreshCartBadge();
    }

    async function loadCart() {
        if (!ariaAuth.requireLogin()) return;
        try {
            const res = await ariaAuth.authFetch('/api/cart');
            const data = await cartResponse(res);
            if (!Array.isArray(data)) throw new Error('Не удалось загрузить корзину');
            items = data;
            render();
            await loadCartCount();
        } catch(e) { window.showToast(e.message || 'Ошибка загрузки корзины', 'error'); }
    }

    function render() {
        const box = document.getElementById('cart-items');
        if (items.length === 0) {
            box.innerHTML = '<p style="text-align:center;color:var(--text-muted);padding:40px;"><span data-i18n="cart.empty">Корзина пуста. </span><a href="/index.html" style="color:var(--accent-blue);" data-i18n="cart.emptyLink">Перейти в каталог</a></p>';
            document.getElementById('cart-total').innerHTML = '';
            return;
        }
        box.innerHTML = items.map(it =>
            '<div class="glass-card cart-line">' +
            CatalogImages.imageTag({ productName: it.productName, productSku: it.productSku, category: it.category, imageUrl: it.imageUrl }, { className: "cart-item-image", alt: it.productName, style: "width:80px;height:80px;object-fit:cover;border-radius:8px;" }) +
            '<div class="cart-line-description">' +
            '<h3 style="margin:0 0 5px 0;">' + ariaEscape(it.productName) + '</h3>' +
            '<div style="color:var(--text-muted);font-size:12px;">SKU: ' + ariaEscape(it.productSku) + '</div>' +
            '<div style="color:var(--accent-blue);font-weight:700;margin-top:5px;">' + ariaMoney.format(it.priceAtAddTime) + '</div>' +
            '</div>' +
            '<div style="display:flex;align-items:center;gap:10px;">' +
            '<button class="btn-icon" onclick="changeQty(' + it.id + ',' + (it.quantity - 1) + ')" style="background:var(--bg-surface);border:1px solid var(--border-color);color:var(--text-primary);">−</button>' +
            '<span style="min-width:30px;text-align:center;">' + it.quantity + '</span>' +
            '<button class="btn-icon" onclick="changeQty(' + it.id + ',' + (it.quantity + 1) + ')" style="background:var(--bg-surface);border:1px solid var(--border-color);color:var(--text-primary);">+</button>' +
            '</div>' +
            '<div style="font-weight:700;min-width:100px;text-align:right;">' + ariaMoney.format(it.priceAtAddTime * it.quantity) + '</div>' +
            '<button class="btn-icon" aria-label="Удалить товар" onclick="removeItem(' + it.id + ')" style="background:transparent;border:none;color:var(--accent-red);cursor:pointer;font-size:20px;">✕</button>' +
            '</div>'
        ).join('');

        const total = items.reduce((sum, it) => sum + it.priceAtAddTime * it.quantity, 0);
        document.getElementById('cart-total').innerHTML =
            '<label class="cart-address-label" for="delivery-address">Адрес доставки</label><input id="delivery-address" type="text" maxlength="500" autocomplete="street-address" value="' + ariaEscape(deliveryAddress) + '" placeholder="Город, улица, дом, квартира" aria-label="Адрес доставки">' +
            '<div class="cart-checkout-row">' +
            '<div><b style="font-size:20px;"><span data-i18n="cart.total">Итого:</span> ' + ariaMoney.format(total) + '</b></div>' +
            '<button class="btn-glow" onclick="checkout()" data-i18n="cart.checkout">Оформить заказ</button>' +
            '</div>';
    }

    async function changeQty(id, newQty) {
        return cartAction(async () => {
            const res = await ariaAuth.authFetch('/api/cart/' + id + '?qty=' + newQty, {method:'PUT'});
            await cartResponse(res);
            await loadCart();
        });
    }

    async function removeItem(id) {
        return cartAction(async () => {
            await cartResponse(await ariaAuth.authFetch('/api/cart/' + id, { method: 'DELETE' }));
            await loadCart();
            window.showToast('Товар удалён');
        });
    }

    async function checkout() {
        const address = document.getElementById('delivery-address')?.value || '';
        if (!address.trim()) { window.showToast('Укажите адрес доставки', 'error'); return; }
        return cartAction(async () => {
            const res = await ariaAuth.authFetch('/api/orders/checkout', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ address: address.trim() })
            });
            const data = await cartResponse(res);
            window.showToast('Заказ #' + data.id + ' принят. Статус: ' + data.status);
            items = [];
            render();
            await loadCartCount();
        });
    }

    ariaTheme.initTheme();
    ariaI18n.initI18n();
    loadCart();
