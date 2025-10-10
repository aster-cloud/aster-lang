# Quarkus Policy API

A high-performance reactive REST API for evaluating Aster policies. Built with Quarkus 3.17.5 and Mutiny for reactive programming.

## Features

- **Single Policy Evaluation**: Evaluate individual policies with caching support
- **Policy Composition**: Execute multiple policies in sequence with data flow between steps
- **Batch Evaluation**: Parallel execution of multiple policies with two modes:
  - **Fail-fast mode**: Stops on first failure
  - **Partial results mode**: Collects both successes and failures
- **Policy Validation**: Check if a policy exists and retrieve its signature
- **Reactive Architecture**: Built on Mutiny for non-blocking, asynchronous operations
- **Caching**: Caffeine-based caching for improved performance
- **OpenAPI/Swagger UI**: Interactive API documentation

## Quick Start

### Running the Application

```bash
# Development mode with live reload
./gradlew :quarkus-policy-api:quarkusDev

# Build and run in JVM mode
./gradlew :quarkus-policy-api:build
java -jar quarkus-policy-api/build/quarkus-app/quarkus-run.jar

# Build native executable (requires GraalVM)
./gradlew :quarkus-policy-api:build -Dquarkus.package.type=native
```

The API will be available at: `http://localhost:8080`

### Accessing API Documentation

- **Swagger UI**: http://localhost:8080/q/swagger-ui
- **OpenAPI Spec**: http://localhost:8080/q/openapi

## API Endpoints

### 1. Evaluate Single Policy

**POST** `/api/policies/evaluate`

Evaluates a specific policy against provided context data.

**Request Body:**
```json
{
  "policyModule": "aster.finance.loan",
  "policyFunction": "evaluateLoanEligibility",
  "context": [750, 50000.0, 3.5]
}
```

**Response:**
```json
{
  "result": true,
  "executionTimeMs": 12.5,
  "policyModule": "aster.finance.loan",
  "policyFunction": "evaluateLoanEligibility",
  "timestamp": 1704067200000,
  "fromCache": false,
  "policyExecutionTimeMs": 10.2
}
```

**Status Codes:**
- `200`: Success
- `400`: Invalid request
- `404`: Policy not found
- `500`: Policy evaluation failed

---

### 2. Policy Composition

**POST** `/api/policies/evaluate/composition`

Executes multiple policies in sequence, optionally using the result of one as input to the next.

**Request Body:**
```json
{
  "steps": [
    {
      "policyModule": "aster.finance.risk",
      "policyFunction": "assessRisk",
      "useResultAsInput": false
    },
    {
      "policyModule": "aster.finance.fraud",
      "policyFunction": "detectFraud",
      "useResultAsInput": false
    }
  ],
  "initialContext": [750, 50000.0, 5]
}
```

**Response:**
```json
{
  "finalResult": false,
  "stepResults": [
    {
      "policyModule": "aster.finance.risk",
      "policyFunction": "assessRisk",
      "result": "HIGH",
      "executionTimeMs": 5.2,
      "stepIndex": 0
    },
    {
      "policyModule": "aster.finance.fraud",
      "policyFunction": "detectFraud",
      "result": false,
      "executionTimeMs": 3.8,
      "stepIndex": 1
    }
  ],
  "totalExecutionTimeMs": 12.5,
  "timestamp": 1704067200000
}
```

**Status Codes:**
- `200`: Success
- `400`: Invalid composition request

---

### 3. Validate Policy

**POST** `/api/policies/validate`

Checks if a policy exists, can be loaded, and returns its signature information.

**Request Body:**
```json
{
  "policyModule": "aster.finance.loan",
  "policyFunction": "evaluateLoanEligibility"
}
```

**Response:**
```json
{
  "valid": true,
  "message": "Policy exists and is valid",
  "policyModule": "aster.finance.loan",
  "policyFunction": "evaluateLoanEligibility",
  "parameters": [
    {
      "name": "creditScore",
      "type": "int",
      "fullTypeName": "int"
    },
    {
      "name": "loanAmount",
      "type": "double",
      "fullTypeName": "double"
    },
    {
      "name": "employmentYears",
      "type": "double",
      "fullTypeName": "double"
    }
  ],
  "returnType": "boolean",
  "returnTypeFullName": "boolean"
}
```

**Status Codes:**
- `200`: Validation completed (check `valid` field)
- `400`: Invalid validation request

---

### 4. Batch Evaluation (Fail-Fast)

**POST** `/api/policies/evaluate/batch`

Evaluates multiple policies in parallel. If any fails, the entire batch fails.

**Request Body:**
```json
{
  "requests": [
    {
      "policyModule": "aster.finance.loan",
      "policyFunction": "evaluateLoanEligibility",
      "context": [750, 50000.0, 3.5]
    },
    {
      "policyModule": "aster.finance.fraud",
      "policyFunction": "detectFraud",
      "context": [10000.0, 2, 1.5]
    }
  ]
}
```

