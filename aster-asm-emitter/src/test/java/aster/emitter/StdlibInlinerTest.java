package aster.emitter;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 StdlibInliner 的表驱动 stdlib 函数内联功能
 */
class StdlibInlinerTest {

    /**
     * 测试 Text.concat 内联（STRING_WRAPPED 类型）
     */
    @Test
    void testTextConcat() {
        RecordingMethodVisitor mv = new RecordingMethodVisitor();

        var arg1 = new CoreModel.StringE();
        arg1.value = "Hello";
        var arg2 = new CoreModel.StringE();
        arg2.value = "World";

        boolean result = StdlibInliner.tryInline(
            mv,
            "Text.concat",
            List.of(arg1, arg2),
            Map.of(),
            Map.of(),
            (m, expr, env, pt) -> {
                if (expr instanceof CoreModel.StringE se) {
                    m.visitLdcInsn(se.value);
                }
            },
            null
        );

        assertTrue(result, "Text.concat 应该成功内联");

        // 验证字节码序列：LDC "Hello" → String.valueOf → LDC "World" → String.valueOf → concat
        List<Instruction> expected = List.of(
            new Instruction.Ldc("Hello"),
            new Instruction.Method("java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;"),
            new Instruction.Ldc("World"),
            new Instruction.Method("java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;"),
            new Instruction.Method("java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;")
        );

        assertEquals(expected, mv.records, "Text.concat 应生成正确的字节码序列");
    }

    /**
     * 测试 Text.contains 内联（STRING_WRAPPED 类型，返回 Boolean）
     */
    @Test
    void testTextContains() {
        RecordingMethodVisitor mv = new RecordingMethodVisitor();

        var arg1 = new CoreModel.StringE();
        arg1.value = "Hello World";
        var arg2 = new CoreModel.StringE();
        arg2.value = "World";

        boolean result = StdlibInliner.tryInline(
            mv,
            "Text.contains",
            List.of(arg1, arg2),
            Map.of(),
            Map.of(),
            (m, expr, env, pt) -> {
                if (expr instanceof CoreModel.StringE se) {
                    m.visitLdcInsn(se.value);
                }
            },
            null
        );

        assertTrue(result);

        // 验证字节码序列：包含装箱步骤（Boolean.valueOf）
        assertTrue(mv.records.stream().anyMatch(i ->
            i instanceof Instruction.Method m &&
            "java/lang/String".equals(m.owner) &&
            "contains".equals(m.name)
        ), "应调用 String.contains");

        assertTrue(mv.records.stream().anyMatch(i ->
            i instanceof Instruction.Method m &&
            "java/lang/Boolean".equals(m.owner) &&
            "valueOf".equals(m.name)
        ), "应对 boolean 结果进行装箱");
    }

    /**
     * 测试 List.get 内联（CAST_WRAPPED 类型）
     */
    @Test
    void testListGet() {
        RecordingMethodVisitor mv = new RecordingMethodVisitor();

        var listExpr = new CoreModel.Name();
        listExpr.name = "myList";
        var indexExpr = new CoreModel.IntE();
        indexExpr.value = 2;

        boolean result = StdlibInliner.tryInline(
            mv,
            "List.get",
            List.of(listExpr, indexExpr),
            Map.of("myList", 1),
            Map.of(),
            (m, expr, env, pt) -> {
                if (expr instanceof CoreModel.Name n) {
                    m.visitVarInsn(Opcodes.ALOAD, env.get(n.name));
                } else if (expr instanceof CoreModel.IntE ie) {
                    m.visitLdcInsn(ie.value);
                }
            },
            null
        );

        assertTrue(result);

        // 验证字节码序列：ALOAD → CHECKCAST List → LDC index → CHECKCAST Integer → unbox → List.get
        assertTrue(mv.records.stream().anyMatch(i ->
            i instanceof Instruction.TypeInsn t &&
            "java/util/List".equals(t.type)
        ), "应对 List 进行类型转换");

        assertTrue(mv.records.stream().anyMatch(i ->
            i instanceof Instruction.Method m &&
            "java/lang/Integer".equals(m.owner) &&
            "intValue".equals(m.name)
        ), "应对 Integer 索引进行拆箱");

        assertTrue(mv.records.stream().anyMatch(i ->
            i instanceof Instruction.Method m &&
            "java/util/List".equals(m.owner) &&
            "get".equals(m.name)
        ), "应调用 List.get");
    }

