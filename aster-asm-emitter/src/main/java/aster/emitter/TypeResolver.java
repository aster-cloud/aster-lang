package aster.emitter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 根据CoreModel表达式和ScopeStack推断JVM类型。
 */
final class TypeResolver {
  private final ScopeStack scopeStack;
  private final Map<String, Character> funcHints;
  private final Map<String, CoreModel.Func> functionSchemas;
  private final Map<String, CoreModel.Data> dataSchema;
  private final ContextBuilder contextBuilder;

  TypeResolver(
      ScopeStack scopeStack,
      Map<String, Character> funcHints,
      Map<String, CoreModel.Func> functionSchemas,
      Map<String, CoreModel.Data> dataSchema
  ) {
    this(scopeStack, funcHints, functionSchemas, dataSchema, null);
  }

  TypeResolver(
      ScopeStack scopeStack,
      Map<String, Character> funcHints,
      Map<String, CoreModel.Func> functionSchemas,
      ContextBuilder contextBuilder
  ) {
    this(scopeStack, funcHints, functionSchemas, contextBuilder != null ? contextBuilder.dataSchema() : Map.of(), contextBuilder);
  }

  private TypeResolver(
      ScopeStack scopeStack,
      Map<String, Character> funcHints,
      Map<String, CoreModel.Func> functionSchemas,
      Map<String, CoreModel.Data> dataSchema,
      ContextBuilder contextBuilder
  ) {
    this.scopeStack = Objects.requireNonNull(scopeStack, "scopeStack");
    this.funcHints = Objects.requireNonNull(funcHints, "funcHints");
    this.functionSchemas = (functionSchemas == null) ? Map.of() : functionSchemas;
    this.dataSchema = (dataSchema == null) ? Map.of() : dataSchema;
    this.contextBuilder = contextBuilder;
  }

  /**
   * 获取关联的 ScopeStack 实例。
   *
   * @return ScopeStack 实例
   */
  ScopeStack getScopeStack() {
    return scopeStack;
  }

  /**
   * 推断表达式的JVM类型。
   *
   * @return 'I'(int), 'J'(long), 'D'(double), 'Z'(boolean), null(Object)
   */
  Character inferType(CoreModel.Expr expr) {
    if (expr == null) return null;
    if (expr instanceof CoreModel.Bool) return 'Z';
    if (expr instanceof CoreModel.IntE) return 'I';
    if (expr instanceof CoreModel.LongE) return 'J';
    if (expr instanceof CoreModel.DoubleE) return 'D';

    if (expr instanceof CoreModel.Name n) {
      Character resolved = scopeStack.getType(n.name);
      if (resolved != null) return resolved;
      int dot = n.name.lastIndexOf('.');
      if (dot > 0) {
        Character fieldKind = inferDottedNameKind(n.name);
        if (fieldKind != null) return fieldKind;
      }
      return null;
    }

    if (expr instanceof CoreModel.Ok ok) return inferType(ok.expr);
    if (expr instanceof CoreModel.Err err) return inferType(err.expr);
    if (expr instanceof CoreModel.Some some) return inferType(some.expr);

    if (expr instanceof CoreModel.Call call) {
      Character callKind = inferCall(call);
      if (callKind != null) return callKind;
      if (call.target instanceof CoreModel.Name name) {
        Character schemaKind = inferFromSchema(name.name);
        if (schemaKind != null) return schemaKind;
        Character hint = funcHints.get(name.name);
        if (hint != null) return normalize(hint);
      }
      return null;
    }

    return null;
  }

  private Character inferDottedNameKind(String dottedName) {
    if (dottedName == null || dataSchema.isEmpty()) return null;
    var type = resolveTypeForPath(dottedName);
    if (type instanceof CoreModel.TypeName tn) {
      return switch (tn.name) {
        case "Int" -> 'I';
        case "Bool" -> 'Z';
        case "Long" -> 'J';
        case "Double" -> 'D';
        default -> null;
      };
    }
    return null;
  }

  private CoreModel.Type resolveTypeForPath(String dottedName) {
    if (dottedName == null || dottedName.isEmpty()) return null;
    String[] parts = dottedName.split("\\.");
    if (parts.length < 2) return null;
    String base = parts[0];
    String baseDesc = scopeStack.getDescriptor(base);
    if (baseDesc == null) return null;
    CoreModel.Data currentData = findDataByDescriptor(baseDesc);
    if (currentData == null) return null;
    CoreModel.Type currentType = null;
    for (int i = 1; i < parts.length; i++) {
      if (currentData.fields == null) return null;
      String fieldName = parts[i];
      currentType = null;
      for (var field : currentData.fields) {
        if (Objects.equals(field.name, fieldName)) {
          currentType = field.type;
          break;
        }
      }
      if (currentType == null) return null;
      if (i < parts.length - 1) {
        if (currentType instanceof CoreModel.TypeName tn) {
          currentData = findDataByTypeName(tn.name);
          if (currentData == null) return null;
        } else {
          return null;
        }
      }
    }
    return currentType;
  }

