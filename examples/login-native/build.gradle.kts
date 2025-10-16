plugins {
  application
  id("org.graalvm.buildtools.native") version "0.11.1"
}

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

application {
  mainClass.set("example.LoginNativeMain")
}

graalvmNative {
  binaries {
    named("main") {
      imageName.set("login-aster")
      buildArgs.addAll(listOf(
        "--no-fallback",
        "--strict-image-heap",
        "-H:+ReportExceptionStackTraces",
        "--initialize-at-build-time=aster.runtime"
      ))
      resources.autodetect()
    }
  }
}

val generateAsterJar by tasks.registering(Exec::class) {
  workingDir = rootProject.projectDir
  commandLine = if (System.getProperty("os.name").lowercase().contains("win"))
    listOf("cmd", "/c", "npm", "run", "emit:class", "cnl/examples/login.aster", "&&", "npm", "run", "jar:jvm")
  else listOf("sh", "-c", "npm run emit:class cnl/examples/login.aster && npm run jar:jvm")
}
tasks.withType<JavaCompile>().configureEach {
  dependsOn(generateAsterJar)
}
