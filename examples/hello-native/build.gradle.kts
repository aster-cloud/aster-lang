import dev.aster.build.GenerateAsterJarTask

plugins {
  application
  id("org.graalvm.buildtools.native")
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
        "--initialize-at-build-time=org.slf4j.impl.Slf4jLogger",
        "--initialize-at-build-time=org.jboss.logmanager.Level",
        "--initialize-at-build-time=org.jboss.logmanager",
        "--initialize-at-build-time=io.quarkus.bootstrap.logging.QuarkusDelayedHandler",
        "--initialize-at-build-time=io.quarkus.bootstrap.logging",
        "--initialize-at-build-time=io.smallrye.common.ref",
        "--initialize-at-build-time=io.quarkus.vertx.mdc.provider"
      ))
      resources.autodetect()
    }
  }
}

val greetSource = rootProject.layout.projectDirectory.file("test/cnl/programs/examples/greet.aster")
val generateAsterJar by tasks.registering(GenerateAsterJarTask::class) {
  description = "为 hello-native 示例生成独立的 aster.jar"
  workingDirectory.set(rootProject.layout.projectDirectory)
  outputDirectory.set(moduleOut)
  outputJar.set(moduleOut.map { it.file("aster.jar") })
  asterSources.from(greetSource)
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
