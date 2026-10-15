(function () {
  const escape = value => String(value ?? "").replace(/[&<>"']/g, ch => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch]);
  let rows = [];
  function render() {
    const query = document.getElementById("history-search").value.trim().toLocaleLowerCase("ru");
    const from = document.getElementById("history-from").value;
    const to = document.getElementById("history-to").value;
    const filtered = rows.filter(log => {
      const date = String(log.analyzedAt || "").slice(0, 10);
      return (!query || String(log.details || "").toLocaleLowerCase("ru").includes(query)) && (!from || date >= from) && (!to || date <= to);
    });
    const box = document.getElementById("analysis-history-list");
    box.innerHTML = filtered.length ? filtered.map(log => '<article class="glass-card history-log"><div class="history-log-top"><strong>' + new Date(log.analyzedAt).toLocaleString("ru-KZ") + '</strong><span>' + Number(log.totalScanned) + ' товаров</span></div><div class="history-stats"><span>Кандидаты <b>' + Number(log.candidatesFound) + '</b></span><span>Возвращено в актив <b>' + Number(log.returnedToActive) + '</b></span><span>Срочные <b>' + Number(log.urgentItems) + '</b></span></div><p>' + escape(log.details || "") + '</p></article>').join("") : '<div class="glass-card history-empty">Записей по выбранным фильтрам нет.</div>';
  }
  document.addEventListener("DOMContentLoaded", async () => {
    if (!ariaAuth.requireAdmin()) return;
    ["history-search", "history-from", "history-to"].forEach(id => document.getElementById(id).addEventListener("input", render));
    try {
      const response = await ariaAuth.authFetch("/api/aria/logs");
      if (response.status === 403) throw new Error("Нужна роль ADMIN");
      if (!response.ok) throw new Error("Не удалось загрузить историю");
      rows = await response.json();
      render();
    } catch (error) { document.getElementById("analysis-history-list").innerHTML = '<div class="glass-card history-empty">' + escape(error.message) + '</div>'; }
  });
})();