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
  // 模块独享的 aster.jar，避免示例之间的 jar 争用
  implementation(files(moduleOut.map { it.file("aster.jar") }))
}

application {
  mainClass.set("example.LoginNativeMain")
}

graalvmNative {
  binaries {
    named("main") {
      imageName.set("login-aster")
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

val loginSource = rootProject.layout.projectDirectory.file("test/cnl/programs/examples/login.aster")
val generateAsterJar by tasks.registering(GenerateAsterJarTask::class) {
  description = "为 login-native 示例生成独立的 aster.jar"
  workingDirectory.set(rootProject.layout.projectDirectory)
  outputDirectory.set(moduleOut)
  outputJar.set(moduleOut.map { it.file("aster.jar") })
  asterSources.from(loginSource)
}
tasks.withType<JavaCompile>().configureEach {
  dependsOn(generateAsterJar)
  dependsOn(":quarkus-policy-api:generateAsterJar")
}
configurations.compileClasspath.get().let {
  // 触发配置解析以确保 Gradle 在解析类路径前运行 generateAsterJar
  it.dependencies
  it.attributes
}
tasks.named("compileJava").configure {
  dependsOn(generateAsterJar)
}
tasks.named("jar").configure {
  dependsOn(generateAsterJar)
}
