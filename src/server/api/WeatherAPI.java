package server.api;

import model.WeatherData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * WeatherAPI — Fetches current weather conditions from Open-Meteo API.
 * Endpoint: https://api.open-meteo.com/v1/forecast
 * Completely free, no API key required, no rate limiting concerns.
 * Demonstrates: Proxy pattern, HTTP client, manual JSON parsing.
 */
public class WeatherAPI {

    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Default coordinates (New Delhi, India — can be overridden)
    private static double defaultLat = 28.6139;
    private static double defaultLon = 77.2090;
    private static String defaultLocation = "New Delhi";

    /**
     * Set the default location for weather lookups.
     */
    public static void setDefaultLocation(double lat, double lon, String name) {
        defaultLat = lat;
        defaultLon = lon;
        defaultLocation = name;
    }

    /**
     * Get current weather for the default location.
     */
    public static WeatherData getCurrentWeather() {
        return getCurrentWeather(defaultLat, defaultLon, defaultLocation);
    }

    /**
     * Get current weather for a specific location.
     */
    public static WeatherData getCurrentWeather(double lat, double lon, String locationName) {
        try {
            String url = BASE_URL + "?latitude=" + lat + "&longitude=" + lon
                    + "&current_weather=true&timezone=auto";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseWeather(response.body(), locationName);
            } else {
                System.err.println("[Weather] HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("[Weather] Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Parse the Open-Meteo response JSON.
     * Response format: {"current_weather": {"temperature": 24.5, "windspeed": 10.2, "weathercode": 2, ...}}
     */
    private static WeatherData parseWeather(String json, String locationName) {
        WeatherData data = new WeatherData();
        data.setLocationName(locationName);

        // Find the current_weather object
        int cwPos = json.indexOf("\"current_weather\"");
        if (cwPos < 0) return null;

        int objStart = json.indexOf('{', cwPos);
        if (objStart < 0) return null;

        int objEnd = ITunesAPI.findMatchingBrace(json, objStart);
        if (objEnd < 0) return null;

        String cw = json.substring(objStart, objEnd + 1);

        String temp = ITunesAPI.extractString(cw, "temperature");
        if (temp != null) {
            try { data.setTemperature(Double.parseDouble(temp)); } catch (NumberFormatException ignored) {}
        }

        String wind = ITunesAPI.extractString(cw, "windspeed");
        if (wind != null) {
            try { data.setWindspeed(Double.parseDouble(wind)); } catch (NumberFormatException ignored) {}
        }

        String code = ITunesAPI.extractString(cw, "weathercode");
        if (code != null) {
            try { data.setWeatherCode(Integer.parseInt(code)); } catch (NumberFormatException ignored) {}
        }

        return data;
    }
}
