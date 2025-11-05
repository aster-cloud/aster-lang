package aster.truffle;

import aster.truffle.core.CoreModel;
import aster.truffle.nodes.*;
import aster.truffle.runtime.FrameSlotBuilder;
import com.fasterxml.jackson.databind.*;
import com.oracle.truffle.api.nodes.Node;
import java.io.*;
import java.util.List;

public final class Loader {
  public static final class Program {
    public final Node root; public final Env env; public final List<CoreModel.Param> params; public final String entry; public final java.util.List<String> effects;
    public Program(Node root, Env env, List<CoreModel.Param> params, String entry, java.util.List<String> effects) { this.root = root; this.env = env; this.params = params; this.entry = entry; this.effects = effects; }
  }

  private final ObjectMapper mapper = new ObjectMapper().configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private final AsterLanguage language;

  public Loader(AsterLanguage language) {
    this.language = language;
  }

  public Node buildFromJson(File f) throws IOException { return buildProgram(f, null, null).root; }

  public Program buildProgram(File f, String funcName) throws IOException { return buildProgram(f, funcName, null); }

  public Program buildProgram(File f, String funcName, java.util.List<String> rawArgs) throws IOException {
    var mod = mapper.readValue(f, CoreModel.Module.class);
    return buildProgramInternal(mod, funcName, rawArgs);
  }

  public Program buildProgram(String jsonContent, String funcName, java.util.List<String> rawArgs) throws IOException {
    var mod = mapper.readValue(jsonContent, CoreModel.Module.class);
    return buildProgramInternal(mod, funcName, rawArgs);
  }

