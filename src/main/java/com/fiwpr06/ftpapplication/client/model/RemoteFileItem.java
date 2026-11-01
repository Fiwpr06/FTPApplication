package com.fiwpr06.ftpapplication.client.model;

import com.fiwpr06.ftpapplication.common.util.FileUtils;

/**
 * Mo hinh tap tin tu xa tren FTP Server
 */
public class RemoteFileItem {
    private final String name;
    private final boolean directory;
    private final long size;
    private final String formattedSize;
    private final String permissions;
    private final String lastModified;

    public RemoteFileItem(String name, boolean directory, long size, String permissions, String lastModified) {
        this.name = name;
        this.directory = directory;
        this.size = size;
        this.formattedSize = directory ? "<DIR>" : FileUtils.formatSize(size);
        this.permissions = permissions;
        this.lastModified = lastModified;
    }

    public String getName() { return name; }
    public boolean isDirectory() { return directory; }
    public long getSize() { return size; }
    public String getFormattedSize() { return formattedSize; }
    public String getPermissions() { return permissions; }
    public String getLastModified() { return lastModified; }
}
