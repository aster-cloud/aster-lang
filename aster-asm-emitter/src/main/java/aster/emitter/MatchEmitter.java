package aster.emitter;

import aster.core.ir.CoreModel;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

/**
 * 负责 Match 语句字节码生成的类。
 *
 * 职责：
 * - 处理 Match 表达式求值和 scrutinee 存储
 * - 生成 Pattern 匹配逻辑（PatNull, PatInt, PatCtor, PatName）
 * - 优化特殊情况（Enum TableSwitch, Int TableSwitch/LookupSwitch）
 * - 管理 Match case 作用域和变量绑定
 *
 * 设计原则：
 * - 从 FunctionEmitter 中解耦 Match 逻辑（~376 行）
 * - 保持与 Main.emitFunc 的字节码输出一致
 * - 通过 Golden 测试验证正确性
 */
public class MatchEmitter {
    private final Main.Ctx ctx;
    private final TypeResolver typeResolver;
    private final ExpressionEmitter expressionEmitter;

    public MatchEmitter(Main.Ctx ctx, TypeResolver typeResolver, ExpressionEmitter expressionEmitter) {
        this.ctx = ctx;
        this.typeResolver = typeResolver;
        this.expressionEmitter = expressionEmitter;
    }

    /**
     * 发射 Match 语句字节码。
     *
     * @param mv MethodVisitor
     * @param match Match 语句节点
     * @param emitCtx 发射上下文
     * @param scopeStack 作用域栈
     * @param lvars 局部变量表记录
     * @throws IOException 如果发射失败
     */
    public void emitMatch(MethodVisitor mv, CoreModel.Match match, EmitContext emitCtx,
                         ScopeStack scopeStack, List<FunctionEmitter.LV> lvars) throws IOException {
        int scrSlot = emitCtx.nextSlot();

        // 发射 scrutinee 表达式
        emitExpr(mv, match.expr, null, emitCtx, scopeStack);
        mv.visitVarInsn(ASTORE, scrSlot);
        lvars.add(new FunctionEmitter.LV("_scr", "Ljava/lang/Object;", scrSlot));

        var lEndMatch = new Label();
        if (match.cases == null) {
            mv.visitLabel(lEndMatch);
            return;
        }

        // 尝试优化：Enum TableSwitch
        if (tryEmitEnumTableSwitch(mv, match, scrSlot, emitCtx, scopeStack, lEndMatch)) {
            return;
        }

        // 尝试优化：Int Pattern（TableSwitch 或 LookupSwitch）
        if (tryEmitIntSwitch(mv, match, scrSlot, emitCtx, scopeStack, lEndMatch)) {
            return;
        }

        // Fall-through: 逐个匹配 Pattern
        emitSequentialMatching(mv, match, scrSlot, emitCtx, scopeStack, lvars, lEndMatch);

        mv.visitLabel(lEndMatch);
    }

