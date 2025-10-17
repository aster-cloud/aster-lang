package io.aster.policy.api.validation;

import io.aster.validation.metadata.PolicyMetadataLoader;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * CDI 包装类，暴露 {@link PolicyMetadataLoader} 供 Quarkus 注入使用。
 */
@ApplicationScoped
public class QuarkusPolicyMetadataLoader extends PolicyMetadataLoader {
}
