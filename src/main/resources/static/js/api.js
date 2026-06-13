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
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.data || "Registration failed");
        }
        return data;
    }

    async login(username, password) {
            const response = await fetch(`${this.baseURL}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });

            // 1. Check if the server rejected the login first
            if (!response.ok) {
                throw new Error("Invalid username or password");
            }

            // 2. Read the response as raw text so JavaScript doesn't crash!
            const text = await response.text();

            // 3. Smart parsing
            try {
                // Try to parse it as JSON (just in case)
                const data = JSON.parse(text);
                this.token = data.token || data.jwt || data.data;
            } catch (e) {
                // If parsing fails, it means Java sent the raw token string perfectly!
                this.token = text;
            }

            // 4. Save the token and finish
            localStorage.setItem('token', this.token);
            console.log('Login successful, token stored!');
            return { success: true };
        }

    logout() {
        this.token = null;
        localStorage.removeItem('token');
        console.log('Logged out');
    }

    isLoggedIn() {
        return this.token != null;
    }

    // --- HELPER TO EXTRACT DATA FROM APIRESPONSE ---
    _unwrap(json) {
        // Backend wraps everything in: { message: "...", data: actual_data }
        if (json && json.data !== undefined) {
            return json.data;
        }
        return json;
    }

    // --- VAULT OPERATIONS ---

    /**
     * Save a new password
     * ⚠️ IMPORTANT: PasswordRequestDTO has fields:
     *    - platform (not website)
     *    - loginUsername (not username)
     *    - password
     */
    async savePassword(platform, loginUsername, password, notes = '', category = '') {
        try {
            const payload = {
                platform: platform,           // ← MUST be "platform"
                loginUsername: loginUsername, // ← MUST be "loginUsername"
                password: password
            };

            console.log('Saving password with payload:', payload);

            const response = await fetch(`${this.baseURL}/passwords`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.token}`
                },
                body: JSON.stringify(payload)
            });

            console.log('Response status:', response.status);
            const data = await response.json();
            console.log('Response data:', data);

            if (!response.ok) {
                throw new Error(data.data || data.message || "Failed to save password");
            }

            return this._unwrap(data);
        } catch (error) {
            console.error('savePassword error:', error);
            throw error;
        }
    }

    // Get all passwords
    async getAllPasswords() {
        try {
            const response = await fetch(`${this.baseURL}/passwords`, {
                headers: { 'Authorization': `Bearer ${this.token}` }
            });
            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.data || "Failed to fetch passwords");
            }
            return this._unwrap(data);
        } catch (error) {
            console.error('getAllPasswords error:', error);
            throw error;
        }
    }

    // Update a password
    async updatePassword(passwordId, platform, loginUsername, password) {
        try {
            const response = await fetch(`${this.baseURL}/passwords/${passwordId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.token}`
                },
                body: JSON.stringify({
                    platform,
                    loginUsername,
                    password
                })
            });

            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.data || "Failed to update password");
            }
            return this._unwrap(data);
        } catch (error) {
            console.error('updatePassword error:', error);
            throw error;
        }
    }

    // Delete a password
    async deletePassword(passwordId) {
        try {
            const response = await fetch(`${this.baseURL}/passwords/${passwordId}`, {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${this.token}` }
            });

            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.data || "Failed to delete password");
            }
            return this._unwrap(data);
        } catch (error) {
            console.error('deletePassword error:', error);
            throw error;
        }
    }

    // Search passwords by platform
    async searchPasswords(keyword) {
        try {
            const response = await fetch(`${this.baseURL}/passwords/search?keyword=${keyword}`, {
                headers: { 'Authorization': `Bearer ${this.token}` }
            });
            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.data || "Search failed");
            }
            return this._unwrap(data);
        } catch (error) {
            console.error('searchPasswords error:', error);
            throw error;
        }
    }

    // Generate a random password
    async generatePassword(length = 16, useSpecial = true) {
        try {
            const response = await fetch(
                `${this.baseURL}/passwords/generate?length=${length}&useSpecial=${useSpecial}`,
                {
                    headers: { 'Authorization': `Bearer ${this.token}` }
                }
            );
            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.data || "Failed to generate password");
            }
            return this._unwrap(data);
        } catch (error) {
            console.error('generatePassword error:', error);
            throw error;
        }
    }

    // Check password strength
    async checkPasswordStrength(password) {
        try {
            const response = await fetch(`${this.baseURL}/passwords/check-strength`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.token}`
                },
                body: JSON.stringify({ password })
            });
            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.data || "Failed to check strength");
            }
            return this._unwrap(data);
        } catch (error) {
            console.error('checkPasswordStrength error:', error);
            throw error;
        }
    }

    // Scan for breaches
    async scanForBreaches() {
        try {
            const response = await fetch(`${this.baseURL}/passwords/scan-breaches`, {
                headers: { 'Authorization': `Bearer ${this.token}` }
            });
            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.data || "Failed to scan breaches");
            }
            return this._unwrap(data);
        } catch (error) {
            console.error('scanForBreaches error:', error);
            throw error;
        }
    }

    // Get breached passwords
    async getBreachedPasswords() {
        try {
            const response = await fetch(`${this.baseURL}/passwords/breached-passwords`, {
                headers: { 'Authorization': `Bearer ${this.token}` }
            });
            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.data || "Failed to get breached passwords");
            }
            return this._unwrap(data);
        } catch (error) {
            console.error('getBreachedPasswords error:', error);
            throw error;
        }
    }
}

// Initialize globally
const api = new PasswordVaultAPI();
console.log('API initialized');