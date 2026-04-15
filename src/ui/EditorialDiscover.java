package ui;

import animatefx.animation.FadeIn;
import client.ServerConnection;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import model.Packet;
import model.Post;
import model.User;
import model.UserProfile;
import model.WikiResult;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EditorialDiscover extends VBox {
    private final ServerConnection connection;
    private final User currentUser;
    private final Function<String, UserProfile> profileResolver;
    private final Consumer<String> profileRequester;

    private final Label networkSummary;
    private final Label feedPulse;
    private final FlowPane peopleGrid;
    private final Label spotlightName;
    private final Label spotlightMeta;
    private final Label signalTrivia;
    private final Label signalWiki;

    public EditorialDiscover(ServerConnection connection,
                            User currentUser,
                            Function<String, UserProfile> profileResolver,
                            Consumer<String> profileRequester) {
        this.connection = connection;
        this.currentUser = currentUser;
        this.profileResolver = profileResolver;
        this.profileRequester = profileRequester;
        setSpacing(24);

        Label title = new Label("Discover: Network Radar");
        title.getStyleClass().add("heading");

        VBox overviewCard = new VBox(16);
        overviewCard.getStyleClass().add("editorial-card");
        HBox overviewHeader = sectionHeader("mdi2r-radar", "Network Pulse", "Refresh");
        ((Button) overviewHeader.getChildren().get(3)).setOnAction(e -> {
            connection.send(new Packet(Packet.Type.GET_ONLINE_USERS, null));
            connection.send(new Packet(Packet.Type.GET_FEED, null));
        });

        networkSummary = new Label("Waiting for network data...");
        networkSummary.getStyleClass().add("body-text");
        networkSummary.setWrapText(true);

        feedPulse = new Label("No activity yet.");
        feedPulse.getStyleClass().add("body-text");
        feedPulse.setWrapText(true);

        overviewCard.getChildren().addAll(overviewHeader, networkSummary, feedPulse);

        VBox peopleCard = new VBox(16);
        peopleCard.getStyleClass().add("editorial-card");
        HBox peopleHeader = sectionHeader("mdi2a-account-group-outline", "Who Is On This LAN", null);

        peopleGrid = new FlowPane();
        peopleGrid.setHgap(12);
        peopleGrid.setVgap(12);
        peopleGrid.setPrefWrapLength(850);

        peopleCard.getChildren().addAll(peopleHeader, peopleGrid);

        VBox profileLensCard = new VBox(16);
        profileLensCard.getStyleClass().add("editorial-card");
        HBox lensHeader = sectionHeader("mdi2m-magnify", "Profile Lens", null);

        HBox lookupRow = new HBox(10);
        TextField lookupField = new TextField();
        lookupField.setPromptText("Lookup username...");
        lookupField.getStyleClass().add("text-field");
        HBox.setHgrow(lookupField, Priority.ALWAYS);

        Button lookupBtn = new Button("Inspect", new FontIcon("mdi2c-card-account-details-outline"));
        lookupBtn.getStyleClass().add("button-primary");
        lookupBtn.setOnAction(e -> inspectUser(lookupField.getText().trim()));
        lookupField.setOnAction(e -> inspectUser(lookupField.getText().trim()));

        lookupRow.getChildren().addAll(lookupField, lookupBtn);

        spotlightName = new Label("No profile selected");
        spotlightName.getStyleClass().add("subheading");
        spotlightMeta = new Label("Search for someone online to inspect their public profile.");
        spotlightMeta.getStyleClass().add("body-text");
        spotlightMeta.setWrapText(true);

        profileLensCard.getChildren().addAll(lensHeader, lookupRow, spotlightName, spotlightMeta);

        VBox signalsCard = new VBox(16);
        signalsCard.getStyleClass().add("editorial-card");
        HBox signalsHeader = sectionHeader("mdi2t-transmission-tower", "Network Signals", "Reload Signals");
        ((Button) signalsHeader.getChildren().get(3)).setOnAction(e -> {
            connection.send(new Packet(Packet.Type.TRIVIA_REQUEST, null));
            connection.send(new Packet(Packet.Type.WIKI_LOOKUP_REQUEST, "Local area network"));
        });

        signalTrivia = new Label("Trivia signal pending...");
        signalTrivia.getStyleClass().add("body-text");
        signalTrivia.setWrapText(true);

        signalWiki = new Label("Wiki signal pending...");
        signalWiki.getStyleClass().add("body-text");
        signalWiki.setWrapText(true);

        signalsCard.getChildren().addAll(signalsHeader, signalTrivia, signalWiki);

        getChildren().addAll(title, overviewCard, peopleCard, profileLensCard, signalsCard);
        new FadeIn(this).setSpeed(1.4).play();
    }

    private HBox sectionHeader(String iconLiteral, String label, String actionText) {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconColor(Color.web("#0a0a0a"));
        icon.setIconSize(20);

        Label title = new Label(label);
        title.getStyleClass().add("subheading");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (actionText != null && !actionText.isBlank()) {
            Button action = new Button(actionText, new FontIcon("mdi2r-refresh"));
            action.getStyleClass().add("button-outline");
            header.getChildren().addAll(icon, title, spacer, action);
        } else {
            header.getChildren().addAll(icon, title, spacer);
        }

        return header;
    }

    public void updateOnlineUsers(List<String> users) {
        List<String> online = users == null ? List.of() : users.stream()
            .filter(name -> !name.equals(currentUser.getUsername()))
            .sorted(String::compareToIgnoreCase)
            .collect(Collectors.toList());

        peopleGrid.getChildren().clear();
        for (String username : online) {
            peopleGrid.getChildren().add(buildPersonCard(username));
        }

        networkSummary.setText("Users online on this LAN: " + online.size() +
            (online.isEmpty() ? ". You're currently solo." : ". Live profile cards are shown below."));
        new FadeIn(networkSummary).play();
    }

    public void updateFeedSnapshot(List<Post> posts) {
        List<Post> safePosts = posts == null ? List.of() : posts;
        long marketplace = safePosts.stream().filter(p -> p.getType() == Post.PostType.MARKETPLACE).count();
        long social = safePosts.stream().filter(p -> p.getType() == Post.PostType.SOCIAL).count();

        Map<String, Long> authors = safePosts.stream()
            .collect(Collectors.groupingBy(Post::getAuthor, Collectors.counting()));
        String topAuthor = authors.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(e -> e.getKey() + " (" + e.getValue() + " posts)")
            .orElse("No activity yet");

        feedPulse.setText("Feed Pulse: " + social + " social posts, " + marketplace
            + " marketplace posts. Most active: " + topAuthor + ".");
        new FadeIn(feedPulse).play();
    }

    private VBox buildPersonCard(String username) {
        UserProfile profile = resolveProfile(username);
        VBox card = new VBox(8);
        card.setPrefWidth(180);
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #000000; -fx-border-width: 2; -fx-padding: 12;");

        ImageView avatar = AvatarFactory.createCircularAvatar(profile, 44);

        Label name = new Label(username);
        name.setStyle("-fx-font-weight: 900; -fx-text-fill: #000000;");

        Label status = new Label(profile.getDisplayStatus().isBlank() ? "No status set" : profile.getDisplayStatus());
        status.setStyle("-fx-font-size: 11px; -fx-text-fill: #222222;");
        status.setWrapText(true);

        card.getChildren().addAll(avatar, name, status);
        return card;
    }

    private void inspectUser(String username) {
        if (username == null || username.isBlank()) return;
        profileRequester.accept(username);
        UserProfile profile = resolveProfile(username);
        spotlightName.setText("Profile: " + profile.getUsername());
        String status = profile.getDisplayStatus().isBlank() ? "No status" : profile.getDisplayStatus();
        spotlightMeta.setText("Bio: " + profile.getBio() + "\n"
            + "Status: " + status + "\n"
            + profile.getJoinedDisplay());
        new FadeIn(spotlightMeta).play();
    }

    private UserProfile resolveProfile(String username) {
        UserProfile profile = profileResolver.apply(username);
        return profile != null ? profile : new UserProfile(username);
    }

    public void updateTrivia(String fact) {
        signalTrivia.setText("Trivia Signal: " + fact);
        new FadeIn(signalTrivia).play();
    }

    public void updateWiki(WikiResult result) {
        signalWiki.setText("Wiki Signal: " + result.getTitle() + "\n" + result.getExtract());
        new FadeIn(signalWiki).play();
    }
}
