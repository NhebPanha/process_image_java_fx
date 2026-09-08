Master Prompt: Traffic Light Image Processing System (JavaFX)
Act as a Senior Java Software Engineer, JavaFX Expert, Image Processing Engineer, and Operating Systems Lecturer.
Create a complete desktop application called:
"Traffic Light Vehicle Image Processing System"
Technology Requirements
•	Java 17+
•	JavaFX
•	Maven
•	OOP Principles
•	MVC Architecture
•	Multithreading
•	Thread Synchronization
•	Image Processing
•	JavaFX Image / ImageView
•	Well-documented code
•	Javadoc
•	CSS
•	FXML
•	PlantUML diagrams
The application must compile and run as a complete Maven JavaFX project without requiring modification.
________________________________________
1. PROJECT PURPOSE
The project demonstrates image processing and computer vision concepts using a traffic-light scenario.
Real-world scenario
A camera is positioned at a traffic light.
When a vehicle arrives at the traffic light, the user can:
1.	Take/import a picture.
2.	Process the image.
3.	Detect the vehicle.
4.	Classify the vehicle.
5.	Display the result.
6.	Show image-processing information.
7.	Record processing statistics.
The system should support these vehicle categories:
•	🚗 Car
•	🏍 Motorcycle
•	🚲 Bicycle
•	🛺 Tricycle
•	🛵 Moped
________________________________________
2. MAIN SYSTEM WORKFLOW
The application workflow should be:
Traffic Light
      ↓
Capture / Import Image
      ↓
Original Image
      ↓
Image Preprocessing
      ↓
Vehicle Detection
      ↓
Vehicle Classification
      ↓
Result
      ↓
Display Vehicle Type
      ↓
Display Confidence / Processing Information
      ↓
Save Result
Example:
Camera Image
     ↓
┌──────────────────────────┐
│      Traffic Light       │
│                          │
│        🚦                │
│                          │
│      🚗 Car             │
└──────────────────────────┘
             ↓
       Image Processing
             ↓
       Detected Vehicle
             ↓
          CAR
________________________________________
3. IMPORTANT FUNCTIONAL REQUIREMENT
The application must allow the user to select an image containing a vehicle near a traffic light.
The application should support:
•	Take Picture
•	Import Image
•	Drag & Drop Image
•	Process Image
•	Reset Image
•	Save Result
If direct camera access is implemented, provide a JavaFX camera integration layer.
If camera access is unavailable on the user's machine, provide an Import Image fallback.
________________________________________
4. VEHICLE TYPES
Create an enum:
public enum VehicleType {
    CAR,
    MOTORCYCLE,
    BICYCLE,
    TRICYCLE,
    MOPED,
    UNKNOWN
}
Each detected vehicle should contain:
•	Vehicle ID
•	Vehicle Type
•	Confidence
•	Detection Time
•	Processing Time
•	Image Path
•	Status
Example:
Vehicle ID: VEH-001
Type: Car
Confidence: 94.5%
Processing Time: 128 ms
Status: DETECTED
________________________________________
5. IMAGE PROCESSING PIPELINE
Implement a clear image-processing pipeline.
Step 1 — Load Image
Load the image using JavaFX:
Image image = new Image(...);
Display it using:
ImageView
________________________________________
Step 2 — Image Preprocessing
Implement image preprocessing operations such as:
•	Resize
•	Grayscale
•	Brightness adjustment
•	Contrast adjustment
•	Noise reduction
•	Image normalization
•	Sharpening
•	Edge detection
The user should be able to select processing operations.
Example:
Original
   ↓
Resize
   ↓
Grayscale
   ↓
Noise Reduction
   ↓
Edge Detection
   ↓
Vehicle Detection
________________________________________
6. IMAGE PROCESSING MODES
Provide selectable processing modes:
Original
Display the original image.
Grayscale
Convert the image to grayscale.
Edge Detection
Detect vehicle edges.
Threshold
Apply binary thresholding.
Blur
Apply Gaussian-style blur or another suitable smoothing technique.
Sharpen
Improve image details.
Contrast
Increase/decrease image contrast.
Vehicle Detection
Run the vehicle detection/classification process.
________________________________________
7. VEHICLE DETECTION
Create an abstraction:
public interface VehicleDetector {

