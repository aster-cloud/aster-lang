# 效果推断算法设计

> **状态**: 设计文档
> **版本**: 1.0
> **最后更新**: 2025-10-06

## 1. 概述

### 1.1 动机

当前 Aster 的效果系统要求开发者手动声明函数的效果（`It performs IO` 或 `It performs CPU`），并在编译时检查函数体是否符合声明。这种方式虽然显式且可控，但存在以下问题：

1. **维护负担**：当函数调用链变化时，需要手动更新所有受影响函数的效果声明
2. **声明冗余**：大多数情况下，函数的效果可以从其调用的函数自动推断
3. **错误易发**：开发者容易忘记添加或更新效果声明，导致编译错误

效果推断（Effect Inference）旨在自动推导函数的最小效果集，减少手动声明，同时保持类型安全。

### 1.2 目标

设计一个效果推断算法，满足以下需求：

- **自动化**：从函数调用关系自动推断最小效果集
- **保守性**：推断结果应该是安全的上界（over-approximation）
- **增量性**：支持模块级别的增量推断
- **诊断友好**：当推断效果与显式声明冲突时，生成清晰的错误信息
- **效果多态**：支持泛型函数的效果参数化（如 `map<E>(f: T -> U with E)`）

### 1.3 理论基础

效果推断基于 Hindley-Milner 类型推断的约束求解框架：

1. **约束收集**：遍历函数调用图，为每个调用点生成效果约束
2. **约束求解**：使用最小不动点算法求解约束系统
3. **诊断生成**：比较推断效果与显式声明，报告不一致

效果系统的格（Lattice）结构：

```
∅ (pure) ⊑ CPU ⊑ IO[*]
         ⊑ IO[Http] ⊑ IO[Http, Sql] ⊑ ...
```

其中 `⊑` 表示子效果关系，`IO[*]` 表示任意 IO 能力组合。

## 2. 效果系统回顾

Aster 的效果系统包含三类效果：

1. **Pure（纯计算）**：无副作用，无 IO，无 CPU 密集计算
2. **CPU（计算密集）**：纯计算但计算密集（如加密、压缩）
3. **IO（输入输出）**：与外界交互，可细化为能力子集
   - `IO[Http]`：HTTP 请求
   - `IO[Sql]`：数据库查询
   - `IO[Time]`：时间获取
   - `IO[Files]`：文件读写
   - `IO[Secrets]`：密钥访问
   - `IO[AiModel]`：AI 模型调用

效果声明语法：

```
To func_name with params, produce RetType. It performs IO:
  ...

To func_name with params, produce RetType. It performs CPU:
  ...

To func_name with params, produce RetType. It performs io with Http and Sql:
  ...
```

当前实现使用前缀匹配（`src/config/effects.ts`）判断调用是否属于特定效果。

## 3. 算法设计

效果推断分为四个阶段：

```
┌─────────────────┐
│ 1. 约束收集     │  遍历调用图，生成效果约束
└────────┬────────┘
         │
         v
┌─────────────────┐
│ 2. 约束求解     │  使用最小不动点算法求解
└────────┬────────┘
         │
         v
┌─────────────────┐
│ 3. 效果多态处理 │  实例化泛型函数的效果参数
└────────┬────────┘
         │
         v
┌─────────────────┐
│ 4. 诊断生成     │  比较推断与声明，生成错误
└─────────────────┘
```

### 3.1 输入与输出

**输入**：
- Core IR 模块（已完成词法、语法、AST 降级）
- 效果配置（`effects.ts` 中的前缀规则）
- 可选的显式效果声明

**输出**：
- 每个函数的推断效果集 `EffectEnv: Map<FunctionName, EffectSet>`
- 类型检查诊断列表（效果不匹配的错误/警告）

## 4. 约束收集阶段

### 4.1 约束类型

定义三种约束：

```typescript
type EffectConstraint =
  | { kind: 'Subset'; sub: EffectExpr; super: EffectExpr }  // sub ⊆ super
  | { kind: 'Equal'; left: EffectExpr; right: EffectExpr }  // left = right
  | { kind: 'Join'; result: EffectExpr; inputs: EffectExpr[] } // result = ⋃ inputs

type EffectExpr =
  | { kind: 'Var'; name: string }           // 效果变量（函数名）
  | { kind: 'Const'; effects: EffectSet }   // 具体效果集
  | { kind: 'Param'; param: string }        // 效果参数（用于泛型）
```

### 4.2 约束收集算法

