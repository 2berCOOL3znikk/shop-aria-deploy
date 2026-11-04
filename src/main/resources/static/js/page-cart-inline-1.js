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
        finally { cartBusy = false; render(); }
    }

    async function loadCartCount() {
        if (typeof window.refreshCartBadge === 'function') await window.refreshCartBadge();
    }

    async function loadCart() {
        if (!ariaAuth.requireLogin()) return;
        try {
            await Promise.all([window.ariaCart.refresh(), window.ariaCart.loadStocks().catch(() => window.showToast('Не удалось проверить наличие. Цена и количество будут проверены сервером перед заказом.', 'warning'))]);
            items = window.ariaCart.snapshot();
            render();
        } catch(e) {
            document.getElementById('cart-items').innerHTML = '<div class="glass-card" role="alert" style="padding:24px;"><p>Не удалось загрузить корзину. Сохранённые товары не потеряны.</p><button class="btn-outline" type="button" onclick="loadCart()">Повторить</button></div>';
            window.showToast(e.message || 'Ошибка загрузки корзины', 'error');
        }
    }

    function render() {
        const addressInput = document.getElementById('delivery-address');
        deliveryAddress = addressInput?.value ?? deliveryAddress;
        const addressFocused = document.activeElement === addressInput;
        const selection = addressFocused ? [addressInput.selectionStart, addressInput.selectionEnd] : null;
        const box = document.getElementById('cart-items');
        if (items.length === 0) {
            box.innerHTML = '<p style="text-align:center;color:var(--text-muted);padding:40px;"><span data-i18n="cart.empty">Корзина пуста. </span><a href="/index.html" style="color:var(--accent-blue);" data-i18n="cart.emptyLink">Перейти в каталог</a></p>';
            document.getElementById('cart-total').innerHTML = '';
            return;
        }
        box.innerHTML = items.map(it =>
            '<div class="glass-card cart-line" data-cart-line="' + Number(it.productId) + '">' +
            CatalogImages.imageTag({ productName: it.productName, productSku: it.productSku, category: it.category, imageUrl: it.imageUrl }, { className: "cart-item-image", alt: it.productName, style: "width:80px;height:80px;object-fit:cover;border-radius:8px;" }) +
            '<div class="cart-line-description">' +
            '<h3 style="margin:0 0 5px 0;"><a class="cart-line-title" href="/product.html?id=' + encodeURIComponent(it.productId) + '">' + ariaEscape(it.productName) + '</a></h3>' +
            '<div style="color:var(--text-muted);font-size:12px;">Артикул: ' + ariaEscape(it.productSku) + '</div>' +
            '<div style="color:var(--accent-blue);font-weight:700;margin-top:5px;">' + ariaMoney.format(it.priceAtAddTime) + '</div>' +
            '</div>' +
            window.ariaCart.control({id:it.productId}) +
            '<div class="cart-line-total">' + ariaMoney.format(it.priceAtAddTime * it.quantity) + '</div>' +
            '<button class="btn-icon cart-line-remove" type="button" aria-label="Удалить товар" onclick="removeItem(' + it.id + ')" ' + (cartBusy || window.ariaCart.isBusy() ? 'disabled' : '') + '>✕</button>' +
            '</div>'
        ).join('');

        const total = items.reduce((sum, it) => sum + it.priceAtAddTime * it.quantity, 0);
        document.getElementById('cart-total').innerHTML =
            '<label class="cart-address-label" for="delivery-address">Адрес доставки</label><input id="delivery-address" type="text" maxlength="500" autocomplete="street-address" value="' + ariaEscape(deliveryAddress) + '" placeholder="Город, улица, дом, квартира" aria-label="Адрес доставки">' +
            '<div class="cart-checkout-row">' +
            '<div><b style="font-size:20px;"><span data-i18n="cart.total">Итого:</span> ' + ariaMoney.format(total) + '</b></div>' +
            '<button class="btn-glow" onclick="checkout()" data-i18n="cart.checkout" ' + (cartBusy || window.ariaCart.isBusy() ? 'disabled' : '') + '>Оформить заказ</button>' +
            '</div>';
        if (cartBusy) box.querySelectorAll('button').forEach(button => button.disabled = true);
        if (addressFocused) {const input=document.getElementById('delivery-address');input.focus({preventScroll:true});input.setSelectionRange(...selection);}
    }

    async function changeQty(id, newQty) {
        return cartAction(async () => {
            const item=items.find(row=>Number(row.id)===Number(id));
            if (item) await window.ariaCart.adjust(item.productId, newQty-item.quantity);
        });
    }

    async function removeItem(id) {
        return cartAction(async () => {
            const item=items.find(row=>Number(row.id)===Number(id));
            if (item) await window.ariaCart.remove(item.productId);
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
            window.dispatchEvent(new Event('aria:order-recorded'));
            items = [];
            render();
            await loadCartCount();
        });
    }

    ariaTheme.initTheme();
    ariaI18n.initI18n();
    window.addEventListener('aria:cart-changed',event=>{if(event.detail.loaded){items=event.detail.items;render();}});
    loadCart();
