plugins {
  application
  id("org.graalvm.buildtools.native") version "0.10.2"
}

repositories { mavenCentral() }

dependencies {
  implementation(files("${rootProject.projectDir}/build/aster-out/aster.jar"))
}

application {
  mainClass.set("example.Main")
}

graalvmNative {
  binaries {
    named("main") {
      imageName.set("hello-aster")
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

