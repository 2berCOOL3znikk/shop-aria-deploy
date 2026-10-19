let isAnalyzing = false;
    let allProducts = [];
    let selectedProductIds = new Set();
    let selectCriticalCandidatesOnNextLoad = true;

    function showToast(msg, type = 'success') {
        const container = document.getElementById('toast-container');
        container.setAttribute('aria-live', 'polite');
        const toast = document.createElement('div');
        toast.className = 'toast ' + type;
        const icon = document.createElement('span');
        icon.className = 'toast-icon';
        icon.setAttribute('aria-hidden', 'true');
        icon.textContent = type === 'error' ? '❌' : type === 'warning' ? '⚠️' : '✅';
        const message = document.createElement('span');
        message.className = 'toast-message';
        message.textContent = String(msg ?? '');
        toast.append(icon, message);
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

    // Expiry level calculation
    function getExpiryLevel(expiryDate) {
        if (!expiryDate) return { class: 'expiry-good', text: 'Н/Д' };
        const today = new Date();
        const expiry = new Date(expiryDate);
        const daysLeft = Math.ceil((expiry - today) / (1000 * 60 * 60 * 24));

        if (daysLeft < 0) return { class: 'expiry-expired', text: 'Просрочен' };
        if (daysLeft <= 7) return { class: 'expiry-critical', text: 'Критично' };
        if (daysLeft <= 30) return { class: 'expiry-warning', text: 'Внимание' };
        return { class: 'expiry-good', text: 'Активен' };
    }

    function renderProductRow(p) {
        const expiryLevel = getExpiryLevel(p.expiryDate);
        const statusLabels = { 'ACTIVE': 'Активен', 'CANDIDATE': 'Кандидат', 'RESERVED': 'Зарезервирован', 'TRANSFERRED': 'Передан', 'SOCIAL':'На соцполке', 'DISCOUNTED':'Скидка', 'WRITTEN_OFF':'Списан' };
        const badgeClass = 'badge-' + p.status.toLowerCase();
        const statusText = statusLabels[p.status] || p.status;
        const daysLeft = p.expiryDate ? Math.ceil((new Date(p.expiryDate) - new Date()) / (1000 * 60 * 60 * 24)) : null;
        const expired = daysLeft !== null && daysLeft < 0;
        const stockDays = Math.max(0, Math.floor((Date.now() - new Date(p.receivedAt).getTime()) / 86400000));
        const qty = Number(p.quantityOnHand || 0);
        const unitPrice = Number(p.unitPrice || 0);
        const unitCost = Number(p.unitCost || 0);
        const marginPercent = unitCost > 0 ? Math.round((unitPrice - unitCost) / unitCost * 100) : null;
        const isCritical = !expired && daysLeft !== null && daysLeft <= 7;
        const canTransfer = p.status === 'CANDIDATE' && !expired && qty > 0;
        const aboveMarket = Number(p.marketAvgPrice || 0) > 0 && unitPrice > Number(p.marketAvgPrice) * 1.2;
        const riskText = expired ? 'Срок истёк' : isCritical ? 'Срочно · передать сейчас' : daysLeft !== null && daysLeft <= 30 ? 'Скоро истекает' : marginPercent !== null && marginPercent < 0 ? 'Минусовая маржа' : aboveMarket ? 'Выше рынка' : stockDays >= 180 && Number(p.salesLast30Days || 0) === 0 ? 'Долго лежит' : 'Наблюдение';

        return `<tr>
            <td>${canTransfer ? `<input aria-label="Добавить ${p.sku} в лот" type="checkbox" ${selectedProductIds.has(Number(p.id)) ? 'checked' : ''} onchange="toggleProductSelection(${p.id},this.checked)">` : '—'}</td>
            <td class="mono" style="font-weight:600;">${p.sku}</td>
            <td>
                <div class="admin-product-cell">${CatalogImages.imageTag(p, { className: "admin-product-thumb", alt: p.name })}<div><strong>${p.name}</strong><small>${p.category || 'Без категории'} · ${stockDays} дн. на складе</small></div></div>
            </td>
            <td>${p.expiryDate ? new Date(p.expiryDate).toLocaleDateString('ru-RU') : '—'}</td>
            <td class="${daysLeft < 0 ? 'days-negative' : ''}">${daysLeft !== null ? daysLeft + ' дн.' : '—'}</td>
            <td>${qty}</td>
            <td>${p.salesLast30Days || 0}</td>
            <td style="color:${marginPercent !== null && marginPercent < 0 ? 'var(--accent-red)' : 'var(--text-primary)'}">${marginPercent === null ? '—' : (marginPercent > 0 ? '+' : '') + marginPercent + '%'}</td>
            <td><span class="${badgeClass}">${statusText}</span></td>
            <td>
                <span class="expiry-pill ${expired || isCritical ? 'expiry-critical' : expiryLevel.class}">${riskText}</span>
            </td>
            <td class="table-actions">${canTransfer ? `<button class="btn-outline" onclick="publishSocialOffer(${p.id},${qty},'${p.expiryDate || ''}')">На соцполку</button>` : ''}${p.status !== 'WRITTEN_OFF' && p.status !== 'TRANSFERRED' && p.status !== 'RESERVED' ? `<button class="admin-action-btn" onclick="writeOffProduct(${p.id})">Списать</button>` : ''}<label class="btn-outline" style="padding:6px 9px;cursor:pointer;">Фото<input type="file" accept="image/jpeg,image/png" aria-label="Загрузить фото товара" style="display:none" onchange="uploadProductPhoto(${p.id},this)"></label></td>
        </tr>`;
    }

    function applyFilters() {
        const search = document.getElementById('filter-search').value.toLowerCase();
        const dateFrom = document.getElementById('filter-date-from').value;
        const dateTo = document.getElementById('filter-date-to').value;
        const status = document.getElementById('filter-status').value;

        const filtered = allProducts.filter(p => {
            const matchSearch = !search || p.name.toLowerCase().includes(search) || p.sku.toLowerCase().includes(search);
            const matchStatus = !status || p.status === status;
            let matchDate = true;
            if (dateFrom || dateTo) {
                const received = new Date(p.receivedAt);
                if (dateFrom) matchDate = matchDate && received >= new Date(dateFrom);
                if (dateTo) matchDate = matchDate && received <= new Date(dateTo);
            }
            return matchSearch && matchStatus && matchDate;
        });

        const tbody = document.getElementById('aria-table-body');
        if (filtered.length === 0) {
            tbody.innerHTML = '<tr><td colspan="11" style="text-align: center; padding: 40px; color: var(--text-muted);">Нет товаров, соответствующих фильтрам</td></tr>';
        } else {
            tbody.innerHTML = filtered.map(renderProductRow).join('');
        }
    }

    function toggleProductSelection(productId, checked) {
        const id = Number(productId);
        if (checked) selectedProductIds.add(id);
        else selectedProductIds.delete(id);
        document.getElementById('btn-transfer').textContent = `Сформировать акт · ${selectedProductIds.size}`;
    }

    async function writeOffProduct(productId) {
        if (!ariaAuth.requireAdmin()) return;
        const product = allProducts.find(item => item.id === productId);
        if (!await window.ariaShell.confirm(`Списать «${product ? product.name : 'товар'}»? Остаток будет установлен в 0.`)) return;

        try {
            const res = await ariaAuth.authFetch('/api/aria/products/' + productId + '/write-off', { method: 'POST' });
            if (!res.ok) throw new Error((await res.json()).message || 'Не удалось списать товар');
            showToast('Товар списан');
            await loadProducts();
        } catch(e) {
            showToast('Ошибка: ' + e.message, 'error');
        }
    }

    async function runAnalysis() {
        if (!ariaAuth.requireLogin()) return;
        if (!ariaAuth.requireAdmin()) return;
        if (isAnalyzing) return;
        isAnalyzing = true;

        const overlay = document.getElementById('analysis-overlay');
        const btn = document.getElementById('btn-analyze');
        const result = document.getElementById('analysis-result');
        const progressFill = document.getElementById('progress-fill');

        result.classList.remove('show');
        progressFill.style.animation = 'none';
        void progressFill.offsetWidth;
        progressFill.style.width = '0%';

        for (let i = 1; i <= 6; i++) {
            const step = document.getElementById('step-' + i);
            step.className = 'step';
            step.querySelector('.step-icon').textContent = '';
        }

        overlay.classList.add('active');
        btn.textContent = ' Анализ...';
        btn.disabled = true;

        try {
            const request = ariaAuth.authFetch('/api/aria/analyze', { method: 'POST' });
            const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
            for (let i = 1; i <= 5; i++) {
                const step = document.getElementById('step-' + i);
                step.className = 'step active';
                step.querySelector('.step-icon').textContent = '…';
                progressFill.style.width = Math.min(i * 15, 85) + '%';
                await pause(320);
                step.className = 'step done';
                step.querySelector('.step-icon').textContent = '✓';
            }
            const saving = document.getElementById('step-6');
            saving.className = 'step active';
            saving.querySelector('.step-icon').textContent = '…';
            const res = await request;
            if (!res.ok) throw new Error('Сервер вернул ошибку ' + res.status);
            const data = await res.json();
            await pause(250);
            saving.className = 'step done';
            saving.querySelector('.step-icon').textContent = '✓';
            progressFill.style.width = '100%';

            document.getElementById('res-total').textContent = data.totalScanned || 0;
            document.getElementById('res-candidate').textContent = data.markedCandidate || 0;
            document.getElementById('res-active').textContent = data.returnedToActive || 0;
            document.getElementById('res-urgent').textContent = data.urgent || 0;
            document.getElementById('res-price').textContent = data.priceOutliers || 0;

            result.classList.add('show');
            selectCriticalCandidatesOnNextLoad = true;
            await loadAll();

        } catch(e) {
            showToast('Ошибка анализа: ' + e.message, 'error');
            overlay.classList.remove('active');
        } finally {
            isAnalyzing = false;
            btn.textContent = '🔍 Запустить анализ ARIA';
            btn.disabled = false;
        }
    }

    async function createTransferLot() {
        if (!ariaAuth.requireAdmin()) return;
        const btn = document.getElementById('btn-transfer');
        const productIds = Array.from(selectedProductIds);
        if (productIds.length === 0) {
            showToast('Отметьте товары для партии. Критические позиции уже выбраны автоматически.', 'warning');
            return;
        }
        const partnerName = document.getElementById('transfer-partner')?.value.trim() || '';
        if (!partnerName) { showToast('Укажите организацию-получателя акта.', 'warning'); return; }
        const noteText = 'Состав лота выбран администратором; фактическая передача подтверждается отдельно.';
        btn.textContent = '⏳ Обработка...';
        btn.disabled = true;

        try {
            const params = new URLSearchParams({partner: partnerName.trim(), note: noteText});
            productIds.forEach(id => params.append('productIds', id));
            const res = await ariaAuth.authFetch('/api/aria/transfer?' + params.toString(), {method: 'POST'});

            if (!res.ok) {
                const err = await res.json();
                throw new Error(err.message || 'Не удалось создать лот');
            }

            const lot = await res.json();
            selectedProductIds.clear();
            showToast('Черновик лота создан. Проверьте состав, подтвердите акт и перейдите к печати.');

            await loadAll();
        } catch(e) {
            if (e.message && e.message.includes('403')) {
                showToast('Доступ запрещён: нужна роль ADMIN', 'error');
            } else {
                showToast('Ошибка: ' + e.message, 'error');
            }
        } finally {
            btn.textContent = `Сформировать акт · ${selectedProductIds.size}`;
            btn.disabled = false;
        }
    }

    async function downloadAct(lotId) {
        const res = await ariaAuth.authFetch('/api/aria/reports/acts/' + encodeURIComponent(lotId) + '/printable');
        if (!res.ok) throw new Error('Акт недоступен: проверьте, что партия существует и у вас есть доступ администратора.');
        const fileUrl = URL.createObjectURL(await res.blob());
        const link = document.createElement('a');
        link.href = fileUrl;
        link.download = 'aria-act-' + lotId + '.html';
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(fileUrl), 30000);
    }

    async function publishSocialOffer(productId, quantity, expiryDate) {
        if (!ariaAuth.requireAdmin()) return;
        const validUntil = expiryDate && expiryDate < new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 10)
            ? expiryDate : new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 10);
        try {
            const res = await ariaAuth.authFetch('/api/social/offers', {
                method: 'POST', headers: {'Content-Type':'application/json'},
                body: JSON.stringify({productId, quantity, validUntil})
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || 'Не удалось опубликовать лот');
            showToast('Лот опубликован на соцполке. Фонды увидят доступное количество.');
            await loadAll();
        } catch(e) { showToast(e.message, 'error'); }
    }

    async function uploadProductPhoto(productId, input) {
        const file = input.files && input.files[0];
        if (!file) return;
        const form = new FormData();
        form.append('photo', file);
        try {
            const res = await ariaAuth.authFetch('/api/admin/products/' + productId + '/photo', {method:'POST', body:form});
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || 'Не удалось загрузить фотографию');
            showToast('Фото товара сохранено. Оно будет видно в каталоге и соцполке.');
            await loadProducts();
        } catch(e) { showToast(e.message, 'error'); }
        finally { input.value = ''; }
    }

    async function loadLots() {
        const box = document.getElementById('lots-container');
        const escapeText = value => String(value ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
        const statusLabels = {DRAFT:'Черновик',CONFIRMED:'Подтверждён',PRINTED:'Распечатан',SIGNED:'Подписан',TRANSFERRED:'Передан'};
        try {
            const res = await ariaAuth.authFetch('/api/aria/logistics/history');
            if (!res.ok) throw new Error('Не удалось загрузить историю передач');
            const lots = await res.json();
            if (!lots.length) { box.innerHTML = '<p class="admin-note">Передач пока нет. Созданные лоты появятся здесь после первого действия.</p>'; return; }
            box.innerHTML = lots.map(lot => {
                const items = (lot.items || []).map(item => '<li><strong>' + escapeText(item.sku) + '</strong> · ' + escapeText(item.name) + ' × ' + Number(item.quantity || 0) + ' шт.' + (item.reason ? '<br><small>' + escapeText(item.reason) + '</small>' : '') + '</li>').join('');
                const state = statusLabels[lot.state] || escapeText(lot.state);
                return '<article class="lot-history-entry"><div class="lot-history-heading"><strong>Акт ARIA-' + Number(lot.id) + ' · ' + state + '</strong><span class="admin-note">' + new Date(lot.createdAt).toLocaleString('ru-KZ') + '</span></div><p class="admin-note">Получатель: ' + escapeText(lot.partner || 'не указан') + ' · ' + Number(lot.itemCount || 0) + ' поз. · ' + Number(lot.quantity || 0) + ' шт.</p><ul class="lot-history-items">' + items + '</ul><div class="lot-history-total">Итого: ' + ariaMoney.format(lot.totalAmount || 0) + '</div><button class="btn-outline" type="button" onclick="printTransferAct(' + Number(lot.id) + ',false)">Открыть / печать</button></article>';
            }).join('');
        } catch(e) { box.textContent = e.message; }
    }

    async function createOrganization(event) {
        event.preventDefault();
        const result = document.getElementById('org-result');
        const request = {
            name: document.getElementById('org-name').value,
            email: document.getElementById('org-email').value,
            password: document.getElementById('org-password').value
        };
        try {
            const res = await ariaAuth.authFetch('/api/admin/organizations', {
                method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(request)
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || 'Не удалось создать доступ');
            result.textContent = `Доступ создан для ${data.name} (${data.email}). Передайте организации пароль отдельно.`;
            document.getElementById('org-form').reset();
        } catch(e) { result.textContent = e.message; }
    }

    async function loadStats() {
        try {
            const res = await ariaAuth.authFetch('/api/aria/stats');
            if (!res.ok) throw new Error('Нет доступа к статистике');
            const stats = await res.json();
            document.getElementById('stat-dead-stock').textContent = ariaMoney.format(stats.deadStockValue);
            document.getElementById('stat-tax-benefit').textContent = ariaMoney.format(stats.taxBenefitEstimate);
            document.getElementById('stat-saved').textContent = stats.savedItemsCount;
            document.getElementById('stat-total').textContent = stats.totalProducts;
            document.getElementById('stat-active').textContent = stats.activeProducts;
            const statusLabels = {ACTIVE:'Активные', CANDIDATE:'Кандидаты', RESERVED:'В резерве', SOCIAL:'На соцполке', DISCOUNTED:'Со скидкой', TRANSFERRED:'Переданы', WRITTEN_OFF:'Списаны'};
            const statusValues = stats.statusCounts || {};
            const statusMax = Math.max(1, ...Object.values(statusValues));
            document.getElementById('status-chart').innerHTML = Object.entries(statusValues).map(([key,value]) => `<div class="chart-row"><span>${statusLabels[key] || key}</span><div class="chart-track"><div class="chart-fill" style="width:${Math.round(value/statusMax*100)}%"></div></div><strong>${value}</strong></div>`).join('');
            const categories = stats.categoryBreakdown || {};
            const categoryMax = Math.max(1, ...Object.values(categories));
            document.getElementById('category-chart').innerHTML = Object.entries(categories).sort((a,b) => b[1]-a[1]).map(([name,value]) => `<div class="chart-row"><span>${name}</span><div class="chart-track"><div class="chart-fill" style="width:${Math.round(value/categoryMax*100)}%"></div></div><strong>${value}</strong></div>`).join('') || '<p class="admin-note">Нет категорий.</p>';
        } catch(e) {
            console.error('Ошибка загрузки статистики:', e);
        }
    }

    async function loadProducts() {
        try {
            const res = await ariaAuth.authFetch('/api/products');
            if (!res.ok) throw new Error('Нет доступа к товарам');
            const products = await res.json();
            allProducts = products;
            const today = new Date().toISOString().slice(0, 10);
            const selectableIds = new Set(allProducts
                .filter(p => p.status === 'CANDIDATE' && Number(p.quantityOnHand || 0) > 0 && (!p.expiryDate || p.expiryDate >= today))
                .map(p => Number(p.id)));
            selectedProductIds = new Set(Array.from(selectedProductIds).filter(id => selectableIds.has(id)));
            if (selectCriticalCandidatesOnNextLoad) {
                allProducts.filter(p => p.status === 'CANDIDATE' && p.expiryDate && p.expiryDate >= today && Math.ceil((new Date(p.expiryDate) - new Date()) / 86400000) <= 7)
                    .forEach(p => selectedProductIds.add(Number(p.id)));
                selectCriticalCandidatesOnNextLoad = false;
            }
            document.getElementById('btn-transfer').textContent = `Сформировать акт · ${selectedProductIds.size}`;
            applyFilters();
        } catch(e) {
            console.error('Ошибка загрузки товаров:', e);
            document.getElementById('aria-table-body').innerHTML = '<tr><td colspan="11" style="text-align: center; padding: 40px; color: var(--accent-red);">Ошибка загрузки данных</td></tr>';
        }
    }

    async function loadAll() {
        await Promise.all([loadStats(), loadProducts(), loadLots()]);
    }

    updateAuthNav();
    ariaTheme.initTheme();
    ariaI18n.initI18n();
    if (ariaAuth.getRole() !== 'ADMIN') {
        if (!ariaAuth.isLoggedIn()) location.replace('/login.html?next=%2Fadmin.html');
        else location.replace('/index.html');
    } else {
        document.getElementById('org-form').addEventListener('submit', createOrganization);
        loadAll();
    }
