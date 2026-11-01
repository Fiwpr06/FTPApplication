package com.fiwpr06.ftpapplication.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quan ly ket noi va khoi tao Co so du lieu MySQL cho FTP Server
 */
public class FtpDatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(FtpDatabaseManager.class.getName());

    private static DatabaseConfig config = new DatabaseConfig();
    private static boolean initialized = false;

    private FtpDatabaseManager() {}

    public static synchronized void setConfig(DatabaseConfig newConfig) {
        config = newConfig;
        initialized = false;
    }

    public static DatabaseConfig getConfig() {
        return config;
    }

    public static synchronized boolean initialize() {
        if (initialized) return true;

        try {
            // 1. Kiem tra va nap Driver MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 2. Ket noi vao MySQL Server cap he thong de dam bao Database ton tai
            LOGGER.info("[DB] Dang ket noi toi MySQL Server tai " + config.getHost() + ":" + config.getPort() + "...");
            try (Connection serverConn = DriverManager.getConnection(config.getServerJdbcUrl(), config.getUser(), config.getPassword());
                 Statement stmt = serverConn.createStatement()) {
                String createDbSql = "CREATE DATABASE IF NOT EXISTS `" + config.getDatabaseName() + "` " +
                        "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;";
                stmt.executeUpdate(createDbSql);
                LOGGER.info("[DB] Co so du lieu '" + config.getDatabaseName() + "' da san sang.");
            }

            // 3. Ket noi vao Database va tao cac bang neu chua co
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                
                // Bang users
                String createUsersTable = "CREATE TABLE IF NOT EXISTS `users` (" +
                        "  `id` INT AUTO_INCREMENT PRIMARY KEY," +
                        "  `username` VARCHAR(50) NOT NULL UNIQUE," +
                        "  `password` VARCHAR(100) NOT NULL," +
                        "  `home_dir` VARCHAR(255) NOT NULL," +
                        "  `permission_read` TINYINT DEFAULT 1," +
                        "  `permission_write` TINYINT DEFAULT 1," +
                        "  `is_active` TINYINT DEFAULT 1" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
                stmt.executeUpdate(createUsersTable);

                // Bang transfer_logs
                String createLogsTable = "CREATE TABLE IF NOT EXISTS `transfer_logs` (" +
                        "  `id` INT AUTO_INCREMENT PRIMARY KEY," +
                        "  `username` VARCHAR(50) NOT NULL," +
                        "  `action` VARCHAR(20) NOT NULL," +
                        "  `filename` VARCHAR(255) NOT NULL," +
                        "  `filesize` BIGINT DEFAULT 0," +
                        "  `status` VARCHAR(20) NOT NULL," +
                        "  `timestamp` DATETIME NOT NULL" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
                stmt.executeUpdate(createLogsTable);

                // 4. Chen tai khoan mac dinh neu chua co
                String seedUsers = "INSERT IGNORE INTO `users` (`id`, `username`, `password`, `home_dir`, `permission_read`, `permission_write`, `is_active`) VALUES " +
                        "(1, 'admin', 'admin', 'server_storage/files', 1, 1, 1), " +
                        "(2, 'user', '123456', 'server_storage/files', 1, 1, 1);";
                stmt.executeUpdate(seedUsers);

                LOGGER.info("[DB] Khoi tao bang 'users' va 'transfer_logs' thanh cong.");
            }

            initialized = true;
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[DB_ERR] Khoi tao CSDL MySQL that bai: " + e.getMessage(), e);
            return false;
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(config.getJdbcUrl(), config.getUser(), config.getPassword());
    }

    public static boolean isAvailable() {
        try (Connection conn = getConnection()) {
            return conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }
}
