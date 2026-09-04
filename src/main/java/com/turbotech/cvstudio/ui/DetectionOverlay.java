package com.turbotech.cvstudio.ui;

import com.turbotech.cvstudio.model.Detection;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transparent JavaFX Canvas overlay rendered atop the Recognition Stage view.
 * Renders color-coded bounding boxes, labels, confidence percentages, and coordinates.
 */
public class DetectionOverlay extends Canvas {

    private static final Map<String, Color> COLOR_MAP = new HashMap<>();
    private static final Color DEFAULT_COLOR = Color.web("#00e5ff");
    private static final Font LABEL_FONT = Font.font("System", FontWeight.BOLD, 12);

    static {
        COLOR_MAP.put("person", Color.web("#ff5252"));
        COLOR_MAP.put("car", Color.web("#448aff"));
        COLOR_MAP.put("bicycle", Color.web("#69f0ae"));
        COLOR_MAP.put("dog", Color.web("#ffd740"));
        COLOR_MAP.put("cat", Color.web("#ff4081"));
        COLOR_MAP.put("bottle", Color.web("#7c4dff"));
        COLOR_MAP.put("chair", Color.web("#00e676"));
        COLOR_MAP.put("laptop", Color.web("#ffab40"));
        COLOR_MAP.put("cell phone", Color.web("#18ffff"));
    }

    public DetectionOverlay() {
        setMouseTransparent(true);
    }

    public static Color colorFor(String label) {
        if (label == null) return DEFAULT_COLOR;
        return COLOR_MAP.getOrDefault(label.toLowerCase(), DEFAULT_COLOR);
    }

    /**
     * Clears and redraws detection bounding boxes and labels according to the scale
     * between native Mat dimensions and current displayed ImageView dimensions.
     */
    public void render(List<Detection> detections, double displayWidth, double displayHeight, int matWidth, int matHeight) {
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, getWidth(), getHeight());

        if (detections == null || detections.isEmpty() || matWidth <= 0 || matHeight <= 0) {
            return;
        }

        double scaleX = displayWidth / matWidth;
        double scaleY = displayHeight / matHeight;

        g.setFont(LABEL_FONT);

        for (Detection d : detections) {
            double x = d.x() * scaleX;
            double y = d.y() * scaleY;
            double w = d.w() * scaleX;
            double h = d.h() * scaleY;

            Color boxColor = colorFor(d.label());

            // 1. Draw bounding box rectangle
            g.setLineWidth(2.5);
            g.setStroke(boxColor);
            g.strokeRoundRect(x, y, w, h, 6, 6);

            // 2. Format label pill
            String text = String.format("%s %.0f%%", d.label(), d.confidence() * 100);
            double pillWidth = text.length() * 7.5 + 14;
            double pillHeight = 20;
            double pillY = (y >= pillHeight + 4) ? (y - pillHeight - 2) : (y + 2);

            // Semi-transparent pill background
            g.setFill(Color.color(boxColor.getRed(), boxColor.getGreen(), boxColor.getBlue(), 0.85));
            g.fillRoundRect(x, pillY, pillWidth, pillHeight, 4, 4);

            // Text
            g.setFill(Color.WHITE);
            g.fillText(text, x + 6, pillY + 14);
        }
    }

    public void clear() {
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, getWidth(), getHeight());
    }
}
