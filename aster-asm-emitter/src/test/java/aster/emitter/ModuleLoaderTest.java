package aster.emitter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 ModuleLoader 在不同输入与环境变量下的行为。
 */
class ModuleLoaderTest {

  @AfterEach
  void tearDown() {
    ModuleLoader.clearEnvOverrideForTesting();
  }

  @Test
  void testLoadValidModule() throws IOException {
    Map<String, String> env = new HashMap<>();
    ModuleLoader.setEnvOverrideForTesting(env);

    CoreContext ctx = ModuleLoader.load(sampleModule());

    assertNotNull(ctx.module());
    assertEquals("demo", ctx.module().name);
    assertTrue(ctx.hints().isEmpty());
    assertFalse(ctx.nullPolicy().strict());
    assertTrue(ctx.nullPolicy().overrides().isEmpty());
    assertTrue(ctx.diagOverload());
  }

  @Test
  void testLoadHintsFile(@TempDir Path tempDir) throws IOException {
    Path hintsFile = tempDir.resolve("hints.json");
    String hintsJson = """
        {
          "functions": {
            "app.add": {
              "lhs": "I",
              "rhs": "J",
              "ignored": "X"
            }
          }
        }
        """;
    Files.writeString(hintsFile, hintsJson, StandardCharsets.UTF_8);

    Map<String, String> env = new HashMap<>();
    env.put("HINTS_PATH", hintsFile.toString());
    ModuleLoader.setEnvOverrideForTesting(env);

    CoreContext ctx = ModuleLoader.load(sampleModule());

    assertTrue(ctx.hints().containsKey("app.add"));
    Map<String, String> addHints = ctx.hints().get("app.add");
    assertEquals("I", addHints.get("lhs"));
    assertEquals("J", addHints.get("rhs"));
    assertNull(addHints.get("ignored"));
  }

  @Test
  void testLoadNullPolicyFromEnv(@TempDir Path tempDir) throws IOException {
    Path policyFile = tempDir.resolve("policy.json");
    String policyJson = """
        {
          "Text.concat": [true, false],
          "Text.length": [false]
        }
        """;
    Files.writeString(policyFile, policyJson, StandardCharsets.UTF_8);

    Map<String, String> env = new HashMap<>();
    env.put("NULL_STRICT", "true");
    env.put("NULL_POLICY_OVERRIDE", policyFile.toString());
    env.put("DIAG_OVERLOAD", "false");
    ModuleLoader.setEnvOverrideForTesting(env);

    CoreContext ctx = ModuleLoader.load(sampleModule());

    assertTrue(ctx.nullPolicy().strict());
    assertEquals(2, ctx.nullPolicy().overrides().size());
    assertArrayEquals(new boolean[]{ true, false }, ctx.nullPolicy().overrides().get("Text.concat"));
    assertArrayEquals(new boolean[]{ false }, ctx.nullPolicy().overrides().get("Text.length"));
    assertFalse(ctx.diagOverload());
  }

  @Test
  void testLoadWithMissingHints(@TempDir Path tempDir) throws IOException {
    Path missing = tempDir.resolve("missing.json");
    Map<String, String> env = new HashMap<>();
    env.put("HINTS_PATH", missing.toString());
    ModuleLoader.setEnvOverrideForTesting(env);

    CoreContext ctx = ModuleLoader.load(sampleModule());

    assertTrue(ctx.hints().isEmpty());
  }

  private InputStream sampleModule() {
    String json = """
        {
          "name": "demo",
          "decls": []
        }
        """;
    return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
  }
}
