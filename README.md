# 飞书 Dify Agent 机器人

基于Spring Boot的飞书机器人应用，用于连接飞书和Dify API，实现流式消息交互。

## 功能特点

### 1. 消息处理
- 支持飞书消息接收和卡片交互
- 集成Dify API实现流式对话
- 并行处理消息发送，提高响应速度
- 基于消息触发的缓冲区管理

### 2. 卡片池管理
- 系统启动时预创建20个卡片
- 每天0点自动重建卡片池
- 卡片24小时过期自动处理
- 异步创建和失败重试机制

### 3. 会话管理
- 基于userId和messageId的会话索引
- 12小时会话自动过期
- conversationId映射管理
- 会话状态自动维护

### 4. 性能优化
- 消息缓冲区100ms发送间隔
- 基于消息触发的发送机制
- 300ms超时自动清理
- 非agent消息立即触发发送

## 部署说明

### 环境要求
- Docker & Docker Compose
- JDK 8+
- Maven 3.6+

### 快速部署

1. 克隆项目
```bash
git clone [repository_url]
cd feishuDifyAgentChatBot
```

2. 配置环境变量
```bash
cp .env.example .env
```
编辑.env文件，填入以下配置：
```properties
# 飞书配置
FEISHU_APP_ID=your_feishu_app_id
FEISHU_APP_SECRET=your_feishu_app_secret
FEISHU_APP_VERIFICATION_TOKEN=your_feishu_verification_token

# Dify配置
DIFY_API_ENDPOINT=your_dify_api_endpoint
DIFY_API_KEY=your_dify_api_key
```

3. 构建和启动
```bash
# 构建服务
docker compose build

# 启动服务
docker compose up -d
```

4. 验证部署
```bash
# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f
```

### 配置说明

#### 应用配置 (application.yml)
```yaml
server:
  port: 9000  # 默认端口

spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=12h  # 缓存配置

feishu:
  api-endpoint: https://open.feishu.cn/open-apis/bot/v2  # 飞书API地址

logging:
  file:
    name: logs/app.log  # 日志文件路径
```

#### 环境变量说明
| 变量名 | 说明 | 必填 |
|--------|------|------|
| FEISHU_APP_ID | 飞书应用ID | 是 |
| FEISHU_APP_SECRET | 飞书应用密钥 | 是 |
| FEISHU_APP_VERIFICATION_TOKEN | 飞书验证Token | 是 |
| DIFY_API_ENDPOINT | Dify API地址 | 是 |
| DIFY_API_KEY | Dify API密钥 | 是 |

## 开发指南

### 本地开发
1. 安装依赖
```bash
mvn clean install
```

2. 运行应用
```bash
mvn spring-boot:run
```

### 目录结构
```
src/main/java/com/sdxpub/feishubot/
├── application/    # 应用入口
├── config/         # 配置类
├── controller/     # 控制器
├── model/          # 数据模型
│   ├── dify/      # Dify相关模型
│   ├── feishu/    # 飞书相关模型
│   └── message/   # 消息相关模型
├── service/        # 服务实现
│   ├── card/      # 卡片服务
│   ├── dify/      # Dify服务
│   ├── feishu/    # 飞书服务
│   └── message/   # 消息服务
└── common/         # 公共组件
    ├── constants/ # 常量定义
    ├── exception/ # 异常处理
    └── utils/     # 工具类
```

## 监控和维护

### 日志查看
```bash
# 查看应用日志
tail -f logs/app.log

# 查看容器日志
docker compose logs -f
```

### 服务管理
```bash
# 停止服务
docker compose down

# 重启服务
docker compose restart

# 查看服务状态
docker compose ps
```

## 许可证

[MIT License](LICENSE)
