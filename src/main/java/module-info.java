module com.fiwpr06.ftpapplication {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;

    opens com.fiwpr06.ftpapplication.common.model to javafx.base;

    exports com.fiwpr06.ftpapplication.common.model;
    exports com.fiwpr06.ftpapplication.common.util;
    exports com.fiwpr06.ftpapplication.server.core;
    exports com.fiwpr06.ftpapplication.server.command;
    exports com.fiwpr06.ftpapplication.server.database;
}
