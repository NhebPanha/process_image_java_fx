package com.rupp.trafficvision;

import com.rupp.trafficvision.detection.ImageProcessingVehicleDetector;
import com.rupp.trafficvision.model.DetectionResult;
import com.rupp.trafficvision.model.TrafficLightColor;
import com.rupp.trafficvision.model.VehicleType;
import com.rupp.trafficvision.service.CameraService;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class VehicleDetectionTest {

    private final ImageProcessingVehicleDetector detector = new ImageProcessingVehicleDetector();
    private final CameraService cameraService = new CameraService();

    @Test
    public void testDetectCar() {
        BufferedImage carImage = cameraService.renderSyntheticFrame(VehicleType.CAR, 0.50, TrafficLightColor.RED);
        DetectionResult result = detector.detect(carImage);

        assertNotNull(result);
        assertTrue(result.isDetected());
        assertEquals(VehicleType.CAR, result.getVehicleType());
        assertTrue(result.getConfidence() > 0.80);
        assertNotNull(result.getBoundingBox());
    }

    @Test
    public void testDetectMotorcycle() {
        BufferedImage mcImage = cameraService.renderSyntheticFrame(VehicleType.MOTORCYCLE, 0.50, TrafficLightColor.GREEN);
        DetectionResult result = detector.detect(mcImage);

        assertNotNull(result);
        assertTrue(result.isDetected());
        assertEquals(VehicleType.MOTORCYCLE, result.getVehicleType());
        assertTrue(result.getConfidence() > 0.75);
    }

    @Test
    public void testDetectTricycle() {
        BufferedImage tricycleImage = cameraService.renderSyntheticFrame(VehicleType.TRICYCLE, 0.50, TrafficLightColor.YELLOW);
        DetectionResult result = detector.detect(tricycleImage);

        assertNotNull(result);
        assertTrue(result.isDetected());
        assertEquals(VehicleType.TRICYCLE, result.getVehicleType());
        assertTrue(result.getConfidence() > 0.70);
    }

    @Test
    public void testNullOrEmptyImageSafety() {
        DetectionResult nullResult = detector.detect(null);
        assertNotNull(nullResult);
        assertEquals("FAILED", nullResult.getStatus());

        BufferedImage tinyImage = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        DetectionResult tinyResult = detector.detect(tinyImage);
        assertNotNull(tinyResult);
        assertEquals("FAILED", tinyResult.getStatus());
    }
}
