/**
 * Green Coin - Collector Dashboard Logic
 */

let map;
let markers = [];
let userProfile = null;
let currentLocation = null;

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
    if (!userProfile || userProfile.role !== 'COLLECTOR') {
        alert('Access denied. Collectors only.');
        window.location.href = 'login.html';
        return;
    }


    // Initialize map
    initMap();

    // Load data
    await loadAvailableReports();
    await loadMyPickups();

    // Setup WebSocket
    wsClient.connect();
    wsClient.onNewReport(handleNewReport);
}

// Initialize Leaflet map
function initMap() {
    map = L.map('map-overhaul').setView([12.9716, 77.5946], 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors',
        maxZoom: 19
    }).addTo(map);
}

// Use current location
function useMyLocation() {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            (position) => {
                currentLocation = {
                    lat: position.coords.latitude,
                    lng: position.coords.longitude
                };

                map.setView([currentLocation.lat, currentLocation.lng], 14);

                // Add marker
                L.marker([currentLocation.lat, currentLocation.lng])
                    .addTo(map)
                    .bindPopup('Your Location')
                    .openPopup();

                // Load nearby reports
                loadNearbyReports();
            },
            (error) => {
                console.error('Location error:', error);
                alert('Location unavailable');
            }
        );
    }
}

// Load nearby reports
async function loadNearbyReports() {
    if (!currentLocation) {
        alert('Please enable location first');
        return;
    }

    try {
        const reports = await api.getNearbyReports(currentLocation.lat, currentLocation.lng);
        displayReportsOnMap(reports);
    } catch (error) {
        console.error('Failed to load nearby reports:', error);
    }
}

// Load all available reports
async function loadAvailableReports() {
    try {
        const reports = await api.getAvailableReports();
        displayReportsOnMap(reports);
    } catch (error) {
        console.error('Failed to load reports:', error);
    }
}

// Display reports on map
function displayReportsOnMap(reports) {
    // Clear existing markers
    markers.forEach(marker => map.removeLayer(marker));
    markers = [];

    reports.forEach(report => {
        const marker = L.circleMarker([report.latitude, report.longitude], {
            radius: 10,
            fillColor: '#2196F3',
            color: '#fff',
            weight: 2,
            opacity: 1,
            fillOpacity: 0.8
        }).addTo(map);

        marker.on('click', () => showReportDetails(report));
        markers.push(marker);
    });

    // Fit bounds if reports exist
    if (reports.length > 0) {
        const bounds = reports.map(r => [r.latitude, r.longitude]);
        map.fitBounds(bounds, { padding: [50, 50] });
    }
}

// Show report details in modal
function showReportDetails(report) {
    const modal = document.getElementById('reportModal');
    const title = document.getElementById('modalTitle');
    const content = document.getElementById('modalContent');
    const actionBtn = document.getElementById('actionBtn');

    title.textContent = `Report #${report.id}`;
    content.innerHTML = `
        <img src="${report.imageUrl}" style="width: 100%; border-radius: 8px; margin-bottom: 1rem;" />
        <p><strong>Status:</strong> <span class="status-badge status-${report.status.toLowerCase()}">${report.status}</span></p>
        <p><strong>Description:</strong> ${report.description || 'No description'}</p>
        <p><strong>Location:</strong> ${report.latitude.toFixed(5)}, ${report.longitude.toFixed(5)}</p>
        <p><strong>Coins:</strong> ${report.coinsAwarded}</p>
        <p><strong>Reported:</strong> ${new Date(report.reportedAt).toLocaleString()}</p>
    `;

    // Configure action button
    if (report.status === 'OPEN') {
        actionBtn.textContent = '🚚 Pick Up';
        actionBtn.onclick = () => pickUpReport(report.id);
    } else if (report.collectorId === userProfile.id && report.status === 'PICKING') {
        actionBtn.textContent = '✅ Mark Collected';
        actionBtn.onclick = () => markCollected(report.id);
    } else {
        actionBtn.style.display = 'none';
    }

    modal.style.display = 'flex';
}

// Close modal
function closeModal() {
    document.getElementById('reportModal').style.display = 'none';
}

