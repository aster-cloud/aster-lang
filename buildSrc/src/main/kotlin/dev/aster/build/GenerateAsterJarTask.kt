package dev.aster.build

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "执行 npm 构建存在副作用，不适合缓存")
abstract class GenerateAsterJarTask @Inject constructor(
  private val execOperations: ExecOperations
) : DefaultTask() {

  // Gradle 任务：通过 npm 单步生成 aster.jar，并可选先编译 .aster 源文件

  @get:Internal
  abstract val workingDirectory: DirectoryProperty

  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  @get:OutputFile
  abstract val outputJar: RegularFileProperty

  @get:InputFiles
  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val asterSources: ConfigurableFileCollection

  @get:Classpath
  @get:Optional
  abstract val workflowClasspath: ConfigurableFileCollection

  init {
    workingDirectory.convention(project.layout.projectDirectory)
    outputDirectory.convention(project.layout.buildDirectory.dir("aster-out"))
    outputJar.convention(outputDirectory.map { it.file("aster.jar") })
  }

  @TaskAction
  fun generate() {
    val workDirFile = workingDirectory.get().asFile
    val outDirFile = outputDirectory.get().asFile

    if (!outDirFile.exists() && !outDirFile.mkdirs()) {
      throw GradleException("无法创建输出目录: ${outDirFile.absolutePath}")
    }

    val env = mutableMapOf<String, Any>("ASTER_OUT_DIR" to computeOutDirEnv(workDirFile, outDirFile))
    val classpathEntries = workflowClasspath.files
    if (classpathEntries.isNotEmpty()) {
      val existing = System.getenv("CLASSPATH").orEmpty()
      val combined = buildString {
        if (existing.isNotBlank()) {
          append(existing)
          append(File.pathSeparator)
        }
        append(classpathEntries.joinToString(File.pathSeparator) { it.absolutePath })
      }
      env["CLASSPATH"] = combined
    }

    val sources = asterSources.files.sortedBy { it.absolutePath }
    if (sources.isNotEmpty()) {
      execOperations.exec {
        workingDir = workDirFile
        environment(env)
        commandLine(buildNpmCommand("emit:class", sources.map { it.toRelativeCommandArg(workDirFile) }))
      }
    } else {
      logger.lifecycle("未提供 .aster 源文件，直接执行 jar:jvm，可能生成占位 JAR")
    }

    execOperations.exec {
      workingDir = workDirFile
      environment(env)
      commandLine(buildNpmCommand("jar:jvm"))
    }

    val jarFile = outputJar.get().asFile
    if (!jarFile.exists()) {
      throw GradleException("npm run jar:jvm 未生成预期的 JAR: ${jarFile.absolutePath}")
    }
  }

  private fun buildNpmCommand(script: String, args: List<String> = emptyList()): List<String> {
    val base = mutableListOf("npm", "run", script)
    if (args.isNotEmpty()) {
      base.add("--")
      base.addAll(args)
    }
    return if (isWindows()) listOf("cmd", "/c") + base else base
  }

  private fun computeOutDirEnv(workDir: File, outDir: File): String {
    val workPath = workDir.toPath().toAbsolutePath().normalize()
    val outPath = outDir.toPath().toAbsolutePath().normalize()
    return if (outPath.startsWith(workPath)) {
      workPath.relativize(outPath).toString().replace(File.separatorChar, '/')
    } else {
      outPath.toString()
    }
  }

  private fun File.toRelativeCommandArg(baseDir: File): String {
    val basePath = baseDir.toPath().toAbsolutePath().normalize()
    val targetPath = toPath().toAbsolutePath().normalize()
    return if (targetPath.startsWith(basePath)) {
      basePath.relativize(targetPath).toString().replace(File.separatorChar, '/')
    } else {
      targetPath.toString()
    }
  }

  private fun isWindows(): Boolean = System.getProperty("os.name").lowercase().contains("win")
}
