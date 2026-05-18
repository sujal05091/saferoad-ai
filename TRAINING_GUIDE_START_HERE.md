# 🚗🚦 Model Training - Quick Start Guide

## 📋 Division of Work

### 👤 Friend → Pothole Detection Model
**File**: [`TRAIN_POTHOLE_MODEL.md`](./TRAIN_POTHOLE_MODEL.md)

**Task**: Train model to detect potholes and road damage
- **Dataset**: Kaggle Pothole Dataset + RoboFlow (5K+ images)
- **Output**: `patholes.tflite` (300x300 input)
- **Classes**: 10 types (Pothole, Crack, Debris, etc.)
- **Time**: 3-4 hours
- **Platform**: Google Colab (FREE GPU)

### 👤 You → Traffic Sign Detection Model  
**File**: [`TRAIN_TRAFFIC_SIGNS.md`](./TRAIN_TRAFFIC_SIGNS.md)

**Task**: Train model to detect and classify traffic signs
- **Dataset**: GTSRB (German Traffic Signs) - 50K images
- **Output**: `traffic_signs.tflite` (640x640 input)
- **Classes**: 43 sign types (Stop, Speed Limits, Yield, etc.)
- **Time**: 2-3 hours
- **Platform**: Google Colab (FREE GPU)

---

## ⚡ Quick Links

