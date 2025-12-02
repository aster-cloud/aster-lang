package aster.emitter.test;

import aster.runtime.AsterCapability;
import aster.runtime.AsterPii;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 端到端集成测试专用工具：遍历指定模块下生成的 JVM 类，打印方法上的 @AsterPii/@AsterCapability 注解。
 */
public final class AnnotationVerifier {

  private AnnotationVerifier() {}

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println("用法: AnnotationVerifier <模块名称> [class 输出目录]");
      System.exit(2);
    }
    String moduleName = args[0];
    Path classesDir = args.length >= 2 ? Paths.get(args[1]) : Paths.get("build/jvm-classes");
    verifyModule(moduleName, classesDir);
  }

  private static void verifyModule(String moduleName, Path classesDir) throws Exception {
    if (!Files.exists(classesDir)) {
      throw new IOException("未找到类输出目录: " + classesDir.toAbsolutePath());
    }
    String modulePath = moduleName.replace('.', '/');
    Path moduleDir = classesDir.resolve(modulePath);
    if (!Files.exists(moduleDir)) {
      throw new IOException("模块 " + moduleName + " 未生成类文件，期待路径: " + moduleDir.toAbsolutePath());
    }
    List<Path> classFiles;
    try (Stream<Path> files = Files.walk(moduleDir)) {
      classFiles = files
        .filter(Files::isRegularFile)
        .filter(path -> path.getFileName().toString().endsWith(".class"))
        .sorted()
        .collect(Collectors.toList());
    }
    if (classFiles.isEmpty()) {
      throw new IOException("模块 " + moduleName + " 没有可用的 .class 文件");
    }
    try (URLClassLoader loader = new URLClassLoader(new URL[] { classesDir.toUri().toURL() })) {
      for (Path classFile : classFiles) {
        String relative = moduleDir.relativize(classFile).toString().replace('/', '.').replace('\\', '.');
        if (relative.endsWith(".class")) {
          relative = relative.substring(0, relative.length() - ".class".length());
        }
        String className = moduleName + "." + relative;
        inspectClass(loader, className);
      }
    }
  }

  private static void inspectClass(ClassLoader loader, String className) throws ClassNotFoundException {
    Class<?> clazz = Class.forName(className, true, loader);
    System.out.println("类: " + className);
    for (Method method : clazz.getDeclaredMethods()) {
      System.out.println("  方法: " + method.getName());
      AsterPii pii = method.getAnnotation(AsterPii.class);
      if (pii != null) {
        System.out.printf(
          "    @AsterPii(level=\"%s\", categories=%s)%n",
          pii.level(),
          Arrays.toString(pii.categories())
        );
      }
      AsterCapability capability = method.getAnnotation(AsterCapability.class);
      if (capability != null) {
        System.out.printf(
          "    @AsterCapability(effects=%s, capabilities=%s)%n",
          Arrays.toString(capability.effects()),
          Arrays.toString(capability.capabilities())
        );
      }
    }
  }
}
