# Traffic Vision: Comprehensive User Guide

---

## 1. System Requirements

- **Operating System:** Windows 10/11, macOS (11+), or Linux (Ubuntu 20.04+)
- **Java Development Kit (JDK):** Java 21 (LTS) or higher
- **Build Tool:** Apache Maven 3.9+ (or use the included `./mvnw.cmd` / `./mvnw`)
- **Display Resolution:** 1400 x 900 minimum recommended for optimal dashboard rendering

---

## 2. Compilation and Launch Instructions

### 2.1 Compiling the Codebase
Open PowerShell or a terminal in the root project directory and execute:
```powershell
.\mvnw.cmd clean compile
```

### 2.2 Running Unit Tests
To execute all 12 test suites (processing filters, thread locks, semaphores, and vehicle detection):
```powershell
.\mvnw.cmd test
```

### 2.3 Launching the Application
Launch the JavaFX dashboard using the Maven JavaFX plugin:
```powershell
.\mvnw.cmd javafx:run
```
Alternatively, execute using the non-modular launcher:
```powershell
java -cp target/classes com.rupp.trafficvision.Launcher
```

---

## 3. Graphical Interface Walkthrough

```
+-----------------------------------------------------------------------------------------------+
| [TOP BAR]  Traffic Light Vehicle Image Processing System     [RED|YELLOW|GREEN] [CLOCK] [RUN] |
+------------------+-----------------------------------------------+----------------------------+
| [LEFT PANEL]     | [CENTER VIEWPORT]                             | [RIGHT METRICS]            |
| • Camera Controls|  +-----------------------------------------+  | • Active Detection Result  |
|   - Snapshot     |  | Live Camera Viewport / Static Image      |  |   - Type: CAR 🚗           |
|   - Cycle Light  |  | (Overlaid with emerald Bounding Box     |  |   - Confidence: 94.5%      |
| • Presets        |  |  and high-contrast classification badge)|  |   - Elapsed: 42 ms         |
|   - Sample 1..5  |  +-----------------------------------------+  | • Aggregate Counters       |
| • Filters        |  [PROGRESS BAR: 100% | Detection Complete]    |   - Cars: 12  Bikes: 4     |
|   - Mode Combo   +-----------------------------------------------+   - Avg Time: 48.2 ms      |
|   - Sliders      | [BOTTOM SPLIT PANE]                           | • Performance Chart        |
| • Sync Strategy  |  [TAB 1] Detected Vehicles History Table      | • Distribution Pie Chart   |
|   - Lock / Sem   |  [TAB 2] OS Thread Concurrency Monitor Table  |                            |
+------------------+-----------------------------------------------+----------------------------+
```

### 3.1 Top Header Bar
- **System Title & Branding:** Displays application version and institution metadata.
- **Interactive Traffic Signal Indicator:** Visual 3-lens signal indicating current street traffic light status.
- **Real-Time Clock:** High-precision system time updated every second.
- **Primary Action Buttons:** `[Process Image]`, `[Take Picture]`, `[Reset]`.

### 3.2 Left Control Panel
1. **Camera Controls:**
   - `Take Picture`: Captures the current dynamic road frame from the simulated camera stream.
   - `Next Light Color`: Rotates the intersection signal between Red, Yellow, and Green.
2. **Sample Datasets:**
   - Quick-select buttons for 5 built-in vehicle archetypes: **Car**, **Motorcycle**, **Bicycle**, **Tricycle**, and **Moped**.
3. **Processing Parameter Tunables:**
   - **Mode Combo:** Select from *None*, *Grayscale*, *Edge Detection*, *Blur*, *Threshold*, or *Contrast*.
   - **Brightness & Contrast Sliders:** Fine-tune image dynamic range.
   - **Threshold Slider:** Adjust binary segmentation cutoffs (0–255).
   - **Pipeline Checkboxes:** Enable/disable Grayscale, Noise Reduction, Edge Detection, or Contrast Enhancement.
4. **Synchronization Strategy Selector:**
   - **ReentrantLock (Mutex):** Enforces strict mutual exclusion for single-lane processing.
   - **Semaphore (2 Permits):** Allows up to 2 concurrent worker tasks across dual lanes.

### 3.3 Center Image Viewport
- Displays the loaded or captured frame.
- Drag-and-drop enabled: Drop any PNG, JPG, or BMP file directly onto the viewport to load.
- During detection, intermediate processing stages stream visually in real time.
- Upon completion, draws an emerald bounding box around the vehicle with a vehicle badge showing type and confidence percentage.

### 3.4 Right Metrics & Analytics
- **Primary Detection Card:** Large glowing vehicle icon, classification label, confidence score, and processing time.
- **Metric Summary Cards:** Total images submitted, processed count, success count, and average latency.
- **Vehicle Distribution Breakdown:** Individual counters for Cars, Motorcycles, Bicycles, Tricycles, and Mopeds.
- **Visual Analytics:**
  - Dynamic **Line Chart** plotting latency over successive runs.
  - Interactive **Pie Chart** plotting vehicle fleet composition.

### 3.5 Bottom Tables & Thread Monitor
- **Vehicle Log Table:** Detailed log of each vehicle processed with Timestamp, Vehicle ID, Type, Confidence, Duration, Status, and Aspect Ratio.
- **Thread Concurrency Monitor:** Real-time operating systems monitor displaying all 4 worker threads (`Worker-1` to `Worker-4`), their current OS state (`IDLE`, `PROCESSING`, `WAITING`), thread IDs, and active tasks.

---

## 4. Step-by-Step Workflow Example

1. **Step 1:** Click **"Sample 1: Car"** in the left presets panel (or click **"Take Picture"** to snapshot the live camera).
2. **Step 2:** Ensure the synchronization strategy is set to **"ReentrantLock"**.
3. **Step 3:** Click the prominent blue **"Process Image"** button.
4. **Step 4:** Observe the bottom progress bar advance through all 7 pipeline stages.
5. **Step 5:** Inspect the detection result:
   - Green bounding box appears on the vehicle.
   - Vehicle Type shows **🚗 Car**.
   - Confidence indicates **> 90%**.
   - Performance metrics and charts update instantly.
6. **Step 6:** Switch synchronization strategy to **"Semaphore (2 Permits)"** and process additional samples to observe multi-threaded permit scheduling in the Thread Concurrency Monitor!
