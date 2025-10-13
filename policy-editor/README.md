# Policy Editor - 可视化策略编辑器

这是一个基于 **Vaadin** 和 **Quarkus** 构建的 Web 应用，用于可视化地编辑和管理 Aster 语言的策略文件。

## 功能特性

- ✅ **策略列表视图**：以表格形式展示所有策略
- ✅ **创建新策略**：通过表单创建新的策略文件
- ✅ **编辑策略**：修改现有策略的 allow 和 deny 规则
- ✅ **删除策略**：删除不需要的策略文件
- ✅ **REST API**：提供完整的 CRUD 操作接口
- ✅ **JSON 格式**：支持标准的 JSON 格式策略文件

## 技术栈

- **Java 21**：编程语言
- **Quarkus 3.28.3**：后端框架，提供 REST API
- **Vaadin 24.9.2**：前端 UI 框架（支持 GraalVM Native Image）
- **Gradle 9.0**：构建工具
- **Jackson 2.17.2**：JSON 处理
- **GraalVM Native Image**：原生镜像编译（可选）

## 快速开始

### 1. 构建项目

```bash
cd examples/policy-editor
../../gradlew build
```

### 2. 运行应用

#### 开发模式（支持热重载）
```bash
../../gradlew quarkusDev
```

#### 生产模式
```bash
../../gradlew build
java -jar build/quarkus-app/quarkus-run.jar
```

### 3. 访问应用

- **Web UI**：http://localhost:8080
- **REST API**：http://localhost:8080/api/policies

## REST API 文档

### 获取所有策略
```bash
GET http://localhost:8080/api/policies
```

### 获取单个策略
```bash
GET http://localhost:8080/api/policies/{id}
```

### 创建策略
```bash
POST http://localhost:8080/api/policies
Content-Type: application/json

{
  "id": "my-policy",
  "name": "我的策略",
  "allow": {
    "io": ["*"],
    "cpu": ["*"]
  },
  "deny": {
    "io": ["/etc/passwd"]
  }
}
```

### 更新策略
```bash
PUT http://localhost:8080/api/policies/{id}
Content-Type: application/json

{
  "name": "更新后的策略名称",
  "allow": {...},
  "deny": {...}
}
```

### 删除策略
```bash
DELETE http://localhost:8080/api/policies/{id}
```

## 策略文件格式

策略文件存储在 `src/main/resources/policies/` 目录下，采用 JSON 格式：

```json
{
  "id": "example-policy",
  "name": "示例策略",
  "allow": {
    "io": ["*"],
    "cpu": ["*"],
    "network": ["http://*", "https://*"]
  },
  "deny": {
    "io": ["/etc/passwd", "/etc/shadow"]
  }
}
```

### 字段说明

- `id`：策略的唯一标识符
- `name`：策略的可读名称
- `allow`：允许的资源访问规则
  - `io`：IO 操作权限模式列表
  - `cpu`：CPU 操作权限模式列表
  - `network`：网络访问权限模式列表
- `deny`：拒绝的资源访问规则（优先级高于 allow）

## 项目结构

```
examples/policy-editor/
├── build.gradle.kts                    # Gradle 构建配置
├── README.md                           # 本文档
└── src/main/
    ├── java/editor/
    │   ├── model/                      # 数据模型
    │   │   ├── Policy.java            # 策略实体
    │   │   ├── PolicyRule.java        # 策略规则
    │   │   └── PolicyRuleSet.java     # 规则集
    │   ├── service/                    # 业务逻辑
    │   │   └── PolicyService.java     # 策略服务
    │   ├── api/                        # REST API
    │   │   └── PolicyResource.java    # API 端点
    │   └── ui/                         # Vaadin UI
    │       ├── MainView.java          # 主视图
    │       └── PolicyEditorDialog.java # 编辑对话框
    └── resources/
        ├── application.properties      # 应用配置
        └── policies/                   # 策略文件存储目录
            └── example-policy.json    # 示例策略
```

## 开发指南

### 添加新的资源类型

1. 在策略文件的 `allow` 或 `deny` 部分添加新的键值对
2. 使用通配符 `*` 允许所有访问
3. 使用具体模式进行细粒度控制

示例：
```json
{
  "allow": {
    "database": ["mysql://*", "postgres://*"],
    "filesystem": ["/home/*", "/tmp/*"]
  }
}
```

### 自定义 UI

Vaadin 组件位于 `src/main/java/editor/ui/` 目录：

- `MainView.java`：修改主列表视图
- `PolicyEditorDialog.java`：修改编辑表单

### 修改存储路径

在 `PolicyService.java` 中修改 `POLICIES_DIR` 常量：

```java
private static final String POLICIES_DIR = "your/custom/path";
```

## 故障排除

### 端口冲突

如果 8080 端口已被占用，在 `application.properties` 中修改：

```properties
quarkus.http.port=9090
```

### 策略文件权限

确保应用有权限读写 `src/main/resources/policies/` 目录。

### 依赖下载失败

使用国内 Maven 镜像加速（在项目根目录的 `build.gradle.kts` 中配置）。

## GraalVM Native Image 编译（后端专用）

### ⚠️ 当前限制

**注意**：Vaadin + Quarkus 的 GraalVM Native Image 支持目前存在已知问题：

- Vaadin 24.x 与 Quarkus 3.x 的 Native Image 集成仍在实验阶段
- 已知问题包括：
  1. ✅ **OSHI 库初始化** - 已通过运行时初始化配置解决
  2. ❌ **Vaadin 开发服务器** - DevModeInitializer 在 Native Image 构建时无法正确初始化
  3. ❌ **前端资源构建** - Vaadin 生产模式的前端编译在 Gradle + Native Image 环境下存在问题

