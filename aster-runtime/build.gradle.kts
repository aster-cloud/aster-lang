plugins {
  java
}

java {
  toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
}

repositories { mavenCentral() }

dependencies {
  implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.28.3"))
  implementation("io.quarkus:quarkus-cache")
  implementation("io.quarkus:quarkus-core")
  implementation("io.smallrye.common:smallrye-common-net") // For CidrAddress (GraalVM substitutions)
  implementation("jakarta.enterprise:jakarta.enterprise.cdi-api:4.0.1")
  implementation("jakarta.inject:jakarta.inject-api:2.0.1")
}

tasks.withType<Jar> {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
