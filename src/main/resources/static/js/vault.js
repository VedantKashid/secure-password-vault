// 1. Kick out anyone who isn't logged in
if (!api.isLoggedIn()) {
    window.location.href = "login.html";
}

// 2. Load and display passwords
async function loadPasswords() {
    const keyword = document.getElementById('searchInput').value;
    const vaultList = document.getElementById('vaultList');
    vaultList.innerHTML = "<p>Loading...</p>";

    try {
        const passwords = await api.searchPasswords(keyword);

        if (passwords.length === 0) {
            vaultList.innerHTML = "<p>No passwords found in your vault.</p>";
            return;
        }

        vaultList.innerHTML = ""; // Clear the loading text

        passwords.forEach(p => {
            // If the backend flagged it as breached, show a warning and red border
            const warningBadge = p.isBreached ? '<span style="color: #ff5252; float: right; font-weight: bold;">⚠️ BREACHED!</span>' : '';
            const cardClass = p.isBreached ? 'password-card breached' : 'password-card';

            vaultList.innerHTML += `
                <div class="${cardClass}">
                    ${warningBadge}
                    <h3 style="margin-top: 0;">${p.platform}</h3>
                    <p style="margin: 5px 0;"><strong>Username:</strong> ${p.loginUsername}</p>
                    <p style="margin: 5px 0; color: #888;"><strong>Password:</strong> ******** (Encrypted)</p>
                </div>
            `;
        });
    } catch (error) {
        console.error(error);
        vaultList.innerHTML = "<p style='color: #ff5252;'>Failed to load vault. Your session may have expired.</p>";
    }
}

// 3. Trigger the Breach Scan
async function scanBreaches() {
    alert("Starting live breach scan... This checks the Have I Been Pwned database.");
    try {
        const result = await api.scanForBreaches();
        alert(`Scan complete! Checked ${result.totalPasswords} passwords. Found ${result.breachedCount} breaches.`);
        loadPasswords(); // Refresh the list to show any new red warnings
    } catch (error) {
        alert("Error scanning for breaches.");
    }
}

// 4. Logout functionality
function logout() {
    api.logout();
    window.location.href = "index.html";
}

// Automatically load passwords when the page opens
window.onload = loadPasswords;
// 5. Save a new password to the database
async function saveNewPassword() {
    const platform = document.getElementById('newPlatform').value;
    const username = document.getElementById('newUsername').value;
    const password = document.getElementById('newPassword').value;

    if (!platform || !username || !password) {
        alert("Please fill in all fields.");
        return;
    }

    try {
        await api.savePassword(platform, username, password);

        // Clear the input boxes
        document.getElementById('newPlatform').value = '';
        document.getElementById('newUsername').value = '';
        document.getElementById('newPassword').value = '';

        // Refresh the vault list to show the new entry!
        loadPasswords();
    } catch (error) {
        alert("Error saving password.");
        console.error(error);
    }
}

// 6. Generate a random secure password from the backend
async function generateRandom() {
    try {
        const result = await api.generatePassword(16, true);
        // Put the generated password directly into the text box
        document.getElementById('newPassword').value = result.password;
    } catch (error) {
        alert("Error generating password.");
        console.error(error);
    }
}