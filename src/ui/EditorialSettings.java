package ui;

import animatefx.animation.FadeIn;
import client.ServerConnection;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import model.Packet;
import model.User;
import model.UserProfile;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class EditorialSettings extends VBox {
    private static final String[] AVATAR_STYLES = {
        "bottts", "adventurer", "personas", "lorelei", "pixel-art"
    };

    private final User currentUser;
    private final ServerConnection connection;
    private final Consumer<UserProfile> localProfileUpdated;
    private final Consumer<Boolean> darkModeToggled;
    private final Supplier<Boolean> darkModeState;

    private final TextArea bioField;
    private final TextField statusEmojiField;
    private final TextField statusTextField;
    private final ComboBox<String> avatarStyleBox;
    private final TextField avatarSeedField;
    private final StackPane avatarPreviewHolder;
    private final Label infoLabel;
    private final CheckBox darkModeToggle;

    public EditorialSettings(User currentUser,
                             ServerConnection connection,
                             Consumer<UserProfile> localProfileUpdated,
                             Consumer<Boolean> darkModeToggled,
                             Supplier<Boolean> darkModeState) {
        this.currentUser = currentUser;
        this.connection = connection;
        this.localProfileUpdated = localProfileUpdated;
        this.darkModeToggled = darkModeToggled;
        this.darkModeState = darkModeState;
        setSpacing(24);

        Label title = new Label("Settings");
        title.getStyleClass().add("heading");

        VBox card = new VBox(16);
        card.getStyleClass().add("editorial-card");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        FontIcon icon = new FontIcon("mdi2c-cog-outline");
        icon.getStyleClass().add("settings-icon");
        Label subtitle = new Label("Public Profile");
        subtitle.getStyleClass().add("subheading");
        header.getChildren().addAll(icon, subtitle);

        HBox appearanceRow = new HBox(10);
        appearanceRow.setAlignment(Pos.CENTER_LEFT);
        darkModeToggle = new CheckBox("Dark Mode");
        darkModeToggle.getStyleClass().add("body-text");
        darkModeToggle.setSelected(this.darkModeState.get());
        darkModeToggle.selectedProperty().addListener((obs, oldVal, enabled) -> this.darkModeToggled.accept(enabled));
        appearanceRow.getChildren().add(darkModeToggle);

        bioField = new TextArea();
        bioField.setPromptText("Write your bio visible to LAN users...");
        bioField.getStyleClass().add("text-area");
        bioField.setPrefRowCount(3);

        HBox statusRow = new HBox(10);
        statusEmojiField = new TextField();
        statusEmojiField.setPromptText("Status emoji");
        statusEmojiField.getStyleClass().add("text-field");
        statusEmojiField.setPrefWidth(150);

        statusTextField = new TextField();
        statusTextField.setPromptText("Status text");
        statusTextField.getStyleClass().add("text-field");
        HBox.setHgrow(statusTextField, Priority.ALWAYS);
        statusRow.getChildren().addAll(statusEmojiField, statusTextField);

        HBox avatarRow = new HBox(10);
        avatarStyleBox = new ComboBox<>(FXCollections.observableArrayList(AVATAR_STYLES));
        avatarStyleBox.getSelectionModel().select("bottts");
        avatarStyleBox.getStyleClass().add("text-field");
        avatarStyleBox.setPrefWidth(180);

        avatarSeedField = new TextField(currentUser.getUsername());
        avatarSeedField.setPromptText("DiceBear seed");
        avatarSeedField.getStyleClass().add("text-field");
        HBox.setHgrow(avatarSeedField, Priority.ALWAYS);
        avatarRow.getChildren().addAll(avatarStyleBox, avatarSeedField);

        avatarPreviewHolder = new StackPane();
        avatarPreviewHolder.setPrefHeight(90);
        avatarPreviewHolder.setAlignment(Pos.CENTER_LEFT);

        infoLabel = new Label("Update your profile and avatar, then save.");
        infoLabel.getStyleClass().add("body-text");

        Button randomSeedBtn = new Button("Random Seed", new FontIcon("mdi2d-dice-multiple-outline"));
        randomSeedBtn.getStyleClass().add("button-outline");
        randomSeedBtn.setOnAction(e -> {
            avatarSeedField.setText(currentUser.getUsername() + "-" + (int) (Math.random() * 99999));
            refreshAvatarPreview();
        });

        Button saveBtn = new Button("Save Settings", new FontIcon("mdi2c-content-save-outline"));
        saveBtn.getStyleClass().add("button-primary");
        saveBtn.setOnAction(e -> saveProfile());

        HBox actionRow = new HBox(10, randomSeedBtn, saveBtn);

        avatarStyleBox.valueProperty().addListener((obs, o, n) -> refreshAvatarPreview());
        avatarSeedField.textProperty().addListener((obs, o, n) -> refreshAvatarPreview());

        card.getChildren().addAll(
            header,
            new Label("Appearance") {{ getStyleClass().add("subheading"); }}, appearanceRow,
            new Label("Bio") {{ getStyleClass().add("subheading"); }}, bioField,
            new Label("Status") {{ getStyleClass().add("subheading"); }}, statusRow,
            new Label("DiceBear Avatar") {{ getStyleClass().add("subheading"); }}, avatarRow, avatarPreviewHolder,
            actionRow,
            infoLabel
        );

        getChildren().addAll(title, card);
        refreshAvatarPreview();
        new FadeIn(this).setSpeed(1.5).play();
    }

    public void populateFrom(UserProfile profile) {
        if (profile == null) return;
        bioField.setText(profile.getBio());
        statusEmojiField.setText(profile.getStatusEmoji());
        statusTextField.setText(profile.getStatusText());
        avatarStyleBox.getSelectionModel().select(profile.getAvatarStyle());
        avatarSeedField.setText(profile.getAvatarSeed());
        refreshAvatarPreview();
    }

    private void refreshAvatarPreview() {
        UserProfile previewProfile = new UserProfile(currentUser.getUsername());
        previewProfile.setAvatarStyle(avatarStyleBox.getValue());
        previewProfile.setAvatarSeed(avatarSeedField.getText());
        ImageView avatar = AvatarFactory.createCircularAvatar(previewProfile, 72);
        avatarPreviewHolder.getChildren().setAll(avatar);
    }

    private void saveProfile() {
        UserProfile updated = new UserProfile(currentUser.getUsername());
        updated.setBio(bioField.getText());
        updated.setStatusEmoji(statusEmojiField.getText());
        updated.setStatusText(statusTextField.getText());
        updated.setAvatarStyle(avatarStyleBox.getValue());
        updated.setAvatarSeed(avatarSeedField.getText());

        connection.send(new Packet(Packet.Type.UPDATE_PROFILE, updated));
        localProfileUpdated.accept(updated);
        infoLabel.setText("Saved. Profile synced with the LAN server.");
    }
}
