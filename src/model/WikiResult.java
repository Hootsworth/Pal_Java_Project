package model;

import java.io.Serializable;

/**
 * WikiResult — Represents a Wikipedia article summary.
 * Contains title, extract text, thumbnail URL, and page URL.
 * Demonstrates: Encapsulation, Serialization, Value Object pattern.
 */
public class WikiResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String title;
    private String extract;        // First paragraph summary
    private String thumbnailUrl;   // Thumbnail image URL (may be null)
    private String pageUrl;        // Full Wikipedia page URL
    private String description;    // Short description (e.g. "Programming language")

    public WikiResult() {}

    public WikiResult(String title, String extract, String thumbnailUrl, String pageUrl, String description) {
        this.title = title;
        this.extract = extract;
        this.thumbnailUrl = thumbnailUrl;
        this.pageUrl = pageUrl;
        this.description = description;
    }

    // ── Getters ──
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getExtract() { return extract; }
    public void setExtract(String extract) { this.extract = extract; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    /** Returns a truncated extract (first N chars). */
    public String getShortExtract(int maxLength) {
        if (extract == null) return "";
        if (extract.length() <= maxLength) return extract;
        return extract.substring(0, maxLength) + "...";
    }

    @Override
    public String toString() {
        return "WikiResult[" + title + "]";
    }
}
