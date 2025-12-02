# CLI 命令参考（包管理）

> 更新：2025-11-25 09:46 NZST · Codex

本页聚焦包管理相关的 CLI 子命令：`install`、`list`、`update`、`search`。每个命令都包含常用选项、示例与典型终端输出，便于在本地、CI 以及生产回滚时复用。

## install — 安装依赖

**语法**：`aster pkg install [<name@range> ...] [options]`

**常用选项**：

- `--registry <url>`：覆盖默认 registry（GitHub Releases）。
- `--dev/--no-dev`：是否安装 `devDependencies`。
- `--explain`：输出解析与回溯过程。
- `--lockfile <path>`：自定义锁文件位置，适合多工作树。

```bash
aster pkg install wontlost/http@^1.6 wontlost/serde@~2.0 --explain
```

```text
Resolving wontlost/http@^1.6 -> 1.6.2
Resolving wontlost/serde@~2.0 -> 2.0.4
✓ wrote aster.lock (2 packages)
```

安装完成后即可在模块中直接引用：

```aster
import wontlost/http as http

fn heartbeat(url: Text): IO[Unit] {
  http::get(url)
}
```

::: tip
在 CI 中建议配合 `--locked`（默认读取现有 `aster.lock`，若锁缺失则失败），保证分支始终使用经过审查的依赖集。
:::

## list — 查看依赖状态

**语法**：`aster pkg list [options]`

**常用选项**：

- `--status <linked|cached|outdated>`：过滤展示范围。
- `--summary`：仅输出统计信息。
- `--json`：以 JSON 形式输出，方便脚本消费。

```bash
aster pkg list --status linked --json
```

```text
[
  {"name":"wontlost/http","version":"1.6.2","capabilities":["io.net"]},
  {"name":"wontlost/serde","version":"2.0.4","capabilities":["cpu"]}
]
```

::: warning
`--json` 输出会被写到 stdout，若你需要同时查看人类可读格式，请重定向 `--json > report.json`，否则终端信息会被 JSON 覆盖。
:::

## update — 升级已安装包

**语法**：`aster pkg update [<name>] [options]`

- 若省略包名，则根据 SemVer 范围升级全部依赖。
- `--to <version>`：强制跳到指定版本。
- `--preid <tag>`：允许安装预发布标签（如 `beta`）。

```bash
aster pkg update wontlost/http --to 1.7.0 --lockfile release/aster.lock
```

```text
Current lock: 1.6.2 → Candidate: 1.7.0 (compatible)
✓ updated wontlost/http in release/aster.lock
```

升级后请运行冒烟测试确认：

```bash
npm run test:golden && aster pkg list --status outdated
```

```text
Outdated packages: 0
```

## search — 查找远程包

**语法**：`aster pkg search <keyword> [options]`

**常用选项**：

- `--limit <n>`：最多返回 `n` 个结果。
- `--capability <name>`：按能力过滤，例如 `io.db`。
- `--registry <url>`：查询其他 registry。

```bash
aster pkg search http --capability io.net --limit 3
```

```text
• wontlost/http           1.7.0  ⭐ 390  — HTTP client with IO net capability
• asterlabs/http-metrics  0.3.2  ⭐  58  — HTTP hooks with metrics signals
• skylight/http-server    0.6.1  ⭐ 104  — Async HTTP server primitives
```

如需根据搜索结果快速验证 API，可在 REPL 中导入：

```aster
import skylight/http-server as server

fn main(): IO[Unit] {
  server::start(8080)
}
```

执行 `aster pkg install skylight/http-server@0.6.1` 后即可运行上面的示例。