```pseudocode
function collectEffectConstraints(module: Core.Module): Constraint[] {
  let constraints: Constraint[] = []

  // 为每个函数创建效果变量
  for each function f in module.decls {
    if f.kind == 'Func' {
      let effVar = EffectVar(f.name)

      // 如果有显式声明，添加等价约束
      if f.effects is explicitly declared {
        constraints.add(Equal(effVar, Const(f.effects)))
      }

      // 收集函数体的约束
      constraints.addAll(collectFromExpr(f.body, effVar))
    }
  }

  return constraints
}

function collectFromExpr(expr: Core.Expr, parentEff: EffectExpr): Constraint[] {
  let constraints: Constraint[] = []

  switch expr.kind {
    case 'Call':
      // 调用点约束：调用者效果 ⊇ 被调用者效果
      let calleeEff = inferCalleeEffect(expr.target)
      constraints.add(Subset(calleeEff, parentEff))

      // 递归处理参数
      for each arg in expr.args {
        constraints.addAll(collectFromExpr(arg, parentEff))
      }
      break

    case 'Block':
      // 块的效果 = 所有语句效果的并集
      let stmtEffects: EffectExpr[] = []
      for each stmt in expr.statements {
        let stmtEff = freshEffectVar()
        constraints.addAll(collectFromStmt(stmt, stmtEff))
        stmtEffects.add(stmtEff)
      }
      constraints.add(Join(parentEff, stmtEffects))
      break

    case 'If':
      // if 表达式的效果 = 条件 ∪ then 分支 ∪ else 分支
      let condEff = freshEffectVar()
      let thenEff = freshEffectVar()
      let elseEff = freshEffectVar()

      constraints.addAll(collectFromExpr(expr.cond, condEff))
      constraints.addAll(collectFromExpr(expr.then, thenEff))
      constraints.addAll(collectFromExpr(expr.else, elseEff))

      constraints.add(Join(parentEff, [condEff, thenEff, elseEff]))
      break

    case 'Match':
      // match 表达式的效果 = 所有分支效果的并集
      let caseEffects: EffectExpr[] = []
      for each case in expr.cases {
        let caseEff = freshEffectVar()
        constraints.addAll(collectFromExpr(case.body, caseEff))
        caseEffects.add(caseEff)
      }
      constraints.add(Join(parentEff, caseEffects))
      break

    case 'Lambda':
      // Lambda 的效果独立分析（闭包捕获的效果需要特殊处理）
      let lambdaEff = freshEffectVar()
      constraints.addAll(collectFromExpr(expr.body, lambdaEff))
      // Lambda 本身不直接贡献效果，除非立即调用
      break

    default:
      // 字面量、变量等无效果
      break
  }

  return constraints
}

function inferCalleeEffect(target: Core.Expr): EffectExpr {
  switch target.kind {
    case 'Name':
      // 函数名 → 效果变量
      return EffectVar(target.name)

    case 'Field':
      // 方法调用 → 检查前缀规则
      let fullName = target.obj.name + '.' + target.name

      if matchesPrefix(fullName, CAPABILITY_PREFIXES.Http) {
        return Const({ IO: ['Http'] })
      }
      else if matchesPrefix(fullName, CAPABILITY_PREFIXES.Sql) {
        return Const({ IO: ['Sql'] })
      }
      // ... 其他能力检查
      else if matchesPrefix(fullName, IO_PREFIXES) {
        return Const({ IO: ['*'] })  // 通用 IO
      }
      else if matchesPrefix(fullName, CPU_PREFIXES) {
        return Const({ CPU: true })
      }
      else {
        return Const({ Pure: true })  // 默认纯函数
      }

    default:
      return Const({ Pure: true })
  }
}
```

### 4.3 示例：约束收集

**示例 1：简单调用**

```
To helper, produce Text:
  Return "hello".

To main, produce Text:
  Return helper().
```

生成约束：
```
eff(helper) = ∅ (pure)
eff(main) ⊇ eff(helper)
```

**示例 2：IO 调用**

```
To fetch_user with id: Text, produce User. It performs io with Http:
  Return Http.get("/users/" + id).
```

生成约束：
```
eff(fetch_user) = IO[Http]  // 显式声明
eff(fetch_user) ⊇ IO[Http]  // Http.get 调用
```

**示例 3：传递调用**

```
To get_profile with id: Text, produce Profile:
  Let user = fetch_user(id).
  Return user.profile.

To fetch_user with id: Text, produce User. It performs io with Http:
  Return Http.get("/users/" + id).
```

生成约束：
```
eff(fetch_user) = IO[Http]
eff(get_profile) ⊇ eff(fetch_user)
→ eff(get_profile) ⊇ IO[Http]
```

