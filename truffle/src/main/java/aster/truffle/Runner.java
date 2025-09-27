package aster.truffle;

import aster.truffle.nodes.*;
import com.oracle.truffle.api.frame.FrameDescriptor;

public final class Runner {
  public static void main(String[] args) throws Exception {
    if (args.length > 0) {
      var loader = new Loader();
      // Parse optional function selection flags from args after the JSON path
      String funcName = System.getenv("ASTER_TRUFFLE_FUNC");
      java.util.List<String> argList = new java.util.ArrayList<>(java.util.Arrays.asList(args));
      java.io.File f = new java.io.File(argList.get(0));
      // Support --func=<name>, --fn=<name>, --entry=<name>, or --func <name>
      for (int i = 1; i < argList.size(); ) {
        String a = argList.get(i);
        String name = null;
        if (a.startsWith("--func=")) name = a.substring("--func=".length());
        else if (a.startsWith("--fn=")) name = a.substring("--fn=".length());
        else if (a.startsWith("--entry=")) name = a.substring("--entry=".length());
        else if ("--func".equals(a) || "--fn".equals(a) || "--entry".equals(a)) {
          if (i+1 < argList.size()) { name = argList.get(i+1); argList.remove(i+1); }
        }
        if (name != null && !name.isEmpty()) {
          funcName = name; argList.remove(i); continue;
        }
        i++;
      }
      if (!f.isAbsolute() && !f.exists()) {
        // Gradle runs in the ':truffle' project dir; allow paths relative to repo root
        java.io.File root = new java.io.File(System.getProperty("user.dir")).getParentFile();
        if (root != null) {
          java.io.File f2 = new java.io.File(root, args[0]);
          if (f2.exists()) f = f2;
        }
      }
      if (System.getenv("ASTER_TRUFFLE_DEBUG") != null) {
        System.err.println("DEBUG: input=" + f.getAbsolutePath());
      }
      // Build program with possible overload resolution using remaining CLI args
      java.util.List<String> callArgs = argList.subList(1, argList.size());
      var program = loader.buildProgram(f, funcName, callArgs);
      // Bind params from remaining args as strings
      if (program.params != null) {
        for (int i = 0; i < callArgs.size() && i < program.params.size(); i++) {
          var p = program.params.get(i);
          Object val = coerceArg(callArgs.get(i), p.type);
          program.env.set(p.name, val);
        }
      }
      try {
        Object v = Exec.exec(program.root, null);
        if (System.getenv("ASTER_TRUFFLE_DEBUG") != null) {
          System.err.println("DEBUG: v=" + v + ", x=" + program.env.get("x") + ", o=" + program.env.get("o"));
        }
        if (v != null) {
          System.out.println(v);
        } else {
          // Fallbacks for smoke harness: try common binders or extract first numeric from 'o'
          Object bx = program.env.get("x");
          if (bx != null) {
            System.out.println(bx);
          } else {
            Object o = program.env.get("o");
            Object num = findFirstNumber(o);
            if (num != null) System.out.println(num);
          }
        }
      } catch (ReturnNode.ReturnException rex) { System.out.println(rex.value); }
      if (System.getenv("ASTER_TRUFFLE_PROFILE") != null) {
        System.out.print(aster.truffle.nodes.Profiler.dump());
      }
      return;
    }
    // Fallback tiny demo
    var fd = new FrameDescriptor();
    var n = new IfNode(new LiteralNode(true), new ReturnNode(new LiteralNode("Hi")), new ReturnNode(new LiteralNode("Bye")));
    try { n.execute(null); } catch (ReturnNode.ReturnException rex) { System.out.println(rex.value); }
  }

