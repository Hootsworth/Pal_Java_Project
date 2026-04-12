package server.api;

import model.WikiResult;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * WikipediaAPI — Fetches article summaries from the Wikipedia REST API.
 * Endpoint: https://en.wikipedia.org/api/rest_v1/page/summary/{title}
 * No API key required. Requires User-Agent header.
 * Demonstrates: Proxy pattern, HTTP client, manual JSON parsing.
 */
public class WikipediaAPI {

    private static final String BASE_URL = "https://en.wikipedia.org/api/rest_v1/page/summary/";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Look up a Wikipedia article by title/search term.
     * @param query The article title or search term
     * @return WikiResult with summary, or null if not found
     */
    public static WikiResult lookup(String query) {
        try {
            String encoded = URLEncoder.encode(query.replace(" ", "_"), StandardCharsets.UTF_8);
            String url = BASE_URL + encoded;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Pal/5.0 (Java; Educational Project)")
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseResult(response.body());
            } else {
                System.err.println("[Wikipedia] HTTP " + response.statusCode() + " for: " + query);
            }
        } catch (Exception e) {
            System.err.println("[Wikipedia] Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Parse the Wikipedia REST API response JSON into a WikiResult.
     */
    private static WikiResult parseResult(String json) {
        WikiResult result = new WikiResult();

        result.setTitle(ITunesAPI.extractString(json, "title"));
        result.setExtract(ITunesAPI.extractString(json, "extract"));
        result.setDescription(ITunesAPI.extractString(json, "description"));

        // Page URL
        String desktopUrl = null;
        int contentUrlsPos = json.indexOf("\"content_urls\"");
        if (contentUrlsPos >= 0) {
            int desktopPos = json.indexOf("\"desktop\"", contentUrlsPos);
            if (desktopPos >= 0) {
                int pagePos = json.indexOf("\"page\"", desktopPos);
                if (pagePos >= 0) {
                    desktopUrl = ITunesAPI.extractString(json.substring(pagePos), "page");
                }
            }
        }
        result.setPageUrl(desktopUrl);

        // Thumbnail URL
        int thumbPos = json.indexOf("\"thumbnail\"");
        if (thumbPos >= 0) {
            int sourcePos = json.indexOf("\"source\"", thumbPos);
            if (sourcePos >= 0) {
                String thumbUrl = ITunesAPI.extractString(json.substring(sourcePos), "source");
                result.setThumbnailUrl(thumbUrl);
            }
        }

        return result;
    }
}
