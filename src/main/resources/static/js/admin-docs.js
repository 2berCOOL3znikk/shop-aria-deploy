(function () {
  function requireAdmin() {
    if (window.ariaAuth && window.ariaAuth.getRole() === "ADMIN") return true;
    window.showToast("Нужна роль ADMIN", "error");
    return false;
  }
  async function reportRequest(path) {
    const response = await ariaAuth.authFetch("/api/aria/reports/" + path);
    if (!response.ok) {
      const body = await response.text();
      throw new Error(response.status === 403 ? "Нужна роль ADMIN" : body || "Не удалось получить документ");
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
  window.printTransferAct = async function (lotId) {
    if (!requireAdmin()) return;
    const printWindow = window.open("about:blank", "_blank");
    if (!printWindow) { showToast("Разрешите открытие окна для печати акта", "error"); return; }
    try {
      const blob = await reportRequest("acts/" + encodeURIComponent(lotId) + "/printable");
      printWindow.location.href = URL.createObjectURL(blob);
    } catch (error) {
      printWindow.close();
      showToast(error.message, "error");
    }
  };
})();