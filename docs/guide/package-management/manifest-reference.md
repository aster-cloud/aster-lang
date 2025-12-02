# manifest.json 参考

> 更新：2025-11-25 09:46 NZST · Codex

`manifest.json` 是包管理器识别项目的唯一入口。本页列出全部字段、`dependencies` 与 `devDependencies` 的差异、效果 (effects) / 能力 (capabilities) 声明方式，以及一个可直接复用的完整示例。

## 字段说明

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `name` | `Text` | 唯一的包名称，推荐格式 `owner/project` |
| `version` | `Text` | SemVer 版本，`1.2.3` 或 `1.2.3-beta.1` |
| `description` | `Text` | 简单描述，便于搜索与审查 |
| `entry` | `Text` | 入口 `.aster` 文件路径，如 `src/main.aster` |
| `license` | `Text` | 许可证标识，例 `MIT` |
| `repository` | `Text` | SCM 地址，支持 `https://` 或 `git@` |
| `authors` | `List<Text>` | 维护者列表 |
| `dependencies` | `Map<Text, Text>` | 运行时需要的包，SemVer 范围 |
| `devDependencies` | `Map<Text, Text>` | 仅在开发期间需要，如测试框架 |
| `targets` | `List<Text>` | 目标平台，例如 `['jvm', 'truffle']` |
| `effects` | `Map<Text, List<Text>>` | 模块与其效果需求映射 |
| `capabilities` | `Map<Text, Text>` | 包提供的能力标签，例如 `io.db` |
| `scripts` | `Map<Text, Text>` | 自定义命令，`aster pkg run <name>` |
| `checksum` | `Text` | 可选，配合外部分发的完整性校验 |

::: tip
如果你的包需要自定义构建步骤，可在 `scripts` 中提供 `preinstall` / `postinstall`，CLI 会在链接依赖前后自动执行。
:::

## dependencies vs devDependencies

- `dependencies` 中的条目会在 `aster pkg install` 和 `aster pkg list` 时全部解析，并被写入锁文件。
- `devDependencies` 只会在运行 `aster pkg install --dev` 或 `aster pkg run test` 时被拉取；生成最终算子或部署产物时，这些包不会出现在产出物内。

```bash
aster pkg install --no-dev   # 生产构建
aster pkg install --dev      # 本地开发
```

```text
Skipped devDependencies (2 packages) because --no-dev was supplied
```

## Effects 与 Capabilities

`effects` 描述了模块需要的效果等级，`capabilities` 描述包对外暴露的能力前缀，两者共同让类型系统在链接阶段捕获越权访问。

示例：

```aster
module wontlost/http

fn get(url: Text): IO[Response] requires io.net

effect httpClient {
  capability io.net
}
```

对应该包的 manifest 片段：

```json
{
  "effects": {
    "src/http.aster": ["io.net"]
  },
  "capabilities": {
    "io": "net",
    "cpu": "default"
  }
}
```

::: warning
如果 `effects` 中的模块路径与实际文件不一致，`aster pkg link` 会被迫降级为 "unknown" 能力，导致运行期拒绝执行。务必使用相对路径（相对 manifest 所在目录）。
:::

## 完整示例

```json
{
  "name": "wontlost/example-service",
  "version": "0.7.0",
  "description": "Demo package for HTTP + Metrics",
  "entry": "src/main.aster",
  "license": "Apache-2.0",
  "repository": "https://github.com/wontlost-ltd/example-service",
  "authors": ["Aster Team"],
  "targets": ["jvm", "truffle"],
  "dependencies": {
    "wontlost/http": "^1.6.0",
    "wontlost/metrics": "~0.4.0"
  },
  "devDependencies": {
    "wontlost/testing": "^0.5.0"
  },
  "scripts": {
    "preinstall": "npm run lint",
    "postinstall": "aster pkg list --summary"
  },
  "effects": {
    "src/main.aster": ["io.net", "cpu"],
    "src/metrics.aster": ["cpu"]
  },
  "capabilities": {
    "io": "net",
    "cpu": "bounded"
  }
}
```

校验 manifest：

```bash
aster pkg manifest validate
```

```text
Manifest OK — 3 dependencies, 1 devDependency, 2 effects entries
```

至此即可依赖 Manifest 描述来驱动解析器、能力系统以及 CI 审查流程。
