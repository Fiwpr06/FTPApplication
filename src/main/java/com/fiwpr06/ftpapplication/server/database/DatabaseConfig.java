package com.fiwpr06.ftpapplication.server.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Cau hinh ket noi MySQL cho FTP Server
 */
public class DatabaseConfig {
    private String host = "localhost";
    private int port = 3306;
    private String databaseName = "ftp_db";
    private String user = "root";
    private String password = "";

    public DatabaseConfig() {
        loadFromProperties();
    }

    public DatabaseConfig(String host, int port, String databaseName, String user, String password) {
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.user = user;
        this.password = password;
    }

    public void loadFromProperties() {
        File propFile = new File("db.properties");
        if (propFile.exists()) {
            try (InputStream is = new FileInputStream(propFile)) {
                Properties props = new Properties();
                props.load(is);
                this.host = props.getProperty("db.host", "localhost");
                this.port = Integer.parseInt(props.getProperty("db.port", "3306"));
                this.databaseName = props.getProperty("db.name", "ftp_db");
                this.user = props.getProperty("db.user", "root");
                this.password = props.getProperty("db.password", "");
            } catch (Exception ignored) {}
        }
    }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getJdbcUrl() {
        return String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false",
                host, port, databaseName);
    }

    public String getServerJdbcUrl() {
        return String.format("jdbc:mysql://%s:%d/?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false",
                host, port);
    }
}
