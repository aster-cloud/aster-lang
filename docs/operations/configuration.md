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

### 1.3 CI、发布与集成
| 变量 | 必需 | 默认值 | 用途 | 示例值 | 生产建议 |
| --- | --- | --- | --- | --- | --- |
| `GITHUB_TOKEN` | CI 必需 | GitHub 注入 | Changesets/GitHub Release 工作流推送变更 | `GITHUB_TOKEN=ghp_xxx` | CI 自动提供；本地需申请 PAT |
| `GH_TOKEN` | 否 | 未设置 | `scripts/create-pr.js` 的备用 token | `GH_TOKEN=ghp_alt` | 仅本地或自托管使用 |
| `GITHUB_REPOSITORY` | CI 必需 | GitHub 注入 | `owner/repo` 标识用于 PR 自动化 | `GITHUB_REPOSITORY=wontlost-ltd/aster-lang` | CI 自动提供 |
| `NODE_AUTH_TOKEN` | 发布必需 | 未设置 | `npm publish` 所需凭据（通常等于 `NPM_TOKEN`） | `export NODE_AUTH_TOKEN=$NPM_TOKEN` | 发布前显式导出 |
| `NPM_TOKEN` | 发布必需 | 未设置 | npm registry 凭据 | `NPM_TOKEN=xxxx-xxxx` | 保存在 CI secrets |
| `CI_DEBUG` | 否 | 未设置 | CI 流程调试开关，控制部分 LSP smoke 脚本 | `CI_DEBUG=1` | 仅在排查时使用 |

## 2. 能力 manifest 格式
- 环境变量 `ASTER_CAPS` 指向 JSON 文件，示例结构：
  ```json
  {
    "allow": {
      "io": ["demo.service.*", "demo.service.fetch"],
      "cpu": ["*"]
    },
    "deny": {
      "io": ["demo.service.legacy*"],
      "cpu": []
    }
  }
  ```
- 规则：
  - `deny` 优先级高于 `allow`。
  - 支持通配符：`*`、`module.*`、`module.func`、`module.func*`。
  - Manifest 更新后需重启 CLI/LSP 进程或重新运行 `npm run typecheck` 以重新加载。

## 3. 配置验证方法
1. 健康检查脚本：
   ```bash
   NODE_ENV=production node scripts/health-check.ts
   ```
   - 若 `missingRequired` 非空，需要补全必需变量。
2. 结构化日志验证：
   ```bash
   LOG_LEVEL=DEBUG node dist/scripts/cli.js cnl/examples/greet.cnl
   ```
   - 确认输出 JSON 包含 `level`, `timestamp`, `component`, `message`。
3. 能力校验验证：
   ```bash
   ASTER_CAPS=cnl/examples/capabilities.json npm run typecheck
   ```
   - 确认能力违规会触发带有错误 ID（如 `E1001`）的诊断。
4. 安全基线：
   ```bash
   npm run audit           # audit-ci --moderate
   npx audit-ci --high     # release/security workflow 同款
   ```
5. Gradle/Java 链路：
   ```bash
  npm run emit:class -- cnl/examples/greet.cnl
   npm run javap:verify
   ```
   - 若 `JAVA_HOME` 缺失会立即报错，部署前必须验证。

## 4. 配置变更记录建议
- 所有环境变量变更需在配置管理系统（如 Vault/ConfigMap）中备份版本。
- Manifest 文件应通过代码审查合入（建议放置于 `configs/aster/capabilities.json`），并在变更 PR 中附带能力差异说明。
- 日志等级、能力校验开关等关键变量在生产环境变更前需通过变更审批。
- 发布脚本涉及的 token（`NPM_TOKEN`/`GITHUB_TOKEN`）统一由 CI 管理，避免写入仓库文件。
