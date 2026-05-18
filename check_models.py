import tensorflow as tf
import numpy as np

def inspect_tflite_model(model_path):
    """Inspect TFLite model to determine input/output shapes and num classes"""
    print(f"\n{'='*60}")
    print(f"Inspecting: {model_path}")
    print(f"{'='*60}")
    
    try:
        # Load the TFLite model
        interpreter = tf.lite.Interpreter(model_path=model_path)
        interpreter.allocate_tensors()
        
        # Get input details
        input_details = interpreter.get_input_details()
        output_details = interpreter.get_output_details()
        
        print("\n📥 INPUT DETAILS:")
        for i, detail in enumerate(input_details):
            print(f"  Input {i}:")
            print(f"    Shape: {detail['shape']}")
            print(f"    Type: {detail['dtype']}")
            print(f"    Name: {detail['name']}")
        
        print("\n📤 OUTPUT DETAILS:")
        for i, detail in enumerate(output_details):
            print(f"  Output {i}:")
            print(f"    Shape: {detail['shape']}")
            print(f"    Type: {detail['dtype']}")
            print(f"    Name: {detail['name']}")
            
            # Calculate num classes from output shape
            shape = detail['shape']
            if len(shape) == 3:  # [1, channels, detections]
                num_channels = shape[1]
                num_detections = shape[2]
                num_classes = num_channels - 4  # Subtract bbox coords
                
                print(f"\n📊 ANALYSIS:")
                print(f"    Output channels: {num_channels}")
                print(f"    Number of detections: {num_detections}")
                print(f"    👉 Number of classes: {num_classes}")
                print(f"    Format: [batch, {num_channels}, {num_detections}]")
                print(f"    First 4 channels = bbox (cx, cy, w, h)")
                print(f"    Next {num_classes} channels = class scores")
        
        print(f"\n{'='*60}\n")
        
    except Exception as e:
        print(f"❌ Error inspecting model: {e}\n")

# Inspect both models
print("\n🔍 CHECKING YOUR TRAINED MODELS\n")
inspect_tflite_model("app/src/main/assets/models/patholes.tflite")
inspect_tflite_model("app/src/main/assets/models/traffic_signs.tflite.tflite")

print("\n✅ INSPECTION COMPLETE!")
print("\nNOTE: Use the 'Number of classes' value to update your detector configurations:")
print("  - RoadHazardDetector: Update inputSize and numClasses")
print("  - TrafficSignDetector: Update inputSize and numClasses")
