package aster.emitter;

import aster.core.ir.CoreModel;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 读取 CoreModel 模块及其辅助配置的加载器。
 */
public final class ModuleLoader {

  private static Map<String, String> envOverride;

  private ModuleLoader() {}

  static void setEnvOverrideForTesting(Map<String, String> env) {
    envOverride = env;
  }

  static void clearEnvOverrideForTesting() {
    envOverride = null;
  }

  public static CoreContext load(InputStream in) throws IOException {
    Objects.requireNonNull(in, "输入流不能为空");
    ObjectMapper mapper = newMapper();
    CoreModel.Module module = loadModule(mapper, in);
    Map<String, Map<String, String>> hints = loadHints(mapper, getEnv("HINTS_PATH"));
    NullPolicy nullPolicy = loadNullPolicy(mapper);
    boolean diagOverload = loadDiagOverload();
    return new CoreContext(module, hints, nullPolicy, diagOverload);
  }

  static CoreModel.Module loadModule(InputStream in) throws IOException {
    return loadModule(newMapper(), in);
  }

  private static CoreModel.Module loadModule(ObjectMapper mapper, InputStream in) throws IOException {
    var stdin = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    return mapper.readValue(stdin, CoreModel.Module.class);
  }

  static Map<String, Map<String, String>> loadHints(String hintsPath) {
    return loadHints(newMapper(), hintsPath);
  }

  private static Map<String, Map<String, String>> loadHints(ObjectMapper mapper, String hintsPath) {
    Map<String, Map<String, String>> hints = new LinkedHashMap<>();
    if (hintsPath != null && !hintsPath.isEmpty()) {
      try {
        var txt = Files.readString(Paths.get(hintsPath));
        JsonNode node = mapper.readTree(txt);
        JsonNode functions = node.get("functions");
        if (functions != null && functions.isObject()) {
          var it = functions.fields();
          while (it.hasNext()) {
            var entry = it.next();
            String fnName = entry.getKey();
            JsonNode obj = entry.getValue();
            if (obj != null && obj.isObject()) {
              Map<String, String> m = new LinkedHashMap<>();
              var it2 = obj.fields();
              while (it2.hasNext()) {
                var arg = it2.next();
                String kind = arg.getValue().asText("");
                if ("I".equals(kind) || "J".equals(kind) || "D".equals(kind)) {
                  m.put(arg.getKey(), kind);
                }
              }
              hints.put(fnName, m);
            }
          }
        }
        System.out.println("Loaded hints from " + hintsPath + ": " + hints.size() + " functions");
      } catch (Exception ex) {
        System.err.println("WARN: failed to load hints: " + ex.getMessage());
      }
    }
    return hints;
  }

  static NullPolicy loadNullPolicy() {
    return loadNullPolicy(newMapper());
  }

  private static NullPolicy loadNullPolicy(ObjectMapper mapper) {
    boolean strict = false;
    Map<String, boolean[]> overrides = new LinkedHashMap<>();
    try {
      String ns = firstNonEmpty(getEnv("NULL_STRICT"), getEnv("INTEROP_NULL_STRICT"));
      if (ns != null && !ns.isEmpty()) {
        strict = Boolean.parseBoolean(ns);
      }
      String overrideEnvName = null;
      String overridePath = firstNonEmpty(
          getEnv("NULL_POLICY_OVERRIDE"),
          getEnv("INTEROP_NULL_POLICY")
      );
      if (overridePath != null) {
        if (overridePath.equals(getEnv("NULL_POLICY_OVERRIDE"))) {
          overrideEnvName = "NULL_POLICY_OVERRIDE";
        } else if (overridePath.equals(getEnv("INTEROP_NULL_POLICY"))) {
          overrideEnvName = "INTEROP_NULL_POLICY";
        }
      }
      if (overridePath != null && !overridePath.isEmpty()) {
        try {
          Path path = Paths.get(overridePath);
          JsonNode node = mapper.readTree(Files.readString(path));
          var f = node.fields();
          while (f.hasNext()) {
            var e = f.next();
            JsonNode arr = e.getValue();
            if (arr != null && arr.isArray()) {
              boolean[] vals = new boolean[arr.size()];
              for (int i = 0; i < arr.size(); i++) {
                vals[i] = arr.get(i).asBoolean(false);
              }
              overrides.put(e.getKey(), vals);
            }
          }
        } catch (Exception ex) {
          String name = (overrideEnvName != null) ? overrideEnvName : "NULL_POLICY_OVERRIDE";
          System.err.println("WARN: failed to load " + name + ": " + ex.getMessage());
        }
      }
    } catch (Throwable __) {
      // 保持与旧实现一致：忽略所有异常
    }
    return new NullPolicy(strict, overrides);
  }

  static boolean loadDiagOverload() {
    boolean diag = true;
    try {
      String d = getEnv("DIAG_OVERLOAD");
      if (d != null && !d.isEmpty()) {
        diag = Boolean.parseBoolean(d);
      }
    } catch (Throwable __) {
      // 忽略异常，保持默认值
    }
    return diag;
  }

  private static ObjectMapper newMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }

  private static String getEnv(String key) {
    Map<String, String> env = envOverride;
    if (env != null) {
      return env.get(key);
    }
    return System.getenv(key);
  }

  private static String firstNonEmpty(String... values) {
    if (values == null) return null;
    for (String v : values) {
      if (v != null && !v.isEmpty()) return v;
    }
    return null;
  }
}
