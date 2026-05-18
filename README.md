# SafeRoadAI 🚗🤖

**Affordable dual-camera Android app for driver drowsiness detection, road hazard & traffic sign detection, and crowdsourced road-quality dashboard.**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)

---

## 🎯 Project Overview

SafeRoadAI is a production-ready Android application that leverages AI and computer vision to enhance road safety through:

1. **Driver Drowsiness Detection** - Real-time face monitoring using MediaPipe/ML Kit to detect eye closure (EAR), yawning (MAR), and head pose
2. **Road Hazard Detection** - TensorFlow Lite models detect potholes, cracks, debris, and road damage
3. **Traffic Sign Recognition** - Automated detection and classification of traffic signs
4. **Smart Dashboard** - Google Maps-based crowdsourced hazard visualization with clustering and filtering
5. **Cloud Sync** - Firebase Firestore integration for real-time hazard sharing across users
6. **Local Storage** - Room database for offline-first functionality
7. **Voice Alerts** - Context-aware TTS notifications with intelligent rate limiting

---

## ✨ Key Features

### 🎥 Dual Camera Support
- **Front Camera**: Face monitoring for drowsiness detection
- **Rear Camera**: Road surface and traffic sign detection
- **Simultaneous Mode**: Use both cameras on supported devices
- **Graceful Fallback**: Auto-fallback to single camera on older devices

### 🧠 AI-Powered Detection
- **TensorFlow Lite**: On-device inference for road hazards and traffic signs
- **ML Kit Face Detection**: Real-time facial landmark tracking
- **MediaPipe Ready**: Can be swapped for more accurate face mesh
- **Optimized Performance**: Configurable FPS (5-15) for battery efficiency

### 📊 Smart Dashboard
- **Real-time Map**: Google Maps integration with custom markers
- **Hazard Clustering**: Automatic grouping of nearby hazards
- **Status Filtering**: View Active/Fixed/All hazards
- **Crowdsourced Data**: See hazards reported by other users
- **Offline Support**: Cache and sync when network available

### 🔔 Intelligent Alerts
- **Voice Alerts**: TTS announcements for hazards and drowsiness
- **Rate Limiting**: Prevents alert fatigue with smart cooldowns
- **Priority Levels**: Critical alerts interrupt, normal alerts queue
- **Customizable**: Toggle alerts on/off in settings

---

## 🏗️ Architecture

```
SafeRoadAI/
├── activities/          # UI layer (Splash, Auth, Main, Ride, Dashboard, History, Settings)
├── camera/             # CameraX manager with dual-camera support
├── detectors/          # TFLite base class and specific detectors
├── face/               # Face monitoring analyzer (drowsiness detection)
├── audio/              # Alert manager with TTS
├── location/           # GPS location manager with distance calculations
├── firebase/           # Cloud sync and real-time streaming
├── db/                 # Room database (entities, DAOs, AppDatabase)
└── util/               # Utility functions and helpers
```

### Technology Stack
- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34
- **Camera**: CameraX 1.3.1
- **AI/ML**: TensorFlow Lite 2.14.0, ML Kit, MediaPipe
- **Database**: Room 2.6.1
- **Cloud**: Firebase (Auth, Firestore, Storage)
- **Maps**: Google Maps SDK 18.2.0
- **UI**: Jetpack Compose + XML layouts, Lottie animations
- **Concurrency**: Kotlin Coroutines, Flow

---

## 🚀 Quick Start

### Prerequisites
1. **Android Studio** (Electric Eel or newer)
2. **Android device or emulator** with:
   - Android 8.0+ (API 26+)
   - Camera(s)
   - GPS
   - 2GB+ RAM recommended

### Installation Steps

#### 1. Clone Repository
```bash
git clone <your-repo-url>
cd SafeRoadAI
```

#### 2. Add Required Files

**A. TFLite Models** (Required)
Place your trained models in `app/src/main/assets/models/`:
- `traffic_signs.tflite` - Traffic sign detection model
- `road_hazards.tflite` - Pothole/hazard detection model

> **Note**: Model files are NOT included. See [TRAINING_AND_EXPORT.md](docs/TRAINING_AND_EXPORT.md) for training instructions.

**B. Firebase Configuration** (Required for cloud features)
1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
2. Add Android app with package name: `com.saferoadai`
3. Download `google-services.json`
4. Place in `app/` directory

