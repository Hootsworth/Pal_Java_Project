package ui;

import client.ServerConnection;
import animatefx.animation.FadeInUp;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import model.Packet;
import model.Post;
import model.User;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class EditorialFeed extends VBox {

    private final ServerConnection connection;
    private final ServerConnection.PacketListener mainListener;
    private final User user;
    private final VBox feedContainer;
    private final TextArea composer;
    private Post.PostType currentFilter = null; // null means All
    private List<Post> allPostsCache = new java.util.ArrayList<>();

    public EditorialFeed(User user, ServerConnection connection, ServerConnection.PacketListener mainListener) {
        this.user = user;
        this.connection = connection;
        this.mainListener = mainListener;
        setSpacing(24);

        Label title = new Label("Feed");
        title.getStyleClass().add("heading");

        // Composer
        VBox composeBox = new VBox(15);
        composeBox.getStyleClass().add("editorial-card");
        
        HBox composeHeader = new HBox(10);
        composeHeader.setAlignment(Pos.CENTER_LEFT);
        FontIcon userIcon = new FontIcon("mdi2a-account-circle");
        userIcon.setIconSize(24);
        userIcon.setIconColor(Color.web("#8B949E"));
        Label composeTitle = new Label("Create Post");
        composeTitle.getStyleClass().add("subheading");
        composeHeader.getChildren().addAll(userIcon, composeTitle);

        composer = new TextArea();
        composer.setPromptText("What's on your mind, " + user.getUsername() + "?");
        composer.getStyleClass().add("text-area");
        composer.setPrefRowCount(3);

        Button publishBtn = new Button("POST", new FontIcon("mdi2s-send"));
        publishBtn.getStyleClass().add("button-primary");

        Button searchMusicBtn = new Button("MUSIC", new FontIcon("mdi2m-music"));
        searchMusicBtn.getStyleClass().add("button-outline");
        searchMusicBtn.setOnAction(e -> {
            new EditorialMusicSearch(this.connection, this.mainListener).show();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(publishBtn, spacer, searchMusicBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        CheckBox marketToggle = new CheckBox("MARKETPLACE POST");
        marketToggle.getStyleClass().add("subheading");
        
        TextField priceField = new TextField();
        priceField.setPromptText("Price (e.g. $10)");
        priceField.setVisible(false);
        priceField.getStyleClass().add("text-field");
        priceField.setMaxWidth(150);

        marketToggle.selectedProperty().addListener((obs, oldV, newV) -> priceField.setVisible(newV));
        HBox marketBox = new HBox(15, marketToggle, priceField);
        marketBox.setAlignment(Pos.CENTER_LEFT);

        publishBtn.setOnAction(e -> {
            if (!composer.getText().isEmpty()) {
                Post p = new Post(this.user.getUsername(), composer.getText(), 
                    marketToggle.isSelected() ? Post.PostType.MARKETPLACE : Post.PostType.SOCIAL);
                if (marketToggle.isSelected() && !priceField.getText().isEmpty()) {
                    p.setMetadata("price", priceField.getText());
                }
                this.connection.send(new Packet(Packet.Type.NEW_POST, p));
                composer.clear();
                priceField.clear();
                marketToggle.setSelected(false);
            }
        });

        composeBox.getChildren().addAll(composeHeader, composer, marketBox, actions);

        // Filter Bar
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        
        ToggleGroup filterGroup = new ToggleGroup();
        ToggleButton allBtn = new ToggleButton("All");
        ToggleButton socialBtn = new ToggleButton("Social");
        ToggleButton marketBtn = new ToggleButton("Marketplace");
        
        allBtn.setToggleGroup(filterGroup);
        socialBtn.setToggleGroup(filterGroup);
        marketBtn.setToggleGroup(filterGroup);
        
        allBtn.getStyleClass().addAll("button-outline", "reaction-pill");
        socialBtn.getStyleClass().addAll("button-outline", "reaction-pill");
        marketBtn.getStyleClass().addAll("button-outline", "reaction-pill");
        
        filterGroup.selectToggle(allBtn);

        allBtn.setOnAction(e -> applyFilter(null));
        socialBtn.setOnAction(e -> applyFilter(Post.PostType.SOCIAL));
        marketBtn.setOnAction(e -> applyFilter(Post.PostType.MARKETPLACE));

        filterBar.getChildren().addAll(new Label("Filter:") {{ getStyleClass().add("subheading"); }}, allBtn, socialBtn, marketBtn);

        // Feed list
        feedContainer = new VBox(16);
        ScrollPane scroll = new ScrollPane(feedContainer);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");

        getChildren().addAll(title, composeBox, filterBar, scroll);
    }

    private void applyFilter(Post.PostType type) {
        this.currentFilter = type;
        renderFeed();
    }

    public void updateFeed(List<Post> posts) {
        this.allPostsCache = posts;
        renderFeed();
    }

    private void renderFeed() {
        feedContainer.getChildren().clear();
        int delay = 0;
        for (Post p : allPostsCache) {
            // Only render top-level posts here; replies are rendered inside the parent loop
            if (p.getParentPostId() != null) continue;
            if (currentFilter != null && p.getType() != currentFilter) continue;

            VBox postNode = createPostNode(p, false);
            
            // Add replies if any
            if (!p.getReplies().isEmpty()) {
                VBox repliesContainer = new VBox(10);
                repliesContainer.setPadding(new Insets(10, 0, 0, 40));
                for (Post reply : p.getReplies()) {
                    repliesContainer.getChildren().add(createPostNode(reply, true));
                }
                postNode.getChildren().add(repliesContainer);
            }

            FadeInUp anim = new FadeInUp(postNode);
            anim.setSpeed(1.5);
            anim.setDelay(javafx.util.Duration.millis(delay += 50));
            feedContainer.getChildren().add(postNode);
            anim.play();
        }
    }

    private VBox createPostNode(Post p, boolean isReply) {
        VBox card = new VBox(12);
        if (isReply) {
            card.getStyleClass().add("reply-card");
        } else {
            card.getStyleClass().add("editorial-card");
            card.getStyleClass().add("post-card");
        }
        
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon pIcon = new FontIcon("mdi2a-account-circle");
        pIcon.setIconColor(Color.web("#58A6FF"));
        
        Label author = new Label(p.getAuthor());
        author.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
        
        Label time = new Label(p.getFormattedTime());
        time.getStyleClass().add("subheading");
        time.setStyle("-fx-font-size: 11px;");
        
        header.getChildren().addAll(pIcon, author, time);
        
        if (!isReply && p.getType() == Post.PostType.MARKETPLACE) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label badge = new Label("MARKETPLACE");
            badge.setStyle("-fx-background-color: #D29922; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");
            header.getChildren().addAll(spacer, badge);
        }
        
        String rawText = p.getContent();
        String previewUrl = null;
        
        int pIdx = rawText.indexOf("[PREVIEW:");
        if (pIdx >= 0) {
            int endIdx = rawText.indexOf("]", pIdx);
            if (endIdx >= pIdx + 9) {
                previewUrl = rawText.substring(pIdx + 9, endIdx);
                rawText = rawText.substring(0, pIdx).trim();
            }
        }

        Label content = new Label(rawText);
        content.getStyleClass().add("body-text");
        content.setWrapText(true);

        card.getChildren().addAll(header, content);

        if (!isReply && p.getType() == Post.PostType.MARKETPLACE && p.getMetadata().containsKey("price")) {
            Label price = new Label(p.getMetadata().get("price"));
            price.setStyle("-fx-text-fill: #3FB950; -fx-font-size: 18px; -fx-font-weight: bold;");
            card.getChildren().add(price);
        }

        if (previewUrl != null) {
            Button playBtn = createPlayButton(previewUrl);
            card.getChildren().add(playBtn);
        }

        // Action Bar (Reactions, Reply, Share, Report)
        HBox actionBar = new HBox(15);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        
        // Reactions
        HBox reactionBox = new HBox(8);
        Map<String, Set<String>> reactions = p.getReactions();
        for (String emoji : Post.REACTION_EMOJIS) {
            int count = p.getReactionCount(emoji);
            boolean active = p.hasUserReacted(emoji, user.getUsername());
            
            Button rBtn = new Button(emoji + (count > 0 ? " " + count : ""));
            rBtn.getStyleClass().add("reaction-pill");
            if (active) rBtn.getStyleClass().add("reaction-pill-active");
            rBtn.setOnAction(e -> connection.send(new Packet(Packet.Type.REACT_POST, new String[]{p.getPostId(), emoji})));
            reactionBox.getChildren().add(rBtn);
        }

        Region aSpacer = new Region();
        HBox.setHgrow(aSpacer, Priority.ALWAYS);
        
        // Reply Button
        Button replyBtn = new Button("Reply", new FontIcon("mdi2r-reply"));
        replyBtn.getStyleClass().add("action-btn");
        
        VBox replyBox = new VBox(8);
        HBox replyInputBox = new HBox(8);
        TextField replyFld = new TextField();
        replyFld.setPromptText("Write a reply...");
        replyFld.getStyleClass().add("text-field");
        HBox.setHgrow(replyFld, Priority.ALWAYS);
        Button sendReplyBtn = new Button("", new FontIcon("mdi2s-send"));
        sendReplyBtn.getStyleClass().add("button-primary");
        replyInputBox.getChildren().addAll(replyFld, sendReplyBtn);
        replyBox.setVisible(false);
        replyBox.setManaged(false);
        replyBox.getChildren().add(replyInputBox);
        
        replyBtn.setOnAction(e -> {
            boolean isVisible = !replyBox.isVisible();
            replyBox.setVisible(isVisible);
            replyBox.setManaged(isVisible);
        });

        sendReplyBtn.setOnAction(e -> {
            if (!replyFld.getText().isEmpty()) {
                Post reply = new Post(user.getUsername(), replyFld.getText());
                reply.setParentPostId(p.getPostId());
                connection.send(new Packet(Packet.Type.REPLY_POST, reply));
                replyFld.clear();
                replyBox.setVisible(false);
                replyBox.setManaged(false);
            }
        });

        // Share Button
        Button shareBtn = new Button("Share", new FontIcon("mdi2s-share-variant"));
        shareBtn.getStyleClass().add("action-btn");
        if (p.getShareCount() > 0) shareBtn.setText("Share (" + p.getShareCount() + ")");
        shareBtn.setOnAction(e -> {
            connection.send(new Packet(Packet.Type.SHARE_POST, p.getPostId()));
        });

        // Report Button
        Button reportBtn = new Button("Report", new FontIcon("mdi2f-flag-outline"));
        reportBtn.getStyleClass().add("action-btn");
        if (p.hasReported(user.getUsername())) {
            reportBtn.setText("Reported");
            reportBtn.setDisable(true);
        }
        reportBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("");
            dialog.setTitle("Report Post");
            dialog.setHeaderText("Why are you reporting this post?");
            dialog.setContentText("Reason:");
            dialog.showAndWait().ifPresent(reason -> {
                connection.send(new Packet(Packet.Type.REPORT_POST, new String[]{p.getPostId(), user.getUsername(), reason}));
                reportBtn.setText("Reported");
                reportBtn.setDisable(true);
            });
        });

        if (!isReply) {
            actionBar.getChildren().addAll(reactionBox, aSpacer, replyBtn, shareBtn, reportBtn);
        } else {
            actionBar.getChildren().addAll(reactionBox, aSpacer, reportBtn);
        }
        
        card.getChildren().addAll(actionBar, replyBox);

        return card;
    }

    private Button createPlayButton(String url) {
        Button playObj = new Button("Play Audio", new FontIcon("mdi2p-play-circle"));
        playObj.getStyleClass().add("button-outline");
        playObj.setStyle("-fx-border-radius: 20px; -fx-background-radius: 20px;");
        
        MediaPlayer player = null;
        try {
            player = new MediaPlayer(new Media(url));
        } catch (Exception ex) {
            playObj.setText("Playback Error");
            playObj.setDisable(true);
            return playObj;
        }

        final MediaPlayer mp = player;
        playObj.setOnAction(e -> {
            if (mp.getStatus() == MediaPlayer.Status.PLAYING) {
                mp.pause();
                playObj.setText("Play Audio");
                playObj.setGraphic(new FontIcon("mdi2p-play-circle"));
            } else {
                mp.play();
                playObj.setText("Pause");
                playObj.setGraphic(new FontIcon("mdi2p-pause-circle"));
            }
        });

        mp.setOnEndOfMedia(() -> {
            mp.stop();
            playObj.setText("Play Audio");
            playObj.setGraphic(new FontIcon("mdi2p-play-circle"));
        });

        return playObj;
    }
}
