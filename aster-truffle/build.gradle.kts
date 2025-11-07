plugins {
  application
  java
  id("org.graalvm.buildtools.native")
}
repositories { mavenCentral() }

dependencies {
  implementation("org.graalvm.truffle:truffle-api:25.0.0")
  annotationProcessor("org.graalvm.truffle:truffle-dsl-processor:25.0.0")
  implementation("org.graalvm.sdk:graal-sdk:25.0.0")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
  testImplementation("org.graalvm.truffle:truffle-api:25.0.0")
  testRuntimeOnly("org.graalvm.truffle:truffle-runtime:25.0.0")
  testRuntimeOnly("org.graalvm.truffle:truffle-compiler:25.0.0")
  testRuntimeOnly("org.graalvm.compiler:compiler:25.0.0")
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
  // Phase 3C P0-2: 支持 Profiler 数据收集
  // 通过 -Daster.profiler.enabled=true 启用 profiling
  systemProperty("aster.profiler.enabled", System.getProperty("aster.profiler.enabled", "false"))
}

// GraalVM Native Image 配置
graalvmNative {
  metadataRepository {
    enabled.set(true) // 重新启用以使用官方 Jackson/NIO 元数据
  }
  binaries {
    named("main") {
      imageName.set("aster")
      mainClass.set("aster.truffle.Runner")
      buildArgs.add("--no-fallback")
      buildArgs.add("-H:+ReportExceptionStackTraces")
      buildArgs.add("--initialize-at-build-time=")
      buildArgs.add("-H:+UnlockExperimentalVMOptions")
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
