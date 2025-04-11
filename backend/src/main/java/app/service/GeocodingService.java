package app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GeocodingService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converts latitude and longitude coordinates into a human-readable address using OpenStreetMap's Nominatim API.
     *
     * @param lat The latitude value.
     * @param lng The longitude value.
     * @return The formatted address if found; otherwise, a fallback message.
     */
    public String getAddress(double lat, double lng) {
        // Build the URL for Nominatim reverse geocoding
        String url = UriComponentsBuilder.fromHttpUrl("https://nominatim.openstreetmap.org/reverse")
                .queryParam("format", "json")
                .queryParam("lat", lat)
                .queryParam("lon", lng)
                .queryParam("zoom", 18) // Optional: detail level (18 gives maximum detail)
                .queryParam("addressdetails", 1)
                .toUriString();

        // Set necessary headers.
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "CypressProject/1.0 (cypressproject37@gmail.com)"); // Replace with your contact info

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> responseEntity =
                    restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String response = responseEntity.getBody();

            // Parse the JSON response.
            JsonNode root = objectMapper.readTree(response);
            // "display_name" holds the formatted address.
            if (root.has("display_name")) {
                return root.get("display_name").asText();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Address not found";
    }
}
