package aster.emitter;

import static org.junit.jupiter.api.Assertions.*;

import aster.core.ir.CoreModel;
import aster.core.lowering.CoreLowering;
import aster.core.parser.AsterCustomLexer;
import aster.core.parser.AsterParser;
import aster.core.parser.AstBuilder;
import aster.runtime.AsterCapability;
import aster.runtime.AsterPii;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class AnnotationEmissionTest {

  @TempDir
  Path tempDir;

  @Test
  void emittedFunctionCarriesPiiAndCapabilityAnnotations() throws Exception {
    Path source = resolveFixture("test/fixtures/annotation-test.aster");
    CoreModel.Module module = lowerFixture(source);
    Main.CompileResult result = Main.compile(module, tempDir, Map.of());
    assertTrue(result.success, "编译 fixture 失败: " + result.errors);

    InMemoryClassLoader loader = new InMemoryClassLoader();
    Path classFile = tempDir.resolve("test/annotations/runtime/get_user_profile_fn.class");
    assertTrue(Files.exists(classFile), "未生成期望的函数类文件");
    Class<?> clazz =
        defineGeneratedClass(loader, "test.annotations.runtime.get_user_profile_fn", classFile);
    Method method = clazz.getMethod("get_user_profile", String.class);

    AsterPii pii = method.getAnnotation(AsterPii.class);
    assertNotNull(pii, "缺少 @AsterPii 注解");
    assertEquals("L2", pii.level());
    assertArrayEquals(new String[] { "name", "email" }, pii.categories());

    AsterCapability capability = method.getAnnotation(AsterCapability.class);
    assertNotNull(capability, "缺少 @AsterCapability 注解");
    assertArrayEquals(new String[] { "io" }, capability.effects());
    assertArrayEquals(new String[] { "Http" }, capability.capabilities());
  }

  private CoreModel.Module lowerFixture(Path sourcePath) throws IOException {
    String source = Files.readString(sourcePath);
    CharStream stream = CharStreams.fromString(source);
    AsterCustomLexer lexer = new AsterCustomLexer(stream);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    AsterParser parser = new AsterParser(tokens);
    aster.core.ast.Module astModule = new AstBuilder().visitModule(parser.module());
    return new CoreLowering().lowerModule(astModule);
  }

  private Class<?> defineGeneratedClass(InMemoryClassLoader loader, String name, Path classFile)
      throws IOException {
    byte[] bytecode = Files.readAllBytes(classFile);
    return loader.define(name, bytecode);
  }

  private Path resolveFixture(String relative) throws IOException {
    Path cursor = Paths.get("").toAbsolutePath();
    while (cursor != null) {
      Path candidate = cursor.resolve(relative);
      if (Files.exists(candidate)) {
        return candidate;
      }
      cursor = cursor.getParent();
    }
    throw new IOException("无法定位 fixture: " + relative);
  }

  private static final class InMemoryClassLoader extends ClassLoader {
    Class<?> define(String name, byte[] bytecode) {
      return defineClass(name, bytecode, 0, bytecode.length);
    }
  }
}
