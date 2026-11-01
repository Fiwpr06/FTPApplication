package com.fiwpr06.ftpapplication.server;

import javafx.application.Application;

/**
 * Launcher khong ke thua Application de tranh loi JavaFX module/runtime khi chay bang fat jar hoac script
 */
public class ServerLauncher {
    public static void main(String[] args) {
        Application.launch(ServerApp.class, args);
    }
}