    /**
     * 尝试优化为 Enum TableSwitch。
     */
    private boolean tryEmitEnumTableSwitch(MethodVisitor mv, CoreModel.Match match, int scrSlot,
                                          EmitContext emitCtx, ScopeStack scopeStack, Label lEndMatch) throws IOException {
        boolean allNames = match.cases.stream().allMatch(c -> c.pattern instanceof CoreModel.PatName);
        if (!allNames) return false;

        String enumOwner = null;
        boolean mixedEnums = false;
        for (var c : match.cases) {
            var variant = ((CoreModel.PatName) c.pattern).name;
            var owner = ctx.enumOwner(variant);
            if (owner == null) {
                mixedEnums = true;
                break;
            }
            if (enumOwner == null) {
                enumOwner = owner;
            } else if (!enumOwner.equals(owner)) {
                mixedEnums = true;
                break;
            }
        }

        if (mixedEnums || enumOwner == null || !ctx.hasEnumVariants(enumOwner)) {
            return false;
        }

        var enumInternal = Main.resolveTypeInternalName(emitCtx.getPkg(), enumOwner);

        mv.visitVarInsn(ALOAD, scrSlot);
        mv.visitTypeInsn(CHECKCAST, enumInternal);
        mv.visitMethodInsn(INVOKEVIRTUAL, enumInternal, "ordinal", "()I", false);

        int ordSlot = emitCtx.nextSlot();
        mv.visitVarInsn(ISTORE, ordSlot);

        var variants = ctx.enumVariants(enumOwner);
        var defaultLabel = new Label();
        var labels = new Label[variants.size()];
        for (int idx = 0; idx < labels.length; idx++) {
            labels[idx] = new Label();
        }
        var seen = new boolean[labels.length];

        mv.visitVarInsn(ILOAD, ordSlot);
        mv.visitTableSwitchInsn(0, labels.length - 1, defaultLabel, labels);

        for (var c : match.cases) {
            var variant = ((CoreModel.PatName) c.pattern).name;
            int caseIdx = variants.indexOf(variant);
            if (caseIdx < 0) continue;

            var target = labels[caseIdx];
            mv.visitLabel(target);
            mv.visitLineNumber(emitCtx.nextLineNo(), target);
            seen[caseIdx] = true;

            scopeStack.pushScope();
            int[] nextSlotBox = {emitCtx.peekNextSlot()};
            boolean returned = Main.emitCaseStmt(
                ctx,
                mv,
                c.body,
                emitCtx.getRetDesc(),
                emitCtx.getPkg(),
                0,
                emitCtx.getEnv(),
                scopeStack,
                typeResolver,
                emitCtx.getFnHints(),
                nextSlotBox,
                new java.util.concurrent.atomic.AtomicInteger(emitCtx.nextLineNo())
            );
            emitCtx.setNextSlot(nextSlotBox[0]);
            scopeStack.popScope();

            if (!returned) {
                mv.visitJumpInsn(GOTO, lEndMatch);
            }
        }

        for (int idx = 0; idx < labels.length; idx++) {
            if (!seen[idx]) {
                mv.visitLabel(labels[idx]);
                mv.visitJumpInsn(GOTO, lEndMatch);
            }
        }

        mv.visitLabel(defaultLabel);
        mv.visitJumpInsn(GOTO, lEndMatch);
        mv.visitLabel(lEndMatch);
        return true;
    }

    /**
     * 尝试优化为 Int Pattern TableSwitch/LookupSwitch。
     */
    private boolean tryEmitIntSwitch(MethodVisitor mv, CoreModel.Match match, int scrSlot,
                                    EmitContext emitCtx, ScopeStack scopeStack, Label lEndMatch) throws IOException {
        boolean allInts = match.cases.stream().allMatch(c -> c.pattern instanceof CoreModel.PatInt);
        boolean anyInt = match.cases.stream().anyMatch(c -> c.pattern instanceof CoreModel.PatInt);

        if (!anyInt) return false;

        // Case 1: 部分 Int Pattern + 单个 PatName default
        if (!allInts) {
            return tryEmitMixedIntSwitch(mv, match, scrSlot, emitCtx, scopeStack, lEndMatch);
        }

        // Case 2: 全部 Int Pattern
        return emitAllIntSwitch(mv, match, scrSlot, emitCtx, scopeStack, lEndMatch);
    }

