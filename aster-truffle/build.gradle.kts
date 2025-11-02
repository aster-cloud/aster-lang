plugins { application; java }
repositories { mavenCentral() }

dependencies {
  implementation("org.graalvm.truffle:truffle-api:25.0.0")
  annotationProcessor("org.graalvm.truffle:truffle-dsl-processor:25.0.0")
  implementation("org.graalvm.sdk:graal-sdk:25.0.0")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
  testImplementation("org.graalvm.truffle:truffle-api:25.0.0")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.1")
}

application { mainClass.set("aster.truffle.Runner") }

java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }

tasks.withType<JavaCompile>().configureEach {
  // Override global -Werror setting from reproducible-builds.gradle.kts
  // because Truffle DSL and Jackson annotation processors produce benign warnings
  options.compilerArgs.remove("-Werror")
  options.isDeprecation = true
}

tasks.test {
  useJUnitPlatform()
  testLogging {
    events("passed", "skipped", "failed")
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    showStandardStreams = true
  }
}
