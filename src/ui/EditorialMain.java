package ui;

import client.PalClient;
import client.ServerConnection;
import animatefx.animation.FadeIn;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import model.Message;
import model.Packet;
import model.Post;
import model.User;
import model.WeatherData;
import model.WikiResult;
import org.kordamp.ikonli.javafx.FontIcon;

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
        sidebar.setPrefWidth(240);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(30, 0, 0, 0));

        Label logo = new Label("P A L");
        logo.getStyleClass().add("heading");
        logo.setPadding(new Insets(0, 0, 20, 24));

        ListView<NavEntry> navList = new ListView<>();
        ObservableList<NavEntry> items = FXCollections.observableArrayList(
            new NavEntry("Feed", "mdi2h-home-outline"),
            new NavEntry("Discover", "mdi2c-compass-outline"),
            new NavEntry("Chat", "mdi2m-message-text-outline"),
            new NavEntry("Mind Board", "mdi2p-palette-outline")
        );
        navList.setItems(items);
        navList.getStyleClass().add("nav-list");
        navList.setPrefHeight(300);
        
        navList.setCellFactory(lv -> new ListCell<NavEntry>() {
            @Override
            protected void updateItem(NavEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.name);
                    FontIcon icon = new FontIcon(item.icon);
                    icon.setIconSize(20);
                    // Color updates based on selection via CSS, but we can set default
                    setGraphic(icon);
                }
            }
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Sidebar Footer User Profile
        HBox userProfile = new HBox(12);
        userProfile.setAlignment(Pos.CENTER_LEFT);
        userProfile.setPadding(new Insets(20));
        userProfile.setStyle("-fx-border-color: rgba(255,255,255,0.05); -fx-border-width: 1 0 0 0;");
        
        FontIcon avatar = new FontIcon("mdi2a-account-circle");
        avatar.setIconSize(36);
        avatar.setIconColor(Color.web("#58A6FF"));
        
        VBox userInfo = new VBox(2);
        Label uName = new Label(currentUser.getUsername());
        uName.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        Label uStat = new Label("Online");
        uStat.setStyle("-fx-text-fill: #10B981; -fx-font-size: 12px;");
        userInfo.getChildren().addAll(uName, uStat);

        Button logoutBtn = new Button("", new FontIcon("mdi2l-logout"));
        logoutBtn.getStyleClass().add("action-btn");
        logoutBtn.setOnAction(e -> app.showLogin());

        Region pSpacer = new Region();
        HBox.setHgrow(pSpacer, Priority.ALWAYS);

        userProfile.getChildren().addAll(avatar, userInfo, pSpacer, logoutBtn);

        sidebar.getChildren().addAll(logo, navList, spacer, userProfile);

        // ── Topbar ──
        HBox topbar = new HBox(15);
        topbar.setAlignment(Pos.CENTER_LEFT);
        topbar.getStyleClass().add("topbar");
        topbar.setPadding(new Insets(16, 24, 16, 24));
        
        FontIcon weatherIcon = new FontIcon("mdi2w-weather-cloudy");
        weatherIcon.setIconColor(Color.web("#8B949E"));
        weatherIcon.setIconSize(20);
        
        topStatus = new Label("Weather: Loading...");
        topStatus.getStyleClass().add("subheading");
        
        Region tSpacer = new Region();
        HBox.setHgrow(tSpacer, Priority.ALWAYS);
        
        FontIcon notifIcon = new FontIcon("mdi2b-bell-outline");
        notifIcon.setIconColor(Color.web("#8B949E"));
        notifIcon.setIconSize(20);

        topbar.getChildren().addAll(weatherIcon, topStatus, tSpacer, notifIcon);

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
            if (newV != null) {
                feedView.setVisible("Feed".equals(newV.name));
                discoverView.setVisible("Discover".equals(newV.name));
                chatView.setVisible("Chat".equals(newV.name));
                mindBoardView.setVisible("Mind Board".equals(newV.name));
                
                // Crossfade animation
                if (feedView.isVisible()) new FadeIn(feedView).setSpeed(2.0).play();
                if (discoverView.isVisible()) new FadeIn(discoverView).setSpeed(2.0).play();
                if (chatView.isVisible()) new FadeIn(chatView).setSpeed(2.0).play();
                if (mindBoardView.isVisible()) new FadeIn(mindBoardView).setSpeed(2.0).play();
            }
        });
        navList.getSelectionModel().select(0);

        setLeft(sidebar);
        setTop(topbar);
        setCenter(contentArea);
    }

    private static class NavEntry {
        String name;
        String icon;
        NavEntry(String name, String icon) { this.name = name; this.icon = icon; }
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
                
                case FILE_OFFER, FILE_ACCEPT, FILE_REJECT -> chatView.handleFilePacket(packet);
                case VIBE_SYNC -> handleVibeSync(packet);
                case WEATHER_RESPONSE -> {
                    WeatherData data = (WeatherData) packet.getPayload();
                    topStatus.setText(data.getShortDisplay());
                    // Update icon dynamically
                    FontIcon icon = (FontIcon) topStatus.getParent().getChildrenUnmodifiable().get(0);
                    if (data.getDescription().toLowerCase().contains("clear")) icon.setIconLiteral("mdi2w-weather-sunny");
                    else if (data.getDescription().toLowerCase().contains("rain")) icon.setIconLiteral("mdi2w-weather-pouring");
                    else icon.setIconLiteral("mdi2w-weather-cloudy");
                }
                case ERROR -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, packet.getPayload().toString());
                    a.show();
                }
                default -> {}
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
        String originalStatus = topStatus.getText();
        topStatus.setText("Vibe Ongoing: " + share.getTrackName() + " by " + share.getArtistName());
        FontIcon icon = (FontIcon) topStatus.getParent().getChildrenUnmodifiable().get(0);
        String originalIcon = icon.getIconLiteral();
        icon.setIconLiteral("mdi2m-music");
        icon.setIconColor(Color.web("#8A2BE2"));
        
        if (share.getPreviewUrl() != null && !share.getPreviewUrl().isEmpty()) {
            try {
                currentVibePlayer = new javafx.scene.media.MediaPlayer(new javafx.scene.media.Media(share.getPreviewUrl()));
                currentVibePlayer.play();
                currentVibePlayer.setOnEndOfMedia(() -> {
                    Platform.runLater(() -> {
                        topStatus.setText(originalStatus);
                        icon.setIconLiteral(originalIcon);
                        icon.setIconColor(Color.web("#8B949E"));
                    });
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
