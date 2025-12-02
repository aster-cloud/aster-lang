## 2025-11-05 16:06 NZST — Codex
- 使用 `sed` 阅读 `aster-truffle/src/test/java/aster/truffle/GoldenTestAdapter.java` 理解现有跳过逻辑与预期失败判断。
- 通过 `apply_patch` 更新 GoldenTestAdapter：
  - 引入 Jackson 解析入口函数参数，允许 `main` 无参而其余函数带参。
  - 新增 `bad_` 前缀预期失败为通过逻辑，并为高优先级错误用例校验异常信息。
  - 添加分类统计数据结构与 `@AfterAll` 输出。
- 执行 `./gradlew :aster-truffle:test --tests aster.truffle.GoldenTestAdapter` 验证现有用例全部保持原状并生成分类统计输出。
## 2025-11-05 17:33 NZST — Codex
- 按计划新增 10 个 golden 用例（boundary/bad 类别），为每个用例编写 `.aster` 并通过 `node dist/scripts/emit-core.js … > expected_*.json` 生成 Core IR。
- 引入 `AsterInteropAdapter`、`AsterListValue`、`AsterMapValue` 以封装 List/Map 返回值，并在 `AsterRootNode` 中统一调用 `Env.asGuestValue`，确保运行结果可被 Polyglot 接受。
- 更新 `GoldenTestAdapter` 预期失败匹配逻辑、分类统计和 `bad_*` 测试消息校验。
- 执行 `./gradlew :aster-truffle:test --tests aster.truffle.GoldenTestAdapter --rerun-tasks`，确认现有用例无回归且新增 6 PASS + 4 EXPECTED FAIL 均执行。
