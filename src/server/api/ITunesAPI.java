package server.api;

import model.MusicShare;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * ITunesAPI — Searches the Apple iTunes Store for music tracks.
 * Endpoint: https://itunes.apple.com/search
 * No API key required. Rate limit: ~20 calls/minute.
 * Demonstrates: Proxy pattern, HTTP client usage, manual JSON parsing.
 */
public class ITunesAPI {

    private static final String BASE_URL = "https://itunes.apple.com/search";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Search iTunes for music tracks matching the given term.
     * @param term Search query (e.g. "Taylor Swift", "Bohemian Rhapsody")
     * @param limit Max results (1-50)
     * @return List of MusicShare results
     */
    public static List<MusicShare> search(String term, int limit) {
        List<MusicShare> results = new ArrayList<>();
        try {
            String encoded = URLEncoder.encode(term, StandardCharsets.UTF_8);
            String url = BASE_URL + "?term=" + encoded + "&media=music&entity=song&limit=" + limit;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                results = parseResults(response.body());
            } else {
                System.err.println("[iTunes] HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("[iTunes] Error: " + e.getMessage());
        }
        return results;
    }

    /** Search with default limit of 8 results. */
    public static List<MusicShare> search(String term) {
        return search(term, 8);
    }

    /**
     * Manual JSON parsing — extracts MusicShare objects from iTunes response.
     * The response has format: {"resultCount": N, "results": [{...}, ...]}
     */
    private static List<MusicShare> parseResults(String json) {
        List<MusicShare> results = new ArrayList<>();

        // Find the "results" array
        int resultsStart = json.indexOf("\"results\"");
        if (resultsStart < 0) return results;

        int arrayStart = json.indexOf('[', resultsStart);
        if (arrayStart < 0) return results;

        // Parse each object in the array
        int pos = arrayStart + 1;
        while (pos < json.length()) {
            int objStart = json.indexOf('{', pos);
            if (objStart < 0) break;

            // Find matching closing brace (handle nested objects)
            int objEnd = findMatchingBrace(json, objStart);
            if (objEnd < 0) break;

            String objStr = json.substring(objStart, objEnd + 1);
            MusicShare share = parseTrack(objStr);
            if (share != null && share.getTrackName() != null) {
                results.add(share);
            }

            pos = objEnd + 1;
        }

        return results;
    }

    private static MusicShare parseTrack(String obj) {
        MusicShare share = new MusicShare();
        share.setTrackName(extractString(obj, "trackName"));
        share.setArtistName(extractString(obj, "artistName"));
        share.setCollectionName(extractString(obj, "collectionName"));
        share.setArtworkUrl100(extractString(obj, "artworkUrl100"));
        share.setPreviewUrl(extractString(obj, "previewUrl"));
        share.setTrackViewUrl(extractString(obj, "trackViewUrl"));
        share.setPrimaryGenre(extractString(obj, "primaryGenreName"));

        String price = extractString(obj, "trackPrice");
        if (price != null) {
            try { share.setTrackPrice(Double.parseDouble(price)); } catch (NumberFormatException ignored) {}
        }

        String duration = extractString(obj, "trackTimeMillis");
        if (duration != null) {
            try { share.setTrackTimeMillis(Long.parseLong(duration)); } catch (NumberFormatException ignored) {}
        }

        return share;
    }

    // ── JSON Helpers (no external deps) ──

    /** Extract a string value for a given key from a JSON object string. */
    static String extractString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyPos = json.indexOf(searchKey);
        if (keyPos < 0) return null;

        int colonPos = json.indexOf(':', keyPos + searchKey.length());
        if (colonPos < 0) return null;

        // Skip whitespace after colon
        int valueStart = colonPos + 1;
        while (valueStart < json.length() && json.charAt(valueStart) == ' ') valueStart++;

        if (valueStart >= json.length()) return null;

        char firstChar = json.charAt(valueStart);

        if (firstChar == '"') {
            // String value
            int strStart = valueStart + 1;
            int strEnd = strStart;
            while (strEnd < json.length()) {
                if (json.charAt(strEnd) == '"' && json.charAt(strEnd - 1) != '\\') break;
                strEnd++;
            }
            return json.substring(strStart, strEnd)
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\\\", "\\");
        } else if (firstChar == 'n') {
            return null; // null value
        } else {
            // Number or boolean — read until comma or brace
            int end = valueStart;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}' && json.charAt(end) != ']') {
                end++;
            }
            return json.substring(valueStart, end).trim();
        }
    }

    /** Find the matching closing brace for an opening brace. */
    static int findMatchingBrace(String json, int openPos) {
        int depth = 0;
        boolean inString = false;
        for (int i = openPos; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }
}
