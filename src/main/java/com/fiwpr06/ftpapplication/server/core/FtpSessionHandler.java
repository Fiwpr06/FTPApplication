package com.fiwpr06.ftpapplication.server.core;

import com.fiwpr06.ftpapplication.common.model.FileInfo;
import com.fiwpr06.ftpapplication.common.model.FtpReplyCode;
import com.fiwpr06.ftpapplication.common.util.DateTimeUtil;
import com.fiwpr06.ftpapplication.common.util.FileUtils;
import com.fiwpr06.ftpapplication.common.util.NetworkUtil;
import com.fiwpr06.ftpapplication.server.command.FtpCommand;
import com.fiwpr06.ftpapplication.server.database.TransferLogDao;
import com.fiwpr06.ftpapplication.server.database.User;
import com.fiwpr06.ftpapplication.server.database.UserDao;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Xu ly mot phien ket noi FTP Client tren kenh dieu khien (Control Connection)
 */
public class FtpSessionHandler implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(FtpSessionHandler.class.getName());

    public enum AuthState {
        NOT_AUTHENTICATED,
        NEED_PASSWORD,
        AUTHENTICATED
    }

    private final String sessionId;
    private final Socket controlSocket;
    private final FtpServer server;
    private final UserDao userDao;
    private final TransferLogDao logDao;

    private BufferedReader reader;
    private BufferedWriter writer;
    private volatile boolean running = true;

    private AuthState authState = AuthState.NOT_AUTHENTICATED;
    private String pendingUsername;
    private User currentUser;
    private Path rootPath;
    private String currentVirtualPath = "/";
    private PassiveDataChannel passiveDataChannel;
    private String transferType = "I"; // Binary mode default
    private final long connectedTimeMillis;

    private final Map<String, FtpCommand> commandMap = new HashMap<>();

    public FtpSessionHandler(Socket controlSocket, FtpServer server) {
        this.sessionId = UUID.randomUUID().toString().substring(0, 8);
        this.controlSocket = controlSocket;
        this.server = server;
        this.userDao = new UserDao();
        this.logDao = new TransferLogDao();
        this.connectedTimeMillis = System.currentTimeMillis();
        this.rootPath = Paths.get("server_storage/files").toAbsolutePath().normalize();

        registerCommands();
    }

    private void registerCommands() {
        commandMap.put("USER", (s, arg) -> handleUser(arg));
        commandMap.put("PASS", (s, arg) -> handlePass(arg));
        commandMap.put("PWD", (s, arg) -> handlePwd());
        commandMap.put("XPWD", (s, arg) -> handlePwd());
        commandMap.put("CWD", (s, arg) -> handleCwd(arg));
        commandMap.put("XCWD", (s, arg) -> handleCwd(arg));
        commandMap.put("CDUP", (s, arg) -> handleCdup());
        commandMap.put("XCUP", (s, arg) -> handleCdup());
        commandMap.put("TYPE", (s, arg) -> handleType(arg));
        commandMap.put("PASV", (s, arg) -> handlePasv());
        commandMap.put("LIST", (s, arg) -> handleList(arg));
        commandMap.put("NLST", (s, arg) -> handleNlst(arg));
        commandMap.put("RETR", (s, arg) -> handleRetr(arg));
        commandMap.put("STOR", (s, arg) -> handleStor(arg));
        commandMap.put("DELE", (s, arg) -> handleDele(arg));
        commandMap.put("MKD", (s, arg) -> handleMkd(arg));
        commandMap.put("XMKD", (s, arg) -> handleMkd(arg));
        commandMap.put("RMD", (s, arg) -> handleRmd(arg));
        commandMap.put("XRMD", (s, arg) -> handleRmd(arg));
        commandMap.put("SYST", (s, arg) -> sendReply(FtpReplyCode.NAME_SYSTEM_TYPE, "UNIX Type: L8"));
        commandMap.put("FEAT", (s, arg) -> {
            sendReplyRaw("211-Features:\r\n PASV\r\n UTF8\r\n SIZE\r\n211 End\r\n");
        });
        commandMap.put("OPTS", (s, arg) -> {
            if (arg != null && arg.toUpperCase().startsWith("UTF8")) {
                sendReply(FtpReplyCode.COMMAND_OK, "Always in UTF8 mode.");
            } else {
                sendReply(FtpReplyCode.COMMAND_OK, "OPTS accepted.");
            }
        });
        commandMap.put("NOOP", (s, arg) -> sendReply(FtpReplyCode.COMMAND_OK, "NOOP ok."));
        commandMap.put("QUIT", (s, arg) -> {
            sendReply(FtpReplyCode.SERVICE_CLOSING, "Goodbye.");
            close();
        });
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream(), StandardCharsets.UTF_8));

            // Gui thong diep chao mung 220
            sendReply(FtpReplyCode.SERVICE_READY, "Service ready (FileZilla Compatible FTP Server).");

            String line;
            while (running && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                server.logCommand(sessionId, getClientIp(), line);

                int spaceIndex = line.indexOf(' ');
                String cmd = (spaceIndex != -1 ? line.substring(0, spaceIndex) : line).toUpperCase();
                String arg = spaceIndex != -1 ? line.substring(spaceIndex + 1).trim() : "";

                FtpCommand command = commandMap.get(cmd);
                if (command != null) {
                    try {
                        command.execute(this, arg);
                    } catch (Exception ex) {
                        LOGGER.warning("[CMD_ERR] Loi xu ly lenh " + cmd + ": " + ex.getMessage());
                        sendReply(FtpReplyCode.ACTION_ABORTED_LOCAL_ERROR, "Local error processing command.");
                    }
                } else {
                    sendReply(FtpReplyCode.COMMAND_NOT_IMPLEMENTED, "Command '" + cmd + "' not implemented.");
                }
            }
        } catch (IOException e) {
            // Client ngat ket noi
        } finally {
            close();
        }
    }

    // ================= XU LY CAC LENH CHINH =================

    private void handleUser(String username) throws IOException {
        if (username == null || username.isEmpty()) {
            sendReply(FtpReplyCode.SYNTAX_ERROR_PARAMETERS, "Username required.");
            return;
        }
        this.pendingUsername = username;
        this.authState = AuthState.NEED_PASSWORD;
        sendReply(FtpReplyCode.USER_NAME_OK, "User name okay, need password.");
    }

    private void handlePass(String password) throws IOException {
        if (authState != AuthState.NEED_PASSWORD || pendingUsername == null) {
            sendReply(FtpReplyCode.BAD_SEQUENCE_OF_COMMANDS, "Login with USER first.");
            return;
        }

        // Kiem tra MySQL
        User user = userDao.authenticate(pendingUsername, password);
        if (user != null) {
            this.currentUser = user;
            this.authState = AuthState.AUTHENTICATED;
            this.currentVirtualPath = "/";

            // Thiet lap thu muc goc
            if (user.getHomeDir() != null && !user.getHomeDir().isEmpty()) {
                this.rootPath = Paths.get(user.getHomeDir()).toAbsolutePath().normalize();
            } else {
                this.rootPath = Paths.get("server_storage/files").toAbsolutePath().normalize();
            }
            try {
                Files.createDirectories(this.rootPath);
            } catch (Exception ignored) {}

            sendReply(FtpReplyCode.USER_LOGGED_IN, "User logged in, proceed.");
            server.onSessionAuthenticated(this);
        } else {
            this.authState = AuthState.NOT_AUTHENTICATED;
            this.pendingUsername = null;
            sendReply(FtpReplyCode.NOT_LOGGED_IN, "Authentication failed. Check username and password in MySQL database.");
        }
    }

    private boolean checkAuthenticated() throws IOException {
        if (authState != AuthState.AUTHENTICATED) {
            sendReply(FtpReplyCode.NOT_LOGGED_IN, "Please login first.");
            return false;
        }
        return true;
    }

    private void handlePwd() throws IOException {
        if (!checkAuthenticated()) return;
        sendReply(FtpReplyCode.PATHNAME_CREATED, "\"" + currentVirtualPath + "\" is current directory.");
    }

    private void handleCwd(String dir) throws IOException {
        if (!checkAuthenticated()) return;
        if (dir == null || dir.isEmpty()) {
            sendReply(FtpReplyCode.FILE_ACTION_OK, "CWD okay.");
            return;
        }

        String targetVirtual;
        if (dir.startsWith("/")) {
            targetVirtual = dir;
        } else {
            targetVirtual = (currentVirtualPath.endsWith("/") ? currentVirtualPath : currentVirtualPath + "/") + dir;
        }

        Path resolved = FileUtils.resolveSafePath(rootPath, targetVirtual);
        if (Files.exists(resolved) && Files.isDirectory(resolved)) {
            // Cap nhat virtual path
            Path relative = rootPath.relativize(resolved);
            String relStr = relative.toString().replace('\\', '/');
            this.currentVirtualPath = relStr.isEmpty() ? "/" : "/" + relStr;
            sendReply(FtpReplyCode.FILE_ACTION_OK, "Directory changed to " + currentVirtualPath);
        } else {
            sendReply(FtpReplyCode.FILE_NOT_FOUND_OR_NO_ACCESS, "Directory not found.");
        }
    }

    private void handleCdup() throws IOException {
        if (!checkAuthenticated()) return;
        if (currentVirtualPath.equals("/")) {
            sendReply(FtpReplyCode.FILE_ACTION_OK, "Already at root directory.");
            return;
        }

        int lastSlash = currentVirtualPath.lastIndexOf('/');
        String parentVirtual = (lastSlash <= 0) ? "/" : currentVirtualPath.substring(0, lastSlash);
        handleCwd(parentVirtual);
    }

    private void handleType(String type) throws IOException {
        if (!checkAuthenticated()) return;
        if (type != null && (type.equalsIgnoreCase("I") || type.equalsIgnoreCase("L 8") || type.equalsIgnoreCase("L8"))) {
            this.transferType = "I";
            sendReply(FtpReplyCode.COMMAND_OK, "Type set to I (Binary).");
        } else if (type != null && type.equalsIgnoreCase("A")) {
            this.transferType = "A";
            sendReply(FtpReplyCode.COMMAND_OK, "Type set to A (ASCII).");
        } else {
            sendReply(FtpReplyCode.COMMAND_OK, "Type set to " + type);
        }
    }

    private void handlePasv() throws IOException {
        if (!checkAuthenticated()) return;
        if (passiveDataChannel != null) {
            passiveDataChannel.close();
        }

        this.passiveDataChannel = new PassiveDataChannel();
        String serverIp = controlSocket.getLocalAddress().getHostAddress();
        if (serverIp.equals("0.0.0.0") || serverIp.equals("127.0.0.1")) {
            serverIp = NetworkUtil.getLocalIpAddress();
        }

        String pasvStr = passiveDataChannel.getPasvAddress(serverIp);
        sendReply(FtpReplyCode.ENTERING_PASSIVE_MODE, "Entering Passive Mode (" + pasvStr + ").");
    }

    private void handleList(String arg) throws IOException {
        if (!checkAuthenticated()) return;
        if (passiveDataChannel == null) {
            sendReply(FtpReplyCode.CANT_OPEN_DATA_CONNECTION, "Use PASV first.");
            return;
        }

        Path targetDir = FileUtils.resolveSafePath(rootPath, currentVirtualPath);
        if (!Files.exists(targetDir)) {
            sendReply(FtpReplyCode.FILE_NOT_FOUND_OR_NO_ACCESS, "Directory not found.");
            passiveDataChannel.close();
            passiveDataChannel = null;
            return;
        }

        sendReply(FtpReplyCode.FILE_STATUS_OK, "Opening ASCII mode data connection for file list.");

        StringBuilder sb = new StringBuilder();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetDir)) {
            for (Path p : stream) {
                String name = p.getFileName().toString();
                boolean isDir = Files.isDirectory(p);
                long size = isDir ? 0 : Files.size(p);
                long lastModified = Files.getLastModifiedTime(p).toMillis();
                String dateStr = DateTimeUtil.formatUnixFtp(lastModified);

                FileInfo info = new FileInfo(name, size, isDir, lastModified, null);
                sb.append(info.toUnixListFormat(dateStr));
            }
        }

        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
        passiveDataChannel.sendData(data);
        passiveDataChannel = null;

        sendReply(FtpReplyCode.CLOSING_DATA_CONNECTION, "Closing data connection. Transfer complete.");
    }

    private void handleNlst(String arg) throws IOException {
        if (!checkAuthenticated()) return;
        if (passiveDataChannel == null) {
            sendReply(FtpReplyCode.CANT_OPEN_DATA_CONNECTION, "Use PASV first.");
            return;
        }

        Path targetDir = FileUtils.resolveSafePath(rootPath, currentVirtualPath);
        sendReply(FtpReplyCode.FILE_STATUS_OK, "Opening ASCII mode data connection for name list.");

        StringBuilder sb = new StringBuilder();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetDir)) {
            for (Path p : stream) {
                sb.append(p.getFileName().toString()).append("\r\n");
            }
        }

        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
        passiveDataChannel.sendData(data);
        passiveDataChannel = null;

        sendReply(FtpReplyCode.CLOSING_DATA_CONNECTION, "Closing data connection. Transfer complete.");
    }

    private void handleRetr(String filename) throws IOException {
        if (!checkAuthenticated()) return;
        if (!currentUser.isPermissionRead()) {
            sendReply(FtpReplyCode.FILE_NOT_FOUND_OR_NO_ACCESS, "Permission denied (Read not allowed).");
            return;
        }
        if (passiveDataChannel == null) {
            sendReply(FtpReplyCode.CANT_OPEN_DATA_CONNECTION, "Use PASV first.");
            return;
        }

        Path filePath = FileUtils.resolveSafePath(rootPath, currentVirtualPath + "/" + filename);
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            sendReply(FtpReplyCode.FILE_NOT_FOUND_OR_NO_ACCESS, "File not found: " + filename);
            passiveDataChannel.close();
            passiveDataChannel = null;
            return;
        }

        long fileSize = Files.size(filePath);
        sendReply(FtpReplyCode.FILE_STATUS_OK, String.format("Opening data connection for %s (%d bytes).", filename, fileSize));

        try (InputStream fis = Files.newInputStream(filePath)) {
            long sent = passiveDataChannel.sendStream(fis);
            logDao.log(currentUser.getUsername(), "DOWNLOAD", filename, sent, "SUCCESS");
            sendReply(FtpReplyCode.CLOSING_DATA_CONNECTION, "Transfer complete.");
        } catch (Exception e) {
            logDao.log(currentUser.getUsername(), "DOWNLOAD", filename, 0, "FAILED");
            sendReply(FtpReplyCode.ACTION_ABORTED_LOCAL_ERROR, "Transfer failed.");
        } finally {
            passiveDataChannel = null;
        }
    }

    private void handleStor(String filename) throws IOException {
        if (!checkAuthenticated()) return;
        if (!currentUser.isPermissionWrite()) {
            sendReply(FtpReplyCode.FILE_NOT_FOUND_OR_NO_ACCESS, "Permission denied (Write not allowed).");
            return;
        }
        if (passiveDataChannel == null) {
            sendReply(FtpReplyCode.CANT_OPEN_DATA_CONNECTION, "Use PASV first.");
            return;
        }

        Path filePath = FileUtils.resolveSafePath(rootPath, currentVirtualPath + "/" + filename);
        sendReply(FtpReplyCode.FILE_STATUS_OK, "Opening data connection for upload " + filename);

        try (OutputStream fos = Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            long received = passiveDataChannel.receiveStream(fos);
            logDao.log(currentUser.getUsername(), "UPLOAD", filename, received, "SUCCESS");
            sendReply(FtpReplyCode.CLOSING_DATA_CONNECTION, "Transfer complete.");
        } catch (Exception e) {
            logDao.log(currentUser.getUsername(), "UPLOAD", filename, 0, "FAILED");
            sendReply(FtpReplyCode.ACTION_ABORTED_LOCAL_ERROR, "Upload failed.");
        } finally {
            passiveDataChannel = null;
        }
    }

    private void handleDele(String filename) throws IOException {
        if (!checkAuthenticated()) return;
        if (!currentUser.isPermissionWrite()) {
            sendReply(FtpReplyCode.FILE_NOT_FOUND_OR_NO_ACCESS, "Permission denied (Write not allowed).");
            return;
        }

        Path filePath = FileUtils.resolveSafePath(rootPath, currentVirtualPath + "/" + filename);
        if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
            Files.delete(filePath);
            logDao.log(currentUser.getUsername(), "DELETE", filename, 0, "SUCCESS");
            sendReply(FtpReplyCode.FILE_ACTION_OK, "File deleted successfully.");
        } else {
            sendReply(FtpReplyCode.FILE_NOT_FOUND_OR_NO_ACCESS, "File not found or is a directory.");
        }
    }

    private void handleMkd(String dirname) throws IOException {
        if (!checkAuthenticated()) return;
        if (!currentUser.isPermissionWrite()) {
            sendReply(FtpReplyCode.FILE_NOT_FOUND_OR_NO_ACCESS, "Permission denied.");
            return;
        }

        Path dirPath = FileUtils.resolveSafePath(rootPath, currentVirtualPath + "/" + dirname);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
            sendReply(FtpReplyCode.PATHNAME_CREATED, "\"" + dirname + "\" directory created.");
        } else {
            sendReply(FtpReplyCode.FILE_NOT_FOUND_OR_NO_ACCESS, "Directory already exists.");
        }
    }

    private void handleRmd(String dirname) throws IOException {
        if (!checkAuthenticated()) return;
        if (!currentUser.isPermissionWrite()) {
            sendReply(FtpReplyCode.FILE_NOT_FOUND_OR_NO_ACCESS, "Permission denied.");
            return;
        }

        Path dirPath = FileUtils.resolveSafePath(rootPath, currentVirtualPath + "/" + dirname);
        if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
            FileUtils.deleteRecursively(dirPath);
            sendReply(FtpReplyCode.FILE_ACTION_OK, "Directory removed.");
        } else {
            sendReply(FtpReplyCode.FILE_NOT_FOUND_OR_NO_ACCESS, "Directory not found.");
        }
    }

    // ================= GUI THONG DIEP =================

    public void sendReply(int code, String message) throws IOException {
        String reply = code + " " + message + "\r\n";
        sendReplyRaw(reply);
        server.logReply(sessionId, getClientIp(), code + " " + message);
    }

    public synchronized void sendReplyRaw(String raw) throws IOException {
        if (writer != null) {
            writer.write(raw);
            writer.flush();
        }
    }

    public void close() {
        running = false;
        if (passiveDataChannel != null) {
            passiveDataChannel.close();
            passiveDataChannel = null;
        }
        try {
            if (controlSocket != null && !controlSocket.isClosed()) {
                controlSocket.close();
            }
        } catch (IOException ignored) {}
        server.onSessionClosed(this);
    }

    public String getSessionId() { return sessionId; }
    public String getClientIp() { return controlSocket.getInetAddress().getHostAddress(); }
    public int getClientPort() { return controlSocket.getPort(); }
    public String getUsername() { return currentUser != null ? currentUser.getUsername() : (pendingUsername != null ? pendingUsername : "anonymous"); }
    public String getCurrentVirtualPath() { return currentVirtualPath; }
    public long getConnectedTimeMillis() { return connectedTimeMillis; }
}
