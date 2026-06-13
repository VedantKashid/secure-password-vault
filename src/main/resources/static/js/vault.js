// ==========================================
// VAULT.JS - Password Management Functions
// ==========================================

let editingPasswordId = null;

function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: ${type === 'success' ? '#4CAF50' : '#ff5252'};
        color: white;
        padding: 15px 20px;
        border-radius: 5px;
        z-index: 1000;
        display: block;
    `;
    setTimeout(() => {
        toast.style.display = 'none';
    }, 3000);
}

// Load and display all passwords
async function loadPasswords() {
    try {
        const keyword = document.getElementById('searchInput').value;
        let passwords;

        if (keyword.trim()) {
            passwords = await api.searchPasswords(keyword);
        } else {
            passwords = await api.getAllPasswords();
        }

        displayPasswords(passwords);
    } catch (error) {
        console.error('Error loading passwords:', error);
        showToast('Error loading passwords: ' + error.message, 'error');
    }
}

// Display passwords in the vault
function displayPasswords(passwords) {
    const vaultList = document.getElementById('vaultList');

    if (!Array.isArray(passwords) || passwords.length === 0) {
        vaultList.innerHTML = '<p>No passwords found in your vault.</p>';
        return;
    }

    let html = '';
    passwords.forEach((pwd, index) => {
        const isBreached = pwd.isBreached ? 'breached' : '';
        const breachLabel = pwd.isBreached ? '<span style="color: #ff5252; font-weight: bold; font-size: 12px;">⚠️ BREACHED</span>' : '';

        html += `
            <div class="password-card ${isBreached}" style="padding: 15px; margin: 10px 0; border-radius: 5px; text-align: left;">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <div>
                        <h4 style="margin: 0 0 5px 0; color: #4CAF50;">${pwd.platform || pwd.website}</h4>
                        <p style="margin: 5px 0; color: #aaa;">Username: ${pwd.loginUsername || pwd.username}</p>
                        <p style="margin: 5px 0; color: #aaa;">Password:
                            <span id="pwd-${index}" style="display: none;">${pwd.encryptedPassword}</span>
                            <span id="mask-${index}">••••••••</span>
                            <button onclick="togglePassword(${index})" style="background: none; border: none; color: #2196F3; cursor: pointer; padding: 0;">👁️</button>
                        </p>
                        ${breachLabel}
                    </div>
                    <div style="display: flex; gap: 10px;">
                        <button onclick="editPassword(${index})" style="width: auto; padding: 5px 10px; background: #ff9800;">✏️ Edit</button>
                        <button onclick="copyPassword(${index})" style="width: auto; padding: 5px 10px; background: #2196F3;">📋 Copy</button>
                        <button onclick="deletePassword(${index})" style="width: auto; padding: 5px 10px; background: #ff5252;">🗑️ Delete</button>
                    </div>
                </div>
            </div>
        `;
    });

    vaultList.innerHTML = html;
    window.currentPasswords = passwords;
}

function togglePassword(index) {
    const pwdSpan = document.getElementById(`pwd-${index}`);
    const maskSpan = document.getElementById(`mask-${index}`);

    if (maskSpan.style.display === 'none') {
        maskSpan.style.display = 'inline';
        pwdSpan.style.display = 'none';
    } else {
        maskSpan.style.display = 'none';
        pwdSpan.style.display = 'inline';
    }
}

function copyPassword(index) {
    const password = window.currentPasswords[index].encryptedPassword;
    navigator.clipboard.writeText(password).then(() => {
        showToast('Password copied to clipboard!', 'success');
    }).catch(() => {
        showToast('Failed to copy password', 'error');
    });
}

async function deletePassword(index) {
    if (!confirm('Are you sure you want to delete this password?')) {
        return;
    }
    try {
        const passwordId = window.currentPasswords[index].id;
        await api.deletePassword(passwordId);
        showToast('Password deleted successfully!', 'success');
        loadPasswords();
    } catch (error) {
        showToast('Error deleting password: ' + error.message, 'error');
    }
}

function editPassword(index) {
    const pwd = window.currentPasswords[index];
    document.getElementById('newPlatform').value = pwd.platform || pwd.website;
    document.getElementById('newUsername').value = pwd.loginUsername || pwd.username;
    document.getElementById('newPassword').value = pwd.encryptedPassword;

    editingPasswordId = pwd.id;
    const saveBtn = document.getElementById('saveBtn');
    saveBtn.innerHTML = '🔄 Update Password';
    saveBtn.style.backgroundColor = '#ff9800';
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

async function saveNewPassword() {
    try {
        const platform = document.getElementById('newPlatform').value.trim();
        const loginUsername = document.getElementById('newUsername').value.trim();
        const password = document.getElementById('newPassword').value.trim();

        if (!platform || !loginUsername || !password) {
            showToast('Please fill in all fields', 'error');
            return;
        }

        const saveBtn = document.getElementById('saveBtn');
        const originalText = saveBtn.innerHTML;
        saveBtn.innerHTML = '⏳ Processing...';
        saveBtn.disabled = true;

        if (editingPasswordId) {
            await api.updatePassword(editingPasswordId, platform, loginUsername, password);
            showToast('Password updated successfully!', 'success');
            editingPasswordId = null;
            saveBtn.innerHTML = '💾 Save to Vault';
            saveBtn.style.backgroundColor = '#4CAF50';
        } else {
            await api.savePassword(platform, loginUsername, password);
            showToast('Password saved successfully!', 'success');
            saveBtn.innerHTML = originalText;
        }

        saveBtn.disabled = false;
        document.getElementById('newPlatform').value = '';
        document.getElementById('newUsername').value = '';
        document.getElementById('newPassword').value = '';
        loadPasswords();
    } catch (error) {
        showToast('Error: ' + error.message, 'error');
        const saveBtn = document.getElementById('saveBtn');
        saveBtn.innerHTML = editingPasswordId ? '🔄 Update Password' : '💾 Save to Vault';
        saveBtn.disabled = false;
    }
}

async function generateRandom() {
    try {
        const result = await api.generatePassword(16, true);
        const generatedPassword = result.password || result;
        document.getElementById('newPassword').value = generatedPassword;
        showToast('Password generated!', 'success');
    } catch (error) {
        showToast('Error generating password: ' + error.message, 'error');
    }
}

async function scanBreaches() {
    try {
        showToast('Scanning for breaches...', 'success');
        const result = await api.scanForBreaches();
        showToast(`Breach scan complete! ${result.breachedCount || 0} breached password(s) found.`, 'success');
        loadPasswords();
    } catch (error) {
        showToast('Error scanning breaches: ' + error.message, 'error');
    }
}

// 👇 NEW: Export Vault to CSV locally in the browser 👇
function exportToCSV() {
    if (!window.currentPasswords || window.currentPasswords.length === 0) {
        showToast('Your vault is empty!', 'error');
        return;
    }

    // Create the CSV Header
    let csvContent = "data:text/csv;charset=utf-8,";
    csvContent += "Platform,Username,Password,Breached\n";

    // Loop through memory and build rows
    window.currentPasswords.forEach(pwd => {
        const platform = `"${(pwd.platform || '').replace(/"/g, '""')}"`;
        const username = `"${(pwd.loginUsername || '').replace(/"/g, '""')}"`;
        const password = `"${(pwd.encryptedPassword || '').replace(/"/g, '""')}"`;
        const breached = pwd.isBreached ? "Yes" : "No";

        csvContent += `${platform},${username},${password},${breached}\n`;
    });

    // Create a hidden link and trigger the download
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", "secure_vault_export.csv");
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    showToast('Vault exported successfully!', 'success');
}

// 👇 NEW: Toggle Light/Dark Mode 👇
function toggleTheme() {
    const body = document.body;
    const themeBtn = document.getElementById('themeBtn');

    if (body.classList.contains('light-theme')) {
        body.classList.remove('light-theme');
        body.classList.add('dark-theme');
        themeBtn.innerHTML = '☀️ Switch to Light Mode';
    } else {
        body.classList.remove('dark-theme');
        body.classList.add('light-theme');
        themeBtn.innerHTML = '🌙 Switch to Dark Mode';
    }
}

function logout() {
    if (confirm('Are you sure you want to logout?')) {
        api.logout();
        window.location.href = 'login.html';
    }
}

document.addEventListener('DOMContentLoaded', function() {
    if (!api.isLoggedIn()) {
        window.location.href = 'login.html';
    } else {
        loadPasswords();
    }
});