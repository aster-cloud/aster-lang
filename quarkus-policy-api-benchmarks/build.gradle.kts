plugins {
    id("java")
    id("me.champeau.jmh") version "0.7.2"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.28.3"))
    implementation("io.quarkus:quarkus-cache")
    implementation("org.openjdk.jmh:jmh-core:1.37")
    implementation(project(":quarkus-policy-api"))
    implementation(project(":aster-finance"))
    implementation(project(":aster-policy-common"))
    implementation(project(":aster-validation"))
    implementation(project(":aster-runtime"))

    implementation("io.smallrye.reactive:mutiny:2.6.1")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    implementation(files("${rootProject.projectDir}/build/aster-out/aster.jar"))

    jmhImplementation("org.openjdk.jmh:jmh-core:1.37")
    jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.remove("-Werror")
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all"))
}

listOf("compileJava", "jmhCompileGeneratedClasses", "jmhJar", "jmh").forEach { taskName ->
    tasks.matching { it.name == taskName }.configureEach {
        dependsOn(":quarkus-policy-api:generateAsterJar")
    }
}

jmh {
    warmupIterations.set(2)
    iterations.set(5)
    fork.set(1)
    threads.set(1)
    timeUnit.set("ms")
    includes.set(listOf(".*PolicyEvaluationBenchmark.*"))
    resultFormat.set("JSON")
    resultsFile.set(layout.buildDirectory.file("reports/jmh/policy-evaluation.json"))
}
