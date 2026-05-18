# 🚦 Traffic Sign Detection Model Training Guide
## (For You - Traffic Sign Dataset)

---

## 📋 Overview
- **Your Task**: Train traffic sign detection model
- **Target**: Detect and classify traffic signs (Stop, Speed Limit, Yield, etc.)
- **Output**: `traffic_signs.tflite` (640x640 input size)
- **Time**: 2-3 hours
- **Platform**: Google Colab (FREE GPU)

---

## 🎯 Important: Understanding Sign Detection

### What the model will do:
1. **Detect** where traffic signs are in the image (bounding box)
2. **Classify** which type of sign it is (Stop Sign, Speed Limit 50, etc.)
3. **Output** the sign name + confidence percentage

### How it shows in the app:
When model detects a sign, the app will display:
```
🚦 Stop Sign
Confidence: 92% | Importance: 10/10
```

The **sign name** comes from the class label in your training data!

---

## 🎯 Step 1: Setup Google Colab

1. Go to: https://colab.research.google.com/
2. Click "New Notebook"
3. Change runtime to GPU:
   - Click "Runtime" → "Change runtime type"
   - Select "T4 GPU"
   - Click "Save"

---

## 📦 Step 2: Install Dependencies

```python
# Install YOLOv8
!pip install ultralytics==8.0.196

# Install additional tools
!pip install kaggle

# Verify installation
from ultralytics import YOLO
import torch
print(f"✅ PyTorch version: {torch.__version__}")
print(f"✅ CUDA available: {torch.cuda.is_available()}")
print(f"✅ GPU: {torch.cuda.get_device_name(0) if torch.cuda.is_available() else 'None'}")
```

---

## 📊 Step 3: Choose and Download Dataset

### Option A: GTSRB (German Signs - RECOMMENDED)
**Best for variety and accuracy - 43 sign types**

```python
# Setup Kaggle API
!mkdir -p ~/.kaggle
!echo '{"username":"YOUR_KAGGLE_USERNAME","key":"YOUR_KAGGLE_API_KEY"}' > ~/.kaggle/kaggle.json
!chmod 600 ~/.kaggle/kaggle.json

# Download GTSRB dataset
!kaggle datasets download -d meowmeowmeowmeowmeow/gtsrb-german-traffic-sign
!unzip gtsrb-german-traffic-sign.zip -d gtsrb_dataset
```

**How to get Kaggle API key:**
1. Go to https://www.kaggle.com/settings
2. Scroll to "API" section
3. Click "Create New API Token"
4. Download `kaggle.json` and copy username/key

### Option B: Indian Traffic Signs (For Indian Roads)
**Best if you're testing in India**

```python
# Download Indian Traffic Sign Dataset
!kaggle datasets download -d pkdarabi/indian-traffic-sign-dataset
!unzip indian-traffic-sign-dataset.zip -d indian_signs_dataset
```

---

## 🔧 Step 4: Convert Dataset to YOLO Format

GTSRB comes in classification format. We need to convert it to YOLO detection format.

```python
import os
import cv2
import shutil
from pathlib import Path

# Create YOLO format dataset structure
base_path = '/content/traffic_signs_yolo'
os.makedirs(f'{base_path}/train/images', exist_ok=True)
os.makedirs(f'{base_path}/train/labels', exist_ok=True)
os.makedirs(f'{base_path}/valid/images', exist_ok=True)
os.makedirs(f'{base_path}/valid/labels', exist_ok=True)

# GTSRB has 43 classes - these are German traffic signs
# They are universal and work for most countries
sign_classes = {
    0: "Speed Limit 20",
    1: "Speed Limit 30", 
    2: "Speed Limit 50",
    3: "Speed Limit 60",
    4: "Speed Limit 70",
    5: "Speed Limit 80",
    6: "End Speed Limit 80",
    7: "Speed Limit 100",
    8: "Speed Limit 120",
    9: "No Overtaking",
    10: "No Overtaking Trucks",
    11: "Priority Road",
    12: "Priority Sign",
    13: "Yield",
    14: "Stop Sign",
    15: "No Vehicles",
    16: "No Trucks",
    17: "No Entry",
    18: "General Caution",
    19: "Dangerous Curve Left",
    20: "Dangerous Curve Right",
    21: "Double Curve",
    22: "Bumpy Road",
    23: "Slippery Road",
    24: "Road Narrows Right",
    25: "Road Work",
    26: "Traffic Signals",
    27: "Pedestrians",
    28: "Children Crossing",
    29: "Bicycles Crossing",
    30: "Ice Snow",
    31: "Wild Animals",
    32: "End All Limits",
    33: "Turn Right",
    34: "Turn Left",
    35: "Ahead Only",
    36: "Straight or Right",
    37: "Straight or Left",
    38: "Keep Right",
    39: "Keep Left",
    40: "Roundabout",
    41: "End No Overtaking",
    42: "End No Overtaking Trucks"
}

print(f"✅ Dataset will have {len(sign_classes)} sign classes")
```

