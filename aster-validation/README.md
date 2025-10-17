# aster-validation
> 更新时间：2025-10-17 09:27 NZDT（Codex）

`aster-validation` 模块提供 Aster 策略运行时的独立验证能力，负责在无 CDI 环境下执行 Schema 校验与语义约束校验。模块以纯 Java 21 编写，依赖最小化，可直接复用于 CLI、Serverless 等运行场景。

## 模块内容
- **构造器元数据**：`io.aster.validation.metadata` 包含 `ConstructorMetadataCache` 与 `PolicyMetadataLoader`，用于缓存领域对象构造器及策略方法元信息。
- **Schema 校验**：`io.aster.validation.schema.SchemaValidator` 接收 `ConstructorMetadataCache`，通过构造器参数映射校验请求字段。
- **语义校验**：`io.aster.validation.semantic.SemanticValidator` 搭配 `@Range`、`@NotEmpty`、`@Pattern` 注解在实例上执行约束验证。
- **异常模型**：`SchemaValidationException` 与 `SemanticValidationException` 提供详细错误描述，便于上层展示。

## 使用示例
```java
import io.aster.validation.metadata.ConstructorMetadataCache;
import io.aster.validation.schema.SchemaValidator;
import io.aster.validation.semantic.SemanticValidator;

ConstructorMetadataCache cache = new ConstructorMetadataCache();
SchemaValidator schemaValidator = new SchemaValidator(cache);
SemanticValidator semanticValidator = new SemanticValidator(cache);

schemaValidator.validateSchema(TargetClass.class, payload);
TargetClass instance = /* 使用 ConstructorMetadata 构造 */;
semanticValidator.validateSemantics(instance);
```

## 与 Quarkus 集成
- `quarkus-policy-api` 通过 `QuarkusValidationAdapter` 将模块暴露为 CDI Bean。
- 旧的 `io.aster.policy.api.validation.*` 类已移除，请统一使用 `io.aster.validation.*` 包名。

## 测试与构建
- 运行独立验证：`./gradlew :aster-validation:build`
- 仅执行模块测试：`./gradlew :aster-validation:test`

项目默认使用 SLF4J 作为日志门面，测试依赖 JUnit 5 + AssertJ + Mockito。根据需要可在上层应用注入自定义日志实现或替换元数据缓存策略。
