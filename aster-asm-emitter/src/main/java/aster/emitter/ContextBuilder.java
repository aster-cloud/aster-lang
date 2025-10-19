package aster.emitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 负责从 CoreModel.Module 构建符号上下文索引。
 */
public final class ContextBuilder {
  private final Map<String, CoreModel.Enum> enumMap = new LinkedHashMap<>();
  private final Map<String, CoreModel.Data> dataSchema = new LinkedHashMap<>();
  private final Map<String, List<String>> enumVariants = new LinkedHashMap<>();
  private final Map<String, String> variantToEnum = new LinkedHashMap<>();
  private final CoreModel.Module module;
  private final String moduleName;

  public ContextBuilder(CoreModel.Module module) {
    this.module = Objects.requireNonNull(module, "module");
    this.moduleName = normalizeModuleName(module.name);
    buildIndexes();
  }

  public CoreModel.Module module() {
    return module;
  }

  public CoreModel.Data lookupData(String pkg, String name) {
    return dataSchema.get(canonicalKey(pkg, name));
  }

  public CoreModel.Data lookupData(String fullName) {
    return dataSchema.get(canonicalKey(fullName));
  }

  public CoreModel.Enum lookupEnum(String pkg, String name) {
    return enumMap.get(canonicalKey(pkg, name));
  }

  public CoreModel.Enum lookupEnum(String fullName) {
    return enumMap.get(canonicalKey(fullName));
  }

  public List<String> getEnumVariants(String enumName) {
    var variants = enumVariants.get(canonicalKey(enumName));
    return variants == null ? null : variants;
  }

  public String findEnumOwner(String variant) {
    return variantToEnum.get(variant);
  }

  public Map<String, CoreModel.Enum> enumMap() {
    return Collections.unmodifiableMap(enumMap);
  }

  public Map<String, CoreModel.Data> dataSchema() {
    return Collections.unmodifiableMap(dataSchema);
  }

  public Map<String, List<String>> enumVariants() {
    return Collections.unmodifiableMap(enumVariants);
  }

  private void buildIndexes() {
    if (module.decls == null) return;
    for (var decl : module.decls) {
      if (decl instanceof CoreModel.Enum en) {
        String enumKey = canonicalKey(en.name);
        enumMap.put(enumKey, en);
        List<String> variants = enumVariants.computeIfAbsent(enumKey, __ -> new ArrayList<>());
        if (en.variants != null) {
          for (var variant : en.variants) {
            variants.add(variant);
            variantToEnum.put(variant, enumKey);
          }
        }
      } else if (decl instanceof CoreModel.Data data) {
        dataSchema.put(canonicalKey(data.name), data);
      }
    }
    enumVariants.replaceAll((__, variants) -> List.copyOf(variants));
  }

  private String canonicalKey(String pkg, String name) {
    if (name == null || name.isBlank()) return name;
    if (name.contains(".")) return name;
    String effectivePkg = normalizeModuleName(pkg);
    if (effectivePkg == null || effectivePkg.isBlank()) return name;
    return effectivePkg + "." + name;
  }

  private String canonicalKey(String fullName) {
    if (fullName == null || fullName.isBlank()) return fullName;
    if (fullName.contains(".")) return fullName;
    if (moduleName == null || moduleName.isBlank()) return fullName;
    return moduleName + "." + fullName;
  }

  private String normalizeModuleName(String name) {
    if (name == null || name.isBlank()) return "app";
    return name;
  }
}

