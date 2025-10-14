> 更新时间：2025-10-08 14:57（NZDT）  
> 执行者：Codex

# 回滚指南

## 1. 版本兼容性
- npm 包版本与 Git tag 一一对应（遵循 Changesets 流程），建议以最新 `v*` tag 作为回滚锚点。
- TypeScript 产物与 CLI/LSP 共享同一 `dist` 目录，回滚时必须同时替换 `dist` 与 `package.json`。
- JVM/Gradle 示例依赖 GraalVM 21；若回滚到较旧版本，确保 `JAVA_HOME` 指向兼容 JDK（仍建议保持 21）。
- 能力 manifest 与错误 ID 系统（`src/utils/errors.ts`）在近期迭代中有新增枚举，回滚旧版本后需同步更新监控/文档。

## 2. 回滚步骤
1. **锁定目标版本**
   ```bash
   git fetch --tags
   git checkout v0.x.y
   ```
2. **恢复依赖与产物**
   ```bash
   npm ci
   npm run build
   ```
3. **清理旧缓存**（确保无混合构建）：
   ```bash
   rm -rf dist node_modules/.cache
   npm ci && npm run build
   ```
4. **同步配置**：验证 `docs/operations/configuration.md` 中列出的环境变量与 manifest 是否与目标版本匹配。
5. **发布回滚版本（如需重新发布 npm）**
   ```bash
   npm run prepublishOnly
   npm publish --tag latest --access public
   ```
   > 若需撤销最新 npm 版本，可联系 npm 支持或使用 `npm dist-tag add` 将旧版本重新标记为 `latest`。

## 3. 数据与状态注意事项
- 项目本身不持久化业务数据，但会生成以下临时产物：
  - `dist/`：TypeScript 编译结果，必须随回滚版本重新生成。
  - `build/jvm-*`：JVM/ASM 产物，如需调试旧版本，应删除后重新执行对应脚本。
- 能力 manifest、配置文件等部署层资源不自动回滚，需手动恢复到与目标版本匹配的快照。
- 结构化日志与错误 ID 可能在不同版本之间新增字段，回滚后需通知日志消费方更新解析规则。

## 4. 回滚后的验证
1. **健康检查**
   ```bash
   NODE_ENV=production node scripts/health-check.ts
   ```
2. **能力校验**
   ```bash
   ASTER_CAP_EFFECTS_ENFORCE=1 npm run typecheck
   ```
   - 确认关键错误 ID（如 `E1001` 能力违规）仍按预期触发。
3. **核心测试**
   ```bash
   npm run ci
   ```
4. **CLI/LSP 烟雾测试**
   ```bash
   node dist/scripts/cli.js cnl/examples/greet.aster > /tmp/greet.json
   npm run lsp:workspace-diags:smoke
   ```
5. **依赖安全**
   ```bash
   npm run audit
   npx audit-ci --high
   ```

## 5. 紧急回滚流程（生产环境）
1. **触发条件**：部署后出现生产级故障（编译错误、能力误判、结构化日志异常）。
2. **执行路径**：
   - 立即切换到最近的稳定 tag（如 `v0.x.(y-1)`），执行上述步骤 2。
   - 在 CD 系统中将部署 artifact 切换至旧版本（容器镜像或 npm 包）。
3. **公告与监控**：
   - 在运维渠道同步回滚时间、影响范围、使用的错误 ID/日志。
   - 监控告警恢复后，再评估重新上线新版本的时间窗口。
4. **后续动作**：
   - 建议在 `.claude/workstreams/<task>/operations-log` 记录回滚原因。
   - 主动对比新旧版本的 capability manifest、错误 ID 定义差异，定位根因。
