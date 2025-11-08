// Root build file to orchestrate example compilation with generated Aster jar.

// 应用确定性构建配置
apply(from = "gradle/reproducible-builds.gradle.kts")

// 应用 JaCoCo 插件以支持聚合覆盖率报告
plugins {
    jacoco
    id("org.graalvm.buildtools.native") version "0.11.2" apply false
}

// 根项目需要配置仓库以下载 JaCoCo 依赖
repositories {
    mavenCentral()
}

// JaCoCo 版本配置（与子模块保持一致）
jacoco {
    toolVersion = "0.8.14"
}

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

  // Compile all example subprojects (only if they exist in settings)
  val exampleProjects = listOf(
    ":examples:list-jvm",
    ":examples:login-jvm",
    ":examples:map-jvm",
    ":examples:math-jvm",
    ":examples:text-jvm",
    ":examples:hello-native",
    ":examples:rest-jvm",
    ":examples:login-native"
  )

  exampleProjects.forEach { projectPath ->
    try {
      project(projectPath)
      dependsOn("$projectPath:compileJava")
    } catch (e: Exception) {
      // Project not included in settings.gradle.kts (e.g., in Docker build)
      logger.info("Skipping example project $projectPath (not included in settings)")
    }
  }
}

// 聚合覆盖率报告任务
tasks.register<JacocoReport>("jacocoAggregateReport") {
  group = "verification"
  description = "生成所有核心模块的聚合覆盖率报告"

  // 自动发现核心模块：以 "aster-" 开头且不是示例/工具模块
  val coreProjects = subprojects.filter { proj ->
    proj.name.startsWith("aster-") &&
    !proj.name.startsWith("aster-lang-") &&  // 排除 CLI 工具
    proj.path !in listOf(":aster-validation")  // 排除验证工具
  }

  coreProjects.forEach { proj ->
    // 只处理同时应用了 JavaPlugin 和 JacocoPlugin 的项目
    proj.plugins.withType<JavaPlugin> {
      proj.plugins.withType<JacocoPlugin> {
        // 依赖该模块的测试任务
        dependsOn(proj.tasks.named("test"))

        // 使用 SourceSet 获取源码和类文件目录（支持 Kotlin 等扩展）
        val sourceSets = proj.the<SourceSetContainer>()
        sourceDirectories.from(sourceSets["main"].allSource.srcDirs)
        classDirectories.from(sourceSets["main"].output)

        // 收集执行数据文件（JaCoCo 会自动过滤不存在的文件）
        val testTask = proj.tasks.named<Test>("test").get()
        executionData.from(testTask.extensions.getByType<JacocoTaskExtension>().destinationFile)
      }
    }
  }

  reports {
    xml.required.set(true)
    html.required.set(true)
    csv.required.set(false)

    xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/aggregate/jacocoTestReport.xml"))
    html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/aggregate/html"))
  }
}

// JaCoCo 覆盖率阈值验证任务
tasks.register<JacocoCoverageVerification>("jacocoAggregateVerification") {
  group = "verification"
  description = "验证聚合覆盖率是否达到阈值 (≥80%)"

  dependsOn(tasks.named("jacocoAggregateReport"))

  // 使用聚合报告的执行数据
  val reportTask = tasks.named<JacocoReport>("jacocoAggregateReport").get()
  sourceDirectories.from(reportTask.sourceDirectories)
  classDirectories.from(reportTask.classDirectories)
  executionData.from(reportTask.executionData)

  violationRules {
    rule {
      limit {
        minimum = "0.80".toBigDecimal()
      }
    }

    rule {
      limit {
        counter = "BRANCH"
        minimum = "0.75".toBigDecimal()
      }
    }
  }
}

// PIT Mutation Testing 全局配置
// 注意：各子模块需要自行应用 info.solidsoft.pitest 插件（在各自 build.gradle.kts 中）
// 这里提供全局默认配置供子模块继承
// 配置方式：在子模块 build.gradle.kts 中添加 apply(plugin = "info.solidsoft.pitest")
// 然后可以覆盖或继承这里的默认配置
