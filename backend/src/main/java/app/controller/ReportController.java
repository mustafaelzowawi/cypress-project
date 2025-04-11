package app.controller;

import app.model.Report;
import app.service.GeocodingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import app.repository.ReportRepository;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.exception.GenericJDBCException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.client.HttpClientErrorException;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private ReportRepository reportRepository;
    
    @Autowired
    private GeocodingService geocodingService;

    // A RestTemplate to make HTTP calls to the Go service.
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping()
    public ResponseEntity<List<Report>> getAllReports() {
        logger.info("Received request to get all reports");
        List<Report> reports = reportRepository.findAll();
        return new ResponseEntity<>(reports, HttpStatus.OK);
    }

    @PostMapping()
    public ResponseEntity<?> createReport(@Valid @RequestBody Report report, BindingResult bindingResult) {
        logger.info("Received report submission: {}", report);
        
        // Check for validation errors
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = bindingResult.getFieldErrors().stream()
                .collect(Collectors.toMap(
                    FieldError::getField,
                    FieldError::getDefaultMessage
                ));
            logger.error("Validation errors: {}", errors);
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }
        
        // Additional validation for the location format
        String[] coordinates = report.getLocation().split(",");
        if (coordinates.length == 2) {
            try {
                double lat = Double.parseDouble(coordinates[0]);
                double lng = Double.parseDouble(coordinates[1]);
                
                // Basic range check for coordinates
                if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                    logger.error("Coordinates out of range: lat={}, lng={}", lat, lng);
                    return new ResponseEntity<>("Coordinates out of valid range", HttpStatus.BAD_REQUEST);
                }
                
                logger.info("Parsed coordinates: lat={}, lng={}", lat, lng);
                
                // Sanitize inputs to replace any potentially dangerous characters
                String sanitizedDescription = sanitizeInput(report.getDescription());
                report.setDescription(sanitizedDescription);
                
                String sanitizedCategory = sanitizeInput(report.getCategory());
                report.setCategory(sanitizedCategory);
                
                // Use the geocoding service to get the address for display purposes,
                // but the duplicate detection will rely on proximity and description similarity
                String address = geocodingService.getAddress(lat, lng);
                logger.info("Geocoded address: {}", address);
                
                // Create payload for duplicate detection
                Map<String, Object> payload = new HashMap<>();
                payload.put("description", report.getDescription());
                payload.put("location", report.getLocation());
                payload.put("category", report.getCategory());
                payload.put("address", address);
                
                // Check for duplicates using the Go microservice.
                String goServiceUrl = "http://localhost:8081/detect";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

                ResponseEntity<String> responseEntity = null; // Initialize to null
                boolean isDuplicate = false;

                try {
                    logger.info("Checking for duplicates via microservice");
                    responseEntity = restTemplate.postForEntity(goServiceUrl, requestEntity, String.class);
                    logger.info("Duplicate check response: {}", responseEntity.getStatusCode());
                    // If successful and status is OK, it's not a duplicate
                    isDuplicate = (responseEntity.getStatusCode() == HttpStatus.CONFLICT);

                } catch (HttpClientErrorException ex) {
                    // Specifically catch 4xx errors from RestTemplate
                    logger.warn("HTTP Client Error checking for duplicates: Status={}, Body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
                    if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                        // Go service correctly identified a duplicate
                        isDuplicate = true;
                    } else {
                        // Different client error (e.g., 400 Bad Request from Go service)
                        // Treat as an internal error for now, don't save report
                         return new ResponseEntity<>("Error during duplicate check: " + ex.getStatusCode(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                } catch (Exception e) {
                    // Catch other errors (Go service down)
                    logger.error("Error connecting to duplicate check service", e);
                    // Return an error if checker is down.
                    return new ResponseEntity<>("Duplicate check service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
                }

                // Check the flag determined in the try/catch blocks
                if (isDuplicate) {
                    logger.info("Duplicate report detected");
                    return new ResponseEntity<>("Duplicate report detected in the vicinity", HttpStatus.CONFLICT);
                }
                
                // If not a duplicate, proceed to save
                report.setAddress(address);
                report.setTimestamp(java.time.LocalDateTime.now());
                
                try {
                    logger.info("Attempting to save report to database");
                    Report savedReport = reportRepository.save(report);
                    logger.info("Report saved successfully with ID: {}", savedReport.getId());
                    return new ResponseEntity<>(savedReport, HttpStatus.CREATED);
                } catch (Exception e) {
                    logger.error("Database error while saving report", e);
                    if (e.getCause() instanceof GenericJDBCException) {
                        logger.error("SQL Error: {}", ((GenericJDBCException) e.getCause()).getSQLException().getMessage());
                    }
                    return new ResponseEntity<>("Database error: " + e.getMessage(), 
                                               HttpStatus.INTERNAL_SERVER_ERROR);
                }
                
            } catch (NumberFormatException e) {
                logger.error("Invalid coordinates format: {}", report.getLocation(), e);
                return new ResponseEntity<>("Invalid coordinates format", HttpStatus.BAD_REQUEST);
            } catch (Exception e) {
                logger.error("Unexpected error processing report", e);
                return new ResponseEntity<>("Error processing report: " + e.getMessage(), 
                                           HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            logger.error("Invalid location format: {}", report.getLocation());
            return new ResponseEntity<>("Invalid location format", HttpStatus.BAD_REQUEST);
        }
    }
    
    // Helper method to sanitize user inputs
    private String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove any HTML/script tags that could be used for attacks
        String sanitized = input.replaceAll("<[^>]*>", "");
        
        // Replace other potentially dangerous characters
        sanitized = sanitized.replaceAll("\\\\", "\\\\\\\\")
                             .replaceAll("'", "\\\\'")
                             .replaceAll("\"", "\\\\\"");
        
        return sanitized;
    }
    
    // Global exception handler for validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage
            ));
        
        logger.error("Validation errors in request: {}", errors);
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }
}
