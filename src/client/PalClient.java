package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import model.Packet;
import model.User;
import ui.EditorialLogin;
import ui.EditorialMain;

public class PalClient extends Application {

    private Stage primaryStage;
    private ServerConnection connection;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.connection = new ServerConnection();

        primaryStage.setTitle("Pal — The Editorial Experience");
        primaryStage.setWidth(1024);
        primaryStage.setHeight(768);

        showLogin();
        primaryStage.show();
    }

    public void showLogin() {
        EditorialLogin loginView = new EditorialLogin(this, connection);
        Scene scene = new Scene(loginView, 1024, 768);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setScene(scene);
    }

    public void showMain(User user) {
        EditorialMain mainView = new EditorialMain(this, connection, user);
        Scene scene = new Scene(mainView, 1024, 768);
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
