package io.quarkus.logging;

/**
 * Quarkus Log 的 Stub 实现，用于 JMH 基准测试环境
 *
 * JMH 运行在独立 JVM 中，没有 Quarkus 字节码转换支持。
 * 此 stub 提供所有日志方法的空实现（no-op），避免 UnsupportedOperationException。
 *
 * 生产环境仍使用 Quarkus 原生 Log，不受影响。
 */
public final class Log {

    private Log() {
        // Utility class - 禁止实例化
    }

    // ======== INFO 级别 ========

    public static void info(Object message) {
        // No-op for JMH
    }

    public static void infof(String format, Object... params) {
        // No-op for JMH
    }

    public static void infof(String format, Object param1) {
        // No-op for JMH - 特定重载
    }

    public static void infof(String format, Object param1, Object param2) {
        // No-op for JMH - 特定重载
    }

    public static void infof(String format, Object param1, Object param2, Object param3) {
        // No-op for JMH - 特定重载
    }

    public static void infov(String format, Object... params) {
        // No-op for JMH
    }

    // ======== WARN 级别 ========

    public static void warn(Object message) {
        // No-op for JMH
    }

    public static void warnf(String format, Object... params) {
        // No-op for JMH
    }

    public static void warnf(String format, Object param1) {
        // No-op for JMH - 特定重载
    }

    public static void warnf(String format, Object param1, Object param2) {
        // No-op for JMH - 特定重载
    }

    public static void warnf(Throwable t, String format, Object... params) {
        // No-op for JMH
    }

    public static void warnf(Throwable t, String format, Object param1) {
        // No-op for JMH - 特定重载
    }

    public static void warnf(Throwable t, String format, Object param1, Object param2) {
        // No-op for JMH - 特定重载
    }

    public static void warnv(String format, Object... params) {
        // No-op for JMH
    }

    public static void warnv(Throwable t, String format, Object... params) {
        // No-op for JMH
    }

    // ======== DEBUG 级别 ========

    public static void debug(Object message) {
        // No-op for JMH
    }

    public static void debugf(String format, Object... params) {
        // No-op for JMH
    }

    public static void debugf(String format, Object param1) {
        // No-op for JMH - 特定重载
    }

    public static void debugf(String format, Object param1, Object param2) {
        // No-op for JMH - 特定重载
    }

    public static void debugv(String format, Object... params) {
        // No-op for JMH
    }

    // ======== ERROR 级别 ========

    public static void error(Object message) {
        // No-op for JMH
    }

    public static void errorf(String format, Object... params) {
        // No-op for JMH
    }

    public static void errorf(String format, Object param1) {
        // No-op for JMH - 特定重载，处理单参数情况
    }

    public static void errorf(String format, Object param1, Object param2) {
        // No-op for JMH - 特定重载，处理双参数情况
    }

    public static void errorf(Throwable t, String format, Object... params) {
        // No-op for JMH
    }

    public static void errorf(Throwable t, String format, Object param1) {
        // No-op for JMH - 特定重载，处理 Throwable + 单参数情况
    }

    public static void errorf(Throwable t, String format, Object param1, Object param2) {
        // No-op for JMH - 特定重载，处理 Throwable + 双参数情况
    }

    public static void errorv(String format, Object... params) {
        // No-op for JMH
    }

    public static void errorv(Throwable t, String format, Object... params) {
        // No-op for JMH
    }

    // ======== TRACE 级别 ========

    public static void trace(Object message) {
        // No-op for JMH
    }

    public static void tracef(String format, Object... params) {
        // No-op for JMH
    }

    public static void tracev(String format, Object... params) {
        // No-op for JMH
    }

    // ======== FATAL 级别 ========

    public static void fatal(Object message) {
        // No-op for JMH
    }

    public static void fatalf(String format, Object... params) {
        // No-op for JMH
    }

    public static void fatalv(String format, Object... params) {
        // No-op for JMH
    }

    // ======== 日志级别检查 ========

    public static boolean isInfoEnabled() {
        return false;  // JMH 环境关闭所有日志
    }

    public static boolean isWarnEnabled() {
        return false;
    }

    public static boolean isDebugEnabled() {
        return false;
    }

    public static boolean isTraceEnabled() {
        return false;
    }

    public static boolean isErrorEnabled() {
        return false;
    }

    public static boolean isFatalEnabled() {
        return false;
    }

    // ======== Fail 方法（原始实现抛异常，JMH 中保持相同行为）========

    public static RuntimeException fail() {
        throw new UnsupportedOperationException(
            "Using io.quarkus.logging.Log is only possible with Quarkus bytecode transformation");
    }
}
