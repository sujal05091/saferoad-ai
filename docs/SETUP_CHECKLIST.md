# SafeRoadAI - Setup Checklist ✅

## Required Setup Steps

### 1. TensorFlow Lite Models ⚠️ REQUIRED
**Status**: ❌ **NOT PROVIDED** - You must train/obtain these

**Location**: `app/src/main/assets/models/`

**Files Needed**:
- [ ] `traffic_signs.tflite` (Traffic sign detection model)
- [ ] `road_hazards.tflite` (Pothole/hazard detection model)

**How to Get**:
1. Follow instructions in `docs/TRAINING_AND_EXPORT.md`
2. Train YOLOv8 models using Google Colab (free GPU)
3. Convert to TFLite format
4. Copy `.tflite` files to assets folder

**Without these files**: App will start but detection will not work. Error messages will appear in logs.

---

### 2. Firebase Configuration ⚠️ REQUIRED FOR CLOUD FEATURES
**Status**: ❌ **NOT PROVIDED**

**Location**: `app/google-services.json`

**Steps**:
1. [ ] Go to [Firebase Console](https://console.firebase.google.com)
2. [ ] Create new project or use existing
3. [ ] Add Android app with package name: `com.saferoadai`
4. [ ] Download `google-services.json`
5. [ ] Place in `app/` directory (same level as `build.gradle.kts`)

**Services to Enable**:
- [ ] Firebase Authentication (Email/Password + Google Sign-In)
- [ ] Cloud Firestore (for hazard storage)
- [ ] Firebase Storage (optional, for image upload)

**Without this file**: App will crash on startup due to missing Firebase config.

**Workaround**: Comment out Firebase initialization in code if you don't need cloud features.

---

### 3. Google Maps API Key ⚠️ REQUIRED FOR DASHBOARD
**Status**: ❌ **NOT PROVIDED**

**Location**: `app/src/main/AndroidManifest.xml`

**Steps**:
1. [ ] Go to [Google Cloud Console](https://console.cloud.google.com)
2. [ ] Enable "Maps SDK for Android"
3. [ ] Create API Key
4. [ ] Restrict key to Android apps with SHA-1 fingerprint
5. [ ] Open `AndroidManifest.xml`
6. [ ] Replace `YOUR_GOOGLE_MAPS_API_KEY_HERE` with actual key:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" />
```

**Without this key**: Maps will show "For development purposes only" watermark and may not load.

---

### 4. Lottie Animation Files (Optional)
**Status**: ⚠️ **RECOMMENDED**

**Location**: `app/src/main/res/raw/`

**Files**:
- [ ] `anim_splash.json` - Splash screen logo animation
- [ ] `anim_detecting.json` - Detection active indicator
- [ ] `anim_success.json` - Success animation (upload/fix)
- [ ] `anim_warning.json` - Drowsiness warning animation

**Where to Get**:
- [LottieFiles.com](https://lottiefiles.com) - Free animations
- Search for: car, road, safety, warning, checkmark

**Without these files**: App will work but without animations (fallback to static images).

---

### 5. Permissions (Granted at Runtime)
**Status**: ✅ **HANDLED IN CODE**

App will request these permissions on first launch:
- [x] `CAMERA` - For front/rear camera access
- [x] `ACCESS_FINE_LOCATION` - For GPS coordinates
- [x] `ACCESS_COARSE_LOCATION` - Fallback location
- [x] `INTERNET` - For Firebase sync
- [x] `ACCESS_NETWORK_STATE` - Network status check

**Note**: Declared in `AndroidManifest.xml`, requested in Activities.

---

### 6. Build & Dependencies
**Status**: ✅ **CONFIGURED**

All dependencies are specified in `app/build.gradle.kts`:
- [x] CameraX 1.3.1
- [x] TensorFlow Lite 2.14.0
- [x] ML Kit Face Detection
- [x] Firebase SDKs
- [x] Google Maps SDK
- [x] Room Database 2.6.1
- [x] Lottie 6.2.0

**First Build**: May take 5-10 minutes to download all dependencies.

---

## Post-Setup Verification

### Test Checklist

1. **Build Success**
   ```bash
   ./gradlew build
   ```
   - [ ] Build completes without errors

2. **App Launch**
   - [ ] Splash screen appears
   - [ ] No crashes on launch
   - [ ] Auth screen appears after splash

3. **Permissions**
   - [ ] Camera permission dialog appears
   - [ ] Location permission dialog appears
   - [ ] App continues after granting permissions

4. **Firebase (if configured)**
   - [ ] Can create account with email/password
   - [ ] Can sign in with Google
   - [ ] Check Firebase Console for new user

5. **Camera**
   - [ ] Front camera preview works
   - [ ] Rear camera preview works
   - [ ] Dual camera mode works (or gracefully falls back)

6. **Detection (if models present)**
   - [ ] No "Model not found" errors in logs
   - [ ] Detection overlay appears
   - [ ] Bounding boxes drawn on objects

7. **Maps (if API key set)**
   - [ ] Dashboard shows map (not blank)
   - [ ] No "For development purposes" watermark
   - [ ] Can add markers and see them

8. **TTS Alerts**
   - [ ] Voice alerts play when detection occurs
   - [ ] Alerts respect rate limiting (not spamming)
   - [ ] Can disable in Settings

---

## Optional Enhancements

### 1. Custom App Icon
**Location**: `app/src/main/res/mipmap-*/`

- [ ] Replace `ic_launcher.png` with custom icon
- [ ] Use Android Asset Studio or Image Asset wizard

### 2. App Signing (for Release)
**Location**: `app/build.gradle.kts`

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("your-keystore.jks")
        storePassword = "your-password"
        keyAlias = "your-alias"
        keyPassword = "your-password"
    }
}
```

### 3. ProGuard Rules (for Release)
**Location**: `app/proguard-rules.pro`

- [ ] Add TFLite keep rules
- [ ] Add Firebase keep rules
- [ ] Test release build

---

## Known Issues & Workarounds

### Issue: "Execution failed for task ':app:processDebugGoogleServices'"
**Cause**: Missing `google-services.json`  
**Fix**: Add Firebase config file OR disable Firebase plugin in `build.gradle.kts`

### Issue: Maps show blank or gray tiles
**Cause**: Invalid API key or missing API key  
**Fix**: Verify API key in manifest, check key restrictions in Cloud Console

### Issue: TFLite model loading fails
**Cause**: Model file missing or wrong path  
**Fix**: Verify `.tflite` files are in `assets/models/` folder

### Issue: Dual camera not working
**Cause**: Device doesn't support concurrent cameras  
**Fix**: App will automatically fallback to single camera (expected behavior)

### Issue: High battery drain
**Cause**: Detection running at high FPS  
**Fix**: Go to Settings → Detection FPS → Change to 5 FPS

---

## File Structure Summary

```
SafeRoadAI/
├── app/
│   ├── google-services.json ⚠️ ADD THIS
│   ├── build.gradle.kts ✅ CONFIGURED
│   └── src/main/
│       ├── AndroidManifest.xml ⚠️ ADD MAPS KEY
│       ├── assets/
│       │   └── models/
│       │       ├── traffic_signs.tflite ⚠️ ADD THIS
│       │       └── road_hazards.tflite ⚠️ ADD THIS
│       ├── res/
│       │   └── raw/
│       │       ├── anim_splash.json 📦 OPTIONAL
│       │       ├── anim_detecting.json 📦 OPTIONAL
│       │       ├── anim_success.json 📦 OPTIONAL
│       │       └── anim_warning.json 📦 OPTIONAL
│       └── java/com/saferoadai/ ✅ ALL CODE GENERATED
├── docs/
│   ├── README.md ✅ COMPLETE
│   ├── TRAINING_AND_EXPORT.md ✅ COMPLETE
│   └── SETUP_CHECKLIST.md ✅ THIS FILE
└── build.gradle.kts ✅ CONFIGURED
```

---

## Quick Start Command

```bash
# 1. Clone/open project in Android Studio
# 2. Add required files (see above)
# 3. Sync Gradle
# 4. Build and run

./gradlew clean build
./gradlew installDebug

# Or simply click "Run" button in Android Studio
```

---

## Support & Resources

- **GitHub Issues**: [Report bugs or request features]
- **Documentation**: Check `docs/` folder
- **Training Guide**: See `TRAINING_AND_EXPORT.md`
- **Test Plan**: See `TEST_PLAN.md` (when available)

---

## Completion Status

**Core Code**: ✅ 100% Complete  
**Documentation**: ✅ 100% Complete  
**Required External Files**: ❌ 0% Complete (you must add)  

**Estimated Setup Time**: 
- With models ready: 15-30 minutes
- Training models from scratch: 2-4 hours
- Total end-to-end: 2-4 hours

---

**Next Steps**: 
1. Train or obtain TFLite models
2. Set up Firebase project
3. Get Google Maps API key
4. Add all files to project
5. Build and test!

Good luck! 🚀
