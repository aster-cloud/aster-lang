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

application { mainClass.set("example.PolicyMain") }

val generateAsterJar by tasks.registering(GenerateAsterJarTask::class) {
  description = "生成 policy-jvm 模块的 aster.jar"
  workingDirectory.set(rootProject.layout.projectDirectory)
  outputDirectory.set(moduleOut)
  outputJar.set(moduleOut.map { it.file("aster.jar") })
  asterSources.from(
    rootProject.file("test/cnl/programs/business/policy/policy_engine.aster"),
    rootProject.file("test/cnl/programs/business/policy/policy_demo.aster")
  )
}

tasks.withType<JavaCompile>().configureEach {
  dependsOn(generateAsterJar)
}
