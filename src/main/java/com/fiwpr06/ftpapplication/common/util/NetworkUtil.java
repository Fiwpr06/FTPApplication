package com.fiwpr06.ftpapplication.common.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tien ich mang ho tro giao thuc FTP va Passive Mode
 */
public final class NetworkUtil {
    private static final Pattern PASV_PATTERN = Pattern.compile("(?:\\[|\\()(\\d{1,3},\\d{1,3},\\d{1,3},\\d{1,3}),(\\d{1,3}),(\\d{1,3})(?:\\]|\\))");

    private NetworkUtil() {}

    /**
     * Lay dia chi IPv4 that cua may (uu tien dia chi LAN non-loopback, bo qua cac card mang ao va link-local 169.254.x.x)
     */
    public static String getLocalIpAddress() {
        try {
            // Uu tien 1: IPv4 site-local (192.168.x.x, 172.16-31.x.x, 10.x.x.x) tren card mang dang hoat dong
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) continue;
                String displayName = iface.getDisplayName().toLowerCase();
                if (displayName.contains("vmware") || displayName.contains("virtual") || displayName.contains("vbox") || displayName.contains("wsl")) {
                    continue;
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress() && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }

            // Uu tien 2: Bat ky IPv4 non-loopback, non-link-local tren card mang hop le
            interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    /**
     * Dinh dang thong tin Passive Mode cho lenh PASV cua FTP Server:
     * Vi du IP 192.168.1.5, Port 20500 -> "192,168,1,5,80,20" (20500 = 80*256 + 20)
     */
    public static String formatPasvAddress(String ipAddress, int port) {
        String formattedIp = ipAddress.replace('.', ',');
        int p1 = port / 256;
        int p2 = port % 256;
        return String.format("%s,%d,%d", formattedIp, p1, p2);
    }

    /**
     * Phan tich phan hoi 227 cua FTP Server de lay IP va Port truyen du lieu (Data Connection)
     * Tra ve mang String[]: [0] la Host (e.g. 192.168.1.5), [1] la Port (e.g. 20500)
     */
    public static PasvHostPort parsePasvResponse(String response) {
        if (response == null) return null;
        Matcher matcher = PASV_PATTERN.matcher(response);
        if (matcher.find()) {
            String host = matcher.group(1).replace(',', '.');
            int p1 = Integer.parseInt(matcher.group(2));
            int p2 = Integer.parseInt(matcher.group(3));
            int port = (p1 * 256) + p2;
            return new PasvHostPort(host, port);
        }
        return null;
    }

    public static class PasvHostPort {
        private final String host;
        private final int port;

        public PasvHostPort(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() { return host; }
        public int getPort() { return port; }
    }
}
