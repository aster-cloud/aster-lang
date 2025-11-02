# 变量流动分析

> 更新时间：2025-11-02 23:35 NZST｜执行者：Codex

## 1. 当前架构

### Env 数据结构
- 基于 `HashMap<String, Object>` 保存名称到值的映射。
- `Loader` 在 `buildProgramInternal` 中创建唯一的 `Env` 实例，并在构建语法树时注入到各个节点。
- 所有语法节点共享同一个 `Env`；不存在父作用域链或嵌套环境。
- 不携带类型信息或作用域元数据，完全运行时动态解析。

### 变量分类
1. **参数变量**
   - 来源：函数形参，在 `CoreModel.Func` 转换过程中产生。
   - 作用域：`LambdaValue.apply` 执行期间，以 `env.set(param, arg)` 注入，同步保存旧值以便恢复。
   - 当前实现：`Loader` 在 `CallNode` 构建前将参数名写入 `Env`（初始化为 `null`），调用时由 `LambdaValue` 绑定并在返回后恢复旧值。
   - 读取方式：`NameNode.execute` 直接从共享 `Env` 中取值，属于动态作用域读取。

2. **局部变量**
   - 来源：`Let` 语句以及 `Match` 模式绑定。
   - 作用域：缺乏显式生命周期管理，绑定后留在全局 `Env`，可能被后续语句覆盖。
   - 当前实现：`LetNode.execute` 将初始值绑定到 `Env`；`MatchNode.PatCtorNode` 在匹配成功时调用 `env.set` 绑定模式变量；无自动清理。
   - 读取方式：后续 `NameNode` 调用同名变量即获取。

3. **闭包变量**
   - 来源：`CoreModel.Lambda` 的 `captures` 列表，以及函数定义捕获的外部名称。
   - 作用域：保存在 `LambdaValue.captures` 的 `Map` 中，同时 `LambdaValue` 仍持有对全局 `Env` 的引用。
   - 当前实现：`Loader.buildExpr` 在创建 `LiteralNode(new LambdaValue(...))` 时预先读取当前 `Env` 中的捕获值并存入 `captures`；在调用 `apply` 时，先保存旧值，再写入捕获值，执行后恢复旧值。

## 2. 变量读写模式

### NameNode（读取）
- 路径：`aster-truffle/nodes/NameNode.java`。
- 行为：忽略 `VirtualFrame`，直接对注入的 `Env` 调用 `env.get(name)`。
- 影响：无法区分变量类型；参数、局部以及全局函数名称全部通过同一映射解析。

### LetNode（绑定）
- 路径：`aster-truffle/nodes/LetNode.java`。
- 行为：计算初始化表达式后调用 `env.set(name, value)`；无作用域清理。
- 特性：在 `AsterConfig.DEBUG` 时打印绑定日志；对同名变量进行覆盖写入。

### SetNode（赋值）
- 路径：`aster-truffle/nodes/SetNode.java`。
- 行为：求值后执行 `env.set(name, value)`；若原变量不存在，会创建新条目。
- 影响：缺少未定义检查，赋值语句可能默默引入新变量。

### CallNode（调用）
- 路径：`aster-truffle/nodes/CallNode.java`。
- 行为：解析目标后区分 `LambdaValue` 与内建函数调用。
- 环境交互：调用 `LambdaValue.apply` 时传入共享的 `Env`；`CallNode` 本身不直接访问 `Env`，但通过 `NameNode` 解析函数名称与参数。

### LambdaValue（闭包）
- 路径：`aster-truffle/nodes/LambdaValue.java`。
- 行为：在 `apply` 内保存当前 `Env` 中的捕获与形参旧值，写入新值执行体节点，最后统一恢复。
- 闭包模型：捕获值在创建时存入 `captures`，但执行仍依赖共享 `Env` 进行覆盖与恢复，属于动态环境模拟。

## 3. Frame 迁移策略

### 槽位分配
1. **编译期分配**
   - 形参槽位：根据函数签名顺序分配索引 `0..n-1`。
   - 局部变量槽位：在语句解析阶段建立符号表，分配 `n..m-1`。
   - 需要新的符号表结构记录名称到槽位的映射，以替换当前 `Env` 查表。

2. **运行期访问**
   - `NameNode`：从 `VirtualFrame` 使用 `frame.getObject(slotIndex)` 获取。
   - `LetNode`：通过 `frame.setObject(slotIndex, value)` 或者 `FrameSlot` API 写入。
   - `SetNode`：与 `LetNode` 相同，执行赋值写入。

### 闭包处理
- 方案 A：使用 `MaterializedFrame` 捕获整个调用帧，简单直接但内存成本高。
- 方案 B：基于符号表对需要捕获的槽位进行拷贝，构建局部环境快照。
- 建议：迁移初期采用方案 A，优先保证行为一致，再优化为按需捕获。

## 4. 迁移步骤
1. 引入 `FrameSlotBuilder` 等工具，基于语义模型分配槽位并生成 `FrameDescriptor`。
2. 修改 `NameNode` 使用帧槽位读取，移除对 `Env` 的依赖。
3. 调整 `LetNode` 在执行时写入帧槽位，并在调试模式下打印槽位信息。
4. 调整 `SetNode` 的赋值逻辑，兼容帧槽位及必要的未定义检查。
5. 更新 `LambdaValue`，在闭包创建时捕获 `MaterializedFrame` 或槽位快照。
6. 清理剩余节点中的 `Env` 注入逻辑，并在 `Loader` 中改为构建帧描述。

## 5. 风险点
- 共享 `Env` 的现有语义等同于动态作用域，迁移到 Truffle 帧需要确认是否保持既有覆盖行为。
- `MatchNode` 与 `StartNode` 等模式绑定节点需要同步迁移，防止槽位分配遗漏临时变量。
- 闭包恢复逻辑目前依赖 `Map` 顺序，转换为帧后需要确保恢复顺序与重入安全。
- 函数重入和递归依赖 `LambdaValue` 保存旧值，改用帧后必须验证递归场景无回归。

