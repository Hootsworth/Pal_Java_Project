package ui;

import client.ServerConnection;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import model.Packet;
import model.WikiResult;

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
        VBox triviaCard = new VBox(16);
        triviaCard.getStyleClass().add("editorial-card");
        HBox tHeader = new HBox(10);
        Label tTitle = new Label("Fact of the Day");
        tTitle.getStyleClass().add("subheading");
        Button refreshT = new Button("Refresh");
        refreshT.getStyleClass().add("button-outline");
        refreshT.setOnAction(e -> connection.send(new Packet(Packet.Type.TRIVIA_REQUEST, null)));
        tHeader.getChildren().addAll(tTitle, refreshT);

        triviaFact = new Label("Loading...");
        triviaFact.getStyleClass().add("body-text");
        triviaFact.setWrapText(true);
        triviaCard.getChildren().addAll(tHeader, triviaFact);

        // Wiki Spotlight Card
        VBox wikiCard = new VBox(16);
        wikiCard.getStyleClass().add("editorial-card");
        HBox wHeader = new HBox(10);
        Label wLbl = new Label("Encyclopedia Spotlight");
        wLbl.getStyleClass().add("subheading");
        Button surpriseW = new Button("Surprise Me");
        surpriseW.getStyleClass().add("button-outline");
        surpriseW.setOnAction(e -> {
            String[] topics = {"Modernism", "Typography", "Helvetica", "Bauhaus", "Minimalism"};
            String topic = topics[(int)(Math.random() * topics.length)];
            connection.send(new Packet(Packet.Type.WIKI_LOOKUP_REQUEST, topic));
        });
        wHeader.getChildren().addAll(wLbl, surpriseW);

        wikiTitle = new Label("Click Surprise Me");
        wikiTitle.getStyleClass().add("heading");
        wikiExtract = new Text("");
        wikiExtract.getStyleClass().add("body-text");
        wikiExtract.setWrappingWidth(600);
        
        wikiCard.getChildren().addAll(wHeader, wikiTitle, wikiExtract);

        getChildren().addAll(title, triviaCard, wikiCard);
    }

    public void updateTrivia(String fact) {
        triviaFact.setText(fact);
    }

    public void updateWiki(WikiResult result) {
        wikiTitle.setText(result.getTitle());
        wikiExtract.setText(result.getExtract());
    }
}
