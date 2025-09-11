plugins {
  java
  application
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

repositories { mavenCentral() }

dependencies {
  implementation("org.ow2.asm:asm:9.6")
  implementation("org.ow2.asm:asm-commons:9.6")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
}

application {
  mainClass.set("aster.emitter.Main")
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
