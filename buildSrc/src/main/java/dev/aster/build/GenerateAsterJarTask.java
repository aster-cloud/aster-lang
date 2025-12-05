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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gradle 任务：通过 npm 单步生成 aster.jar，并可选先编译 .aster 源文件
 */
@DisableCachingByDefault(because = "执行 npm 构建存在副作用，不适合缓存")
public abstract class GenerateAsterJarTask extends DefaultTask {

    private final ExecOperations execOperations;

    @Inject
    public GenerateAsterJarTask(ExecOperations execOperations) {
        this.execOperations = execOperations;
        getWorkingDirectory().convention(getProject().getLayout().getProjectDirectory());
        getOutputDirectory().convention(getProject().getLayout().getBuildDirectory().dir("aster-out"));
        getOutputJar().convention(getOutputDirectory().map(dir -> dir.file("aster.jar")));
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
            execOperations.exec(spec -> {
                spec.setWorkingDir(workDirFile);
                spec.environment(env);
                spec.commandLine(buildNpmCommand("emit:class", args));
            });
        } else {
            getLogger().lifecycle("未提供 .aster 源文件，直接执行 jar:jvm，可能生成占位 JAR");
        }

        execOperations.exec(spec -> {
            spec.setWorkingDir(workDirFile);
            spec.environment(env);
            spec.commandLine(buildNpmCommand("jar:jvm", List.of()));
        });

        File jarFile = getOutputJar().get().getAsFile();
        if (!jarFile.exists()) {
            throw new GradleException("npm run jar:jvm 未生成预期的 JAR: " + jarFile.getAbsolutePath());
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
