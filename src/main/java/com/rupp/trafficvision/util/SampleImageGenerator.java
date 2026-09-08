package com.rupp.trafficvision.util;

import com.rupp.trafficvision.model.TrafficLightColor;
import com.rupp.trafficvision.model.VehicleType;
import com.rupp.trafficvision.service.CameraService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Utility to generate and ensure presence of high-fidelity synthetic traffic vehicle sample images.
 */
public final class SampleImageGenerator {

    private SampleImageGenerator() {}

    /**
     * Generates all 5 default vehicle sample images into the specified output directory.
     *
     * @param targetDir directory where sample PNGs should be saved
     */
    public static void generateSamples(File targetDir) {
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        CameraService renderer = new CameraService();
        VehicleType[] types = {
                VehicleType.CAR,
                VehicleType.MOTORCYCLE,
                VehicleType.BICYCLE,
                VehicleType.TRICYCLE,
                VehicleType.MOPED
        };

        for (VehicleType type : types) {
            String filename = "sample_" + type.name().toLowerCase() + ".png";
            File dest = new File(targetDir, filename);
            TrafficLightColor color = switch (type) {
                case CAR -> TrafficLightColor.RED;
                case MOTORCYCLE -> TrafficLightColor.GREEN;
                case BICYCLE -> TrafficLightColor.YELLOW;
                case TRICYCLE -> TrafficLightColor.RED;
                case MOPED -> TrafficLightColor.GREEN;
                default -> TrafficLightColor.RED;
            };

            BufferedImage img = renderer.renderSyntheticFrame(type, 0.48, color);
            try {
                ImageIO.write(img, "PNG", dest);
                LoggerUtil.info("Generated sample image: " + dest.getAbsolutePath());
            } catch (IOException e) {
                LoggerUtil.error("Failed to write sample " + filename + ": " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        File dir = new File("src/main/resources/com/rupp/trafficvision/images");
        generateSamples(dir);
    }
}
