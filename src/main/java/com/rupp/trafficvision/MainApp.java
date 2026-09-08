package com.rupp.trafficvision;

import com.rupp.trafficvision.controller.DashboardController;
import com.rupp.trafficvision.util.Constants;
import com.rupp.trafficvision.util.LoggerUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main application entry point for the Traffic Light Vehicle Image Processing System.
 * Initializes the JavaFX primary stage, loads the FXML dashboard, and manages application lifecycle.
 */
public class MainApp extends Application {

    private DashboardController controller;

    @Override
    public void start(Stage primaryStage) {
        LoggerUtil.info("Starting " + Constants.APP_TITLE + "...");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/rupp/trafficvision/view/DashboardView.fxml"));
            Parent root = loader.load();
            this.controller = loader.getController();

            Scene scene = new Scene(root, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);

            primaryStage.setTitle(Constants.APP_TITLE + " — " + Constants.APP_SUBTITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(Constants.MIN_WINDOW_WIDTH);
            primaryStage.setMinHeight(Constants.MIN_WINDOW_HEIGHT);

            primaryStage.setOnCloseRequest(event -> {
                LoggerUtil.info("Window close requested. Shutting down system...");
                if (controller != null) {
                    controller.cleanup();
                }
            });

            primaryStage.show();
            LoggerUtil.info("Dashboard UI initialized and visible.");
        } catch (IOException e) {
            LoggerUtil.error("Failed to load DashboardView.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        LoggerUtil.info("Application stopping...");
        if (controller != null) {
            controller.cleanup();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
