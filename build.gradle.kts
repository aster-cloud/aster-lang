// Root build file to orchestrate example compilation with generated Aster jar.

// 应用确定性构建配置
apply(from = "gradle/reproducible-builds.gradle.kts")

// 全局测试依赖配置（供所有子模块使用）
subprojects {
    repositories {
        mavenCentral()
    }

    plugins.withType<JavaPlugin> {
        dependencies {
            // jqwik 属性测试库（类似 TypeScript fast-check）
            "testImplementation"("net.jqwik:jqwik:1.8.2")
            // JSON 对比库（用于黄金测试）
            "testImplementation"("org.skyscreamer:jsonassert:1.5.1")
        }
    }
}

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
