> 更新时间：2025-10-08 14:57（NZDT）  
> 执行者：Codex

# 配置手册

## 1. 环境变量清单

### 1.1 核心运行时
| 变量 | 必需 | 默认值 | 用途 | 示例值 | 生产建议 |
| --- | --- | --- | --- | --- | --- |
| `NODE_ENV` | 是 | 未设置会导致健康检查失败 | 标识运行环境，决定配置与日志策略 | `NODE_ENV=production` | 必须设为 `production` |
| `ASTER_CAP_EFFECTS_ENFORCE` | 是（必须保持开启） | 任意非 `0` 值视为开启 | 控制能力校验是否启用 (`src/config/runtime.ts`) | `ASTER_CAP_EFFECTS_ENFORCE=1` | 生产禁止设为 `0` |
| `ASTER_CAPS` | 否 | 未设时跳过 manifest 校验 | 指向能力 manifest JSON，Typecheck/LSP 根据此校验 | `ASTER_CAPS=/etc/aster/caps.json` | 建议配置，保证能力治理 |
| `LOG_LEVEL` | 否 | `INFO` | 控制结构化日志最小级别 (`src/utils/logger.ts`) | `LOG_LEVEL=DEBUG` | 生产保持 `INFO` 或以上 |
| `ASTER_DEBUG_TYPES` | 否 | 关闭 | 启用解析类型调试输出 (`src/parser.ts`) | `ASTER_DEBUG_TYPES=1` | 仅调试开启 |

### 1.2 构建与 JVM 工具链
| 变量 | 必需 | 默认值 | 用途 | 示例值 | 生产建议 |
| --- | --- | --- | --- | --- | --- |
| `JAVA_HOME` | 是 | 依赖宿主环境 | 指向 GraalVM/Java 21 安装路径（Truffle/Gradle 脚本读取） | `JAVA_HOME=/usr/lib/jvm/graalvm-21` | 确保指向可执行 `bin/java` |
| `GRADLE_OPTS` | 否 | 空字符串 | 追加到 Gradle 子进程参数（prefers IPv4/IPv6 由脚本默认追加） | `GRADLE_OPTS='-Xmx4g'` | 可用于调节内存 |
| `JAVA_OPTS` | 否 | 空字符串 | JVM 启动参数，脚本默认追加网络偏好 | `JAVA_OPTS='-Xmx4g -Xms2g'` | 根据容器资源调整 |
| `GRADLE_USER_HOME` | 否 | `~/.gradle` | 覆盖 Gradle 缓存目录，以便在沙箱/容器中持久化 | `GRADLE_USER_HOME=/var/cache/gradle` | 挂载到持久卷 |
| `ASTER_TRUFFLE_DEBUG` | 否 | `1`（脚本默认） | 控制 Truffle 调试开关 | `ASTER_TRUFFLE_DEBUG=0` | 生产关闭以减少日志噪声 |
| `ASTER_ROOT` | 否 | 自动注入 | CLI/脚本推断仓库根目录 | `ASTER_ROOT=/srv/aster` | 无需手动设置 |
| `INTEROP_NULL_STRICT` | 否 | 关闭 | 互操作空值严格模式验证脚本使用 | `INTEROP_NULL_STRICT=true` | 仅在验收或回归测试时开启 |

### 1.3 LSP 与 PII 诊断配置
| 变量/参数 | 必需 | 默认值 | 用途 | 示例值 | 生产建议 |
| --- | --- | --- | --- | --- | --- |
| `--enforce-pii` | 否 | `false` | **启用类型层 PII 流分析**，检测隐私数据泄漏（E070/E072/E073） | `aster-lsp --enforce-pii` | 开发环境建议启用 |
| `ENFORCE_PII` | 否 | `false` | 环境变量方式启用 PII 检查（优先级低于 `--enforce-pii`） | `ENFORCE_PII=true` | 生产审计环境启用 |
| `ASTER_ENFORCE_PII` | 否 | `false` | 备用环境变量（与 `ENFORCE_PII` 等价，大小写无关） | `ASTER_ENFORCE_PII=true` | 同上 |
| `--strict-pii` | 否 | `false` | **控制语义层 PII 诊断严重级别**：false=Warning，true=Error | `aster-lsp --strict-pii` | 结合 `--enforce-pii` 使用 |

#### PII 诊断配置详解

**配置优先级**（从高到低）：
1. LSP 配置注入（`--enforce-pii` CLI 参数）
2. 环境变量（`ENFORCE_PII` / `ASTER_ENFORCE_PII`，大小写无关）
3. 默认值：`false`（opt-in 策略，需显式启用）

**--enforce-pii 与 --strict-pii 的区别**：

| 参数 | 作用范围 | 检测层次 | 诊断类型 | 严重级别控制 |
| --- | --- | --- | --- | --- |
| `--enforce-pii` | 启用/禁用 PII 检查 | **类型层**（完整流分析） | E070/E072/E073 | 固定为 Error |
| `--strict-pii` | 控制诊断严重性 | **语义层**（HTTP 场景） | E400（HTTP 未加密） | false=Warning，true=Error |

**类型层 PII 诊断范围**（需启用 `--enforce-pii`）：
- **E070** (`PII_ASSIGN_DOWNGRADE`)：PII 数据赋值给 plain 变量，导致隐私级别降级
- **E072** (`PII_SINK_UNSANITIZED`)：PII 数据流向未授权 sink（如 HTTP/日志）
- **E073** (`PII_ARG_VIOLATION`)：函数参数 PII 级别不匹配，导致污染传播

