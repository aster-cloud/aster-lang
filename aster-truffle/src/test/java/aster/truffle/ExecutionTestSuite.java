package aster.truffle;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Execution Test Suite - 验证 Truffle 后端的正确执行
 *
 * 与 GoldenTestAdapter 的区别：
 * - GoldenTestAdapter: 验证执行不崩溃（功能性测试）
 * - ExecutionTestSuite: 验证执行结果正确性（正确性测试）
 *
 * 测试策略：
 * 1. 明确的输入参数
 * 2. 明确的预期输出
 * 3. 涵盖所有核心功能
 * 4. 边界条件和错误情况
 */
public class ExecutionTestSuite {

  // ==================== Arithmetic Operations ====================

  @Test
  public void testAddition() throws Exception {
    String json = """
        {
          "name": "test.add",
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
                    "kind": "Return",
                    "expr": {
                      "kind": "Call",
                      "target": { "kind": "Name", "name": "add" },
                      "args": [
                        { "kind": "Int", "value": 10 },
                        { "kind": "Int", "value": 20 }
                      ]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertEquals(30, result.asInt(), "10 + 20 should equal 30");
    }
  }

  @Test
  public void testSubtraction() throws Exception {
    String json = """
        {
          "name": "test.sub",
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
                    "kind": "Return",
                    "expr": {
                      "kind": "Call",
                      "target": { "kind": "Name", "name": "sub" },
                      "args": [
                        { "kind": "Int", "value": 50 },
                        { "kind": "Int", "value": 20 }
                      ]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertEquals(30, result.asInt(), "50 - 20 should equal 30");
    }
  }

  @Test
  public void testMultiplication() throws Exception {
    String json = """
        {
          "name": "test.mul",
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
                    "kind": "Return",
                    "expr": {
                      "kind": "Call",
                      "target": { "kind": "Name", "name": "mul" },
                      "args": [
                        { "kind": "Int", "value": 6 },
                        { "kind": "Int", "value": 7 }
                      ]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertEquals(42, result.asInt(), "6 * 7 should equal 42");
    }
  }

  @Test
  public void testDivision() throws Exception {
    String json = """
        {
          "name": "test.div",
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
                    "kind": "Return",
                    "expr": {
                      "kind": "Call",
                      "target": { "kind": "Name", "name": "div" },
                      "args": [
                        { "kind": "Int", "value": 100 },
                        { "kind": "Int", "value": 5 }
                      ]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertEquals(20, result.asInt(), "100 / 5 should equal 20");
    }
  }

  // ==================== Comparison Operations ====================

  @Test
  public void testEquality() throws Exception {
    String json = """
        {
          "name": "test.eq",
          "decls": [
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": { "kind": "TypeName", "name": "Bool" },
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [
                  {
                    "kind": "Return",
                    "expr": {
                      "kind": "Call",
                      "target": { "kind": "Name", "name": "eq" },
                      "args": [
                        { "kind": "Int", "value": 42 },
                        { "kind": "Int", "value": 42 }
                      ]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertTrue(result.asBoolean(), "42 == 42 should be true");
    }
  }

  @Test
  public void testLessThan() throws Exception {
    String json = """
        {
          "name": "test.lt",
          "decls": [
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": { "kind": "TypeName", "name": "Bool" },
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [
                  {
                    "kind": "Return",
                    "expr": {
                      "kind": "Call",
                      "target": { "kind": "Name", "name": "lt" },
                      "args": [
                        { "kind": "Int", "value": 10 },
                        { "kind": "Int", "value": 20 }
                      ]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertTrue(result.asBoolean(), "10 < 20 should be true");
    }
  }

  // ==================== Text Operations ====================

  @Test
  public void testTextConcat() throws Exception {
    String json = """
        {
          "name": "test.text",
          "decls": [
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": { "kind": "TypeName", "name": "Text" },
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [
                  {
                    "kind": "Return",
                    "expr": {
                      "kind": "Call",
                      "target": { "kind": "Name", "name": "Text.concat" },
                      "args": [
                        { "kind": "String", "value": "Hello" },
                        { "kind": "String", "value": "World" }
                      ]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertEquals("HelloWorld", result.asString(), "Text.concat should concatenate strings");
    }
  }

  @Test
  public void testTextLength() throws Exception {
    String json = """
        {
          "name": "test.text.length",
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
                    "kind": "Return",
                    "expr": {
                      "kind": "Call",
                      "target": { "kind": "Name", "name": "Text.length" },
                      "args": [
                        { "kind": "String", "value": "Hello" }
                      ]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertEquals(5, result.asInt(), "Text.length('Hello') should be 5");
    }
  }

  @Test
  public void testTextContains() throws Exception {
    String json = """
        {
          "name": "test.text.contains",
          "decls": [
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": { "kind": "TypeName", "name": "Bool" },
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [
                  {
                    "kind": "Return",
                    "expr": {
                      "kind": "Call",
                      "target": { "kind": "Name", "name": "Text.contains" },
                      "args": [
                        { "kind": "String", "value": "Hello World" },
                        { "kind": "String", "value": "World" }
                      ]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertTrue(result.asBoolean(), "Text.contains should find 'World' in 'Hello World'");
    }
  }

  // ==================== Control Flow ====================

  @Test
  public void testIfTrueBranch() throws Exception {
    String json = """
        {
          "name": "test.if.true",
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
                    "kind": "If",
                    "cond": { "kind": "Bool", "value": true },
                    "thenBlock": {
                      "kind": "Block",
                      "statements": [
                        { "kind": "Return", "expr": { "kind": "Int", "value": 42 } }
                      ]
                    },
                    "elseBlock": {
                      "kind": "Block",
                      "statements": [
                        { "kind": "Return", "expr": { "kind": "Int", "value": 0 } }
                      ]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertEquals(42, result.asInt(), "If true should return 42");
    }
  }

  @Test
  public void testIfFalseBranch() throws Exception {
    String json = """
        {
          "name": "test.if.false",
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
                    "kind": "If",
                    "cond": { "kind": "Bool", "value": false },
                    "thenBlock": {
                      "kind": "Block",
                      "statements": [
                        { "kind": "Return", "expr": { "kind": "Int", "value": 0 } }
                      ]
                    },
                    "elseBlock": {
                      "kind": "Block",
                      "statements": [
                        { "kind": "Return", "expr": { "kind": "Int", "value": 99 } }
                      ]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertEquals(99, result.asInt(), "If false should return 99");
    }
  }

  // ==================== Variables (Let/Set) ====================

  @Test
  public void testLetBinding() throws Exception {
    String json = """
        {
          "name": "test.let",
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
                    "kind": "Let",
                    "name": "x",
                    "expr": { "kind": "Int", "value": 42 }
                  },
                  {
                    "kind": "Return",
                    "expr": { "kind": "Name", "name": "x" }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertEquals(42, result.asInt(), "Let binding should work");
    }
  }

  @Test
  public void testSetVariable() throws Exception {
    String json = """
        {
          "name": "test.set",
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
                    "kind": "Let",
                    "name": "x",
                    "expr": { "kind": "Int", "value": 10 }
                  },
                  {
                    "kind": "Set",
                    "name": "x",
                    "expr": { "kind": "Int", "value": 20 }
                  },
                  {
                    "kind": "Return",
                    "expr": { "kind": "Name", "name": "x" }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertEquals(20, result.asInt(), "Set should update variable value");
    }
  }

  // ==================== Lambda/Closure ====================

  @Test
  public void testSimpleLambda() throws Exception {
    String json = """
        {
          "name": "test.lambda",
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
                    "kind": "Let",
                    "name": "identity",
                    "expr": {
                      "kind": "Lambda",
                      "params": [
                        { "name": "x", "type": { "kind": "TypeName", "name": "Int" } }
                      ],
                      "ret": { "kind": "TypeName", "name": "Int" },
                      "captures": [],
                      "body": {
                        "kind": "Block",
                        "statements": [
                          { "kind": "Return", "expr": { "kind": "Name", "name": "x" } }
                        ]
                      }
                    }
                  },
                  {
                    "kind": "Return",
                    "expr": {
                      "kind": "Call",
                      "target": { "kind": "Name", "name": "identity" },
                      "args": [{ "kind": "Int", "value": 42 }]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertEquals(42, result.asInt(), "Lambda identity function should return input");
    }
  }

  // ==================== Result/Maybe Types ====================

  @Test
  public void testOkConstruction() throws Exception {
    String json = """
        {
          "name": "test.ok",
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
                    "kind": "Let",
                    "name": "result",
                    "expr": {
                      "kind": "Ok",
                      "expr": { "kind": "Int", "value": 42 }
                    }
                  },
                  {
                    "kind": "Return",
                    "expr": {
                      "kind": "Call",
                      "target": { "kind": "Name", "name": "Result.unwrap" },
                      "args": [{ "kind": "Name", "name": "result" }]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertEquals(42, result.asInt(), "Ok value should unwrap to 42");
    }
  }

  // ==================== Result Operations ====================

  @Test
  public void testResultIsOk() throws Exception {
    String json = """
        {
          "name": "test.result.isok",
          "decls": [
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": { "kind": "TypeName", "name": "Bool" },
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [
                  {
                    "kind": "Let",
                    "name": "result",
                    "expr": {
                      "kind": "Ok",
                      "expr": { "kind": "Int", "value": 100 }
                    }
                  },
                  {
                    "kind": "Return",
                    "expr": {
                      "kind": "Call",
                      "target": { "kind": "Name", "name": "Result.isOk" },
                      "args": [{ "kind": "Name", "name": "result" }]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertTrue(result.asBoolean(), "Result.isOk should return true for Ok variant");
    }
  }

  @Test
  public void testResultIsErr() throws Exception {
    String json = """
        {
          "name": "test.result.iserr",
          "decls": [
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": { "kind": "TypeName", "name": "Bool" },
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [
                  {
                    "kind": "Let",
                    "name": "result",
                    "expr": {
                      "kind": "Err",
                      "expr": { "kind": "String", "value": "error" }
                    }
                  },
                  {
                    "kind": "Return",
                    "expr": {
                      "kind": "Call",
                      "target": { "kind": "Name", "name": "Result.isErr" },
                      "args": [{ "kind": "Name", "name": "result" }]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertTrue(result.asBoolean(), "Result.isErr should return true for Err variant");
    }
  }

  // ==================== Option Operations ====================

  @Test
  public void testOptionIsSome() throws Exception {
    String json = """
        {
          "name": "test.option.issome",
          "decls": [
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": { "kind": "TypeName", "name": "Bool" },
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [
                  {
                    "kind": "Let",
                    "name": "option",
                    "expr": {
                      "kind": "Some",
                      "expr": { "kind": "Int", "value": 42 }
                    }
                  },
                  {
                    "kind": "Return",
                    "expr": {
                      "kind": "Call",
                      "target": { "kind": "Name", "name": "Option.isSome" },
                      "args": [{ "kind": "Name", "name": "option" }]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertTrue(result.asBoolean(), "Option.isSome should return true for Some variant");
    }
  }

  @Test
  public void testOptionIsNone() throws Exception {
    String json = """
        {
          "name": "test.option.isnone",
          "decls": [
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": { "kind": "TypeName", "name": "Bool" },
              "effects": [],
              "body": {
                "kind": "Block",
                "statements": [
                  {
                    "kind": "Let",
                    "name": "option",
                    "expr": { "kind": "None" }
                  },
                  {
                    "kind": "Return",
                    "expr": {
                      "kind": "Call",
                      "target": { "kind": "Name", "name": "Option.isNone" },
                      "args": [{ "kind": "Name", "name": "option" }]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertTrue(result.asBoolean(), "Option.isNone should return true for None variant");
    }
  }

  // ==================== Async Basic Operations ====================

  // Note: Effect context propagation through async boundaries has been implemented
  // in StartNode to capture and restore parent effect permissions

  @Test
  public void testAsyncBasic() throws Exception {
    String json = """
        {
          "name": "test.async.basic",
          "decls": [
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": { "kind": "TypeName", "name": "Int" },
              "effects": ["Async"],
              "body": {
                "kind": "Block",
                "statements": [
                  {
                    "kind": "Start",
                    "name": "task1",
                    "expr": { "kind": "Int", "value": 42 }
                  },
                  {
                    "kind": "Return",
                    "expr": {
                      "kind": "Await",
                      "expr": { "kind": "Name", "name": "task1" }
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertEquals(42, result.asInt(), "Async task should return 42");
    }
  }

  @Test
  public void testAsyncMultipleTasks() throws Exception {
    String json = """
        {
          "name": "test.async.multiple",
          "decls": [
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": { "kind": "TypeName", "name": "Int" },
              "effects": ["Async"],
              "body": {
                "kind": "Block",
                "statements": [
                  {
                    "kind": "Start",
                    "name": "task1",
                    "expr": { "kind": "Int", "value": 10 }
                  },
                  {
                    "kind": "Start",
                    "name": "task2",
                    "expr": { "kind": "Int", "value": 20 }
                  },
                  {
                    "kind": "Let",
                    "name": "result1",
                    "expr": {
                      "kind": "Await",
                      "expr": { "kind": "Name", "name": "task1" }
                    }
                  },
                  {
                    "kind": "Let",
                    "name": "result2",
                    "expr": {
                      "kind": "Await",
                      "expr": { "kind": "Name", "name": "task2" }
                    }
                  },
                  {
                    "kind": "Return",
                    "expr": { "kind": "Name", "name": "result1" }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertEquals(10, result.asInt(), "Multiple async tasks should return first result 10");
    }
  }

  @Test
  public void testAsyncWaitMultipleTasks() throws Exception {
    String json = """
        {
          "name": "test.async.wait",
          "decls": [
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": { "kind": "TypeName", "name": "Int" },
              "effects": ["Async"],
              "body": {
                "kind": "Block",
                "statements": [
                  {
                    "kind": "Start",
                    "name": "task1",
                    "expr": { "kind": "Int", "value": 5 }
                  },
                  {
                    "kind": "Start",
                    "name": "task2",
                    "expr": { "kind": "Int", "value": 10 }
                  },
                  {
                    "kind": "Wait",
                    "names": ["task1", "task2"]
                  },
                  {
                    "kind": "Return",
                    "expr": { "kind": "Int", "value": 42 }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertEquals(42, result.asInt(), "Wait should complete all tasks and return 42");
    }
  }

  // ==================== Scope and Wait Semantics ====================

  @Test
  public void testScopeVariableShadowing() throws Exception {
    String json = """
        {
          "name": "test.scope.shadowing",
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
                    "kind": "Let",
                    "name": "x",
                    "expr": { "kind": "Int", "value": 10 }
                  },
                  {
                    "kind": "Scope",
                    "statements": [
                      {
                        "kind": "Let",
                        "name": "x",
                        "expr": { "kind": "Int", "value": 20 }
                      }
                    ]
                  },
                  {
                    "kind": "Return",
                    "expr": { "kind": "Name", "name": "x" }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertEquals(10, result.asInt(), "Inner scope binding should not override outer variable");
    }
  }

  @Test
  public void testScopeNoVariableLeaking() throws Exception {
    String json = """
        {
          "name": "test.scope.isolation",
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
                    "kind": "Let",
                    "name": "outer",
                    "expr": { "kind": "Int", "value": 1 }
                  },
                  {
                    "kind": "Scope",
                    "statements": [
                      {
                        "kind": "Let",
                        "name": "inner",
                        "expr": { "kind": "Int", "value": 2 }
                      },
                      {
                        "kind": "Set",
                        "name": "outer",
                        "expr": { "kind": "Int", "value": 3 }
                      }
                    ]
                  },
                  {
                    "kind": "Return",
                    "expr": { "kind": "Name", "name": "outer" }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertEquals(3, result.asInt(), "Scope should allow updating outer variables while inner bindings remain isolated");
    }
  }

  @Test
  public void testWaitSingleTaskResult() throws Exception {
    String json = """
        {
          "name": "test.wait.single",
          "decls": [
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": { "kind": "TypeName", "name": "Int" },
              "effects": ["Async"],
              "body": {
                "kind": "Block",
                "statements": [
                  {
                    "kind": "Start",
                    "name": "task1",
                    "expr": { "kind": "Int", "value": 42 }
                  },
                  {
                    "kind": "Wait",
                    "names": ["task1"]
                  },
                  {
                    "kind": "Return",
                    "expr": { "kind": "Name", "name": "task1" }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertEquals(42, result.asInt(), "wait task should expose completed result in the same scope");
    }
  }

  @Test
  public void testWaitMultipleTasksResult() throws Exception {
    String json = """
        {
          "name": "test.wait.multi",
          "decls": [
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": { "kind": "TypeName", "name": "List" },
              "effects": ["Async"],
              "body": {
                "kind": "Block",
                "statements": [
                  {
                    "kind": "Start",
                    "name": "task1",
                    "expr": { "kind": "Int", "value": 10 }
                  },
                  {
                    "kind": "Start",
                    "name": "task2",
                    "expr": { "kind": "Int", "value": 20 }
                  },
                  {
                    "kind": "Wait",
                    "names": ["task1", "task2"]
                  },
                  {
                    "kind": "Return",
                    "expr": {
                      "kind": "Call",
                      "target": { "kind": "Name", "name": "List.append" },
                      "args": [
                        {
                          "kind": "Call",
                          "target": { "kind": "Name", "name": "List.append" },
                          "args": [
                            {
                              "kind": "Call",
                              "target": { "kind": "Name", "name": "List.empty" },
                              "args": []
                            },
                            { "kind": "Name", "name": "task1" }
                          ]
                        },
                        { "kind": "Name", "name": "task2" }
                      ]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertTrue(result.hasArrayElements(), "wait should allow aggregating results into a list");
      assertEquals(2, result.getArraySize(), "Aggregated wait result should contain both task values");
      assertEquals(10, result.getArrayElement(0).asInt(), "First task result should be 10");
      assertEquals(20, result.getArrayElement(1).asInt(), "Second task result should be 20");
    }
  }

  @Test
  public void testWaitInNestedScope() throws Exception {
    String json = """
        {
          "name": "test.wait.scope",
          "decls": [
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": { "kind": "TypeName", "name": "Int" },
              "effects": ["Async"],
              "body": {
                "kind": "Block",
                "statements": [
                  {
                    "kind": "Start",
                    "name": "task",
                    "expr": { "kind": "Int", "value": 99 }
                  },
                  {
                    "kind": "Scope",
                    "statements": [
                      {
                        "kind": "Wait",
                        "names": ["task"]
                      },
                      {
                        "kind": "Return",
                        "expr": { "kind": "Name", "name": "task" }
                      }
                    ]
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertEquals(99, result.asInt(), "wait inside nested scope should still return the awaited value");
    }
  }

  // ==================== Effect Violations ====================

  @Test
  public void testEffectViolation_Async() throws Exception {
    String json = """
        {
          "name": "test.effect.async.violation",
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
                    "kind": "Start",
                    "name": "task1",
                    "expr": { "kind": "Int", "value": 42 }
                  },
                  {
                    "kind": "Return",
                    "expr": { "kind": "Int", "value": 0 }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Exception exception = assertThrows(Exception.class, () -> context.eval(source));
      assertTrue(exception.getMessage().contains("Async effect") ||
                 exception.getMessage().contains("start requires Async effect"),
                 "Should throw exception for missing Async effect");
    }
  }

  @Test
  public void testEffectViolation_IO() throws Exception {
    String json = """
        {
          "name": "test.effect.io.violation",
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
                    "kind": "Expr",
                    "expr": {
                      "kind": "Call",
                      "target": { "kind": "Name", "name": "print" },
                      "args": [{ "kind": "String", "value": "Hello" }]
                    }
                  },
                  {
                    "kind": "Return",
                    "expr": { "kind": "Int", "value": 0 }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Exception exception = assertThrows(Exception.class, () -> context.eval(source));
      assertTrue(exception.getMessage().contains("IO") ||
                 exception.getMessage().contains("effect"),
                 "Should throw exception for missing IO effect");
    }
  }

  // ==================== Advanced Async Scenarios ====================

  @Test
  public void testAsyncLiterals() throws Exception {
    String json = """
        {
          "name": "test.async.literals",
          "decls": [
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": { "kind": "TypeName", "name": "Int" },
              "effects": ["Async"],
              "body": {
                "kind": "Block",
                "statements": [
                  {
                    "kind": "Start",
                    "name": "task",
                    "expr": { "kind": "Int", "value": 99 }
                  },
                  {
                    "kind": "Return",
                    "expr": {
                      "kind": "Await",
                      "expr": { "kind": "Name", "name": "task" }
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertEquals(99, result.asInt(), "Async literal should return 99");
    }
  }

  @Test
  public void testResultInAsync() throws Exception {
    String json = """
        {
          "name": "test.result.in.async",
          "decls": [
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": { "kind": "TypeName", "name": "Int" },
              "effects": ["Async"],
              "body": {
                "kind": "Block",
                "statements": [
                  {
                    "kind": "Start",
                    "name": "task",
                    "expr": {
                      "kind": "Ok",
                      "expr": { "kind": "Int", "value": 99 }
                    }
                  },
                  {
                    "kind": "Let",
                    "name": "result",
                    "expr": {
                      "kind": "Await",
                      "expr": { "kind": "Name", "name": "task" }
                    }
                  },
                  {
                    "kind": "Return",
                    "expr": {
                      "kind": "Call",
                      "target": { "kind": "Name", "name": "Result.unwrap" },
                      "args": [{ "kind": "Name", "name": "result" }]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertEquals(99, result.asInt(), "Result in async should unwrap Ok value 99");
    }
  }

  @Test
  public void testOptionInAsync() throws Exception {
    String json = """
        {
          "name": "test.option.in.async",
          "decls": [
            {
              "kind": "Func",
              "name": "main",
              "params": [],
              "ret": { "kind": "TypeName", "name": "Int" },
              "effects": ["Async"],
              "body": {
                "kind": "Block",
                "statements": [
                  {
                    "kind": "Start",
                    "name": "task",
                    "expr": {
                      "kind": "Some",
                      "expr": { "kind": "Int", "value": 77 }
                    }
                  },
                  {
                    "kind": "Let",
                    "name": "option",
                    "expr": {
                      "kind": "Await",
                      "expr": { "kind": "Name", "name": "task" }
                    }
                  },
                  {
                    "kind": "Return",
                    "expr": {
                      "kind": "Call",
                      "target": { "kind": "Name", "name": "Option.unwrap" },
                      "args": [{ "kind": "Name", "name": "option" }]
                    }
                  }
                ]
              }
            }
          ]
        }
        """;

    try (Context context = Context.newBuilder("aster").allowAllAccess(true).build()) {
      Source source = Source.newBuilder("aster", json, "test.json").build();
      Value result = context.eval(source);
      assertEquals(77, result.asInt(), "Option in async should unwrap Some value 77");
    }
  }
}
