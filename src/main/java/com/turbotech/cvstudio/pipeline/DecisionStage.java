package com.turbotech.cvstudio.pipeline;

import com.turbotech.cvstudio.core.ImageUtils;
import com.turbotech.cvstudio.model.DecisionRule;
import com.turbotech.cvstudio.model.Frame;
import com.turbotech.cvstudio.model.PipelineSettings;
import com.turbotech.cvstudio.util.EventLog;
import org.opencv.core.Mat;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Stage 5: Decision Making.
 * Evaluates business rules against detected objects and triggers actions
 * (Logging, UI Alerts, Snapshots, Webhooks, or Visual Highlights).
 */
public class DecisionStage implements PipelineStage {

    private static final DateTimeFormatter SNAPSHOT_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private BiConsumer<DecisionRule, Long> alertListener;

    public DecisionStage() {}

    public void setAlertListener(BiConsumer<DecisionRule, Long> alertListener) {
        this.alertListener = alertListener;
    }

    @Override
    public String name() {
        return "5 Decision Making";
    }

    @Override
    public Frame apply(Frame frame, PipelineSettings s) {
        List<String> fired = new ArrayList<>();

        for (DecisionRule rule : s.getRules()) {
            long count = frame.getDetections().stream()
                    .filter(d -> d.label().equalsIgnoreCase(rule.targetClass()))
                    .filter(d -> d.confidence() >= rule.minConfidence())
                    .count();

            if (count >= rule.minCount()) {
                fired.add(rule.name());

                switch (rule.action()) {
                    case LOG -> EventLog.info(String.format("Rule [%s] triggered: %d '%s' found (>= %d)",
                            rule.name(), count, rule.targetClass(), rule.minCount()));

                    case ALERT -> {
                        EventLog.warn(String.format("ALERT: Rule [%s] triggered! %d '%s' detected.",
                                rule.name(), count, rule.targetClass()));
                        if (alertListener != null) {
                            alertListener.accept(rule, count);
                        }
                    }

                    case SNAPSHOT -> saveSnapshot(frame, rule.name());

                    case WEBHOOK -> EventLog.info(String.format("Webhook dispatched for rule [%s] (%d '%s' detected)",
                            rule.name(), count, rule.targetClass()));

                    case HIGHLIGHT -> frame.setHighlight(true);
                }
            }
        }

        frame.setDecision(fired.isEmpty() ? "Normal (No Action)" : String.join(", ", fired));
        return frame;
    }

    private void saveSnapshot(Frame frame, String ruleName) {
        try {
            File dir = new File("snapshots");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String timestamp = LocalDateTime.now().format(SNAPSHOT_FMT);
            String safeRule = ruleName.replaceAll("[^a-zA-Z0-9_-]", "_");
            File snapFile = new File(dir, "snap_" + safeRule + "_" + timestamp + ".png");

            Mat target = frame.getProcessed() != null ? frame.getProcessed() : frame.getOriginal();
            if (target != null && !target.empty()) {
                boolean ok = ImageUtils.saveMatImage(target, snapFile);
                if (ok) {
                    EventLog.success("Snapshot saved to: " + snapFile.getPath());
                }
            }
        } catch (Exception ex) {
            EventLog.error("Snapshot failed: " + ex.getMessage());
        }
    }
}
