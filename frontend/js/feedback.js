document.addEventListener('DOMContentLoaded', function() {
    const feedbackForm = document.getElementById('feedbackForm');
    const feedbackResponse = document.getElementById('feedbackResponse');
    
    if (feedbackForm) {
        feedbackForm.addEventListener('submit', submitFeedback);
    }
});

// Submit feedback form
function submitFeedback(event) {
    event.preventDefault();
    
    // Get form data
    const formData = new FormData(event.target);
    const feedbackData = {};
    
    // Convert FormData to object
    for (let [key, value] of formData.entries()) {
        // Handle checkbox groups by collecting values into arrays
        if (key === 'usedFeatures') {
            if (!feedbackData[key]) {
                feedbackData[key] = [];
            }
            feedbackData[key].push(value);
        } else {
            feedbackData[key] = value;
        }
    }
    
    // Add timestamp
    feedbackData.timestamp = new Date().toISOString();
    
    // Send to backend
    sendFeedbackToBackend(feedbackData);
    
    // Show initial success message
    showResponseMessage('Submitting your feedback...', 'info');
    
    // Reset form
    event.target.reset();
}

// Send feedback to backend
function sendFeedbackToBackend(feedbackData) {
    fetch('/api/feedback', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(feedbackData)
    })
    .then(response => {
        if (!response.ok) {
            // Error handling
            return response.text().then(text => { throw new Error('Server error: ' + text) });
        }
        return response.json();
    })
    .then(data => {
        console.log('Feedback submitted successfully:', data);
        // Show success message only after backend confirmation
        showResponseMessage('Thank you for your feedback! It has been received.', 'success'); 
    })
    .catch(error => {
        console.error('Error submitting feedback:', error);
        // Updated message to reflect data loss on error
        showResponseMessage('Failed to submit feedback. Please try again later.', 'error'); 
    });
}

// Display response message to user
function showResponseMessage(message, type) {
    const responseElement = document.getElementById('feedbackResponse');
    if (responseElement) {
        responseElement.textContent = message;

        // Add 'info' and 'error' classes
        responseElement.className = `feedback-response ${type}`; 
        responseElement.style.display = 'block';
        
        // Scroll to message
        responseElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
        
        // Hide after 5 seconds
        const duration = (type === 'error') ? 8000 : 5000;
        setTimeout(() => {
            responseElement.style.display = 'none';
        }, duration);
    }
} 