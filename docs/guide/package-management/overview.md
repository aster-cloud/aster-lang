# 包管理系统概述

> 更新：2025-11-25 09:46 NZST · Codex

本节概览 Aster 包管理系统的核心组件、依赖解析算法、缓存策略以及与 GitHub Releases 的集成方式，帮助你评估在团队与 CI 中的部署可行性。

## 架构

包管理系统由四个层次组成：

1. **CLI 命令层**：`aster pkg <command>` 负责解析 CLI 参数并调度具体动作。
2. **解析器 (Resolver)**：根据 `manifest.json` 与 `aster.lock` 计算依赖图，执行 SemVer 匹配与冲突回溯。
3. **存储层**：包含全局缓存 (`~/.aster/cache`) 与项目本地链接目录 (`.aster/modules`)。
4. **分发适配器**：目前内置 GitHub Releases 适配器，未来可扩展 OCI/自建源。

```aster
module aster.pkg.resolver

type Resolution = {
  package: Text,
  version: Text,
  source: Text,
  checksum: Text,
}

fn link(resolutions: List[Resolution]): IO[Unit] {
  for res in resolutions {
    cache::link(res)
  }
}
```

::: tip
在 CI 中运行包命令时，可通过 `ASTER_CACHE_DIR=$CI_PROJECT/.cache/aster` 将缓存放在可复用的工作目录，配合 `npm ci` 达到完全可复现构建。
:::

## 依赖解析：SemVer 与回溯

解析器会按以下顺序探索解：

1. 针对 `dependencies` 中的每个条目，优先尝试 `aster.lock` 中已存在的版本，保持稳定性。
2. 若锁文件缺少条目，则在满足 SemVer 的范围内，从最高可用版本开始向下搜索。
3. 出现冲突时，解析器会回溯到最近决策点，尝试次优版本；若全部失败则提示无法满足的约束链。

使用 `--explain` 可以查看详细的解析过程：

```bash
aster pkg install --explain wontlost/http@^1.5 wontlost/serde@~2.0
```

示例输出：

```text
Resolving wontlost/http@^1.5 -> 1.6.2
Resolving wontlost/serde@~2.0 -> 2.0.5
Conflict detected: serde 2.0.5 requires http >=1.7
↺ backtracking http -> 1.7.0
✓ solution found (2 packages, depth=3)
```

::: warning
当 `manifest.json` 同时声明 Git 引用与 SemVer 范围时，解析器会优先取 Git commit，这可能绕过锁文件。生产环境请保持单一来源并在审查中确认。
:::

## 缓存与链接策略

- **全局缓存**：按照 `<registry>/<package>/<version>/package.tgz` 结构存放，下载完成后立即写入 SHA-256 校验。
- **项目链接**：解析完成后将包展开到 `.aster/modules/<package>`，并在 `node_modules/.aster` 写入 shim 以供工具链发现。
- **清理命令**：`aster pkg cache prune --days 30` 会删除 30 天未访问的缓存条目。

```bash
aster pkg cache prune --days 14 && aster pkg list --summary
```

```text
Pruned 12 entries (3.2 GB) from /Users/dev/.aster/cache
Active packages: 8
```

## GitHub Releases 集成

默认 registry 会使用 GitHub Releases API：

1. 解析器读取包元数据，定位 `owner/repo` 与期望标签。
2. 下载命名为 `aster-package.tgz` 的 Release 资产。
3. 校验 `manifest.json` 中 `checksum` 字段（如存在）。

可以通过 CLI 强制刷新 release：

```bash
aster pkg install wontlost/http --from github --tag v1.6.2 --force-refresh
```

在企业私有仓库中，只需设置 `GITHUB_TOKEN`，包管理器会使用该 token 访问受保护的 Release 资产。这种设计让包的分发管道与现有发布流程复用 GitHub 安全模型，无需额外的私有 registry 维护成本。