| Person | Training Guide | Dataset | Output Model |
|--------|---------------|---------|--------------|
| **Friend** | [TRAIN_POTHOLE_MODEL.md](./TRAIN_POTHOLE_MODEL.md) | [Kaggle Pothole](https://www.kaggle.com/datasets/atulyakumar98/pothole-detection-dataset) | `patholes.tflite` |
| **You** | [TRAIN_TRAFFIC_SIGNS.md](./TRAIN_TRAFFIC_SIGNS.md) | [GTSRB Signs](https://www.kaggle.com/datasets/meowmeowmeowmeowmeow/gtsrb-german-traffic-sign) | `traffic_signs.tflite` |

---

## 🎯 Answer to Your Question

### "How will the app show which sign is detected?"

**Answer**: The sign name comes from the **class labels** in your training data!

#### Example Flow:

1. **In Training** (`data.yaml`):
```yaml
names:
  0: Speed Limit 20
  14: Stop Sign  
  13: Yield
  17: No Entry
```

2. **Model Learns**: Class 14 = Stop Sign, Class 17 = No Entry, etc.

3. **In Android App** (`TrafficSignDetector.kt`):
```kotlin
private val TRAFFIC_SIGN_LABELS = mapOf(
    0 to "Speed Limit 20",
    14 to "Stop Sign",
    13 to "Yield",
    17 to "No Entry"
)
```

4. **When Detected**, app displays:
```
🚦 Stop Sign
Confidence: 92% | Importance: 10/10
```

**The model outputs the class ID (e.g., 14), and your app code maps it to the sign name ("Stop Sign").**

---

## 📊 Training Data Requirements

### For Traffic Signs (Your Task):

**What data do you need?**
1. **Images of each sign type** you want to detect
2. **Bounding box labels** showing where signs are in images
3. **Class labels** mapping each sign to a name

**Best Dataset**: GTSRB (German Traffic Sign Recognition Benchmark)
- ✅ 50,000+ images
- ✅ 43 sign types with names
- ✅ High quality, varied conditions (day/night, different angles)
- ✅ Annotations already included
- ✅ **Universal signs** (work globally, including India)

**Sign Types Included**:
- Stop Sign
- Yield
- Speed Limits (20, 30, 50, 60, 70, 80, 100, 120)
- No Entry
- No Overtaking
- Priority Road
- Dangerous Curves
- Road Work
- Pedestrian Crossing
- School Zone
- Traffic Signals
- Roundabout
- Keep Right/Left
- Turn Right/Left
- And 29 more...

---

## 🚀 Parallel Training Workflow

### Week Timeline:

| Day | Friend (Potholes) | You (Traffic Signs) |
|-----|-------------------|---------------------|
| **Day 1** | Setup Colab, download dataset | Setup Colab, download GTSRB |
| **Day 2** | Train pothole model (3-4 hrs) | Convert dataset, train model (2-3 hrs) |
| **Day 3** | Export TFLite, test | Export TFLite, update app code |
| **Day 4** | **Share** `patholes.tflite` | **Share** `traffic_signs.tflite` + updated code |
| **Day 5** | **Both**: Integrate both models, test app |

---

## 📁 File Replacement Guide

### After Both Models Are Trained:

1. **Friend shares**: `patholes.tflite`
   - You place in: `app/src/main/assets/models/patholes.tflite`

2. **You share**: `traffic_signs.tflite` 
   - Rename to: `traffic_signs.tflite.tflite` (double extension)
   - Place in: `app/src/main/assets/models/traffic_signs.tflite.tflite`

3. **Update Code**: `app/src/main/java/com/saferoadai/detectors/TrafficSignDetector.kt`

Change these lines:

**Line 14** (number of classes):
```kotlin
numClasses: Int = 21  // OLD - WRONG
```
To:
```kotlin
numClasses: Int = 43  // NEW - Matches GTSRB
```

**Lines 20-61** (add all 43 sign labels):
```kotlin
private val TRAFFIC_SIGN_LABELS = mapOf(
    0 to "Speed Limit 20",
    1 to "Speed Limit 30",
    2 to "Speed Limit 50",
    3 to "Speed Limit 60",
    4 to "Speed Limit 70",
    5 to "Speed Limit 80",
    6 to "End Speed Limit 80",
    7 to "Speed Limit 100",
    8 to "Speed Limit 120",
    9 to "No Overtaking",
    10 to "No Overtaking Trucks",
    11 to "Priority Road",
    12 to "Priority Sign",
    13 to "Yield",
    14 to "Stop Sign",
    15 to "No Vehicles",
    16 to "No Trucks",
    17 to "No Entry",
    18 to "General Caution",
    19 to "Dangerous Curve Left",
    20 to "Dangerous Curve Right",
    21 to "Double Curve",
    22 to "Bumpy Road",
    23 to "Slippery Road",
    24 to "Road Narrows Right",
    25 to "Road Work",
    26 to "Traffic Signals",
    27 to "Pedestrians",
    28 to "Children Crossing",
    29 to "Bicycles Crossing",
    30 to "Ice Snow",
    31 to "Wild Animals",
    32 to "End All Limits",
    33 to "Turn Right",
    34 to "Turn Left",
    35 to "Ahead Only",
    36 to "Straight or Right",
    37 to "Straight or Left",
    38 to "Keep Right",
    39 to "Keep Left",
    40 to "Roundabout",
    41 to "End No Overtaking",
    42 to "End No Overtaking Trucks"
)
```

4. **Rebuild**:
```bash
cd "d:\project by sujal\RoadAi"
.\gradlew assembleDebug
```

5. **Test**: Install and test both detections!

---

## ✅ Pre-Training Checklist

### Friend (Potholes):
- [ ] Google Colab account created
- [ ] Kaggle account created + API key obtained
- [ ] Read `TRAIN_POTHOLE_MODEL.md` completely
- [ ] Understand model outputs 10 hazard classes
- [ ] 3-4 hours blocked for training

### You (Traffic Signs):
- [ ] Google Colab account created
- [ ] Kaggle account created + API key obtained
- [ ] Read `TRAIN_TRAFFIC_SIGNS.md` completely
- [ ] Understand model outputs 43 sign classes with names
- [ ] 2-3 hours blocked for training
- [ ] Know how to update `TrafficSignDetector.kt`

---

## 🎓 Expected Results

### After Both Models Are Trained:

**Pothole Model**:
- Detects: Potholes, Cracks, Debris, Road Damage, etc.
- Accuracy: 85-92% mAP50
- Size: 6-10 MB
- Speed: 50-80ms per frame

**Traffic Sign Model**:
- Detects: Stop, Yield, Speed Limits, No Entry, etc.
- Accuracy: 90-95% mAP50
- Size: 8-12 MB
- Speed: 80-120ms per frame

**Combined App Performance**:
- Real-time detection (15-25 FPS)
- Shows detection name on screen
- Saves to database with location
- Uploads to Firebase

---

## 💬 Common Questions

**Q: Can we use pre-trained models instead?**  
A: Yes, but they may not work well for your specific roads. Training ensures better accuracy.

**Q: What if we don't have GPU access?**  
A: Google Colab provides FREE T4 GPU. Just select it in runtime settings.

**Q: How do we know if our models are good?**  
A: Check mAP50 score:
- > 85% = Excellent
- 75-85% = Good
- < 75% = Needs more training

**Q: Can we add more sign types later?**  
A: Yes! Just retrain with additional classes in the dataset.

**Q: Will German signs work in India?**  
A: Most signs are universal (Stop, Speed Limits, etc.). GTSRB works globally.

**Q: What if model is too large for Android?**  
A: Use INT8 quantization during export to reduce size by 75%.

---

## 🐛 Troubleshooting

### Issue: Model detecting wrong things
**Solution**: Retrain with more epochs or better dataset

### Issue: Low confidence scores
**Solution**: Lower threshold in app or retrain with more data

### Issue: App crashes on detection
**Solution**: Verify `numClasses` matches your training classes

### Issue: Sign names not showing
**Solution**: Update `TRAFFIC_SIGN_LABELS` map in code

---

## 🚀 Let's Get Started!

1. **Friend**: Open [`TRAIN_POTHOLE_MODEL.md`](./TRAIN_POTHOLE_MODEL.md)
2. **You**: Open [`TRAIN_TRAFFIC_SIGNS.md`](./TRAIN_TRAFFIC_SIGNS.md)
3. **Both**: Start training in parallel!
4. **After training**: Share models and integrate
5. **Test together**: Verify app detects both potholes and signs

---

## 📞 Need Help?

If you run into issues:
1. Check the troubleshooting section in your training guide
2. Review the logs in Google Colab
3. Verify dataset format matches YOLO requirements
4. Ask for help with specific error messages

**Good luck! 🎉**

Both models should be ready in 4-6 hours of training time!
