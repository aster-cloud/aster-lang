# Policy Editor - 部署指南

## 概览

本指南提供 Policy Editor 的多种部署方式，包括本地开发、Docker 容器和 Kubernetes (K3S) 部署。

## 🚀 部署选项

### 1. 本地开发模式

最适合开发和调试。

```bash
# 进入项目目录
cd examples/policy-editor

# 运行开发模式（支持热重载）
../../gradlew quarkusDev
```

访问：http://localhost:8080

**特点**：
- ✅ 自动热重载
- ✅ 开发工具支持
- ✅ 快速迭代

### 2. JVM 生产模式

推荐用于生产环境。

```bash
# 构建
../../gradlew build

# 运行
java -jar build/quarkus-app/quarkus-run.jar
```

**特点**：
- ✅ 稳定可靠
- ✅ 完整功能支持
- ✅ 资源占用合理 (~256MB 内存)

### 3. Docker 容器部署

适合容器化环境和 CI/CD 流程。

#### 3.1 构建 Docker 镜像

```bash
# 1. 构建应用
../../gradlew build

# 2. 构建 Docker 镜像
docker build -f Dockerfile.jvm -t policy-editor:jvm .

# 3. 运行容器
docker run -d \
  --name policy-editor \
  -p 8080:8080 \
  -e JAVA_OPTS="-Xmx256m -Xms128m" \
  policy-editor:jvm
```

#### 3.2 使用 Docker Compose

创建 `docker-compose.yml`:

```yaml
version: '3.8'
services:
  policy-editor:
    image: policy-editor:jvm
    build:
      context: .
      dockerfile: Dockerfile.jvm
    ports:
      - "8080:8080"
    environment:
      - JAVA_OPTS=-Xmx256m -Xms128m
    volumes:
      - ./policies:/work/policies
    restart: unless-stopped
```

运行：
```bash
docker-compose up -d
```

**特点**：
- ✅ 轻量级镜像 (~200MB)
- ✅ 快速启动 (~3-5 秒)
- ✅ 易于扩展

### 4. Kubernetes (K3S) 部署

适合生产级容器编排环境。

#### 4.1 前提条件

- K3S 或 Kubernetes 集群
- kubectl 命令行工具
- 镜像仓库访问权限

#### 4.2 部署步骤

```bash
# 1. 构建并推送镜像（如果使用私有仓库）
docker build -f Dockerfile.jvm -t your-registry/policy-editor:jvm .
docker push your-registry/policy-editor:jvm

# 2. 应用 Kubernetes 配置
kubectl apply -f k8s/deployment.yaml

# 3. 检查部署状态
kubectl get pods -l app=policy-editor
kubectl get svc policy-editor

# 4. 查看日志
kubectl logs -l app=policy-editor -f
```

#### 4.3 访问应用

```bash
# 获取 LoadBalancer IP
kubectl get svc policy-editor

# 或使用端口转发
kubectl port-forward svc/policy-editor 8080:8080
```

访问：http://localhost:8080 或 http://<LoadBalancer-IP>:8080

#### 4.4 扩容

```bash
# 扩展到 3 个副本
kubectl scale deployment policy-editor --replicas=3

# 查看副本状态
kubectl get pods -l app=policy-editor
```

**特点**：
- ✅ 高可用（多副本）
- ✅ 自动重启和健康检查
- ✅ 负载均衡
- ✅ 滚动更新

## 📊 资源配置建议

### 最小配置
```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "200m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

### 推荐配置
```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "500m"
  limits:
    memory: "1Gi"
    cpu: "1000m"
```

## 🔒 健康检查

Quarkus 提供内置健康检查端点：

- **存活探针**：`/q/health/live` - 检查应用是否运行
- **就绪探针**：`/q/health/ready` - 检查应用是否准备接收流量

## 🔧 环境变量配置

| 变量名 | 描述 | 默认值 |
|--------|------|--------|
| `JAVA_OPTS` | JVM 参数 | `-Xmx256m -Xms128m` |
| `QUARKUS_HTTP_PORT` | HTTP 端口 | `8080` |
| `QUARKUS_HTTP_HOST` | 监听地址 | `0.0.0.0` |
| `QUARKUS_LOG_LEVEL` | 日志级别 | `INFO` |

## 📈 监控和日志

### 查看应用日志

**Docker**:
```bash
docker logs -f policy-editor
```

**Kubernetes**:
```bash
kubectl logs -l app=policy-editor -f
```

### 性能监控

Quarkus 提供 Micrometer 集成，可以添加以下依赖启用监控：

```kotlin
// build.gradle.kts
implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
```

访问指标：http://localhost:8080/q/metrics

## 🐛 故障排查

### 应用无法启动

1. 检查端口占用：
   ```bash
   lsof -i :8080
   ```

2. 查看日志：
   ```bash
   kubectl logs <pod-name>
   ```

3. 检查资源限制：
   ```bash
   kubectl describe pod <pod-name>
   ```

### 内存不足

调整 `JAVA_OPTS`:
```bash
-e JAVA_OPTS="-Xmx512m -Xms256m"
```

### 连接数据库失败

确保数据库可访问：
```bash
kubectl get svc
```

## 🔄 更新部署

### Docker
```bash
# 重新构建镜像
../../gradlew build
docker build -f Dockerfile.jvm -t policy-editor:jvm .

# 重启容器
docker restart policy-editor
```

### Kubernetes
```bash
# 滚动更新
kubectl rollout restart deployment policy-editor

# 查看更新状态
kubectl rollout status deployment policy-editor

# 回滚（如果需要）
kubectl rollout undo deployment policy-editor
```

## 📚 相关资源

- [Quarkus 部署指南](https://quarkus.io/guides/deploying-to-kubernetes)
- [K3S 文档](https://docs.k3s.io/)
- [Docker 最佳实践](https://docs.docker.com/develop/dev-best-practices/)

## ⚠️ 注意事项

1. **GraalVM Native Image**：当前 Vaadin + Quarkus 的 Native Image 支持不稳定，推荐使用 JVM 模式
2. **持久化存储**：默认策略文件存储在容器内，生产环境应使用持久卷 (PersistentVolume)
3. **安全性**：生产环境建议配置 HTTPS 和访问控制
4. **备份**：定期备份策略文件目录

## 📞 获取帮助

遇到问题？
- 查看 [README.md](README.md)
- 查看 [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)
- 提交 Issue 到项目仓库
