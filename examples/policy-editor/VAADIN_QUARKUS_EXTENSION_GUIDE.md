# 扩展 Vaadin Quarkus 集成：完整指南

## 📋 目录

1. [架构概述](#架构概述)
2. [核心问题分析](#核心问题分析)
3. [解决方案路线图](#解决方案路线图)
4. [实施步骤](#实施步骤)
5. [测试策略](#测试策略)
6. [贡献流程](#贡献流程)

---

## 🏗️ 架构概述

### Quarkus Extension 双模块结构

```
vaadin-quarkus-extension/
├── runtime/                # 运行时模块
│   ├── pom.xml
│   └── src/main/java/
│       └── com/vaadin/quarkus/
│           ├── runtime/
│           │   ├── VaadinServlet.java
│           │   └── VaadinServletConfig.java
│           └── graal/       # Native Image 特定代码
│               ├── AtmosphereDeferredInitializerRecorder.java
│               └── DelayedSchedulerExecutorsFactory.java
│
└── deployment/             # 构建时模块（仅编译期）
    ├── pom.xml
    └── src/main/java/
        └── com/vaadin/quarkus/deployment/
            ├── VaadinQuarkusNativeProcessor.java  # ⭐ Native Image 配置核心
            ├── VaadinQuarkusProcessor.java
            └── nativebuild/
                └── AtmospherePatches.java
```

### 关键组件职责

| 组件 | 职责 | 影响范围 |
|------|------|----------|
| **VaadinQuarkusNativeProcessor** | Native Image 构建配置 | 反射注册、资源包含、运行时初始化 |
| **AtmosphereDeferredInitializerRecorder** | 延迟 Atmosphere 初始化 | Push 功能支持 |
| **AtmospherePatches** | 字节码修改 | 绕过不兼容代码 |
| **Runtime InitializedPackage** | 指定运行时初始化的包 | 避免构建时初始化失败 |

---

## 🔍 核心问题分析

### 问题 1: OSHI 库初始化失败 ✅ 已解决

**现象**:
```
Error: Class initialization of oshi.software.os.unix.freebsd.FreeBsdOperatingSystem failed
```

**根本原因**:
- Vaadin Dev Server 依赖 OSHI 库获取系统信息
- OSHI 在构建时初始化会失败（访问系统资源）

**解决方案**:
```java
// 在 VaadinQuarkusNativeProcessor 中添加
@BuildStep
void deferOshiInit(BuildProducer<RuntimeInitializedPackageBuildItem> producer) {
    producer.produce(new RuntimeInitializedPackageBuildItem("oshi.software.os"));
}
```

**配置方式**（应用层）:
```properties
%native.quarkus.native.additional-build-args=\
  --initialize-at-run-time=oshi.software.os.unix.freebsd.FreeBsdOperatingSystem,\
  --initialize-at-run-time=oshi.software.os.linux.LinuxOperatingSystem
```

### 问题 2: Dev Mode 初始化失败 ❌ 当前障碍

**现象**:
```
Failed to determine project directory for dev mode
```

**根本原因**:
```java
// DevModeInitializer.java (Vaadin 源码)
public void initDevModeHandler(VaadinServletContext context,
                                DevModeHandlerManager manager) {
    // 在 Native Image 构建时被调用
    File projectFolder = getProjectFolder();  // ❌ 失败：无法确定项目路径
    // ...
}
```

**为什么失败**:
1. Native Image 构建在 `build/` 目录下的临时位置
2. Vaadin 尝试查找 `pom.xml` 或 `build.gradle`
3. 找不到这些文件，认为不是有效项目

**需要的解决方案**:
- 在 Native Image 模式下**完全禁用** Dev Mode
- 强制启用 Production Mode

---

## 🛣️ 解决方案路线图

### 方案 A: 修改 Vaadin Quarkus Extension（推荐）

#### 步骤 1: 禁用 Native Image 中的 Dev Mode

在 `VaadinQuarkusNativeProcessor.java` 中添加：

```java
@BuildStep(onlyIf = IsNativeBuild.class)
void disableDevMode(BuildProducer<RunTimeConfigurationDefaultBuildItem> config) {
    // 强制 Native Image 使用生产模式
    config.produce(new RunTimeConfigurationDefaultBuildItem(
        "quarkus.vaadin.production-mode", "true"));
}
```

#### 步骤 2: 排除 Dev Mode 相关类

```java
@BuildStep(onlyIf = IsNativeBuild.class)
void excludeDevModeClasses(BuildProducer<ReflectiveClassBuildItem> reflective) {
    // 明确排除开发服务器类
    reflective.produce(ReflectiveClassBuildItem.builder(
        "com.vaadin.base.devserver.DevModeInitializer",
        "com.vaadin.base.devserver.DevModeHandlerManagerImpl",
        "com.vaadin.base.devserver.startup.DevModeStartupListener"
    ).build());
}
```

#### 步骤 3: 注册生产模式必需资源

```java
@BuildStep(onlyIf = IsNativeBuild.class)
void registerProductionResources(
        BuildProducer<NativeImageResourcePatternsBuildItem> resources) {
    resources.produce(NativeImageResourcePatternsBuildItem.builder()
        // 包含前端构建产物
        .includeGlobs("META-INF/VAADIN/build/**")
        .includeGlobs("META-INF/VAADIN/config/**")
        // 包含静态资源
        .includeGlobs("META-INF/resources/**")
        .build());
}
```

#### 步骤 4: 前端构建流程

```java
@BuildStep(onlyIf = IsNativeBuild.class)
void prepareFrontend(BuildProducer<VaadinFrontendBuildItem> frontend) {
    // 触发前端构建
    // 类似 Maven 的 vaadin:prepare-frontend 和 vaadin:build-frontend

    // 实现思路:
    // 1. 检查是否已有构建产物
    // 2. 如果没有,运行 Vaadin Build Tools
    // 3. 将产物复制到 META-INF/VAADIN/
}
```

### 方案 B: Fork 并修改 Vaadin Core

**更激进，但可能更彻底**

修改 `DevModeInitializer.java`:

```java
public class DevModeInitializer implements VaadinServletContextStartupInitializer {

    @Override
    public void initialize(Set<Class<?>> classes, VaadinServletContext context) {
        // 添加 Native Image 检测
        if (isNativeImage()) {
            LOG.info("Native Image detected, skipping dev mode initialization");
            return;
        }

        // 原有逻辑
        // ...
    }

    private boolean isNativeImage() {
        // GraalVM Native Image 特定检测
        return "substrate".equals(System.getProperty("org.graalvm.nativeimage.imagecode"));
    }
}
```

---

## 🛠️ 实施步骤

### 准备工作

#### 1. Fork Vaadin Quarkus Extension

```bash
# Fork https://github.com/vaadin/quarkus
git clone https://github.com/YOUR_USERNAME/quarkus.git vaadin-quarkus-native-fix
cd vaadin-quarkus-native-fix

# 创建特性分支
git checkout -b feature/native-image-support
```

#### 2. 设置开发环境

```bash
# 安装 GraalVM
sdk install java 25-graalvm

# 安装依赖
mvn clean install -DskipTests
```

### 核心修改

#### 修改 1: 禁用 Dev Mode (deployment 模块)

**文件**: `deployment/src/main/java/com/vaadin/quarkus/deployment/VaadinQuarkusNativeProcessor.java`

```java
/**
 * Disables Vaadin Dev Mode in Native Image builds.
 * Dev Mode requires file system access and project structure that is not
 * available in compiled native images.
 */
@BuildStep(onlyIf = IsNativeBuild.class)
void forceProductionMode(
        BuildProducer<SystemPropertyBuildItem> systemProperty,
        BuildProducer<RunTimeConfigurationDefaultBuildItem> config) {

    // 设置系统属性
    systemProperty.produce(new SystemPropertyBuildItem(
        "vaadin.productionMode", "true"));

    // 设置配置默认值
    config.produce(new RunTimeConfigurationDefaultBuildItem(
        "quarkus.vaadin.production-mode", "true"));

    // 禁用前端热重载
    systemProperty.produce(new SystemPropertyBuildItem(
        "vaadin.frontend.hotdeploy", "false"));
}
```

#### 修改 2: 排除 Dev Mode 类 (deployment 模块)

```java
/**
 * Prevents DevMode classes from being included in the native image.
 * These classes assume a development environment and will fail at runtime.
 */
@BuildStep(onlyIf = IsNativeBuild.class)
void excludeDevModeClasses(
        BuildProducer<NativeImageFeatureBuildItem> feature) {

    feature.produce(new NativeImageFeatureBuildItem(
        "com.vaadin.quarkus.graal.VaadinNativeImageFeature"));
}
```

**新文件**: `runtime/src/main/java/com/vaadin/quarkus/graal/VaadinNativeImageFeature.java`

```java
package com.vaadin.quarkus.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.graalvm.nativeimage.hosted.Feature;

/**
 * Native Image Feature that substitutes Dev Mode classes with no-op implementations.
 */
public class VaadinNativeImageFeature implements Feature {

    @TargetClass(className = "com.vaadin.base.devserver.startup.DevModeStartupListener")
    static final class Target_DevModeStartupListener {
        @Substitute
        public void initialize(Set<?> classes, Object context) {
            // No-op: Dev Mode is disabled in Native Image
        }
    }

    @TargetClass(className = "com.vaadin.base.devserver.DevModeInitializer")
    static final class Target_DevModeInitializer {
        @Substitute
        public void initDevModeHandler(Object context, Object manager) {
            // No-op: Dev Mode is disabled in Native Image
        }
    }
}
```

#### 修改 3: 前端构建集成 (deployment 模块)

```java
/**
 * Ensures frontend resources are built and included in the native image.
 */
@BuildStep(onlyIf = IsNativeBuild.class)
void buildFrontend(
        BuildProducer<GeneratedResourceBuildItem> generatedResources,
        BuildProducer<NativeImageResourcePatternsBuildItem> nativeResources) {

    // 检查前端构建产物
    Path vaadinBuildDir = Paths.get("build", "vaadin-generated");
    if (!Files.exists(vaadinBuildDir)) {
        throw new IllegalStateException(
            "Frontend build artifacts not found. " +
            "Run './gradlew vaadinPrepareFrontend vaadinBuildFrontend' before native build");
    }

    // 注册前端资源
    nativeResources.produce(NativeImageResourcePatternsBuildItem.builder()
        .includeGlobs(
            "META-INF/VAADIN/build/**",
            "META-INF/VAADIN/config/**",
            "META-INF/resources/VAADIN/**"
        )
        .build());
}
```

### 测试修改

#### 创建测试项目

```bash
# 在 vaadin-quarkus-native-fix 目录下
cd integration-tests
mkdir native-image-test
cd native-image-test
```

**build.gradle.kts**:

```kotlin
plugins {
    java
    id("io.quarkus") version "3.28.3"
}

dependencies {
    implementation(project(":runtime"))
    implementation("io.quarkus:quarkus-undertow")
    implementation("com.vaadin:vaadin-core:24.9.2")
}
```

**测试应用**:

```java
@Route("")
public class TestView extends VerticalLayout {
    public TestView() {
        add(new H1("Native Image Test"));
        add(new Button("Click me", e ->
            Notification.show("Works in Native Image!")));
    }
}
```

#### 运行测试

```bash
# 编译修改后的扩展
cd ../..
mvn clean install -DskipTests

# 测试 Native Image 编译
cd integration-tests/native-image-test
../../gradlew build -Dquarkus.package.type=native

# 运行 Native Image
./build/native-image-test-runner

# 测试
curl http://localhost:8080
```

---

## 🧪 测试策略

### 1. 单元测试

在 `deployment/src/test/java/` 下创建测试：

```java
@QuarkusTest
@TestProfile(NativeImageTestProfile.class)
public class NativeImageSupportTest {

    @Test
    public void testProductionModeForced() {
        // 验证生产模式已启用
        assertTrue(VaadinService.getCurrent().getDeploymentConfiguration()
            .isProductionMode());
    }

    @Test
    public void testDevModeClassesExcluded() {
        // 验证 Dev Mode 类不存在
        assertThrows(ClassNotFoundException.class, () ->
            Class.forName("com.vaadin.base.devserver.DevModeInitializer"));
    }
}
```

### 2. 集成测试

```bash
# 创建完整应用并测试
./gradlew :integration-tests:native-image-test:build -Dquarkus.package.type=native

# 验证启动时间
time ./build/native-image-test-runner &
sleep 1
curl http://localhost:8080

# 验证内存占用
ps aux | grep native-image-test-runner
```

### 3. 回归测试

确保 JVM 模式仍然正常工作：

```bash
./gradlew :integration-tests:native-image-test:quarkusDev
```

---

## 🚀 贡献流程

### 提交到上游

#### 1. 创建 Pull Request

```bash
git add .
git commit -m "feat: Add Native Image support for Vaadin Quarkus extension

- Disable Dev Mode in Native Image builds
- Substitute Dev Mode classes with no-ops
- Ensure frontend resources are included
- Add comprehensive tests

Fixes: #XXX (reference relevant issue)"

git push origin feature/native-image-support
```

#### 2. PR 描述模板

```markdown
## 🎯 目标

支持 Vaadin Quarkus 应用编译为 GraalVM Native Image。

## 📝 变更内容

- [ ] 在 Native Image 构建时强制启用生产模式
- [ ] 使用 Substitution 替换 Dev Mode 类为无操作实现
- [ ] 确保前端构建产物正确包含
- [ ] 添加 OSHI 库运行时初始化配置
- [ ] 添加集成测试验证 Native Image 功能

## 🧪 测试

- [ ] 单元测试通过
- [ ] JVM 模式集成测试通过
- [ ] Native Image 模式集成测试通过
- [ ] 启动时间 < 100ms
- [ ] 内存占用 < 100MB

## 📚 文档

- [ ] 更新 README 添加 Native Image 构建说明
- [ ] 添加故障排除指南
- [ ] 更新限制说明

## ⚠️ 已知限制

- 需要预先运行前端构建
- 不支持热重载（生产模式限制）
- 某些 Vaadin 组件可能需要额外配置
```

#### 3. 与维护者沟通

- 在 [Vaadin Quarkus Discussions](https://github.com/vaadin/quarkus/discussions) 发起讨论
- 引用相关 Issues (#45315)
- 提供性能对比数据

---

## 📊 预期成果

### 成功指标

| 指标 | 目标 | 当前 JVM 模式 |
|------|------|---------------|
| 启动时间 | < 100ms | ~800ms |
| 内存占用 | < 100MB | ~200MB |
| 镜像大小 | < 80MB | N/A |
| 首次请求 | < 50ms | ~100ms |

### 使用示例

编译成功后，用户可以这样使用：

```bash
# 1. 前端构建
./gradlew vaadinPrepareFrontend vaadinBuildFrontend

# 2. Native Image 编译
./gradlew build -Dquarkus.package.type=native

# 3. 运行
./build/my-app-runner

# 4. 访问
curl http://localhost:8080
```

---

## 🔗 参考资料

- [Quarkus Writing Extensions Guide](https://quarkus.io/guides/writing-extensions)
- [GraalVM Native Image Compatibility](https://www.graalvm.org/latest/reference-manual/native-image/metadata/Compatibility/)
- [Vaadin Quarkus GitHub](https://github.com/vaadin/quarkus)
- [Quarkus Issue #45315](https://github.com/quarkusio/quarkus/issues/45315)
- [Spring Boot Native Image with Vaadin](https://vaadin.com/docs/latest/flow/production/native)

---

## 💬 需要帮助？

- Vaadin Discord: https://discord.gg/vaadin
- Quarkus Zulip: https://quarkusio.zulipchat.com
- Stack Overflow: [vaadin] + [quarkus] + [graalvm]

---

**创建日期**: 2025-10-12
**最后更新**: 2025-10-12
**状态**: 📋 规划阶段
