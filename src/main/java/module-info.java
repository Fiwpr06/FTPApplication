module com.fiwpr06.ftpapplication {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;

    opens com.fiwpr06.ftpapplication.common.model to javafx.base;
    opens com.fiwpr06.ftpapplication.client.model to javafx.base;
    opens com.fiwpr06.ftpapplication.server to javafx.fxml;
    opens com.fiwpr06.ftpapplication.server.controller to javafx.fxml;
    opens com.fiwpr06.ftpapplication.server.database to javafx.base;

    exports com.fiwpr06.ftpapplication.common.model;
    exports com.fiwpr06.ftpapplication.common.util;
    exports com.fiwpr06.ftpapplication.client.model;
    exports com.fiwpr06.ftpapplication.client.network;
    exports com.fiwpr06.ftpapplication.server;
    exports com.fiwpr06.ftpapplication.server.controller;
    exports com.fiwpr06.ftpapplication.server.core;
    exports com.fiwpr06.ftpapplication.server.command;
    exports com.fiwpr06.ftpapplication.server.database;
}
