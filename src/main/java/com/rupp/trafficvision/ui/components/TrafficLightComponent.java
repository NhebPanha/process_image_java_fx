package com.rupp.trafficvision.ui.components;

import com.rupp.trafficvision.model.TrafficLightColor;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Interactive animated JavaFX component visualizing a standard 3-state traffic signal
 * (RED, YELLOW, GREEN) with realistic lamp glow shaders and automatic state progression.
 */
public class TrafficLightComponent extends VBox {

    private final Circle redLamp;
    private final Circle yellowLamp;
    private final Circle greenLamp;
    private final Label stateLabel;

    private TrafficLightColor currentColor = TrafficLightColor.RED;
    private Timeline cycleTimeline;
    private Consumer<TrafficLightColor> onColorChange;

    public TrafficLightComponent() {
        setAlignment(Pos.CENTER);
        setSpacing(8);
        setPadding(new Insets(10, 12, 10, 12));

        // Signal Housing
        VBox housing = new VBox(10);
        housing.setAlignment(Pos.CENTER);
        housing.setPadding(new Insets(12, 10, 12, 10));
        housing.setStyle("""
                -fx-background-color: #111827;
                -fx-background-radius: 24;
                -fx-border-color: #374151;
                -fx-border-width: 2;
                -fx-border-radius: 24;
                -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 4);
                """);

        redLamp = createLamp(Color.web("#EF4444"), Color.web("#450A0A"));
        yellowLamp = createLamp(Color.web("#F59E0B"), Color.web("#451A03"));
        greenLamp = createLamp(Color.web("#10B981"), Color.web("#064E3B"));

        housing.getChildren().addAll(redLamp, yellowLamp, greenLamp);

        stateLabel = new Label("🔴 RED");
        stateLabel.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #EF4444;");

        getChildren().addAll(housing, stateLabel);

        // Click to cycle color manually
        housing.setOnMouseClicked(e -> cycleNext());
        updateVisuals();
    }

    private Circle createLamp(Color active, Color dim) {
        Circle lamp = new Circle(14);
        lamp.setFill(dim);
        lamp.setStroke(Color.web("#1F2937"));
        lamp.setStrokeWidth(2);
        return lamp;
    }

    public TrafficLightColor getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(TrafficLightColor color) {
        if (color != null && this.currentColor != color) {
            this.currentColor = color;
            updateVisuals();
            if (onColorChange != null) {
                onColorChange.accept(this.currentColor);
            }
        }
    }

    public void cycleNext() {
        setCurrentColor(currentColor.next());
    }

    public void setOnColorChange(Consumer<TrafficLightColor> callback) {
        this.onColorChange = callback;
    }

    /**
     * Toggles automatic traffic signal cycling (RED -> GREEN -> YELLOW -> RED).
     */
    public void setAutoCycle(boolean active) {
        if (active) {
            if (cycleTimeline == null) {
                cycleTimeline = new Timeline(new KeyFrame(Duration.seconds(3.5), e -> cycleNext()));
                cycleTimeline.setCycleCount(Animation.INDEFINITE);
            }
            cycleTimeline.play();
        } else {
            if (cycleTimeline != null) {
                cycleTimeline.stop();
            }
        }
    }

    private void updateVisuals() {
        DropShadow redGlow = new DropShadow(18, Color.web("#EF4444"));
        DropShadow yellowGlow = new DropShadow(18, Color.web("#F59E0B"));
        DropShadow greenGlow = new DropShadow(18, Color.web("#10B981"));

        switch (currentColor) {
            case RED -> {
                redLamp.setFill(Color.web("#EF4444"));
                redLamp.setEffect(redGlow);

                yellowLamp.setFill(Color.web("#451A03"));
                yellowLamp.setEffect(null);

                greenLamp.setFill(Color.web("#064E3B"));
                greenLamp.setEffect(null);

                stateLabel.setText("🔴 RED (STOP)");
                stateLabel.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #EF4444;");
            }
            case YELLOW -> {
                redLamp.setFill(Color.web("#450A0A"));
                redLamp.setEffect(null);

                yellowLamp.setFill(Color.web("#F59E0B"));
                yellowLamp.setEffect(yellowGlow);

                greenLamp.setFill(Color.web("#064E3B"));
                greenLamp.setEffect(null);

                stateLabel.setText("🟡 YELLOW (WAIT)");
                stateLabel.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #F59E0B;");
            }
            case GREEN -> {
                redLamp.setFill(Color.web("#450A0A"));
                redLamp.setEffect(null);

                yellowLamp.setFill(Color.web("#451A03"));
                yellowLamp.setEffect(null);

                greenLamp.setFill(Color.web("#10B981"));
                greenLamp.setEffect(greenGlow);

                stateLabel.setText("🟢 GREEN (GO)");
                stateLabel.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #10B981;");
            }
        }
    }
}
