package com.fiwpr06.ftpapplication.server.core;

import com.fiwpr06.ftpapplication.common.util.NetworkUtil;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Kenh truyen du lieu che do bi dong (PASV) cho FTP Server
 */
public class PassiveDataChannel implements Closeable {
    private final ServerSocket dataServerSocket;
    private Socket activeDataSocket;

    public PassiveDataChannel() throws IOException {
        // Mo dynamic port ngau nhien
        this.dataServerSocket = new ServerSocket(0);
        this.dataServerSocket.setSoTimeout(30000); // 30 giay timeout cho ket noi du lieu
    }

    public int getPort() {
        return dataServerSocket.getLocalPort();
    }

    public String getPasvAddress(String serverIp) {
        return NetworkUtil.formatPasvAddress(serverIp, getPort());
    }

    public synchronized Socket accept() throws IOException {
        if (activeDataSocket == null || activeDataSocket.isClosed()) {
            activeDataSocket = dataServerSocket.accept();
        }
        return activeDataSocket;
    }

    public void sendData(byte[] bytes) throws IOException {
        try (Socket socket = accept();
             OutputStream os = socket.getOutputStream()) {
            os.write(bytes);
            os.flush();
        } finally {
            close();
        }
    }

    public long sendStream(InputStream is) throws IOException {
        try (Socket socket = accept();
             OutputStream os = socket.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            long total = 0;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
                total += read;
            }
            os.flush();
            return total;
        } finally {
            close();
        }
    }

    public long receiveStream(OutputStream os) throws IOException {
        try (Socket socket = accept();
             InputStream is = socket.getInputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            long total = 0;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
                total += read;
            }
            os.flush();
            return total;
        } finally {
            close();
        }
    }

    @Override
    public synchronized void close() {
        if (activeDataSocket != null && !activeDataSocket.isClosed()) {
            try {
                activeDataSocket.close();
            } catch (IOException ignored) {}
        }
        if (dataServerSocket != null && !dataServerSocket.isClosed()) {
            try {
                dataServerSocket.close();
            } catch (IOException ignored) {}
        }
    }
}
