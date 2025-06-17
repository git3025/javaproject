# YOLOv8 Flask Service

这是一个基于Flask的Python服务，集成了YOLOv8模型用于目标检测。

## 功能特点

- 基于Flask的RESTful API
- 集成YOLOv8目标检测模型
- 支持图片上传和检测结果返回
- CORS支持
- 环境变量配置

## 安装

1. 创建虚拟环境（推荐）：
```bash
python -m venv venv
source venv/bin/activate  # Linux/Mac
venv\Scripts\activate     # Windows
```

2. 安装依赖：
```bash
pip install -r requirements.txt
```

3. 下载YOLOv8模型（首次运行时会自动下载）

## 运行服务

开发环境：
```bash
python app.py
```

生产环境：
```bash
gunicorn -w 4 -b 0.0.0.0:5000 app:app
```

## API 使用

### 图片检测

**端点**: `POST /detect`

**请求格式**:
- Content-Type: multipart/form-data
- 参数: image (文件)

**响应格式**:
```json
{
    "success": true,
    "detections": [
        {
            "class": 0,
            "class_name": "person",
            "confidence": 0.95,
            "bbox": [x1, y1, x2, y2]
        }
    ]
}
```

## 性能优化

1. 使用gunicorn多工作进程
2. 模型推理优化
3. 图片预处理优化

## 注意事项

1. 确保有足够的内存运行YOLOv8模型
2. 建议使用GPU进行推理
3. 生产环境部署时注意安全性配置 