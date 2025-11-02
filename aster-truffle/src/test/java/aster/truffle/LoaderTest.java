package aster.truffle;

import aster.truffle.nodes.Env;
import aster.truffle.nodes.Exec;
import aster.truffle.runtime.FrameSlotBuilder;
import com.oracle.truffle.api.Truffle;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 Loader 的基本功能，确保可以解析简单 JSON 并执行。
 */
public class LoaderTest {

  @Test
  public void testLoadSimpleLiteral() throws Exception {
    Loader loader = new Loader(null);  // null language for legacy tests
    String json =
        """
        {
          "name": "test.simple.literal",
          "decls": [
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": {
                "kind": "TypeName",
                "name": "Int"
              },
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [
                  {
                    "kind": "Return",
                    "expr": {
                      "kind": "Int",
                      "value": 42
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    Loader.Program program = loader.buildProgram(json, "main", null);

    assertNotNull(program);
    assertNotNull(program.root);

    Env env = program.env;
    assertNotNull(env);
    Object result = Exec.exec(program.root, null);

    assertEquals(42, result);
  }

  @Test
  public void testLoadFromResource() throws Exception {
    Loader loader = new Loader(null);  // null language for legacy tests
    String json;
    try (InputStream stream =
        Objects.requireNonNull(
            getClass().getResourceAsStream("/simple-literal.json"), "资源 simple-literal.json 缺失")) {
      json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }

    Loader.Program program = loader.buildProgram(json, "main", null);
    assertNotNull(program);

    Env env = program.env;
    assertNotNull(env);
    Object result = Exec.exec(program.root, null);
    assertNotNull(result);
  }

  @Test
  public void testParameterAccessViaFrame() throws Exception {
    Loader loader = new Loader(null);  // null language for legacy tests
    String json =
        """
        {
          "name": "test.param.access",
          "decls": [
            {
              "kind": "Func",
              "name": "main",
              "params": [
                {
                  "name": "x",
                  "type": { "kind": "TypeName", "name": "Int" }
                }
              ],
              "ret": { "kind": "TypeName", "name": "Int" },
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [
                  {
                    "kind": "Return",
                    "expr": {
                      "kind": "Name",
                      "name": "x"
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    Loader.Program program = loader.buildProgram(json, "main", null);
    program.env.set("x", 42);
    FrameSlotBuilder builder = new FrameSlotBuilder();
    builder.addParameter("x");
    com.oracle.truffle.api.frame.FrameDescriptor descriptor = builder.build();
    com.oracle.truffle.api.frame.VirtualFrame frame =
        Truffle.getRuntime().createVirtualFrame(new Object[] {42}, descriptor);
    frame.setObject(0, 42);

    Object result = Exec.exec(program.root, frame);
    assertEquals(42, result);
  }

  @Test
  public void testEntryFunctionWithParamsAndLocals() throws Exception {
    Loader loader = new Loader(null);  // legacy 模式下验证入口函数帧槽位
    String json =
        """
        {
          "name": "test.entry.frame.slots",
          "decls": [
            {
              "kind": "Func",
              "name": "main",
              "params": [
                {
                  "name": "x",
                  "type": { "kind": "TypeName", "name": "Int" }
                }
              ],
              "ret": { "kind": "TypeName", "name": "Int" },
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [
                  {
                    "kind": "Let",
                    "name": "y",
                    "expr": { "kind": "Name", "name": "x" }
                  },
                  {
                    "kind": "Return",
                    "expr": { "kind": "Name", "name": "y" }
                  }
                ]
              }
            }
          ]
        }
        """;

    Loader.Program program = loader.buildProgram(json, "main", null);

    java.lang.reflect.Field slotField = Loader.class.getDeclaredField("entryParamSlots");
    slotField.setAccessible(true);
    @SuppressWarnings("unchecked")
    java.util.Map<String, Integer> slots =
        (java.util.Map<String, Integer>) slotField.get(loader);

    assertNotNull(slots, "入口函数应生成帧槽位映射");
    assertTrue(slots.containsKey("x"), "参数 x 应获得帧槽位");
    assertTrue(slots.containsKey("y"), "局部变量 y 应获得帧槽位");
    assertEquals(0, slots.get("x"), "参数 x 应位于槽位 0");
    assertEquals(1, slots.get("y"), "局部变量 y 应位于槽位 1");

    java.util.Set<String> paramNames = new java.util.HashSet<>();
    if (program.params != null) {
      for (var p : program.params) {
        paramNames.add(p.name);
      }
    }

    java.util.List<java.util.Map.Entry<String, Integer>> ordered =
        new java.util.ArrayList<>(slots.entrySet());
    ordered.sort(java.util.Map.Entry.comparingByValue());

    FrameSlotBuilder builder = new FrameSlotBuilder();
    for (var entry : ordered) {
      if (paramNames.contains(entry.getKey())) {
        builder.addParameter(entry.getKey());
      } else {
        builder.addLocal(entry.getKey());
      }
    }
    int slotCount = builder.getSlotCount();
    com.oracle.truffle.api.frame.FrameDescriptor descriptor = builder.build();
    com.oracle.truffle.api.frame.VirtualFrame frame =
        Truffle.getRuntime().createVirtualFrame(new Object[slotCount], descriptor);
    frame.setObject(slots.get("x"), 5);

    program.env.set("x", 5);

    Object result = Exec.exec(program.root, frame);
    assertEquals(5, result, "入口函数应能够经由帧读取参数并返回局部变量结果");
  }

  @Test
  public void testMatchCaseWithoutScopeCollectsLocals() throws Exception {
    Loader loader = new Loader(null);  // null language for legacy tests
    String json =
        """
        {
          "name": "test.match.case.locals",
          "decls": [
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": { "kind": "TypeName", "name": "Int" },
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [
                  {
                    "kind": "Match",
                    "expr": { "kind": "Int", "value": 1 },
                    "cases": [
                      {
                        "kind": "Case",
                        "pattern": { "kind": "PatInt", "value": 1 },
                        "body": {
                          "kind": "If",
                          "cond": { "kind": "Bool", "value": true },
                          "thenBlock": {
                            "kind": "Block",
                            "statements": [
                              {
                                "kind": "Let",
                                "name": "tmp",
                                "expr": { "kind": "Int", "value": 42 }
                              },
                              {
                                "kind": "Return",
                                "expr": { "kind": "Name", "name": "tmp" }
                              }
                            ]
                          },
                          "elseBlock": {
                            "kind": "Block",
                            "statements": [
                              {
                                "kind": "Return",
                                "expr": { "kind": "Int", "value": 0 }
                              }
                            ]
                          }
                        }
                      }
                    ]
                  }
                ]
              }
            }
          ]
        }
        """;

    Loader.Program program = loader.buildProgram(json, "main", null);
    java.lang.reflect.Field slotField = Loader.class.getDeclaredField("entryParamSlots");
    slotField.setAccessible(true);
    @SuppressWarnings("unchecked")
    java.util.Map<String,Integer> slots = (java.util.Map<String,Integer>) slotField.get(loader);
    assertNotNull(slots);

    FrameSlotBuilder builder = new FrameSlotBuilder();
    java.util.List<java.util.Map.Entry<String,Integer>> ordered = new java.util.ArrayList<>(slots.entrySet());
    ordered.sort(java.util.Map.Entry.comparingByValue());
    for (var entry : ordered) {
      builder.addLocal(entry.getKey());
    }
    com.oracle.truffle.api.frame.VirtualFrame frame =
        Truffle.getRuntime()
            .createVirtualFrame(new Object[builder.getSlotCount()], builder.build());

    Object result = Exec.exec(program.root, frame);

    assertEquals(42, result);
    assertNull(program.env.get("tmp"), "match case 中的局部变量应保留在帧内而不是泄漏到 Env");
  }
}
