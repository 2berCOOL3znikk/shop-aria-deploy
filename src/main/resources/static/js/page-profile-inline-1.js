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
            if (!res.ok) throw new Error(ariaCopy.errorText(data.message || ariaCopy.text("text.7fd0b348008f2b")));
            document.getElementById('email').value = data.found ? (data.data.email || email) : email;
            if (data.found) {
                document.getElementById('email').value = data.data.email || '';
                document.getElementById('fullName').value = data.data.fullName || '';
                document.getElementById('phone').value = data.data.phone || '';
                document.getElementById('address').value = data.data.address || '';
            }
        } catch(e) { window.showToast(e.message || ariaCopy.text("text.7fd0b348008f2b"), 'error'); }
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
            if (!response.ok) throw new Error(ariaCopy.errorText(result.message || ariaCopy.text("text.6093b8a820a28f")));
            window.showToast(ariaCopy.text("text.d7ae5c2bcdb09d"));
        } catch(e) { window.showToast(e.message || ariaCopy.text("text.6093b8a820a28f"), 'error'); }
        finally { button.disabled = false; }
    }

    async function loadFavorites() {
        if (!ariaAuth.requireLogin()) return;
        try {
            const res = await ariaAuth.authFetch('/api/favorites');
            const items = await res.json();
            const box = document.getElementById('favorites-list');
            if (items.length === 0) {
                ariaCopy.setProperty(box,"innerHTML",ariaCopy.text("text.8f42a2c6d26960"));
                return;
            }
            ariaCopy.setProperty(box,"innerHTML",items.map(p =>
                '<div style="display:flex;align-items:center;gap:12px;padding:12px;border-bottom:1px solid var(--border-color);">' +
                CatalogImages.imageTag(p, { className: "profile-product-image", alt: p.name, style: "width:60px;height:60px;object-fit:cover;border-radius:8px;" }) +
                '<div style="flex:1;">' +
                '<div style="font-weight:600;">' + ariaEscape(p.name) + '</div>' +
                '<div style="color:var(--accent-blue);">' + ariaMoney.format(p.displayPrice ?? p.unitPrice) + '</div>' +
                '</div>' +
                '<a href="/product.html?id=' + p.id + ariaCopy.text("text.393ce2ee5c7916") +
                '</div>'
            ).join(''));
        } catch(e) {}
    }

    async function loadOrders() {
        if (!ariaAuth.requireLogin()) return;
        try {
            const res = await ariaAuth.authFetch('/api/orders');
            const orders = await res.json();
            const box = document.getElementById('orders');
            if (orders.length === 0) {
                ariaCopy.setProperty(box,"innerHTML",ariaCopy.text("text.b8498add958c23"));
                return;
            }
            ariaCopy.setProperty(box,"innerHTML",orders.map(o =>
                '<div class="glass-card" style="padding:16px;margin-bottom:15px;">' +
                '<div style="display:flex;justify-content:space-between;margin-bottom:10px;">' +
                ariaCopy.text("text.41aadaf378d125") + o.id + '</strong>' +
                '<span style="color:var(--accent-green);">' + ({PENDING:ariaCopy.text("text.f9a506077fa0d9"),SHIPPED:ariaCopy.text("text.0729a720f892d3"),DELIVERED:ariaCopy.text("text.ffb780a5ced6e9"),RETURNED:ariaCopy.text("text.ce05bd1e0d1ee5"),CANCELLED:ariaCopy.text("text.12e2261247cb0a")}[o.status] || o.status) + '</span>' +
                '</div>' +
                '<div style="color:var(--text-muted);font-size:13px;margin-bottom:10px;">' + new Date(o.createdAt).toLocaleString(ariaCopy.locale()) + '</div>' +
                '<div style="margin-bottom:10px;">' + o.items.map(i => ariaEscape(i.productName) + ' × ' + i.quantity).join(', ') + '</div>' +
                ariaCopy.text("text.651d9fbe702069") + ariaMoney.format(o.totalAmount) + '</div>' +
                '</div>'
            ).join(''));
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
    if(newPassword!==document.getElementById('repeat-password').value){ariaCopy.setProperty(message,"textContent",ariaCopy.text("text.6bf79d4284a76e"));return;}
    if(new TextEncoder().encode(newPassword).length>72){ariaCopy.setProperty(message,"textContent",ariaCopy.text("text.6c8665536aa1fd"));return;}
    button.disabled=true;ariaCopy.setProperty(message,"textContent",ariaCopy.text("text.2655340b7a26cb"));
    try{
        const response=await ariaAuth.authFetch('/api/profile/security/password',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({currentPassword,newPassword})});
        const data=await response.json().catch(()=>({}));
        if(!response.ok)throw new Error(ariaCopy.errorText(data.message||data.error||ariaCopy.text("text.1b2bcce3ede640")));
        localStorage.removeItem('aria_token');localStorage.removeItem('aria_role');localStorage.removeItem('aria_email');
        form.reset();ariaCopy.setProperty(message,"textContent",data.message);location.href='/login.html?passwordChanged=1';
    }catch(error){ariaCopy.setProperty(message,"textContent",error.message);}finally{button.disabled=false;}
});
