package client;

import model.Packet;

import java.io.*;
import java.net.*;

/**
 * ServerConnection - Manages the client's connection to the LAN server.
 * Encapsulates all network I/O. Runs a listener thread for incoming packets.
 */
public class ServerConnection {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile PacketListener listener;
    private volatile boolean connected = false;

    public interface PacketListener {
        void onPacketReceived(Packet packet);
        void onDisconnected();
    }

    private void notifyDisconnected() {
        PacketListener current = listener;
        if (current != null) {
            current.onDisconnected();
        }
    }

    /** Allows swapping the listener (e.g. from LoginFrame to MainFrame). */
    public void setListener(PacketListener listener) {
        this.listener = listener;
    }

    public synchronized boolean connect(String host, int port, PacketListener listener) {
        this.listener = listener;
        if (socket != null && socket.isConnected() && !socket.isClosed()) {
            connected = true;
            return true;
        }
        // Ensure stale streams/sockets are closed before opening a fresh connection.
        disconnect();
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;
            startListening();
            return true;
        } catch (IOException e) {
            connected = false;
            System.err.println("Connection failed: " + e.getMessage());
            return false;
        }
    }

    private void startListening() {
        final Socket activeSocket = socket;
        final ObjectInputStream activeIn = in;
        Thread t = new Thread(() -> {
            try {
                Packet packet;
                while ((packet = (Packet) activeIn.readObject()) != null) {
                    PacketListener current = listener;
                    if (current != null) {
                        current.onPacketReceived(packet);
                    }
                }
            } catch (EOFException | SocketException e) {
                if (activeSocket == socket) {
                    notifyDisconnected();
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Listener error: " + e.getMessage());
                if (activeSocket == socket) {
                    notifyDisconnected();
                }
            } finally {
                synchronized (this) {
                    if (activeSocket == socket) {
                        connected = false;
                    }
                }
            }
        });
        t.setDaemon(true);
        t.setName("Pal-Client-Listener");
        t.start();
    }

    public synchronized void send(Packet packet) {
        if (!connected) return;
        try {
            out.writeObject(packet);
            out.flush();
            out.reset();
        } catch (IOException e) {
            System.err.println("Send error: " + e.getMessage());
        }
    }

    public synchronized void disconnect() {
        connected = false;
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        try { if (out != null) out.close(); } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        in = null;
        out = null;
        socket = null;
    }

    public boolean isConnected() { return connected; }
}
