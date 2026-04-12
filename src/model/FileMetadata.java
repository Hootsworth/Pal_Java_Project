package model;

import java.io.Serializable;

/**
 * FileMetadata - Metadata for P2P file transfer.
 */
public class FileMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String fileName;
    private final long fileSize;
    private final String sender;
    private final String receiver;

    public FileMetadata(String fileName, long fileSize, String sender, String receiver) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.sender = sender;
        this.receiver = receiver;
    }

    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }

    public String getFormattedSize() {
        if (fileSize < 1024) return fileSize + " B";
        int exp = (int) (Math.log(fileSize) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", fileSize / Math.pow(1024, exp), pre);
    }
}
