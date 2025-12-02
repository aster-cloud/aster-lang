plugins {
    id("java")
    id("jacoco")
    id("info.solidsoft.pitest") version "1.19.0-rc.2"
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    // Jackson for JSON serialization
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")

    // Caffeine cache for compilation result caching
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // Jakarta CDI API (provided scope - only for annotations)
    compileOnly("jakarta.enterprise:jakarta.enterprise.cdi-api:4.0.1")

    // Logging facade
    implementation("org.slf4j:slf4j-api:2.0.9")

    // Testing dependencies
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.0")
    testImplementation("org.skyscreamer:jsonassert:1.5.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.9")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-parameters"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jacoco {
    toolVersion = "0.8.14"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                // 当前覆盖率为 81%，设置阈值为 80%
                // TODO: 通过增加测试用例提升覆盖率至 85%
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

// PIT Mutation Testing 配置
pitest {
    // JUnit 5 支持
    junit5PluginVersion.set("1.2.1")

    // 性能优化：使用多线程
    threads.set(Runtime.getRuntime().availableProcessors())

    // 目标类：仅测试核心业务代码
    targetClasses.set(listOf("com.wontlost.aster.policy.*"))

    // 变异器配置：使用默认变异器组
    mutators.set(setOf("DEFAULTS"))

    // 输出格式：HTML + XML
    outputFormats.set(setOf("HTML", "XML"))

    // 时间限制：每个测试最多 10 秒
    timeoutConstInMillis.set(10000)

    // 覆盖率阈值（低于阈值会导致构建失败）
    mutationThreshold.set(75)  // ≥75% mutation score
    coverageThreshold.set(80)  // ≥80% line coverage

    // 历史数据支持（加速增量分析）
    timestampedReports.set(false)

    // 详细输出
    verbose.set(false)
}
