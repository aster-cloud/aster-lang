plugins {
    id("java")
}

group = "org.asterlang"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    // Placeholders; add ANTLR and logging as needed
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(org.gradle.api.JavaVersion.VERSION_21)
    }
}
