package aster.emitter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * ScopeStack负责记录局部变量的槽位与JVM类型信息，后续会逐步替换基于名称的原生类型集合。
 * 目前仅用于在emitFunc阶段并行采集数据，不影响既有生成逻辑。
 */
final class ScopeStack {

  private final Deque<ScopeFrame> frames = new ArrayDeque<>();
  private final Map<String, Deque<TypedLocal>> byName = new HashMap<>();
  private final Map<Integer, TypedLocal> bySlot = new HashMap<>();

  ScopeStack() {
    frames.addLast(new ScopeFrame(0));
  }

  ScopeFrame pushScope() {
    var frame = new ScopeFrame(frames.size());
    frames.addLast(frame);
    return frame;
  }

  void popScope() {
    if (frames.size() <= 1) {
      throw new IllegalStateException("无法弹出根作用域");
    }
    var frame = frames.removeLast();
    for (var local : frame.ordered) {
      var stack = byName.get(local.name());
      if (stack != null) {
        stack.removeFirstOccurrence(local);
        if (stack.isEmpty()) {
          byName.remove(local.name());
        }
      }
      bySlot.remove(local.slot());
    }
  }

  TypedLocal declare(String name, int slot, String descriptor, JvmKind kind) {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(descriptor, "descriptor");
    Objects.requireNonNull(kind, "kind");
    var frame = frames.peekLast();
    if (frame == null) {
      throw new IllegalStateException("缺少当前作用域帧");
    }
    // 同一作用域内重复声明时，先移除旧绑定
    var existing = frame.byName.remove(name);
    if (existing != null) {
      var stack = byName.get(name);
      if (stack != null) {
        stack.removeFirstOccurrence(existing);
        if (stack.isEmpty()) {
          byName.remove(name);
        }
      }
      bySlot.remove(existing.slot());
      frame.ordered.remove(existing);
    }
    var local = new TypedLocal(name, slot, descriptor, kind, frame.depth);
    frame.byName.put(name, local);
    frame.ordered.add(local);
    bySlot.put(slot, local);
    byName.computeIfAbsent(name, k -> new ArrayDeque<>()).addFirst(local);
    return local;
  }

  Optional<TypedLocal> lookup(String name) {
    var stack = byName.get(name);
    if (stack == null || stack.isEmpty()) return Optional.empty();
    return Optional.ofNullable(stack.peekFirst());
  }

  Optional<TypedLocal> lookup(int slot) {
    return Optional.ofNullable(bySlot.get(slot));
  }

  Character getType(String name) {
    return lookup(name).map(local -> kindToChar(local.kind())).orElse(null);
  }

  Character getType(int slot) {
    return lookup(slot).map(local -> kindToChar(local.kind())).orElse(null);
  }

  String getDescriptor(String name) {
    return lookup(name).map(TypedLocal::descriptor).orElse(null);
  }

  String getDescriptor(int slot) {
    return lookup(slot).map(TypedLocal::descriptor).orElse(null);
  }

  int depth() {
    return frames.size() - 1;
  }

  List<TypedLocal> snapshotCurrentScope() {
    var frame = frames.peekLast();
    if (frame == null) return List.of();
    return List.copyOf(frame.ordered);
  }

  List<TypedLocal> snapshotAll() {
    var ordered = new ArrayList<TypedLocal>();
    for (var frame : frames) {
      ordered.addAll(frame.ordered);
    }
    return List.copyOf(ordered);
  }

  public List<TypedLocal> getAllVariables() {
    return snapshotAll();
  }

  public String dump() {
    var sb = new StringBuilder();
    sb.append("ScopeStack{depth=").append(depth()).append(", frames=[");
    int idx = 0;
    for (var it = frames.iterator(); it.hasNext(); ) {
      var frame = it.next();
      if (idx > 0) sb.append(", ");
      sb.append("{depth=").append(frame.depth).append(", locals=[");
      for (int i = 0; i < frame.ordered.size(); i++) {
        var local = frame.ordered.get(i);
        if (i > 0) sb.append(", ");
        sb.append(local.name()).append('@').append(local.slot()).append(':').append(local.descriptor());
      }
      sb.append("]}");
      idx++;
    }
    sb.append("]}");
    return sb.toString();
  }

  enum JvmKind {
    INT("I"),
    LONG("J"),
    DOUBLE("D"),
    BOOLEAN("Z"),
    OBJECT("Ljava/lang/Object;"),
    UNKNOWN("");

    private static final EnumSet<JvmKind> PRIMITIVES = EnumSet.of(INT, LONG, DOUBLE, BOOLEAN);
    private final String descriptorHint;

    JvmKind(String descriptorHint) {
      this.descriptorHint = descriptorHint;
    }

    boolean isPrimitive() {
      return PRIMITIVES.contains(this);
    }

    String descriptorHint() {
      return descriptorHint;
    }
  }

  record TypedLocal(String name, int slot, String descriptor, JvmKind kind, int depth) {}

  private static final class ScopeFrame {
    private final int depth;
    private final Map<String, TypedLocal> byName = new HashMap<>();
    private final List<TypedLocal> ordered = new ArrayList<>();

    private ScopeFrame(int depth) {
      this.depth = depth;
    }
  }

  private static Character kindToChar(JvmKind kind) {
    return switch (kind) {
      case INT -> 'I';
      case BOOLEAN -> 'Z';
      case LONG -> 'J';
      case DOUBLE -> 'D';
      default -> null;
    };
  }
}
