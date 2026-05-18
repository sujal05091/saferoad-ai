#!/usr/bin/env python3
"""
Diagnostic script to test pothole detection model
"""

import tensorflow as tf
import numpy as np
from PIL import Image
import os

# Path to your pothole model
MODEL_PATH = "app/src/main/assets/models/patholes.tflite"

print("=" * 70)
print("🔍 POTHOLE MODEL DIAGNOSTIC TEST")
print("=" * 70)

# Check if model exists
if not os.path.exists(MODEL_PATH):
    print(f"\n❌ ERROR: Model not found at {MODEL_PATH}")
    print("\n📝 Please ensure:")
    print("   1. patholes.tflite is in app/src/main/assets/models/")
    print("   2. File name is exactly 'patholes.tflite' (check spelling)")
    exit(1)

print(f"\n✅ Model found: {MODEL_PATH}")
file_size = os.path.getsize(MODEL_PATH) / (1024 * 1024)
print(f"📏 File size: {file_size:.2f} MB")

# Load the model
print("\n📦 Loading TFLite model...")
interpreter = tf.lite.Interpreter(model_path=MODEL_PATH)
interpreter.allocate_tensors()

# Get input details
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

print("\n" + "=" * 70)
print("📊 MODEL INFORMATION")
print("=" * 70)

print("\n🔍 INPUT DETAILS:")
for i, detail in enumerate(input_details):
    print(f"   Input {i}:")
    print(f"      Name: {detail['name']}")
    print(f"      Shape: {detail['shape']}")
    print(f"      Type: {detail['dtype']}")

print("\n🔍 OUTPUT DETAILS:")
for i, detail in enumerate(output_details):
    print(f"   Output {i}:")
    print(f"      Name: {detail['name']}")
    print(f"      Shape: {detail['shape']}")
    print(f"      Type: {detail['dtype']}")

# Create dummy input (640x640x3 RGB image, normalized to [0,1])
input_shape = input_details[0]['shape']
print(f"\n📐 Creating test input with shape: {input_shape}")

# Generate a test image (gray road with a dark spot simulating pothole)
test_img = np.ones((640, 640, 3), dtype=np.float32) * 0.5  # Gray background
# Add a dark "pothole" in the center
test_img[280:360, 280:360, :] = 0.1  # Dark spot

# Reshape to match model input
test_input = test_img.reshape(input_shape).astype(np.float32)

print(f"   Input data type: {test_input.dtype}")
print(f"   Input value range: [{test_input.min():.3f}, {test_input.max():.3f}]")

# Run inference
print("\n🧠 Running inference...")
interpreter.set_tensor(input_details[0]['index'], test_input)
interpreter.invoke()

# Get output
output_data = interpreter.get_tensor(output_details[0]['index'])
print(f"✅ Inference complete!")

print("\n" + "=" * 70)
print("📊 MODEL OUTPUT ANALYSIS")
print("=" * 70)

print(f"\n📐 Output shape: {output_data.shape}")
print(f"   Expected: [1, 5, 8400] for 1-class pothole model")

# Analyze output
if len(output_data.shape) == 3:
    batch, channels, detections = output_data.shape
    print(f"\n✅ Output format looks correct!")
    print(f"   Batch: {batch}")
    print(f"   Channels: {channels} (4 bbox + {channels-4} classes)")
    print(f"   Detections: {detections}")
    
    # Check for any detections with confidence > threshold
    confidence_threshold = 0.05
    
    # Extract confidence scores (class confidence is in channel 4 for 1-class model)
    if channels == 5:  # 4 bbox + 1 class
        confidences = output_data[0, 4, :]
        max_conf = np.max(confidences)
        num_above_threshold = np.sum(confidences > confidence_threshold)
        
        print(f"\n🎯 CONFIDENCE ANALYSIS:")
        print(f"   Threshold: {confidence_threshold:.2%}")
        print(f"   Max confidence: {max_conf:.4f} ({max_conf*100:.2f}%)")
        print(f"   Detections above threshold: {num_above_threshold}")
        
        if max_conf > confidence_threshold:
            print(f"\n✅ Model CAN detect! (max conf: {max_conf*100:.2f}%)")
            
            # Find top 5 detections
            top_indices = np.argsort(confidences)[-5:][::-1]
            print(f"\n📊 Top 5 confidence scores:")
            for idx in top_indices:
                print(f"      Detection {idx}: {confidences[idx]:.4f} ({confidences[idx]*100:.2f}%)")
        else:
            print(f"\n⚠️ WARNING: Max confidence ({max_conf*100:.2f}%) is below threshold!")
            print(f"   Model is working but NOT detecting anything strongly")
            print(f"\n💡 POSSIBLE REASONS:")
            print(f"      1. Model was trained on very specific pothole types")
            print(f"      2. Test input doesn't match training data characteristics")
            print(f"      3. Model needs more training or better data")
            print(f"      4. Confidence threshold in app is too high")
    else:
        print(f"\n⚠️ Unexpected number of channels: {channels}")
        print(f"   Expected 5 for 1-class model (4 bbox + 1 class)")
else:
    print(f"\n❌ Unexpected output shape: {output_data.shape}")

print("\n" + "=" * 70)
print("📱 APP CONFIGURATION CHECK")
print("=" * 70)

print("\n✅ Verify in your app:")
print("   RoadHazardDetector:")
print("      inputSize = 640")
print("      numClasses = 1")
print("      CONFIDENCE_THRESHOLD = 0.05 (in TFLiteDetector.kt)")
print("\n   If traffic signs work but potholes don't:")
print("      → Pothole model needs retraining with more/better data")
print("      → OR use same dataset quality as traffic signs")

print("\n" + "=" * 70)
print("✨ DIAGNOSTIC COMPLETE")
print("=" * 70)
