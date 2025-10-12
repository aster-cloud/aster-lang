plugins { application; java }
repositories { mavenCentral() }

dependencies {
  implementation("org.graalvm.truffle:truffle-api:24.1.1")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
}

application { mainClass.set("aster.truffle.Runner") }

java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }

tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
  options.isDeprecation = true
}
