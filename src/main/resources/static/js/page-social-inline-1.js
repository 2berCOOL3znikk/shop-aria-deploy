const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));

  function updateAuthNav() {
    const authNav = document.getElementById('auth-nav');
    if (ariaAuth.isLoggedIn()) {
      authNav.innerHTML = '<span style="font-size:13px;color:var(--text-muted);">' + escapeHtml(ariaAuth.getUserEmail()) + '</span> <button class="btn-outline" onclick="ariaAuth.logout()" style="padding:6px 10px;">Выйти</button>';
    } else {
      authNav.innerHTML = '<a href="/login.html?next=%2Fsocial.html">Войти</a>';
    }
  }

  function renderOffer(offer) {
    const daysToExpiry = offer.expiryDate ? Math.ceil((new Date(offer.expiryDate) - new Date()) / 86400000) : null;
    const urgency = daysToExpiry !== null && daysToExpiry <= 7 ? 'Срочный лот' : 'Доступен';
    const claimButton = ariaAuth.getRole() === 'ORG'
      ? `<button class="btn-glow" onclick="claimOffer(${offer.id})">Забрать весь лот · ${offer.quantityAvailable} шт.</button>`
      : '<a class="btn-outline" href="/login.html?next=%2Fsocial.html" style="text-decoration:none;">Войти как фонд или приют</a>';
    return `<article class="offer-card">
      ${CatalogImages.imageTag({ productName: offer.productName, category: offer.category, imageUrl: offer.imageUrl }, { alt: offer.productName })}
      <div class="offer-body">
        <div style="display:flex;justify-content:space-between;gap:10px"><span class="product-card-category">${urgency}</span><span class="mono">${escapeHtml(offer.productSku)}</span></div>
        <h3 class="offer-title">${escapeHtml(offer.productName)}</h3>
        <div class="offer-meta">В партии: <strong>${offer.quantityAvailable} шт.</strong><br>Срок передачи предложения: ${offer.validUntil ? new Date(offer.validUntil).toLocaleDateString('ru-KZ') : 'не указан'}<br>${offer.expiryDate ? 'Годен до: ' + new Date(offer.expiryDate).toLocaleDateString('ru-KZ') : 'Срок годности не установлен'}</div>
        <div class="offer-reason">Причина отбора: ${escapeHtml(offer.ariaReason || 'проверено ARIA')}</div>
        <div class="offer-price">Передача без оплаты</div>${claimButton}
      </div>
    </article>`;
  }

  async function loadClaims() {
    if (ariaAuth.getRole() !== 'ORG') return;
    const section = document.getElementById('claims-section');
    section.style.display = 'block';
    try {
      const res = await ariaAuth.authFetch('/api/social/my-claims');
      const claims = await res.json();
      document.getElementById('claims-list').innerHTML = claims.length ? claims.map(c => `<div class="offer-meta" style="display:flex;justify-content:space-between;align-items:center;gap:12px;padding:12px;border-bottom:1px solid var(--border-color)"><span><strong>${escapeHtml(c.productName)}</strong> · получено ${c.claimedQuantity || 0} шт. · статус ${escapeHtml(c.status)}</span><button class="btn-outline" onclick="downloadClaimAct(${c.id})">Скачать акт</button></div>`).join('') : '<p class="admin-note">Вы пока не резервировали лоты.</p>';
    } catch(e) { document.getElementById('claims-list').textContent = 'Не удалось загрузить бронирования.'; }
  }

  async function loadSocial() {
    const grid = document.getElementById('grid');
    try {
      const res = await fetch('/api/social/offers');
      if (!res.ok) throw new Error('Не удалось загрузить предложения');
      const offers = await res.json();
      offers.sort((a,b) => {
        const aDays = a.expiryDate ? new Date(a.expiryDate) - new Date() : Infinity;
        const bDays = b.expiryDate ? new Date(b.expiryDate) - new Date() : Infinity;
        return aDays - bDays;
      });
      grid.innerHTML = offers.length ? offers.map(renderOffer).join('') : '<div class="empty-state glass-card" style="grid-column:1/-1"><h3>Пока нет опубликованных лотов</h3><p style="margin-top:8px">Магазин разместит здесь товары после проверки остатков и срока годности.</p></div>';
    } catch(e) { grid.innerHTML = '<div class="empty-state glass-card" style="grid-column:1/-1">Не удалось загрузить соцполку. Обновите страницу позже.</div>'; }
  }

  async function claimOffer(id) {
    if (ariaAuth.getRole() !== 'ORG') { location.href = '/login.html?next=%2Fsocial.html'; return; }
    try {
      const res = await ariaAuth.authFetch('/api/social/offers/' + id + '/claim', {method:'POST'});
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'Не удалось забрать лот');
      document.getElementById('partner-message').textContent = 'Лот зарезервирован. Свяжитесь с магазином для согласования времени и документов передачи.';
      await Promise.all([loadSocial(), loadClaims()]);
    } catch(e) { showToast(e.message, 'error'); }
  }

  async function downloadClaimAct(id) {
    try {
      const res = await ariaAuth.authFetch('/api/social/my-claims/' + id + '/act');
      if (!res.ok) throw new Error('Акт не найден');
      const url = URL.createObjectURL(await res.blob());
      const link = document.createElement('a');
      link.href = url; link.download = `aria-social-act-${id}.html`;
      document.body.appendChild(link); link.click(); link.remove();
      setTimeout(() => URL.revokeObjectURL(url), 30000);
    } catch(e) { showToast(e.message, 'error'); }
  }

  updateAuthNav();
  ariaTheme.initTheme();
  ariaI18n.initI18n();
  if (ariaAuth.getRole() === 'ADMIN') document.getElementById('admin-publish-link').style.display = 'inline-flex';
  if (ariaAuth.getRole() === 'ORG') document.getElementById('partner-message').textContent = 'Вы вошли как организация: можно резервировать опубликованные партии.';
  loadSocial();
  loadClaims();
