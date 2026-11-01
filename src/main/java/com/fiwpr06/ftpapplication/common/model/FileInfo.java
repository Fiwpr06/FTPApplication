package com.fiwpr06.ftpapplication.common.model;

import java.io.Serializable;

/**
 * Mo hinh thong tin tap tin tren FTP Server hoac Local Client
 */
public class FileInfo implements Serializable {
    private String name;
    private long size;
    private boolean isDirectory;
    private long lastModified;
    private String permissions;

    public FileInfo() {}

    public FileInfo(String name, long size, boolean isDirectory, long lastModified, String permissions) {
        this.name = name;
        this.size = size;
        this.isDirectory = isDirectory;
        this.lastModified = lastModified;
        this.permissions = permissions;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public boolean isDirectory() { return isDirectory; }
    public void setDirectory(boolean directory) { isDirectory = directory; }

    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }

    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }

    /**
     * Dinh dang danh sach file chuan UNIX cho lenh LIST cua FTP
     * Vi du: drwxr-xr-x 1 owner group 0 Nov 01 10:00 folder_name
     *        -rw-r--r-- 1 owner group 1024 Nov 01 10:00 file.txt
     */
    public String toUnixListFormat(String dateFormatted) {
        String perm = permissions != null ? permissions : (isDirectory ? "drwxr-xr-x" : "-rw-r--r--");
        return String.format("%s 1 ftp ftp %12d %s %s\r\n", perm, size, dateFormatted, name);
    }
}
