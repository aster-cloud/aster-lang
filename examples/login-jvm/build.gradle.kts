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

val generateAsterJar by tasks.registering(Exec::class) {
  workingDir = rootProject.projectDir
  // Emit required CNL modules for this example before creating the JAR.
  // In addition to login.aster (app.service.*), PolicyTest.java depends on demo.policy and demo.policy_demo.
  commandLine = if (System.getProperty("os.name").lowercase().contains("win"))
    listOf(
      "cmd", "/c",
      "npm", "run", "emit:class",
      "cnl/examples/login.aster",
      "cnl/examples/policy_engine.aster",
      "cnl/examples/policy_demo.aster",
      "&&", "npm", "run", "jar:jvm"
    )
  else listOf(
    "sh", "-c",
    "ASTER_OUT_DIR=examples/login-jvm/build/aster-out npm run emit:class cnl/examples/login.aster cnl/examples/policy_engine.aster cnl/examples/policy_demo.aster && ASTER_OUT_DIR=examples/login-jvm/build/aster-out npm run jar:jvm"
  )
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
