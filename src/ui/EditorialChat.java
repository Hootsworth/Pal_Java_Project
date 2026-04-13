package ui;

import client.ServerConnection;
import animatefx.animation.SlideInRight;
import animatefx.animation.SlideInLeft;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import model.*;
import client.FileTransferManager;
import org.kordamp.ikonli.javafx.FontIcon;

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
    private final ScrollPane scroll;
    
    private final java.util.Map<String, File> pendingFiles = new java.util.HashMap<>();

    public EditorialChat(User user, ServerConnection connection) {
        this.user = user;
        this.connection = connection;
        setSpacing(24);

        Label title = new Label("Messages");
        title.getStyleClass().add("heading");

        HBox split = new HBox(20);
        
        // Active Users Sidebar
        VBox usersBox = new VBox(15);
        usersBox.setPrefWidth(220);
        usersBox.getStyleClass().add("editorial-card");
        HBox uHeader = new HBox(8);
        uHeader.setAlignment(Pos.CENTER_LEFT);
        FontIcon usersIcon = new FontIcon("mdi2a-account-group-outline");
        usersIcon.setIconColor(Color.web("#8B949E"));
        Label uTitle = new Label("Active Users");
        uTitle.getStyleClass().add("subheading");
        uHeader.getChildren().addAll(usersIcon, uTitle);

        onlineList = new ListView<>();
        onlineList.getStyleClass().add("nav-list");
        onlineList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    FontIcon dot = new FontIcon("mdi2c-circle-medium");
                    dot.setIconColor(Color.web("#10B981")); // green dot
                    setGraphic(dot);
                }
            }
        });
        inputField = new TextField();
        inputField.setPromptText("Type a message or /wiki query...");
        inputField.getStyleClass().add("text-field");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputField.setDisable(true); // Disable until user selected

        messageContainer = new VBox(12);
        scroll = new ScrollPane(messageContainer);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(450);
        scroll.getStyleClass().add("scroll-pane");

        Button fileBtn = new Button("", new FontIcon("mdi2p-paperclip"));
        fileBtn.getStyleClass().add("button-outline");
        fileBtn.setStyle("-fx-border-radius: 50%; -fx-background-radius: 50%; -fx-padding: 8px 12px;");
        fileBtn.setOnAction(e -> initiateFileTransfer());
        fileBtn.setDisable(true);

        Button sendBtn = new Button("", new FontIcon("mdi2s-send"));
        sendBtn.getStyleClass().add("button-primary");
        sendBtn.setStyle("-fx-border-radius: 50%; -fx-background-radius: 50%; -fx-padding: 8px 12px;");
        sendBtn.setOnAction(e -> sendMessage());
        sendBtn.setDisable(true);

        onlineList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean hasSelected = (n != null);
            if (hasSelected) {
                activeChatUser = n;
                messageContainer.getChildren().clear();
                connection.send(new Packet(Packet.Type.CHAT_HISTORY_REQUEST, n));
                inputField.setPromptText("Message " + n + "...");
            }
            inputField.setDisable(!hasSelected);
            fileBtn.setDisable(!hasSelected);
            sendBtn.setDisable(!hasSelected);
        });

        usersBox.getChildren().addAll(uHeader, onlineList);

        VBox chatBox = new VBox(15);
        chatBox.getStyleClass().add("editorial-card");
        HBox.setHgrow(chatBox, Priority.ALWAYS);

        HBox inputBox = new HBox(10);
        inputBox.setAlignment(Pos.CENTER_LEFT);
        inputBox.getChildren().addAll(fileBtn, inputField, sendBtn);
        
        chatBox.getChildren().addAll(scroll, inputBox);

        split.getChildren().addAll(usersBox, chatBox);
        getChildren().addAll(title, split);
    }

    private void sendMessage() {
        if (activeChatUser == null || inputField.getText().isEmpty()) return;
        String text = inputField.getText();
        
        if (text.startsWith("/wiki ")) {
            connection.send(new Packet(Packet.Type.WIKI_LOOKUP_REQUEST, text.substring(6)));
        } else {
            Message msg = new Message(user.getUsername(), activeChatUser, text);
            connection.send(new Packet(Packet.Type.CHAT_MESSAGE, msg));
            receiveMessage(msg);
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

            VBox bubbleBox = new VBox(4);
            boolean isMine = msg.getFrom().equals(user.getUsername());
            
            Label lbl = new Label(rawText);
            lbl.getStyleClass().add(isMine ? "chat-bubble-sent" : "chat-bubble-received");
            lbl.setWrapText(true);
            
            Label timeLbl = new Label(msg.getFormattedTime());
            timeLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #8B949E;");

            if (previewUrl != null) {
                Button playBtn = createPlayButton(previewUrl);
                VBox mediaBox = new VBox(5, lbl, playBtn);
                bubbleBox.getChildren().addAll(mediaBox, timeLbl);
            } else {
                bubbleBox.getChildren().addAll(lbl, timeLbl);
            }

            HBox row = new HBox(bubbleBox);
            if (isMine) {
                row.setAlignment(Pos.CENTER_RIGHT);
                bubbleBox.setAlignment(Pos.CENTER_RIGHT);
            } else {
                row.setAlignment(Pos.CENTER_LEFT);
                bubbleBox.setAlignment(Pos.CENTER_LEFT);
            }
            
            messageContainer.getChildren().add(row);

            // Animate entrance
            if (isMine) {
                new SlideInRight(row).setSpeed(2.0).play();
            } else {
                new SlideInLeft(row).setSpeed(2.0).play();
            }
            
            javafx.application.Platform.runLater(() -> scroll.setVvalue(1.0));
        }
    }

    private Button createPlayButton(String url) {
        Button playObj = new Button("Play Preview", new FontIcon("mdi2p-play-circle"));
        playObj.getStyleClass().add("button-outline");
        playObj.setStyle("-fx-border-radius: 20px; -fx-background-radius: 20px; -fx-font-size: 11px;");
        
        MediaPlayer player = null;
        try {
            player = new MediaPlayer(new Media(url));
        } catch (Exception ex) {
            playObj.setText("Media Error");
            playObj.setDisable(true);
            return playObj;
        }

        final MediaPlayer mp = player;
        playObj.setOnAction(e -> {
            if (mp.getStatus() == MediaPlayer.Status.PLAYING) {
                mp.pause();
                playObj.setText("Play Preview");
                playObj.setGraphic(new FontIcon("mdi2p-play-circle"));
            } else {
                mp.play();
                playObj.setText("Pause");
                playObj.setGraphic(new FontIcon("mdi2p-pause-circle"));
            }
        });

        mp.setOnEndOfMedia(() -> {
            mp.stop();
            playObj.setText("Play Preview");
            playObj.setGraphic(new FontIcon("mdi2p-play-circle"));
        });

        return playObj;
    }

    public void loadHistory(List<Message> messages) {
        messageContainer.getChildren().clear();
        for (Message m : messages) receiveMessage(m);
        javafx.application.Platform.runLater(() -> scroll.setVvalue(1.0));
    }

    // ── File Transfer Logic ──

    private void initiateFileTransfer() {
        if (activeChatUser == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select File to Send");
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            FileMetadata meta = new FileMetadata(file.getName(), file.length(), user.getUsername(), activeChatUser);
            pendingFiles.put(activeChatUser, file);
            connection.send(new Packet(Packet.Type.FILE_OFFER, meta));
            showSystemMessage("Waiting for " + activeChatUser + " to accept your file...", "mdi2c-clock-outline");
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
                String[] data = (String[]) packet.getPayload(); 
                String[] target = data[1].split(":");
                String receiverName = data.length > 2 ? data[2] : activeChatUser;
                handleFileAcceptance(receiverName, target[0], Integer.parseInt(target[1]));
            }
            case FILE_REJECT -> {
                String[] data = (String[]) packet.getPayload();
                String rejecter = data.length > 2 ? data[2] : "Unknown";
                showSystemMessage("Transfer rejected by " + rejecter + ": " + data[1], "mdi2c-close-circle-outline");
            }
            default -> {}
        }
    }

    private void startFileReceiver(FileMetadata meta, File saveFile) {
        FileTransferManager.receiveFile(meta, saveFile, 
            port -> {
                try {
                    String ip = InetAddress.getLocalHost().getHostAddress();
                    connection.send(new Packet(Packet.Type.FILE_ACCEPT, new String[]{meta.getSender(), ip + ":" + port, user.getUsername()}));
                    showSystemMessage("Port discovered: " + port + ". Ready to receive.", "mdi2l-lan-connect");
                } catch (Exception e) {
                    showSystemMessage("Error detecting IP: " + e.getMessage(), "mdi2a-alert-circle");
                }
            },
            prog -> showSystemMessage("Receiving: " + (int)(prog * 100) + "%", "mdi2d-download"),
            () -> showSystemMessage("Successfully received: " + meta.getFileName(), "mdi2c-check-circle-outline"),
            err -> showSystemMessage("Error: " + err, "mdi2a-alert-circle"));
    }
    
    private void showSystemMessage(String text, String iconCode) {
        javafx.application.Platform.runLater(() -> {
            HBox box = new HBox(8);
            box.setAlignment(Pos.CENTER);
            FontIcon icon = new FontIcon(iconCode);
            icon.setIconColor(Color.web("#58A6FF"));
            Label status = new Label(text);
            status.setStyle("-fx-text-fill: #8B949E; -fx-font-size: 12px; -fx-font-style: italic;");
            box.getChildren().addAll(icon, status);
            
            messageContainer.getChildren().add(box);
            scroll.setVvalue(1.0);
        });
    }

    private void handleFileAcceptance(String receiverName, String ip, int port) {
        File file = pendingFiles.get(receiverName);
        if (file != null) {
            showSystemMessage("Streaming " + file.getName() + " to " + receiverName + "...", "mdi2u-upload");
            FileTransferManager.sendFile(file, ip, port, 
                prog -> showSystemMessage("Sending: " + (int)(prog * 100) + "%", "mdi2u-upload"),
                () -> showSystemMessage("Successfully sent: " + file.getName(), "mdi2c-check-circle-outline"),
                err -> showSystemMessage("Send Error: " + err, "mdi2a-alert-circle"));
            pendingFiles.remove(receiverName);
        } else {
            showSystemMessage("Error: No pending file for " + receiverName, "mdi2a-alert-circle");
        }
    }
}
