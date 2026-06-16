// ==========================================
// VAULT.JS - Password Management Functions
// ==========================================

let editingPasswordId = null;

// ── Toast ─────────────────────────────────────────────────────────────────────
// FIX: Old version used raw cssText, bypassing the .show/.success/.error CSS classes.
// Now uses the class system so animations work correctly.
function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    if (!toast) return;
    toast.textContent = message;
    toast.className = '';
    void toast.offsetWidth;            // Force reflow so the slide-in animation restarts
    toast.className = `show ${type}`;
    setTimeout(() => { toast.className = ''; }, 3500);
}

// ── XSS Protection ────────────────────────────────────────────────────────────
// FIX: User content (platform, username, password) was injected raw into innerHTML.
function escapeHtml(text) {
    if (!text) return '';
    return String(text).replace(/[&<>"']/g, m => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;'
    }[m]));
}

// ── Password Field Accessor ────────────────────────────────────────────────────
// The service may decrypt before returning; try all likely field names.
function getPasswordValue(pwd) {
    return pwd.password || pwd.decryptedPassword || pwd.encryptedPassword || '';
}

// ── Platform Icons ────────────────────────────────────────────────────────────
function getPlatformIcon(name) {
    const map = {
        google: '🔵', gmail: '📧', facebook: '📘', twitter: '🐦', x: '𝕏',
        instagram: '📸', netflix: '🎬', amazon: '📦', github: '🐙',
        linkedin: '💼', apple: '🍎', microsoft: '🪟', discord: '💬',
        spotify: '🎵', youtube: '▶️', reddit: '🤖', twitch: '🎮',
        paypal: '💳', dropbox: '📂', slack: '💬', notion: '📝', steam: '🎮',
    };
    const key = (name || '').toLowerCase();
    const hit = Object.keys(map).find(k => key.includes(k));
    return hit ? map[hit] : '🌐';
}

// ── Load Passwords ────────────────────────────────────────────────────────────
async function loadPasswords() {
    const vaultList = document.getElementById('vaultList');
    if (vaultList) {
        vaultList.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon" style="font-size:40px;opacity:.6;">⏳</div>
                <p>Loading your vault...</p>
            </div>`;
    }

    try {
        const keyword   = document.getElementById('searchInput')?.value?.trim() || '';
        const passwords = keyword
            ? await api.searchPasswords(keyword)
            : await api.getAllPasswords();
        displayPasswords(passwords);
    } catch (error) {
        console.error('Load error:', error);
        // Auto-logout on expired session
        if (error.message?.includes('401') || error.message?.toLowerCase().includes('unauthorized')) {
            showToast('Session expired — logging you out.', 'error');
            setTimeout(() => { api.logout(); window.location.href = 'login.html'; }, 2000);
            return;
        }
        if (vaultList) {
            vaultList.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">⚠️</div>
                    <p>Could not load passwords.</p>
                    <p style="font-size:13px;color:#888;">Is the server running at localhost:8080?</p>
                </div>`;
        }
        showToast('❌ ' + error.message, 'error');
    }
}

