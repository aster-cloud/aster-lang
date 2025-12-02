plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    api(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.28.3"))
    implementation(project(":aster-vaadin-native"))
    implementation("io.quarkus:quarkus-core-deployment")
    implementation("io.quarkus:quarkus-arc-deployment")
    implementation("io.quarkus:quarkus-undertow-deployment")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // 关闭此模块的 -Werror 影响，避免使用被标记为 removal 的部署期API触发失败
    options.compilerArgs.remove("-Werror")
    options.compilerArgs.addAll(listOf("-Xlint:all","-Xlint:-removal"))
}

repositories {
    mavenLocal()
    mavenCentral()
}
