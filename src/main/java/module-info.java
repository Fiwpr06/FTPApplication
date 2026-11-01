module com.fiwpr06.ftpapplication {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.fiwpr06.ftpapplication to javafx.fxml;
    exports com.fiwpr06.ftpapplication;
}