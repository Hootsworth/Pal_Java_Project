package server;

import model.*;
import server.api.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PalServer - Central server for the LAN Social Media application.
 * Demonstrates: OOP design, multithreading, TCP socket communication.
 * Phase 3: Acts as API gateway (proxy pattern) for iTunes, Wikipedia, Weather, Trivia.
 */
public class PalServer {

    public static final int PORT = 9090;

    // Thread-safe maps for server state
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, ClientHandler> onlineClients = new ConcurrentHashMap<>();
    private final List<Post> globalFeed = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, List<Message>> messageHistory = new ConcurrentHashMap<>();
    
    private BeaconServer beacon;

    // Phase 3: Cached data
    private WeatherData cachedWeather = null;
    private long weatherCacheTime = 0;
    private static final long WEATHER_CACHE_DURATION = 30 * 60 * 1000; // 30 minutes

    private String cachedTrivia = null;
    private long triviaCacheTime = 0;
    private static final long TRIVIA_CACHE_DURATION = 60 * 60 * 1000; // 1 hour

    public void start() {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║            Pal Server v5.0           ║");
        System.out.println("║         Nebula Edition               ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println("Starting on port " + PORT + "...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[OK] Server is running. Waiting for clients...");
            
            beacon = new BeaconServer(PORT);
            beacon.start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[+] New connection from: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket, this);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Server error: " + e.getMessage());
        }
    }

    // ---- User Management ----

    public synchronized boolean registerUser(String username, String password) {
        if (users.containsKey(username)) return false;
        users.put(username, new User(username, password));
        System.out.println("[+] Registered new user: " + username);
        return true;
    }

    public User authenticate(String username, String password) {
        User u = users.get(username);
        if (u != null && u.getPassword().equals(password)) return u;
        return null;
    }

    public User getUser(String username) { return users.get(username); }

    public List<String> getOnlineUsernames() { return new ArrayList<>(onlineClients.keySet()); }

    public boolean isOnline(String username) { return onlineClients.containsKey(username); }

    public void registerOnline(String username, ClientHandler handler) {
        onlineClients.put(username, handler);
        broadcastOnlineUsers();
    }

    public void unregisterOnline(String username) {
        onlineClients.remove(username);
        broadcastOnlineUsers();
    }

    public ClientHandler getHandler(String username) {
        return onlineClients.get(username);
    }

    // ---- Feed ----

    public void addPost(Post post) {
        globalFeed.add(post);
        User user = users.get(post.getAuthor());
        if (user != null) user.incrementPostCount();
        broadcastToAll(new Packet(Packet.Type.FEED_RESPONSE, getGlobalFeed()));
    }

    public List<Post> getGlobalFeed() {
        List<Post> sorted;
        synchronized (globalFeed) {
            sorted = new ArrayList<>(globalFeed);
        }
        Collections.sort(sorted);
        return sorted;
    }

    public void reactToPost(String postId, String emoji, String username) {
        synchronized (globalFeed) {
            for (Post post : globalFeed) {
                if (post.getPostId().equals(postId)) {
                    post.toggleReaction(emoji, username);
                    break;
                }
            }
        }
        broadcastToAll(new Packet(Packet.Type.FEED_RESPONSE, getGlobalFeed()));
    }

    // ── Phase 4: Social Features ──

    public void addReply(Post reply) {
        synchronized (globalFeed) {
            for (Post post : globalFeed) {
                if (post.getPostId().equals(reply.getParentPostId())) {
                    post.addReply(reply);
                    break;
                }
            }
        }
        broadcastToAll(new Packet(Packet.Type.FEED_RESPONSE, getGlobalFeed()));
    }

    public void reportPost(String postId, String reporterUsername, String reason) {
        synchronized (globalFeed) {
            for (Post post : globalFeed) {
                if (post.getPostId().equals(postId)) {
                    if (post.reportBy(reporterUsername)) {
                        System.out.println("[⚠️ REPORT] Post " + postId + " by " + reporterUsername + " (Reason: " + reason + ")");
                    }
                    break;
                }
            }
        }
    }

    public void sharePost(String postId, String sharerUsername) {
        Post toShare = null;
        synchronized (globalFeed) {
            for (Post post : globalFeed) {
                if (post.getPostId().equals(postId)) {
                    post.incrementShareCount();
                    toShare = post;
                    break;
                }
            }
        }
        if (toShare != null) {
            Post sharePost = new Post(sharerUsername, "Shared a post by " + toShare.getAuthor() + ":\n" + toShare.getContent());
            addPost(sharePost);
        }
    }

    // ---- Friends ----

    public synchronized boolean sendFriendRequest(String from, String to) {
        User target = users.get(to);
        if (target == null || target.isFriend(from)) return false;
        target.addPendingRequest(from);
        ClientHandler targetHandler = onlineClients.get(to);
        if (targetHandler != null) {
            targetHandler.send(new Packet(Packet.Type.PENDING_RESPONSE, target.getPendingRequests()));
        }
        return true;
    }

    public synchronized boolean acceptFriendRequest(String acceptor, String requester) {
        User u1 = users.get(acceptor);
        User u2 = users.get(requester);
        if (u1 == null || u2 == null) return false;
        u1.removePendingRequest(requester);
        u1.addFriend(requester);
        u2.addFriend(acceptor);
        return true;
    }

    public synchronized boolean declineFriendRequest(String decliner, String requester) {
        User u = users.get(decliner);
        if (u == null) return false;
        u.removePendingRequest(requester);
        return true;
    }

    // ---- Chat ----

    public void storeMessage(Message msg) {
        String key = getChatKey(msg.getFrom(), msg.getTo());
        messageHistory.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>())).add(msg);
        ClientHandler recipientHandler = onlineClients.get(msg.getTo());
        if (recipientHandler != null) {
            recipientHandler.send(new Packet(Packet.Type.CHAT_MESSAGE, msg, msg.getFrom()));
        }
    }

    public List<Message> getChatHistory(String user1, String user2) {
        String key = getChatKey(user1, user2);
        List<Message> list = messageHistory.getOrDefault(key, new ArrayList<>());
        List<Message> snapshot;
        synchronized (list) {
            snapshot = new ArrayList<>(list);
        }
        return snapshot;
    }

    private String getChatKey(String a, String b) {
        return a.compareTo(b) < 0 ? a + ":" + b : b + ":" + a;
    }

    // ---- Profile ----

    public UserProfile getProfile(String username) {
        User user = users.get(username);
        if (user == null) return null;
        return new UserProfile(user, isOnline(username));
    }

    public void updateProfile(String username, UserProfile profile) {
        User user = users.get(username);
        if (user == null) return;
        user.setBio(profile.getBio());
        user.setStatusEmoji(profile.getStatusEmoji());
        user.setStatusText(profile.getStatusText());
        user.setAvatarStyle(profile.getAvatarStyle());
        user.setAvatarSeed(profile.getAvatarSeed());
        System.out.println("[~] Profile updated for: " + username);
    }

    // ---- Typing ----

    public void relayTyping(String from, String to, boolean started) {
        ClientHandler targetHandler = onlineClients.get(to);
        if (targetHandler != null) {
            Packet.Type type = started ? Packet.Type.TYPING_START : Packet.Type.TYPING_STOP;
            targetHandler.send(new Packet(type, from, from));
        }
    }

    // ---- Phase 3: API Gateway Methods ----

    /** Search iTunes for music tracks. Runs in a worker thread to not block. */
    public List<MusicShare> searchMusic(String term) {
        System.out.println("[API] iTunes search: " + term);
        return ITunesAPI.search(term);
    }

    /** Look up a Wikipedia article. */
    public WikiResult lookupWiki(String query) {
        System.out.println("[API] Wikipedia lookup: " + query);
        return WikipediaAPI.lookup(query);
    }

    /** Get current weather (cached for 30 minutes). */
    public synchronized WeatherData getWeather() {
        long now = System.currentTimeMillis();
        if (cachedWeather != null && (now - weatherCacheTime) < WEATHER_CACHE_DURATION) {
            return cachedWeather;
        }
        System.out.println("[API] Fetching weather...");
        WeatherData data = WeatherAPI.getCurrentWeather();
        if (data != null) {
            cachedWeather = data;
            weatherCacheTime = now;
        }
        return data;
    }

    /** Get today's trivia fact (cached for 1 hour). */
    public synchronized String getTrivia() {
        long now = System.currentTimeMillis();
        if (cachedTrivia != null && (now - triviaCacheTime) < TRIVIA_CACHE_DURATION) {
            return cachedTrivia;
        }
        System.out.println("[API] Fetching trivia...");
        String fact = NumbersAPI.getTodayFact();
        if (fact != null) {
            cachedTrivia = fact;
            triviaCacheTime = now;
        }
        return fact;
    }

    /** Add a music share as a special post in the feed. */
    public void addMusicSharePost(MusicShare share) {
        // Create a post with music metadata embedded in the content
        String content = "🎵 Shared a track: " + share.getTrackName() + " by " + share.getArtistName();
        if (share.getCollectionName() != null && !share.getCollectionName().isEmpty()) {
            content += " — from " + share.getCollectionName();
        }
        if (!share.getFormattedDuration().isEmpty()) {
            content += " (" + share.getFormattedDuration() + ")";
        }
        if (share.getPrimaryGenre() != null) {
            content += " [" + share.getPrimaryGenre() + "]";
        }
        if (share.getPreviewUrl() != null && !share.getPreviewUrl().isEmpty()) {
            content += "\n[PREVIEW:" + share.getPreviewUrl() + "]";
        }
        Post post = new Post(share.getSharedBy(), content);
        addPost(post);
    }

    // ---- Broadcasts ----

    public void broadcastToAll(Packet packet) {
        for (ClientHandler handler : onlineClients.values()) {
            handler.send(packet);
        }
    }

    private void broadcastOnlineUsers() {
        broadcastToAll(new Packet(Packet.Type.ONLINE_USERS_RESPONSE, getOnlineUsernames()));
    }

    public static void main(String[] args) {
        new PalServer().start();
    }
}
