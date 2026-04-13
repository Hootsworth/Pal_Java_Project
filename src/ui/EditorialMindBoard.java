package ui;

import client.ServerConnection;
import animatefx.animation.FadeIn;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import model.DrawAction;
import model.Packet;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * EditorialMindBoard - Collaborative real-time drawing board.
 */
public class EditorialMindBoard extends VBox {

    private final ServerConnection connection;
    private final Canvas canvas;
    private final GraphicsContext gc;
    private double lastX, lastY;
    private Color strokeColor = Color.web("#58A6FF");

    public EditorialMindBoard(ServerConnection connection) {
        this.connection = connection;
        setSpacing(20);
        getStyleClass().add("editorial-card");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon icon = new FontIcon("mdi2p-palette-outline");
        icon.setIconColor(Color.web("#8A2BE2"));
        icon.setIconSize(32);
        
        VBox titles = new VBox(2);
        Label title = new Label("Campus Mind-Board");
        title.getStyleClass().add("heading");
        Label subtitle = new Label("Real-time collaborative brainstorming workspace.");
        subtitle.getStyleClass().add("subheading");
        titles.getChildren().addAll(title, subtitle);
        
        header.getChildren().addAll(icon, titles);

        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        ColorPicker cp = new ColorPicker(strokeColor);
        cp.setOnAction(e -> strokeColor = cp.getValue());
        cp.getStyleClass().add("button-outline");

        Button clearBtn = new Button("Clear Board", new FontIcon("mdi2e-eraser"));
        clearBtn.getStyleClass().add("button-outline");
        clearBtn.setOnAction(e -> {
            sendDrawAction(0, 0, 0, 0, DrawAction.Type.CLEAR);
        });

        toolbar.getChildren().addAll(new Label("Tools:"), cp, clearBtn);

        canvas = new Canvas(850, 500);
        gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(3);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        
        Pane canvasContainer = new Pane(canvas);
        // Dark theme canvas
        canvasContainer.setStyle("-fx-background-color: #0D1117; -fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 1px; -fx-background-radius: 8px; -fx-border-radius: 8px;");
        
        // Local drawing logic
        canvas.setOnMousePressed(e -> {
            lastX = e.getX();
            lastY = e.getY();
            sendDrawAction(lastX, lastY, lastX, lastY, DrawAction.Type.START);
        });

        canvas.setOnMouseDragged(e -> {
            double x = e.getX();
            double y = e.getY();
            sendDrawAction(x, y, lastX, lastY, DrawAction.Type.DRAG);
            lastX = x;
            lastY = y;
        });

        getChildren().addAll(header, toolbar, canvasContainer);
        
        new FadeIn(this).setSpeed(1.2).play();
    }

    private void sendDrawAction(double x, double y, double px, double py, DrawAction.Type type) {
        String colorHex = toHexString(strokeColor);
        DrawAction action = new DrawAction(x, y, px, py, colorHex, 3.0, type);
        connection.send(new Packet(Packet.Type.DRAW_EVENT, action));
    }

    public void applyDrawAction(DrawAction action) {
        javafx.application.Platform.runLater(() -> {
            if (action.getType() == DrawAction.Type.CLEAR) {
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                return;
            }
            
            gc.setStroke(Color.web(action.getColor()));
            gc.setLineWidth(action.getLineWidth());
            
            if (action.getType() == DrawAction.Type.START) {
                gc.beginPath();
                gc.moveTo(action.getX(), action.getY());
            } else if (action.getType() == DrawAction.Type.DRAG) {
                gc.strokeLine(action.getPrevX(), action.getPrevY(), action.getX(), action.getY());
            }
        });
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
