from flask import Flask, request, jsonify
from flask_cors import CORS
from ultralytics import YOLO
import numpy as np
from PIL import Image
import io
import os
from dotenv import load_dotenv

# 加载环境变量
load_dotenv()

app = Flask(__name__)
CORS(app)

# 初始化YOLOv8模型
model_path = os.getenv('YOLO_MODEL_PATH', 'yolov8n.pt')
confidence_threshold = float(os.getenv('YOLO_CONFIDENCE_THRESHOLD', 0.5))
model = YOLO(model_path)

def process_image(image_data):
    """处理图片并返回检测结果"""
    try:
        # 将图片数据转换为PIL Image对象
        image = Image.open(io.BytesIO(image_data))
        
        # 执行目标检测
        results = model(image, conf=confidence_threshold)
        
        # 处理检测结果
        detections = []
        for result in results:
            boxes = result.boxes
            for box in boxes:
                if float(box.conf[0]) >= confidence_threshold:
                    detection = {
                        'class': int(box.cls[0]),
                        'class_name': model.names[int(box.cls[0])],
                        'confidence': float(box.conf[0]),
                        'bbox': box.xyxy[0].tolist()  # [x1, y1, x2, y2]
                    }
                    detections.append(detection)
        
        return detections
    except Exception as e:
        print(f"Error processing image: {str(e)}")
        return None

@app.route('/detect', methods=['POST'])
def detect():
    """处理图片检测请求"""
    try:
        # 检查是否有文件上传
        if 'image' not in request.files:
            return jsonify({'error': 'No image file provided'}), 400
        
        file = request.files['image']
        if file.filename == '':
            return jsonify({'error': 'No selected file'}), 400
        
        # 读取图片数据
        image_data = file.read()
        
        # 处理图片
        detections = process_image(image_data)
        
        if detections is None:
            return jsonify({'error': 'Failed to process image'}), 500
        
        return jsonify({
            'success': True,
            'detections': detections
        })
    
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/health', methods=['GET'])
def health_check():
    """健康检查接口"""
    return jsonify({'status': 'healthy'})

if __name__ == '__main__':
    # 获取端口配置，默认为5000
    port = int(os.getenv('PORT', 5000))
    app.run(host='0.0.0.0', port=port) 