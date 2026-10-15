window.ariaTheme = {
  getTheme() { return localStorage.getItem("aria_theme") || "light"; },
  applyTheme(value) {
    const theme = value === "dark" ? "dark" : "light";
    document.documentElement.dataset.theme = theme;
    localStorage.setItem("aria_theme", theme);
    this.updateThemeButton();
  },
  toggleTheme() { this.applyTheme(this.getTheme() === "dark" ? "light" : "dark"); },
  updateThemeButton() {
    const button = document.getElementById("theme-toggle-btn");
    if (!button) return;
    button.textContent = this.getTheme() === "dark" ? "☀" : "🌙";
    button.title = this.getTheme() === "dark" ? "Светлая тема" : "Тёмная тема";
  },
  initTheme() { this.applyTheme(this.getTheme()); }
};
window.theme = window.ariaTheme;
document.documentElement.dataset.theme = localStorage.getItem("aria_theme") || "light";
if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", () => window.ariaTheme.initTheme());
else window.ariaTheme.initTheme();