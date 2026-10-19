function showToast(msg, type = 'success') {
        const container = document.getElementById('toast-container');
        const toast = document.createElement('div');
        toast.className = 'toast ' + type;
        toast.innerHTML = '<span class="toast-icon">' + (type === 'error' ? '❌' : '✅') + '</span><span class="toast-message">' + msg + '</span>';
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
        try {
            const res = await ariaAuth.authFetch('/api/cart/total');
            const data = await res.json();
            document.getElementById('cart-count').textContent = data.itemCount || 0;
        } catch(e) {}
    }

    async function loadProfile() {
        if (!ariaAuth.requireLogin()) return;
        try {
            const email = ariaAuth.getUserEmail();
            const res = await ariaAuth.authFetch('/api/profile/' + encodeURIComponent(email));
            const data = await res.json();
            document.getElementById('email').value = data.found ? (data.data.email || email) : email;
            if (data.found) {
                document.getElementById('email').value = data.data.email || '';
                document.getElementById('fullName').value = data.data.fullName || '';
                document.getElementById('phone').value = data.data.phone || '';
                document.getElementById('address').value = data.data.address || '';
            }
        } catch(e) {}
    }

    async function saveProfile() {
        try {
            await ariaAuth.authFetch('/api/profile', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    email: document.getElementById('email').value,
                    fullName: document.getElementById('fullName').value,
                    phone: document.getElementById('phone').value,
                    address: document.getElementById('address').value
                })
            });
            showToast('Профиль сохранён');
        } catch(e) { showToast('Ошибка', 'error'); }
    }

    async function loadFavorites() {
        if (!ariaAuth.requireLogin()) return;
        try {
            const res = await ariaAuth.authFetch('/api/favorites');
            const items = await res.json();
            const box = document.getElementById('favorites-list');
            if (items.length === 0) {
                box.innerHTML = '<p style="color:var(--text-muted);" data-i18n="profile.noFavorites">Нет избранных товаров</p>';
                return;
            }
            box.innerHTML = items.map(p =>
                '<div style="display:flex;align-items:center;gap:12px;padding:12px;border-bottom:1px solid var(--border-color);">' +
                CatalogImages.imageTag(p, { className: "profile-product-image", alt: p.name, style: "width:60px;height:60px;object-fit:cover;border-radius:8px;" }) +
                '<div style="flex:1;">' +
                '<div style="font-weight:600;">' + p.name + '</div>' +
                '<div style="color:var(--accent-blue);">' + ariaMoney.format(p.displayPrice ?? p.unitPrice) + '</div>' +
                '</div>' +
                '<a href="/product.html?id=' + p.id + '" class="btn-outline" style="text-decoration:none;padding:8px 12px;font-size:13px;" data-i18n="catalog.details">Открыть</a>' +
                '</div>'
            ).join('');
        } catch(e) {}
    }

    async function loadOrders() {
        if (!ariaAuth.requireLogin()) return;
        try {
            const res = await ariaAuth.authFetch('/api/orders');
            const orders = await res.json();
            const box = document.getElementById('orders');
            if (orders.length === 0) {
                box.innerHTML = '<div class="empty-state"><span aria-hidden="true">▣</span><p data-i18n="profile.noOrders">Заказов пока нет</p><a href="/index.html" class="btn-outline">Открыть каталог</a></div>';
                return;
            }
            box.innerHTML = orders.map(o =>
                '<div class="glass-card" style="padding:16px;margin-bottom:15px;">' +
                '<div style="display:flex;justify-content:space-between;margin-bottom:10px;">' +
                '<strong><span data-i18n="profile.orderNum">Заказ #</span>' + o.id + '</strong>' +
                '<span style="color:var(--accent-green);">' + o.status + '</span>' +
                '</div>' +
                '<div style="color:var(--text-muted);font-size:13px;margin-bottom:10px;">' + new Date(o.createdAt).toLocaleString('ru-RU') + '</div>' +
                '<div style="margin-bottom:10px;">' + o.items.map(i => i.productName + ' × ' + i.quantity).join(', ') + '</div>' +
                '<div style="font-weight:700;color:var(--accent-blue);"><span data-i18n="cart.total">Итого:</span> ' + ariaMoney.format(o.totalAmount) + '</div>' +
                '</div>'
            ).join('');
        } catch(e) {}
    }

    updateAuthNav();
    ariaTheme.initTheme();
    ariaI18n.initI18n();
    loadCartCount();
    loadProfile();
    loadOrders();
