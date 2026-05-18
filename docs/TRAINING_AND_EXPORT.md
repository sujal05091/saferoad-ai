# Model Training and TFLite Export Guide

This document explains how to train YOLOv8 models for traffic signs and road hazards, then convert them to TensorFlow Lite format for Android deployment.

---

## 🎯 Overview

SafeRoadAI requires two TFLite models:
1. **traffic_signs.tflite** - Detects 40+ traffic sign classes
2. **road_hazards.tflite** - Detects potholes, cracks, debris, road damage, etc.

Both models are based on YOLOv8 (You Only Look Once v8) architecture, optimized for mobile inference.

---

## 📦 Prerequisites

### Software Requirements
- Python 3.8+
- Google Colab (recommended) or local GPU
- Ultralytics YOLOv8
- TensorFlow 2.14+
- ONNX

### Install Dependencies
```bash
pip install ultralytics tensorflow onnx onnx-tf
```

---

## 1️⃣ Traffic Sign Detection Model

### Dataset Preparation

#### Recommended Datasets
1. **German Traffic Sign Recognition Benchmark (GTSRB)**
   - URL: https://benchmark.ini.rub.de/gtsrb_dataset.html
   - 43 classes, 50,000+ images

2. **LISA Traffic Sign Dataset**
   - URL: https://www.kaggle.com/datasets/meowmeowmeowmeowmeow/gtsrb-german-traffic-sign
   - US traffic signs

3. **Custom Dataset**
   - Collect local traffic signs with phone camera
   - Use Roboflow for annotation

#### Dataset Structure
```
traffic_signs_dataset/
├── train/
│   ├── images/
│   └── labels/  # YOLO format: class_id x_center y_center width height
├── val/
│   ├── images/
│   └── labels/
└── data.yaml
```

#### data.yaml Example
```yaml
path: /content/traffic_signs_dataset
train: train/images
val: val/images

nc: 40  # number of classes
names: ['Stop Sign', 'Speed Limit 20', 'Speed Limit 30', ...]  # class names
```

### Training Script (Google Colab)

```python
# ============================================
# Traffic Sign Detection Training - YOLOv8
# Run in Google Colab with GPU
# ============================================

# 1. Install Ultralytics
!pip install ultralytics

# 2. Import libraries
from ultralytics import YOLO
import torch

print(f"PyTorch version: {torch.__version__}")
print(f"CUDA available: {torch.cuda.is_available()}")

# 3. Load pretrained YOLOv8 nano model (fastest for mobile)
model = YOLO('yolov8n.pt')  # or yolov8s.pt for better accuracy

# 4. Train model
results = model.train(
    data='/content/traffic_signs_dataset/data.yaml',
    epochs=100,
    imgsz=640,
    batch=16,
    device=0,  # GPU
    project='traffic_sign_training',
    name='yolov8n_traffic_signs',
    
    # Optimization flags
    patience=20,  # Early stopping
    save=True,
    plots=True,
    
    # Augmentation
    hsv_h=0.015,  # HSV-Hue augmentation
    hsv_s=0.7,    # HSV-Saturation
    hsv_v=0.4,    # HSV-Value
    degrees=10,   # Rotation
    translate=0.1,  # Translation
    scale=0.5,    # Scale
    flipud=0.0,   # No vertical flip for signs
    fliplr=0.5,   # Horizontal flip
    mosaic=1.0    # Mosaic augmentation
)

# 5. Evaluate model
metrics = model.val()
print(f"mAP50: {metrics.box.map50}")
print(f"mAP50-95: {metrics.box.map}")

# 6. Export to TFLite
success = model.export(
    format='tflite',
    imgsz=640,
    int8=True,  # Quantization for smaller size
    data='/content/traffic_signs_dataset/data.yaml'
)

print(f"Model exported: {success}")

# 7. Test inference
model_tflite = YOLO('traffic_sign_training/yolov8n_traffic_signs/weights/best-fp16.tflite')
results = model_tflite('test_image.jpg')
results[0].show()
```

### Model Optimization
```python
# Additional quantization options

# Option 1: FP16 (Half precision - smaller, slightly faster)
model.export(format='tflite', imgsz=640, half=True)

# Option 2: INT8 (8-bit quantization - smallest, fastest)
model.export(format='tflite', imgsz=640, int8=True, data='data.yaml')

# Option 3: Dynamic range quantization (no dataset needed)
model.export(format='tflite', imgsz=640)
```

