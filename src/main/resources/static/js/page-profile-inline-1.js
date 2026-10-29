function ariaEscape(value){return String(value??'').replace(/[&<>\"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;',"'":'&#39;'}[c]));}
    async function loadCartCount() {
        if (typeof window.refreshCartBadge === 'function') await window.refreshCartBadge();
    }

    async function loadProfile() {
        if (!ariaAuth.requireLogin()) return;
        try {
            const email = ariaAuth.getUserEmail();
            const res = await ariaAuth.authFetch('/api/profile/' + encodeURIComponent(email));
            const data = await res.json();
            if (!res.ok) throw new Error(data.message || 'Не удалось загрузить профиль');
            document.getElementById('email').value = data.found ? (data.data.email || email) : email;
            if (data.found) {
                document.getElementById('email').value = data.data.email || '';
                document.getElementById('fullName').value = data.data.fullName || '';
                document.getElementById('phone').value = data.data.phone || '';
                document.getElementById('address').value = data.data.address || '';
            }
        } catch(e) { window.showToast(e.message || 'Не удалось загрузить профиль', 'error'); }
    }

    async function saveProfile() {
        const button = document.getElementById('profile-save');
        if (button.disabled) return;
        button.disabled = true;
        try {
            const response = await ariaAuth.authFetch('/api/profile', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    email: document.getElementById('email').value,
                    fullName: document.getElementById('fullName').value,
                    phone: document.getElementById('phone').value,
                    address: document.getElementById('address').value
                })
            });
            const result = await response.json().catch(() => ({}));
            if (!response.ok) throw new Error(result.message || 'Не удалось сохранить профиль');
            window.showToast('Профиль сохранён');
        } catch(e) { window.showToast(e.message || 'Не удалось сохранить профиль', 'error'); }
        finally { button.disabled = false; }
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
                '<div style="font-weight:600;">' + ariaEscape(p.name) + '</div>' +
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
                '<span style="color:var(--accent-green);">' + ({PENDING:'Новый',SHIPPED:'Отправлен',DELIVERED:'Доставлен',RETURNED:'Возврат в карантин',CANCELLED:'Отменён'}[o.status] || o.status) + '</span>' +
                '</div>' +
                '<div style="color:var(--text-muted);font-size:13px;margin-bottom:10px;">' + new Date(o.createdAt).toLocaleString('ru-RU') + '</div>' +
                '<div style="margin-bottom:10px;">' + o.items.map(i => ariaEscape(i.productName) + ' × ' + i.quantity).join(', ') + '</div>' +
                '<div style="font-weight:700;color:var(--accent-blue);"><span data-i18n="cart.total">Итого:</span> ' + ariaMoney.format(o.totalAmount) + '</div>' +
                '</div>'
            ).join('');
        } catch(e) {}
    }

    ariaTheme.initTheme();
    ariaI18n.initI18n();
    loadCartCount();
    loadProfile();
    loadOrders();

document.getElementById('password-form').addEventListener('submit',async event=>{
    event.preventDefault();
    const form=event.currentTarget, message=document.getElementById('password-message'), button=form.querySelector('button');
    const currentPassword=document.getElementById('current-password').value, newPassword=document.getElementById('new-password').value;
    if(newPassword!==document.getElementById('repeat-password').value){message.textContent='Новые пароли не совпадают';return;}
    if(new TextEncoder().encode(newPassword).length>72){message.textContent='Пароль должен занимать не более 72 байт UTF-8';return;}
    button.disabled=true;message.textContent='Сохраняем новый пароль…';
    try{
        const response=await ariaAuth.authFetch('/api/profile/security/password',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({currentPassword,newPassword})});
        const data=await response.json().catch(()=>({}));
        if(!response.ok)throw new Error(data.message||data.error||'Не удалось изменить пароль');
        localStorage.removeItem('aria_token');localStorage.removeItem('aria_role');localStorage.removeItem('aria_email');
        form.reset();message.textContent=data.message;location.href='/login.html?passwordChanged=1';
    }catch(error){message.textContent=error.message;}finally{button.disabled=false;}
});
