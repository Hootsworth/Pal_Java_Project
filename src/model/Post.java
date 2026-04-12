package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Post — Represents a user post in the global feed.
 * Demonstrates: Comparable, Serialization, Encapsulation.
 * Enhanced in Phase 2 with emoji reaction system replacing simple like count.
 */
public class Post implements Serializable, Comparable<Post> {
    private static final long serialVersionUID = 3L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM dd, HH:mm");

    public enum PostType { SOCIAL, MARKETPLACE }

    // Supported reaction emojis
    public static final String[] REACTION_EMOJIS = {"❤️", "🔥", "😂", "👏", "🤯"};

    private String author;
    private String content;
    private LocalDateTime timestamp;
    private int likes; // kept for backward compatibility, derived from reactions

    // Phase 2: Reaction system — maps emoji → set of usernames who reacted
    private Map<String, Set<String>> reactions;

    // Phase 2: Unique post ID for reliable reaction targeting
    private String postId;

    // Phase 3: Post categories and rich metadata
    private PostType type;
    private Map<String, String> metadata;

    public Post(String author, String content) {
        this(author, content, PostType.SOCIAL);
    }

    public Post(String author, String content, PostType type) {
        this.author = author;
        this.content = content;
        this.type = type;
        this.timestamp = LocalDateTime.now();
        this.likes = 0;
        this.reactions = new HashMap<>();
        this.metadata = new HashMap<>();
        this.postId = UUID.randomUUID().toString().substring(0, 8);
    }

    // ── Basic Getters ──
    public String getAuthor() { return author; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getPostId() { return postId; }

    // ── Likes (backward compat — now derived from total reactions) ──
    public int getLikes() {
        int total = 0;
        if (reactions != null) {
            for (Set<String> users : reactions.values()) {
                total += users.size();
            }
        }
        return total > 0 ? total : likes;
    }

    public void like() {
        likes++;
    }

    // ── Phase 2: Reactions ──

    /**
     * Toggle a reaction: if user already reacted with this emoji, remove it; otherwise add.
     * Returns true if reaction was added, false if removed.
     */
    public boolean toggleReaction(String emoji, String username) {
        if (reactions == null) reactions = new HashMap<>();
        Set<String> users = reactions.computeIfAbsent(emoji, k -> new HashSet<>());
        if (users.contains(username)) {
            users.remove(username);
            if (users.isEmpty()) reactions.remove(emoji);
            return false;
        } else {
            users.add(username);
            return true;
        }
    }

    /** Get the reaction map (emoji → set of usernames). */
    public Map<String, Set<String>> getReactions() {
        return reactions != null ? reactions : Collections.emptyMap();
    }

    /** Get count for a specific reaction emoji. */
    public int getReactionCount(String emoji) {
        if (reactions == null) return 0;
        Set<String> users = reactions.get(emoji);
        return users != null ? users.size() : 0;
    }

    /** Check if a specific user reacted with a specific emoji. */
    public boolean hasUserReacted(String emoji, String username) {
        if (reactions == null) return false;
        Set<String> users = reactions.get(emoji);
        return users != null && users.contains(username);
    }

    public String getFormattedTime() {
        return timestamp.format(FMT);
    }

    // ── Phase 3: Marketplace Helpers ──
    public PostType getType() { return type != null ? type : PostType.SOCIAL; }
    public void setType(PostType type) { this.type = type; }
    
    public Map<String, String> getMetadata() {
        if (metadata == null) metadata = new HashMap<>();
        return metadata;
    }

    public void setMetadata(String key, String value) {
        getMetadata().put(key, value);
    }

    @Override
    public int compareTo(Post other) {
        return other.timestamp.compareTo(this.timestamp); // newest first
    }

    @Override
    public String toString() {
        return "[" + author + " @ " + getFormattedTime() + "] " + content + " ❤ " + getLikes();
    }
}