**参考资料**：
- [Quarkus Issue #45315](https://github.com/quarkusio/quarkus/issues/45315) - Vaadin Native Image 编译失败（标记为"不计划修复"）
- [Vaadin Blog: Quarkus Native](https://vaadin.com/blog/vaadin-apps-as-native-executables-using-quarkus-native) - 2022年的实验性支持

Vaadin 团队和 Quarkus 团队正在协作改进 Native Image 支持，但截至 2025 年 10 月，仍不建议在生产环境使用。

### 临时解决方案

在 Native Image 支持完善之前，推荐以下部署方式：

#### 1. JVM 模式部署（推荐用于生产）

```bash
# 构建 JVM 版本
../../gradlew build

# 运行
java -jar build/quarkus-app/quarkus-run.jar
```

**优势**：
- ✅ 完全支持所有 Vaadin 特性
- ✅ 稳定可靠
- ✅ 适合容器化部署

#### 2. Docker 容器部署

创建 `Dockerfile.jvm`:
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /work/
COPY build/quarkus-app/lib/ /work/lib/
COPY build/quarkus-app/*.jar /work/
COPY build/quarkus-app/app/ /work/app/
COPY build/quarkus-app/quarkus/ /work/quarkus/
EXPOSE 8080
CMD ["java", "-jar", "/work/quarkus-run.jar"]
```

构建和运行：
```bash
../../gradlew build
docker build -f Dockerfile.jvm -t policy-editor:jvm .
docker run -p 8080:8080 policy-editor:jvm
```

#### 3. K3S 部署

创建 `k8s/deployment.yaml`:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: policy-editor
spec:
  replicas: 2
  selector:
    matchLabels:
      app: policy-editor
  template:
    metadata:
      labels:
        app: policy-editor
    spec:
      containers:
      - name: policy-editor
        image: policy-editor:jvm
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "256Mi"
            cpu: "200m"
          limits:
            memory: "512Mi"
            cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: policy-editor
spec:
  type: LoadBalancer
  ports:
  - port: 8080
    targetPort: 8080
  selector:
    app: policy-editor
```

部署：
```bash
kubectl apply -f k8s/deployment.yaml
```

### 当前策略与使用

本模块在 JVM 产物中提供完整 Vaadin UI；在原生镜像中仅包含后端 API（REST/GraphQL 代理），不包含 Vaadin UI。

构建原生二进制：

```bash
cd policy-editor
./build-native.sh
```

运行原生二进制：

```bash
./run-native.sh
```

构建并运行原生 Docker 镜像：

```bash
docker build -f Dockerfile.native -t policy-editor:native .
docker run --rm -p 8081:8081 policy-editor:native
```

跟踪 Native Image 支持进展：
- [Vaadin Quarkus GitHub](https://github.com/vaadin/quarkus)
- [Quarkus Native Image Guide](https://quarkus.io/guides/building-native-image)

## 身份认证与授权（OIDC/Keycloak）

本模块已集成 Quarkus Security + OIDC（可选启用）。开发者可使用内置的 Keycloak 开发配置快速体验登录/登出、基于角色的 UI 控制与接口授权。

### 本地快速启动（Keycloak Dev）

1. 启动 Keycloak（内置 realm 与用户）

- 使用 Docker:
  ```
  cd policy-editor/dev/keycloak
  docker compose up -d
  ```

- 使用 Podman:
  ```
  cd policy-editor/dev/keycloak
  ./podman-run.sh          # 默认端口 8083
  # 停止/清理
  ./podman-stop.sh
  ```

2. 启动应用（dev 模式默认启用 OIDC 开发配置）

```
./gradlew :policy-editor:quarkusDev
```

3. 访问 http://localhost:8081 并在顶栏使用“登录/登出”按钮

- 账号：
  - 管理员 alice / alice（角色 admin）
  - 普通用户 bob / bob（角色 user）

4. 角色与权限演示

- 顶栏显示当前用户名与角色
- 仅管理员可见：删除策略、同步“推送”、撤销/重做按钮（UI 隐藏 + 后端 @RolesAllowed 强制）
- 所有用户可见：策略列表、导出 ZIP、历史查看

### 关键配置

文件：`policy-editor/src/main/resources/application.properties`

```
%dev.quarkus.oidc.auth-server-url=http://localhost:8083/realms/aster
%dev.quarkus.oidc.client-id=policy-editor
%dev.quarkus.oidc.application-type=web-app
%dev.quarkus.http.auth.permission.public.paths=/*
%dev.quarkus.http.auth.permission.public.policy=permit
%dev.quarkus.http.auth.permission.secured.paths=/api/*
%dev.quarkus.http.auth.permission.secured.policy=authenticated
```

生产环境请改为你的 IdP 地址与权限策略（按需拆分公开与受保护路径）。

### 常见问题（FAQ）

- 端口冲突
  - Keycloak 默认占用 8083；若冲突，修改 docker-compose 暴露端口，并同步调整 `%dev.quarkus.oidc.auth-server-url`。
- CORS 问题
  - 本应用前后端同源（Vaadin + Quarkus），一般无需额外 CORS 配置；
  - 若从不同域访问 API，请启用 Quarkus CORS：
    - `quarkus.http.cors=true`
    - `quarkus.http.cors.origins=*`（或指定具体来源）
- 未登录访问受保护接口返回 401/403
  - 预期行为；使用顶栏“登录”完成认证；管理员权限方可执行增删改与同步推送等敏感操作。

## 相关项目

- [Aster Language](../../README.md)
- [Policy Engine Example](../policy-jvm/README.md)

## 许可证

与 Aster 语言项目相同。
