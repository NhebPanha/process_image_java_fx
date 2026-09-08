package com.rupp.trafficvision.model;

/**
 * Represents a rectangular bounding box enclosing a detected vehicle in pixel coordinates.
 */
public class BoundingBox {
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    /**
     * Constructs a new BoundingBox.
     *
     * @param x      the top-left X coordinate
     * @param y      the top-left Y coordinate
     * @param width  width of the bounding region
     * @param height height of the bounding region
     */
    public BoundingBox(int x, int y, int width, int height) {
        this.x = Math.max(0, x);
        this.y = Math.max(0, y);
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getArea() {
        return width * height;
    }

    public double getAspectRatio() {
        return (double) width / (double) height;
    }

    public int getCenterX() {
        return x + width / 2;
    }

    public int getCenterY() {
        return y + height / 2;
    }

    /**
     * Scales bounding box coordinates relative to original and displayed dimensions.
     *
     * @param scaleX horizontal scale ratio
     * @param scaleY vertical scale ratio
     * @return scaled BoundingBox instance
     */
    public BoundingBox scale(double scaleX, double scaleY) {
        return new BoundingBox(
                (int) Math.round(x * scaleX),
                (int) Math.round(y * scaleY),
                (int) Math.round(width * scaleX),
                (int) Math.round(height * scaleY)
        );
    }

    @Override
    public String toString() {
        return String.format("[ x=%d y=%d w=%d h=%d ]", x, y, width, height);
    }
}
