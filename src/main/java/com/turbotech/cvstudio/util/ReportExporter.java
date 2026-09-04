package com.turbotech.cvstudio.util;

import com.turbotech.cvstudio.core.ImageUtils;
import com.turbotech.cvstudio.model.Detection;
import com.turbotech.cvstudio.model.Frame;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Generates inspection report files including CSV metrics and annotated snapshot images.
 */
public final class ReportExporter {

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private ReportExporter() {}

    /**
     * Exports frame detections, rules, and decision outcomes to a CSV file.
     */
    public static void exportCsv(Frame frame, File csvFile) throws Exception {
        if (csvFile == null) return;

        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            writer.println("Timestamp,Stage1_Width,Stage1_Height,Total_Latency_ms,Decision,Detection_Index,Label,Confidence,Box_X,Box_Y,Box_W,Box_H");

            String timeStr = ISO_FMT.format(Instant.ofEpochMilli(frame.getTimestampMs()));
            int w = (frame.getOriginal() != null) ? frame.getOriginal().cols() : 0;
            int h = (frame.getOriginal() != null) ? frame.getOriginal().rows() : 0;
            long latency = frame.getTotalLatencyMs();
            String decision = frame.getDecision() != null ? frame.getDecision().replace(",", ";") : "None";

            if (frame.getDetections().isEmpty()) {
                writer.printf("%s,%d,%d,%d,\"%s\",0,None,0.00,0,0,0,0%n",
                        timeStr, w, h, latency, decision);
            } else {
                int idx = 1;
                for (Detection d : frame.getDetections()) {
                    writer.printf("%s,%d,%d,%d,\"%s\",%d,%s,%.4f,%d,%d,%d,%d%n",
                            timeStr, w, h, latency, decision,
                            idx++, d.label(), d.confidence(), d.x(), d.y(), d.w(), d.h());
                }
            }
        }
    }

    /**
     * Saves an annotated image with bounding boxes drawn onto the original or processed Mat.
     */
    public static void exportAnnotatedImage(Frame frame, File imageFile) {
        if (frame == null || imageFile == null) return;

        Mat base = frame.getProcessed() != null ? frame.getProcessed() : frame.getOriginal();
        if (base == null || base.empty()) return;

        Mat annotated = base.clone();
        if (annotated.channels() == 1) {
            Mat bgr = new Mat();
            Imgproc.cvtColor(annotated, bgr, Imgproc.COLOR_GRAY2BGR);
            annotated.release();
            annotated = bgr;
        }

        for (Detection d : frame.getDetections()) {
            Point p1 = new Point(d.x(), d.y());
            Point p2 = new Point(d.x() + d.w(), d.y() + d.h());
            Imgproc.rectangle(annotated, p1, p2, new Scalar(0, 230, 255), 2);

            String label = String.format("%s (%.0f%%)", d.label(), d.confidence() * 100);
            Point textPoint = new Point(d.x(), Math.max(20, d.y() - 6));
            Imgproc.putText(annotated, label, textPoint, Imgproc.FONT_HERSHEY_SIMPLEX, 0.55, new Scalar(0, 230, 255), 2);
        }

        ImageUtils.saveMatImage(annotated, imageFile);
        annotated.release();
    }
}
