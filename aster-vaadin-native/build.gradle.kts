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
    compileOnly("io.quarkus:quarkus-arc")
    compileOnly("io.quarkus:quarkus-undertow")  // For Jakarta Servlet API
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

repositories {
    mavenLocal()
    mavenCentral()
}
