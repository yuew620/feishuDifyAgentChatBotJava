# 飞书 Dify Agent 机器人

基于Spring Boot的飞书机器人应用，用于连接飞书和Dify API，实现流式消息交互。

## Ubuntu 24.04 安装流程

### 1. 系统准备
```bash
# 更新系统
sudo apt update
sudo apt upgrade -y

# 安装必要工具
sudo apt install -y curl git docker.io
```

### 2. 安装Docker Compose
```bash
# 安装Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 验证安装
docker --version
docker-compose --version

# 将当前用户添加到docker组
sudo usermod -aG docker $USER
newgrp docker
```

### 3. 克隆项目
```bash
# 克隆代码
git clone [repository_url]
cd feishuDifyAgentChatBot
```

### 4. 配置环境变量
```bash
# 复制环境变量模板
cp .env.example .env

# 编辑环境变量
nano .env
```

填入以下配置：
```properties
# 飞书配置
FEISHU_APP_ID=your_feishu_app_id
FEISHU_APP_SECRET=your_feishu_app_secret
FEISHU_VERIFICATION_TOKEN=your_feishu_verification_token
FEISHU_CHAT_ID=your_feishu_chat_id

# 服务器配置
SERVER_PORT=9000
```

### 5. 创建日志目录
```bash
# 创建日志目录并设置权限
mkdir -p logs
chmod 777 logs
```

### 6. 构建和启动服务
```bash
# 构建服务
docker-compose build

# 启动服务
docker-compose up -d
```

### 7. 验证部署
```bash
# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 检查服务健康状态
curl http://localhost:9000/health
```

### 8. 常用维护命令
```bash
# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 更新服务
git pull
docker-compose build
docker-compose up -d

# 查看实时日志
tail -f logs/feishu-bot.log
```

### 9. 故障排查
```bash
# 检查容器状态
docker ps -a

# 查看容器日志
docker logs feishu-dify-bot

# 检查系统资源
docker stats

# 检查网络连接
netstat -tulpn | grep 9000
```

## 监控和维护

### 日志位置
- 应用日志：`./logs/feishu-bot.log`
- Docker日志：`docker-compose logs -f`

### 自动重启
服务配置了自动重启策略，如果发生崩溃会自动恢复：
```yaml
restart: unless-stopped
```

### 资源限制
在`docker-compose.yml`中配置了内存限制：
```yaml
environment:
  - JAVA_OPTS=-Xmx512m -Xms256m
```

### 健康检查
服务包含健康检查端点：
```bash
curl http://localhost:9000/health
```

## 许可证

[MIT License](LICENSE)