  private Program buildProgramInternal(CoreModel.Module mod, String funcName, java.util.List<String> rawArgs) throws IOException {
    this.entryFunction = null;
    this.entryParamSlots = null;
    this.paramSlotStack.clear();
    // collect enum variants mapping
    this.enumVariantToEnum = new java.util.HashMap<>();
    if (mod.decls != null) for (var d : mod.decls) if (d instanceof CoreModel.Enum en) for (var v : en.variants) enumVariantToEnum.put(v, en.name);
    // Group functions by name for possible overloading
    java.util.Map<String, java.util.List<CoreModel.Func>> funcGroups = new java.util.LinkedHashMap<>();
    if (mod.decls != null) for (var d : mod.decls) if (d instanceof CoreModel.Func fn) funcGroups.computeIfAbsent(fn.name, k -> new java.util.ArrayList<>()).add(fn);
    // Resolve entry function with simple overload selection if needed
    CoreModel.Func entry = null;
    if (funcName != null && !funcName.isEmpty()) {
      var list = funcGroups.get(funcName);
      if (list != null && !list.isEmpty()) entry = selectOverload(list, rawArgs);
    }
    if (entry == null && funcGroups.containsKey("main")) entry = selectOverload(funcGroups.get("main"), rawArgs);
    if (entry == null) {
      // fallback to first available function
      for (var e : funcGroups.entrySet()) { if (!e.getValue().isEmpty()) { entry = selectOverload(e.getValue(), rawArgs); break; } }
    }
    if (entry == null) throw new IOException("No function in module");
    this.entryFunction = entry;
    if (entry != null) {
      FrameSlotBuilder slotBuilder = new FrameSlotBuilder();
      // Add entry function parameters
      if (entry.params != null) {
        for (var p : entry.params) slotBuilder.addParameter(p.name);
      }
      // Collect local variables from entry function body (Let statements)
      java.util.Set<String> entryLocals = new java.util.LinkedHashSet<>();
      collectLocalVariables(entry.body, entryLocals);
      for (String local : entryLocals) {
        if (!slotBuilder.hasVariable(local)) {  // 避免与参数重复
          slotBuilder.addLocal(local);
        }
      }
      this.entryParamSlots = slotBuilder.getSymbolTable();
    } else {
      this.entryParamSlots = null;
    }

    // Build env and predefine all functions as lambdas to enable cross-calls
    this.env = new Env();
    java.util.Map<String, CoreModel.Func> funcs = new java.util.LinkedHashMap<>();
    for (var e : funcGroups.entrySet()) {
      // if overloaded, the last selected takes that name, but we still store one by that name; others are not directly addressable by name
      // Basic approach: choose the first overload per name for env publishing; entry chosen above controls the root call target
      funcs.put(e.getKey(), e.getValue().get(0));
    }
    // First pass: reserve names
    for (var name : funcs.keySet()) env.set(name, null);
    // Second pass: build lambda values and set into env
    for (var e : funcs.entrySet()) {
      var fn = e.getValue();
      java.util.List<String> params = new java.util.ArrayList<>();
      if (fn.params != null) for (var p : fn.params) params.add(p.name);
      java.util.Map<String,Object> captured = java.util.Map.of();

      if (language != null) {
        // New CallTarget-based approach
        // Build FrameDescriptor for function parameters and local variables
        FrameSlotBuilder slotBuilder = new FrameSlotBuilder();
        for (String param : params) {
          slotBuilder.addParameter(param);
        }

        // Collect local variables from function body (Let statements)
        java.util.Set<String> bodyLocals = new java.util.LinkedHashSet<>();
        collectLocalVariables(fn.body, bodyLocals);
        for (String local : bodyLocals) {
          if (!slotBuilder.hasVariable(local)) {  // 避免与参数重复
            slotBuilder.addLocal(local);
          }
        }

        com.oracle.truffle.api.frame.FrameDescriptor frameDescriptor = slotBuilder.build();

        // Build function body with parameter slots in scope
        java.util.Map<String,Integer> funcParamSlots = slotBuilder.getSymbolTable();
        Node body = withParamSlots(funcParamSlots, () -> buildFunctionBody(fn));

        // Create LambdaRootNode for this function (no captures for top-level functions)
        String lambdaFuncName = "func_" + e.getKey();
        LambdaRootNode rootNode = new LambdaRootNode(
            language,
            frameDescriptor,
            lambdaFuncName,
            params.size(),
            0,  // captureCount = 0 for top-level functions
            body
        );

        // Get CallTarget
        com.oracle.truffle.api.CallTarget callTarget = rootNode.getCallTarget();

        // 从 Core IR 函数声明中提取 effects（如 ["IO", "Async"]）
        java.util.Set<String> requiredEffects = fn.effects != null ? new java.util.HashSet<>(fn.effects) : java.util.Set.of();

        // Set LambdaValue with CallTarget and effects into env
        env.set(e.getKey(), new aster.truffle.nodes.LambdaValue(env, params, List.of(), new Object[0], callTarget, requiredEffects));
      } else {
        // Legacy approach for direct Loader usage (non-Polyglot tests, deprecated)
        // Use old LambdaValue without CallTarget (for backward compatibility)
        Node body = buildFunctionBody(fn);
        env.set(e.getKey(), new aster.truffle.nodes.LambdaValue(env, params, captured, body));
      }
    }
    // Ensure params exist in env for binding (they shadow any function names)
    if (entry.params != null) for (var p : entry.params) env.set(p.name, null);
    // Build entry invocation as a call to the function lambda with param names as arguments
    Node target = new NameNodeEnv(env, entry.name);
    java.util.ArrayList<Node> argNodes = new java.util.ArrayList<>();
    if (entry.params != null) for (var p : entry.params) argNodes.add(new NameNodeEnv(env, p.name));
    Node root = CallNode.create(target, argNodes);
    return new Program(root, env, entry.params, entry.name, entry.effects);
  }

  private CoreModel.Func selectOverload(java.util.List<CoreModel.Func> funcs, java.util.List<String> rawArgs) {
    if (funcs == null || funcs.isEmpty()) return null;
    if (rawArgs == null || rawArgs.isEmpty()) return funcs.get(0);
    int bestScore = Integer.MIN_VALUE;
    CoreModel.Func best = null;
    for (var fn : funcs) {
      int arity = fn.params == null ? 0 : fn.params.size();
      if (arity != rawArgs.size()) continue;
      int s = 0;
      for (int i = 0; i < arity; i++) {
        String raw = rawArgs.get(i);
        CoreModel.Type ty = fn.params.get(i).type;
        s += score(raw, ty);
      }
      if (s > bestScore) { bestScore = s; best = fn; }
    }
    return best != null ? best : funcs.get(0);
  }

