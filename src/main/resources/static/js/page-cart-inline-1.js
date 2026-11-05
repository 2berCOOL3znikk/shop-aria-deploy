function ariaEscape(value){return String(value??'').replace(/[&<>\"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;',"'":'&#39;'}[c]));}
let items = [];
let cartBusy = false;
let deliveryAddress = '';

    async function cartResponse(response) {
        const data = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(ariaCopy.errorText(data.message || data.error || ariaCopy.text("text.22a30634fc0998")));
        return data;
    }

    async function cartAction(action) {
        if (cartBusy) return;
        cartBusy = true;
        deliveryAddress = document.getElementById('delivery-address')?.value ?? deliveryAddress;
        const buttons = [...document.querySelectorAll('#cart-items button, #cart-total button')];
        buttons.forEach(button => button.disabled = true);
        try { await action(); }
        catch (error) { window.showToast(error.message || ariaCopy.text("text.c2fda52a47c4ab"), 'error'); }
        finally { cartBusy = false; render(); }
    }

    async function loadCartCount() {
        if (typeof window.refreshCartBadge === 'function') await window.refreshCartBadge();
    }

    async function loadCart() {
        if (!ariaAuth.requireLogin()) return;
        try {
            await Promise.all([window.ariaCart.refresh(), window.ariaCart.loadStocks().catch(() => window.showToast(ariaCopy.text("text.3f385b7d25c86d"), 'warning'))]);
            items = window.ariaCart.snapshot();
            render();
        } catch(e) {
            ariaCopy.setProperty(document.getElementById('cart-items'),"innerHTML",ariaCopy.text("text.f03e480d54d2f9"));
            window.showToast(e.message || ariaCopy.text("text.f05394458b036f"), 'error');
        }
    }

    function render() {
        const addressInput = document.getElementById('delivery-address');
        deliveryAddress = addressInput?.value ?? deliveryAddress;
        const addressFocused = document.activeElement === addressInput;
        const selection = addressFocused ? [addressInput.selectionStart, addressInput.selectionEnd] : null;
        const box = document.getElementById('cart-items');
        if (items.length === 0) {
            ariaCopy.setProperty(box,"innerHTML",ariaCopy.text("text.5720a48f5e2aea"));
            ariaCopy.setProperty(document.getElementById('cart-total'),"innerHTML",'');
            return;
        }
        ariaCopy.setProperty(box,"innerHTML",items.map(it =>
            '<div class="glass-card cart-line" data-cart-line="' + Number(it.productId) + '">' +
            CatalogImages.imageTag({ productName: it.productName, productSku: it.productSku, category: it.category, imageUrl: it.imageUrl }, { className: "cart-item-image", alt: it.productName, style: "width:80px;height:80px;object-fit:cover;border-radius:8px;" }) +
            '<div class="cart-line-description">' +
            '<h3 style="margin:0 0 5px 0;"><a class="cart-line-title" href="/product.html?id=' + encodeURIComponent(it.productId) + '">' + ariaEscape(it.productName) + '</a></h3>' +
            ariaCopy.text("text.d9ae1c6338a55a") + ariaEscape(it.productSku) + '</div>' +
            '<div style="color:var(--accent-blue);font-weight:700;margin-top:5px;">' + ariaMoney.format(it.priceAtAddTime) + '</div>' +
            '</div>' +
            window.ariaCart.control({id:it.productId}) +
            '<div class="cart-line-total">' + ariaMoney.format(it.priceAtAddTime * it.quantity) + '</div>' +
            ariaCopy.text("text.66cf0d7f2cf52b") + it.id + ')" ' + (cartBusy || window.ariaCart.isBusy() ? 'disabled' : '') + '>✕</button>' +
            '</div>'
        ).join(''));

        const total = items.reduce((sum, it) => sum + it.priceAtAddTime * it.quantity, 0);
        ariaCopy.setProperty(document.getElementById('cart-total'),"innerHTML",ariaCopy.text("text.764a7a72863a86") + ariaEscape(deliveryAddress) + ariaCopy.text("text.188f69e5c0b98e") +
            '<div class="cart-checkout-row">' +
            ariaCopy.text("text.4be8f13e5ac938") + ariaMoney.format(total) + '</b></div>' +
            '<button class="btn-glow" onclick="checkout()" data-i18n="cart.checkout" ' + (cartBusy || window.ariaCart.isBusy() ? 'disabled' : '') + ariaCopy.text("text.dd7a30a1e6b5da") +
            '</div>');
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
            window.showToast(ariaCopy.text("text.2a5b6066502662"));
        });
    }

    async function checkout() {
        const address = document.getElementById('delivery-address')?.value || '';
        if (!address.trim()) { window.showToast(ariaCopy.text("text.62584299bd8478"), 'error'); return; }
        return cartAction(async () => {
            const res = await ariaAuth.authFetch('/api/orders/checkout', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ address: address.trim() })
            });
            const data = await cartResponse(res);
            window.showToast(ariaCopy.text("text.dc5a73c517b9f5") + data.id + ariaCopy.text("text.93a168730ee5ab") + data.status);
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
