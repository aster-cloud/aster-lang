plugins {
    id("java")
    // Using latest Quarkus 3.28.3 - testing Gradle 9.0 compatibility
    id("io.quarkus") version "3.28.3"
}

repositories {
    mavenCentral()
    mavenLocal()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    // Quarkus BOM (Bill of Materials) for dependency management
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.28.3"))

    // Quarkus核心依赖 - Reactive REST endpoints (quarkus-rest already includes reactive support)
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")

    // OpenAPI & Health checks
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-smallrye-health")

    // Metrics
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")

    // Caching - Caffeine cache
    implementation("io.quarkus:quarkus-cache")

    // GraphQL支持
    implementation("io.quarkus:quarkus-smallrye-graphql")

    // Aster运行时和编译后的策略
    implementation(project(":aster-runtime"))
    implementation(project(":aster-validation"))
    implementation(files("${rootProject.projectDir}/build/aster-out/aster.jar"))

    // 测试依赖
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.quarkus:quarkus-junit5-mockito")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.smallrye.reactive:smallrye-mutiny-vertx-junit5")
    testImplementation("org.assertj:assertj-core:3.26.0")
    testImplementation("org.mockito:mockito-core:5.7.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all", "-Werror"))
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}

// 确保在编译前生成Aster JAR（包含loan policy + creditcard + healthcare + insurance + lending）
val generateAsterJar by tasks.registering(Exec::class) {
    workingDir = rootProject.projectDir
    commandLine = if (System.getProperty("os.name").lowercase().contains("win")) {
        listOf("cmd", "/c",
            "npm", "run", "emit:class",
            "cnl/stdlib/finance/loan.aster",
            "cnl/stdlib/finance/creditcard.aster",
            "cnl/stdlib/finance/lending/enterprise.aster",
            "cnl/stdlib/finance/lending/personal.aster",
            "cnl/stdlib/healthcare/eligibility.aster",
            "cnl/stdlib/healthcare/claims.aster",
            "cnl/stdlib/insurance/auto.aster",
            "cnl/stdlib/insurance/life.aster",
            "&&",
            "npm", "run", "jar:jvm")
    } else {
        listOf("sh", "-c",
            "npm run emit:class " +
            "cnl/stdlib/finance/loan.aster " +
            "cnl/stdlib/finance/creditcard.aster " +
            "cnl/stdlib/finance/lending/enterprise.aster " +
            "cnl/stdlib/finance/lending/personal.aster " +
            "cnl/stdlib/healthcare/eligibility.aster " +
            "cnl/stdlib/healthcare/claims.aster " +
            "cnl/stdlib/insurance/auto.aster " +
            "cnl/stdlib/insurance/life.aster " +
            "&& npm run jar:jvm")
    }
}

tasks.named("compileJava") {
    dependsOn(generateAsterJar)
}

val syncPolicyJar by tasks.registering(Copy::class) {
    dependsOn(generateAsterJar)
    from(rootProject.layout.projectDirectory.file("build/aster-out/aster.jar"))
    into(layout.projectDirectory.dir("src/main/resources"))
    rename { "policy-rules-merged.jar" }
}

val cleanPolicyClasses by tasks.registering(Delete::class) {
    delete(
        layout.projectDirectory.dir("src/main/resources/classes"),
        layout.projectDirectory.dir("src/main/resources/aster")
    )
}

val syncPolicyClasses by tasks.registering(Copy::class) {
    dependsOn(generateAsterJar, cleanPolicyClasses)
    from(rootProject.layout.projectDirectory.dir("build/jvm-classes"))
    into(layout.projectDirectory.dir("src/main/resources"))
}

tasks.named("processResources") {
    dependsOn(syncPolicyJar, syncPolicyClasses)
}