// Pick up report
async function pickUpReport(reportId) {
    try {
        await api.markPicking(reportId);
        alert('✅ Report marked as PICKING. Don\'t forget to mark it COLLECTED after pickup!');
        closeModal();
        await loadAvailableReports();
        await loadMyPickups();
    } catch (error) {
        console.error('Failed to pick up:', error);
        alert('Failed to pick up report: ' + error.message);
    }
}

// Mark as collected
async function markCollected(reportId) {
    if (!confirm('Mark this report as collected? This will award coins to the reporter.')) {
        return;
    }

    try {
        await api.markCollected(reportId);
        alert('✅ Report marked as COLLECTED! Coins awarded to reporter.');
        closeModal();
        await loadAvailableReports();
        await loadMyPickups();
    } catch (error) {
        console.error('Failed to mark collected:', error);
        alert('Failed to mark collected: ' + error.message);
    }
}

// Load my active pickups
async function loadMyPickups() {
    try {
        const pickups = await api.getMyPickups();
        const container = document.getElementById('pickupsList');
        const countEl = document.getElementById('activeCount');

        if (countEl) countEl.textContent = pickups.length;

        if (pickups.length === 0) {
            container.innerHTML = `
                <div style="text-align: center; padding: 3rem 1rem; color: var(--text-light);">
                    <i class="fas fa-truck-loading" style="font-size: 2.5rem; margin-bottom: 1rem; opacity: 0.2;"></i>
                    <p style="font-size: 0.9rem; opacity: 0.6;">No active pickups at the moment</p>
                </div>
            `;
            return;
        }

        container.innerHTML = pickups.map(p => {
            const isUrgent = p.description && p.description.toLowerCase().includes('urgent');
            const dist = (Math.random() * 3).toFixed(1); // Placeholder for distance

            return `
                <div class="report-card">
                    <div class="tag ${isUrgent ? 'urgent' : 'new'}">${isUrgent ? 'URGENT' : 'ACTIVE'}</div>
                    <div class="distance-tag">${dist} km away</div>
                    
                    <h3 style="margin: 0.5rem 0 0.25rem;">${p.description || 'Waste Report'}</h3>
                    <div style="font-size: 0.75rem; color: var(--text-light); margin-bottom: 1rem;">
                        <i class="fas fa-map-marker-alt"></i> ${p.latitude.toFixed(4)}, ${p.longitude.toFixed(4)}
                    </div>

                    <div class="card-main">
                        <img src="${p.imageUrl || 'https://via.placeholder.com/150?text=Waste+Image'}" class="card-img-placeholder" alt="Waste">
                    </div>

                    <div class="card-actions">
                        <button class="btn btn-secondary btn-sm" onclick="showReportDetailsById(${p.id})">Details</button>
                        <button class="btn btn-primary btn-sm" onclick="markCollected(${p.id})">Complete</button>
                    </div>
                </div>
            `;
        }).join('');

        // Add markers for pickups
        pickups.forEach(pickup => {
            const marker = L.circleMarker([pickup.latitude, pickup.longitude], {
                radius: 12,
                fillColor: '#FF9800',
                color: '#fff',
                weight: 3,
                opacity: 1,
                fillOpacity: 1
            }).addTo(map);

            marker.on('click', () => showReportDetails(pickup));
            markers.push(marker);
        });

    } catch (error) {
        console.error('Failed to load pickups:', error);
    }
}

// Show report details by ID
async function showReportDetailsById(reportId) {
    try {
        const report = await api.getReportById(reportId);
        showReportDetails(report);
    } catch (error) {
        console.error('Failed to load report:', error);
    }
}

// Handle new report notification
function handleNewReport(data) {
    console.log('🔔 REAL-TIME: New report received via WebSocket!', data);

    // Show notification
    if (Notification.permission === 'granted') {
        new Notification('New Waste Report', {
            body: `New report available for pickup nearby`,
            icon: '/favicon.ico'
        });
    }

    // Refresh the map and records
    console.log('Refreshing dashboard data...');
    loadAvailableReports();
}

// Request notification permission on load
if ('Notification' in window && Notification.permission === 'default') {
    Notification.requestPermission();
}

// Logout
function logout() {
    firebase.auth().signOut();
    localStorage.clear();
    window.location.href = 'login.html';
}

// Initialize on page load
window.addEventListener('DOMContentLoaded', init);
