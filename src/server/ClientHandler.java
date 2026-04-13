package server;

import model.*;

import java.io.*;
import java.net.*;
import java.util.List;

/**
 * ClientHandler - Manages communication with a single connected client.
 * Runs in its own thread. Demonstrates: Runnable, OOP encapsulation, polymorphism via Packet types.
 * Phase 3: Handles API request packets by delegating to server gateway methods in worker threads.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final PalServer server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User currentUser;

    public ClientHandler(Socket socket, PalServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Packet packet;
            while ((packet = (Packet) in.readObject()) != null) {
                handlePacket(packet);
            }
        } catch (EOFException | SocketException e) {
            System.out.println("[-] Client disconnected: " + (currentUser != null ? currentUser.getUsername() : "unknown"));
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[ERROR] Handler error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void handlePacket(Packet packet) {
        switch (packet.getType()) {
            // Auth
            case LOGIN -> handleLogin(packet);
            case REGISTER -> handleRegister(packet);

            // Posts
            case NEW_POST -> handleNewPost(packet);
            case GET_FEED -> send(new Packet(Packet.Type.FEED_RESPONSE, server.getGlobalFeed()));
            case LIKE_POST -> handleLike(packet);
            case REACT_POST -> handleReact(packet);
            case REPLY_POST -> handleReply(packet);
            case REPORT_POST -> handleReport(packet);
            case SHARE_POST -> handleShare(packet);

            // Friends
            case FRIEND_REQUEST -> handleFriendRequest(packet);
            case FRIEND_ACCEPT -> handleFriendAccept(packet);
            case FRIEND_DECLINE -> handleFriendDecline(packet);
            case GET_FRIENDS -> handleGetFriends();
            case GET_PENDING -> handleGetPending();

            // Chat
            case CHAT_MESSAGE -> handleChatMessage(packet);
            case CHAT_HISTORY_REQUEST -> handleChatHistoryRequest(packet);

            // Typing
            case TYPING_START -> handleTyping(packet, true);
            case TYPING_STOP -> handleTyping(packet, false);

            // Profile
            case UPDATE_PROFILE -> handleUpdateProfile(packet);
            case GET_PROFILE -> handleGetProfile(packet);

            // Phase 3: API requests
            case MUSIC_SEARCH_REQUEST -> handleMusicSearch(packet);
            case MUSIC_SHARE_POST -> handleMusicSharePost(packet);
            case WIKI_LOOKUP_REQUEST -> handleWikiLookup(packet);
            case WEATHER_REQUEST -> handleWeatherRequest();
            case TRIVIA_REQUEST -> handleTriviaRequest();

            // Phase 1: File Transfer
            case FILE_OFFER -> handleFileOffer(packet);
            case FILE_ACCEPT -> handleFileAccept(packet);
            case FILE_REJECT -> handleFileReject(packet);

            // Phase 1: Vibe Sync
            case VIBE_SYNC -> handleVibeSync(packet);

            // Phase 2: Collaborative Mind-Board
            case DRAW_EVENT -> handleDrawEvent(packet);

            // Online
            case GET_ONLINE_USERS -> send(new Packet(Packet.Type.ONLINE_USERS_RESPONSE, server.getOnlineUsernames()));

            default -> send(new Packet(Packet.Type.ERROR, "Unknown packet type"));
        }
    }

    // ── Auth ──

    private void handleLogin(Packet packet) {
        String[] creds = (String[]) packet.getPayload();
        User user = server.authenticate(creds[0], creds[1]);
        if (user != null) {
            currentUser = user;
            server.registerOnline(user.getUsername(), this);
            send(new Packet(Packet.Type.LOGIN_SUCCESS, user));
            System.out.println("[✓] User logged in: " + user.getUsername());
        } else {
            send(new Packet(Packet.Type.LOGIN_FAIL, "Invalid username or password"));
        }
    }

    private void handleRegister(Packet packet) {
        String[] creds = (String[]) packet.getPayload();
        if (server.registerUser(creds[0], creds[1])) {
            send(new Packet(Packet.Type.REGISTER_SUCCESS, "Registered successfully!"));
        } else {
            send(new Packet(Packet.Type.REGISTER_FAIL, "Username already taken"));
        }
    }

    // ── Posts ──

    private void handleNewPost(Packet packet) {
        if (currentUser == null) return;
        Object payload = packet.getPayload();
        Post post;
        if (payload instanceof Post) {
            // Phase 3: Marketplace posts are sent as full Post objects
            post = (Post) payload;
            post = new Post(currentUser.getUsername(), post.getContent(), post.getType());
            // Copy metadata from the client-sent post
            for (java.util.Map.Entry<String, String> entry : ((Post) payload).getMetadata().entrySet()) {
                post.setMetadata(entry.getKey(), entry.getValue());
            }
        } else {
            // Legacy: plain text content
            String content = (String) payload;
            post = new Post(currentUser.getUsername(), content);
        }
        server.addPost(post);
    }

    private void handleLike(Packet packet) {
        List<Post> feed = server.getGlobalFeed();
        int index = (int) packet.getPayload();
        if (index >= 0 && index < feed.size()) {
            feed.get(index).like();
            send(new Packet(Packet.Type.FEED_RESPONSE, server.getGlobalFeed()));
        }
    }

    private void handleReact(Packet packet) {
        if (currentUser == null) return;
        String[] data = (String[]) packet.getPayload();
        if (data.length == 2) {
            server.reactToPost(data[0], data[1], currentUser.getUsername());
        }
    }

    // ── Phase 4: Social Features ──

    private void handleReply(Packet packet) {
        if (currentUser == null) return;
        Post reply = (Post) packet.getPayload();
        reply = new Post(currentUser.getUsername(), reply.getContent());
        reply.setParentPostId(((Post) packet.getPayload()).getParentPostId());
        server.addReply(reply);
    }

    private void handleReport(Packet packet) {
        if (currentUser == null) return;
        String[] data = (String[]) packet.getPayload(); // {postId, username, reason}
        server.reportPost(data[0], currentUser.getUsername(), data.length > 2 ? data[2] : "No reason");
    }

    private void handleShare(Packet packet) {
        if (currentUser == null) return;
        String postId = (String) packet.getPayload();
        server.sharePost(postId, currentUser.getUsername());
    }

    // ── Friends ──

    private void handleFriendRequest(Packet packet) {
        String targetUsername = (String) packet.getPayload();
        boolean ok = server.sendFriendRequest(currentUser.getUsername(), targetUsername);
        if (!ok) send(new Packet(Packet.Type.ERROR, "Could not send friend request to " + targetUsername));
    }

    private void handleFriendAccept(Packet packet) {
        String requester = (String) packet.getPayload();
        server.acceptFriendRequest(currentUser.getUsername(), requester);
        handleGetFriends();
        handleGetPending();
    }

    private void handleFriendDecline(Packet packet) {
        String requester = (String) packet.getPayload();
        server.declineFriendRequest(currentUser.getUsername(), requester);
        handleGetPending();
    }

    private void handleGetFriends() {
        if (currentUser == null) return;
        User freshUser = server.getUser(currentUser.getUsername());
        send(new Packet(Packet.Type.FRIENDS_RESPONSE, freshUser.getFriends()));
    }

    private void handleGetPending() {
        if (currentUser == null) return;
        User freshUser = server.getUser(currentUser.getUsername());
        send(new Packet(Packet.Type.PENDING_RESPONSE, freshUser.getPendingRequests()));
    }

    // ── Chat ──

    private void handleChatMessage(Packet packet) {
        if (currentUser == null) return;
        Message msg = (Message) packet.getPayload();
        server.storeMessage(msg);
    }

    private void handleChatHistoryRequest(Packet packet) {
        if (currentUser == null) return;
        String otherUser = (String) packet.getPayload();
        List<Message> history = server.getChatHistory(currentUser.getUsername(), otherUser);
        send(new Packet(Packet.Type.CHAT_HISTORY_RESPONSE, history));
    }

    // ── Typing ──

    private void handleTyping(Packet packet, boolean started) {
        if (currentUser == null) return;
        String targetUser = (String) packet.getPayload();
        server.relayTyping(currentUser.getUsername(), targetUser, started);
    }

    // ── Profile ──

    private void handleUpdateProfile(Packet packet) {
        if (currentUser == null) return;
        UserProfile profile = (UserProfile) packet.getPayload();
        server.updateProfile(currentUser.getUsername(), profile);
        UserProfile updated = server.getProfile(currentUser.getUsername());
        send(new Packet(Packet.Type.PROFILE_RESPONSE, updated));
    }

    private void handleGetProfile(Packet packet) {
        String username = (String) packet.getPayload();
        UserProfile profile = server.getProfile(username);
        if (profile != null) {
            send(new Packet(Packet.Type.PROFILE_RESPONSE, profile));
        } else {
            send(new Packet(Packet.Type.ERROR, "User not found: " + username));
        }
    }

    // ── Phase 3: API Handlers (run in worker threads to avoid blocking) ──

    private void handleMusicSearch(Packet packet) {
        if (currentUser == null) return;
        String term = (String) packet.getPayload();
        // Run API call in a worker thread to not block the client handler
        new Thread(() -> {
            List<MusicShare> results = server.searchMusic(term);
            send(new Packet(Packet.Type.MUSIC_SEARCH_RESPONSE, new java.util.ArrayList<>(results)));
        }).start();
    }

    private void handleMusicSharePost(Packet packet) {
        if (currentUser == null) return;
        MusicShare share = (MusicShare) packet.getPayload();
        share.setSharedBy(currentUser.getUsername());
        server.addMusicSharePost(share);
    }

    private void handleWikiLookup(Packet packet) {
        if (currentUser == null) return;
        String query = (String) packet.getPayload();
        new Thread(() -> {
            WikiResult result = server.lookupWiki(query);
            if (result != null) {
                send(new Packet(Packet.Type.WIKI_LOOKUP_RESPONSE, result));
            } else {
                send(new Packet(Packet.Type.ERROR, "No Wikipedia article found for: " + query));
            }
        }).start();
    }

    private void handleWeatherRequest() {
        new Thread(() -> {
            WeatherData data = server.getWeather();
            if (data != null) {
                send(new Packet(Packet.Type.WEATHER_RESPONSE, data));
            }
        }).start();
    }

    private void handleTriviaRequest() {
        new Thread(() -> {
            String fact = server.getTrivia();
            send(new Packet(Packet.Type.TRIVIA_RESPONSE, fact));
        }).start();
    }

    // ── Phase 1: File Transfer ──

    private void handleFileOffer(Packet packet) {
        FileMetadata meta = (FileMetadata) packet.getPayload();
        ClientHandler target = server.getHandler(meta.getReceiver());
        if (target != null) {
            target.send(packet);
        } else {
            send(new Packet(Packet.Type.ERROR, "Receiver " + meta.getReceiver() + " is offline."));
        }
    }

    private void handleFileAccept(Packet packet) {
        String[] data = (String[]) packet.getPayload(); // [sender, ip:port]
        ClientHandler target = server.getHandler(data[0]);
        if (target != null) {
            target.send(packet);
        }
    }

    private void handleFileReject(Packet packet) {
        String[] data = (String[]) packet.getPayload(); // [sender, reason]
        ClientHandler target = server.getHandler(data[0]);
        if (target != null) {
            target.send(packet);
        }
    }

    // ── Phase 1: Vibe Sync ──

    private void handleVibeSync(Packet packet) {
        if (currentUser == null) return;
        // Broadcast the vibe to everyone online
        server.broadcastToAll(packet);
    }

    private void handleDrawEvent(Packet packet) {
        // Relay the drawing action to everyone on the LAN
        server.broadcastToAll(packet);
    }

    // ── Network I/O ──

    public synchronized void send(Packet packet) {
        try {
            out.writeObject(packet);
            out.flush();
            out.reset();
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to send packet: " + e.getMessage());
        }
    }

    private void cleanup() {
        if (currentUser != null) {
            server.unregisterOnline(currentUser.getUsername());
        }
        try { socket.close(); } catch (IOException ignored) {}
    }
}
