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
    private void handleConnect() {
        String host = hostField.getText().trim();
        String portStr = portField.getText().trim();
        String code = codeField.getText().trim();
        String name = nameField.getText().trim();

        if (host.isEmpty()) {
            setStatus("[ CẢNH BÁO ] Địa chỉ máy chủ không được để trống!", true);
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port <= 0 || port > 65535) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            setStatus("[ CẢNH BÁO ] Cổng kết nối không hợp lệ (1 - 65535)!", true);
            return;
        }

        if (code.isEmpty()) {
            setStatus("[ CẢNH BÁO ] Vui lòng nhập mã kết nối Passcode!", true);
            return;
        }

        if (name.isEmpty()) {
            setStatus("[ CẢNH BÁO ] Vui lòng nhập bí danh / tên hiển thị!", true);
            return;
        }

        connectButton.setDisable(true);
        setStatus("[ ĐANG KẾT NỐI ] Đang thiết lập kết nối tới " + host + ":" + port + "...", false);

        new Thread(() -> {
            FtpClient client = new FtpClient();
            try {
                boolean connected = client.connect(host, port);
                if (!connected) {
                    Platform.runLater(() -> {
                        connectButton.setDisable(false);
                        setStatus("[ LỖI KẾT NỐI ] Không thể kết nối tới FTP Server tại " + host + ":" + port, true);
                    });
                    return;
                }

                // Dang nhap bang displayName va passcode
                boolean authOk = client.login(name, code);
                if (!authOk) {
                    client.disconnect();
                    Platform.runLater(() -> {
                        connectButton.setDisable(false);
                        setStatus("[ XÁC THỰC THẤT BẠI ] Sai mã kết nối Passcode! Vui lòng kiểm tra lại mã trên máy chủ.", true);
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
                        Scene scene = connectButton.getScene();
                        scene.setRoot(root);

                        stage.setTitle("⚡ FTP Application - " + name + "@" + host + ":" + port + " (FileZilla Style)");
                        stage.setMinWidth(1000);
                        stage.setMinHeight(640);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        connectButton.setDisable(false);
                        setStatus("[ LỖI TẢI GIAO DIỆN ] " + ex.getMessage(), true);
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    connectButton.setDisable(false);
                    setStatus("[ LỖI NGOẠI LỆ ] " + ex.getMessage(), true);
                });
            }
        }).start();
    }

    private void setStatus(String message, boolean isError) {
        statusLabel.setText(message);
        if (isError) {
            statusLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
        } else {
            statusLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
        }
    }
}
