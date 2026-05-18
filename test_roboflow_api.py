#!/usr/bin/env python3
"""
Test Roboflow API with your pothole images
"""

import base64
import requests
import glob
import os

# Roboflow settings
API_URL = "https://detect.roboflow.com"
API_KEY = "wIKONKl0vQde5wH0eXAY"
MODEL_ID = "pothole-detection-i00zy/2"
CONFIDENCE_THRESHOLD = 0.40  # 40%

IMAGE_DIR = "demo_test_images"

def test_roboflow_image(image_path):
    """Test single image with Roboflow API"""
    print(f"\n📷 Testing: {os.path.basename(image_path)}")
    
    try:
        # Read and encode image
        with open(image_path, 'rb') as f:
            image_data = f.read()
        
        base64_image = base64.b64encode(image_data).decode('utf-8')
        
        # API request
        url = f"{API_URL}/{MODEL_ID}?api_key={API_KEY}&confidence={CONFIDENCE_THRESHOLD}"
        
        response = requests.post(
            url,
            data=base64_image,
            headers={'Content-Type': 'application/x-www-form-urlencoded'},
            timeout=10
        )
        
        if response.status_code == 200:
            result = response.json()
            predictions = result.get('predictions', [])
            
            if predictions:
                print(f"   ✅ DETECTED {len(predictions)} potholes!")
                for i, pred in enumerate(predictions[:3], 1):  # Top 3
                    confidence = pred.get('confidence', 0) * 100
                    class_name = pred.get('class', 'pothole')
                    x, y = pred.get('x'), pred.get('y')
                    print(f"      {i}. {class_name}: {confidence:.1f}% at ({x:.0f}, {y:.0f})")
                
                max_conf = max(pred.get('confidence', 0) for pred in predictions) * 100
                return {'status': 'PASS', 'confidence': max_conf, 'count': len(predictions)}
            else:
                print(f"   ❌ NO DETECTION (below {CONFIDENCE_THRESHOLD*100:.0f}% threshold)")
                return {'status': 'FAIL', 'confidence': 0, 'count': 0}
        else:
            print(f"   ❌ API ERROR: {response.status_code}")
            print(f"   Response: {response.text[:200]}")
            return {'status': 'ERROR', 'confidence': 0, 'count': 0}
            
    except Exception as e:
        print(f"   ❌ ERROR: {e}")
        return {'status': 'ERROR', 'confidence': 0, 'count': 0}

def main():
    print("=" * 70)
    print("🌐 ROBOFLOW API POTHOLE DETECTION TEST")
    print("=" * 70)
    print(f"API: {API_URL}")
    print(f"Model: {MODEL_ID}")
    print(f"Confidence Threshold: {CONFIDENCE_THRESHOLD*100:.0f}%")
    print("=" * 70)
    
    # Find images
    image_files = glob.glob(os.path.join(IMAGE_DIR, "*.jpg")) + \
                  glob.glob(os.path.join(IMAGE_DIR, "*.png")) + \
                  glob.glob(os.path.join(IMAGE_DIR, "*.jpeg"))
    
    if not image_files:
        print(f"\n❌ No images found in {IMAGE_DIR}")
        return
    
    print(f"\n🖼️ Found {len(image_files)} images")
    
    # Test each image
    results = []
    for img_path in sorted(image_files):
        result = test_roboflow_image(img_path)
        result['image'] = os.path.basename(img_path)
        results.append(result)
    
    # Summary
    print("\n" + "=" * 70)
    print("📊 SUMMARY - Roboflow vs Local Model")
    print("=" * 70)
    
    pass_count = sum(1 for r in results if r['status'] == 'PASS')
    fail_count = sum(1 for r in results if r['status'] == 'FAIL')
    error_count = sum(1 for r in results if r['status'] == 'ERROR')
    
    print(f"\n✅ Detected by Roboflow (≥40%): {pass_count}/{len(results)}")
    print(f"❌ Not detected (<40%): {fail_count}/{len(results)}")
    print(f"💥 Errors: {error_count}/{len(results)}")
    
    print(f"\n📋 Comparison:")
    print(f"{'Image':<20} {'Local Model':<20} {'Roboflow':<20}")
    print("-" * 70)
    
    for r in results:
        local_conf = "1.3-1.38% ⚠️"  # Your weak model
        if r['status'] == 'PASS':
            roboflow_result = f"{r['confidence']:.1f}% ✅ ({r['count']} detected)"
        elif r['status'] == 'FAIL':
            roboflow_result = "<40% ❌"
        else:
            roboflow_result = "ERROR 💥"
        
        print(f"{r['image']:<20} {local_conf:<20} {roboflow_result:<20}")
    
    print("\n" + "=" * 70)
    print("💡 RECOMMENDATION:")
    print("=" * 70)
    
    if pass_count >= len(results) * 0.8:
        print("✅ Roboflow works MUCH better than local model!")
        print("   Use Roboflow for accurate pothole detection")
        print("   Local model only as offline fallback")
    elif pass_count >= len(results) * 0.5:
        print("⚠️ Roboflow detects more than local model")
        print("   Use Roboflow when internet available")
    else:
        print("❌ Roboflow needs better images or different model")
        print("   Try better lighting or closer shots")
    
    print("\n✅ Test complete!")

if __name__ == "__main__":
    main()
