package ui;

import client.ServerConnection;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import model.Packet;
import model.Post;
import model.User;

import java.util.List;

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
        VBox composeBox = new VBox(10);
        composeBox.getStyleClass().add("editorial-card");
        
        composer = new TextArea();
        composer.setPromptText("What's on your mind?");
        composer.getStyleClass().add("text-area");
        composer.setPrefRowCount(3);

        Button publishBtn = new Button("PUBLISH");
        publishBtn.getStyleClass().add("button-primary");

        Button searchMusicBtn = new Button("SEARCH iTUNES");
        searchMusicBtn.getStyleClass().add("button-outline");
        searchMusicBtn.setOnAction(e -> {
            new EditorialMusicSearch(connection, mainListener).show();
        });

        HBox actions = new HBox(publishBtn, searchMusicBtn);
        actions.setSpacing(10);

        CheckBox marketToggle = new CheckBox("MARKETPLACE POST");
        marketToggle.getStyleClass().add("subheading");
        
        TextField priceField = new TextField();
        priceField.setPromptText("Price (e.g. $10 or Lend)");
        priceField.setVisible(false);
        priceField.getStyleClass().add("text-field");
        priceField.setMaxWidth(150);

        marketToggle.selectedProperty().addListener((obs, oldV, newV) -> priceField.setVisible(newV));

        publishBtn.setOnAction(e -> {
            if (!composer.getText().isEmpty()) {
                Post p = new Post(user.getUsername(), composer.getText(), 
                    marketToggle.isSelected() ? Post.PostType.MARKETPLACE : Post.PostType.SOCIAL);
                if (marketToggle.isSelected() && !priceField.getText().isEmpty()) {
                    p.setMetadata("price", priceField.getText());
                }
                connection.send(new Packet(Packet.Type.NEW_POST, p));
                composer.clear();
                priceField.clear();
                marketToggle.setSelected(false);
            }
        });

        VBox composerInternal = new VBox(10, composer, new HBox(15, marketToggle, priceField), actions);
        composeBox.getChildren().addAll(composerInternal);

        // Filter Bar
        HBox filterBar = new HBox(15);
        filterBar.getStyleClass().add("filter-bar");
        Button allBtn = new Button("ALL");
        Button socialBtn = new Button("SOCIAL");
        Button marketBtn = new Button("MARKETPLACE");
        
        allBtn.getStyleClass().add("button-outline");
        socialBtn.getStyleClass().add("button-outline");
        marketBtn.getStyleClass().add("button-outline");

        allBtn.setOnAction(e -> applyFilter(null));
        socialBtn.setOnAction(e -> applyFilter(Post.PostType.SOCIAL));
        marketBtn.setOnAction(e -> applyFilter(Post.PostType.MARKETPLACE));

        filterBar.getChildren().addAll(new Label("Filter:"), allBtn, socialBtn, marketBtn);

        // Feed list
        feedContainer = new VBox(16);
        ScrollPane scroll = new ScrollPane(feedContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

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
        for (Post p : allPostsCache) {
            if (currentFilter != null && p.getType() != currentFilter) continue;

            VBox card = new VBox(8);
            card.getStyleClass().add("editorial-card");
            
            HBox header = new HBox();
            Label author = new Label(p.getAuthor());
            author.getStyleClass().add("subheading");
            header.getChildren().add(author);
            
            if (p.getType() == Post.PostType.MARKETPLACE) {
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                Label badge = new Label("MARKETPLACE");
                badge.getStyleClass().add("marketplace-badge");
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

            if (p.getType() == Post.PostType.MARKETPLACE && p.getMetadata().containsKey("price")) {
                Label price = new Label(p.getMetadata().get("price"));
                price.getStyleClass().add("price-tag");
                card.getChildren().add(price);
            }

            if (previewUrl != null) {
                Button playBtn = createPlayButton(previewUrl);
                card.getChildren().add(playBtn);
            }

            feedContainer.getChildren().add(card);
        }
    }

    private Button createPlayButton(String url) {
        Button playObj = new Button("▶ Play Preview");
        playObj.getStyleClass().add("button-outline");
        playObj.setStyle("-fx-text-fill: #6c63ff; -fx-border-color: #6c63ff;");
        
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
}
