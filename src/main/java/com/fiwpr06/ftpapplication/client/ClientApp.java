package com.fiwpr06.ftpapplication.client;

import com.fiwpr06.ftpapplication.client.controller.FtpClientController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX Application khoi chay FTP Client (Giao dien chuan FileZilla)
 */
public class ClientApp extends Application {
    private FtpClientController controller;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/fiwpr06/ftpapplication/fxml/client_view.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/com/fiwpr06/ftpapplication/css/client.css").toExternalForm());

        stage.setTitle("⚡ FTP Client (FileZilla Style) - Lab 2");
        stage.setScene(scene);
        stage.setMinWidth(950);
        stage.setMinHeight(600);

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