**Response:**
```json
{
  "results": [
    {
      "result": true,
      "executionTimeMs": 5.2,
      "policyModule": "aster.finance.loan",
      "policyFunction": "evaluateLoanEligibility",
      "timestamp": 1704067200000,
      "fromCache": false,
      "policyExecutionTimeMs": 4.8
    },
    {
      "result": false,
      "executionTimeMs": 3.8,
      "policyModule": "aster.finance.fraud",
      "policyFunction": "detectFraud",
      "timestamp": 1704067200000,
      "fromCache": false,
      "policyExecutionTimeMs": 3.5
    }
  ],
  "count": 2,
  "totalExecutionTimeMs": 15.3,
  "timestamp": 1704067200000
}
```

**Status Codes:**
- `200`: All policies evaluated successfully
- `400`: Invalid batch request
- `500`: One or more policies failed

---

### 5. Batch Evaluation with Partial Results

**POST** `/api/policies/evaluate/batch/partial`

Evaluates multiple policies in parallel. Collects both successful and failed results.

**Request Body:**
```json
{
  "requests": [
    {
      "policyModule": "aster.finance.loan",
      "policyFunction": "evaluateLoanEligibility",
      "context": [750, 50000.0, 3.5]
    },
    {
      "policyModule": "aster.finance.fraud",
      "policyFunction": "detectFraud",
      "context": [10000.0, 2, 1.5]
    },
    {
      "policyModule": "aster.finance.risk",
      "policyFunction": "assessRisk",
      "context": [650, 40000.0, 15000.0]
    }
  ]
}
```

**Response:**
```json
{
  "successes": [
    {
      "index": 0,
      "policyModule": "aster.finance.loan",
      "policyFunction": "evaluateLoanEligibility",
      "result": {
        "result": true,
        "executionTimeMs": 5.2,
        "fromCache": false
      },
      "error": null
    },
    {
      "index": 1,
      "policyModule": "aster.finance.fraud",
      "policyFunction": "detectFraud",
      "result": {
        "result": false,
        "executionTimeMs": 3.8,
        "fromCache": false
      },
      "error": null
    }
  ],
  "failures": [
    {
      "index": 2,
      "policyModule": "aster.finance.risk",
      "policyFunction": "assessRisk",
      "result": null,
      "error": "Policy execution failed: Invalid parameters"
    }
  ],
  "successCount": 2,
  "failureCount": 1,
  "totalCount": 3,
  "totalExecutionTimeMs": 15.3,
  "timestamp": 1704067200000
}
```

**Status Codes:**
- `200`: Batch evaluation completed (may contain failures)
- `400`: Invalid batch request

---

### 6. List Available Policies

**GET** `/api/policies/list`

Returns a list of all available compiled policies.

**Response:**
```json
{
  "policies": [
    {
      "module": "aster.finance.loan",
      "function": "evaluateLoanEligibility",
      "description": "Evaluate loan application eligibility"
    },
    {
      "module": "aster.finance.loan",
      "function": "determineInterestRateBps",
      "description": "Determine interest rate based on credit score"
    },
    {
      "module": "aster.finance.fraud",
      "function": "detectFraud",
      "description": "Detect fraudulent transactions"
    },
    {
      "module": "aster.finance.risk",
      "function": "assessRisk",
      "description": "Assess financial risk"
    }
  ],
  "count": 4,
  "timestamp": 1704067200000
}
```

**Status Code:** `200`

---

### 7. Clear Policy Cache

**DELETE** `/api/policies/cache`

Clears all cached policy evaluation results.

**Response:**
```json
{
  "status": "success",
  "message": "Policy cache cleared",
  "timestamp": 1704067200000
}
```

**Status Code:** `200`

---

### 8. Invalidate Specific Cache Entry

**DELETE** `/api/policies/cache/invalidate`

Invalidates cache for a specific policy and context.

**Request Body:**
```json
{
  "policyModule": "aster.finance.loan",
  "policyFunction": "evaluateLoanEligibility",
  "context": [750, 50000.0, 3.5]
}
```

**Response:**
```json
{
  "status": "success",
  "message": "Cache invalidated for aster.finance.loan.evaluateLoanEligibility",
  "timestamp": 1704067200000
}
```

**Status Codes:**
- `200`: Success
- `400`: Invalid request

---

### 9. Health Check

**GET** `/api/policies/health`

Check if policy evaluation service is healthy.

**Response:**
```json
{
  "status": "UP",
  "service": "aster-policy-api",
  "timestamp": 1704067200000
}
```

**Status Code:** `200`

---

## Architecture

### Technology Stack

- **Quarkus 3.17.5**: Kubernetes-native Java framework
- **Mutiny**: Reactive programming library for asynchronous operations
- **Caffeine**: High-performance caching library
- **SmallRye OpenAPI**: API documentation generation
- **RestAssured**: Integration testing

### Key Components

1. **PolicyEvaluationResource**: REST API layer handling HTTP requests
2. **PolicyEvaluationService**: Business logic layer with caching and policy invocation
3. **Reactive Pipeline**: Uses `Uni<T>` for non-blocking operations
4. **MethodHandle**: Fast reflection for policy invocation
5. **Caffeine Cache**: Automatic expiration and size-based eviction

