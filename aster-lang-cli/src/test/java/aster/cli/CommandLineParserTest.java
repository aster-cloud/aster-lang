package aster.cli;

import static org.junit.jupiter.api.Assertions.*;

import aster.cli.CommandLineParser.CommandLineException;
import aster.cli.CommandLineParser.ParsedCommand;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommandLineParserTest {

  @Test
  void parseSupportsFlagValue() {
    final ParsedCommand parsed =
        CommandLineParser.parse(new String[] {"compile", "hello.aster", "--output", "build/out"});
    assertEquals("compile", parsed.command());
    assertEquals(1, parsed.arguments().size());
    assertEquals("hello.aster", parsed.arguments().getFirst());
    assertEquals("build/out", parsed.options().get("output"));
  }

  @Test
  void parseSupportsFlagEquals() {
    final ParsedCommand parsed =
        CommandLineParser.parse(new String[] {"typecheck", "demo.aster", "--caps=config.json"});
    assertEquals("typecheck", parsed.command());
    assertEquals(Map.of("caps", "config.json"), parsed.options());
  }

  @Test
  void helpFlagSetsHelpRequested() {
    final ParsedCommand parsed =
        CommandLineParser.parse(new String[] {"compile", "--help"});
    assertTrue(parsed.isHelpRequested());
  }

  @Test
  void requireExistingFileValidatesFile(@TempDir Path tempDir) throws IOException {
    final Path file = tempDir.resolve("hello.aster");
    Files.writeString(file, "content");
    assertDoesNotThrow(() -> CommandLineParser.requireExistingFile(file.toString(), "测试文件"));
  }

  @Test
  void requireExistingFileThrowsWhenMissing() {
    final CommandLineException exception =
        assertThrows(
            CommandLineException.class,
            () -> CommandLineParser.requireExistingFile("missing.aster", "测试文件"));
    assertTrue(exception.getMessage().contains("不存在"));
  }
}
