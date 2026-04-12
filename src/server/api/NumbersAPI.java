package server.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;

/**
 * NumbersAPI — Fetches fun trivia facts about numbers and dates.
 * Endpoint: http://numbersapi.com/
 * Completely free, no API key required.
 * Demonstrates: Proxy pattern, simple HTTP usage.
 */
public class NumbersAPI {

    private static final String BASE_URL = "http://numbersapi.com/";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Get a trivia fact about today's date.
     * Example: "April 11th is the day in 1951 that Stone of Scone is found..."
     */
    public static String getTodayFact() {
        try {
            LocalDate today = LocalDate.now();
            String url = BASE_URL + today.getMonthValue() + "/" + today.getDayOfMonth() + "/date";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Pal/5.0")
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body().trim();
            }
        } catch (Exception e) {
            System.err.println("[NumbersAPI] Error: " + e.getMessage());
        }
        return "Did you know? The number 42 is the answer to life, the universe, and everything!";
    }

    /**
     * Get a random trivia fact about a random number.
     */
    public static String getRandomTrivia() {
        try {
            String url = BASE_URL + "random/trivia";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Pal/5.0")
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body().trim();
            }
        } catch (Exception e) {
            System.err.println("[NumbersAPI] Error: " + e.getMessage());
        }
        return "7 is the number of days in a week.";
    }

    /**
     * Get a math fact about a specific number.
     */
    public static String getMathFact(int number) {
        try {
            String url = BASE_URL + number + "/math";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Pal/5.0")
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body().trim();
            }
        } catch (Exception e) {
            System.err.println("[NumbersAPI] Error: " + e.getMessage());
        }
        return number + " is a number.";
    }
}