    DetectionResult detect(BufferedImage image);
}
Implement:
vehicle
 ├── VehicleDetector.java
 ├── ImageProcessingVehicleDetector.java
 └── DetectionResult.java
The detector should classify:
CAR
MOTORCYCLE
BICYCLE
TRICYCLE
MOPED
UNKNOWN
________________________________________
8. DETECTION RESULT
Create:
public class DetectionResult {

    private VehicleType vehicleType;
    private double confidence;
    private long processingTime;
    private BoundingBox boundingBox;
}
Example result:
----------------------------------
        DETECTION RESULT
----------------------------------

Vehicle:       CAR
Confidence:    94.5%
Processing:    128 ms

Bounding Box:
X: 235
Y: 180
Width: 420
Height: 260

Status: DETECTED
----------------------------------
________________________________________
9. IMAGE PROCESSING THREAD
Image processing must not freeze the JavaFX UI.
Use a background thread:
JavaFX Application Thread
           │
           │ Start Processing
           ↓
    ImageProcessingTask
           │
           ↓
     Process Image
           │
           ↓
      Detection
           │
           ↓
    Update JavaFX UI
Use:
•	Task
•	ExecutorService
•	Future
•	Background threads
Never perform expensive image-processing operations directly on the JavaFX Application Thread.
________________________________________
10. THREAD STATES
Create:
public enum ThreadStatus {
    IDLE,
    PROCESSING,
    WAITING,
    COMPLETED,
    ERROR,
    STOPPED
}
Monitor:
•	Thread Name
•	Thread ID
•	State
•	Processing Status
•	Last Activity
•	Processing Time
________________________________________
11. MULTITHREADING
Implement a processing executor:
ExecutorService executorService;
Example:
Image Loader Thread
       │
       ├── Preprocessing
       │
       ├── Detection
       │
       └── Statistics
The UI must remain responsive while processing.
________________________________________
12. SYNCHRONIZATION
Create separate synchronization implementations.
sync
├── lock
│   └── ProcessingLock.java
│
└── semaphore
    └── ProcessingSemaphore.java
Option A — ReentrantLock
Use:
ReentrantLock
to protect shared processing statistics.
Option B — Semaphore
Use:
Semaphore
to limit concurrent image-processing operations.
Allow the user to select:
Synchronization Strategy

○ ReentrantLock
○ Semaphore
________________________________________
13. STATISTICS MANAGER
Create:
StatisticsManager
Calculate:
•	Total Images
•	Processed Images
•	Successful Detections
•	Failed Detections
•	Average Processing Time
•	Fastest Processing
•	Slowest Processing
•	Cars Detected
•	Motorcycles Detected
•	Bicycles Detected
•	Tricycles Detected
•	Mopeds Detected
Example:
Total Images        42
Processed           40
Successful          38
Failed              2

Average Time        132 ms

Cars                18
Motorcycles         10
Bicycles             5
Tricycles            3
Mopeds               2
________________________________________
14. JAVAFX DASHBOARD
Create a professional desktop dashboard.
Window:
1400 x 900
Theme:
Modern Dark Traffic Intelligence Dashboard
Design inspiration:
•	Smart Traffic Management System
•	Tesla Dashboard
•	Modern Analytics Dashboard
•	Computer Vision Dashboard
•	Smart City UI
Use:
•	Dark background
•	Glassmorphism cards
•	Rounded cards
•	Subtle shadows
•	Smooth animations
•	Modern typography
•	Icons
•	Progress indicators
•	Charts
________________________________________
15. TOP NAVIGATION BAR
Display:
🚦 Traffic Vision
Vehicle Image Processing System

                     ● SYSTEM READY

                     08:53:21
Components:
•	Application title
•	Current date/time
•	Processing status
•	Synchronization strategy
•	System status
________________________________________
16. LEFT CONTROL PANEL
Create controls:
Image
[ 📷 Take Picture ]

[ 🖼 Import Image ]

[ ↻ Reset ]
Processing
Processing Mode

[ Original       ▼ ]

☑ Resize
☑ Grayscale
☑ Noise Reduction
☑ Edge Detection
☑ Contrast Enhancement
Processing Parameters
Brightness
[──────●──────]

Contrast
[──────●──────]

