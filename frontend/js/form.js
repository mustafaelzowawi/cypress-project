// Get URL parameters for lat/lng when page loads
document.addEventListener('DOMContentLoaded', function() {
  const urlParams = new URLSearchParams(window.location.search);
  const lat = urlParams.get('lat');
  const lng = urlParams.get('lng');
  
  if (lat && lng) {
    // Validate coordinates
    if (isValidCoordinate(lat, -90, 90) && isValidCoordinate(lng, -180, 180)) {
      document.getElementById('latitude').value = lat;
      document.getElementById('longitude').value = lng;
      
      // Also display the coordinates to the user
      const coordInfo = document.createElement('div');
      coordInfo.innerHTML = `<p>Reporting issue at location: ${escapeHtml(lat)}, ${escapeHtml(lng)}</p>`;
      document.getElementById('reportForm').prepend(coordInfo);
    } else {
      showError('Invalid coordinates provided');
    }
  } else {
    showError('No location coordinates provided');
  }
  
  // Create a response message container
  const responseMessage = document.createElement('div');
  responseMessage.id = 'responseMessage';
  responseMessage.style.display = 'none';
  responseMessage.style.padding = '10px';
  responseMessage.style.marginTop = '10px';
  responseMessage.style.borderRadius = '5px';
  document.getElementById('reportForm').after(responseMessage);
});

// Function to validate coordinates
function isValidCoordinate(value, min, max) {
  const num = parseFloat(value);
  return !isNaN(num) && num >= min && num <= max;
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

// Function to show error messages
function showError(message) {
  const responseMessage = document.getElementById('responseMessage');
  if (responseMessage) {
    responseMessage.style.display = 'block';
    responseMessage.style.backgroundColor = '#f8d7da';
    responseMessage.textContent = message;
  } else {
    alert(message);
  }
}

// Function to validate form inputs
function validateForm() {
  const description = document.getElementById('description').value.trim();
  const category = document.getElementById('category').value;
  const latitude = document.getElementById('latitude').value;
  const longitude = document.getElementById('longitude').value;
  
  // Validate description
  if (!description) {
    showError('Description is required');
    return false;
  }
  
  if (description.length < 10 || description.length > 1000) {
    showError('Description must be between 10 and 1000 characters');
    return false;
  }
  
  // Validate category
  if (!category) {
    showError('Category is required');
    return false;
  }
  
  // Validate coordinates
  if (!latitude || !longitude) {
    showError('Location coordinates are required');
    return false;
  }
  
  if (!isValidCoordinate(latitude, -90, 90) || !isValidCoordinate(longitude, -180, 180)) {
    showError('Invalid location coordinates');
    return false;
  }
  
  return true;
}

document.getElementById('reportForm').addEventListener('submit', function(e) {
    e.preventDefault();
    
    // Validate form inputs before submission
    if (!validateForm()) {
      return;
    }
    
    // Retrieve form values
    const description = document.getElementById('description').value.trim();
    const category = document.getElementById('category').value;
    const latitude = document.getElementById('latitude').value;
    const longitude = document.getElementById('longitude').value;
    
    // Build payload: combining latitude and longitude into one "location" field
    const payload = {
      description: description,
      category: category,
      location: latitude + ',' + longitude
    };
    
    // Show loading message
    const responseMessage = document.getElementById('responseMessage');
    responseMessage.style.display = 'block';
    responseMessage.style.backgroundColor = '#f0f0f0';
    responseMessage.textContent = 'Submitting report...';
    
    console.log('Sending payload:', payload);
    
    // Send the report to the backend
    fetch('http://localhost:8080/api/report', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    })
    .then(response => {
      console.log('Response status:', response.status);
      
      if (response.status === 409) {
        // Handle duplicate report
        responseMessage.style.backgroundColor = '#fff3cd';
        responseMessage.textContent = 'Duplicate report detected in the vicinity. Your issue may have already been reported.';
        return Promise.reject('Duplicate report');
      }
      
      if (!response.ok) {
        return response.text().then(text => {
          console.error('Error response:', text);
          throw new Error(text || 'Failed to submit report');
        });
      }
      
      return response.json();
    })
    .then(data => {
      console.log('Success data:', data);
      // Success response
      responseMessage.style.backgroundColor = '#d4edda';
      responseMessage.textContent = "Report submitted successfully!";
      
      // Clear the form after successful submission
      document.getElementById('reportForm').reset();
      
      // Redirect back to map page after 2 seconds
      setTimeout(() => {
        window.location.href = "index.html";
      }, 2000);
    })
    .catch(error => {
      console.error("Error:", error);

      // Check if it's the specific duplicate error we rejected
      // If so, the message and style were already set, so do nothing here.
      if (error === 'Duplicate report') {
        return; 
      }

      // For all other errors, display an error message
      responseMessage.style.backgroundColor = '#f8d7da'; // Red error background
      responseMessage.textContent = error.message || "Error submitting report.";
    });
  });
  