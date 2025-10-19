package aster.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 命令行解析工具，支持 <code>--flag value</code> 与 <code>--flag=value</code> 两种模式。
 * 解析结果用于 Main 中的命令路由，保持外层逻辑的 switch-case 简洁。
 */
public final class CommandLineParser {

  private CommandLineParser() {}

  /**
   * 解析原始参数并拆分出命令、位置参数与选项。
   *
   * @param args CLI 输入
   * @return 解析结果
   */
  public static ParsedCommand parse(String[] args) {
    Objects.requireNonNull(args, "args");
    if (args.length == 0) {
      return new ParsedCommand(null, List.of(), Map.of(), false, false);
    }

    final List<String> tokens = List.of(args);
    final List<String> positionals = new ArrayList<>();
    final Map<String, String> options = new LinkedHashMap<>();
    boolean endOfOptions = false;

    for (int i = 0; i < tokens.size(); i++) {
      final String token = tokens.get(i);
      if (endOfOptions) {
        positionals.add(token);
        continue;
      }
      if ("--".equals(token)) {
        endOfOptions = true;
        continue;
      }
      if (token.startsWith("--")) {
        final String withoutPrefix = token.substring(2);
        if (withoutPrefix.isEmpty()) {
          // "--" 已在前面处理，这里兜底避免空键
          continue;
        }
        final int eqIdx = withoutPrefix.indexOf('=');
        if (eqIdx >= 0) {
          final String key = withoutPrefix.substring(0, eqIdx);
          final String value = withoutPrefix.substring(eqIdx + 1);
          options.put(key, value);
        } else {
          final String nextValue = i + 1 < tokens.size() ? tokens.get(i + 1) : null;
          if (nextValue != null && !nextValue.startsWith("--")) {
            options.put(withoutPrefix, nextValue);
            i++;
          } else {
            options.put(withoutPrefix, "true");
          }
        }
        continue;
      }
      positionals.add(token);
    }

    final String command = positionals.isEmpty() ? null : positionals.get(0);
    final List<String> remaining =
        positionals.isEmpty()
            ? List.of()
            : Collections.unmodifiableList(positionals.subList(1, positionals.size()));
    final boolean helpRequested =
        options.containsKey("help") || options.containsKey("h") || "--help".equals(command);
    final boolean versionRequested =
        options.containsKey("version") || options.containsKey("v") || "--version".equals(command);
    return new ParsedCommand(command, remaining, Collections.unmodifiableMap(options),
        helpRequested, versionRequested);
  }

  /**
   * 校验输入文件是否存在。
   *
   * @param value 参数提供的文件路径
   * @param description 用于报错的友好描述
   * @return 绝对路径
   * @throws CommandLineException 当文件不存在或参数为空时抛出
   */
  public static Path requireExistingFile(String value, String description)
      throws CommandLineException {
    if (value == null || value.isBlank()) {
      throw new CommandLineException(description + " 未提供");
    }
    final Path path = Paths.get(value).toAbsolutePath().normalize();
    if (!Files.exists(path)) {
      throw new CommandLineException(description + " 不存在: " + path);
    }
    if (!Files.isRegularFile(path)) {
      throw new CommandLineException(description + " 不是文件: " + path);
    }
    return path;
  }

  /**
   * 将输出路径标准化为绝对路径。目录不存在时延迟到下游命令处理。
   *
   * @param value 原始路径
   * @param description 用于报错的说明
   * @return 绝对路径
   * @throws CommandLineException 当参数为空时抛出
   */
  public static Path normalizePath(String value, String description) throws CommandLineException {
    if (value == null || value.isBlank()) {
      throw new CommandLineException(description + " 未提供");
    }
    return Paths.get(value).toAbsolutePath().normalize();
  }

  /**
   * 解析结果的不可变封装。
   */
  public static final class ParsedCommand {
    private final String command;
    private final List<String> arguments;
    private final Map<String, String> options;
    private final boolean helpRequested;
    private final boolean versionRequested;

    ParsedCommand(
        String command,
        List<String> arguments,
        Map<String, String> options,
        boolean helpRequested,
        boolean versionRequested) {
      this.command = command;
      this.arguments = arguments;
      this.options = options;
      this.helpRequested = helpRequested;
      this.versionRequested = versionRequested;
    }

    public String command() {
      return command;
    }

    public List<String> arguments() {
      return arguments;
    }

    public Map<String, String> options() {
      return options;
    }

    public boolean isHelpRequested() {
      return helpRequested;
    }

    public boolean isVersionRequested() {
      return versionRequested;
    }

    public String option(String name) {
      return options.get(name);
    }

    public boolean flag(String name) {
      return "true".equalsIgnoreCase(options.get(name));
    }
  }

  /**
   * 命令行参数错误，用于统一错误处理。
   */
  public static final class CommandLineException extends Exception {
    private static final long serialVersionUID = 1L;

    public CommandLineException(String message) {
      super(message);
    }
  }
}