## 5. 约束求解阶段

### 5.1 最小不动点算法

约束求解使用 Worklist 算法计算最小不动点：

```pseudocode
function solveConstraints(constraints: Constraint[]): EffectEnv {
  // 初始化效果环境：所有函数初始为 ∅
  let env: Map<String, EffectSet> = {}
  for each constraint in constraints {
    for each variable v in constraint {
      env[v] = ∅  // 初始为 pure
    }
  }

  // Worklist 算法
  let worklist: Constraint[] = constraints
  let changed = true

  while changed {
    changed = false

    for each constraint in worklist {
      switch constraint.kind {
        case 'Subset':
          // sub ⊆ super: super = super ∪ sub
          let subEffects = evalEffectExpr(constraint.sub, env)
          let superVar = extractVar(constraint.super)
          let oldSuper = env[superVar]
          let newSuper = union(oldSuper, subEffects)

          if newSuper != oldSuper {
            env[superVar] = newSuper
            changed = true
          }
          break

        case 'Equal':
          // left = right: 双向传播
          let leftEffects = evalEffectExpr(constraint.left, env)
          let rightEffects = evalEffectExpr(constraint.right, env)
          let merged = union(leftEffects, rightEffects)

          if env[extractVar(constraint.left)] != merged {
            env[extractVar(constraint.left)] = merged
            changed = true
          }
          if env[extractVar(constraint.right)] != merged {
            env[extractVar(constraint.right)] = merged
            changed = true
          }
          break

        case 'Join':
          // result = ⋃ inputs
          let joinedEffects = ∅
          for each input in constraint.inputs {
            joinedEffects = union(joinedEffects, evalEffectExpr(input, env))
          }

          let resultVar = extractVar(constraint.result)
          if env[resultVar] != joinedEffects {
            env[resultVar] = joinedEffects
            changed = true
          }
          break
      }
    }
  }

  return env
}

function union(a: EffectSet, b: EffectSet): EffectSet {
  // 效果并集：CPU ∪ IO = IO, IO[Http] ∪ IO[Sql] = IO[Http, Sql]
  if a.IO || b.IO {
    let caps = (a.IO?.caps || []).concat(b.IO?.caps || [])
    return { IO: { caps: unique(caps) } }
  }
  else if a.CPU || b.CPU {
    return { CPU: true }
  }
  else {
    return { Pure: true }
  }
}
```

### 5.2 迭代过程示例

**示例：传递推断**

```
To f, produce Text:
  Return g().

To g, produce Text:
  Return h().

To h, produce Text. It performs io with Http:
  Return Http.get("/data").
```

迭代过程：

```
初始化：
eff(f) = ∅, eff(g) = ∅, eff(h) = IO[Http]

约束：
eff(f) ⊇ eff(g)
eff(g) ⊇ eff(h)

第 1 轮迭代：
eff(g) = eff(g) ∪ eff(h) = ∅ ∪ IO[Http] = IO[Http] ✓ changed

第 2 轮迭代：
eff(f) = eff(f) ∪ eff(g) = ∅ ∪ IO[Http] = IO[Http] ✓ changed

第 3 轮迭代：
无变化 → 收敛

最终结果：
eff(f) = IO[Http], eff(g) = IO[Http], eff(h) = IO[Http]
```

## 6. 效果多态支持

### 6.1 效果参数化

支持泛型函数的效果参数化，类似于类型参数：

```
To map<T, U, E> with list: List of T, f: T -> U with E, produce List of U with E:
  ...
```

这里 `E` 是效果参数，表示 `map` 的效果取决于传入函数 `f` 的效果。

### 6.2 效果实例化

当调用泛型函数时，效果参数被实例化：

```
To process_users with ids: List of Text, produce List of User. It performs io with Http:
  Return map(ids, fetch_user).  // E 被实例化为 IO[Http]
```

约束生成：

```
eff(map) = E  // 效果参数
eff(fetch_user) = IO[Http]
E := IO[Http]  // 实例化
eff(process_users) ⊇ map[E=IO[Http]]
→ eff(process_users) ⊇ IO[Http]
```

### 6.3 效果参数推断算法

```pseudocode
function instantiateEffectParams(
  genericFunc: Func,
  args: Expr[],
  env: EffectEnv
): EffectSet {

  // 推断类型参数（省略类型推断细节）
  let typeSubst = inferTypeParams(genericFunc, args)

  // 推断效果参数
  let effectSubst: Map<EffectParam, EffectSet> = {}

  for each param in genericFunc.params {
    if param.type is FuncType with effect E {
      // 从实参推断效果
      let argEffect = inferExprEffect(args[param.index], env)
      effectSubst[E] = argEffect
    }
  }

  // 实例化泛型函数的效果
  return substituteEffectParams(genericFunc.effect, effectSubst)
}
```

