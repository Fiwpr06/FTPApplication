package com.fiwpr06.ftpapplication.client.controller;

import com.fiwpr06.ftpapplication.client.network.FtpClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller dieu khien man hinh dang nhap ket noi FTP theo phong cach PeerChat
 * (Tu dat ten, chon IP, cong va nhap ma ket noi Passcode, khong can dang ky tai khoan)
 */
public class ConnectController {

    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField codeField;
    @FXML private TextField nameField;
    @FXML private Label statusLabel;
    @FXML private Button connectButton;

    @FXML
    public void initialize() {
        if (nameField != null) {
            Platform.runLater(() -> nameField.requestFocus());
        }
    }

    @FXML
    public void handleFillLocalHost() {
        hostField.setText("127.0.0.1");
        setStatus("Đã đặt địa chỉ máy chủ: 127.0.0.1", false);
    }

    @FXML
    public void handleFillDefaultCode() {
        codeField.setText("FTP888");
        setStatus("Đã đặt mã kết nối mặc định: FTP888", false);
    }

    @FXML
    public void handlePasteCode() {
        String clipboardText = Clipboard.getSystemClipboard().getString();
        if (clipboardText != null && !clipboardText.trim().isEmpty()) {
            codeField.setText(clipboardText.trim());
            setStatus("Đã dán mã kết nối từ bộ nhớ tạm: " + clipboardText.trim(), false);
        } else {
            setStatus("Bộ nhớ tạm hiện tại không có văn bản!", true);
        }
    }

    @FXML
    private void handleConnect() {
        String host = hostField.getText().trim();
        String portStr = portField.getText().trim();
        String code = codeField.getText().trim();
        String name = nameField.getText().trim();

        if (host.isEmpty()) {
            setStatus("Địa chỉ máy chủ không được để trống!", true);
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port <= 0 || port > 65535) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            setStatus("Cổng kết nối không hợp lệ! Vui lòng nhập từ 1 đến 65535.", true);
            return;
        }

        if (code.isEmpty()) {
            setStatus("Vui lòng nhập mã kết nối!", true);
            return;
        }

        if (name.isEmpty()) {
            setStatus("Vui lòng nhập tên hiển thị!", true);
            return;
        }

        connectButton.setDisable(true);
        setStatus("Đang kết nối tới máy chủ " + host + ":" + port + "...", false);

        new Thread(() -> {
            FtpClient client = new FtpClient();
            try {
                boolean connected = client.connect(host, port);
                if (!connected) {
                    Platform.runLater(() -> {
                        connectButton.setDisable(false);
                        setStatus("Không thể kết nối tới máy chủ FTP tại " + host + ":" + port, true);
                    });
                    return;
                }

                // Dang nhap bang displayName va passcode
                boolean authOk = client.login(name, code);
                if (!authOk) {
                    client.disconnect();
                    Platform.runLater(() -> {
                        connectButton.setDisable(false);
                        setStatus("Sai mã kết nối! Vui lòng kiểm tra lại mã trên máy chủ.", true);
                    });
                    return;
                }

                // Dang nhap thanh cong -> Chuyen vao man hinh chinh FileZilla Explorer
                Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/fiwpr06/ftpapplication/fxml/client_view.fxml"));
                        Parent root = loader.load();

                        FtpClientController mainController = loader.getController();
                        mainController.initSession(client, host, port, name, code);

                        Stage stage = (Stage) connectButton.getScene().getWindow();
                        Scene scene = new Scene(root);
                        scene.getStylesheets().add(getClass().getResource("/com/fiwpr06/ftpapplication/css/client.css").toExternalForm());

                        stage.setTitle("FTP Application - Quản lý và truyền tệp (" + name + "@" + host + ":" + port + ")");
                        stage.setScene(scene);
                        stage.setMinWidth(1120);
                        stage.setMinHeight(720);
                        stage.centerOnScreen();
                    } catch (IOException e) {
                        e.printStackTrace();
                        connectButton.setDisable(false);
                        setStatus("Lỗi tải giao diện truyền tệp: " + e.getMessage(), true);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    connectButton.setDisable(false);
                    setStatus("Lỗi trong quá trình kết nối: " + e.getMessage(), true);
                });
            }
        }).start();
    }

    private void setStatus(String text, boolean isError) {
        if (statusLabel != null) {
            statusLabel.setText(text);
            if (isError) {
                statusLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
            } else {
                statusLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
            }
        }
    }
}
