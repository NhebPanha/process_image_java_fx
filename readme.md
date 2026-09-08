# Traffic Light Vehicle Image Processing System 🚦🚗

[![Java](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21.0.6-007396?style=for-the-badge&logo=java&logoColor=white)](https://openjfx.io/)
[![Maven](https://img.shields.io/badge/Build-Maven%203.9-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![Architecture](https://img.shields.io/badge/Architecture-MVC%20%2B%20Strategy-10B981?style=for-the-badge)](docs/ARCHITECTURE.md)
[![Tests](https://img.shields.io/badge/Tests-12%2F12%20Passing-brightgreen?style=for-the-badge)](docs/USER_GUIDE.md)

An enterprise-grade desktop computer vision application built with **Java 21** and **JavaFX**. The system simulates and processes live traffic light camera feeds to detect, localize, and classify urban vehicles into five key categories without relying on external cloud APIs or heavyweight neural network runtimes.

---

## 📸 System Overview & Dashboard UI

```
========================================================================================================
[HEADER]  🚦 Traffic Light Vehicle Image Processing System     [🔴 🟡 🟢]    [14:32:05]    [⚡ RUN]
========================================================================================================
[CONTROLS]             | [CAMERA VIEWPORT & OVERLAY]                 | [METRICS & ANALYTICS]
-----------------------+---------------------------------------------+----------------------------------
📷 Camera Feed:        |                                             | 🏆 Latest Detection:
[📸 Take Picture]      |       +-----------------------------+       |    Vehicle: 🚗 Car
[🔄 Cycle Light]       |       |  /========================\ |       |    Confidence: 94.5%
                       |       |  |   [🚗 Bounding Box]     | |       |    Elapsed: 42 ms
🚗 Quick Samples:      |       |  |  +--------------------+ | |       |
[Car] [Motorcycle]     |       |  |  |    CAR DETECTED    | | |       | 📊 Aggregate Metrics:
[Bicycle] [Tricycle]   |       |  |  +--------------------+ | |       |    Total: 24  Success: 24
[Moped]                |       |  \========================/ |       |    Avg Time: 48.2 ms
                       |       +-----------------------------+       |
⚙ Processing Filters:  |                                             | 📈 Latency Line Chart:
Mode: [Sobel Edges ▼]  |                                             |    [~~~~~~~~~~~~~~~~~~]
Brightness: [──●────]  |                                             |
Contrast:   [────●──]  |                                             | 🥧 Vehicle Distribution:
Threshold:  [───●───]  | [Progress: 100% | Stage 7: Complete]        |    [Car 50% | Moto 25% | ...]
                       +---------------------------------------------+----------------------------------
🔒 Synchronization:    | [TAB 1: Vehicle Log Table]                  | [TAB 2: OS Thread Monitor]
(o) ReentrantLock      | Time  | ID      | Type    | Conf | Status   | Thread   | ID | State   | Task
( ) Semaphore (2 Perm) | 14:32 | VEH-001 | 🚗 Car  | 94%  | DETECTED | Worker-1 | 24 | IDLE    | Ready
========================================================================================================
```

---

## 🌟 Key Features

1. **5 Supported Vehicle Categories**:
   - 🚗 **Car**: Horizontal elongated aspect ratio ($AR \ge 1.32$).
   - 🏍 **Motorcycle**: Slender vertical silhouette with mechanical density ($0.40 \le AR < 0.72$).
   - 🚲 **Bicycle**: Thin skeletal frame structure with low edge density ($D < 0.22$).
   - 🛺 **Tricycle (Tuk-Tuk / Auto-Rickshaw)**: Tall boxy cabin profile ($0.80 \le AR \le 1.32$, width $\ge 105\text{ px}$).
   - 🛵 **Moped**: Compact urban step-through chassis ($0.72 \le AR < 1.10$).

2. **Deterministic Computer Vision Pipeline**:
   - 100% local, self-contained implementation in pure Java.
   - **Luminance Grayscale Conversion**: ITU-R BT.601 standard ($Y = 0.299R + 0.587G + 0.114B$).
   - **Edge Detection**: $3 \times 3$ Sobel spatial gradient convolution ($G_x$, $G_y$, magnitude clamping).
   - **Smoothing & Thresholding**: $3 \times 3$ Gaussian blur kernel, binary thresholding, and contrast stretching.
   - **Projection Profiling**: 1D horizontal and vertical projection histograms isolating the vehicle body and detaching background poles/curbs.

3. **Operating Systems Multithreading & Synchronization**:
   - **Worker Thread Model**: Asynchronous execution via `FixedThreadPool` preventing UI thread starvation.
   - **Strategy Pattern for Synchronization**:
     - **Option A (`ProcessingLock`)**: Fair `ReentrantLock` for single-lane mutual exclusion.
     - **Option B (`ProcessingSemaphore`)**: Counting `Semaphore` (2 permits) for dual-lane concurrency throttling.
   - **Real-Time Thread Monitor**: Live table tracking `Worker-1` through `Worker-4`, their OS states (`IDLE`, `PROCESSING`, `WAITING`), and active workloads.

4. **Synthetic Camera & Traffic Light Simulator**:
   - Autonomous 20 FPS daemon camera simulation rendering realistic perspective roadways, lane dashes, stop lines, approaching vehicles, and live traffic light signals.
   - Instant camera snapshot captures with interactive traffic light color cycling (Red, Yellow, Green).

5. **Modern Dark-Themed UI**:
   - Custom CSS theme with glassmorphic cards, emerald glowing accents, modern typography, dynamic line/pie charts, and drag-and-drop image import.

---

## 🚀 Quick Start & Build Instructions

### Prerequisites
- **JDK 21 LTS** or higher
- **Maven 3.9+** (or use the included wrapper `./mvnw.cmd`)

### 1. Compile the Project
```powershell
.\mvnw.cmd clean compile
```

### 2. Execute the Test Suite
```powershell
.\mvnw.cmd test
```
*Executes all 12 unit tests across image processing filters, ReentrantLock mutual exclusion, counting semaphores, and vehicle detection heuristics.*

### 3. Launch the Application
```powershell
.\mvnw.cmd javafx:run
```

---

## 🏛 Software Architecture & Patterns

The codebase is engineered according to strict enterprise patterns:

```
com.rupp.trafficvision
├── MainApp.java                       # Application lifecycle entry
├── Launcher.java                      # Safe runtime launcher
├── model                              # Domain models (Vehicle, BoundingBox, Enums)
├── processing                         # ImageProcessor Strategy implementations
├── detection                          # Computer vision detection engine
├── sync                               # SynchronizationStrategy (Lock vs Semaphore)
├── thread                             # Asynchronous ImageProcessingTask & Worker metadata
├── statistics                         # Thread-safe metric aggregation
├── service                            # Background thread pool and camera services
├── controller                         # Dashboard presentation controller
└── ui.components                      # Interactive animated TrafficLightComponent
```

---

## 📚 Academic Documentation & Reports

Detailed technical documentation and reports are provided in the `/docs` directory:

| Document | Description |
| :--- | :--- |
| 📖 [**Architecture Guide**](docs/ARCHITECTURE.md) | In-depth MVC design, package decomposition, and class interactions |
| 🖥 [**User Guide**](docs/USER_GUIDE.md) | Complete GUI walkthrough, control explanations, and workflows |
| 🧵 [**Thread Synchronization Report**](docs/THREAD_SYNCHRONIZATION_REPORT.md) | OS concurrency analysis, race condition prevention, Lock vs Semaphore benchmarks |
| 📐 [**Image Processing Pipeline**](docs/IMAGE_PROCESSING_PIPELINE.md) | Mathematical derivations of Sobel operators, projection histograms, and classification |
| 📊 [**PlantUML Diagrams**](docs/diagrams/) | Class, Sequence, State, Activity, and Concurrency architecture diagrams |

---

## 🧪 Unit Test Coverage

```
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running com.rupp.trafficvision.ImageProcessingTest
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
Running com.rupp.trafficvision.SynchronizationTest
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
Running com.rupp.trafficvision.VehicleDetectionTest
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0

RESULTS: 12 tests passed, 0 failures, 0 errors
BUILD SUCCESS
```

---

## 👥 Academic Credits & Acknowledgements

- **Institution:** Royal University of Phnom Penh (RUPP)
- **Course:** Advanced Software Engineering & Operating Systems
- **Author:** Nheb Panha
- **Instructor:** Faculty of Engineering & Computer Science
