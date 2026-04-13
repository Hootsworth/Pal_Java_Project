package client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.User;
import ui.EditorialLogin;
import ui.EditorialMain;
import atlantafx.base.theme.PrimerDark;

public class PalClient extends Application {

    private Stage primaryStage;
    private ServerConnection connection;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.connection = new ServerConnection();
        // Apply AtlantaFX PrimerDark theme
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        primaryStage.setTitle("Pal — The Editorial Experience");
        primaryStage.setWidth(1200);
        primaryStage.setHeight(800);

        showLogin();
        primaryStage.show();
    }

    public void showLogin() {
        EditorialLogin loginView = new EditorialLogin(this, connection);
        Scene scene = new Scene(loginView, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setScene(scene);
    }

    public void showMain(User user) {
        EditorialMain mainView = new EditorialMain(this, connection, user);
        Scene scene = new Scene(mainView, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setScene(scene);
    }

    @Override
    public void stop() {
        // TCP closes automatically on exit.
    }

    public static void main(String[] args) {
        launch(args);
    }
}
