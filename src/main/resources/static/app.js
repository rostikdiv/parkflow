const API_BASE = '/api/v1';

// Initialize Map (Centered on Kyiv as per seed data)
const map = L.map('map').setView([50.4475, 30.5225], 13);

// Dark theme tile layer (CartoDB Dark Matter)
L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>',
    subdomains: 'abcd',
    maxZoom: 20
}).addTo(map);

// Custom Icons
const createIcon = (color) => {
    return L.divIcon({
        className: 'custom-div-icon',
        html: `<svg viewBox="0 0 24 24" width="36" height="36" fill="${color}" stroke="#fff" stroke-width="2">
                 <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                 <circle cx="12" cy="10" r="3" fill="#fff"></circle>
               </svg>`,
        iconSize: [36, 36],
        iconAnchor: [18, 36],
        popupAnchor: [0, -36]
    });
};

const defaultIcon = createIcon('#3b82f6'); // Accent Primary
const selectedIcon = createIcon('#a78bfa'); // Accent Secondary

let currentLotId = null;
let pollInterval = null;
let parkingLotsData = {};

// Load Parking Lots GeoJSON
async function loadParkingLots() {
    try {
        const response = await fetch(`${API_BASE}/parking-lots/geojson`);
        if (!response.ok) throw new Error('Failed to fetch parking lots');
        const geojson = await response.json();

        L.geoJSON(geojson, {
            pointToLayer: function (feature, latlng) {
                const lotId = feature.properties.id;
                const marker = L.marker(latlng, { icon: defaultIcon });
                parkingLotsData[lotId] = { marker, feature };

                marker.on('click', () => {
                    selectLot(lotId);
                });

                // Add popup tooltip on hover
                marker.bindPopup(`<h3>${feature.properties.name}</h3><p>${feature.properties.type}</p>`);
                marker.on('mouseover', function (e) { this.openPopup(); });
                marker.on('mouseout', function (e) { this.closePopup(); });

                return marker;
            }
        }).addTo(map);

        // Auto-select first lot for demo purposes
        if (geojson.features && geojson.features.length > 0) {
            selectLot(geojson.features[0].properties.id);
        }

    } catch (error) {
        console.error('Error loading map data:', error);
    }
}

// Select a parking lot and start polling its spots
async function selectLot(lotId) {
    if (currentLotId === lotId) return;

    // Reset previous selection
    if (currentLotId && parkingLotsData[currentLotId]) {
        parkingLotsData[currentLotId].marker.setIcon(defaultIcon);
    }

    currentLotId = lotId;
    parkingLotsData[lotId].marker.setIcon(selectedIcon);

    // UI Update
    document.querySelector('.sidebar-empty').classList.add('hidden');
    document.getElementById('lot-details').classList.remove('hidden');

    const props = parkingLotsData[lotId].feature.properties;
    document.getElementById('lot-name').textContent = props.name;
    document.getElementById('lot-address').textContent = props.address || 'Address not available';
    document.getElementById('lot-type').textContent = props.type.replace('_', ' ');
    document.getElementById('lot-price').textContent = `$${props.hourlyRate}/hr`;

    // Clear old polling
    if (pollInterval) clearInterval(pollInterval);

    // Fetch immediately, then poll
    await fetchSpots(lotId);
    pollInterval = setInterval(() => fetchSpots(lotId), 2000); // 2 second polling
}

// Fetch spots for the selected lot
async function fetchSpots(lotId) {
    try {
        const response = await fetch(`${API_BASE}/parking-lots/${lotId}/spots`);
        if (!response.ok) throw new Error('Failed to fetch spots');
        const data = await response.json();

        renderSpots(data);
    } catch (error) {
        console.error('Error fetching spots:', error);
    }
}

// Render the spots grid
function renderSpots(spots) {
    const grid = document.getElementById('spots-grid');
    grid.innerHTML = ''; // clear

    let freeCount = 0;
    let occupiedCount = 0;

    // Sort spots by code (A-1, A-2, etc)
    spots.sort((a, b) => a.code.localeCompare(b.code, undefined, { numeric: true }));

    spots.forEach(spot => {
        const div = document.createElement('div');
        div.className = `spot-item ${spot.physicalStatus}`;
        div.textContent = spot.code;
        div.title = `Type: ${spot.type} | Last Update: ${new Date(spot.lastSensorUpdate).toLocaleTimeString()}`;

        if (spot.physicalStatus === 'FREE') {
            div.addEventListener('click', () => openReservationModal(spot));
        }

        grid.appendChild(div);

        if (spot.physicalStatus === 'FREE') freeCount++;
        else if (spot.physicalStatus === 'OCCUPIED') occupiedCount++;
    });

    // Update summary counts
    document.getElementById('count-free').textContent = freeCount;
    document.getElementById('count-occupied').textContent = occupiedCount;
}

// Reservation Logic
const modal = document.getElementById('reservation-modal');
const closeBtn = document.getElementById('close-modal');
const form = document.getElementById('reservation-form');
const errorMsg = document.getElementById('reservation-error');

function openReservationModal(spot) {
    document.getElementById('reserve-spot-id').value = spot.id;
    document.getElementById('reserve-spot-code').textContent = spot.code;
    errorMsg.classList.add('hidden');
    form.reset();
    modal.classList.remove('hidden');
}

closeBtn.addEventListener('click', () => {
    modal.classList.add('hidden');
});

// Close modal when clicking outside
modal.addEventListener('click', (e) => {
    if (e.target === modal) {
        modal.classList.add('hidden');
    }
});

// Generate random idempotency key
const generateIdempotencyKey = () => {
    return 'idemp-' + Math.random().toString(36).substr(2, 9) + '-' + Date.now();
};

form.addEventListener('submit', async (e) => {
    e.preventDefault();
    errorMsg.classList.add('hidden');

    const spotId = document.getElementById('reserve-spot-id').value;
    const licensePlate = document.getElementById('reserve-plate').value.trim();
    const durationHours = parseInt(document.getElementById('reserve-duration').value);

    const now = new Date();
    // Add 1 minute buffer to prevent @FutureOrPresent validation failure on the backend
    const fromTimeObj = new Date(now.getTime() + 60 * 1000);
    const fromTime = fromTimeObj.toISOString();

    const toTimeObj = new Date(fromTimeObj.getTime() + durationHours * 60 * 60 * 1000);
    const toTime = toTimeObj.toISOString();

    const payload = {
        spotId: spotId,
        from: fromTime,
        to: toTime,
        licensePlate: licensePlate
    };

    try {
        const btn = document.getElementById('submit-reservation');
        btn.disabled = true;
        btn.textContent = 'Reserving...';

        const response = await fetch(`${API_BASE}/reservations`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Idempotency-Key': generateIdempotencyKey()
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const errData = await response.json().catch(() => ({}));
            throw new Error(errData.message || 'Failed to create reservation');
        }

        // Success
        modal.classList.add('hidden');
        alert(`Successfully reserved spot!`);

        // Refresh spots to see status
        if (currentLotId) {
            fetchSpots(currentLotId);
        }
    } catch (err) {
        errorMsg.textContent = err.message;
        errorMsg.classList.remove('hidden');
    } finally {
        const btn = document.getElementById('submit-reservation');
        btn.disabled = false;
        btn.textContent = 'Confirm Reservation';
    }
});

// Init
document.addEventListener('DOMContentLoaded', () => {
    loadParkingLots();
});
