package com.fiwpr06.ftpapplication.common.model;

import javafx.beans.property.*;

/**
 * Mo hinh tac vu truyen tai file hien thi tren Queue TableView cua Client
 */
public class TransferTask {
    public enum Type { UPLOAD, DOWNLOAD }

    private final String id;
    private final StringProperty fileName;
    private final StringProperty localPath;
    private final StringProperty remotePath;
    private final ObjectProperty<Type> type;
    private final LongProperty totalBytes;
    private final LongProperty transferredBytes;
    private final DoubleProperty progress; // 0.0 -> 1.0
    private final StringProperty speed;
    private final ObjectProperty<TransferStatus> status;

    public TransferTask(String id, String fileName, String localPath, String remotePath, Type type, long totalBytes) {
        this.id = id;
        this.fileName = new SimpleStringProperty(fileName);
        this.localPath = new SimpleStringProperty(localPath);
        this.remotePath = new SimpleStringProperty(remotePath);
        this.type = new SimpleObjectProperty<>(type);
        this.totalBytes = new SimpleLongProperty(totalBytes);
        this.transferredBytes = new SimpleLongProperty(0);
        this.progress = new SimpleDoubleProperty(0.0);
        this.speed = new SimpleStringProperty("0 KB/s");
        this.status = new SimpleObjectProperty<>(TransferStatus.QUEUED);
    }

    public String getId() { return id; }

    public String getFileName() { return fileName.get(); }
    public StringProperty fileNameProperty() { return fileName; }

    public String getLocalPath() { return localPath.get(); }
    public StringProperty localPathProperty() { return localPath; }

    public String getRemotePath() { return remotePath.get(); }
    public StringProperty remotePathProperty() { return remotePath; }

    public Type getType() { return type.get(); }
    public ObjectProperty<Type> typeProperty() { return type; }

    public long getTotalBytes() { return totalBytes.get(); }
    public LongProperty totalBytesProperty() { return totalBytes; }

    public long getTransferredBytes() { return transferredBytes.get(); }
    public LongProperty transferredBytesProperty() { return transferredBytes; }

    public double getProgress() { return progress.get(); }
    public DoubleProperty progressProperty() { return progress; }

    public String getSpeed() { return speed.get(); }
    public StringProperty speedProperty() { return speed; }

    public TransferStatus getStatus() { return status.get(); }
    public ObjectProperty<TransferStatus> statusProperty() { return status; }

    public void updateProgress(long transferred, double speedKbps) {
        this.transferredBytes.set(transferred);
        if (totalBytes.get() > 0) {
            this.progress.set((double) transferred / totalBytes.get());
        }
        if (speedKbps >= 1024) {
            this.speed.set(String.format("%.2f MB/s", speedKbps / 1024.0));
        } else {
            this.speed.set(String.format("%.1f KB/s", speedKbps));
        }
    }
}
