# 包管理快速入门

> 更新：2025-11-25 09:46 NZST · Codex

Aster 自带的包管理器帮助你在不同项目之间重用模块、锁定依赖版本，并保持构建结果可复现。本指南通过三个步骤带你完成：理解包管理的作用、安装第一个包、查看与验证当前安装。

## 什么是包管理

Aster 包管理器会读取项目根目录的 `manifest.json`，解析依赖树并将下载的包缓存在 `~/.aster/cache`。解析过程遵循 SemVer 匹配和回溯策略：当多个依赖请求不同版本时，解析器会尝试按照“最高兼容版本 → 次高版本 → 回退”的顺序寻找解，直到所有约束得到满足。解析结果会写入 `aster.lock`，确保团队成员获得完全一致的依赖集。

::: tip
当你在 monorepo 中维护多个子项目时，可以将它们指向同一个 `~/.aster/cache`，利用硬链接在秒级完成安装。
:::

下面的示例展示了如何在代码中引用外部包提供的模块：

```aster
import net/http as http
import wontlost/collections/list as List

fn fetchStatus(url: Text): Result[Int, Text] {
  let response = http::get(url)
  if response.status == 200 {
    Result::Ok(response.status)
  } else {
    Result::Err("unexpected status")
  }
}
```

## 安装第一个包

确保 `manifest.json` 中声明了依赖，例如 `"dependencies": { "wontlost/collections": "^1.2.0" }`。随后运行安装命令：

```bash
aster pkg install
```

典型终端输出如下：

```text
→ reading manifest.json
→ resolving wontlost/collections@^1.2.0
✓ pinned wontlost/collections@1.2.3 (dist: github)
✓ wrote lockfile aster.lock
✓ linked packages to .aster/modules
```

如果想单独安装某个包，可指定名称与版本：

```bash
aster pkg install wontlost/http@1.5.0 --registry https://packages.aster.dev
```

::: warning
默认 registry 会优先尝试 GitHub Releases。若你在离线环境，请在命令中显式传入 `--registry` 指向镜像，并同步缓存目录，否则解析会持续重试导致安装超时。
:::

## 查看已安装包

使用 `aster pkg list` 检查当前项目可用的包：

```bash
aster pkg list --status linked
```

可能的输出：

```text
Linked modules (project-root/.aster/modules)
• wontlost/collections 1.2.3  (capabilities: cpu)
• wontlost/http         1.5.0  (capabilities: io.net)
```

若需要验证缓存，可运行：

```bash
aster pkg cache --ls net/http
```

完成安装后，重新编译即可使用新依赖：

```bash
npm run build && aster pkg list
```

```text
Build completed in 3.4s — packages ready
```

将 `import wontlost/collections/list as List` 写入模块后，IDE 会立即获得类型补全。若 `aster pkg list` 未列出期望版本，请删除 `aster.lock` 并重新执行 `aster pkg install` 以触发解析与回溯。这样一来，你已经完成了包管理的快速入门流程。

