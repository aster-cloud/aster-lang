package io.aster.validation.metadata;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 策略元数据加载器，负责动态加载策略类并缓存反射信息。
 */
public class PolicyMetadataLoader {

    private final ConcurrentHashMap<String, PolicyMetadata> metadataCache = new ConcurrentHashMap<>();

    /**
     * 根据策略限定名加载元数据信息，并进行缓存。
     *
     * @param qualifiedName 策略限定名（形如 module.function）
     * @return 策略元数据缓存对象
     */
    public PolicyMetadata loadPolicyMetadata(String qualifiedName) {
        return metadataCache.computeIfAbsent(qualifiedName, this::createMetadata);
    }

    /**
     * 清空所有已缓存的元数据信息。
     */
    public void clear() {
        metadataCache.clear();
    }

    private PolicyMetadata createMetadata(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == qualifiedName.length() - 1) {
            throw new IllegalArgumentException("非法策略标识: " + qualifiedName);
        }

        String policyModule = qualifiedName.substring(0, lastDot);
        String policyFunction = qualifiedName.substring(lastDot + 1);

        try {
            String className = policyModule + "." + policyFunction + "_fn";
            Class<?> policyClass = Class.forName(className);

            Method functionMethod = findPolicyMethod(policyClass, policyFunction);
            MethodHandle handle = MethodHandles.publicLookup().unreflect(functionMethod);

            return new PolicyMetadata(
                policyClass,
                functionMethod,
                handle,
                functionMethod.getParameters()
            );
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load policy metadata: " + qualifiedName, e);
        }
    }

    private Method findPolicyMethod(Class<?> policyClass, String functionName) {
        for (Method method : policyClass.getDeclaredMethods()) {
            if (method.getName().equals(functionName) &&
                java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                return method;
            }
        }
        throw new IllegalArgumentException("未找到策略方法: " + functionName);
    }
}