### Performance Features

- **Asynchronous Execution**: All operations return `Uni<T>` for non-blocking processing
- **Parallel Batch Processing**: Concurrent policy evaluation with `Uni.join().all()`
- **Caching**: Configurable TTL (5 minutes) and size (1000 entries) based caching
- **Worker Thread Pool**: Policy execution offloaded to worker threads

### Caching Strategy

```java
@CacheResult(cacheName = "policy-evaluations")
public Uni<EvaluationResult> evaluatePolicy(String module, String function, Object[] context)
```

- **Cache Key**: `module.function + context hash`
- **TTL**: 5 minutes
- **Max Size**: 1000 entries
- **Eviction**: LRU (Least Recently Used)

---

## Configuration

### Application Properties

```properties
# Quarkus application settings
quarkus.application.name=aster-policy-api
quarkus.http.port=8080

# OpenAPI/Swagger UI
quarkus.swagger-ui.always-include=true
quarkus.swagger-ui.path=/q/swagger-ui

# Caffeine cache settings
quarkus.cache.caffeine."policy-evaluations".expire-after-write=5M
quarkus.cache.caffeine."policy-evaluations".maximum-size=1000
```

### Environment Variables

- `ASTER_POLICY_JAR`: Path to Aster compiled policies JAR (default: `build/aster-out/aster.jar`)

---

## Testing

### Run Tests

```bash
# Run all tests
./gradlew :quarkus-policy-api:test

# Run specific test class
./gradlew :quarkus-policy-api:test --tests PolicyEvaluationResourceTest

# Run with coverage
./gradlew :quarkus-policy-api:test jacocoTestReport
```

### Test Coverage

The test suite includes:
- ✅ Single policy evaluation (success/failure cases)
- ✅ Policy composition (valid/invalid requests)
- ✅ Policy validation (existing/non-existent policies)
- ✅ Batch evaluation fail-fast mode
- ✅ Batch evaluation partial results mode
- ✅ Cache operations
- ✅ Error handling and validation

---

## Performance Benchmarking

Performance tests are available in `PolicyEvaluationPerformanceTest.java`:

```bash
# Run performance tests
./gradlew :quarkus-policy-api:test --tests PolicyEvaluationPerformanceTest
```

**Typical Performance Metrics:**
- Single policy evaluation: ~10-15ms (cached: ~1-2ms)
- Batch evaluation (10 policies): ~50-80ms
- Policy composition (3 steps): ~30-50ms

---

## Error Handling

### Common Error Responses

**400 Bad Request:**
```json
{
  "error": "Missing required fields: policyModule, policyFunction"
}
```

**404 Not Found:**
```json
{
  "error": "Policy not found: nonexistent.module.testFunction"
}
```

**500 Internal Server Error:**
```json
{
  "error": "Policy evaluation failed",
  "details": "java.lang.IllegalArgumentException: Invalid parameters"
}
```

---

## Best Practices

### 1. Policy Composition

When using policy composition, consider:
- Set `useResultAsInput: true` only when the next policy expects the previous result as input
- Keep composition chains short (3-5 steps maximum) for better performance
- Handle different result types appropriately

### 2. Batch Evaluation

Choose the right batch mode:
- **Fail-fast**: Use when all policies must succeed (e.g., validation pipeline)
- **Partial results**: Use when you need to know which policies failed (e.g., bulk validation)

### 3. Caching

- Cache is automatic for single policy evaluations
- Batch and composition results are not cached
- Clear cache after policy redeployment: `DELETE /api/policies/cache`

### 4. Performance Optimization

- Use batch endpoints for multiple evaluations instead of sequential single calls
- Monitor cache hit rate via metrics
- Adjust cache TTL based on policy update frequency

---

## Monitoring and Observability

### Health Endpoints

- **Health Check**: `GET /q/health`
- **Liveness**: `GET /q/health/live`
- **Readiness**: `GET /q/health/ready`

### Metrics

Quarkus exposes metrics at `/q/metrics` including:
- HTTP request counts and latencies
- Cache hit/miss rates
- Worker thread pool utilization

---

## Development

### Project Structure

```
quarkus-policy-api/
├── src/
│   ├── main/
│   │   ├── java/io/aster/policy/api/
│   │   │   ├── PolicyEvaluationResource.java    # REST endpoints
│   │   │   └── PolicyEvaluationService.java     # Business logic
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/io/aster/policy/api/
│           ├── PolicyEvaluationResourceTest.java
│           └── PolicyEvaluationPerformanceTest.java
└── build.gradle.kts
```

### Adding New Endpoints

1. Add method to `PolicyEvaluationService.java` with business logic
2. Add REST endpoint to `PolicyEvaluationResource.java`
3. Add OpenAPI annotations for documentation
4. Add integration tests to `PolicyEvaluationResourceTest.java`

---

## License

This project is part of the Aster Language ecosystem.