### Convert images to YOLO format:

```python
import pandas as pd
from sklearn.model_selection import train_test_split

# Load GTSRB metadata (adjust path based on your download)
train_csv = '/content/gtsrb_dataset/Train.csv'  # Adjust if needed
df = pd.read_csv(train_csv)

print(f"📊 Total images: {len(df)}")
print(f"📊 Classes: {df['ClassId'].nunique()}")

# Split into train/val (80/20)
train_df, val_df = train_test_split(df, test_size=0.2, stratify=df['ClassId'], random_state=42)

print(f"📊 Training images: {len(train_df)}")
print(f"📊 Validation images: {len(val_df)}")

# Function to convert and save images
def process_images(df, split='train'):
    for idx, row in df.iterrows():
        # Read image
        img_path = f"/content/gtsrb_dataset/{row['Path']}"  # Adjust path
        if not os.path.exists(img_path):
            continue
            
        img = cv2.imread(img_path)
        if img is None:
            continue
        
        h, w = img.shape[:2]
        class_id = row['ClassId']
        
        # Save image
        img_name = f"{split}_{idx}.jpg"
        cv2.imwrite(f'{base_path}/{split}/images/{img_name}', img)
        
        # Create YOLO label (bbox covers entire image since these are cropped signs)
        # Format: class_id center_x center_y width height (normalized 0-1)
        label_txt = f"{class_id} 0.5 0.5 0.9 0.9\n"
        
        # Save label
        label_name = f"{split}_{idx}.txt"
        with open(f'{base_path}/{split}/labels/{label_name}', 'w') as f:
            f.write(label_txt)
        
        if idx % 1000 == 0:
            print(f"Processed {idx} images...")

# Process train and validation sets
print("Processing training images...")
process_images(train_df, 'train')

print("Processing validation images...")
process_images(val_df, 'valid')

print("✅ Dataset conversion complete!")
```

---

## 🔧 Step 5: Create data.yaml Configuration

```python
# Create data.yaml with ALL 43 classes
data_yaml = f"""
path: {base_path}
train: train/images
val: valid/images

# 43 German Traffic Sign Classes (GTSRB)
names:
  0: Speed Limit 20
  1: Speed Limit 30
  2: Speed Limit 50
  3: Speed Limit 60
  4: Speed Limit 70
  5: Speed Limit 80
  6: End Speed Limit 80
  7: Speed Limit 100
  8: Speed Limit 120
  9: No Overtaking
  10: No Overtaking Trucks
  11: Priority Road
  12: Priority Sign
  13: Yield
  14: Stop Sign
  15: No Vehicles
  16: No Trucks
  17: No Entry
  18: General Caution
  19: Dangerous Curve Left
  20: Dangerous Curve Right
  21: Double Curve
  22: Bumpy Road
  23: Slippery Road
  24: Road Narrows Right
  25: Road Work
  26: Traffic Signals
  27: Pedestrians
  28: Children Crossing
  29: Bicycles Crossing
  30: Ice Snow
  31: Wild Animals
  32: End All Limits
  33: Turn Right
  34: Turn Left
  35: Ahead Only
  36: Straight or Right
  37: Straight or Left
  38: Keep Right
  39: Keep Left
  40: Roundabout
  41: End No Overtaking
  42: End No Overtaking Trucks
"""

with open(f'{base_path}/data.yaml', 'w') as f:
    f.write(data_yaml)

print("✅ data.yaml created with 43 sign classes")
print("✅ App will show sign names like 'Stop Sign', 'Speed Limit 50', etc.")
```