**C. Google Maps API Key** (Required for dashboard)
1. Get API key from [Google Cloud Console](https://console.cloud.google.com)
2. Enable Maps SDK for Android
3. Open `app/src/main/AndroidManifest.xml`
4. Replace `YOUR_GOOGLE_MAPS_API_KEY_HERE` with your actual key:
```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_ACTUAL_API_KEY_HERE" />
```

#### 3. Build & Run
```bash
# Open in Android Studio
# File → Open → Select SafeRoadAI folder

# Or via command line:
./gradlew assembleDebug
./gradlew installDebug
```

---

## 📱 Usage

### First Launch
1. **Splash Screen** - App logo animation (Lottie)
2. **Authentication** - Email/password or Google Sign-In
3. **Permissions** - Grant Camera, Location, Storage permissions

### Start a Ride
1. Tap **"Start Ride"** from home screen
2. Select camera mode:
   - **Front Only**: Drowsiness detection
   - **Rear Only**: Road hazard detection
   - **Dual**: Both (if supported)
3. Press **START** button
4. Drive safely while app monitors:
   - Face for drowsiness signs
   - Road for hazards and traffic signs
5. Receive voice alerts for detections
6. Press **STOP** to end ride

### View Dashboard
1. Tap **"Dashboard"** from home
2. View map with hazard markers:
   - 🔴 Red: Potholes
   - 🟡 Yellow: Road damage
   - 🟢 Green: Fixed hazards
   - 🔵 Blue: Traffic signs
3. Filter by status (Active/Fixed/All)
4. Tap marker for details
5. Mark hazards as fixed

### Settings
- **Voice Alerts**: Enable/disable TTS
- **Firebase Upload**: Auto-upload detections
- **Camera Mode**: Default camera selection
- **Detection FPS**: Adjust sampling rate (5/10/15 FPS)
- **Battery Saver**: Reduce performance for longer battery life

---

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

Key test files:
- `EARCalculationTest.kt` - Eye aspect ratio calculations
- `DistanceTest.kt` - GPS distance calculations
- `HazardDeduplicationTest.kt` - Duplicate hazard filtering

### Instrumentation Tests
```bash
./gradlew connectedAndroidTest
```

### Field Testing
See [TEST_PLAN.md](docs/TEST_PLAN.md) for comprehensive testing procedures:
- Drowsiness detection scenarios
- Road hazard detection accuracy
- GPS accuracy and hazard localization
- Network sync and offline behavior
- Battery consumption benchmarks

---

## 📂 Project Structure

### Core Modules

#### Detection System
- **TFLiteDetector.kt**: Base class for TFLite models with NMS
- **TrafficSignDetector.kt**: 40+ traffic sign classes with importance scoring
- **RoadHazardDetector.kt**: Pothole, crack, debris detection with severity assessment
- **FaceMonitorAnalyzer.kt**: EAR, MAR, head pose calculation for drowsiness

#### Data Layer
- **AppDatabase.kt**: Room database singleton
- **HazardEvent.kt**: Road hazard entity
- **SignEvent.kt**: Traffic sign entity
- **DrowsinessEvent.kt**: Drowsiness detection entity
- **EventDao.kt**: Data access object with queries and flows

#### Managers
- **CameraXManager.kt**: Dual-camera orchestration
- **LocationManager.kt**: GPS with Haversine distance calculations
- **FirebaseManager.kt**: Cloud sync and real-time streaming
- **AlertManager.kt**: TTS with rate limiting

### Activities
- **SplashActivity**: Animated splash screen with Lottie
- **AuthActivity**: Firebase authentication (email + Google)
- **MainActivity**: Home screen with navigation
- **RideActivity**: Main detection screen with camera previews
- **DashboardActivity**: Google Maps with hazard markers
- **HistoryActivity**: Past rides and detections
- **SettingsActivity**: App configuration with DataStore

---

## 🎨 UI/UX Design

### Lottie Animations
Place JSON files in `app/src/main/res/raw/`:
- `anim_splash.json` - Splash logo animation
- `anim_detecting.json` - Active detection indicator
- `anim_success.json` - Upload/fix success
- `anim_warning.json` - Drowsiness alert

### Themes
- **Light Mode**: High contrast for daylight driving
- **Dark Mode**: Reduced eye strain for night driving
- **Accessibility**: Large fonts, TTS-friendly messages

---

## 🔧 Configuration

### Performance Tuning
Edit in `SettingsActivity` or directly in code:

```kotlin
// Detection FPS (lower = better battery)
const val INFERENCE_FPS = 10  // 5, 10, or 15

// Camera resolution
val CAMERA_RESOLUTION = Size(1280, 720)  // 720p default

// Alert cooldowns
const val MIN_ALERT_INTERVAL_MS = 5000L  // 5 seconds

// GPS update frequency
const val UPDATE_INTERVAL = 5000L  // 5 seconds
```

### Model Configuration
Adjust detection thresholds in detector classes:

```kotlin
// TFLiteDetector.kt
private const val CONFIDENCE_THRESHOLD = 0.25f
private const val IOU_THRESHOLD = 0.45f

// FaceMonitorAnalyzer.kt
private const val EAR_THRESHOLD = 0.21f
private const val MAR_THRESHOLD = 0.6f
```

---

## 📊 Data Privacy

SafeRoadAI is designed with privacy in mind:

- **Local-First**: All detections stored locally in Room DB
- **Opt-In Cloud Sync**: Firebase upload requires explicit user consent
- **Anonymous Mode**: Can use app without authentication (no cloud features)
- **No Image Upload**: Only metadata (location, type, timestamp) uploaded by default
- **User Control**: Delete local data anytime from Settings

### Permissions
- **CAMERA**: Required for detection
- **ACCESS_FINE_LOCATION**: Required for GPS coordinates
- **INTERNET**: Required for Firebase sync (optional)
- **WAKE_LOCK**: Keeps screen on during ride

---

## 🛠️ Troubleshooting

### Common Issues

#### 1. Models Not Found
**Error**: `Failed to load TFLite model`  
**Solution**: Place `.tflite` files in `app/src/main/assets/models/`

#### 2. Firebase Errors
**Error**: `google-services.json missing`  
**Solution**: Add Firebase config file to `app/` directory

#### 3. Maps Not Showing
**Error**: Blank map or "For development purposes only"  
**Solution**: Add valid Google Maps API key in `AndroidManifest.xml`

#### 4. Camera Not Starting
**Error**: `Failed to bind use cases`  
**Solution**: Grant camera permission, try single camera mode instead of dual

#### 5. TTS Not Working
**Error**: Silent alerts  
**Solution**: Check Settings → Voice Alerts enabled, verify TTS language pack installed

### Debug Mode
Enable detailed logging:
```kotlin
// In any file
android.util.Log.d("SafeRoadAI", "Debug message")
```

View logs:
```bash
adb logcat -s TFLiteDetector CameraXManager FaceMonitorAnalyzer
```

---

## 📚 Documentation

- **[TRAINING_AND_EXPORT.md](docs/TRAINING_AND_EXPORT.md)** - Model training guide (YOLOv8 → TFLite conversion)
- **[TEST_PLAN.md](docs/TEST_PLAN.md)** - Field testing procedures and test cases
- **[API_REFERENCE.md](docs/API_REFERENCE.md)** - Code documentation for developers
- **[HACKATHON_NOTES.md](docs/HACKATHON_NOTES.md)** - INNOVEX presentation notes

---

## 🤝 Contributing

Contributions welcome! Please:
1. Fork the repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

---

## 📝 License

This project is licensed under the MIT License - see [LICENSE](LICENSE) file for details.

---

## 👥 Team

**SafeRoadAI Development Team**
- Lead Developer: [Your Name]
- AI/ML Engineer: [Name]
- Mobile Engineer: [Name]
- UX Designer: [Name]

---

## 🙏 Acknowledgments

- **TensorFlow Lite** - On-device ML inference
- **Google ML Kit** - Face detection
- **MediaPipe** - Facial landmark tracking
- **Firebase** - Cloud infrastructure
- **CameraX** - Modern camera API
- **Lottie** - Smooth animations
- **OpenCV** - Image processing utilities

---

## 📧 Contact

Project Link: [https://github.com/yourusername/SafeRoadAI](https://github.com/yourusername/SafeRoadAI)

For questions or support: support@saferoadai.com

---

## 🗺️ Roadmap

### Phase 1 (Current)
- ✅ Core detection system
- ✅ Dual camera support
- ✅ Firebase cloud sync
- ✅ Google Maps dashboard

### Phase 2 (Planned)
- [ ] Improved model accuracy (retrain with more data)
- [ ] Offline maps caching
- [ ] Route hazard prediction
- [ ] Driver analytics dashboard
- [ ] Multi-language support

### Phase 3 (Future)
- [ ] Fleet management features
- [ ] Insurance integration
- [ ] Government road authority API
- [ ] AR hazard overlay
- [ ] Wearable device integration

---

**Made with ❤️ for safer roads**
