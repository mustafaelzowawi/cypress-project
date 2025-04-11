package main

import (
	"database/sql"
	"encoding/json"
	"log"
	"math"
	"net/http"
	"strconv"
	"strings"
	_ "github.com/mattn/go-sqlite3" // SQLite driver for go
)

// EnhancedReport represents a report with coordinates and description.
type EnhancedReport struct {
	ID          int     `json:"id,omitempty"` // ID not used in duplicate check
	Description string  `json:"description,omitempty"`
	Location    string  `json:"location"`            // "lat,lng"
	Address     string  `json:"address"`             // Address not used in duplicate check
	Timestamp   string  `json:"timestamp,omitempty"` // Timestamp not used in duplicate check
	Latitude    float64 `json:"-"`                   // Parsed from Location
	Longitude   float64 `json:"-"`                   // Parsed from Location
}

var (
	// Database connection pool
	db *sql.DB

	// Maximum distance in meters to consider reports as duplicates
	maxDistanceThreshold = 100.0

	// Text similarity threshold (if descriptions are similar)
	descriptionSimilarityThreshold = 0.5
)

// haversineDistance calculates the distance between two points on earth in meters.
func haversineDistance(lat1, lon1, lat2, lon2 float64) float64 {
	// Convert latitude and longitude from degrees to radians
	lat1 = lat1 * math.Pi / 180
	lon1 = lon1 * math.Pi / 180
	lat2 = lat2 * math.Pi / 180
	lon2 = lon2 * math.Pi / 180

	// Haversine formula
	dLat := lat2 - lat1
	dLon := lon2 - lon1
	a := math.Sin(dLat/2)*math.Sin(dLat/2) + math.Cos(lat1)*math.Cos(lat2)*math.Sin(dLon/2)*math.Sin(dLon/2)
	c := 2 * math.Atan2(math.Sqrt(a), math.Sqrt(1-a))

	// Earth radius in meters
	earthRadius := 6371000.0

	// Distance in meters
	return earthRadius * c
}

// textSimilarity calculates a simple similarity score between two descriptions
func textSimilarity(desc1, desc2 string) float64 {
	// Convert to lowercase for case-insensitive comparison
	desc1 = strings.ToLower(desc1)
	desc2 = strings.ToLower(desc2)

	// Simple word-based Jaccard similarity
	words1 := strings.Fields(desc1)
	words2 := strings.Fields(desc2)

	// Create sets of words
	set1 := make(map[string]bool)
	set2 := make(map[string]bool)

	for _, word := range words1 {
		set1[word] = true
	}

	for _, word := range words2 {
		set2[word] = true
	}

	// Calculate intersection and union sizes
	intersection := 0
	for word := range set1 {
		if set2[word] {
			intersection++
		}
	}

	union := len(set1) + len(set2) - intersection

	if union == 0 {
		// Handle case where both descriptions are empty or identical after processing
		if desc1 == desc2 {
			return 1.0 // Identical descriptions are 100% similar
		}
		return 0.0 // Otherwise, similarity is 0 if union is 0
	}

	return float64(intersection) / float64(union)
}

// parseLocation splits "lat,lng" string and returns floats or error
func parseLocation(location string) (float64, float64, error) {
	coordinates := strings.Split(location, ",")
	if len(coordinates) != 2 {
		return 0, 0, &strconv.NumError{Func: "ParseFloat", Num: location, Err: strconv.ErrSyntax}
	}

	lat, err1 := strconv.ParseFloat(strings.TrimSpace(coordinates[0]), 64)
	lng, err2 := strconv.ParseFloat(strings.TrimSpace(coordinates[1]), 64)

	if err1 != nil {
		return 0, 0, err1
	}
	if err2 != nil {
		return 0, 0, err2
	}
	return lat, lng, nil
}

// duplicateHandler checks if a report is a duplicate by querying the database.
func duplicateHandler(w http.ResponseWriter, r *http.Request) {
	var newReport EnhancedReport
	if err := json.NewDecoder(r.Body).Decode(&newReport); err != nil {
		http.Error(w, "Invalid request payload", http.StatusBadRequest)
		return
	}

	// Parse coordinates from the location string for the new report
	lat, lng, err := parseLocation(newReport.Location)
	if err != nil {
		log.Printf("Error parsing new report location '%s': %v", newReport.Location, err)
		http.Error(w, "Invalid location format", http.StatusBadRequest)
		return
	}
	newReport.Latitude = lat
	newReport.Longitude = lng

	// Query the database for existing reports
	rows, err := db.Query("SELECT description, location FROM report")
	if err != nil {
		log.Printf("Error querying database: %v", err)
		http.Error(w, "Internal server error during duplicate check", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	// Check for duplicates based on proximity and description similarity
	for rows.Next() {
		var existingDescription, existingLocation string
		if err := rows.Scan(&existingDescription, &existingLocation); err != nil {
			log.Printf("Error scanning row: %v", err)
			continue // Skip this row if scanning fails
		}

		// Parse coordinates for the existing report
		existingLat, existingLng, parseErr := parseLocation(existingLocation)
		if parseErr != nil {
			log.Printf("Error parsing existing location '%s': %v. Skipping comparison.", existingLocation, parseErr)
			continue // Skip comparison if location is invalid
		}

		// Calculate distance between reports
		distance := haversineDistance(
			newReport.Latitude, newReport.Longitude,
			existingLat, existingLng,
		)

		// If within distance threshold, check description similarity
		if distance <= maxDistanceThreshold {
			descSimilarity := textSimilarity(newReport.Description, existingDescription)
			// If either very close in distance or similar in description
			if distance <= maxDistanceThreshold/2 || descSimilarity >= descriptionSimilarityThreshold {
				w.WriteHeader(http.StatusConflict)
				w.Write([]byte("Duplicate report detected"))
				log.Printf("Duplicate found based on DB: distance=%.2fm, similarity=%.2f. New: '%s' @ %f,%f. Existing: '%s' @ %f,%f",
					distance, descSimilarity, newReport.Description, newReport.Latitude, newReport.Longitude, existingDescription, existingLat, existingLng)
				return // Found duplicate, stop checking
			}
		}
	}

	if err = rows.Err(); err != nil {
		log.Printf("Error iterating through rows: %v", err)
	}

	// If no duplicate is found after checking all DB rows
	w.WriteHeader(http.StatusOK)
	w.Write([]byte("No duplicate found"))
	log.Printf("No duplicate found in DB for location: %s", newReport.Location)
}

func main() {
	var err error
	// Connect to SQLite database
	dbPath := "../../backend/cypress.db"
	db, err = sql.Open("sqlite3", dbPath)
	if err != nil {
		log.Fatalf("Failed to open database connection to %s: %v", dbPath, err)
	}
	defer db.Close()

	// Check if the connection is working
	if err = db.Ping(); err != nil {
		log.Fatalf("Failed to ping database %s: %v", dbPath, err)
	}

	log.Printf("Successfully connected to SQLite database: %s", dbPath)

	http.HandleFunc("/detect", duplicateHandler)
	log.Println("Go Duplicate Detection Service running on port 8081...")
	log.Fatal(http.ListenAndServe(":8081", nil))
}
