/**
 * Green Coin - API Client
 * 
 * Handles all backend API calls with authentication
 */

class APIClient {
    constructor() {
        this.baseURL = config.API_BASE_URL;
        this.idToken = localStorage.getItem('idToken');
    }

    // Get auth headers
    getHeaders() {
        return {
            'Authorization': `Bearer ${this.idToken}`,
            'Content-Type': 'application/json'
        };
    }

    // Refresh token if needed
    async refreshToken() {
        const user = firebase.auth().currentUser;
        if (user) {
            this.idToken = await user.getIdToken(true);
            localStorage.setItem('idToken', this.idToken);
        }
    }

    // Generic API call
    async call(endpoint, options = {}) {
        try {
            const response = await fetch(`${this.baseURL}${endpoint}`, {
                ...options,
                headers: this.getHeaders()
            });

            if (response.status === 401) {
                await this.refreshToken();
                // Retry request
                return this.call(endpoint, options);
            }

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'API request failed');
            }

            return await response.json();
        } catch (error) {
            console.error('API Error:', error);
            throw error;
        }
    }

    // User APIs
    async getUserProfile() {
        return this.call('/api/user/profile');
    }

    // Waste Report APIs
    async createReport(reportData) {
        return this.call('/api/reports', {
            method: 'POST',
            body: JSON.stringify(reportData)
        });
    }

    async getAvailableReports() {
        return this.call('/api/reports/available');
    }

    async getNearbyReports(lat, lng) {
        return this.call(`/api/reports/nearby?latitude=${lat}&longitude=${lng}`);
    }

    async getMyReports() {
        return this.call('/api/reports/my-reports');
    }

    async getMyPickups() {
        return this.call('/api/reports/my-pickups');
    }

    async markPicking(reportId) {
        return this.call(`/api/reports/${reportId}/pick`, {
            method: 'PATCH'
        });
    }

    async markCollected(reportId) {
        return this.call(`/api/reports/${reportId}/collect`, {
            method: 'PATCH'
        });
    }

    async getReportById(reportId) {
        return this.call(`/api/reports/${reportId}`);
    }

    // Transaction APIs
    async getTransactions() {
        return this.call('/api/user/transactions');
    }

    async getCoinBalance() {
        return this.call('/api/user/profile');
    }
}

// Export instance
window.api = new APIClient();
