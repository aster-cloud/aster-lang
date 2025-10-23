allprojects {
  repositories { mavenCentral() }
}

subprojects {
  if (name.endsWith("-jvm")) {
    tasks.matching { it.name == "generateAsterJar" }.configureEach {
      notCompatibleWithConfigurationCache("generateAsterJar uses external processes and dynamic IO")
    }
  }

  if (name.endsWith("-native")) {
    // GraalVM Build Tools 生成任务在 Gradle 9 配置缓存下不可序列化，禁用缓存写入
    tasks.matching { it.name == "generateResourcesConfigFile" }.configureEach {
      notCompatibleWithConfigurationCache("Graal generateResourcesConfigFile not cache-safe on Gradle 9")
    }
    tasks.matching { it.name == "generateReachabilityMetadata" }.configureEach {
      notCompatibleWithConfigurationCache("Graal generateReachabilityMetadata not cache-safe on Gradle 9")
    }
    tasks.matching { it.name == "nativeCompile" }.configureEach {
      notCompatibleWithConfigurationCache("Graal nativeCompile uses non-serializable configs on Gradle 9")
    }
  }
}