Threshold
[──────●──────]
Detection
[ ▶ Process Image ]
________________________________________
17. CENTER PANEL — TRAFFIC LIGHT IMAGE
Create a large image-processing viewer.
Layout:
┌──────────────────────────────────────────────┐
│                                              │
│             TRAFFIC CAMERA                   │
│                                              │
│                  🚦                          │
│                                              │
│             ┌───────────────┐                │
│             │               │                │
│             │      CAR      │                │
│             │               │                │
│             └───────────────┘                │
│                                              │
└──────────────────────────────────────────────┘
Display:
•	Original image
•	Processed image
•	Bounding box
•	Vehicle label
•	Confidence percentage
Example overlay:
CAR
94.5%

[ x=235 y=180
  w=420 h=260 ]
________________________________________
18. IMAGE COMPARISON
Provide a before/after viewer.
┌──────────────────┬──────────────────┐
│ Original Image   │ Processed Image  │
│                  │                  │
│      🚗          │     🚗           │
│                  │                  │
└──────────────────┴──────────────────┘
Allow:
•	Side-by-side
•	Processed only
•	Original only
________________________________________
19. RIGHT PANEL — DETECTION RESULT
Create a large result card.
┌──────────────────────────────┐
│       DETECTION RESULT       │
├──────────────────────────────┤
│                              │
│             🚗               │
│                              │
│           CAR                │
│                              │
│       94.5% Confidence       │
│                              │
│ Processing: 128 ms           │
│                              │
│ Status: ✓ DETECTED           │
└──────────────────────────────┘
Use different visual indicators for each vehicle type.
________________________________________
20. VEHICLE STATISTICS CARDS
Display:
┌──────────────┐
│ 🚗 Cars      │
│     18       │
└──────────────┘

┌──────────────┐
│ 🏍 Motorcycles│
│     10       │
└──────────────┘

┌──────────────┐
│ 🚲 Bicycles  │
│      5       │
└──────────────┘

┌──────────────┐
│ 🛺 Tricycles │
│      3       │
└──────────────┘
________________________________________
21. PROCESSING PERFORMANCE
Create a live chart showing:
Processing Time (ms)
     │
200  │       ╭─╮
150  │   ╭───╯ ╰──╮
100  │───╯        ╰──
 50  │
     └──────────────────
        Images Processed
Use JavaFX:
LineChart
________________________________________
22. VEHICLE DISTRIBUTION CHART
Create a JavaFX:
PieChart
Example:
Vehicle Distribution

Car             45%
Motorcycle      25%
Bicycle         15%
Tricycle        10%
Moped            5%
________________________________________
23. PROCESSING PROGRESS
During processing display:
Processing Image...

[████████████████░░░░]

82%

Step:
Vehicle Detection
Processing stages:
1. Loading Image
2. Preprocessing
3. Grayscale
4. Edge Detection
5. Vehicle Detection
6. Classification
7. Result
________________________________________
24. BOTTOM PANEL — PROCESSING MONITOR
Create a TableView.
Columns:
Image ID
Vehicle Type
Confidence
Processing Time
Thread
Status
Timestamp
Example:
IMG-001 | CAR        | 94.5% | 128ms | Worker-1 | ✓
IMG-002 | MOTORCYCLE | 91.2% | 145ms | Worker-2 | ✓
IMG-003 | BICYCLE    | 88.7% | 112ms | Worker-1 | ✓
________________________________________
25. TRAFFIC LIGHT VISUALIZATION
Create a traffic light component.
States:
🔴 RED
🟡 YELLOW
🟢 GREEN
Example:
      ┌─────┐
      │ 🔴  │
      │     │
      │ 🟡  │
      │     │
      │ 🟢  │
      └─────┘
Allow the traffic light to animate.
Example cycle:
RED
 ↓
GREEN
 ↓
YELLOW
 ↓
RED
Display:
Traffic Light: RED
Waiting for vehicle image...
________________________________________
26. CAMERA SIMULATION
Create a simulated traffic-camera view.
Example:
┌──────────────────────────────────────────────┐
│ CAMERA 01                           LIVE ●   │
│                                              │
│                 🚦                           │
│                                              │
│         Vehicle approaching                 │
│                                              │
│              🚗                             │
│                                              │
│              [CAR]                           │
│                                              │
└──────────────────────────────────────────────┘
Add:
•	LIVE indicator
•	Camera ID
•	Timestamp
•	FPS
•	Processing status
________________________________________
27. MVC ARCHITECTURE
Use strict MVC.
Model
 ├── Vehicle.java
 ├── VehicleType.java
 ├── DetectionResult.java
 ├── BoundingBox.java
 └── ThreadStatus.java

