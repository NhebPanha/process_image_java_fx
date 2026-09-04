package com.nhebpanha.cvstudio.core;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

/**
 * Generates synthetic, rich OpenCV test scenes representing typical vision workflows
 * (traffic, pedestrians, office desk, objects) for instant evaluation without requiring external files.
 */
public final class SampleImages {

    private SampleImages() {}

    public static Mat createSample(String name) {
        if (name == null) return createTrafficSample();
        if (name.contains("Traffic") || name.contains("Vehicle")) {
            return createTrafficSample();
        } else if (name.contains("Pedestrian") || name.contains("City")) {
            return createPedestrianSample();
        } else if (name.contains("Office") || name.contains("Desk")) {
            return createOfficeSample();
        } else {
            return createObjectsSample();
        }
    }

    /**
     * Creates a synthetic highway/traffic scene with vehicles and lane markings.
     */
    public static Mat createTrafficSample() {
        int w = 960, h = 540;
        Mat img = new Mat(h, w, CvType.CV_8UC3, new Scalar(160, 190, 220)); // Sky

        // Ground / asphalt road
        Imgproc.rectangle(img, new Point(0, 200), new Point(w, h), new Scalar(55, 55, 60), -1);

        // Lane markings
        for (int y = 240; y < h; y += 70) {
            Imgproc.rectangle(img, new Point(w / 2.0 - 6, y), new Point(w / 2.0 + 6, y + 40), new Scalar(220, 220, 230), -1);
            Imgproc.rectangle(img, new Point(w / 4.0 - 5, y), new Point(w / 4.0 + 5, y + 35), new Scalar(220, 220, 230), -1);
            Imgproc.rectangle(img, new Point(w * 0.75 - 5, y), new Point(w * 0.75 + 5, y + 35), new Scalar(220, 220, 230), -1);
        }

        // Vehicle 1: Blue Sedan (car)
        drawVehicle(img, 160, 280, 200, 100, new Scalar(210, 80, 30));

        // Vehicle 2: Red SUV (car)
        drawVehicle(img, 520, 310, 240, 120, new Scalar(40, 40, 220));

        // Vehicle 3: Silver Van (truck/car)
        drawVehicle(img, 360, 220, 150, 75, new Scalar(180, 180, 185));

        return img;
    }

    /**
     * Creates a pedestrian street scene.
     */
    public static Mat createPedestrianSample() {
        int w = 960, h = 540;
        Mat img = new Mat(h, w, CvType.CV_8UC3, new Scalar(180, 200, 215)); // Sky

        // Buildings background
        Imgproc.rectangle(img, new Point(40, 80), new Point(280, 380), new Scalar(110, 115, 130), -1);
        Imgproc.rectangle(img, new Point(320, 40), new Point(620, 380), new Scalar(90, 95, 110), -1);
        Imgproc.rectangle(img, new Point(660, 100), new Point(920, 380), new Scalar(120, 125, 140), -1);

        // Windows
        for (int r = 100; r < 340; r += 45) {
            for (int c = 60; c < 260; c += 40) {
                Imgproc.rectangle(img, new Point(c, r), new Point(c + 22, r + 28), new Scalar(230, 240, 255), -1);
            }
        }

        // Pavement
        Imgproc.rectangle(img, new Point(0, 380), new Point(w, h), new Scalar(140, 145, 150), -1);

        // Pedestrians (person silhouettes)
        drawPedestrian(img, 220, 270, 70, 180, new Scalar(40, 70, 160));
        drawPedestrian(img, 440, 250, 80, 200, new Scalar(30, 140, 40));
        drawPedestrian(img, 720, 290, 65, 165, new Scalar(140, 40, 120));

        return img;
    }

    /**
     * Creates an office desk scene with laptop, bottle, cup, phone.
     */
    public static Mat createOfficeSample() {
        int w = 960, h = 540;
        Mat img = new Mat(h, w, CvType.CV_8UC3, new Scalar(225, 230, 235)); // Wall

        // Wooden desk
        Imgproc.rectangle(img, new Point(0, 240), new Point(w, h), new Scalar(60, 100, 140), -1);

        // Laptop
        drawLaptop(img, 280, 200, 320, 200);

        // Water bottle
        drawBottle(img, 680, 220, 65, 180, new Scalar(210, 140, 40));

        // Coffee cup
        Imgproc.circle(img, new Point(190, 360), 45, new Scalar(240, 240, 245), -1);
        Imgproc.circle(img, new Point(190, 360), 38, new Scalar(35, 50, 75), -1); // coffee

        // Smartphone
        Imgproc.rectangle(img, new Point(780, 320), new Point(850, 420), new Scalar(40, 40, 40), -1);
        Imgproc.rectangle(img, new Point(785, 330), new Point(845, 410), new Scalar(120, 140, 180), -1);

        return img;
    }

