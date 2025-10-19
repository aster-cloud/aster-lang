# Production Build Pipeline

**状态**: ✅ 已实现 (Phase 0, Priority 2)
**版本**: 0.2.0
**最后更新**: 2025-10-10

---

## 概述

Aster Lang 提供完整的生产级构建流水线，包括：

1. **GraalVM Native-Image** - 编译为原生可执行文件
2. **Docker 运行时** - 容器化部署
3. **确定性构建** - 可重现的构建产物
4. **CI/CD 自动化** - GitHub Actions 集成

---

## 🚀 快速开始

### 构建 Native Image

```bash
# 完整构建（包含测试）
npm run native:build

# 快速构建（跳过测试）
npm run native:build:quick

# 验收测试（大小、启动时间）
npm run native:acceptance
```

### Docker 部署

```bash
# 构建 Docker 镜像
npm run docker:build

# 运行容器
npm run docker:run

# 启动完整开发环境（包括 PostgreSQL、Redis）
npm run docker:compose:up
```

---

## 📦 Native Image 构建

### 系统要求

- **GraalVM JDK 21** with native-image installed
- **Node.js 20+** (用于 TypeScript 编译)
- **macOS**: Xcode toolchain (运行 `sudo xcodebuild -license`)
- **Linux**: GCC 工具链
- **Windows**: Visual Studio 2022 (可选)

### 构建配置

Native Image 配置位于 `aster-lang-cli/build.gradle.kts`:

```kotlin
graalvmNative {
  binaries {
    named("main") {
      imageName.set("aster")

      buildArgs.addAll(listOf(
        "--no-fallback",           // 禁用 JVM fallback
        "-O3",                     // 最高优化级别
        "--gc=G1",                 // G1 垃圾回收器
        "-march=native",           // CPU 架构优化
        "-H:+RemoveUnusedSymbols", // 移除未使用符号
        "-H:+UseCompressedReferences"  // 压缩指针
      ))
    }
  }
}
```

### 验收标准

✅ **二进制大小** < 50MB
✅ **启动时间** < 100ms
✅ **功能完整性** 支持 compile、typecheck 命令

验证脚本:

```bash
# 检查二进制大小
npm run native:check

# 性能基准测试（10次迭代）
npm run native:benchmark

# 综合验收测试
npm run native:acceptance
```

---

## 🐳 Docker 部署

### 镜像架构

采用**多阶段构建**优化镜像大小：

1. **Builder Stage** - 编译 TypeScript + 构建 Native Image
2. **Runtime Stage** - 最小化运行时环境

最终镜像基于 `quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21`。

### Docker Compose 环境

完整开发环境包含:

- **aster-runtime** - Aster 语言运行时
- **postgres** - PostgreSQL 16 (workflow 状态持久化)
- **redis** - Redis 7 (缓存 + 任务队列)
- **prometheus** (可选) - 监控指标收集
- **grafana** (可选) - 可视化仪表盘

启动环境:

```bash
# 启动核心服务
docker-compose up -d

# 启动包含监控的完整环境
docker-compose --profile monitoring up -d

# 查看日志
npm run docker:compose:logs

# 停止环境
npm run docker:compose:down
```

### 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `ASTER_ENV` | `development` | 环境模式 (development/production) |
| `ASTER_LOG_LEVEL` | `info` | 日志级别 (debug/info/warn/error) |
| `POSTGRES_DB` | `aster` | 数据库名称 |
| `POSTGRES_USER` | `aster` | 数据库用户 |
| `POSTGRES_PASSWORD` | `aster_dev_password` | 数据库密码 |

---

## 🔒 确定性构建

### 原理

确定性构建确保**相同输入 → 相同输出**：

- 固定时间戳 (`isPreserveFileTimestamps = false`)
- 固定文件顺序 (`isReproducibleFileOrder = true`)
- 固定文件权限 (`fileMode = 0644, dirMode = 0755`)
- 禁用增量编译 (`options.isIncremental = false`)

### 启用构建缓存

本地缓存（默认启用）:

```kotlin
buildCache {
  local {
    directory = file("${rootProject.projectDir}/.gradle/build-cache")
    removeUnusedEntriesAfterDays = 30
  }
}
```

远程缓存（生产环境）:

```kotlin
remote<HttpBuildCache> {
  url = uri("https://build-cache.example.com/")
  isEnabled = System.getenv("CI") == "true"
  isPush = System.getenv("CI_BRANCH") == "main"
}
```

### 验证可重现性

```bash
# 第一次构建
npm run native:build
shasum -a 256 aster-lang-cli/build/native/nativeCompile/aster > checksum1.txt

# 清理 + 第二次构建
./gradlew clean
npm run native:build
shasum -a 256 aster-lang-cli/build/native/nativeCompile/aster > checksum2.txt

# 比较校验和（应该相同）
diff checksum1.txt checksum2.txt
```