View
 ├── DashboardView.fxml
 ├── dashboard.css
 └── components

Controller
 └── DashboardController.java

Service
 ├── ImageProcessingService.java
 ├── VehicleDetectionService.java
 ├── CameraService.java
 └── StatisticsService.java

Thread
 ├── ImageProcessingTask.java
 └── ProcessingWorker.java
________________________________________
28. PROJECT STRUCTURE
Create:
TrafficLightVehicleProcessing/
│
├── pom.xml
├── README.md
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── rupp/
│   │   │           └── trafficvision/
│   │   │
│   │   │               ├── MainApp.java
│   │   │
│   │   │               ├── model/
│   │   │               │   ├── Vehicle.java
│   │   │               │   ├── VehicleType.java
│   │   │               │   ├── DetectionResult.java
│   │   │               │   ├── BoundingBox.java
│   │   │               │   └── ThreadStatus.java
│   │   │
│   │   │               ├── controller/
│   │   │               │   └── DashboardController.java
│   │   │
│   │   │               ├── service/
│   │   │               │   ├── ImageProcessingService.java
│   │   │               │   ├── VehicleDetectionService.java
│   │   │               │   ├── CameraService.java
│   │   │               │   └── StatisticsService.java
│   │   │
│   │   │               ├── processing/
│   │   │               │   ├── GrayscaleProcessor.java
│   │   │               │   ├── EdgeDetectionProcessor.java
│   │   │               │   ├── BlurProcessor.java
│   │   │               │   ├── ThresholdProcessor.java
│   │   │               │   └── ContrastProcessor.java
│   │   │
│   │   │               ├── detection/
│   │   │               │   ├── VehicleDetector.java
│   │   │               │   ├── ImageProcessingVehicleDetector.java
│   │   │               │   └── DetectionResult.java
│   │   │
│   │   │               ├── thread/
│   │   │               │   ├── ImageProcessingTask.java
│   │   │               │   └── ProcessingWorker.java
│   │   │
│   │   │               ├── sync/
│   │   │               │   ├── SynchronizationStrategy.java
│   │   │               │   ├── lock/
│   │   │               │   │   └── ProcessingLock.java
│   │   │               │   └── semaphore/
│   │   │               │       └── ProcessingSemaphore.java
│   │   │
│   │   │               ├── statistics/
│   │   │               │   └── StatisticsManager.java
│   │   │
│   │   │               └── util/
│   │   │                   ├── Constants.java
│   │   │                   ├── LoggerUtil.java
│   │   │                   └── TimeUtil.java
│   │   │
│   │   └── resources/
│   │       └── com/
│   │           └── rupp/
│   │               └── trafficvision/
│   │
│   │               ├── view/
│   │               │   └── DashboardView.fxml
│   │               │
│   │               ├── css/
│   │               │   └── dashboard.css
│   │               │
│   │               └── images/
│   │
│   └── test/
│       └── java/
│
├── docs/
│   ├── architecture.puml
│   ├── class-diagram.puml
│   ├── sequence-diagram.puml
│   ├── use-case.puml
│   └── report.md
│
└── README.md
________________________________________
29. IMAGE PROCESSING ALGORITHM
Implement image processing using Java-compatible image APIs.
Do not make the project dependent on an external AI API.
The basic implementation should work locally.
Implement:
BufferedImage
     ↓
Pixel Processing
     ↓
Color Analysis
     ↓
Grayscale
     ↓
Edge Detection
     ↓
Shape / Region Analysis
     ↓
Vehicle Classification
For the educational version, clearly explain that traditional image-processing classification is a simplified simulation and is not equivalent to a production deep-learning object detector.
Create a clean abstraction so a machine-learning detector can later replace the educational detector.
________________________________________
30. OPTIONAL AI / COMPUTER VISION EXTENSION
Design the architecture so that a future detector can use:
•	OpenCV
•	YOLO
•	TensorFlow
•	ONNX Runtime
without changing the JavaFX UI.
For example:
VehicleDetector detector;
Possible implementations:
ImageProcessingVehicleDetector
OpenCVVehicleDetector
YOLOVehicleDetector
The GUI should depend on the interface rather than a specific detector.
________________________________________
31. ERROR HANDLING
Handle:
•	Invalid image
•	Unsupported image format
•	Missing image
•	Corrupted image
•	Processing failure
•	Thread interruption
•	Executor shutdown
•	Invalid processing parameters
•	Camera unavailable
•	File access errors
•	Unexpected exceptions
Show JavaFX alerts:
⚠ Processing Error

