package ui;

import client.ServerConnection;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import model.*;
import client.FileTransferManager;
import java.io.File;
import java.net.InetAddress;

import java.util.List;

public class EditorialChat extends VBox {
    private final ServerConnection connection;
    private final User user;
    
    private final VBox messageContainer;
    private final TextField inputField;
    private final ListView<String> onlineList;
    private String activeChatUser = null;
    
    private final java.util.Map<String, File> pendingFiles = new java.util.HashMap<>();

    public EditorialChat(User user, ServerConnection connection) {
        this.user = user;
        this.connection = connection;
        setSpacing(24);

        Label title = new Label("Messages");
        title.getStyleClass().add("heading");

        HBox split = new HBox(20);
        
        // Chat Area
        VBox chatBox = new VBox(10);
        chatBox.getStyleClass().add("editorial-card");
        HBox.setHgrow(chatBox, Priority.ALWAYS);

        messageContainer = new VBox(10);

        // Active Users Sidebar
        VBox usersBox = new VBox(10);
        usersBox.setPrefWidth(200);
        usersBox.getStyleClass().add("editorial-card");
        Label uTitle = new Label("Active Users");
        uTitle.getStyleClass().add("subheading");
        onlineList = new ListView<>();
        onlineList.getStyleClass().add("nav-list");
        onlineList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                activeChatUser = n;
                messageContainer.getChildren().clear();
                connection.send(new Packet(Packet.Type.CHAT_HISTORY_REQUEST, n));
            }
        });
        usersBox.getChildren().addAll(uTitle, onlineList);
        ScrollPane scroll = new ScrollPane(messageContainer);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(400);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        HBox inputBox = new HBox(10);
        inputField = new TextField();
        inputField.setPromptText("Type a message or /wiki query...");
        inputField.getStyleClass().add("text-field");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        Button sendBtn = new Button("SEND");
        sendBtn.getStyleClass().add("button-primary");
        sendBtn.setOnAction(e -> sendMessage());
        inputField.setOnAction(e -> sendMessage());

        Button fileBtn = new Button("FILE");
        fileBtn.getStyleClass().add("button-outline");
        fileBtn.setOnAction(e -> initiateFileTransfer());

        inputBox.getChildren().addAll(inputField, fileBtn, sendBtn);
        chatBox.getChildren().addAll(scroll, inputBox);

        split.getChildren().addAll(usersBox, chatBox);
        getChildren().addAll(title, split);
    }

    private void sendMessage() {
        if (activeChatUser == null || inputField.getText().isEmpty()) return;
        String text = inputField.getText();
        
        // Slash commands
        if (text.startsWith("/wiki ")) {
            connection.send(new Packet(Packet.Type.WIKI_LOOKUP_REQUEST, text.substring(6)));
        } else {
            Message msg = new Message(user.getUsername(), activeChatUser, text);
            connection.send(new Packet(Packet.Type.CHAT_MESSAGE, msg));
            receiveMessage(msg); // echo locally
        }
        inputField.clear();
    }

    public void updateOnline(List<String> online) {
        online.remove(user.getUsername());
        onlineList.getItems().setAll(online);
    }

    public void receiveMessage(Message msg) {
        if (msg.getFrom().equals(activeChatUser) || msg.getTo().equals(activeChatUser)) {
            String rawText = msg.getContent();
            String previewUrl = null;
            
            int pIdx = rawText.indexOf("[PREVIEW:");
            if (pIdx >= 0) {
                int endIdx = rawText.indexOf("]", pIdx);
                if (endIdx >= pIdx + 9) {
                    previewUrl = rawText.substring(pIdx + 9, endIdx);
                    rawText = rawText.substring(0, pIdx).trim();
                }
            }

            VBox bubble = new VBox(5);
            Label lbl = new Label(msg.getFrom() + ": " + rawText);
            lbl.getStyleClass().add("body-text");
            bubble.getChildren().add(lbl);
            
            if (previewUrl != null) {
                Button playBtn = createPlayButton(previewUrl);
                bubble.getChildren().add(playBtn);
            }
            
            messageContainer.getChildren().add(bubble);
        }
    }

    private Button createPlayButton(String url) {
        Button playObj = new Button("▶ Play Preview");
        playObj.getStyleClass().add("button-outline");
        playObj.setStyle("-fx-text-fill: #6c63ff; -fx-border-color: #6c63ff; -fx-font-size: 10px; -fx-padding: 4px 8px;");
        
        MediaPlayer player = null;
        try {
            player = new MediaPlayer(new Media(url));
        } catch (Exception ex) {
            playObj.setText("X Media Error");
            playObj.setDisable(true);
            return playObj;
        }

        final MediaPlayer mp = player;
        playObj.setOnAction(e -> {
            if (mp.getStatus() == MediaPlayer.Status.PLAYING) {
                mp.pause();
                playObj.setText("▶ Play Preview");
            } else {
                mp.play();
                playObj.setText("⏸ Pause");
            }
        });

        mp.setOnEndOfMedia(() -> {
            mp.stop();
            playObj.setText("▶ Play Preview");
        });

        return playObj;
    }

    public void loadHistory(List<Message> messages) {
        messageContainer.getChildren().clear();
        for (Message m : messages) receiveMessage(m);
    }

    // ── Phase 1: File Transfer Logic ──

    private void initiateFileTransfer() {
        if (activeChatUser == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select File to Send");
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            FileMetadata meta = new FileMetadata(file.getName(), file.length(), user.getUsername(), activeChatUser);
            pendingFiles.put(activeChatUser, file);
            connection.send(new Packet(Packet.Type.FILE_OFFER, meta));
            showStatus("Waiting for " + activeChatUser + " to accept...");
        }
    }

    public void handleFilePacket(Packet packet) {
        switch (packet.getType()) {
            case FILE_OFFER -> {
                FileMetadata meta = (FileMetadata) packet.getPayload();
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Incoming File");
                alert.setHeaderText(meta.getSender() + " wants to send a file");
                alert.setContentText("File: " + meta.getFileName() + " (" + meta.getFormattedSize() + ")");
                
                if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    FileChooser saver = new FileChooser();
                    saver.setInitialFileName(meta.getFileName());
                    File targetFile = saver.showSaveDialog(getScene().getWindow());
                    if (targetFile != null) {
                        startFileReceiver(meta, targetFile);
                    } else {
                        connection.send(new Packet(Packet.Type.FILE_REJECT, new String[]{meta.getSender(), "User cancelled save", user.getUsername()}));
                    }
                } else {
                    connection.send(new Packet(Packet.Type.FILE_REJECT, new String[]{meta.getSender(), "User declined", user.getUsername()}));
                }
            }
            case FILE_ACCEPT -> {
                String[] data = (String[]) packet.getPayload(); // [original_sender, ip:port, receiver_who_accepted]
                String[] target = data[1].split(":");
                String receiverName = data.length > 2 ? data[2] : activeChatUser;
                handleFileAcceptance(receiverName, target[0], Integer.parseInt(target[1]));
            }
            case FILE_REJECT -> {
                String[] data = (String[]) packet.getPayload();
                String rejecter = data.length > 2 ? data[2] : "Unknown";
                showStatus("Transfer rejected by " + rejecter + ": " + data[1]);
            }
            default -> {
                // Safely ignore all non-file related packets
            }
        }
    }

    private void startFileReceiver(FileMetadata meta, File saveFile) {
        FileTransferManager.receiveFile(meta, saveFile, 
            port -> {
                try {
                    String ip = InetAddress.getLocalHost().getHostAddress();
                    connection.send(new Packet(Packet.Type.FILE_ACCEPT, new String[]{meta.getSender(), ip + ":" + port, user.getUsername()}));
                    showStatus("Port discovered: " + port + ". Ready.");
                } catch (Exception e) {
                    showStatus("Error detecting IP: " + e.getMessage());
                }
            },
            prog -> showStatus("Receiving: " + (int)(prog * 100) + "%"),
            () -> showStatus("Received: " + meta.getFileName()),
            err -> showStatus("Error: " + err));
    }
    
    private void showStatus(String text) {
        javafx.application.Platform.runLater(() -> {
            Label status = new Label("[System] " + text);
            status.getStyleClass().add("body-text");
            status.setStyle("-fx-text-fill: #6c63ff; -fx-font-style: italic;");
            messageContainer.getChildren().add(status);
        });
    }

    private void handleFileAcceptance(String receiverName, String ip, int port) {
        File file = pendingFiles.get(receiverName);
        if (file != null) {
            showStatus("Streaming " + file.getName() + " to " + receiverName + " at " + ip + ":" + port);
            FileTransferManager.sendFile(file, ip, port, 
                prog -> showStatus("Sending: " + (int)(prog * 100) + "%"),
                () -> showStatus("Sent: " + file.getName()),
                err -> showStatus("Send Error: " + err));
            pendingFiles.remove(receiverName);
        } else {
            showStatus("Error: No pending file for " + receiverName);
        }
    }
}
