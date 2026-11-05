let isAnalyzing = false;
    let allProducts = [];
    let selectedProductIds = new Set();
    let selectCriticalCandidatesOnNextLoad = true;

    // Expiry level calculation
    function getExpiryLevel(expiryDate) {
        if (!expiryDate) return { class: 'expiry-good', text: ariaCopy.text("text.7e269d39127ec6") };
        const today = new Date();
        const expiry = new Date(expiryDate);
        const daysLeft = Math.ceil((expiry - today) / (1000 * 60 * 60 * 24));

        if (daysLeft < 0) return { class: 'expiry-expired', text: ariaCopy.text("text.534b8c25b4cefb") };
        if (daysLeft <= 7) return { class: 'expiry-critical', text: ariaCopy.text("text.f186cf196afa16") };
        if (daysLeft <= 30) return { class: 'expiry-warning', text: ariaCopy.text("text.f17ceff0aa95a3") };
        return { class: 'expiry-good', text: ariaCopy.text("text.a87a4b39c30404") };
    }

    function renderProductRow(p) {
        const expiryLevel = getExpiryLevel(p.expiryDate);
        const statusLabels = { 'ACTIVE': ariaCopy.text("text.a87a4b39c30404"), 'CANDIDATE': ariaCopy.text("text.ea90b3879b2986"), 'RESERVED': ariaCopy.text("text.50eaa1827e710a"), 'TRANSFERRED': ariaCopy.text("text.41b04cf04adccf"), 'SOCIAL':ariaCopy.text("text.f5c1b2a98487b3"), 'DISCOUNTED':ariaCopy.text("text.53bf07ab6cc701"), 'WRITTEN_OFF':ariaCopy.text("text.e69636a6e234d7") };
        const badgeClass = 'badge-' + p.status.toLowerCase();
        const statusText = statusLabels[p.status] || p.status;
        const daysLeft = p.expiryDate ? Math.ceil((new Date(p.expiryDate) - new Date()) / (1000 * 60 * 60 * 24)) : null;
        const expired = daysLeft !== null && daysLeft < 0;
        const stockDays = Math.max(0, Math.floor((Date.now() - new Date(p.receivedAt).getTime()) / 86400000));
        const qty = Number(p.quantityOnHand || 0);
        const unitPrice = Number(p.unitPrice || 0);
        const unitCost = Number(p.unitCost || 0);
        const marginPercent = unitPrice > 0 ? Math.round((unitPrice - unitCost) / unitPrice * 100) : null;
        const isCritical = !expired && daysLeft !== null && daysLeft <= 7;
        const canTransfer = p.status === 'CANDIDATE' && !expired && qty > 0;
        const aboveMarket = Number(p.marketAvgPrice || 0) > 0 && unitPrice > Number(p.marketAvgPrice) * 1.2;
        const riskText = expired ? ariaCopy.text("text.409e310831bdcf") : isCritical ? ariaCopy.text("text.8976a65a00a48a") : daysLeft !== null && daysLeft <= 30 ? ariaCopy.text("text.b21d8a96f28c2d") : marginPercent !== null && marginPercent < 0 ? ariaCopy.text("text.3710ec971c59a6") : aboveMarket ? ariaCopy.text("text.d6cba56b94bfb8") : stockDays >= 180 && Number(p.salesLast30Days || 0) === 0 ? ariaCopy.text("text.ddc7f7ae63d22c") : ariaCopy.text("text.574d43b44984e7");

        return ariaCopy.template("template.5ef7aa26de6f00",[canTransfer ? ariaCopy.template("template.3e905f53333169",[p.sku,selectedProductIds.has(Number(p.id)) ? 'checked' : '',p.id]) : '—',p.sku,CatalogImages.imageTag(p, { className: "admin-product-thumb", alt: p.name }),p.name,p.category || ariaCopy.text("text.c1952d18056a9d"),stockDays,p.expiryDate ? new Date(p.expiryDate).toLocaleDateString(ariaCopy.locale()) : '—',daysLeft < 0 ? 'days-negative' : '',daysLeft !== null ? daysLeft + ariaCopy.text("text.ce4d047b4d52ff") : '—',qty,p.salesLast30Days || 0,marginPercent !== null && marginPercent < 0 ? 'var(--accent-red)' : 'var(--text-primary)',marginPercent === null ? '—' : (marginPercent > 0 ? '+' : '') + marginPercent + '%',badgeClass,statusText,expired || isCritical ? 'expiry-critical' : expiryLevel.class,riskText,canTransfer ? ariaCopy.template("template.9a21adec4475e4",[p.id,qty,p.expiryDate || '']) : '',p.status !== 'WRITTEN_OFF' && p.status !== 'TRANSFERRED' && p.status !== 'RESERVED' ? ariaCopy.template("template.6dfb11a51edb0c",[p.id]) : '']);
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
            ariaCopy.setProperty(tbody,"innerHTML",ariaCopy.text("text.4000b65294defc"));
        } else {
            ariaCopy.setProperty(tbody,"innerHTML",filtered.map(renderProductRow).join(''));
        }
    }

    function toggleProductSelection(productId, checked) {
        const id = Number(productId);
        if (checked) selectedProductIds.add(id);
        else selectedProductIds.delete(id);
        ariaCopy.setProperty(document.getElementById('btn-transfer'),"textContent",ariaCopy.template("template.212e1d735c6ed0",[selectedProductIds.size]));
    }

    async function writeOffProduct(productId) {
        if (!ariaAuth.requireAdmin()) return;
        const product = allProducts.find(item => item.id === productId);
        if (!await window.ariaShell.confirm(ariaCopy.template("template.b9dfb5e3c3713f",[product ? product.name : ariaCopy.text("text.563c5a36f9ee10")]))) return;

        try {
            const res = await ariaAuth.authFetch('/api/aria/products/' + productId + '/write-off', { method: 'POST' });
            if (!res.ok) throw new Error(ariaCopy.errorText((await res.json()).message || ariaCopy.text("text.31c63539d74e0b")));
            showToast(ariaCopy.text("text.9c7d8b706af851"));
            await loadProducts();
        } catch(e) {
            showToast(ariaCopy.text("text.37eceb9f52a56f") + e.message, 'error');
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
            ariaCopy.setProperty(step.querySelector('.step-icon'),"textContent",'');
        }

        overlay.classList.add('active');
        ariaCopy.setProperty(btn,"textContent",ariaCopy.text("text.8d7fedf7433e12"));
        btn.disabled = true;

        try {
            const request = ariaAuth.authFetch('/api/aria/analyze', { method: 'POST' });
            const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
            for (let i = 1; i <= 5; i++) {
                const step = document.getElementById('step-' + i);
                step.className = 'step active';
                ariaCopy.setProperty(step.querySelector('.step-icon'),"textContent",'…');
                progressFill.style.width = Math.min(i * 15, 85) + '%';
                await pause(320);
                step.className = 'step done';
                ariaCopy.setProperty(step.querySelector('.step-icon'),"textContent",'✓');
            }
            const saving = document.getElementById('step-6');
            saving.className = 'step active';
            ariaCopy.setProperty(saving.querySelector('.step-icon'),"textContent",'…');
            const res = await request;
            if (!res.ok) throw new Error(ariaCopy.errorText(ariaCopy.text("text.38d9eb2aba531e") + res.status));
            const data = await res.json();
            await pause(250);
            saving.className = 'step done';
            ariaCopy.setProperty(saving.querySelector('.step-icon'),"textContent",'✓');
            progressFill.style.width = '100%';

            ariaCopy.setProperty(document.getElementById('res-total'),"textContent",data.totalScanned || 0);
            ariaCopy.setProperty(document.getElementById('res-candidate'),"textContent",data.markedCandidate || 0);
            ariaCopy.setProperty(document.getElementById('res-active'),"textContent",data.returnedToActive || 0);
            ariaCopy.setProperty(document.getElementById('res-urgent'),"textContent",data.urgent || 0);
            ariaCopy.setProperty(document.getElementById('res-price'),"textContent",data.priceOutliers || 0);

            result.classList.add('show');
            selectCriticalCandidatesOnNextLoad = true;
            await loadAll();

        } catch(e) {
            showToast(ariaCopy.text("text.3c1842f8be99fa") + e.message, 'error');
            overlay.classList.remove('active');
        } finally {
            isAnalyzing = false;
            ariaCopy.setProperty(btn,"textContent",ariaCopy.text("text.1dad3fd007d92f"));
            btn.disabled = false;
        }
    }

    async function createTransferLot() {
        if (!ariaAuth.requireAdmin()) return;
        const btn = document.getElementById('btn-transfer');
        const productIds = Array.from(selectedProductIds);
        if (productIds.length === 0) {
            showToast(ariaCopy.text("text.17fda17bbc5392"), 'warning');
            return;
        }
        const partnerName = document.getElementById('transfer-partner')?.value.trim() || '';
        if (!partnerName) { showToast(ariaCopy.text("text.1e146103ec8919"), 'warning'); return; }
        const noteText = ariaCopy.text("text.50747ce7057167");
        ariaCopy.setProperty(btn,"textContent",ariaCopy.text("text.24afa3bb063aac"));
        btn.disabled = true;

        try {
            const params = new URLSearchParams({partner: partnerName.trim(), note: noteText});
            productIds.forEach(id => params.append('productIds', id));
            const res = await ariaAuth.authFetch('/api/aria/transfer?' + params.toString(), {method: 'POST'});

            if (!res.ok) {
                const err = await res.json();
                throw new Error(ariaCopy.errorText(err.message || ariaCopy.text("text.1a73334aaf4f05")));
            }

            const lot = await res.json();
            selectedProductIds.clear();
            showToast(ariaCopy.text("text.6ab9598e2e3256"));

            await loadAll();
        } catch(e) {
            if (e.message && e.message.includes('403')) {
                showToast(ariaCopy.text("text.45287502ca3494"), 'error');
            } else {
                showToast(ariaCopy.text("text.37eceb9f52a56f") + e.message, 'error');
            }
        } finally {
            ariaCopy.setProperty(btn,"textContent",ariaCopy.template("template.212e1d735c6ed0",[selectedProductIds.size]));
            btn.disabled = false;
        }
    }

    async function downloadAct(lotId) {
        const res = await ariaAuth.authFetch('/api/aria/reports/acts/' + encodeURIComponent(lotId) + '/printable');
        if (!res.ok) throw new Error(ariaCopy.errorText(ariaCopy.text("text.896b33405aa5ce")));
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
            if (!res.ok) throw new Error(ariaCopy.errorText(data.error || ariaCopy.text("text.a8e9a93e52b335")));
            showToast(ariaCopy.text("text.b1a683dcfeb61d"));
            await loadAll();
        } catch(e) { showToast(e.message, 'error'); }
    }

    async function loadLots() {
        const box = document.getElementById('lots-container');
        const escapeText = value => String(value ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
        const statusLabels = {DRAFT:ariaCopy.text("text.65fd24f1cb37fe"),CONFIRMED:ariaCopy.text("text.f19bb8c78d60b7"),PRINTED:ariaCopy.text("text.9b97b724b72d9f"),SIGNED:ariaCopy.text("text.524bdbc8592106"),TRANSFERRED:ariaCopy.text("text.41b04cf04adccf")};
        try {
            const res = await ariaAuth.authFetch('/api/aria/logistics/history');
            if (!res.ok) throw new Error(ariaCopy.errorText(ariaCopy.text("text.44a3c2ef907b22")));
            const lots = await res.json();
            if (!lots.length) { ariaCopy.setProperty(box,"innerHTML",ariaCopy.text("text.6a0876435ceb9a")); return; }
            ariaCopy.setProperty(box,"innerHTML",lots.map(lot => {
                const items = (lot.items || []).map(item => '<li><strong>' + escapeText(item.sku) + '</strong> · ' + escapeText(item.name) + ' × ' + Number(item.quantity || 0) + ariaCopy.text("text.297cac49ffa570") + (item.reason ? '<br><small>' + escapeText(item.reason) + '</small>' : '') + '</li>').join('');
                const state = statusLabels[lot.state] || escapeText(lot.state);
                return ariaCopy.text("text.b1d4007904e621") + Number(lot.id) + ' · ' + state + '</strong><span class="admin-note">' + new Date(lot.createdAt).toLocaleString(ariaCopy.locale()) + ariaCopy.text("text.447a491159e2a9") + escapeText(lot.partner || ariaCopy.text("text.f250ed099246c6")) + ' · ' + Number(lot.itemCount || 0) + ariaCopy.text("text.9b92e6f8dedc80") + Number(lot.quantity || 0) + ariaCopy.text("text.a64048d6c72378") + items + ariaCopy.text("text.235019f01d3f12") + ariaMoney.format(lot.totalAmount || 0) + '</div><button class="btn-outline" type="button" onclick="printTransferAct(' + Number(lot.id) + ariaCopy.text("text.008fbc15054922");
            }).join(''));
        } catch(e) { ariaCopy.setProperty(box,"textContent",e.message); }
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
            if (!res.ok) throw new Error(ariaCopy.errorText(data.error || ariaCopy.text("text.26ddfa21068c41")));
            ariaCopy.setProperty(result,"textContent",ariaCopy.template("template.6501b403bd4eee",[data.name,data.email]));
            document.getElementById('org-form').reset();
        } catch(e) { ariaCopy.setProperty(result,"textContent",e.message); }
    }

    async function loadStats() {
        try {
            const res = await ariaAuth.authFetch('/api/aria/stats');
            if (!res.ok) throw new Error(ariaCopy.errorText(ariaCopy.text("text.9328463bd69ef8")));
            const stats = await res.json();
            ariaCopy.setProperty(document.getElementById('stat-dead-stock'),"textContent",ariaMoney.format(stats.deadStockValue));
            ariaCopy.setProperty(document.getElementById('stat-tax-benefit'),"textContent",ariaMoney.format(stats.taxBenefitEstimate));
            ariaCopy.setProperty(document.getElementById('stat-saved'),"textContent",stats.savedItemsCount);
            ariaCopy.setProperty(document.getElementById('stat-total'),"textContent",stats.totalProducts);
            ariaCopy.setProperty(document.getElementById('stat-active'),"textContent",stats.activeProducts);
            const statusLabels = {ACTIVE:ariaCopy.text("text.eeaeb97638d821"), CANDIDATE:ariaCopy.text("text.42b77f795fd9ea"), RESERVED:ariaCopy.text("text.316feb5263c4a3"), SOCIAL:ariaCopy.text("text.f5c1b2a98487b3"), DISCOUNTED:ariaCopy.text("text.002be70a2f1630"), TRANSFERRED:ariaCopy.text("text.ee96c20ad666e6"), WRITTEN_OFF:ariaCopy.text("text.3698e41d76ab6c")};
            const statusValues = stats.statusCounts || {};
            const statusMax = Math.max(1, ...Object.values(statusValues));
            ariaCopy.setProperty(document.getElementById('status-chart'),"innerHTML",Object.entries(statusValues).map(([key,value]) => `<div class="chart-row"><span>${statusLabels[key] || key}</span><div class="chart-track"><div class="chart-fill" style="width:${Math.round(value/statusMax*100)}%"></div></div><strong>${value}</strong></div>`).join(''));
            const categories = stats.categoryBreakdown || {};
            const categoryMax = Math.max(1, ...Object.values(categories));
            ariaCopy.setProperty(document.getElementById('category-chart'),"innerHTML",Object.entries(categories).sort((a,b) => b[1]-a[1]).map(([name,value]) => `<div class="chart-row"><span>${name}</span><div class="chart-track"><div class="chart-fill" style="width:${Math.round(value/categoryMax*100)}%"></div></div><strong>${value}</strong></div>`).join('') || ariaCopy.text("text.e86d6b7fd4397c"));
        } catch(e) {
            console.error(ariaCopy.text("text.eaf87d318fec3d"), e);
        }
    }

    async function loadProducts() {
        try {
            const res = await ariaAuth.authFetch('/api/products');
            if (!res.ok) throw new Error(ariaCopy.errorText(ariaCopy.text("text.1e203d00492172")));
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
            ariaCopy.setProperty(document.getElementById('btn-transfer'),"textContent",ariaCopy.template("template.212e1d735c6ed0",[selectedProductIds.size]));
            applyFilters();
        } catch(e) {
            console.error(ariaCopy.text("text.cc5fe3527397f4"), e);
            ariaCopy.setProperty(document.getElementById('aria-table-body'),"innerHTML",ariaCopy.text("text.3e930d9650073d"));
        }
    }

    async function loadAll() {
        await Promise.all([loadStats(), loadProducts(), loadLots()]);
    }

    ariaTheme.initTheme();
    ariaI18n.initI18n();
    if (ariaAuth.getRole() !== 'ADMIN') {
        if (!ariaAuth.isLoggedIn()) location.replace('/login.html?next=%2Fadmin.html');
        else location.replace('/index.html');
    } else {
        document.getElementById('org-form').addEventListener('submit', createOrganization);
        loadAll();
    }
