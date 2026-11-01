package com.fiwpr06.ftpapplication.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX Application khoi chay FTP Client (Giao dien hien dai, ket noi PeerChat style)
 */
public class ClientApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/fiwpr06/ftpapplication/fxml/connect_view.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/com/fiwpr06/ftpapplication/css/client.css").toExternalForm());

        stage.setTitle("⚡ FTP Application - File Transfer System");
        stage.setScene(scene);
        stage.setMinWidth(720);
        stage.setMinHeight(550);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
