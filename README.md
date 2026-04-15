# Pal — Java OOP Project

A Local Area Network (LAN) based Social Media application built with Java,
demonstrating core Object-Oriented Programming principles.

---

## 📁 Project Structure

```
Pal/
├── src/
│   ├── model/          ← Data classes (OOP: Encapsulation, Serializable)
│   │   ├── User.java       - User entity with friends list
│   │   ├── Post.java       - Post entity implementing Comparable
│   │   ├── Message.java    - Direct message entity
│   │   └── Packet.java     - Universal network communication unit
│   │
│   ├── server/         ← Server-side logic (OOP: Composition, Multithreading)
│   │   ├── PalServer.java  - Main server, manages all state
│   │   └── ClientHandler.java   - Per-client thread (implements Runnable)
│   │
│   ├── client/         ← Client-side networking
│   │   └── ServerConnection.java - Encapsulates socket I/O, observer pattern
│   │
│   └── ui/             ← Swing GUI (OOP: Inheritance, Polymorphism)
│       ├── LoginFrame.java   - Login & Registration window
│       ├── EditorialMain.java    - Main app window (implements PacketListener)
│       ├── EditorialFeed.java    - News feed panel with custom renderer
│       ├── FriendsPanel.java - Friends & requests management
│       └── ChatPanel.java    - Real-time private chat
│
└── build.sh            ← Compile script
```

---

## How to Run

### Step 1 — Compile
```bash
chmod +x build.sh
./build.sh
```

### Step 2 — Start the Server (one machine)
```bash
cd out
java server.PalServer
```
The server listens on **port 9090**.

### Step 3 — Start the Client (any machine on the LAN)
```bash
./launch-client.sh
```
The App uses local network discovery, so the server IP will pop up automatically.
Leave it blank to connect to `localhost` (same machine testing).

On Windows, use:
```bat
launch-client.bat
```

---

## Features

| Feature | Description |
|---------|-------------|
| 📝 Register / Login | Secure credential-based auth over TCP |
| 📰 News Feed | Global post feed, auto-updates for all clients |
| 👥 Friend Requests | Send, accept, or decline friend requests |
| 💬 Real-time Chat | Private messages between online users |
| 🟢 Online Status | See who's currently connected |

---

## OOP Concepts Demonstrated

| Concept | Where |
|---------|-------|
| **Encapsulation** | `User`, `Post`, `Message` — private fields + getters/setters |
| **Abstraction** | `Packet` hides network complexity; `PacketListener` interface |
| **Inheritance** | `Post` implements `Comparable<Post>`; `MainFrame` implements `PacketListener` |
| **Polymorphism** | `Packet.Type` enum dispatches different behaviors via switch |
| **Multithreading** | `ClientHandler implements Runnable` — one thread per client |
| **Serialization** | All model classes implement `Serializable` for socket transport |

---

## Technical Details

- **Protocol**: TCP/IP via Java `ServerSocket` / `Socket`
- **Communication**: Object serialization (`ObjectOutputStream` / `ObjectInputStream`)
- **GUI**: Java Swing (`JFrame`, `JTabbedPane`, `JList` with custom `ListCellRenderer`)
- **Concurrency**: `ConcurrentHashMap`, `Collections.synchronizedList`, per-client threads
- **Port**: 9090 (configurable in `PalServer.java`)

---

## Testing Locally

Run the server, then open **two terminal windows** and run the client twice.
Register two different users and try posting, adding friends, and chatting!
