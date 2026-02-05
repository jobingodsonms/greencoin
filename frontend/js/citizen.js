/**
 * Green Coin - Citizen Dashboard Logic
 */

let map;
let currentLocation = null;
let markers = [];
let userProfile = null;

// Initialize dashboard
async function init() {
    // Check authentication
    const idToken = localStorage.getItem('idToken');
    if (!idToken) {
        window.location.href = 'login.html';
        return;
    }

    // Load user profile
    userProfile = JSON.parse(localStorage.getItem('userProfile'));
    if (!userProfile || userProfile.role !== 'CITIZEN') {
        alert('Access denied. Citizens only.');
        window.location.href = 'login.html';
        return;
    }

    // Update UI
    document.getElementById('userName').textContent = userProfile.displayName || userProfile.email;

    // Initialize map
    initMap();

    // Get current location
    refreshLocation();

    // Load data
    await loadCoinBalance();
    await loadTransactions();
    await loadMyReports();

    // Setup WebSocket
    wsClient.connect();
    wsClient.onCoinUpdate(handleCoinUpdate);

    // Setup image preview
    document.getElementById('wasteImage').addEventListener('change', handleImagePreview);
}

// Initialize Leaflet map
function initMap() {
    map = L.map('map').setView([12.9716, 77.5946], 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors',
        maxZoom: 19
    }).addTo(map);
}

// Get current GPS location
function refreshLocation() {
    const locationDisplay = document.getElementById('locationDisplay');
    locationDisplay.value = 'Getting location...';

    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            (position) => {
                currentLocation = {
                    lat: position.coords.latitude,
                    lng: position.coords.longitude
                };

                document.getElementById('latitude').value = currentLocation.lat;
                document.getElementById('longitude').value = currentLocation.lng;
                locationDisplay.value = `${currentLocation.lat.toFixed(5)}, ${currentLocation.lng.toFixed(5)}`;

                // Center map on current location
                map.setView([currentLocation.lat, currentLocation.lng], 15);

                // Add marker for current location
                L.marker([currentLocation.lat, currentLocation.lng])
                    .addTo(map)
                    .bindPopup('Your Location')
                    .openPopup();
            },
            (error) => {
                console.error('Location error:', error);
                locationDisplay.value = 'Location unavailable';
                alert('Please enable location services to report waste.');
            }
        );
    } else {
        alert('Geolocation is not supported by your browser.');
    }
}

// Handle image preview
function handleImagePreview(e) {
    const file = e.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = (event) => {
            document.getElementById('previewImg').src = event.target.result;
            document.getElementById('imagePreview').style.display = 'block';
        };
        reader.readAsDataURL(file);
    }
}

// Submit waste report
document.getElementById('reportForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();

    const imageFile = document.getElementById('wasteImage').files[0];
    const description = document.getElementById('description').value;
    const latitude = document.getElementById('latitude').value;
    const longitude = document.getElementById('longitude').value;

    if (!latitude || !longitude) {
        alert('Please enable location to submit report');
        return;
    }

    if (!imageFile) {
        alert('Please select an image');
        return;
    }

    try {
        // Upload image to Firebase Storage
        const imageUrl = await uploadImage(imageFile);

        // Submit report to backend
        const report = await api.createReport({
            latitude: parseFloat(latitude),
            longitude: parseFloat(longitude),
            imageUrl: imageUrl,
            description: description
        });

        alert('✅ Report submitted successfully! You will earn 50 coins when collected.');

        // Reset form
        document.getElementById('reportForm').reset();
        document.getElementById('imagePreview').style.display = 'none';

        // Reload reports
        await loadMyReports();

    } catch (error) {
        console.error('Submit error:', error);
        alert('Failed to submit report. Please try again.');
    }
});

// Upload image to Firebase Storage
async function uploadImage(file) {
    // For simplicity, we'll use a placeholder URL
    // In production, upload to Firebase Storage

    const storage = firebase.storage();
    const storageRef = storage.ref();
    const timestamp = Date.now();
    const imageRef = storageRef.child(`waste-reports/${timestamp}_${file.name}`);

    await imageRef.put(file);
    const downloadURL = await imageRef.getDownloadURL();

    return downloadURL;
}

