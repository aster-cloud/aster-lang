import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Zip

plugins { java }

group = "io.aster.idea"
version = "0.1.0"

repositories {
  mavenCentral()
  maven("https://cache-redirector.jetbrains.com/intellij-repository/releases")
}

val ideaVersion = "2025.1"

val intellijDist by configurations.creating {
  isCanBeConsumed = false
  isCanBeResolved = true
  isTransitive = false
}

val intellijRoot = layout.buildDirectory.dir("intellij/$ideaVersion")
val intellijLibDir = intellijRoot.map { it.dir("lib") }
val intellijLibs = objects.fileCollection().from(
  intellijLibDir.map { dir -> dir.asFileTree.matching { include("*.jar") } }
)

// 用于测试的 IntelliJ 库（排除可能导致 JUnit 冲突的 JAR，但保留 testFramework）
val intellijTestLibs = objects.fileCollection().from(
  intellijLibDir.map { dir ->
    dir.asFileTree.matching {
      include("*.jar")
      // 排除 JUnit 4 相关 JAR（使用 JUnit 5 Vintage 引擎代替）
      exclude("junit-*.jar")
      exclude("junit4.jar")
      exclude("*ant-junit*")
      // 保留 testFramework.jar - IntelliJ 2025.1 支持 JUnit 5
      // exclude("testFramework.jar")
      exclude("intellij-test-discovery.jar")
    }
  }
)

dependencies {
  intellijDist("com.jetbrains.intellij.idea:ideaIC:$ideaVersion")
  compileOnly("org.jetbrains:annotations:24.1.0")

  // IntelliJ Platform SDK (for IDE resolution - actual JARs added via classpath)
  compileOnly(intellijLibs)

  // Aster core compiler dependency
  implementation(project(":aster-core"))

  // Test dependencies
  testImplementation(platform("org.junit:junit-bom:5.11.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.mockito:mockito-core:5.14.2")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  // JUnit 4 依赖 - IntelliJ ParsingTestCase 等测试基类需要
  testImplementation("junit:junit:4.13.2")
  // JUnit Vintage 引擎 - 允许在 JUnit 5 平台运行 JUnit 4 测试
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(25))
  }
  withSourcesJar()
}

tasks.jar {
  archiveBaseName.set("aster-idea")

  // Include aster-core classes in the plugin jar
  from(project(":aster-core").sourceSets.main.get().output)
}

val assemblePlugin by tasks.registering(Copy::class) {
  dependsOn(tasks.jar)
  into(layout.buildDirectory.dir("idea-plugin/aster-idea"))
  from(tasks.jar) { into("lib") }

  // Include ANTLR runtime
  from(configurations.runtimeClasspath.get().filter { it.name.contains("antlr4-runtime") }) {
    into("lib")
  }
}

tasks.register<Zip>("buildPlugin") {
  dependsOn(assemblePlugin)
  archiveFileName.set("aster-idea-${project.version}.zip")
  destinationDirectory.set(layout.buildDirectory.dir("distributions"))
  from(assemblePlugin) { into("aster-idea") }
}

tasks.build {
  dependsOn("buildPlugin")
}

val extractIntellij by tasks.registering(Sync::class) {
  from({ intellijDist.resolve().map { zipTree(it) } })
  into(intellijRoot)
}

// 仅为主源码编译添加完整的 IntelliJ 库
tasks.compileJava {
  dependsOn(extractIntellij)
  classpath = classpath.plus(intellijLibs)
}

// 为测试编译使用排除 JUnit 的 IntelliJ 库
tasks.compileTestJava {
  dependsOn(extractIntellij)
  classpath = classpath.plus(intellijTestLibs)
}

// 测试资源目录配置 - 同时包含默认资源目录和 testData
sourceSets {
  test {
    resources {
      srcDirs("src/test/resources", "src/test/testData")
    }
  }
}

tasks.processTestResources {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
  useJUnitPlatform()

  // 添加 IntelliJ 库到测试 classpath
  classpath = classpath.plus(intellijTestLibs)

  // 设置工作目录为项目根目录（testData 相对路径需要）
  workingDir = projectDir

  // JVM 参数 - IntelliJ 测试框架完整配置
  jvmArgs(
    // 模块开放 - Java 基础模块
    "--add-opens", "java.base/java.lang=ALL-UNNAMED",
    "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED",
    "--add-opens", "java.base/java.util=ALL-UNNAMED",
    "--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED",
    "--add-opens", "java.base/java.util.concurrent.atomic=ALL-UNNAMED",
    "--add-opens", "java.base/java.io=ALL-UNNAMED",
    "--add-opens", "java.base/java.nio=ALL-UNNAMED",
    "--add-opens", "java.base/java.nio.charset=ALL-UNNAMED",
    "--add-opens", "java.base/java.text=ALL-UNNAMED",
    "--add-opens", "java.base/java.time=ALL-UNNAMED",
    "--add-opens", "java.base/java.security=ALL-UNNAMED",
    "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
    "--add-opens", "java.base/sun.nio.fs=ALL-UNNAMED",
    "--add-opens", "java.base/sun.security.ssl=ALL-UNNAMED",
    "--add-opens", "java.base/sun.security.util=ALL-UNNAMED",
    // 模块开放 - Desktop 模块
    "--add-opens", "java.desktop/java.awt=ALL-UNNAMED",
    "--add-opens", "java.desktop/java.awt.event=ALL-UNNAMED",
    "--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED",
    "--add-opens", "java.desktop/java.awt.font=ALL-UNNAMED",
    "--add-opens", "java.desktop/sun.awt=ALL-UNNAMED",
    "--add-opens", "java.desktop/sun.awt.image=ALL-UNNAMED",
    "--add-opens", "java.desktop/sun.font=ALL-UNNAMED",
    "--add-opens", "java.desktop/sun.java2d=ALL-UNNAMED",
    "--add-opens", "java.desktop/javax.swing=ALL-UNNAMED",
    "--add-opens", "java.desktop/javax.swing.text=ALL-UNNAMED",
    "--add-opens", "java.desktop/javax.swing.plaf.basic=ALL-UNNAMED",
    // Headless 模式
    "-Djava.awt.headless=true",
    // IntelliJ 测试框架核心属性
    "-Didea.home.path=${intellijRoot.get().asFile.absolutePath}",
    "-Didea.config.path=${layout.buildDirectory.get().asFile.absolutePath}/test-config",
    "-Didea.system.path=${layout.buildDirectory.get().asFile.absolutePath}/test-system",
    "-Didea.plugins.path=${layout.buildDirectory.get().asFile.absolutePath}/test-plugins",
    "-Didea.log.path=${layout.buildDirectory.get().asFile.absolutePath}/test-log",
    "-Didea.test.cyclic.buffer.size=1048576",
    "-Didea.force.default.config=true",
    // 测试模式标志
    "-Didea.is.unit.test=true"
    // 注意：不设置 java.system.class.loader，让 JVM 使用默认类加载器
  )

  // 系统属性
  systemProperty("idea.force.use.core.classloader", "true")
  systemProperty("NO_FS_ROOTS_ACCESS_CHECK", "true")

  // 环境变量
  environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
}
