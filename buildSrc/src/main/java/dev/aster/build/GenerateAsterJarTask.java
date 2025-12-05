package dev.aster.build;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;
import org.gradle.process.ExecOperations;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Gradle 任务：通过 npm 单步生成 aster.jar，并可选先编译 .aster 源文件
 *
 * 改进：
 * - 自动检测并替换占位符 JAR（小于 1KB 或不包含 .class 文件）
 * - 生成前清理输出目录，确保干净构建
 * - 验证生成的 JAR 包含实际的类文件
 */
@DisableCachingByDefault(because = "执行 npm 构建存在副作用，不适合缓存")
public abstract class GenerateAsterJarTask extends DefaultTask {

    /** 有效 JAR 的最小大小（字节），小于此值视为占位符 */
    private static final long MIN_VALID_JAR_SIZE = 1024;

    private final ExecOperations execOperations;

    @Inject
    public GenerateAsterJarTask(ExecOperations execOperations) {
        this.execOperations = execOperations;
        getWorkingDirectory().convention(getProject().getLayout().getProjectDirectory());
        getOutputDirectory().convention(getProject().getLayout().getBuildDirectory().dir("aster-out"));
        getOutputJar().convention(getOutputDirectory().map(dir -> dir.file("aster.jar")));

        // 如果输出 JAR 是无效的占位符，强制重新执行任务
        getOutputs().upToDateWhen(task -> {
            File jarFile = getOutputJar().get().getAsFile();
            return isValidJar(jarFile);
        });
    }

    @Internal
    public abstract DirectoryProperty getWorkingDirectory();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @OutputFile
    public abstract RegularFileProperty getOutputJar();

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getAsterSources();

    @Classpath
    @Optional
    public abstract ConfigurableFileCollection getWorkflowClasspath();

    @TaskAction
    public void generate() {
        File workDirFile = getWorkingDirectory().get().getAsFile();
        File outDirFile = getOutputDirectory().get().getAsFile();
        File jarFile = getOutputJar().get().getAsFile();

        // 如果存在无效的 JAR，先清理
        if (jarFile.exists() && !isValidJar(jarFile)) {
            getLogger().lifecycle("检测到无效的占位符 JAR，正在清理: {}", jarFile.getAbsolutePath());
            cleanOutputDirectory(outDirFile);
        }

        if (!outDirFile.exists() && !outDirFile.mkdirs()) {
            throw new GradleException("无法创建输出目录: " + outDirFile.getAbsolutePath());
        }

        Map<String, Object> env = new HashMap<>();
        env.put("ASTER_OUT_DIR", computeOutDirEnv(workDirFile, outDirFile));

        var classpathEntries = getWorkflowClasspath().getFiles();
        if (!classpathEntries.isEmpty()) {
            String existing = System.getenv("CLASSPATH");
            StringBuilder combined = new StringBuilder();
            if (existing != null && !existing.isBlank()) {
                combined.append(existing).append(File.pathSeparator);
            }
            combined.append(classpathEntries.stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.joining(File.pathSeparator)));
            env.put("CLASSPATH", combined.toString());
        }

        List<File> sources = getAsterSources().getFiles().stream()
                .sorted((a, b) -> a.getAbsolutePath().compareTo(b.getAbsolutePath()))
                .toList();

        if (!sources.isEmpty()) {
            List<String> args = sources.stream()
                    .map(f -> toRelativeCommandArg(f, workDirFile))
                    .toList();
            getLogger().lifecycle("编译 {} 个 .aster 源文件...", sources.size());
            execOperations.exec(spec -> {
                spec.setWorkingDir(workDirFile);
                spec.environment(env);
                spec.commandLine(buildNpmCommand("emit:class", args));
            });
        } else {
            getLogger().warn("未提供 .aster 源文件，JAR 可能为空");
        }

        getLogger().lifecycle("生成 JAR: {}", jarFile.getAbsolutePath());
        execOperations.exec(spec -> {
            spec.setWorkingDir(workDirFile);
            spec.environment(env);
            spec.commandLine(buildNpmCommand("jar:jvm", List.of()));
        });

        // 验证生成的 JAR
        if (!jarFile.exists()) {
            throw new GradleException("npm run jar:jvm 未生成预期的 JAR: " + jarFile.getAbsolutePath());
        }

        if (!isValidJar(jarFile)) {
            throw new GradleException("生成的 JAR 无效（可能是占位符或不包含类文件）: " + jarFile.getAbsolutePath() +
                    " (大小: " + jarFile.length() + " 字节)");
        }

        getLogger().lifecycle("✓ JAR 验证通过: {} ({} 字节)", jarFile.getName(), jarFile.length());
    }

    /**
     * 检查 JAR 是否有效（不是占位符）
     */
    private boolean isValidJar(File jarFile) {
        if (!jarFile.exists()) {
            return false;
        }

        // 检查文件大小
        if (jarFile.length() < MIN_VALID_JAR_SIZE) {
            getLogger().debug("JAR 文件过小 ({} 字节 < {} 字节最小值): {}",
                    jarFile.length(), MIN_VALID_JAR_SIZE, jarFile.getAbsolutePath());
            return false;
        }

        // 检查是否包含 .class 文件
        try (JarFile jar = new JarFile(jarFile)) {
            boolean hasClassFiles = jar.stream()
                    .anyMatch(entry -> entry.getName().endsWith(".class") &&
                                      !entry.getName().contains("META-INF"));
            if (!hasClassFiles) {
                getLogger().debug("JAR 不包含类文件: {}", jarFile.getAbsolutePath());
                return false;
            }
            return true;
        } catch (IOException e) {
            getLogger().debug("无法读取 JAR 文件: {} - {}", jarFile.getAbsolutePath(), e.getMessage());
            return false;
        }
    }

    /**
     * 清理输出目录
     */
    private void cleanOutputDirectory(File outDir) {
        if (outDir.exists()) {
            try {
                Files.walk(outDir.toPath())
                        .sorted((a, b) -> b.compareTo(a)) // 反向排序，先删除文件再删除目录
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                getLogger().warn("无法删除: {}", path);
                            }
                        });
            } catch (IOException e) {
                getLogger().warn("清理输出目录失败: {}", e.getMessage());
            }
        }
    }

    private List<String> buildNpmCommand(String script, List<String> args) {
        List<String> base = new ArrayList<>();
        base.add("npm");
        base.add("run");
        base.add(script);
        if (!args.isEmpty()) {
            base.add("--");
            base.addAll(args);
        }
        if (isWindows()) {
            List<String> result = new ArrayList<>();
            result.add("cmd");
            result.add("/c");
            result.addAll(base);
            return result;
        }
        return base;
    }

    private String computeOutDirEnv(File workDir, File outDir) {
        Path workPath = workDir.toPath().toAbsolutePath().normalize();
        Path outPath = outDir.toPath().toAbsolutePath().normalize();
        if (outPath.startsWith(workPath)) {
            return workPath.relativize(outPath).toString().replace(File.separatorChar, '/');
        }
        return outPath.toString();
    }

    private String toRelativeCommandArg(File file, File baseDir) {
        Path basePath = baseDir.toPath().toAbsolutePath().normalize();
        Path targetPath = file.toPath().toAbsolutePath().normalize();
        if (targetPath.startsWith(basePath)) {
            return basePath.relativize(targetPath).toString().replace(File.separatorChar, '/');
        }
        return targetPath.toString();
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
