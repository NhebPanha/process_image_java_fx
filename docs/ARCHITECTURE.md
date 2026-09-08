# Traffic Vision: System Architecture Documentation

**Institution:** Royal University of Phnom Penh (RUPP)  
**Department:** Computer Science  
**Course:** Advanced Object-Oriented Programming & Operating Systems  
**Project:** Traffic Light Vehicle Image Processing System  
**Version:** 1.0.0 (Java 21 / JavaFX)

---

## 1. Executive Summary

The **Traffic Light Vehicle Image Processing System** (`traffic-vision`) is an enterprise-grade desktop computer-vision application developed in **Java 21** using **JavaFX**. The system processes live traffic camera video feeds and static intersection photographs to identify, bound, and classify five primary vehicle categories common in urban traffic:
- 🚗 **Car**
- 🏍 **Motorcycle**
- 🚲 **Bicycle**
- 🛺 **Tricycle (Tuk-Tuk / Auto-Rickshaw)**
- 🛵 **Moped**

The application showcases a strict separation of concerns via the **Model-View-Controller (MVC)** architectural pattern, combined with the **Strategy Pattern** for interchangeable image processors and synchronization primitives, and the **Worker Thread Model** for responsive asynchronous computing.

---

## 2. Architectural Design Patterns

```
+-------------------------------------------------------------------------+
|                               VIEW LAYER                                |
|  DashboardView.fxml • dashboard.css • TrafficLightComponent • Charts    |
+------------------------------------+------------------------------------+
                                     | Events & Bindings
                                     v
+-------------------------------------------------------------------------+
|                            CONTROLLER LAYER                             |
|                           DashboardController                           |
+-------------------+---------------------------------+-------------------+
                    |                                 |
         Delegates Tasks                   Observes Data & Events
                    v                                 v
+-----------------------------------+   +---------------------------------+
|           SERVICE LAYER           |   |           MODEL LAYER           |
| • VehicleDetectionService         |   | • Vehicle (Observable Model)    |
| • ImageProcessingService          |   | • DetectionResult               |
| • CameraService (20 FPS Sim)      |   | • BoundingBox                   |
| • StatisticsService               |   | • ProcessingParameters          |
+-------------------+---------------+   | • VehicleType (Enum)            |
                    |                   | • TrafficLightColor (Enum)      |
      Executes Tasks In Background      | • ThreadStatus (Enum)           |
                    v                   +---------------------------------+
+-----------------------------------+
|      CONCURRENCY & SYNC LAYER     |
| • ImageProcessingTask (JavaFX)    |
| • FixedThreadPool (Worker-1..4)   |
| • ProcessingLock (ReentrantLock)  |
| • ProcessingSemaphore (Semaphore) |
| • StatisticsManager (Thread-Safe) |
+-----------------------------------+
```

### 2.1 Model-View-Controller (MVC)
- **Model (`com.rupp.trafficvision.model`)**: Pure domain entities encapsulating system state, domain enums, and observable properties for JavaFX data-binding without any dependencies on the UI or controllers.
- **View (`src/main/resources/com/rupp/trafficvision/view`, `css`)**: Declarative FXML markup defining layout hierarchies, responsive split-panes, data tables, charts, and CSS styling.
- **Controller (`com.rupp.trafficvision.controller`)**: Manages user actions, drives services, orchestrates animations, and delegates heavy computing away from the JavaFX Application Thread.

### 2.2 Strategy Pattern
The Strategy Pattern is implemented in two critical subsystems:
1. **Image Processing Pipeline (`com.rupp.trafficvision.processing.ImageProcessor`)**:
   - Allows dynamic runtime swapping between `GrayscaleProcessor`, `EdgeDetectionProcessor`, `BlurProcessor`, `ThresholdProcessor`, and `ContrastProcessor`.
2. **Thread Synchronization Strategy (`com.rupp.trafficvision.sync.SynchronizationStrategy`)**:
   - Dynamically toggles between **Mutex Protection (`ProcessingLock`)** and **Concurrency Throttling (`ProcessingSemaphore`)**.

### 2.3 Observer & Event-Dispatch Pattern
- `StatisticsService` notifies UI listeners via `Platform.runLater()` whenever detection tasks complete.
- JavaFX observable properties (`SimpleStringProperty`, `SimpleObjectProperty`) allow automatic table and chart updates without manual polling.

