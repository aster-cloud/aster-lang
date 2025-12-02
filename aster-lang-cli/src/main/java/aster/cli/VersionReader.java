package aster.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 读取 CLI 所需的版本号信息，优先使用 package.json，其次回退到 build.gradle.kts。
 */
public final class VersionReader {
  private static final Pattern PACKAGE_VERSION =
      Pattern.compile("\"version\"\\s*:\\s*\"(?<version>[^\"]+)\"");
  private static final Pattern GRADLE_VERSION =
      Pattern.compile("version\\s*=\\s*\"(?<version>[^\"]+)\"");

  /**
   * @param projectRoot 仓库根目录
   * @return 成功解析到的版本号；若无法解析则返回空
   */
  public Optional<String> readVersion(Path projectRoot) {
    final Path root = projectRoot.toAbsolutePath().normalize();
    final Path packageJson = root.resolve("package.json");
    return readFromPackage(packageJson)
        .or(() -> readFromGradle(root.resolve("build.gradle.kts")));
  }

  private Optional<String> readFromPackage(Path packageJson) {
    if (!Files.exists(packageJson)) {
      return Optional.empty();
    }
    try {
      final String content = Files.readString(packageJson, StandardCharsets.UTF_8);
      final var matcher = PACKAGE_VERSION.matcher(content);
      return matcher.find() ? Optional.ofNullable(matcher.group("version")) : Optional.empty();
    } catch (IOException _) {
      return Optional.empty();
    }
  }

  private Optional<String> readFromGradle(Path gradleFile) {
    if (!Files.exists(gradleFile)) {
      return Optional.empty();
    }
    try {
      final String content = Files.readString(gradleFile, StandardCharsets.UTF_8);
      final var matcher = GRADLE_VERSION.matcher(content);
      return matcher.find() ? Optional.ofNullable(matcher.group("version")) : Optional.empty();
    } catch (IOException _) {
      return Optional.empty();
    }
  }
}
