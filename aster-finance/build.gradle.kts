plugins {
    id("java")
    id("jacoco")
    id("info.solidsoft.pitest") version "1.19.0-rc.2"
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
    // Aster runtime dependency
    implementation(project(":aster-runtime"))

    // Jackson for JSON serialization
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")

    // Testing dependencies
    testImplementation("net.jqwik:jqwik:1.8.2")
    testImplementation("org.skyscreamer:jsonassert:1.5.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.assertj:assertj-core:3.26.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf(
        "-parameters",
        "--enable-preview"  // Enable preview features for Java Records enhancements
    ))
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("jqwik", "junit-jupiter")
    }
    jvmArgs = listOf("--enable-preview")
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
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

// PIT Mutation Testing 配置
pitest {
    // JUnit 5 支持
    junit5PluginVersion.set("1.2.1")

    // 性能优化：使用多线程
    threads.set(Runtime.getRuntime().availableProcessors())

    // 目标类：仅测试核心业务代码
    targetClasses.set(listOf("com.wontlost.aster.finance.*"))

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
