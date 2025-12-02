package aster.emitter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.Printer;

/**
 * 提供结构化的字节码比较能力，忽略调试信息但确保类与方法的真实行为保持一致。
 */
final class BytecodeComparator {

  private BytecodeComparator() {
  }

  /**
   * 比较两份字节码是否等价，发现差异时抛出断言错误并提供详细上下文。
   */
  static void assertEquals(byte[] expectedBytes, byte[] actualBytes) {
    try {
      ClassNode expected = readClass(expectedBytes);
      ClassNode actual = readClass(actualBytes);
      compareClassStructure(expected, actual);
    } catch (AssertionError ex) {
      throw ex;
    } catch (Exception ex) {
      Assertions.fail("比较字节码时出现异常: " + ex.getMessage(), ex);
    }
  }

  /**
   * 对比类级别结构，包括基本元数据、字段/方法/内部类列表。
   */
  static void compareClassStructure(ClassNode expected, ClassNode actual) {
    Assertions.assertEquals(expected.version, actual.version, () -> formatClassDiff(expected, actual, "字节码版本不一致"));
    Assertions.assertEquals(expected.access, actual.access, () -> formatClassDiff(expected, actual, "类访问标志不一致"));
    Assertions.assertEquals(expected.name, actual.name, () -> formatClassDiff(expected, actual, "类名不一致"));
    Assertions.assertEquals(expected.superName, actual.superName, () -> formatClassDiff(expected, actual, "父类不一致"));
    Assertions.assertEquals(
      new LinkedHashSet<>(expected.interfaces),
      new LinkedHashSet<>(actual.interfaces),
      () -> formatClassDiff(expected, actual, "接口集合不一致")
    );
    Assertions.assertEquals(expected.signature, actual.signature, () -> formatClassDiff(expected, actual, "泛型签名不一致"));
    compareAnnotations(expected.invisibleAnnotations, actual.invisibleAnnotations, () -> diffPrefix(expected, "类不可见注解不一致"));
    compareAnnotations(expected.visibleAnnotations, actual.visibleAnnotations, () -> diffPrefix(expected, "类可见注解不一致"));
    compareFields(expected, actual);
    compareMethods(expected, actual);
    compareInnerClasses(expected, actual);
  }

  /**
   * 对比方法结构和指令序列。
   */
  static void compareMethod(String owner, MethodNode expected, MethodNode actual) {
    String context = owner + "#" + expected.name + expected.desc;
    Assertions.assertEquals(expected.access, actual.access, () -> context + " 的访问标志不同");
    Assertions.assertEquals(expected.signature, actual.signature, () -> context + " 的泛型签名不同");
    Assertions.assertEquals(expected.exceptions, actual.exceptions, () -> context + " 的异常声明不同");
    Assertions.assertEquals(expected.maxLocals, actual.maxLocals, () -> context + " 的局部变量槽数量不同");
    Assertions.assertEquals(expected.maxStack, actual.maxStack, () -> context + " 的操作数栈深度不同");
    compareAnnotations(expected.visibleAnnotations, actual.visibleAnnotations, () -> context + " 的可见注解不同");
    compareAnnotations(expected.invisibleAnnotations, actual.invisibleAnnotations, () -> context + " 的不可见注解不同");

    List<String> expectedTryCatch = normalizeTryCatch(expected.tryCatchBlocks);
    List<String> actualTryCatch = normalizeTryCatch(actual.tryCatchBlocks);
    Assertions.assertEquals(expectedTryCatch, actualTryCatch, () -> context + " 的异常处理块结构不同:\n期望: " + expectedTryCatch + "\n实际:  " + actualTryCatch);

    List<String> expectedInstructions = normalizeInstructions(expected.instructions);
    List<String> actualInstructions = normalizeInstructions(actual.instructions);
    int max = Math.max(expectedInstructions.size(), actualInstructions.size());
    for (int i = 0; i < max; i++) {
      String left = i < expectedInstructions.size() ? expectedInstructions.get(i) : "<END>";
      String right = i < actualInstructions.size() ? actualInstructions.get(i) : "<END>";
      if (!Objects.equals(left, right)) {
        String message = context + " 的指令序列在索引 " + i + " 处不同:\n期望: " + left + "\n实际:  " + right + "\n完整期望: " + expectedInstructions + "\n完整实际:  " + actualInstructions;
        Assertions.fail(message);
      }
    }
  }

  private static ClassNode readClass(byte[] bytes) {
    ClassReader reader = new ClassReader(bytes);
    ClassNode node = new ClassNode();
    reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    return node;
  }

