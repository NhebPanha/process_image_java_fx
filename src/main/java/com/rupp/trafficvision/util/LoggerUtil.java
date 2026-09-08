package com.rupp.trafficvision.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Thread-aware logger utility providing formatted console output with timestamps and thread identification.
 */
public final class LoggerUtil {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private LoggerUtil() {}

    public static void info(String message) {
        log("INFO", message);
    }

    public static void warn(String message) {
        log("WARN", message);
    }

    public static void error(String message) {
        log("ERROR", message);
    }

    public static void debug(String message) {
        log("DEBUG", message);
    }

    private static void log(String level, String message) {
        String timestamp = LocalTime.now().format(TIME_FMT);
        String threadName = Thread.currentThread().getName();
        System.out.printf("[%s] [%-5s] [%-16s] %s%n", timestamp, level, threadName, message);
    }
}
