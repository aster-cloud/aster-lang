package aster.emitter;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 签名解析器：通过反射解析 Java 方法签名
 *
 * 核心功能：
 * 1. 反射方法查找与匹配
 * 2. 参数类型兼容性评分
 * 3. 方法签名缓存（性能优化）
 * 4. Varargs 支持
 */
public class SignatureResolver {
    private final Map<String, String> reflectCache = new LinkedHashMap<>();
    private final Map<String, List<String>> methodCache = new LinkedHashMap<>();
    private final boolean diagOverload;

    public SignatureResolver(boolean diagOverload) {
        this.diagOverload = diagOverload;
    }

    /**
     * 通过反射解析方法签名
     *
     * @param ownerInternal 类内部名称（例如 "java/util/List"）
     * @param method 方法名
     * @param argDescs 参数类型描述符列表
     * @param retDesc 返回类型描述符（用于缓存键）
     * @return 完整的方法描述符，如果无法解析则返回 null
     */
    public String resolveMethodSignature(String ownerInternal, String method,
                                          List<String> argDescs, String retDesc) {
        try {
            // 检查缓存
            String key = ownerInternal + "#" + method + "#" + String.join(",", argDescs) + "->" + retDesc;
            if (reflectCache.containsKey(key)) {
                return reflectCache.get(key);
            }

            // 加载类
            String ownerName = ownerInternal.replace('/', '.');
            Class<?> cls = Class.forName(ownerName);

            // 获取所有方法
            Method[] methods = cls.getDeclaredMethods();

            // 更新方法缓存（轻量级，仅方法名+描述符）
            try {
                List<String> list = new ArrayList<>();
                for (Method mm : methods) {
                    list.add(mm.getName() + buildMethodDesc(mm));
                }
                Collections.sort(list);
                methodCache.put(ownerInternal, list);
            } catch (Throwable ignored) {
                // 忽略缓存更新失败
            }

            // 排序方法（确保确定性选择）
            Arrays.sort(methods, (a, b) -> {
                int c = a.getName().compareTo(b.getName());
                if (c != 0) return c;
                String sa = buildMethodDesc(a);
                String sb = buildMethodDesc(b);
                return sa.compareTo(sb);
            });

            // 查找最佳匹配方法
            Method best = null;
            int bestScore = Integer.MIN_VALUE;
            List<String> bestDescs = new ArrayList<>();

            for (Method m : methods) {
                if (!m.getName().equals(method)) continue;

                Class<?>[] params = m.getParameterTypes();
                boolean varargs = m.isVarArgs();

                // 参数数量检查
                if (!varargs && params.length != argDescs.size()) continue;
                if (varargs && params.length - 1 > argDescs.size()) continue;

                // 计算匹配评分
                int score = 0;
                boolean compatible = true;
                int fixed = varargs ? params.length - 1 : params.length;

                // 固定参数评分
                for (int i = 0; i < fixed; i++) {
                    Class<?> p = params[i];
                    String a = argDescs.get(i);
                    int s = scoreParameterMatch(p, a);
                    if (s < 0) {
                        compatible = false;
                        break;
                    }
                    score += s;
                }

                // Varargs 参数评分
                if (compatible && varargs) {
                    Class<?> comp = params[params.length - 1].getComponentType();
                    for (int i = fixed; i < argDescs.size(); i++) {
                        String a = argDescs.get(i);
                        int s = scoreParameterMatch(comp, a);
                        if (s < 0) {
                            compatible = false;
                            break;
                        }
                        score += s;
                    }
                }

                if (!compatible) continue;

                // 添加基本类型权重（优先选择基本类型参数的方法）
                int primCount = 0;
                for (Class<?> p : params) {
                    if (p.isPrimitive()) primCount++;
                }
                int total = score * 10 + primCount;

                // 更新最佳匹配
                if (total > bestScore) {
                    bestScore = total;
                    best = m;
                    bestDescs.clear();
                    bestDescs.add(buildMethodDesc(m));
                } else if (total == bestScore) {
                    bestDescs.add(buildMethodDesc(m));
                }
            }

            // 返回最佳匹配
            if (best != null) {
                String desc = buildMethodDesc(best);

                // 如果有多个相同评分的方法，选择字典序最小的（确定性）
                if (bestDescs.size() > 1) {
                    Collections.sort(bestDescs);
                    desc = bestDescs.get(0);
                }

                // 诊断输出（如果有歧义）
                if (diagOverload && bestDescs.size() > 1) {
                    System.err.println("AMBIGUOUS OVERLOAD: " +
                            ownerInternal.replace('/', '.') + "." + method +
                            "(" + String.join(",", argDescs) + ") -> candidates=" +
                            bestDescs + ", selected=" + desc);
                }

                // 缓存结果
                reflectCache.put(key, desc);
                return desc;
            }

        } catch (Throwable t) {
            // 反射失败时回退到方法缓存
            return fallbackToMethodCache(ownerInternal, method, argDescs);
        }

        return null;
    }

