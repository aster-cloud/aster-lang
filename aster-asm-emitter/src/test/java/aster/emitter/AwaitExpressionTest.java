package aster.emitter;

import aster.core.ir.CoreModel;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 13: Await 表达式测试
 *
 * 测试 Await 表达式的字节码生成是否正确。
 * Await 表达式使用透明传递语义：await expr 等价于 expr（同步执行）。
 */
class AwaitExpressionTest {

    @Test
    void testAwaitSimpleValue(@TempDir Path tempDir) throws Exception {
        // 测试：await 42 -> 直接返回 42
        String coreIR = """
            {
              "name": "app",
              "decls": [
                {
                  "kind": "Func",
                  "name": "awaitValue",
                  "params": [],
                  "ret": {"kind": "TypeName", "name": "Int"},
                  "effects": [],
                  "body": {
                    "kind": "Block",
                    "statements": [
                      {
                        "kind": "Return",
                        "expr": {
                          "kind": "Await",
                          "expr": {"kind": "Int", "value": 42}
                        }
                      }
                    ]
                  }
                }
              ]
            }
            """;

        // Parse and emit
        var mapper = new ObjectMapper();
        var module = mapper.readValue(coreIR, CoreModel.Module.class);

        var context = new ContextBuilder(module);
        var ctx = new Main.Ctx(
            tempDir,
            context,
            new java.util.concurrent.atomic.AtomicInteger(0),
            java.util.Collections.emptyMap(),
            new java.util.LinkedHashMap<>(),
            java.util.Collections.emptyMap()
        );

        // 发射字节码
        assertDoesNotThrow(() -> Main.emitFunc(ctx, "app", module, (CoreModel.Func) module.decls.get(0)));

        // 验证 .class 文件生成
        Path classFile = tempDir.resolve("app/awaitValue_fn.class");
        assertTrue(Files.exists(classFile), "Class file should be generated");
        assertTrue(Files.size(classFile) > 0, "Class file should not be empty");
    }

    @Test
    void testAwaitCall(@TempDir Path tempDir) throws Exception {
        // 测试：await (x + 10) -> 计算 x + 10 并返回结果
        String coreIR = """
            {
              "name": "app",
              "decls": [
                {
                  "kind": "Func",
                  "name": "awaitCall",
                  "params": [
                    {
                      "name": "x",
                      "type": {"kind": "TypeName", "name": "Int"},
                      "annotations": []
                    }
                  ],
                  "ret": {"kind": "TypeName", "name": "Int"},
                  "effects": [],
                  "body": {
                    "kind": "Block",
                    "statements": [
                      {
                        "kind": "Return",
                        "expr": {
                          "kind": "Await",
                          "expr": {
                            "kind": "Call",
                            "target": {"kind": "Name", "name": "+"},
                            "args": [
                              {"kind": "Name", "name": "x"},
                              {"kind": "Int", "value": 10}
                            ]
                          }
                        }
                      }
                    ]
                  }
                }
              ]
            }
            """;

        var mapper = new ObjectMapper();
        var module = mapper.readValue(coreIR, CoreModel.Module.class);

        var context = new ContextBuilder(module);
        var ctx = new Main.Ctx(
            tempDir,
            context,
            new java.util.concurrent.atomic.AtomicInteger(0),
            java.util.Collections.emptyMap(),
            new java.util.LinkedHashMap<>(),
            java.util.Collections.emptyMap()
        );

        assertDoesNotThrow(() -> Main.emitFunc(ctx, "app", module, (CoreModel.Func) module.decls.get(0)));

        Path classFile = tempDir.resolve("app/awaitCall_fn.class");
        assertTrue(Files.exists(classFile));
        assertTrue(Files.size(classFile) > 0);
    }

    @Test
    void testAwaitInLet(@TempDir Path tempDir) throws Exception {
        // 测试：let x = await val; return await (x * 2)
        String coreIR = """
            {
              "name": "app",
              "decls": [
                {
                  "kind": "Func",
                  "name": "awaitNested",
                  "params": [
                    {
                      "name": "val",
                      "type": {"kind": "TypeName", "name": "Int"},
                      "annotations": []
                    }
                  ],
                  "ret": {"kind": "TypeName", "name": "Int"},
                  "effects": [],
                  "body": {
                    "kind": "Block",
                    "statements": [
                      {
                        "kind": "Let",
                        "name": "x",
                        "expr": {
                          "kind": "Await",
                          "expr": {"kind": "Name", "name": "val"}
                        }
                      },
                      {
                        "kind": "Return",
                        "expr": {
                          "kind": "Await",
                          "expr": {
                            "kind": "Call",
                            "target": {"kind": "Name", "name": "*"},
                            "args": [
                              {"kind": "Name", "name": "x"},
                              {"kind": "Int", "value": 2}
                            ]
                          }
                        }
                      }
                    ]
                  }
                }
              ]
            }
            """;

        var mapper = new ObjectMapper();
        var module = mapper.readValue(coreIR, CoreModel.Module.class);

        var context = new ContextBuilder(module);
        var ctx = new Main.Ctx(
            tempDir,
            context,
            new java.util.concurrent.atomic.AtomicInteger(0),
            java.util.Collections.emptyMap(),
            new java.util.LinkedHashMap<>(),
            java.util.Collections.emptyMap()
        );

        assertDoesNotThrow(() -> Main.emitFunc(ctx, "app", module, (CoreModel.Func) module.decls.get(0)));

        Path classFile = tempDir.resolve("app/awaitNested_fn.class");
        assertTrue(Files.exists(classFile));
        assertTrue(Files.size(classFile) > 0);
    }
}
