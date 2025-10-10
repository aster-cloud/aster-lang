package io.aster.policy.api;

import java.util.Arrays;
import java.util.Objects;

/**
 * Cache key for policy evaluation results
 *
 * 该类用于生成缓存键，基于策略模块、函数名和输入参数。
 * 实现了equals和hashCode以确保缓存正确工作。
 */
public class PolicyCacheKey {
    private final String policyModule;
    private final String policyFunction;
    private final Object[] context;
    private final int hashCode;

    public PolicyCacheKey(String policyModule, String policyFunction, Object[] context) {
        this.policyModule = policyModule;
        this.policyFunction = policyFunction;
        this.context = context;
        // 预计算哈希码以提高性能
        this.hashCode = computeHashCode();
    }

    private int computeHashCode() {
        int result = Objects.hash(policyModule, policyFunction);
        result = 31 * result + Arrays.deepHashCode(context);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PolicyCacheKey that = (PolicyCacheKey) o;
        return Objects.equals(policyModule, that.policyModule) &&
               Objects.equals(policyFunction, that.policyFunction) &&
               Arrays.deepEquals(context, that.context);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "PolicyCacheKey{" +
                "policyModule='" + policyModule + '\'' +
                ", policyFunction='" + policyFunction + '\'' +
                ", contextSize=" + (context != null ? context.length : 0) +
                '}';
    }

    public String getPolicyModule() {
        return policyModule;
    }

    public String getPolicyFunction() {
        return policyFunction;
    }

    public Object[] getContext() {
        return context;
    }
}
