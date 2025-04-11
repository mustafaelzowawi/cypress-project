package app.controller;

import app.model.Report;
import app.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    @Autowired
    private ReportRepository reportRepository;
    
    /**
     * Get all reports with optional filtering
     */
    @GetMapping("/reports")
    public ResponseEntity<?> getReports(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String timeframe,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String order) {
        
        List<Report> reports = reportRepository.findAll();
        
        // Apply category filter
        if (category != null && !category.isEmpty()) {
            reports = reports.stream()
                .filter(report -> category.equals(report.getCategory()))
                .collect(Collectors.toList());
        }
        
        // Apply timeframe filter
        if (timeframe != null && !timeframe.isEmpty()) {
            LocalDateTime cutoffDate = LocalDateTime.now();
            
            switch (timeframe) {
                case "day":
                    cutoffDate = cutoffDate.minus(1, ChronoUnit.DAYS);
                    break;
                case "week":
                    cutoffDate = cutoffDate.minus(7, ChronoUnit.DAYS);
                    break;
                case "month":
                    cutoffDate = cutoffDate.minus(30, ChronoUnit.DAYS);
                    break;
                case "year":
                    cutoffDate = cutoffDate.minus(365, ChronoUnit.DAYS);
                    break;
            }
            
            final LocalDateTime finalCutoffDate = cutoffDate;
            reports = reports.stream()
                .filter(report -> report.getTimestamp().isAfter(finalCutoffDate))
                .collect(Collectors.toList());
        }
        
        // Apply sorting
        if (sortBy != null && !sortBy.isEmpty()) {
            Comparator<Report> comparator = null;
            
            switch (sortBy) {
                case "date":
                    comparator = Comparator.comparing(Report::getTimestamp);
                    break;
                case "category":
                    comparator = Comparator.comparing(Report::getCategory);
                    break;
                case "address":
                    comparator = Comparator.comparing(Report::getAddress, 
                            Comparator.nullsLast(String::compareTo));
                    break;
            }
            
            if (comparator != null) {
                if ("asc".equalsIgnoreCase(order)) {
                    reports.sort(comparator);
                } else {
                    reports.sort(comparator.reversed());
                }
            }
        }
        
        return new ResponseEntity<>(reports, HttpStatus.OK);
    }
    
    /**
     * Get report statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        List<Report> allReports = reportRepository.findAll();
        
        // Total reports count
        int totalReports = allReports.size();
        
        // Reports by category
        Map<String, Long> reportsByCategory = allReports.stream()
                .collect(Collectors.groupingBy(
                        Report::getCategory,
                        Collectors.counting()
                ));
        
        // Reports in the last day/week/month
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayAgo = now.minus(1, ChronoUnit.DAYS);
        LocalDateTime weekAgo = now.minus(7, ChronoUnit.DAYS);
        LocalDateTime monthAgo = now.minus(30, ChronoUnit.DAYS);
        
        long reportsLastDay = allReports.stream()
                .filter(report -> report.getTimestamp().isAfter(dayAgo))
                .count();
        
        long reportsLastWeek = allReports.stream()
                .filter(report -> report.getTimestamp().isAfter(weekAgo))
                .count();
        
        long reportsLastMonth = allReports.stream()
                .filter(report -> report.getTimestamp().isAfter(monthAgo))
                .count();
        
        // Build the statistics response
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalReports", totalReports);
        statistics.put("reportsByCategory", reportsByCategory);
        statistics.put("reportsLastDay", reportsLastDay);
        statistics.put("reportsLastWeek", reportsLastWeek);
        statistics.put("reportsLastMonth", reportsLastMonth);
        
        return new ResponseEntity<>(statistics, HttpStatus.OK);
    }
}
