package model;

import java.io.Serializable;

/**
 * UserProfile — A serializable snapshot of a user's public profile information.
 * Sent over the network in response to GET_PROFILE requests.
 * Demonstrates: Encapsulation, Serialization, Value Object pattern.
 */
public class UserProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String bio;
    private String statusEmoji;
    private String statusText;
    private long joinedTimestamp;
    private int postCount;
    private int friendCount;
    private boolean online;
    private String avatarStyle;
    private String avatarSeed;

    public UserProfile(User user, boolean online) {
        this.username = user.getUsername();
        this.bio = user.getBio();
        this.statusEmoji = user.getStatusEmoji();
        this.statusText = user.getStatusText();
        this.joinedTimestamp = user.getJoinedTimestamp();
        this.postCount = user.getPostCount();
        this.friendCount = user.getFriends().size();
        this.online = online;
        this.avatarStyle = user.getAvatarStyle();
        this.avatarSeed = user.getAvatarSeed();
    }

    public UserProfile(String username) {
        this.username = username;
        this.bio = "";
        this.statusEmoji = "";
        this.statusText = "";
        this.joinedTimestamp = System.currentTimeMillis();
        this.postCount = 0;
        this.friendCount = 0;
        this.online = true;
        this.avatarStyle = "bottts";
        this.avatarSeed = username;
    }

    // ── Getters ──
    public String getUsername() { return username; }
    public String getBio() { return bio; }
    public String getStatusEmoji() { return statusEmoji; }
    public String getStatusText() { return statusText; }
    public long getJoinedTimestamp() { return joinedTimestamp; }
    public int getPostCount() { return postCount; }
    public int getFriendCount() { return friendCount; }
    public boolean isOnline() { return online; }
    public String getAvatarStyle() { return avatarStyle != null && !avatarStyle.isBlank() ? avatarStyle : "bottts"; }
    public String getAvatarSeed() { return avatarSeed != null && !avatarSeed.isBlank() ? avatarSeed : username; }

    // ── Setters for profile edits sent from client ──
    public void setBio(String bio) { this.bio = bio != null ? bio : ""; }
    public void setStatusEmoji(String statusEmoji) { this.statusEmoji = statusEmoji != null ? statusEmoji : ""; }
    public void setStatusText(String statusText) { this.statusText = statusText != null ? statusText : ""; }
    public void setAvatarStyle(String avatarStyle) {
        this.avatarStyle = (avatarStyle == null || avatarStyle.isBlank()) ? "bottts" : avatarStyle;
    }
    public void setAvatarSeed(String avatarSeed) {
        this.avatarSeed = (avatarSeed == null || avatarSeed.isBlank()) ? username : avatarSeed;
    }

    /** Returns combined display status. */
    public String getDisplayStatus() {
        String e = statusEmoji != null ? statusEmoji : "";
        String t = statusText != null ? statusText : "";
        if (!e.isEmpty() && !t.isEmpty()) return e + " " + t;
        if (!e.isEmpty()) return e;
        if (!t.isEmpty()) return t;
        return "";
    }

    /** Returns a human-readable "Joined X ago" string. */
    public String getJoinedDisplay() {
        long diff = System.currentTimeMillis() - joinedTimestamp;
        long days = diff / (1000 * 60 * 60 * 24);
        if (days < 1) return "Joined today";
        if (days == 1) return "Joined yesterday";
        if (days < 30) return "Joined " + days + " days ago";
        long months = days / 30;
        if (months < 12) return "Joined " + months + " month" + (months > 1 ? "s" : "") + " ago";
        return "Joined over a year ago";
    }

    @Override
    public String toString() {
        return "UserProfile[" + username + "]";
    }
}
