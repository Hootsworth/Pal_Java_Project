package client;

import javafx.application.Platform;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.function.Consumer;

/**
 * DiscoveryClient - Listens for UDP multicast beacons from the server.
 */
public class DiscoveryClient extends Thread {

    private static final String MULTICAST_ADDRESS = "230.1.1.1";
    private static final int MULTICAST_PORT = 9091;
    private final Consumer<String[]> onDiscovered;
    private volatile boolean running = true;

    public DiscoveryClient(Consumer<String[]> onDiscovered) {
        this.onDiscovered = onDiscovered;
        setDaemon(true);
        setName("DiscoveryClientThread");
    }

    @Override
    public void run() {
        try (MulticastSocket socket = new MulticastSocket(MULTICAST_PORT)) {
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(new java.net.InetSocketAddress(group, MULTICAST_PORT), null);

            byte[] buffer = new byte[1024];
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                if (message.startsWith("PAL_SERVER|")) {
                    String[] parts = message.split("\\|");
                    if (parts.length >= 3) {
                        // Use the sender's IP for better reliability
                        String ip = packet.getAddress().getHostAddress();
                        Platform.runLater(() -> onDiscovered.accept(new String[]{ip, parts[2]}));
                    }
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[UDP] Discovery error: " + e.getMessage());
            }
        }
    }

    public void stopDiscovery() {
        running = false;
        interrupt();
    }
}
