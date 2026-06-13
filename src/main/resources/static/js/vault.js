// ==========================================
// VAULT.JS - Password Management Functions
// ==========================================

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
        const breachLabel = pwd.isBreached ? '<span style="color: #ff5252;">⚠️ BREACHED</span>' : '';

        html += `
            <div class="password-card ${isBreached}">
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
                        <button onclick="copyPassword(${index})" style="width: auto; background: #2196F3;">Copy</button>
                        <button onclick="deletePassword(${index})" style="width: auto; background: #ff5252;">Delete</button>
                    </div>
                </div>
            </div>
        `;
    });

    vaultList.innerHTML = html;

    // Store passwords globally for delete/copy operations
    window.currentPasswords = passwords;
}

// Toggle password visibility
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

// Copy password to clipboard
function copyPassword(index) {
    // 👇 FIXED: Now copies the encryptedPassword 👇
    const password = window.currentPasswords[index].encryptedPassword;
    navigator.clipboard.writeText(password).then(() => {
        showToast('Password copied to clipboard!', 'success');
    }).catch(() => {
        showToast('Failed to copy password', 'error');
    });
}

// Delete a password
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
        console.error('Error deleting password:', error);
        showToast('Error deleting password: ' + error.message, 'error');
    }
}

// Save a new password
async function saveNewPassword() {
    try {
        const platform = document.getElementById('newPlatform').value.trim();
        const loginUsername = document.getElementById('newUsername').value.trim();
        const password = document.getElementById('newPassword').value.trim();

        if (!platform || !loginUsername || !password) {
            showToast('Please fill in all fields', 'error');
            return;
        }

        console.log('Saving password:', { platform, loginUsername });

        // ⚠️ IMPORTANT: Send correct field names to match PasswordRequestDTO
        await api.savePassword(platform, loginUsername, password);

        showToast('Password saved successfully!', 'success');

        // Clear form
        document.getElementById('newPlatform').value = '';
        document.getElementById('newUsername').value = '';
        document.getElementById('newPassword').value = '';

        // Reload passwords
        loadPasswords();
    } catch (error) {
        console.error('Error saving password:', error);
        showToast('Error saving password: ' + error.message, 'error');
    }
}

// Generate random password
async function generateRandom() {
    try {
        const result = await api.generatePassword(16, true);

        // result could be the entire DTO or just the password string
        const generatedPassword = result.password || result;

        document.getElementById('newPassword').value = generatedPassword;
        showToast('Password generated!', 'success');
    } catch (error) {
        console.error('Error generating password:', error);
        showToast('Error generating password: ' + error.message, 'error');
    }
}

// Scan for breaches
async function scanBreaches() {
    try {
        showToast('Scanning for breaches...', 'success');
        const result = await api.scanForBreaches();
        showToast(`Breach scan complete! ${result.breachedCount || 0} breached password(s) found.`, 'success');
        loadPasswords();
    } catch (error) {
        console.error('Error scanning breaches:', error);
        showToast('Error scanning breaches: ' + error.message, 'error');
    }
}

// Logout
function logout() {
    if (confirm('Are you sure you want to logout?')) {
        api.logout();
        window.location.href = 'login.html';
    }
}

// Initial load when page opens
document.addEventListener('DOMContentLoaded', function() {
    console.log('Vault page loaded');
    console.log('Logged in:', api.isLoggedIn());

    if (!api.isLoggedIn()) {
        alert('You are not logged in. Redirecting to login page...');
        window.location.href = 'login.html';
    } else {
        loadPasswords();
    }
});