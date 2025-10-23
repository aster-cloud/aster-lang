plugins {
    `java-library`
    antlr
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    // ANTLR4 工具链
    antlr("org.antlr:antlr4:4.13.1")
    implementation("org.antlr:antlr4-runtime:4.13.1")

    // JSON 序列化支持（用于黄金测试与跨语言对比）
    // 使用 api 而非 implementation，使依赖此模块的项目可以访问 Jackson 注解
    api("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    api("com.fasterxml.jackson.core:jackson-annotations:2.18.2")

    // 测试框架
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// ANTLR4 生成配置
tasks.generateGrammarSource {
    arguments = arguments + listOf(
        "-visitor",
        "-long-messages",
        "-package", "aster.core.parser"
    )
    outputDirectory = file("build/generated-src/antlr/main/aster/core/parser")
}

// 为 ANTLR4 生成的代码禁用严格检查
tasks.withType<JavaCompile>().configureEach {
    if (name == "compileJava") {
        options.compilerArgs.removeAll { it == "-Werror" }
        options.compilerArgs.addAll(listOf(
            "-Xlint:-this-escape",  // 忽略 ANTLR4 生成代码的 this-escape 警告
            "-Xlint:-cast",
            "-Xlint:-deprecation"
        ))
    }
}

// 黄金测试任务（用于 TypeScript/Java 输出对比）
tasks.register<Test>("goldenTest") {
    description = "运行黄金测试，对比 TypeScript 和 Java 编译器输出"
    group = "verification"

    useJUnitPlatform {
        includeTags("golden")
    }

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}
