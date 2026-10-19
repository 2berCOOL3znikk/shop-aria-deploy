(function () {
  const escape = value => String(value ?? "").replace(/[&<>"']/g, ch => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch]);
  const api = async (url, options = {}) => {
    const response = await ariaAuth.authFetch(url, options);
    const body = await response.json().catch(() => ({}));
    if (response.status === 403) throw new Error("Нужна роль ADMIN");
    if (!response.ok) throw new Error(body.message || body.error || "Запрос не выполнен");
    return body;
  };
  const stageInfo = {
    DRAFT: { label: "Черновик", action: "Подтвердить состав акта", next: "confirm" },
    CONFIRMED: { label: "Подтверждён", action: "Распечатать акт", print: true },
    PRINTED: { label: "Распечатан", action: "Подписан обеими сторонами", next: "sign" },
    SIGNED: { label: "Подписан", action: "Подтвердить фактическую передачу", next: "confirm-transfer" }
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
    loadOrganizations();
    loadLogistics();
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
      const waiting = Array.isArray(data.waiting) ? data.waiting : [];
      const critical = Array.isArray(data.critical) ? data.critical : [];
      const expired = Array.isArray(data.expired) ? data.expired : [];
      const items = rows => rows.map(item => '<li><span><strong>' + escape(item.name) + '</strong> · ' + escape(item.sku) + ' · ' + Number(item.quantity) + ' шт.' + (item.expiryDate ? ' · до ' + escape(item.expiryDate) : '') + '</span><small>' + escape(item.reason || "") + '</small></li>').join("");
      const waitingUnits = Math.max(0, Number(data.waitingUnits) || waiting.reduce((total, item) => total + Math.max(0, Number(item.quantity || 0)), 0));
      const threshold = Math.max(1, Number(data.threshold) || 10);
      const displayedUnits = Math.min(waitingUnits, threshold);
      const waitingProgress = Math.min(100, Math.floor(displayedUnits * 100 / threshold));
      const waitingBacklog = Math.max(0, waitingUnits - threshold);
      const waitingAction = waitingUnits >= threshold ? '<button class="btn-glow" onclick="createLotFromQueue(\'waiting\')">Сформировать лот из ожидающих товаров</button>' : '<p class="admin-note">Партия копится до порога. Передача не запускается автоматически.</p>';
      const criticalAction = critical.length ? '<button class="btn-glow" onclick="createLotFromQueue(\'critical\')">Сформировать срочный лот</button>' : '<p class="admin-note">Срочных позиций сейчас нет.</p>';
      const acts = Array.isArray(data.draftLots) ? data.draftLots : [];
      const pendingActs = acts.length ? acts.map(lot => {
        const stage = stageInfo[lot.state] || { label: lot.state, action: "Открыть акт", print: true };
        const button = stage.print
          ? '<button class="btn-outline" onclick="printTransferAct(' + Number(lot.id) + ')">' + stage.action + '</button>'
          : '<button class="btn-outline" onclick="advanceActStage(' + Number(lot.id) + ',\'' + escape(lot.state) + '\')">' + stage.action + '</button>';
        return '<div class="claim-row"><span><strong>Акт ARIA-' + Number(lot.id) + ' · ' + stage.label + '</strong><br>' + escape(lot.partner) + ' · ' + Number(lot.itemCount) + ' SKU · ' + Number(lot.quantity || 0) + ' шт.</span>' + button + '</div>';
      }).join("") : '<p class="admin-note">Нет актов, ожидающих завершения.</p>';
      box.innerHTML = '<div class="logistics-block"><h3>Ждут наполнения</h3><p class="logistics-progress-label"><strong>' + displayedUnits + ' / ' + threshold + ' единиц</strong> · порог партии</p><div class="lot-progress" role="progressbar" aria-valuemin="0" aria-valuemax="' + threshold + '" aria-valuenow="' + displayedUnits + '" aria-valuetext="' + displayedUnits + ' из ' + threshold + ' единиц до порога"><span style="width:' + waitingProgress + '%"></span></div>' + (waitingBacklog ? '<p class="admin-note logistics-backlog">В очереди сверх порога: <strong>' + waitingBacklog + ' ед.</strong></p>' : '') + '<ul>' + items(waiting) + '</ul>' + waitingAction + '</div><div class="logistics-block critical-block"><h3>Критические — отправить сейчас</h3><ul>' + items(critical) + '</ul>' + criticalAction + '</div><div class="logistics-block expired-block"><h3>Срок истёк — передача заблокирована</h3><ul>' + items(expired) + '</ul>' + (expired.length ? '<p class="admin-error">Просроченное нельзя резервировать или передавать.</p>' : '<p class="admin-note">Просроченных позиций нет.</p>') + '</div><div class="logistics-block"><h3>Акты в работе</h3><p class="admin-note">Акт — документ для фонда: распечатать, сверить позиции, подписать обеим сторонам.</p>' + pendingActs + '</div>';
    } catch (error) { box.innerHTML = '<p class="admin-error">' + escape(error.message) + '</p>'; }
  }
  window.refreshLogisticsQueue = loadLogistics;
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
      const partner = document.getElementById("transfer-partner")?.value.trim();
      if (!partner) { showToast("Укажите организацию-получателя акта.", "error"); return; }
      const query = new URLSearchParams({ partner });
      selected.forEach(item => query.append("productIds", item.id));
      await api("/api/aria/transfer?" + query.toString(), { method: "POST" });
      showToast("Черновик лота создан. Сверьте позиции и подтвердите акт.");
      loadLogistics();
    } catch (error) { showToast(error.message, "error"); }
  };
  window.advanceActStage = async (id, state) => {
    const endpoints = { DRAFT: "confirm", PRINTED: "sign", SIGNED: "confirm-transfer" };
    const endpoint = endpoints[state];
    if (!endpoint) return;
    try {
      await api("/api/aria/logistics/lots/" + encodeURIComponent(id) + "/" + endpoint, { method: "POST" });
      showToast(state === "SIGNED" ? "Фактическая передача подтверждена" : "Статус акта обновлён");
      loadLogistics();
    } catch (error) { showToast(error.message, "error"); }
  };
  window.confirmLotTransfer = id => window.advanceActStage(id, "SIGNED");
  document.addEventListener("DOMContentLoaded", mount);
})();