// ── Display Passwords ─────────────────────────────────────────────────────────
// FIX: Now uses the CSS classes defined in style.css that were never being used:
//      .password-card-header, .password-card-details, .password-card-actions
// FIX: Added Edit button (was missing — users could never edit a saved password)
// FIX: All user content is escaped to prevent XSS
function displayPasswords(passwords) {
    const vaultList  = document.getElementById('vaultList');
    const countBadge = document.getElementById('passwordCount');
    if (!vaultList) return;
    window.currentPasswords = passwords;

    if (!Array.isArray(passwords) || passwords.length === 0) {
        if (countBadge) countBadge.textContent = '0 passwords';
        vaultList.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">📭</div>
                <p>No passwords in your vault.</p>
                <p style="font-size:14px;color:#888;">Add your first password using the form above!</p>
            </div>`;
        return;
    }

    if (countBadge) {
        countBadge.textContent = `${passwords.length} password${passwords.length !== 1 ? 's' : ''}`;
    }

    vaultList.innerHTML = passwords.map((pwd, i) => {
        const platform = escapeHtml(pwd.platform || pwd.website || 'Unknown');
        const username = escapeHtml(pwd.loginUsername || pwd.username || '—');
        const password = escapeHtml(getPasswordValue(pwd));
        const icon     = getPlatformIcon(platform);

        return `
        <div class="password-card ${pwd.isBreached ? 'breached' : ''}">
            <div class="password-card-header">
                <div>
                    <h4>${icon} ${platform}</h4>
                    ${pwd.isBreached
                        ? '<span class="breach-warning">⚠️ BREACHED — Change this password immediately!</span>'
                        : ''}
                </div>
                <div class="password-card-actions" style="margin-top:0;">
                    <button onclick="editPassword(${i})"   class="warning small">✏️ Edit</button>
                    <button onclick="copyPassword(${i})"   class="small" style="background:linear-gradient(135deg,#2196F3,#1565C0);">📋 Copy</button>
                    <button onclick="deletePassword(${i})" class="danger small">🗑️ Delete</button>
                </div>
            </div>
            <div class="password-card-details">
                <p><strong>Username</strong> ${username}</p>
                <p>
                    <strong>Password</strong>
                    <span id="pwd-${i}" class="password-text" style="display:none;">${password}</span>
                    <span id="mask-${i}">••••••••</span>
                    <button onclick="togglePassword(${i})" class="toggle-password-btn" title="Show / Hide">👁️</button>
                </p>
            </div>
        </div>`;
    }).join('');
}

// ── Toggle Password Visibility ────────────────────────────────────────────────
function togglePassword(i) {
    const pw   = document.getElementById(`pwd-${i}`);
    const mask = document.getElementById(`mask-${i}`);
    if (!pw || !mask) return;
    const hidden = mask.style.display !== 'none';
    mask.style.display = hidden ? 'none'   : 'inline';
    pw.style.display   = hidden ? 'inline' : 'none';
}

// ── Copy Password ─────────────────────────────────────────────────────────────
function copyPassword(i) {
    const pwd = window.currentPasswords?.[i];
    if (!pwd) { showToast('❌ Not found', 'error'); return; }
    const value = getPasswordValue(pwd);
    if (!value) { showToast('❌ Password is empty', 'error'); return; }

    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(value)
            .then(() => showToast('📋 Password copied!', 'success'))
            .catch(() => _fallbackCopy(value));
    } else {
        _fallbackCopy(value);
    }
}

function _fallbackCopy(text) {
    const el = document.createElement('textarea');
    el.value = text;
    el.style.cssText = 'position:fixed;opacity:0;';
    document.body.appendChild(el);
    el.select();
    try { document.execCommand('copy'); showToast('📋 Password copied!', 'success'); }
    catch { showToast('❌ Copy failed — please copy manually.', 'error'); }
    document.body.removeChild(el);
}

// ── Delete Password ───────────────────────────────────────────────────────────
async function deletePassword(i) {
    const pwd = window.currentPasswords?.[i];
    if (!pwd) { showToast('❌ Not found', 'error'); return; }

    const name = pwd.platform || pwd.website || 'this password';
    if (!confirm(`Delete "${name}"?\nThis cannot be undone.`)) return;

    try {
        await api.deletePassword(pwd.id);
        showToast(`🗑️ "${name}" deleted.`, 'success');
        loadPasswords();
    } catch (error) {
        showToast('❌ ' + error.message, 'error');
    }
}

// ── Edit Password ─────────────────────────────────────────────────────────────
// FIX: editPassword() was missing entirely — users had no way to edit entries.
function editPassword(i) {
    const pwd = window.currentPasswords?.[i];
    if (!pwd) return;

    document.getElementById('newPlatform').value = pwd.platform  || pwd.website       || '';
    document.getElementById('newUsername').value = pwd.loginUsername || pwd.username  || '';
    document.getElementById('newPassword').value = getPasswordValue(pwd);
    updatePasswordStrength(getPasswordValue(pwd));

    editingPasswordId = pwd.id;

    const saveBtn = document.getElementById('saveBtn');
    if (saveBtn) {
        saveBtn.textContent = '✏️ Update Password';
        saveBtn.style.background  = 'linear-gradient(135deg, #ff9800, #e65100)';
        saveBtn.style.boxShadow   = '0 4px 15px rgba(255,152,0,0.4)';
    }

    const titleEl = document.getElementById('formTitle');
    if (titleEl) titleEl.textContent = '✏️ Edit Password';

    const cancelBtn = document.getElementById('cancelEditBtn');
    if (cancelBtn) cancelBtn.style.display = 'inline-block';

    document.querySelector('.add-password-form')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

// ── Cancel Edit ───────────────────────────────────────────────────────────────
// FIX: cancelEdit() was also missing — once in edit mode, users were stuck.
function cancelEdit() {
    editingPasswordId = null;

    ['newPlatform', 'newUsername', 'newPassword'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = '';
    });
    updatePasswordStrength('');

    const saveBtn = document.getElementById('saveBtn');
    if (saveBtn) {
        saveBtn.textContent    = '💾 Save to Vault';
        saveBtn.style.background  = '';
        saveBtn.style.boxShadow   = '';
    }

    const titleEl = document.getElementById('formTitle');
    if (titleEl) titleEl.textContent = '➕ Add Password';

    const cancelBtn = document.getElementById('cancelEditBtn');
    if (cancelBtn) cancelBtn.style.display = 'none';
}

// ── Save / Update Password ────────────────────────────────────────────────────
async function saveNewPassword() {
    const platform      = document.getElementById('newPlatform')?.value.trim();
    const loginUsername = document.getElementById('newUsername')?.value.trim();
    const password      = document.getElementById('newPassword')?.value.trim();

    if (!platform || !loginUsername || !password) {
        showToast('⚠️ Please fill in all fields.', 'error');
        return;
    }

    const isEditing = !!editingPasswordId;
    const saveBtn   = document.getElementById('saveBtn');
    if (saveBtn) { saveBtn.textContent = '⏳ Saving...'; saveBtn.disabled = true; }

    try {
        if (isEditing) {
            await api.updatePassword(editingPasswordId, platform, loginUsername, password);
            showToast(`✅ "${platform}" updated!`, 'success');
            cancelEdit();                            // resets button + editingPasswordId
        } else {
            await api.savePassword(platform, loginUsername, password);
            showToast(`✅ "${platform}" saved to vault!`, 'success');
            ['newPlatform', 'newUsername', 'newPassword'].forEach(id => {
                const el = document.getElementById(id);
                if (el) el.value = '';
            });
            updatePasswordStrength('');
            if (saveBtn) saveBtn.textContent = '💾 Save to Vault';
        }
        loadPasswords();
    } catch (error) {
        console.error('Save error:', error);
        showToast('❌ ' + error.message, 'error');
        if (saveBtn) saveBtn.textContent = isEditing ? '✏️ Update Password' : '💾 Save to Vault';
    } finally {
        if (saveBtn) saveBtn.disabled = false;
    }
}

// ── Generate Password ─────────────────────────────────────────────────────────
async function generateRandom() {
    try {
        const result    = await api.generatePassword(16, true);
        const generated = result.password || result;
        document.getElementById('newPassword').value = generated;
        updatePasswordStrength(generated);
        showToast('🎲 Secure password generated!', 'success');
    } catch {
        // Fallback: generate locally using crypto API
        const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+[]{}|;:,.<>?';
        const arr   = new Uint32Array(16);
        crypto.getRandomValues(arr);
        const pwd = Array.from(arr).map(v => chars[v % chars.length]).join('');
        document.getElementById('newPassword').value = pwd;
        updatePasswordStrength(pwd);
        showToast('🎲 Password generated (offline)!', 'success');
    }
}

// ── Scan Breaches ─────────────────────────────────────────────────────────────
async function scanBreaches() {
    const btn = document.getElementById('scanBtn');
    if (btn) { btn.textContent = '⏳ Scanning...'; btn.disabled = true; }

    try {
        const result = await api.scanForBreaches();
        const count  = result.breachedCount    || 0;
        const total  = result.totalPasswords   || window.currentPasswords?.length || 0;
        showToast(
            count > 0
                ? `⚠️ ${count} of ${total} password${count !== 1 ? 's are' : ' is'} breached — update them now!`
                : `✅ All ${total} passwords are safe!`,
            count > 0 ? 'error' : 'success'
        );
        loadPasswords();
    } catch (error) {
        showToast('❌ Scan failed: ' + error.message, 'error');
    } finally {
        if (btn) { btn.textContent = '🛡️ Scan Breaches'; btn.disabled = false; }
    }
}

// ── Export to CSV ─────────────────────────────────────────────────────────────
// FIX: exportToCSV() was missing. Also fixed: old version used data: URI (blocked
//      by modern browsers). Now uses Blob + URL.createObjectURL().
function exportToCSV() {
    if (!window.currentPasswords?.length) {
        showToast('⚠️ Vault is empty — nothing to export.', 'error');
        return;
    }
    const q    = s => `"${String(s || '').replace(/"/g, '""')}"`;
    const rows = [
        ['Platform', 'Username', 'Password', 'Breached'],
        ...window.currentPasswords.map(p => [
            q(p.platform || ''),
            q(p.loginUsername || ''),
            q(getPasswordValue(p)),
            p.isBreached ? 'Yes' : 'No'
        ])
    ].map(r => r.join(',')).join('\n');

    const blob = new Blob([rows], { type: 'text/csv;charset=utf-8;' });
    const url  = URL.createObjectURL(blob);
    const date = new Date().toISOString().split('T')[0];
    const a    = Object.assign(document.createElement('a'), { href: url, download: `vault_export_${date}.csv` });
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    showToast('📤 Vault exported!', 'success');
}

