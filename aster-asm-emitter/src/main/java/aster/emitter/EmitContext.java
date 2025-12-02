package aster.emitter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 封装函数字节码生成过程中的可变上下文状态。
 *
 * 职责：
 * - 管理局部变量环境（变量名 -> slot 映射）
 * - 维护下一个可用的 slot 索引
 * - 跟踪当前行号
 * - 记录包名和返回类型描述符
 *
 * 设计原则：
 * - 集中管理可变状态，减少参数传递
 * - 提供清晰的读写接口
 * - 支持作用域嵌套（通过 ScopeStack 配合）
 */
public class EmitContext {
    private final Map<String, Integer> env;
    private int nextSlot;
    private final AtomicInteger lineNo;
    private final String pkg;
    private final String retDesc;
    private final java.util.Map<String, Character> fnHints;

    /**
     * 创建发射上下文。
     *
     * @param initialSlot 初始 slot 索引（通常是参数数量）
     * @param pkg 包名
     * @param retDesc 返回类型描述符
     */
    public EmitContext(int initialSlot, String pkg, String retDesc, java.util.Map<String, Character> fnHints) {
        this.env = new LinkedHashMap<>();
        this.nextSlot = initialSlot;
        this.lineNo = new AtomicInteger(2); // 行号从 2 开始
        this.pkg = pkg;
        this.retDesc = retDesc;
        this.fnHints = fnHints;
    }

    // ========== 环境管理 ==========

    public Map<String, Integer> getEnv() {
        return env;
    }

    public void putVar(String name, int slot) {
        env.put(name, slot);
    }

    public Integer getVar(String name) {
        return env.get(name);
    }

    public boolean hasVar(String name) {
        return env.containsKey(name);
    }

    // ========== Slot 管理 ==========

    public int nextSlot() {
        return nextSlot++;
    }

    public int peekNextSlot() {
        return nextSlot;
    }

    public void setNextSlot(int slot) {
        this.nextSlot = slot;
    }

    // ========== 行号管理 ==========

    public int nextLineNo() {
        return lineNo.getAndIncrement();
    }

    public void setLineNo(int line) {
        lineNo.set(line);
    }

    // ========== 只读属性 ==========

    public String getPkg() {
        return pkg;
    }

    public String getRetDesc() {
        return retDesc;
    }

    public java.util.Map<String, Character> getFnHints() {
        return fnHints;
    }
}
