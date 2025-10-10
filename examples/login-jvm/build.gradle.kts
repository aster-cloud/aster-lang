plugins { application }

repositories { mavenCentral() }

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }
tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
  options.isDeprecation = true
}

dependencies {
  implementation(project(":aster-runtime"))
  implementation(fileTree("${rootProject.projectDir}/build/aster-out") { include("aster.jar") })
}

application { mainClass.set("example.LoginMain") }

val generateAsterJar by tasks.registering(Exec::class) {
  workingDir = rootProject.projectDir
  // Emit required CNL modules for this example before creating the JAR.
  // In addition to login.cnl (app.service.*), PolicyTest.java depends on demo.policy and demo.policy_demo.
  commandLine = if (System.getProperty("os.name").lowercase().contains("win"))
    listOf(
      "cmd", "/c",
      "npm", "run", "emit:class",
      "cnl/examples/login.cnl",
      "cnl/examples/policy_engine.cnl",
      "cnl/examples/policy_demo.cnl",
      "&&", "npm", "run", "jar:jvm"
    )
  else listOf(
    "sh", "-c",
    "npm run emit:class cnl/examples/login.cnl cnl/examples/policy_engine.cnl cnl/examples/policy_demo.cnl && npm run jar:jvm"
  )
}
tasks.withType<JavaCompile>().configureEach {
  dependsOn(generateAsterJar)
}
