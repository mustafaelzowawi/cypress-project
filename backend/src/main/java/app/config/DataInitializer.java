package app.config;

import app.model.Report;
import app.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Random;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private ReportRepository reportRepository;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Deleting existing reports...");
        reportRepository.deleteAll();
        logger.info("Existing reports deleted.");

        logger.info("Seeding database with sample reports...");

        Random random = new Random();
        String[] categories = {"Pothole", "Graffiti", "Streetlight_Out", "Trash_Overflow", "Damaged_Sign"};
        String[] descriptions = {
            "Deep pothole reported on the main road near the intersection.",
            "Graffiti spray-painted on the park bench, needs removal.",
            "Streetlight is flickering and needs urgent repair.",
            "Public trash bin is overflowing with garbage.",
            "Stop sign at the corner is bent and difficult to see.",
            "Large crack in the pavement requires attention.",
            "Offensive graffiti found on the wall near the school.",
            "Streetlight completely out on a busy street.",
            "Illegal dumping of trash behind the community center.",
            "Yield sign is missing from the junction."
        };
        // Base Toronto coordinates
        double baseLat = 43.6532;
        double baseLng = -79.3832;

        // Sample Size
        int sample_size = 100;

        for (int i = 0; i < sample_size; i++) {
            Report report = new Report();
            // Add slight random variation to coordinates
            double lat = baseLat + (random.nextDouble() - 0.5) * 0.05;
            double lng = baseLng + (random.nextDouble() - 0.5) * 0.1;
            
            report.setLocation(String.format("%.5f,%.5f", lat, lng));
            report.setCategory(categories[random.nextInt(categories.length)]);
            report.setDescription(descriptions[i % descriptions.length] + " (Sample " + (i+1) + ")");
            // Set timestamp slightly in the past
            report.setTimestamp(LocalDateTime.now().minusDays(random.nextInt(30)).minusHours(random.nextInt(24))); 
            // Optionally set a simple address
            report.setAddress("Approx. Location " + (i+1) + ", Toronto, ON"); 

            logger.debug("Saving report: {}", report);
            reportRepository.save(report);
        }

        logger.info(String.format("Successfully seeded %d sample reports.", sample_size));
    }
} 