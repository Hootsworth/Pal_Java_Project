package model;

import java.io.Serializable;

/**
 * DrawAction - Represents a single drawing event on the Mind-Board.
 */
public class DrawAction implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type { START, DRAG, CLEAR }

    private final double x, y;
    private final double prevX, prevY;
    private final String color;
    private final double lineWidth;
    private final Type type;

    public DrawAction(double x, double y, double prevX, double prevY, String color, double lineWidth, Type type) {
        this.x = x;
        this.y = y;
        this.prevX = prevX;
        this.prevY = prevY;
        this.color = color;
        this.lineWidth = lineWidth;
        this.type = type;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getPrevX() { return prevX; }
    public double getPrevY() { return prevY; }
    public String getColor() { return color; }
    public double getLineWidth() { return lineWidth; }
    public Type getType() { return type; }
}
