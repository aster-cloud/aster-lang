plugins {
    java
    id("io.quarkus") version "3.28.3"
}

repositories {
    // 先使用 Maven Central，避免本地 ~/.m2 缓存损坏导致解析到不存在的 *-classes.jar 等变体
    mavenCentral()
    mavenLocal()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

// 检测是否为 Native Image 构建（当使用 -Dquarkus.package.type=native 时）
val isNativeBuild: Boolean by lazy {
    val p = (project.findProperty("quarkus.package.type")?.toString()
        ?: System.getProperty("quarkus.package.type")
        ?: "").lowercase()
    p == "native"
}

// 检测是否在执行 quarkusDev（开发模式），用于启用 Vaadin DevServer
val isQuarkusDevTask: Boolean by lazy {
    gradle.startParameter.taskNames.any { it.contains("quarkusDev", ignoreCase = true) }
}

dependencies {
    // Quarkus BOM
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.28.3"))

    // Quarkus REST
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")

    // Vaadin with Quarkus（在原生镜像构建时禁用；开发模式保留 DevServer）
    if (!isNativeBuild) {
        if (isQuarkusDevTask) {
            // 开发模式：不排除 dev-server，确保 DevModeStartupListener 可用
            implementation("com.vaadin:vaadin-quarkus-extension:24.9.2")
        } else {
            // 非开发模式：排除 dev 相关以避免构建时加载
            implementation("com.vaadin:vaadin-quarkus-extension:24.9.2") {
                exclude(group = "com.vaadin", module = "vaadin-dev-server")
                exclude(group = "com.vaadin", module = "vaadin-dev-bundle")
            }
        }
        // Aster Vaadin Native integration (runtime artifact brings deployment via quarkus extension mechanism)
        implementation(project(":aster-vaadin-native"))
    }

    // JSON Processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.2")

    // GraphQL Client + WebClient（转发 GraphQL 请求）
    implementation("io.quarkus:quarkus-smallrye-graphql-client")

    // JSON Schema 校验
    implementation("com.networknt:json-schema-validator:1.0.86")

    // 安全与身份（OIDC/JWT）
    implementation("io.quarkus:quarkus-oidc")
    implementation("io.quarkus:quarkus-security")

    // Testing
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.quarkus:quarkus-test-security")
}

// 移除严格警告依赖于 Vaadin 的编译器提示

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf(
        "-parameters",
        "-Xlint:all",
        "-Xlint:-processing",
        "-Xlint:-this-escape",  // Vaadin 组件初始化时的误报警告
        "-Werror"
    ))
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}

// 在原生镜像构建时，排除 Vaadin UI 源码与 Web 资源，确保编译期无 Vaadin 依赖
sourceSets {
    if (isNativeBuild) {
        named("main") {
            java {
                // 排除 UI 相关 Java 源码（Vaadin 组件与 @Route 视图）
                exclude("editor/ui/**")
            }
            resources {
                // 排除前端与 webapp 相关资源（避免不必要打包）
                exclude("src/main/frontend/**")
                exclude("src/main/webapp/**")
            }
        }
    }
}

// 记录当前构建模式，方便排障
println("[policy-editor] Build mode: ${if (isNativeBuild) "native" else "jvm"}")

// 仅对 policy-editor 项目关闭 Configuration Cache（该项目包含 Quarkus/Graal 任务，易触发缓存序列化问题）
// 注意：存在该项目参与的构建将不写入缓存，但不影响其他仅构建非 policy-editor 的任务使用缓存。
tasks.configureEach {
    notCompatibleWithConfigurationCache("policy-editor uses Quarkus/Graal native tasks and dynamic plugin behavior")
}

// 开发模式下为 Vaadin 设置工作目录与前端生成目录，避免写入到根路径导致的只读错误
val prepareVaadinDevDirs by tasks.registering {
    doLast {
        val gen = project.layout.projectDirectory.dir("build/vaadin/generated").asFile
        val comps = project.layout.projectDirectory.dir("build/vaadin/web-components").asFile
        gen.mkdirs()
        comps.mkdirs()
    }
}

