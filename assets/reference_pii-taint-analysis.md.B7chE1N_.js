import{_ as n,c as a,o as i,aj as p}from"./chunks/framework.Bz2R-749.js";const k=JSON.parse('{"title":"PII 污点分析算法设计","description":"","frontmatter":{},"headers":[],"relativePath":"reference/pii-taint-analysis.md","filePath":"reference/pii-taint-analysis.md"}'),e={name:"reference/pii-taint-analysis.md"};function l(t,s,r,h,o,c){return i(),a("div",null,[...s[0]||(s[0]=[p(`<h1 id="pii-污点分析算法设计" tabindex="-1">PII 污点分析算法设计 <a class="header-anchor" href="#pii-污点分析算法设计" aria-label="Permalink to “PII 污点分析算法设计”">​</a></h1><blockquote><p><strong>状态</strong>: 设计文档 <strong>版本</strong>: 1.0 <strong>最后更新</strong>: 2025-10-06</p></blockquote><h2 id="_1-概述" tabindex="-1">1. 概述 <a class="header-anchor" href="#_1-概述" aria-label="Permalink to “1. 概述”">​</a></h2><h3 id="_1-1-动机" tabindex="-1">1.1 动机 <a class="header-anchor" href="#_1-1-动机" aria-label="Permalink to “1.1 动机”">​</a></h3><p>在现代应用中，个人身份信息（PII，Personally Identifiable Information）的安全处理至关重要。GDPR、CCPA 等隐私法规要求开发者：</p><ol><li><strong>最小化收集</strong>：仅收集必要的 PII</li><li><strong>安全传输</strong>：加密传输敏感数据</li><li><strong>访问控制</strong>：限制对 PII 的访问</li><li><strong>数据脱敏</strong>：日志和分析中移除 PII</li><li><strong>泄露通知</strong>：检测并报告 PII 泄露</li></ol><p>手动审计代码以确保 PII 安全非常困难且易错。污点分析（Taint Analysis）通过自动跟踪 PII 数据流，检测潜在的安全风险。</p><h3 id="_1-2-目标" tabindex="-1">1.2 目标 <a class="header-anchor" href="#_1-2-目标" aria-label="Permalink to “1.2 目标”">​</a></h3><p>设计一个 PII 污点分析算法，满足以下需求：</p><ul><li><strong>自动化</strong>：自动识别 PII 数据源和不安全使用</li><li><strong>保守性</strong>：避免漏报（false negatives），宁可误报（false positives）</li><li><strong>细粒度</strong>：区分不同敏感级别（L1/L2/L3）和数据类别</li><li><strong>可操作</strong>：提供清晰的修复建议</li><li><strong>增量性</strong>：支持 LSP 实时分析</li></ul><h3 id="_1-3-威胁模型" tabindex="-1">1.3 威胁模型 <a class="header-anchor" href="#_1-3-威胁模型" aria-label="Permalink to “1.3 威胁模型”">​</a></h3><p>检测以下 PII 安全风险：</p><table tabindex="0"><thead><tr><th>威胁类型</th><th>描述</th><th>示例</th></tr></thead><tbody><tr><td><strong>未加密传输</strong></td><td>PII 通过明文 HTTP 传输</td><td><code>Http.post(&quot;/api&quot;, email)</code></td></tr><tr><td><strong>日志泄露</strong></td><td>PII 写入日志文件</td><td><code>Log.info(&quot;User: &quot; + ssn)</code></td></tr><tr><td><strong>未授权访问</strong></td><td>PII 未经认证即可访问</td><td>缺少 <code>@auth</code> 标注的 API</td></tr><tr><td><strong>未脱敏存储</strong></td><td>高敏感 PII 直接存储</td><td><code>Db.insert(&quot;users&quot;, { ssn })</code></td></tr><tr><td><strong>跨境传输</strong></td><td>PII 发送到未批准的地区</td><td><code>Http.post(&quot;https://foreign.api&quot;, data)</code></td></tr></tbody></table><p>本文档重点关注<strong>未加密传输</strong>和<strong>日志泄露</strong>的检测，其他威胁可在后续扩展。</p><h2 id="_2-pii-类型系统回顾" tabindex="-1">2. PII 类型系统回顾 <a class="header-anchor" href="#_2-pii-类型系统回顾" aria-label="Permalink to “2. PII 类型系统回顾”">​</a></h2><p>Aster 的 PII 类型系统（见 Task 5 实现）提供了静态标注：</p><h3 id="_2-1-类型标注语法" tabindex="-1">2.1 类型标注语法 <a class="header-anchor" href="#_2-1-类型标注语法" aria-label="Permalink to “2.1 类型标注语法”">​</a></h3><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>@pii(sensitivity, category) BaseType</span></span></code></pre></div><p><strong>敏感级别</strong>：</p><ul><li><code>L1</code>：低敏感（如姓名、用户名）</li><li><code>L2</code>：中敏感（如邮箱、电话）</li><li><code>L3</code>：高敏感（如 SSN、信用卡号、健康记录）</li></ul><p><strong>数据类别</strong>：</p><ul><li><code>email</code>：电子邮件地址</li><li><code>phone</code>：电话号码</li><li><code>ssn</code>：社会保障号</li><li><code>address</code>：物理地址</li><li><code>financial</code>：金融信息</li><li><code>health</code>：健康记录</li><li><code>name</code>：姓名</li><li><code>biometric</code>：生物特征</li></ul><h3 id="_2-2-类型标注示例" tabindex="-1">2.2 类型标注示例 <a class="header-anchor" href="#_2-2-类型标注示例" aria-label="Permalink to “2.2 类型标注示例”">​</a></h3><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>Define User with</span></span>
<span class="line"><span>  email: @pii(L2, email) Text,</span></span>
<span class="line"><span>  ssn: @pii(L3, ssn) Text,</span></span>
<span class="line"><span>  name: @pii(L1, name) Text.</span></span>
<span class="line"><span></span></span>
<span class="line"><span>To get_user with id: Text, produce User. It performs io with Sql:</span></span>
<span class="line"><span>  Return Db.query(&quot;SELECT * FROM users WHERE id = ?&quot;, [id]).</span></span></code></pre></div><h3 id="_2-3-类型系统的局限" tabindex="-1">2.3 类型系统的局限 <a class="header-anchor" href="#_2-3-类型系统的局限" aria-label="Permalink to “2.3 类型系统的局限”">​</a></h3><p>静态类型标注只能识别<strong>显式声明</strong>的 PII，无法处理：</p><ol><li><strong>动态污点</strong>：从 IO 输入读取的未标注数据</li><li><strong>间接污点</strong>：通过计算派生的 PII（如用户 ID → 邮箱）</li><li><strong>容器污点</strong>：包含 PII 的集合类型</li></ol><p>污点分析通过数据流追踪弥补这些局限。</p><h2 id="_3-污点分析基础" tabindex="-1">3. 污点分析基础 <a class="header-anchor" href="#_3-污点分析基础" aria-label="Permalink to “3. 污点分析基础”">​</a></h2><h3 id="_3-1-核心概念" tabindex="-1">3.1 核心概念 <a class="header-anchor" href="#_3-1-核心概念" aria-label="Permalink to “3.1 核心概念”">​</a></h3><p><strong>污点（Taint）</strong>：标记数据是否包含 PII 及其属性（敏感级别、类别）。</p><div class="language-typescript"><button title="Copy Code" class="copy"></button><span class="lang">typescript</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">type</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> Taint</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;"> =</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  |</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> { </span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">kind</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;"> &#39;Clean&#39;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }  </span><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">// 无 PII</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  |</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> { </span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">kind</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;"> &#39;Tainted&#39;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">; </span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">sensitivity</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;"> &#39;L1&#39;</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;"> |</span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;"> &#39;L2&#39;</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;"> |</span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;"> &#39;L3&#39;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">; </span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">category</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> PiiCategory</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> }</span></span></code></pre></div><p><strong>污点源（Source）</strong>：PII 数据的来源</p><ul><li>类型标注为 <code>@pii</code> 的变量</li><li>IO 输入（<code>Http.request.body</code>, <code>Sql.query</code> 结果）</li></ul><p><strong>污点传播（Propagation）</strong>：污点如何在数据流中传递</p><ul><li>赋值、函数调用、容器操作、字符串拼接</li></ul><p><strong>敏感操作（Sink）</strong>：可能导致 PII 泄露的操作</p><ul><li><code>Http.post</code>, <code>Http.get</code>（未加密）</li><li><code>Log.info</code>, <code>Log.debug</code>（日志记录）</li><li><code>Db.insert</code>, <code>Db.update</code>（未脱敏存储）</li></ul><h3 id="_3-2-分析流程" tabindex="-1">3.2 分析流程 <a class="header-anchor" href="#_3-2-分析流程" aria-label="Permalink to “3.2 分析流程”">​</a></h3><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>┌──────────────┐</span></span>
<span class="line"><span>│ 1. 污点源识别 │  标记所有 PII 数据</span></span>
<span class="line"><span>└──────┬───────┘</span></span>
<span class="line"><span>       │</span></span>
<span class="line"><span>       v</span></span>
<span class="line"><span>┌──────────────┐</span></span>
<span class="line"><span>│ 2. 污点传播   │  跟踪数据流图</span></span>
<span class="line"><span>└──────┬───────┘</span></span>
<span class="line"><span>       │</span></span>
<span class="line"><span>       v</span></span>
<span class="line"><span>┌──────────────┐</span></span>
<span class="line"><span>│ 3. Sink 检测  │  识别敏感操作</span></span>
<span class="line"><span>└──────┬───────┘</span></span>
<span class="line"><span>       │</span></span>
<span class="line"><span>       v</span></span>
<span class="line"><span>┌──────────────┐</span></span>
<span class="line"><span>│ 4. 诊断生成   │  报告安全风险</span></span>
<span class="line"><span>└──────────────┘</span></span></code></pre></div><h2 id="_4-污点标记与追踪" tabindex="-1">4. 污点标记与追踪 <a class="header-anchor" href="#_4-污点标记与追踪" aria-label="Permalink to “4. 污点标记与追踪”">​</a></h2><h3 id="_4-1-污点环境" tabindex="-1">4.1 污点环境 <a class="header-anchor" href="#_4-1-污点环境" aria-label="Permalink to “4.1 污点环境”">​</a></h3><p>污点环境记录每个变量的污点状态：</p><div class="language-typescript"><button title="Copy Code" class="copy"></button><span class="lang">typescript</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">type</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> TaintEnv</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;"> =</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> Map</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">&lt;</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">VariableName</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">, </span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">TaintSet</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">&gt;</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">type</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> TaintSet</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;"> =</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> {</span></span>
<span class="line"><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">  taints</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> Taint</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">[]  </span><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">// 可能包含多个 PII 类别</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">}</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">function</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> mergeTaints</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(</span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">a</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> TaintSet</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">, </span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">b</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> TaintSet</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">)</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> TaintSet</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> {</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  return</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> { taints: [</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">...</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">a.taints, </span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">...</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">b.taints] }</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">}</span></span></code></pre></div><h3 id="_4-2-污点源识别算法" tabindex="-1">4.2 污点源识别算法 <a class="header-anchor" href="#_4-2-污点源识别算法" aria-label="Permalink to “4.2 污点源识别算法”">​</a></h3><div class="language-text"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>function identifySources(module: Core.Module): TaintEnv {</span></span>
<span class="line"><span>  let env: TaintEnv = {}</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  for each function f in module.decls {</span></span>
<span class="line"><span>    // 1. 识别参数中的 PII 类型标注</span></span>
<span class="line"><span>    for each param in f.params {</span></span>
<span class="line"><span>      if param.type is @pii(sensitivity, category) {</span></span>
<span class="line"><span>        env[param.name] = { taints: [{ kind: &#39;Tainted&#39;, sensitivity, category }] }</span></span>
<span class="line"><span>      }</span></span>
<span class="line"><span>    }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    // 2. 识别 Data 字段中的 PII</span></span>
<span class="line"><span>    for each field in f.body.bindings {</span></span>
<span class="line"><span>      if field.type is @pii(sensitivity, category) {</span></span>
<span class="line"><span>        env[field.name] = { taints: [{ kind: &#39;Tainted&#39;, sensitivity, category }] }</span></span>
<span class="line"><span>      }</span></span>
<span class="line"><span>    }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    // 3. 识别 IO 输入（保守假设）</span></span>
<span class="line"><span>    for each call in findCalls(f.body) {</span></span>
<span class="line"><span>      if call.target matches IO_INPUT_PATTERNS {</span></span>
<span class="line"><span>        // Http.request.body, Sql.query 等返回值假设为污点</span></span>
<span class="line"><span>        env[call.resultVar] = { taints: [{ kind: &#39;Tainted&#39;, sensitivity: &#39;L2&#39;, category: &#39;unknown&#39; }] }</span></span>
<span class="line"><span>      }</span></span>
<span class="line"><span>    }</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  return env</span></span>
<span class="line"><span>}</span></span>
<span class="line"><span></span></span>
<span class="line"><span>const IO_INPUT_PATTERNS = [</span></span>
<span class="line"><span>  &#39;Http.request.body&#39;,</span></span>
<span class="line"><span>  &#39;Http.request.query&#39;,</span></span>
<span class="line"><span>  &#39;Sql.query&#39;,</span></span>
<span class="line"><span>  &#39;Db.find&#39;,</span></span>
<span class="line"><span>  &#39;Files.read&#39;</span></span>
<span class="line"><span>]</span></span></code></pre></div><h3 id="_4-3-示例-污点源识别" tabindex="-1">4.3 示例：污点源识别 <a class="header-anchor" href="#_4-3-示例-污点源识别" aria-label="Permalink to “4.3 示例：污点源识别”">​</a></h3><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>Define User with</span></span>
<span class="line"><span>  email: @pii(L2, email) Text,</span></span>
<span class="line"><span>  ssn: @pii(L3, ssn) Text.</span></span>
<span class="line"><span></span></span>
<span class="line"><span>To get_user with id: Text, produce User. It performs io with Sql:</span></span>
<span class="line"><span>  Let user = Db.query(&quot;SELECT * FROM users WHERE id = ?&quot;, [id]).</span></span>
<span class="line"><span>  Return user.</span></span></code></pre></div><p>污点环境：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>env = {</span></span>
<span class="line"><span>  &#39;user&#39;: { taints: [{ kind: &#39;Tainted&#39;, sensitivity: &#39;L2&#39;, category: &#39;unknown&#39; }] }</span></span>
<span class="line"><span>  // Db.query 返回值保守标记为污点</span></span>
<span class="line"><span>}</span></span></code></pre></div><h2 id="_5-污点传播规则" tabindex="-1">5. 污点传播规则 <a class="header-anchor" href="#_5-污点传播规则" aria-label="Permalink to “5. 污点传播规则”">​</a></h2><h3 id="_5-1-基本传播规则" tabindex="-1">5.1 基本传播规则 <a class="header-anchor" href="#_5-1-基本传播规则" aria-label="Permalink to “5.1 基本传播规则”">​</a></h3><div class="language-text"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>function propagateTaint(expr: Core.Expr, env: TaintEnv): TaintSet {</span></span>
<span class="line"><span>  switch expr.kind {</span></span>
<span class="line"><span>    case &#39;Name&#39;:</span></span>
<span class="line"><span>      // 变量引用：返回变量的污点</span></span>
<span class="line"><span>      return env[expr.name] || { taints: [] }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    case &#39;String&#39;, &#39;Int&#39;, &#39;Double&#39;, &#39;Bool&#39;:</span></span>
<span class="line"><span>      // 字面量：无污点</span></span>
<span class="line"><span>      return { taints: [] }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    case &#39;Call&#39;:</span></span>
<span class="line"><span>      // 函数调用：保守假设返回值继承所有参数的污点</span></span>
<span class="line"><span>      let argTaints = expr.args.map(arg =&gt; propagateTaint(arg, env))</span></span>
<span class="line"><span>      return unionAll(argTaints)</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    case &#39;Construct&#39;:</span></span>
<span class="line"><span>      // 构造数据类型：继承字段污点</span></span>
<span class="line"><span>      let fieldTaints = expr.fields.map(field =&gt; propagateTaint(field.expr, env))</span></span>
<span class="line"><span>      return unionAll(fieldTaints)</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    case &#39;Field&#39;:</span></span>
<span class="line"><span>      // 字段访问：obj.field 继承 obj 的污点</span></span>
<span class="line"><span>      let objTaint = propagateTaint(expr.obj, env)</span></span>
<span class="line"><span></span></span>
<span class="line"><span>      // 如果字段本身有 @pii 标注，合并两者</span></span>
<span class="line"><span>      let fieldType = lookupFieldType(expr.obj, expr.name)</span></span>
<span class="line"><span>      if fieldType is @pii(sensitivity, category) {</span></span>
<span class="line"><span>        return merge(objTaint, { taints: [{ kind: &#39;Tainted&#39;, sensitivity, category }] })</span></span>
<span class="line"><span>      }</span></span>
<span class="line"><span>      return objTaint</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    case &#39;BinOp&#39;:</span></span>
<span class="line"><span>      // 二元操作（+, -, *, /, ==, etc.）</span></span>
<span class="line"><span>      if expr.op == &#39;+&#39; and (isString(expr.left) or isString(expr.right)) {</span></span>
<span class="line"><span>        // 字符串拼接：继承两边污点</span></span>
<span class="line"><span>        return union(propagateTaint(expr.left, env), propagateTaint(expr.right, env))</span></span>
<span class="line"><span>      }</span></span>
<span class="line"><span>      else {</span></span>
<span class="line"><span>        // 数值/布尔运算：保守假设继承污点</span></span>
<span class="line"><span>        return union(propagateTaint(expr.left, env), propagateTaint(expr.right, env))</span></span>
<span class="line"><span>      }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    case &#39;If&#39;:</span></span>
<span class="line"><span>      // if 表达式：返回两个分支污点的并集</span></span>
<span class="line"><span>      let thenTaint = propagateTaint(expr.then, env)</span></span>
<span class="line"><span>      let elseTaint = propagateTaint(expr.else, env)</span></span>
<span class="line"><span>      return union(thenTaint, elseTaint)</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    case &#39;Match&#39;:</span></span>
<span class="line"><span>      // match 表达式：返回所有分支污点的并集</span></span>
<span class="line"><span>      let caseTaints = expr.cases.map(c =&gt; propagateTaint(c.body, env))</span></span>
<span class="line"><span>      return unionAll(caseTaints)</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    case &#39;List&#39;:</span></span>
<span class="line"><span>      // List 构造：继承所有元素污点</span></span>
<span class="line"><span>      let elemTaints = expr.elems.map(e =&gt; propagateTaint(e, env))</span></span>
<span class="line"><span>      return unionAll(elemTaints)</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    case &#39;Map&#39;:</span></span>
<span class="line"><span>      // Map 构造：继承所有 key 和 value 的污点</span></span>
<span class="line"><span>      let kvTaints = expr.entries.flatMap(e =&gt; [</span></span>
<span class="line"><span>        propagateTaint(e.key, env),</span></span>
<span class="line"><span>        propagateTaint(e.value, env)</span></span>
<span class="line"><span>      ])</span></span>
<span class="line"><span>      return unionAll(kvTaints)</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    default:</span></span>
<span class="line"><span>      // 未知表达式：保守假设为污点（误报优于漏报）</span></span>
<span class="line"><span>      return { taints: [{ kind: &#39;Tainted&#39;, sensitivity: &#39;L3&#39;, category: &#39;unknown&#39; }] }</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span>}</span></span>
<span class="line"><span></span></span>
<span class="line"><span>function unionAll(taintSets: TaintSet[]): TaintSet {</span></span>
<span class="line"><span>  let allTaints = taintSets.flatMap(ts =&gt; ts.taints)</span></span>
<span class="line"><span>  return { taints: allTaints }</span></span>
<span class="line"><span>}</span></span>
<span class="line"><span></span></span>
<span class="line"><span>function union(a: TaintSet, b: TaintSet): TaintSet {</span></span>
<span class="line"><span>  return { taints: [...a.taints, ...b.taints] }</span></span>
<span class="line"><span>}</span></span></code></pre></div><h3 id="_5-2-语句级污点传播" tabindex="-1">5.2 语句级污点传播 <a class="header-anchor" href="#_5-2-语句级污点传播" aria-label="Permalink to “5.2 语句级污点传播”">​</a></h3><div class="language-text"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>function propagateStmt(stmt: Core.Statement, env: TaintEnv): TaintEnv {</span></span>
<span class="line"><span>  switch stmt.kind {</span></span>
<span class="line"><span>    case &#39;Let&#39;:</span></span>
<span class="line"><span>      // Let x = expr: 更新 env[x] 为 expr 的污点</span></span>
<span class="line"><span>      let exprTaint = propagateTaint(stmt.expr, env)</span></span>
<span class="line"><span>      env[stmt.name] = exprTaint</span></span>
<span class="line"><span>      return env</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    case &#39;Return&#39;:</span></span>
<span class="line"><span>      // Return 不更新环境</span></span>
<span class="line"><span>      return env</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    default:</span></span>
<span class="line"><span>      return env</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span>}</span></span>
<span class="line"><span></span></span>
<span class="line"><span>function propagateFunc(f: Core.Func): TaintEnv {</span></span>
<span class="line"><span>  let env = identifySourcesInFunc(f)</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  for each stmt in f.body.statements {</span></span>
<span class="line"><span>    env = propagateStmt(stmt, env)</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  return env</span></span>
<span class="line"><span>}</span></span></code></pre></div><h3 id="_5-3-示例-污点传播" tabindex="-1">5.3 示例：污点传播 <a class="header-anchor" href="#_5-3-示例-污点传播" aria-label="Permalink to “5.3 示例：污点传播”">​</a></h3><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>Define User with</span></span>
<span class="line"><span>  email: @pii(L2, email) Text,</span></span>
<span class="line"><span>  name: @pii(L1, name) Text.</span></span>
<span class="line"><span></span></span>
<span class="line"><span>To format_user with user: User, produce Text:</span></span>
<span class="line"><span>  Let greeting = &quot;Hello, &quot; + user.name.</span></span>
<span class="line"><span>  Let contact = &quot;Email: &quot; + user.email.</span></span>
<span class="line"><span>  Return greeting + &quot;\\n&quot; + contact.</span></span></code></pre></div><p>污点传播：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>初始环境：</span></span>
<span class="line"><span>env = { &#39;user&#39;: { taints: [] } }  // 参数 user 本身未污染</span></span>
<span class="line"><span></span></span>
<span class="line"><span>user.name 访问：</span></span>
<span class="line"><span>  user.name 的类型为 @pii(L1, name) Text</span></span>
<span class="line"><span>  taint(user.name) = { taints: [{ kind: &#39;Tainted&#39;, sensitivity: &#39;L1&#39;, category: &#39;name&#39; }] }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>&quot;Hello, &quot; + user.name：</span></span>
<span class="line"><span>  taint(&quot;Hello, &quot;) = { taints: [] }</span></span>
<span class="line"><span>  union(∅, L1:name) = { taints: [L1:name] }</span></span>
<span class="line"><span>  env[&#39;greeting&#39;] = { taints: [L1:name] }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>user.email 访问：</span></span>
<span class="line"><span>  taint(user.email) = { taints: [L2:email] }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>&quot;Email: &quot; + user.email：</span></span>
<span class="line"><span>  env[&#39;contact&#39;] = { taints: [L2:email] }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>greeting + &quot;\\n&quot; + contact：</span></span>
<span class="line"><span>  taint(result) = union(L1:name, L2:email) = { taints: [L1:name, L2:email] }</span></span></code></pre></div><h2 id="_6-敏感操作检测" tabindex="-1">6. 敏感操作检测 <a class="header-anchor" href="#_6-敏感操作检测" aria-label="Permalink to “6. 敏感操作检测”">​</a></h2><h3 id="_6-1-sink-分类" tabindex="-1">6.1 Sink 分类 <a class="header-anchor" href="#_6-1-sink-分类" aria-label="Permalink to “6.1 Sink 分类”">​</a></h3><p>定义三类 Sink 及其安全要求：</p><table tabindex="0"><thead><tr><th>Sink 类型</th><th>安全要求</th><th>允许的 PII 级别</th></tr></thead><tbody><tr><td><strong>加密传输</strong></td><td>HTTPS, TLS 1.2+</td><td>L1, L2, L3</td></tr><tr><td><strong>未加密传输</strong></td><td>HTTP</td><td>禁止所有 PII</td></tr><tr><td><strong>日志记录</strong></td><td>生产环境日志</td><td>禁止 L2, L3</td></tr><tr><td><strong>本地存储</strong></td><td>文件系统</td><td>需要加密</td></tr><tr><td><strong>数据库存储</strong></td><td>SQL/NoSQL</td><td>L3 需要加密/hash</td></tr></tbody></table><h3 id="_6-2-sink-检测算法" tabindex="-1">6.2 Sink 检测算法 <a class="header-anchor" href="#_6-2-sink-检测算法" aria-label="Permalink to “6.2 Sink 检测算法”">​</a></h3><div class="language-text"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>function detectSinks(module: Core.Module, env: TaintEnv): SinkViolation[] {</span></span>
<span class="line"><span>  let violations: SinkViolation[] = []</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  for each function f in module.decls {</span></span>
<span class="line"><span>    for each call in findCalls(f.body) {</span></span>
<span class="line"><span>      let callTaint = propagateTaint(call, env)</span></span>
<span class="line"><span></span></span>
<span class="line"><span>      // 检测未加密 HTTP 传输</span></span>
<span class="line"><span>      if matchesPattern(call.target, HTTP_UNENCRYPTED_PATTERNS) {</span></span>
<span class="line"><span>        if hasTaint(callTaint) {</span></span>
<span class="line"><span>          violations.add({</span></span>
<span class="line"><span>            kind: &#39;UnencryptedTransmission&#39;,</span></span>
<span class="line"><span>            location: call.span,</span></span>
<span class="line"><span>            taint: callTaint,</span></span>
<span class="line"><span>            message: generateHttpWarning(callTaint)</span></span>
<span class="line"><span>          })</span></span>
<span class="line"><span>        }</span></span>
<span class="line"><span>      }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>      // 检测日志记录</span></span>
<span class="line"><span>      if matchesPattern(call.target, LOG_PATTERNS) {</span></span>
<span class="line"><span>        let highSensitivity = callTaint.taints.filter(t =&gt; t.sensitivity == &#39;L2&#39; || t.sensitivity == &#39;L3&#39;)</span></span>
<span class="line"><span>        if highSensitivity.length &gt; 0 {</span></span>
<span class="line"><span>          violations.add({</span></span>
<span class="line"><span>            kind: &#39;PiiInLogs&#39;,</span></span>
<span class="line"><span>            location: call.span,</span></span>
<span class="line"><span>            taint: { taints: highSensitivity },</span></span>
<span class="line"><span>            message: generateLogWarning(highSensitivity)</span></span>
<span class="line"><span>          })</span></span>
<span class="line"><span>        }</span></span>
<span class="line"><span>      }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>      // 检测未脱敏数据库存储</span></span>
<span class="line"><span>      if matchesPattern(call.target, DB_WRITE_PATTERNS) {</span></span>
<span class="line"><span>        let l3Taints = callTaint.taints.filter(t =&gt; t.sensitivity == &#39;L3&#39;)</span></span>
<span class="line"><span>        if l3Taints.length &gt; 0 {</span></span>
<span class="line"><span>          violations.add({</span></span>
<span class="line"><span>            kind: &#39;UnsanitizedStorage&#39;,</span></span>
<span class="line"><span>            location: call.span,</span></span>
<span class="line"><span>            taint: { taints: l3Taints },</span></span>
<span class="line"><span>            message: generateDbWarning(l3Taints)</span></span>
<span class="line"><span>          })</span></span>
<span class="line"><span>        }</span></span>
<span class="line"><span>      }</span></span>
<span class="line"><span>    }</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  return violations</span></span>
<span class="line"><span>}</span></span>
<span class="line"><span></span></span>
<span class="line"><span>const HTTP_UNENCRYPTED_PATTERNS = [</span></span>
<span class="line"><span>  &#39;Http.post&#39;,</span></span>
<span class="line"><span>  &#39;Http.put&#39;,</span></span>
<span class="line"><span>  &#39;Http.patch&#39;,</span></span>
<span class="line"><span>  &#39;Http.get&#39;</span><span>  // 如果 URL 是 http:// 而非 https://</span></span>
<span class="line"><span>]</span></span>
<span class="line"><span></span></span>
<span class="line"><span>const LOG_PATTERNS = [</span></span>
<span class="line"><span>  &#39;Log.info&#39;,</span></span>
<span class="line"><span>  &#39;Log.debug&#39;,</span></span>
<span class="line"><span>  &#39;Log.warn&#39;,</span></span>
<span class="line"><span>  &#39;Log.error&#39;,</span></span>
<span class="line"><span>  &#39;Console.log&#39;</span></span>
<span class="line"><span>]</span></span>
<span class="line"><span></span></span>
<span class="line"><span>const DB_WRITE_PATTERNS = [</span></span>
<span class="line"><span>  &#39;Db.insert&#39;,</span></span>
<span class="line"><span>  &#39;Db.update&#39;,</span></span>
<span class="line"><span>  &#39;Sql.execute&#39;</span></span>
<span class="line"><span>]</span></span></code></pre></div><h3 id="_6-3-精细化-sink-检测" tabindex="-1">6.3 精细化 Sink 检测 <a class="header-anchor" href="#_6-3-精细化-sink-检测" aria-label="Permalink to “6.3 精细化 Sink 检测”">​</a></h3><p>对于 HTTP 调用，检查 URL 是否使用 HTTPS：</p><div class="language-text"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>function isSecureHttp(call: Core.Call): boolean {</span></span>
<span class="line"><span>  // 检查第一个参数（URL）是否以 https:// 开头</span></span>
<span class="line"><span>  if call.args.length &gt; 0 {</span></span>
<span class="line"><span>    let urlArg = call.args[0]</span></span>
<span class="line"><span>    if urlArg.kind == &#39;String&#39; {</span></span>
<span class="line"><span>      return urlArg.value.startsWith(&#39;https://&#39;)</span></span>
<span class="line"><span>    }</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  // 无法静态确定：保守假设不安全</span></span>
<span class="line"><span>  return false</span></span>
<span class="line"><span>}</span></span>
<span class="line"><span></span></span>
<span class="line"><span>function detectHttpSink(call: Core.Call, taint: TaintSet): SinkViolation? {</span></span>
<span class="line"><span>  if call.target.name == &#39;Http.post&#39; or call.target.name == &#39;Http.put&#39; {</span></span>
<span class="line"><span>    if !isSecureHttp(call) and hasTaint(taint) {</span></span>
<span class="line"><span>      return {</span></span>
<span class="line"><span>        kind: &#39;UnencryptedTransmission&#39;,</span></span>
<span class="line"><span>        location: call.span,</span></span>
<span class="line"><span>        taint: taint,</span></span>
<span class="line"><span>        message: \`PII data transmitted over unencrypted HTTP. Use HTTPS instead.\`</span></span>
<span class="line"><span>      }</span></span>
<span class="line"><span>    }</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span>  return null</span></span>
<span class="line"><span>}</span></span></code></pre></div><h2 id="_7-保守分析策略" tabindex="-1">7. 保守分析策略 <a class="header-anchor" href="#_7-保守分析策略" aria-label="Permalink to “7. 保守分析策略”">​</a></h2><h3 id="_7-1-精度与召回率权衡" tabindex="-1">7.1 精度与召回率权衡 <a class="header-anchor" href="#_7-1-精度与召回率权衡" aria-label="Permalink to “7.1 精度与召回率权衡”">​</a></h3><p>污点分析的两个指标：</p><ul><li><strong>精度（Precision）</strong>：报告的问题中有多少是真实问题（1 - 误报率）</li><li><strong>召回率（Recall）</strong>：真实问题中有多少被检测到（1 - 漏报率）</li></ul><p>安全分析优先召回率（避免漏报），接受一定误报：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>精度  召回率  策略</span></span>
<span class="line"><span>────────────────────</span></span>
<span class="line"><span>90%    70%   过于宽松，可能漏报高风险</span></span>
<span class="line"><span>70%    95%   理想平衡（目标）</span></span>
<span class="line"><span>50%    99%   过于保守，误报过多</span></span></code></pre></div><h3 id="_7-2-保守假设" tabindex="-1">7.2 保守假设 <a class="header-anchor" href="#_7-2-保守假设" aria-label="Permalink to “7.2 保守假设”">​</a></h3><p><strong>函数调用</strong>：</p><div class="language-text"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>// 保守假设：返回值继承所有参数污点</span></span>
<span class="line"><span>taint(f(x, y, z)) = union(taint(x), taint(y), taint(z))</span></span>
<span class="line"><span></span></span>
<span class="line"><span>// 实际上：f 可能只使用 x，但静态分析无法确定</span></span></code></pre></div><p><strong>容器操作</strong>：</p><div class="language-text"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>// 保守假设：整个容器被污染</span></span>
<span class="line"><span>taint([x, y, z]) = union(taint(x), taint(y), taint(z))</span></span>
<span class="line"><span></span></span>
<span class="line"><span>// 实际上：访问 list[0] 只应返回 taint(x)，但需要索引分析</span></span></code></pre></div><p><strong>控制流</strong>：</p><div class="language-text"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>// 保守假设：所有分支的并集</span></span>
<span class="line"><span>taint(if cond then x else y) = union(taint(x), taint(y))</span></span>
<span class="line"><span></span></span>
<span class="line"><span>// 实际上：只有执行的分支会污染结果，但需要符号执行</span></span></code></pre></div><h3 id="_7-3-误报控制策略" tabindex="-1">7.3 误报控制策略 <a class="header-anchor" href="#_7-3-误报控制策略" aria-label="Permalink to “7.3 误报控制策略”">​</a></h3><p><strong>白名单（Allowlist）</strong>：</p><div class="language-text"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>// 允许特定函数处理 PII（如加密、脱敏）</span></span>
<span class="line"><span>const SANITIZER_FUNCTIONS = [</span></span>
<span class="line"><span>  &#39;Crypto.hash&#39;,</span></span>
<span class="line"><span>  &#39;Crypto.encrypt&#39;,</span></span>
<span class="line"><span>  &#39;Pii.mask&#39;,</span></span>
<span class="line"><span>  &#39;Pii.anonymize&#39;</span></span>
<span class="line"><span>]</span></span>
<span class="line"><span></span></span>
<span class="line"><span>function propagateTaint(call: Core.Call, env: TaintEnv): TaintSet {</span></span>
<span class="line"><span>  if call.target.name in SANITIZER_FUNCTIONS {</span></span>
<span class="line"><span>    // 净化函数：移除污点</span></span>
<span class="line"><span>    return { taints: [] }</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span>  // 正常传播</span></span>
<span class="line"><span>  return propagateCall(call, env)</span></span>
<span class="line"><span>}</span></span></code></pre></div><p><strong>敏感级别过滤</strong>：</p><div class="language-text"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>// 只报告 L2/L3 的问题，忽略 L1</span></span>
<span class="line"><span>function shouldReport(taint: Taint): boolean {</span></span>
<span class="line"><span>  return taint.sensitivity == &#39;L2&#39; || taint.sensitivity == &#39;L3&#39;</span></span>
<span class="line"><span>}</span></span></code></pre></div><p><strong>用户标注</strong>：</p><div class="language-text"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>// 允许开发者标注安全审计过的代码</span></span>
<span class="line"><span>// @safe-pii: 该函数已通过安全审计</span></span>
<span class="line"><span>To send_verified with data: @pii(L2, email) Text, produce Result. @safe-pii:</span></span>
<span class="line"><span>  Return Http.post(&quot;https://verified-api.com/send&quot;, data).</span></span></code></pre></div><h3 id="_7-4-误报分析示例" tabindex="-1">7.4 误报分析示例 <a class="header-anchor" href="#_7-4-误报分析示例" aria-label="Permalink to “7.4 误报分析示例”">​</a></h3><p><strong>误报场景</strong>：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>To process with user: User, produce Text:</span></span>
<span class="line"><span>  Let name_length = Text.length(user.name).  // user.name 是 @pii(L1, name)</span></span>
<span class="line"><span>  Return &quot;Name length: &quot; + name_length.</span></span></code></pre></div><p>污点分析报告：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>WARNING: PII data &#39;name&#39; (L1) used in string concatenation</span></span></code></pre></div><p><strong>实际情况</strong>： <code>name_length</code> 是一个数字，不包含 PII 信息。这是误报。</p><p><strong>改进方案</strong>： 识别&quot;脱敏函数&quot;（如 <code>Text.length</code>, <code>List.size</code>）并移除污点：</p><div class="language-text"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>const SANITIZING_FUNCTIONS = [</span></span>
<span class="line"><span>  &#39;Text.length&#39;,</span></span>
<span class="line"><span>  &#39;List.size&#39;,</span></span>
<span class="line"><span>  &#39;Map.keys&#39;,</span><span>  // 仅返回 key，不包含 value</span></span>
<span class="line"><span>]</span></span></code></pre></div><h2 id="_8-诊断生成" tabindex="-1">8. 诊断生成 <a class="header-anchor" href="#_8-诊断生成" aria-label="Permalink to “8. 诊断生成”">​</a></h2><h3 id="_8-1-诊断级别" tabindex="-1">8.1 诊断级别 <a class="header-anchor" href="#_8-1-诊断级别" aria-label="Permalink to “8.1 诊断级别”">​</a></h3><table tabindex="0"><thead><tr><th>级别</th><th>敏感度</th><th>触发条件</th><th>示例</th></tr></thead><tbody><tr><td><strong>Error</strong></td><td>L3</td><td>高敏感 PII 未加密传输/存储</td><td>SSN 通过 HTTP 发送</td></tr><tr><td><strong>Warning</strong></td><td>L2</td><td>中敏感 PII 可能泄露</td><td>Email 写入日志</td></tr><tr><td><strong>Info</strong></td><td>L1</td><td>低敏感 PII 使用提示</td><td>Name 拼接到 URL</td></tr></tbody></table><h3 id="_8-2-诊断生成算法" tabindex="-1">8.2 诊断生成算法 <a class="header-anchor" href="#_8-2-诊断生成算法" aria-label="Permalink to “8.2 诊断生成算法”">​</a></h3><div class="language-text"><button title="Copy Code" class="copy"></button><span class="lang">text</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>function generateDiagnostics(violations: SinkViolation[]): Diagnostic[] {</span></span>
<span class="line"><span>  let diagnostics: Diagnostic[] = []</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  for each violation in violations {</span></span>
<span class="line"><span>    let severity = determineSeverity(violation.taint)</span></span>
<span class="line"><span>    let message = formatMessage(violation)</span></span>
<span class="line"><span>    let suggestions = generateSuggestions(violation)</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    diagnostics.add({</span></span>
<span class="line"><span>      severity: severity,</span></span>
<span class="line"><span>      message: message,</span></span>
<span class="line"><span>      location: violation.location,</span></span>
<span class="line"><span>      code: violation.kind,</span></span>
<span class="line"><span>      suggestions: suggestions,</span></span>
<span class="line"><span>      relatedInfo: traceDataFlow(violation)</span></span>
<span class="line"><span>    })</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  return diagnostics</span></span>
<span class="line"><span>}</span></span>
<span class="line"><span></span></span>
<span class="line"><span>function determineSeverity(taint: TaintSet): &#39;error&#39; | &#39;warning&#39; | &#39;info&#39; {</span></span>
<span class="line"><span>  let maxSensitivity = max(taint.taints.map(t =&gt; t.sensitivity))</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  switch maxSensitivity {</span></span>
<span class="line"><span>    case &#39;L3&#39;: return &#39;error&#39;</span></span>
<span class="line"><span>    case &#39;L2&#39;: return &#39;warning&#39;</span></span>
<span class="line"><span>    case &#39;L1&#39;: return &#39;info&#39;</span></span>
<span class="line"><span>    default: return &#39;info&#39;</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span>}</span></span>
<span class="line"><span></span></span>
<span class="line"><span>function formatMessage(violation: SinkViolation): string {</span></span>
<span class="line"><span>  let categories = violation.taint.taints.map(t =&gt; t.category).join(&#39;, &#39;)</span></span>
<span class="line"><span>  let sensitivity = violation.taint.taints.map(t =&gt; t.sensitivity).join(&#39;, &#39;)</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  switch violation.kind {</span></span>
<span class="line"><span>    case &#39;UnencryptedTransmission&#39;:</span></span>
<span class="line"><span>      return \`PII data (\${categories}, sensitivity \${sensitivity}) transmitted over unencrypted HTTP\`</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    case &#39;PiiInLogs&#39;:</span></span>
<span class="line"><span>      return \`PII data (\${categories}, sensitivity \${sensitivity}) written to logs\`</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    case &#39;UnsanitizedStorage&#39;:</span></span>
<span class="line"><span>      return \`High-sensitivity PII (\${categories}) stored without encryption/hashing\`</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    default:</span></span>
<span class="line"><span>      return \`PII security violation: \${violation.kind}\`</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span>}</span></span>
<span class="line"><span></span></span>
<span class="line"><span>function generateSuggestions(violation: SinkViolation): Suggestion[] {</span></span>
<span class="line"><span>  switch violation.kind {</span></span>
<span class="line"><span>    case &#39;UnencryptedTransmission&#39;:</span></span>
<span class="line"><span>      return [</span></span>
<span class="line"><span>        { message: &quot;Change HTTP to HTTPS in the URL&quot;, code: &quot;use-https&quot; },</span></span>
<span class="line"><span>        { message: &quot;Encrypt PII data before transmission&quot;, code: &quot;encrypt-data&quot; },</span></span>
<span class="line"><span>        { message: &quot;Remove PII from request payload&quot;, code: &quot;remove-pii&quot; }</span></span>
<span class="line"><span>      ]</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    case &#39;PiiInLogs&#39;:</span></span>
<span class="line"><span>      return [</span></span>
<span class="line"><span>        { message: &quot;Use Pii.mask() to redact sensitive data&quot;, code: &quot;mask-pii&quot; },</span></span>
<span class="line"><span>        { message: &quot;Remove PII from log message&quot;, code: &quot;remove-from-log&quot; },</span></span>
<span class="line"><span>        { message: &quot;Use structured logging with PII filtering&quot;, code: &quot;structured-log&quot; }</span></span>
<span class="line"><span>      ]</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    case &#39;UnsanitizedStorage&#39;:</span></span>
<span class="line"><span>      return [</span></span>
<span class="line"><span>        { message: &quot;Hash sensitive data before storage (Crypto.hash)&quot;, code: &quot;hash-data&quot; },</span></span>
<span class="line"><span>        { message: &quot;Encrypt data at rest (Crypto.encrypt)&quot;, code: &quot;encrypt-data&quot; },</span></span>
<span class="line"><span>        { message: &quot;Use tokenization for sensitive fields&quot;, code: &quot;tokenize&quot; }</span></span>
<span class="line"><span>      ]</span></span>
<span class="line"><span></span></span>
<span class="line"><span>    default:</span></span>
<span class="line"><span>      return []</span></span>
<span class="line"><span>  }</span></span>
<span class="line"><span>}</span></span>
<span class="line"><span></span></span>
<span class="line"><span>function traceDataFlow(violation: SinkViolation): RelatedInfo[] {</span></span>
<span class="line"><span>  // 追踪污点从源到 sink 的路径</span></span>
<span class="line"><span>  let path = findDataFlowPath(violation.taint)</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  return path.map(node =&gt; ({</span></span>
<span class="line"><span>    message: \`PII data flows through &#39;\${node.expr}&#39;\`,</span></span>
<span class="line"><span>    location: node.span</span></span>
<span class="line"><span>  }))</span></span>
<span class="line"><span>}</span></span></code></pre></div><h3 id="_8-3-诊断示例" tabindex="-1">8.3 诊断示例 <a class="header-anchor" href="#_8-3-诊断示例" aria-label="Permalink to “8.3 诊断示例”">​</a></h3><p><strong>示例 1：未加密 HTTP 传输</strong></p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>To notify_user with email: @pii(L2, email) Text, produce Result:</span></span>
<span class="line"><span>  Return Http.post(&quot;http://api.example.com/notify&quot;, { email }).</span></span></code></pre></div><p>诊断：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>WARNING: PII data (email, sensitivity L2) transmitted over unencrypted HTTP</span></span>
<span class="line"><span>  at src/notify.aster:2:10</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  Suggestions:</span></span>
<span class="line"><span>    1. Change HTTP to HTTPS in the URL</span></span>
<span class="line"><span>    2. Encrypt PII data before transmission</span></span>
<span class="line"><span>    3. Remove PII from request payload</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  Data flow:</span></span>
<span class="line"><span>    - &#39;email&#39; parameter defined at line 1:18 (L2:email)</span></span>
<span class="line"><span>    - Used in Http.post call at line 2:10</span></span></code></pre></div><p><strong>示例 2：PII 日志记录</strong></p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>To process_payment with ssn: @pii(L3, ssn) Text, amount: Int, produce Result:</span></span>
<span class="line"><span>  Log.info(&quot;Processing payment for SSN: &quot; + ssn + &quot;, amount: &quot; + amount).</span></span>
<span class="line"><span>  Return PaymentService.charge(ssn, amount).</span></span></code></pre></div><p>诊断：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>ERROR: PII data (ssn, sensitivity L3) written to logs</span></span>
<span class="line"><span>  at src/payment.aster:2:3</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  Suggestions:</span></span>
<span class="line"><span>    1. Use Pii.mask() to redact sensitive data</span></span>
<span class="line"><span>    2. Remove PII from log message</span></span>
<span class="line"><span>    3. Use structured logging with PII filtering</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  Example fix:</span></span>
<span class="line"><span>    Log.info(&quot;Processing payment for SSN: &quot; + Pii.mask(ssn) + &quot;, amount: &quot; + amount).</span></span></code></pre></div><p><strong>示例 3：跨函数传播</strong></p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>To get_user_email with id: Text, produce @pii(L2, email) Text. It performs io with Sql:</span></span>
<span class="line"><span>  Let user = Db.query(&quot;SELECT email FROM users WHERE id = ?&quot;, [id]).</span></span>
<span class="line"><span>  Return user.email.</span></span>
<span class="line"><span></span></span>
<span class="line"><span>To send_welcome with user_id: Text, produce Result:</span></span>
<span class="line"><span>  Let email = get_user_email(user_id).</span></span>
<span class="line"><span>  Return Http.post(&quot;http://welcome-service.com/send&quot;, { email }).</span></span></code></pre></div><p>诊断：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>WARNING: PII data (email, sensitivity L2) transmitted over unencrypted HTTP</span></span>
<span class="line"><span>  at src/welcome.aster:6:10</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  Data flow:</span></span>
<span class="line"><span>    - &#39;email&#39; originates from get_user_email() at line 5:14 (L2:email)</span></span>
<span class="line"><span>    - Db.query returns tainted data at line 2:14</span></span>
<span class="line"><span>    - Used in Http.post call at line 6:10</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  Suggestions:</span></span>
<span class="line"><span>    1. Change HTTP to HTTPS in the URL</span></span>
<span class="line"><span>    2. Encrypt PII data before transmission</span></span></code></pre></div><h2 id="_9-真实场景分析" tabindex="-1">9. 真实场景分析 <a class="header-anchor" href="#_9-真实场景分析" aria-label="Permalink to “9. 真实场景分析”">​</a></h2><h3 id="_9-1-场景-1-用户注册流程" tabindex="-1">9.1 场景 1：用户注册流程 <a class="header-anchor" href="#_9-1-场景-1-用户注册流程" aria-label="Permalink to “9.1 场景 1：用户注册流程”">​</a></h3><p><strong>代码</strong>：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>Define UserInput with</span></span>
<span class="line"><span>  email: Text,</span></span>
<span class="line"><span>  password: Text,</span></span>
<span class="line"><span>  name: Text.</span></span>
<span class="line"><span></span></span>
<span class="line"><span>To register_user with input: UserInput, produce Result. It performs io with Http and Sql:</span></span>
<span class="line"><span>  // 步骤 1：验证邮箱格式</span></span>
<span class="line"><span>  Let is_valid = Email.validate(input.email).</span></span>
<span class="line"><span>  If !is_valid:</span></span>
<span class="line"><span>    Return Err(&quot;Invalid email&quot;).</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  // 步骤 2：检查邮箱是否已存在（污点源）</span></span>
<span class="line"><span>  Let existing = Db.query(&quot;SELECT * FROM users WHERE email = ?&quot;, [input.email]).</span></span>
<span class="line"><span>  If existing != null:</span></span>
<span class="line"><span>    Return Err(&quot;Email already exists&quot;).</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  // 步骤 3：创建用户（污点传播）</span></span>
<span class="line"><span>  Let hashed_password = Crypto.hash(input.password).</span></span>
<span class="line"><span>  Let user = User(input.email, hashed_password, input.name).</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  // 步骤 4：存储到数据库</span></span>
<span class="line"><span>  Db.insert(&quot;users&quot;, user).</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  // 步骤 5：发送欢迎邮件（敏感 sink）</span></span>
<span class="line"><span>  Http.post(&quot;http://email-service.com/send&quot;, {</span></span>
<span class="line"><span>    to: user.email,</span></span>
<span class="line"><span>    subject: &quot;Welcome!&quot;,</span></span>
<span class="line"><span>    body: &quot;Hello &quot; + user.name</span></span>
<span class="line"><span>  }).</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  Return Ok(&quot;User registered&quot;).</span></span></code></pre></div><p><strong>污点分析</strong>：</p><ol><li><p><strong>污点源</strong>：</p><ul><li><code>Db.query</code> 返回值 <code>existing</code> 被标记为 <code>{ taints: [L2:unknown] }</code>（保守假设）</li><li><code>input.email</code> 未标注但从外部输入，标记为 <code>{ taints: [L2:email] }</code>（启发式）</li></ul></li><li><p><strong>污点传播</strong>：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>existing (L2:unknown)</span></span>
<span class="line"><span>user.email = input.email (L2:email)</span></span>
<span class="line"><span>user.name = input.name (L1:name)</span></span></code></pre></div></li><li><p><strong>Sink 检测</strong>：</p><ul><li><code>Db.insert</code> 接收 <code>user</code>（包含 L2:email）→ <strong>通过</strong>（数据库存储允许 L2）</li><li><code>Http.post</code> 到 <code>http://</code> URL 且包含 <code>user.email</code> (L2) → <strong>警告</strong></li></ul></li><li><p><strong>诊断</strong>：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>WARNING: PII data (email, sensitivity L2) transmitted over unencrypted HTTP</span></span>
<span class="line"><span>  at register_user.aster:23:3</span></span>
<span class="line"><span></span></span>
<span class="line"><span>Suggestions:</span></span>
<span class="line"><span>  1. Change URL to https://email-service.com/send</span></span>
<span class="line"><span>  2. Encrypt email data before transmission</span></span></code></pre></div></li></ol><p><strong>修复</strong>：</p><div class="language-diff"><button title="Copy Code" class="copy"></button><span class="lang">diff</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span style="--shiki-light:#B31D28;--shiki-dark:#FDAEB7;">- Http.post(&quot;http://email-service.com/send&quot;, {</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+ Http.post(&quot;https://email-service.com/send&quot;, {</span></span></code></pre></div><h3 id="_9-2-场景-2-日志调试陷阱" tabindex="-1">9.2 场景 2：日志调试陷阱 <a class="header-anchor" href="#_9-2-场景-2-日志调试陷阱" aria-label="Permalink to “9.2 场景 2：日志调试陷阱”">​</a></h3><p><strong>代码</strong>：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>Define Order with</span></span>
<span class="line"><span>  id: Text,</span></span>
<span class="line"><span>  user_ssn: @pii(L3, ssn) Text,</span></span>
<span class="line"><span>  amount: Int.</span></span>
<span class="line"><span></span></span>
<span class="line"><span>To process_order with order: Order, produce Result. It performs io with Sql:</span></span>
<span class="line"><span>  // 调试日志（不安全！）</span></span>
<span class="line"><span>  Log.debug(&quot;Processing order: &quot; + JSON.stringify(order)).</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  Let result = PaymentGateway.charge(order.user_ssn, order.amount).</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  Match result:</span></span>
<span class="line"><span>    Case Ok(tx_id):</span></span>
<span class="line"><span>      Log.info(&quot;Order &quot; + order.id + &quot; completed, transaction: &quot; + tx_id).</span></span>
<span class="line"><span>      Return Ok(tx_id).</span></span>
<span class="line"><span>    Case Err(msg):</span></span>
<span class="line"><span>      Log.error(&quot;Order failed: &quot; + msg + &quot;, SSN: &quot; + order.user_ssn).</span></span>
<span class="line"><span>      Return Err(msg).</span></span></code></pre></div><p><strong>污点分析</strong>：</p><ol><li><p><strong>污点源</strong>：</p><ul><li><code>order.user_ssn</code> 标注为 <code>@pii(L3, ssn)</code></li></ul></li><li><p><strong>污点传播</strong>：</p><ul><li><code>JSON.stringify(order)</code> 继承 <code>order</code> 的所有字段污点 → <code>{ taints: [L3:ssn] }</code></li><li><code>&quot;Order failed: &quot; + msg + &quot;, SSN: &quot; + order.user_ssn</code> → <code>{ taints: [L3:ssn] }</code></li></ul></li><li><p><strong>Sink 检测</strong>：</p><ul><li><code>Log.debug(...)</code> 包含 L3:ssn → <strong>错误</strong></li><li><code>Log.error(...)</code> 包含 L3:ssn → <strong>错误</strong></li></ul></li><li><p><strong>诊断</strong>：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>ERROR: PII data (ssn, sensitivity L3) written to logs</span></span>
<span class="line"><span>  at process_order.aster:8:3</span></span>
<span class="line"><span>  at process_order.aster:16:7</span></span>
<span class="line"><span></span></span>
<span class="line"><span>Suggestions:</span></span>
<span class="line"><span>  1. Use Pii.mask() to redact SSN</span></span>
<span class="line"><span>  2. Remove SSN from log messages</span></span>
<span class="line"><span>  3. Use structured logging with field-level filtering</span></span></code></pre></div></li></ol><p><strong>修复</strong>：</p><div class="language-diff"><button title="Copy Code" class="copy"></button><span class="lang">diff</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span style="--shiki-light:#B31D28;--shiki-dark:#FDAEB7;">- Log.debug(&quot;Processing order: &quot; + JSON.stringify(order)).</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+ Log.debug(&quot;Processing order: &quot; + JSON.stringify({ id: order.id, amount: order.amount })).</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#B31D28;--shiki-dark:#FDAEB7;">- Log.error(&quot;Order failed: &quot; + msg + &quot;, SSN: &quot; + order.user_ssn).</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+ Log.error(&quot;Order failed: &quot; + msg + &quot;, SSN: &quot; + Pii.mask(order.user_ssn)).</span></span></code></pre></div><h3 id="_9-3-场景-3-第三方-api-集成" tabindex="-1">9.3 场景 3：第三方 API 集成 <a class="header-anchor" href="#_9-3-场景-3-第三方-api-集成" aria-label="Permalink to “9.3 场景 3：第三方 API 集成”">​</a></h3><p><strong>代码</strong>：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>Define Analytics with</span></span>
<span class="line"><span>  user_id: Text,</span></span>
<span class="line"><span>  event: Text,</span></span>
<span class="line"><span>  properties: Map Text to Text.</span></span>
<span class="line"><span></span></span>
<span class="line"><span>To track_event with user_email: @pii(L2, email) Text, event: Text, produce Result. It performs io with Http:</span></span>
<span class="line"><span>  Let analytics = Analytics(</span></span>
<span class="line"><span>    user_id: user_email,  // 使用邮箱作为 user_id（不安全！）</span></span>
<span class="line"><span>    event: event,</span></span>
<span class="line"><span>    properties: Map.empty()</span></span>
<span class="line"><span>  ).</span></span>
<span class="line"><span></span></span>
<span class="line"><span>  // 发送到第三方分析服务</span></span>
<span class="line"><span>  Return Http.post(&quot;https://analytics-vendor.com/track&quot;, analytics).</span></span></code></pre></div><p><strong>污点分析</strong>：</p><ol><li><p><strong>污点源</strong>：</p><ul><li><code>user_email</code> 参数标注为 <code>@pii(L2, email)</code></li></ul></li><li><p><strong>污点传播</strong>：</p><ul><li><code>analytics.user_id = user_email</code> → <code>analytics</code> 继承 <code>{ taints: [L2:email] }</code></li></ul></li><li><p><strong>Sink 检测</strong>：</p><ul><li><code>Http.post</code> 到 HTTPS URL → <strong>检查进一步</strong></li><li>虽然使用 HTTPS，但发送到第三方（非内部域名）→ <strong>警告</strong></li></ul></li><li><p><strong>诊断</strong>：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>WARNING: PII data (email, sensitivity L2) sent to third-party domain &#39;analytics-vendor.com&#39;</span></span>
<span class="line"><span>  at track_event.aster:11:10</span></span>
<span class="line"><span></span></span>
<span class="line"><span>Suggestions:</span></span>
<span class="line"><span>  1. Hash or anonymize user_email before sending</span></span>
<span class="line"><span>  2. Use internal user_id instead of email</span></span>
<span class="line"><span>  3. Review data processing agreement with vendor</span></span></code></pre></div></li></ol><p><strong>修复</strong>：</p><div class="language-diff"><button title="Copy Code" class="copy"></button><span class="lang">diff</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+ To hash_email with email: @pii(L2, email) Text, produce Text:</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+   Return Crypto.hash(email).</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">To track_event with user_email: @pii(L2, email) Text, event: Text, produce Result. It performs io with Http:</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  Let analytics = Analytics(</span></span>
<span class="line"><span style="--shiki-light:#B31D28;--shiki-dark:#FDAEB7;">-   user_id: user_email,</span></span>
<span class="line"><span style="--shiki-light:#22863A;--shiki-dark:#85E89D;">+   user_id: hash_email(user_email),  // 使用 hash 后的标识符</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">    event: event,</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">    properties: Map.empty()</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  ).</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  Return Http.post(&quot;https://analytics-vendor.com/track&quot;, analytics).</span></span></code></pre></div><h2 id="_10-实现指南" tabindex="-1">10. 实现指南 <a class="header-anchor" href="#_10-实现指南" aria-label="Permalink to “10. 实现指南”">​</a></h2><h3 id="_10-1-lsp-集成" tabindex="-1">10.1 LSP 集成 <a class="header-anchor" href="#_10-1-lsp-集成" aria-label="Permalink to “10.1 LSP 集成”">​</a></h3><p>污点分析应集成到 LSP（Language Server Protocol）以提供实时反馈：</p><div class="language-typescript"><button title="Copy Code" class="copy"></button><span class="lang">typescript</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">// src/lsp/pii_diagnostics.ts</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">export</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;"> function</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> analyzePii</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(</span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">document</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> TextDocument</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">)</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> Diagnostic</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">[] {</span></span>
<span class="line"><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">  // 1. 解析文档</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  let</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> source </span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">=</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> document.</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">getText</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">()</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  let</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> ast </span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">=</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> parse</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">lex</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">canonicalize</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(source)))</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  let</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> core </span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">=</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> lowerModule</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(ast)</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">  // 2. 运行污点分析</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  let</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> taintEnv </span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">=</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> identifySources</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(core)</span></span>
<span class="line"><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">  propagateTaints</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(core, taintEnv)</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  let</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> violations </span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">=</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> detectSinks</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(core, taintEnv)</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">  // 3. 生成 LSP 诊断</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  return</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> violations.</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">map</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(</span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">v</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;"> =&gt;</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> toLspDiagnostic</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(v))</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">}</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">// src/lsp/analysis.ts (集成点)</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">export</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;"> function</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> analyzeDocument</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(</span><span style="--shiki-light:#E36209;--shiki-dark:#FFAB70;">document</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> TextDocument</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">)</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> Diagnostic</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">[] {</span></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  let</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> diagnostics</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">:</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;"> Diagnostic</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">[] </span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">=</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> []</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">  // 现有分析</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  diagnostics.</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">push</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">...</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">typecheck</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(document))</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  diagnostics.</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">push</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">...</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">lintCode</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(document))</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">  // 新增：PII 污点分析</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  diagnostics.</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">push</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(</span><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">...</span><span style="--shiki-light:#6F42C1;--shiki-dark:#B392F0;">analyzePii</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">(document))</span></span>
<span class="line"></span>
<span class="line"><span style="--shiki-light:#D73A49;--shiki-dark:#F97583;">  return</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;"> diagnostics</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">}</span></span></code></pre></div><h3 id="_10-2-配置选项" tabindex="-1">10.2 配置选项 <a class="header-anchor" href="#_10-2-配置选项" aria-label="Permalink to “10.2 配置选项”">​</a></h3><p>允许用户自定义分析行为：</p><div class="language-typescript"><button title="Copy Code" class="copy"></button><span class="lang">typescript</span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">// aster.config.json</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">{</span></span>
<span class="line"><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">  &quot;pii&quot;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">: {</span></span>
<span class="line"><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">    &quot;enabled&quot;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">: </span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">true</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">,</span></span>
<span class="line"><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">    &quot;sensitivity&quot;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">: {</span></span>
<span class="line"><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">      &quot;minLevel&quot;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">: </span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">&quot;L2&quot;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">,  </span><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">// 只报告 L2+ 的问题</span></span>
<span class="line"><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">      &quot;categories&quot;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">: [</span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">&quot;email&quot;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">, </span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">&quot;ssn&quot;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">, </span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">&quot;financial&quot;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">]  </span><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">// 关注特定类别</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">    },</span></span>
<span class="line"><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">    &quot;sinks&quot;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">: {</span></span>
<span class="line"><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">      &quot;http&quot;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">: {</span></span>
<span class="line"><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">        &quot;allowedDomains&quot;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">: [</span><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">&quot;internal-api.company.com&quot;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">],  </span><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">// 白名单域名</span></span>
<span class="line"><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">        &quot;requireHttps&quot;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">: </span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">true</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">      },</span></span>
<span class="line"><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">      &quot;logs&quot;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">: {</span></span>
<span class="line"><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">        &quot;allowL1&quot;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">: </span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">true</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">,  </span><span style="--shiki-light:#6A737D;--shiki-dark:#6A737D;">// 允许 L1 数据写入日志</span></span>
<span class="line"><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">        &quot;allowL2&quot;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">: </span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">false</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">,</span></span>
<span class="line"><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">        &quot;allowL3&quot;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">: </span><span style="--shiki-light:#005CC5;--shiki-dark:#79B8FF;">false</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">      }</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">    },</span></span>
<span class="line"><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">    &quot;sanitizers&quot;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">: [</span></span>
<span class="line"><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">      &quot;Crypto.hash&quot;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">,</span></span>
<span class="line"><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">      &quot;Crypto.encrypt&quot;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">,</span></span>
<span class="line"><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">      &quot;Pii.mask&quot;</span><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">,</span></span>
<span class="line"><span style="--shiki-light:#032F62;--shiki-dark:#9ECBFF;">      &quot;Pii.anonymize&quot;</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">    ]</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">  }</span></span>
<span class="line"><span style="--shiki-light:#24292E;--shiki-dark:#E1E4E8;">}</span></span></code></pre></div><h3 id="_10-3-测试策略" tabindex="-1">10.3 测试策略 <a class="header-anchor" href="#_10-3-测试策略" aria-label="Permalink to “10.3 测试策略”">​</a></h3><p>创建黄金测试覆盖：</p><ol><li><strong>基础污点传播</strong>：变量赋值、函数调用、字符串拼接</li><li><strong>Sink 检测</strong>：HTTP、日志、数据库</li><li><strong>跨函数传播</strong>：污点通过多个函数调用传递</li><li><strong>容器操作</strong>：List、Map 中的污点</li><li><strong>控制流</strong>：if/match 分支合并污点</li><li><strong>误报控制</strong>：净化函数、白名单域名</li><li><strong>真实场景</strong>：注册、支付、分析集成</li></ol><p>测试文件位置：<code>test/cnl/examples/pii_taint_*.aster</code></p><p>期望诊断文件：<code>test/cnl/examples/expected_pii_taint_*.diag.txt</code></p><h2 id="_11-未来扩展" tabindex="-1">11. 未来扩展 <a class="header-anchor" href="#_11-未来扩展" aria-label="Permalink to “11. 未来扩展”">​</a></h2><h3 id="_11-1-路径敏感分析" tabindex="-1">11.1 路径敏感分析 <a class="header-anchor" href="#_11-1-路径敏感分析" aria-label="Permalink to “11.1 路径敏感分析”">​</a></h3><p>当前分析是路径不敏感的（path-insensitive），即合并所有分支：</p><div class="language-"><button title="Copy Code" class="copy"></button><span class="lang"></span><pre class="shiki shiki-themes github-light github-dark" style="--shiki-light:#24292e;--shiki-dark:#e1e4e8;--shiki-light-bg:#fff;--shiki-dark-bg:#24292e;" tabindex="0" dir="ltr"><code><span class="line"><span>If condition:</span></span>
<span class="line"><span>  x = pii_data.</span></span>
<span class="line"><span>Else:</span></span>
<span class="line"><span>  x = &quot;safe&quot;.</span></span>
<span class="line"><span></span></span>
<span class="line"><span>Log.info(x).  // 报告警告（误报）</span></span></code></pre></div><p>路径敏感分析可以区分不同执行路径，减少误报。</p><h3 id="_11-2-跨模块分析" tabindex="-1">11.2 跨模块分析 <a class="header-anchor" href="#_11-2-跨模块分析" aria-label="Permalink to “11.2 跨模块分析”">​</a></h3><p>当前分析限于单个模块。跨模块分析需要：</p><ol><li><strong>函数摘要（Function Summary）</strong>：记录每个函数的污点行为</li><li><strong>模块间传播</strong>：导入函数的污点传播规则</li><li><strong>增量更新</strong>：仅重新分析受影响的模块</li></ol><h3 id="_11-3-机器学习辅助" tabindex="-1">11.3 机器学习辅助 <a class="header-anchor" href="#_11-3-机器学习辅助" aria-label="Permalink to “11.3 机器学习辅助”">​</a></h3><p>使用 ML 模型改进：</p><ol><li><strong>自动识别 PII</strong>：从字段名、类型推断 PII（如 <code>email_address</code> → L2:email）</li><li><strong>减少误报</strong>：学习哪些警告被开发者忽略</li><li><strong>推荐修复</strong>：基于历史修复建议最佳实践</li></ol><h3 id="_11-4-运行时监控" tabindex="-1">11.4 运行时监控 <a class="header-anchor" href="#_11-4-运行时监控" aria-label="Permalink to “11.4 运行时监控”">​</a></h3><p>将污点分析扩展到运行时：</p><ol><li><strong>动态污点追踪</strong>：运行时标记和传播污点</li><li><strong>实时审计</strong>：记录 PII 数据流</li><li><strong>入侵检测</strong>：检测异常 PII 访问模式</li></ol><h2 id="_12-参考文献" tabindex="-1">12. 参考文献 <a class="header-anchor" href="#_12-参考文献" aria-label="Permalink to “12. 参考文献”">​</a></h2><ol><li><p><strong>Taint Analysis: A Survey</strong> Schwartz, E.J., Avgerinos, T., and Brumley, D. (2010) [Carnegie Mellon University Technical Report]</p></li><li><p><strong>Static Taint Analysis for Privacy Compliance</strong> Arzt, S., et al. (2014) FlowDroid: Precise Context, Flow, Field, Object-sensitive and Lifecycle-aware Taint Analysis for Android Apps [ACM PLDI 2014]</p></li><li><p><strong>Information Flow Control for Standard ML</strong> Zheng, L. and Myers, A.C. (2007) [ACM CSFW 2007]</p></li><li><p><strong>Practical Static Analysis of JavaScript Applications</strong> Kristensen, E.K. and Møller, A. (2014) [ACM OOPSLA 2014]</p></li><li><p><strong>GDPR Compliance by Design</strong> Bonatti, P.A., et al. (2020) <em>Data Protection in the Era of AI</em> [Springer]</p></li></ol><hr><p><strong>注</strong>：本文档描述的算法为设计阶段，实际实现时可根据性能和精度需求调整策略。优先级应放在避免漏报（高召回率），逐步改进精度（减少误报）。</p>`,166)])])}const g=n(e,[["render",l]]);export{k as __pageData,g as default};
