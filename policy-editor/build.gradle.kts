plugins {
    java
    id("io.quarkus") version "3.28.3"
    id("com.vaadin") version "24.9.5"
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

// 标记当前是否为 Vaadin 生产构建（需生成 flow-build-info.json）
val isVaadinProductionBuild: Boolean by lazy {
    project.hasProperty("vaadin.productionMode")
}

dependencies {
    // Quarkus BOM
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.28.3"))

    // Quarkus REST
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")

    // Vaadin with Quarkus
    if (!isNativeBuild) {
        implementation("com.vaadin:vaadin-quarkus-extension:24.9.5")
        // Aster Vaadin Native integration (runtime artifact brings deployment via quarkus extension mechanism)
        implementation(project(":aster-vaadin-native"))
    }

    // JSON Processing
    implementation("tools.jackson.core:jackson-databind:3.0.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.20.0")

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
    testImplementation("com.github.tomakehurst:wiremock-jre8:3.0.1")
    testRuntimeOnly("com.vaadin:vaadin-dev-server:24.9.5")
}

vaadin {
    // CI/生产构建使用 pnpm，避免重复下载 node_modules
    pnpmEnable = true
    productionMode = isVaadinProductionBuild
}

// 移除严格警告依赖于 Vaadin 的编译器提示

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf(
        "-parameters",
        "-Xlint:all",
        "-Xlint:-processing",
        "-Xlint:-this-escape",  // Vaadin 组件初始化时的误报警告
        "-Xlint:-classfile",    // MicroProfile Config 依赖缺失 OSGI 注解导致的警告
        "-Werror"
    ))
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}

// quarkusDev 任务配置：添加 Java 25 兼容性 JVM 参数
// 通过系统属性传递给 Quarkus Dev Mode
quarkus {
    // Quarkus Dev Mode JVM arguments
    val jvmArgsValue = listOf("--add-opens", "java.base/java.lang=ALL-UNNAMED")
    System.setProperty("quarkus.args", jvmArgsValue.joinToString(" "))
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

if (isVaadinProductionBuild && !isNativeBuild) {
    // 生产构建确保 Vaadin 前端在打包前完成
    tasks.named("processResources") {
        dependsOn("vaadinPrepareFrontend")
    }
    tasks.named("quarkusBuild") {
        dependsOn("vaadinBuildFrontend")
    }
}
