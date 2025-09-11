plugins {
  application
  id("org.graalvm.buildtools.native") version "0.10.2"
}

repositories { mavenCentral() }

dependencies {
  implementation(files("${rootProject.projectDir}/build/aster-out/aster.jar"))
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

