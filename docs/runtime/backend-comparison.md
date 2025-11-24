> 更新：2025-11-22 16:57 NZST · 执行者：Codex

# Backend 对比

## 概览

Aster 提供三种执行 backend，各有不同的功能支持和使用场景。

## 功能对比表

| 功能 | Truffle | Java | TypeScript |
|------|---------|------|------------|
| 纯计算 | ✅ | ✅ | ✅ |
| IO.print | ❌ | ✅ | ✅ |
| IO.readLine | ❌ | ✅ | ✅ |
| IO.readFile | ❌ | ✅ | ✅ |
| IO.writeFile | ❌ | ✅ | ✅ |
| 网络请求（Http.*） | ❌ | ✅ | ✅ |
| 数据库操作（Db.*） | ❌ | ✅ | ✅ |
| GraalVM 优化 | ✅ | ❌ | ❌ |
| 性能（纯计算） | 最高 | 高 | 中 |

## 选择指南

### 使用 Truffle Backend
- ✅ 纯计算任务（数学运算、业务规则计算）
- ✅ 需要最高性能的场景
- ✅ 无 IO、网络、数据库需求
- ❌ 不支持任何 IO 操作

### 使用 Java Backend
- ✅ 需要文件/网络/数据库 IO
- ✅ 生产环境完整功能
- ✅ 与 Java 生态集成

### 使用 TypeScript Backend
- ✅ 需要文件/网络 IO
- ✅ 前端或 Node.js 集成
- ✅ 快速原型开发

## 常见问题

### Q: 为什么 Truffle 不支持 IO？
A: Truffle 是 GraalVM 的语言实现框架，专为纯计算和多语言互操作设计。IO 操作会引入平台依赖和副作用，与 Truffle 的设计理念冲突。

### Q: 如何切换 Backend？
A: [待补充：说明命令行参数或配置方式]

## 相关文档
- [Truffle Backend 详细说明](./truffle-backend.md)
- [Java Backend 使用指南](./java-backend.md)
- [TypeScript Backend 使用指南](./typescript-backend.md)
