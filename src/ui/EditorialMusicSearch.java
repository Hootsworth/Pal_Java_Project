package ui;

import client.ServerConnection;
import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.MusicShare;
import model.Packet;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class EditorialMusicSearch extends Stage implements ServerConnection.PacketListener {

    private final ServerConnection connection;
    private final ServerConnection.PacketListener previousListener;
    private final VBox resultsBox;
    private final TextField searchField;

    public EditorialMusicSearch(ServerConnection connection, ServerConnection.PacketListener currentListener) {
        this.connection = connection;
        this.previousListener = currentListener;
        this.connection.setListener(this); // temporarily hijack packets

        initOwner(null);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Search iTunes Music");
        
        VBox root = new VBox(24);
        root.setPadding(new javafx.geometry.Insets(30));
        root.getStyleClass().add("root");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        FontIcon appleIcon = new FontIcon("mdi2a-apple");
        appleIcon.setIconColor(Color.web("#FFFFFF"));
        appleIcon.setIconSize(32);
        Label title = new Label("Search iTunes");
        title.getStyleClass().add("heading");
        header.getChildren().addAll(appleIcon, title);

        HBox searchBar = new HBox(15);
        searchBar.setAlignment(Pos.CENTER);
        searchField = new TextField();
        searchField.setPromptText("Song, Artist, Album...");
        searchField.getStyleClass().add("text-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchBtn = new Button("Search", new FontIcon("mdi2m-magnify"));
        searchBtn.getStyleClass().add("button-primary");
        searchBtn.setOnAction(e -> doSearch());
        searchField.setOnAction(e -> doSearch());
        searchBar.getChildren().addAll(searchField, searchBtn);

        resultsBox = new VBox(12);
        ScrollPane scroll = new ScrollPane(resultsBox);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(450);
        scroll.getStyleClass().add("scroll-pane");

        root.getChildren().addAll(header, searchBar, scroll);
        
        Scene scene = new Scene(root, 650, 600);
        // Important: Load both PrimerDark and our overlay
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        setScene(scene);

        setOnCloseRequest(e -> connection.setListener(previousListener));
    }

    private void doSearch() {
        if (!searchField.getText().isEmpty()) {
            resultsBox.getChildren().clear();
            HBox loader = new HBox(10);
            loader.setAlignment(Pos.CENTER);
            FontIcon loadingIcon = new FontIcon("mdi2l-loading");
            loadingIcon.setIconSize(24);
            loadingIcon.setIconColor(Color.web("#58A6FF"));
            Label loading = new Label("Searching...");
            loading.getStyleClass().add("subheading");
            loader.getChildren().addAll(loadingIcon, loading);
            resultsBox.getChildren().add(loader);
            connection.send(new Packet(Packet.Type.MUSIC_SEARCH_REQUEST, searchField.getText()));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onPacketReceived(Packet packet) {
        Platform.runLater(() -> {
            if (packet.getType() == Packet.Type.MUSIC_SEARCH_RESPONSE) {
                renderResults((List<MusicShare>) packet.getPayload());
            } else {
                // Pass it down
                previousListener.onPacketReceived(packet);
            }
        });
    }

    private void renderResults(List<MusicShare> list) {
        resultsBox.getChildren().clear();
        if (list.isEmpty()) {
            Label noRes = new Label("No results found.");
            noRes.getStyleClass().add("body-text");
            resultsBox.getChildren().add(noRes);
            return;
        }

        for (MusicShare share : list) {
            HBox row = new HBox(15);
            row.getStyleClass().add("editorial-card");
            row.setAlignment(Pos.CENTER_LEFT);
            
            FontIcon musicIcon = new FontIcon("mdi2m-music-circle-outline");
            musicIcon.setIconColor(Color.web("#58A6FF"));
            musicIcon.setIconSize(36);
            
            VBox info = new VBox(5);
            Label tName = new Label(share.getTrackName());
            tName.getStyleClass().add("subheading");
            tName.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
            Label aName = new Label(share.getArtistName() + " — " + share.getCollectionName());
            aName.getStyleClass().add("body-text");
            info.getChildren().addAll(tName, aName);
            HBox.setHgrow(info, Priority.ALWAYS);

            Button vibeBtn = new Button("Vibe", new FontIcon("mdi2w-waveform"));
            vibeBtn.getStyleClass().add("button-primary");
            vibeBtn.setOnAction(e -> {
                connection.send(new Packet(Packet.Type.VIBE_SYNC, share));
                connection.setListener(previousListener);
                close();
            });

            Button shareBtn = new Button("Share", new FontIcon("mdi2s-share"));
            shareBtn.getStyleClass().add("button-outline");
            shareBtn.setOnAction(e -> {
                connection.send(new Packet(Packet.Type.MUSIC_SHARE_POST, share));
                connection.setListener(previousListener);
                close();
            });

            row.getChildren().addAll(musicIcon, info, vibeBtn, shareBtn);
            resultsBox.getChildren().add(row);
        }
    }

    @Override
    public void onDisconnected() {
        previousListener.onDisconnected();
    }
}
