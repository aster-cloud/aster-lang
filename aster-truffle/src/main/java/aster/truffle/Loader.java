package aster.truffle;

import aster.truffle.core.CoreModel;
import aster.truffle.nodes.*;
import com.fasterxml.jackson.databind.*;
import com.oracle.truffle.api.nodes.Node;
import java.io.*;
import java.util.List;

public final class Loader {
  public static final class Program {
    public final Node root; public final Env env; public final List<CoreModel.Param> params; public final String entry;
    public Program(Node root, Env env, List<CoreModel.Param> params, String entry) { this.root = root; this.env = env; this.params = params; this.entry = entry; }
  }

  private final ObjectMapper mapper = new ObjectMapper().configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public Node buildFromJson(File f) throws IOException { return buildProgram(f, null, null).root; }

  public Program buildProgram(File f, String funcName) throws IOException { return buildProgram(f, funcName, null); }

  public Program buildProgram(File f, String funcName, java.util.List<String> rawArgs) throws IOException {
    var mod = mapper.readValue(f, CoreModel.Module.class);
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
      Node body = buildBlock(fn.body);
      env.set(e.getKey(), new aster.truffle.nodes.LambdaValue(env, params, captured, body));
    }
    // Ensure params exist in env for binding (they shadow any function names)
    if (entry.params != null) for (var p : entry.params) env.set(p.name, null);
    // Build entry invocation as a call to the function lambda with param names as arguments
    Node target = new NameNode(env, entry.name);
    java.util.ArrayList<Node> argNodes = new java.util.ArrayList<>();
    if (entry.params != null) for (var p : entry.params) argNodes.add(new NameNode(env, p.name));
    Node root = new CallNode(target, argNodes);
    return new Program(root, env, entry.params, entry.name);
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

  private Node buildBlock(CoreModel.Block b) {
    if (b == null || b.statements == null || b.statements.isEmpty()) return new LiteralNode(null);
    var list = new java.util.ArrayList<Node>();
    for (var s : b.statements) {
      if (s instanceof CoreModel.Return r) list.add(new ReturnNode(buildExpr(r.expr)));
      else if (s instanceof CoreModel.If iff) list.add(new IfNode(buildExpr(iff.cond), buildBlock(iff.thenBlock), buildBlock(iff.elseBlock)));
      else if (s instanceof CoreModel.Let let) list.add(new LetNode(env, let.name, buildExpr(let.expr)));
      else if (s instanceof CoreModel.Match mm) list.add(buildMatch(mm));
      else if (s instanceof CoreModel.Scope sc) list.add(buildScope(sc));
      else if (s instanceof CoreModel.Set set) list.add(new SetNode(env, set.name, buildExpr(set.expr)));
      else if (s instanceof CoreModel.Start st) list.add(new StartNode(env, st.name, buildExpr(st.expr)));
      else if (s instanceof CoreModel.Wait wt) list.add(new WaitNode(((wt.names != null) ? wt.names : java.util.List.<String>of()).toArray(new String[0])));
    }
    return new BlockNode(list);
  }

  private Node buildExpr(CoreModel.Expr e) {
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
      // Build body node
      Node body = buildBlock(lam.body);
      // Capture current env values for listed captures
      java.util.Map<String,Object> captured = new java.util.HashMap<>();
      for (var c : caps) captured.put(c, env.get(c));
      return new LiteralNode(new aster.truffle.nodes.LambdaValue(env, params, captured, body));
    }
    if (e instanceof CoreModel.Name n) return buildName(n.name);
    if (e instanceof CoreModel.Call c) {
      Node target;
      if (c.target instanceof CoreModel.Name nn) {
        // Dotted name → treat as literal (builtins like Text.concat). Simple name → env lookup.
        target = (nn.name != null && nn.name.contains(".")) ? new LiteralNode(nn.name) : new NameNode(env, nn.name);
      } else target = buildExpr(c.target);
      var args = new java.util.ArrayList<Node>();
      if (c.args != null) for (var a : c.args) args.add(buildExpr(a));
      return new CallNode(target, args);
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
        if (c.body instanceof CoreModel.Return r) body = new ReturnNode(buildExpr(r.expr));
        else if (c.body instanceof CoreModel.If iff) body = new IfNode(buildExpr(iff.cond), buildBlock(iff.thenBlock), buildBlock(iff.elseBlock));
        else if (c.body instanceof CoreModel.Scope sc) body = buildScope(sc);
        else body = new LiteralNode(null);
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
      else if (s instanceof CoreModel.Let let) list.add(new LetNode(env, let.name, buildExpr(let.expr)));
      else if (s instanceof CoreModel.If iff) list.add(new IfNode(buildExpr(iff.cond), buildBlock(iff.thenBlock), buildBlock(iff.elseBlock)));
      else if (s instanceof CoreModel.Set set) list.add(new SetNode(env, set.name, buildExpr(set.expr)));
      else if (s instanceof CoreModel.Start st) list.add(new StartNode(env, st.name, buildExpr(st.expr)));
      else if (s instanceof CoreModel.Wait wt) list.add(new WaitNode(((wt.names != null) ? wt.names : java.util.List.<String>of()).toArray(new String[0])));
    }
    return new BlockNode(list);
  }

  private Node buildConstruct(CoreModel.Construct cons) {
    var fields = new java.util.LinkedHashMap<String, Node>();
    if (cons.fields != null) for (var f : cons.fields) fields.put(f.name, buildExpr(f.expr));
    return new ConstructNode(cons.typeName, fields);
  }

  private Node buildName(String name) {
    // If name is an enum variant, return an enum value object
    if (enumVariantToEnum != null) {
      String en = enumVariantToEnum.get(name);
      if (en != null) {
        return new LiteralNode(new java.util.LinkedHashMap<String,Object>() {{ put("_enum", en); put("value", name); }});
      }
    }
    // If name contains '.', treat as fully-qualified enum value string literal
    if (name.contains(".")) return new LiteralNode(name);
    return new NameNode(env, name);
  }
}
