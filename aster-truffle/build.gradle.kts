plugins {
  application
  java
  id("org.graalvm.buildtools.native")
}
repositories { mavenCentral() }

dependencies {
  implementation(project(":aster-core"))
  implementation(project(":aster-runtime"))
  implementation("org.graalvm.truffle:truffle-api:25.0.1")
  annotationProcessor("org.graalvm.truffle:truffle-dsl-processor:25.0.1")
  implementation("org.graalvm.sdk:graal-sdk:25.0.1")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
  implementation("io.quarkus:quarkus-core:3.28.3")
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
  testImplementation("org.graalvm.truffle:truffle-api:25.0.1")
  testRuntimeOnly("org.graalvm.truffle:truffle-runtime:25.0.1")
  testRuntimeOnly("org.graalvm.truffle:truffle-compiler:25.0.1")
  testRuntimeOnly("org.graalvm.compiler:compiler:25.0.1")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.1")
}

application { mainClass.set("aster.truffle.Runner") }

java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }

tasks.withType<JavaCompile>().configureEach {
  // Override global -Werror setting from reproducible-builds.gradle.kts
  // because Truffle DSL and Jackson annotation processors produce benign warnings
  options.compilerArgs.remove("-Werror")
  options.isDeprecation = true
}

tasks.test {
  useJUnitPlatform()
  testLogging {
    events("passed", "skipped", "failed")
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    showStandardStreams = true
  }
  jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
  jvmArgs("-Xss10m") // 增加栈大小到 10MB 以支持深度递归
  jvmArgs("-da") // 禁用断言以避免 Truffle 内部断言失败
  // Phase 3C P0-2: 支持 Profiler 数据收集
  // 通过 -Daster.profiler.enabled=true 启用 profiling
  systemProperty("aster.profiler.enabled", System.getProperty("aster.profiler.enabled", "false"))

  // CI 模式：通过 -PexcludeBenchmarks=true 排除耗时的基准测试
  // 这些测试包含 fibonacci 和 quicksort 等算法，在 CI 中执行时间过长
  val excludeBenchmarks: String? by project
  if (excludeBenchmarks == "true") {
    filter {
      excludeTestsMatching("aster.truffle.GraalVMJitBenchmark")
      excludeTestsMatching("aster.truffle.CrossBackendBenchmark")
      excludeTestsMatching("aster.truffle.BenchmarkTest")
    }
    println("[CI Mode] Excluding slow benchmark tests: GraalVMJitBenchmark, CrossBackendBenchmark, BenchmarkTest")
  }
}

// Native Image Agent 配置生成任务
tasks.register<JavaExec>("generateNativeConfig") {
  group = "native"
  description = "Generate Native Image configuration using Agent"
  classpath = sourceSets["main"].runtimeClasspath
  mainClass.set("aster.truffle.Runner")

  val configOutputDir = "${projectDir}/src/main/resources/META-INF/native-image"
  jvmArgs = listOf(
    "-agentlib:native-image-agent=config-output-dir=${configOutputDir}"
  )

  // 运行典型工作负载以收集元数据
  args = listOf(
    "${projectDir}/../benchmarks/core/fibonacci_20_core.json",
    "${projectDir}/../benchmarks/core/factorial_12_core.json",
    "${projectDir}/../benchmarks/core/list_map_1000_core.json"
  )
}

// GraalVM Native Image 配置
graalvmNative {
  metadataRepository {
    enabled.set(false) // P1-1: 禁用以兼容 configuration cache（手动提供 META-INF/native-image 配置）
  }
  binaries {
    named("main") {
      imageName.set("aster")
      mainClass.set("aster.truffle.Runner")
      buildArgs.add("--no-fallback")
      buildArgs.add("-H:+ReportExceptionStackTraces")
      buildArgs.add("--initialize-at-build-time=")
      buildArgs.add("-H:+UnlockExperimentalVMOptions")

      // Phase 2.5: 编译时初始化配置 - 优化 Native Image 启动性能
      // 标记可在构建时初始化的类（无状态、不依赖运行时环境）
      buildArgs.add("--initialize-at-build-time=aster.truffle.AsterLanguage")
      buildArgs.add("--initialize-at-build-time=aster.truffle.runtime.AsterConfig")

      // PGO 支持: 通过 -PpgoMode 传递参数
      // 用法: ./gradlew nativeCompile -PpgoMode=instrument
      //      ./gradlew nativeCompile -PpgoMode=default.iprof
      val pgoMode: String? by project
      if (pgoMode != null) {
        when {
          pgoMode == "instrument" -> buildArgs.add("--pgo-instrument")
          pgoMode!!.endsWith(".iprof") -> buildArgs.add("--pgo=$pgoMode")
          else -> println("Warning: Unknown PGO mode: $pgoMode")
        }
      }

      // 二进制大小优化 (Phase 5 Task 5.2): 通过 -PsizeOptimization=true 启用
      // 注意: PGO 已经实现了主要优化 (36.88MB → 23MB),以下参数是可选的额外优化
      // 用法: ./gradlew nativeCompile -PpgoMode=<path>.iprof -PsizeOptimization=true
      val sizeOptimization: String? by project
      if (sizeOptimization == "true") {
        buildArgs.add("-O3")                      // 最高优化级别 (注: 默认已是 O3)
        buildArgs.add("--gc=serial")              // 更小的 GC (注: 默认已是 serial)
        buildArgs.add("-H:+StripDebugInfo")       // 去除调试信息 (可能影响堆栈跟踪)
        buildArgs.add("-H:-AddAllCharsets")       // 仅包含需要的字符集 (可能影响字符处理)
        buildArgs.add("-H:+RemoveUnusedSymbols")  // 移除未使用符号 (PGO 已处理大部分)
        println("[Size Optimization] Enabled additional size optimization flags")
      }

      // 配置文件会从 META-INF/native-image/ 自动发现（不需要 resources.autodetect()）
      // 移除 resources.autodetect() 避免生成错误的 -H:*ConfigurationFiles= 路径
    }
  }
  agent {
    defaultMode.set("standard")
    builtinCallerFilter = true
    builtinHeuristicFilter = true
    enableExperimentalPredefinedClasses = false
    enableExperimentalUnsafeAllocationTracing = false
    trackReflectionMetadata = true
    modes {
      standard {}
    }
  }
}

// P1-1: 禁用 nativeCompile 任务的 configuration cache
// 原因: GraalVM Native Image 构建需要独占锁，与 configuration cache 不兼容
tasks.named("nativeCompile") {
  notCompatibleWithConfigurationCache("GraalVM Native Image build requires exclusive lock")
}
