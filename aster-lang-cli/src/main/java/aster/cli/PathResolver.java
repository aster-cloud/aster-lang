package aster.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 负责路径解析与校验，确保 CLI 传入的路径全部基于项目根目录进行解析。
 */
public final class PathResolver {
  private final Path projectRoot;

  /**
   * @param projectRoot 项目根目录，作为相对路径解析基准
   */
  public PathResolver(Path projectRoot) {
    this.projectRoot = projectRoot.toAbsolutePath().normalize();
  }

  /**
   * 根据传入字符串解析路径，若为空则抛出参数错误。
   *
   * @param rawPath 原始路径参数
   * @param description 出错时的中文描述
   * @return 解析后的绝对路径
   * @throws CommandLineParser.CommandLineException 当参数为空时抛出
   */
  public Path resolvePath(String rawPath, String description)
      throws CommandLineParser.CommandLineException {
    if (rawPath == null || rawPath.isBlank()) {
      throw new CommandLineParser.CommandLineException("%s 未提供".formatted(description));
    }
    return resolveRelativeToRoot(rawPath);
  }

  /**
   * 解析并校验文件必须存在，为不可见文件或目录时报错。
   *
   * @param rawPath 原始路径参数
   * @param description 出错时的中文描述
   * @return 合法的文件路径
   * @throws CommandLineParser.CommandLineException 当文件不存在或类型不符时抛出
   */
  public Path resolveExistingFile(String rawPath, String description)
      throws CommandLineParser.CommandLineException {
    if (rawPath == null || rawPath.isBlank()) {
      throw new CommandLineParser.CommandLineException("%s 未提供".formatted(description));
    }
    final Path candidate = resolveRelativeToRoot(rawPath);
    if (!Files.exists(candidate)) {
      throw new CommandLineParser.CommandLineException(
          "%s 不存在: %s".formatted(description, candidate));
    }
    if (!Files.isRegularFile(candidate)) {
      throw new CommandLineParser.CommandLineException(
          "%s 不是文件: %s".formatted(description, candidate));
    }
    return candidate;
  }

  /**
   * 解析相对路径并归一化，保证路径安全。
   *
   * @param rawPath 原始路径
   * @return 标准化绝对路径
   */
  public Path resolveRelativeToRoot(String rawPath) {
    if (rawPath == null || rawPath.isBlank()) {
      throw new IllegalArgumentException("路径参数不能为空");
    }
    final Path given = Paths.get(rawPath);
    final Path resolved = given.isAbsolute() ? given : projectRoot.resolve(given);
    return resolved.toAbsolutePath().normalize();
  }
}
