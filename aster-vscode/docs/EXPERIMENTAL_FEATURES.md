# 实验性功能 GA 路线图

本文档描述 aster-vscode 扩展中实验性功能的当前状态、GA（正式发布）标准和计划。

## 功能清单

### 1. Build Native Executable (原生构建)

**命令**: `aster.buildNative`
**状态**: 🧪 实验性
**版本引入**: v0.3.0

#### 当前行为

- 执行标准 JVM 编译流程
- 输出 Java 字节码到配置的输出目录
- 显示警告消息说明当前为实验性功能

#### GA 标准

| 标准 | 描述 | 状态 |
|------|------|------|
| GraalVM Native Image 集成 | 使用 GraalVM Native Image 编译为原生可执行文件 | 🔴 未开始 |
| 跨平台支持 | 支持 Windows、macOS、Linux 原生构建 | 🔴 未开始 |
| 反射配置自动生成 | 自动生成 reflect-config.json | 🔴 未开始 |
| 资源配置 | 正确处理静态资源打包 | 🔴 未开始 |
| 构建缓存 | 增量构建支持，减少重复编译时间 | 🔴 未开始 |
| 错误处理 | GraalVM 不可用时提供清晰的安装指引 | 🔴 未开始 |
| 文档完善 | 完整的用户指南和故障排除文档 | 🔴 未开始 |
| 集成测试 | 端到端测试覆盖 | 🔴 未开始 |

#### 实施计划

**阶段 1: 基础设施 (v0.4.0)**
- [ ] 检测系统 GraalVM 安装
- [ ] 添加 `aster.graalvm.home` 配置项
- [ ] 实现 GraalVM Native Image 命令调用
- [ ] 基本错误处理

**阶段 2: 功能完善 (v0.5.0)**
- [ ] 自动生成反射配置
- [ ] 资源文件处理
- [ ] 构建进度报告
- [ ] 跨平台测试

**阶段 3: 优化与 GA (v0.6.0)**
- [ ] 构建缓存
- [ ] 性能优化
- [ ] 完整文档
- [ ] 移除实验性标记

#### 依赖项

- GraalVM CE/EE 21+
- Native Image 组件 (`gu install native-image`)

#### 配置项 (计划中)

```json
{
  "aster.graalvm.home": "",
  "aster.native.additionalArgs": [],
  "aster.native.enableCache": true,
  "aster.native.targetPlatform": "auto"
}
```

---

## 实验性功能使用指南

### 如何识别实验性功能

1. **命令标题**: 包含 "(实验性)" 后缀
2. **执行消息**: 显示 ⚠️ 警告图标
3. **文档**: 在本文件中列出

### 反馈渠道

如果您在使用实验性功能时遇到问题或有改进建议：

1. **GitHub Issues**: [aster-lang/issues](https://github.com/wontlost-ltd/aster-lang/issues)
2. **标签**: 使用 `experimental` 和功能名称标签

### 风险说明

实验性功能可能：
- 在未来版本中发生重大变更
- 存在已知限制或 bug
- 在某些环境下不可用

建议仅在开发/测试环境中使用实验性功能。

---

## 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| v0.3.0 | 2024-12 | 添加 buildNative 实验性命令 |

---

## 相关文档

- [扩展配置说明](../README.md#配置选项)
- [命令参考](../README.md#命令)
- [故障排除](../TROUBLESHOOTING.md)
