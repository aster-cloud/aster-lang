rootProject.name = "aster-lang"
include(":aster-asm-emitter")
include(":aster-runtime")
project(":aster-runtime").projectDir = file("aster-runtime")

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


