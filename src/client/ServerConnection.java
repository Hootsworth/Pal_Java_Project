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
    private boolean connected = false;

    public interface PacketListener {
        void onPacketReceived(Packet packet);
        void onDisconnected();
    }

    /** Allows swapping the listener (e.g. from LoginFrame to MainFrame). */
    public void setListener(PacketListener listener) {
        this.listener = listener;
    }

    public boolean connect(String host, int port, PacketListener listener) {
        this.listener = listener;
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;
            startListening();
            return true;
        } catch (IOException e) {
            System.err.println("Connection failed: " + e.getMessage());
            return false;
        }
    }

    private void startListening() {
        Thread t = new Thread(() -> {
            try {
                Packet packet;
                while ((packet = (Packet) in.readObject()) != null) {
                    listener.onPacketReceived(packet);
                }
            } catch (EOFException | SocketException e) {
                listener.onDisconnected();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Listener error: " + e.getMessage());
                listener.onDisconnected();
            }
        });
        t.setDaemon(true);
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

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public boolean isConnected() { return connected; }
}