---

## 3. Package Structure

```
com.rupp.trafficvision
├── MainApp.java                        # JavaFX Application lifecycle entry
├── Launcher.java                       # Standard classpath main launcher
├── model
│   ├── Vehicle.java                   # Observable vehicle record for TableView
│   ├── VehicleType.java               # Enum (CAR, MOTORCYCLE, BICYCLE, TRICYCLE, MOPED)
│   ├── TrafficLightColor.java         # Enum (RED, YELLOW, GREEN)
│   ├── ThreadStatus.java              # Enum (IDLE, PROCESSING, WAITING, COMPLETED)
│   ├── BoundingBox.java               # Spatial bounds and aspect ratio calculations
│   ├── DetectionResult.java           # Complete analysis outputDTO
│   └── ProcessingParameters.java      # State tunables (mode, sliders, checkboxes)
├── processing
│   ├── ImageProcessor.java            # Functional Strategy interface
│   ├── GrayscaleProcessor.java        # ITU-R BT.601 Luminance filter
│   ├── EdgeDetectionProcessor.java    # 3x3 Sobel spatial gradient convolution
│   ├── BlurProcessor.java             # 3x3 Gaussian smoothing filter
│   ├── ThresholdProcessor.java        # Binary thresholding segmenter
│   └── ContrastProcessor.java         # Parametric brightness/contrast adjustor
├── detection
│   ├── VehicleDetector.java           # Swappable CV engine interface
│   └── ImageProcessingVehicleDetector.java # Projection-histogram CV engine
├── thread
│   ├── ImageProcessingTask.java       # 7-stage JavaFX asynchronous task
│   └── ProcessingWorker.java          # Worker metadata for Thread Monitor
├── sync
│   ├── SynchronizationStrategy.java   # Concurrency contract
│   ├── lock
│   │   └── ProcessingLock.java        # ReentrantLock mutual exclusion
│   └── semaphore
│       └── ProcessingSemaphore.java   # Semaphore permit throttling
├── statistics
│   └── StatisticsManager.java         # Thread-safe metric aggregator
├── service
│   ├── ImageProcessingService.java    # Transformation coordinator
│   ├── VehicleDetectionService.java   # Background thread pool executor
│   ├── CameraService.java             # Synthetic 20 FPS video simulation & snapshot
│   └── StatisticsService.java         # UI notification dispatcher
├── controller
│   └── DashboardController.java       # Presentation controller
├── ui.components
│   └── TrafficLightComponent.java     # Interactive animated traffic signal control
└── util
    ├── Constants.java                 # Theme tokens and app metadata
    ├── ImageUtil.java                 # BufferedImage/JavaFX conversion & PNG export
    ├── LoggerUtil.java                # Thread-formatted console logger
    ├── TimeUtil.java                  # Date/time & duration formatters
    └── SampleImageGenerator.java      # Synthetic sample dataset builder
```

---

## 4. Key Subsystem Highlights

### 4.1 Multithreading Architecture
Heavy computer vision convolutions are never permitted to run on the JavaFX Application Thread. Tasks are wrapped in `javafx.concurrent.Task<DetectionResult>` and dispatched into a daemon-backed `FixedThreadPool` (`Worker-1` through `Worker-4`). The UI progress bar, stage labels, and intermediate preview images are streamed using standard JavaFX concurrency hooks (`updateProgress`, `updateMessage`).

### 4.2 Traffic Light & Camera Simulation
The system includes a dedicated `CameraService` running an autonomous daemon scheduler at ~20 FPS. It renders a perspective asphalt roadway, approaching vehicles with physical scaling, yellow dashed road markings, stop lines, and a live traffic signal housing. Users can take instant snapshots at any moment to evaluate detection accuracy.

### 4.3 Clean Shutdown Mechanism
When the primary stage close request is intercepted (`primaryStage.setOnCloseRequest(...)`), the controller cleanly stops:
1. Scheduled camera rendering threads.
2. The UI live clock timeline.
3. The traffic signal auto-cycle timeline.
4. The background `ExecutorService` pool via `shutdown()` and `shutdownNow()` timeouts.
