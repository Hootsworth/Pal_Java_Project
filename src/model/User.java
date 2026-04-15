package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * User — Represents a registered user in the LAN Social network.
 * Demonstrates OOP encapsulation with private fields and controlled access.
 * Enhanced in Phase 2 with status, avatar, and profile fields.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 3L;

    private String username;
    private String password;
    private String bio;
    private List<String> friends;
    private List<String> pendingRequests; // incoming friend requests

    // Phase 2: Status system
    private String statusEmoji;   // e.g. "🎵", "🎮", "📖", "💻"
    private String statusText;    // e.g. "Listening to music"

    // Phase 2: Profile metadata
    private long joinedTimestamp;
    private int postCount;

    // Phase 5: DiceBear avatar identity
    private String avatarStyle;
    private String avatarSeed;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.bio = "";
        this.friends = new ArrayList<>();
        this.pendingRequests = new ArrayList<>();
        this.statusEmoji = "";
        this.statusText = "";
        this.joinedTimestamp = System.currentTimeMillis();
        this.postCount = 0;
        this.avatarStyle = "bottts";
        this.avatarSeed = username;
    }

    // ── Basic Getters ──
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    // ── Friends ──
    public List<String> getFriends() { return friends; }
    public List<String> getPendingRequests() { return pendingRequests; }

    public void addFriend(String username) {
        if (!friends.contains(username)) friends.add(username);
    }

    public void addPendingRequest(String from) {
        if (!pendingRequests.contains(from)) pendingRequests.add(from);
    }

    public void removePendingRequest(String from) {
        pendingRequests.remove(from);
    }

    public boolean isFriend(String username) {
        return friends.contains(username);
    }

    // ── Phase 2: Status ──
    public String getStatusEmoji() { return statusEmoji != null ? statusEmoji : ""; }
    public void setStatusEmoji(String emoji) { this.statusEmoji = emoji; }
    public String getStatusText() { return statusText != null ? statusText : ""; }
    public void setStatusText(String text) { this.statusText = text; }

    /** Returns combined display status like "🎵 Listening to music" */
    public String getDisplayStatus() {
        String e = getStatusEmoji();
        String t = getStatusText();
        if (!e.isEmpty() && !t.isEmpty()) return e + " " + t;
        if (!e.isEmpty()) return e;
        if (!t.isEmpty()) return t;
        return "";
    }

    // ── Phase 2: Profile metadata ──
    public long getJoinedTimestamp() { return joinedTimestamp; }
    public int getPostCount() { return postCount; }
    public void incrementPostCount() { postCount++; }

    // ── Phase 5: Avatar metadata ──
    public String getAvatarStyle() { return avatarStyle != null && !avatarStyle.isBlank() ? avatarStyle : "bottts"; }
    public void setAvatarStyle(String avatarStyle) {
        this.avatarStyle = (avatarStyle == null || avatarStyle.isBlank()) ? "bottts" : avatarStyle;
    }

    public String getAvatarSeed() { return avatarSeed != null && !avatarSeed.isBlank() ? avatarSeed : username; }
    public void setAvatarSeed(String avatarSeed) {
        this.avatarSeed = (avatarSeed == null || avatarSeed.isBlank()) ? username : avatarSeed;
    }

    @Override
    public String toString() {
        return username;
    }
}
