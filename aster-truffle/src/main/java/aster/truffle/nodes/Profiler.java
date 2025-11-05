package aster.truffle.nodes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Profiler {
  private static final Map<String, Long> COUNTERS = new ConcurrentHashMap<>();
  private static final boolean ENABLED = Boolean.getBoolean("aster.profiler.enabled");
  private Profiler() {}
  public static void inc(String key) {
    if (!ENABLED) {
      return;
    }
    COUNTERS.merge(key, 1L, Long::sum);
    COUNTERS.merge("total", 1L, Long::sum);
  }
  public static String dump() {
    var sb = new StringBuilder();
    sb.append("Truffle profile (counts):\n");
    COUNTERS.forEach((k,v) -> sb.append(k).append(": ").append(v).append('\n'));
    return sb.toString();
  }
}