    private boolean tryEmitMixedIntSwitch(MethodVisitor mv, CoreModel.Match match, int scrSlot,
                                         EmitContext emitCtx, ScopeStack scopeStack, Label lEndMatch) throws IOException {
        var nonIntCases = new ArrayList<CoreModel.Case>();
        for (var c : match.cases) {
            if (!(c.pattern instanceof CoreModel.PatInt)) {
                nonIntCases.add(c);
            }
        }

        if (nonIntCases.isEmpty() || nonIntCases.size() != 1
            || !(nonIntCases.get(0).pattern instanceof CoreModel.PatName)) {
            return false;
        }

        mv.visitVarInsn(ALOAD, scrSlot);
        mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int countInt = 0;

        for (var c : match.cases) {
            if (c.pattern instanceof CoreModel.PatInt pi) {
                if (pi.value < min) min = pi.value;
                if (pi.value > max) max = pi.value;
                countInt++;
            }
        }

        if (countInt == 0) return false;

        int span = max - min + 1;
        var defaultLabelInt = new Label();
        var endLabelInt = new Label();
        boolean usedEnd = false;

        if (span > 0 && span <= 6 * countInt) {
            // TableSwitch
            var labels = new Label[span];
            for (int idx = 0; idx < span; idx++) {
                labels[idx] = new Label();
            }
            mv.visitTableSwitchInsn(min, max, defaultLabelInt, labels);

            boolean[] seen = new boolean[span];
            for (var c : match.cases) {
                if (c.pattern instanceof CoreModel.PatInt pi) {
                    int idx = pi.value - min;
                    mv.visitLabel(labels[idx]);
                    seen[idx] = true;

                    var lCase = new Label();
                    mv.visitLabel(lCase);
                    mv.visitLineNumber(emitCtx.nextLineNo(), lCase);

                    scopeStack.pushScope();
                    try {
                        if (c.body instanceof CoreModel.Return rr) {
                            emitExpr(mv, rr.expr, emitCtx.getRetDesc(), emitCtx, scopeStack);
                            emitReturn(mv, emitCtx.getRetDesc());
                        } else if (c.body instanceof CoreModel.Block bb) {
                            for (var st2 : bb.statements) {
                                if (st2 instanceof CoreModel.Return r2) {
                                    emitExpr(mv, r2.expr, emitCtx.getRetDesc(), emitCtx, scopeStack);
                                    emitReturn(mv, emitCtx.getRetDesc());
                                }
                            }
                            mv.visitJumpInsn(GOTO, endLabelInt);
                            usedEnd = true;
                            continue;
                        }
                    } finally {
                        scopeStack.popScope();
                    }
                }
            }

            for (int idx = 0; idx < span; idx++) {
                if (!seen[idx]) {
                    mv.visitLabel(labels[idx]);
                    mv.visitJumpInsn(GOTO, defaultLabelInt);
                }
            }
        } else {
            // LookupSwitch
            int[] keys = new int[countInt];
            Label[] labels = new Label[countInt];
            int k = 0;

            for (var c : match.cases) {
                if (c.pattern instanceof CoreModel.PatInt pi) {
                    keys[k] = pi.value;
                    labels[k] = new Label();
                    k++;
                }
            }

            mv.visitLookupSwitchInsn(defaultLabelInt, keys, labels);
            k = 0;

            for (var c : match.cases) {
                if (c.pattern instanceof CoreModel.PatInt pi) {
                    mv.visitLabel(labels[k++]);

                    var lCase = new Label();
                    mv.visitLabel(lCase);
                    mv.visitLineNumber(emitCtx.nextLineNo(), lCase);

                    scopeStack.pushScope();
                    try {
                        if (c.body instanceof CoreModel.Return rr) {
                            emitExpr(mv, rr.expr, emitCtx.getRetDesc(), emitCtx, scopeStack);
                            emitReturn(mv, emitCtx.getRetDesc());
                        } else if (c.body instanceof CoreModel.Block bb) {
                            for (var st2 : bb.statements) {
                                if (st2 instanceof CoreModel.Return r2) {
                                    emitExpr(mv, r2.expr, emitCtx.getRetDesc(), emitCtx, scopeStack);
                                    emitReturn(mv, emitCtx.getRetDesc());
                                }
                            }
                            mv.visitJumpInsn(GOTO, endLabelInt);
                            usedEnd = true;
                            continue;
                        }
                    } finally {
                        scopeStack.popScope();
                    }
                }
            }
        }

        // Default case
        mv.visitLabel(defaultLabelInt);
        var lCaseDefault = new Label();
        mv.visitLabel(lCaseDefault);
        mv.visitLineNumber(emitCtx.nextLineNo(), lCaseDefault);

        var defaultCase = nonIntCases.get(0);
        scopeStack.pushScope();

        if (defaultCase.body instanceof CoreModel.Return rr) {
            emitExpr(mv, rr.expr, emitCtx.getRetDesc(), emitCtx, scopeStack);
            emitReturn(mv, emitCtx.getRetDesc());
        } else if (defaultCase.body instanceof CoreModel.Block bb) {
            for (var st2 : bb.statements) {
                if (st2 instanceof CoreModel.Return r2) {
                    emitExpr(mv, r2.expr, emitCtx.getRetDesc(), emitCtx, scopeStack);
                    emitReturn(mv, emitCtx.getRetDesc());
                }
            }
        }

        scopeStack.popScope();
        mv.visitLabel(endLabelInt);
        return true;
    }

