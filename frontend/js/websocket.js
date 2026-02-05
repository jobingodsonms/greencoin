/**
 * Green Coin - WebSocket Client
 * 
 * Real-time updates for reports and coins
 */

class WebSocketClient {
    constructor() {
        this.stompClient = null;
        this.connected = false;
        this.callbacks = {
            newReport: [],
            statusChange: [],
            coinUpdate: []
        };
    }

    connect() {
        const socket = new SockJS(config.WS_URL);
        this.stompClient = Stomp.over(socket);

        // Disable debug logging
        this.stompClient.debug = null;

        this.stompClient.connect({}, (frame) => {
            console.log('✅ WebSocket connected');
            this.connected = true;

            // Subscribe to new reports (for collectors)
            this.stompClient.subscribe('/topic/reports/new', (message) => {
                const data = JSON.parse(message.body);
                this.callbacks.newReport.forEach(cb => cb(data));
            });

            // Subscribe to personal coin updates
            const userProfile = JSON.parse(localStorage.getItem('userProfile'));
            if (userProfile) {
                this.stompClient.subscribe(`/user/queue/coins`, (message) => {
                    const data = JSON.parse(message.body);
                    this.callbacks.coinUpdate.forEach(cb => cb(data));
                });
            }
        }, (error) => {
            console.error('❌ WebSocket error:', error);
            this.connected = false;
            // Retry connection after 5 seconds
            setTimeout(() => this.connect(), 5000);
        });
    }

    subscribeToReport(reportId, callback) {
        if (this.connected) {
            this.stompClient.subscribe(`/topic/reports/${reportId}/status`, (message) => {
                const data = JSON.parse(message.body);
                callback(data);
            });
        }
    }

    onNewReport(callback) {
        this.callbacks.newReport.push(callback);
    }

    onStatusChange(callback) {
        this.callbacks.statusChange.push(callback);
    }

    onCoinUpdate(callback) {
        this.callbacks.coinUpdate.push(callback);
    }

    disconnect() {
        if (this.stompClient) {
            this.stompClient.disconnect();
            this.connected = false;
        }
    }
}

// Export instance
window.wsClient = new WebSocketClient();
