class PasswordVaultAPI {
    constructor() {
        this.baseURL = 'http://localhost:8080/api';
        this.token = localStorage.getItem('token');
    }

    // --- AUTHENTICATION ---
    async register(username, password, email) {
        const response = await fetch(`${this.baseURL}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password, email })
        });
        return response.json();
    }

    async login(username, password) {
            const response = await fetch(`${this.baseURL}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });

            // If the server rejects the login (e.g., wrong password), throw an error
            if (!response.ok) {
                throw new Error("Invalid credentials");
            }

            // Read the response as raw text first so it doesn't crash
            const text = await response.text();

            try {
                // Try to parse it as JSON (if your backend sends an object)
                const data = JSON.parse(text);
                this.token = data.token || data.jwt;
                localStorage.setItem('token', this.token);
                return data;
            } catch (e) {
                // If parsing fails, it means the backend sent the raw token string!
                this.token = text;
                localStorage.setItem('token', this.token);
                return { token: text }; // Wrap it in an object for auth.js
            }
        }

    logout() {
        this.token = null;
        localStorage.removeItem('token');
    }

    isLoggedIn() {
        return this.token != null;
    }

    // --- VAULT OPERATIONS ---
    async savePassword(platform, loginUsername, password) {
        const response = await fetch(`${this.baseURL}/vault/add`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${this.token}`
            },
            body: JSON.stringify({ platform, loginUsername, password })
        });
        return response.json();
    }

    async searchPasswords(keyword) {
        const response = await fetch(`${this.baseURL}/vault/search?keyword=${keyword}`, {
            headers: { 'Authorization': `Bearer ${this.token}` }
        });
        return response.json();
    }

    // --- UTILITIES ---
    async generatePassword(length = 16, useSpecial = true) {
        const response = await fetch(
            `${this.baseURL}/vault/generate?length=${length}&useSpecial=${useSpecial}`,
            { headers: { 'Authorization': `Bearer ${this.token}` } }
        );
        return response.json();
    }

    async checkPasswordStrength(password) {
        const response = await fetch(`${this.baseURL}/vault/check-strength`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${this.token}`
            },
            body: JSON.stringify({ password })
        });
        return response.json();
    }

    async scanForBreaches() {
        const response = await fetch(`${this.baseURL}/vault/scan-breaches`, {
            headers: { 'Authorization': `Bearer ${this.token}` }
        });
        return response.json();
    }
}

// Initialize the API globally
const api = new PasswordVaultAPI();