  private static void compareFields(ClassNode expected, ClassNode actual) {
    Map<String, FieldNode> expectedFields = indexFields(expected.fields);
    Map<String, FieldNode> actualFields = indexFields(actual.fields);
    Assertions.assertEquals(expectedFields.keySet(), actualFields.keySet(), () -> diffPrefix(expected, "字段集合不同") + "\n期望: " + expectedFields.keySet() + "\n实际:  " + actualFields.keySet());
    for (String key : expectedFields.keySet()) {
      FieldNode e = expectedFields.get(key);
      FieldNode a = actualFields.get(key);
      String context = diffPrefix(expected, "字段差异") + " -> " + key;
      Assertions.assertEquals(e.access, a.access, () -> context + " 的访问标志不同");
      Assertions.assertEquals(e.signature, a.signature, () -> context + " 的泛型签名不同");
      Assertions.assertEquals(e.value, a.value, () -> context + " 的常量值不同");
      compareAnnotations(e.visibleAnnotations, a.visibleAnnotations, () -> context + " 的可见注解不同");
      compareAnnotations(e.invisibleAnnotations, a.invisibleAnnotations, () -> context + " 的不可见注解不同");
    }
  }

  private static void compareMethods(ClassNode expected, ClassNode actual) {
    Map<String, MethodNode> expectedMethods = indexMethods(expected.methods);
    Map<String, MethodNode> actualMethods = indexMethods(actual.methods);
    Assertions.assertEquals(expectedMethods.keySet(), actualMethods.keySet(), () -> diffPrefix(expected, "方法集合不同") + "\n期望: " + expectedMethods.keySet() + "\n实际:  " + actualMethods.keySet());
    for (Map.Entry<String, MethodNode> entry : expectedMethods.entrySet()) {
      MethodNode actualNode = actualMethods.get(entry.getKey());
      compareMethod(expected.name, entry.getValue(), actualNode);
    }
  }

  private static void compareInnerClasses(ClassNode expected, ClassNode actual) {
    Set<String> expectedInner = normalizeInnerClasses(expected.innerClasses);
    Set<String> actualInner = normalizeInnerClasses(actual.innerClasses);
    Assertions.assertEquals(expectedInner, actualInner, () -> diffPrefix(expected, "内部类声明不同") + "\n期望: " + expectedInner + "\n实际:  " + actualInner);
  }

  private static Map<String, FieldNode> indexFields(List<FieldNode> fields) {
    Map<String, FieldNode> result = new TreeMap<>();
    if (fields != null) for (FieldNode field : fields) result.put(fieldKey(field), field);
    return result;
  }

  private static Map<String, MethodNode> indexMethods(List<MethodNode> methods) {
    Map<String, MethodNode> result = new LinkedHashMap<>();
    if (methods != null) {
      for (MethodNode method : methods) {
        result.put(methodKey(method), method);
      }
    }
    return result;
  }

