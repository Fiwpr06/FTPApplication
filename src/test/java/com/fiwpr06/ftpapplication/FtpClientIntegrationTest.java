package com.fiwpr06.ftpapplication;

import com.fiwpr06.ftpapplication.client.model.RemoteFileItem;
import com.fiwpr06.ftpapplication.client.network.FtpClient;
import com.fiwpr06.ftpapplication.server.core.FtpServer;
import com.fiwpr06.ftpapplication.server.database.FtpDatabaseManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class FtpClientIntegrationTest {
    private static FtpServer server;
    private static final int TEST_PORT = 21211;

    @BeforeAll
    public static void startServer() {
        FtpDatabaseManager.initialize();
        server = new FtpServer();
        boolean started = server.start(TEST_PORT);
        Assertions.assertTrue(started, "FTP Server phai bat thanh cong");
    }

    @AfterAll
    public static void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testFullClientWorkflow() throws Exception {
        FtpClient client = new FtpClient();
        boolean connected = client.connect("127.0.0.1", TEST_PORT);
        Assertions.assertTrue(connected, "Client ket noi thanh cong");

        boolean loggedIn = client.login("admin", "admin");
        Assertions.assertTrue(loggedIn, "Dang nhap user admin phai thanh cong");

        // 1. Kiem tra thu muc hien tai
        String pwd = client.pwd();
        Assertions.assertEquals("/", pwd);

        // 2. Tao thu muc test_dir
        boolean mkd = client.makeDirectory("test_dir");
        Assertions.assertTrue(mkd, "Tao thu muc test_dir phai thanh cong");

        // 3. Chuyen vao test_dir
        boolean cwd = client.cwd("test_dir");
        Assertions.assertTrue(cwd, "CWD vao test_dir phai thanh cong");
        Assertions.assertEquals("/test_dir", client.pwd());

        // 4. Upload file mau
        File tempUpload = File.createTempFile("ftp_upload_test", ".txt");
        String content = "Hello FTP RFC 959 World from JavaFX Integration Test!";
        Files.writeString(tempUpload.toPath(), content);

        boolean uploaded = client.uploadFile(tempUpload, "remote_sample.txt", null);
        Assertions.assertTrue(uploaded, "Upload file len server phai thanh cong");

        // 5. Danh sach file tren server
        List<RemoteFileItem> remoteFiles = client.list();
        Assertions.assertFalse(remoteFiles.isEmpty(), "Danh sach file tren server phai co tap tin vua upload");
        boolean fileFound = remoteFiles.stream().anyMatch(f -> f.getName().equals("remote_sample.txt"));
        Assertions.assertTrue(fileFound, "remote_sample.txt phai xuat hien trong danh sach LIST");

        // 6. Download file ve may
        File tempDownload = File.createTempFile("ftp_download_test", ".txt");
        boolean downloaded = client.downloadFile("remote_sample.txt", tempDownload, null);
        Assertions.assertTrue(downloaded, "Download file ve may phai thanh cong");

        String downloadedContent = Files.readString(tempDownload.toPath());
        Assertions.assertEquals(content, downloadedContent, "Noi dung file download phai trung khop voi noi dung da upload");

        // 7. Xoa file va don dep
        boolean deleted = client.deleteFile("remote_sample.txt");
        Assertions.assertTrue(deleted, "Xoa remote file phai thanh cong");

        client.cdup();
        boolean rmd = client.removeDirectory("test_dir");
        Assertions.assertTrue(rmd, "Xoa remote directory phai thanh cong");

        client.disconnect();
        Assertions.assertFalse(client.isConnected());

        tempUpload.delete();
        tempDownload.delete();
    }
}
