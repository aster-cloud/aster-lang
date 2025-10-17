package io.aster.vaadin.nativeimage;

import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.undertow.deployment.IgnoredServletContainerInitializerBuildItem;
import io.quarkus.undertow.deployment.WebMetadataBuildItem;
import jakarta.enterprise.inject.Vetoed;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.WebMetaData;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

/**
 * 在构建期将 Vaadin DevServer 包下的类型打上 @Vetoed，避免 Arc 进行装配，
 * 并阻断其在原生镜像分析阶段被触发。
 */
public class DevServerRemovalProcessor {

    private static final Predicate<DotName> IS_VAADIN_DEVSERVER = name -> {
        return isVaadinDevServer(name.toString());
    };

    private static boolean isVaadinDevServer(String name) {
        if (name == null) {
            return false;
        }
        return name.startsWith("com.vaadin.base.devserver.")
                || name.equals("com.vaadin.base.devserver")
                || name.startsWith("com.vaadin.base.devserver.startup.");
    }

    @BuildStep
    void vetoVaadinDevServer(BuildProducer<AnnotationsTransformerBuildItem> producer) {
        producer.produce(new AnnotationsTransformerBuildItem(
                AnnotationTransformation.builder()
                        .whenDeclaration(declaration -> declaration.kind() == AnnotationTarget.Kind.CLASS)
                        .transform(context -> {
                            ClassInfo clazz = context.declaration().asClass();
                            DotName name = clazz.name();
                            if (IS_VAADIN_DEVSERVER.test(name)) {
                                context.add(Vetoed.class);
                            }
                        })));
    }

    @BuildStep
    void ignoreDevServerScis(BuildProducer<IgnoredServletContainerInitializerBuildItem> ignoredScis) throws IOException {
        for (String initializer : ServiceUtil.classNamesNamedIn(
                Thread.currentThread().getContextClassLoader(),
                "META-INF/services/jakarta.servlet.ServletContainerInitializer")) {
            if (isVaadinDevServer(initializer)) {
                ignoredScis.produce(new IgnoredServletContainerInitializerBuildItem(initializer));
            }
        }
    }

    @BuildStep
    void pruneDevServerListeners(WebMetadataBuildItem webMetadataBuildItem) {
        WebMetaData metaData = webMetadataBuildItem.getWebMetaData();
        List<ListenerMetaData> listeners = metaData.getListeners();
        if (listeners != null) {
            listeners.removeIf(listener -> isVaadinDevServer(listener.getListenerClass()));
        }
    }
}