  private static int score(String raw, CoreModel.Type ty) {
    if (raw == null) return 0;
    String t = raw.trim();
    if (ty instanceof CoreModel.TypeName tn) {
      String n = tn.name;
      if ("Int".equals(n)) return looksInt(t) ? 3 : 0;
      if ("Bool".equals(n) || "Boolean".equals(n)) return looksBool(t) ? 3 : 0;
      return 1; // Text/String or others
    }
    if (ty instanceof CoreModel.Option opt || ty instanceof CoreModel.Maybe mb) {
      if ("null".equalsIgnoreCase(t) || "none".equalsIgnoreCase(t)) return 2;
      CoreModel.Type inner = (ty instanceof CoreModel.Option) ? ((CoreModel.Option)ty).type : ((CoreModel.Maybe)ty).type;
      return 1 + score(t, inner);
    }
    if (ty instanceof CoreModel.ListT) {
      if (t.startsWith("[") && t.endsWith("]")) return 3;
      if (t.contains(",") || t.contains(";") || t.contains("|")) return 2;
      return 1;
    }
    if (ty instanceof CoreModel.MapT) {
      if (t.startsWith("{") && t.endsWith("}")) return 3;
      if (t.contains(":")) return 2;
      return 0;
    }
    if (ty instanceof CoreModel.Result) {
      if (t.startsWith("{") || t.startsWith("Ok(") || t.startsWith("Err(")) return 2;
      return 0;
    }
    return 0;
  }

