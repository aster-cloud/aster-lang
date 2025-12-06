import{_ as a,c as n,o as i,aj as p}from"./chunks/framework.Bz2R-749.js";const o=JSON.parse('{"title":"效果推断算法设计","description":"","frontmatter":{},"headers":[],"relativePath":"reference/effect-inference-algorithm.md","filePath":"reference/effect-inference-algorithm.md"}'),e={name:"reference/effect-inference-algorithm.md"};function l(t,s,h,r,c,k){return i(),n("div",null,[...s[0]||(s[0]=[p(`<h1 id="效果推断算法设计" tabindex="-1">效果推断算法设计 <a class="header-anchor" href="#效果推断算法设计" aria-label="Permalink to “效果推断算法设计”">​</a></h1><blockquote><p><strong>状态</strong>: 设计文档 <strong>版本</strong>: 1.0 <strong>最后更新</strong>: 2025-10-06</p></blockquote><h2 id="_1-概述" tabindex="-1">1. 概述 <a class="header-anchor" href="#_1-概述" aria-label="Permalink to “1. 概述”">​</a></h2><h3 id="_1-1-动机" tabindex="-1">1.1 动机 <a class="header-anchor" href="#_1-1-动机" aria-label="Permalink to “1.1 动机”">​</a></h3><p>当前 Aster 的效果系统要求开发者手动声明函数的效果（<code>It performs IO</code> 或 <code>It performs CPU</code>），并在编译时检查函数体是否符合声明。这种方式虽然显式且可控，但存在以下问题：</p><ol><li><strong>维护负担</strong>：当函数调用链变化时，需要手动更新所有受影响函数的效果声明</li><li><strong>声明冗余</strong>：大多数情况下，函数的效果可以从其调用的函数自动推断</li><li><strong>错误易发</strong>：开发者容易忘记添加或更新效果声明，导致编译错误</li></ol><p>效果推断（Effect Inference）旨在自动推导函数的最小效果集，减少手动声明，同时保持类型安全。</p><h3 id="_1-2-目标" tabindex="-1">1.2 目标 <a class="header-anchor" href="#_1-2-目标" aria-label="Permalink to “1.2 目标”">​</a></h3><p>设计一个效果推断算法，满足以下需求：</p><ul><li><strong>自动化</strong>：从函数调用关系自动推断最小效果集</li><li><strong>保守性</strong>：推断结果应该是安全的上界（over-approximation）</li><li><strong>增量性</strong>：支持模块级别的增量推断</li><li><strong>诊断友好</strong>：当推断效果与显式声明冲突时，生成清晰的错误信息</li><li><strong>效果多态</strong>：支持泛型函数的效果参数化（如 <code>map&lt;E&gt;(f: T -&gt; U with E)</code>）</li></ul><h3 id="_1-3-理论基础" tabindex="-1">1.3 理论基础 <a class="header-anchor" href="#_1-3-理论基础" aria-label="Permalink to “1.3 理论基础”">​</a></h3><p>效果推断基于 Hindley-Milner 类型推断的约束求解框架：</p><ol><li><strong>约束收集</strong>：遍历函数调用图，为每个调用点生成效果约束</li><li><strong>约束求解</strong>：使用最小不动点算法求解约束系统</li><li><strong>诊断生成</strong>：比较推断效果与显式声明，报告不一致</li></ol><p>效果系统的格（Lattice）结构：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>∅ (pure) ⊑ CPU ⊑ IO[*]</span></span>
<span class="line"><span>         ⊑ IO[Http] ⊑ IO[Http, Sql] ⊑ ...</span></span></code></pre></div><p>其中 <code>⊑</code> 表示子效果关系，<code>IO[*]</code> 表示任意 IO 能力组合。</p><h2 id="_2-效果系统回顾" tabindex="-1">2. 效果系统回顾 <a class="header-anchor" href="#_2-效果系统回顾" aria-label="Permalink to “2. 效果系统回顾”">​</a></h2><p>Aster 的效果系统包含三类效果：</p><ol><li><strong>Pure（纯计算）</strong>：无副作用，无 IO，无 CPU 密集计算</li><li><strong>CPU（计算密集）</strong>：纯计算但计算密集（如加密、压缩）</li><li><strong>IO（输入输出）</strong>：与外界交互，可细化为能力子集 <ul><li><code>IO[Http]</code>：HTTP 请求</li><li><code>IO[Sql]</code>：数据库查询</li><li><code>IO[Time]</code>：时间获取</li><li><code>IO[Files]</code>：文件读写</li><li><code>IO[Secrets]</code>：密钥访问</li><li><code>IO[AiModel]</code>：AI 模型调用</li></ul></li></ol><p>效果声明语法：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>To func_name with params, produce RetType. It performs IO:</span></span>
<span class="line"><span>  ...</span></span>
<span class="line"><span></span></span>
<span class="line"><span>To func_name with params, produce RetType. It performs CPU:</span></span>
<span class="line"><span>  ...</span></span>
<span class="line"><span></span></span>
<span class="line"><span>To func_name with params, produce RetType. It performs io with Http and Sql:</span></span>
<span class="line"><span>  ...</span></span></code></pre></div><p>当前实现使用前缀匹配（<code>src/config/effects.ts</code>）判断调用是否属于特定效果。</p><h2 id="_3-算法设计" tabindex="-1">3. 算法设计 <a class="header-anchor" href="#_3-算法设计" aria-label="Permalink to “3. 算法设计”">​</a></h2><p>效果推断分为四个阶段：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>┌─────────────────┐</span></span>
<span class="line"><span>│ 1. 约束收集     │  遍历调用图，生成效果约束</span></span>
<span class="line"><span>└────────┬────────┘</span></span>
<span class="line"><span>         │</span></span>
<span class="line"><span>         v</span></span>
<span class="line"><span>┌─────────────────┐</span></span>
<span class="line"><span>│ 2. 约束求解     │  使用最小不动点算法求解</span></span>
<span class="line"><span>└────────┬────────┘</span></span>
<span class="line"><span>         │</span></span>
<span class="line"><span>         v</span></span>
<span class="line"><span>┌─────────────────┐</span></span>
<span class="line"><span>│ 3. 效果多态处理 │  实例化泛型函数的效果参数</span></span>
<span class="line"><span>└────────┬────────┘</span></span>
<span class="line"><span>         │</span></span>
<span class="line"><span>         v</span></span>
<span class="line"><span>┌─────────────────┐</span></span>
<span class="line"><span>│ 4. 诊断生成     │  比较推断与声明，生成错误</span></span>
<span class="line"><span>└─────────────────┘</span></span></code></pre></div><h3 id="_3-1-输入与输出" tabindex="-1">3.1 输入与输出 <a class="header-anchor" href="#_3-1-输入与输出" aria-label="Permalink to “3.1 输入与输出”">​</a></h3><p><strong>输入</strong>：</p><ul><li>Core IR 模块（已完成词法、语法、AST 降级）</li><li>效果配置（<code>effects.ts</code> 中的前缀规则）</li><li>可选的显式效果声明</li></ul><p><strong>输出</strong>：</p><ul><li>每个函数的推断效果集 <code>EffectEnv: Map&lt;FunctionName, EffectSet&gt;</code></li><li>类型检查诊断列表（效果不匹配的错误/警告）</li></ul><h2 id="_4-约束收集阶段" tabindex="-1">4. 约束收集阶段 <a class="header-anchor" href="#_4-约束收集阶段" aria-label="Permalink to “4. 约束收集阶段”">​</a></h2><h3 id="_4-1-约束类型" tabindex="-1">4.1 约束类型 <a class="header-anchor" href="#_4-1-约束类型" aria-label="Permalink to “4.1 约束类型”">​</a></h3><p>定义三种约束：</p><div class="language-typescript"><button title="Copy Code" class="copy"></button><span class="lang">typescript</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">type</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> EffectConstraint</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;"> =</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  |</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> { </span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">kind</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;"> &#39;Subset&#39;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">; </span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">sub</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> EffectExpr</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">; </span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">super</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> EffectExpr</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }  </span><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">// sub ⊆ super</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  |</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> { </span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">kind</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;"> &#39;Equal&#39;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">; </span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">left</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> EffectExpr</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">; </span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">right</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> EffectExpr</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }  </span><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">// left = right</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  |</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> { </span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">kind</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;"> &#39;Join&#39;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">; </span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">result</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> EffectExpr</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">; </span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">inputs</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> EffectExpr</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">[] } </span><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">// result = ⋃ inputs</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">type</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> EffectExpr</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;"> =</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  |</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> { </span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">kind</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;"> &#39;Var&#39;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">; </span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">name</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;"> string</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }           </span><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">// 效果变量（函数名）</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  |</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> { </span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">kind</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;"> &#39;Const&#39;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">; </span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">effects</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> EffectSet</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }   </span><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">// 具体效果集</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  |</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> { </span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">kind</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;"> &#39;Param&#39;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">; </span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">param</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;"> string</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }        </span><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">// 效果参数（用于泛型）</span></span></code></pre></div><h3 id="_4-2-约束收集算法" tabindex="-1">4.2 约束收集算法 <a class="header-anchor" href="#_4-2-约束收集算法" aria-label="Permalink to “4.2 约束收集算法”">​</a></h3><div class="language-text"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>function collectEffectConstraints(module: Core.Module): Constraint[] {</span></span>
<span class="line"><span>  let constraints: Constraint[] = []</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  // 为每个函数创建效果变量</span></span>
<span class="line"><span>  for each function f in module.decls {</span></span>
<span class="line"><span>    if f.kind == &#39;Func&#39; {</span></span>
<span class="line"><span>      let effVar = EffectVar(f.name)</span></span>
<span class="line"><span></span></span>
<span class="line"><span>      // 如果有显式声明，添加等价约束</span></span>
<span class="line"><span>      if f.effects is explicitly declared {</span></span>
<span class="line"><span>        constraints.add(Equal(effVar, Const(f.effects)))</span></span>
<span class="line"><span>      }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>      // 收集函数体的约束</span></span>
<span class="line"><span>      constraints.addAll(collectFromExpr(f.body, effVar))</span></span>
<span class="line"><span>    }</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  return constraints</span></span>
<span class="line"><span>}</span></span>
<span class="line"><span></span></span>
<span class="line"><span>function collectFromExpr(expr: Core.Expr, parentEff: EffectExpr): Constraint[] {</span></span>
<span class="line"><span>  let constraints: Constraint[] = []</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  switch expr.kind {</span></span>
<span class="line"><span>    case &#39;Call&#39;:</span></span>
<span class="line"><span>      // 调用点约束：调用者效果 ⊇ 被调用者效果</span></span>
<span class="line"><span>      let calleeEff = inferCalleeEffect(expr.target)</span></span>
<span class="line"><span>      constraints.add(Subset(calleeEff, parentEff))</span></span>
<span class="line"><span></span></span>
<span class="line"><span>      // 递归处理参数</span></span>
<span class="line"><span>      for each arg in expr.args {</span></span>
<span class="line"><span>        constraints.addAll(collectFromExpr(arg, parentEff))</span></span>
<span class="line"><span>      }</span></span>
<span class="line"><span>      break</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    case &#39;Block&#39;:</span></span>
<span class="line"><span>      // 块的效果 = 所有语句效果的并集</span></span>
<span class="line"><span>      let stmtEffects: EffectExpr[] = []</span></span>
<span class="line"><span>      for each stmt in expr.statements {</span></span>
<span class="line"><span>        let stmtEff = freshEffectVar()</span></span>
<span class="line"><span>        constraints.addAll(collectFromStmt(stmt, stmtEff))</span></span>
<span class="line"><span>        stmtEffects.add(stmtEff)</span></span>
<span class="line"><span>      }</span></span>
<span class="line"><span>      constraints.add(Join(parentEff, stmtEffects))</span></span>
<span class="line"><span>      break</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    case &#39;If&#39;:</span></span>
<span class="line"><span>      // if 表达式的效果 = 条件 ∪ then 分支 ∪ else 分支</span></span>
<span class="line"><span>      let condEff = freshEffectVar()</span></span>
<span class="line"><span>      let thenEff = freshEffectVar()</span></span>
<span class="line"><span>      let elseEff = freshEffectVar()</span></span>
<span class="line"><span></span></span>
<span class="line"><span>      constraints.addAll(collectFromExpr(expr.cond, condEff))</span></span>
<span class="line"><span>      constraints.addAll(collectFromExpr(expr.then, thenEff))</span></span>
<span class="line"><span>      constraints.addAll(collectFromExpr(expr.else, elseEff))</span></span>
<span class="line"><span></span></span>
<span class="line"><span>      constraints.add(Join(parentEff, [condEff, thenEff, elseEff]))</span></span>
<span class="line"><span>      break</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    case &#39;Match&#39;:</span></span>
<span class="line"><span>      // match 表达式的效果 = 所有分支效果的并集</span></span>
<span class="line"><span>      let caseEffects: EffectExpr[] = []</span></span>
<span class="line"><span>      for each case in expr.cases {</span></span>
<span class="line"><span>        let caseEff = freshEffectVar()</span></span>
<span class="line"><span>        constraints.addAll(collectFromExpr(case.body, caseEff))</span></span>
<span class="line"><span>        caseEffects.add(caseEff)</span></span>
<span class="line"><span>      }</span></span>
<span class="line"><span>      constraints.add(Join(parentEff, caseEffects))</span></span>
<span class="line"><span>      break</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    case &#39;Lambda&#39;:</span></span>
<span class="line"><span>      // Lambda 的效果独立分析（闭包捕获的效果需要特殊处理）</span></span>
<span class="line"><span>      let lambdaEff = freshEffectVar()</span></span>
<span class="line"><span>      constraints.addAll(collectFromExpr(expr.body, lambdaEff))</span></span>
<span class="line"><span>      // Lambda 本身不直接贡献效果，除非立即调用</span></span>
<span class="line"><span>      break</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    default:</span></span>
<span class="line"><span>      // 字面量、变量等无效果</span></span>
<span class="line"><span>      break</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  return constraints</span></span>
<span class="line"><span>}</span></span>
<span class="line"><span></span></span>
<span class="line"><span>function inferCalleeEffect(target: Core.Expr): EffectExpr {</span></span>
<span class="line"><span>  switch target.kind {</span></span>
<span class="line"><span>    case &#39;Name&#39;:</span></span>
<span class="line"><span>      // 函数名 → 效果变量</span></span>
<span class="line"><span>      return EffectVar(target.name)</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    case &#39;Field&#39;:</span></span>
<span class="line"><span>      // 方法调用 → 检查前缀规则</span></span>
<span class="line"><span>      let fullName = target.obj.name + &#39;.&#39; + target.name</span></span>
<span class="line"><span></span></span>
<span class="line"><span>      if matchesPrefix(fullName, CAPABILITY_PREFIXES.Http) {</span></span>
<span class="line"><span>        return Const({ IO: [&#39;Http&#39;] })</span></span>
<span class="line"><span>      }</span></span>
<span class="line"><span>      else if matchesPrefix(fullName, CAPABILITY_PREFIXES.Sql) {</span></span>
<span class="line"><span>        return Const({ IO: [&#39;Sql&#39;] })</span></span>
<span class="line"><span>      }</span></span>
<span class="line"><span>      // ... 其他能力检查</span></span>
<span class="line"><span>      else if matchesPrefix(fullName, IO_PREFIXES) {</span></span>
<span class="line"><span>        return Const({ IO: [&#39;*&#39;] })  // 通用 IO</span></span>
<span class="line"><span>      }</span></span>
<span class="line"><span>      else if matchesPrefix(fullName, CPU_PREFIXES) {</span></span>
<span class="line"><span>        return Const({ CPU: true })</span></span>
<span class="line"><span>      }</span></span>
<span class="line"><span>      else {</span></span>
<span class="line"><span>        return Const({ Pure: true })  // 默认纯函数</span></span>
<span class="line"><span>      }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    default:</span></span>
<span class="line"><span>      return Const({ Pure: true })</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span>}</span></span></code></pre></div><h3 id="_4-3-示例-约束收集" tabindex="-1">4.3 示例：约束收集 <a class="header-anchor" href="#_4-3-示例-约束收集" aria-label="Permalink to “4.3 示例：约束收集”">​</a></h3><p><strong>示例 1：简单调用</strong></p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>To helper, produce Text:</span></span>
<span class="line"><span>  Return &quot;hello&quot;.</span></span>
<span class="line"><span></span></span>
<span class="line"><span>To main, produce Text:</span></span>
<span class="line"><span>  Return helper().</span></span></code></pre></div><p>生成约束：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>eff(helper) = ∅ (pure)</span></span>
<span class="line"><span>eff(main) ⊇ eff(helper)</span></span></code></pre></div><p><strong>示例 2：IO 调用</strong></p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>To fetch_user with id: Text, produce User. It performs io with Http:</span></span>
<span class="line"><span>  Return Http.get(&quot;/users/&quot; + id).</span></span></code></pre></div><p>生成约束：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>eff(fetch_user) = IO[Http]  // 显式声明</span></span>
<span class="line"><span>eff(fetch_user) ⊇ IO[Http]  // Http.get 调用</span></span></code></pre></div><p><strong>示例 3：传递调用</strong></p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>To get_profile with id: Text, produce Profile:</span></span>
<span class="line"><span>  Let user = fetch_user(id).</span></span>
<span class="line"><span>  Return user.profile.</span></span>
<span class="line"><span></span></span>
<span class="line"><span>To fetch_user with id: Text, produce User. It performs io with Http:</span></span>
<span class="line"><span>  Return Http.get(&quot;/users/&quot; + id).</span></span></code></pre></div><p>生成约束：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>eff(fetch_user) = IO[Http]</span></span>
<span class="line"><span>eff(get_profile) ⊇ eff(fetch_user)</span></span>
<span class="line"><span>→ eff(get_profile) ⊇ IO[Http]</span></span></code></pre></div><h2 id="_5-约束求解阶段" tabindex="-1">5. 约束求解阶段 <a class="header-anchor" href="#_5-约束求解阶段" aria-label="Permalink to “5. 约束求解阶段”">​</a></h2><h3 id="_5-1-最小不动点算法" tabindex="-1">5.1 最小不动点算法 <a class="header-anchor" href="#_5-1-最小不动点算法" aria-label="Permalink to “5.1 最小不动点算法”">​</a></h3><p>约束求解使用 Worklist 算法计算最小不动点：</p><div class="language-text"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>function solveConstraints(constraints: Constraint[]): EffectEnv {</span></span>
<span class="line"><span>  // 初始化效果环境：所有函数初始为 ∅</span></span>
<span class="line"><span>  let env: Map&lt;String, EffectSet&gt; = {}</span></span>
<span class="line"><span>  for each constraint in constraints {</span></span>
<span class="line"><span>    for each variable v in constraint {</span></span>
<span class="line"><span>      env[v] = ∅  // 初始为 pure</span></span>
<span class="line"><span>    }</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  // Worklist 算法</span></span>
<span class="line"><span>  let worklist: Constraint[] = constraints</span></span>
<span class="line"><span>  let changed = true</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  while changed {</span></span>
<span class="line"><span>    changed = false</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    for each constraint in worklist {</span></span>
<span class="line"><span>      switch constraint.kind {</span></span>
<span class="line"><span>        case &#39;Subset&#39;:</span></span>
<span class="line"><span>          // sub ⊆ super: super = super ∪ sub</span></span>
<span class="line"><span>          let subEffects = evalEffectExpr(constraint.sub, env)</span></span>
<span class="line"><span>          let superVar = extractVar(constraint.super)</span></span>
<span class="line"><span>          let oldSuper = env[superVar]</span></span>
<span class="line"><span>          let newSuper = union(oldSuper, subEffects)</span></span>
<span class="line"><span></span></span>
<span class="line"><span>          if newSuper != oldSuper {</span></span>
<span class="line"><span>            env[superVar] = newSuper</span></span>
<span class="line"><span>            changed = true</span></span>
<span class="line"><span>          }</span></span>
<span class="line"><span>          break</span></span>
<span class="line"><span></span></span>
<span class="line"><span>        case &#39;Equal&#39;:</span></span>
<span class="line"><span>          // left = right: 双向传播</span></span>
<span class="line"><span>          let leftEffects = evalEffectExpr(constraint.left, env)</span></span>
<span class="line"><span>          let rightEffects = evalEffectExpr(constraint.right, env)</span></span>
<span class="line"><span>          let merged = union(leftEffects, rightEffects)</span></span>
<span class="line"><span></span></span>
<span class="line"><span>          if env[extractVar(constraint.left)] != merged {</span></span>
<span class="line"><span>            env[extractVar(constraint.left)] = merged</span></span>
<span class="line"><span>            changed = true</span></span>
<span class="line"><span>          }</span></span>
<span class="line"><span>          if env[extractVar(constraint.right)] != merged {</span></span>
<span class="line"><span>            env[extractVar(constraint.right)] = merged</span></span>
<span class="line"><span>            changed = true</span></span>
<span class="line"><span>          }</span></span>
<span class="line"><span>          break</span></span>
<span class="line"><span></span></span>
<span class="line"><span>        case &#39;Join&#39;:</span></span>
<span class="line"><span>          // result = ⋃ inputs</span></span>
<span class="line"><span>          let joinedEffects = ∅</span></span>
<span class="line"><span>          for each input in constraint.inputs {</span></span>
<span class="line"><span>            joinedEffects = union(joinedEffects, evalEffectExpr(input, env))</span></span>
<span class="line"><span>          }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>          let resultVar = extractVar(constraint.result)</span></span>
<span class="line"><span>          if env[resultVar] != joinedEffects {</span></span>
<span class="line"><span>            env[resultVar] = joinedEffects</span></span>
<span class="line"><span>            changed = true</span></span>
<span class="line"><span>          }</span></span>
<span class="line"><span>          break</span></span>
<span class="line"><span>      }</span></span>
<span class="line"><span>    }</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  return env</span></span>
<span class="line"><span>}</span></span>
<span class="line"><span></span></span>
<span class="line"><span>function union(a: EffectSet, b: EffectSet): EffectSet {</span></span>
<span class="line"><span>  // 效果并集：CPU ∪ IO = IO, IO[Http] ∪ IO[Sql] = IO[Http, Sql]</span></span>
<span class="line"><span>  if a.IO || b.IO {</span></span>
<span class="line"><span>    let caps = (a.IO?.caps || []).concat(b.IO?.caps || [])</span></span>
<span class="line"><span>    return { IO: { caps: unique(caps) } }</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span>  else if a.CPU || b.CPU {</span></span>
<span class="line"><span>    return { CPU: true }</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span>  else {</span></span>
<span class="line"><span>    return { Pure: true }</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span>}</span></span></code></pre></div><h3 id="_5-2-迭代过程示例" tabindex="-1">5.2 迭代过程示例 <a class="header-anchor" href="#_5-2-迭代过程示例" aria-label="Permalink to “5.2 迭代过程示例”">​</a></h3><p><strong>示例：传递推断</strong></p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>To f, produce Text:</span></span>
<span class="line"><span>  Return g().</span></span>
<span class="line"><span></span></span>
<span class="line"><span>To g, produce Text:</span></span>
<span class="line"><span>  Return h().</span></span>
<span class="line"><span></span></span>
<span class="line"><span>To h, produce Text. It performs io with Http:</span></span>
<span class="line"><span>  Return Http.get(&quot;/data&quot;).</span></span></code></pre></div><p>迭代过程：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>初始化：</span></span>
<span class="line"><span>eff(f) = ∅, eff(g) = ∅, eff(h) = IO[Http]</span></span>
<span class="line"><span></span></span>
<span class="line"><span>约束：</span></span>
<span class="line"><span>eff(f) ⊇ eff(g)</span></span>
<span class="line"><span>eff(g) ⊇ eff(h)</span></span>
<span class="line"><span></span></span>
<span class="line"><span>第 1 轮迭代：</span></span>
<span class="line"><span>eff(g) = eff(g) ∪ eff(h) = ∅ ∪ IO[Http] = IO[Http] ✓ changed</span></span>
<span class="line"><span></span></span>
<span class="line"><span>第 2 轮迭代：</span></span>
<span class="line"><span>eff(f) = eff(f) ∪ eff(g) = ∅ ∪ IO[Http] = IO[Http] ✓ changed</span></span>
<span class="line"><span></span></span>
<span class="line"><span>第 3 轮迭代：</span></span>
<span class="line"><span>无变化 → 收敛</span></span>
<span class="line"><span></span></span>
<span class="line"><span>最终结果：</span></span>
<span class="line"><span>eff(f) = IO[Http], eff(g) = IO[Http], eff(h) = IO[Http]</span></span></code></pre></div><h2 id="_6-效果多态支持" tabindex="-1">6. 效果多态支持 <a class="header-anchor" href="#_6-效果多态支持" aria-label="Permalink to “6. 效果多态支持”">​</a></h2><h3 id="_6-1-效果参数化" tabindex="-1">6.1 效果参数化 <a class="header-anchor" href="#_6-1-效果参数化" aria-label="Permalink to “6.1 效果参数化”">​</a></h3><p>支持泛型函数的效果参数化，类似于类型参数：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>To map&lt;T, U, E&gt; with list: List of T, f: T -&gt; U with E, produce List of U with E:</span></span>
<span class="line"><span>  ...</span></span></code></pre></div><p>这里 <code>E</code> 是效果参数，表示 <code>map</code> 的效果取决于传入函数 <code>f</code> 的效果。</p><h3 id="_6-2-效果实例化" tabindex="-1">6.2 效果实例化 <a class="header-anchor" href="#_6-2-效果实例化" aria-label="Permalink to “6.2 效果实例化”">​</a></h3><p>当调用泛型函数时，效果参数被实例化：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>To process_users with ids: List of Text, produce List of User. It performs io with Http:</span></span>
<span class="line"><span>  Return map(ids, fetch_user).  // E 被实例化为 IO[Http]</span></span></code></pre></div><p>约束生成：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>eff(map) = E  // 效果参数</span></span>
<span class="line"><span>eff(fetch_user) = IO[Http]</span></span>
<span class="line"><span>E := IO[Http]  // 实例化</span></span>
<span class="line"><span>eff(process_users) ⊇ map[E=IO[Http]]</span></span>
<span class="line"><span>→ eff(process_users) ⊇ IO[Http]</span></span></code></pre></div><h3 id="_6-3-效果参数推断算法" tabindex="-1">6.3 效果参数推断算法 <a class="header-anchor" href="#_6-3-效果参数推断算法" aria-label="Permalink to “6.3 效果参数推断算法”">​</a></h3><div class="language-text"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>function instantiateEffectParams(</span></span>
<span class="line"><span>  genericFunc: Func,</span></span>
<span class="line"><span>  args: Expr[],</span></span>
<span class="line"><span>  env: EffectEnv</span></span>
<span class="line"><span>): EffectSet {</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  // 推断类型参数（省略类型推断细节）</span></span>
<span class="line"><span>  let typeSubst = inferTypeParams(genericFunc, args)</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  // 推断效果参数</span></span>
<span class="line"><span>  let effectSubst: Map&lt;EffectParam, EffectSet&gt; = {}</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  for each param in genericFunc.params {</span></span>
<span class="line"><span>    if param.type is FuncType with effect E {</span></span>
<span class="line"><span>      // 从实参推断效果</span></span>
<span class="line"><span>      let argEffect = inferExprEffect(args[param.index], env)</span></span>
<span class="line"><span>      effectSubst[E] = argEffect</span></span>
<span class="line"><span>    }</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  // 实例化泛型函数的效果</span></span>
<span class="line"><span>  return substituteEffectParams(genericFunc.effect, effectSubst)</span></span>
<span class="line"><span>}</span></span></code></pre></div><h3 id="_6-4-示例-map-效果推断" tabindex="-1">6.4 示例：map 效果推断 <a class="header-anchor" href="#_6-4-示例-map-效果推断" aria-label="Permalink to “6.4 示例：map 效果推断”">​</a></h3><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>To map&lt;T, U, E&gt; with list: List of T, f: T -&gt; U with E, produce List of U with E:</span></span>
<span class="line"><span>  Match list:</span></span>
<span class="line"><span>    Case []: Return [].</span></span>
<span class="line"><span>    Case [head, ...tail]:</span></span>
<span class="line"><span>      Let result = f(head).</span></span>
<span class="line"><span>      Let rest = map(tail, f).</span></span>
<span class="line"><span>      Return [result, ...rest].</span></span>
<span class="line"><span></span></span>
<span class="line"><span>To get_user with id: Text, produce User. It performs io with Http:</span></span>
<span class="line"><span>  Return Http.get(&quot;/users/&quot; + id).</span></span>
<span class="line"><span></span></span>
<span class="line"><span>To get_all_users with ids: List of Text, produce List of User. It performs io with Http:</span></span>
<span class="line"><span>  Return map(ids, get_user).</span></span></code></pre></div><p>推断过程：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>eff(map) = E (效果参数)</span></span>
<span class="line"><span>eff(get_user) = IO[Http]</span></span>
<span class="line"><span></span></span>
<span class="line"><span>调用 map(ids, get_user):</span></span>
<span class="line"><span>E := eff(get_user) = IO[Http]</span></span>
<span class="line"><span></span></span>
<span class="line"><span>eff(get_all_users) ⊇ map[E=IO[Http]] = IO[Http]</span></span></code></pre></div><h2 id="_7-诊断生成" tabindex="-1">7. 诊断生成 <a class="header-anchor" href="#_7-诊断生成" aria-label="Permalink to “7. 诊断生成”">​</a></h2><h3 id="_7-1-诊断类型" tabindex="-1">7.1 诊断类型 <a class="header-anchor" href="#_7-1-诊断类型" aria-label="Permalink to “7.1 诊断类型”">​</a></h3><p>生成三类诊断：</p><ol><li><p><strong>效果缺失（Error）</strong>：推断效果 &gt; 声明效果</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>ERROR: Function &#39;fetch_user&#39; is declared as pure but performs IO[Http]</span></span></code></pre></div></li><li><p><strong>效果冗余（Warning）</strong>：声明效果 &gt; 推断效果</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>WARNING: Function &#39;helper&#39; is declared with IO but only performs pure computation</span></span></code></pre></div></li><li><p><strong>效果提示（Info）</strong>：未声明效果，自动推断</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>INFO: Function &#39;process&#39; inferred to have effect IO[Http, Sql]</span></span></code></pre></div></li></ol><h3 id="_7-2-诊断生成算法" tabindex="-1">7.2 诊断生成算法 <a class="header-anchor" href="#_7-2-诊断生成算法" aria-label="Permalink to “7.2 诊断生成算法”">​</a></h3><div class="language-text"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>function generateDiagnostics(</span></span>
<span class="line"><span>  module: Core.Module,</span></span>
<span class="line"><span>  inferredEnv: EffectEnv</span></span>
<span class="line"><span>): Diagnostic[] {</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  let diagnostics: Diagnostic[] = []</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  for each function f in module.decls {</span></span>
<span class="line"><span>    let declared = f.effects  // 显式声明的效果</span></span>
<span class="line"><span>    let inferred = inferredEnv[f.name]  // 推断的效果</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    if declared == null {</span></span>
<span class="line"><span>      // 未声明：提示推断结果</span></span>
<span class="line"><span>      diagnostics.add({</span></span>
<span class="line"><span>        severity: &#39;info&#39;,</span></span>
<span class="line"><span>        message: \`Function &#39;\${f.name}&#39; inferred to have effect \${formatEffect(inferred)}\`,</span></span>
<span class="line"><span>        location: f.span</span></span>
<span class="line"><span>      })</span></span>
<span class="line"><span>    }</span></span>
<span class="line"><span>    else if !isSubset(inferred, declared) {</span></span>
<span class="line"><span>      // 推断效果超出声明：错误</span></span>
<span class="line"><span>      let missing = difference(inferred, declared)</span></span>
<span class="line"><span>      diagnostics.add({</span></span>
<span class="line"><span>        severity: &#39;error&#39;,</span></span>
<span class="line"><span>        message: \`Function &#39;\${f.name}&#39; is declared with \${formatEffect(declared)} but performs \${formatEffect(inferred)}. Missing: \${formatEffect(missing)}\`,</span></span>
<span class="line"><span>        location: f.span,</span></span>
<span class="line"><span>        relatedInfo: findCausingCalls(f, missing)</span></span>
<span class="line"><span>      })</span></span>
<span class="line"><span>    }</span></span>
<span class="line"><span>    else if !isSubset(declared, inferred) {</span></span>
<span class="line"><span>      // 声明效果超出推断：警告</span></span>
<span class="line"><span>      let superfluous = difference(declared, inferred)</span></span>
<span class="line"><span>      diagnostics.add({</span></span>
<span class="line"><span>        severity: &#39;warning&#39;,</span></span>
<span class="line"><span>        message: \`Function &#39;\${f.name}&#39; is declared with \${formatEffect(declared)} but only performs \${formatEffect(inferred)}. Superfluous: \${formatEffect(superfluous)}\`,</span></span>
<span class="line"><span>        location: f.span</span></span>
<span class="line"><span>      })</span></span>
<span class="line"><span>    }</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  return diagnostics</span></span>
<span class="line"><span>}</span></span>
<span class="line"><span></span></span>
<span class="line"><span>function findCausingCalls(f: Func, missingEffect: EffectSet): RelatedInfo[] {</span></span>
<span class="line"><span>  // 追踪哪些调用点导致了缺失的效果</span></span>
<span class="line"><span>  let causing: RelatedInfo[] = []</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  for each call in findCallsInFunc(f) {</span></span>
<span class="line"><span>    let callEffect = inferCalleeEffect(call.target)</span></span>
<span class="line"><span>    if isSubset(missingEffect, callEffect) {</span></span>
<span class="line"><span>      causing.add({</span></span>
<span class="line"><span>        message: \`Call to &#39;\${call.target}&#39; requires \${formatEffect(callEffect)}\`,</span></span>
<span class="line"><span>        location: call.span</span></span>
<span class="line"><span>      })</span></span>
<span class="line"><span>    }</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  return causing</span></span>
<span class="line"><span>}</span></span></code></pre></div><h3 id="_7-3-诊断示例" tabindex="-1">7.3 诊断示例 <a class="header-anchor" href="#_7-3-诊断示例" aria-label="Permalink to “7.3 诊断示例”">​</a></h3><p><strong>示例 1：效果缺失</strong></p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>To fetch_data with id: Text, produce Data:  // 未声明效果</span></span>
<span class="line"><span>  Return Http.get(&quot;/data/&quot; + id).</span></span></code></pre></div><p>诊断：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>ERROR: Function &#39;fetch_data&#39; performs IO[Http] but no effect is declared.</span></span>
<span class="line"><span>  Hint: Add &#39;It performs io with Http&#39; to the function signature.</span></span>
<span class="line"><span>  Related:</span></span>
<span class="line"><span>    - Call to &#39;Http.get&#39; at line 2:10 requires IO[Http]</span></span></code></pre></div><p><strong>示例 2：效果冗余</strong></p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>To calculate with x: Int, produce Int. It performs io with Http:  // 声明了 IO 但未使用</span></span>
<span class="line"><span>  Return x * 2.</span></span></code></pre></div><p>诊断：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>WARNING: Function &#39;calculate&#39; is declared with IO[Http] but only performs pure computation.</span></span>
<span class="line"><span>  Hint: Remove the effect declaration or add IO operations.</span></span></code></pre></div><p><strong>示例 3：效果不足</strong></p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>To process with id: Text, produce Result. It performs io with Http:  // 声明了 Http 但还调用了 Sql</span></span>
<span class="line"><span>  Let user = Http.get(&quot;/users/&quot; + id).</span></span>
<span class="line"><span>  Let profile = Db.query(&quot;SELECT * FROM profiles WHERE user_id = ?&quot;, [id]).</span></span>
<span class="line"><span>  Return Ok(Profile(user, profile)).</span></span></code></pre></div><p>诊断：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>ERROR: Function &#39;process&#39; is declared with IO[Http] but performs IO[Http, Sql]. Missing: IO[Sql]</span></span>
<span class="line"><span>  Related:</span></span>
<span class="line"><span>    - Call to &#39;Db.query&#39; at line 3:17 requires IO[Sql]</span></span>
<span class="line"><span>  Hint: Change declaration to &#39;It performs io with Http and Sql&#39;</span></span></code></pre></div><h2 id="_8-复杂度分析" tabindex="-1">8. 复杂度分析 <a class="header-anchor" href="#_8-复杂度分析" aria-label="Permalink to “8. 复杂度分析”">​</a></h2><h3 id="_8-1-时间复杂度" tabindex="-1">8.1 时间复杂度 <a class="header-anchor" href="#_8-1-时间复杂度" aria-label="Permalink to “8.1 时间复杂度”">​</a></h3><p>设：</p><ul><li><code>n</code> = 函数数量</li><li><code>m</code> = 调用边数量（调用图中的边）</li><li><code>k</code> = 平均效果集大小（通常 k ≤ 10）</li></ul><p><strong>约束收集</strong>：O(m) 遍历每个调用点生成约束。</p><p><strong>约束求解</strong>：O(m × k × log k)</p><ul><li>最坏情况下需要 O(m) 轮迭代（调用图深度）</li><li>每轮迭代处理 O(m) 条约束</li><li>每次效果并集操作为 O(k log k)（假设使用有序集合）</li></ul><p><strong>诊断生成</strong>：O(n + m) 遍历所有函数和调用点。</p><p><strong>总体</strong>：O(m × k × log k) ≈ O(m log m)（当 k 为常数时）</p><h3 id="_8-2-空间复杂度" tabindex="-1">8.2 空间复杂度 <a class="header-anchor" href="#_8-2-空间复杂度" aria-label="Permalink to “8.2 空间复杂度”">​</a></h3><p><strong>效果环境</strong>：O(n × k) 存储每个函数的效果集。</p><p><strong>约束集合</strong>：O(m) 存储所有约束。</p><p><strong>总体</strong>：O(n × k + m) ≈ O(n + m)</p><h3 id="_8-3-优化策略" tabindex="-1">8.3 优化策略 <a class="header-anchor" href="#_8-3-优化策略" aria-label="Permalink to “8.3 优化策略”">​</a></h3><ol><li><strong>增量求解</strong>：仅重新计算受影响的函数</li><li><strong>强连通分量</strong>：先对调用图做 SCC 分解，自底向上求解</li><li><strong>效果缓存</strong>：缓存标准库函数的效果</li><li><strong>并行化</strong>：独立 SCC 可并行求解</li></ol><h2 id="_9-实现指南" tabindex="-1">9. 实现指南 <a class="header-anchor" href="#_9-实现指南" aria-label="Permalink to “9. 实现指南”">​</a></h2><h3 id="_9-1-集成点" tabindex="-1">9.1 集成点 <a class="header-anchor" href="#_9-1-集成点" aria-label="Permalink to “9.1 集成点”">​</a></h3><p>效果推断应集成到 <code>src/typecheck.ts</code> 中：</p><div class="language-typescript"><button title="Copy Code" class="copy"></button><span class="lang">typescript</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">// src/typecheck.ts (伪代码)</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">export</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;"> function</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> typecheckModule</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(</span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">module</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> Core</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">.</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">Module</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">)</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> Diagnostic</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">[] {</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  let</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> diagnostics</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> Diagnostic</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">[] </span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">=</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> []</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">  // 1. 类型检查（现有逻辑）</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  diagnostics.</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">push</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">...</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">typecheck</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(</span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">module</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">))</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">  // 2. 效果推断（新增）</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  let</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> effectEnv </span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">=</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> inferEffects</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(</span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">module</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  diagnostics.</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">push</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">...</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">checkEffects</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(</span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">module</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">, effectEnv))</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  return</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> diagnostics</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">}</span></span></code></pre></div><h3 id="_9-2-模块结构" tabindex="-1">9.2 模块结构 <a class="header-anchor" href="#_9-2-模块结构" aria-label="Permalink to “9.2 模块结构”">​</a></h3><p>建议创建新文件 <code>src/effect_inference.ts</code>：</p><div class="language-typescript"><button title="Copy Code" class="copy"></button><span class="lang">typescript</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">// src/effect_inference.ts</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">export</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;"> interface</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> EffectSet</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> {</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">  pure</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">?:</span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;"> boolean</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">  cpu</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">?:</span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;"> boolean</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">  io</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">?:</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> { </span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">caps</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> Capability</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">[] }</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">}</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">export</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;"> type</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> EffectEnv</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;"> =</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> Map</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">&lt;</span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">string</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">, </span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">EffectSet</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">&gt;</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">export</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;"> function</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> inferEffects</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(</span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">module</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> Core</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">.</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">Module</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">)</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> EffectEnv</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> {</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  let</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> constraints </span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">=</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> collectConstraints</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(</span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">module</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">)</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  return</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> solveConstraints</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(constraints)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">}</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">export</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;"> function</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> checkEffects</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">  module</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> Core</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">.</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">Module</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">,</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">  env</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> EffectEnv</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">)</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> TypecheckDiagnostic</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">[] {</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  return</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> generateDiagnostics</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(</span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">module</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">, env)</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">}</span></span></code></pre></div><h3 id="_9-3-测试策略" tabindex="-1">9.3 测试策略 <a class="header-anchor" href="#_9-3-测试策略" aria-label="Permalink to “9.3 测试策略”">​</a></h3><p>创建黄金测试用例覆盖：</p><ol><li><strong>基础推断</strong>：纯函数、CPU、单一 IO</li><li><strong>传递推断</strong>：调用链推断</li><li><strong>效果合并</strong>：多个 IO 能力的并集</li><li><strong>分支推断</strong>：if/match 分支的并集</li><li><strong>效果多态</strong>：泛型函数的效果实例化</li><li><strong>诊断测试</strong>：效果缺失、冗余、不足的错误消息</li></ol><p>测试文件位置：<code>test/cnl/examples/effect_infer_*.aster</code></p><h2 id="_10-未来扩展" tabindex="-1">10. 未来扩展 <a class="header-anchor" href="#_10-未来扩展" aria-label="Permalink to “10. 未来扩展”">​</a></h2><h3 id="_10-1-效果行-effect-rows" tabindex="-1">10.1 效果行（Effect Rows） <a class="header-anchor" href="#_10-1-效果行-effect-rows" aria-label="Permalink to “10.1 效果行（Effect Rows）”">​</a></h3><p>支持更精细的效果跟踪：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>type Effect = { http: Bool, sql: Bool, ... }</span></span></code></pre></div><p>使用行多态（row polymorphism）支持开放效果集。</p><h3 id="_10-2-条件效果" tabindex="-1">10.2 条件效果 <a class="header-anchor" href="#_10-2-条件效果" aria-label="Permalink to “10.2 条件效果”">​</a></h3><p>支持条件效果声明：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>To process with data: Data, produce Result with if data.needsNetwork then IO[Http] else ∅:</span></span>
<span class="line"><span>  ...</span></span></code></pre></div><h3 id="_10-3-效果掩码-effect-masking" tabindex="-1">10.3 效果掩码（Effect Masking） <a class="header-anchor" href="#_10-3-效果掩码-effect-masking" aria-label="Permalink to “10.3 效果掩码（Effect Masking）”">​</a></h3><p>支持局部屏蔽效果：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>To run_isolated with f: () -&gt; T with IO, produce T:</span></span>
<span class="line"><span>  Mask IO:</span></span>
<span class="line"><span>    Return f().  // 效果被隔离，不传播到 run_isolated</span></span></code></pre></div><h2 id="_11-参考文献" tabindex="-1">11. 参考文献 <a class="header-anchor" href="#_11-参考文献" aria-label="Permalink to “11. 参考文献”">​</a></h2><ol><li><p><strong>Effect Systems Revisited</strong> Lucassen, J.M. and Gifford, D.K. (1988) [ACM POPL 1988]</p></li><li><p><strong>Type and Effect Systems</strong> Nielson, F. and Nielson, H.R. (1999) <em>Correct System Design</em></p></li><li><p><strong>Koka: Programming with Row Polymorphic Effect Types</strong> Leijen, D. (2014) Microsoft Research Technical Report</p></li><li><p><strong>Algebraic Effects for Functional Programming</strong> Pretnar, M. (2015) <em>PhD Thesis, University of Edinburgh</em></p></li><li><p><strong>Frank: First-class effect handlers</strong> Lindley, S., McBride, C., and McLaughlin, C. (2017) [ACM POPL 2017]</p></li></ol><hr><p><strong>注</strong>：本文档描述的算法为设计阶段，实际实现可能根据工程需求进行调整。优先级应放在正确性和可维护性，性能优化可在稳定后迭代。</p>`,134)])])}const g=a(e,[["render",l]]);export{o as __pageData,g as default};
