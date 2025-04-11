// Initialize the map on the "map" div with a center and zoom level
var map = L.map('map').setView([43.6532, -79.3832], 13); // Center on Toronto

// Add OpenStreetMap tiles
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
  maxZoom: 19,
  attribution: '© OpenStreetMap contributors'
}).addTo(map);

// Array to store all markers
var markers = [];

// Function to fetch and display all existing reports
function fetchAndDisplayReports() {
    fetch('http://localhost:8080/api/report') // Assuming the backend API endpoint for all reports
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to fetch reports');
            }
            return response.json();
        })
        .then(reports => {
            reports.forEach(report => {
                if (report.location) {
                    const [lat, lng] = report.location.split(',').map(parseFloat);
                    if (!isNaN(lat) && !isNaN(lng)) {
                        // Add a simple marker for existing reports
                        const existingMarker = L.marker([lat, lng]).addTo(map);
                        // Add a simple popup
                        existingMarker.bindPopup(`
                            <strong>Category:</strong> ${escapeHtml(report.category || 'N/A')}<br>
                            <strong>Description:</strong> ${escapeHtml(report.description || 'N/A')}<br>
                            <small>Reported: ${new Date(report.timestamp).toLocaleDateString()}</small>
                        `);
                        // Note: We aren't adding these to the 'markers' array used for click-based markers
                        // to avoid them being removed when a user clicks elsewhere.
                    }
                }
            });
        })
        .catch(error => {
            console.error('Error fetching existing reports:', error);
            // Notify the user that fetching failed
        });
}

// Function to escape HTML (to prevent XSS, similar to admin.js)
function escapeHtml(text) {
    if (!text) return '';
    var map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.toString().replace(/[&<>"']/g, function(m) { return map[m]; });
}

// Function to update coordinate display
function updateCoordinates(lat, lng) {
  document.getElementById('latDisplay').textContent = lat.toFixed(5);
  document.getElementById('lngDisplay').textContent = lng.toFixed(5);
}

// Function to remove a specific marker
function removeMarker(marker) {
  map.removeLayer(marker);
  // Remove from the markers array
  var index = markers.indexOf(marker);
  if (index > -1) {
    markers.splice(index, 1);
  }
}

// Function to navigate to report page with coordinates
function reportIssue(lat, lng) {
  console.log(`reportIssue called with lat: ${lat}, lng: ${lng}`);
  window.location.href = `report.html?lat=${lat.toFixed(5)}&lng=${lng.toFixed(5)}`;
}

// Listen for map clicks
map.on('click', function(e) {
  var lat = e.latlng.lat;
  var lng = e.latlng.lng;
  
  // Update display values
  updateCoordinates(lat, lng);

  // Create a div element for the popup content
  var popupNode = document.createElement('div');
  popupNode.innerHTML = `<p>Selected Location: ${lat.toFixed(5)}, ${lng.toFixed(5)}</p>`;

  // Create the report button
  var reportButton = document.createElement('button');
  reportButton.textContent = 'Report Issue';
  // Add event listener to the button
  reportButton.addEventListener('click', function() {
    reportIssue(lat, lng);
  });

  // Append the button to the popup content div
  popupNode.appendChild(reportButton);
  
  // Add a marker at the clicked location and store it in the markers array
  var marker = L.marker([lat, lng]).addTo(map)
    .bindPopup(popupNode) // Use the DOM node instead of the string
    .openPopup();
  
  
  // Add event to remove marker when popup is closed
  marker.on('popupclose', function() {
    removeMarker(marker);
  });
  
  // Store the marker in the array
  markers.push(marker);
});

// Fetch and display existing reports when the script loads
fetchAndDisplayReports();
