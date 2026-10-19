(function () {
  const escape = value => String(value ?? "").replace(/[&<>"']/g, ch => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch]);
  let selectedReason = "ALL";
  const tabs = [["PERISHABLE","Скоропорт"],["DEAD_STOCK","Мёртвый сток"],["PRICE","Ценовые риски"],["MARGIN","Отриц. маржа"],["ALL","Все"]];
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
    box.innerHTML = tabs.map(([key,label]) => '<button type="button" data-reason="' + key + '" aria-pressed="' + (key === selectedReason) + '">' + label + "</button>").join("");
    table.before(box);
    box.addEventListener("click", event => {
      const button = event.target.closest("button[data-reason]");
      if (!button) return;
      selectedReason = button.dataset.reason;
      box.querySelectorAll("button").forEach(tab => tab.setAttribute("aria-pressed", String(tab === button)));
      window.applyFilters();
    });
    const navLinks = document.getElementById("aria-shell-links");
    if (navLinks && !document.getElementById("analysis-history-link")) {
      const link = document.createElement("a");
      link.id = "analysis-history-link";
      link.href = "/analysis-history.html";
      link.className = "admin-history-nav-link";
      link.textContent = "История анализов";
      const supportLink = navLinks.querySelector('a[href="/support.html"]');
      navLinks.insertBefore(link, supportLink || null);
    }
    const note = document.querySelector(".admin-filter-row")?.previousElementSibling;
    if (note && note.classList.contains("admin-note")) note.textContent = "Показываются предложения ARIA. Администратор подтверждает скидку, списание или передачу; просроченные товары заблокированы.";
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
    tbody.innerHTML = filtered.length ? filtered.map(renderProductRow).join("") : '<tr><td colspan="11" class="admin-table-empty">Нет товаров, соответствующих выбранным фильтрам.</td></tr>';
  };
  async function loadSvgStats() {
    if (ariaAuth.getRole() !== "ADMIN") return;
    try {
      const response = await ariaAuth.authFetch("/api/aria/stats");
      if (response.status === 403) throw new Error("Нужна роль ADMIN");
      if (!response.ok) throw new Error("Не удалось загрузить статистику");
      const stats = await response.json();
      const labels = { ACTIVE:"Активны", CANDIDATE:"Кандидаты", RESERVED:"В резерве", SOCIAL:"Соц. полка", DISCOUNTED:"Со скидкой", TRANSFERRED:"Переданы", WRITTEN_OFF:"Списаны" };
      const colors = ["#648a78","#bd8a4f","#788da0","#859f76","#b99b5b","#587c6c","#9b7770"];
      const entries = Object.entries(stats.statusCounts || {});
      const total = Math.max(1, entries.reduce((sum,[,count]) => sum + Number(count), 0));
      const width = 640, rowHeight = 36, height = Math.max(64, entries.length * rowHeight + 20);
      const rows = entries.map(([key,value],index) => {
        const y = 16 + index * rowHeight, barWidth = Math.round(Number(value) / total * 360);
        return '<text x="0" y="' + (y+17) + '" fill="currentColor" font-size="13">' + escape(labels[key] || key) + '</text><rect x="150" y="' + y + '" width="360" height="20" rx="6" fill="var(--bg-elevated)"/><rect x="150" y="' + y + '" width="' + barWidth + '" height="20" rx="6" fill="' + colors[index % colors.length] + '"/><text x="526" y="' + (y+16) + '" fill="currentColor" font-size="13">' + Number(value) + "</text>";
      }).join("");
      const chart = document.getElementById("status-chart");
      if (chart) chart.innerHTML = '<svg class="admin-inline-chart" viewBox="0 ' + "0 " + width + " " + height + '" role="img" aria-label="Товары по статусам">' + rows + "</svg>";
      const categories = Object.entries(stats.categoryBreakdown || {}).sort((a,b) => b[1]-a[1]).slice(0,8);
      const max = Math.max(1, ...categories.map(([,value]) => Number(value)));
      const catRows = categories.map(([name,value],index) => {
        const y = 16 + index * rowHeight, barWidth = Math.round(Number(value) / max * 360);
        return '<text x="0" y="' + (y+17) + '" fill="currentColor" font-size="13">' + escape(name) + '</text><rect x="150" y="' + y + '" width="360" height="20" rx="6" fill="var(--bg-elevated)"/><rect x="150" y="' + y + '" width="' + barWidth + '" height="20" rx="6" fill="#718d83"/><text x="526" y="' + (y+16) + '" fill="currentColor" font-size="13">' + Number(value) + "</text>";
      }).join("");
      const category = document.getElementById("category-chart");
      if (category) category.innerHTML = categories.length ? '<svg class="admin-inline-chart" viewBox="0 ' + "0 " + width + " " + Math.max(64,categories.length*rowHeight+20) + '" role="img" aria-label="Товары по категориям">' + catRows + "</svg>" : '<p class="admin-note">Нет категорий.</p>';
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