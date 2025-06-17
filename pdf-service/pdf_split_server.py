from flask import Flask, request, jsonify
from pdf2image import convert_from_path
import os

app = Flask(__name__)

@app.route('/split', methods=['POST'])
def split_pdf():
    data = request.json
    pdf_path = data.get('pdf_path')
    output_folder = data.get('output_folder')

    if not pdf_path or not os.path.exists(pdf_path):
        return jsonify({"error": "PDF 文件不存在"}), 400

    if not os.path.exists(output_folder):
        os.makedirs(output_folder)

    try:
        images = convert_from_path(pdf_path)
        for idx, image in enumerate(images, start=1):
            output_file = os.path.join(output_folder, f'page{idx}.png')
            image.save(output_file, 'PNG')
        return jsonify({"message": f"转换完成，图片保存至 {output_folder}"}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)