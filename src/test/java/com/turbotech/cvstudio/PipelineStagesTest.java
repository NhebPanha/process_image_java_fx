package com.turbotech.cvstudio;

import com.turbotech.cvstudio.core.NativeLoader;
import com.turbotech.cvstudio.core.SampleImages;
import com.turbotech.cvstudio.model.DecisionRule;
import com.turbotech.cvstudio.model.Detection;
import com.turbotech.cvstudio.model.Frame;
import com.turbotech.cvstudio.model.PipelineSettings;
import com.turbotech.cvstudio.pipeline.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless unit tests for OpenCV pipeline stages and rule processing.
 */
public class PipelineStagesTest {

    @BeforeAll
    static void initOpenCV() {
        NativeLoader.load();
        assertTrue(NativeLoader.isLoaded(), "OpenCV native binaries must be extracted and loaded.");
    }

    @Test
    void testAcquisitionStage() {
        AcquisitionStage stage = new AcquisitionStage();
        PipelineSettings settings = new PipelineSettings();

        Mat mat = new Mat(100, 100, CvType.CV_8UC3, new Scalar(10, 20, 30));
        Frame frame = new Frame(mat);

        Frame out = stage.apply(frame, settings);
        assertNotNull(out.getOriginal());
        assertEquals(100, out.getOriginal().cols());
        assertEquals(100, out.getOriginal().rows());

        out.release();
    }

    @Test
    void testPreprocessStage() {
        PreprocessStage stage = new PreprocessStage();
        PipelineSettings settings = new PipelineSettings();

        Mat mat = new Mat(200, 200, CvType.CV_8UC3, new Scalar(100, 100, 100));
        Frame frame = new Frame(mat);

        // Test default pass-through
        Frame out = stage.apply(frame, settings);
        assertNotNull(out.getProcessed());
        assertEquals(3, out.getProcessed().channels());

        // Test grayscale conversion
        settings.setGrayscale(true);
        out = stage.apply(out, settings);
        assertEquals(1, out.getProcessed().channels(), "Processed Mat should now be single-channel grayscale.");

        // Test CLAHE
        settings.setClaheEnabled(true);
        settings.setClaheClipLimit(3.0);
        out = stage.apply(out, settings);
        assertEquals(1, out.getProcessed().channels());

        out.release();
    }

    @Test
    void testFeatureExtractionCannyEdges() {
        FeatureExtractionStage stage = new FeatureExtractionStage();
        PipelineSettings settings = new PipelineSettings();
        settings.setFeatureMode(PipelineSettings.FeatureMode.EDGES);
        settings.setCannyLow(50);
        settings.setCannyHigh(150);

        Mat sample = SampleImages.createTrafficSample();
        Frame frame = new Frame(sample);
        frame.setProcessed(sample.clone());

        Frame out = stage.apply(frame, settings);
        assertNotNull(out.getFeatures());
        assertNotNull(out.getFeatures().getEdges());
        assertNotNull(out.getFeatureView());
        assertEquals(3, out.getFeatureView().channels(), "Feature view should be converted to BGR for display.");

        out.release();
    }

    @Test
    void testFeatureExtractionContours() {
        FeatureExtractionStage stage = new FeatureExtractionStage();
        PipelineSettings settings = new PipelineSettings();
        settings.setFeatureMode(PipelineSettings.FeatureMode.CONTOURS);
        settings.setMinContourArea(50);

        Mat sample = SampleImages.createObjectsSample();
        Frame frame = new Frame(sample);
        frame.setProcessed(sample.clone());

        Frame out = stage.apply(frame, settings);
        assertNotNull(out.getFeatures());
        assertTrue(out.getFeatures().getContourCount() >= 0);
        assertNotNull(out.getFeatureView());

        out.release();
    }

    @Test
    void testFeatureExtractionOrbKeypoints() {
        FeatureExtractionStage stage = new FeatureExtractionStage();
        PipelineSettings settings = new PipelineSettings();
        settings.setFeatureMode(PipelineSettings.FeatureMode.KEYPOINTS);

        Mat sample = SampleImages.createOfficeSample();
        Frame frame = new Frame(sample);
        frame.setProcessed(sample.clone());

        Frame out = stage.apply(frame, settings);
        assertNotNull(out.getFeatures());
        assertNotNull(out.getFeatures().getKeypoints());
        assertNotNull(out.getFeatureView());

        out.release();
    }

    @Test
    void testFeatureExtractionHistogram() {
        FeatureExtractionStage stage = new FeatureExtractionStage();
        PipelineSettings settings = new PipelineSettings();
        settings.setFeatureMode(PipelineSettings.FeatureMode.HISTOGRAM);

        Mat sample = SampleImages.createPedestrianSample();
        Frame frame = new Frame(sample);
        frame.setProcessed(sample.clone());

        Frame out = stage.apply(frame, settings);
        assertNotNull(out.getFeatureView());
        assertEquals(3, out.getFeatureView().channels());

        out.release();
    }

    @Test
    void testDecisionStage() {
        DecisionStage stage = new DecisionStage();
        PipelineSettings settings = new PipelineSettings();
        settings.getRules().clear();

        // Add test rules
        settings.getRules().add(new DecisionRule("Person Alert", "person", 1, 0.50, DecisionRule.Action.ALERT));
        settings.getRules().add(new DecisionRule("Car Counter", "car", 2, 0.40, DecisionRule.Action.LOG));

        Mat mat = new Mat(100, 100, CvType.CV_8UC3, new Scalar(0, 0, 0));
        Frame frame = new Frame(mat);

        // Feed detections
        frame.setDetections(List.of(
                new Detection("person", 0.85, 10, 10, 40, 80),
                new Detection("car", 0.70, 50, 50, 60, 40),
                new Detection("car", 0.65, 120, 50, 60, 40)
        ));

        Frame out = stage.apply(frame, settings);
        assertNotNull(out.getDecision());
        assertTrue(out.getDecision().contains("Person Alert"), "Person alert rule should fire.");
        assertTrue(out.getDecision().contains("Car Counter"), "Car counter rule should fire.");

        out.release();
    }
}
