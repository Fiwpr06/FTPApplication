package com.fiwpr06.ftpapplication.client.network;

import com.fiwpr06.ftpapplication.client.model.RemoteFileItem;
import com.fiwpr06.ftpapplication.common.model.FtpReplyCode;
import com.fiwpr06.ftpapplication.common.util.NetworkUtil;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bo dieu khien ket noi FTP Client tuong thich chuan RFC 959 (chuyen che do PASV)
 */
public class FtpClient {

    public interface ClientEventListener {
        void onCommandSent(String command);
        void onReplyReceived(int code, String fullReply);
        void onConnectionLost();
    }

    public interface ProgressCallback {
        void onProgress(long transferredBytes, long totalBytes, double speedKbps);
    }

    private Socket controlSocket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private boolean connected = false;
    private String currentDirectory = "/";

    private ClientEventListener listener;

    public void setListener(ClientEventListener listener) {
        this.listener = listener;
    }

    public synchronized boolean connect(String host, int port) {
        try {
            controlSocket = new Socket(host, port);
            controlSocket.setSoTimeout(15000);
            reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream(), StandardCharsets.UTF_8));

            String welcome = readReply();
            if (welcome != null && welcome.startsWith("220")) {
                connected = true;
                return true;
            }
        } catch (IOException e) {
            disconnect();
        }
        return false;
    }

    public synchronized boolean login(String username, String password) throws IOException {
        if (!connected) return false;

        sendCommand("USER " + username);
        String userReply = readReply();
        if (userReply == null || !userReply.startsWith("331")) {
            return false;
        }

        sendCommand("PASS " + password);
        String passReply = readReply();
        if (passReply != null && passReply.startsWith("230")) {
            sendCommand("TYPE I");
            readReply();
            pwd();
            return true;
        }
        return false;
    }

    public synchronized String pwd() throws IOException {
        sendCommand("PWD");
        String reply = readReply();
        if (reply != null && reply.startsWith("257")) {
            // Trich xuat noi dung trong dau ngoac kep: 257 "/home/dir" is current directory.
            int firstQuote = reply.indexOf('"');
            int lastQuote = reply.lastIndexOf('"');
            if (firstQuote != -1 && lastQuote > firstQuote) {
                this.currentDirectory = reply.substring(firstQuote + 1, lastQuote);
            }
        }
        return currentDirectory;
    }

    public synchronized boolean cwd(String path) throws IOException {
        sendCommand("CWD " + path);
        String reply = readReply();
        if (reply != null && reply.startsWith("250")) {
            pwd();
            return true;
        }
        return false;
    }

    public synchronized boolean cdup() throws IOException {
        sendCommand("CDUP");
        String reply = readReply();
        if (reply != null && reply.startsWith("250")) {
            pwd();
            return true;
        }
        return false;
    }

    public synchronized List<RemoteFileItem> list() throws IOException {
        List<RemoteFileItem> list = new ArrayList<>();
        Socket dataSocket = openPassiveDataSocket();
        if (dataSocket == null) return list;

        sendCommand("LIST");
        String fileStatus = readReply();
        if (fileStatus == null || (!fileStatus.startsWith("150") && !fileStatus.startsWith("125"))) {
            dataSocket.close();
            return list;
        }

        try (BufferedReader dataReader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = dataReader.readLine()) != null) {
                RemoteFileItem item = parseUnixListLine(line);
                if (item != null) {
                    list.add(item);
                }
            }
        } finally {
            dataSocket.close();
        }

        readReply(); // Nhan ma 226 Transfer complete
        return list;
    }

    public synchronized boolean uploadFile(File localFile, String remoteFileName, ProgressCallback callback) throws IOException {
        if (localFile == null || !localFile.exists()) return false;

        sendCommand("TYPE I");
        readReply();

        Socket dataSocket = openPassiveDataSocket();
        if (dataSocket == null) return false;

        long fileSize = localFile.length();
        sendCommand("STOR " + remoteFileName);
        String reply150 = readReply();
        if (reply150 == null || (!reply150.startsWith("150") && !reply150.startsWith("125"))) {
            dataSocket.close();
            return false;
        }

        long transferred = 0;
        long startTime = System.currentTimeMillis();

        try (FileInputStream fis = new FileInputStream(localFile);
             OutputStream os = dataSocket.getOutputStream()) {
            byte[] buffer = new byte[16384];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                os.write(buffer, 0, read);
                transferred += read;

                if (callback != null) {
                    long elapsed = Math.max(1, System.currentTimeMillis() - startTime);
                    double speed = (transferred / 1024.0) / (elapsed / 1000.0);
                    callback.onProgress(transferred, fileSize, speed);
                }
            }
            os.flush();
        } finally {
            dataSocket.close();
        }

        String complete = readReply();
        return complete != null && complete.startsWith("226");
    }

    public synchronized boolean downloadFile(String remoteFileName, File targetLocalFile, ProgressCallback callback) throws IOException {
        sendCommand("TYPE I");
        readReply();

        Socket dataSocket = openPassiveDataSocket();
        if (dataSocket == null) return false;

        sendCommand("RETR " + remoteFileName);
        String reply150 = readReply();
        if (reply150 == null || (!reply150.startsWith("150") && !reply150.startsWith("125"))) {
            dataSocket.close();
            return false;
        }

        // Tinh size tu reply 150 neu co
        long totalBytes = 0;
        Pattern sizePattern = Pattern.compile("\\((\\d+)\\s+bytes\\)");
        Matcher m = sizePattern.matcher(reply150);
        if (m.find()) {
            totalBytes = Long.parseLong(m.group(1));
        }

        long transferred = 0;
        long startTime = System.currentTimeMillis();

        try (InputStream is = dataSocket.getInputStream();
             FileOutputStream fos = new FileOutputStream(targetLocalFile)) {
            byte[] buffer = new byte[16384];
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
                transferred += read;

                if (callback != null) {
                    long elapsed = Math.max(1, System.currentTimeMillis() - startTime);
                    double speed = (transferred / 1024.0) / (elapsed / 1000.0);
                    callback.onProgress(transferred, totalBytes > 0 ? totalBytes : transferred, speed);
                }
            }
            fos.flush();
        } finally {
            dataSocket.close();
        }

        String complete = readReply();
        return complete != null && complete.startsWith("226");
    }

    public synchronized boolean deleteFile(String remoteFileName) throws IOException {
        sendCommand("DELE " + remoteFileName);
        String reply = readReply();
        return reply != null && reply.startsWith("250");
    }

    public synchronized boolean makeDirectory(String dirName) throws IOException {
        sendCommand("MKD " + dirName);
        String reply = readReply();
        return reply != null && reply.startsWith("257");
    }

    public synchronized boolean removeDirectory(String dirName) throws IOException {
        sendCommand("RMD " + dirName);
        String reply = readReply();
        return reply != null && reply.startsWith("250");
    }

    private Socket openPassiveDataSocket() throws IOException {
        sendCommand("PASV");
        String pasvReply = readReply();
        if (pasvReply == null || !pasvReply.startsWith("227")) {
            return null;
        }

        NetworkUtil.PasvHostPort hp = NetworkUtil.parsePasvResponse(pasvReply);
        if (hp == null) return null;

        // Luon uu tien su dung dia chi host ma Control Socket da ket noi thanh cong toi Server.
        // Dieu nay tranh truong hop Server nam sau NAT hoac tra ve IP card mang ao (VMware/WSL/VirtualBox),
        // loopback IP hoac IP link-local (169.254.x.x) khong the dinh tuyen qua lai giua cac may.
        String host = (controlSocket != null && controlSocket.getInetAddress() != null)
                ? controlSocket.getInetAddress().getHostAddress()
                : hp.getHost();

        Socket dataSocket = new Socket(host, hp.getPort());
        dataSocket.setSoTimeout(30000);
        return dataSocket;
    }

    private void sendCommand(String cmd) throws IOException {
        if (writer == null) return;
        writer.write(cmd + "\r\n");
        writer.flush();
        if (listener != null) {
            listener.onCommandSent(cmd);
        }
    }

    private String readReply() throws IOException {
        if (reader == null) return null;
        String line = reader.readLine();
        if (line == null) {
            if (listener != null) listener.onConnectionLost();
            return null;
        }

        // Xu ly multiline replies (ví dụ: 211-Features ... 211 End)
        StringBuilder sb = new StringBuilder(line);
        if (line.length() >= 4 && line.charAt(3) == '-') {
            String codePrefix = line.substring(0, 3) + " ";
            String subLine;
            while ((subLine = reader.readLine()) != null) {
                sb.append("\n").append(subLine);
                if (subLine.startsWith(codePrefix)) {
                    break;
                }
            }
        }

        int code = 0;
        try {
            code = Integer.parseInt(line.substring(0, 3));
        } catch (Exception ignored) {}

        if (listener != null) {
            listener.onReplyReceived(code, sb.toString());
        }
        return sb.toString();
    }

    private RemoteFileItem parseUnixListLine(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        // Mau chuan Unix: drwxr-xr-x 1 owner group 0 Nov 01 10:00 filename
        String[] parts = line.trim().split("\\s+", 9);
        if (parts.length >= 9) {
            String perms = parts[0];
            boolean isDir = perms.startsWith("d");
            long size = 0;
            try {
                size = Long.parseLong(parts[4]);
            } catch (Exception ignored) {}
            String date = parts[5] + " " + parts[6] + " " + parts[7];
            String name = parts[8];
            return new RemoteFileItem(name, isDir, size, perms, date);
        }
        return null;
    }

    public synchronized void disconnect() {
        connected = false;
        try {
            if (writer != null) {
                writer.write("QUIT\r\n");
                writer.flush();
            }
        } catch (Exception ignored) {}
        try {
            if (controlSocket != null && !controlSocket.isClosed()) {
                controlSocket.close();
            }
        } catch (Exception ignored) {}
        controlSocket = null;
        reader = null;
        writer = null;
    }

    public boolean isConnected() { return connected; }
    public String getCurrentDirectory() { return currentDirectory; }
}
