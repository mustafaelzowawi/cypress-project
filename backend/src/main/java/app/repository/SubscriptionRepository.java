package app.repository;

import app.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    // Find all subscriptions for a given report ID
    List<Subscription> findByReportId(Long reportId);
    
    // Find a specific subscription by email and report ID
    Optional<Subscription> findByEmailAndReportId(String email, Long reportId);
} 