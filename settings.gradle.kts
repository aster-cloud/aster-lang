rootProject.name = "aster-lang"
include(":aster-asm-emitter")
include(":aster-runtime")
project(":aster-runtime").projectDir = file("aster-runtime")

// Native Image 主项目
include(":aster-lang-native")
project(":aster-lang-native").projectDir = file("aster-lang-native")

// Examples: include all JVM/native example projects
include(":examples:hello-native")
project(":examples:hello-native").projectDir = file("examples/hello-native")
include(":examples:list-jvm")
project(":examples:list-jvm").projectDir = file("examples/list-jvm")
include(":examples:login-jvm")
project(":examples:login-jvm").projectDir = file("examples/login-jvm")
include(":examples:login-native")
project(":examples:login-native").projectDir = file("examples/login-native")
include(":examples:map-jvm")
project(":examples:map-jvm").projectDir = file("examples/map-jvm")
include(":examples:math-jvm")
project(":examples:math-jvm").projectDir = file("examples/math-jvm")
include(":examples:text-jvm")
project(":examples:text-jvm").projectDir = file("examples/text-jvm")
include(":examples:cli-jvm")
project(":examples:cli-jvm").projectDir = file("examples/cli-jvm")
include(":examples:rest-jvm")
project(":examples:rest-jvm").projectDir = file("examples/rest-jvm")
include(":examples:policy-jvm")
project(":examples:policy-jvm").projectDir = file("examples/policy-jvm")
// Expose policy-editor as a root-level module (alias to former examples/policy-editor)
include(":policy-editor")
project(":policy-editor").projectDir = file("policy-editor")

// Vaadin native integration extension
include(":aster-vaadin-native")
project(":aster-vaadin-native").projectDir = file("aster-vaadin-native")
include(":aster-vaadin-native-deployment")
project(":aster-vaadin-native-deployment").projectDir = file("aster-vaadin-native-deployment")
// Phase 1: Quarkus Policy API
include(":quarkus-policy-api")
project(":quarkus-policy-api").projectDir = file("quarkus-policy-api")
