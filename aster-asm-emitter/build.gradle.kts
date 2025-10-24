plugins {
  java
  application
  id("me.champeau.jmh") version "0.7.2"
  jacoco
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(25))
  }
}

repositories { mavenCentral() }

dependencies {
  // Core IR 数据模型（共享模块）
  implementation(project(":aster-core"))

  // ASM 字节码生成
  implementation("org.ow2.asm:asm:9.9")
  implementation("org.ow2.asm:asm-commons:9.9")
  implementation("org.ow2.asm:asm-util:9.9")
  testImplementation("org.ow2.asm:asm-tree:9.9")

  // JSON 解析（读取 Core IR 输入）
  implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

  // 运行时库
  implementation(project(":aster-runtime"))

  testImplementation("org.junit.jupiter:junit-jupiter:6.0.0")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")

  // JMH dependencies
  jmh("org.openjdk.jmh:jmh-core:1.37")
  jmh("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

application {
  mainClass.set("aster.emitter.Main")
}

tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
  options.isDeprecation = true
}

tasks.withType<JavaExec>().configureEach {
  if (name == "run") {
    standardInput = System.`in`
  }
}

tasks.withType<Jar> {
  manifest { attributes["Main-Class"] = "aster.emitter.Main" }
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
  useJUnitPlatform()
}

// JMH 配置
jmh {
  warmupIterations.set(2)
  iterations.set(5)
  fork.set(1)
  threads.set(1)
  timeUnit.set("ms")
  benchmarkMode.set(listOf("avgt"))
  includes.set(listOf(".*Benchmark.*"))
}

// JaCoCo 覆盖率配置
jacoco {
  toolVersion = "0.8.14"  // 官方支持 Java 25
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)

  reports {
    xml.required.set(true)
    html.required.set(true)
    csv.required.set(false)
  }
}