// ── Password Strength Meter ───────────────────────────────────────────────────
function updatePasswordStrength(password) {
    const bar  = document.getElementById('strengthBar');
    const text = document.getElementById('strengthText');
    if (!bar || !text) return;

    if (!password) {
        bar.style.width   = '0%';
        bar.style.background = 'transparent';
        text.textContent  = '';
        return;
    }

    let score = 0;
    if (password.length >= 8)            score++;
    if (password.length >= 12)           score++;
    if (password.length >= 16)           score++;
    if (/[A-Z]/.test(password))          score++;
    if (/[a-z]/.test(password))          score++;
    if (/[0-9]/.test(password))          score++;
    if (/[^A-Za-z0-9]/.test(password))  score++;

    const levels = [
        { pct: '14%',  color: '#ff1744', label: '🔴 Very Weak'   },
        { pct: '28%',  color: '#ff5252', label: '🔴 Weak'        },
        { pct: '42%',  color: '#ff9800', label: '🟠 Fair'        },
        { pct: '57%',  color: '#ffeb3b', label: '🟡 Moderate'    },
        { pct: '71%',  color: '#8bc34a', label: '🟢 Strong'      },
        { pct: '85%',  color: '#4CAF50', label: '🟢 Very Strong' },
        { pct: '100%', color: '#00e676', label: '✅ Excellent'   },
    ];
    const lvl = levels[Math.min(score, 6)];
    bar.style.width       = lvl.pct;
    bar.style.background  = lvl.color;
    text.textContent      = lvl.label;
    text.style.color      = lvl.color;
}

