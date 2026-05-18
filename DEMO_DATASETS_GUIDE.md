# 📊 Small Demo Datasets Guide

This guide provides links to download small datasets for **demo purposes** and instructions on how to prepare and use them with the training notebooks.

---

## 🚦 Traffic Signs - Small Demo Datasets

### Option 1: Download Pre-labeled Traffic Sign Images
**Source**: Roboflow Public Datasets
- **Dataset**: [Traffic Signs Detection (Small)](https://public.roboflow.com/object-detection/road-signs)
- **Size**: ~100-200 images
- **Classes**: Multiple traffic sign types
- **Format**: YOLO format (ready to use)
- **Link**: https://public.roboflow.com/object-detection/road-signs

**How to Download:**
1. Visit: https://public.roboflow.com/object-detection/road-signs
2. Click "Download Dataset"
3. Select "YOLO v8" format
4. Download ZIP file
5. Extract to get `images/` and `labels/` folders

---

### Option 2: Kaggle Traffic Signs Dataset (Subset)
**Source**: Kaggle
- **Dataset**: [Traffic Signs Dataset](https://www.kaggle.com/datasets/ahemateja19bec1025/traffic-sign-dataset-classification)
- **Action**: Download and create subset (20-50 images)
- **Format**: Needs YOLO annotation (use Roboflow or LabelImg)
- **Link**: https://www.kaggle.com/datasets/ahemateja19bec1025/traffic-sign-dataset-classification

---

### Option 3: Collect Your Own Images
**Best for Demo**: Take 20-50 photos of traffic signs around your area

**Tools for Labeling:**
1. **Roboflow** (Recommended): https://roboflow.com/
   - Upload images
   - Draw bounding boxes
   - Export in YOLO format
   - Free for small datasets

2. **LabelImg**: https://github.com/HumanSignal/labelImg
   - Desktop tool
   - Save in YOLO format
   - Open source

---

## 🕳️ Potholes - Small Demo Datasets

### Option 1: Roboflow Pothole Dataset (Small)
**Source**: Roboflow Public Datasets
- **Dataset**: [Pothole Detection](https://universe.roboflow.com/pothole-detection-nfvsr/pothole-segmentation-3swyf)
- **Size**: ~100-150 images
- **Classes**: pothole
- **Format**: YOLO format (ready to use)
- **Link**: https://universe.roboflow.com/pothole-detection-nfvsr/pothole-segmentation-3swyf

**How to Download:**
1. Visit: https://universe.roboflow.com/pothole-detection-nfvsr/pothole-segmentation-3swyf
2. Click "Download Dataset"
3. Select "YOLO v8" format
4. Download ZIP file
5. Extract to get `images/` and `labels/` folders

---

### Option 2: Kaggle Pothole Dataset (Subset)
**Source**: Kaggle
- **Dataset**: [Pothole Image Dataset](https://www.kaggle.com/datasets/atulyakumar98/pothole-detection-dataset)
- **Action**: Download and create subset (20-50 images)
- **Format**: Needs YOLO annotation
- **Link**: https://www.kaggle.com/datasets/atulyakumar98/pothole-detection-dataset

---

### Option 3: GitHub Pothole Dataset
**Source**: GitHub
- **Dataset**: [Annotated Potholes](https://github.com/mshaban/Pothole-Detection)
- **Size**: Small dataset with annotations
- **Format**: May need conversion to YOLO
- **Link**: https://github.com/mshaban/Pothole-Detection

---

### Option 4: Collect Your Own Images
**Best for Demo**: Take 20-50 photos of potholes on roads

**Labeling Tips:**
- Draw tight bounding boxes around potholes
- Include various sizes and shapes
- Different lighting conditions

---

## 📁 Required Dataset Structure

After downloading or collecting data, organize it like this:

### For Traffic Signs:
```
traffic_signs_mini/
├── images/
│   ├── train/
│   │   ├── img001.jpg
│   │   ├── img002.jpg
│   │   └── ...
│   └── val/
│       ├── img051.jpg
│       └── ...
└── labels/
    ├── train/
    │   ├── img001.txt
    │   ├── img002.txt
    │   └── ...
    └── val/
        ├── img051.txt
        └── ...
```

### For Potholes:
```
potholes_mini/
├── images/
│   ├── train/
│   │   ├── img001.jpg
│   │   ├── img002.jpg
│   │   └── ...
│   └── val/
│       ├── img051.jpg
│       └── ...
└── labels/
    ├── train/
    │   ├── img001.txt
    │   ├── img002.txt
    │   └── ...
    └── val/
        ├── img051.txt
        └── ...
```

---

## 📝 YOLO Label Format

Each `.txt` file should contain one line per object:
```
class_id x_center y_center width height
```

**All values normalized (0-1)**:
- `class_id`: Integer (0, 1, 2, etc.)
- `x_center`: Center X coordinate (0.0 to 1.0)
- `y_center`: Center Y coordinate (0.0 to 1.0)
- `width`: Box width (0.0 to 1.0)
- `height`: Box height (0.0 to 1.0)

**Example** (`img001.txt`):
```
0 0.5 0.5 0.3 0.4
1 0.2 0.3 0.15 0.2
```

---

## ☁️ Upload to Google Drive

### Step 1: Create Folder Structure in Google Drive
1. Open Google Drive
2. Create folder: `traffic_signs_mini` (or `potholes_mini`)
3. Inside, create: `images` and `labels` folders
4. Inside each, create: `train` and `val` folders

### Step 2: Upload Files
1. Upload training images to: `traffic_signs_mini/images/train/`
2. Upload training labels to: `traffic_signs_mini/labels/train/`
3. Upload validation images to: `traffic_signs_mini/images/val/`
4. Upload validation labels to: `traffic_signs_mini/labels/val/`

### Step 3: Verify Structure
Your Google Drive should look like:
```
MyDrive/
└── traffic_signs_mini/
    ├── images/
    │   ├── train/ (30-40 .jpg files)
    │   └── val/ (10-15 .jpg files)
    └── labels/
        ├── train/ (30-40 .txt files)
        └── val/ (10-15 .txt files)
```

---

## 🎯 Recommended Dataset Sizes for Demo

### For Quick Testing (10-15 minutes training):
- **Training images**: 20-30
- **Validation images**: 5-10
- **Total**: 25-40 images

### For Better Demo Results (20-30 minutes training):
- **Training images**: 40-60
- **Validation images**: 10-15
- **Total**: 50-75 images

---

## 🚀 Using the Notebooks

### For Traffic Signs:
1. Upload dataset to: `/content/drive/MyDrive/traffic_signs_mini`
2. Open: `traffic_minidata.ipynb` in Google Colab
3. Update `DATASET_PATH` in Step 3
4. Update `CLASS_NAMES` in Step 4 (match your classes)
5. Run all cells
6. Download model from: `/content/drive/MyDrive/RoadAI_Models/traffic_signs_mini.tflite`

### For Potholes:
1. Upload dataset to: `/content/drive/MyDrive/potholes_mini`
2. Open: `pothole_minidata.ipynb` in Google Colab
3. Update `DATASET_PATH` in Step 3
4. Run all cells
5. Download model from: `/content/drive/MyDrive/RoadAI_Models/patholes_mini.tflite`

---

## 📱 Integrating Models into App

### Traffic Signs Model:
1. Download: `traffic_signs_mini.tflite` from Google Drive
2. Rename to: `traffic_signs.tflite.tflite`
3. Replace in: `app/src/main/assets/models/traffic_signs.tflite.tflite`
4. Update `TrafficSignDetector.kt`:
   - Change `numClasses` to match your dataset
   - Update `TRAFFIC_SIGN_LABELS` map
5. Rebuild: `.\gradlew assembleDebug`

### Potholes Model:
1. Download: `patholes_mini.tflite` from Google Drive
2. Rename to: `patholes.tflite`
3. Replace in: `app/src/main/assets/models/patholes.tflite`
4. Rebuild: `.\gradlew assembleDebug`

---

## ⚠️ Important Notes

### Dataset Quality Over Quantity:
- **For demo**: 30-50 good quality images > 100 poor quality images
- **Diverse conditions**: Different lighting, angles, distances
- **Accurate labels**: Tight bounding boxes around objects

### Training Time Estimates:
- **20-30 images**: 10-15 minutes
- **40-60 images**: 20-30 minutes
- **Uses**: Google Colab free GPU

### Accuracy Expectations:
- **Small dataset**: 60-75% accuracy (demo only)
- **Production**: Need 500+ images for 85%+ accuracy

### Tips for Demo Success:
1. **Use clear, well-lit images**
2. **Label carefully** (accurate bounding boxes)
3. **Include variety** (different objects, angles)
4. **Test thoroughly** after training
5. **Start small** (20-30 images) to verify workflow

---

## 🔗 Quick Links Summary

### Traffic Signs:
- Roboflow: https://public.roboflow.com/object-detection/road-signs
- Kaggle: https://www.kaggle.com/datasets/ahemateja19bec1025/traffic-sign-dataset-classification

### Potholes:
- Roboflow: https://universe.roboflow.com/pothole-detection-nfvsr/pothole-segmentation-3swyf
- Kaggle: https://www.kaggle.com/datasets/atulyakumar98/pothole-detection-dataset
- GitHub: https://github.com/mshaban/Pothole-Detection

### Annotation Tools:
- Roboflow: https://roboflow.com/ (Online, YOLO export)
- LabelImg: https://github.com/HumanSignal/labelImg (Desktop)

---

## 📞 Need Help?

If you encounter issues:
1. **Dataset structure**: Verify folders match required structure
2. **Label format**: Check YOLO format (class_id x y w h)
3. **File names**: Ensure image and label files match
4. **Google Drive path**: Update `DATASET_PATH` correctly
5. **Class names**: Update `CLASS_NAMES` to match your data

**Common Errors:**
- "No training images found" → Check `DATASET_PATH`
- "Mismatch between images and labels" → Ensure same filenames
- "Training fails" → Reduce epochs or batch size

---

Good luck with your demo! 🚀
