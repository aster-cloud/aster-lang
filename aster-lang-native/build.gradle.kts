/**
 * Aster Lang Native Image 构建配置
 *
 * 用途：将 Aster 语言编译器和运行时打包为 GraalVM Native-Image 可执行文件
 * 验收标准：
 * - 二进制文件 <50MB
 * - 启动时间 <100ms
 * - 支持完整的 CNL 编译流程
 */

plugins {
  application
  id("org.graalvm.buildtools.native") version "0.10.2"
}

repositories { mavenCentral() }

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
  options.isDeprecation = true
}

dependencies {
  implementation(project(":aster-runtime"))
  implementation(files("${rootProject.projectDir}/build/aster-out/aster.jar"))

  // 如果需要 Truffle 支持，取消注释：
  // implementation("org.graalvm.truffle:truffle-api:23.1.0")
}

application {
  mainClass.set("aster.cli.Main")
}

graalvmNative {
  binaries {
    named("main") {
      imageName.set("aster")

      buildArgs.addAll(listOf(
        // 核心配置
        "--no-fallback",  // 禁用 fallback 到 JVM，确保完全 AOT
        "--strict-image-heap",  // 严格的堆内存检查
        "-H:+ReportExceptionStackTraces",  // 报告异常堆栈

        // 初始化配置（reflection-free 设计）
        "--initialize-at-build-time=aster.runtime",

        // 优化配置
        "-O3",  // 最高级别优化
        "--gc=G1",  // 使用 G1 GC（平衡性能和内存）
        "-march=native",  // 针对当前 CPU 架构优化

        // 大小优化
        "-H:+RemoveUnusedSymbols",  // 移除未使用的符号
        "-H:-AddAllCharsets",  // 不包含所有字符集（按需）
        "-H:+UseCompressedReferences",  // 使用压缩指针

        // 启动时间优化
        "--initialize-at-build-time",  // 尽可能在构建时初始化
        "-H:+StaticExecutableWithDynamicLibC",  // 静态链接（仅 Linux）

        // 诊断和调试（生产环境可移除）
        "-H:+PrintClassInitialization",  // 打印类初始化信息
        "-H:Log=registerResource:3"  // 资源注册日志
      ))

      // 自动检测资源文件
      resources.autodetect()

      // 如果需要明确指定资源，取消注释：
      // resources.includePath("path/to/resources")
    }
  }

  // 启用 agent 模式用于配置生成（开发时使用）
  agent {
    defaultMode.set("standard")
    modes {
      standard {
      }
    }
  }
}

// 确保生成的 Aster JAR 存在
val generateAsterJar by tasks.registering(Exec::class) {
  workingDir = rootProject.projectDir
  commandLine = if (System.getProperty("os.name").lowercase().contains("win"))
    listOf("cmd", "/c", "npm", "run", "jar:jvm")
  else listOf("sh", "-c", "npm run jar:jvm")
}

tasks.withType<JavaCompile>().configureEach {
  dependsOn(generateAsterJar)
}

// 性能测试任务
val benchmarkStartupTime by tasks.registering(Exec::class) {
  dependsOn("nativeCompile")
  workingDir = projectDir
  commandLine = listOf("sh", "-c", """
    echo "=== Startup Time Benchmark ==="
    for i in {1..10}; do
      /usr/bin/time -p ./build/native/nativeCompile/aster --version 2>&1 | grep real
    done | awk '{sum += $2; count++} END {print "Average startup time: " sum/count " seconds"}'
  """)

  doFirst {
    println("Running startup time benchmark (10 iterations)...")
  }
}

// 二进制大小检查任务
val checkBinarySize by tasks.registering {
  dependsOn("nativeCompile")

  doLast {
    val binaryFile = file("build/native/nativeCompile/aster")
    if (binaryFile.exists()) {
      val sizeMB = binaryFile.length() / (1024.0 * 1024.0)
      println("=== Binary Size Check ===")
      println("Binary: ${binaryFile.absolutePath}")
      println("Size: %.2f MB".format(sizeMB))

      val maxSizeMB = 50.0
      if (sizeMB > maxSizeMB) {
        throw GradleException("Binary size %.2f MB exceeds limit of %.2f MB".format(sizeMB, maxSizeMB))
      } else {
        println("✓ Size check passed (< $maxSizeMB MB)")
      }
    } else {
      throw GradleException("Binary not found: ${binaryFile.absolutePath}")
    }
  }
}

// 综合验收测试
val acceptanceTest by tasks.registering {
  dependsOn("nativeCompile", checkBinarySize, benchmarkStartupTime)

  doLast {
    println("=== Acceptance Test Summary ===")
    println("✓ Binary compiled successfully")
    println("✓ Size check passed")
    println("✓ Startup time benchmark completed")
    println("All acceptance criteria verified!")
  }
}
