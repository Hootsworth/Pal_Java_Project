package ui;

import client.DiscoveryClient;
import client.PalClient;
import client.ServerConnection;
import animatefx.animation.FadeIn;
import animatefx.animation.Pulse;
import animatefx.animation.SlideInUp;
import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.Packet;
import model.User;
import org.kordamp.ikonli.javafx.FontIcon;

public class EditorialLogin extends BorderPane implements ServerConnection.PacketListener {

    private final PalClient app;
    private final ServerConnection connection;

    private TextField hostField;
    private TextField usernameField;
    private PasswordField passwordField;
    private Label statusLabel;
    private Button loginBtn;
    private Button registerBtn;
    
    private DiscoveryClient discovery;

    public EditorialLogin(PalClient app, ServerConnection connection) {
        this.app = app;
        this.connection = connection;
        this.connection.setListener(this);
        initUI();
        startDiscovery();
    }

    private void startDiscovery() {
        discovery = new DiscoveryClient(data -> {
            if (hostField.getText().equals("localhost") || hostField.getText().isEmpty()) {
                Platform.runLater(() -> {
                    hostField.setText(data[0]);
                    setStatus("Discovered LAN server at " + data[0], "mdi2l-lan-connect");
                });
            }
        });
        discovery.start();
    }

    private void initUI() {
        getStyleClass().add("root");

        // Subtle gradient background
        setStyle("-fx-background-color: linear-gradient(to bottom right, #0D1117, #161B22);");

        VBox centerBox = new VBox(40);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setMaxWidth(450);

        // Header Title
        Label title = new Label("P A L");
        title.getStyleClass().add("heading");
        title.setStyle("-fx-font-size: 48px; -fx-letter-spacing: 5px;");
        Label subtitle = new Label("The Premium Editorial Network");
        subtitle.getStyleClass().add("subheading");

        VBox headerBox = new VBox(10, title, subtitle);
        headerBox.setAlignment(Pos.CENTER);

        // Card containing inputs
        VBox card = new VBox(25);
        card.getStyleClass().add("editorial-card");
        
        hostField = new TextField("localhost");
        hostField.setPromptText("localhost");
        hostField.getStyleClass().add("text-field");
        
        usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.getStyleClass().add("text-field");

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("password-field");

        statusLabel = new Label("Ready");
        statusLabel.getStyleClass().add("body-text");
        statusLabel.setGraphic(new FontIcon("mdi2i-information-outline"));

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        loginBtn = new Button("LOGIN");
        loginBtn.getStyleClass().add("button-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(loginBtn, Priority.ALWAYS);
        loginBtn.setOnAction(e -> handleLogin());

        registerBtn = new Button("CREATE ACCOUNT");
        registerBtn.getStyleClass().add("button-outline");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(registerBtn, Priority.ALWAYS);
        registerBtn.setOnAction(e -> handleRegister());

        btnBox.getChildren().addAll(loginBtn, registerBtn);

        card.getChildren().addAll(
            createInputRow("mdi2s-server-network", "Server Host", hostField),
            createInputRow("mdi2a-account", "Username", usernameField),
            createInputRow("mdi2l-lock", "Password", passwordField),
            btnBox,
            statusLabel
        );

        centerBox.getChildren().addAll(headerBox, card);
        setCenter(centerBox);

        // Entrance animations
        new FadeIn(headerBox).setSpeed(0.5).play();
        new SlideInUp(card).setSpeed(0.8).play();
        new Pulse(title).setCycleCount(Animation.INDEFINITE).setSpeed(0.2).play();
    }

    private VBox createInputRow(String iconLiteral, String labelText, TextField field) {
        HBox labelBox = new HBox(8);
        labelBox.setAlignment(Pos.CENTER_LEFT);
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconColor(javafx.scene.paint.Color.web("#8B949E"));
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("subheading");
        labelBox.getChildren().addAll(icon, lbl);
        
        VBox row = new VBox(8, labelBox, field);
        return row;
    }

    private void handleLogin() {
        String h = hostField.getText().trim();
        String u = usernameField.getText().trim();
        String p = passwordField.getText();
        if (h.isEmpty() || u.isEmpty() || p.isEmpty()) {
            setStatus("Please fill all fields.", "mdi2a-alert-circle");
            return;
        }
        setStatus("Connecting...", "mdi2l-loading");
        setInputsEnabled(false);
        if (discovery != null) discovery.stopDiscovery();
        
        new Thread(() -> {
            if (connection.connect(h, 9090, this)) {
                Platform.runLater(() -> setStatus("Authenticating...", "mdi2l-loading"));
                connection.send(new Packet(Packet.Type.LOGIN, new String[]{u, p}));
            } else {
                Platform.runLater(() -> {
                    setStatus("Connection failed.", "mdi2c-close-circle-outline");
                    setInputsEnabled(true);
                });
            }
        }).start();
    }

    private void handleRegister() {
        String h = hostField.getText().trim();
        String u = usernameField.getText().trim();
        String p = passwordField.getText();
        if (h.isEmpty() || u.isEmpty() || p.isEmpty()) {
            setStatus("Please fill all fields.", "mdi2a-alert-circle");
            return;
        }
        setStatus("Connecting...", "mdi2l-loading");
        setInputsEnabled(false);

        new Thread(() -> {
            if (connection.connect(h, 9090, this)) {
                Platform.runLater(() -> setStatus("Registering...", "mdi2l-loading"));
                connection.send(new Packet(Packet.Type.REGISTER, new String[]{u, p}));
            } else {
                Platform.runLater(() -> {
                    setStatus("Connection failed.", "mdi2c-close-circle-outline");
                    setInputsEnabled(true);
                });
            }
        }).start();
    }

    private void setStatus(String msg, String iconCode) {
        statusLabel.setText(msg);
        statusLabel.setGraphic(new FontIcon(iconCode));
    }

    private void setInputsEnabled(boolean b) {
        hostField.setDisable(!b);
        usernameField.setDisable(!b);
        passwordField.setDisable(!b);
        loginBtn.setDisable(!b);
        registerBtn.setDisable(!b);
    }

    @Override
    public void onPacketReceived(Packet packet) {
        Platform.runLater(() -> {
            switch (packet.getType()) {
                case LOGIN_SUCCESS -> {
                    setStatus("Success! Loading...", "mdi2c-check-circle-outline");
                    app.showMain((User) packet.getPayload());
                }
                case LOGIN_FAIL -> {
                    setStatus("Login Failed: " + packet.getPayload(), "mdi2c-close-circle-outline");
                    setInputsEnabled(true);
                }
                case REGISTER_SUCCESS -> {
                    setStatus("Registered successfully. Please Login.", "mdi2c-check-circle-outline");
                    setInputsEnabled(true);
                }
                case ERROR -> {
                    setStatus("Error: " + packet.getPayload(), "mdi2a-alert-circle");
                    setInputsEnabled(true);
                }
                default -> {}
            }
        });
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            setStatus("Disconnected from server.", "mdi2l-lan-disconnect");
            setInputsEnabled(true);
        });
    }
}
