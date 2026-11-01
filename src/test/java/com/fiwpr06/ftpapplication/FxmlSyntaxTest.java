package com.fiwpr06.ftpapplication;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FxmlSyntaxTest {

    @BeforeAll
    public static void initJavaFx() throws Exception {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX Platform startup timed out");
        } catch (IllegalStateException e) {
            // Already started
        }
    }

    @Test
    public void testServerViewFxmlLoad() {
        assertNotNull(getClass().getResource("/com/fiwpr06/ftpapplication/fxml/server_view.fxml"), "server_view.fxml must exist");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/fiwpr06/ftpapplication/fxml/server_view.fxml"));
        try {
            Object root = loader.load();
            assertNotNull(root, "server_view.fxml must load successfully");
        } catch (Exception e) {
            e.printStackTrace();
            org.junit.jupiter.api.Assertions.fail("server_view.fxml failed to load: " + e.getMessage());
        }
    }

    @Test
    public void testConnectViewFxmlLoad() {
        assertNotNull(getClass().getResource("/com/fiwpr06/ftpapplication/fxml/connect_view.fxml"), "connect_view.fxml must exist");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/fiwpr06/ftpapplication/fxml/connect_view.fxml"));
        try {
            Object root = loader.load();
            assertNotNull(root, "connect_view.fxml must load successfully");
        } catch (Exception e) {
            e.printStackTrace();
            org.junit.jupiter.api.Assertions.fail("connect_view.fxml failed to load: " + e.getMessage());
        }
    }

    @Test
    public void testClientViewFxmlLoad() {
        assertNotNull(getClass().getResource("/com/fiwpr06/ftpapplication/fxml/client_view.fxml"), "client_view.fxml must exist");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/fiwpr06/ftpapplication/fxml/client_view.fxml"));
        try {
            Object root = loader.load();
            assertNotNull(root, "client_view.fxml must load successfully");
        } catch (Exception e) {
            e.printStackTrace();
            org.junit.jupiter.api.Assertions.fail("client_view.fxml failed to load: " + e.getMessage());
        }
    }
}
