const state = { products: [], favoriteIds: new Set(), cartCount: 0, allCategories: new Set() };

    function showToast(message, type = 'success') {
        message = String(message ?? '');

        // Автоматически различаем добавление/удаление даже если вызвали одним типом
        if (!type || type === 'success') {
            if (/удал|убран|removed|dismiss|снят|отмен|очищ/i.test(message)) type = 'remove';
            else if (/ошибк|error|не удалось|запрещ|доступ|403|401|500|пуст/i.test(message)) type = 'error';
            else if (/важно|вниман|warning|осторож|критич|срочн/i.test(message)) type = 'warning';
            else if (/инфо|info|справк|обнов/i.test(message)) type = 'info';
            else type = 'success';
        }

        const ICON = { success: "✓", info: "i", warning: "!", error: "✕", remove: "−" };
        const container = document.getElementById('toast-container') ||
            document.body.appendChild(Object.assign(document.createElement('div'), { id: 'toast-container', className: 'toast-container' }));

        const el = document.createElement('div');
        el.className = 'toast ' + type;
        el.innerHTML = '<span class="toast-icon">' + (ICON[type] || '•') + '</span><span class="toast-message"></span>';
        el.querySelector('.toast-message').textContent = message;
        container.appendChild(el);

        requestAnimationFrame(() => el.classList.add('show'));           // плавный вход

        let killed = false;
        const kill = () => {
            if (killed) return;
            killed = true;
            el.classList.remove('show');
            el.classList.add('out');                                     // плавный уход
            setTimeout(() => el.remove(), 340);
        };

        let timer = setTimeout(kill, 5200);                              // ~5 секунд — успеваешь прочитать
        el.addEventListener('mouseenter', () => clearTimeout(timer));   // пауза под курсором
        el.addEventListener('mouseleave', () => { timer = setTimeout(kill, 1400); });

        return el;
    }

    function escapeHtml(value) {
        return String(value ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
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
            state.cartCount = data.itemCount || 0;
            document.getElementById('cart-count').textContent = state.cartCount;
        } catch(e) { console.error(e); }
    }

    async function loadFavorites() {
        if (!ariaAuth.isLoggedIn()) { state.favoriteIds.clear(); return; }
        try {
            const res = await ariaAuth.authFetch('/api/favorites/ids');
            const data = await res.json();
            state.favoriteIds = new Set(data.ids || []);
        } catch(e) { console.error(e); }
    }

    async function toggleFavorite(productId, btn) {
        if (!ariaAuth.requireLogin()) return;
        try {
            const res = await ariaAuth.authFetch('/api/favorites/' + productId, { method: 'POST' });
            const data = await res.json();
            if (data.isFavorite) {
                state.favoriteIds.add(productId);
                btn.innerHTML = '♥';
                btn.classList.add('is-active', 'pop');
                setTimeout(() => btn.classList.remove('pop'), 340);
                showToast('Добавлено в избранное');
            } else {
                state.favoriteIds.delete(productId);
                btn.innerHTML = '♡';
                btn.classList.remove('is-active');
                btn.classList.add('pop');
                setTimeout(() => btn.classList.remove('pop'), 340);
                showToast('Удалено из избранного');
            }
        } catch(e) { showToast('Ошибка', 'error'); }
    }

    async function addToCart(productId) {
        if (!ariaAuth.requireLogin()) return;
        try {
            await ariaAuth.authFetch('/api/cart/add?productId=' + productId + '&qty=1', { method: 'POST' });
            await loadCartCount();
            showToast('Товар добавлен в корзину');
        } catch(e) { showToast('Ошибка добавления', 'error'); }
    }

    // Calculate expiry level pill
    function getExpiryLevel(expiryDate) {
        if (!expiryDate) return null;
        const today = new Date();
        const expiry = new Date(expiryDate);
        const daysLeft = Math.ceil((expiry - today) / (1000 * 60 * 60 * 24));

        if (daysLeft < 0) return { class: 'expiry-expired', text: 'Просрочен' };
        if (daysLeft <= 7) return { class: 'expiry-critical', text: 'Критично' };
        if (daysLeft <= 30) return { class: 'expiry-warning', text: 'Внимание' };
        return { class: 'expiry-good', text: 'Активен' };
    }

    // Animate number count-up
    function animateNumber(element, target, duration = 1000) {
        const start = parseInt(element.textContent) || 0;
        const increment = (target - start) / (duration / 16);
        let current = start;

        const timer = setInterval(() => {
            current += increment;
            if ((increment > 0 && current >= target) || (increment < 0 && current <= target)) {
                element.textContent = target;
                clearInterval(timer);
            } else {
                element.textContent = Math.floor(current);
            }
        }, 16);
    }

    function renderProducts(products) {
        const grid = document.getElementById('products');
        if (!grid) return;
        if (!products.length) {
            grid.innerHTML = '<div class="catalog-empty" style="grid-column:1/-1"><h3>Ничего не найдено</h3><p>Попробуйте изменить запрос или выбрать другую категорию.</p></div>';
            return;
        }
        grid.innerHTML = products.map(p => {
            const expiryLevel = getExpiryLevel(p.expiryDate);
            const isFav = state.favoriteIds.has(Number(p.id));
            const currentPrice = p.displayPrice ?? p.unitPrice;
            return '<article class="product-card">' +
                '<a href="/product.html?id=' + encodeURIComponent(p.id) + '" aria-label="Подробнее о товаре ' + escapeHtml(p.name) + '">' +
                CatalogImages.imageTag(p, { className: 'product-card-image', alt: p.name }) + '</a>' +
                '<div class="product-card-content">' +
                '<div class="product-card-category">' + escapeHtml(p.category || 'Без категории') + '</div>' +
                '<a class="product-card-name" href="/product.html?id=' + encodeURIComponent(p.id) + '">' + escapeHtml(p.name) + '</a>' +
                (p.brand ? '<div class="product-card-brand">' + escapeHtml(p.brand) + '</div>' : '') +
                (p.description ? '<p class="product-card-description">' + escapeHtml(p.description) + '</p>' : '') +
                '<div class="product-card-stock">В наличии: ' + Number(p.quantityOnHand || 0) + ' шт.</div>' +
                (expiryLevel ? '<span class="expiry-pill ' + expiryLevel.class + '">' + expiryLevel.text + '</span>' : '') +
                '<div class="product-card-price">' + ariaMoney.format(currentPrice) + '</div>' +
                '<div class="product-card-actions"><button class="btn-add-cart" onclick="addToCart(' + Number(p.id) + ')">В корзину</button>' +
                '<button class="aria-favorite-button' + (isFav ? ' is-active' : '') + '" data-id="' + Number(p.id) + '" aria-label="Избранное">' + (isFav ? '♥' : '♡') + '</button></div>' +
                '</div></article>';
        }).join('');
        grid.querySelectorAll('.aria-favorite-button').forEach(button => {
            const id = Number(button.dataset.id);
            window.bindFavoriteButton(button, id, state.favoriteIds.has(id));
        });
    }

    function filterAndSort() {
        const search = document.getElementById('search').value.toLowerCase();
        const category = document.getElementById('category-chips').querySelector('.active').dataset.category;
        const sort = document.getElementById('sort').value;

        let filtered = state.products.filter(p => {
            const matchSearch = (p.name || '').toLowerCase().includes(search) || (p.sku || '').toLowerCase().includes(search) || (p.description || '').toLowerCase().includes(search);
            const matchCategory = !category || p.category === category;
            return matchSearch && matchCategory;
        });

        if (sort === 'price-asc') filtered.sort((a, b) => a.unitPrice - b.unitPrice);
        else if (sort === 'price-desc') filtered.sort((a, b) => b.unitPrice - a.unitPrice);
        else if (sort === 'popular') filtered.sort((a, b) => (b.popularityRank || 0) - (a.popularityRank || 0));

        renderProducts(filtered);
    }

    function updateCategoryChips() {
        const container = document.getElementById('category-chips');
        const categories = [''].concat(Array.from(state.allCategories).sort());

        container.innerHTML = categories.map(cat =>
            '<button class="category-chip' + (cat === '' ? ' active' : '') + '" data-category="' + cat + '">' +
            (cat === '' ? 'Все категории' : cat) +
            '</button>'
        ).join('');

        container.querySelectorAll('.category-chip').forEach(chip => {
            chip.addEventListener('click', () => {
                container.querySelectorAll('.category-chip').forEach(c => c.classList.remove('active'));
                chip.classList.add('active');
                filterAndSort();
            });
        });
    }

    async function updateImpactStats() {
        try {
            const res = await fetch('/api/products/impact/positions');
            const impact = await res.json();
            animateNumber(document.getElementById('impact-saved'), impact.savedCount || 0);
            animateNumber(document.getElementById('impact-social'), impact.socialCount || 0);
            animateNumber(document.getElementById('impact-active'), impact.processingCount || 0);
        } catch(e) {}
    }

    async function init() {
        updateAuthNav();
        ariaTheme.initTheme();
        ariaI18n.initI18n();
        await Promise.all([loadFavorites(), loadCartCount()]);

        try {
            const res = await fetch('/api/products/store');
            state.products = await res.json();

            // Extract unique categories
            state.products.forEach(p => {
                if (p.category) state.allCategories.add(p.category);
            });

            updateCategoryChips();
            await updateImpactStats();
            filterAndSort();
        } catch(e) {
            showToast(ariaI18n.t('toast.error'), 'error');
        }

        document.getElementById('search').addEventListener('input', filterAndSort);
        document.getElementById('sort').addEventListener('change', filterAndSort);
    }

    init();
