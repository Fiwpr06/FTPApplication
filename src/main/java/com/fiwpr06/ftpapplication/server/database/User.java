package com.fiwpr06.ftpapplication.server.database;

/**
 * Thuc the User dai dien cho tai khoan FTP luu trong MySQL (plain-text password)
 */
public class User {
    private int id;
    private String username;
    private String password; // Plain-text theo yeu cau de bai
    private String homeDir;
    private boolean permissionRead;
    private boolean permissionWrite;
    private boolean active;

    public User() {
        this.permissionRead = true;
        this.permissionWrite = true;
        this.active = true;
    }

    public User(int id, String username, String password, String homeDir, boolean permissionRead, boolean permissionWrite, boolean active) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.homeDir = homeDir;
        this.permissionRead = permissionRead;
        this.permissionWrite = permissionWrite;
        this.active = active;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getHomeDir() { return homeDir; }
    public void setHomeDir(String homeDir) { this.homeDir = homeDir; }

    public boolean isPermissionRead() { return permissionRead; }
    public void setPermissionRead(boolean permissionRead) { this.permissionRead = permissionRead; }

    public boolean isPermissionWrite() { return permissionWrite; }
    public void setPermissionWrite(boolean permissionWrite) { this.permissionWrite = permissionWrite; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
