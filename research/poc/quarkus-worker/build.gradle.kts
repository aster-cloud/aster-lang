plugins {
    id("java")
    id("io.quarkus") version "3.9.4"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.9.4"))
    implementation("io.quarkus:quarkus-resteasy-reactive")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}

java {
    toolchain {
        languageVersion.set(org.gradle.api.JavaVersion.VERSION_21)
    }
}

group = "com.example"
version = "0.1.0"
