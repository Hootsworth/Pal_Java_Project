package ui;

import client.ServerConnection;
import animatefx.animation.FadeIn;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import model.Packet;
import model.WikiResult;
import org.kordamp.ikonli.javafx.FontIcon;

public class EditorialDiscover extends VBox {
    private final ServerConnection connection;
    
    private final Label triviaFact;
    private final Label wikiTitle;
    private final Text wikiExtract;

    public EditorialDiscover(ServerConnection connection) {
        this.connection = connection;
        setSpacing(24);

        Label title = new Label("Discover");
        title.getStyleClass().add("heading");

        // Trivia Card
        VBox triviaCard = new VBox(20);
        triviaCard.getStyleClass().add("editorial-card");
        HBox tHeader = new HBox(12);
        tHeader.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon triviaIcon = new FontIcon("mdi2l-lightbulb-on-outline");
        triviaIcon.setIconColor(Color.web("#EAB308"));
        triviaIcon.setIconSize(24);
        
        Label tTitle = new Label("Fact of the Day");
        tTitle.getStyleClass().add("subheading");
        
        Region tSpacer = new Region();
        HBox.setHgrow(tSpacer, Priority.ALWAYS);
        
        Button refreshT = new Button("Refresh", new FontIcon("mdi2r-refresh"));
        refreshT.getStyleClass().add("button-outline");
        refreshT.setOnAction(e -> connection.send(new Packet(Packet.Type.TRIVIA_REQUEST, null)));
        tHeader.getChildren().addAll(triviaIcon, tTitle, tSpacer, refreshT);

        triviaFact = new Label("Loading...");
        triviaFact.getStyleClass().add("body-text");
        triviaFact.setWrapText(true);
        triviaFact.setStyle("-fx-font-size: 18px; -fx-font-style: italic;");
        triviaCard.getChildren().addAll(tHeader, triviaFact);

        // Wiki Spotlight Card
        VBox wikiCard = new VBox(20);
        wikiCard.getStyleClass().add("editorial-card");
        HBox wHeader = new HBox(12);
        wHeader.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon wikiIcon = new FontIcon("mdi2b-book-open-page-variant-outline");
        wikiIcon.setIconColor(Color.web("#58A6FF"));
        wikiIcon.setIconSize(24);
        
        Label wLbl = new Label("Encyclopedia Spotlight");
        wLbl.getStyleClass().add("subheading");
        
        Region wSpacer = new Region();
        HBox.setHgrow(wSpacer, Priority.ALWAYS);
        
        Button surpriseW = new Button("Surprise Me", new FontIcon("mdi2d-dice-multiple-outline"));
        surpriseW.getStyleClass().add("button-primary");
        surpriseW.setOnAction(e -> {
            String[] topics = {"Modernism", "Typography", "Helvetica", "Bauhaus", "Minimalism", "Cyberpunk", "Artificial Intelligence", "Space exploration"};
            String topic = topics[(int)(Math.random() * topics.length)];
            connection.send(new Packet(Packet.Type.WIKI_LOOKUP_REQUEST, topic));
        });
        wHeader.getChildren().addAll(wikiIcon, wLbl, wSpacer, surpriseW);

        wikiTitle = new Label("Click Surprise Me");
        wikiTitle.getStyleClass().add("heading");
        wikiExtract = new Text("");
        wikiExtract.getStyleClass().add("body-text");
        wikiExtract.setWrappingWidth(700);
        wikiExtract.setFill(Color.web("#C9D1D9"));
        
        wikiCard.getChildren().addAll(wHeader, wikiTitle, wikiExtract);

        getChildren().addAll(title, triviaCard, wikiCard);
        
        new FadeIn(this).setSpeed(1.5).play();
    }

    public void updateTrivia(String fact) {
        triviaFact.setText(fact);
        new FadeIn(triviaFact).play();
    }

    public void updateWiki(WikiResult result) {
        wikiTitle.setText(result.getTitle());
        wikiExtract.setText(result.getExtract());
        new FadeIn(wikiTitle).play();
        new FadeIn(wikiExtract).play();
    }
}
