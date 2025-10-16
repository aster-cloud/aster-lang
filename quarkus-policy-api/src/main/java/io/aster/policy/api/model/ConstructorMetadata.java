package io.aster.policy.api.model;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

/**
 * 构造器元数据，缓存构造方法及参数字段映射。
 */
public class ConstructorMetadata {

    private final Constructor<?> constructor;
    private final Parameter[] parameters;
    private final Field[] fields;

    public ConstructorMetadata(Constructor<?> constructor,
                               Parameter[] parameters,
                               Field[] fields) {
        this.constructor = constructor;
        this.parameters = parameters;
        this.fields = fields;
    }

    public Constructor<?> getConstructor() {
        return constructor;
    }

    public Parameter[] getParameters() {
        return parameters;
    }

    public Field[] getFields() {
        return fields;
    }
}