    /**
     * 测试算术运算 (+) 内联（ARITHMETIC 类型）
     */
    @Test
    void testArithmeticAdd() {
        RecordingMethodVisitor mv = new RecordingMethodVisitor();

        var left = new CoreModel.IntE();
        left.value = 10;
        var right = new CoreModel.IntE();
        right.value = 20;

        boolean result = StdlibInliner.tryInline(
            mv,
            "+",
            List.of(left, right),
            Map.of(),
            Map.of(),
            (m, expr, env, pt) -> {
                if (expr instanceof CoreModel.IntE ie) {
                    m.visitLdcInsn(ie.value);
                }
            },
            null
        );

        assertTrue(result);

        // 验证字节码序列：包含 unbox、加法、装箱
        assertTrue(mv.records.stream().anyMatch(i ->
            i instanceof Instruction.Method m &&
            "java/lang/Integer".equals(m.owner) &&
            "intValue".equals(m.name)
        ), "应对 Integer 进行 unbox");

        assertTrue(mv.records.stream().anyMatch(i ->
            i instanceof Instruction.Insn insn &&
            insn.opcode == Opcodes.IADD
        ), "应生成 IADD 指令");

        assertTrue(mv.records.stream().anyMatch(i ->
            i instanceof Instruction.Method m &&
            "java/lang/Integer".equals(m.owner) &&
            "valueOf".equals(m.name)
        ), "应对结果进行装箱");
    }

    /**
     * 测试比较运算 (<) 内联（ARITHMETIC 类型，返回 Boolean）
     */
    @Test
    void testComparisonLessThan() {
        RecordingMethodVisitor mv = new RecordingMethodVisitor();

        var left = new CoreModel.IntE();
        left.value = 5;
        var right = new CoreModel.IntE();
        right.value = 10;

        boolean result = StdlibInliner.tryInline(
            mv,
            "<",
            List.of(left, right),
            Map.of(),
            Map.of(),
            (m, expr, env, pt) -> {
                if (expr instanceof CoreModel.IntE ie) {
                    m.visitLdcInsn(ie.value);
                }
            },
            null
        );

        assertTrue(result);

        // 验证字节码序列：unbox → IF_ICMPLT 条件跳转 → 装箱为 Boolean
        assertTrue(mv.records.stream().anyMatch(i ->
            i instanceof Instruction.Method m &&
            "java/lang/Integer".equals(m.owner) &&
            "intValue".equals(m.name)
        ), "应对 Integer 进行 unbox");

        assertTrue(mv.records.stream().anyMatch(i ->
            i instanceof Instruction.Jump jump &&
            jump.opcode == Opcodes.IF_ICMPLT
        ), "应生成 IF_ICMPLT 跳转指令");

        assertTrue(mv.records.stream().anyMatch(i ->
            i instanceof Instruction.Method m &&
            "java/lang/Boolean".equals(m.owner) &&
            "valueOf".equals(m.name)
        ), "应装箱为 Boolean");
    }