---

## 2️⃣ Road Hazard Detection Model

### Dataset Preparation

#### Recommended Datasets
1. **RDD2020 (Road Damage Dataset)**
   - URL: https://github.com/sekilab/RoadDamageDetector
   - Japan, India, Czech road damage images

2. **Pothole Dataset (Kaggle)**
   - URL: https://www.kaggle.com/datasets/atulyakumar98/pothole-detection-dataset
   - 665+ annotated pothole images

3. **Custom Collection**
   - Record dashcam footage while driving
   - Extract frames with road hazards
   - Annotate using Roboflow or LabelImg

#### Hazard Classes
```python
HAZARD_CLASSES = [
    "Pothole",
    "Crack",
    "Debris",
    "Road Damage",
    "Construction Zone",
    "Obstruction",
    "Water Puddle",
    "Manhole",
    "Speed Bump",
    "Uneven Surface"
]
```

### Training Script (Google Colab)

```python
# ============================================
# Road Hazard Detection Training - YOLOv8
# ============================================

# 1. Prepare dataset
!pip install roboflow

from roboflow import Roboflow
rf = Roboflow(api_key="YOUR_ROBOFLOW_API_KEY")
project = rf.workspace("road-safety").project("road-hazards")
dataset = project.version(1).download("yolov8")

# 2. Train YOLOv8
from ultralytics import YOLO

model = YOLO('yolov8n.pt')

results = model.train(
    data='road-hazards-1/data.yaml',
    epochs=150,
    imgsz=640,
    batch=16,
    device=0,
    project='hazard_training',
    name='yolov8n_hazards',
    
    # More aggressive augmentation for varied road conditions
    hsv_h=0.02,
    hsv_s=0.7,
    hsv_v=0.4,
    degrees=15,
    translate=0.2,
    scale=0.9,
    shear=2.0,
    perspective=0.001,
    flipud=0.0,
    fliplr=0.5,
    mosaic=1.0,
    mixup=0.15  # Mix two images
)

# 3. Evaluate
metrics = model.val()

# 4. Export to TFLite
model.export(
    format='tflite',
    imgsz=640,
    int8=True,
    data='road-hazards-1/data.yaml'
)

# Move file to Android assets
!cp hazard_training/yolov8n_hazards/weights/best-int8.tflite road_hazards.tflite
```

---

## 3️⃣ TFLite Conversion (Manual Method)

If you have a PyTorch `.pt` file and need to convert manually:

```python
# Step 1: PyTorch → ONNX
from ultralytics import YOLO

model = YOLO('best.pt')
model.export(format='onnx', imgsz=640)

# Step 2: ONNX → TensorFlow
!pip install onnx-tf

import onnx
from onnx_tf.backend import prepare

onnx_model = onnx.load('best.onnx')
tf_rep = prepare(onnx_model)
tf_rep.export_graph('model_tf')

# Step 3: TensorFlow → TFLite
import tensorflow as tf

converter = tf.lite.TFLiteConverter.from_saved_model('model_tf')

# Quantization
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.target_spec.supported_types = [tf.float16]  # FP16 quantization

tflite_model = converter.convert()

# Save
with open('model.tflite', 'wb') as f:
    f.write(tflite_model)

print(f"TFLite model size: {len(tflite_model) / 1024 / 1024:.2f} MB")
```

---

## 4️⃣ Model Testing & Validation

### Test TFLite Model
```python
import numpy as np
import tensorflow as tf
from PIL import Image

# Load TFLite model
interpreter = tf.lite.Interpreter(model_path="traffic_signs.tflite")
interpreter.allocate_tensors()

# Get input/output details
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

print(f"Input shape: {input_details[0]['shape']}")
print(f"Output shape: {output_details[0]['shape']}")

# Test inference
def test_inference(image_path):
    # Load and preprocess image
    img = Image.open(image_path).resize((640, 640))
    img_array = np.array(img, dtype=np.float32) / 255.0
    img_array = np.expand_dims(img_array, axis=0)
    
    # Run inference
    interpreter.set_tensor(input_details[0]['index'], img_array)
    interpreter.invoke()
    
    # Get output
    output = interpreter.get_tensor(output_details[0]['index'])
    print(f"Output shape: {output.shape}")
    return output

# Test on sample image
result = test_inference('test_sign.jpg')
```

