/**
 * Green Coin - Citizen Dashboard Logic
 */

let map;
let currentLocation = null;
let markers = [];
let userProfile = null;
let capturedBlob = null;
let stream = null;

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
    try {
        console.log('Loading dashboard data...');
        await Promise.all([
            loadCoinBalance(),
            loadTransactions(),
            loadMyReports()
        ]);
        console.log('Dashboard data loaded successfully');
    } catch (error) {
        console.error('Initial data load error:', error);
        // Don't alert here as individual loaders might have alerts, 
        // but ensure we don't just hang.
    }

    // Setup WebSocket
    try {
        wsClient.connect();
        wsClient.onCoinUpdate(handleCoinUpdate);
    } catch (e) {
        console.error('WebSocket connection failed:', e);
    }

    // Setup image preview
    document.getElementById('wasteImage')?.addEventListener('change', handleImagePreview);
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
        capturedBlob = file;
        const reader = new FileReader();
        reader.onload = (event) => {
            document.getElementById('previewImg').src = event.target.result;
            document.getElementById('imagePreview').style.display = 'block';
        };
        reader.readAsDataURL(file);
    }
}

// Camera Logic
async function openCamera() {
    const modal = document.getElementById('cameraModal');
    const video = document.getElementById('video');

    try {
        stream = await navigator.mediaDevices.getUserMedia({
            video: { facingMode: 'environment' },
            audio: false
        });
        video.srcObject = stream;
        modal.style.display = 'flex';
    } catch (err) {
        console.error("Camera access denied:", err);
        alert("Could not access camera. Please check permissions.");
    }
}

function closeCamera() {
    if (stream) {
        stream.getTracks().forEach(track => track.stop());
    }
    document.getElementById('cameraModal').style.display = 'none';
}

function takeSnapshot() {
    const video = document.getElementById('video');
    const canvas = document.getElementById('canvas');
    const context = canvas.getContext('2d');

    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    context.drawImage(video, 0, 0, canvas.width, canvas.height);

    canvas.toBlob((blob) => {
        capturedBlob = blob;
        const url = URL.createObjectURL(blob);
        document.getElementById('previewImg').src = url;
        document.getElementById('imagePreview').style.display = 'block';
        closeCamera();
    }, 'image/jpeg', 0.8);
}

function clearImage() {
    capturedBlob = null;
    document.getElementById('previewImg').src = '';
    document.getElementById('imagePreview').style.display = 'none';
}

/**
 * Convert File/Blob to Base64
 */
async function fileToBase64(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = () => resolve(reader.result);
        reader.onerror = error => reject(error);
    });
}

// Set up form submission
document.getElementById('reportForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();

    const description = document.getElementById('description').value;
    const latitude = document.getElementById('latitude').value;
    const longitude = document.getElementById('longitude').value;

    if (!capturedBlob) {
        alert('Please capture or select a photo first');
        return;
    }

    if (!latitude || !longitude) {
        alert('Please enable location to submit report');
        return;
    }

    const submitBtn = e.target.querySelector('button[type="submit"]');
    const originalBtnText = submitBtn.innerHTML;

    try {
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Submitting...';

        console.log('Step 1: Converting image to Base64...');
        const base64Image = await fileToBase64(capturedBlob);
        console.log('Step 1 Complete: Base64 string created (length: ' + base64Image.length + ')');

        console.log('Step 2: Submitting report to backend API...');
        const report = await api.createReport({
            latitude: parseFloat(latitude),
            longitude: parseFloat(longitude),
            imageUrl: base64Image, // Sending Base64 string directly
            description: description
        });
        console.log('Step 2 Complete: Report created:', report);

        // Success!
        alert('✅ Report submitted successfully! You will earn 50 coins when collected.');

        // Reset form
        document.getElementById('reportForm').reset();
        capturedBlob = null;
        document.getElementById('imagePreview').style.display = 'none';

        // Refresh UI
        loadTransactions();
        loadCoinBalance();
        loadMyReports();

    } catch (error) {
        console.error('Submit error:', error);
        alert('Failed to submit report: ' + error.message);
    } finally {
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalBtnText;
    }
});

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
        document.getElementById('coinBalance').textContent = 'Error';
    }
}

// Load transaction history
async function loadTransactions() {
    try {
        const transactions = await api.getTransactions();
        const container = document.getElementById('transactionList');

        if (transactions.length === 0) {
            container.innerHTML = '<p style="color: var(--text-light); text-align: center; padding: 2rem; opacity: 0.5;">No transactions yet</p>';
            return;
        }

        container.innerHTML = transactions.slice(0, 5).map(tx => `
            <div style="display: flex; align-items: center; gap: 12px; padding: 12px; background: white; border-radius: 12px; margin-bottom: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.05);">
                <div style="width: 40px; height: 40px; border-radius: 10px; background: ${tx.amount > 0 ? '#f0f7f0' : '#fff0f0'}; display: flex; align-items: center; justify-content: center; color: ${tx.amount > 0 ? 'var(--primary-color)' : 'var(--danger-color)'};">
                    <i class="fas ${tx.amount > 0 ? 'fa-arrow-down' : 'fa-arrow-up'}"></i>
                </div>
                <div style="flex: 1;">
                    <div style="font-weight: 700; font-size: 0.9rem;">${tx.transactionType}</div>
                    <div style="font-size: 0.75rem; color: var(--text-light);">${new Date(tx.createdAt).toLocaleDateString()}</div>
                </div>
                <div style="font-weight: 800; color: ${tx.amount > 0 ? 'var(--primary-color)' : 'var(--danger-color)'};">
                    ${tx.amount > 0 ? '+' : ''}${tx.amount}
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Failed to load transactions:', error);
        document.getElementById('transactionList').innerHTML =
            `<p style="color: var(--danger-color); text-align: center;">Failed to load history</p>`;
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
        document.getElementById('reportsTable').innerHTML =
            `<p style="color: var(--danger-color); text-align: center;">Failed to load reports: ${error.message}</p>`;
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
