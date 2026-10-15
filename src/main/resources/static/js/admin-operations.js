(function () {
  const escape = value => String(value ?? "").replace(/[&<>"']/g, ch => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch]);
  const api = async (url, options = {}) => {
    const response = await ariaAuth.authFetch(url, options);
    const body = await response.json().catch(() => ({}));
    if (response.status === 403) throw new Error("Нужна роль ADMIN");
    if (!response.ok) throw new Error(body.message || body.error || "Запрос не выполнен");
    return body;
  };
  function mount() {
    if (ariaAuth.getRole() !== "ADMIN") return;
    const host = document.querySelector("main") || document.body;
    const sections = document.createElement("div");
    sections.className = "admin-operations-grid";
    sections.innerHTML = '<section class="glass-card admin-operation-card"><div class="admin-operation-heading"><div><span class="product-card-category">Партнёры</span><h2>Проверка организаций</h2></div><button class="btn-outline" id="refresh-org-queue">Обновить</button></div><div id="organization-queue"><p class="admin-note">Загрузка заявок…</p></div></section><section class="glass-card admin-operation-card"><div class="admin-operation-heading"><div><span class="product-card-category">Передача запасов</span><h2>Логистика лотов</h2></div><button class="btn-outline" id="refresh-logistics">Обновить</button></div><div id="logistics-queue"><p class="admin-note">Загрузка очереди…</p></div></section>';
    const overlay = document.getElementById("analysis-overlay");
    if (overlay?.parentElement === host) host.insertBefore(sections, overlay); else host.appendChild(sections);
    document.getElementById("refresh-org-queue").addEventListener("click", loadOrganizations);
    document.getElementById("refresh-logistics").addEventListener("click", loadLogistics);
    loadOrganizations(); loadLogistics();
  }
  async function loadOrganizations() {
    const box = document.getElementById("organization-queue");
    try {
      const rows = await api("/api/admin/organizations/verification-requests");
      box.innerHTML = rows.length ? rows.map(row => '<article class="verification-row"><div><strong>' + escape(row.organizationName) + '</strong><p>' + escape(row.email) + ' · № ' + escape(row.registrationNumber) + '</p><small>Заявка ' + new Date(row.submittedAt).toLocaleString("ru-KZ") + '</small></div><button class="btn-glow" onclick="approveOrganization(' + Number(row.id) + ')">Подтвердить организацию</button></article>').join("") : '<div class="admin-empty">Нет заявок, ожидающих проверки.</div>';
    } catch (error) { box.innerHTML = '<p class="admin-error">' + escape(error.message) + '</p>'; }
  }
  async function loadLogistics() {
    const box = document.getElementById("logistics-queue");
    try {
      const data = await api("/api/aria/logistics/queue");
      const items = rows => rows.map(item => '<li><span><strong>' + escape(item.name) + '</strong> · ' + escape(item.sku) + ' · ' + Number(item.quantity) + ' шт.' + (item.expiryDate ? ' · до ' + escape(item.expiryDate) : '') + '</span><small>' + escape(item.reason || "") + '</small></li>').join("");
      const waitingAction = data.waitingReady ? '<button class="btn-glow" onclick="createLotFromQueue(\'waiting\')">Сформировать обычный лот</button>' : '<p class="admin-note">Партия копится до порога. Передача не запускается автоматически.</p>';
      const criticalAction = data.critical.length ? '<button class="btn-glow" onclick="createLotFromQueue(\'critical\')">Сформировать срочный лот</button>' : '<p class="admin-note">Срочных позиций сейчас нет.</p>';
      const drafts = data.draftLots.length ? data.draftLots.map(lot => '<div class="claim-row"><span>Черновик #' + Number(lot.id) + ' · ' + Number(lot.itemCount) + ' SKU · ' + escape(lot.partner) + '</span><button class="btn-outline" onclick="confirmLotTransfer(' + Number(lot.id) + ')">Подтвердить фактическую передачу</button></div>').join("") : '<p class="admin-note">Нет черновиков, ожидающих подтверждения.</p>';
      box.innerHTML = '<div class="logistics-block"><h3>Ждут наполнения</h3><p>' + Number(data.waitingUnits) + ' / ' + Number(data.threshold) + ' единиц</p><div class="lot-progress"><span style="width:' + Number(data.waitingProgress) + '%"></span></div><ul>' + items(data.waiting) + '</ul>' + waitingAction + '</div><div class="logistics-block critical-block"><h3>Критические — отправить сейчас</h3><ul>' + items(data.critical) + '</ul>' + criticalAction + '</div><div class="logistics-block expired-block"><h3>Срок истёк — передача заблокирована</h3><ul>' + items(data.expired) + '</ul>' + (data.expired.length ? '<p class="admin-error">Просроченное нельзя резервировать или передавать.</p>' : '<p class="admin-note">Просроченных позиций нет.</p>') + '</div><div class="logistics-block"><h3>Черновики актов</h3>' + drafts + '</div>';
    } catch (error) { box.innerHTML = '<p class="admin-error">' + escape(error.message) + '</p>'; }
  }
  window.approveOrganization = async id => {
    try {
      await api("/api/admin/organizations/verification-requests/" + encodeURIComponent(id) + "/approve", { method: "POST" });
      showToast("Организация подтверждена");
      loadOrganizations();
    } catch (error) { showToast(error.message, "error"); }
  };
  window.createLotFromQueue = async group => {
    try {
      const data = await api("/api/aria/logistics/queue");
      const selected = group === "critical" ? data.critical : data.waiting;
      if (!selected.length) return;
      const query = new URLSearchParams({ partner: "Благотворительная организация" });
      selected.forEach(item => query.append("productIds", item.id));
      const lot = await api("/api/aria/transfer?" + query.toString(), { method: "POST" });
      showToast("Черновик лота создан. Подтвердите передачу после фактической выдачи.");
      loadLogistics();
    } catch (error) { showToast(error.message, "error"); }
  };
  window.confirmLotTransfer = async id => {
    try {
      await api("/api/aria/logistics/lots/" + encodeURIComponent(id) + "/confirm-transfer", { method: "POST" });
      showToast("Фактическая передача подтверждена");
      loadLogistics();
    } catch (error) { showToast(error.message, "error"); }
  };
  document.addEventListener("DOMContentLoaded", mount);
})();