### 6.4 示例：map 效果推断

```
To map<T, U, E> with list: List of T, f: T -> U with E, produce List of U with E:
  Match list:
    Case []: Return [].
    Case [head, ...tail]:
      Let result = f(head).
      Let rest = map(tail, f).
      Return [result, ...rest].

To get_user with id: Text, produce User. It performs io with Http:
  Return Http.get("/users/" + id).

To get_all_users with ids: List of Text, produce List of User. It performs io with Http:
  Return map(ids, get_user).
```

推断过程：

```
eff(map) = E (效果参数)
eff(get_user) = IO[Http]

调用 map(ids, get_user):
E := eff(get_user) = IO[Http]

eff(get_all_users) ⊇ map[E=IO[Http]] = IO[Http]
```

## 7. 诊断生成

### 7.1 诊断类型

生成三类诊断：

1. **效果缺失（Error）**：推断效果 > 声明效果
   ```
   ERROR: Function 'fetch_user' is declared as pure but performs IO[Http]
   ```

2. **效果冗余（Warning）**：声明效果 > 推断效果
   ```
   WARNING: Function 'helper' is declared with IO but only performs pure computation
   ```

3. **效果提示（Info）**：未声明效果，自动推断
   ```
   INFO: Function 'process' inferred to have effect IO[Http, Sql]
   ```

### 7.2 诊断生成算法

```pseudocode
function generateDiagnostics(
  module: Core.Module,
  inferredEnv: EffectEnv
): Diagnostic[] {

  let diagnostics: Diagnostic[] = []

  for each function f in module.decls {
    let declared = f.effects  // 显式声明的效果
    let inferred = inferredEnv[f.name]  // 推断的效果

    if declared == null {
      // 未声明：提示推断结果
      diagnostics.add({
        severity: 'info',
        message: `Function '${f.name}' inferred to have effect ${formatEffect(inferred)}`,
        location: f.span
      })
    }
    else if !isSubset(inferred, declared) {
      // 推断效果超出声明：错误
      let missing = difference(inferred, declared)
      diagnostics.add({
        severity: 'error',
        message: `Function '${f.name}' is declared with ${formatEffect(declared)} but performs ${formatEffect(inferred)}. Missing: ${formatEffect(missing)}`,
        location: f.span,
        relatedInfo: findCausingCalls(f, missing)
      })
    }
    else if !isSubset(declared, inferred) {
      // 声明效果超出推断：警告
      let superfluous = difference(declared, inferred)
      diagnostics.add({
        severity: 'warning',
        message: `Function '${f.name}' is declared with ${formatEffect(declared)} but only performs ${formatEffect(inferred)}. Superfluous: ${formatEffect(superfluous)}`,
        location: f.span
      })
    }
  }

  return diagnostics
}

function findCausingCalls(f: Func, missingEffect: EffectSet): RelatedInfo[] {
  // 追踪哪些调用点导致了缺失的效果
  let causing: RelatedInfo[] = []

  for each call in findCallsInFunc(f) {
    let callEffect = inferCalleeEffect(call.target)
    if isSubset(missingEffect, callEffect) {
      causing.add({
        message: `Call to '${call.target}' requires ${formatEffect(callEffect)}`,
        location: call.span
      })
    }
  }

  return causing
}
```

### 7.3 诊断示例

**示例 1：效果缺失**

```
To fetch_data with id: Text, produce Data:  // 未声明效果
  Return Http.get("/data/" + id).
```

诊断：
```
ERROR: Function 'fetch_data' performs IO[Http] but no effect is declared.
  Hint: Add 'It performs io with Http' to the function signature.
  Related:
    - Call to 'Http.get' at line 2:10 requires IO[Http]
```

**示例 2：效果冗余**

```
To calculate with x: Int, produce Int. It performs io with Http:  // 声明了 IO 但未使用
  Return x * 2.
```

诊断：
```
WARNING: Function 'calculate' is declared with IO[Http] but only performs pure computation.
  Hint: Remove the effect declaration or add IO operations.
```

**示例 3：效果不足**

```
To process with id: Text, produce Result. It performs io with Http:  // 声明了 Http 但还调用了 Sql
  Let user = Http.get("/users/" + id).
  Let profile = Db.query("SELECT * FROM profiles WHERE user_id = ?", [id]).
  Return Ok(Profile(user, profile)).
```