    /**
     * Creates an assortment of inspectable objects & bottles on a test surface.
     */
    public static Mat createObjectsSample() {
        int w = 960, h = 540;
        Mat img = new Mat(h, w, CvType.CV_8UC3, new Scalar(215, 220, 225));

        // Surface
        Imgproc.rectangle(img, new Point(0, 280), new Point(w, h), new Scalar(175, 180, 185), -1);

        // Multiple bottles
        drawBottle(img, 120, 180, 70, 210, new Scalar(60, 160, 60));
        drawBottle(img, 280, 160, 75, 230, new Scalar(190, 100, 40));
        drawBottle(img, 450, 200, 60, 190, new Scalar(50, 80, 190));

        // Boxes / items
        Imgproc.rectangle(img, new Point(620, 240), new Point(780, 390), new Scalar(70, 120, 180), -1);
        Imgproc.rectangle(img, new Point(810, 260), new Point(920, 380), new Scalar(120, 70, 140), -1);

        return img;
    }

    private static void drawVehicle(Mat img, int x, int y, int w, int h, Scalar color) {
        // Body
        Imgproc.rectangle(img, new Point(x, y + h * 0.35), new Point(x + w, y + h * 0.85), color, -1);
        // Cabin
        Imgproc.rectangle(img, new Point(x + w * 0.2, y), new Point(x + w * 0.75, y + h * 0.45), color, -1);
        // Windows
        Imgproc.rectangle(img, new Point(x + w * 0.25, y + 6), new Point(x + w * 0.70, y + h * 0.35), new Scalar(230, 235, 240), -1);
        // Wheels
        Imgproc.circle(img, new Point(x + w * 0.25, y + h * 0.85), (int) (h * 0.18), new Scalar(20, 20, 20), -1);
        Imgproc.circle(img, new Point(x + w * 0.75, y + h * 0.85), (int) (h * 0.18), new Scalar(20, 20, 20), -1);
    }

    private static void drawPedestrian(Mat img, int x, int y, int w, int h, Scalar color) {
        // Head
        Imgproc.circle(img, new Point(x + w / 2.0, y + h * 0.15), (int) (w * 0.35), new Scalar(200, 215, 235), -1);
        // Torso
        Imgproc.rectangle(img, new Point(x + w * 0.15, y + h * 0.3), new Point(x + w * 0.85, y + h * 0.68), color, -1);
        // Legs
        Imgproc.rectangle(img, new Point(x + w * 0.2, y + h * 0.68), new Point(x + w * 0.45, y + h), new Scalar(40, 45, 50), -1);
        Imgproc.rectangle(img, new Point(x + w * 0.55, y + h * 0.68), new Point(x + w * 0.8, y + h), new Scalar(40, 45, 50), -1);
    }

    private static void drawLaptop(Mat img, int x, int y, int w, int h) {
        // Screen
        Imgproc.rectangle(img, new Point(x + 20, y), new Point(x + w - 20, y + h * 0.75), new Scalar(30, 30, 35), -1);
        Imgproc.rectangle(img, new Point(x + 26, y + 6), new Point(x + w - 26, y + h * 0.75 - 6), new Scalar(190, 160, 60), -1); // Screen content
        // Base keyboard
        Imgproc.rectangle(img, new Point(x, y + h * 0.75), new Point(x + w, y + h), new Scalar(180, 185, 190), -1);
    }

    private static void drawBottle(Mat img, int x, int y, int w, int h, Scalar color) {
        // Bottle neck
        Imgproc.rectangle(img, new Point(x + w * 0.35, y), new Point(x + w * 0.65, y + h * 0.25), color, -1);
        // Cap
        Imgproc.rectangle(img, new Point(x + w * 0.32, y - 10), new Point(x + w * 0.68, y), new Scalar(240, 240, 240), -1);
        // Body
        Imgproc.rectangle(img, new Point(x, y + h * 0.25), new Point(x + w, y + h), color, -1);
        // Label
        Imgproc.rectangle(img, new Point(x + 4, y + h * 0.45), new Point(x + w - 4, y + h * 0.75), new Scalar(245, 245, 250), -1);
    }
}
