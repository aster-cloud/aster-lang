plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

// 显式设置 Kotlin 编译目标为 24 (Kotlin 当前最高支持版本)
// 避免 "Kotlin does not yet support 25 JDK target" 警告
// 注意: 使用 compilerOptions 而非 jvmToolchain,因为系统只有 Java 25
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_24)
    }
}
