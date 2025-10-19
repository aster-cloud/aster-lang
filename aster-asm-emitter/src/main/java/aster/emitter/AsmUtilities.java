package aster.emitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DCONST_1;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.ICONST_M1;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.LCONST_1;
import static org.objectweb.asm.Opcodes.SIPUSH;

/**
 * ASM 常用工具方法。
 */
public final class AsmUtilities {
  private AsmUtilities() {
    // 工具类不应被实例化
  }

  /**
   * 发射字符串常量。
   */
  public static void emitConstString(MethodVisitor mv, String value) {
    mv.visitLdcInsn(value);
  }

  /**
   * 发射整型常量，自动选择最优指令。
   */
  public static void emitConstInt(MethodVisitor mv, int value) {
    switch (value) {
      case -1:
        mv.visitInsn(ICONST_M1);
        return;
      case 0:
        mv.visitInsn(ICONST_0);
        return;
      case 1:
        mv.visitInsn(ICONST_1);
        return;
      case 2:
        mv.visitInsn(ICONST_2);
        return;
      case 3:
        mv.visitInsn(ICONST_3);
        return;
      case 4:
        mv.visitInsn(ICONST_4);
        return;
      case 5:
        mv.visitInsn(ICONST_5);
        return;
      default:
        if (value >= -128 && value <= 127) {
          mv.visitIntInsn(BIPUSH, value);
          return;
        }
        if (value >= -32768 && value <= 32767) {
          mv.visitIntInsn(SIPUSH, value);
          return;
        }
        mv.visitLdcInsn(Integer.valueOf(value));
    }
  }

  /**
   * 发射长整型常量。
   */
  public static void emitConstLong(MethodVisitor mv, long value) {
    if (value == 0L) {
      mv.visitInsn(LCONST_0);
      return;
    }
    if (value == 1L) {
      mv.visitInsn(LCONST_1);
      return;
    }
    mv.visitLdcInsn(Long.valueOf(value));
  }

  /**
   * 发射双精度常量。
   */
  public static void emitConstDouble(MethodVisitor mv, double value) {
    if (value == 0.0d) {
      mv.visitInsn(DCONST_0);
      return;
    }
    if (value == 1.0d) {
      mv.visitInsn(DCONST_1);
      return;
    }
    mv.visitLdcInsn(Double.valueOf(value));
  }

  /**
   * 将原始类型结果装箱为对象。
   */
  public static void boxPrimitiveResult(MethodVisitor mv, char kind, String expectedDesc) {
    if (!"Ljava/lang/Object;".equals(expectedDesc)) {
      return;
    }
    switch (kind) {
      case 'I' -> mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
      case 'Z' -> mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
      case 'J' -> mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
      case 'D' -> mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
      default -> {
        // 无需装箱
      }
    }
  }

  /**
   * 创建 GeneratorAdapter 以便使用高级 API。
   */
  public static GeneratorAdapter createGenerator(MethodVisitor mv, int access, String name, String desc) {
    return new GeneratorAdapter(mv, access, name, desc);
  }

  /**
   * 使用 GeneratorAdapter 执行加法运算。
   */
  public static void emitAdd(GeneratorAdapter gen, Type type) {
    gen.math(GeneratorAdapter.ADD, type);
  }

  /**
   * 使用 GeneratorAdapter 执行减法运算。
   */
  public static void emitSub(GeneratorAdapter gen, Type type) {
    gen.math(GeneratorAdapter.SUB, type);
  }

  /**
   * 使用 GeneratorAdapter 执行乘法运算。
   */
  public static void emitMul(GeneratorAdapter gen, Type type) {
    gen.math(GeneratorAdapter.MUL, type);
  }

  /**
   * 使用 GeneratorAdapter 执行除法运算。
   */
  public static void emitDiv(GeneratorAdapter gen, Type type) {
    gen.math(GeneratorAdapter.DIV, type);
  }

  /**
   * 使用 GeneratorAdapter 进行比较跳转。
   */
  public static void emitCompare(GeneratorAdapter gen, Type type, int op, Label label) {
    gen.ifCmp(type, op, label);
  }

  /**
   * 创建带有公共父类回退策略的 ClassWriter。
   */
  public static ClassWriter createClassWriter() {
    return new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
      @Override
      protected String getCommonSuperClass(String type1, String type2) {
        // 避免加载用户类，统一回退到 Object
        return "java/lang/Object";
      }
    };
  }

  /**
   * 将字节码写入磁盘。
   */
  public static void writeClass(String outputDir, String className, byte[] bytecode) throws IOException {
    Path root = Paths.get(outputDir);
    Path target = root.resolve(className + ".class");
    System.out.println("WRITE ATTEMPT: " + target.toAbsolutePath() + " (" + bytecode.length + " bytes)");
    System.out.println("  outDir=" + root.toAbsolutePath() + ", internal=" + className);
    try {
      Files.createDirectories(target.getParent());
      Files.write(target, bytecode);
      System.out.println("WRITE SUCCESS: " + target.toAbsolutePath() + " (exists=" + Files.exists(target) + ")");
    } catch (IOException e) {
      System.out.println("WRITE FAILED: " + e.getMessage());
      throw e;
    }
  }
}
