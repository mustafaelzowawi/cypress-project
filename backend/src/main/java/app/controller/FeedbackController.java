package app.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api")
public class FeedbackController {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackController.class);
    private static final String FEEDBACK_DIR = "data/feedback/";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Store list of feedback in memory
    private List<Map<String, Object>> feedbackList = new ArrayList<>();
    
    /**
     * Submit user feedback
     */
    @PostMapping("/feedback")
    public ResponseEntity<?> submitFeedback(@RequestBody Map<String, Object> feedback) {
        logger.info("Received feedback submission");
        
        try {
            // Add timestamp if not provided
            if (!feedback.containsKey("timestamp")) {
                feedback.put("timestamp", new Date());
            }
            
            // Store feedback in memory
            feedbackList.add(feedback);
            
            // Store feedback to file system
            saveFeedbackToFile(feedback);
            
            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Feedback submitted successfully");
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
            
        } catch (Exception e) {
            logger.error("Error processing feedback", e);
            return new ResponseEntity<>("Error processing feedback: " + e.getMessage(), 
                                       HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Save feedback to file system
     */
    private void saveFeedbackToFile(Map<String, Object> feedback) {
        try {
            // Create directory if it doesn't exist
            File directory = new File(FEEDBACK_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            // Generate filename with timestamp
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = dateFormat.format(new Date());
            String filename = FEEDBACK_DIR + "feedback_" + timestamp + ".json";
            
            // Write feedback data to file
            File file = new File(filename);
            FileWriter writer = new FileWriter(file);
            writer.write(objectMapper.writeValueAsString(feedback));
            writer.close();
            
            logger.info("Feedback saved to file: {}", filename);
            
        } catch (IOException e) {
            logger.error("Error saving feedback to file", e);
        }
    }
} 