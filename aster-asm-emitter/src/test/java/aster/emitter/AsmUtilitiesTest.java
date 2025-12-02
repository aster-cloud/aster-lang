package aster.emitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 针对 AsmUtilities 常量发射与装箱逻辑的行为验证。
 */
class AsmUtilitiesTest {

  /**
   * 验证 -1~5 使用 ICONST 系列指令发射。
   */
  @Test
  void testEmitConstIntSmall() {
    Map<Integer, Integer> expectedOpcodes = Map.of(
      -1, Opcodes.ICONST_M1,
      0, Opcodes.ICONST_0,
      1, Opcodes.ICONST_1,
      2, Opcodes.ICONST_2,
      3, Opcodes.ICONST_3,
      4, Opcodes.ICONST_4,
      5, Opcodes.ICONST_5
    );
    for (Map.Entry<Integer, Integer> entry : expectedOpcodes.entrySet()) {
      RecordingMethodVisitor mv = new RecordingMethodVisitor();
      AsmUtilities.emitConstInt(mv, entry.getKey());
      assertEquals(List.of(new Instruction.Insn(entry.getValue())), mv.records, "发射值 " + entry.getKey() + " 时指令应匹配 ICONST");
    }
  }

  /**
   * 验证 byte 范围常量使用 BIPUSH。
   */
  @Test
  void testEmitConstIntByte() {
    RecordingMethodVisitor mv = new RecordingMethodVisitor();
    AsmUtilities.emitConstInt(mv, 100);
    assertEquals(List.of(new Instruction.IntInsn(Opcodes.BIPUSH, 100)), mv.records, "byte 范围常量应使用 BIPUSH");
  }

  /**
   * 验证 short 范围常量使用 SIPUSH。
   */
  @Test
  void testEmitConstIntShort() {
    RecordingMethodVisitor mv = new RecordingMethodVisitor();
    AsmUtilities.emitConstInt(mv, 2000);
    assertEquals(List.of(new Instruction.IntInsn(Opcodes.SIPUSH, 2000)), mv.records, "short 范围常量应使用 SIPUSH");
  }

  /**
   * 验证大整型常量使用 LDC。
   */
  @Test
  void testEmitConstIntLarge() {
    RecordingMethodVisitor mv = new RecordingMethodVisitor();
    AsmUtilities.emitConstInt(mv, 100_000);
    assertEquals(List.of(new Instruction.Ldc(Integer.valueOf(100_000))), mv.records, "大常量应回退到 LDC");
  }

  /**
   * 验证 int 结果在需要对象返回值时正确装箱。
   */
  @Test
  void testBoxPrimitiveInt() {
    RecordingMethodVisitor mv = new RecordingMethodVisitor();
    AsmUtilities.boxPrimitiveResult(mv, 'I', "Ljava/lang/Object;");
    assertEquals(
      List.of(new Instruction.Method("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;")),
      mv.records,
      "int 结果应调用 Integer.valueOf 进行装箱"
    );
  }

  /**
   * 简易指令记录结构，便于断言。
   */
  private sealed interface Instruction permits Instruction.Insn, Instruction.IntInsn, Instruction.Ldc, Instruction.Method {
    record Insn(int opcode) implements Instruction { }
    record IntInsn(int opcode, int operand) implements Instruction { }
    record Ldc(Object value) implements Instruction { }
    record Method(String owner, String name, String descriptor) implements Instruction { }
  }

  /**
   * 记录 MethodVisitor 输出的指令序列，支持多类型指令验证。
   */
  private static final class RecordingMethodVisitor extends MethodVisitor {
    final List<Instruction> records = new ArrayList<>();

    RecordingMethodVisitor() {
      super(Opcodes.ASM9);
    }

    @Override
    public void visitInsn(int opcode) {
      records.add(new Instruction.Insn(opcode));
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
      records.add(new Instruction.IntInsn(opcode, operand));
    }

    @Override
    public void visitLdcInsn(Object value) {
      records.add(new Instruction.Ldc(value));
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
      records.add(new Instruction.Method(owner, name, descriptor));
    }
  }
}
