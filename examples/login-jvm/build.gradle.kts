plugins { application }

repositories { mavenCentral() }

dependencies {
  implementation(files("${rootProject.projectDir}/build/aster-out/aster.jar"))
}

application { mainClass.set("example.LoginMain") }

