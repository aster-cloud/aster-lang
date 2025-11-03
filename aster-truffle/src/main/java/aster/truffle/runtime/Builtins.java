package aster.truffle.runtime;

import aster.truffle.nodes.LambdaValue;
import com.oracle.truffle.api.CallTarget;
import java.util.*;

/**
 * Stdlib Builtins Registry - 统一管理所有内置函数
 *
 * 架构重构：
 * - 之前：CallNode硬编码 if-else (lines 37-44)
 * - 现在：注册表 + 函数对象模式
 *
 * 设计原则：
 * - 单一职责：每个Builtin只做一件事
 * - 开放扩展：新增builtin无需修改CallNode
 * - 类型安全：参数校验前置
 */
public final class Builtins {

  /**
   * Builtin函数接口
   */
  @FunctionalInterface
  public interface BuiltinFunction {
    Object call(Object[] args) throws BuiltinException;
  }

  /**
   * Builtin异常
   */
  public static final class BuiltinException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public BuiltinException(String message) { super(message); }
    public BuiltinException(String message, Throwable cause) { super(message, cause); }
  }

  private static final Map<String, BuiltinFunction> REGISTRY = new HashMap<>();

  static {
    // === Arithmetic Operations ===
    register("add", args -> {
      checkArity("add", args, 2);
      return toInt(args[0]) + toInt(args[1]);
    });

    register("sub", args -> {
      checkArity("sub", args, 2);
      return toInt(args[0]) - toInt(args[1]);
    });

    register("mul", args -> {
      checkArity("mul", args, 2);
      return toInt(args[0]) * toInt(args[1]);
    });

    register("div", args -> {
      checkArity("div", args, 2);
      int divisor = toInt(args[1]);
      if (divisor == 0) throw new BuiltinException("div: division by zero");
      return toInt(args[0]) / divisor;
    });

    register("mod", args -> {
      checkArity("mod", args, 2);
      return toInt(args[0]) % toInt(args[1]);
    });

    // === Comparison Operations ===
    register("eq", args -> {
      checkArity("eq", args, 2);
      return Objects.equals(args[0], args[1]);
    });

    register("ne", args -> {
      checkArity("ne", args, 2);
      return !Objects.equals(args[0], args[1]);
    });

    register("lt", args -> {
      checkArity("lt", args, 2);
      return toInt(args[0]) < toInt(args[1]);
    });

    register("lte", args -> {
      checkArity("lte", args, 2);
      return toInt(args[0]) <= toInt(args[1]);
    });

    register("gt", args -> {
      checkArity("gt", args, 2);
      return toInt(args[0]) > toInt(args[1]);
    });

    register("gte", args -> {
      checkArity("gte", args, 2);
      return toInt(args[0]) >= toInt(args[1]);
    });

    // === Boolean Operations ===
    register("not", args -> {
      checkArity("not", args, 1);
      return !toBool(args[0]);
    });

    // === Text Operations ===
    register("Text.concat", args -> {
      checkArity("Text.concat", args, 2);
      return String.valueOf(args[0]) + String.valueOf(args[1]);
    });

    register("Text.toUpper", args -> {
      checkArity("Text.toUpper", args, 1);
      return String.valueOf(args[0]).toUpperCase();
    });

    register("Text.toLower", args -> {
      checkArity("Text.toLower", args, 1);
      return String.valueOf(args[0]).toLowerCase();
    });

    register("Text.startsWith", args -> {
      checkArity("Text.startsWith", args, 2);
      return String.valueOf(args[0]).startsWith(String.valueOf(args[1]));
    });

    register("Text.indexOf", args -> {
      checkArity("Text.indexOf", args, 2);
      return String.valueOf(args[0]).indexOf(String.valueOf(args[1]));
    });

    register("Text.length", args -> {
      checkArity("Text.length", args, 1);
      return String.valueOf(args[0]).length();
    });

    register("Text.substring", args -> {
      checkArity("Text.substring", args, 2, 3);
      String s = String.valueOf(args[0]);
      int start = toInt(args[1]);
      if (args.length == 3) {
        int end = toInt(args[2]);
        return s.substring(start, end);
      }
      return s.substring(start);
    });

    register("Text.trim", args -> {
      checkArity("Text.trim", args, 1);
      return String.valueOf(args[0]).trim();
    });

    register("Text.split", args -> {
      checkArity("Text.split", args, 2);
      String s = String.valueOf(args[0]);
      String delimiter = String.valueOf(args[1]);
      return Arrays.asList(s.split(java.util.regex.Pattern.quote(delimiter)));
    });

    register("Text.replace", args -> {
      checkArity("Text.replace", args, 3);
      String s = String.valueOf(args[0]);
      String target = String.valueOf(args[1]);
      String replacement = String.valueOf(args[2]);
      return s.replace(target, replacement);
    });

    register("Text.contains", args -> {
      checkArity("Text.contains", args, 2);
      String haystack = String.valueOf(args[0]);
      String needle = String.valueOf(args[1]);
      return haystack.contains(needle);
    });

    // === List Operations ===
    register("List.empty", args -> {
      checkArity("List.empty", args, 0);
      return new ArrayList<>();
    });

    register("List.length", args -> {
      checkArity("List.length", args, 1);
      if (args[0] instanceof List<?> l) return l.size();
      throw new BuiltinException("List.length: expected List, got " + typeName(args[0]));
    });

    register("List.get", args -> {
      checkArity("List.get", args, 2);
      if (args[0] instanceof List<?> l) {
        int idx = toInt(args[1]);
        if (idx < 0 || idx >= l.size()) throw new BuiltinException("List.get: index out of bounds: " + idx);
        return l.get(idx);
      }
      throw new BuiltinException("List.get: expected List, got " + typeName(args[0]));
    });

    register("List.append", args -> {
      checkArity("List.append", args, 2);
      if (args[0] instanceof List<?> l) {
        @SuppressWarnings("unchecked")
        List<Object> mutable = new ArrayList<>((List<Object>)l);
        mutable.add(args[1]);
        return mutable;
      }
      throw new BuiltinException("List.append: expected List, got " + typeName(args[0]));
    });

    register("List.concat", args -> {
      checkArity("List.concat", args, 2);
      if (args[0] instanceof List<?> l1 && args[1] instanceof List<?> l2) {
        @SuppressWarnings("unchecked")
        List<Object> result = new ArrayList<>((List<Object>)l1);
        @SuppressWarnings("unchecked")
        List<Object> l2Cast = (List<Object>)l2;
        result.addAll(l2Cast);
        return result;
      }
      throw new BuiltinException("List.concat: expected two Lists");
    });

    register("List.contains", args -> {
      checkArity("List.contains", args, 2);
      if (args[0] instanceof List<?> l) {
        return l.contains(args[1]);
      }
      throw new BuiltinException("List.contains: expected List, got " + typeName(args[0]));
    });

    register("List.slice", args -> {
      checkArity("List.slice", args, 2, 3);
      if (args[0] instanceof List<?> l) {
        int start = toInt(args[1]);
        int end = args.length == 3 ? toInt(args[2]) : l.size();
        @SuppressWarnings("unchecked")
        List<Object> lCast = (List<Object>)l;
        return new ArrayList<>(lCast.subList(start, end));
      }
      throw new BuiltinException("List.slice: expected List, got " + typeName(args[0]));
    });

    register("List.map", args -> {
      checkArity("List.map", args, 2);
      if (!(args[0] instanceof List<?> l)) {
        throw new BuiltinException("List.map: expected List, got " + typeName(args[0]));
      }
      if (!(args[1] instanceof LambdaValue lambda)) {
        throw new BuiltinException("List.map: expected Lambda function, got " + typeName(args[1]));
      }

      CallTarget callTarget = lambda.getCallTarget();
      if (callTarget == null) {
        throw new BuiltinException("List.map: Lambda must have CallTarget (legacy mode not supported)");
      }

      List<Object> result = new ArrayList<>();
      for (Object item : l) {
        // Prepare arguments: [item, ...captures]
        Object[] capturedValues = lambda.getCapturedValues();
        Object[] callArgs = new Object[1 + capturedValues.length];
        callArgs[0] = item;
        System.arraycopy(capturedValues, 0, callArgs, 1, capturedValues.length);

        Object mapped = callTarget.call(callArgs);
        result.add(mapped);
      }
      return result;
    });

    register("List.filter", args -> {
      checkArity("List.filter", args, 2);
      if (!(args[0] instanceof List<?> l)) {
        throw new BuiltinException("List.filter: expected List, got " + typeName(args[0]));
      }
      if (!(args[1] instanceof LambdaValue lambda)) {
        throw new BuiltinException("List.filter: expected Lambda function, got " + typeName(args[1]));
      }

      CallTarget callTarget = lambda.getCallTarget();
      if (callTarget == null) {
        throw new BuiltinException("List.filter: Lambda must have CallTarget (legacy mode not supported)");
      }

      List<Object> result = new ArrayList<>();
      for (Object item : l) {
        // Prepare arguments: [item, ...captures]
        Object[] capturedValues = lambda.getCapturedValues();
        Object[] callArgs = new Object[1 + capturedValues.length];
        callArgs[0] = item;
        System.arraycopy(capturedValues, 0, callArgs, 1, capturedValues.length);

        Object predicate = callTarget.call(callArgs);
        if (Boolean.TRUE.equals(predicate)) {
          result.add(item);
        }
      }
      return result;
    });

    register("List.reduce", args -> {
      checkArity("List.reduce", args, 3);
      if (!(args[0] instanceof List<?> l)) {
        throw new BuiltinException("List.reduce: expected List, got " + typeName(args[0]));
      }
      Object accumulator = args[1]; // initial value
      if (!(args[2] instanceof LambdaValue lambda)) {
        throw new BuiltinException("List.reduce: expected Lambda function, got " + typeName(args[2]));
      }

      CallTarget callTarget = lambda.getCallTarget();
      if (callTarget == null) {
        throw new BuiltinException("List.reduce: Lambda must have CallTarget (legacy mode not supported)");
      }

      for (Object item : l) {
        // Prepare arguments: [accumulator, item, ...captures]
        Object[] capturedValues = lambda.getCapturedValues();
        Object[] callArgs = new Object[2 + capturedValues.length];
        callArgs[0] = accumulator;
        callArgs[1] = item;
        System.arraycopy(capturedValues, 0, callArgs, 2, capturedValues.length);

        accumulator = callTarget.call(callArgs);
      }
      return accumulator;
    });

    // === Map Operations ===
    register("Map.empty", args -> {
      checkArity("Map.empty", args, 0);
      return new HashMap<>();
    });

    register("Map.get", args -> {
      checkArity("Map.get", args, 2);
      if (args[0] instanceof Map<?,?> m) {
        return m.get(args[1]);
      }
      throw new BuiltinException("Map.get: expected Map, got " + typeName(args[0]));
    });

    register("Map.put", args -> {
      checkArity("Map.put", args, 3);
      if (args[0] instanceof Map<?,?> m) {
        @SuppressWarnings("unchecked")
        Map<Object,Object> mutable = new HashMap<>((Map<Object,Object>)m);
        mutable.put(args[1], args[2]);
        return mutable;
      }
      throw new BuiltinException("Map.put: expected Map, got " + typeName(args[0]));
    });

    register("Map.remove", args -> {
      checkArity("Map.remove", args, 2);
      if (args[0] instanceof Map<?,?> m) {
        @SuppressWarnings("unchecked")
        Map<Object,Object> mutable = new HashMap<>((Map<Object,Object>)m);
        mutable.remove(args[1]);
        return mutable;
      }
      throw new BuiltinException("Map.remove: expected Map, got " + typeName(args[0]));
    });

    register("Map.contains", args -> {
      checkArity("Map.contains", args, 2);
      if (args[0] instanceof Map<?,?> m) {
        return m.containsKey(args[1]);
      }
      throw new BuiltinException("Map.contains: expected Map, got " + typeName(args[0]));
    });

    register("Map.keys", args -> {
      checkArity("Map.keys", args, 1);
      if (args[0] instanceof Map<?,?> m) {
        return new ArrayList<>(m.keySet());
      }
      throw new BuiltinException("Map.keys: expected Map, got " + typeName(args[0]));
    });

    register("Map.values", args -> {
      checkArity("Map.values", args, 1);
      if (args[0] instanceof Map<?,?> m) {
        return new ArrayList<>(m.values());
      }
      throw new BuiltinException("Map.values: expected Map, got " + typeName(args[0]));
    });

    register("Map.size", args -> {
      checkArity("Map.size", args, 1);
      if (args[0] instanceof Map<?,?> m) {
        return m.size();
      }
      throw new BuiltinException("Map.size: expected Map, got " + typeName(args[0]));
    });

    // === Result Operations ===
    register("Result.isOk", args -> {
      checkArity("Result.isOk", args, 1);
      // Check for Java Result.Ok class
      if (args[0] != null && args[0].getClass().getSimpleName().equals("Ok")) {
        return true;
      }
      // Check for Map-based Ok
      if (args[0] instanceof Map<?,?> m) {
        return "Ok".equals(m.get("_type"));
      }
      return false;
    });

    register("Result.isErr", args -> {
      checkArity("Result.isErr", args, 1);
      // Check for Java Result.Err class
      if (args[0] != null && args[0].getClass().getSimpleName().equals("Err")) {
        return true;
      }
      // Check for Map-based Err
      if (args[0] instanceof Map<?,?> m) {
        return "Err".equals(m.get("_type"));
      }
      return false;
    });

    register("Result.unwrap", args -> {
      checkArity("Result.unwrap", args, 1);
      // Check for Java Result.Ok class (has public 'value' field)
      if (args[0] != null && args[0].getClass().getSimpleName().equals("Ok")) {
        try {
          var field = args[0].getClass().getField("value");
          return field.get(args[0]);
        } catch (Exception e) {
          throw new BuiltinException("Result.unwrap: failed to access value field");
        }
      }
      // Check for Map-based Ok
      if (args[0] instanceof Map<?,?> m && "Ok".equals(m.get("_type"))) {
        return m.get("value");
      }
      throw new BuiltinException("Result.unwrap: called on Err");
    });

    register("Result.unwrapErr", args -> {
      checkArity("Result.unwrapErr", args, 1);
      // Check for Java Result.Err class (has public 'value' field)
      if (args[0] != null && args[0].getClass().getSimpleName().equals("Err")) {
        try {
          var field = args[0].getClass().getField("value");
          return field.get(args[0]);
        } catch (Exception e) {
          throw new BuiltinException("Result.unwrapErr: failed to access value field");
        }
      }
      // Check for Map-based Err
      if (args[0] instanceof Map<?,?> m && "Err".equals(m.get("_type"))) {
        return m.get("value");
      }
      throw new BuiltinException("Result.unwrapErr: called on Ok");
    });

    // === Maybe Operations ===
    register("Maybe.isSome", args -> {
      checkArity("Maybe.isSome", args, 1);
      if (args[0] instanceof Map<?,?> m) {
        return "Some".equals(m.get("_type"));
      }
      return false;
    });

    register("Maybe.isNone", args -> {
      checkArity("Maybe.isNone", args, 1);
      if (args[0] instanceof Map<?,?> m) {
        return "None".equals(m.get("_type"));
      }
      return args[0] == null;
    });

    register("Maybe.unwrap", args -> {
      checkArity("Maybe.unwrap", args, 1);
      if (args[0] instanceof Map<?,?> m && "Some".equals(m.get("_type"))) {
        return m.get("value");
      }
      throw new BuiltinException("Maybe.unwrap: called on None");
    });

    register("Maybe.unwrapOr", args -> {
      checkArity("Maybe.unwrapOr", args, 2);
      if (args[0] instanceof Map<?,?> m && "Some".equals(m.get("_type"))) {
        return m.get("value");
      }
      return args[1]; // default value
    });

    // Alias for unwrapOr
    register("Maybe.withDefault", args -> {
      checkArity("Maybe.withDefault", args, 2);
      if (args[0] instanceof Map<?,?> m && "Some".equals(m.get("_type"))) {
        return m.get("value");
      }
      if (args[0] == null) return args[1];
      return args[1]; // default value
    });

    register("Maybe.map", args -> {
      checkArity("Maybe.map", args, 2);

      // If None, return None
      if (args[0] == null || (args[0] instanceof Map<?,?> m && "None".equals(m.get("_type")))) {
        return null; // None
      }

      // If Some, apply function
      if (args[0] instanceof Map<?,?> m && "Some".equals(m.get("_type"))) {
        if (!(args[1] instanceof LambdaValue lambda)) {
          throw new BuiltinException("Maybe.map: expected Lambda function, got " + typeName(args[1]));
        }

        CallTarget callTarget = lambda.getCallTarget();
        if (callTarget == null) {
          throw new BuiltinException("Maybe.map: Lambda must have CallTarget (legacy mode not supported)");
        }

        Object value = m.get("value");
        Object[] capturedValues = lambda.getCapturedValues();
        Object[] callArgs = new Object[1 + capturedValues.length];
        callArgs[0] = value;
        System.arraycopy(capturedValues, 0, callArgs, 1, capturedValues.length);

        Object mapped = callTarget.call(callArgs);

        // Return Some(mapped)
        Map<String, Object> result = new HashMap<>();
        result.put("_type", "Some");
        result.put("value", mapped);
        return result;
      }

      throw new BuiltinException("Maybe.map: expected Maybe (Some or None), got " + typeName(args[0]));
    });

    register("Result.mapOk", args -> {
      checkArity("Result.mapOk", args, 2);

      // Check for Java Result.Ok/Err class
      if (args[0] != null && args[0].getClass().getSimpleName().equals("Err")) {
        return args[0]; // Return Err unchanged
      }

      // Check for Map-based Err
      if (args[0] instanceof Map<?,?> m && "Err".equals(m.get("_type"))) {
        return args[0]; // Return Err unchanged
      }

      // Apply function to Ok value
      if (!(args[1] instanceof LambdaValue lambda)) {
        throw new BuiltinException("Result.mapOk: expected Lambda function, got " + typeName(args[1]));
      }

      CallTarget callTarget = lambda.getCallTarget();
      if (callTarget == null) {
        throw new BuiltinException("Result.mapOk: Lambda must have CallTarget (legacy mode not supported)");
      }

      Object value;
      // Check for Java Result.Ok class
      if (args[0] != null && args[0].getClass().getSimpleName().equals("Ok")) {
        try {
          var field = args[0].getClass().getField("value");
          value = field.get(args[0]);
        } catch (Exception e) {
          throw new BuiltinException("Result.mapOk: failed to access value field");
        }
      } else if (args[0] instanceof Map<?,?> m && "Ok".equals(m.get("_type"))) {
        value = m.get("value");
      } else {
        throw new BuiltinException("Result.mapOk: expected Result (Ok or Err), got " + typeName(args[0]));
      }

      Object[] capturedValues = lambda.getCapturedValues();
      Object[] callArgs = new Object[1 + capturedValues.length];
      callArgs[0] = value;
      System.arraycopy(capturedValues, 0, callArgs, 1, capturedValues.length);

      Object mapped = callTarget.call(callArgs);

      // Return Ok(mapped)
      Map<String, Object> result = new HashMap<>();
      result.put("_type", "Ok");
      result.put("value", mapped);
      return result;
    });

    register("Result.mapErr", args -> {
      checkArity("Result.mapErr", args, 2);

      // Check for Java Result.Ok class
      if (args[0] != null && args[0].getClass().getSimpleName().equals("Ok")) {
        return args[0]; // Return Ok unchanged
      }

      // Check for Map-based Ok
      if (args[0] instanceof Map<?,?> m && "Ok".equals(m.get("_type"))) {
        return args[0]; // Return Ok unchanged
      }

      // Apply function to Err value
      if (!(args[1] instanceof LambdaValue lambda)) {
        throw new BuiltinException("Result.mapErr: expected Lambda function, got " + typeName(args[1]));
      }

      CallTarget callTarget = lambda.getCallTarget();
      if (callTarget == null) {
        throw new BuiltinException("Result.mapErr: Lambda must have CallTarget (legacy mode not supported)");
      }

      Object value;
      // Check for Java Result.Err class
      if (args[0] != null && args[0].getClass().getSimpleName().equals("Err")) {
        try {
          var field = args[0].getClass().getField("value");
          value = field.get(args[0]);
        } catch (Exception e) {
          throw new BuiltinException("Result.mapErr: failed to access value field");
        }
      } else if (args[0] instanceof Map<?,?> m && "Err".equals(m.get("_type"))) {
        value = m.get("value");
      } else {
        throw new BuiltinException("Result.mapErr: expected Result (Ok or Err), got " + typeName(args[0]));
      }

      Object[] capturedValues = lambda.getCapturedValues();
      Object[] callArgs = new Object[1 + capturedValues.length];
      callArgs[0] = value;
      System.arraycopy(capturedValues, 0, callArgs, 1, capturedValues.length);

      Object mapped = callTarget.call(callArgs);

      // Return Err(mapped)
      Map<String, Object> result = new HashMap<>();
      result.put("_type", "Err");
      result.put("value", mapped);
      return result;
    });

    register("Result.tapError", args -> {
      checkArity("Result.tapError", args, 2);

      // Check for Java Result.Ok class - return unchanged
      if (args[0] != null && args[0].getClass().getSimpleName().equals("Ok")) {
        return args[0];
      }

      // Check for Map-based Ok - return unchanged
      if (args[0] instanceof Map<?,?> m && "Ok".equals(m.get("_type"))) {
        return args[0];
      }

      // Apply function to Err value for side effects, then return original Err
      if (!(args[1] instanceof LambdaValue lambda)) {
        throw new BuiltinException("Result.tapError: expected Lambda function, got " + typeName(args[1]));
      }

      CallTarget callTarget = lambda.getCallTarget();
      if (callTarget == null) {
        throw new BuiltinException("Result.tapError: Lambda must have CallTarget (legacy mode not supported)");
      }

      Object value;
      // Check for Java Result.Err class
      if (args[0] != null && args[0].getClass().getSimpleName().equals("Err")) {
        try {
          var field = args[0].getClass().getField("value");
          value = field.get(args[0]);
        } catch (Exception e) {
          throw new BuiltinException("Result.tapError: failed to access value field");
        }
      } else if (args[0] instanceof Map<?,?> m && "Err".equals(m.get("_type"))) {
        value = m.get("value");
      } else {
        throw new BuiltinException("Result.tapError: expected Result (Ok or Err), got " + typeName(args[0]));
      }

      // Call lambda for side effects (discard return value)
      Object[] capturedValues = lambda.getCapturedValues();
      Object[] callArgs = new Object[1 + capturedValues.length];
      callArgs[0] = value;
      System.arraycopy(capturedValues, 0, callArgs, 1, capturedValues.length);
      callTarget.call(callArgs);

      // Return original Err unchanged
      return args[0];
    });

    // === IO Operations ===
    register("IO.print", args -> {
      checkArity("IO.print", args, 1);
      throw new UnsupportedOperationException("IO operations not supported in Truffle backend. Use Java or TypeScript backend for IO.");
    });

    register("IO.readLine", args -> {
      checkArity("IO.readLine", args, 0);
      throw new UnsupportedOperationException("IO operations not supported in Truffle backend. Use Java or TypeScript backend for IO.");
    });

    register("IO.readFile", args -> {
      checkArity("IO.readFile", args, 1);
      throw new UnsupportedOperationException("IO operations not supported in Truffle backend. Use Java or TypeScript backend for IO.");
    });

    register("IO.writeFile", args -> {
      checkArity("IO.writeFile", args, 2);
      throw new UnsupportedOperationException("IO operations not supported in Truffle backend. Use Java or TypeScript backend for IO.");
    });

    // === Async Operations ===
    register("await", args -> {
      checkArity("await", args, 1);
      // 当前简化实现：直接返回值（与AwaitNode语义一致）
      return args[0];
    });
  }

  /**
   * 注册builtin函数
   */
  public static void register(String name, BuiltinFunction func) {
    REGISTRY.put(name, func);
  }

  /**
   * 调用builtin函数
   * @param name 函数名
   * @param args 参数
   * @return 返回值，如果不存在返回null
   */
  public static Object call(String name, Object[] args) throws BuiltinException {
    BuiltinFunction func = REGISTRY.get(name);
    if (func == null) return null;
    return func.call(args);
  }

  /**
   * 检查builtin是否存在
   */
  public static boolean has(String name) {
    return REGISTRY.containsKey(name);
  }

  // ===  辅助方法 ===

  private static void checkArity(String name, Object[] args, int expected) {
    if (args.length != expected) {
      throw new BuiltinException(name + ": expected " + expected + " args, got " + args.length);
    }
  }

  private static void checkArity(String name, Object[] args, int min, int max) {
    if (args.length < min || args.length > max) {
      throw new BuiltinException(name + ": expected " + min + "-" + max + " args, got " + args.length);
    }
  }

  private static boolean toBool(Object o) {
    if (o instanceof Boolean b) return b;
    if (o == null) return false;
    if (o instanceof Number n) return n.doubleValue() != 0.0;
    if (o instanceof String s) return !s.isEmpty();
    return true;
  }

  private static int toInt(Object o) {
    if (o instanceof Number n) return n.intValue();
    if (o instanceof String s) return Integer.parseInt(s);
    throw new BuiltinException("Expected number, got " + typeName(o));
  }

  private static String typeName(Object o) {
    if (o == null) return "null";
    if (o instanceof Map<?,?> m) {
      Object t = m.get("_type");
      if (t instanceof String s) return s;
      return "Map";
    }
    return o.getClass().getSimpleName();
  }
}
