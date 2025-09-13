package aster.truffle;

import aster.truffle.core.CoreModel;
import aster.truffle.nodes.*;
import com.fasterxml.jackson.databind.*;
import com.oracle.truffle.api.nodes.Node;
import java.io.*;
import java.util.List;

public final class Loader {
  public static final class Program {
    public final Node root; public final Env env; public final List<CoreModel.Param> params;
    public Program(Node root, Env env, List<CoreModel.Param> params) { this.root = root; this.env = env; this.params = params; }
  }

  private final ObjectMapper mapper = new ObjectMapper().configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public Node buildFromJson(File f) throws IOException { return buildProgram(f).root; }

  public Program buildProgram(File f) throws IOException {
    var mod = mapper.readValue(f, CoreModel.Module.class);
    // collect enum variants mapping
    this.enumVariantToEnum = new java.util.HashMap<>();
    if (mod.decls != null) for (var d : mod.decls) if (d instanceof CoreModel.Enum en) for (var v : en.variants) enumVariantToEnum.put(v, en.name);
    for (var d : mod.decls) if (d instanceof CoreModel.Func fn) return buildFunc(fn);
    throw new IOException("No function in module");
  }

  private Program buildFunc(CoreModel.Func fn) {
    this.env = new Env();
    if (fn.params != null) {
      for (var p : fn.params) env.set(p.name, null);
    }
    return new Program(buildBlock(fn.body), env, fn.params);
  }

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
    if (e instanceof CoreModel.NullE) return new LiteralNode(null);
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
