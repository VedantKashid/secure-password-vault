// ==========================================
// AUTH.JS - Login and Registration Logic
// ==========================================

// ── Auto-redirect already-logged-in users ─────────────────────────────────────
document.addEventListener('DOMContentLoaded', function () {
    setTimeout(() => {
        if (api && api.isLoggedIn()) {
            window.location.href = 'vault.html';
        }
    }, 100); // tiny delay so api.js constructor runs first
});

// ── Shared helpers ────────────────────────────────────────────────────────────

function showError(message) {
    const el = document.getElementById('errorMsg');
    if (!el) return;
    el.textContent = message;
    el.classList.add('show');
}

function clearError() {
    const el = document.getElementById('errorMsg');
    if (!el) return;
    el.textContent = '';
    el.classList.remove('show');
}

// ── LOGIN ─────────────────────────────────────────────────────────────────────

async function executeLogin() {
    const username = document.getElementById('username')?.value.trim();
    const password = document.getElementById('password')?.value.trim();
    const btn      = document.getElementById('loginBtn');

    clearError();

    if (!username || !password) {
        showError('⚠️ Please enter both username and password.');
        return;
    }

    if (btn) { btn.textContent = '⏳ Logging in...'; btn.disabled = true; }

    try {
        const response = await api.login(username, password);

        if (response && response.success) {
            // Brief success feedback before redirect
            const toast = document.getElementById('toast');
            if (toast) { toast.textContent = '✅ Login successful!'; toast.className = 'show success'; }
            setTimeout(() => { window.location.href = 'vault.html'; }, 800);
        } else {
            throw new Error('Invalid response from server');
        }
    } catch (error) {
        let msg = error.message || 'Login failed.';
        if (msg.toLowerCase().includes('invalid') || msg.toLowerCase().includes('bad credentials')) {
            msg = 'Invalid username or password.';
        } else if (msg.toLowerCase().includes('connect') || msg.toLowerCase().includes('fetch')) {
            msg = 'Cannot reach server — is the backend running?';
        }
        showError('❌ ' + msg);
        if (btn) { btn.textContent = '🚀 Login to Vault'; btn.disabled = false; }
    }
}

// ── REGISTER ─────────────────────────────────────────────────────────────────
// FIX: executeRegister() was completely missing — the register button did nothing.

async function executeRegister() {
    const username        = document.getElementById('regUsername')?.value.trim();
    const email           = document.getElementById('regEmail')?.value.trim();
    const password        = document.getElementById('regPassword')?.value.trim();
    const confirmPassword = document.getElementById('regConfirmPassword')?.value.trim();
    const btn             = document.getElementById('registerBtn');

    clearError();

    if (!username || !email || !password || !confirmPassword) {
        showError('⚠️ Please fill in all fields.');
        return;
    }

    // Email format validation
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        showError('⚠️ Please enter a valid email address.');
        return;
    }

    if (password.length < 8) {
        showError('⚠️ Password must be at least 8 characters.');
        return;
    }

    // Confirm password check
    if (password !== confirmPassword) {
        showError('⚠️ Passwords do not match.');
        return;
    }

    if (btn) { btn.textContent = '⏳ Creating account...'; btn.disabled = true; }

    try {
        await api.register(username, password, email);
        alert('✅ Account created! Redirecting to login...');
        window.location.href = 'login.html';
    } catch (error) {
        showError('❌ ' + (error.message || 'Registration failed.'));
        if (btn) { btn.textContent = '🚀 Create Secure Account'; btn.disabled = false; }
    }
}

// ── UX: clear error on keypress, submit on Enter ─────────────────────────────

document.addEventListener('DOMContentLoaded', function () {
    // Clear error banner when user starts typing in any input
    document.querySelectorAll('input').forEach(input => {
        input.addEventListener('input', clearError);
    });

    // Enter key submits whichever form is on this page
    document.addEventListener('keypress', function (e) {
        if (e.key !== 'Enter') return;
        if (document.getElementById('username'))    executeLogin();
        if (document.getElementById('regUsername')) executeRegister();
    });
});