// Load coin balance
async function loadCoinBalance() {
    try {
        const data = await api.getCoinBalance();
        document.getElementById('coinBalance').textContent = `${data.balance} Coins`;

        // Update in profile
        userProfile.coinBalance = data.balance;
        localStorage.setItem('userProfile', JSON.stringify(userProfile));
    } catch (error) {
        console.error('Failed to load coin balance:', error);
    }
}

// Load transaction history
async function loadTransactions() {
    try {
        const transactions = await api.getTransactions();
        const container = document.getElementById('transactionList');

        if (transactions.length === 0) {
            container.innerHTML = '<p style="color: var(--text-light); text-align: center;">No transactions yet</p>';
            return;
        }

        container.innerHTML = transactions.slice(0, 5).map(tx => `
            <div style="display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid var(--border-color);">
                <div>
                    <div style="font-weight: 600;">${tx.transactionType}</div>
                    <div style="font-size: 0.875rem; color: var(--text-light);">${new Date(tx.createdAt).toLocaleDateString()}</div>
                </div>
                <div style="font-weight: 600; color: ${tx.amount > 0 ? 'var(--primary-color)' : 'var(--danger-color)'};">
                    ${tx.amount > 0 ? '+' : ''}${tx.amount}
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Failed to load transactions:', error);
    }
}

// Load my reports
async function loadMyReports() {
    try {
        const reports = await api.getMyReports();

        // Clear existing markers
        markers.forEach(marker => map.removeLayer(marker));
        markers = [];

        // Add markers for each report
        reports.forEach(report => {
            const color = getStatusColor(report.status);
            const marker = L.circleMarker([report.latitude, report.longitude], {
                radius: 8,
                fillColor: color,
                color: '#fff',
                weight: 2,
                opacity: 1,
                fillOpacity: 0.8
            }).addTo(map);

            marker.bindPopup(`
                <strong>Status:</strong> ${report.status}<br>
                <strong>Coins:</strong> ${report.coinsAwarded}<br>
                ${report.description ? `<strong>Description:</strong> ${report.description}` : ''}
            `);

            markers.push(marker);
        });

        // Update reports table
        const tableContainer = document.getElementById('reportsTable');
        if (reports.length === 0) {
            tableContainer.innerHTML = '<p style="color: var(--text-light); text-align: center;">No reports yet. Submit your first report above!</p>';
        } else {
            tableContainer.innerHTML = `
                <table class="table">
                    <thead>
                        <tr>
                            <th>Date</th>
                            <th>Status</th>
                            <th>Coins</th>
                            <th>Collector</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${reports.map(r => `
                            <tr>
                                <td>${new Date(r.reportedAt).toLocaleDateString()}</td>
                                <td><span class="status-badge status-${r.status.toLowerCase()}">${r.status}</span></td>
                                <td>${r.coinsAwarded}</td>
                                <td>${r.collectorName || '-'}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            `;
        }

    } catch (error) {
        console.error('Failed to load reports:', error);
    }
}

// Get status color for markers
function getStatusColor(status) {
    switch (status) {
        case 'OPEN': return '#2196F3';
        case 'PICKING': return '#FF9800';
        case 'COLLECTED': return '#4CAF50';
        default: return '#757575';
    }
}

// Handle coin updates from WebSocket
function handleCoinUpdate(data) {
    if (data.type === 'COINS_AWARDED') {
        // Update balance
        document.getElementById('coinBalance').textContent = `${data.newBalance} Coins`;

        // Show notification
        alert(`🎉 You earned ${data.amount} coins! New balance: ${data.newBalance}`);

        // Reload data
        loadTransactions();
        loadMyReports();
    }
}

// Logout
function logout() {
    firebase.auth().signOut();
    localStorage.clear();
    window.location.href = 'login.html';
}

// Initialize on page load
window.addEventListener('DOMContentLoaded', init);
