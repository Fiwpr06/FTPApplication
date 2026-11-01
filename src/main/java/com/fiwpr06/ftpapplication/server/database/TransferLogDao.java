package com.fiwpr06.ftpapplication.server.database;

import com.fiwpr06.ftpapplication.common.util.DateTimeUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho bang `transfer_logs`
 */
public class TransferLogDao {

    public boolean log(String username, String action, String filename, long filesize, String status) {
        String sql = "INSERT INTO transfer_logs (username, action, filename, filesize, status, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = FtpDatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username != null ? username : "anonymous");
            ps.setString(2, action);
            ps.setString(3, filename);
            ps.setLong(4, filesize);
            ps.setString(5, status);
            ps.setString(6, DateTimeUtil.now());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<TransferLog> getRecentLogs(int limit) {
        List<TransferLog> list = new ArrayList<>();
        String sql = "SELECT * FROM transfer_logs ORDER BY id DESC LIMIT ?";
        try (Connection conn = FtpDatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TransferLog(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("action"),
                            rs.getString("filename"),
                            rs.getLong("filesize"),
                            rs.getString("status"),
                            rs.getString("timestamp")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
