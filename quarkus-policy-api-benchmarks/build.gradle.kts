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

    // JMH benchmark 依赖
    jmhImplementation("org.openjdk.jmh:jmh-core:1.37")
    jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")

    // Testcontainers for PostgreSQL (与主项目版本一致)
    jmhImplementation("org.testcontainers:testcontainers:1.19.3")
    jmhImplementation("org.testcontainers:postgresql:1.19.3")

    // HikariCP DataSource
    jmhImplementation("com.zaxxer:HikariCP:5.1.0")
    jmhImplementation("org.postgresql:postgresql")

    // Flyway 数据库迁移 (Task 4)
    jmhImplementation("org.flywaydb:flyway-core:10.4.1")
    jmhImplementation("org.flywaydb:flyway-database-postgresql:10.4.1")

    // Mockito 和 Hibernate (Task 5)
    jmhImplementation("org.mockito:mockito-core:5.8.0")
    jmhImplementation("org.hibernate:hibernate-core:6.6.4.Final")
    jmhImplementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    jmhImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
}

// 将主项目的数据库迁移脚本添加到 JMH classpath (Task 4)
sourceSets {
    named("jmh") {
        resources {
            srcDir("${project(":quarkus-policy-api").projectDir}/src/main/resources")
        }
    }
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

// 启用 zip64 支持大型 JMH JAR（超过 65535 个条目）
tasks.named<Jar>("jmhJar") {
    isZip64 = true
}

jmh {
    warmupIterations.set(2)
    iterations.set(5)
    fork.set(1)
    threads.set(1)
    timeUnit.set("ms")
    includes.set(listOf(".*WorkflowSchedulingBenchmark.*"))
    resultFormat.set("JSON")
    resultsFile.set(layout.buildDirectory.file("reports/jmh/workflow-scheduling.json"))
}