// ── Theme ─────────────────────────────────────────────────────────────────────
// FIX: toggleTheme() was missing. Theme was also lost on every page reload.
//      Now persisted to localStorage and restored on load.
function toggleTheme() {
    const isLight = document.body.classList.contains('light-mode');
    document.body.classList.toggle('dark-mode',  isLight);
    document.body.classList.toggle('light-mode', !isLight);
    const btn = document.getElementById('themeBtn');
    if (btn) btn.textContent = isLight ? '☀️ Light Mode' : '🌙 Dark Mode';
    localStorage.setItem('theme', isLight ? 'dark' : 'light');
}

// ── Logout ────────────────────────────────────────────────────────────────────
function logout() {
    if (confirm('Log out from your vault?')) {
        api.logout();
        window.location.href = 'login.html';
    }
}

// ── Init ──────────────────────────────────────────────────────────────────────
// FIX: DOMContentLoaded was truncated mid-sentence — auth guard and loadPasswords()
//      call were never executing.
document.addEventListener('DOMContentLoaded', function () {
    if (!api.isLoggedIn()) {
        alert('Please log in to access your vault.');
        window.location.href = 'login.html';
        return;
    }

    // Restore saved theme preference
    const saved = localStorage.getItem('theme') || 'dark';
    document.body.className = saved + '-mode';
    const themeBtn = document.getElementById('themeBtn');
    if (themeBtn) themeBtn.textContent = saved === 'dark' ? '☀️ Light Mode' : '🌙 Dark Mode';

    // Search on Enter key
    document.getElementById('searchInput')?.addEventListener('keypress', e => {
        if (e.key === 'Enter') loadPasswords();
    });

    // Live password strength meter
    document.getElementById('newPassword')?.addEventListener('input', function () {
        updatePasswordStrength(this.value);
    });

    loadPasswords();
});