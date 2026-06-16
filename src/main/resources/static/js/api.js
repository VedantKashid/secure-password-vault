// ==========================================
// API.JS - Backend Communication Layer
// ==========================================

class PasswordVaultAPI {
    constructor() {
        this.baseURL = 'http://localhost:8080/api';
        this.token = localStorage.getItem('token');
    }

    // ── Helpers ──────────────────────────────────

    _headers(includeAuth = true) {
        const h = { 'Content-Type': 'application/json' };
        if (includeAuth && this.token) h['Authorization'] = `Bearer ${this.token}`;
        return h;
    }

    /**
     * Backend wraps all responses in { message: "...", data: <payload> }.
     * This extracts the payload.
     */
    _unwrap(json) {
        if (!json) return null;
        if (Array.isArray(json)) return json;       // bare array (shouldn't happen, but safe)
        if (json.data !== undefined) return json.data;
        return json;
    }

    async _handle(response) {
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.data || data.message || `HTTP ${response.status}`);
        }
        return data;
    }

    // ── Auth ──────────────────────────────────────

    async register(username, password, email) {
        const res  = await fetch(`${this.baseURL}/auth/register`, {
            method:  'POST',
            headers: this._headers(false),
            body:    JSON.stringify({ username, password, email })
        });
        const data = await this._handle(res);
        return data;
    }

    /**
     * Returns { success: true, token: "..." } on success.
     * auth.js checks response.success.
     */
    async login(username, password) {
        const res  = await fetch(`${this.baseURL}/auth/login`, {
            method:  'POST',
            headers: this._headers(false),
            body:    JSON.stringify({ username, password })
        });
        const data = await this._handle(res);

        // Backend: ApiResponse("Login successful", tokenString)
        const token = data.data || data.token;
        if (!token) throw new Error('No token received from server');

        this.token = token;
        localStorage.setItem('token', this.token);
        return { success: true, token: this.token };
    }

    logout() {
        this.token = null;
        localStorage.removeItem('token');
    }

    isLoggedIn() {
        return !!this.token && this.token.length > 0;
    }

    // ── Passwords ─────────────────────────────────

    async getAllPasswords() {
        const res  = await fetch(`${this.baseURL}/passwords`, {
            method:  'GET',
            headers: this._headers()
        });
        const data = await this._handle(res);
        const result = this._unwrap(data);
        return Array.isArray(result) ? result : [];
    }

    async searchPasswords(keyword) {
        if (!keyword?.trim()) return this.getAllPasswords();
        const res  = await fetch(
            `${this.baseURL}/passwords/search?keyword=${encodeURIComponent(keyword)}`,
            { method: 'GET', headers: this._headers() }
        );
        const data   = await this._handle(res);
        const result = this._unwrap(data);
        return Array.isArray(result) ? result : [];
    }

    async savePassword(platform, loginUsername, password) {
        const res  = await fetch(`${this.baseURL}/passwords`, {
            method:  'POST',
            headers: this._headers(),
            body:    JSON.stringify({ platform, loginUsername, password })
        });
        const data = await this._handle(res);
        return this._unwrap(data);
    }

    async updatePassword(id, platform, loginUsername, password) {
        const res  = await fetch(`${this.baseURL}/passwords/${id}`, {
            method:  'PUT',
            headers: this._headers(),
            body:    JSON.stringify({ platform, loginUsername, password })
        });
        const data = await this._handle(res);
        return this._unwrap(data);
    }

    /**
     * DELETE returns ResponseEntity.ok(ApiResponse(...)) — NOT 204 — so we parse JSON.
     */
    async deletePassword(id) {
        const res  = await fetch(`${this.baseURL}/passwords/${id}`, {
            method:  'DELETE',
            headers: this._headers()
        });
        const data = await this._handle(res);
        return this._unwrap(data);
    }

    /**
     * Backend param is `useSpecial` (not includeSymbols).
     * Response: ApiResponse("Success", PasswordGeneratorDTO{ password, length, useSpecial })
     */
    async generatePassword(length = 16, useSpecial = true) {
        const res  = await fetch(
            `${this.baseURL}/passwords/generate?length=${length}&useSpecial=${useSpecial}`,
            { method: 'GET', headers: this._headers() }
        );
        const data = await this._handle(res);
        return this._unwrap(data); // { password: "...", length: 16, useSpecial: true }
    }

    /**
     * Endpoint: GET /api/passwords/scan-breaches (NOT POST /breach-scan)
     * Response: ApiResponse("Scan complete", BreachScanResultDTO{ breachedCount, totalPasswords })
     */
    async scanForBreaches() {
        const res  = await fetch(`${this.baseURL}/passwords/scan-breaches`, {
            method:  'GET',
            headers: this._headers()
        });
        const data = await this._handle(res);
        return this._unwrap(data);
    }

    async checkPasswordStrength(password) {
        const res  = await fetch(`${this.baseURL}/passwords/check-strength`, {
            method:  'POST',
            headers: this._headers(),
            body:    JSON.stringify({ password })
        });
        const data = await this._handle(res);
        return this._unwrap(data);
    }

    async getBreachedPasswords() {
        const res  = await fetch(`${this.baseURL}/passwords/breached-passwords`, {
            method:  'GET',
            headers: this._headers()
        });
        const data   = await this._handle(res);
        const result = this._unwrap(data);
        return Array.isArray(result) ? result : [];
    }
}

const api = new PasswordVaultAPI();