**IMPORTANT**: These 43 class names will appear in your app when signs are detected!

---

## 🚀 Step 6: Train the Model

```python
from ultralytics import YOLO

# Load pretrained YOLOv8n
model = YOLO('yolov8n.pt')

# Train traffic sign detection model
results = model.train(
    data=f'{base_path}/data.yaml',
    epochs=150,              # More epochs for 43 classes
    imgsz=640,               # IMPORTANT: Match your app (640x640)
    batch=16,                # Adjust based on GPU memory
    device=0,                # Use GPU
    patience=30,             # Early stopping
    save=True,
    project='sign_training',
    name='yolov8n_traffic_signs_v1',
    exist_ok=True,
    pretrained=True,
    optimizer='AdamW',
    verbose=True,
    seed=42,
    lr0=0.01,
    lrf=0.01,
    momentum=0.937,
    weight_decay=0.0005,
    warmup_epochs=3,
    warmup_momentum=0.8,
    warmup_bias_lr=0.1,
    box=7.5,
    cls=0.5,
    dfl=1.5,
    pose=12.0,
    kobj=1.0,
    label_smoothing=0.0,
    nbs=64,
    hsv_h=0.015,
    hsv_s=0.7,
    hsv_v=0.4,
    degrees=10.0,
    translate=0.1,
    scale=0.5,
    shear=0.0,
    perspective=0.0,
    flipud=0.0,
    fliplr=0.5,
    mosaic=1.0,
    mixup=0.1,
    copy_paste=0.0
)

print("✅ Training completed!")
```

**Training will take 2-3 hours** for 150 epochs with 43 classes.

---

## 📈 Step 7: Evaluate Model

```python
# Validate the trained model
metrics = model.val()

print(f"📊 mAP50: {metrics.box.map50:.3f}")
print(f"📊 mAP50-95: {metrics.box.map:.3f}")
print(f"📊 Precision: {metrics.box.mp:.3f}")
print(f"📊 Recall: {metrics.box.mr:.3f}")

# View results
from IPython.display import Image, display
display(Image(filename=f'{base_path}/../sign_training/yolov8n_traffic_signs_v1/results.png', width=800))

# Confusion matrix
display(Image(filename=f'{base_path}/../sign_training/yolov8n_traffic_signs_v1/confusion_matrix.png', width=800))
```

**Good results:**
- mAP50 > 0.90 (90%)
- Precision > 0.85
- Recall > 0.85

---

## 🎯 Step 8: Test Sign Detection

```python
# Test on validation images
results = model.predict(
    source=f'{base_path}/valid/images',
    conf=0.30,  # 30% confidence threshold
    save=True,
    project='test_results',
    name='sign_predictions',
    show_labels=True,
    show_conf=True
)

# View sample predictions
import glob
test_images = glob.glob('/content/test_results/sign_predictions/*.jpg')[:5]
for img_path in test_images:
    display(Image(filename=img_path, width=600))
    print(f"Image: {img_path}\n")
```

You should see bounding boxes with labels like:
- "Stop Sign 0.95"
- "Speed Limit 50 0.88"
- "Yield 0.91"

---

## 📱 Step 9: Export to TFLite (CRITICAL!)

```python
# Export to TFLite for Android
success = model.export(
    format='tflite',
    imgsz=640,           # MUST be 640x640
    int8=False,          # Float32 for better accuracy
    optimize=True,
    batch=1
)

print(f"✅ Model exported successfully!")
print(f"📁 Location: {success}")

# The model is saved at:
# /content/sign_training/yolov8n_traffic_signs_v1/weights/best_saved_model/best_float32.tflite
```

---

## 💾 Step 10: Download the Model

```python
# Copy to easy location
!cp /content/sign_training/yolov8n_traffic_signs_v1/weights/best_saved_model/best_float32.tflite /content/traffic_signs.tflite

# Download
from google.colab import files
files.download('/content/traffic_signs.tflite')

print("✅ Model downloaded!")
print("⚠️ IMPORTANT: Rename to 'traffic_signs.tflite.tflite' (double extension)")
```

