package com.turbotech.cvstudio.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;

/**
 * Reusable stage preview card displaying stage title, latency badge, resolution metrics,
 * image preview, and optional interactive canvas overlay.
 */
public class StagePanelView extends VBox {

    private final String stageTitle;
    private final boolean hasOverlay;
    private final Label titleLabel;
    private final Label badgeLabel;
    private final Label latencyBadge;
    private final Label resolutionBadge;
    private final ImageView imageView;
    private final DetectionOverlay overlay;
    private final StackPane viewport;
    private final Label emptyPlaceholder;

    private int lastMatWidth = 0;
    private int lastMatHeight = 0;

    public StagePanelView(String stageNumber, String stageTitle, boolean hasOverlay) {
        this.stageTitle = stageTitle;
        this.hasOverlay = hasOverlay;

        getStyleClass().add("stage-card");
        setSpacing(8);
        setPadding(new Insets(10));
        VBox.setVgrow(this, Priority.ALWAYS);

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        badgeLabel = new Label(stageNumber);
        badgeLabel.getStyleClass().addAll("badge", "stage-badge");

        titleLabel = new Label(stageTitle);
        titleLabel.getStyleClass().add("stage-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        resolutionBadge = new Label("—");
        resolutionBadge.getStyleClass().addAll("badge", "badge-subtle");

        latencyBadge = new Label("0 ms");
        latencyBadge.getStyleClass().addAll("badge", "badge-latency");

        header.getChildren().addAll(badgeLabel, titleLabel, spacer, resolutionBadge, latencyBadge);

        // Viewport
        viewport = new StackPane();
        viewport.getStyleClass().add("stage-viewport");
        VBox.setVgrow(viewport, Priority.ALWAYS);

        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        // Bind imageview sizing to viewport
        imageView.fitWidthProperty().bind(viewport.widthProperty().subtract(16));
        imageView.fitHeightProperty().bind(viewport.heightProperty().subtract(16));

        emptyPlaceholder = new Label("Waiting for frame...");
        emptyPlaceholder.getStyleClass().add("placeholder-text");

        if (hasOverlay) {
            overlay = new DetectionOverlay();
            overlay.widthProperty().bind(viewport.widthProperty());
            overlay.heightProperty().bind(viewport.heightProperty());
            viewport.getChildren().addAll(emptyPlaceholder, imageView, overlay);
        } else {
            overlay = null;
            viewport.getChildren().addAll(emptyPlaceholder, imageView);
        }

        getChildren().addAll(header, viewport);

        // Redraw overlay on resize if active
        viewport.widthProperty().addListener((obs, o, n) -> redrawOverlay());
        viewport.heightProperty().addListener((obs, o, n) -> redrawOverlay());
    }

    public void update(WritableImage image, int origWidth, int origHeight) {
        if (image == null) {
            imageView.setImage(null);
            emptyPlaceholder.setVisible(true);
            resolutionBadge.setText("—");
            if (overlay != null) overlay.clear();
            return;
        }

        this.lastMatWidth = origWidth;
        this.lastMatHeight = origHeight;

        emptyPlaceholder.setVisible(false);
        imageView.setImage(image);
        resolutionBadge.setText(origWidth + "x" + origHeight);
    }

    public void setLatency(long ms) {
        latencyBadge.setText(ms + " ms");
        if (ms > 80) {
            latencyBadge.setStyle("-fx-background-color: rgba(239, 83, 80, 0.25); -fx-text-fill: #ef5350;");
        } else if (ms > 35) {
            latencyBadge.setStyle("-fx-background-color: rgba(255, 167, 38, 0.25); -fx-text-fill: #ffa726;");
        } else {
            latencyBadge.setStyle("-fx-background-color: rgba(76, 175, 80, 0.25); -fx-text-fill: #66bb6a;");
        }
    }

    public DetectionOverlay getOverlay() {
        return overlay;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void redrawOverlay() {
        if (overlay != null && imageView.getImage() != null && lastMatWidth > 0 && lastMatHeight > 0) {
            // Compute current rendered bounds of the image inside viewport
            double vpW = viewport.getWidth();
            double vpH = viewport.getHeight();
            double imgRatio = (double) lastMatWidth / lastMatHeight;
            double vpRatio = vpW / vpH;

            double renderedW, renderedH, offsetX, offsetY;
            if (vpRatio > imgRatio) {
                renderedH = vpH - 16;
                renderedW = renderedH * imgRatio;
            } else {
                renderedW = vpW - 16;
                renderedH = renderedW / imgRatio;
            }
            offsetX = (vpW - renderedW) / 2.0;
            offsetY = (vpH - renderedH) / 2.0;

            overlay.setTranslateX(offsetX);
            overlay.setTranslateY(offsetY);
        }
    }
}