    /**
     * 测试 Boolean 运算 (not) 内联（BOOLEAN_OP 类型）
     */
    @Test
    void testBooleanNot() {
        RecordingMethodVisitor mv = new RecordingMethodVisitor();

        var arg = new CoreModel.Bool();
        arg.value = true;

        boolean result = StdlibInliner.tryInline(
            mv,
            "not",
            List.of(arg),
            Map.of(),
            Map.of(),
            (m, expr, env, pt) -> {
                if (expr instanceof CoreModel.Bool b) {
                    m.visitInsn(b.value ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
                    // 模拟装箱
                    m.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                }
            },
            null
        );

        assertTrue(result);

        // 验证字节码序列：unbox → IFEQ 条件跳转 → 装箱为 Boolean
        assertTrue(mv.records.stream().anyMatch(i ->
            i instanceof Instruction.Method m &&
            "java/lang/Boolean".equals(m.owner) &&
            "booleanValue".equals(m.name)
        ), "应对 Boolean 进行 unbox");

        assertTrue(mv.records.stream().anyMatch(i ->
            i instanceof Instruction.Jump jump &&
            jump.opcode == Opcodes.IFEQ
        ), "应生成 IFEQ 跳转指令（Boolean negation）");

        assertTrue(mv.records.stream().anyMatch(i ->
            i instanceof Instruction.Method m &&
            "java/lang/Boolean".equals(m.owner) &&
            "valueOf".equals(m.name)
        ), "应对结果装箱为 Boolean");
    }

    /**
     * 测试未知函数不内联（返回 false）
     */
    @Test
    void testUnknownFunctionReturnsFalse() {
        RecordingMethodVisitor mv = new RecordingMethodVisitor();

        boolean result = StdlibInliner.tryInline(
            mv,
            "UnknownFunction",
            List.of(),
            Map.of(),
            Map.of(),
            (m, expr, env, pt) -> {},
            null
        );

        assertFalse(result, "未知函数应返回 false");
        assertTrue(mv.records.isEmpty(), "未知函数不应生成任何字节码");
    }

    /**
     * 测试 Text.split 内联（特殊处理：Arrays.asList）
     */
    @Test
    void testTextSplit() {
        RecordingMethodVisitor mv = new RecordingMethodVisitor();

        var text = new CoreModel.StringE();
        text.value = "a,b,c";
        var delimiter = new CoreModel.StringE();
        delimiter.value = ",";

        boolean result = StdlibInliner.tryInline(
            mv,
            "Text.split",
            List.of(text, delimiter),
            Map.of(),
            Map.of(),
            (m, expr, env, pt) -> {
                if (expr instanceof CoreModel.StringE se) {
                    m.visitLdcInsn(se.value);
                }
            },
            null
        );

        assertTrue(result);

        // 验证字节码序列：String.split → Arrays.asList
        assertTrue(mv.records.stream().anyMatch(i ->
            i instanceof Instruction.Method m &&
            "java/lang/String".equals(m.owner) &&
            "split".equals(m.name)
        ), "应调用 String.split");

        assertTrue(mv.records.stream().anyMatch(i ->
            i instanceof Instruction.Method m &&
            "java/util/Arrays".equals(m.owner) &&
            "asList".equals(m.name)
        ), "应调用 Arrays.asList 转换为 List");
    }

    // ==================== 测试辅助类 ====================

    /**
     * 简易指令记录结构，便于断言
     */
    private sealed interface Instruction permits Instruction.Insn, Instruction.IntInsn, Instruction.Ldc,
        Instruction.Method, Instruction.TypeInsn, Instruction.Jump, Instruction.LabelMark {
        record Insn(int opcode) implements Instruction { }
        record IntInsn(int opcode, int operand) implements Instruction { }
        record Ldc(Object value) implements Instruction { }
        record Method(String owner, String name, String descriptor) implements Instruction { }
        record TypeInsn(int opcode, String type) implements Instruction { }
        record Jump(int opcode, Label label) implements Instruction { }
        record LabelMark(Label label) implements Instruction { }
    }

    /**
     * 记录 MethodVisitor 输出的指令序列，支持多类型指令验证
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

        @Override
        public void visitTypeInsn(int opcode, String type) {
            records.add(new Instruction.TypeInsn(opcode, type));
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            records.add(new Instruction.Jump(opcode, label));
        }

        @Override
        public void visitLabel(Label label) {
            records.add(new Instruction.LabelMark(label));
        }
    }
}
