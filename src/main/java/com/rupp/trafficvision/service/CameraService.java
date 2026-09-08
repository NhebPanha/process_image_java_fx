package com.rupp.trafficvision.service;

import com.rupp.trafficvision.model.TrafficLightColor;
import com.rupp.trafficvision.model.VehicleType;
import com.rupp.trafficvision.util.LoggerUtil;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Traffic camera integration and simulation service.
 * Emulates a real-time live traffic monitoring camera feed with approaching vehicles,
 * timestamp HUD overlays, FPS counter, and instant snapshot capture.
 */
public class CameraService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private final String cameraId;
    private ScheduledExecutorService cameraScheduler;
    private Consumer<BufferedImage> frameListener;
    private volatile boolean isRunning = false;
    private volatile BufferedImage latestFrame;

    // Simulation animation state
    private double vehicleProgress = 0.0;
    private VehicleType currentSimulatedType = VehicleType.CAR;
    private TrafficLightColor lightColor = TrafficLightColor.RED;
    private int frameCounter = 0;
    private long lastFpsCheck = System.currentTimeMillis();
    private double currentFps = 20.0;

    public CameraService(String cameraId) {
        this.cameraId = cameraId != null ? cameraId : "CAM-01";
        this.latestFrame = renderSyntheticFrame(currentSimulatedType, 0.4, TrafficLightColor.RED);
    }

    public CameraService() {
        this("CAM-01");
    }

    public synchronized void start(Consumer<BufferedImage> listener) {
        this.frameListener = listener;
        if (isRunning) return;

        isRunning = true;
        cameraScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Camera-Sim-Thread");
            t.setDaemon(true);
            return t;
        });

        cameraScheduler.scheduleAtFixedRate(this::tickCamera, 0, 50, TimeUnit.MILLISECONDS); // ~20 FPS
        LoggerUtil.info("Traffic Camera " + cameraId + " started.");
    }

    public synchronized void stop() {
        if (!isRunning) return;
        isRunning = false;
        if (cameraScheduler != null) {
            cameraScheduler.shutdownNow();
            cameraScheduler = null;
        }
        LoggerUtil.info("Traffic Camera " + cameraId + " stopped.");
    }

    public boolean isRunning() {
        return isRunning;
    }

    public String getCameraId() {
        return cameraId;
    }

    public double getCurrentFps() {
        return currentFps;
    }

    public void setSimulatedVehicle(VehicleType type) {
        if (type != null) {
            this.currentSimulatedType = type;
            this.vehicleProgress = 0.05;
        }
    }

    public void setTrafficLightColor(TrafficLightColor color) {
        if (color != null) {
            this.lightColor = color;
        }
    }

    /**
     * Captures the current camera frame snapshot ("Take Picture").
     *
     * @return current BufferedImage snapshot
     */
    public BufferedImage captureSnapshot() {
        return latestFrame != null ? latestFrame : renderSyntheticFrame(currentSimulatedType, 0.45, lightColor);
    }

    private void tickCamera() {
        try {
            // Animate vehicle approaching intersection
            vehicleProgress += 0.015;
            if (vehicleProgress > 0.85) {
                vehicleProgress = 0.10;
                cycleNextVehicle();
            }

            // Calculate live FPS
            frameCounter++;
            long now = System.currentTimeMillis();
            if (now - lastFpsCheck >= 1000) {
                currentFps = (frameCounter * 1000.0) / (now - lastFpsCheck);
                frameCounter = 0;
                lastFpsCheck = now;
            }

            latestFrame = renderSyntheticFrame(currentSimulatedType, vehicleProgress, lightColor);

            if (frameListener != null && isRunning) {
                frameListener.accept(latestFrame);
            }
        } catch (Exception e) {
            LoggerUtil.error("Camera simulation tick error: " + e.getMessage());
        }
    }

    private void cycleNextVehicle() {
        VehicleType[] types = {VehicleType.CAR, VehicleType.MOTORCYCLE, VehicleType.BICYCLE, VehicleType.TRICYCLE, VehicleType.MOPED};
        int nextIdx = (currentSimulatedType.ordinal() + 1) % types.length;
        currentSimulatedType = types[nextIdx];
    }

    /**
     * Renders a synthetic traffic scene frame with road, traffic light, vehicle, and HUD.
     */
    public BufferedImage renderSyntheticFrame(VehicleType type, double progress, TrafficLightColor color) {
        int width = 720;
        int height = 480;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. Sky & Background Environment
        g.setColor(new Color(15, 23, 42)); // Deep night sky
        g.fillRect(0, 0, width, height);

        // Horizon buildings silhouette
        g.setColor(new Color(30, 41, 59));
        g.fillRect(30, 100, 70, 120);
        g.fillRect(120, 70, 80, 150);
        g.fillRect(220, 120, 60, 100);
        g.fillRect(480, 80, 90, 140);
        g.fillRect(590, 110, 80, 110);

        // 2. Asphalt Road with Perspective
        Polygon road = new Polygon();
        road.addPoint((int) (width * 0.35), (int) (height * 0.42));
        road.addPoint((int) (width * 0.65), (int) (height * 0.42));
        road.addPoint(width + 80, height);
        road.addPoint(-80, height);
        g.setColor(new Color(24, 30, 42));
        g.fillPolygon(road);

        // Road lane markings
        g.setColor(new Color(245, 158, 11, 200));
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{20, 20}, 0));
        g.drawLine(width / 2, (int) (height * 0.42), width / 2, height);

        // Stop line
        g.setColor(new Color(255, 255, 255, 220));
        g.setStroke(new BasicStroke(6));
        g.drawLine((int) (width * 0.15), (int) (height * 0.88), (int) (width * 0.85), (int) (height * 0.88));

        // 3. Traffic Light Pole & Signal Box on right side
        int poleX = (int) (width * 0.82);
        int poleY = (int) (height * 0.18);
        g.setColor(new Color(75, 85, 99));
        g.fillRect(poleX + 16, poleY + 110, 8, height - (poleY + 110)); // Pole

        // Signal Housing
        g.setColor(new Color(17, 24, 39));
        g.fillRoundRect(poleX, poleY, 40, 110, 12, 12);
        g.setColor(new Color(107, 114, 128));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(poleX, poleY, 40, 110, 12, 12);

        // Red Light
        g.setColor(color == TrafficLightColor.RED ? new Color(239, 68, 68) : new Color(69, 10, 10));
        g.fillOval(poleX + 8, poleY + 8, 24, 24);
        if (color == TrafficLightColor.RED) {
            g.setColor(new Color(239, 68, 68, 90));
            g.fillOval(poleX + 4, poleY + 4, 32, 32);
        }

        // Yellow Light
        g.setColor(color == TrafficLightColor.YELLOW ? new Color(245, 158, 11) : new Color(69, 39, 10));
        g.fillOval(poleX + 8, poleY + 42, 24, 24);
        if (color == TrafficLightColor.YELLOW) {
            g.setColor(new Color(245, 158, 11, 90));
            g.fillOval(poleX + 4, poleY + 38, 32, 32);
        }

        // Green Light
        g.setColor(color == TrafficLightColor.GREEN ? new Color(16, 185, 129) : new Color(6, 49, 33));
        g.fillOval(poleX + 8, poleY + 76, 24, 24);
        if (color == TrafficLightColor.GREEN) {
            g.setColor(new Color(16, 185, 129, 90));
            g.fillOval(poleX + 4, poleY + 72, 32, 32);
        }

        // 4. Render Approaching Vehicle
        renderVehicleOnRoad(g, type, progress, width, height);

        // 5. Traffic Camera HUD Overlay
        renderCameraHUD(g, width, height);

        g.dispose();
        return img;
    }

    private void renderVehicleOnRoad(Graphics2D g, VehicleType type, double progress, int w, int h) {
        // Perspective scaling: as vehicle approaches (progress 0.1 -> 0.8), it grows larger
        double scale = 0.35 + progress * 0.95;
        int centerY = (int) (h * 0.44 + progress * (h * 0.44));
        int centerX = (int) (w * 0.50 + (progress - 0.5) * (w * 0.12));

        int baseW = switch (type) {
            case CAR -> 220;
            case TRICYCLE -> 140;
            case MOTORCYCLE -> 90;
            case MOPED -> 85;
            case BICYCLE -> 80;
            default -> 160;
        };
        int baseH = switch (type) {
            case CAR -> 130;
            case TRICYCLE -> 145;
            case MOTORCYCLE -> 125;
            case MOPED -> 115;
            case BICYCLE -> 120;
            default -> 120;
        };

        int curW = (int) (baseW * scale);
        int curH = (int) (baseH * scale);
        int vx = centerX - curW / 2;
        int vy = centerY - curH;

        // Draw Vehicle Shadow
        g.setColor(new Color(10, 15, 25, 180));
        g.fillOval(vx, vy + curH - (int) (18 * scale), curW, (int) (22 * scale));

        switch (type) {
            case CAR -> {
                // Car Body
                g.setColor(new Color(37, 99, 235)); // Blue body
                g.fillRoundRect(vx, vy + (int) (curH * 0.45), curW, (int) (curH * 0.42), (int) (16 * scale), (int) (16 * scale));
                // Cabin & Windshield
                g.setColor(new Color(29, 78, 216));
                int cabinInset = (int) (curW * 0.18);
                g.fillRoundRect(vx + cabinInset, vy + (int) (curH * 0.12), curW - 2 * cabinInset, (int) (curH * 0.40), (int) (14 * scale), (int) (14 * scale));
                g.setColor(new Color(191, 219, 254, 220)); // Glass
                g.fillRoundRect(vx + cabinInset + 6, vy + (int) (curH * 0.16), curW - 2 * cabinInset - 12, (int) (curH * 0.26), 8, 8);
                // Headlights
                g.setColor(new Color(254, 240, 138));
                g.fillOval(vx + (int) (curW * 0.08), vy + (int) (curH * 0.55), (int) (16 * scale), (int) (16 * scale));
                g.fillOval(vx + curW - (int) (curW * 0.08) - (int) (16 * scale), vy + (int) (curH * 0.55), (int) (16 * scale), (int) (16 * scale));
                // Grille
                g.setColor(new Color(15, 23, 42));
                g.fillRoundRect(vx + (int) (curW * 0.32), vy + (int) (curH * 0.60), (int) (curW * 0.36), (int) (curH * 0.18), 4, 4);
                // Wheels
                g.setColor(new Color(17, 24, 39));
                g.fillOval(vx + (int) (curW * 0.05), vy + (int) (curH * 0.70), (int) (curW * 0.22), (int) (curH * 0.28));
                g.fillOval(vx + curW - (int) (curW * 0.27), vy + (int) (curH * 0.70), (int) (curW * 0.22), (int) (curH * 0.28));
            }
            case TRICYCLE -> {
                // Tuk-Tuk / Auto-Rickshaw
                g.setColor(new Color(219, 39, 119)); // Vibrant Magenta
                g.fillRoundRect(vx, vy + (int) (curH * 0.30), curW, (int) (curH * 0.55), (int) (14 * scale), (int) (14 * scale));
                // Yellow Canopy Roof
                g.setColor(new Color(245, 158, 11));
                g.fillRoundRect(vx - 2, vy + (int) (curH * 0.08), curW + 4, (int) (curH * 0.25), (int) (12 * scale), (int) (12 * scale));
                // Windshield
                g.setColor(new Color(224, 242, 254, 230));
                g.fillRoundRect(vx + (int) (curW * 0.15), vy + (int) (curH * 0.32), (int) (curW * 0.70), (int) (curH * 0.24), 6, 6);
                // Center Single Headlight
                g.setColor(new Color(254, 240, 138));
                g.fillOval(vx + curW / 2 - (int) (10 * scale), vy + (int) (curH * 0.62), (int) (20 * scale), (int) (20 * scale));
                // Wheels
                g.setColor(new Color(17, 24, 39));
                g.fillOval(vx + (int) (curW * 0.06), vy + (int) (curH * 0.72), (int) (curW * 0.24), (int) (curH * 0.26));
                g.fillOval(vx + curW - (int) (curW * 0.30), vy + (int) (curH * 0.72), (int) (curW * 0.24), (int) (curH * 0.26));
            }
            case MOTORCYCLE -> {
                // Slender rider & sporty chassis
                g.setColor(new Color(16, 185, 129)); // Emerald body
                g.fillRect(vx + (int) (curW * 0.35), vy + (int) (curH * 0.40), (int) (curW * 0.30), (int) (curH * 0.35));
                // Rider helmet & torso
                g.setColor(new Color(30, 41, 59));
                g.fillOval(vx + curW / 2 - (int) (16 * scale), vy + (int) (curH * 0.06), (int) (32 * scale), (int) (32 * scale));
                g.fillRect(vx + curW / 2 - (int) (20 * scale), vy + (int) (curH * 0.24), (int) (40 * scale), (int) (30 * scale));
                // Handlebars
                g.setColor(new Color(209, 213, 219));
                g.setStroke(new BasicStroke((float) (4 * scale)));
                g.drawLine(vx + (int) (curW * 0.15), vy + (int) (curH * 0.35), vx + (int) (curW * 0.85), vy + (int) (curH * 0.35));
                // Headlamp
                g.setColor(new Color(254, 240, 138));
                g.fillOval(vx + curW / 2 - (int) (12 * scale), vy + (int) (curH * 0.42), (int) (24 * scale), (int) (24 * scale));
                // Front wheel
                g.setColor(new Color(17, 24, 39));
                g.fillOval(vx + curW / 2 - (int) (16 * scale), vy + (int) (curH * 0.65), (int) (32 * scale), (int) (curH * 0.34));
            }
            case MOPED -> {
                // Scooter step-through frame
                g.setColor(new Color(139, 92, 246)); // Purple scooter
                g.fillRoundRect(vx + (int) (curW * 0.25), vy + (int) (curH * 0.45), (int) (curW * 0.50), (int) (curH * 0.35), 8, 8);
                // Rider helmet
                g.setColor(new Color(30, 41, 59));
                g.fillOval(vx + curW / 2 - (int) (15 * scale), vy + (int) (curH * 0.10), (int) (30 * scale), (int) (30 * scale));
                // Cute round headlight
                g.setColor(new Color(254, 240, 138));
                g.fillOval(vx + curW / 2 - (int) (11 * scale), vy + (int) (curH * 0.46), (int) (22 * scale), (int) (22 * scale));
                // Wheels
                g.setColor(new Color(17, 24, 39));
                g.fillOval(vx + curW / 2 - (int) (14 * scale), vy + (int) (curH * 0.70), (int) (28 * scale), (int) (curH * 0.28));
            }
            case BICYCLE -> {
                // Thin frame geometry & cyclist
                g.setColor(new Color(245, 158, 11)); // Amber accents
                g.setStroke(new BasicStroke((float) (3 * scale)));
                // Frame triangles
                int bx = vx + curW / 2;
                int by = vy + (int) (curH * 0.55);
                g.drawLine(bx, by, bx - (int) (25 * scale), by + (int) (35 * scale));
                g.drawLine(bx, by, bx + (int) (25 * scale), by + (int) (35 * scale));
                // Rider
                g.setColor(new Color(55, 65, 81));
                g.fillOval(bx - (int) (14 * scale), vy + (int) (curH * 0.12), (int) (28 * scale), (int) (28 * scale));
                // Wheels (thin spoke rims)
                g.setColor(new Color(17, 24, 39));
                g.drawOval(bx - (int) (18 * scale), vy + (int) (curH * 0.65), (int) (36 * scale), (int) (curH * 0.32));
            }
            default -> {}
        }
    }

    private void renderCameraHUD(Graphics2D g, int w, int h) {
        // Top HUD banner
        g.setColor(new Color(15, 23, 42, 190));
        g.fillRect(0, 0, w, 36);

        g.setFont(new Font("Consolas", Font.BOLD, 13));

        // Camera ID & LIVE Indicator
        g.setColor(new Color(239, 68, 68)); // Red glowing dot
        g.fillOval(14, 12, 10, 10);
        g.setColor(Color.WHITE);
        g.drawString("LIVE ●  CAMERA: " + cameraId + " [INTERSECTION NORTH]", 32, 23);

        // Live Timestamp & FPS
        String timeStr = LocalDateTime.now().format(TIME_FMT);
        String fpsStr = String.format("FPS: %.1f", currentFps);
        g.setColor(new Color(16, 185, 129));
        g.drawString(fpsStr + " | " + timeStr, w - 320, 23);

        // Bottom HUD banner
        g.setColor(new Color(15, 23, 42, 190));
        g.fillRect(0, h - 28, w, 28);
        g.setColor(new Color(203, 213, 225));
        g.drawString("Traffic Light: " + lightColor.name() + " | Approaching: " + currentSimulatedType.name(), 14, h - 10);
        g.setColor(new Color(6, 182, 212));
        g.drawString("SYSTEM ACTIVE [AUTO RECOGNITION READY]", w - 310, h - 10);
    }
}