  private CoreModel.Data findDataByDescriptor(String descriptor) {
    if (descriptor == null || descriptor.length() < 2 || descriptor.charAt(0) != 'L') return null;
    String internal = descriptor.substring(1, descriptor.length() - 1);
    String dotName = internal.replace('/', '.');
    return findDataByCandidateNames(dotName);
  }

  private CoreModel.Data findDataByTypeName(String typeName) {
    if (typeName == null || typeName.isEmpty()) return null;
    String dotName = typeName.replace('/', '.');
    return findDataByCandidateNames(dotName);
  }

  private CoreModel.Data findDataByCandidateNames(String dotName) {
    if (dotName == null || dotName.isEmpty()) return null;
    String current = dotName;
    while (current != null && !current.isEmpty()) {
      var data = dataSchema.get(current);
      if (data == null && contextBuilder != null) data = contextBuilder.lookupData(current);
      if (data != null) return data;
      int idx = current.indexOf('.');
      if (idx < 0) break;
      current = current.substring(idx + 1);
    }
    int lastDot = dotName.lastIndexOf('.');
    if (lastDot >= 0) {
      String simple = dotName.substring(lastDot + 1);
      var data = dataSchema.get(simple);
      if (data == null && contextBuilder != null) data = contextBuilder.lookupData(simple);
      if (data != null) return data;
    }
    return null;
  }

  private Character inferCall(CoreModel.Call call) {
    if (!(call.target instanceof CoreModel.Name name)) return null;
    String op = name.name;
    List<CoreModel.Expr> args = (call.args == null) ? List.of() : call.args;
    if (args.isEmpty()) return null;

    if (isNumericBinary(op) && args.size() == 2) {
      Character left = inferType(args.get(0));
      Character right = inferType(args.get(1));
      return promoteNumeric(left, right);
    }
    if (isComparison(op) && args.size() == 2) {
      return 'Z';
    }
    if ("not".equals(op) && args.size() == 1) {
      return 'Z';
    }
    if (("negate".equals(op) || "minus".equals(op)) && args.size() == 1) {
      return inferType(args.get(0));
    }
    return null;
  }

  private static Character promoteNumeric(Character left, Character right) {
    if (left == null && right == null) return null;
    if (left == null) return normalize(right);
    if (right == null) return normalize(left);
    if (left == 'D' || right == 'D') return 'D';
    if (left == 'J' || right == 'J') return 'J';
    left = normalize(left);
    right = normalize(right);
    if (left == 'D' || right == 'D') return 'D';
    if (left == 'J' || right == 'J') return 'J';
    return 'I';
  }

  private static Character normalize(Character kind) {
    if (kind == null) return null;
    return switch (kind) {
      case 'Z', 'I' -> 'I';
      case 'J' -> 'J';
      case 'D' -> 'D';
      default -> kind;
    };
  }

  private static boolean isNumericBinary(String op) {
    return "+".equals(op)
        || "-".equals(op)
        || "*".equals(op)
        || "/".equals(op)
        || "times".equals(op)
        || "divided by".equals(op);
  }

  private static boolean isComparison(String op) {
    return "<".equals(op)
        || ">".equals(op)
        || "<=".equals(op)
        || ">=".equals(op)
        || "==".equals(op)
        || "!=".equals(op)
        || "equals".equals(op);
  }

  private Character inferFromSchema(String funcName) {
    if (functionSchemas == null || functionSchemas.isEmpty() || funcName == null) return null;
    CoreModel.Func schema = functionSchemas.get(funcName);
    if (schema == null && funcName.contains(".")) {
      String simple = funcName.substring(funcName.lastIndexOf('.') + 1);
      schema = functionSchemas.get(simple);
    }
    if (schema != null && schema.ret instanceof CoreModel.TypeName rtn) {
      return switch (rtn.name) {
        case "Int" -> 'I';
        case "Bool" -> 'Z';
        case "Long" -> 'J';
        case "Double" -> 'D';
        default -> null;
      };
    }
    return null;
  }
}
