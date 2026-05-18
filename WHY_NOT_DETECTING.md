# 🔍 Why Detection Isn't Working - Troubleshooting Guide

## 🚨 Common Issues and Solutions

Based on testing your models, here are the likely problems:

---

## ❌ Issue 1: INT8 Quantization Problem

### What's Wrong:
Your models were exported with `int8=True` quantization, which changes how the model expects input data.

### Symptoms:
- Model loads successfully
- No runtime errors
- But **zero detections** or very low confidence (<0.01)

### Solution:
**Re-export models WITHOUT INT8 quantization**

Update your training notebooks (Step 11):

**CHANGE FROM:**
```python
tflite_path = best_model.export(
    format='tflite',
    imgsz=640,
    int8=True,      # ❌ REMOVE THIS
    data=config_path
)
```

**CHANGE TO:**
```python
tflite_path = best_model.export(
    format='tflite',
    imgsz=640,
    # int8=True removed - use float32 for better accuracy
    data=config_path
)
```

**Why**: INT8 quantization requires specific input normalization that might not match your app's preprocessing.

---

## ❌ Issue 2: Not Enough Training Data

### Symptoms:
- Model detects during training validation
- But fails on new images
- Low confidence scores (<0.15)

### Your Dataset:
From Roboflow, you likely have:
- **Traffic signs**: 50-100 images
- **Potholes**: 50-100 images

### Problem:
**Mini datasets produce weak models!**

### Solution Options:

#### Option A: Retrain with MORE data (Recommended)
1. Collect 200-500 images per class
2. Train for 100-150 epochs
3. Use data augmentation
4. Expected accuracy: 85-90%

#### Option B: Use pretrained models (Faster)
1. Search Hugging Face for "yolov8 traffic signs" or "yolov8 pothole"
2. Download float32 TFLite models
3. Replace your current models

#### Option C: Accept lower accuracy for demo
1. Lower confidence threshold to 0.05
2. Accept false positives
3. Only for demonstration purposes

---

## ❌ Issue 3: Training Didn't Converge

### Symptoms:
- Training finished but mAP50 < 0.30
- High loss values
- Model guessing randomly

### Check Your Training Results:
Look at the `results.png` from training:
- **mAP50 should be > 0.50** (50% accuracy)
- **mAP50-95 should be > 0.30**
- **Loss should decrease over time**

### If Training Failed:
```python
# In your training notebook, increase epochs:
results = model.train(
    data=config_path,
    epochs=100,        # INCREASE from 30 to 100
    imgsz=640,
    batch=8,
    patience=20,       # INCREASE patience
    # ... rest of params
)
```

---

## ❌ Issue 4: Wrong Input Preprocessing

### Current App Code:
```kotlin
.add(NormalizeOp(0f, 255f))  // Normalizes to [0,1]
```

### YOLOv8 Expects:
- Float32 models: **[0, 1]** range ✅ (correct)
- INT8 models: **[0, 255]** range ❌ (your issue!)

### Fix (If keeping INT8):
```kotlin
// For INT8 quantized models
.add(NormalizeOp(0f, 1f))  // No normalization, keep [0, 255]
```

**BUT BETTER**: Re-export as float32 and keep current code!

---

## ❌ Issue 5: Model Files Corrupted

### Check File Sizes:
```
patholes.tflite: 3.3 MB       ✅ Seems OK
traffic_signs.tflite: 3.3 MB  ✅ Seems OK
```

Your files look fine. Not corrupted.

---

## 🎯 RECOMMENDED FIX (Do These in Order):

### Step 1: Re-export Models as Float32

**For Traffic Signs:**
1. Open `traffic_minidata.ipynb` in Google Colab
2. Go to Step 11 (Export to TFLite)
3. **Remove** `int8=True` line
4. Run the cell
5. Download new model
6. Replace in `app/src/main/assets/models/traffic_signs.tflite.tflite`

**For Potholes:**
1. Open `pothole_minidata.ipynb` in Google Colab
2. Go to Step 11 (Export to TFLite)
3. **Remove** `int8=True` line
4. Run the cell
5. Download new model
6. Replace in `app/src/main/assets/models/patholes.tflite`

### Step 2: Lower Confidence Threshold (Temporary)

Edit `TFLiteDetector.kt`:
```kotlin
private const val CONFIDENCE_THRESHOLD = 0.10f  // Lower to 10%
```

### Step 3: Rebuild and Test
```bash
.\gradlew assembleDebug
```

### Step 4: Check Logcat
```bash
adb logcat -s TFLiteDetector:* TrafficSignDetector:* RoadHazardDetector:*
```

Look for:
- "Max confidence found: X.XXXX"
- If > 0.10, should detect
- If < 0.01, model is broken

---

## 🧪 Quick Test

Before changing anything, let's verify the model works:

### Test 1: Check Training Results
1. Open your Google Colab notebook
2. Find the "results.png" image
3. Check mAP50 value:
   - **> 0.50**: Model is good ✅
   - **0.30-0.50**: Model is weak ⚠️
   - **< 0.30**: Model failed ❌

### Test 2: Test in Colab
Add this cell after training:
```python
# Test model on validation image
from PIL import Image
test_img = val_images[0]
results = model.predict(test_img, conf=0.10)
print(f"Detections found: {len(results[0].boxes)}")
results[0].show()
```

If it detects in Colab but not in app → **INT8 quantization issue**

---

## 📊 Expected Behavior After Fix:

| Dataset Size | Training Epochs | Expected mAP50 | Real-world Performance |
|--------------|----------------|----------------|------------------------|
| 20-30 images | 30 | 0.30-0.50 | Poor (demo only) |
| 50-100 images | 50 | 0.50-0.70 | Fair (low accuracy) |
| 200+ images | 100 | 0.70-0.85 | Good |
| 500+ images | 150 | 0.85-0.95 | Excellent |

Your current models (30 images, 30 epochs) → **0.30-0.50 mAP** → Poor accuracy

---

## 🎯 **IMMEDIATE ACTION:**

1. **Re-export models without INT8** (takes 5 minutes)
2. **Replace model files in app**
3. **Rebuild app**
4. **Test again**

If still not working:
5. **Check training mAP50** (should be > 0.50)
6. **Retrain with 50-100 images and 50-100 epochs**

---

## 💡 Why This Happens:

INT8 quantization converts float32 weights to 8-bit integers to reduce model size. This requires:
1. Specific input scaling
2. Specific output dequantization
3. Calibration dataset during export

**Without proper calibration, INT8 models can produce garbage output!**

**Solution**: Use float32 models for better compatibility and accuracy.

---

## Still Not Working?

Share:
1. Training results.png screenshot
2. mAP50 value from training
3. Number of images in training dataset
4. Logcat output showing "Max confidence found"

I'll help debug further!
