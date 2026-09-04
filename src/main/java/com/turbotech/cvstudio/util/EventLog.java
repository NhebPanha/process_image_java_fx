package com.turbotech.cvstudio.util;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Thread-safe rolling event logger with an observable list for JavaFX UI binding.
 */
public final class EventLog {

    public enum Level {
        INFO("INFO", "#4fc3f7"),
        WARN("WARN", "#ffb74d"),
        ERROR("ERROR", "#e57373"),
        SUCCESS("SUCCESS", "#81c784");

        private final String label;
        private final String colorHex;

        Level(String label, String colorHex) {
            this.label = label;
            this.colorHex = colorHex;
        }

        public String getLabel() { return label; }
        public String getColorHex() { return colorHex; }
    }

    public record LogEntry(String timestamp, Level level, String message) {
        @Override
        public String toString() {
            return String.format("[%s] [%s] %s", timestamp, level.getLabel(), message);
        }
    }

    private static final int MAX_ENTRIES = 500;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final ObservableList<LogEntry> ENTRIES = FXCollections.observableArrayList();

    private EventLog() {}

    public static ObservableList<LogEntry> getEntries() {
        return ENTRIES;
    }

    public static void log(Level level, String message) {
        String time = LocalTime.now().format(TIME_FORMATTER);
        LogEntry entry = new LogEntry(time, level, message);

        try {
            if (Platform.isFxApplicationThread()) {
                addEntry(entry);
            } else {
                Platform.runLater(() -> addEntry(entry));
            }
        } catch (IllegalStateException e) {
            // JavaFX toolkit is not running (e.g., during headless unit tests)
            addEntry(entry);
        }
    }

    private static void addEntry(LogEntry entry) {
        if (ENTRIES.size() >= MAX_ENTRIES) {
            ENTRIES.remove(0);
        }
        ENTRIES.add(entry);
    }

    public static void info(String message) {
        log(Level.INFO, message);
    }

    public static void warn(String message) {
        log(Level.WARN, message);
    }

    public static void error(String message) {
        log(Level.ERROR, message);
    }

    public static void success(String message) {
        log(Level.SUCCESS, message);
    }

    public static void clear() {
        try {
            if (Platform.isFxApplicationThread()) {
                ENTRIES.clear();
            } else {
                Platform.runLater(ENTRIES::clear);
            }
        } catch (IllegalStateException e) {
            ENTRIES.clear();
        }
    }
}
