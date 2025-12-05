plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

// 显式设置 Java 和 Kotlin 编译目标为 24 (Kotlin 当前最高支持版本)
// 避免 "Inconsistent JVM-target compatibility" 警告
// 注意: Kotlin 尚不支持 Java 25,所以必须统一到 24
java {
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_24)
    }
}