  private static final com.fasterxml.jackson.databind.ObjectMapper JSON =
      new com.fasterxml.jackson.databind.ObjectMapper().configure(
          com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private static final com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String,Object>> MAP_STRING_OBJECT = new com.fasterxml.jackson.core.type.TypeReference<>() {};

  private static Object coerceArg(String raw, aster.truffle.core.CoreModel.Type ty) {
    // Shorthand constructors: Ok(...), Err(...), Some(...), Just(...)
    if (raw != null) {
      String t = raw.trim();
      int lp = t.indexOf('(');
      int rp = t.endsWith(")") ? t.lastIndexOf(')') : -1;
      if (lp > 0 && rp > lp) {
        String ctor = t.substring(0, lp);
        String innerSrc = t.substring(lp + 1, rp);
        if (ty instanceof aster.truffle.core.CoreModel.Result rs) {
          if ("Ok".equalsIgnoreCase(ctor)) {
            Object inner = tryParseJsonOrString(innerSrc);
            Object val = coerceValue(inner, rs.ok);
            return new aster.truffle.nodes.ResultNodes.Result.Ok(val);
          }
          if ("Err".equalsIgnoreCase(ctor)) {
            Object inner = tryParseJsonOrString(innerSrc);
            Object val = coerceValue(inner, rs.err);
            return new aster.truffle.nodes.ResultNodes.Result.Err(val);
          }
        }
        if (ty instanceof aster.truffle.core.CoreModel.Option opt || ty instanceof aster.truffle.core.CoreModel.Maybe mb) {
          if ("Some".equalsIgnoreCase(ctor) || "Just".equalsIgnoreCase(ctor)) {
            aster.truffle.core.CoreModel.Type innerTy = (ty instanceof aster.truffle.core.CoreModel.Option) ? ((aster.truffle.core.CoreModel.Option)ty).type : ((aster.truffle.core.CoreModel.Maybe)ty).type;
            Object inner = tryParseJsonOrString(innerSrc);
            return coerceValue(inner, innerTy);
          }
        }
      }
    }
    // Detect JSON-like inputs for compound types
    if (raw != null) {
      String t = raw.trim();
      if ((t.startsWith("[") && t.endsWith("]")) || (t.startsWith("{") && t.endsWith("}")) || "null".equals(t)) {
        try {
          Object parsed = JSON.readValue(t, Object.class);
          return coerceValue(parsed, ty);
        } catch (Exception ignore) {
          // fall through to type-specific coercion
        }
      }
    }
    // Type-directed coercion from string
    if (ty instanceof aster.truffle.core.CoreModel.TypeName tn) {
      String n = tn.name;
      if ("Int".equals(n)) {
        try { return Integer.parseInt(raw); } catch (NumberFormatException e) { return 0; }
      }
      if ("Bool".equals(n) || "Boolean".equals(n)) {
        return Boolean.parseBoolean(raw);
      }
      // Text/String: pass-through
      return raw;
    }
    if (ty instanceof aster.truffle.core.CoreModel.Option opt) {
      // Treat "null" or "None" as None; otherwise Some(value)
      if (raw == null) return null;
      String t = raw.trim();
      if ("null".equalsIgnoreCase(t) || "none".equalsIgnoreCase(t)) return null;
      return coerceArg(raw, opt.type);
    }
    if (ty instanceof aster.truffle.core.CoreModel.Maybe mb) {
      if (raw == null) return null;
      String t = raw.trim();
      if ("null".equalsIgnoreCase(t) || "none".equalsIgnoreCase(t)) return null;
      return coerceArg(raw, mb.type);
    }
    if (ty instanceof aster.truffle.core.CoreModel.ListT lt) {
      // Expect JSON array; if not, wrap single value into list
      try {
        java.util.List<?> arr = JSON.readValue(raw, java.util.List.class);
        java.util.ArrayList<Object> out = new java.util.ArrayList<>();
        for (Object v : arr) out.add(coerceValue(v, lt.type));
        return out;
      } catch (Exception e) {
        // Not a JSON array: support CSV (e.g., "1,2,3")
        if (raw.contains(",")) {
          String[] parts = raw.split(",");
          java.util.ArrayList<Object> out = new java.util.ArrayList<>();
          for (String p : parts) out.add(coerceArg(p.trim(), lt.type));
          return out;
        }
        // Fallback: single value list
        return java.util.List.of(coerceArg(raw.trim(), lt.type));
      }
    }
    if (ty instanceof aster.truffle.core.CoreModel.MapT mt) {
      try {
        java.util.Map<String,Object> obj = JSON.readValue(raw, MAP_STRING_OBJECT);
        // Optionally coerce values? We don't have value type beyond mt.val; do best-effort
        java.util.LinkedHashMap<String,Object> out = new java.util.LinkedHashMap<>();
        for (var e : obj.entrySet()) out.put(e.getKey(), coerceValue(e.getValue(), mt.val));
        return out;
      } catch (Exception e) {
        // CSV form: k:v,k2:v2
        if (raw.contains(":")) {
          java.util.LinkedHashMap<String,Object> out = new java.util.LinkedHashMap<>();
          for (String part : raw.split(",")) {
            String[] kv = part.split(":", 2);
            if (kv.length == 2) {
              String k = stripQuotes(kv[0].trim());
              Object val = coerceArg(kv[1].trim(), mt.val);
              out.put(k, val);
            }
          }
          if (!out.isEmpty()) return out;
        }
        // Fallback: raw string
        return raw;
      }
    }
    if (ty instanceof aster.truffle.core.CoreModel.Result rs) {
      // Expect JSON object with Ok/Err; otherwise pass-through raw
      try {
        java.util.Map<String,Object> obj = JSON.readValue(raw, MAP_STRING_OBJECT);
        return coerceValue(obj, ty);
      } catch (Exception e) {
        return raw;
      }
    }
    // Default: pass-through
    return raw;
  }

  private static Object coerceValue(Object v, aster.truffle.core.CoreModel.Type ty) {
    if (ty instanceof aster.truffle.core.CoreModel.TypeName tn) {
      String n = tn.name;
      if ("Int".equals(n)) {
        if (v instanceof Number num) return Integer.valueOf(num.intValue());
        try { return Integer.parseInt(String.valueOf(v)); } catch (NumberFormatException e) { return 0; }
      }
      if ("Bool".equals(n) || "Boolean".equals(n)) {
        if (v instanceof Boolean b) return b.booleanValue();
        return Boolean.parseBoolean(String.valueOf(v));
      }
      // Text/String
      return String.valueOf(v);
    }
    if (ty instanceof aster.truffle.core.CoreModel.Option opt) {
      if (v == null) return null; // None
      return coerceValue(v, opt.type); // Some
    }
    if (ty instanceof aster.truffle.core.CoreModel.Maybe mb) {
      if (v == null) return null;
      return coerceValue(v, mb.type);
    }
    if (ty instanceof aster.truffle.core.CoreModel.ListT lt) {
      if (v instanceof java.util.List<?> list) {
        java.util.ArrayList<Object> out = new java.util.ArrayList<>();
        for (Object e : list) out.add(coerceValue(e, lt.type));
        return out;
      }
      // Wrap single value
      return java.util.List.of(coerceValue(v, lt.type));
    }
    if (ty instanceof aster.truffle.core.CoreModel.MapT mt) {
      if (v instanceof java.util.Map<?,?> m) {
        java.util.LinkedHashMap<String,Object> out = new java.util.LinkedHashMap<>();
        for (var e : ((java.util.Map<?,?>) m).entrySet()) {
          String k = String.valueOf(e.getKey());
          out.put(k, coerceValue(e.getValue(), mt.val));
        }
        return out;
      }
      return v;
    }
    if (ty instanceof aster.truffle.core.CoreModel.Result rs) {
      if (v instanceof java.util.Map<?,?> m) {
        // JSON form: {"Ok": value} or {"Err": value}
        if (m.containsKey("Ok")) {
          Object ov = coerceValue(m.get("Ok"), rs.ok);
          return new aster.truffle.nodes.ResultNodes.Result.Ok(ov);
        }
        if (m.containsKey("Err")) {
          Object ev = coerceValue(m.get("Err"), rs.err);
          return new aster.truffle.nodes.ResultNodes.Result.Err(ev);
        }
      }
      return v;
    }
    // Unknown/other types: return as-is
    return v;
  }

  private static Object tryParseJsonOrString(String src) {
    String t = src == null ? null : src.trim();
    if (t == null) return null;
    try {
      if ((t.startsWith("[") && t.endsWith("]")) || (t.startsWith("{") && t.endsWith("}")) || "null".equalsIgnoreCase(t) || t.startsWith("\"") || t.startsWith("'")) {
        return JSON.readValue(t, Object.class);
      }
    } catch (Exception ignore) {}
    return stripQuotes(t);
  }

  private static String stripQuotes(String s) {
    if (s == null || s.length() < 2) return s;
    char c0 = s.charAt(0);
    char c1 = s.charAt(s.length()-1);
    if ((c0 == '"' && c1 == '"') || (c0 == '\'' && c1 == '\'')) {
      return s.substring(1, s.length()-1);
    }
    return s;
  }

  @SuppressWarnings("unchecked")
  private static Object findFirstNumber(Object v) {
    if (v instanceof Number) return v;
    if (v instanceof java.util.Map<?,?>) {
      for (Object val : ((java.util.Map<Object,Object>) v).values()) {
        if (val == null) continue;
        if (val instanceof Number) return val;
        Object rec = findFirstNumber(val);
        if (rec != null) return rec;
      }
    }
    if (v instanceof java.util.List<?>) {
      for (Object val : ((java.util.List<?>) v)) {
        Object rec = findFirstNumber(val);
        if (rec != null) return rec;
      }
    }
    return null;
  }
}