    private boolean emitAllIntSwitch(MethodVisitor mv, CoreModel.Match match, int scrSlot,
                                    EmitContext emitCtx, ScopeStack scopeStack, Label lEndMatch) throws IOException {
        if (match.cases.isEmpty()) return false;

        mv.visitVarInsn(ALOAD, scrSlot);
        mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        for (var c : match.cases) {
            int value = ((CoreModel.PatInt) c.pattern).value;
            if (value < min) min = value;
            if (value > max) max = value;
        }

        int span = max - min + 1;
        var defaultLabelInt = new Label();
        var endLabelInt = new Label();
        boolean usedEnd = false;

        if (span > 0 && span <= 6 * match.cases.size()) {
            // TableSwitch
            var labels = new Label[span];
            for (int idx = 0; idx < span; idx++) {
                labels[idx] = new Label();
            }
            mv.visitTableSwitchInsn(min, max, defaultLabelInt, labels);

            for (var c : match.cases) {
                int idx = ((CoreModel.PatInt) c.pattern).value - min;
                mv.visitLabel(labels[idx]);

                var lCase = new Label();
                mv.visitLabel(lCase);
                mv.visitLineNumber(emitCtx.nextLineNo(), lCase);

                scopeStack.pushScope();
                try {
                    if (c.body instanceof CoreModel.Return rr) {
                        emitExpr(mv, rr.expr, emitCtx.getRetDesc(), emitCtx, scopeStack);
                        emitReturn(mv, emitCtx.getRetDesc());
                    } else if (c.body instanceof CoreModel.Block bb) {
                        for (var st2 : bb.statements) {
                            if (st2 instanceof CoreModel.Return r2) {
                                emitExpr(mv, r2.expr, emitCtx.getRetDesc(), emitCtx, scopeStack);
                                emitReturn(mv, emitCtx.getRetDesc());
                            }
                        }
                        mv.visitJumpInsn(GOTO, endLabelInt);
                        usedEnd = true;
                        continue;
                    }
                } finally {
                    scopeStack.popScope();
                }
            }

            mv.visitLabel(defaultLabelInt);
            if (usedEnd) mv.visitLabel(endLabelInt);
            return true;
        } else {
            // LookupSwitch
            int size = match.cases.size();
            int[] keys = new int[size];
            Label[] labels = new Label[size];

            for (int idx = 0; idx < size; idx++) {
                keys[idx] = ((CoreModel.PatInt) match.cases.get(idx).pattern).value;
                labels[idx] = new Label();
            }

            mv.visitLookupSwitchInsn(defaultLabelInt, keys, labels);

            for (int idx = 0; idx < size; idx++) {
                var c = match.cases.get(idx);
                mv.visitLabel(labels[idx]);

                var lCase = new Label();
                mv.visitLabel(lCase);
                mv.visitLineNumber(emitCtx.nextLineNo(), lCase);

                scopeStack.pushScope();
                try {
                    if (c.body instanceof CoreModel.Return rr) {
                        emitExpr(mv, rr.expr, emitCtx.getRetDesc(), emitCtx, scopeStack);
                        emitReturn(mv, emitCtx.getRetDesc());
                    } else if (c.body instanceof CoreModel.Block bb) {
                        for (var st2 : bb.statements) {
                            if (st2 instanceof CoreModel.Return r2) {
                                emitExpr(mv, r2.expr, emitCtx.getRetDesc(), emitCtx, scopeStack);
                                emitReturn(mv, emitCtx.getRetDesc());
                            }
                        }
                        mv.visitJumpInsn(GOTO, endLabelInt);
                        usedEnd = true;
                        continue;
                    }
                } finally {
                    scopeStack.popScope();
                }
            }

            mv.visitLabel(defaultLabelInt);
            if (usedEnd) mv.visitLabel(endLabelInt);
            return true;
        }
    }

