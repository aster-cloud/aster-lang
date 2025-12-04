import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.Project

plugins {
    id("java")
    id("jacoco")
    id("info.solidsoft.pitest") version "1.19.0-rc.2"
}

repositories {
    mavenCentral()
    mavenLocal()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    // Aster runtime dependency
    implementation(project(":aster-runtime"))
    implementation(project(":aster-validation"))
    implementation("jakarta.validation:jakarta.validation-api:3.0.2")

    // Jackson for JSON serialization
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")

    // Testing dependencies
    testImplementation("net.jqwik:jqwik:1.8.2")
    testImplementation("org.skyscreamer:jsonassert:1.5.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.assertj:assertj-core:3.27.6")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf(
        "-parameters",
        "--enable-preview"  // Enable preview features for Java Records enhancements
    ))
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("jqwik", "junit-jupiter")
    }
    jvmArgs = listOf("--enable-preview")
}

jacoco {
    toolVersion = "0.8.14"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

// PIT Mutation Testing 配置
pitest {
    // JUnit 5 支持
    junit5PluginVersion.set("1.2.1")

    // 性能优化：使用多线程
    threads.set(Runtime.getRuntime().availableProcessors())

    // 目标类：仅测试核心业务代码
    targetClasses.set(listOf("com.wontlost.aster.finance.*"))

    // 变异器配置：使用默认变异器组
    mutators.set(setOf("DEFAULTS"))

    // 输出格式：HTML + XML
    outputFormats.set(setOf("HTML", "XML"))

    // 时间限制：每个测试最多 10 秒
    timeoutConstInMillis.set(10000)

    // 覆盖率阈值（低于阈值会导致构建失败）
    mutationThreshold.set(75)  // ≥75% mutation score
    coverageThreshold.set(80)  // ≥80% line coverage

    // 历史数据支持（加速增量分析）
    timestampedReports.set(false)

    // 详细输出
    verbose.set(false)
}

private val MODULE_REGEX = Regex(
    pattern = "^This\\s+module\\s+is\\s+([A-Za-z0-9_.]+)\\.",
    options = setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
)

private data class DtoTarget(
    val name: String,
    val modulePrefix: String,
    val basePackage: String,
    val sourceDir: File,
    val outputDir: File
)

private fun Project.dtoTargets(): List<DtoTarget> = listOf(
    DtoTarget(
        name = "finance",
        modulePrefix = "aster.finance",
        basePackage = "com.wontlost.aster.finance.dto",
        sourceDir = rootProject.layout.projectDirectory.dir("quarkus-policy-api/src/main/resources/policies/finance").asFile,
        outputDir = layout.projectDirectory.dir("src/main/java/com/wontlost/aster/finance/dto").asFile
    ),
    DtoTarget(
        name = "insurance",
        modulePrefix = "aster.insurance",
        basePackage = "com.wontlost.aster.insurance.dto",
        sourceDir = rootProject.layout.projectDirectory.dir("quarkus-policy-api/src/main/resources/policies/insurance").asFile,
        outputDir = layout.projectDirectory.dir("src/main/java/com/wontlost/aster/insurance/dto").asFile
    ),
    DtoTarget(
        name = "healthcare",
        modulePrefix = "aster.healthcare",
        basePackage = "com.wontlost.aster.healthcare.dto",
        sourceDir = rootProject.layout.projectDirectory.dir("quarkus-policy-api/src/main/resources/policies/healthcare").asFile,
        outputDir = layout.projectDirectory.dir("src/main/java/com/wontlost/aster/healthcare/dto").asFile
    )
)

val generateFinanceDtos by tasks.registering {
    // 确保先同步 policy classes，避免使用未生成的 DSL 输出
    dependsOn(
        ":quarkus-policy-api:syncPolicyClasses",
        ":quarkus-policy-api:syncPolicyJar"
    )
    val targets = dtoTargets()
    targets.forEach { target ->
        inputs.dir(target.sourceDir)
        outputs.dir(target.outputDir)
    }
    notCompatibleWithConfigurationCache("DTO 生成器依赖运行期扫描 DSL 并写入源码目录")

    doLast {
        val rootDir = rootProject.layout.projectDirectory.asFile
        var totalGenerated = 0
        targets.forEach { target ->
            target.outputDir.deleteRecursively()
            val specs = parseDtoSpecs(target)
            if (specs.isEmpty()) {
                logger.warn("[finance-dto:${target.name}] 未在 ${target.sourceDir.relativeToOrSelf(rootDir)} 找到可生成的 DSL 模块")
                return@forEach
            }
            specs.forEach { spec ->
                val targetFile = File(target.outputDir, spec.relativePath)
                targetFile.parentFile.mkdirs()
                targetFile.writeText(renderDtoRecord(spec), Charsets.UTF_8)
            }
            totalGenerated += specs.size
            logger.lifecycle("[finance-dto:${target.name}] 生成 ${specs.size} 个 DTO，覆盖 ${specs.map { it.module }.toSet().size} 个模块")
        }
        logger.lifecycle("[finance-dto] 累计生成 ${totalGenerated} 个 DTO（${targets.size} 个领域）")
    }
}

tasks.named("compileJava") {
    dependsOn(generateFinanceDtos)
}

private data class DtoSpec(
    val source: File,
    val module: String,
    val name: String,
    val fields: List<DslField>,
    val target: DtoTarget
) {
    private val packageSuffix = module.removePrefix(target.modulePrefix).trimStart('.')
    val packageName: String = if (packageSuffix.isBlank()) {
        target.basePackage
    } else {
        "${target.basePackage}.$packageSuffix"
    }
    private val relativeDir = packageName.removePrefix(target.basePackage).trimStart('.').replace('.', '/')
    val relativePath: String = buildString {
        if (relativeDir.isNotBlank()) {
            append(relativeDir)
            append('/')
        }
        append(name)
        append(".java")
    }
}

private data class DslField(
    val name: String,
    val typeName: String,
    val nullable: Boolean,
    val annotations: List<DslAnnotation>
)

private data class DslAnnotation(
    val name: String,
    val params: List<Pair<String, String>>
)

private data class JavaTypeInfo(
    val type: String,
    val primitive: Boolean,
    val nullable: Boolean,
    val imports: MutableSet<String>
)

private fun parseDtoSpecs(target: DtoTarget): List<DtoSpec> {
    val dslRoot = target.sourceDir
    if (!dslRoot.exists()) return emptyList()
    val files = dslRoot.walkTopDown()
        .filter { it.isFile && it.extension == "aster" }
        .sortedBy { it.relativeToOrSelf(dslRoot).path }
    val specMap = LinkedHashMap<String, DtoSpec>()
    files.forEach { file ->
        val content = file.readText()
        val module = MODULE_REGEX.find(content)?.groupValues?.getOrNull(1)?.trim()
            ?: throw GradleException("未在 ${file.relativeToOrSelf(dslRoot)} 中找到模块声明")
        if (!module.startsWith(target.modulePrefix)) return@forEach
        val definitions = extractDtoDefinitions(content, file)
        definitions.forEach { (name, rawFields) ->
            val fields = parseDtoFields(rawFields, file)
            val spec = DtoSpec(file, module, name, fields, target)
            val key = "${spec.module}:${spec.name}"
            specMap.putIfAbsent(key, spec)
        }
    }
    return specMap.values.sortedWith(compareBy({ it.module }, { it.name }))
}

private fun extractDtoDefinitions(content: String, source: File): List<Pair<String, String>> {
    val lines = content.lines()
    val result = mutableListOf<Pair<String, String>>()
    var index = 0
    while (index < lines.size) {
        val trimmed = lines[index].trim()
        if (!trimmed.startsWith("Define ")) {
            index++
            continue
        }
        val afterDefine = trimmed.removePrefix("Define ").trim()
        val name = afterDefine.substringBefore(" with").trim()
        var remainder = afterDefine.substringAfter(" with", "")
        val buffer = StringBuilder()
        fun appendChunk(chunk: String) {
            val clean = chunk.trim()
            if (clean.isEmpty() || clean.startsWith("//")) return
            if (buffer.hasContent()) buffer.append(' ')
            buffer.append(clean)
        }
        appendChunk(remainder)
        var cursor = index + 1
        var closed = remainder.trim().endsWith(".")
        while (!closed) {
            if (cursor >= lines.size) {
                throw GradleException("DTO 定义 \"$name\" 在 ${source.name} 中未正确结束")
            }
            val chunk = lines[cursor].trim()
            appendChunk(chunk)
            closed = chunk.endsWith(".")
            cursor++
        }
        if (!buffer.hasContent()) {
            throw GradleException("DTO 定义 \"$name\" 缺少字段内容：${source.name}")
        }
        if (buffer[buffer.length - 1] == '.') {
            buffer.setLength(buffer.length - 1)
        }
        result.add(name to buffer.toString().trim())
        index = cursor
    }
    return result
}

private fun parseDtoFields(raw: String, source: File): List<DslField> {
    val segments = splitFieldSegments(raw)
    if (segments.isEmpty()) {
        throw GradleException("在 ${source.name} 中未解析到任何字段")
    }
    return segments.map { segment ->
        val trimmed = segment.trim()
        val (annotations, remainder) = extractAnnotations(trimmed)
        val colonIndex = remainder.indexOf(':')
        if (colonIndex == -1) {
            throw GradleException("字段缺少类型声明：\"$remainder\" @ ${source.name}")
        }
        val fieldName = remainder.substring(0, colonIndex).trim()
        val typeToken = remainder.substring(colonIndex + 1).trim()
        val (typeName, nullable) = normalizeType(typeToken)
        DslField(fieldName, typeName, nullable, annotations)
    }
}

private fun splitFieldSegments(raw: String): List<String> {
    val result = mutableListOf<String>()
    val builder = StringBuilder()
    var depth = 0
    var inString = false
    var quoteChar = '"'
    raw.forEach { ch ->
        when (ch) {
            '"', '\'' -> {
                if (inString && ch == quoteChar) {
                    inString = false
                } else if (!inString) {
                    inString = true
                    quoteChar = ch
                }
                builder.append(ch)
            }
            '(' -> {
                if (!inString) depth++
                builder.append(ch)
            }
            ')' -> {
                if (!inString && depth > 0) depth--
                builder.append(ch)
            }
            ',' -> {
                if (!inString && depth == 0) {
                    val value = builder.toString().trim().removeSuffix(".")
                    if (value.isNotEmpty()) result.add(value)
                    builder.setLength(0)
                } else {
                    builder.append(ch)
                }
            }
            else -> builder.append(ch)
        }
    }
    val last = builder.toString().trim().removeSuffix(".")
    if (last.isNotEmpty()) {
        result.add(last)
    }
    return result
}

private fun extractAnnotations(segment: String): Pair<List<DslAnnotation>, String> {
    val annotations = mutableListOf<DslAnnotation>()
    var cursor = segment.trimStart()
    val pattern = Regex("^@([A-Za-z_][A-Za-z0-9_]*)(\\(([^)]*)\\))?\\s*")
    while (true) {
        val match = pattern.find(cursor) ?: break
        val name = match.groupValues[1]
        val paramsRaw = match.groupValues.getOrNull(3)
        annotations.add(DslAnnotation(name, parseAnnotationParams(paramsRaw)))
        cursor = cursor.substring(match.value.length).trimStart()
    }
    return annotations to cursor
}

private fun parseAnnotationParams(raw: String?): List<Pair<String, String>> {
    if (raw.isNullOrBlank()) return emptyList()
    val segments = splitFieldSegments(raw)
    return segments.mapNotNull {
        val idx = it.indexOf(':')
        if (idx == -1) return@mapNotNull null
        val key = it.substring(0, idx).trim()
        val value = it.substring(idx + 1).trim()
        if (key.isEmpty() || value.isEmpty()) null else key to value
    }
}

private fun normalizeType(typeToken: String): Pair<String, Boolean> {
    val trimmed = typeToken.trim()
    return if (trimmed.endsWith("?")) {
        trimmed.dropLast(1).trim() to true
    } else {
        trimmed to false
    }
}

private fun renderDtoRecord(spec: DtoSpec): String {
    val imports = sortedSetOf<String>()
    val fieldBlocks = spec.fields.mapIndexed { index, field ->
        renderField(field, index == spec.fields.lastIndex, imports)
    }
    val importSection = if (imports.isEmpty()) "" else imports.joinToString(separator = "\n", postfix = "\n\n") { "import $it;" }
    val doc = "/**\n * Aster DSL 自动生成 DTO：${spec.name}（模块 ${spec.module}）。\n */"
    return buildString {
        appendLine("package ${spec.packageName};")
        appendLine()
        if (importSection.isNotEmpty()) {
            append(importSection)
        }
        appendLine(doc)
        appendLine("public record ${spec.name}(")
        appendLine(fieldBlocks.joinToString("\n"))
        appendLine(") {}")
    }
}

private fun renderField(field: DslField, isLast: Boolean, imports: MutableSet<String>): String {
    val typeInfo = mapJavaType(field)
    imports.addAll(typeInfo.imports)
    val annotations = mutableListOf<String>()
    if (!typeInfo.primitive && !typeInfo.nullable) {
        annotations.add("@NotNull")
        imports.add("jakarta.validation.constraints.NotNull")
    }
    annotations.addAll(field.annotations.mapNotNull { renderAnnotation(it, imports) })
    val suffix = if (isLast) "" else ","
    val lines = mutableListOf<String>()
    annotations.forEach { lines.add("  $it") }
    lines.add("  ${typeInfo.type} ${field.name}$suffix")
    return lines.joinToString("\n")
}

private fun renderAnnotation(annotation: DslAnnotation, imports: MutableSet<String>): String? =
    when (annotation.name) {
        "Range" -> {
            imports.add("io.aster.validation.constraints.Range")
            val params = renderAnnotationParams(annotation.params)
            if (params.isEmpty()) "@Range" else "@Range($params)"
        }
        "NotEmpty" -> {
            imports.add("io.aster.validation.constraints.NotEmpty")
            "@NotEmpty"
        }
        "Pattern" -> {
            imports.add("io.aster.validation.constraints.Pattern")
            val params = renderAnnotationParams(annotation.params)
            if (params.isEmpty()) "@Pattern" else "@Pattern($params)"
        }
        else -> "@${annotation.name}"
    }

private fun renderAnnotationParams(params: List<Pair<String, String>>): String =
    params.joinToString(", ") { (key, value) -> "$key = ${formatAnnotationValue(value)}" }

private fun formatAnnotationValue(rawValue: String): String {
    val normalized = rawValue.trim()
    normalized.toLongOrNull()?.let { return normalized }
    normalized.toDoubleOrNull()?.let { return normalized }
    if (normalized.equals("true", true) || normalized.equals("false", true)) {
        return normalized.lowercase()
    }
    val unquoted = normalized.removeSurrounding("\"").removeSurrounding("'")
    return "\"${escapeJavaString(unquoted)}\""
}

private fun escapeJavaString(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

private fun mapJavaType(field: DslField): JavaTypeInfo {
    return when (field.typeName) {
        "Int" -> {
            if (field.nullable) {
                JavaTypeInfo("Integer", false, true, mutableSetOf())
            } else {
                JavaTypeInfo("int", true, false, mutableSetOf())
            }
        }
        "Long" -> {
            if (field.nullable) {
                JavaTypeInfo("Long", false, true, mutableSetOf())
            } else {
                JavaTypeInfo("long", true, false, mutableSetOf())
            }
        }
        "Double" -> {
            if (field.nullable) {
                JavaTypeInfo("Double", false, true, mutableSetOf())
            } else {
                JavaTypeInfo("double", true, false, mutableSetOf())
            }
        }
        "Bool", "Boolean" -> {
            if (field.nullable) {
                JavaTypeInfo("Boolean", false, true, mutableSetOf())
            } else {
                JavaTypeInfo("boolean", true, false, mutableSetOf())
            }
        }
        "Text" -> JavaTypeInfo("String", false, field.nullable, mutableSetOf())
        else -> JavaTypeInfo(field.typeName, false, field.nullable, mutableSetOf())
    }
}

private fun StringBuilder.hasContent(): Boolean = length > 0

private fun File.relativeToOrSelf(root: File): File =
    if (absolutePath.startsWith(root.absolutePath)) {
        relativeTo(root)
    } else {
        this
    }
