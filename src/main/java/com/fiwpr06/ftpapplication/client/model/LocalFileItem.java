package com.fiwpr06.ftpapplication.client.model;

import com.fiwpr06.ftpapplication.common.util.DateTimeUtil;
import com.fiwpr06.ftpapplication.common.util.FileUtils;

import java.io.File;

/**
 * Mo hinh tap tin cuc bo tren may Client
 */
public class LocalFileItem {
    private final File file;
    private final String name;
    private final boolean directory;
    private final long size;
    private final String formattedSize;
    private final String lastModified;

    public LocalFileItem(File file) {
        this.file = file;
        this.name = file.getName().isEmpty() ? file.getAbsolutePath() : file.getName();
        this.directory = file.isDirectory();
        this.size = directory ? 0 : file.length();
        this.formattedSize = directory ? "<DIR>" : FileUtils.formatSize(size);
        this.lastModified = DateTimeUtil.formatEpochMillis(file.lastModified());
    }

    public File getFile() { return file; }
    public String getName() { return name; }
    public boolean isDirectory() { return directory; }
    public long getSize() { return size; }
    public String getFormattedSize() { return formattedSize; }
    public String getLastModified() { return lastModified; }
}