**推荐配置场景**：

```bash
# 场景 1：开发环境（全量检查 + 错误级别阻塞）
aster-lsp --enforce-pii --strict-pii

# 场景 2：CI/CD（启用检查，警告不阻塞构建）
aster-lsp --enforce-pii

# 场景 3：生产审计（通过环境变量启用）
export ENFORCE_PII=true
aster-lsp

# 场景 4：默认行为（仅语义层 HTTP 警告，类型层 PII 检查禁用）
aster-lsp  # 无参数，向后兼容
```

**验证配置生效**：
```bash
# 1. 创建测试文件 test-pii.aster，包含 PII 违规代码
echo 'To test with pii@L2, produce Text: Return Http.post("url", pii).' > test-pii.aster

# 2. 启用 PII 检查并验证诊断
ENFORCE_PII=true npm run typecheck -- test-pii.aster
# 预期：显示 E072 (PII_SINK_UNSANITIZED) 诊断

# 3. 禁用 PII 检查（默认行为）
npm run typecheck -- test-pii.aster
# 预期：不显示类型层 PII 诊断（E070/E072/E073）
```

### 1.4 CI、发布与集成
| 变量 | 必需 | 默认值 | 用途 | 示例值 | 生产建议 |
| --- | --- | --- | --- | --- | --- |
| `GITHUB_TOKEN` | CI 必需 | GitHub 注入 | Changesets/GitHub Release 工作流推送变更 | `GITHUB_TOKEN=ghp_xxx` | CI 自动提供；本地需申请 PAT |
| `GH_TOKEN` | 否 | 未设置 | `scripts/create-pr.js` 的备用 token | `GH_TOKEN=ghp_alt` | 仅本地或自托管使用 |
| `GITHUB_REPOSITORY` | CI 必需 | GitHub 注入 | `owner/repo` 标识用于 PR 自动化 | `GITHUB_REPOSITORY=wontlost-ltd/aster-lang` | CI 自动提供 |
| `NODE_AUTH_TOKEN` | 发布必需 | 未设置 | `npm publish` 所需凭据（通常等于 `NPM_TOKEN`） | `export NODE_AUTH_TOKEN=$NPM_TOKEN` | 发布前显式导出 |
| `NPM_TOKEN` | 发布必需 | 未设置 | npm registry 凭据 | `NPM_TOKEN=xxxx-xxxx` | 保存在 CI secrets |
| `CI_DEBUG` | 否 | 未设置 | CI 流程调试开关，控制部分 LSP smoke 脚本 | `CI_DEBUG=1` | 仅在排查时使用 |

## 2. 能力 manifest 格式
- 环境变量 `ASTER_CAPS` 指向 JSON 文件。
- Manifest 更新后需重启 CLI/LSP 进程或重新运行 `npm run typecheck` 以重新加载。

### 2.1 细粒度能力示例
- 细粒度能力 manifest 示例：
  ```json
  {
    "allow": {
      "Http": ["api.example.com/*"],
      "Sql": ["db.query*"],
      "Files": ["*"]
    }
  }
  ```
- 支持同时配置 `deny`，仍然遵循 `deny` 优先于 `allow` 的规则。
- 通配符规则保持不变：`*`、`module.*`、`module.func`、`module.func*`。

### 2.2 Legacy 语法兼容
- 旧版 manifest 仍然兼容，可继续使用原有粗粒度能力：
  ```json
  {
    "allow": {
      "io": ["*"],
      "cpu": ["*"]
    }
  }
  ```
- 粗粒度 `io` 等价于同时授予 `Http` 与 `Sql` 类能力，`cpu` 语法保持原状。

### 2.3 能力类型速查表
| 能力 | 描述 |
| --- | --- |
| Http | HTTP 网络请求 |
| Sql | 数据库查询 |
| Time | 时间相关操作 |
| Files | 文件系统操作 |
| Secrets | 密钥和敏感数据访问 |
| AiModel | AI 模型调用 |
| CPU | CPU 密集型计算 |

## 3. 配置验证方法
1. 健康检查脚本：
   ```bash
   NODE_ENV=production node scripts/health-check.ts
   ```
   - 若 `missingRequired` 非空，需要补全必需变量。
2. 结构化日志验证：
   ```bash
   LOG_LEVEL=DEBUG node dist/scripts/cli.js test/cnl/examples/greet.aster
   ```
   - 确认输出 JSON 包含 `level`, `timestamp`, `component`, `message`。
3. 能力校验验证：
   ```bash
   ASTER_CAPS=test/cnl/examples/capabilities.json npm run typecheck
   ```
   - 确认能力违规会触发带有错误 ID（如 `E1001`）的诊断。
4. 安全基线：
   ```bash
   npm run audit           # audit-ci --moderate
   npx audit-ci --high     # release/security workflow 同款
   ```
5. Gradle/Java 链路：
   ```bash
  npm run emit:class -- test/cnl/examples/greet.aster
   npm run javap:verify
   ```
   - 若 `JAVA_HOME` 缺失会立即报错，部署前必须验证。

## 4. 配置变更记录建议
- 所有环境变量变更需在配置管理系统（如 Vault/ConfigMap）中备份版本。
- Manifest 文件应通过代码审查合入（建议放置于 `configs/aster/capabilities.json`），并在变更 PR 中附带能力差异说明。
- 日志等级、能力校验开关等关键变量在生产环境变更前需通过变更审批。
- 发布脚本涉及的 token（`NPM_TOKEN`/`GITHUB_TOKEN`）统一由 CI 管理，避免写入仓库文件。
