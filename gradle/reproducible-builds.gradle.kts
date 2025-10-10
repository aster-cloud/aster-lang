/**
 * Gradle 确定性构建配置
 *
 * 用途：确保构建产物可重现（相同输入 → 相同输出）
 * 参考：https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives
 */

// 应用到所有项目
allprojects {
  // 启用构建缓存
  gradle.startParameter.isBuildCacheEnabled = true

  tasks.withType<AbstractArchiveTask>().configureEach {
    // 固定时间戳：确保 JAR/TAR 文件时间戳一致
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    // 固定文件权限
    fileMode = 0b110100100  // 0644
    dirMode = 0b111101101   // 0755
  }

  tasks.withType<JavaCompile>().configureEach {
    // 固定编译参数
    options.compilerArgs.addAll(listOf(
      "-Xlint:all",
      "-Werror"
    ))

    // 禁用时间戳依赖
    options.isIncremental = false  // 可选：禁用增量编译以提高确定性
  }

  // Gradle 元数据
  tasks.withType<GenerateModuleMetadata>().configureEach {
    // 确保模块元数据确定性
    enabled = true
  }
}

// 配置构建缓存
gradle.settingsEvaluated {
  buildCache {
    local {
      isEnabled = true
      directory = file("${rootProject.projectDir}/.gradle/build-cache")
      removeUnusedEntriesAfterDays = 30
    }

    // 生产环境可配置远程缓存（如 S3）
    // remote<HttpBuildCache> {
    //   url = uri("https://build-cache.example.com/")
    //   isEnabled = System.getenv("CI") == "true"
    //   isPush = System.getenv("CI_BRANCH") == "main"
    // }
  }
}
