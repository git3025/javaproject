#!/bin/bash

# 确保脚本在错误时退出
set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 打印带颜色的消息
print_message() {
    echo -e "${2}${1}${NC}"
}

# 检查必要的命令
check_commands() {
    print_message "检查必要的命令..." "${YELLOW}"
    
    commands=("docker" "docker-compose")
    for cmd in "${commands[@]}"; do
        if ! command -v $cmd &> /dev/null; then
            print_message "错误: 未找到 $cmd 命令" "${RED}"
            exit 1
        fi
    done
}

# 检查环境变量文件
check_env_file() {
    print_message "检查环境变量文件..." "${YELLOW}"
    
    if [ ! -f .env ]; then
        print_message "创建 .env 文件..." "${YELLOW}"
        cat > .env << EOL
# MySQL配置
MYSQL_ROOT_PASSWORD=rootpassword
MYSQL_DATABASE=pdf_processor
MYSQL_USER=pdf_user
MYSQL_PASSWORD=pdf_password

# JWT配置
JWT_SECRET=your-secret-key-here

# YOLO配置
YOLO_MODEL_PATH=yolov8n.pt
YOLO_CONFIDENCE_THRESHOLD=0.5
EOL
        print_message "已创建 .env 文件，请根据需要修改配置" "${GREEN}"
    fi
}

# 创建必要的目录
create_directories() {
    print_message "创建必要的目录..." "${YELLOW}"
    
    mkdir -p mysql/init
    mkdir -p backend/uploads
    mkdir -p frontend/images
}

# 停止并删除现有容器
cleanup() {
    print_message "清理现有容器..." "${YELLOW}"
    docker-compose down -v
}

# 构建并启动服务
start_services() {
    print_message "构建并启动服务..." "${YELLOW}"
    docker-compose up -d --build
}

# 等待服务就绪
wait_for_services() {
    print_message "等待服务就绪..." "${YELLOW}"
    
    services=("mysql" "backend" "yolo-service" "frontend")
    for service in "${services[@]}"; do
        print_message "等待 $service 服务就绪..." "${YELLOW}"
        while ! docker-compose ps $service | grep -q "healthy"; do
            sleep 5
        done
        print_message "$service 服务已就绪" "${GREEN}"
    done
}

# 主函数
main() {
    print_message "开始部署PDF处理系统..." "${GREEN}"
    
    check_commands
    check_env_file
    create_directories
    cleanup
    start_services
    wait_for_services
    
    print_message "部署完成！" "${GREEN}"
    print_message "前端访问地址: http://localhost" "${GREEN}"
    print_message "后端API地址: http://localhost" "${GREEN}"
    print_message "YOLO服务地址: http://localhost/yolo" "${GREEN}"
}

# 执行主函数
main 