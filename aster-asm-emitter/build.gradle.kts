plugins {
  java
  application
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(25))
  }
}

repositories { mavenCentral() }

dependencies {
  implementation("org.ow2.asm:asm:9.8")
  implementation("org.ow2.asm:asm-commons:9.8")
  implementation("org.ow2.asm:asm-util:9.8")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
  implementation(project(":aster-runtime"))

  testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
