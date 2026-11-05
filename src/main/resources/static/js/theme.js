window.ariaTheme = {
  DEFAULT: "light",
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
    ariaCopy.setProperty(b,"textContent",dark ? "☀" : "🌙");
    ariaCopy.setProperty(b,"title",dark ? ariaCopy.text("text.46bf625a0f170f") : ariaCopy.text("text.46fd0410f99d68"));
  },
  initTheme() { this.applyTheme(this.getTheme()); }
};
window.theme = window.ariaTheme;
document.documentElement.dataset.theme = (function(){ const v = localStorage.getItem("aria_theme"); return v === "light" || v === "dark" ? v : "light"; })();
if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", () => window.ariaTheme.initTheme());
else window.ariaTheme.initTheme();