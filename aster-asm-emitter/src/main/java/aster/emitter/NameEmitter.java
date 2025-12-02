package aster.emitter;

import aster.core.ir.CoreModel;

import java.util.Map;
import java.util.Objects;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.D2I;
import static org.objectweb.asm.Opcodes.D2L;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.I2D;
import static org.objectweb.asm.Opcodes.I2L;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.L2D;
import static org.objectweb.asm.Opcodes.L2I;
import static org.objectweb.asm.Opcodes.LLOAD;

/**
 * 负责从 Name 表达式生成对应的字节码指令。
 */
final class NameEmitter {

  private final TypeResolver typeResolver;
  private final Main.Ctx ctx;

  NameEmitter(TypeResolver typeResolver, Main.Ctx ctx) {
    this.typeResolver = typeResolver;
    this.ctx = ctx;
  }

  /**
   * 发射 Name 表达式字节码
   *
   * @return true 如果成功处理，false 如果需要回退
   */
  boolean tryEmitName(
      MethodVisitor mv,
      CoreModel.Name name,
      String expectedDesc,
      String currentPkg,
      int paramBase,
      Map<String, Integer> env,
      ScopeStack scopeStack
  ) {
    if (name == null) return false;
    if (emitLocalVariable(mv, name, expectedDesc, env, scopeStack)) return true;
    if (emitEnumValue(mv, name, expectedDesc, currentPkg)) return true;
    if (emitFieldAccess(mv, name, expectedDesc, currentPkg, env, scopeStack)) return true;
    if (emitBuiltinField(mv, name)) return true;
    mv.visitInsn(ACONST_NULL);
    return true;
  }

  private boolean emitLocalVariable(
      MethodVisitor mv,
      CoreModel.Name name,
      String expectedDesc,
      Map<String, Integer> env,
      ScopeStack scopeStack
  ) {
    if (env == null || !env.containsKey(name.name)) return false;
    var slot = env.get(name.name);
    Character localType = (scopeStack != null) ? scopeStack.getType(slot) : null;
    if ("Ljava/lang/Object;".equals(expectedDesc)) {
      if (localType != null) {
        switch (localType) {
          case 'I' -> {
            mv.visitVarInsn(ILOAD, slot);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
          }
          case 'Z' -> {
            mv.visitVarInsn(ILOAD, slot);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
          }
          case 'J' -> {
            mv.visitVarInsn(LLOAD, slot);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
          }
          case 'D' -> {
            mv.visitVarInsn(DLOAD, slot);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
          }
          default -> mv.visitVarInsn(ALOAD, slot);
        }
      } else {
        mv.visitVarInsn(ALOAD, slot);
      }
    } else if ("J".equals(expectedDesc)) {
      if (localType != null && (localType == 'I' || localType == 'Z')) {
        mv.visitVarInsn(ILOAD, slot);
        mv.visitInsn(I2L);
      } else if (localType != null && localType == 'J') {
        mv.visitVarInsn(LLOAD, slot);
      } else if (localType != null && localType == 'D') {
        mv.visitVarInsn(DLOAD, slot);
        mv.visitInsn(D2L);
      } else {
        mv.visitVarInsn(ALOAD, slot);
      }
    } else if ("D".equals(expectedDesc)) {
      if (localType != null && (localType == 'I' || localType == 'Z')) {
        mv.visitVarInsn(ILOAD, slot);
        mv.visitInsn(I2D);
      } else if (localType != null && localType == 'J') {
        mv.visitVarInsn(LLOAD, slot);
        mv.visitInsn(L2D);
      } else if (localType != null && localType == 'D') {
        mv.visitVarInsn(DLOAD, slot);
      } else {
        mv.visitVarInsn(ALOAD, slot);
      }
    } else if ("Z".equals(expectedDesc)) {
      if (localType != null && (localType == 'I' || localType == 'Z')) {
        mv.visitVarInsn(ILOAD, slot);
      } else if (localType != null && localType == 'J') {
        mv.visitVarInsn(LLOAD, slot);
        mv.visitInsn(L2I);
      } else if (localType != null && localType == 'D') {
        mv.visitVarInsn(DLOAD, slot);
        mv.visitInsn(D2I);
      } else {
        mv.visitVarInsn(ALOAD, slot);
      }
    } else {
      if (localType != null) {
        switch (localType) {
          case 'J' -> mv.visitVarInsn(LLOAD, slot);
          case 'D' -> mv.visitVarInsn(DLOAD, slot);
          case 'I', 'Z' -> mv.visitVarInsn(ILOAD, slot);
          default -> mv.visitVarInsn(ALOAD, slot);
        }
      } else {
        mv.visitVarInsn(ALOAD, slot);
      }
    }
    return true;
  }

