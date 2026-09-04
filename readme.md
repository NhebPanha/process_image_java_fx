# CV Studio — JavaFX Desktop Application

**A window-based (desktop) implementation of the Computer Vision Process**

Image Acquisition → Preprocessing → Feature Extraction → Object Recognition & Classification → Decision Making

| Item | Value |
| --- | --- |
| Application type | Desktop (JavaFX, windowed) |
| Java version | JDK 21 (LTS) |
| UI toolkit | JavaFX 21 |
| Vision library | OpenCV 4.9 (Java bindings) |
| Inference | DJL (PyTorch engine) or ONNX Runtime |
| Build | Maven + jpackage |
| Target OS | Windows / Linux / macOS |

---

## 1. Scope

The application loads an image or a live camera stream, pushes it through five pipeline stages, shows the intermediate result of every stage in its own panel, and produces a decision (alert, log entry, export).

Each stage of the diagram becomes:

- one **panel** in the UI (visual output),
- one **class** implementing a common `PipelineStage` interface (logic),
- one **controls group** (parameters the user can tune live).

---

## 2. Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     JavaFX UI Layer                         │
│  MainView · StagePanelView · ControlsPane · StatusBar       │
└───────────────┬─────────────────────────────────────────────┘
                │ (JavaFX Properties + Task/Service)
┌───────────────▼─────────────────────────────────────────────┐
│                   Pipeline Layer                            │
│  PipelineRunner → [Acquisition] → [Preprocess] →            │
│                   [Features] → [Recognition] → [Decision]   │
└───────────────┬─────────────────────────────────────────────┘
                │
