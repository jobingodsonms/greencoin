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
        const idToken = localStorage.getItem('idToken');
        return {
            'Authorization': `Bearer ${idToken}`,
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
    async call(endpoint, options = {}, retryCount = 0) {
        const url = `${this.baseURL}${endpoint}`;
        console.log(`[API] Request: ${options.method || 'GET'} ${url} (Retry: ${retryCount})`);

        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 30000); // 30s timeout

        try {
            const response = await fetch(url, {
                ...options,
                headers: this.getHeaders(),
                signal: controller.signal
            });
            clearTimeout(timeoutId);

            console.log(`[API] Response: ${response.status} ${url}`);

            if (response.status === 401 && retryCount < 1) {
                console.warn('[API] 401 Unauthorized. Attempting to refresh token...');
                await this.refreshToken();
                // Retry request once
                return this.call(endpoint, options, retryCount + 1);
            }

            if (!response.ok) {
                let errorMessage = `API Error ${response.status}`;
                try {
                    const error = await response.json();
                    errorMessage = error.message || errorMessage;
                } catch (e) {
                    try {
                        const text = await response.text();
                        if (text && text.length < 100) errorMessage = text;
                    } catch (e2) { }
                }

                console.error(`[API] FAILED (${response.status}): ${errorMessage}`);

                // If 403, specifically mention synchronization/auth issue
                if (response.status === 403) {
                    errorMessage = "Access Denied (403). Your session may have expired or backend configuration changed. Please try logging out and back in.";
                }

                throw new Error(errorMessage);
            }

            return await response.json();
        } catch (error) {
            clearTimeout(timeoutId);
            if (error.name === 'AbortError') {
                console.error(`[API] Timeout: ${url}`);
                throw new Error('Request timed out after 30 seconds. The server might be busy or down.');
            }
            console.error('[API] Network Error:', error);
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
