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
    ariaCopy.setProperty(box,"innerHTML",filtered.length ? filtered.map(log => '<article class="glass-card history-log"><div class="history-log-top"><strong>' + new Date(log.analyzedAt).toLocaleString(ariaCopy.locale()) + '</strong><span>' + Number(log.totalScanned) + ariaCopy.text("text.118b9941f7ad8e") + Number(log.candidatesFound) + ariaCopy.text("text.7ec701d42cafc7") + Number(log.returnedToActive) + ariaCopy.text("text.4b3197c3c1e511") + Number(log.urgentItems) + '</b></span></div><p>' + escape(log.details || "") + '</p></article>').join("") : ariaCopy.text("text.0b7688996ac814"));
  }
  document.addEventListener("DOMContentLoaded", async () => {
    if (!ariaAuth.requireAdmin()) return;
    ["history-search", "history-from", "history-to"].forEach(id => document.getElementById(id).addEventListener("input", render));
    try {
      const response = await ariaAuth.authFetch("/api/aria/logs");
      if (response.status === 403) throw new Error(ariaCopy.errorText(ariaCopy.text("text.30e9f1a23e16ca")));
      if (!response.ok) throw new Error(ariaCopy.errorText(ariaCopy.text("text.ba3aef863acf87")));
      rows = await response.json();
      render();
    } catch (error) { ariaCopy.setProperty(document.getElementById("analysis-history-list"),"innerHTML",'<div class="glass-card history-empty">' + escape(error.message) + '</div>'); }
  });
})();