┌───────────────▼─────────────────────────────────────────────┐
│              Service / Native Layer                         │
│  CameraService (OpenCV VideoCapture) · ModelService (DJL)   │
│  ImageUtils(Mat ⇄ javafx.scene.image.Image) · Logger       |     
└─────────────────────────────────────────────────────────────┘
```

**Rules**

1. Nothing in the Pipeline layer imports JavaFX except property types. Vision code stays testable headless.
2. All OpenCV work runs on a background thread. Only the final `Image` handoff touches the FX Application Thread.
3. Every stage takes a `Frame` in and returns a `Frame` out — stages can be reordered, disabled, or unit-tested individually.

---

## 3. Project structure

```
cv-studio/
├── pom.xml
├── src/main/java/com/turbotech/cvstudio/
│   ├── App.java                        # JavaFX entry point
│   ├── ui/
│   │   ├── MainView.java               # BorderPane root
│   │   ├── StagePanelView.java         # reusable image + caption card
│   │   ├── ControlsPane.java           # sliders, combos, toggles
│   │   └── DetectionOverlay.java       # canvas for boxes/labels
│   ├── pipeline/
│   │   ├── PipelineStage.java          # interface
│   │   ├── PipelineRunner.java         # orchestrator
│   │   ├── AcquisitionStage.java
│   │   ├── PreprocessStage.java
│   │   ├── FeatureExtractionStage.java
│   │   ├── RecognitionStage.java
│   │   └── DecisionStage.java
│   ├── core/
│   │   ├── CameraService.java          # javafx.concurrent.Service
│   │   ├── ModelService.java           # DJL predictor wrapper
│   │   ├── ImageUtils.java             # Mat <-> FX Image
│   │   └── NativeLoader.java
│   ├── model/
│   │   ├── Frame.java                  # Mat + metadata
│   │   ├── Detection.java              # label, confidence, box
│   │   ├── FeatureSet.java             # keypoints, edges, histogram
│   │   ├── DecisionRule.java
│   │   └── PipelineSettings.java       # all tunable params (observable)
│   └── util/EventLog.java
├── src/main/resources/com/turbotech/cvstudio/
│   ├── main.fxml
│   ├── styles.css
│   └── models/                         # .onnx / .pt files
└── src/test/java/...
```

---

## 4. Dependencies (`pom.xml`)

xml

```xml
<properties>    <maven.compiler.release>21</maven.compiler.release>    <javafx.version>21.0.4</javafx.version>    <djl.version>0.30.0</djl.version></properties><dependencies>    <!-- JavaFX -->    <dependency>        <groupId>org.openjfx</groupId>        <artifactId>javafx-controls</artifactId>        <version>${javafx.version}</version>    </dependency>    <dependency>        <groupId>org.openjfx</groupId>        <artifactId>javafx-fxml</artifactId>        <version>${javafx.version}</version>    </dependency>    <dependency>        <groupId>org.openjfx</groupId>        <artifactId>javafx-swing</artifactId>   <!-- SwingFXUtils fallback -->        <version>${javafx.version}</version>    </dependency>    <!-- OpenCV with bundled native libraries -->    <dependency>        <groupId>org.openpnp</groupId>        <artifactId>opencv</artifactId>        <version>4.9.0-0</version>    </dependency>    <!-- Deep learning inference (choose ONE approach) -->    <dependency>        <groupId>ai.djl</groupId>        <artifactId>api</artifactId>        <version>${djl.version}</version>    </dependency>    <dependency>        <groupId>ai.djl.pytorch</groupId>        <artifactId>pytorch-engine</artifactId>        <version>${djl.version}</version>        <scope>runtime</scope>    </dependency>    <dependency>        <groupId>ai.djl.pytorch</groupId>        <artifactId>pytorch-model-zoo</artifactId>        <version>${djl.version}</version>    </dependency></dependencies><build>    <plugins>        <plugin>            <groupId>org.openjfx</groupId>            <artifactId>javafx-maven-plugin</artifactId>            <version>0.0.8</version>            <configuration>                <mainClass>com.turbotech.cvstudio.App</mainClass>            </configuration>        </plugin>    </plugins></build>
```

> **Alternative to DJL:** `com.microsoft.onnxruntime:onnxruntime:1.19.0` if you prefer to ship a `.onnx` model (YOLOv8n exports cleanly to ONNX and keeps the bundle ~50 MB instead of ~400 MB).
> 
> 
> **Alternative to OpenCV Java:** OpenCV's own DNN module (`Dnn.readNetFromONNX`) removes the DJL dependency entirely — smaller install, slightly more manual pre/post-processing.
> 

---

## 5. Window layout

```
┌───────────────────────────────────────────────────────────────────────┐
│ File   Camera   Pipeline   Help                          [ MenuBar ]  │
├───────────────────────────────────────────────────────────────────────┤
│ [Open Image] [Start Camera] [Snapshot] [▶ Run] [⏸] [Export Report]     │
├──────────────────────────────────────────┬────────────────────────────┤
│                                          │  Controls (Accordion)      │
│   Stage panels — TabPane or GridPane     │  ▸ 1 Acquisition           │
│   ┌────────────┬────────────┐            │     source, resolution     │
│   │ 1 Source   │ 2 Preproc  │            │  ▾ 2 Preprocessing         │
│   ├────────────┼────────────┤            │     brightness  ──○──      │
│   │ 3 Features │ 4 Detected │            │     contrast    ──○──      │
│   └────────────┴────────────┘            │     blur / grayscale ☑     │
│                                          │  ▸ 3 Feature Extraction    │
│                                          │  ▸ 4 Recognition           │
│                                          │  ▸ 5 Decision Rules        │
├──────────────────────────────────────────┴────────────────────────────┤
│ Detections table: label | confidence | x,y,w,h        Decision: ALERT  │
├───────────────────────────────────────────────────────────────────────┤
│ Status: 1280×720 · 24.6 FPS · pipeline 41 ms · model yolov8n           │
└───────────────────────────────────────────────────────────────────────┘
```

`BorderPane` root → `MenuBar` + `ToolBar` in `top`, `GridPane` of `StagePanelView` in `center`, `Accordion` in `right`, `TableView` + status `HBox` in `bottom`.

---

## 6. Core data model

java

```java
package com.turbotech.cvstudio.model;import org.opencv.core.Mat;import java.util.ArrayList;import java.util.List;public class Frame {    private Mat original;         // stage 1 output    private Mat processed;        // stage 2 output    private Mat featureView;      // stage 3 visualization    private FeatureSet features;  // stage 3 data    private final List<Detection> detections = new ArrayList<>();    private String decision = "—";    private long timestampMs = System.currentTimeMillis();    private final List<StageTiming> timings = new ArrayList<>();// getters / setters omitted    public record StageTiming(String stage, long millis) {}}
```

java

```java
public record Detection(String label, double confidence,                        int x, int y, int w, int h) {}
```

java

```java
public interface PipelineStage {    String name();    Frame apply(Frame frame, PipelineSettings settings);    default boolean enabled(PipelineSettings s) { return true; }}
```

`PipelineSettings` holds JavaFX observable properties (`DoubleProperty brightness`, `IntegerProperty blurKernel`, `DoubleProperty confidenceThreshold`, …) so sliders bind directly and any change can trigger a re-run.

---

## 7. Stage 1 — Image Acquisition

*Capturing images or videos using cameras and sensors.*

### 7.1 Native library loading

Call once before any OpenCV class is touched, in `App.init()`:

java

```java
public final class NativeLoader {    private static boolean loaded = false;    public static synchronized void load() {        if (!loaded) {            nu.pattern.OpenCV.loadLocally();   // extracts + System.load            loaded = true;        }    }}
```

### 7.2 File source

java

```java
FileChooser chooser = new FileChooser();chooser.getExtensionFilters().add(    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.bmp"));File file = chooser.showOpenDialog(stage.getWindow());if (file != null) {    Mat src = Imgcodecs.imread(file.getAbsolutePath());    if (src.empty()) { showError("Unsupported or corrupt image."); return; }    pipelineRunner.submit(new Frame(src));}
```

### 7.3 Camera source (`javafx.concurrent.Service`)

java

```java
public class CameraService extends Service<Void> {    private final IntegerProperty deviceIndex = new SimpleIntegerProperty(0);    private final Consumer<Mat> frameConsumer;    private volatile boolean running;    public CameraService(Consumer<Mat> frameConsumer) {        this.frameConsumer = frameConsumer;    }    @Override protected Task<Void> createTask() {        return new Task<>() {            @Override protected Void call() {                VideoCapture capture = new VideoCapture(deviceIndex.get());                capture.set(Videoio.CAP_PROP_FRAME_WIDTH, 1280);                capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, 720);                if (!capture.isOpened()) {                    updateMessage("Camera " + deviceIndex.get() + " unavailable");                    return null;                }                running = true;                Mat buffer = new Mat();                try {                    while (running && !isCancelled()) {                        if (capture.read(buffer) && !buffer.empty()) {                            frameConsumer.accept(buffer.clone());                        }                        Thread.sleep(15);          // ~60 fps ceiling                    }                } catch (InterruptedException e) {                    Thread.currentThread().interrupt();                } finally {                    capture.release();                    buffer.release();                }                return null;            }        };    }    public void stop() { running = false; cancel(); }}
```

### 7.4 Mat → JavaFX Image

Fast path (no encoding), used for every preview:

java

```java
public static WritableImage toFxImage(Mat mat, WritableImage reuse) {    Mat bgr = mat;    if (mat.channels() == 1) {        bgr = new Mat();        Imgproc.cvtColor(mat, bgr, Imgproc.COLOR_GRAY2BGR);    }    int w = bgr.cols(), h = bgr.rows();    byte[] buffer = new byte[w * h * 3];    bgr.get(0, 0, buffer);    WritableImage img = (reuse != null && reuse.getWidth() == w                         && reuse.getHeight() == h) ? reuse : new WritableImage(w, h);    img.getPixelWriter().setPixels(0, 0, w, h,        PixelFormat.getByteBgrInstance(), buffer, 0, w * 3);    if (bgr != mat) bgr.release();    return img;}
```

Reusing the `WritableImage` avoids allocating a new buffer 30×/second.

---

## 8. Stage 2 — Preprocessing

*Enhancing images by adjusting brightness and contrast.*

java

```java
public class PreprocessStage implements PipelineStage {    @Override public String name() { return "Preprocessing"; }    @Override public Frame apply(Frame frame, PipelineSettings s) {        Mat src = frame.getOriginal();        Mat out = new Mat();// 1. Brightness (beta) and contrast (alpha):  out = alpha*src + beta        src.convertTo(out, -1, s.getContrast(), s.getBrightness());// 2. Optional resize for consistent downstream cost        if (s.isResizeEnabled()) {            Imgproc.resize(out, out, new Size(s.getTargetWidth(), s.getTargetHeight()),                           0, 0, Imgproc.INTER_AREA);        }// 3. Denoise        int k = s.getBlurKernel();        if (k > 1) {            if (k % 2 == 0) k++;                      // kernel must be odd            Imgproc.GaussianBlur(out, out, new Size(k, k), 0);        }// 4. Grayscale        if (s.isGrayscale() && out.channels() == 3) {            Imgproc.cvtColor(out, out, Imgproc.COLOR_BGR2GRAY);        }// 5. Contrast-limited adaptive histogram equalisation        if (s.isClaheEnabled()) {            Mat gray = out.channels() == 1 ? out : new Mat();            if (out.channels() != 1) Imgproc.cvtColor(out, gray, Imgproc.COLOR_BGR2GRAY);            CLAHE clahe = Imgproc.createCLAHE(s.getClaheClipLimit(), new Size(8, 8));            clahe.apply(gray, gray);            out = gray;        }        frame.setProcessed(out);        return frame;    }}
```

**UI bindings**

| Control | Property | Range | Default |
| --- | --- | --- | --- |
| Brightness slider | `brightness` (beta) | −100 … 100 | 0 |
| Contrast slider | `contrast` (alpha) | 0.5 … 3.0 | 1.0 |
| Blur slider | `blurKernel` | 1 … 15 (odd) | 3 |
| Grayscale toggle | `grayscale` | — | off |
| CLAHE toggle + clip | `claheEnabled`, `claheClipLimit` | 1 … 8 | off / 2.0 |

Bind and re-run on change:

java

```java
brightnessSlider.valueProperty().bindBidirectional(settings.brightnessProperty());settings.brightnessProperty().addListener((o, a, b) -> runner.rerunFromStage(2));
```

Debounce with a `PauseTransition` (≈120 ms) so dragging a slider doesn't queue 200 pipeline runs.

---

## 9. Stage 3 — Feature Extraction

*Identifying key patterns and textures in an image.*

java

```java
public class FeatureExtractionStage implements PipelineStage {    private final ORB orb = ORB.create(500);    @Override public String name() { return "Feature Extraction"; }    @Override public Frame apply(Frame frame, PipelineSettings s) {        Mat src = frame.getProcessed();        Mat gray = new Mat();        if (src.channels() == 3) Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);        else gray = src.clone();        FeatureSet fs = new FeatureSet();        Mat view = new Mat();        Imgproc.cvtColor(gray, view, Imgproc.COLOR_GRAY2BGR);        switch (s.getFeatureMode()) {            case EDGES -> {                Mat edges = new Mat();                Imgproc.Canny(gray, edges, s.getCannyLow(), s.getCannyHigh());                fs.setEdges(edges);                Imgproc.cvtColor(edges, view, Imgproc.COLOR_GRAY2BGR);            }            case CONTOURS -> {                Mat edges = new Mat();                Imgproc.Canny(gray, edges, s.getCannyLow(), s.getCannyHigh());                List<MatOfPoint> contours = new ArrayList<>();                Imgproc.findContours(edges, contours, new Mat(),                        Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);                contours.removeIf(c -> Imgproc.contourArea(c) < s.getMinContourArea());                Imgproc.drawContours(view, contours, -1, new Scalar(0, 255, 0), 2);                fs.setContourCount(contours.size());            }            case KEYPOINTS -> {                MatOfKeyPoint kp = new MatOfKeyPoint();                Mat descriptors = new Mat();                orb.detectAndCompute(gray, new Mat(), kp, descriptors);                Features2d.drawKeypoints(view, kp, view, new Scalar(0, 200, 255), 0);                fs.setKeypoints(kp);                fs.setDescriptors(descriptors);            }            case HISTOGRAM -> {                fs.setHistogram(computeHistogram(gray));                view = renderHistogram(fs.getHistogram(), 512, 300);            }        }        frame.setFeatures(fs);        frame.setFeatureView(view);        gray.release();        return frame;    }}
```

The histogram mode can render straight into a JavaFX `BarChart` instead of an OpenCV `Mat` — cleaner and interactive. Both are valid; the `Mat` route keeps all four panels visually consistent.

---

## 10. Stage 4 — Object Recognition and Classification

*Using AI models to classify and recognize objects.*

### 10.1 Model service (DJL)

java

```java
public class ModelService implements AutoCloseable {    private ZooModel<Image, DetectedObjects> model;    private Predictor<Image, DetectedObjects> predictor;    public void load(Path modelPath, float threshold) throws Exception {        Criteria<Image, DetectedObjects> criteria = Criteria.builder()            .setTypes(Image.class, DetectedObjects.class)            .optModelPath(modelPath)            .optEngine("PyTorch")            .optTranslatorFactory(new YoloV8TranslatorFactory())            .optArgument("threshold", threshold)            .optProgress(new ProgressBar())            .build();        model = criteria.loadModel();        predictor = model.newPredictor();    }    public List<Detection> detect(Mat mat) throws TranslateException {        BufferedImage bi = ImageUtils.toBufferedImage(mat);        Image img = ImageFactory.getInstance().fromImage(bi);        DetectedObjects results = predictor.predict(img);        List<Detection> list = new ArrayList<>();        for (Classifications.Classification c : results.items()) {            DetectedObjects.DetectedObject d = (DetectedObjects.DetectedObject) c;            BoundingBox box = d.getBoundingBox();            Rectangle r = box.getBounds();            list.add(new Detection(                d.getClassName(), d.getProbability(),                (int) (r.getX() * mat.cols()), (int) (r.getY() * mat.rows()),                (int) (r.getWidth() * mat.cols()), (int) (r.getHeight() * mat.rows())));        }        return list;    }    @Override public void close() {        if (predictor != null) predictor.close();        if (model != null) model.close();    }}
```

Load the model **once**, in a background `Task` at startup, with a progress indicator — first load can take several seconds and will download the engine natives on a fresh machine.

### 10.2 Stage wrapper

java

```java
public class RecognitionStage implements PipelineStage {    private final ModelService models;    @Override public Frame apply(Frame frame, PipelineSettings s) {        try {            List<Detection> found = models.detect(frame.getProcessed());            found.stream()                 .filter(d -> d.confidence() >= s.getConfidenceThreshold())                 .filter(d -> s.getClassFilter().isEmpty()                           || s.getClassFilter().contains(d.label()))                 .forEach(frame.getDetections()::add);        } catch (Exception e) {            EventLog.error("Inference failed: " + e.getMessage());        }        return frame;    }}
```

### 10.3 Drawing boxes

Draw on a JavaFX `Canvas` layered over the `ImageView` in a `StackPane`, not into the `Mat`. That keeps the pixel data clean and lets you toggle the overlay without re-running inference.

java

```java
public void render(List<Detection> detections, double scaleX, double scaleY) {    GraphicsContext g = canvas.getGraphicsContext2D();    g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());    g.setLineWidth(2);    for (Detection d : detections) {        double x = d.x() * scaleX, y = d.y() * scaleY;        double w = d.w() * scaleX, h = d.h() * scaleY;        g.setStroke(colorFor(d.label()));        g.strokeRect(x, y, w, h);        String label = "%s %.0f%%".formatted(d.label(), d.confidence() * 100);        g.setFill(colorFor(d.label()));        g.fillRect(x, y - 18, label.length() * 7.5 + 8, 18);        g.setFill(Color.WHITE);        g.fillText(label, x + 4, y - 5);    }}
```

---

## 11. Stage 5 — Decision Making

*Acting on extracted information for various applications.*

A small rule engine turns detections into an action. Rules are data, so they can be edited in the UI or loaded from JSON.

java

```java
public record DecisionRule(String name,                           String targetClass,                           int minCount,                           double minConfidence,                           Action action) {    public enum Action { LOG, HIGHLIGHT, ALERT, SNAPSHOT, WEBHOOK }}
```

java

```java
public class DecisionStage implements PipelineStage {    @Override public Frame apply(Frame frame, PipelineSettings s) {        List<String> fired = new ArrayList<>();        for (DecisionRule rule : s.getRules()) {            long count = frame.getDetections().stream()                .filter(d -> d.label().equalsIgnoreCase(rule.targetClass()))                .filter(d -> d.confidence() >= rule.minConfidence())                .count();            if (count >= rule.minCount()) {                fired.add(rule.name());                switch (rule.action()) {                    case LOG       -> EventLog.info(rule.name() + " matched (" + count + ")");                    case ALERT     -> Platform.runLater(() -> showAlert(rule, count));                    case SNAPSHOT  -> saveSnapshot(frame, rule.name());                    case WEBHOOK   -> webhookClient.postAsync(rule, frame);                    case HIGHLIGHT -> frame.setHighlight(true);                }            }        }        frame.setDecision(fired.isEmpty() ? "No action" : String.join(", ", fired));        return frame;    }}
```

**Example rules**

| Rule | Target | Min count | Min conf. | Action |
| --- | --- | --- | --- | --- |
| Person in restricted zone | `person` | 1 | 0.60 | ALERT + SNAPSHOT |
| Vehicle counting | `car` | 1 | 0.45 | LOG |
| Empty shelf | `bottle` | 0 | — | WEBHOOK |
| Helmet compliance | `helmet` | 1 | 0.70 | HIGHLIGHT |

**Outputs**

- `TableView<Detection>` bound to `frame.getDetections()`.
- Rolling event log (`ListView<String>`), capped at 500 rows.
- `Export Report` → CSV (timestamp, label, confidence, box, decision) and optionally a PNG of the annotated frame.

---

## 12. Orchestration & threading

The single most common way a JavaFX vision app breaks is doing OpenCV work on the FX thread. Structure it like this:

java

```java
public class PipelineRunner {    private final List<PipelineStage> stages;    private final PipelineSettings settings;    private final ExecutorService executor =        Executors.newSingleThreadExecutor(r -> {            Thread t = new Thread(r, "cv-pipeline");            t.setDaemon(true);            return t;        });    private final AtomicBoolean busy = new AtomicBoolean(false);    private final Consumer<Frame> onComplete;   // called on FX thread    public void submit(Frame frame) {// Drop frames instead of queueing — keeps live preview real-time        if (!busy.compareAndSet(false, true)) {            frame.release();            return;        }        executor.submit(() -> {            try {                Frame result = frame;                for (PipelineStage stage : stages) {                    if (!stage.enabled(settings)) continue;                    long t0 = System.nanoTime();                    result = stage.apply(result, settings);                    result.addTiming(stage.name(), (System.nanoTime() - t0) / 1_000_000);                }                Frame done = result;                Platform.runLater(() -> onComplete.accept(done));            } catch (Exception e) {                Platform.runLater(() -> EventLog.error(e.getMessage()));            } finally {                busy.set(false);            }        });    }}
```

**Frame-dropping** (rather than an unbounded queue) is deliberate: if inference takes 80 ms and the camera delivers every 33 ms, a queue grows without bound and latency climbs until the app is showing 10-second-old video.

**Mat lifetime:** OpenCV `Mat` holds native memory that the GC doesn't account for. Call `release()` on every intermediate `Mat` once you're done, or wrap frames in a try-with-resources holder. A leak here shows up as steadily climbing RSS with a flat Java heap.

---

## 13. Application entry point

java

```java
public class App extends Application {    private PipelineRunner runner;    private CameraService camera;    private ModelService models;    @Override public void init() {        NativeLoader.load();        models = new ModelService();    }    @Override public void start(Stage stage) {        PipelineSettings settings = new PipelineSettings();        MainView view = new MainView(settings);        runner = new PipelineRunner(            List.of(new AcquisitionStage(),                    new PreprocessStage(),                    new FeatureExtractionStage(),                    new RecognitionStage(models),                    new DecisionStage()),            settings,            view::update);        camera = new CameraService(mat -> runner.submit(new Frame(mat)));        view.bindCamera(camera);        Scene scene = new Scene(view, 1440, 900);        scene.getStylesheets().add(            getClass().getResource("/com/turbotech/cvstudio/styles.css").toExternalForm());        stage.setTitle("CV Studio — Computer Vision Pipeline");        stage.setScene(scene);        stage.setMinWidth(1100);        stage.setMinHeight(720);        stage.show();// Load model off the FX thread        Task<Void> load = new Task<>() {            @Override protected Void call() throws Exception {                models.load(Path.of("models/yolov8n.pt"), 0.35f);                return null;            }        };        load.setOnSucceeded(e -> view.setModelReady(true));        load.setOnFailed(e -> view.showModelError(load.getException()));        new Thread(load, "model-loader").start();    }    @Override public void stop() {        if (camera != null) camera.stop();        if (models != null) models.close();    }    public static void main(String[] args) { launch(args); }}
```

---

## 14. Build and packaging

**Run in development**

bash

```bash
mvn clean javafx:run
```

**Native installer with jpackage**

bash

```bash
mvn clean packagejpackage \  --type msi \  --name "CV Studio" \  --app-version 1.0.0 \  --vendor "TURBOTECH Co., Ltd." \  --input target/dependency \  --main-jar cv-studio-1.0.0.jar \  --main-class com.turbotech.cvstudio.App \  --java-options "-Xmx2g" \  --java-options "--add-modules=javafx.controls,javafx.fxml,javafx.swing" \  --icon src/main/resources/icon.ico \  --win-shortcut --win-menu
```

Use `--type deb`/`--type rpm` on Linux and `--type dmg` on macOS. Model files go in `--input` alongside the jars, or ship them separately and download on first run to keep the installer small.

---

## 15. Testing

| Layer | Approach |
| --- | --- |
| Stages | Plain JUnit 5 — feed a fixture `Mat` from `src/test/resources`, assert on output dimensions, channel count, detection labels. No JavaFX needed. |
| ImageUtils | Round-trip test: `Mat → WritableImage → Mat`, compare pixel buffers. |
| Rules | Table-driven test over `DecisionRule` × synthetic `Detection` lists. |
| UI | TestFX for smoke tests (window opens, Open Image enables Run, slider changes fire re-run). |
| Performance | Assert pipeline stays under a budget (e.g. 100 ms) on a fixed 1280×720 fixture; catches accidental full-resolution inference. |

---

## 16. Performance notes

1. **Downscale before inference.** Most detectors take 640×640. Feeding 4K wastes the whole budget on resize.
2. **Reuse `Mat` and `WritableImage` buffers** across frames instead of allocating per frame.
3. **Run recognition on every Nth frame** for live video (`s.getInferenceInterval()`), reusing previous boxes in between. 30 FPS preview with 6 FPS inference feels smooth and cuts CPU by 80%.
4. **Cap preview resolution** at the `ImageView` display size. Rendering a 4K image into a 480-px panel costs real time.
5. **GPU:** DJL picks up CUDA automatically if present; otherwise cap OpenCV threads with `Core.setNumThreads(n)` so the UI thread isn't starved.

---

## 17. Implementation order

| Step | Deliverable | Verifies |
| --- | --- | --- |
| 1 | Empty JavaFX window + Maven build | Toolchain, JavaFX modules resolve |
| 2 | Open image → display in `ImageView` | OpenCV natives, `Mat` → FX conversion |
| 3 | Preprocessing panel with live sliders | Property binding, debounce, re-run |
| 4 | Camera start/stop with preview | Threading, `Service` lifecycle, frame dropping |
| 5 | Feature extraction panel (Canny first) | Stage interface generalises |
| 6 | Model load + detections in `TableView` | Inference path, startup task |
| 7 | Canvas overlay with boxes | Coordinate scaling, layering |
| 8 | Decision rules + event log + CSV export | End-to-end value |
| 9 | jpackage installer | Deployment on a clean machine |

Steps 1–4 are roughly a day. Step 6 is where most surprises live — budget extra time for model format and native download issues on the first run.

---

## 18. Extension points

- **Multi-camera** — turn `CameraService` into a factory, one `PipelineRunner` per source, tabs per camera.
- **Recording** — `VideoWriter` fed from the annotated frame for evidence clips.
- **Persistence** — write detections to PostgreSQL for reporting and trend charts.
- **Remote model** — swap `ModelService` for an HTTP client hitting a Python inference server; the `PipelineStage` interface stays identical.
- **Custom training** — replace the COCO model with a fine-tuned one; only `ModelService.load()` and the class-filter list change.