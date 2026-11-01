package com.fiwpr06.ftpapplication.common.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Tien ich thao tac voi tap tin va thu muc an toan
 */
public final class FileUtils {
    private FileUtils() {}

    /**
     * Dinh dang kich thuoc byte thanh B, KB, MB, GB de doc
     */
    public static String formatSize(long bytes) {
        if (bytes < 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(java.util.Locale.US, "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Giai quyet duong dan an toan chong tan cong Path Traversal (Directory Traversal)
     * Neu requestedPath co y dinh vuot ra ngoai root, tra ve root
     */
    public static Path resolveSafePath(Path root, String requestedRelativePath) {
        if (root == null) throw new IllegalArgumentException("Root path cannot be null");
        Path normalizedRoot = root.toAbsolutePath().normalize();
        if (requestedRelativePath == null || requestedRelativePath.trim().isEmpty() || requestedRelativePath.equals("/")) {
            return normalizedRoot;
        }

        // Chuan hoa duong dan yeu cau
        String cleanPath = requestedRelativePath.replace('\\', '/').trim();
        while (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }

        Path resolved = normalizedRoot.resolve(cleanPath).normalize();
        // Kiem tra xem resolved co nam trong normalizedRoot khong
        if (!resolved.startsWith(normalizedRoot)) {
            return normalizedRoot; // Ngăn chặn truy cập ra ngoài thư mục cho phép
        }
        return resolved;
    }

    /**
     * Xoa thu muc de quy (bao gom ca tap tin va thu muc con ben trong)
     */
    public static void deleteRecursively(Path path) throws IOException {
        if (path == null || !Files.exists(path)) return;
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
