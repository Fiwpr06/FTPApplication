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
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FtpClientController implements FtpClient.ClientEventListener {

    private static final Logger LOGGER = Logger.getLogger(FtpClientController.class.getName());

    // Quickconnect Bar
    @FXML private TextField txtHost;
    @FXML private TextField txtUser;
    @FXML private PasswordField txtPass;
    @FXML private TextField txtPort;
    @FXML private Button btnQuickconnect;
    @FXML private Button btnDisconnect;
    @FXML private Label lblConnectionStatus;
    @FXML private Label lblAutoSyncStatus;

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

    private final ExecutorService transferExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "FtpTransferWorker");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean isTransferring = new AtomicBoolean(false);
    private ScheduledExecutorService syncScheduler;

    @FXML
    public void initialize() {
        this.client = new FtpClient();
        this.client.setListener(this);

        setupTables();
        setupDoubleClicks();
        setupDragAndDrop();

        // Khoi tao thu muc Local ban dau
        currentLocalDir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        loadLocalDirectory(currentLocalDir);
    }

    private void setupTables() {
        // Ho tro chon nhieu tap tin
        tblLocalFiles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tblRemoteFiles.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

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
        colQType.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getType() == TransferTask.Type.UPLOAD ? "Tải lên ↑" : "Tải về ↓")
        );
        colQSize.setCellValueFactory(cellData -> 
            new SimpleStringProperty(FileUtils.formatSize(cellData.getValue().getTotalBytes()))
        );
        colQProgress.setCellValueFactory(cellData -> cellData.getValue().progressProperty().asObject());
        colQProgress.setCellFactory(ProgressBarTableCell.forTableColumn());
        colQSpeed.setCellValueFactory(cellData -> cellData.getValue().speedProperty());

        // Dynamic Status Binding: Cap nhat ngay lap tuc theo trang thai thoi gian thuc cua Task
        colQStatus.setCellValueFactory(cellData -> 
            javafx.beans.binding.Bindings.createStringBinding(
                () -> cellData.getValue().getStatus() != null ? cellData.getValue().getStatus().getDescription() : "",
                cellData.getValue().statusProperty()
            )
        );
        colQStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("Thành công")) {
                        setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                    } else if (item.contains("Thất bại")) {
                        setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                    } else if (item.contains("Đang truyền")) {
                        setStyle("-fx-text-fill: #2563EB; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #D97706; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Context Menu cho hang doi
        ContextMenu queueMenu = new ContextMenu();
        MenuItem clearCompletedItem = new MenuItem("Xóa tác vụ đã hoàn thành");
        clearCompletedItem.setOnAction(e -> queueList.removeIf(t -> t.getStatus() == TransferStatus.COMPLETED));
        MenuItem clearAllItem = new MenuItem("Xóa toàn bộ hàng đợi");
        clearAllItem.setOnAction(e -> queueList.removeIf(t -> t.getStatus() != TransferStatus.TRANSFERRING));
        queueMenu.getItems().addAll(clearCompletedItem, clearAllItem);
        tblQueue.setContextMenu(queueMenu);

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

    private void setupDragAndDrop() {
        // Drag tu tblLocalFiles de upload len Remote
        tblLocalFiles.setOnDragDetected(event -> {
            List<LocalFileItem> selected = tblLocalFiles.getSelectionModel().getSelectedItems();
            if (selected == null || selected.isEmpty()) return;

            List<File> files = selected.stream()
                    .filter(item -> item != null && !item.isDirectory() && item.getFile() != null)
                    .map(LocalFileItem::getFile)
                    .collect(Collectors.toList());

            if (!files.isEmpty()) {
                Dragboard db = tblLocalFiles.startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                content.putFiles(files);
                db.setContent(content);
                event.consume();
            }
        });

        // tblRemoteFiles nhan file keo tha tu tblLocalFiles hoac tu ngoai de upload
        tblRemoteFiles.setOnDragOver(event -> {
            if (event.getGestureSource() != tblRemoteFiles && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        tblRemoteFiles.setOnDragEntered(event -> {
            if (event.getGestureSource() != tblRemoteFiles && event.getDragboard().hasFiles()) {
                if (!tblRemoteFiles.getStyleClass().contains("table-drag-active")) {
                    tblRemoteFiles.getStyleClass().add("table-drag-active");
                }
            }
        });

        tblRemoteFiles.setOnDragExited(event -> {
            tblRemoteFiles.getStyleClass().remove("table-drag-active");
        });

        tblRemoteFiles.setOnDragDropped(event -> {
            tblRemoteFiles.getStyleClass().remove("table-drag-active");
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                if (!client.isConnected()) {
                    Platform.runLater(() -> AlertHelper.showWarning("Chưa kết nối", "Vui lòng kết nối tới FTP Server trước khi kéo thả tải lên."));
                } else {
                    List<File> files = db.getFiles();
                    if (files != null && !files.isEmpty()) {
                        for (File f : files) {
                            if (f.isFile()) {
                                enqueueUploadTask(f);
                            }
                        }
                        success = true;
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        // Drag tu tblRemoteFiles de download ve Local
        tblRemoteFiles.setOnDragDetected(event -> {
            List<RemoteFileItem> selected = tblRemoteFiles.getSelectionModel().getSelectedItems();
            if (selected == null || selected.isEmpty()) return;

            List<RemoteFileItem> files = selected.stream()
                    .filter(item -> item != null && !item.isDirectory())
                    .collect(Collectors.toList());

            if (!files.isEmpty()) {
                Dragboard db = tblRemoteFiles.startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                String serialized = "FTP_REMOTE:" + files.stream()
                        .map(RemoteFileItem::getName)
                        .collect(Collectors.joining(";"));
                content.putString(serialized);
                db.setContent(content);
                event.consume();
            }
        });

        // tblLocalFiles nhan tap tin tu tblRemoteFiles de download HOAC tu Windows Desktop ngoai de sao chep
        tblLocalFiles.setOnDragOver(event -> {
            if (event.getGestureSource() != tblLocalFiles) {
                if (event.getDragboard().hasString() && event.getDragboard().getString().startsWith("FTP_REMOTE:")) {
                    event.acceptTransferModes(TransferMode.COPY);
                } else if (event.getDragboard().hasFiles()) {
                    event.acceptTransferModes(TransferMode.COPY);
                }
            }
            event.consume();
        });

        tblLocalFiles.setOnDragEntered(event -> {
            if (event.getGestureSource() != tblLocalFiles) {
                boolean isValid = (event.getDragboard().hasString() && event.getDragboard().getString().startsWith("FTP_REMOTE:"))
                        || event.getDragboard().hasFiles();
                if (isValid && !tblLocalFiles.getStyleClass().contains("table-drag-active")) {
                    tblLocalFiles.getStyleClass().add("table-drag-active");
                }
            }
        });

        tblLocalFiles.setOnDragExited(event -> {
            tblLocalFiles.getStyleClass().remove("table-drag-active");
        });

        tblLocalFiles.setOnDragDropped(event -> {
            tblLocalFiles.getStyleClass().remove("table-drag-active");
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString() && db.getString().startsWith("FTP_REMOTE:")) {
                if (!client.isConnected()) {
                    Platform.runLater(() -> AlertHelper.showWarning("Chưa kết nối", "Vui lòng kết nối tới FTP Server trước khi kéo thả tải về."));
                } else {
                    String payload = db.getString().substring("FTP_REMOTE:".length());
                    String[] fileNames = payload.split(";");
                    loadLocalDirectory(currentLocalDir);

                    for (String name : fileNames) {
                        if (name == null || name.trim().isEmpty()) continue;
                        String cleanName = name.trim();
                        RemoteFileItem matched = tblRemoteFiles.getItems().stream()
                                .filter(item -> item != null && item.getName().equals(cleanName))
                                .findFirst()
                                .orElse(new RemoteFileItem(cleanName, false, 0L, "-rw-r--r--", ""));
                        enqueueDownloadTask(matched, currentLocalDir);
                    }
                    success = true;
                }
            } else if (db.hasFiles()) {
                // Keo tha file tu Windows Explorer hoac Desktop vao bang may tinh
                List<File> files = db.getFiles();
                if (files != null && !files.isEmpty() && currentLocalDir != null && currentLocalDir.exists()) {
                    int copiedCount = 0;
                    for (File src : files) {
                        if (src.isFile()) {
                            try {
                                File dest = new File(currentLocalDir, src.getName());
                                Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                copiedCount++;
                                success = true;
                            } catch (IOException ex) {
                                LOGGER.log(Level.WARNING, "Loi sao chep file vao thu muc local: " + ex.getMessage());
                            }
                        }
                    }
                    if (success) {
                        final int totalCopied = copiedCount;
                        Platform.runLater(() -> {
                            loadLocalDirectory(currentLocalDir);
                            lblBottomStatus.setText("Đã sao chép " + totalCopied + " tệp từ ngoài vào thư mục máy tính.");
                        });
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    // ================= DONG BO TU DONG (AUTO-SYNC ENGINE) =================

    private synchronized void startAutoSync() {
        stopAutoSync();
        if (lblAutoSyncStatus != null) {
            lblAutoSyncStatus.setText("Auto-Sync: BẬT");
            lblAutoSyncStatus.setStyle("-fx-text-fill: #059669; -fx-background-color: #ECFDF5;");
        }
        syncScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "FtpAutoSyncWorker");
            t.setDaemon(true);
            return t;
        });

        syncScheduler.scheduleWithFixedDelay(() -> {
            try {
                if (client == null || !client.isConnected() || isTransferring.get()) {
                    return;
                }

                // 1. Kiem tra thu muc Remote khi kenh truyen khong ban
                String currentRemotePwd = client.pwd();
                List<RemoteFileItem> latestRemoteList = client.list();
                if (latestRemoteList != null && hasRemoteChanged(latestRemoteList)) {
                    Platform.runLater(() -> updateRemoteFilesTable(currentRemotePwd, latestRemoteList));
                }

                // 2. Kiem tra thu muc Local
                if (currentLocalDir != null && currentLocalDir.exists() && currentLocalDir.isDirectory()) {
                    if (hasLocalChanged(currentLocalDir)) {
                        Platform.runLater(() -> loadLocalDirectory(currentLocalDir));
                    }
                }
            } catch (Exception ignored) {
                // Bo qua ngoai le nhat thoi khi ping socket
            }
        }, 1500, 2500, TimeUnit.MILLISECONDS);
    }

    private synchronized void stopAutoSync() {
        if (lblAutoSyncStatus != null) {
            lblAutoSyncStatus.setText("Auto-Sync: TẮT");
            lblAutoSyncStatus.setStyle("-fx-text-fill: #94A3B8; -fx-background-color: #F1F5F9;");
        }
        if (syncScheduler != null && !syncScheduler.isShutdown()) {
            syncScheduler.shutdownNow();
            syncScheduler = null;
        }
    }

    private boolean hasRemoteChanged(List<RemoteFileItem> newList) {
        if (newList == null) return false;
        if (newList.size() != remoteFilesList.size()) return true;
        for (int i = 0; i < newList.size(); i++) {
            RemoteFileItem itemNew = newList.get(i);
            RemoteFileItem itemOld = remoteFilesList.get(i);
            if (!itemNew.getName().equals(itemOld.getName())
                    || itemNew.getSize() != itemOld.getSize()
                    || itemNew.isDirectory() != itemOld.isDirectory()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasLocalChanged(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return false;
        File[] files = dir.listFiles();
        int count = files == null ? 0 : files.length;
        if (count != localFilesList.size()) return true;
        if (files != null) {
            for (File f : files) {
                boolean found = false;
                for (LocalFileItem item : localFilesList) {
                    if (item.getFile().getName().equals(f.getName())) {
                        if (item.getFile().length() != f.length() || item.getFile().lastModified() != f.lastModified()) {
                            return true;
                        }
                        found = true;
                        break;
                    }
                }
                if (!found) return true;
            }
        }
        return false;
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
                        startAutoSync();
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
        startAutoSync();
    }

    @FXML
    public void handleDisconnect() {
        stopAutoSync();
        if (client != null) {
            client.disconnect();
        }
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/fiwpr06/ftpapplication/fxml/connect_view.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (Stage) btnDisconnect.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("FTP Application - Kết nối máy chủ");
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
        if (tblLocalFiles != null) {
            tblLocalFiles.refresh();
        }
    }

    private void updateRemoteFilesTable(String pwd, List<RemoteFileItem> list) {
        if (pwd != null && txtRemotePath != null) {
            txtRemotePath.setText(pwd);
        }
        if (list != null) {
            list.sort((a, b) -> {
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });
            remoteFilesList.setAll(list);
            if (tblRemoteFiles != null) {
                tblRemoteFiles.refresh();
            }
        }
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
        TextInputDialog dialog = new TextInputDialog("ThuMucMoi");
        dialog.setTitle("Tạo thư mục cục bộ mới");
        dialog.setHeaderText(null);
        dialog.setContentText("Nhập tên thư mục mới:");
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
                Platform.runLater(() -> updateRemoteFilesTable(pwd, list));
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
        TextInputDialog dialog = new TextInputDialog("ThuMucMoi");
        dialog.setTitle("Tạo thư mục máy chủ mới");
        dialog.setHeaderText(null);
        dialog.setContentText("Nhập tên thư mục mới trên máy chủ:");
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
    public void handlePickAndUpload() {
        if (!client.isConnected()) {
            AlertHelper.showWarning("Chưa kết nối", "Vui lòng kết nối tới FTP Server trước khi tải lên.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn tệp tải lên FTP Server");
        if (currentLocalDir != null && currentLocalDir.exists() && currentLocalDir.isDirectory()) {
            fileChooser.setInitialDirectory(currentLocalDir);
        }
        Window window = tblLocalFiles.getScene().getWindow();
        List<File> files = fileChooser.showOpenMultipleDialog(window);
        if (files == null || files.isEmpty()) return;

        for (File f : files) {
            enqueueUploadTask(f);
        }
    }

    @FXML
    public void handleUploadSelected() {
        if (!client.isConnected()) {
            AlertHelper.showWarning("Chưa kết nối", "Vui lòng kết nối tới FTP Server trước khi tải lên.");
            return;
        }

        List<LocalFileItem> selectedItems = new ArrayList<>(tblLocalFiles.getSelectionModel().getSelectedItems());
        if (selectedItems.isEmpty()) {
            AlertHelper.showWarning("Thông báo", "Vui lòng chọn ít nhất một tập tin cục bộ để tải lên.");
            return;
        }

        for (LocalFileItem selected : selectedItems) {
            if (selected == null || selected.isDirectory()) continue;
            enqueueUploadTask(selected.getFile());
        }
    }

    private void enqueueUploadTask(File fileToUpload) {
        if (fileToUpload == null || !fileToUpload.exists() || fileToUpload.isDirectory()) return;

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

        transferExecutor.submit(() -> {
            isTransferring.set(true);
            Platform.runLater(() -> {
                task.statusProperty().set(TransferStatus.TRANSFERRING);
                progressBarBottom.setVisible(true);
                progressBarBottom.setProgress(0.0);
                lblBottomStatus.setText("Đang tải lên " + remoteFileName + "...");
            });

            try {
                boolean ok = client.uploadFile(fileToUpload, remoteFileName, (transferred, total, speed) -> {
                    Platform.runLater(() -> {
                        task.updateProgress(transferred, speed);
                        progressBarBottom.setProgress(task.getProgress());
                    });
                });

                // Lay danh sach file remote ngay tren worker thread mot cach an toan va tuan tu
                String pwd = null;
                List<RemoteFileItem> remoteList = null;
                if (ok) {
                    try {
                        pwd = client.pwd();
                        remoteList = client.list();
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Loi lay danh sach file remote sau khi upload: " + ex.getMessage());
                    }
                }

                final String finalPwd = pwd;
                final List<RemoteFileItem> finalRemoteList = remoteList;

                Platform.runLater(() -> {
                    if (ok) {
                        task.updateProgress(fileToUpload.length(), 0);
                        task.progressProperty().set(1.0);
                        task.statusProperty().set(TransferStatus.COMPLETED);
                        lblBottomStatus.setText("Đã tải lên thành công: " + remoteFileName);

                        // Khi tải lên server thành công -> Refresh ngay thư mục máy chủ
                        if (finalRemoteList != null) {
                            updateRemoteFilesTable(finalPwd, finalRemoteList);
                        } else {
                            refreshRemoteDirectory();
                        }
                    } else {
                        task.statusProperty().set(TransferStatus.FAILED);
                        lblBottomStatus.setText("Tải lên thất bại: " + remoteFileName);
                    }
                    progressBarBottom.setVisible(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    task.statusProperty().set(TransferStatus.FAILED);
                    lblBottomStatus.setText("Lỗi tải lên: " + e.getMessage());
                    progressBarBottom.setVisible(false);
                });
            } finally {
                isTransferring.set(false);
            }
        });
    }

    @FXML
    public void handleDownloadSelected() {
        if (!client.isConnected()) {
            AlertHelper.showWarning("Chưa kết nối", "Vui lòng kết nối tới FTP Server trước khi tải về.");
            return;
        }

        List<RemoteFileItem> selectedItems = new ArrayList<>(tblRemoteFiles.getSelectionModel().getSelectedItems());
        if (selectedItems.isEmpty()) {
            AlertHelper.showWarning("Thông báo", "Vui lòng chọn ít nhất một tập tin trên máy chủ để tải về.");
            return;
        }

        // Khi bấm tải về -> Refresh ngay thư mục máy tính
        loadLocalDirectory(currentLocalDir);

        for (RemoteFileItem selected : selectedItems) {
            if (selected == null || selected.isDirectory()) continue;
            enqueueDownloadTask(selected, currentLocalDir);
        }
    }

    @FXML
    public void handleDownloadToFolder() {
        if (!client.isConnected()) {
            AlertHelper.showWarning("Chưa kết nối", "Vui lòng kết nối tới FTP Server trước khi tải về.");
            return;
        }

        List<RemoteFileItem> selectedItems = new ArrayList<>(tblRemoteFiles.getSelectionModel().getSelectedItems());
        if (selectedItems.isEmpty()) {
            AlertHelper.showWarning("Thông báo", "Vui lòng chọn ít nhất một tập tin trên máy chủ để tải về.");
            return;
        }

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Chọn thư mục lưu tệp tải về");
        if (currentLocalDir != null && currentLocalDir.exists()) {
            dirChooser.setInitialDirectory(currentLocalDir);
        }
        Window window = tblRemoteFiles.getScene().getWindow();
        File destFolder = dirChooser.showDialog(window);
        if (destFolder == null) return;

        // Cập nhật thư mục máy tính sang nơi lưu vừa chọn và refresh ngay
        currentLocalDir = destFolder;
        loadLocalDirectory(destFolder);

        for (RemoteFileItem selected : selectedItems) {
            if (selected == null || selected.isDirectory()) continue;
            enqueueDownloadTask(selected, destFolder);
        }
    }

    private void enqueueDownloadTask(RemoteFileItem selected, File destFolder) {
        String remoteFileName = selected.getName();
        File targetLocalFile = new File(destFolder, remoteFileName);

        TransferTask task = new TransferTask(
                UUID.randomUUID().toString().substring(0, 8),
                remoteFileName,
                targetLocalFile.getAbsolutePath(),
                txtRemotePath.getText() + "/" + remoteFileName,
                TransferTask.Type.DOWNLOAD,
                selected.getSize()
        );
        queueList.add(task);

        transferExecutor.submit(() -> {
            isTransferring.set(true);
            Platform.runLater(() -> {
                task.statusProperty().set(TransferStatus.TRANSFERRING);
                progressBarBottom.setVisible(true);
                progressBarBottom.setProgress(0.0);
                lblBottomStatus.setText("Đang tải về " + remoteFileName + "...");
            });

            try {
                boolean ok = client.downloadFile(remoteFileName, targetLocalFile, (transferred, total, speed) -> {
                    Platform.runLater(() -> {
                        task.updateProgress(transferred, speed);
                        progressBarBottom.setProgress(task.getProgress());
                    });
                });

                Platform.runLater(() -> {
                    if (ok) {
                        task.updateProgress(selected.getSize(), 0);
                        task.progressProperty().set(1.0);
                        task.statusProperty().set(TransferStatus.COMPLETED);
                        lblBottomStatus.setText("Đã tải về thành công: " + remoteFileName);

                        // Khi tải về hoàn tất -> Refresh lại thư mục máy tính để cập nhật kích thước / tệp mới
                        loadLocalDirectory(destFolder);
                    } else {
                        task.statusProperty().set(TransferStatus.FAILED);
                        lblBottomStatus.setText("Tải về thất bại: " + remoteFileName);
                    }
                    progressBarBottom.setVisible(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    task.statusProperty().set(TransferStatus.FAILED);
                    lblBottomStatus.setText("Lỗi tải về: " + e.getMessage());
                    progressBarBottom.setVisible(false);
                });
            } finally {
                isTransferring.set(false);
            }
        });
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
        stopAutoSync();
        if (transferExecutor != null) {
            transferExecutor.shutdownNow();
        }
        if (client != null && client.isConnected()) {
            client.disconnect();
        }
    }
}
