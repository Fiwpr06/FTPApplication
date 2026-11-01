package com.fiwpr06.ftpapplication.server.core;

import com.fiwpr06.ftpapplication.server.database.FtpDatabaseManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Bo may FTP Server lang nghe ket noi TCP va quan ly cac phien Client
 */
public class FtpServer {
    private static final Logger LOGGER = Logger.getLogger(FtpServer.class.getName());

    public interface ServerEventListener {
        void onServerStarted(int port);
        void onServerStopped();
        void onClientConnected(FtpSessionHandler session);
        void onClientAuthenticated(FtpSessionHandler session);
        void onClientDisconnected(FtpSessionHandler session);
        void onLog(String timestamp, String sessionId, String clientIp, String message, boolean isCommand);
    }

    private int port = 2121;
    private String connectionPasscode = "FTP888";
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private ExecutorService threadPool;

    private final ConcurrentHashMap<String, FtpSessionHandler> activeSessions = new ConcurrentHashMap<>();
    private final List<ServerEventListener> listeners = Collections.synchronizedList(new ArrayList<>());

    public FtpServer() {}

    public FtpServer(int port) {
        this.port = port;
    }

    public String getConnectionPasscode() {
        return connectionPasscode;
    }

    public void setConnectionPasscode(String passcode) {
        if (passcode != null && !passcode.trim().isEmpty()) {
            this.connectionPasscode = passcode.trim();
        }
    }

    public void addListener(ServerEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ServerEventListener listener) {
        listeners.remove(listener);
    }

    public synchronized boolean start(int portToListen) {
        if (running) return true;
        this.port = portToListen;

        // Dam bao CSDL MySQL da khoi tao
        if (!FtpDatabaseManager.initialize()) {
            LOGGER.severe("[FTP_SERVER] Khong the khoi tao MySQL Database. Server se khong khoi chay.");
            return false;
        }

        new com.fiwpr06.ftpapplication.server.database.UserDao().clearAllSessions();

        try {
            serverSocket = new ServerSocket(this.port);
            running = true;
            threadPool = Executors.newCachedThreadPool();

            // Luong lang nghe ket noi
            threadPool.submit(this::listenLoop);

            LOGGER.info("[FTP_SERVER] Server da khoi dong thanh cong tren cong " + this.port);
            for (ServerEventListener l : listeners) {
                l.onServerStarted(this.port);
            }
            return true;
        } catch (IOException e) {
            LOGGER.severe("[FTP_SERVER_ERR] Loi khoi dong ServerSocket: " + e.getMessage());
            return false;
        }
    }

    private void listenLoop() {
        while (running && serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setTcpNoDelay(true);

                FtpSessionHandler session = new FtpSessionHandler(clientSocket, this);
                activeSessions.put(session.getSessionId(), session);

                for (ServerEventListener l : listeners) {
                    l.onClientConnected(session);
                }

                threadPool.submit(session);
            } catch (IOException e) {
                if (!running) break;
            }
        }
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;

        // Dong tat ca phien active
        for (FtpSessionHandler session : activeSessions.values()) {
            session.close();
        }
        activeSessions.clear();

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}

        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdownNow();
        }

        LOGGER.info("[FTP_SERVER] Server da dung hoan toan.");
        for (ServerEventListener l : listeners) {
            l.onServerStopped();
        }
    }

    // Callbacks tu SessionHandler
    void onSessionAuthenticated(FtpSessionHandler session) {
        for (ServerEventListener l : listeners) {
            l.onClientAuthenticated(session);
        }
    }

    void onSessionClosed(FtpSessionHandler session) {
        activeSessions.remove(session.getSessionId());
        for (ServerEventListener l : listeners) {
            l.onClientDisconnected(session);
        }
    }

    void logCommand(String sessionId, String clientIp, String command) {
        String time = com.fiwpr06.ftpapplication.common.util.DateTimeUtil.now();
        for (ServerEventListener l : listeners) {
            l.onLog(time, sessionId, clientIp, command, true);
        }
    }

    void logReply(String sessionId, String clientIp, String reply) {
        String time = com.fiwpr06.ftpapplication.common.util.DateTimeUtil.now();
        for (ServerEventListener l : listeners) {
            l.onLog(time, sessionId, clientIp, reply, false);
        }
    }

    public boolean isRunning() { return running; }
    public int getPort() { return port; }
    public List<FtpSessionHandler> getActiveSessions() { return new ArrayList<>(activeSessions.values()); }
}
