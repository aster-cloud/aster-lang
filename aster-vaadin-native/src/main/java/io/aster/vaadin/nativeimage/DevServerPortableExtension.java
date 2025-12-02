package io.aster.vaadin.nativeimage;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;

/**
 * CDI Portable Extension：在 Bean 发现阶段剔除 Vaadin DevServer 相关类型，
 * 避免其进入 Arc 容器（生产模式与原生镜像不需要）。
 */
public class DevServerPortableExtension implements Extension {

    private static final String VAADIN_DEVSERVER_PKG = "com.vaadin.base.devserver";

    <T> void vetoDevServerBeans(@Observes ProcessAnnotatedType<T> pat) {
        String name = pat.getAnnotatedType().getJavaClass().getName();
        if (name.startsWith(VAADIN_DEVSERVER_PKG)) {
            pat.veto();
        }
    }
}

