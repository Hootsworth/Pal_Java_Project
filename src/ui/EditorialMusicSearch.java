package ui;

import client.ServerConnection;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.MusicShare;
import model.Packet;

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
        
        VBox root = new VBox(20);
        root.setPadding(new javafx.geometry.Insets(24));
        root.getStyleClass().add("root");

        Label title = new Label("Search iTunes");
        title.getStyleClass().add("heading");

        HBox searchBar = new HBox(10);
        searchField = new TextField();
        searchField.setPromptText("Song, Artist, Album...");
        searchField.getStyleClass().add("text-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchBtn = new Button("SEARCH");
        searchBtn.getStyleClass().add("button-primary");
        searchBtn.setOnAction(e -> doSearch());
        searchField.setOnAction(e -> doSearch());
        searchBar.getChildren().addAll(searchField, searchBtn);

        resultsBox = new VBox(10);
        ScrollPane scroll = new ScrollPane(resultsBox);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(400);

        root.getChildren().addAll(title, searchBar, scroll);
        
        Scene scene = new Scene(root, 600, 500);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        setScene(scene);

        setOnCloseRequest(e -> connection.setListener(previousListener));
    }

    private void doSearch() {
        if (!searchField.getText().isEmpty()) {
            resultsBox.getChildren().clear();
            Label loading = new Label("Searching...");
            loading.getStyleClass().add("subheading");
            resultsBox.getChildren().add(loading);
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
            
            VBox info = new VBox(5);
            Label tName = new Label(share.getTrackName());
            tName.getStyleClass().add("subheading");
            Label aName = new Label(share.getArtistName() + " — " + share.getCollectionName());
            aName.getStyleClass().add("body-text");
            info.getChildren().addAll(tName, aName);
            HBox.setHgrow(info, Priority.ALWAYS);

            Button vibeBtn = new Button("VIBE");
            vibeBtn.getStyleClass().add("button-primary");
            vibeBtn.setOnAction(e -> {
                connection.send(new Packet(Packet.Type.VIBE_SYNC, share));
                connection.setListener(previousListener);
                close();
            });

            Button shareBtn = new Button("SHARE");
            shareBtn.getStyleClass().add("button-outline");
            shareBtn.setOnAction(e -> {
                connection.send(new Packet(Packet.Type.MUSIC_SHARE_POST, share));
                connection.setListener(previousListener);
                close();
            });

            row.getChildren().addAll(info, vibeBtn, shareBtn);
            resultsBox.getChildren().add(row);
        }
    }

    @Override
    public void onDisconnected() {
        previousListener.onDisconnected();
    }
}