---

## 🔄 Step 11: Update Android App Code

The model has 43 classes, so update the detector:

### File: `TrafficSignDetector.kt`

Change line 14:
```kotlin
numClasses: Int = 21  // OLD - WRONG
```

To:
```kotlin
numClasses: Int = 43  // NEW - Matches GTSRB
```

Also update the TRAFFIC_SIGN_LABELS map to include all 43 classes:

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

---

## 🔄 Step 12: Replace in Android App

1. **Download** `traffic_signs.tflite` from Colab
2. **Rename** to `traffic_signs.tflite.tflite` (double extension - match current filename)
3. **Replace** in: `app/src/main/assets/models/traffic_signs.tflite.tflite`
4. **Update** `TrafficSignDetector.kt` with 43 classes
5. **Rebuild**: `.\gradlew assembleDebug`
6. **Test** on real traffic signs!

---

## 📱 How It Will Work in the App

When your model detects a sign, the app will show:

```
🚦 Stop Sign
Confidence: 92% | Importance: 10/10
```

The **sign name** (e.g., "Stop Sign", "Speed Limit 50") comes directly from the class labels in `data.yaml`!

---

## 🐛 Troubleshooting

### Issue: "No Entry" detected on everything
**Cause**: Model not trained properly or wrong numClasses
**Solution**: 
- Retrain with this guide
- Update `numClasses` to 43 in code
- Replace both the model AND update the code

### Issue: Low accuracy (< 85%)
**Solution:**
- Increase epochs to 200
- Add more training data
- Try YOLOv8s (small) instead of YOLOv8n

### Issue: Model size too large
**Solution:**
```python
# Use INT8 quantization
success = model.export(
    format='tflite',
    imgsz=640,
    int8=True,  # Smaller, slightly lower accuracy
)
```

---

## 📊 Expected Results

After training on GTSRB (50K images, 43 classes):
- **mAP50**: 90-95%
- **Model Size**: 8-12 MB
- **Inference Time**: 80-120ms on mobile
- **Sign Detection**: Stop, Yield, Speed Limits, etc. with names
- **False Positives**: < 3%

---

## ✅ Final Checklist

- [ ] Google Colab GPU enabled (T4)
- [ ] GTSRB dataset downloaded (50K+ images)
- [ ] Dataset converted to YOLO format
- [ ] `data.yaml` created with 43 sign classes
- [ ] Training completed (150+ epochs)
- [ ] mAP50 > 90%
- [ ] Model exported to TFLite (640x640 input)
- [ ] Downloaded `traffic_signs.tflite`
- [ ] Renamed to `traffic_signs.tflite.tflite`
- [ ] Updated `TrafficSignDetector.kt` with numClasses=43
- [ ] Updated TRAFFIC_SIGN_LABELS map with all 43 classes
- [ ] Replaced model file in Android project
- [ ] App rebuilt and tested

---

## 🎓 Understanding Class Labels

The sign names that appear in your app come from the `names:` section in `data.yaml`.

**Example:**
```yaml
names:
  14: Stop Sign  # This exact text appears in the app
  2: Speed Limit 50  # This exact text appears in the app
```

When the model detects class 14, the app shows "Stop Sign".  
When it detects class 2, the app shows "Speed Limit 50".

**You control the sign names in `data.yaml`!**

---

## 💬 Common Questions

**Q: Can I add custom Indian signs?**  
A: Yes! Just add them to the dataset with appropriate labels in `data.yaml`

**Q: What if I only want specific signs (Stop, Yield, Speed Limits)?**  
A: Filter the dataset to only include those classes before training

**Q: How accurate will sign names be?**  
A: 90-95% accuracy if trained properly on GTSRB dataset

**Q: Will it work at night?**  
A: Add nighttime images to training data for better night performance

---

## 🚀 Next Steps

After training:
1. Test in Android app with real traffic signs
2. If accuracy is good, you're done!
3. If accuracy is low, retrain with more epochs or YOLOv8s
4. Collect real-world sign images to improve model
5. Consider adding Indian-specific signs if needed

---

**Good luck! 🎉**

**Remember**: The sign names in your app will exactly match what you put in `data.yaml`!
