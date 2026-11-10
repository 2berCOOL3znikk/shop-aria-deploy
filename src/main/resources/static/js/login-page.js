let buyerRegistrationEnabled = false;
ariaCopy.capabilities.then(value => {
    buyerRegistrationEnabled = value?.buyerRegistration === true;
    document.getElementById('register-tab').hidden = !buyerRegistrationEnabled;
    document.getElementById('company-login-note').hidden = value?.publicStorefront !== false;
});

    function switchTab(tab) {
        if (tab === 'register' && !buyerRegistrationEnabled) return;
        document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
        document.querySelectorAll('.auth-form').forEach(form => form.classList.remove('active'));

        if (tab === 'login') {
            document.querySelector('.tab-btn:first-child').classList.add('active');
            document.getElementById('login-form').classList.add('active');
        } else {
            document.querySelector('.tab-btn:last-child').classList.add('active');
            document.getElementById('register-form').classList.add('active');
            window.loadRegisterCaptcha();
        }
    }

    window.loadRegisterCaptcha = async function () {
        const question = document.getElementById('captcha-question');
        const id = document.getElementById('captcha-id');
        const answer = document.getElementById('captcha-answer');
        ariaCopy.setProperty(question,"textContent",ariaCopy.text("text.cb610780272683"));
        id.value = '';
        answer.value = '';
        try {
            const response = await fetch('/api/auth/captcha', { cache: 'no-store' });
            if (!response.ok) throw new Error(ariaCopy.errorText(ariaCopy.text("text.45b2a923235a4a")));
            const challenge = await response.json();
            id.value = challenge.id || '';
            ariaCopy.setProperty(question,"textContent",ariaCopy.captcha(challenge.question) || ariaCopy.text("text.f94111050c2e5e"));
            if (!id.value) throw new Error(ariaCopy.errorText(ariaCopy.text("text.07bf6dc9226e50")));
        } catch (error) {
            ariaCopy.setProperty(question,"textContent",error.message || ariaCopy.text("text.75625f592f58e0"));
        }
    };

    async function handleLogin(event) {
        event.preventDefault();
        const email = document.getElementById('login-email').value;
        const password = document.getElementById('login-password').value;
        const errorDiv = document.getElementById('login-error');

        try {
            const res = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });

            if (res.ok) {
                const data = await res.json();
                localStorage.setItem('aria_token', data.token);
                localStorage.setItem('aria_role', data.role);
                localStorage.setItem('aria_email', data.email);

                const next = localStorage.getItem('aria_next');
                localStorage.removeItem('aria_next');
                location.href = safeNext(next, data.role);
            } else if (res.status === 401) {
                ariaCopy.setProperty(errorDiv,"textContent",ariaCopy.text("text.f0591450e0ab0b"));
                errorDiv.classList.add('show');
            } else {
                const err = await res.json();
                ariaCopy.setProperty(errorDiv,"textContent",ariaCopy.errorText(err.error || err.message || ariaCopy.text("text.4ebf918cfa41d6")));
                errorDiv.classList.add('show');
            }
        } catch (e) {
            ariaCopy.setProperty(errorDiv,"textContent",ariaCopy.text("text.bd5e65f23a7bdb"));
            errorDiv.classList.add('show');
        }
    }

    async function handleRegister(event) {
        event.preventDefault();
        const fullName = document.getElementById('register-fullName').value;
        const email = document.getElementById('register-email').value;
        const password = document.getElementById('register-password').value;
        const errorDiv = document.getElementById('register-error');
        const captchaId = document.getElementById('captcha-id').value.trim();
        const captchaAnswer = document.getElementById('captcha-answer').value.trim();
        const website = document.querySelector('#register-form [name="website"]').value.trim();

        if (!captchaId || !captchaAnswer) {
            ariaCopy.setProperty(errorDiv,"textContent",ariaCopy.text("text.d886fdc7d86a45"));
            errorDiv.classList.add('show');
            if (!captchaId) window.loadRegisterCaptcha();
            return;
        }
        errorDiv.classList.remove('show');

        try {
            const res = await fetch('/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password, fullName, website, captchaId, captchaAnswer })
            });

            if (res.ok) {
                const data = await res.json();
                localStorage.setItem('aria_token', data.token);
                localStorage.setItem('aria_role', data.role);
                localStorage.setItem('aria_email', data.email);

                const next = localStorage.getItem('aria_next');
                localStorage.removeItem('aria_next');
                location.href = safeNext(next, data.role);
            } else {
                const err = await res.json().catch(() => ({}));
                ariaCopy.setProperty(errorDiv,"textContent",ariaCopy.errorText(err.error || err.message || ariaCopy.text("text.4ff5d76ef0914a")));
                errorDiv.classList.add('show');
                window.loadRegisterCaptcha();
            }
        } catch (e) {
            ariaCopy.setProperty(errorDiv,"textContent",ariaCopy.text("text.bd5e65f23a7bdb"));
            errorDiv.classList.add('show');
            window.loadRegisterCaptcha();
        }
    }

    function safeNext(next, role) {
        const fallback=role==='ADMIN'?'/control.html':role==='ORG'?'/social.html':'/index.html';
        if(typeof next!=='string'||!next.startsWith('/')||next.startsWith('//')||/[\\\u0000-\u001f\u007f]/.test(next))return fallback;
        let target;try{target=new URL(next,location.origin);}catch{return fallback;}
        if(target.origin!==location.origin)return fallback;
        if(role==='ORG')return target.pathname==='/social.html'?'/social.html':fallback;
        if(role!=='ADMIN'&&target.pathname.startsWith('/admin'))return fallback;
        return target.pathname+target.search+target.hash;
    }

    // Сохраняем next-параметр при загрузке
    const params = new URLSearchParams(location.search);
    if (params.get('next')) {
        localStorage.setItem('aria_next', params.get('next'));
    }
    if (localStorage.getItem('aria_role') === 'ADMIN') location.replace('/control.html');
    else if (localStorage.getItem('aria_role') === 'ORG') location.replace('/social.html');
    window.loadRegisterCaptcha();
