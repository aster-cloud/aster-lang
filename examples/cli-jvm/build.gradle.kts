plugins { application }

repositories { mavenCentral() }

java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
  options.isDeprecation = true
}

val moduleOut = layout.buildDirectory.dir("aster-out")

dependencies {
  implementation(project(":aster-runtime"))
  implementation(files(moduleOut.map { it.file("aster.jar") }))
}

application { mainClass.set("example.CLIMain") }

val generateAsterJar by tasks.registering(Exec::class) {
  workingDir = rootProject.projectDir
  commandLine = if (System.getProperty("os.name").lowercase().contains("win"))
    listOf("cmd", "/c", "set", "ASTER_OUT_DIR=examples/cli-jvm/build/aster-out", "&&", "npm", "run", "emit:class", "cnl/examples/cli_tool.aster", "&&", "set", "ASTER_OUT_DIR=examples/cli-jvm/build/aster-out", "&&", "npm", "run", "jar:jvm")
  else listOf("sh", "-c", "ASTER_OUT_DIR=examples/cli-jvm/build/aster-out npm run emit:class cnl/examples/cli_tool.aster && ASTER_OUT_DIR=examples/cli-jvm/build/aster-out npm run jar:jvm")
}
tasks.withType<JavaCompile>().configureEach {
  dependsOn(generateAsterJar)
}
