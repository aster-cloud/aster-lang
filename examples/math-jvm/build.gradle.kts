plugins { application }

repositories { mavenCentral() }

java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
  options.isDeprecation = true
}

dependencies {
  implementation(project(":aster-runtime"))
  implementation(fileTree("${rootProject.projectDir}/build/aster-out") { include("aster.jar") })
}

application { mainClass.set("example.MathMain") }

val generateAsterJar by tasks.registering(Exec::class) {
  workingDir = rootProject.projectDir
  commandLine = if (System.getProperty("os.name").lowercase().contains("win"))
    listOf("cmd", "/c", "npm", "run", "emit:class", "cnl/examples/arith_compare.cnl", "&&", "npm", "run", "jar:jvm")
  else listOf("sh", "-c", "npm run emit:class cnl/examples/arith_compare.cnl && npm run jar:jvm")
}
tasks.withType<JavaCompile>().configureEach {
  dependsOn(generateAsterJar)
}
