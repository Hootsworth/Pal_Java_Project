package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * BeaconServer - Periodically multicasts server presence over UDP.
 */
public class BeaconServer extends Thread {

    public static final String MULTICAST_ADDRESS = "230.1.1.1";
    public static final int MULTICAST_PORT = 9091;
    private final int serverTcpPort;
    private volatile boolean running = true;

    public BeaconServer(int serverTcpPort) {
        this.serverTcpPort = serverTcpPort;
        setDaemon(true);
        setName("BeaconServerThread");
    }

    @Override
    public void run() {
        System.out.println("[UDP] Beacon server started on " + MULTICAST_ADDRESS + ":" + MULTICAST_PORT);
        try (DatagramSocket socket = new DatagramSocket()) {
            while (running) {
                // Format: PAL_SERVER|HOSTNAME|PORT
                String hostname = InetAddress.getLocalHost().getHostName();
                String message = "PAL_SERVER|" + hostname + "|" + serverTcpPort;
                byte[] buffer = message.getBytes();

                InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, MULTICAST_PORT);
                
                socket.send(packet);

                Thread.sleep(5000); // Pulse every 5 seconds
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("[UDP] Beacon interrupted: " + e.getMessage());
        }
    }

    public void stopBeacon() {
        running = false;
        interrupt();
    }
}
