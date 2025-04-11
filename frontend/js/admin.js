// Initialize the map
let map = L.map('map').setView([37.7749, -122.4194], 12);

// Add tile layer (OpenStreetMap)
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
}).addTo(map);

// Store markers references to manage them
let markers = [];

// Chart instance
let categoryChart;

// Fetch statistics and update the dashboard
function fetchStatistics() {
    fetch('http://localhost:8080/api/admin/statistics')
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to fetch statistics');
            }
            return response.json();
        })
        .then(data => {
            // Update summary statistics
            document.getElementById('totalReports').textContent = data.totalReports;
            document.getElementById('reportsToday').textContent = data.reportsLastDay;
            document.getElementById('reportsWeek').textContent = data.reportsLastWeek;
            document.getElementById('reportsMonth').textContent = data.reportsLastMonth;
            
            // Update category chart
            updateCategoryChart(data.reportsByCategory);
        })
        .catch(error => {
            console.error('Error fetching statistics:', error);
            alert('Failed to load statistics. Please try again later.');
        });
}

// Update the category chart with data
function updateCategoryChart(categoryData) {
    const ctx = document.getElementById('categoryChart').getContext('2d');
    
    // Prepare data for Chart.js
    const labels = Object.keys(categoryData);
    const values = Object.values(categoryData);
    
    // Define colors for categories
    const backgroundColors = [
        'rgba(75, 192, 192, 0.6)',
        'rgba(255, 99, 132, 0.6)',
        'rgba(255, 205, 86, 0.6)',
        'rgba(54, 162, 235, 0.6)',
        'rgba(153, 102, 255, 0.6)'
    ];
    
    // If chart already exists, destroy it before creating a new one
    if (categoryChart) {
        categoryChart.destroy();
    }
    
    // Create pie chart
    categoryChart = new Chart(ctx, {
        type: 'pie',
        data: {
            labels: labels,
            datasets: [{
                data: values,
                backgroundColor: backgroundColors.slice(0, labels.length),
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'right'
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const label = context.label || '';
                            const value = context.raw || 0;
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = Math.round((value / total) * 100);
                            return `${label}: ${value} (${percentage}%)`;
                        }
                    }
                }
            }
        }
    });
}

// Fetch reports and update the table and map
function fetchReports(filters = {}) {
    // Build query parameters
    let queryParams = new URLSearchParams();
    
    if (filters.category) {
        queryParams.append('category', filters.category);
    }
    
    if (filters.timeframe) {
        queryParams.append('timeframe', filters.timeframe);
    }
    
    if (filters.sortBy) {
        queryParams.append('sortBy', filters.sortBy);
    }
    
    if (filters.order) {
        queryParams.append('order', filters.order);
    }

    
    // Fetch reports with filters
    fetch(`http://localhost:8080/api/admin/reports?${queryParams.toString()}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to fetch reports');
            }
            return response.json();
        })
        .then(reports => {
            updateReportsTable(reports);
            updateReportsMap(reports);
        })
        .catch(error => {
            console.error('Error fetching reports:', error);
            alert('Failed to load reports. Please try again later.');
        });
}

// Update the reports table with fetched data
function updateReportsTable(reports) {
    const tableBody = document.getElementById('reportsTableBody');
    tableBody.innerHTML = '';
    
    if (reports.length === 0) {
        const row = document.createElement('tr');
        row.innerHTML = '<td colspan="5" style="text-align: center;">No reports found</td>';
        tableBody.appendChild(row);
        return;
    }
    
    reports.forEach(report => {
        const row = document.createElement('tr');
        
        // Format date for display
        const date = new Date(report.timestamp);
        const formattedDate = date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
        
        row.innerHTML = `
            <td>${report.id}</td>
            <td>${escapeHtml(report.description)}</td>
            <td>${escapeHtml(report.category)}</td>
            <td>${escapeHtml(report.address || 'Unknown')}</td>
            <td>${formattedDate}</td>
        `;
        
        tableBody.appendChild(row);
    });
}

// Update the map with report markers
function updateReportsMap(reports) {
    reports.forEach(report => {
        if (report.location) {
            const [lat, lng] = report.location.split(',').map(parseFloat);
            
            if (!isNaN(lat) && !isNaN(lng)) {
                // Create marker
                const marker = L.marker([lat, lng]).addTo(map);
                
                // Add popup with report info
                marker.bindPopup(`
                    <strong>ID:</strong> ${report.id}<br>
                    <strong>Category:</strong> ${escapeHtml(report.category)}<br>
                    <strong>Description:</strong> ${escapeHtml(report.description)}<br>
                    <strong>Address:</strong> ${escapeHtml(report.address || 'Unknown')}<br>
                    <strong>Date:</strong> ${new Date(report.timestamp).toLocaleString()}
                `);
                
                // Store marker reference
                markers.push(marker);
            }
        }
    });
    
    // Adjust map view to include all markers if any exist
    if (markers.length > 0) {
        const group = new L.featureGroup(markers);
        map.fitBounds(group.getBounds().pad(0.1));
    }
}


// Function to escape HTML to prevent XSS
function escapeHtml(text) {
    if (!text) return '';
    return text
        .toString()
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

// Event listeners
document.addEventListener('DOMContentLoaded', function() {
    // Fetch initial data
    fetchStatistics();
    fetchReports();
    
    // Apply filters button click handler
    document.getElementById('applyFilters').addEventListener('click', function() {
        const filters = {
            category: document.getElementById('categoryFilter').value,
            timeframe: document.getElementById('timeframeFilter').value,
            sortBy: document.getElementById('sortByFilter').value,
            order: document.getElementById('orderFilter').value
        };
        
        fetchReports(filters);
    });
    
    // Set up periodic refresh for real-time updates (every 60 seconds)
    setInterval(function() {
        fetchStatistics();
        
        // Re-fetch reports with current filters
        const filters = {
            category: document.getElementById('categoryFilter').value,
            timeframe: document.getElementById('timeframeFilter').value,
            sortBy: document.getElementById('sortByFilter').value,
            order: document.getElementById('orderFilter').value
        };
        
        fetchReports(filters);
    }, 60000);
}); 