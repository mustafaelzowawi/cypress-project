package app.controller;

import app.model.Subscription;
import app.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api")
public class SubscriptionController {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @PostMapping("/subscribe")
    public ResponseEntity<String> subscribe(@RequestBody Subscription subscription) {
        if (subscription.getEmail() == null || subscription.getReportId() == null) {
            return new ResponseEntity<>("Email and report ID are required", HttpStatus.BAD_REQUEST);
        }
        
        Optional<Subscription> existingSubscription = subscriptionRepository.findByEmailAndReportId(subscription.getEmail(), subscription.getReportId());
        
        if (existingSubscription.isPresent()) {
            return new ResponseEntity<>("You are already subscribed to this report.", HttpStatus.CONFLICT);
        }
        
        try {
            subscriptionRepository.save(subscription);
            return new ResponseEntity<>("Subscription successful", HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("Error saving subscription: " + e.getMessage(), 
                                      HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/subscriptions/{reportId}")
    public ResponseEntity<?> getSubscriptionsByReport(@PathVariable Long reportId) {
        try {
            return new ResponseEntity<>(subscriptionRepository.findByReportId(reportId), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving subscriptions: " + e.getMessage(), 
                                      HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