---

## 🤖 CI/CD 自动化

### GitHub Actions Workflow

`.github/workflows/build-native.yml` 自动执行：

1. **构建 Native Image** (Ubuntu + macOS)
2. **大小和性能检查**
3. **构建 Docker 镜像**
4. **验收测试**
5. **发布 artifacts** (on tag push)

### 触发条件

- `push` to `main`, `develop`, `release/**`
- `pull_request` to `main`
- 手动触发 (`workflow_dispatch`)

### Artifacts

每次构建产生:

- `aster-native-ubuntu-latest` - Linux 原生二进制
- `aster-native-macos-latest` - macOS 原生二进制
- Docker 镜像 - `ghcr.io/wontlost-ltd/aster-lang:{tag}`

保留期：30天

### Release 流程

创建 tag 自动发布:

```bash
git tag -a v0.2.0 -m "Release v0.2.0"
git push origin v0.2.0
```

GitHub Release 自动创建，附带:
- 原生二进制文件（Linux + macOS）
- Docker 镜像
- Release notes

---

## 📊 性能优化

### Native Image 优化选项

| 选项 | 说明 | 效果 |
|------|------|------|
| `-O3` | 最高优化级别 | 更快执行速度，稍慢编译 |
| `-march=native` | CPU 架构优化 | 利用 SIMD 等指令 |
| `-H:+RemoveUnusedSymbols` | 移除未使用符号 | 减小二进制大小 |
| `-H:+UseCompressedReferences` | 压缩指针 | 减少内存占用 |
| `--initialize-at-build-time` | 构建时初始化 | 更快启动时间 |
| `--gc=G1` | G1 垃圾回收器 | 平衡吞吐量和延迟 |

### PGO (Profile-Guided Optimization)

未来优化（Phase 1）:

```bash
# 1. 构建带 instrumentation 的二进制
./gradlew :aster-lang-cli:nativeCompile \
  -Pgraalvm.native.extra-args=--pgo-instrument

# 2. 运行代表性工作负载，生成 profile
./aster-lang-cli/build/native/nativeCompile/aster compile workload.aster

# 3. 使用 profile 重新构建
./gradlew :aster-lang-cli:nativeCompile \
  -Pgraalvm.native.extra-args=--pgo=default.iprof
```

---

## 🐛 故障排查

### 常见问题

**1. Native Image 构建失败**

```
Error: Image building request failed with exit status 1
```

**解决方案**:
- 确认 GraalVM 版本 (需要 21+)
- 检查 `native-image` 是否已安装: `gu install native-image`
- macOS: 确认 Xcode license: `sudo xcodebuild -license accept`

**2. Docker 构建失败 (无法拉取基础镜像)**

```
Error: failed to resolve source metadata for ghcr.io/graalvm/native-image:21
```

**解决方案**:
- 确认网络连接
- 使用镜像加速器
- 手动拉取基础镜像: `docker pull ghcr.io/graalvm/native-image:21`

**3. 二进制大小超限**

```
Binary size 65.23 MB exceeds limit of 50.00 MB
```

**解决方案**:
- 启用更多优化选项
- 移除未使用的依赖
- 使用 `--no-fallback` 和 `-H:+RemoveUnusedSymbols`
- 考虑动态链接: `-H:-StaticExecutableWithDynamicLibC`

**4. 启动时间超限**

```
Average startup time: 156ms exceeds 100ms limit
```

**解决方案**:
- 增加 `--initialize-at-build-time` 范围
- 使用 PGO 优化
- 检查运行环境（磁盘 I/O、CPU 性能）

---

## 📚 参考资料

- [GraalVM Native Image 文档](https://www.graalvm.org/latest/reference-manual/native-image/)
- [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)
- [Reproducible Builds](https://reproducible-builds.org/)
- [Docker Multi-Stage Builds](https://docs.docker.com/build/building/multi-stage/)
- [GitHub Actions](https://docs.github.com/en/actions)

---

## 🗺️ Roadmap

- [x] Phase 0: Native Image 基础支持 (v0.2.0)
- [x] Phase 0: Docker 运行时 (v0.2.0)
- [x] Phase 0: 确定性构建 (v0.2.0)
- [x] Phase 0: GitHub Actions CI (v0.2.0)
- [ ] Phase 1: PGO 优化 (v0.3.0)
- [ ] Phase 1: 多平台支持 (Windows, ARM64) (v0.3.0)
- [ ] Phase 1: 云原生部署 (Kubernetes) (v0.4.0)
- [ ] Phase 2: 分布式构建缓存 (v0.5.0)

---

**最后更新**: 2025-10-10
**维护者**: Aster Language Team
