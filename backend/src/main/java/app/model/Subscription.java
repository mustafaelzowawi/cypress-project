package app.model;

import javax.persistence.*;

@Entity
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // User's email address for notifications.
    private String email;
    
    // Report ID that the user has subscribed to
    private Long reportId;

    // Constructors
    public Subscription() {}

    public Subscription(String email) {
        this.email = email;
    }
    
    public Subscription(String email, Long reportId) {
        this.email = email;
        this.reportId = reportId;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }
  
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Long getReportId() {
        return reportId;
    }
    
    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }
}
