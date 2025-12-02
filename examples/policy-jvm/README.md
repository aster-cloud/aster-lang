# Policy Engine Example

This example demonstrates a comprehensive Rules/Policy engine built with Aster language, showcasing:

- **Policy Engine**: Role-based access control with complex conditions
- **Rules Engine**: Business rule evaluation with multiple rule types
- **Context-aware Policies**: Time, location, and resource-based access control
- **Rule Composition**: Combining multiple rules for complex decision making

## Features

### Policy Engine (`policy_engine.aster`)
- **Role-based Access Control**: Admin, user roles with different permissions
- **Resource Types**: Document, User, System resources
- **Actions**: Read, Write, Delete, Admin operations
- **Context-aware Evaluation**: Time of day, location, resource ownership
- **Rule Types**: Time-based, location-based, and ownership-based rules

### Rules Engine (`rules_engine.aster`)
- **Business Rules**: Access, Validation, Transformation, Notification rules
- **Rule Priorities**: Low, Medium, High, Critical priority levels
- **Condition Evaluation**: Multiple operators (equals, contains, greater_than, etc.)
- **Rule Composition**: Evaluate multiple rules and return combined actions
- **Dynamic Rule Creation**: Programmatic rule generation

### Policy Demo (`policy_demo.aster`)
- **End-to-end Demonstrations**: Complete policy and rules engine demos
- **Test Suites**: Comprehensive test cases for both engines
- **Complex Scenarios**: Multi-factor authentication and authorization

## Usage

### Build and Run
```bash
# Build the project
./gradlew :examples:policy-jvm:build

# Run the policy engine
./gradlew :examples:policy-jvm:run --args="demo"

# Run the rules engine
./gradlew :examples:policy-jvm:run --args="rules"

# Basic policy check
./gradlew :examples:policy-jvm:run --args="basic user Read user123 user123"

# Complex policy check
./gradlew :examples:policy-jvm:run --args="complex user123 user user123 Document 14.5 office"
```

### Command Line Interface

The policy engine provides several commands:

1. **`basic <role> <action> <ownerId> <userId>`**
   - Simple policy check
   - Example: `basic user Read u123 u123`

2. **`demo`**
   - Run policy engine demonstrations
   - Shows various policy scenarios and test results

3. **`rules`**
   - Run rules engine demonstrations
   - Shows business rule evaluation and test results

4. **`complex <userId> <userRole> <resourceOwner> <resourceType> <timeOfDay> <location>`**
   - Complex policy evaluation with full context
   - Example: `complex user123 user user123 Document 14.5 office`

## Examples

### Policy Engine Examples

```cnl
// Basic access control
To canAccess with role: Text and action: Action and ownerId: Text and userId: Text, produce Bool:
  If Text.equals(role, "admin"),:
    Return true.
  Match action:
    When Read,
      Return Text.equals(ownerId, userId).
    When Write,
      Return Text.equals(ownerId, userId).
    When Delete,
      Return false.

// Time-based access control
To createTimeBasedRule with role: Text and action: Action and resource: Resource, produce PolicyRule:
  Define condition as fn with context: PolicyContext, produce Bool:
    Return Number.lessThan(context.timeOfDay, 18.0).
  Return PolicyRule(role, action, resource, condition).
```

### Rules Engine Examples

```cnl
// Business rule evaluation
To evaluateRule with rule: BusinessRule and context: RuleContext, produce Bool:
  If not rule.enabled,:
    Return false.
  Define evaluateConditions as fn with remainingConditions: List of RuleCondition, produce Bool:
    Match remainingConditions:
      When [],
        Return true.
      When [condition, ...rest],
        If not evaluateCondition(condition, context),:
          Return false.
        Return evaluateConditions(rest).
  Return evaluateConditions(rule.conditions).

// Rule composition
To evaluateRules with rules: List of BusinessRule and context: RuleContext, produce List of Text:
  Define evaluateAll as fn with remainingRules: List of BusinessRule and results: List of Text, produce List of Text:
    Match remainingRules:
      When [],
        Return results.
      When [rule, ...rest],
        If evaluateRule(rule, context),:
          Return evaluateAll(rest, [rule.action, ...results]).
        Return evaluateAll(rest, results).
  Return evaluateAll(rules, []).
```

## Architecture

### Policy Engine Architecture
- **PolicyRule**: Defines role, action, resource, and condition
- **PolicyContext**: Contains user and resource information
- **Rule Evaluation**: Matches rules against context and evaluates conditions
- **Policy Composition**: Combines multiple rules for complex decisions

### Rules Engine Architecture
- **BusinessRule**: Defines rule metadata and conditions
- **RuleContext**: Contains evaluation context
- **Condition Evaluation**: Supports multiple operators and field access
- **Rule Composition**: Evaluates multiple rules and returns combined actions

## Testing

The example includes comprehensive test suites:

- **Policy Tests**: Various user roles, actions, and contexts
- **Rules Tests**: Different rule types and conditions
- **Integration Tests**: End-to-end policy and rules evaluation

Run tests with:
```bash
./gradlew :examples:policy-jvm:test
```

## Extending the Engine

### Adding New Rule Types
1. Define new rule types in the enum
2. Create rule creation functions
3. Add evaluation logic
4. Update test cases

### Adding New Conditions
1. Add new operators to `evaluateCondition`
2. Implement field access in `getFieldValue`
3. Add test cases for new conditions

### Adding New Actions
1. Extend the Action enum
2. Update policy evaluation logic
3. Add corresponding test cases

This example demonstrates the power and flexibility of the Aster language for building complex, domain-specific systems like policy and rules engines.
