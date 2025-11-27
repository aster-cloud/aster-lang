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

// 用于测试的 IntelliJ 库（排除所有可能导致 JUnit 冲突的 JAR）
val intellijTestLibs = objects.fileCollection().from(
  intellijLibDir.map { dir ->
    dir.asFileTree.matching {
      include("*.jar")
      // 排除所有 JUnit 相关 JAR
      exclude("*junit*")
      exclude("*ant-junit*")
      exclude("*kotlin-test*")
      // 排除 IntelliJ 测试框架（它会尝试初始化需要 JUnit 4 的测试环境）
      exclude("testFramework.jar")
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
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

tasks.test {
  useJUnitPlatform()

  // 添加 IntelliJ 库（排除 JUnit 相关以避免冲突）
  classpath = classpath.plus(intellijTestLibs)

  // JVM 参数
  jvmArgs(
    "--add-opens", "java.base/java.lang=ALL-UNNAMED",
    "--add-opens", "java.base/java.util=ALL-UNNAMED"
  )
}
