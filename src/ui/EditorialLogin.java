package ui;

import client.DiscoveryClient;
import client.PalClient;
import client.ServerConnection;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.Packet;
import model.User;

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
                hostField.setText(data[0]);
                setStatus("Discovered local server at " + data[0]);
            }
        });
        discovery.start();
    }

    private void initUI() {
        getStyleClass().add("root");

        VBox centerBox = new VBox(40);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setMaxWidth(400);

        // Header Title
        Label title = new Label("P A L");
        title.getStyleClass().add("heading");
        Label subtitle = new Label("A modern editorial network.");
        subtitle.getStyleClass().add("subheading");

        VBox headerBox = new VBox(5, title, subtitle);
        headerBox.setAlignment(Pos.CENTER);

        // Card containing inputs
        VBox card = new VBox(20);
        card.getStyleClass().add("editorial-card");
        
        hostField = new TextField("localhost");
        hostField.setPromptText("Enter server IP (e.g., localhost)");
        hostField.getStyleClass().add("text-field");

        usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.getStyleClass().add("text-field");

        passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.getStyleClass().add("password-field");

        statusLabel = new Label();
        statusLabel.getStyleClass().add("subheading");

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        loginBtn = new Button("LOGIN");
        loginBtn.getStyleClass().add("button-primary");
        loginBtn.setOnAction(e -> handleLogin());

        registerBtn = new Button("CREATE ACCOUNT");
        registerBtn.getStyleClass().add("button-outline");
        registerBtn.setOnAction(e -> handleRegister());

        btnBox.getChildren().addAll(loginBtn, registerBtn);

        card.getChildren().addAll(
            new Label("Server Host") {{ getStyleClass().add("subheading"); }},
            hostField,
            new Label("Username") {{ getStyleClass().add("subheading"); }},
            usernameField,
            new Label("Password") {{ getStyleClass().add("subheading"); }},
            passwordField,
            btnBox,
            statusLabel
        );

        centerBox.getChildren().addAll(headerBox, card);
        setCenter(centerBox);
    }

    private void handleLogin() {
        String h = hostField.getText().trim();
        String u = usernameField.getText().trim();
        String p = passwordField.getText();
        if (h.isEmpty() || u.isEmpty() || p.isEmpty()) {
            setStatus("Please fill all fields.");
            return;
        }
        setStatus("Connecting...");
        setInputsEnabled(false);
        if (discovery != null) discovery.stopDiscovery();
        
        new Thread(() -> {
            if (connection.connect(h, 9090, this)) {
                Platform.runLater(() -> setStatus("Authenticating..."));
                connection.send(new Packet(Packet.Type.LOGIN, new String[]{u, p}));
            } else {
                Platform.runLater(() -> {
                    setStatus("Connection failed.");
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
            setStatus("Please fill all fields.");
            return;
        }
        setStatus("Connecting...");
        setInputsEnabled(false);

        new Thread(() -> {
            if (connection.connect(h, 9090, this)) {
                Platform.runLater(() -> setStatus("Registering..."));
                connection.send(new Packet(Packet.Type.REGISTER, new String[]{u, p}));
            } else {
                Platform.runLater(() -> {
                    setStatus("Connection failed.");
                    setInputsEnabled(true);
                });
            }
        }).start();
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg);
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
                    setStatus("Success! Loading...");
                    app.showMain((User) packet.getPayload());
                }
                case LOGIN_FAIL -> {
                    setStatus("Login Failed: " + packet.getPayload());
                    setInputsEnabled(true);
                }
                case REGISTER_SUCCESS -> {
                    setStatus("Registered successfully. Please Login.");
                    setInputsEnabled(true);
                }
                case ERROR -> {
                    setStatus("Error: " + packet.getPayload());
                    setInputsEnabled(true);
                }
            }
        });
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            setStatus("Disconnected from server.");
            setInputsEnabled(true);
        });
    }
}
