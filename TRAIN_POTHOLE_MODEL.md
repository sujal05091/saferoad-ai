# 🚗 Pothole Detection Model Training Guide
## (For Friend - Pothole Dataset)

---

## 📋 Overview
- **Your Task**: Train pothole detection model
- **Target**: Detect potholes, cracks, road damage
- **Output**: `patholes.tflite` (300x300 input size)
- **Time**: 3-4 hours
- **Platform**: Google Colab (FREE GPU)

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

Copy and run this in first cell:

```python
# Install YOLOv8
!pip install ultralytics==8.0.196

# Install additional tools
!pip install roboflow kaggle

# Verify installation
from ultralytics import YOLO
import torch
print(f"✅ PyTorch version: {torch.__version__}")
print(f"✅ CUDA available: {torch.cuda.is_available()}")
print(f"✅ GPU: {torch.cuda.get_device_name(0) if torch.cuda.is_available() else 'None'}")
```

---

## 📊 Step 3: Download Datasets

### Option A: Kaggle Dataset (Recommended)

```python
# Setup Kaggle API (one-time)
!mkdir -p ~/.kaggle
!echo '{"username":"YOUR_KAGGLE_USERNAME","key":"YOUR_KAGGLE_API_KEY"}' > ~/.kaggle/kaggle.json
!chmod 600 ~/.kaggle/kaggle.json

# Download Kaggle Pothole Dataset
!kaggle datasets download -d atulyakumar98/pothole-detection-dataset
!unzip pothole-detection-dataset.zip -d pothole_dataset
```

**How to get Kaggle API key:**
1. Go to https://www.kaggle.com/settings
2. Scroll to "API" section
3. Click "Create New API Token"
4. Download `kaggle.json` and copy the username/key

### Option B: RoboFlow Dataset

```python
# Install roboflow
!pip install roboflow

from roboflow import Roboflow
rf = Roboflow(api_key="YOUR_ROBOFLOW_KEY")
project = rf.workspace("pothole-detection-e4ovk").project("pothole-detection-uc6yw")
dataset = project.version(1).download("yolov8")
```

**Get RoboFlow API key:**
1. Go to https://app.roboflow.com/
2. Sign up (free)
3. Settings → API Key

### Option C: Combine Both (BEST - Recommended)

```python
# Download both datasets
# 1. Kaggle dataset
!kaggle datasets download -d atulyakumar98/pothole-detection-dataset
!unzip pothole-detection-dataset.zip -d kaggle_potholes

# 2. RoboFlow dataset  
from roboflow import Roboflow
rf = Roboflow(api_key="YOUR_KEY")
project = rf.workspace("pothole-detection-e4ovk").project("pothole-detection-uc6yw")
dataset = project.version(1).download("yolov8")

# Merge datasets (we'll do this in next step)
```

---

## 🔧 Step 4: Prepare Dataset

### Create `data.yaml` configuration file:

```python
import os

# Create data.yaml for YOLOv8
data_yaml = """
path: /content/pothole_dataset  # Dataset root
train: train/images  # Train images
val: valid/images    # Validation images
test: test/images    # Test images (optional)

# Classes
names:
  0: Pothole
  1: Crack
  2: Debris
  3: Road Damage
  4: Construction Zone
  5: Obstruction
  6: Water Puddle
  7: Manhole
  8: Speed Bump
  9: Uneven Surface
"""

with open('/content/pothole_dataset/data.yaml', 'w') as f:
    f.write(data_yaml)

print("✅ data.yaml created")
```

**Note**: Adjust class names based on what your dataset actually contains. Most pothole datasets have 1-4 classes.

### Verify Dataset Structure:

```python
# Check dataset structure
!ls -la /content/pothole_dataset/

# Count images
import os
train_imgs = len(os.listdir('/content/pothole_dataset/train/images'))
val_imgs = len(os.listdir('/content/pothole_dataset/valid/images'))
print(f"📊 Training images: {train_imgs}")
print(f"📊 Validation images: {val_imgs}")
```

Expected structure:
```
pothole_dataset/
├── data.yaml
├── train/
│   ├── images/
│   └── labels/
└── valid/
    ├── images/
    └── labels/
```

---

## 🚀 Step 5: Train the Model

```python
from ultralytics import YOLO

# Load pretrained YOLOv8n (nano - lightweight for mobile)
model = YOLO('yolov8n.pt')

# Train the model
results = model.train(
    data='/content/pothole_dataset/data.yaml',
    epochs=100,              # More epochs = better accuracy
    imgsz=300,               # IMPORTANT: Match your app (300x300)
    batch=16,                # Adjust based on GPU memory
    device=0,                # Use GPU
    patience=20,             # Early stopping patience
    save=True,               # Save checkpoints
    project='pothole_training',
    name='yolov8n_pothole_v1',
    exist_ok=True,
    pretrained=True,
    optimizer='AdamW',
    verbose=True,
    seed=42,
    deterministic=True,
    single_cls=False,        # Multi-class detection
    rect=False,
    cos_lr=True,
    close_mosaic=10,
    resume=False,
    amp=True,                # Automatic Mixed Precision
    fraction=1.0,
    profile=False,
    overlap_mask=True,
    mask_ratio=4,
    dropout=0.0,
    val=True,
    plots=True
)

print("✅ Training completed!")
```