    /**
     * 回退到方法缓存（当反射失败时）
     */
    private String fallbackToMethodCache(String ownerInternal, String method, List<String> argDescs) {
        try {
            List<String> list = methodCache.get(ownerInternal);
            if (list == null || list.isEmpty()) return null;

            int bestScore = Integer.MIN_VALUE;
            String bestDesc = null;

            for (String nm : list) {
                if (!nm.startsWith(method)) continue;

                String desc = nm.substring(method.length());
                int r = desc.indexOf(')');
                if (!desc.startsWith("(") || r < 0) continue;

                // 解析参数类型
                String params = desc.substring(1, r);
                List<String> ptypes = new ArrayList<>();
                for (int i = 0; i < params.length(); ) {
                    char c = params.charAt(i);
                    if (c == 'L') {
                        int semi = params.indexOf(';', i);
                        if (semi < 0) break;
                        ptypes.add(params.substring(i, semi + 1));
                        i = semi + 1;
                    } else {
                        ptypes.add(String.valueOf(c));
                        i++;
                    }
                }

                // 参数数量检查
                if (ptypes.size() != argDescs.size()) continue;

                // 计算匹配评分
                int score = 0;
                boolean ok = true;
                for (int i = 0; i < ptypes.size(); i++) {
                    String p = ptypes.get(i);
                    String a = argDescs.get(i);
                    int s = scoreDescriptorMatch(p, a);
                    if (s < 0) {
                        ok = false;
                        break;
                    }
                    score += s;
                }

                if (!ok) continue;

                // 更新最佳匹配（选择评分最高的，相同评分选择字典序最小的）
                if (score > bestScore || (score == bestScore && (bestDesc == null || desc.compareTo(bestDesc) < 0))) {
                    bestScore = score;
                    bestDesc = desc;
                }
            }

            return bestDesc;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * 参数类型匹配评分
     *
     * @param paramType Java 参数类型
     * @param argDesc JVM 参数描述符
     * @return 评分（越高越匹配），-1 表示不兼容
     */
    private int scoreParameterMatch(Class<?> paramType, String argDesc) {
        if ("Z".equals(argDesc)) {
            if (paramType == boolean.class) return 30;
            if (paramType == Boolean.class) return 20;
            if (paramType == Object.class) return 5;
            return -1;
        }

        if ("I".equals(argDesc)) {
            if (paramType == int.class) return 30;
            if (paramType == long.class) return 25;
            if (paramType == double.class) return 20;
            if (paramType == Integer.class) return 15;
            if (Number.class.isAssignableFrom(paramType)) return 10;
            if (paramType == Object.class) return 5;
            return -1;
        }

        if ("J".equals(argDesc)) {
            if (paramType == long.class) return 30;
            if (paramType == double.class) return 20;
            if (paramType == Long.class) return 15;
            if (Number.class.isAssignableFrom(paramType)) return 10;
            if (paramType == Object.class) return 5;
            return -1;
        }

        if ("D".equals(argDesc)) {
            if (paramType == double.class) return 30;
            if (paramType == Double.class) return 15;
            if (Number.class.isAssignableFrom(paramType)) return 10;
            if (paramType == Object.class) return 5;
            return -1;
        }

        if ("Ljava/lang/String;".equals(argDesc)) {
            if (paramType == String.class) return 30;
            if (CharSequence.class.isAssignableFrom(paramType)) return 20;
            if (paramType == Object.class) return 5;
            return -1;
        }

        // 其他引用类型
        if (paramType == Object.class) return 5;
        return -1;
    }

    /**
     * 描述符匹配评分（用于方法缓存回退）
     */
    private int scoreDescriptorMatch(String paramDesc, String argDesc) {
        if ("Z".equals(argDesc)) {
            if ("Z".equals(paramDesc)) return 30;
            if ("Ljava/lang/Boolean;".equals(paramDesc)) return 15;
            if ("Ljava/lang/Object;".equals(paramDesc)) return 5;
            return -1;
        }

        if ("I".equals(argDesc)) {
            if ("I".equals(paramDesc)) return 30;
            if ("J".equals(paramDesc)) return 25;
            if ("D".equals(paramDesc)) return 20;
            if (paramDesc.startsWith("Ljava/lang/") || "Ljava/lang/Object;".equals(paramDesc)) return 5;
            return -1;
        }

        if ("J".equals(argDesc)) {
            if ("J".equals(paramDesc)) return 30;
            if ("D".equals(paramDesc)) return 20;
            if (paramDesc.startsWith("Ljava/lang/") || "Ljava/lang/Object;".equals(paramDesc)) return 5;
            return -1;
        }

        if ("D".equals(argDesc)) {
            if ("D".equals(paramDesc)) return 30;
            if (paramDesc.startsWith("Ljava/lang/") || "Ljava/lang/Object;".equals(paramDesc)) return 5;
            return -1;
        }

        if ("Ljava/lang/String;".equals(argDesc)) {
            if ("Ljava/lang/String;".equals(paramDesc)) return 30;
            if ("Ljava/lang/CharSequence;".equals(paramDesc)) return 20;
            if ("Ljava/lang/Object;".equals(paramDesc)) return 5;
            return -1;
        }

        // 其他引用类型
        if ("Ljava/lang/Object;".equals(paramDesc)) return 5;
        return -1;
    }

    /**
     * 构建 JVM 方法描述符
     */
    private String buildMethodDesc(Method m) {
        StringBuilder sb = new StringBuilder("(");
        for (Class<?> p : m.getParameterTypes()) {
            sb.append(javaTypeToDesc(p));
        }
        sb.append(")").append(javaTypeToDesc(m.getReturnType()));
        return sb.toString();
    }

    /**
     * 将 Java 类型转换为 JVM 描述符
     */
    private String javaTypeToDesc(Class<?> t) {
        if (t == void.class) return "V";
        if (t == int.class) return "I";
        if (t == boolean.class) return "Z";
        if (t == long.class) return "J";
        if (t == double.class) return "D";
        if (t == float.class) return "F";
        if (t == char.class) return "C";
        if (t == byte.class) return "B";
        if (t == short.class) return "S";
        if (t.isArray()) return t.getName().replace('.', '/');
        return "L" + t.getName().replace('.', '/') + ";";
    }
}
