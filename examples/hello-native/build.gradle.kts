plugins {
  application
  id("org.graalvm.buildtools.native") version "0.11.1"
}

repositories { mavenCentral() }

java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
  options.isDeprecation = true
}

val moduleOut = layout.buildDirectory.dir("aster-out")

dependencies {
  implementation(project(":aster-runtime"))
  // 依赖模块专属的合并产物，避免多个示例共享同一 JAR 产生竞态覆盖
  implementation(files(moduleOut.map { it.file("aster.jar") }))
}

application {
  mainClass.set("example.Main")
}

graalvmNative {
  metadataRepository {
    enabled.set(false) // Gradle 9 + build tools 0.11.1 兼容性问题，禁用元数据服务
  }
  binaries {
    named("main") {
      imageName.set("hello-aster")
      buildArgs.addAll(listOf(
        "--no-fallback",
        "--strict-image-heap",
        "-H:+ReportExceptionStackTraces",
        "--initialize-at-build-time=aster.runtime"
      ))
      resources.autodetect()
    }
  }
}

// Ensure generated Aster jar exists before compiling
val generateAsterJar by tasks.registering(Exec::class) {
  workingDir = rootProject.projectDir
  commandLine = if (System.getProperty("os.name").lowercase().contains("win"))
    listOf("cmd", "/c", "npm", "run", "emit:class", "cnl/examples/greet.aster", "&&", "npm", "run", "jar:jvm")
  else listOf("sh", "-c", "ASTER_OUT_DIR=examples/hello-native/build/aster-out npm run emit:class cnl/examples/greet.aster && ASTER_OUT_DIR=examples/hello-native/build/aster-out npm run jar:jvm")
  // 输出声明：生成合并 Jar
  outputs.file(moduleOut.map { it.file("aster.jar") })
  // 只有在产物缺失时才需要强制执行；缓存存在时 Gradle 可跳过
  onlyIf { !moduleOut.get().file("aster.jar").asFile.exists() }
}
generateAsterJar.configure {
  // 配置缓存不兼容：该任务依赖外部 Node/Gradle 进程与工作目录副作用
  notCompatibleWithConfigurationCache("Exec uses external processes and dynamic file IO")
}
tasks.withType<JavaCompile>().configureEach {
  dependsOn(generateAsterJar)
}
// 确保类路径解析前已生成 Jar（兼容部分环境的任务排序与缓存）
configurations.compileClasspath.get().dependencies
configurations.compileClasspath.get().attributes
tasks.named("compileJava").configure {
  dependsOn(generateAsterJar)
}
tasks.named("jar").configure {
  dependsOn(generateAsterJar)
}
