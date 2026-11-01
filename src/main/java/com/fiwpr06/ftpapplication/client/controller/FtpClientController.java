package com.fiwpr06.ftpapplication.client.controller;

import com.fiwpr06.ftpapplication.client.model.LocalFileItem;
import com.fiwpr06.ftpapplication.client.model.RemoteFileItem;
import com.fiwpr06.ftpapplication.client.network.FtpClient;
import com.fiwpr06.ftpapplication.common.model.TransferStatus;
import com.fiwpr06.ftpapplication.common.model.TransferTask;
import com.fiwpr06.ftpapplication.common.util.AlertHelper;
import com.fiwpr06.ftpapplication.common.util.FileUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FtpClientController implements FtpClient.ClientEventListener {

    // Quickconnect Bar
    @FXML private TextField txtHost;
    @FXML private TextField txtUser;
    @FXML private PasswordField txtPass;
    @FXML private TextField txtPort;
    @FXML private Button btnQuickconnect;
    @FXML private Button btnDisconnect;
    @FXML private Label lblConnectionStatus;

    // Status Log Console
    @FXML private TextArea txtConsole;

    // Local Site Pane
    @FXML private TextField txtLocalPath;
    @FXML private TableView<LocalFileItem> tblLocalFiles;
    @FXML private TableColumn<LocalFileItem, String> colLocalName;
    @FXML private TableColumn<LocalFileItem, String> colLocalSize;
    @FXML private TableColumn<LocalFileItem, String> colLocalType;
    @FXML private TableColumn<LocalFileItem, String> colLocalTime;

    // Remote Site Pane
    @FXML private TextField txtRemotePath;
    @FXML private TableView<RemoteFileItem> tblRemoteFiles;
    @FXML private TableColumn<RemoteFileItem, String> colRemoteName;
    @FXML private TableColumn<RemoteFileItem, String> colRemoteSize;
    @FXML private TableColumn<RemoteFileItem, String> colRemotePerm;
    @FXML private TableColumn<RemoteFileItem, String> colRemoteTime;

    // Transfer Queue Table
    @FXML private TableView<TransferTask> tblQueue;
    @FXML private TableColumn<TransferTask, String> colQFile;
    @FXML private TableColumn<TransferTask, String> colQType;
    @FXML private TableColumn<TransferTask, String> colQSize;
    @FXML private TableColumn<TransferTask, Double> colQProgress;
    @FXML private TableColumn<TransferTask, String> colQSpeed;
    @FXML private TableColumn<TransferTask, String> colQStatus;

    // Bottom Status Bar
    @FXML private Label lblBottomStatus;
    @FXML private ProgressBar progressBarBottom;

    private FtpClient client;
    private File currentLocalDir;

    private final ObservableList<LocalFileItem> localFilesList = FXCollections.observableArrayList();
    private final ObservableList<RemoteFileItem> remoteFilesList = FXCollections.observableArrayList();
    private final ObservableList<TransferTask> queueList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        this.client = new FtpClient();
        this.client.setListener(this);

        setupTables();
        setupDoubleClicks();

        // Khoi tao thu muc Local ban dau
        currentLocalDir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        loadLocalDirectory(currentLocalDir);
    }

    private void setupTables() {
        // Local files table
        colLocalName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colLocalSize.setCellValueFactory(new PropertyValueFactory<>("formattedSize"));
        colLocalType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isDirectory() ? "Directory" : "File"));
        colLocalTime.setCellValueFactory(new PropertyValueFactory<>("lastModified"));
        tblLocalFiles.setItems(localFilesList);

        // Remote files table
        colRemoteName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colRemoteSize.setCellValueFactory(new PropertyValueFactory<>("formattedSize"));
        colRemotePerm.setCellValueFactory(new PropertyValueFactory<>("permissions"));
        colRemoteTime.setCellValueFactory(new PropertyValueFactory<>("lastModified"));
        tblRemoteFiles.setItems(remoteFilesList);

        // Queue table
        colQFile.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        colQType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType().name()));
        colQSize.setCellValueFactory(data -> new SimpleStringProperty(FileUtils.formatSize(data.getValue().getTotalBytes())));
        colQProgress.setCellValueFactory(data -> data.getValue().progressProperty().asObject());
        colQProgress.setCellFactory(ProgressBarTableCell.forTableColumn());
        colQSpeed.setCellValueFactory(new PropertyValueFactory<>("speed"));
        colQStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        tblQueue.setItems(queueList);
    }

    private void setupDoubleClicks() {
        tblLocalFiles.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                LocalFileItem selected = tblLocalFiles.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    if (selected.isDirectory()) {
                        loadLocalDirectory(selected.getFile());
                    } else {
                        handleUploadSelected();
                    }
                }
            }
        });

        tblRemoteFiles.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                RemoteFileItem selected = tblRemoteFiles.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    if (selected.isDirectory()) {
                        enterRemoteDirectory(selected.getName());
                    } else {
                        handleDownloadSelected();
                    }
                }
            }
        });
    }

    // ================= KET NOI & DANG NHAP =================

    @FXML
    public void handleConnect() {
        String host = txtHost.getText().trim();
        String user = txtUser.getText().trim();
        String pass = txtPass.getText().trim();
        int port;

        try {
            port = Integer.parseInt(txtPort.getText().trim());
        } catch (NumberFormatException e) {
            AlertHelper.showWarning("Input Error", "Port phai la so nguyen!");
            return;
        }

        if (host.isEmpty() || user.isEmpty()) {
            AlertHelper.showWarning("Input Error", "Host va Username khong duoc de trong!");
            return;
        }

        btnQuickconnect.setDisable(true);
        lblBottomStatus.setText("Connecting to " + host + ":" + port + "...");

        new Thread(() -> {
            boolean connected = client.connect(host, port);
            if (!connected) {
                Platform.runLater(() -> {
                    btnQuickconnect.setDisable(false);
                    lblBottomStatus.setText("Connection failed.");
                    AlertHelper.showError("Connection Failed", "Khong the ket noi toi FTP Server tai " + host + ":" + port);
                });
                return;
            }

            try {
                boolean loggedIn = client.login(user, pass);
                Platform.runLater(() -> {
                    if (loggedIn) {
                        lblConnectionStatus.setText("Connected (" + user + ")");
                        lblConnectionStatus.setStyle("-fx-text-fill: #2EE68A; -fx-font-weight: bold;");
                        btnQuickconnect.setDisable(true);
                        btnDisconnect.setDisable(false);
                        lblBottomStatus.setText("Logged in as " + user);
                        refreshRemoteDirectory();
                    } else {
                        btnQuickconnect.setDisable(false);
                        lblBottomStatus.setText("Authentication failed.");
                        AlertHelper.showError("Auth Error", "Sai ten dang nhap hoac mat khau tren MySQL database.");
                        client.disconnect();
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    btnQuickconnect.setDisable(false);
                    AlertHelper.showError("Error", "Loi giao tiep voi FTP Server: " + e.getMessage());
                });
            }
        }).start();
    }

    public void initSession(FtpClient existingClient, String host, int port, String displayName, String passcode) {
        if (this.client != null && this.client != existingClient) {
            this.client.disconnect();
        }
        this.client = existingClient;
        this.client.setListener(this);

        if (txtHost != null) txtHost.setText(host);
        if (txtPort != null) txtPort.setText(String.valueOf(port));
        if (txtUser != null) txtUser.setText(displayName);
        if (txtPass != null) txtPass.setText(passcode);

        if (lblConnectionStatus != null) {
            lblConnectionStatus.setText("Đã kết nối (" + displayName + ")");
            lblConnectionStatus.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
        }
        if (btnQuickconnect != null) btnQuickconnect.setDisable(true);
        if (btnDisconnect != null) btnDisconnect.setDisable(false);
        if (lblBottomStatus != null) lblBottomStatus.setText("Đã đăng nhập thành công với bí danh " + displayName);

        refreshRemoteDirectory();
    }

    @FXML
    public void handleDisconnect() {
        if (client != null) {
            client.disconnect();
        }
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/fiwpr06/ftpapplication/fxml/connect_view.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (Stage) btnDisconnect.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("⚡ FTP Application - File Transfer System");
            stage.setWidth(720);
            stage.setHeight(550);
            stage.centerOnScreen();
        } catch (Exception e) {
            lblConnectionStatus.setText("Disconnected");
            lblConnectionStatus.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
            btnQuickconnect.setDisable(false);
            btnDisconnect.setDisable(true);
            remoteFilesList.clear();
            txtRemotePath.setText("/");
            lblBottomStatus.setText("Disconnected from server.");
        }
    }

    // ================= DUYET THU MUC LOCAL =================

    private void loadLocalDirectory(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        currentLocalDir = dir;
        txtLocalPath.setText(dir.getAbsolutePath());

        File[] files = dir.listFiles();
        ObservableList<LocalFileItem> items = FXCollections.observableArrayList();
        if (files != null) {
            for (File f : files) {
                items.add(new LocalFileItem(f));
            }
        }
        // Sap xep thu muc len truoc, sau do den file
        items.sort((a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        localFilesList.setAll(items);
    }

    @FXML
    public void handleLocalUp() {
        if (currentLocalDir != null && currentLocalDir.getParentFile() != null) {
            loadLocalDirectory(currentLocalDir.getParentFile());
        }
    }

    @FXML
    public void handleLocalRefresh() {
        loadLocalDirectory(currentLocalDir);
    }

    @FXML
    public void handleLocalNewFolder() {
        TextInputDialog dialog = new TextInputDialog("New Folder");
        dialog.setTitle("New Local Folder");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter folder name:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                File newDir = new File(currentLocalDir, name.trim());
                if (newDir.mkdir()) {
                    loadLocalDirectory(currentLocalDir);
                } else {
                    AlertHelper.showError("Error", "Could not create directory.");
                }
            }
        });
    }

    // ================= DUYET THU MUC REMOTE =================

    private void refreshRemoteDirectory() {
        if (!client.isConnected()) return;
        new Thread(() -> {
            try {
                String pwd = client.pwd();
                List<RemoteFileItem> list = client.list();
                Platform.runLater(() -> {
                    txtRemotePath.setText(pwd);
                    // Sap xep thu muc len truoc
                    list.sort((a, b) -> {
                        if (a.isDirectory() && !b.isDirectory()) return -1;
                        if (!a.isDirectory() && b.isDirectory()) return 1;
                        return a.getName().compareToIgnoreCase(b.getName());
                    });
                    remoteFilesList.setAll(list);
                });
            } catch (IOException e) {
                Platform.runLater(() -> AlertHelper.showError("Error", "Loi lay danh sach file remote: " + e.getMessage()));
            }
        }).start();
    }

    private void enterRemoteDirectory(String dirName) {
        if (!client.isConnected()) return;
        new Thread(() -> {
            try {
                if (client.cwd(dirName)) {
                    Platform.runLater(this::refreshRemoteDirectory);
                }
            } catch (IOException e) {
                Platform.runLater(() -> AlertHelper.showError("Error", "CWD error: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void handleRemoteUp() {
        if (!client.isConnected()) return;
        new Thread(() -> {
            try {
                if (client.cdup()) {
                    Platform.runLater(this::refreshRemoteDirectory);
                }
            } catch (IOException e) {
                Platform.runLater(() -> AlertHelper.showError("Error", "CDUP error: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    public void handleRemoteRefresh() {
        refreshRemoteDirectory();
    }

    @FXML
    public void handleRemoteNewFolder() {
        if (!client.isConnected()) return;
        TextInputDialog dialog = new TextInputDialog("New Folder");
        dialog.setTitle("New Remote Directory");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter remote directory name:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                new Thread(() -> {
                    try {
                        if (client.makeDirectory(name.trim())) {
                            Platform.runLater(this::refreshRemoteDirectory);
                        } else {
                            Platform.runLater(() -> AlertHelper.showError("Error", "Khong the tao thu muc tren server."));
                        }
                    } catch (IOException e) {
                        Platform.runLater(() -> AlertHelper.showError("Error", "MKD error: " + e.getMessage()));
                    }
                }).start();
            }
        });
    }

    @FXML
    public void handleRemoteDelete() {
        if (!client.isConnected()) return;
        RemoteFileItem selected = tblRemoteFiles.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("Thong bao", "Vui long chon tap tin tren Remote can xoa.");
            return;
        }

        if (AlertHelper.showConfirmation("Xac nhan", "Ban co chac muon xoa '" + selected.getName() + "' tren Server?")) {
            new Thread(() -> {
                try {
                    boolean ok;
                    if (selected.isDirectory()) {
                        ok = client.removeDirectory(selected.getName());
                    } else {
                        ok = client.deleteFile(selected.getName());
                    }
                    Platform.runLater(() -> {
                        if (ok) {
                            refreshRemoteDirectory();
                        } else {
                            AlertHelper.showError("Error", "Xoa that bai.");
                        }
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> AlertHelper.showError("Error", "Loi xoa file: " + e.getMessage()));
                }
            }).start();
        }
    }

    // ================= TRUYEN TAI TAP TIN (UPLOAD & DOWNLOAD) =================

    @FXML
    public void handleUploadSelected() {
        if (!client.isConnected()) {
            AlertHelper.showWarning("Chua ket noi", "Vui long ket noi toi FTP Server truoc khi upload.");
            return;
        }

        LocalFileItem selected = tblLocalFiles.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isDirectory()) {
            AlertHelper.showWarning("Thong bao", "Vui long chon mot tap tin local can upload.");
            return;
        }

        File fileToUpload = selected.getFile();
        String remoteFileName = fileToUpload.getName();
        TransferTask task = new TransferTask(
                UUID.randomUUID().toString().substring(0, 8),
                remoteFileName,
                fileToUpload.getAbsolutePath(),
                txtRemotePath.getText() + "/" + remoteFileName,
                TransferTask.Type.UPLOAD,
                fileToUpload.length()
        );
        queueList.add(task);

        progressBarBottom.setVisible(true);
        lblBottomStatus.setText("Uploading " + remoteFileName + "...");

        new Thread(() -> {
            task.statusProperty().set(TransferStatus.TRANSFERRING);
            try {
                boolean ok = client.uploadFile(fileToUpload, remoteFileName, (transferred, total, speed) -> {
                    Platform.runLater(() -> {
                        task.updateProgress(transferred, speed);
                        progressBarBottom.setProgress(task.getProgress());
                    });
                });
                Platform.runLater(() -> {
                    if (ok) {
                        task.statusProperty().set(TransferStatus.COMPLETED);
                        lblBottomStatus.setText("Uploaded " + remoteFileName + " successfully.");
                        refreshRemoteDirectory();
                    } else {
                        task.statusProperty().set(TransferStatus.FAILED);
                        lblBottomStatus.setText("Upload " + remoteFileName + " failed.");
                    }
                    progressBarBottom.setVisible(false);
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    task.statusProperty().set(TransferStatus.FAILED);
                    lblBottomStatus.setText("Upload error: " + e.getMessage());
                    progressBarBottom.setVisible(false);
                });
            }
        }).start();
    }

    @FXML
    public void handleDownloadSelected() {
        if (!client.isConnected()) {
            AlertHelper.showWarning("Chua ket noi", "Vui long ket noi toi FTP Server truoc khi download.");
            return;
        }

        RemoteFileItem selected = tblRemoteFiles.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isDirectory()) {
            AlertHelper.showWarning("Thong bao", "Vui long chon mot tap tin tren server can download.");
            return;
        }

        String remoteFileName = selected.getName();
        File targetLocalFile = new File(currentLocalDir, remoteFileName);

        TransferTask task = new TransferTask(
                UUID.randomUUID().toString().substring(0, 8),
                remoteFileName,
                targetLocalFile.getAbsolutePath(),
                txtRemotePath.getText() + "/" + remoteFileName,
                TransferTask.Type.DOWNLOAD,
                selected.getSize()
        );
        queueList.add(task);

        progressBarBottom.setVisible(true);
        lblBottomStatus.setText("Downloading " + remoteFileName + "...");

        new Thread(() -> {
            task.statusProperty().set(TransferStatus.TRANSFERRING);
            try {
                boolean ok = client.downloadFile(remoteFileName, targetLocalFile, (transferred, total, speed) -> {
                    Platform.runLater(() -> {
                        task.updateProgress(transferred, speed);
                        progressBarBottom.setProgress(task.getProgress());
                    });
                });
                Platform.runLater(() -> {
                    if (ok) {
                        task.statusProperty().set(TransferStatus.COMPLETED);
                        lblBottomStatus.setText("Downloaded " + remoteFileName + " successfully.");
                        loadLocalDirectory(currentLocalDir);
                    } else {
                        task.statusProperty().set(TransferStatus.FAILED);
                        lblBottomStatus.setText("Download " + remoteFileName + " failed.");
                    }
                    progressBarBottom.setVisible(false);
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    task.statusProperty().set(TransferStatus.FAILED);
                    lblBottomStatus.setText("Download error: " + e.getMessage());
                    progressBarBottom.setVisible(false);
                });
            }
        }).start();
    }

    // ================= CLIENT EVENT LISTENER =================

    @Override
    public void onCommandSent(String command) {
        Platform.runLater(() -> {
            txtConsole.appendText("-> " + command + "\n");
        });
    }

    @Override
    public void onReplyReceived(int code, String fullReply) {
        Platform.runLater(() -> {
            txtConsole.appendText("<- " + fullReply + "\n");
        });
    }

    @Override
    public void onConnectionLost() {
        Platform.runLater(() -> {
            handleDisconnect();
            AlertHelper.showWarning("Disconnect", "Mat ket noi toi FTP Server.");
        });
    }

    public void shutdown() {
        if (client != null && client.isConnected()) {
            client.disconnect();
        }
    }
}
