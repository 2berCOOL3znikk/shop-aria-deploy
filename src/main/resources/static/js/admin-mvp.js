(function () {
  const escape = value => String(value ?? "").replace(/[&<>"']/g, ch => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch]);
  let selectedReason = "ALL";
  const tabs = [["PERISHABLE",ariaCopy.text("text.ec014fdf2b9b3a")],["DEAD_STOCK",ariaCopy.text("text.0145d355b7f3ec")],["PRICE",ariaCopy.text("text.d42c86561cce88")],["MARGIN",ariaCopy.text("text.b103f1b3d76a06")],["ALL",ariaCopy.text("text.215816bf42103f")]];
  function reasonOf(product) {
    const reason = String(product.ariaReason || "").toLocaleLowerCase("ru");
    if (reason.includes("срок годности") || reason.includes("скоропорт") || reason.includes("истёк")) return "PERISHABLE";
    if (reason.includes("не продаётся") || reason.includes("мёртвый сток")) return "DEAD_STOCK";
    if (reason.includes("маржа")) return "MARGIN";
    if (reason.includes("рынка") || reason.includes("выше рынка")) return "PRICE";
    return "ALL";
  }
  function mountTabs() {
    if (ariaAuth.getRole() !== "ADMIN") return;
    const table = document.querySelector(".admin-table");
    if (!table || document.getElementById("aria-reason-tabs")) return;
    const box = document.createElement("div");
    box.id = "aria-reason-tabs"; box.className = "aria-reason-tabs";
    ariaCopy.setProperty(box,"innerHTML",tabs.map(([key,label]) => '<button type="button" data-reason="' + key + '" aria-pressed="' + (key === selectedReason) + '">' + label + "</button>").join(""));
    table.before(box);
    box.addEventListener("click", event => {
      const button = event.target.closest("button[data-reason]");
      if (!button) return;
      selectedReason = button.dataset.reason;
      box.querySelectorAll("button").forEach(tab => tab.setAttribute("aria-pressed", String(tab === button)));
      window.applyFilters();
    });
    const note = document.querySelector(".admin-filter-row")?.previousElementSibling;
    if (note && note.classList.contains("admin-note")) ariaCopy.setProperty(note,"textContent",ariaCopy.text("text.f058a4d94e0d0a"));
  }
  window.applyFilters = function () {
    const search = document.getElementById("filter-search")?.value.toLocaleLowerCase("ru") || "";
    const dateFrom = document.getElementById("filter-date-from")?.value || "";
    const dateTo = document.getElementById("filter-date-to")?.value || "";
    const status = document.getElementById("filter-status")?.value || "";
    const filtered = allProducts.filter(product => {
      const name = String(product.name || "").toLocaleLowerCase("ru");
      const sku = String(product.sku || "").toLocaleLowerCase("ru");
      const reason = String(product.ariaReason || "").toLocaleLowerCase("ru");
      const received = product.receivedAt ? new Date(product.receivedAt) : null;
      return (!search || name.includes(search) || sku.includes(search) || reason.includes(search))
        && (!status || product.status === status)
        && (!dateFrom || (received && received >= new Date(dateFrom)))
        && (!dateTo || (received && received <= new Date(dateTo + "T23:59:59")))
        && (selectedReason === "ALL" || reasonOf(product) === selectedReason);
    });
    const tbody = document.getElementById("aria-table-body");
    ariaCopy.setProperty(tbody,"innerHTML",filtered.length ? filtered.map(renderProductRow).join("") : ariaCopy.text("text.ebc5799acdf044"));
  };
  async function loadSvgStats() {
    if (ariaAuth.getRole() !== "ADMIN") return;
    try {
      const response = await ariaAuth.authFetch("/api/aria/stats");
      if (response.status === 403) throw new Error(ariaCopy.errorText(ariaCopy.text("text.30e9f1a23e16ca")));
      if (!response.ok) throw new Error(ariaCopy.errorText(ariaCopy.text("text.5b6ddaa618c932")));
      const stats = await response.json();
      const labels = { ACTIVE:ariaCopy.text("text.44b19be1a46aea"), CANDIDATE:ariaCopy.text("text.42b77f795fd9ea"), RESERVED:ariaCopy.text("text.316feb5263c4a3"), SOCIAL:ariaCopy.text("text.fd3069dfffcedf"), DISCOUNTED:ariaCopy.text("text.002be70a2f1630"), TRANSFERRED:ariaCopy.text("text.ee96c20ad666e6"), WRITTEN_OFF:ariaCopy.text("text.3698e41d76ab6c") };
      const colors = ["#648a78","#bd8a4f","#788da0","#859f76","#b99b5b","#587c6c","#9b7770"];
      const entries = Object.entries(stats.statusCounts || {});
      const total = Math.max(1, entries.reduce((sum,[,count]) => sum + Number(count), 0));
      const width = 640, rowHeight = 36, height = Math.max(64, entries.length * rowHeight + 20);
      const rows = entries.map(([key,value],index) => {
        const y = 16 + index * rowHeight, barWidth = Math.round(Number(value) / total * 360);
        return '<text x="0" y="' + (y+17) + '" fill="currentColor" font-size="13">' + escape(labels[key] || key) + '</text><rect x="150" y="' + y + '" width="360" height="20" rx="6" fill="var(--bg-elevated)"/><rect x="150" y="' + y + '" width="' + barWidth + '" height="20" rx="6" fill="' + colors[index % colors.length] + '"/><text x="526" y="' + (y+16) + '" fill="currentColor" font-size="13">' + Number(value) + "</text>";
      }).join("");
      const chart = document.getElementById("status-chart");
      if (chart) ariaCopy.setProperty(chart,"innerHTML",'<svg class="admin-inline-chart" viewBox="0 ' + "0 " + width + " " + height + ariaCopy.text("text.9ac473e81c8afc") + rows + "</svg>");
      const categories = Object.entries(stats.categoryBreakdown || {}).sort((a,b) => b[1]-a[1]).slice(0,8);
      const max = Math.max(1, ...categories.map(([,value]) => Number(value)));
      const catRows = categories.map(([name,value],index) => {
        const y = 16 + index * rowHeight, barWidth = Math.round(Number(value) / max * 360);
        return '<text x="0" y="' + (y+17) + '" fill="currentColor" font-size="13">' + escape(name) + '</text><rect x="150" y="' + y + '" width="360" height="20" rx="6" fill="var(--bg-elevated)"/><rect x="150" y="' + y + '" width="' + barWidth + '" height="20" rx="6" fill="#718d83"/><text x="526" y="' + (y+16) + '" fill="currentColor" font-size="13">' + Number(value) + "</text>";
      }).join("");
      const category = document.getElementById("category-chart");
      if (category) ariaCopy.setProperty(category,"innerHTML",categories.length ? '<svg class="admin-inline-chart" viewBox="0 ' + "0 " + width + " " + Math.max(64,categories.length*rowHeight+20) + ariaCopy.text("text.d3e1c5bc554edd") + catRows + "</svg>" : ariaCopy.text("text.e86d6b7fd4397c"));
    } catch (error) { window.showToast(error.message,"error"); }
  }
  window.loadStats = loadSvgStats;
  document.addEventListener("DOMContentLoaded", () => {
    mountTabs();
    document.getElementById("filter-search")?.addEventListener("input", () => window.applyFilters());
    document.getElementById("filter-status")?.addEventListener("change", () => window.applyFilters());
    document.getElementById("filter-date-from")?.addEventListener("change", () => window.applyFilters());
    document.getElementById("filter-date-to")?.addEventListener("change", () => window.applyFilters());
    loadSvgStats();
  });
})();
