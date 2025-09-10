plugins { application; java }
repositories { mavenCentral() }

dependencies {
  implementation("org.graalvm.truffle:truffle-api:24.1.1")
}

application { mainClass.set("aster.truffle.Runner") }

java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