  private boolean emitEnumValue(
      MethodVisitor mv,
      CoreModel.Name name,
      String expectedDesc,
      String currentPkg
  ) {
    if (ctx == null) return false;
    var ownerName = ctx.enumOwner(name.name);
    if (ownerName == null) return false;
    var ownerInternal = Main.resolveTypeInternalName(currentPkg, ownerName);
    mv.visitFieldInsn(GETSTATIC, ownerInternal, name.name, Main.internalDesc(ownerInternal));
    return true;
  }

  private boolean emitFieldAccess(
      MethodVisitor mv,
      CoreModel.Name name,
      String expectedDesc,
      String currentPkg,
      Map<String, Integer> env,
      ScopeStack scopeStack
  ) {
    int dot = name.name.lastIndexOf('.');
    if (dot <= 0) return false;
    var baseName = name.name.substring(0, dot);
    var fieldName = name.name.substring(dot + 1);

    if (env != null && env.containsKey(baseName)) {
      var baseSlot = env.get(baseName);
      String ownerDesc = (scopeStack != null) ? scopeStack.getDescriptor(baseSlot) : null;
      if (ownerDesc == null && scopeStack != null) ownerDesc = scopeStack.getDescriptor(baseName);
      if (ownerDesc != null && ownerDesc.startsWith("L") && ownerDesc.endsWith(";")) {
        var ownerInternal = ownerDesc.substring(1, ownerDesc.length() - 1);
        mv.visitVarInsn(ALOAD, baseSlot);
        String fieldDesc = resolveFieldDescriptor(ctx, currentPkg, ownerInternal, fieldName);
        if (fieldDesc == null) fieldDesc = "Ljava/lang/Object;";
        Main.loadDataField(mv, ownerInternal, fieldName, fieldDesc);
        emitTypeConversion(mv, fieldDesc, expectedDesc);
        return true;
      }
    }

    if (currentPkg != null) {
      var cls = name.name.substring(0, dot);
      var constName = name.name.substring(dot + 1);
      var owner = Main.resolveTypeInternalName(currentPkg, cls);
      mv.visitFieldInsn(GETSTATIC, owner, constName, Main.internalDesc(owner));
      return true;
    }
    return false;
  }

  private boolean emitBuiltinField(MethodVisitor mv, CoreModel.Name name) {
    String builtinField = Main.getBuiltinField(name.name);
    if (builtinField == null) return false;
    mv.visitFieldInsn(
        GETSTATIC,
        "aster/runtime/Builtins",
        builtinField,
        builtinField.equals("NOT") ? "Laster/runtime/Fn1;" : "Laster/runtime/Fn2;"
    );
    return true;
  }