Unable to process the selected image.

Please select another image.
________________________________________
32. REAL-TIME UI UPDATES
Update:
•	Processing progress
•	Current vehicle
•	Confidence
•	Processing time
•	Thread status
•	Statistics
•	Charts
•	Traffic light state
•	Current time
Use:
Platform.runLater(...)
when updating JavaFX controls from background threads.
________________________________________
33. ANIMATIONS
Implement smooth JavaFX animations.
Examples:
Vehicle Detection
Image
 ↓
Bounding Box fades in
 ↓
Vehicle label appears
 ↓
Confidence counter animates
Processing
Progress bar animation
Traffic Light
Red → Green → Yellow → Red
Detection Result
Use:
•	FadeTransition
•	ScaleTransition
•	TranslateTransition
________________________________________
34. SAVE RESULT
Provide:
[ Save Result ]
Save:
•	Original image
•	Processed image
•	Bounding box
•	Detection label
•	Confidence
•	Timestamp
Example filename:
vehicle_detection_20260908_085321.png
________________________________________
35. OPERATING SYSTEM / THREADING REPORT
Generate an educational report explaining:
1. Process vs Thread
Explain the difference.
2. JavaFX Application Thread
Explain why UI operations must run on the JavaFX Application Thread.
3. Background Threads
Explain why image processing should run outside the UI thread.
4. Critical Section
Identify shared statistics and processing resources.
5. Mutex
Explain mutual exclusion.
6. Semaphore
Explain limiting concurrent processing.
7. ReentrantLock
Explain explicit locking.
8. Race Condition
Demonstrate how shared statistics can suffer from race conditions.
9. Thread Lifecycle
Explain:
NEW
 ↓
RUNNABLE
 ↓
RUNNING
 ↓
WAITING / BLOCKED
 ↓
TERMINATED
10. Deadlock Prevention
Explain:
•	Consistent lock ordering
•	Timeout-based locking
•	Avoid nested locks
•	Proper finally blocks
•	Executor shutdown
•	Interrupt handling
________________________________________
36. CLASS DIAGRAM
Generate a complete PlantUML class diagram.
Show relationships between:
DashboardController
ImageProcessingService
VehicleDetector
DetectionResult
Vehicle
StatisticsManager
ImageProcessingTask
SynchronizationStrategy
ProcessingLock
ProcessingSemaphore
________________________________________
37. SEQUENCE DIAGRAM
Generate a PlantUML sequence diagram showing:
User
 ↓
DashboardController
 ↓
ImageProcessingService
 ↓
Background Thread
 ↓
Image Processor
 ↓
VehicleDetector
 ↓
StatisticsManager
 ↓
DashboardController
 ↓
JavaFX UI
________________________________________
38. USE CASE DIAGRAM
Actors:
User
Use cases:
Import Image
Take Picture
Process Image
Select Processing Mode
Detect Vehicle
View Result
View Statistics
Save Result
Reset
Change Synchronization Strategy
View Thread Monitor
________________________________________
39. README.md
Generate a complete README containing:
•	Project title
•	Description
•	Features
•	Technologies
•	Architecture
•	Project structure
•	Installation
•	Maven commands
•	Running instructions
•	Image processing explanation
•	Threading explanation
•	Synchronization explanation
•	Screenshots section
•	Future improvements
•	Author
•	Academic purpose
Commands:
mvn clean javafx:run
and:
mvn clean package
________________________________________
40. PRESENTATION SLIDES
Generate 10 presentation slides.
Slide 1 — Title
Traffic Light Vehicle Image Processing System
JavaFX + Java 17+
________________________________________
Slide 2 — Problem Statement
Explain:
•	Traffic monitoring
•	Vehicle identification
•	Image processing
•	Need for automated analysis
________________________________________
Slide 3 — System Architecture
Show:
Camera
 ↓
Image Processing
 ↓
Vehicle Detection
 ↓
Classification
 ↓
Statistics
 ↓
