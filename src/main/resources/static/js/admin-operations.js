(function () {
  const escape = value => String(value ?? "").replace(/[&<>"']/g, ch => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch]);
  const api = async (url, options = {}) => {
    const response = await ariaAuth.authFetch(url, options);
    const body = await response.json().catch(() => ({}));
    if (response.status === 403) throw new Error(ariaCopy.errorText(ariaCopy.text("text.30e9f1a23e16ca")));
    if (!response.ok) throw new Error(ariaCopy.errorText(body.message || body.error || ariaCopy.text("text.ee48dc403e1841")));
    return body;
  };
  const stageInfo = {
    DRAFT: { label: ariaCopy.text("text.65fd24f1cb37fe"), action: ariaCopy.text("text.c618f9334b2fb1"), next: "confirm" },
    CONFIRMED: { label: ariaCopy.text("text.f19bb8c78d60b7"), action: ariaCopy.text("text.b58ed2206c19b4"), print: true },
    PRINTED: { label: ariaCopy.text("text.9b97b724b72d9f"), action: ariaCopy.text("text.da855d4a2156f4"), next: "sign" },
    SIGNED: { label: ariaCopy.text("text.524bdbc8592106"), action: ariaCopy.text("text.c6a20d5b9b9fef"), next: "confirm-transfer" }
  };
  function mount() {
    if (ariaAuth.getRole() !== "ADMIN") return;
    const host = document.querySelector("main") || document.body;
    const sections = document.createElement("div");
    sections.className = "admin-operations-grid";
    ariaCopy.setProperty(sections,"innerHTML",ariaCopy.text("text.8d362f69f8cc2e"));
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
      ariaCopy.setProperty(box,"innerHTML",rows.length ? rows.map(row => '<article class="verification-row"><div><strong>' + escape(row.organizationName) + '</strong><p>' + escape(row.email) + ' · № ' + escape(row.registrationNumber) + ariaCopy.text("text.8ae1b765f2a5be") + new Date(row.submittedAt).toLocaleString(ariaCopy.locale()) + '</small></div><button class="btn-glow" onclick="approveOrganization(' + Number(row.id) + ariaCopy.text("text.7684c40129e724")).join("") : ariaCopy.text("text.6fa5295407f917"));
    } catch (error) { ariaCopy.setProperty(box,"innerHTML",'<p class="admin-error">' + escape(error.message) + '</p>'); }
  }
  async function loadLogistics() {
    const box = document.getElementById("logistics-queue");
    try {
      const data = await api("/api/aria/logistics/queue");
      const waiting = Array.isArray(data.waiting) ? data.waiting : [];
      const critical = Array.isArray(data.critical) ? data.critical : [];
      const expired = Array.isArray(data.expired) ? data.expired : [];
      const items = rows => rows.map(item => '<li><span><strong>' + escape(item.name) + '</strong> · ' + escape(item.sku) + ' · ' + Number(item.quantity) + ariaCopy.text("text.297cac49ffa570") + (item.expiryDate ? ariaCopy.text("text.0afb1c2a3da316") + escape(item.expiryDate) : '') + '</span><small>' + escape(item.reason || "") + '</small></li>').join("");
      const waitingUnits = Math.max(0, Number(data.waitingUnits) || waiting.reduce((total, item) => total + Math.max(0, Number(item.quantity || 0)), 0));
      const threshold = Math.max(1, Number(data.threshold) || 10);
      const displayedUnits = Math.min(waitingUnits, threshold);
      const waitingProgress = Math.min(100, Math.floor(displayedUnits * 100 / threshold));
      const waitingBacklog = Math.max(0, waitingUnits - threshold);
      const waitingAction = waitingUnits >= threshold ? ariaCopy.text("text.4acf0888013ea5") : ariaCopy.text("text.51191d9f268360");
      const criticalAction = critical.length ? ariaCopy.text("text.88425cb17e83cc") : ariaCopy.text("text.38129f60ba1927");
      const acts = Array.isArray(data.draftLots) ? data.draftLots : [];
      const pendingActs = acts.length ? acts.map(lot => {
        const stage = stageInfo[lot.state] || { label: lot.state, action: ariaCopy.text("text.e8bf6855b33c9e"), print: true };
        const button = stage.print
          ? '<button class="btn-outline" onclick="printTransferAct(' + Number(lot.id) + ')">' + stage.action + '</button>'
          : '<button class="btn-outline" onclick="advanceActStage(' + Number(lot.id) + ',\'' + escape(lot.state) + '\')">' + stage.action + '</button>';
        return ariaCopy.text("text.e10acc8a6e1f03") + Number(lot.id) + ' · ' + stage.label + '</strong><br>' + escape(lot.partner) + ' · ' + Number(lot.itemCount) + ' SKU · ' + Number(lot.quantity || 0) + ariaCopy.text("text.78996ec8f585b3") + button + '</div>';
      }).join("") : ariaCopy.text("text.cd69529def62ac");
      ariaCopy.setProperty(box,"innerHTML",ariaCopy.text("text.2a2b0a480cc74b") + displayedUnits + ' / ' + threshold + ariaCopy.text("text.ee4cf4bc424468") + threshold + '" aria-valuenow="' + displayedUnits + '" aria-valuetext="' + displayedUnits + ariaCopy.text("text.e029cc4b39ced1") + threshold + ariaCopy.text("text.dd267a54d0180e") + waitingProgress + '%"></span></div>' + (waitingBacklog ? ariaCopy.text("text.b1bf709a5cd75c") + waitingBacklog + ariaCopy.text("text.a061dbf277b734") : '') + '<ul>' + items(waiting) + '</ul>' + waitingAction + ariaCopy.text("text.5e494cc8458643") + items(critical) + '</ul>' + criticalAction + ariaCopy.text("text.0602ce52584125") + items(expired) + '</ul>' + (expired.length ? ariaCopy.text("text.dfbc6281bae8fd") : ariaCopy.text("text.a4653c8b43996c")) + ariaCopy.text("text.ca8b00eb1d4fa6") + pendingActs + '</div>');
    } catch (error) { ariaCopy.setProperty(box,"innerHTML",'<p class="admin-error">' + escape(error.message) + '</p>'); }
  }
  window.refreshLogisticsQueue = loadLogistics;
  window.approveOrganization = async id => {
    try {
      await api("/api/admin/organizations/verification-requests/" + encodeURIComponent(id) + "/approve", { method: "POST" });
      showToast(ariaCopy.text("text.d14cc3f5665a34"));
      loadOrganizations();
    } catch (error) { showToast(error.message, "error"); }
  };
  window.createLotFromQueue = async group => {
    try {
      const data = await api("/api/aria/logistics/queue");
      const selected = group === "critical" ? data.critical : data.waiting;
      if (!selected.length) return;
      const partner = document.getElementById("transfer-partner")?.value.trim();
      if (!partner) { showToast(ariaCopy.text("text.1e146103ec8919"), "error"); return; }
      const query = new URLSearchParams({ partner });
      selected.forEach(item => query.append("productIds", item.id));
      await api("/api/aria/transfer?" + query.toString(), { method: "POST" });
      showToast(ariaCopy.text("text.442403e4cfd939"));
      loadLogistics();
    } catch (error) { showToast(error.message, "error"); }
  };
  window.advanceActStage = async (id, state) => {
    const endpoints = { DRAFT: "confirm", PRINTED: "sign", SIGNED: "confirm-transfer" };
    const endpoint = endpoints[state];
    if (!endpoint) return;
    try {
      await api("/api/aria/logistics/lots/" + encodeURIComponent(id) + "/" + endpoint, { method: "POST" });
      showToast(state === "SIGNED" ? ariaCopy.text("text.ebc4706d38f7ff") : ariaCopy.text("text.3b17c659f778ff"));
      loadLogistics();
    } catch (error) { showToast(error.message, "error"); }
  };
  window.confirmLotTransfer = id => window.advanceActStage(id, "SIGNED");
  document.addEventListener("DOMContentLoaded", mount);
})();
