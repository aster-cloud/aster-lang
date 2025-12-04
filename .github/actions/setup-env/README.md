# Setup Env Action

统一执行 checkout、Node.js、Java/GraalVM、Gradle 权限以及 npm ci，减少各工作流重复配置。

## 输入参数

| 参数 | 必需 | 默认值 | 说明 |
|------|------|--------|------|
| `checkout` | 否 | `true` | 是否自动执行 actions/checkout |
| `fetch-depth` | 否 | `1` | checkout 的 fetch 深度 |
| `enable-node` | 否 | `true` | 是否安装 Node.js |
| `node-version` | 否 | `22` | Node.js 版本 |
| `cache-dependency-path` | 否 | `package-lock.json` | Node 缓存依赖文件路径 |
| `registry-url` | 否 | `https://registry.npmjs.org` | npm registry URL |
| `enable-java` | 否 | `false` | 是否安装 Java/GraalVM |
| `java-provider` | 否 | `graalvm` | Java 提供者：`graalvm` 或 `temurin` |
| `java-version` | 否 | `25` | Java 版本 |
| `java-distribution` | 否 | `graalvm-community` | Java 发行版（temurin、graalvm-community 等） |
| `native-image-job-reports` | 否 | `false` | 是否输出 native-image job 报告 |
| `npm-ci` | 否 | `true` | 是否执行 npm ci |
| `enable-gradle` | 否 | `false` | 是否为 gradlew 设置执行权限并配置缓存 |
| `gradle-cache-read-only` | 否 | `auto` | Gradle 缓存只读模式：`auto`（PR 只读，main 可写）、`true`、`false` |
| `working-directory` | 否 | `.` | npm/gradle 命令执行目录 |
| `github-token` | 否 | `''` | 供 GraalVM 下载使用的 GitHub Token（默认使用 `github.token`） |

## 使用示例

### 基础用法（Node.js 项目）

```yaml
- name: Setup environment
  uses: ./.github/actions/setup-env
```

这将自动：
- Checkout 代码
- 安装 Node.js 22
- 执行 `npm ci`

### Java/GraalVM 项目

```yaml
- name: Setup environment
  uses: ./.github/actions/setup-env
  with:
    enable-java: 'true'
    enable-gradle: 'true'
```

这将自动：
- Checkout 代码
- 安装 GraalVM 25
- 安装 Node.js 22 并执行 `npm ci`
- 设置 gradlew 权限并配置 Gradle 缓存

### 仅 Java（不需要 Node.js）

```yaml
- name: Setup environment
  uses: ./.github/actions/setup-env
  with:
    enable-java: 'true'
    enable-gradle: 'true'
    enable-node: 'false'
    npm-ci: 'false'
```

### 使用 Temurin JDK

```yaml
- name: Setup environment
  uses: ./.github/actions/setup-env
  with:
    enable-java: 'true'
    java-provider: 'temurin'
    java-distribution: 'temurin'
    java-version: '21'
```

### 自定义 checkout（如需要更深的历史）

```yaml
- name: Setup environment
  uses: ./.github/actions/setup-env
  with:
    fetch-depth: '0'  # 完整历史
```

### 跳过 checkout（已在之前步骤完成）

```yaml
- name: Checkout with custom options
  uses: actions/checkout@v4
  with:
    submodules: recursive

- name: Setup environment
  uses: ./.github/actions/setup-env
  with:
    checkout: 'false'
```

## 缓存策略

### Node.js 缓存
- 使用 `actions/setup-node` 内置的 npm 缓存
- 缓存键基于 `package-lock.json`

### Gradle 缓存
- 使用 `gradle/actions/setup-gradle` 配置缓存
- `gradle-cache-read-only: auto` 模式下：
  - `main` 分支：可读可写（更新缓存）
  - PR 分支：只读（避免缓存污染）

### GraalVM 缓存
- 使用 `graalvm/setup-graalvm` 内置的 Gradle 缓存

## 版本说明

| 工具 | 默认版本 | 说明 |
|------|---------|------|
| Node.js | 22 | 当前 LTS 版本 |
| Java/GraalVM | 25 | GraalVM CE 最新版本 |
| Gradle | - | 由项目 `gradle-wrapper.properties` 决定 |
