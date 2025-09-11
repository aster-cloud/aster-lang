plugins { application }

repositories { mavenCentral() }

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

dependencies {
  implementation(files("${rootProject.projectDir}/build/aster-out/aster.jar"))
}

application { mainClass.set("example.MathMain") }
