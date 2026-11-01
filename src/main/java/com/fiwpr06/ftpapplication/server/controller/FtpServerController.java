package com.fiwpr06.ftpapplication.server.controller;

import com.fiwpr06.ftpapplication.common.util.AlertHelper;
import com.fiwpr06.ftpapplication.common.util.DateTimeUtil;
import com.fiwpr06.ftpapplication.common.util.NetworkUtil;
import com.fiwpr06.ftpapplication.server.core.FtpServer;
import com.fiwpr06.ftpapplication.server.core.FtpSessionHandler;
import com.fiwpr06.ftpapplication.server.database.FtpDatabaseManager;
import com.fiwpr06.ftpapplication.server.database.TransferLog;
import com.fiwpr06.ftpapplication.server.database.TransferLogDao;
import com.fiwpr06.ftpapplication.server.database.User;
import com.fiwpr06.ftpapplication.server.database.UserDao;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class FtpServerController implements FtpServer.ServerEventListener {

    @FXML private Label lblServerIp;
    @FXML private TextField txtPort;
    @FXML private Button btnStart;
    @FXML private Button btnStop;
    @FXML private Label lblStatus;

    // Tab 1: Sessions
    @FXML private Label lblClientCount;
    @FXML private TableView<FtpSessionHandler> tblSessions;
    @FXML private TableColumn<FtpSessionHandler, String> colSessionId;
    @FXML private TableColumn<FtpSessionHandler, String> colClientIp;
    @FXML private TableColumn<FtpSessionHandler, Integer> colClientPort;
    @FXML private TableColumn<FtpSessionHandler, String> colSessionUser;
    @FXML private TableColumn<FtpSessionHandler, String> colSessionPath;
    @FXML private TableColumn<FtpSessionHandler, String> colSessionTime;

    // Tab 2: Users
    @FXML private TableView<User> tblUsers;
    @FXML private TableColumn<User, Integer> colUserId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colPassword;
    @FXML private TableColumn<User, String> colHomeDir;
    @FXML private TableColumn<User, Boolean> colPermRead;
    @FXML private TableColumn<User, Boolean> colPermWrite;
    @FXML private TableColumn<User, Boolean> colUserActive;

    @FXML private TextField txtNewUser;
    @FXML private TextField txtNewPass;
    @FXML private TextField txtNewHome;
    @FXML private CheckBox chkRead;
    @FXML private CheckBox chkWrite;

    // Tab 3: Logs
    @FXML private TableView<TransferLog> tblLogs;
    @FXML private TableColumn<TransferLog, Integer> colLogId;
    @FXML private TableColumn<TransferLog, String> colLogUser;
    @FXML private TableColumn<TransferLog, String> colLogAction;
    @FXML private TableColumn<TransferLog, String> colLogFile;
    @FXML private TableColumn<TransferLog, Long> colLogSize;
    @FXML private TableColumn<TransferLog, String> colLogStatus;
    @FXML private TableColumn<TransferLog, String> colLogTime;

    // Tab 4: Console
    @FXML private TextArea txtConsole;

    private FtpServer server;
    private UserDao userDao;
    private TransferLogDao logDao;

    private final ObservableList<FtpSessionHandler> sessionsList = FXCollections.observableArrayList();
    private final ObservableList<User> usersList = FXCollections.observableArrayList();
    private final ObservableList<TransferLog> logsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        this.userDao = new UserDao();
        this.logDao = new TransferLogDao();
        this.server = new FtpServer();
        this.server.addListener(this);

        lblServerIp.setText(NetworkUtil.getLocalIpAddress());

        setupTables();
        initDatabaseAndLoad();
    }

    private void setupTables() {
        // Setup Sessions table
        colSessionId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSessionId()));
        colClientIp.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getClientIp()));
        colClientPort.setCellValueFactory(new PropertyValueFactory<>("clientPort"));
        colSessionUser.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        colSessionPath.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCurrentVirtualPath()));
        colSessionTime.setCellValueFactory(data -> new SimpleStringProperty(DateTimeUtil.formatEpochMillis(data.getValue().getConnectedTimeMillis())));
        tblSessions.setItems(sessionsList);

        // Setup Users table
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colPassword.setCellValueFactory(new PropertyValueFactory<>("password"));
        colHomeDir.setCellValueFactory(new PropertyValueFactory<>("homeDir"));
        colPermRead.setCellValueFactory(new PropertyValueFactory<>("permissionRead"));
        colPermWrite.setCellValueFactory(new PropertyValueFactory<>("permissionWrite"));
        colUserActive.setCellValueFactory(new PropertyValueFactory<>("active"));
        tblUsers.setItems(usersList);

        // Setup Logs table
        colLogId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colLogUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colLogAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        colLogFile.setCellValueFactory(new PropertyValueFactory<>("filename"));
        colLogSize.setCellValueFactory(new PropertyValueFactory<>("filesize"));
        colLogStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colLogTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        tblLogs.setItems(logsList);
    }

    private void initDatabaseAndLoad() {
        new Thread(() -> {
            boolean ok = FtpDatabaseManager.initialize();
            Platform.runLater(() -> {
                if (ok) {
                    logConsole("[SYSTEM] Ket noi CSDL MySQL '" + FtpDatabaseManager.getConfig().getDatabaseName() + "' thanh cong.");
                    refreshUsers();
                    refreshLogs();
                } else {
                    logConsole("[ERROR] Khong the ket noi CSDL MySQL. Vui long kiem tra MySQL port 3306.");
                    AlertHelper.showError("Database Error", "Khong the ket noi MySQL Server tai localhost:3306.\nVui long kiem tra file db.properties.");
                }
            });
        }).start();
    }

    @FXML
    public void handleStartServer() {
        int port;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
        } catch (NumberFormatException e) {
            AlertHelper.showWarning("Input Error", "Cong Port phai la so nguyen hop le!");
            return;
        }

        new Thread(() -> {
            boolean success = server.start(port);
            Platform.runLater(() -> {
                if (!success) {
                    AlertHelper.showError("Server Error", "Khong the khoi dong FTP Server tren port " + port);
                }
            });
        }).start();
    }

    @FXML
    public void handleStopServer() {
        if (server.isRunning()) {
            server.stop();
        }
    }

    @FXML
    public void refreshActiveSessions() {
        sessionsList.setAll(server.getActiveSessions());
        lblClientCount.setText(sessionsList.size() + " clients online");
    }

    @FXML
    public void disconnectSelectedClient() {
        FtpSessionHandler selected = tblSessions.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.close();
            refreshActiveSessions();
        } else {
            AlertHelper.showWarning("Thong bao", "Vui long chon client can ngat ket noi.");
        }
    }

    @FXML
    public void refreshUsers() {
        usersList.setAll(userDao.getAllUsers());
    }

    @FXML
    public void handleAddUser() {
        String u = txtNewUser.getText().trim();
        String p = txtNewPass.getText().trim();
        String h = txtNewHome.getText().trim();
        if (u.isEmpty() || p.isEmpty()) {
            AlertHelper.showWarning("Input Error", "Username va Password khong duoc de trong!");
            return;
        }

        User user = new User(0, u, p, h.isEmpty() ? "server_storage/files" : h, chkRead.isSelected(), chkWrite.isSelected(), true);
        if (userDao.addUser(user)) {
            txtNewUser.clear();
            txtNewPass.clear();
            refreshUsers();
            AlertHelper.showInfo("Thanh cong", "Da them tai khoan '" + u + "' vao CSDL MySQL!");
        } else {
            AlertHelper.showError("Loi", "Them tai khoan that bai (co the username da ton tai).");
        }
    }

    @FXML
    public void handleDeleteUser() {
        User selected = tblUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showWarning("Thong bao", "Vui long chon user can xoa.");
            return;
        }
        if (selected.getUsername().equals("admin")) {
            AlertHelper.showWarning("Canh bao", "Khong the xoa tai khoan admin he thong!");
            return;
        }
        if (AlertHelper.showConfirmation("Xac nhan", "Ban co chac muon xoa user '" + selected.getUsername() + "'?")) {
            if (userDao.deleteUser(selected.getUsername())) {
                refreshUsers();
                AlertHelper.showInfo("Thanh cong", "Da xoa user khoi MySQL.");
            } else {
                AlertHelper.showError("Loi", "Xoa that bai.");
            }
        }
    }

    @FXML
    public void refreshLogs() {
        logsList.setAll(logDao.getRecentLogs(50));
    }

    @FXML
    public void clearConsole() {
        txtConsole.clear();
    }

    private void logConsole(String line) {
        txtConsole.appendText(line + "\n");
    }

    // ================= IMPLEMENT ServerEventListener =================

    @Override
    public void onServerStarted(int port) {
        Platform.runLater(() -> {
            lblStatus.setText("RUNNING (PORT " + port + ")");
            lblStatus.getStyleClass().clear();
            lblStatus.getStyleClass().add("status-badge-running");
            btnStart.setDisable(true);
            btnStop.setDisable(false);
            txtPort.setDisable(true);
            logConsole("[STATUS] FTP Server is RUNNING on port " + port);
        });
    }

    @Override
    public void onServerStopped() {
        Platform.runLater(() -> {
            lblStatus.setText("STOPPED");
            lblStatus.getStyleClass().clear();
            lblStatus.getStyleClass().add("status-badge-stopped");
            btnStart.setDisable(false);
            btnStop.setDisable(true);
            txtPort.setDisable(false);
            refreshActiveSessions();
            logConsole("[STATUS] FTP Server has STOPPED.");
        });
    }

    @Override
    public void onClientConnected(FtpSessionHandler session) {
        Platform.runLater(() -> {
            refreshActiveSessions();
            logConsole("[CONNECT] Client ket noi tu IP: " + session.getClientIp() + ":" + session.getClientPort());
        });
    }

    @Override
    public void onClientAuthenticated(FtpSessionHandler session) {
        Platform.runLater(() -> {
            refreshActiveSessions();
            logConsole("[AUTH] Client " + session.getClientIp() + " da dang nhap voi user: " + session.getUsername());
        });
    }

    @Override
    public void onClientDisconnected(FtpSessionHandler session) {
        Platform.runLater(() -> {
            refreshActiveSessions();
            logConsole("[DISCONNECT] Client " + session.getClientIp() + " da ngat ket noi.");
        });
    }

    @Override
    public void onLog(String timestamp, String sessionId, String clientIp, String message, boolean isCommand) {
        Platform.runLater(() -> {
            String prefix = isCommand ? "-> CMD " : "<- RESP ";
            logConsole(String.format("[%s] [%s] %s%s", timestamp, clientIp, prefix, message));
            if (!isCommand && message.startsWith("226")) {
                refreshLogs();
            }
        });
    }

    public void shutdown() {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }
}
