package aster.emitter;

import org.objectweb.asm.*;
import java.util.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * PatMatchEmitter - 递归模式匹配和变量绑定字节码生成器
 *
 * 职责：
 * 1. 处理3种模式：PatNull（null检查）、PatName（变量绑定）、PatCtor（构造器匹配）
 * 2. 支持自递归调用（Ok/Err 和 Data 嵌套模式）
 * 3. 原始类型支持（int, boolean）及 boxing/unboxing
 *
 * 适用场景：
 * - Lambda apply 上下文中的模式匹配（emitApplyPatMatchAndBind）
 * - 函数体 Match 表达式中的模式匹配（FuncMatchEmitter）
 *
 * 字节码模式：
 * <pre>
 * - PatNull: ALOAD valSlot, IFNONNULL failLabel
 * - PatName: ALOAD valSlot, ASTORE slot, env.put(name, slot)
 * - PatCtor: INSTANCEOF targetInternal, IFEQ failLabel, CHECKCAST, GETFIELD, 递归 emitPatMatch
 * </pre>
 */
final class PatMatchEmitter {
    /**
     * Data 定义查找回调接口
     */
    @FunctionalInterface
    interface DataLookup {
        /**
         * 查找 Data 定义
         *
         * @param typeName 类型名称（例如 "User", "demo.Person"）
         * @return Data 定义，如果类型不存在或不是 Data 类型则返回 null
         */
        CoreModel.Data lookupData(String typeName);
    }

