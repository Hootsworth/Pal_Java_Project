package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");

    private String from;
    private String to;
    private String content;
    private LocalDateTime timestamp;

    public Message(String from, String to, String content) {
        this.from = from;
        this.to = to;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public String getFrom() { return from; }
    public String getTo() { return to; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public String getFormattedTime() {
        return timestamp.format(FMT);
    }

    @Override
    public String toString() {
        return "[" + getFormattedTime() + "] " + from + ": " + content;
    }
}
