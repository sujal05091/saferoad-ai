# 🔧 Update Traffic Sign Labels

## ⚠️ IMPORTANT: Match Your Training Classes

Your traffic sign model has **5 classes**. You need to update the labels to match what you actually trained.

---

## 📝 Step 1: Check Your Training Notebook

Open your `traffic_minidata.ipynb` notebook and find **Step 4** where you defined `CLASS_NAMES`:

```python
CLASS_NAMES = [
    "Stop Sign",
    "Speed Limit 50",
    "Speed Limit 80",
    "No Entry",
    "Yield"
]
```

**Copy these exact class names!**

---

## 📝 Step 2: Update TrafficSignDetector.kt

File: `app/src/main/java/com/saferoadai/detectors/TrafficSignDetector.kt`

Find this section (around line 20-27):

```kotlin
private val TRAFFIC_SIGN_LABELS = mapOf(
    0 to "Stop Sign",
    1 to "No Parking",
    2 to "No U-Turn",
    3 to "Speed Limit",
    4 to "Two Way Traffic",
```

**Replace with YOUR actual class names from training**, matching the order:
- Class 0 = First class in your CLASS_NAMES list
- Class 1 = Second class in your CLASS_NAMES list
- etc.

### Example:
If your training used:
```python
CLASS_NAMES = [
    "Stop Sign",
    "Speed Limit 50", 
    "No Entry",
    "No Parking",
    "Yield"
]
```

Then update to:
```kotlin
private val TRAFFIC_SIGN_LABELS = mapOf(
    0 to "Stop Sign",
    1 to "Speed Limit 50",
    2 to "No Entry",
    3 to "No Parking",
    4 to "Yield"
)
```

---

## ✅ What's Already Fixed

### 1. Model Configuration
- ✅ Input size: 640x640
- ✅ Number of classes: 5
- ✅ Confidence threshold: 0.15 (good for demo models)

### 2. Speed Limit Detection
- ✅ **Only shows when moving** (speed > 1 m/s / 3.6 km/h)
- ✅ Other signs show regardless of speed
- ✅ Checks GPS speed data

### 3. Pothole Detection  
- ✅ Input size: 640x640
- ✅ Number of classes: 1 (pothole only)
- ✅ Always active

---

## 🧪 Testing

After updating class labels:

1. **Rebuild app**: `.\gradlew assembleDebug`
2. **Install new APK**
3. **Test each sign type**:
   - Point camera at trained sign types
   - Check if correct label appears
   - For speed limits: walk/drive to trigger

### Expected Behavior:

| Sign Type | When Stationary | When Moving |
|-----------|----------------|-------------|
| **Speed Limit** | ❌ Not shown | ✅ Shows |
| **Stop Sign** | ✅ Shows | ✅ Shows |
| **No Parking** | ✅ Shows | ✅ Shows |
| **No U-Turn** | ✅ Shows | ✅ Shows |
| **Two Way** | ✅ Shows | ✅ Shows |

---

## 🐛 Troubleshooting

### Issue: Wrong sign labels appear
**Solution**: Update `TRAFFIC_SIGN_LABELS` to match your exact training class names and order

### Issue: No detections
**Causes**:
1. Model trained with very few images (< 20)
2. Different lighting conditions
3. Signs at odd angles
**Solution**: Train with more diverse images (40-60+)

### Issue: Speed limit shows when stationary
**Check**: 
- GPS enabled?
- Location permission granted?
- Wait 10 seconds for GPS to initialize
**Current threshold**: 1 m/s = 3.6 km/h = ~2.2 mph

---

## 📊 Current Configuration

```
Pothole Model (patholes.tflite):
- Classes: 1 (pothole)
- Input: 640x640
- Confidence: 15%

Traffic Sign Model (traffic_signs.tflite.tflite):
- Classes: 5 (YOUR classes)
- Input: 640x640  
- Confidence: 15%
- Speed check: Enabled for "Speed Limit" signs
```

---

## 🎯 Next Steps

1. Check your training notebook CLASS_NAMES
2. Update TrafficSignDetector.kt labels to match
3. Rebuild: `.\gradlew assembleDebug`
4. Test all sign types
5. If accuracy is low, retrain with more images (50-100 per class)

Good luck! 🚀
