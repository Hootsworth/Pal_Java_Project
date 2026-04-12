package ui;

import client.PalClient;
import client.ServerConnection;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.Message;
import model.Packet;
import model.Post;
import model.User;
import model.WeatherData;
import model.WikiResult;

import java.util.List;

public class EditorialMain extends BorderPane implements ServerConnection.PacketListener {

    private final PalClient app;
    private final ServerConnection connection;
    private final User currentUser;

    private EditorialFeed feedView;
    private EditorialChat chatView;
    private EditorialDiscover discoverView;
    private EditorialMindBoard mindBoardView;

    private Label topStatus;

    public EditorialMain(PalClient app, ServerConnection connection, User user) {
        this.app = app;
        this.connection = connection;
        this.currentUser = user;
        this.connection.setListener(this);
        initUI();
        connection.send(new Packet(Packet.Type.GET_FEED, null));
        connection.send(new Packet(Packet.Type.GET_ONLINE_USERS, null));
        connection.send(new Packet(Packet.Type.WEATHER_REQUEST, null));
        connection.send(new Packet(Packet.Type.TRIVIA_REQUEST, null));
    }

    private void initUI() {
        getStyleClass().add("root");

        // ── Sidebar ──
        VBox sidebar = new VBox(20);
        sidebar.setPrefWidth(200);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(30, 0, 0, 0));

        Label logo = new Label("P A L");
        logo.getStyleClass().add("heading");
        logo.setPadding(new Insets(0, 0, 20, 24));

        ListView<String> navList = new ListView<>();
        ObservableList<String> items = FXCollections.observableArrayList(
            "Feed", "Discover", "Chat", "Mind Board"
        );
        navList.setItems(items);
        navList.getStyleClass().add("nav-list");
        navList.setPrefHeight(300);

        sidebar.getChildren().addAll(logo, navList);

        // ── Topbar ──
        HBox topbar = new HBox();
        topbar.getStyleClass().add("topbar");
        topbar.setPadding(new Insets(16, 24, 16, 24));
        topStatus = new Label("Logged in as " + currentUser.getUsername() + " | Weather: Loading...");
        topStatus.getStyleClass().add("subheading");
        topbar.getChildren().add(topStatus);

        // ── Content Area ──
        StackPane contentArea = new StackPane();
        contentArea.setPadding(new Insets(30));
        
        feedView = new EditorialFeed(currentUser, connection, this);
        chatView = new EditorialChat(currentUser, connection);
        discoverView = new EditorialDiscover(connection);
        mindBoardView = new EditorialMindBoard(connection);
        
        feedView.managedProperty().bind(feedView.visibleProperty());
        chatView.managedProperty().bind(chatView.visibleProperty());
        discoverView.managedProperty().bind(discoverView.visibleProperty());
        mindBoardView.managedProperty().bind(mindBoardView.visibleProperty());

        contentArea.getChildren().addAll(discoverView, chatView, feedView, mindBoardView);
        
        navList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            feedView.setVisible("Feed".equals(newV));
            discoverView.setVisible("Discover".equals(newV));
            chatView.setVisible("Chat".equals(newV));
            mindBoardView.setVisible("Mind Board".equals(newV));
        });
        navList.getSelectionModel().select(0);

        setLeft(sidebar);
        setTop(topbar);
        setCenter(contentArea);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onPacketReceived(Packet packet) {
        Platform.runLater(() -> {
            switch (packet.getType()) {
                case FEED_RESPONSE -> {
                    List<Post> posts = (List<Post>) packet.getPayload();
                    feedView.updateFeed(posts);
                }
                case ONLINE_USERS_RESPONSE -> {
                    List<String> online = (List<String>) packet.getPayload();
                    chatView.updateOnline(online);
                }
                case CHAT_MESSAGE -> chatView.receiveMessage((Message) packet.getPayload());
                case CHAT_HISTORY_RESPONSE -> chatView.loadHistory((List<Message>) packet.getPayload());
                case WIKI_LOOKUP_RESPONSE -> discoverView.updateWiki((WikiResult) packet.getPayload());
                case TRIVIA_RESPONSE -> discoverView.updateTrivia((String) packet.getPayload());
                
                case DRAW_EVENT -> mindBoardView.applyDrawAction((model.DrawAction) packet.getPayload());
                
                // Phase 1: File Transfer
                case FILE_OFFER, FILE_ACCEPT, FILE_REJECT -> chatView.handleFilePacket(packet);

                // Phase 1: Vibe Sync
                case VIBE_SYNC -> handleVibeSync(packet);
                case WEATHER_RESPONSE -> {
                    WeatherData data = (WeatherData) packet.getPayload();
                    topStatus.setText("Logged in as " + currentUser.getUsername() + " | " + data.getShortDisplay());
                }
                case ERROR -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, packet.getPayload().toString());
                    a.show();
                }
                default -> {
                    // Ignore background packets intended for other controllers
                }
            }
        });
    }

    private javafx.scene.media.MediaPlayer currentVibePlayer = null;

    private void handleVibeSync(Packet packet) {
        if (currentVibePlayer != null) {
            currentVibePlayer.stop();
            currentVibePlayer.dispose();
        }

        model.MusicShare share = (model.MusicShare) packet.getPayload();
        String originalStatus = "Logged in as " + currentUser.getUsername();
        topStatus.setText("Vibe Ongoing: " + share.getTrackName() + " by " + share.getArtistName());
        
        if (share.getPreviewUrl() != null && !share.getPreviewUrl().isEmpty()) {
            try {
                currentVibePlayer = new javafx.scene.media.MediaPlayer(new javafx.scene.media.Media(share.getPreviewUrl()));
                currentVibePlayer.play();
                currentVibePlayer.setOnEndOfMedia(() -> {
                    Platform.runLater(() -> topStatus.setText(originalStatus));
                    currentVibePlayer.dispose();
                    currentVibePlayer = null;
                }); 
            } catch (Exception e) {
                topStatus.setText(originalStatus);
            }
        }
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> app.showLogin());
    }
}