    /**
     * 逐个匹配 Pattern（Fall-through 路径）。
     */
    private void emitSequentialMatching(MethodVisitor mv, CoreModel.Match match, int scrSlot,
                                       EmitContext emitCtx, ScopeStack scopeStack,
                                       List<FunctionEmitter.LV> lvars, Label lEndMatch) throws IOException {
        for (var caseItem : match.cases) {
            var lNext = new Label();

            if (caseItem.pattern instanceof CoreModel.PatNull) {
                emitPatNull(mv, caseItem, scrSlot, emitCtx, scopeStack, lNext);
            } else if (caseItem.pattern instanceof CoreModel.PatCtor pc) {
                emitPatCtor(mv, caseItem, pc, scrSlot, emitCtx, scopeStack, lvars, lNext);
            } else if (caseItem.pattern instanceof CoreModel.PatName pn) {
                emitPatName(mv, caseItem, pn, scrSlot, emitCtx, scopeStack, lvars, lNext);
            } else if (caseItem.pattern instanceof CoreModel.PatInt pi) {
                emitPatInt(mv, caseItem, pi, scrSlot, emitCtx, scopeStack, lNext);
            }
        }
    }

    private void emitPatNull(MethodVisitor mv, CoreModel.Case caseItem, int scrSlot,
                            EmitContext emitCtx, ScopeStack scopeStack, Label lNext) throws IOException {
        scopeStack.pushScope();
        mv.visitVarInsn(ALOAD, scrSlot);
        mv.visitJumpInsn(IFNONNULL, lNext);

        var lCase = new Label();
        mv.visitLabel(lCase);
        mv.visitLineNumber(emitCtx.nextLineNo(), lCase);

        if (caseItem.body instanceof CoreModel.Return rr) {
            emitExpr(mv, rr.expr, emitCtx.getRetDesc(), emitCtx, scopeStack);
            emitReturn(mv, emitCtx.getRetDesc());
        }

        scopeStack.popScope();
        mv.visitLabel(lNext);
    }

    private void emitPatCtor(MethodVisitor mv, CoreModel.Case caseItem, CoreModel.PatCtor pc, int scrSlot,
                            EmitContext emitCtx, ScopeStack scopeStack, List<FunctionEmitter.LV> lvars, Label lNext) throws IOException {
        var targetInternal = Main.resolveTypeInternalName(emitCtx.getPkg(), pc.typeName);

        mv.visitVarInsn(ALOAD, scrSlot);
        mv.visitTypeInsn(INSTANCEOF, targetInternal);
        mv.visitJumpInsn(IFEQ, lNext);

        var lCase = new Label();
        mv.visitLabel(lCase);
        mv.visitLineNumber(emitCtx.nextLineNo(), lCase);

        scopeStack.pushScope();
        mv.visitVarInsn(ALOAD, scrSlot);
        mv.visitTypeInsn(CHECKCAST, targetInternal);

        int objSlot = emitCtx.nextSlot();
        mv.visitVarInsn(ASTORE, objSlot);

        var data = ctx.lookupData(pc.typeName);
        if (data != null && pc.names != null) {
            for (int idx = 0; idx < Math.min(pc.names.size(), data.fields.size()); idx++) {
                var bindName = pc.names.get(idx);
                if (bindName == null || bindName.isEmpty() || "_".equals(bindName)) continue;

                mv.visitVarInsn(ALOAD, objSlot);
                var field = data.fields.get(idx);
                var fieldDesc = Main.jDesc(emitCtx.getPkg(), field.type);
                Main.loadDataField(mv, targetInternal, field.name, fieldDesc);

                int slot = emitCtx.nextSlot();
                var fieldKind = Main.kindForDescriptor(fieldDesc);

                switch (fieldKind) {
                    case DOUBLE -> {
                        mv.visitVarInsn(DSTORE, slot);
                        fieldDesc = "D";
                    }
                    case LONG -> {
                        mv.visitVarInsn(LSTORE, slot);
                        fieldDesc = "J";
                    }
                    case INT -> {
                        mv.visitVarInsn(ISTORE, slot);
                        fieldDesc = "I";
                    }
                    case BOOLEAN -> {
                        mv.visitVarInsn(ISTORE, slot);
                        fieldDesc = "Z";
                    }
                    default -> mv.visitVarInsn(ASTORE, slot);
                }

                emitCtx.putVar(bindName, slot);
                scopeStack.declare(bindName, slot, fieldDesc, fieldKind);
            }
        }

        if (caseItem.body instanceof CoreModel.Return rr) {
            emitExpr(mv, rr.expr, emitCtx.getRetDesc(), emitCtx, scopeStack);
            emitReturn(mv, emitCtx.getRetDesc());
        }

        scopeStack.popScope();
        mv.visitLabel(lNext);
    }

