import tensorflow as tf
import numpy as np
from PIL import Image
import os

def test_model_inference(model_path, test_image_path):
    """Test if model can actually run inference and produce outputs"""
    print(f"\n{'='*70}")
    print(f"Testing: {model_path}")
    print(f"{'='*70}")
    
    try:
        # Load model
        interpreter = tf.lite.Interpreter(model_path=model_path)
        interpreter.allocate_tensors()
        
        # Get input/output details
        input_details = interpreter.get_input_details()
        output_details = interpreter.get_output_details()
        
        input_shape = input_details[0]['shape']
        input_size = input_shape[1]
        
        print(f"\n📊 Model Info:")
        print(f"   Input shape: {input_shape}")
        print(f"   Output shape: {output_details[0]['shape']}")
        print(f"   Input size: {input_size}x{input_size}")
        
        # Load and preprocess test image
        if os.path.exists(test_image_path):
            img = Image.open(test_image_path).convert('RGB')
            print(f"\n📷 Test Image: {test_image_path}")
            print(f"   Original size: {img.size}")
            
            # Resize to model input size
            img_resized = img.resize((input_size, input_size))
            print(f"   Resized to: {img_resized.size}")
            
            # Convert to numpy array and normalize
            img_array = np.array(img_resized, dtype=np.float32)
            img_array = img_array / 255.0  # Normalize to [0, 1]
            img_array = np.expand_dims(img_array, axis=0)  # Add batch dimension
            
            print(f"   Input array shape: {img_array.shape}")
            print(f"   Input array range: [{img_array.min():.3f}, {img_array.max():.3f}]")
            
            # Run inference
            print(f"\n🧠 Running inference...")
            interpreter.set_tensor(input_details[0]['index'], img_array)
            interpreter.invoke()
            
            # Get output
            output_data = interpreter.get_tensor(output_details[0]['index'])
            print(f"   Output shape: {output_data.shape}")
            
            # Analyze output
            output_shape = output_data.shape
            if len(output_shape) == 3:  # [1, channels, detections]
                num_channels = output_shape[1]
                num_detections = output_shape[2]
                num_classes = num_channels - 4
                
                print(f"\n📈 Output Analysis:")
                print(f"   Channels: {num_channels}")
                print(f"   Detections: {num_detections}")
                print(f"   Classes: {num_classes}")
                
                # Check confidence scores for all detections
                print(f"\n🔍 Checking confidence scores...")
                max_conf_overall = 0
                max_conf_class = -1
                detections_above_01 = 0
                detections_above_015 = 0
                detections_above_02 = 0
                detections_above_03 = 0
                
                for i in range(num_detections):
                    # Get confidence scores for all classes
                    for c in range(4, num_channels):
                        conf = output_data[0, c, i]
                        if conf > max_conf_overall:
                            max_conf_overall = conf
                            max_conf_class = c - 4
                        
                        if conf > 0.1:
                            detections_above_01 += 1
                        if conf > 0.15:
                            detections_above_015 += 1
                        if conf > 0.2:
                            detections_above_02 += 1
                        if conf > 0.3:
                            detections_above_03 += 1
                
                print(f"\n📊 Detection Statistics:")
                print(f"   Max confidence found: {max_conf_overall:.4f} (class {max_conf_class})")
                print(f"   Detections > 0.10: {detections_above_01}")
                print(f"   Detections > 0.15: {detections_above_015}")
                print(f"   Detections > 0.20: {detections_above_02}")
                print(f"   Detections > 0.30: {detections_above_03}")
                
                # Show top 5 detections
                print(f"\n🔝 Top 5 Confidence Scores:")
                all_confidences = []
                for i in range(num_detections):
                    for c in range(4, num_channels):
                        all_confidences.append((output_data[0, c, i], c-4, i))
                
                all_confidences.sort(reverse=True)
                for idx, (conf, cls, det) in enumerate(all_confidences[:5]):
                    print(f"   {idx+1}. Confidence: {conf:.4f} | Class: {cls} | Detection: {det}")
                
                # Verdict
                print(f"\n{'='*70}")
                if max_conf_overall < 0.01:
                    print("❌ VERDICT: Model is NOT detecting anything!")
                    print("   Problem: All confidence scores are extremely low")
                    print("   Likely causes:")
                    print("   1. Model not trained properly (too few epochs/images)")
                    print("   2. Input preprocessing mismatch")
                    print("   3. Model corrupted during export")
                elif max_conf_overall < 0.15:
                    print("⚠️ VERDICT: Model has very low confidence")
                    print("   Max confidence is below detection threshold (0.15)")
                    print("   Likely causes:")
                    print("   1. Model undertrained (need more epochs/images)")
                    print("   2. Test image very different from training data")
                    print("   3. Input normalization issue")
                elif max_conf_overall < 0.30:
                    print("✅ VERDICT: Model is working but with low confidence")
                    print("   Should detect with current threshold (0.15)")
                    print("   Consider:")
                    print("   1. Training with more images")
                    print("   2. More training epochs")
                    print("   3. Better quality training images")
                else:
                    print("✅ VERDICT: Model is working well!")
                    print("   Should detect objects reliably")
                print(f"{'='*70}")
                
        else:
            print(f"\n⚠️ Test image not found: {test_image_path}")
            print("   Run inference without test image...")
            
            # Create dummy input
            dummy_input = np.random.rand(1, input_size, input_size, 3).astype(np.float32)
            interpreter.set_tensor(input_details[0]['index'], dummy_input)
            interpreter.invoke()
            output_data = interpreter.get_tensor(output_details[0]['index'])
            print(f"   Model runs successfully!")
            print(f"   Output shape: {output_data.shape}")
            
    except Exception as e:
        print(f"\n❌ ERROR: {e}")
        import traceback
        traceback.print_exc()

# Test both models
print("\n" + "="*70)
print("MODEL INFERENCE TEST - Checking if models can detect")
print("="*70)

# Test pothole model
test_model_inference(
    "app/src/main/assets/models/patholes.tflite",
    "test_pothole.jpg"  # Replace with actual test image
)

# Test traffic sign model
test_model_inference(
    "app/src/main/assets/models/traffic_signs.tflite.tflite",
    "test_sign.jpg"  # Replace with actual test image
)

print("\n" + "="*70)
print("RECOMMENDATION:")
print("="*70)
print("If max confidence < 0.01:")
print("  → Model is broken or not trained")
print("  → Retrain with more images (50+ per class)")
print("  → Train for more epochs (50-100)")
print("\nIf max confidence < 0.15:")
print("  → Model undertrained")
print("  → Add more diverse training images")
print("  → Increase training epochs")
print("\nIf max confidence > 0.15:")
print("  → Model should work!")
print("  → Check app code/thresholds")
print("="*70)