诊断：
```
ERROR: Function 'process' is declared with IO[Http] but performs IO[Http, Sql]. Missing: IO[Sql]
  Related:
    - Call to 'Db.query' at line 3:17 requires IO[Sql]
  Hint: Change declaration to 'It performs io with Http and Sql'
```

## 8. 复杂度分析

### 8.1 时间复杂度

设：
- `n` = 函数数量
- `m` = 调用边数量（调用图中的边）
- `k` = 平均效果集大小（通常 k ≤ 10）

**约束收集**：O(m)
遍历每个调用点生成约束。

**约束求解**：O(m × k × log k)
- 最坏情况下需要 O(m) 轮迭代（调用图深度）
- 每轮迭代处理 O(m) 条约束
- 每次效果并集操作为 O(k log k)（假设使用有序集合）

**诊断生成**：O(n + m)
遍历所有函数和调用点。

**总体**：O(m × k × log k) ≈ O(m log m)（当 k 为常数时）

### 8.2 空间复杂度

**效果环境**：O(n × k)
存储每个函数的效果集。

**约束集合**：O(m)
存储所有约束。

**总体**：O(n × k + m) ≈ O(n + m)

### 8.3 优化策略

1. **增量求解**：仅重新计算受影响的函数
2. **强连通分量**：先对调用图做 SCC 分解，自底向上求解
3. **效果缓存**：缓存标准库函数的效果
4. **并行化**：独立 SCC 可并行求解

## 9. 实现指南

### 9.1 集成点

效果推断应集成到 `src/typecheck.ts` 中：

```typescript
// src/typecheck.ts (伪代码)
export function typecheckModule(module: Core.Module): Diagnostic[] {
  let diagnostics: Diagnostic[] = []

  // 1. 类型检查（现有逻辑）
  diagnostics.push(...typecheck(module))

  // 2. 效果推断（新增）
  let effectEnv = inferEffects(module)
  diagnostics.push(...checkEffects(module, effectEnv))

  return diagnostics
}
```

### 9.2 模块结构

建议创建新文件 `src/effect_inference.ts`：

```typescript
// src/effect_inference.ts
export interface EffectSet {
  pure?: boolean
  cpu?: boolean
  io?: { caps: Capability[] }
}

export type EffectEnv = Map<string, EffectSet>

export function inferEffects(module: Core.Module): EffectEnv {
  let constraints = collectConstraints(module)
  return solveConstraints(constraints)
}

export function checkEffects(
  module: Core.Module,
  env: EffectEnv
): TypecheckDiagnostic[] {
  return generateDiagnostics(module, env)
}
```

### 9.3 测试策略

创建黄金测试用例覆盖：

1. **基础推断**：纯函数、CPU、单一 IO
2. **传递推断**：调用链推断
3. **效果合并**：多个 IO 能力的并集
4. **分支推断**：if/match 分支的并集
5. **效果多态**：泛型函数的效果实例化
6. **诊断测试**：效果缺失、冗余、不足的错误消息

测试文件位置：`cnl/examples/effect_infer_*.cnl`

## 10. 未来扩展

### 10.1 效果行（Effect Rows）

支持更精细的效果跟踪：

```
type Effect = { http: Bool, sql: Bool, ... }
```

使用行多态（row polymorphism）支持开放效果集。

### 10.2 条件效果

支持条件效果声明：

```
To process with data: Data, produce Result with if data.needsNetwork then IO[Http] else ∅:
  ...
```

### 10.3 效果掩码（Effect Masking）

支持局部屏蔽效果：

```
To run_isolated with f: () -> T with IO, produce T:
  Mask IO:
    Return f().  // 效果被隔离，不传播到 run_isolated
```

## 11. 参考文献

1. **Effect Systems Revisited**
   Lucassen, J.M. and Gifford, D.K. (1988)
   [ACM POPL 1988]

2. **Type and Effect Systems**
   Nielson, F. and Nielson, H.R. (1999)
   *Correct System Design*

3. **Koka: Programming with Row Polymorphic Effect Types**
   Leijen, D. (2014)
   Microsoft Research Technical Report

4. **Algebraic Effects for Functional Programming**
   Pretnar, M. (2015)
   *PhD Thesis, University of Edinburgh*

5. **Frank: First-class effect handlers**
   Lindley, S., McBride, C., and McLaughlin, C. (2017)
   [ACM POPL 2017]

---

**注**：本文档描述的算法为设计阶段，实际实现可能根据工程需求进行调整。优先级应放在正确性和可维护性，性能优化可在稳定后迭代。
