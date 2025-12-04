plugins {
    `java-library`
}

repositories {
    mavenCentral()
    mavenLocal()
}

group = "io.aster"
version = "0.2.0"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    // 日志
    implementation("org.slf4j:slf4j-api:2.0.9")
    
    // 测试
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.assertj:assertj-core:3.27.6")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.19")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf(
        "-parameters",  // 保留参数名
        "-Xlint:deprecation",
        "-Xlint:unchecked"
    ))
}
