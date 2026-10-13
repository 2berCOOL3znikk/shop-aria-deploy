// Модуль аутентификации для ARIA Shop
// Хранит токен, роль и email в localStorage

window.ariaAuth = {
    getToken: function() {
        return localStorage.getItem('aria_token');
    },

    setToken: function(token) {
        localStorage.setItem('aria_token', token);
    },

    getRole: function() {
        return localStorage.getItem('aria_role');
    },

    setRole: function(role) {
        localStorage.setItem('aria_role', role);
    },

    getUserEmail: function() {
        return localStorage.getItem('aria_email');
    },

    setUserEmail: function(email) {
        localStorage.setItem('aria_email', email);
    },

    logout: function() {
        localStorage.removeItem('aria_token');
        localStorage.removeItem('aria_role');
        localStorage.removeItem('aria_email');
        location.href = '/login.html';
    },

    isLoggedIn: function() {
        return !!this.getToken();
    },

    authFetch: async function(url, options = {}) {
        const token = this.getToken();
        const mergedOptions = {
            ...options,
            headers: {
                ...options.headers
            }
        };
        if (token) {
            mergedOptions.headers['Authorization'] = 'Bearer ' + token;
        }
        const response = await fetch(url, mergedOptions);
        if (response.status === 401) {
            this.logout();
        }
        return response;
    },

    requireLogin: function(redirect = true) {
        if (!this.isLoggedIn()) {
            if (redirect) {
                location.href = '/login.html?next=' + encodeURIComponent(location.pathname + location.search);
            }
            return false;
        }
        return true;
    },

    requireAdmin: function(redirect = true) {
        if (this.getRole() !== 'ADMIN') {
            if (typeof showToast === 'function') {
                showToast('Нужна роль администратора', 'error');
            } else {
                alert('Нужна роль администратора');
            }
            return false;
        }
        return true;
    }
};
