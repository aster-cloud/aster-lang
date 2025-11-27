# Contributing to Aster

Thank you for your interest in contributing to Aster! This document provides guidelines and information for contributors.

## Table of Contents

- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Code Style](#code-style)
- [Testing](#testing)
- [开发包管理功能](#开发包管理功能)
- [Pull Request Process](#pull-request-process)
- [Issue Reporting](#issue-reporting)
- [Architecture Overview](#architecture-overview)

## Getting Started

Aster is a pragmatic, safe, fast programming language with human-readable CNL (Controlled Natural Language) syntax that compiles to JVM bytecode. The project consists of:

- **CNL Frontend**: Canonicalizer → Lexer → Parser → AST
- **Core IR**: Distinct intermediate representation with effect system
- **Type System**: Hindley-Milner-lite with non-null by default
- **Backends**: JVM bytecode and Truffle/GraalVM (planned)

## Development Setup

### Prerequisites

- Node.js 22+
- npm
- Git
- JDK 21+ (for JVM modules and examples)

### Setup

```bash
# Clone the repository
git clone https://github.com/wontlost-ltd/aster-lang.git
cd aster

# Install dependencies
npm install

# Build the project
npm run build

# Run tests
npm test

# Run the CLI
npm run build && node dist/scripts/cli.js test/cnl/examples/greet.aster
```

### Development Commands

```bash
# Development mode (watch for changes)
npm run dev

# Type checking
npm run typecheck

# Linting
npm run lint
npm run lint:fix

# Formatting
npm run format
npm run format:check

# Testing
npm run test:golden      # Golden snapshot tests
npm run test:property    # Property-based tests
npm run test:fuzz        # Fuzz tests
npm run bench           # Performance benchmarks

# Full CI pipeline (Node 22+, JDK 21+ for JVM checks)
npm run ci

# CI debug for LSP code actions (prints diagnostics and action titles)
CI_DEBUG=1 npm run ci
```

## Code Style

We use TypeScript with strict settings and enforce consistent code style:

### TypeScript Guidelines

- **Strict mode**: All TypeScript strict flags enabled
- **Explicit types**: Function return types must be explicit
- **No `any`**: Avoid `any` type; use proper types or `unknown`
- **Readonly**: Prefer `readonly` for arrays and objects that shouldn't be mutated
- **Null safety**: Use `| null` explicitly; avoid `undefined` in APIs

### Formatting

- **Prettier**: Automatic formatting with opinionated settings
- **Single quotes**: Use single quotes for strings
- **2 spaces**: Indentation (matches CNL syntax)
- **100 characters**: Line length limit
- **Trailing commas**: ES5 style

### Naming Conventions

- **Files**: kebab-case (`lexer.ts`, `core-ir.ts`)
- **Functions**: camelCase (`parseExpression`, `lowerModule`)
- **Types**: PascalCase (`TokenKind`, `Expression`)
- **Constants**: UPPER_SNAKE_CASE (`TOKEN_KIND`, `EFFECT_IO`)

## Testing

We maintain high test coverage with multiple testing strategies:

### Golden Tests

Snapshot tests that compare actual output with expected JSON:

```bash
npm run test:golden
```

Add new examples in `test/cnl/examples/` and run `npm run test:golden:update` to generate expected outputs.

### Property Tests

Property-based tests using fast-check:

```bash
npm run test:property
```

These test invariants like:
- Canonicalizer idempotency
- Lexer always produces EOF token
- Parser error handling with position info

### Fuzz Tests

Robustness tests with random inputs:

```bash
npm run test:fuzz
```

These ensure the lexer/parser don't crash on malformed input.

### Performance Tests

Benchmark critical paths:

```bash
npm run bench
```

We track performance regressions and maintain throughput targets.

## 开发包管理功能

包管理相关工作必须先完成 CLI 构建与测试，再进行包体验验证。以下流程覆盖运行测试、手动验证命令、构建示例包与 manifest 规范，确保 registry 逻辑稳定。

### 运行 CLI 测试

1. 执行 `npm run build`，生成 `dist/scripts/aster.js` 与 `dist/src/cli/**/*`。任何 CLI 测试都依赖 dist 产物，务必先完成一次完整构建。
2. 在同一终端运行 `npm run test:cli`，覆盖 install/list/search/update/remove 等命令的单元与集成测试。若新增命令，请同步补充 `test/cli/commands/*.test.ts` 并保持测试全部通过。
3. 设置 `NODE_V8_COVERAGE=.coverage/cli` 后运行 `npm run test:cli:coverage`，在 PR 中附上覆盖率截图或数值，确保语句和分支覆盖率没有回退。
4. 需要真实二进制回归时，执行 `npm run test:e2e:cli`。该脚本会在临时目录安装包并验证锁文件写入流程，可捕捉 dist 产物与源码行为不一致的问题。

关键注意事项：

- 在 `git clean -fdx` 后复跑一遍 CLI 测试，确保没有漏掉对生成文件或缓存的依赖。
- 对涉及网络的命令启用 `MOCK_REGISTRY=1`，防止测试过程中访问真实仓库。
- 将 CLI 测试结果写入 `docs/testing.md`，便于追踪历史基线。

### 测试 CLI 命令

以下示例展示如何在本地验证常见命令的行为：

```bash
# 1) 构建 dist 产物
npm run build

# 2) 安装本地 registry 中的示例包
node dist/scripts/aster.js install aster.math --registry=.aster/local-registry

# 3) 列出依赖并检查过期版本
node dist/scripts/aster.js list --outdated --json

# 4) 搜索包并验证远程/本地 fallback
node dist/scripts/aster.js search math

# 5) 运行 CLI 测试套件
npm run test:cli && npm run test:cli:coverage
```

操作建议：

- 使用 `mktemp -d` 或示例项目目录执行上述命令，避免污染仓库根目录。
- 在修改命令选项或输出格式后，务必记录期望输出并附在 PR 中，方便审查。
- 本地验证完成后，附加一轮 `npm run docs:build`，确认 CLI 文档中的示例同步更新。

### 创建示例包

1. 运行 `npm run build:examples`，在 `examples/packages/*` 目录下为每个示例生成 tarball，并自动同步到 `.aster/local-registry`。
2. 新增示例包时，在 `examples/packages/<package>/` 中准备 `manifest.json`、`README.md` 与 `src/*.aster`。保持命名与命令帮助一致，便于文档引用。
3. 使用 `node dist/scripts/aster.js install <包名> --registry=.aster/local-registry` 验证包是否能正确解压、写入 `.aster/packages/<pkg>/<ver>/`，并更新 `manifest.json` 与 `.aster.lock`。
4. 将示例包安装场景写入 `test/cli/commands/*.test.ts` 或文档，保证 CI 可重复同样的体验。
5. 如果示例包会被教程引用，请在 `docs/guide/package-management/` 下补充对应章节，确保用户得到一致的安装指引。

### manifest.json 规范

- 以仓库根目录的 `manifest.schema.json` 为唯一来源，新增字段时需同步修改 schema、TypeScript 类型与 CLI 校验逻辑。
- 包名必须遵循 `aster.<domain>.<name>`，仅允许小写字母、数字与连字符；违反约束将触发 `[M003]` 诊断并在 CLI 中中止。
- `version` 采用严格 SemVer，CLI 会拒绝携带构建元数据或 `latest` 这类浮动标识。
- `dependencies` 与 `devDependencies` 都要求 SemVer 范围，建议使用 `^` 或精确版本，并在示例包中给出最小子集，避免拖慢安装。
- `registry` 字段用于声明默认获取地址；若指向 GitHub Releases，需提供 `org/repo`、`tag` 与 `asset` 信息，方便 CLI 自动下载。
- manifest 结构发生变更时，务必同步更新 `docs/guide/package-management/manifest-reference.md` 与 CLI 帮助文本，保证用户文档和实现一致。

## Pull Request Process

1. **Fork** the repository and create a feature branch
2. **Write tests** for new functionality
3. **Follow code style** (enforced by CI)
4. **Update documentation** if needed
5. **Run the full CI pipeline**: `npm run ci`
6. **Submit PR** with clear description

### PR Requirements

- [ ] All tests pass (`npm test`)
- [ ] Code is formatted (`npm run format:check`)
- [ ] No linting errors (`npm run lint`)
- [ ] TypeScript compiles (`npm run typecheck`)
- [ ] Performance benchmarks pass (`npm run bench`)
- [ ] Documentation updated if needed

### PR Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Added/updated tests
- [ ] All tests pass
- [ ] Performance impact assessed

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
```

## Issue Reporting

### Bug Reports

Use the bug report template and include:

- **Aster version**: `npm list aster-lang`
- **Node.js version**: `node --version`
- **Operating system**: OS and version
- **Input code**: Minimal example that reproduces the issue
- **Expected behavior**: What should happen
- **Actual behavior**: What actually happens
- **Error output**: Full error message with stack trace

### Feature Requests

Use the feature request template and include:

- **Use case**: Why is this feature needed?
- **Proposed syntax**: How should it look in CNL?
- **Alternatives**: Other ways to achieve the same goal
- **Implementation**: Any thoughts on how to implement it

## Architecture Overview

### Pipeline

```
CNL Source → Canonicalizer → Lexer → Parser → CNL AST → Lowering → Core IR
```

### Key Modules

- **`src/canonicalizer.ts`**: Normalizes CNL text (whitespace, keywords, punctuation)
- **`src/lexer.ts`**: Tokenizes canonicalized text with INDENT/DEDENT handling
- **`src/parser.ts`**: Recursive descent parser producing CNL AST
- **`src/lower_to_core.ts`**: Lowers CNL AST to Core IR
- **`src/core_ir.ts`**: Core IR definitions with effect system
- **`src/diagnostics.ts`**: Structured error reporting with fix-its

### Design Principles

1. **Human-readable syntax**: CNL should read like natural language
2. **Type safety**: Non-null by default, explicit effects, exhaustive matching
3. **Performance**: Fast compilation, efficient runtime
4. **Tooling**: Great IDE support, helpful error messages
5. **Interoperability**: Seamless Java/JVM integration

### Adding New Features

1. **CNL syntax**: Update `src/tokens.ts` with new keywords
2. **Lexer**: Handle new token types in `src/lexer.ts`
3. **Parser**: Add parsing logic in `src/parser.ts`
4. **AST**: Define new node types in `src/types.ts` and `src/ast.ts`
5. **Core IR**: Add corresponding Core IR nodes if needed
6. **Lowering**: Update `src/lower_to_core.ts` to handle new nodes
7. **Tests**: Add golden tests, property tests, and examples

## JVM/Quarkus 模块开发

Aster 包含多个 JVM 模块，使用 Gradle 构建：

### 模块结构

```
aster-core/          # 核心运行时（调度器、事件存储、重试机制）
aster-truffle/       # GraalVM Truffle 后端
aster-lang-cli/      # Java CLI 工具
quarkus-policy-api/  # Quarkus REST API（策略评估、审计）
policy-editor/       # Vaadin Policy Editor
```

### JVM 开发环境

```bash
# 前置条件
- JDK 21+
- GraalVM 21+ (可选，用于 Truffle 后端)

# 构建所有 JVM 模块
./gradlew build

# 运行特定模块测试
./gradlew :quarkus-policy-api:test
./gradlew :aster-truffle:test

# 启动 Quarkus 开发模式
cd quarkus-policy-api
./mvnw quarkus:dev
```

### Quarkus 模块贡献

1. **REST API**: 添加到 `quarkus-policy-api/src/main/java/io/aster/policy/`
2. **GraphQL 类型**: 更新 `graphql/types/` 目录
3. **数据库迁移**: 在 `resources/db/migration/` 添加 Flyway 脚本
4. **测试**: 使用 `@QuarkusTest` 注解编写集成测试

### Truffle 后端贡献

1. **新内置函数**: 在 `Builtins.java` 添加 `@NodeChild` 注解的节点
2. **性能基准**: 更新 `benchmarks/` 目录
3. **Golden 测试**: 确保 Core IR 兼容性

---

## AI 代码生成贡献

### AI 模块结构

```
src/ai/
├── generator.ts       # 主生成器
├── llm-provider.ts    # LLM 接口定义
├── providers/         # OpenAI/Anthropic 实现
├── prompt-manager.ts  # Few-shot 示例管理
├── validator.ts       # 代码验证
├── provenance.ts      # 来源追踪
└── generation-cache.ts # 缓存机制
```

### 添加新 LLM Provider

1. 创建 `src/ai/providers/my-provider.ts`：

```typescript
import { LLMProvider, LLMError } from '../llm-provider.js';

export class MyProvider implements LLMProvider {
  async generate(prompt: string): Promise<string> {
    // 实现 API 调用
  }

  getName(): string { return 'my-provider'; }
  getModel(): string { return this.model; }
}
```

2. 在 `src/cli/commands/ai-generate.ts` 注册
3. 添加测试到 `test/unit/ai/`

### Few-shot 示例贡献

在 `prompts/few-shot-examples.jsonl` 添加高质量示例：

```jsonl
{"description": "Your description", "code": "To your_function..."}
```

示例要求：
- 描述清晰、具体
- 代码通过类型检查
- 覆盖不同业务场景

---

## 合规功能开发

### 审计日志贡献

1. **哈希算法**: 修改 `AuditEventListener.computeHashChain()`
2. **数据库 Schema**: 添加 `V*__*.sql` 迁移脚本
3. **验证器**: 更新 `AuditChainVerifier`

### PII 检查贡献

1. **新检测规则**: 修改 `src/lsp/pii_diagnostics.ts`
2. **Quick Fix**: 更新 `src/lsp/codeaction.ts`
3. **测试**: 添加到 `test/unit/lsp/pii-diagnostics.test.ts`

### 合规测试

```bash
# PII 诊断测试
npm run test:pii-default

# 审计链测试
./gradlew :quarkus-policy-api:test --tests "*AuditChain*"

# 合规集成测试
npm run test:compliance
```

---

## 医疗域库开发

### 现有医疗功能

```
quarkus-policy-api/src/main/
├── java/io/aster/policy/graphql/types/HealthcareTypes.java
├── java/io/aster/policy/graphql/converter/HealthcareConverter.java
└── resources/policies/healthcare/
    ├── eligibility.aster
    └── claims.aster
```

### 贡献医疗策略

1. 在 `resources/policies/healthcare/` 添加 `.aster` 文件
2. 更新 `HealthcareTypes.java` 添加新类型
3. 编写 `*ConverterTest.java` 测试
4. 确保 HIPAA 合规注释

### HIPAA 合规检查清单

- [ ] 所有 PHI 字段使用 `@pii(L3, ...)` 标注
- [ ] 不在日志中输出 PHI
- [ ] 使用 HTTPS 传输
- [ ] 包含同意检查

---

## 文档贡献

### 文档结构

```
docs/
├── guide/           # 用户指南
├── reference/       # API 参考
├── operations/      # 运维文档
└── phase0/          # 技术设计文档
```

### 文档标准

- 使用简体中文或英文
- 包含代码示例
- 添加最后更新日期
- 链接相关文档

### 构建文档

```bash
npm run docs:build
npm run docs:serve  # 本地预览
```

---

## Getting Help

- **Discussions**: Use GitHub Discussions for questions
- **Issues**: Report bugs and request features
- **Discord**: Join our community server (link in README)
- **Documentation**: Check the docs site at aster-lang.org

## License

By contributing to Aster, you agree that your contributions will be licensed under the MIT License.
