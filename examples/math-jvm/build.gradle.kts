import dev.aster.build.GenerateAsterJarTask

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

application { mainClass.set("example.MathMain") }

val generateAsterJar by tasks.registering(GenerateAsterJarTask::class) {
  description = "生成 math-jvm 模块的 aster.jar"
  workingDirectory.set(rootProject.layout.projectDirectory)
  outputDirectory.set(moduleOut)
  outputJar.set(moduleOut.map { it.file("aster.jar") })
  asterSources.from(rootProject.file("test/cnl/programs/operators/arith_compare.aster"))
}

tasks.withType<JavaCompile>().configureEach {
  dependsOn(generateAsterJar)
}
