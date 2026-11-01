package com.fiwpr06.ftpapplication.server.database;

import java.time.LocalDateTime;

/**
 * Thuc the Nhat ky truyen tai tap tin tren FTP Server luu vao MySQL
 */
public class TransferLog {
    private int id;
    private String username;
    private String action; // UPLOAD, DOWNLOAD, DELETE
    private String filename;
    private long filesize;
    private String status; // SUCCESS, FAILED
    private String timestamp;

    public TransferLog() {}

    public TransferLog(int id, String username, String action, String filename, long filesize, String status, String timestamp) {
        this.id = id;
        this.username = username;
        this.action = action;
        this.filename = filename;
        this.filesize = filesize;
        this.status = status;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public long getFilesize() { return filesize; }
    public void setFilesize(long filesize) { this.filesize = filesize; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