    /**
     * 生成递归模式匹配和变量绑定字节码
     *
     * 支持3种模式：
     * 1. PatNull：null 检查（ALOAD + IFNONNULL）
     * 2. PatName：变量绑定（ALOAD + ASTORE + env.put）
     * 3. PatCtor：构造器匹配（INSTANCEOF + CHECKCAST + GETFIELD + 递归）
     *
     * 递归调用场景：
     * - Ok/Err 单个 positional field 匹配
     * - Data 嵌套模式匹配
     *
     * @param mv MethodVisitor
     * @param pat 模式节点（PatNull, PatName, PatCtor）
     * @param valSlot 值所在局部变量 slot
     * @param ownerInternal 所有者类型内部名称（用于包路径推导）
     * @param env 局部变量环境（变量名 → slot 映射）
     * @param primTypes 原始类型提示（变量名 → 类型字符 'I'/'Z'）
     * @param failLabel 匹配失败跳转标签
     * @param dataLookup Data 定义查找回调
     */
    static void emitPatMatch(
        MethodVisitor mv,
        CoreModel.Pattern pat,
        int valSlot,
        String ownerInternal,
        Map<String, Integer> env,
        Map<String, Character> primTypes,
        Label failLabel,
        DataLookup dataLookup
    ) {
        // 1. PatNull 处理：null 检查
        if (pat instanceof CoreModel.PatNull) {
            mv.visitVarInsn(ALOAD, valSlot);
            mv.visitJumpInsn(IFNONNULL, failLabel);
            return;
        }

        // 2. PatName 处理：变量绑定
        if (pat instanceof CoreModel.PatName pn) {
            String name = pn.name;
            if (!(name == null || name.isEmpty() || "_".equals(name))) {
                int slot = Main.nextLocal(env);
                mv.visitVarInsn(ALOAD, valSlot);
                mv.visitVarInsn(ASTORE, slot);
                env.put(name, slot);
            }
            return;
        }

        // 3. PatCtor 处理：构造器递归匹配
        if (pat instanceof CoreModel.PatCtor pc) {
            // 3.1 构建路径和类型检查
            String pkgPath = ownerInternal.contains("/")
                ? ownerInternal.substring(0, ownerInternal.lastIndexOf('/'))
                : "";
            boolean isOk = java.util.Objects.equals(pc.typeName, "Ok");
            boolean isErr = java.util.Objects.equals(pc.typeName, "Err");
            String targetInternal = isOk ? "aster/runtime/Ok"
                : (isErr ? "aster/runtime/Err"
                    : (pc.typeName.contains(".")
                        ? pc.typeName.replace('.', '/')
                        : (pkgPath.isEmpty() ? pc.typeName : pkgPath + "/" + pc.typeName)));

            // 3.2 instanceof 检查和类型转换
            mv.visitVarInsn(ALOAD, valSlot);
            mv.visitTypeInsn(INSTANCEOF, targetInternal);
            mv.visitJumpInsn(IFEQ, failLabel);
            mv.visitVarInsn(ALOAD, valSlot);
            mv.visitTypeInsn(CHECKCAST, targetInternal);
            int objSlot = Main.nextLocal(env);
            mv.visitVarInsn(ASTORE, objSlot);

            // 3.3 Ok/Err 特殊处理（单个 positional field）
            if (isOk || isErr) {
                CoreModel.Pattern child = null;
                if (pc.args != null && !pc.args.isEmpty()) {
                    child = pc.args.get(0);
                } else if (pc.names != null && !pc.names.isEmpty()) {
                    var tmp = new CoreModel.PatName();
                    tmp.name = pc.names.get(0);
                    child = tmp;
                }
                if (child != null) {
                    mv.visitVarInsn(ALOAD, objSlot);
                    String field = isOk ? "value" : "error";
                    mv.visitFieldInsn(GETFIELD, targetInternal, field, "Ljava/lang/Object;");
                    int sub = Main.nextLocal(env);
                    mv.visitVarInsn(ASTORE, sub);
                    // 递归调用
                    emitPatMatch(mv, child, sub, ownerInternal, env, primTypes, failLabel, dataLookup);
                }
                return;
            }

            // 3.4 Data 构造器通用处理（按字段顺序绑定）
            var data = dataLookup.lookupData(pc.typeName);
            int arity = 0;
            if (pc.args != null) arity = pc.args.size();
            else if (pc.names != null) arity = pc.names.size();

            for (int i = 0; i < arity; i++) {
                CoreModel.Pattern child = null;
                if (pc.args != null && i < pc.args.size()) {
                    child = pc.args.get(i);
                } else if (pc.names != null && i < pc.names.size()) {
                    var tmp = new CoreModel.PatName();
                    tmp.name = pc.names.get(i);
                    child = tmp;
                }
                if (child == null) continue;

                // 字段名和类型描述符
                String fieldName = "f" + i; // fallback
                String fDesc = "Ljava/lang/Object;";
                if (data != null && data.fields != null && i < data.fields.size()) {
                    var f = data.fields.get(i);
                    fieldName = f.name;
                    fDesc = Main.jDesc(Main.internalToPkg(ownerInternal), f.type);
                }

                // 3.4.1 PatName 直接绑定（支持原始类型 int/boolean）
                if (child instanceof CoreModel.PatName pn) {
                    String bind = pn.name;
                    if (!(bind == null || bind.isEmpty() || "_".equals(bind))) {
                        if ("I".equals(fDesc)) {
                            // int 类型
                            mv.visitVarInsn(ALOAD, objSlot);
                            mv.visitFieldInsn(GETFIELD, targetInternal, fieldName, fDesc);
                            int slotI = Main.nextLocal(env);
                            mv.visitVarInsn(ISTORE, slotI);
                            env.put(bind, slotI);
                            if (primTypes != null) primTypes.put(bind, 'I');
                        } else if ("Z".equals(fDesc)) {
                            // boolean 类型
                            mv.visitVarInsn(ALOAD, objSlot);
                            mv.visitFieldInsn(GETFIELD, targetInternal, fieldName, fDesc);
                            int slotZ = Main.nextLocal(env);
                            mv.visitVarInsn(ISTORE, slotZ);
                            env.put(bind, slotZ);
                            if (primTypes != null) primTypes.put(bind, 'Z');
                        } else {
                            // 对象类型
                            mv.visitVarInsn(ALOAD, objSlot);
                            mv.visitFieldInsn(GETFIELD, targetInternal, fieldName, fDesc);
                            int slotO = Main.nextLocal(env);
                            mv.visitVarInsn(ASTORE, slotO);
                            env.put(bind, slotO);
                        }
                    }
                } else {
                    // 3.4.2 嵌套模式：原始类型 boxing 后递归
                    mv.visitVarInsn(ALOAD, objSlot);
                    mv.visitFieldInsn(GETFIELD, targetInternal, fieldName, fDesc);
                    if ("I".equals(fDesc)) {
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                    } else if ("Z".equals(fDesc)) {
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                    }
                    int sub = Main.nextLocal(env);
                    mv.visitVarInsn(ASTORE, sub);
                    // 递归调用
                    emitPatMatch(mv, child, sub, ownerInternal, env, primTypes, failLabel, dataLookup);
                }
            }
            return;
        }

        // Unknown pattern kind
        mv.visitJumpInsn(GOTO, failLabel);
    }
}
