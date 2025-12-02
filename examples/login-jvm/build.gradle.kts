import dev.aster.build.GenerateAsterJarTask

plugins { application }

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

application { mainClass.set("example.LoginMain") }

// Emit required CNL modules for this example before creating the JAR.
// In addition to login.aster (app.service.*), PolicyTest.java depends on demo.policy and demo.policy_demo.
val generateAsterJar by tasks.registering(GenerateAsterJarTask::class) {
  description = "生成 login-jvm 模块的 aster.jar (包含 login, policy_engine, policy_demo)"
  workingDirectory.set(rootProject.layout.projectDirectory)
  outputDirectory.set(moduleOut)
  outputJar.set(moduleOut.map { it.file("aster.jar") })
  asterSources.from(
    rootProject.file("test/cnl/programs/examples/login.aster"),
    rootProject.file("test/cnl/programs/business/policy/policy_engine.aster"),
    rootProject.file("test/cnl/programs/business/policy/policy_demo.aster")
  )
}

tasks.withType<JavaCompile>().configureEach {
  dependsOn(generateAsterJar)
}
