import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Zip

plugins { java }

group = "io.aster.idea"
version = "0.1.0"

repositories {
  mavenCentral()
  maven("https://cache-redirector.jetbrains.com/intellij-repository/releases")
}

val ideaVersion = "2024.1"

val intellijDist by configurations.creating {
  isCanBeConsumed = false
  isCanBeResolved = true
  isTransitive = false
}

val intellijRoot = layout.buildDirectory.dir("intellij/$ideaVersion")
val intellijLibDir = intellijRoot.map { it.dir("lib") }
val intellijLibs = objects.fileCollection().from(
  intellijLibDir.map { dir -> dir.asFileTree.matching { include("*.jar") } }
)

dependencies {
  intellijDist("com.jetbrains.intellij.idea:ideaIC:$ideaVersion")
  compileOnly("org.jetbrains:annotations:24.1.0")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
  withSourcesJar()
}

tasks.jar {
  archiveBaseName.set("aster-idea")
}

val assemblePlugin by tasks.registering(Copy::class) {
  dependsOn(tasks.jar)
  into(layout.buildDirectory.dir("idea-plugin/aster-idea"))
  from(tasks.jar) { into("lib") }
}

tasks.register<Zip>("buildPlugin") {
  dependsOn(assemblePlugin)
  archiveFileName.set("aster-idea-${project.version}.zip")
  destinationDirectory.set(layout.buildDirectory.dir("distributions"))
  from(assemblePlugin) { into("aster-idea") }
}

tasks.build {
  dependsOn("buildPlugin")
}

val extractIntellij by tasks.registering(Sync::class) {
  from({ intellijDist.resolve().map { zipTree(it) } })
  into(intellijRoot)
}

tasks.withType<JavaCompile>().configureEach {
  dependsOn(extractIntellij)
  classpath = classpath.plus(intellijLibs)
}

tasks.test {
  useJUnitPlatform()
}
