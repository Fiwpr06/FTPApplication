package com.fiwpr06.ftpapplication.server;

import com.fiwpr06.ftpapplication.server.controller.FtpServerController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX Application khoi chay FTP Server Dashboard
 */
public class ServerApp extends Application {
    private FtpServerController controller;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/fiwpr06/ftpapplication/fxml/server_view.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/com/fiwpr06/ftpapplication/css/server.css").toExternalForm());

        stage.setTitle("FTP Application - Máy chủ truyền tệp");
        stage.setScene(scene);
        stage.setMinWidth(1080);
        stage.setMinHeight(680);

        stage.setOnCloseRequest(e -> {
            if (controller != null) {
                controller.shutdown();
            }
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