### Benchmark Performance
```python
import time

# Measure inference time
times = []
for i in range(100):
    start = time.time()
    test_inference('test.jpg')
    times.append(time.time() - start)

print(f"Average inference time: {np.mean(times)*1000:.2f} ms")
print(f"FPS: {1/np.mean(times):.1f}")
```

---

## 5️⃣ Deploy to Android

### Copy Models to Android Project
```bash
# After training and conversion
cp traffic_signs.tflite SafeRoadAI/app/src/main/assets/models/
cp road_hazards.tflite SafeRoadAI/app/src/main/assets/models/
```

### Verify Model Format
The TFLite model output should be in YOLOv8 format:
- **Input**: `[1, 3, 640, 640]` - RGB image
- **Output**: `[1, 84, 8400]` or similar
  - 84 = 4 (bbox coordinates) + 80 (class scores)
  - 8400 = number of detection boxes

### Update Detector Code (if needed)
If your model has different output shape, update `TFLiteDetector.kt`:

```kotlin
// In parseOutput() method
val numDetections = output[0][0].size  // e.g., 8400
val numClasses = output[0].size - 4     // e.g., 80

// Adjust bbox parsing based on your model's output format
```

---

## 6️⃣ Training Tips & Best Practices

### Data Collection
- **Variety**: Different weather, lighting, angles
- **Balance**: Equal samples per class
- **Quality**: Clear, high-resolution images
- **Augmentation**: Let YOLO handle most augmentation

### Hyperparameter Tuning
```python
# Start with default YOLOv8n for speed
model = YOLO('yolov8n.pt')

# If accuracy insufficient, try:
model = YOLO('yolov8s.pt')  # Small (more accurate, slower)
model = YOLO('yolov8m.pt')  # Medium (even better, but slower)

# Key hyperparameters to adjust:
epochs = 100-200  # More for complex datasets
batch = 16-32     # Larger if you have GPU RAM
imgsz = 640       # Higher for small objects (e.g., 800)
patience = 20     # Early stopping patience
```

### Avoiding Overfitting
- Use dropout/regularization (built into YOLO)
- More data augmentation
- Early stopping with patience
- Validate on separate test set

### Improving Accuracy
1. **More data**: Collect 1000+ images per class
2. **Better annotations**: Review and fix labels
3. **Ensemble models**: Combine multiple models
4. **Post-processing**: Adjust confidence threshold
5. **Hard negative mining**: Add difficult examples

---

## 7️⃣ Colab Notebook Template

Complete notebook: [Train_SafeRoadAI_Models.ipynb](https://colab.research.google.com/...)

```python
# Full training pipeline in one notebook
# Upload to your Google Drive for easy access

# Mount Google Drive
from google.colab import drive
drive.mount('/content/drive')

# ... (rest of training code from above)

# Save models to Drive
!cp *.tflite /content/drive/MyDrive/SafeRoadAI/models/
```

---

## 📊 Expected Performance

### Traffic Signs Model
- **mAP50**: 90-95% (with GTSRB dataset)
- **Inference Time**: 50-100ms on mid-range phone
- **Model Size**: 5-10 MB (quantized)

### Road Hazards Model
- **mAP50**: 75-85% (varies by hazard type)
- **Inference Time**: 50-100ms
- **Model Size**: 5-10 MB (quantized)

---

## 🆘 Troubleshooting

### Issue: Low Accuracy
- Collect more training data
- Review annotations for errors
- Try larger model (yolov8s instead of yolov8n)
- Increase training epochs

### Issue: Large Model Size
- Use INT8 quantization
- Reduce input image size
- Use yolov8n (nano) instead of larger variants

### Issue: Slow Inference
- Enable GPU delegate in Android
- Reduce input resolution
- Use quantized model (INT8)

### Issue: Conversion Errors
- Ensure ONNX compatible operations
- Try intermediate ONNX format
- Use latest Ultralytics version

---

## 📚 Additional Resources

- **Ultralytics Docs**: https://docs.ultralytics.com
- **TFLite Guide**: https://www.tensorflow.org/lite/guide
- **Roboflow Tutorial**: https://blog.roboflow.com/train-yolov8
- **Dataset Sources**: https://universe.roboflow.com

---

## 🎓 Next Steps

1. Train both models using provided scripts
2. Test models in Python before Android deployment
3. Copy `.tflite` files to Android assets
4. Run app and validate real-time performance
5. Iterate: collect edge cases, retrain, redeploy

**Good luck with training! 🚀**
