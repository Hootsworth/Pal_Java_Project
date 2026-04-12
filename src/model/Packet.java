package model;

import java.io.Serializable;

/**
 * Packet is the universal communication unit between Client and Server.
 * Uses OOP encapsulation to wrap any type of request/response.
 */
public class Packet implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        // Auth
        LOGIN, REGISTER, LOGIN_SUCCESS, LOGIN_FAIL, REGISTER_SUCCESS, REGISTER_FAIL,

        // Posts
        NEW_POST, GET_FEED, FEED_RESPONSE, LIKE_POST,

        // Reactions (Phase 2)
        REACT_POST,

        // Friends
        FRIEND_REQUEST, FRIEND_ACCEPT, FRIEND_DECLINE, GET_FRIENDS, FRIENDS_RESPONSE,
        GET_PENDING, PENDING_RESPONSE,

        // Chat
        CHAT_MESSAGE, CHAT_HISTORY_REQUEST, CHAT_HISTORY_RESPONSE,

        // Typing Indicator (Phase 2)
        TYPING_START, TYPING_STOP,

        // Online users
        GET_ONLINE_USERS, ONLINE_USERS_RESPONSE,

        // Profile (Phase 2)
        UPDATE_PROFILE, GET_PROFILE, PROFILE_RESPONSE,

        // Phase 3: iTunes Music
        MUSIC_SEARCH_REQUEST,       // payload: search term (String)
        MUSIC_SEARCH_RESPONSE,      // payload: List<MusicShare>
        MUSIC_SHARE_POST,           // payload: MusicShare (post to feed)

        // Phase 3: Wikipedia
        WIKI_LOOKUP_REQUEST,        // payload: search term (String)
        WIKI_LOOKUP_RESPONSE,       // payload: WikiResult

        // Phase 3: Weather
        WEATHER_REQUEST,            // payload: null
        WEATHER_RESPONSE,           // payload: WeatherData

        // Phase 3: Trivia
        TRIVIA_REQUEST,             // payload: null
        TRIVIA_RESPONSE,            // payload: String (trivia fact)

        // Phase 1: LAN-Drop (File Transfer)
        FILE_OFFER,                 // payload: FileMetadata (filename, size)
        FILE_ACCEPT,                // payload: String (receiver IP:Port)
        FILE_REJECT,                // payload: String (reason)
        
        // Phase 1: The "Vibe" Room (Audio Sync)
        VIBE_SYNC,                  // payload: MusicShare metadata

        // Phase 2: Collaborative Mind-Board
        DRAW_EVENT,                 // payload: DrawAction (x,y,type,etc)

        // General
        ERROR, SERVER_BROADCAST
    }

    private Type type;
    private Object payload;
    private String sender;

    public Packet(Type type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public Packet(Type type, Object payload, String sender) {
        this.type = type;
        this.payload = payload;
        this.sender = sender;
    }

    public Type getType() { return type; }
    public Object getPayload() { return payload; }
    public String getSender() { return sender; }

    @Override
    public String toString() {
        return "Packet[" + type + ", sender=" + sender + "]";
    }
}
