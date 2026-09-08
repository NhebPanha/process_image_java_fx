package com.rupp.trafficvision.thread;

import com.rupp.trafficvision.model.ThreadStatus;
import javafx.beans.property.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Encapsulates runtime metadata and state of an executor worker thread.
 * Displayed in the Thread Monitor panel.
 */
public class ProcessingWorker {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final StringProperty threadName;
    private final LongProperty threadId;
    private final ObjectProperty<ThreadStatus> status;
    private final StringProperty currentTask;
    private final ObjectProperty<LocalDateTime> lastActivity;
    private final LongProperty executionTimeMs;

    public ProcessingWorker(String threadName, long threadId) {
        this.threadName = new SimpleStringProperty(threadName);
        this.threadId = new SimpleLongProperty(threadId);
        this.status = new SimpleObjectProperty<>(ThreadStatus.IDLE);
        this.currentTask = new SimpleStringProperty("Awaiting Tasks");
        this.lastActivity = new SimpleObjectProperty<>(LocalDateTime.now());
        this.executionTimeMs = new SimpleLongProperty(0);
    }

    public void updateState(ThreadStatus status, String taskDescription, long elapsedMs) {
        this.status.set(status);
        this.currentTask.set(taskDescription != null ? taskDescription : "Idle");
        this.lastActivity.set(LocalDateTime.now());
        this.executionTimeMs.set(elapsedMs);
    }

    public String getThreadName() {
        return threadName.get();
    }

    public StringProperty threadNameProperty() {
        return threadName;
    }

    public long getThreadId() {
        return threadId.get();
    }

    public LongProperty threadIdProperty() {
        return threadId;
    }

    public ThreadStatus getStatus() {
        return status.get();
    }

    public ObjectProperty<ThreadStatus> statusProperty() {
        return status;
    }

    public String getCurrentTask() {
        return currentTask.get();
    }

    public StringProperty currentTaskProperty() {
        return currentTask;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity.get();
    }

    public ObjectProperty<LocalDateTime> lastActivityProperty() {
        return lastActivity;
    }

    public String getFormattedLastActivity() {
        return lastActivity.get().format(TIME_FMT);
    }

    public long getExecutionTimeMs() {
        return executionTimeMs.get();
    }

    public LongProperty executionTimeMsProperty() {
        return executionTimeMs;
    }
}
