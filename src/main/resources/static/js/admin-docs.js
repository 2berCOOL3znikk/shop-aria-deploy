(function () {
  function requireAdmin() {
    if (window.ariaAuth && window.ariaAuth.getRole() === "ADMIN") return true;
    window.showToast(ariaCopy.text("text.30e9f1a23e16ca"), "error");
    return false;
  }
  async function reportRequest(path) {
    const response = await ariaAuth.authFetch("/api/aria/reports/" + path);
    if (!response.ok) {
      const body = await response.text();
      throw new Error(ariaCopy.errorText(response.status === 403 ? ariaCopy.text("text.30e9f1a23e16ca") : body || ariaCopy.text("text.2e07e38e36d674")));
    }
    return response.blob();
  }
  window.downloadAriaReport = async function (name) {
    if (!requireAdmin()) return;
    try {
      const blob = await reportRequest(name);
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = name;
      document.body.appendChild(link);
      link.click();
      link.remove();
      setTimeout(() => URL.revokeObjectURL(url), 30000);
    } catch (error) { showToast(error.message, "error"); }
  };
  window.printTransferAct = async function (lotId, markPrinted = true) {
    if (!requireAdmin()) return;
    const printWindow = window.open("about:blank", "_blank");
    if (!printWindow) { showToast(ariaCopy.text("text.0494ced02ce9b4"), "error"); return; }
    try {
      if (markPrinted) {
        const response = await ariaAuth.authFetch("/api/aria/logistics/lots/" + encodeURIComponent(lotId) + "/mark-printed", { method: "POST" });
        const result = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(ariaCopy.errorText(result.message || result.error || ariaCopy.text("text.6cec9dbb4a6e41")));
        window.refreshLogisticsQueue?.();
      }
      const blob = await reportRequest("acts/" + encodeURIComponent(lotId) + "/printable");
      printWindow.location.href = URL.createObjectURL(blob);
    } catch (error) {
      printWindow.close();
      showToast(error.message, "error");
    }
  };
})();