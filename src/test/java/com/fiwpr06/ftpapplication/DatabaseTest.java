package com.fiwpr06.ftpapplication;

import com.fiwpr06.ftpapplication.server.database.FtpDatabaseManager;
import com.fiwpr06.ftpapplication.server.database.TransferLogDao;
import com.fiwpr06.ftpapplication.server.database.User;
import com.fiwpr06.ftpapplication.server.database.UserDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

public class DatabaseTest {

    @BeforeAll
    public static void setup() {
        boolean initialized = FtpDatabaseManager.initialize();
        Assertions.assertTrue(initialized, "Khoi tao CSDL MySQL thanh cong");
    }

    @Test
    public void testUserDaoAuthentication() {
        UserDao userDao = new UserDao();
        User admin = userDao.authenticate("admin", "admin");
        Assertions.assertNotNull(admin, "Tai khoan admin mac dinh phai dang nhap duoc");
        Assertions.assertEquals("admin", admin.getUsername());

        User user = userDao.authenticate("user", "123456");
        Assertions.assertNotNull(user, "Tai khoan user mac dinh phai dang nhap duoc");
        Assertions.assertEquals("123456", user.getPassword());
    }

    @Test
    public void testTransferLogDao() {
        TransferLogDao logDao = new TransferLogDao();
        boolean logged = logDao.log("admin", "UPLOAD", "test.txt", 1024, "SUCCESS");
        Assertions.assertTrue(logged, "Ghi nhan transfer log vao MySQL phai thanh cong");

        List<?> logs = logDao.getRecentLogs(10);
        Assertions.assertFalse(logs.isEmpty(), "Danh sach transfer logs khong duoc rong");
    }
}
