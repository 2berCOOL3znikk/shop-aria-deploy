// Модуль управления темой (светлая/тёмная) для ARIA Shop
window.ariaTheme = {
    getTheme: function() {
        return localStorage.getItem('aria_theme') || 'dark';
    },

    applyTheme: function(theme) {
        document.documentElement.dataset.theme = theme;
        localStorage.setItem('aria_theme', theme);
        this.updateThemeButton();
    },

    toggleTheme: function() {
        const current = this.getTheme();
        const next = current === 'dark' ? 'light' : 'dark';
        this.applyTheme(next);
    },

    updateThemeButton: function() {
        const btn = document.getElementById('theme-toggle-btn');
        if (btn) {
            const theme = this.getTheme();
            btn.textContent = theme === 'dark' ? '☀' : '🌙';
        }
    },

    initTheme: function() {
        const theme = this.getTheme();
        document.documentElement.dataset.theme = theme;
        this.updateThemeButton();
    }
};

// Применяем тему сразу при загрузке
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
        window.ariaTheme.initTheme();
    });
} else {
    window.ariaTheme.initTheme();
}
