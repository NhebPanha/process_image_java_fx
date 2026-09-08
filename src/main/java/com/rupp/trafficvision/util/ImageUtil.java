package com.rupp.trafficvision.util;

import com.rupp.trafficvision.model.BoundingBox;
import com.rupp.trafficvision.model.DetectionResult;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Image conversion, transformation, and annotated export utilities.
 */
public final class ImageUtil {

    private ImageUtil() {}

    /**
     * Converts a Java AWT {@link BufferedImage} into a JavaFX {@link Image}.
     *
     * @param bufferedImage source AWT image
     * @return JavaFX Image instance
     */
    public static Image toFXImage(BufferedImage bufferedImage) {
        if (bufferedImage == null) return null;
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    /**
     * Converts a JavaFX {@link Image} into a Java AWT {@link BufferedImage}.
     *
     * @param fxImage source JavaFX image
     * @return BufferedImage instance
     */
    public static BufferedImage toBufferedImage(Image fxImage) {
        if (fxImage == null) return null;
        return SwingFXUtils.fromFXImage(fxImage, null);
    }

    /**
     * Resizes an image preserving aspect ratio to fit within specified maximum dimensions.
     *
     * @param src       source image
     * @param targetWidth max width
     * @param targetHeight max height
     * @return resized BufferedImage
     */
    public static BufferedImage resize(BufferedImage src, int targetWidth, int targetHeight) {
        if (src == null) return null;
        double widthRatio = (double) targetWidth / src.getWidth();
        double heightRatio = (double) targetHeight / src.getHeight();
        double ratio = Math.min(widthRatio, heightRatio);

        int newW = Math.max(1, (int) Math.round(src.getWidth() * ratio));
        int newH = Math.max(1, (int) Math.round(src.getHeight() * ratio));

        BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, newW, newH, null);
        g.dispose();
        return resized;
    }

    /**
     * Creates an annotated copy of the source image with the detection bounding box,
     * label badge, confidence score, and timestamp overlay.
     *
     * @param original original image
     * @param result   detection result to annotate
     * @return annotated BufferedImage
     */
    public static BufferedImage createAnnotatedImage(BufferedImage original, DetectionResult result) {
        if (original == null) return null;
        int w = original.getWidth();
        int h = original.getHeight();

        BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(original, 0, 0, null);

        if (result != null && result.isDetected()) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            BoundingBox box = result.getBoundingBox();

            // 1. Draw glowing outer bounding box
            g.setColor(new Color(16, 185, 129, 60)); // Emerald glow
            g.setStroke(new BasicStroke(7));
            g.drawRect(box.getX(), box.getY(), box.getWidth(), box.getHeight());

            // 2. Draw solid inner bounding box
            g.setColor(new Color(16, 185, 129));
            g.setStroke(new BasicStroke(3));
            g.drawRect(box.getX(), box.getY(), box.getWidth(), box.getHeight());

            // 3. Draw Header Label Badge
            String labelText = String.format("%s %s (%.1f%%)",
                    result.getVehicleType().getEmoji(),
                    result.getVehicleType().getDisplayName().toUpperCase(),
                    result.getConfidencePercentage()
            );

            g.setFont(new Font("Segoe UI", Font.BOLD, Math.max(13, w / 48)));
            FontMetrics fm = g.getFontMetrics();
            int badgeW = fm.stringWidth(labelText) + 18;
            int badgeH = fm.getHeight() + 10;
            int badgeX = Math.max(4, box.getX());
            int badgeY = Math.max(badgeH + 4, box.getY() - 4);

            // Badge shadow & fill
            g.setColor(new Color(15, 23, 42, 230));
            g.fillRoundRect(badgeX, badgeY - badgeH, badgeW, badgeH, 8, 8);
            g.setColor(new Color(16, 185, 129));
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(badgeX, badgeY - badgeH, badgeW, badgeH, 8, 8);

            // Text
            g.setColor(Color.WHITE);
            g.drawString(labelText, badgeX + 9, badgeY - 8);

            // 4. Draw Dimension footer badge
            String dimText = String.format("Box: [%d,%d %dx%d] | %d ms",
                    box.getX(), box.getY(), box.getWidth(), box.getHeight(), result.getProcessingTime());
            g.setFont(new Font("Consolas", Font.PLAIN, Math.max(10, w / 65)));
            FontMetrics dfm = g.getFontMetrics();
            int dimW = dfm.stringWidth(dimText) + 12;
            int dimH = dfm.getHeight() + 4;
            int dimY = Math.min(h - 6, box.getY() + box.getHeight() + dimH + 2);

            g.setColor(new Color(15, 23, 42, 210));
            g.fillRoundRect(badgeX, dimY - dimH, dimW, dimH, 6, 6);
            g.setColor(new Color(6, 182, 212));
            g.drawString(dimText, badgeX + 6, dimY - 4);
        }

        g.dispose();
        return copy;
    }

    /**
     * Saves an annotated image to disk in PNG format.
     *
     * @param image       image to save
     * @param destination target file path
     * @throws IOException if saving fails
     */
    public static void saveImage(BufferedImage image, File destination) throws IOException {
        if (image == null || destination == null) {
            throw new IllegalArgumentException("Image or destination cannot be null");
        }
        File parent = destination.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        ImageIO.write(image, "PNG", destination);
        LoggerUtil.info("Image successfully saved to: " + destination.getAbsolutePath());
    }
}
