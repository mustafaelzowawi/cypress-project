document.addEventListener('DOMContentLoaded', function() {
    const container = document.getElementById('report-container');
    container.innerHTML = "Loading reports...";
    
    console.log("Fetching reports from API...");
    
    // Fetch the list of reports from the backend endpoint: GET /api/reports
    fetch('/api/report')
      .then(response => {
        
        console.log("API Response status:", response.status);
        if (!response.ok) {
          throw new Error("Network response was not ok " + response.statusText);
        }
        return response.json();
      })
      .then(data => {
        console.log("API Response data:", data);
        
        if (!data || data.length === 0 || data.every(item => item === null)) {
          container.innerHTML = "No reports found. The database may be empty.";
          return;
        }
        
        // Filter out null values
        const reports = data.filter(report => report !== null);
        
        if (reports.length === 0) {
          container.innerHTML = "No valid reports found.";
          return;
        }
        
        // Build an HTML list of reports
        let html = '<ul>';
        reports.forEach(report => {
          html += `<li>
                      <strong>ID:</strong> ${report.id} <br>
                      <strong>Category:</strong> ${report.category} <br>
                      <strong>Description:</strong> ${report.description} <br>
                      <strong>Address:</strong> ${report.address || 'Unknown'} <br>
                      <strong>Timestamp:</strong> ${report.timestamp || 'Unknown'} <br>
                      <div class="subscription-form">
                        <input type="email" id="email-${report.id}" placeholder="Enter your email">
                        <button onclick="subscribeToReport(${report.id})">Subscribe for updates</button>
                        <div id="success-${report.id}" class="subscription-success">Successfully subscribed!</div>
                        <div id="error-${report.id}" class="subscription-error">Error subscribing. Please try again.</div>
                      </div>
                   </li>`;
        });
        html += '</ul>';
        container.innerHTML = html;
      })
      .catch(error => {
        console.error("Error fetching reports:", error);
        container.innerHTML = "Error loading reports: " + error.message;
      });
  });
  
// Function to handle report subscription
function subscribeToReport(reportId) {
  const emailInput = document.getElementById(`email-${reportId}`);
  const successMessage = document.getElementById(`success-${reportId}`);
  const errorMessage = document.getElementById(`error-${reportId}`);
  
  // Reset messages
  successMessage.style.display = 'none';
  errorMessage.style.display = 'none';
  
  const email = emailInput.value.trim();
  if (!email) {
    errorMessage.textContent = 'Please enter an email address';
    errorMessage.style.display = 'block';
    return;
  }
  
  // Validate email format
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) {
    errorMessage.textContent = 'Please enter a valid email address';
    errorMessage.style.display = 'block';
    return;
  }
  
  // Create subscription data
  const subscriptionData = {
    email: email,
    reportId: reportId
  };
  
  // Send subscription to server
  fetch('/api/subscribe', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(subscriptionData)
  })
  .then(response => {
    if (response.status === 409) { // Handle conflict (already subscribed)
        return response.text().then(text => {
            throw new Error(text || 'You are already subscribed to this report.'); // Throw error with server message or default
        });
    } else if (!response.ok) {
      // Handle other non-OK responses (like 500 Internal Server Error)
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    // If response is OK (201 Created), proceed to get text
    return response.text(); 
  })
  .then(data => {
    // This block now only runs for successful subscriptions (201)
    console.log('Subscription successful:', data);
    successMessage.style.display = 'block';
    errorMessage.style.display = 'none'; // Ensure error message is hidden on success
    emailInput.value = ''; // Clear input field on success
  })
  .catch(error => {
    console.error('Error subscribing to report:', error);
    errorMessage.textContent = error.message; // Use the error message thrown (either from 409 or other errors)
    errorMessage.style.display = 'block';
    successMessage.style.display = 'none'; // Ensure success message is hidden on error
  });
}
  