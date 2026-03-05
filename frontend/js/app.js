// UI Logic for index.html Auth Modals

function showLogin() {
    document.getElementById('auth-modal').classList.add('active');
    document.getElementById('login-form').classList.add('active');
    document.getElementById('register-form').classList.remove('active');
    document.getElementById('register-form').style.display = 'none';
    document.getElementById('login-form').style.display = 'block';
}

function showRegister(defaultRole = null) {
    document.getElementById('auth-modal').classList.add('active');
    document.getElementById('register-form').classList.add('active');
    document.getElementById('login-form').classList.remove('active');
    document.getElementById('login-form').style.display = 'none';
    document.getElementById('register-form').style.display = 'block';

    if (defaultRole) {
        document.getElementById('reg-role').value = defaultRole;
        toggleRecruiterFields(defaultRole);
    }
}

function toggleRecruiterFields(role) {
    const fields = document.getElementById('recruiter-fields');
    const inputs = fields.querySelectorAll('input');
    if (role === 'ROLE_RECRUITER') {
        fields.style.display = 'block';
        inputs.forEach(input => input.required = true);
    } else {
        fields.style.display = 'none';
        inputs.forEach(input => input.required = false);
    }
}

function closeModal() {
    document.getElementById('auth-modal').classList.remove('active');
}

async function handleLogin(e) {
    e.preventDefault();
    const btn = e.target.querySelector('button');
    const originalText = btn.innerText;
    btn.innerText = 'Signing in...';
    btn.disabled = true;

    try {
        const email = document.getElementById('login-email').value;
        const password = document.getElementById('login-password').value;

        const res = await api.auth.login({ email, password });

        localStorage.setItem('token', res.token);
        localStorage.setItem('role', res.role);
        localStorage.setItem('name', res.name);

        showToast('Login successful!', 'success');
        setTimeout(() => redirectBasedOnRole(res.role), 1000);

    } catch (err) {
        showToast(err.message || 'Login failed', 'error');
        btn.innerText = originalText;
        btn.disabled = false;
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const btn = e.target.querySelector('button');
    const originalText = btn.innerText;
    btn.innerText = 'Creating account...';
    btn.disabled = true;

    try {
        const name = document.getElementById('reg-name').value;
        const email = document.getElementById('reg-email').value;

        if (!email.toLowerCase().endsWith('@gmail.com')) {
            showToast('Only @gmail.com addresses are allowed!', 'error');
            btn.innerText = originalText;
            btn.disabled = false;
            return;
        }

        const password = document.getElementById('reg-password').value;
        const role = document.getElementById('reg-role').value;

        const userData = { name, email, password, role };

        if (role === 'ROLE_RECRUITER') {
            userData.companyName = document.getElementById('reg-company-name').value;
            userData.companyEmail = document.getElementById('reg-company-email').value;
            userData.companyWebsite = document.getElementById('reg-company-website').value;
            userData.companyRegistrationId = document.getElementById('reg-company-id').value;
        }

        await api.auth.register(userData);

        showToast('Registration successful! Please log in.', 'success');
        setTimeout(() => showLogin(), 1500);

    } catch (err) {
        showToast(err.message || 'Registration failed', 'error');
    } finally {
        btn.innerText = originalText;
        btn.disabled = false;
    }
}