  private void emitTypeConversion(MethodVisitor mv, String sourceDesc, String expectedDesc) {
    if (expectedDesc == null) return;
    if (Objects.equals(sourceDesc, expectedDesc)) return;

    if ("J".equals(expectedDesc)) {
      if ("I".equals(sourceDesc) || "Z".equals(sourceDesc)) {
        mv.visitInsn(I2L);
      } else if ("D".equals(sourceDesc)) {
        mv.visitInsn(D2L);
      } else if ("Ljava/lang/Long;".equals(sourceDesc)) {
        mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
      } else if ("Ljava/lang/Integer;".equals(sourceDesc)) {
        mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
        mv.visitInsn(I2L);
      } else if ("Ljava/lang/Double;".equals(sourceDesc)) {
        mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
        mv.visitInsn(D2L);
      }
      return;
    }

    if ("D".equals(expectedDesc)) {
      if ("I".equals(sourceDesc) || "Z".equals(sourceDesc)) {
        mv.visitInsn(I2D);
      } else if ("J".equals(sourceDesc)) {
        mv.visitInsn(L2D);
      } else if ("Ljava/lang/Double;".equals(sourceDesc)) {
        mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
      } else if ("Ljava/lang/Integer;".equals(sourceDesc)) {
        mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
        mv.visitInsn(I2D);
      } else if ("Ljava/lang/Long;".equals(sourceDesc)) {
        mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
        mv.visitInsn(L2D);
      }
      return;
    }

    if ("Z".equals(expectedDesc)) {
      if ("J".equals(sourceDesc)) {
        mv.visitInsn(L2I);
      } else if ("D".equals(sourceDesc)) {
        mv.visitInsn(D2I);
      } else if ("Ljava/lang/Boolean;".equals(sourceDesc)) {
        mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
      } else if ("Ljava/lang/Integer;".equals(sourceDesc)) {
        mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
      }
      return;
    }

    if ("I".equals(expectedDesc)) {
      if ("J".equals(sourceDesc)) {
        mv.visitInsn(L2I);
      } else if ("D".equals(sourceDesc)) {
        mv.visitInsn(D2I);
      } else if ("Ljava/lang/Integer;".equals(sourceDesc)) {
        mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
      } else if ("Ljava/lang/Long;".equals(sourceDesc)) {
        mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
        mv.visitInsn(L2I);
      } else if ("Ljava/lang/Double;".equals(sourceDesc)) {
        mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
        mv.visitInsn(D2I);
      }
      return;
    }

    if ("Ljava/lang/Object;".equals(expectedDesc)) {
      if ("I".equals(sourceDesc)) {
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
      } else if ("Z".equals(sourceDesc)) {
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
      } else if ("J".equals(sourceDesc)) {
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
      } else if ("D".equals(sourceDesc)) {
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
      }
      return;
    }

    if (expectedDesc.startsWith("L") && !Objects.equals(sourceDesc, expectedDesc)) {
      String targetInternal = expectedDesc.substring(1, expectedDesc.length() - 1);
      mv.visitTypeInsn(CHECKCAST, targetInternal);
    }
  }

  private static String resolveFieldDescriptor(
      Main.Ctx ctx,
      String pkg,
      String ownerInternal,
      String fieldName
  ) {
    if (ctx == null) return null;
    var data = lookupData(ctx, ownerInternal);
    if (data == null || data.fields == null) return null;
    for (var field : data.fields) {
      if (Objects.equals(field.name, fieldName)) {
        return Main.jDesc(pkg, field.type);
      }
    }
    return null;
  }

  private static CoreModel.Data lookupData(Main.Ctx ctx, String ownerInternal) {
    if (ctx == null || ownerInternal == null || ownerInternal.isEmpty()) return null;
    String dotName = ownerInternal.replace('/', '.');
    String current = dotName;
    while (current != null && !current.isEmpty()) {
      var data = ctx.lookupData(current);
      if (data != null) return data;
      int idx = current.indexOf('.');
      if (idx < 0) break;
      current = current.substring(idx + 1);
    }
    int lastDot = dotName.lastIndexOf('.');
    if (lastDot >= 0) {
      String simple = dotName.substring(lastDot + 1);
      var data = ctx.lookupData(simple);
      if (data != null) return data;
    }
    return null;
  }
}