  private static Set<String> normalizeInnerClasses(List<InnerClassNode> nodes) {
    if (nodes == null) return Set.of();
    return nodes.stream()
      .map(node -> (node.name == null ? "<anonymous>" : node.name) + "|" + node.outerName + "|" + node.innerName + "|" + node.access)
      .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static List<String> normalizeTryCatch(List<TryCatchBlockNode> blocks) {
    if (blocks == null) return List.of();
    Map<LabelNode, String> labelIds = new IdentityHashMap<>();
    List<String> result = new ArrayList<>();
    for (TryCatchBlockNode block : blocks) {
      String entry = "try{" + id(block.start, labelIds) + "-" + id(block.end, labelIds) + "} catch(" + block.type + ") -> " + id(block.handler, labelIds);
      result.add(entry);
    }
    return result;
  }

  private static List<String> normalizeInstructions(InsnList instructions) {
    List<String> result = new ArrayList<>();
    if (instructions == null) return result;
    Map<LabelNode, String> labelIds = new IdentityHashMap<>();
    for (AbstractInsnNode node = instructions.getFirst(); node != null; node = node.getNext()) {
      if (node instanceof LineNumberNode || node instanceof FrameNode) continue;
      if (node instanceof LabelNode label) {
        result.add("LABEL " + id(label, labelIds));
        continue;
      }
      result.add(formatInstruction(node, labelIds));
    }
    return result;
  }

  private static String formatInstruction(AbstractInsnNode node, Map<LabelNode, String> labelIds) {
    int opcode = node.getOpcode();
    String op = opcode >= 0 ? Printer.OPCODES[opcode] : "#" + node.getType();
    return switch (node.getType()) {
      case AbstractInsnNode.INSN -> op;
      case AbstractInsnNode.INT_INSN -> op + " " + ((IntInsnNode) node).operand;
      case AbstractInsnNode.VAR_INSN -> op + " " + ((VarInsnNode) node).var;
      case AbstractInsnNode.TYPE_INSN -> op + " " + ((TypeInsnNode) node).desc;
      case AbstractInsnNode.FIELD_INSN -> {
        var n = (org.objectweb.asm.tree.FieldInsnNode) node;
        yield op + " " + n.owner + "." + n.name + " " + n.desc;
      }
      case AbstractInsnNode.METHOD_INSN -> {
        var n = (org.objectweb.asm.tree.MethodInsnNode) node;
        yield op + " " + n.owner + "." + n.name + n.desc + " itf=" + n.itf;
      }
      case AbstractInsnNode.INVOKE_DYNAMIC_INSN -> {
        InvokeDynamicInsnNode n = (InvokeDynamicInsnNode) node;
        String args = formatBootstrapArgs(n.bsmArgs);
        yield op + " " + n.name + n.desc + " bsm=" + formatHandle(n.bsm) + " args=" + args;
      }
      case AbstractInsnNode.JUMP_INSN -> {
        JumpInsnNode n = (JumpInsnNode) node;
        yield op + " " + id(n.label, labelIds);
      }
      case AbstractInsnNode.LDC_INSN -> op + " " + formatConstant(((LdcInsnNode) node).cst);
      case AbstractInsnNode.IINC_INSN -> {
        IincInsnNode n = (IincInsnNode) node;
        yield op + " var=" + n.var + " inc=" + n.incr;
      }
      case AbstractInsnNode.TABLESWITCH_INSN -> {
        TableSwitchInsnNode n = (TableSwitchInsnNode) node;
        String labels = n.labels.stream().map(label -> id(label, labelIds)).collect(Collectors.joining(","));
        yield op + " [" + n.min + "," + n.max + "] default=" + id(n.dflt, labelIds) + " -> " + labels;
      }
      case AbstractInsnNode.LOOKUPSWITCH_INSN -> {
        LookupSwitchInsnNode n = (LookupSwitchInsnNode) node;
        List<String> items = new ArrayList<>();
        for (int i = 0; i < n.keys.size(); i++) {
          items.add(n.keys.get(i) + ":" + id(n.labels.get(i), labelIds));
        }
        yield op + " default=" + id(n.dflt, labelIds) + " " + items;
      }
      case AbstractInsnNode.MULTIANEWARRAY_INSN -> {
        MultiANewArrayInsnNode n = (MultiANewArrayInsnNode) node;
        yield op + " " + n.desc + " dims=" + n.dims;
      }
      default -> op;
    };
  }

  private static String formatBootstrapArgs(Object[] args) {
    if (args == null || args.length == 0) return "[]";
    List<String> parts = new ArrayList<>(args.length);
    for (Object arg : args) parts.add(formatConstant(arg));
    return parts.toString();
  }

  private static String formatHandle(Handle handle) {
    return handle.getTag() + ":" + handle.getOwner() + "." + handle.getName() + handle.getDesc() + ":itf=" + handle.isInterface();
  }

  private static String formatConstant(Object constant) {
    if (constant == null) return "null";
    if (constant instanceof Type type) return "Type(" + type.getDescriptor() + ")";
    if (constant instanceof Handle handle) return "Handle(" + formatHandle(handle) + ")";
    if (constant instanceof org.objectweb.asm.ConstantDynamic cd) {
      int count = cd.getBootstrapMethodArgumentCount();
      Object[] args = new Object[count];
      for (int i = 0; i < count; i++) args[i] = cd.getBootstrapMethodArgument(i);
      return "ConstDynamic(" + cd.getName() + cd.getDescriptor() + "," + formatHandle(cd.getBootstrapMethod()) + "," + formatBootstrapArgs(args) + ")";
    }
    return constant.toString();
  }

  private static void compareAnnotations(List<AnnotationNode> expected, List<AnnotationNode> actual, AnnotationContextSupplier supplier) {
    List<String> left = normalizeAnnotations(expected);
    List<String> right = normalizeAnnotations(actual);
    Assertions.assertEquals(left, right, supplier::get);
  }

  private static List<String> normalizeAnnotations(List<AnnotationNode> annotations) {
    if (annotations == null || annotations.isEmpty()) return List.of();
    return annotations.stream()
      .sorted(Comparator.comparing(a -> a.desc))
      .map(BytecodeComparator::annotationToString)
      .collect(Collectors.toList());
  }

  private static String annotationToString(AnnotationNode node) {
    if (node == null) return "<null>";
    String values = node.values == null ? "[]" : node.values.toString();
    return node.desc + ":" + values;
  }

  private static String fieldKey(FieldNode node) {
    return node.name + ":" + node.desc + ":" + Objects.toString(node.signature, "");
  }

  private static String methodKey(MethodNode node) {
    return node.name + node.desc;
  }

  private static String id(LabelNode label, Map<LabelNode, String> labelIds) {
    if (label == null) return "null";
    return labelIds.computeIfAbsent(label, key -> "L" + labelIds.size());
  }

  private static String diffPrefix(ClassNode node, String detail) {
    return "[" + node.name + "] " + detail;
  }

  private static String formatClassDiff(ClassNode expected, ClassNode actual, String detail) {
    return detail + "，期望=" + expected.name + " 实际=" + actual.name;
  }

  @FunctionalInterface
  private interface AnnotationContextSupplier {
    String get();
  }
}