  private static boolean looksInt(String s) {
    int i = (s.startsWith("+") || s.startsWith("-")) ? 1 : 0;
    if (i >= s.length()) return false;
    for (; i < s.length(); i++) if (!Character.isDigit(s.charAt(i))) return false;
    return true;
  }
  private static boolean looksBool(String s) { return "true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s); }

  private Env env;
  private java.util.Map<String,String> enumVariantToEnum;
  private CoreModel.Func entryFunction;
  private java.util.Map<String,Integer> entryParamSlots;
  private final java.util.Deque<java.util.Map<String,Integer>> paramSlotStack = new java.util.ArrayDeque<>();

  private Node buildBlock(CoreModel.Block b) {
    if (b == null || b.statements == null || b.statements.isEmpty()) return new LiteralNode(null);
    var list = new java.util.ArrayList<Node>();
    java.util.Map<String,Integer> slots = currentParamSlots();  // 获取当前 frame slots

    for (var s : b.statements) {
      if (s instanceof CoreModel.Return r) {
        list.add(new ReturnNode(buildExpr(r.expr)));
      } else if (s instanceof CoreModel.If iff) {
        list.add(new IfNode(buildExpr(iff.cond), buildBlock(iff.thenBlock), buildBlock(iff.elseBlock)));
      } else if (s instanceof CoreModel.Let let) {
        // 优先使用 frame-based LetNode，回退到 Env-based
        AsterExpressionNode valueNode = buildExpr(let.expr);
        if (slots != null && slots.containsKey(let.name)) {
          list.add(LetNodeGen.create(let.name, slots.get(let.name), valueNode));
        } else {
          list.add(new LetNodeEnv(let.name, valueNode, env));
        }
      } else if (s instanceof CoreModel.Match mm) {
        list.add(buildMatch(mm));
      } else if (s instanceof CoreModel.Scope sc) {
        list.add(buildScope(sc));
      } else if (s instanceof CoreModel.Set set) {
        // 优先使用 frame-based SetNode，回退到 Env-based
        AsterExpressionNode valueNode = buildExpr(set.expr);
        if (slots != null && slots.containsKey(set.name)) {
          list.add(SetNodeGen.create(set.name, slots.get(set.name), valueNode));
        } else {
          list.add(new SetNodeEnv(set.name, valueNode, env));
        }
      } else if (s instanceof CoreModel.Start st) {
        list.add(new StartNode(env, st.name, buildExpr(st.expr)));
      } else if (s instanceof CoreModel.Wait wt) {
        list.add(new WaitNode(env, ((wt.names != null) ? wt.names : java.util.List.<String>of()).toArray(new String[0])));
      }
    }
    return new BlockNode(list);
  }

  private AsterExpressionNode buildExpr(CoreModel.Expr e) {
    if (e instanceof CoreModel.StringE s) return new LiteralNode(s.value);
    if (e instanceof CoreModel.Bool b) return new LiteralNode(b.value);
    if (e instanceof CoreModel.IntE i) return new LiteralNode(Integer.valueOf(i.value));
    if (e instanceof CoreModel.LongE l) return new LiteralNode(Long.valueOf(l.value));
    if (e instanceof CoreModel.DoubleE d) return new LiteralNode(Double.valueOf(d.value));
    if (e instanceof CoreModel.NullE) return new LiteralNode(null);
    if (e instanceof CoreModel.AwaitE aw) return new aster.truffle.nodes.AwaitNode(buildExpr(aw.expr));
    if (e instanceof CoreModel.Lambda lam) {
      java.util.List<String> params = new java.util.ArrayList<>();
      if (lam.params != null) for (var p : lam.params) params.add(p.name);
      java.util.List<String> caps = lam.captures != null ? lam.captures : java.util.List.of();

      if (language != null) {
        // New CallTarget-based approach with closure capture
        // Build FrameDescriptor: parameters first, then captures, then locals
        FrameSlotBuilder slotBuilder = new FrameSlotBuilder();

        // Add parameter slots (0..paramCount-1)
        for (String param : params) {
          slotBuilder.addParameter(param);
        }

        // Add capture slots (paramCount..paramCount+captureCount-1)
        for (String captureName : caps) {
          slotBuilder.addLocal(captureName);
        }

        // Collect local variables from lambda body (Let statements)
        java.util.Set<String> bodyLocals = new java.util.LinkedHashSet<>();
        collectLocalVariables(lam.body, bodyLocals);
        for (String local : bodyLocals) {
          if (!slotBuilder.hasVariable(local)) {  // 避免与参数/捕获变量重复
            slotBuilder.addLocal(local);
          }
        }

        com.oracle.truffle.api.frame.FrameDescriptor frameDescriptor = slotBuilder.build();

        // Build body node with parameter and capture slots in scope
        java.util.Map<String,Integer> lambdaSlots = slotBuilder.getSymbolTable();
        Node body = withParamSlots(lambdaSlots, () -> buildBlock(lam.body));

        // Create LambdaRootNode
        String lambdaName = "lambda@" + System.identityHashCode(lam);
        LambdaRootNode rootNode = new LambdaRootNode(
            language,
            frameDescriptor,
            lambdaName,
            params.size(),
            caps.size(),
            body
        );

        // Get CallTarget
        com.oracle.truffle.api.CallTarget callTarget = rootNode.getCallTarget();

        // Create nodes to evaluate captured values at runtime
        AsterExpressionNode[] captureExprs = new AsterExpressionNode[caps.size()];
        for (int i = 0; i < caps.size(); i++) {
          // Build expression to read the captured variable at Lambda creation time
          captureExprs[i] = buildName(caps.get(i));
        }

        // Return LambdaNode that will create LambdaValue with captured values at runtime
        return new aster.truffle.nodes.LambdaNode(language, env, params, caps, captureExprs, callTarget);
      } else {
        // Legacy Loader (Runner without AsterLanguage) 不支持 Lambda
        // 这是有意的设计决策,因为:
        // 1. Lambda 需要 AsterLanguage 来创建 CallTarget
        // 2. Legacy Runner 已标记为 deprecated
        // 3. 用户应迁移到 Polyglot API
        throw new UnsupportedOperationException(
            "Lambda 需要 AsterLanguage 支持。\n" +
            "请使用 Polyglot API 运行代码: Context.create(\"aster\").eval(...)\n" +
            "Legacy Runner 不再支持 Lambda 特性。"
        );
      }
    }
    if (e instanceof CoreModel.Name n) return buildName(n.name);
    if (e instanceof CoreModel.Call c) {
      Node target = buildExpr(c.target);
      var args = new java.util.ArrayList<Node>();
      if (c.args != null) for (var a : c.args) args.add(buildExpr(a));
      return CallNode.create(target, args);
    }
    if (e instanceof CoreModel.Ok ok) return new aster.truffle.nodes.ResultNodes.OkNode(buildExpr(ok.expr));
    if (e instanceof CoreModel.Err er) return new aster.truffle.nodes.ResultNodes.ErrNode(buildExpr(er.expr));
    if (e instanceof CoreModel.Some sm) return new aster.truffle.nodes.ResultNodes.SomeNode(buildExpr(sm.expr));
    if (e instanceof CoreModel.NoneE) return new aster.truffle.nodes.ResultNodes.NoneNode();
    if (e instanceof CoreModel.Construct cons) return buildConstruct(cons);
    return new LiteralNode(null);
  }

  private Node buildMatch(CoreModel.Match mm) {
    var patCases = new java.util.ArrayList<aster.truffle.nodes.MatchNode.CaseNode>();
    if (mm.cases != null) {
      for (var c : mm.cases) {
        aster.truffle.nodes.MatchNode.PatternNode pn = buildPatternNode(c.pattern);
        Node body;
        if (c.body instanceof CoreModel.Scope sc) {
          body = buildScope(sc);
        } else if (c.body != null) {
          // 将所有非 Scope 的语句包装为单语句 Block,确保正确处理 Let/Set/Start/Wait 等
          CoreModel.Block singleStmtBlock = new CoreModel.Block();
          singleStmtBlock.statements = java.util.List.of(c.body);
          body = buildBlock(singleStmtBlock);
        } else {
          body = new LiteralNode(null);
        }
        patCases.add(new aster.truffle.nodes.MatchNode.CaseNode(pn, body));
      }
    }
    return new aster.truffle.nodes.MatchNode(env, buildExpr(mm.expr), patCases);
  }

  private aster.truffle.nodes.MatchNode.PatternNode buildPatternNode(CoreModel.Pattern p) {
    if (p instanceof CoreModel.PatNull) return new aster.truffle.nodes.MatchNode.PatNullNode();
    if (p instanceof CoreModel.PatName pn) return new aster.truffle.nodes.MatchNode.PatNameNode(pn.name);
    if (p instanceof CoreModel.PatInt pi) return new aster.truffle.nodes.MatchNode.PatIntNode(pi.value);
    if (p instanceof CoreModel.PatCtor pc) {
      java.util.List<aster.truffle.nodes.MatchNode.PatternNode> args = new java.util.ArrayList<>();
      if (pc.args != null) for (var a : pc.args) args.add(buildPatternNode(a));
      return new aster.truffle.nodes.MatchNode.PatCtorNode(pc.typeName, pc.names, args);
    }
    return new aster.truffle.nodes.MatchNode.PatNameNode("_");
  }

  private Node buildScope(CoreModel.Scope sc) {
    var list = new java.util.ArrayList<Node>();
    if (sc.statements != null) for (var s : sc.statements) {
      if (s instanceof CoreModel.Return r) list.add(new ReturnNode(buildExpr(r.expr)));
      else if (s instanceof CoreModel.Let let) list.add(new LetNodeEnv(let.name, buildExpr(let.expr), env));
      else if (s instanceof CoreModel.If iff) list.add(new IfNode(buildExpr(iff.cond), buildBlock(iff.thenBlock), buildBlock(iff.elseBlock)));
      else if (s instanceof CoreModel.Set set) list.add(new SetNodeEnv(set.name, buildExpr(set.expr), env));
      else if (s instanceof CoreModel.Start st) list.add(new StartNode(env, st.name, buildExpr(st.expr)));
      else if (s instanceof CoreModel.Wait wt) list.add(new WaitNode(((wt.names != null) ? wt.names : java.util.List.<String>of()).toArray(new String[0])));
    }
    return new BlockNode(list);
  }

  private AsterExpressionNode buildConstruct(CoreModel.Construct cons) {
    var fields = new java.util.LinkedHashMap<String, AsterExpressionNode>();
    if (cons.fields != null) for (var f : cons.fields) fields.put(f.name, buildExpr(f.expr));
    return new ConstructNode(cons.typeName, fields);
  }

  private AsterExpressionNode buildName(String name) {
    // If name is an enum variant, return an enum value object
    if (enumVariantToEnum != null) {
      String en = enumVariantToEnum.get(name);
      if (en != null) {
        return new LiteralNode(new java.util.LinkedHashMap<String,Object>() {{ put("_enum", en); put("value", name); }});
      }
    }
    // If name contains '.', treat as fully-qualified enum value string literal
    if (name.contains(".")) return new LiteralNode(name);
    java.util.Map<String,Integer> slots = currentParamSlots();
    if (slots != null && slots.containsKey(name)) {
      return NameNodeGen.create(name, slots.get(name));
    }
    return new NameNodeEnv(env, name);
  }

  private Node buildFunctionBody(CoreModel.Func fn) {
    if (fn == null) return new LiteralNode(null);
    if (fn == entryFunction && entryParamSlots != null && !entryParamSlots.isEmpty()) {
      return withParamSlots(entryParamSlots, () -> buildBlock(fn.body));
    }
    return buildBlock(fn.body);
  }

  private <T> T withParamSlots(java.util.Map<String,Integer> slots, java.util.function.Supplier<T> supplier) {
    if (slots == null || slots.isEmpty()) return supplier.get();
    paramSlotStack.push(slots);
    try {
      return supplier.get();
    } finally {
      paramSlotStack.pop();
    }
  }

  private java.util.Map<String,Integer> currentParamSlots() {
    return paramSlotStack.isEmpty() ? null : paramSlotStack.peek();
  }

  /**
   * 收集 Block 中所有 Let 语句声明的局部变量名。
   *
   * 递归遍历所有语句，包括 If/Match/Scope 中嵌套的 Block。
   * 用于在构建 FrameDescriptor 前预先分配所有局部变量的 slots。
   *
   * @param block 要扫描的 Block
   * @param locals 用于收集变量名的 Set
   */
  private void collectLocalVariables(CoreModel.Block block, java.util.Set<String> locals) {
    if (block == null || block.statements == null) return;

    for (var stmt : block.statements) {
      if (stmt instanceof CoreModel.Let let) {
        // Let 语句声明新的局部变量
        locals.add(let.name);
      } else if (stmt instanceof CoreModel.If iff) {
        // If 语句的两个分支可能声明局部变量
        collectLocalVariables(iff.thenBlock, locals);
        collectLocalVariables(iff.elseBlock, locals);
      } else if (stmt instanceof CoreModel.Scope scope) {
        // Scope 语句创建新的作用域，但暂时不隔离（简化实现）
        // 将 scope.statements 包装为 Block 继续收集
        CoreModel.Block scopeBlock = new CoreModel.Block();
        scopeBlock.statements = scope.statements;
        collectLocalVariables(scopeBlock, locals);
      } else if (stmt instanceof CoreModel.Match match) {
        // Match 语句的每个 case 可能有局部变量
        if (match.cases != null) {
          for (var c : match.cases) {
            if (c.body instanceof CoreModel.Scope scopeBody) {
              CoreModel.Block caseBlock = new CoreModel.Block();
              caseBlock.statements = scopeBody.statements;
              collectLocalVariables(caseBlock, locals);
            } else if (c.body != null) {
              CoreModel.Block caseBlock = new CoreModel.Block();
              caseBlock.statements = java.util.List.of(c.body);
              collectLocalVariables(caseBlock, locals);
            }
          }
        }
      }
      // Set/Return/Wait/Start 不声明新变量，跳过
    }
  }
}
