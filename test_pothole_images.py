#!/usr/bin/env python3
"""
Test pothole detection model on demo images
"""

import tensorflow as tf
import numpy as np
from PIL import Image
import os
import glob

# Model path
MODEL_PATH = r"app\src\main\assets\models\patholes.tflite"
IMAGE_DIR = r"demo_test_images"

# Load TFLite model
print("Loading model...")
interpreter = tf.lite.Interpreter(model_path=MODEL_PATH)
interpreter.allocate_tensors()

# Get input/output details
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

print(f"\n📊 Model Details:")
print(f"Input shape: {input_details[0]['shape']}")
print(f"Input dtype: {input_details[0]['dtype']}")
print(f"Output shape: {output_details[0]['shape']}")
print(f"Output dtype: {output_details[0]['dtype']}")

# Get all images
image_files = glob.glob(os.path.join(IMAGE_DIR, "*.jpg")) + \
              glob.glob(os.path.join(IMAGE_DIR, "*.png")) + \
              glob.glob(os.path.join(IMAGE_DIR, "*.jpeg"))

print(f"\n🖼️ Found {len(image_files)} test images")
print("=" * 70)

if len(image_files) == 0:
    print(f"\n❌ No images found in {IMAGE_DIR}")
    print(f"Please save your pothole images to: {os.path.abspath(IMAGE_DIR)}")
    print("Supported formats: .jpg, .png, .jpeg")
    exit(1)

# Test each image
results = []

for idx, img_path in enumerate(sorted(image_files), 1):
    print(f"\n📷 Image {idx}: {os.path.basename(img_path)}")
    
    try:
        # Load and preprocess image
        img = Image.open(img_path).convert('RGB')
        original_size = img.size
        
        # Resize to 640x640
        img_resized = img.resize((640, 640), Image.BILINEAR)
        
        # Convert to numpy array and normalize to [0, 1]
        img_array = np.array(img_resized, dtype=np.float32) / 255.0
        
        # Add batch dimension: [1, 640, 640, 3]
        input_data = np.expand_dims(img_array, axis=0)
        
        # Run inference
        interpreter.set_tensor(input_details[0]['index'], input_data)
        interpreter.invoke()
        
        # Get output: [1, 5, 8400] for pothole model
        output_data = interpreter.get_tensor(output_details[0]['index'])
        
        # Parse detections
        # Format: [batch, channels, detections]
        # channels: [x, y, w, h, confidence]
        
        detections = []
        num_detections = output_data.shape[2]  # 8400
        
        for i in range(num_detections):
            # Get confidence (5th channel = class confidence for single class)
            confidence = output_data[0, 4, i]
            
            if confidence >= 0.01:  # 1% threshold for testing
                x_center = output_data[0, 0, i]
                y_center = output_data[0, 1, i]
                width = output_data[0, 2, i]
                height = output_data[0, 3, i]
                
                detections.append({
                    'confidence': float(confidence),
                    'bbox': [float(x_center), float(y_center), float(width), float(height)]
                })
        
        # Sort by confidence
        detections.sort(key=lambda x: x['confidence'], reverse=True)
        
        # Show results
        if detections:
            max_conf = detections[0]['confidence']
            print(f"   ✅ DETECTED!")
            print(f"   📊 Total detections (>1%): {len(detections)}")
            print(f"   🎯 Max confidence: {max_conf*100:.2f}%")
            print(f"   🔝 Top 3 confidences:")
            for j, det in enumerate(detections[:3], 1):
                print(f"      {j}. {det['confidence']*100:.2f}%")
            
            # Verdict
            if max_conf >= 0.05:
                print(f"   ✅ WILL DETECT in app (5% threshold)")
                results.append({'image': os.path.basename(img_path), 'status': 'PASS', 'confidence': max_conf})
            elif max_conf >= 0.01:
                print(f"   ⚠️ WEAK detection (1-5%)")
                results.append({'image': os.path.basename(img_path), 'status': 'WEAK', 'confidence': max_conf})
            else:
                print(f"   ❌ Too weak (<1%)")
                results.append({'image': os.path.basename(img_path), 'status': 'FAIL', 'confidence': max_conf})
        else:
            print(f"   ❌ NO DETECTION")
            print(f"   📊 Max confidence found: < 1%")
            results.append({'image': os.path.basename(img_path), 'status': 'NONE', 'confidence': 0.0})
    
    except Exception as e:
        print(f"   ❌ ERROR: {e}")
        results.append({'image': os.path.basename(img_path), 'status': 'ERROR', 'confidence': 0.0})

# Summary
print("\n" + "=" * 70)
print("📊 SUMMARY")
print("=" * 70)

pass_count = sum(1 for r in results if r['status'] == 'PASS')
weak_count = sum(1 for r in results if r['status'] == 'WEAK')
fail_count = sum(1 for r in results if r['status'] in ['FAIL', 'NONE', 'ERROR'])

print(f"\n✅ Will detect in app (≥5%): {pass_count}/{len(results)}")
print(f"⚠️ Weak detection (1-5%): {weak_count}/{len(results)}")
print(f"❌ Won't detect (<1%): {fail_count}/{len(results)}")

print(f"\n📋 Detailed Results:")
for r in results:
    status_icon = {
        'PASS': '✅',
        'WEAK': '⚠️',
        'FAIL': '❌',
        'NONE': '❌',
        'ERROR': '💥'
    }.get(r['status'], '❓')
    
    print(f"   {status_icon} {r['image']}: {r['confidence']*100:.2f}% - {r['status']}")

print("\n" + "=" * 70)
print("💡 RECOMMENDATIONS:")
print("=" * 70)

if pass_count >= len(results) * 0.7:
    print("✅ Model works well with these images!")
    print("   Your demo should work with 5% threshold")
elif pass_count + weak_count >= len(results) * 0.7:
    print("⚠️ Model is weak but usable")
    print("   Lower threshold to 1% for better demo results")
    print("   Or retrain model with more/better images")
else:
    print("❌ Model too weak for these images")
    print("   Options:")
    print("   1. Lower threshold to 1% (may have false positives)")
    print("   2. Retrain model with 50-100 images")
    print("   3. Use different test images (clearer/closer)")

print("\n✅ Test complete!")
