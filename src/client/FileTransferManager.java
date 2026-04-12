package client;

import model.FileMetadata;
import javafx.application.Platform;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * FileTransferManager - Handles direct P2P byte streaming for LAN-Drop.
 */
public class FileTransferManager {

    private static final int BUFFER_SIZE = 64 * 1024; // 64KB

    /**
     * Receiver side: Opens a ServerSocket and waits for the sender to connect.
     */
    public static void receiveFile(FileMetadata meta, File saveFile, Consumer<Integer> onPortDiscovered, Consumer<Double> onProgress, Runnable onComplete, Consumer<String> onError) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(0)) { // Port 0 = find any free port
                int port = serverSocket.getLocalPort();
                Platform.runLater(() -> onPortDiscovered.accept(port));

                // Wait for connection
                serverSocket.setSoTimeout(30000); // 30 sec timeout
                try (Socket socket = serverSocket.accept();
                     InputStream is = socket.getInputStream();
                     FileOutputStream fos = new FileOutputStream(saveFile)) {

                    byte[] buffer = new byte[BUFFER_SIZE];
                    long totalRead = 0;
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                        totalRead += read;
                        double progress = (double) totalRead / meta.getFileSize();
                        Platform.runLater(() -> onProgress.accept(progress));
                    }
                    Platform.runLater(onComplete);
                }
            } catch (IOException e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        }).start();
    }

    /**
     * Sender side: Connects to the receiver and streams the file.
     */
    public static void sendFile(File file, String ip, int port, Consumer<Double> onProgress, Runnable onComplete, Consumer<String> onError) {
        new Thread(() -> {
            try (Socket socket = new Socket(ip, port);
                 OutputStream os = socket.getOutputStream();
                 FileInputStream fis = new FileInputStream(file)) {

                byte[] buffer = new byte[BUFFER_SIZE];
                long totalSent = 0;
                long fileSize = file.length();
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                    totalSent += read;
                    double progress = (double) totalSent / fileSize;
                    Platform.runLater(() -> onProgress.accept(progress));
                }
                Platform.runLater(onComplete);

            } catch (IOException e) {
                Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        }).start();
    }
}
