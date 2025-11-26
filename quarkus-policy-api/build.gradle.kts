import dev.aster.build.GenerateAsterJarTask
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage

plugins {
    id("java")
    // Using latest Quarkus 3.28.3 - testing Gradle 9.0 compatibility
    id("io.quarkus") version "3.28.3"
    id("io.gatling.gradle") version "3.13.1"
}

extra["reportsDir"] = layout.buildDirectory.dir("reports/gatling").get().asFile

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

    // 链路追踪 - OpenTelemetry
    implementation("io.quarkus:quarkus-opentelemetry")
    implementation("io.opentelemetry:opentelemetry-exporter-logging")

    // Caching - Caffeine cache + Redis for distributed invalidation
    implementation("io.quarkus:quarkus-cache")
    implementation("io.quarkus:quarkus-redis-cache")

    // WebSocket support for Live Preview
    implementation("io.quarkus:quarkus-websockets")

    // GraphQL支持
    implementation("io.quarkus:quarkus-smallrye-graphql")
    implementation("commons-codec:commons-codec:1.17.1")

    // Persistence - Hibernate Panache + PostgreSQL + Flyway + Reactive Inbox
    implementation("io.quarkus:quarkus-hibernate-orm-panache")
    implementation("io.quarkus:quarkus-hibernate-reactive-panache")
    implementation("io.quarkus:quarkus-reactive-pg-client")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-flyway")

    // Scheduler - for workflow cleanup tasks
    implementation("io.quarkus:quarkus-scheduler")

    // Aster运行时和编译后的策略
    implementation(project(":aster-core"))
    implementation(project(":aster-runtime"))
    implementation(project(":aster-truffle"))
    implementation(project(":aster-validation"))
    implementation(project(":aster-policy-common"))
    implementation(project(":aster-finance"))
    runtimeOnly(files("${rootProject.projectDir}/build/aster-out/aster.jar"))

    // 测试依赖
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.quarkus:quarkus-junit5-mockito")
    testImplementation("io.quarkus:quarkus-test-vertx")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.smallrye.reactive:smallrye-mutiny-vertx-junit5")
    testImplementation("org.assertj:assertj-core:3.26.0")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("io.quarkus:quarkus-jdbc-h2")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation(project(":aster-ecommerce"))
    testImplementation("com.tngtech.archunit:archunit-junit5:1.2.1")

    // Testcontainers - PostgreSQL 测试环境（Phase 3.4）
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("com.redis:testcontainers-redis:2.0.1")
}

configurations.configureEach {
    exclude(group = "org.jboss.slf4j", module = "slf4j-jboss-logmanager")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.remove("-Werror") // Override global setting from reproducible-builds.gradle.kts
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all"))
    // Note: -Werror removed due to classfile warnings from MicroProfile Config API
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
    systemProperty("quarkus.test.flat-class-path", "true")
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}

// 确保在编译前生成Aster JAR（包含loan policy + creditcard + healthcare + insurance + lending）
// 使用 shell find 自动发现所有策略文件，在执行时动态查找
// 重要：必须一次性传递所有 .aster 文件给 emit:class，因为 emit:class 会清空 build/jvm-classes 目录
val skipGenerateAsterJar = providers.environmentVariable("SKIP_GENERATE_ASTER_JAR").isPresent

val workflowDeps = configurations.detachedConfiguration(
    dependencies.create("io.quarkus:quarkus-cache:3.28.3@jar"),
    dependencies.create("jakarta.enterprise:jakarta.enterprise.cdi-api:4.0.1"),
    dependencies.create("jakarta.inject:jakarta.inject-api:2.0.1")
).apply {
    isCanBeConsumed = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
    }
}

if (skipGenerateAsterJar) {
    logger.lifecycle("Skipping generateAsterJar because SKIP_GENERATE_ASTER_JAR is set")
}

val policySources = layout.projectDirectory
    .dir("src/main/resources/policies")
    .asFileTree
    .matching { include("**/*.aster") }
val sharedAsterOut = rootProject.layout.buildDirectory.dir("aster-out")

val generateAsterJar by tasks.registering(GenerateAsterJarTask::class) {
    description = "扫描 policies 目录并生成最新的 aster.jar"
    enabled = !skipGenerateAsterJar
    workingDirectory.set(rootProject.layout.projectDirectory)
    outputDirectory.set(sharedAsterOut)
    outputJar.set(sharedAsterOut.map { it.file("aster.jar") })
    asterSources.from(policySources)
    workflowClasspath.from(workflowDeps)
}

tasks.named("compileJava") {
    dependsOn(generateAsterJar)
}

// 修复 Gradle 9.0 对 Quarkus 任务的依赖检测
tasks.named("quarkusGenerateAppModel") {
    dependsOn(generateAsterJar)
}
tasks.named("quarkusGenerateTestAppModel") {
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
