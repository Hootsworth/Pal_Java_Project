package ui;

import animatefx.animation.FadeIn;
import client.PalClient;
import client.ServerConnection;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import model.Message;
import model.Packet;
import model.Post;
import model.User;
import model.UserProfile;
import model.WeatherData;
import model.WikiResult;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditorialMain extends BorderPane implements ServerConnection.PacketListener {

    private final PalClient app;
    private final ServerConnection connection;
    private final User currentUser;

    private EditorialFeed feedView;
    private EditorialChat chatView;
    private EditorialDiscover discoverView;
    private EditorialMindBoard mindBoardView;
    private EditorialSettings settingsView;
    private ScrollPane discoverScroll;
    private ScrollPane settingsScroll;

    private final Map<String, UserProfile> profileCache = new HashMap<>();
    private List<String> latestOnlineUsers = new ArrayList<>();

    private Label topStatus;
    private StackPane userAvatarHolder;
    private Label userStatusLabel;
    private FontIcon weatherIcon;
    private FontIcon notifIcon;
    private Label sidebarUsername;
    private boolean darkModeEnabled = false;

    private ListView<NavEntry> navList;
    private NavEntry chatNavEntry;

    public EditorialMain(PalClient app, ServerConnection connection, User user) {
        this.app = app;
        this.connection = connection;
        this.currentUser = user;
        this.connection.setListener(this);
        initUI();

        connection.send(new Packet(Packet.Type.GET_FEED, null));
        connection.send(new Packet(Packet.Type.GET_ONLINE_USERS, null));
        connection.send(new Packet(Packet.Type.GET_PROFILE, currentUser.getUsername()));
        connection.send(new Packet(Packet.Type.WEATHER_REQUEST, null));
        connection.send(new Packet(Packet.Type.TRIVIA_REQUEST, null));
    }

    private void initUI() {
        getStyleClass().add("root");

        VBox sidebar = new VBox(14);
        sidebar.setPrefWidth(260);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(20, 14, 14, 14));

        Label logo = new Label("P A L");
        logo.getStyleClass().add("heading");
        logo.getStyleClass().add("sidebar-logo");

        navList = new ListView<>();
        ObservableList<NavEntry> items = FXCollections.observableArrayList(
            new NavEntry("Feed", "mdi2h-home-outline"),
            new NavEntry("Discover", "mdi2r-radar"),
            new NavEntry("Chat", "mdi2m-message-text-outline"),
            new NavEntry("Mind Board", "mdi2p-palette-outline"),
            new NavEntry("Settings", "mdi2c-cog-outline")
        );
        chatNavEntry = items.stream().filter(e -> "Chat".equals(e.name)).findFirst().orElse(null);

        navList.setItems(items);
        navList.getStyleClass().add("nav-list");
        navList.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(navList, Priority.ALWAYS);

        navList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(NavEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    FontIcon icon = new FontIcon(item.icon);
                    icon.setIconSize(18);
                    icon.getStyleClass().add("sidebar-nav-icon");

                    Label name = new Label(item.name);
                    name.getStyleClass().add("sidebar-nav-label");

                    Label unreadDot = new Label(" ");
                    unreadDot.setVisible(item.hasNotification);
                    unreadDot.setManaged(item.hasNotification);
                    unreadDot.getStyleClass().add("sidebar-unread-dot");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    HBox row = new HBox(10, icon, name, spacer, unreadDot);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.getStyleClass().add("sidebar-nav-row");
                    setGraphic(row);
                    setText(null);
                }
            }
        });

        HBox userProfile = new HBox(12);
        userProfile.setAlignment(Pos.CENTER_LEFT);
        userProfile.setPadding(new Insets(14));
        userProfile.getStyleClass().add("sidebar-profile-card");

        userAvatarHolder = new StackPane(AvatarFactory.createCircularAvatar(new UserProfile(currentUser.getUsername()), 38));

        VBox userInfo = new VBox(4);
        sidebarUsername = new Label(currentUser.getUsername());
        sidebarUsername.getStyleClass().add("sidebar-username");
        userStatusLabel = new Label("Online");
        userStatusLabel.getStyleClass().add("sidebar-status");
        userInfo.getChildren().addAll(sidebarUsername, userStatusLabel);

        Button logoutBtn = new Button("", new FontIcon("mdi2l-logout"));
        logoutBtn.getStyleClass().add("action-btn");
        logoutBtn.setOnAction(e -> app.showLogin());

        Region pSpacer = new Region();
        HBox.setHgrow(pSpacer, Priority.ALWAYS);

        userProfile.getChildren().addAll(userAvatarHolder, userInfo, pSpacer, logoutBtn);

        sidebar.getChildren().addAll(logo, navList, userProfile);

        HBox topbar = new HBox(15);
        topbar.setAlignment(Pos.CENTER_LEFT);
        topbar.getStyleClass().add("topbar");
        topbar.setPadding(new Insets(16, 24, 16, 24));

        weatherIcon = new FontIcon("mdi2w-weather-cloudy");
        weatherIcon.setIconSize(20);
        weatherIcon.getStyleClass().add("topbar-icon");

        topStatus = new Label("Weather: Loading...");
        topStatus.getStyleClass().add("subheading");

        Region tSpacer = new Region();
        HBox.setHgrow(tSpacer, Priority.ALWAYS);

        notifIcon = new FontIcon("mdi2b-bell-outline");
        notifIcon.setIconSize(20);
        notifIcon.getStyleClass().add("topbar-icon");

        topbar.getChildren().addAll(weatherIcon, topStatus, tSpacer, notifIcon);

        StackPane contentArea = new StackPane();
        contentArea.setPadding(new Insets(30));

        feedView = new EditorialFeed(currentUser, connection, this, this::getProfileFromCache);
        chatView = new EditorialChat(currentUser, connection, this::getProfileFromCache, this::refreshChatNavIndicator);
        discoverView = new EditorialDiscover(connection, currentUser, this::getProfileFromCache, this::requestProfile);
        mindBoardView = new EditorialMindBoard(connection);
        settingsView = new EditorialSettings(currentUser, connection, this::applyLocalProfile, this::setDarkModeEnabled, () -> darkModeEnabled);

        discoverScroll = createViewScroll(discoverView);
        settingsScroll = createViewScroll(settingsView);

        feedView.managedProperty().bind(feedView.visibleProperty());
        chatView.managedProperty().bind(chatView.visibleProperty());
        discoverScroll.managedProperty().bind(discoverScroll.visibleProperty());
        mindBoardView.managedProperty().bind(mindBoardView.visibleProperty());
        settingsScroll.managedProperty().bind(settingsScroll.visibleProperty());

        contentArea.getChildren().addAll(discoverScroll, chatView, feedView, mindBoardView, settingsScroll);

        navList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;

            feedView.setVisible("Feed".equals(newV.name));
            discoverScroll.setVisible("Discover".equals(newV.name));
            chatView.setVisible("Chat".equals(newV.name));
            mindBoardView.setVisible("Mind Board".equals(newV.name));
            settingsScroll.setVisible("Settings".equals(newV.name));

            if (feedView.isVisible()) new FadeIn(feedView).setSpeed(2.0).play();
            if (discoverScroll.isVisible()) new FadeIn(discoverScroll).setSpeed(2.0).play();
            if (chatView.isVisible()) {
                chatView.clearActiveUnread();
                refreshChatNavIndicator();
                new FadeIn(chatView).setSpeed(2.0).play();
            }
            if (mindBoardView.isVisible()) new FadeIn(mindBoardView).setSpeed(2.0).play();
            if (settingsScroll.isVisible()) {
                connection.send(new Packet(Packet.Type.GET_PROFILE, currentUser.getUsername()));
                new FadeIn(settingsScroll).setSpeed(2.0).play();
            }
        });
        navList.getSelectionModel().select(0);

        setLeft(sidebar);
        setTop(topbar);
        setCenter(contentArea);
        setDarkModeEnabled(false);
    }

    private ScrollPane createViewScroll(Region content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.getStyleClass().add("scroll-pane");
        return sp;
    }

    private void setDarkModeEnabled(boolean enabled) {
        darkModeEnabled = enabled;
        if (enabled) {
            if (!getStyleClass().contains("dark-mode")) {
                getStyleClass().add("dark-mode");
            }
        } else {
            getStyleClass().remove("dark-mode");
        }
        Color iconColor = darkModeEnabled ? Color.web("#f5f5f5") : Color.web("#0a0a0a");
        weatherIcon.setIconColor(iconColor);
        notifIcon.setIconColor(iconColor);
    }

    private UserProfile getProfileFromCache(String username) {
        return profileCache.get(username);
    }

    private void requestProfile(String username) {
        if (username == null || username.isBlank()) return;
        connection.send(new Packet(Packet.Type.GET_PROFILE, username));
    }

    private void applyLocalProfile(UserProfile profile) {
        mergeProfile(profile);
    }

    private void mergeProfile(UserProfile profile) {
        if (profile == null || profile.getUsername() == null) return;
        profileCache.put(profile.getUsername(), profile);

        if (profile.getUsername().equals(currentUser.getUsername())) {
            currentUser.setBio(profile.getBio());
            currentUser.setStatusEmoji(profile.getStatusEmoji());
            currentUser.setStatusText(profile.getStatusText());
            currentUser.setAvatarStyle(profile.getAvatarStyle());
            currentUser.setAvatarSeed(profile.getAvatarSeed());

            ImageView avatar = AvatarFactory.createCircularAvatar(profile, 38);
            userAvatarHolder.getChildren().setAll(avatar);
            String statusText = profile.getDisplayStatus().isBlank() ? "Online" : profile.getDisplayStatus();
            userStatusLabel.setText(statusText);
        }

        if (chatView != null) {
            chatView.updateOnline(latestOnlineUsers);
        }
        if (discoverView != null) {
            discoverView.updateOnlineUsers(latestOnlineUsers);
        }
        if (feedView != null) {
            feedView.updateFeed(new ArrayList<>(feedViewPostsCache));
        }
    }

    private List<Post> feedViewPostsCache = new ArrayList<>();

    private void refreshChatNavIndicator() {
        if (chatNavEntry == null) return;
        chatNavEntry.hasNotification = chatView.hasUnreadMessages();
        navList.refresh();
    }

    private static class NavEntry {
        String name;
        String icon;
        boolean hasNotification;

        NavEntry(String name, String icon) {
            this.name = name;
            this.icon = icon;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onPacketReceived(Packet packet) {
        Platform.runLater(() -> {
            switch (packet.getType()) {
                case FEED_RESPONSE -> {
                    List<Post> posts = (List<Post>) packet.getPayload();
                    feedViewPostsCache = posts;
                    feedView.updateFeed(posts);
                    discoverView.updateFeedSnapshot(posts);
                    for (Post post : posts) {
                        requestProfile(post.getAuthor());
                        for (Post reply : post.getReplies()) {
                            requestProfile(reply.getAuthor());
                        }
                    }
                }
                case ONLINE_USERS_RESPONSE -> {
                    List<String> online = (List<String>) packet.getPayload();
                    latestOnlineUsers = new ArrayList<>(online);
                    chatView.updateOnline(online);
                    discoverView.updateOnlineUsers(online);
                    for (String username : online) {
                        requestProfile(username);
                    }
                }
                case CHAT_MESSAGE -> {
                    Message msg = (Message) packet.getPayload();
                    requestProfile(msg.getFrom());
                    boolean isChatVisible = chatView.isVisible();
                    chatView.receiveMessage(msg, isChatVisible);
                    refreshChatNavIndicator();
                }
                case CHAT_HISTORY_RESPONSE -> chatView.loadHistory((List<Message>) packet.getPayload());
                case PROFILE_RESPONSE -> {
                    UserProfile profile = (UserProfile) packet.getPayload();
                    mergeProfile(profile);
                    if (profile.getUsername().equals(currentUser.getUsername())) {
                        settingsView.populateFrom(profile);
                    }
                }
                case WIKI_LOOKUP_RESPONSE -> {
                    WikiResult result = (WikiResult) packet.getPayload();
                    discoverView.updateWiki(result);
                    chatView.handleWikiResult(result);
                }
                case TRIVIA_RESPONSE -> discoverView.updateTrivia((String) packet.getPayload());
                case DRAW_EVENT -> mindBoardView.applyDrawAction((model.DrawAction) packet.getPayload());
                case FILE_OFFER, FILE_ACCEPT, FILE_REJECT -> chatView.handleFilePacket(packet);
                case VIBE_SYNC -> handleVibeSync(packet);
                case WEATHER_RESPONSE -> {
                    WeatherData data = (WeatherData) packet.getPayload();
                    topStatus.setText(data.getShortDisplay());
                    FontIcon icon = (FontIcon) topStatus.getParent().getChildrenUnmodifiable().get(0);
                    String desc = data.getDescription().toLowerCase();
                    if (desc.contains("clear")) icon.setIconLiteral("mdi2w-weather-sunny");
                    else if (desc.contains("rain")) icon.setIconLiteral("mdi2w-weather-pouring");
                    else icon.setIconLiteral("mdi2w-weather-cloudy");
                }
                case ERROR -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, packet.getPayload().toString());
                    a.show();
                }
                default -> {
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
        String originalStatus = topStatus.getText();
        topStatus.setText("Vibe Ongoing: " + share.getTrackName() + " by " + share.getArtistName());
        FontIcon icon = (FontIcon) topStatus.getParent().getChildrenUnmodifiable().get(0);
        String originalIcon = icon.getIconLiteral();
        icon.setIconLiteral("mdi2m-music");
        icon.setIconColor(darkModeEnabled ? Color.web("#f5f5f5") : Color.web("#0a0a0a"));

        if (share.getPreviewUrl() != null && !share.getPreviewUrl().isEmpty()) {
            try {
                currentVibePlayer = new javafx.scene.media.MediaPlayer(new javafx.scene.media.Media(share.getPreviewUrl()));
                currentVibePlayer.play();
                currentVibePlayer.setOnEndOfMedia(() -> {
                    Platform.runLater(() -> {
                        topStatus.setText(originalStatus);
                        icon.setIconLiteral(originalIcon);
                        icon.setIconColor(darkModeEnabled ? Color.web("#f5f5f5") : Color.web("#0a0a0a"));
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
