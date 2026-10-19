function updateAuthNav() {
        const authNav = document.getElementById('auth-nav');
        if (ariaAuth.isLoggedIn()) {
            authNav.innerHTML = '<span style="font-size:14px;color:var(--text-muted);margin-right:15px;">' + ariaAuth.getUserEmail() + '</span>' +
                '<button onclick="ariaAuth.logout()" style="background:transparent;border:1px solid var(--border-color);color:var(--text-primary);padding:6px 12px;border-radius:6px;cursor:pointer;font-size:13px;">Выйти</button>';
        } else {
            authNav.innerHTML = '<a href="/login.html" style="color:var(--accent-blue);text-decoration:none;font-size:14px;">Войти</a>';
        }
    }
    updateAuthNav();