**Training will take 2-3 hours**. You'll see output like:
```
Epoch 1/100: 100%|██████████| 45/45 [00:25<00:00,  1.76it/s]
      Class     Images  Instances      P      R  mAP50  mAP50-95
        all        450        892  0.823  0.756  0.812     0.542
```

---

## 📈 Step 6: Evaluate Model

```python
# Validate the model
metrics = model.val()

print(f"📊 mAP50: {metrics.box.map50:.3f}")
print(f"📊 mAP50-95: {metrics.box.map:.3f}")
print(f"📊 Precision: {metrics.box.mp:.3f}")
print(f"📊 Recall: {metrics.box.mr:.3f}")

# View results
from IPython.display import Image
Image(filename='/content/pothole_training/yolov8n_pothole_v1/results.png', width=800)
```

**Good results:**
- mAP50 > 0.85 (85%)
- Precision > 0.80
- Recall > 0.75

---

## 🎯 Step 7: Test with Sample Images

```python
# Test on validation images
results = model.predict(
    source='/content/pothole_dataset/valid/images',
    conf=0.25,
    save=True,
    project='test_results',
    name='pothole_predictions'
)

# Display a prediction
Image(filename='/content/test_results/pothole_predictions/image1.jpg', width=600)
```

---

## 📱 Step 8: Export to TFLite (IMPORTANT!)

```python
# Export to TFLite format for Android
success = model.export(
    format='tflite',
    imgsz=300,           # MUST match training size
    int8=False,          # Use float32 (better accuracy)
    optimize=True,
    batch=1
)

print(f"✅ Model exported successfully!")
print(f"📁 TFLite model location: {success}")

# Model will be saved at:
# /content/pothole_training/yolov8n_pothole_v1/weights/best_saved_model/best_float32.tflite
```

---

## 💾 Step 9: Download the Model

```python
# Copy to easily accessible location
!cp /content/pothole_training/yolov8n_pothole_v1/weights/best_saved_model/best_float32.tflite /content/patholes.tflite

# Download to your computer
from google.colab import files
files.download('/content/patholes.tflite')

print("✅ Model downloaded! Rename it to 'patholes.tflite'")
```

---

## 🔄 Step 10: Replace in Android App

1. **Download** `patholes.tflite` from Colab
2. **Replace** file in: `app/src/main/assets/models/patholes.tflite`
3. **Rebuild** app: `.\gradlew assembleDebug`
4. **Test** on real potholes!

---

## 🐛 Troubleshooting

### Issue: Low accuracy (< 80%)
**Solution:**
- Increase epochs to 150-200
- Add more data augmentation
- Try YOLOv8s (small) instead of YOLOv8n (nano)

### Issue: Out of memory
**Solution:**
```python
# Reduce batch size
results = model.train(
    batch=8,  # Changed from 16
    ...
)
```

### Issue: Model too large for Android
**Solution:**
```python
# Use INT8 quantization
success = model.export(
    format='tflite',
    imgsz=300,
    int8=True,  # Smaller size, slightly lower accuracy
)
```

---

## 📊 Expected Results

After training with 3,000-5,000 images:
- **mAP50**: 85-92%
- **Model Size**: 6-10 MB
- **Inference Time**: 50-80ms on mobile
- **False Positives**: < 5%

---

## 🎓 Advanced: Data Augmentation

If accuracy is low, add more augmentation:

```python
results = model.train(
    data='/content/pothole_dataset/data.yaml',
    epochs=100,
    imgsz=300,
    batch=16,
    # Data Augmentation
    hsv_h=0.015,      # Hue
    hsv_s=0.7,        # Saturation  
    hsv_v=0.4,        # Value
    degrees=10.0,     # Rotation
    translate=0.1,    # Translation
    scale=0.5,        # Scale
    shear=0.0,        # Shear
    perspective=0.0,  # Perspective
    flipud=0.0,       # Flip up-down
    fliplr=0.5,       # Flip left-right (50%)
    mosaic=1.0,       # Mosaic augmentation
    mixup=0.1,        # Mixup augmentation
)
```

---

## ✅ Final Checklist

- [ ] Google Colab GPU enabled (T4)
- [ ] Dataset downloaded (3K+ images)
- [ ] `data.yaml` created with correct paths
- [ ] Training completed (100+ epochs)
- [ ] mAP50 > 85%
- [ ] Model exported to TFLite (300x300 input)
- [ ] Downloaded `patholes.tflite`
- [ ] Tested model on sample images
- [ ] Replaced file in Android project
- [ ] App rebuilt and tested

---

## 💬 Need Help?

Common questions:
- **Q**: Which dataset is best?
  **A**: Combine Kaggle + RoboFlow for best results (5K+ images)

- **Q**: How long will training take?
  **A**: 2-3 hours on Colab T4 GPU for 100 epochs

- **Q**: Can I train on CPU?
  **A**: Not recommended - will take 10-15 hours

- **Q**: What if accuracy is low?
  **A**: Increase epochs, add more data, or try YOLOv8s

---

## 🚀 Next Steps

After training:
1. Share the `.tflite` file with your teammate
2. Test in the Android app
3. If accuracy is good, train traffic sign model next
4. Collect real-world pothole images to improve model

---

**Good luck! 🎉**
