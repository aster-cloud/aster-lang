// Root build file to orchestrate example compilation with generated Aster jar.

// 应用确定性构建配置
apply(from = "gradle/reproducible-builds.gradle.kts")

val generateAsterJarRoot by tasks.registering(Exec::class) {
  workingDir = projectDir
  commandLine = if (System.getProperty("os.name").lowercase().contains("win"))
    listOf("cmd", "/c", "npm", "run", "jar:jvm")
  else listOf("sh", "-c", "npm run jar:jvm")
}

tasks.register("examplesCompileAll") {
  group = "build"
  description = "Builds generated Aster jar then compiles all example projects"

  // Ensure the generated jar exists
  dependsOn(generateAsterJarRoot)

  // Compile all example subprojects
  dependsOn(
    ":examples:list-jvm:compileJava",
    ":examples:login-jvm:compileJava",
    ":examples:map-jvm:compileJava",
    ":examples:math-jvm:compileJava",
    ":examples:text-jvm:compileJava",
    ":examples:hello-native:compileJava",
    ":examples:rest-jvm:compileJava",
    ":examples:login-native:compileJava"
  )
}
