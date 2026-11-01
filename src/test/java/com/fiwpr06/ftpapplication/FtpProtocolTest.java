package com.fiwpr06.ftpapplication;

import com.fiwpr06.ftpapplication.server.core.FtpServer;
import com.fiwpr06.ftpapplication.server.database.FtpDatabaseManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class FtpProtocolTest {
    private static FtpServer server;
    private static final int TEST_PORT = 21210;

    @BeforeAll
    public static void startServer() {
        FtpDatabaseManager.initialize();
        server = new FtpServer();
        boolean started = server.start(TEST_PORT);
        Assertions.assertTrue(started, "FTP Server phai start thanh cong tren port " + TEST_PORT);
    }

    @AfterAll
    public static void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testFtpRfc959HandshakeAndAuth() throws Exception {
        try (Socket socket = new Socket("127.0.0.1", TEST_PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            // 1. Nhan 220 Welcome
            String welcome = reader.readLine();
            Assertions.assertNotNull(welcome);
            Assertions.assertTrue(welcome.startsWith("220"), "Phan hoi phai bat dau bang 220: " + welcome);

            // 2. Gui USER admin
            writer.write("USER admin\r\n");
            writer.flush();
            String userReply = reader.readLine();
            Assertions.assertNotNull(userReply);
            Assertions.assertTrue(userReply.startsWith("331"), "Phan hoi USER phai la 331: " + userReply);

            // 3. Gui PASS admin
            writer.write("PASS admin\r\n");
            writer.flush();
            String passReply = reader.readLine();
            Assertions.assertNotNull(passReply);
            Assertions.assertTrue(passReply.startsWith("230"), "Phan hoi PASS dung phai la 230: " + passReply);

            // 4. Gui PWD
            writer.write("PWD\r\n");
            writer.flush();
            String pwdReply = reader.readLine();
            Assertions.assertNotNull(pwdReply);
            Assertions.assertTrue(pwdReply.startsWith("257"), "Phan hoi PWD phai la 257: " + pwdReply);

            // 5. Gui TYPE I
            writer.write("TYPE I\r\n");
            writer.flush();
            String typeReply = reader.readLine();
            Assertions.assertNotNull(typeReply);
            Assertions.assertTrue(typeReply.startsWith("200"), "Phan hoi TYPE I phai la 200: " + typeReply);

            // 6. Gui PASV
            writer.write("PASV\r\n");
            writer.flush();
            String pasvReply = reader.readLine();
            Assertions.assertNotNull(pasvReply);
            Assertions.assertTrue(pasvReply.startsWith("227"), "Phan hoi PASV phai la 227: " + pasvReply);

            // 7. Gui QUIT
            writer.write("QUIT\r\n");
            writer.flush();
            String quitReply = reader.readLine();
            Assertions.assertNotNull(quitReply);
            Assertions.assertTrue(quitReply.startsWith("221"), "Phan hoi QUIT phai la 221: " + quitReply);
        }
    }
}
