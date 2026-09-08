package com.rupp.trafficvision.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility for formatting date, time, and execution durations.
 */
public final class TimeUtil {

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private TimeUtil() {}

    public static String formatCurrentTime() {
        return LocalDateTime.now().format(DISPLAY_TIME);
    }

    public static String formatForFilename(LocalDateTime time) {
        return (time != null ? time : LocalDateTime.now()).format(FILE_TIME);
    }

    public static String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + " ms";
        }
        return String.format("%.2f s", millis / 1000.0);
    }
}
