package com.fiwpr06.ftpapplication.server.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho bang `users`
 */
public class UserDao {

    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ? AND is_active = 1";
        try (Connection conn = FtpDatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public User getByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = FtpDatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY id ASC";
        try (Connection conn = FtpDatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean addUser(User user) {
        String sql = "INSERT INTO users (username, password, home_dir, permission_read, permission_write, is_active) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = FtpDatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getHomeDir() != null ? user.getHomeDir() : "server_storage/files");
            ps.setBoolean(4, user.isPermissionRead());
            ps.setBoolean(5, user.isPermissionWrite());
            ps.setBoolean(6, user.isActive());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateUser(User user) {
        String sql = "UPDATE users SET password = ?, home_dir = ?, permission_read = ?, permission_write = ?, is_active = ? WHERE username = ?";
        try (Connection conn = FtpDatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getPassword());
            ps.setString(2, user.getHomeDir());
            ps.setBoolean(3, user.isPermissionRead());
            ps.setBoolean(4, user.isPermissionWrite());
            ps.setBoolean(5, user.isActive());
            ps.setString(6, user.getUsername());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteUser(String username) {
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = FtpDatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public synchronized User getOrCreatePasscodeUser(String displayName) {
        User user = getByUsername(displayName);
        if (user == null) {
            user = new User(0, displayName, "", "server_storage/files", true, true, true);
            addUser(user);
            user = getByUsername(displayName);
        }
        return user;
    }

    public void recordSession(String sessionId, String displayName, String clientIp, String passcode) {
        String sql = "INSERT INTO active_sessions (session_id, display_name, client_ip, passcode_used, connected_at, last_active) " +
                "VALUES (?, ?, ?, ?, NOW(), NOW()) ON DUPLICATE KEY UPDATE last_active = NOW()";
        try (Connection conn = FtpDatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.setString(2, displayName);
            ps.setString(3, clientIp);
            ps.setString(4, passcode);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeSession(String sessionId) {
        String sql = "DELETE FROM active_sessions WHERE session_id = ?";
        try (Connection conn = FtpDatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void clearAllSessions() {
        String sql = "DELETE FROM active_sessions";
        try (Connection conn = FtpDatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("home_dir"),
                rs.getBoolean("permission_read"),
                rs.getBoolean("permission_write"),
                rs.getBoolean("is_active")
        );
    }
}