    private void emitPatName(MethodVisitor mv, CoreModel.Case caseItem, CoreModel.PatName pn, int scrSlot,
                            EmitContext emitCtx, ScopeStack scopeStack, List<FunctionEmitter.LV> lvars, Label lNext) throws IOException {
        var variant = pn.name;
        var enumName = ctx.enumOwner(variant);
        scopeStack.pushScope();

        if (enumName != null) {
            var enumInternal = Main.resolveTypeInternalName(emitCtx.getPkg(), enumName);

            mv.visitVarInsn(ALOAD, scrSlot);
            mv.visitFieldInsn(GETSTATIC, enumInternal, variant, Main.internalDesc(enumInternal));
            mv.visitJumpInsn(IF_ACMPNE, lNext);

            var lCase = new Label();
            mv.visitLabel(lCase);
            mv.visitLineNumber(emitCtx.nextLineNo(), lCase);

            if (caseItem.body instanceof CoreModel.Return rr) {
                emitExpr(mv, rr.expr, emitCtx.getRetDesc(), emitCtx, scopeStack);
                emitReturn(mv, emitCtx.getRetDesc());
            }
        } else {
            if (variant != null && !variant.isEmpty() && !"_".equals(variant)) {
                int bindSlot = emitCtx.nextSlot();
                mv.visitVarInsn(ALOAD, scrSlot);
                mv.visitVarInsn(ASTORE, bindSlot);
                emitCtx.putVar(variant, bindSlot);
                lvars.add(new FunctionEmitter.LV(variant, "Ljava/lang/Object;", bindSlot));
                scopeStack.declare(variant, bindSlot, "Ljava/lang/Object;", ScopeStack.JvmKind.OBJECT);
            }

            if (caseItem.body instanceof CoreModel.Return rr) {
                emitExpr(mv, rr.expr, emitCtx.getRetDesc(), emitCtx, scopeStack);
                emitReturn(mv, emitCtx.getRetDesc());
            }
        }

        scopeStack.popScope();
        mv.visitLabel(lNext);
    }

    private void emitPatInt(MethodVisitor mv, CoreModel.Case caseItem, CoreModel.PatInt pi, int scrSlot,
                           EmitContext emitCtx, ScopeStack scopeStack, Label lNext) throws IOException {
        scopeStack.pushScope();
        mv.visitVarInsn(ALOAD, scrSlot);
        mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
        AsmUtilities.emitConstInt(mv, pi.value);
        mv.visitJumpInsn(IF_ICMPNE, lNext);

        var lCase = new Label();
        mv.visitLabel(lCase);
        mv.visitLineNumber(emitCtx.nextLineNo(), lCase);

        if (caseItem.body instanceof CoreModel.Return rr) {
            emitExpr(mv, rr.expr, emitCtx.getRetDesc(), emitCtx, scopeStack);
            emitReturn(mv, emitCtx.getRetDesc());
        }

        scopeStack.popScope();
        mv.visitLabel(lNext);
    }

    // ========== 辅助方法 ==========

    private void emitExpr(MethodVisitor mv, CoreModel.Expr expr, String expectedDesc,
                         EmitContext emitCtx, ScopeStack scopeStack) throws IOException {
        if (isLiteral(expr)) {
            expressionEmitter.emitExpression(expr, mv, scopeStack, expectedDesc);
        } else {
            Main.emitExpr(ctx, mv, expr, expectedDesc, emitCtx.getPkg(), 0,
                emitCtx.getEnv(), scopeStack, typeResolver);
        }
    }

    private static boolean isLiteral(CoreModel.Expr expr) {
        return expr instanceof CoreModel.IntE
            || expr instanceof CoreModel.Bool
            || expr instanceof CoreModel.LongE
            || expr instanceof CoreModel.DoubleE
            || expr instanceof CoreModel.StringE
            || expr instanceof CoreModel.NullE;
    }

    private void emitReturn(MethodVisitor mv, String retDesc) {
        if (retDesc.equals("I") || retDesc.equals("Z")) {
            mv.visitInsn(IRETURN);
        } else {
            mv.visitInsn(ARETURN);
        }
    }
}
