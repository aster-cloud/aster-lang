# Aster Lang Runtime - Production Docker Image
#
# 基于 GraalVM Native-Image 构建的轻量级运行时容器
# 用途：部署 Aster 语言 workflow engine 和应用
#
# 构建：docker build -t aster/runtime:latest .
# 运行：docker run -p 8080:8080 aster/runtime:latest

# ============================================
# Stage 1: Build Native Image
# ============================================
FROM ghcr.io/graalvm/native-image:22 AS builder

# 安装构建依赖
RUN microdnf install -y \
    git \
    tar \
    gzip \
    && microdnf clean all

# 安装 Node.js (用于 TypeScript 编译)
RUN curl -fsSL https://rpm.nodesource.com/setup_20.x | bash - \
    && microdnf install -y nodejs \
    && microdnf clean all

WORKDIR /build

# 复制源代码
COPY package.json package-lock.json ./
COPY tsconfig.json ./
COPY src ./src
COPY scripts ./scripts
COPY cnl ./cnl
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle ./gradle
COPY aster-runtime ./aster-runtime
COPY aster-lang-cli ./aster-lang-cli

# 安装 npm 依赖并构建 TypeScript
RUN npm ci && npm run build

# 构建 Native Image
RUN ./gradlew :aster-lang-cli:nativeCompile --no-daemon

# 验证二进制文件
RUN ls -lh /build/aster-lang-cli/build/native/nativeCompile/ \
    && /build/aster-lang-cli/build/native/nativeCompile/aster --version

# ============================================
# Stage 2: Runtime Image (Minimal)
# ============================================
FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-25 AS runtime

# 安装运行时依赖（仅需 glibc 和基础库）
USER root
RUN microdnf install -y \
    glibc \
    libstdc++ \
    zlib \
    && microdnf clean all

# 创建非 root 用户
RUN useradd -r -u 1001 -g 0 aster

# 复制 Native Image 二进制文件
COPY --from=builder --chown=1001:0 \
    /build/aster-lang-cli/build/native/nativeCompile/aster \
    /usr/local/bin/aster

# 复制运行时资源（如果需要）
# COPY --from=builder --chown=1001:0 /build/resources /opt/aster/resources

# 设置工作目录
WORKDIR /workspace

# 切换到非 root 用户
USER 1001

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD aster version || exit 1

# 默认命令：显示版本信息
CMD ["aster", "version"]

# 元数据
LABEL maintainer="Aster Lang Team" \
      version="0.2.0" \
      description="Aster Lang Native Runtime - Natural Language Programming" \
      org.opencontainers.image.source="https://github.com/wontlost-ltd/aster-lang"
