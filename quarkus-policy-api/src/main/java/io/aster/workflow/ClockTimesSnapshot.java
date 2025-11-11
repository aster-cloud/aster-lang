package io.aster.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ReplayDeterministicClock 时间序列快照（Phase 3.6）
 *
 * 用于持久化到 workflow_state.clock_times JSONB 列，支持确定性时间重放。
 *
 * <p>数据结构：
 * <pre>
 * {
 *   "recordedTimes": ["2025-01-10T08:00:00Z", "2025-01-10T08:00:01.500Z", ...],
 *   "replayIndex": 0,
 *   "replayMode": false,
 *   "version": 1
 * }
 * </pre>
 *
 * @see ReplayDeterministicClock
 * @see WorkflowStateEntity#clockTimes
 */
public class ClockTimesSnapshot {

    /**
     * 容量上限：500 条时间记录
     * 基于性能评估：500 * 30 bytes ≈ 15KB
     */
    private static final int MAX_RECORDS = 500;

    /**
     * 记录的时间决策序列
     * 在正常执行模式下，每次调用 clock.now() 都会追加一个 Instant
     */
    @JsonProperty("recordedTimes")
    public List<Instant> recordedTimes = new ArrayList<>();

    /**
     * 重放索引
     * 指示当前重放到第几个时间记录（0-based）
     */
    @JsonProperty("replayIndex")
    public int replayIndex = 0;

    /**
     * 重放模式标志
     * true 表示当前处于重放模式，clock.now() 从 recordedTimes 中读取而非系统时间
     */
    @JsonProperty("replayMode")
    public boolean replayMode = false;

    /**
     * 数据结构版本号
     * 用于未来扩展时的兼容性判断
     */
    @JsonProperty("version")
    public int version = 1;

    /**
     * 无参构造函数（Jackson 反序列化需要）
     */
    public ClockTimesSnapshot() {
    }

    /**
     * 带参构造函数，创建时间快照
     *
     * @param recordedTimes 记录的时间序列
     * @param replayIndex 重放索引
     * @param replayMode 是否处于重放模式
     * @throws IllegalArgumentException 如果 recordedTimes 超出容量上限
     */
    public ClockTimesSnapshot(List<Instant> recordedTimes, int replayIndex, boolean replayMode) {
        if (recordedTimes.size() > MAX_RECORDS) {
            throw new IllegalArgumentException(
                "recordedTimes 超出容量上限: " + MAX_RECORDS +
                ", 实际: " + recordedTimes.size()
            );
        }
        this.recordedTimes = new ArrayList<>(recordedTimes); // 防御性拷贝
        this.replayIndex = replayIndex;
        this.replayMode = replayMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClockTimesSnapshot that = (ClockTimesSnapshot) o;
        return replayIndex == that.replayIndex &&
               replayMode == that.replayMode &&
               version == that.version &&
               Objects.equals(recordedTimes, that.recordedTimes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordedTimes, replayIndex, replayMode, version);
    }

    @Override
    public String toString() {
        return "ClockTimesSnapshot{" +
               "recordedTimes=" + recordedTimes.size() + " entries" +
               ", replayIndex=" + replayIndex +
               ", replayMode=" + replayMode +
               ", version=" + version +
               '}';
    }
}
