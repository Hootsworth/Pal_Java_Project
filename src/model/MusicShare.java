package model;

import java.io.Serializable;

/**
 * MusicShare — Represents a music track shared from iTunes Search API.
 * Contains track metadata, artwork URL, and the user who shared it.
 * Demonstrates: Encapsulation, Serialization, Value Object pattern.
 */
public class MusicShare implements Serializable {
    private static final long serialVersionUID = 1L;

    private String trackName;
    private String artistName;
    private String collectionName; // album name
    private String artworkUrl100;  // 100x100 album art URL
    private String previewUrl;     // 30-sec audio preview URL
    private String trackViewUrl;   // iTunes store link
    private double trackPrice;
    private String primaryGenre;
    private long trackTimeMillis;  // duration in ms
    private String sharedBy;       // username who shared this

    public MusicShare() {}

    // ── Getters & Setters ──
    public String getTrackName() { return trackName; }
    public void setTrackName(String trackName) { this.trackName = trackName; }

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }

    public String getArtworkUrl100() { return artworkUrl100; }
    public void setArtworkUrl100(String artworkUrl100) { this.artworkUrl100 = artworkUrl100; }

    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }

    public String getTrackViewUrl() { return trackViewUrl; }
    public void setTrackViewUrl(String trackViewUrl) { this.trackViewUrl = trackViewUrl; }

    public double getTrackPrice() { return trackPrice; }
    public void setTrackPrice(double trackPrice) { this.trackPrice = trackPrice; }

    public String getPrimaryGenre() { return primaryGenre; }
    public void setPrimaryGenre(String primaryGenre) { this.primaryGenre = primaryGenre; }

    public long getTrackTimeMillis() { return trackTimeMillis; }
    public void setTrackTimeMillis(long trackTimeMillis) { this.trackTimeMillis = trackTimeMillis; }

    public String getSharedBy() { return sharedBy; }
    public void setSharedBy(String sharedBy) { this.sharedBy = sharedBy; }

    /** Returns formatted duration like "3:42" */
    public String getFormattedDuration() {
        if (trackTimeMillis <= 0) return "";
        long totalSeconds = trackTimeMillis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }

    @Override
    public String toString() {
        return trackName + " — " + artistName;
    }
}
