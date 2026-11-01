package com.fiwpr06.ftpapplication.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Tien ich dinh dang thoi gian cho FTP Client va Server
 */
public final class DateTimeUtil {
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter UNIX_FTP_FORMATTER = DateTimeFormatter.ofPattern("MMM dd HH:mm", Locale.ENGLISH);

    private DateTimeUtil() {}

    public static String now() {
        return LocalDateTime.now().format(DISPLAY_FORMATTER);
    }

    public static String formatEpochMillis(long millis) {
        if (millis <= 0) return "-";
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
                .format(DISPLAY_FORMATTER);
    }

    public static String formatUnixFtp(long millis) {
        if (millis <= 0) return "Jan 01 00:00";
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
                .format(UNIX_FTP_FORMATTER);
    }
}
