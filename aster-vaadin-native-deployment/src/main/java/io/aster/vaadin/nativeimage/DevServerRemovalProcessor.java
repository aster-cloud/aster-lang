package io.aster.vaadin.nativeimage;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.undertow.deployment.UndertowDeploymentInfoCustomizerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import java.util.function.Predicate;

/**
 * 在构建期将 Vaadin DevServer 包下的类型打上 @Vetoed，避免 Arc 进行装配，
 * 并阻断其在原生镜像分析阶段被触发。
 */
public class DevServerRemovalProcessor {

    private static final DotName VETOED = DotName.createSimple("jakarta.enterprise.inject.Vetoed");

    private static final Predicate<DotName> IS_VAADIN_DEVSERVER = name -> {
        String n = name.toString();
        return n.startsWith("com.vaadin.base.devserver.") || n.equals("com.vaadin.base.devserver")
                || n.startsWith("com.vaadin.base.devserver.startup.");
    };

    @BuildStep
    void vetoVaadinDevServer(BuildProducer<AnnotationsTransformerBuildItem> producer) {
        producer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == AnnotationTarget.Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext context) {
                ClassInfo clazz = context.getTarget().asClass();
                DotName name = clazz.name();
                if (IS_VAADIN_DEVSERVER.test(name)) {
                    context.transform().add(VETOED).done();
                }
            }
        }));
    }

    /**
     * Undertow 层补强：移除 DevServer 相关的 ServletContextListener/SCI，避免容器阶段触发开发时逻辑。
     */
    @BuildStep
    UndertowDeploymentInfoCustomizerBuildItem blockDevServerUndertow() {
        return new UndertowDeploymentInfoCustomizerBuildItem(info -> {
            try {
                // 移除 DevServer 相关 Listener
                info.getListeners().removeIf(listener -> {
                    try {
                        String cn = listener.getListenerClass();
                        return cn != null && cn.startsWith("com.vaadin.base.devserver");
                    } catch (Throwable t) {
                        return false;
                    }
                });

                // 谨慎处理 SCI：仅移除 devserver 包下的 initializer（不移除 Vaadin 正常的 Flow 初始器）
                info.getServletContainerInitializers().removeIf(sci -> {
                    try {
                        String cn = sci.getValue().getClassName();
                        return cn != null && cn.startsWith("com.vaadin.base.devserver");
                    } catch (Throwable t) {
                        return false;
                    }
                });
            } catch (Throwable ignored) {
            }
        });
    }
}