JavaFX Dashboard
________________________________________
Slide 4 — Class Diagram
Show the major classes and relationships.
________________________________________
Slide 5 — Image Processing
Explain:
Original
 ↓
Preprocessing
 ↓
Grayscale
 ↓
Edge Detection
 ↓
Vehicle Detection
 ↓
Classification
________________________________________
Slide 6 — Multithreading & Synchronization
Explain:
•	JavaFX Application Thread
•	Background processing
•	ExecutorService
•	ReentrantLock
•	Semaphore
•	Race condition prevention
________________________________________
Slide 7 — GUI Design
Show:
•	Traffic camera
•	Original/processed image
•	Detection result
•	Statistics
•	Charts
•	Thread monitor
________________________________________
Slide 8 — Results
Show example:
Vehicle: CAR
Confidence: 94.5%
Processing: 128 ms
Status: DETECTED
________________________________________
Slide 9 — Challenges
Discuss:
•	Image quality
•	Lighting
•	Vehicle overlap
•	Processing performance
•	Thread synchronization
•	JavaFX UI updates
•	Camera integration
________________________________________
Slide 10 — Conclusion
Explain:
•	Image processing concepts
•	JavaFX development
•	Multithreading
•	Synchronization
•	MVC
•	Future AI/YOLO integration
________________________________________
41. AI APPENDIX
Generate a separate AI Usage Appendix.
Include:
1. AI Tools Used
Example:
ChatGPT
2. Prompts Used
Document the important prompts used during development.
3. Accepted Code
Explain which AI-generated code was accepted and why.
4. Rejected Code
Explain which generated approaches were rejected and why.
5. Human Modifications
Explain what the developer changed.
6. Testing
Document:
•	Compilation
•	Runtime testing
•	Image testing
•	Thread testing
•	Error testing
7. Reflection
Explain:
•	What AI helped with
•	What AI could not solve
•	How generated code was verified
•	Lessons learned
________________________________________
42. CODE QUALITY
The source code must follow:
•	SOLID principles
•	Encapsulation
•	Abstraction
•	Inheritance where appropriate
•	Polymorphism
•	Separation of concerns
•	Dependency inversion
•	Meaningful naming
•	Small methods
•	No duplicated logic
Every public class and important public method must contain Javadoc.
Example:
/**
 * Processes an image and attempts to classify
 * the vehicle contained within the image.
 *
 * @param image source image
 * @return vehicle detection result
 */
public DetectionResult detect(BufferedImage image) {
    ...
}
________________________________________
43. IMPORTANT IMPLEMENTATION RULES
Do NOT:
•	Freeze the JavaFX UI
•	Perform heavy processing directly inside event handlers
•	Access JavaFX controls from worker threads
•	Use global mutable state unnecessarily
•	Hard-code UI values throughout controllers
•	Put all application logic inside DashboardController
•	Use deprecated Java APIs
•	Ignore thread interruption
•	Leave executor threads running after application exit
Use:
•	JavaFX Task
•	ExecutorService
•	Platform.runLater()
•	ReentrantLock
•	Semaphore
•	Interfaces
•	MVC
•	Service classes
________________________________________
44. FINAL DELIVERABLES
Provide the complete project:
1.	pom.xml
2.	Full Maven project structure
3.	Complete Java source code
4.	FXML
5.	CSS
6.	Image-processing classes
7.	Vehicle detection classes
8.	Multithreading implementation
9.	ReentrantLock implementation
10.	Semaphore implementation
11.	Statistics system
12.	Traffic light component
13.	JavaFX dashboard
14.	PlantUML class diagram
15.	PlantUML sequence diagram
16.	PlantUML use-case diagram
17.	README.md
18.	Operating Systems report
19.	Presentation slides content
20.	AI Appendix
21.	Javadocs
The final application must be a professional JavaFX desktop application, not a simple console program.
The application must clearly demonstrate the relationship between:
TRAFFIC LIGHT
      ↓
CAMERA / IMAGE
      ↓
IMAGE PROCESSING
      ↓
VEHICLE DETECTION
      ↓
CLASSIFICATION
      ↓
CAR / MOTORCYCLE / BICYCLE / TRICYCLE / MOPED
      ↓
STATISTICS
      ↓
JAVA FX DASHBOARD
The primary academic focus is:
Image Processing + JavaFX + OOP + MVC + Multithreading + Synchronization.

