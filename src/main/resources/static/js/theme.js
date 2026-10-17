window.ariaTheme = {
  DEFAULT: "dark", // <-- светлая по умолчанию? поставь "light"
  getTheme() { const v = localStorage.getItem("aria_theme"); return v === "light" || v === "dark" ? v : this.DEFAULT; },
  applyTheme(value) {
    const theme = value === "light" ? "light" : "dark";
    document.documentElement.dataset.theme = theme;
    localStorage.setItem("aria_theme", theme);
    this.updateThemeButton();
  },
  toggleTheme() { this.applyTheme(this.getTheme() === "dark" ? "light" : "dark"); },
  updateThemeButton() {
    const b = document.getElementById("theme-toggle-btn"); if (!b) return;
    const dark = this.getTheme() === "dark";
    b.textContent = dark ? "☀" : "🌙";
    b.title = dark ? "Светлая тема" : "Тёмная тема";
  },
  initTheme() { this.applyTheme(this.getTheme()); }
};
window.theme = window.ariaTheme;
document.documentElement.dataset.theme = (function(){ const v = localStorage.getItem("aria_theme"); return v === "light" || v === "dark" ? v : "dark"; })();
if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", () => window.ariaTheme.initTheme());
else window.ariaTheme.initTheme();