# Syntax Reference

This section documents the CNL syntax recognized by the parser.

## Declarations
- `This module is app.service.`
- `Define User with id: Text and name: Text.`
- `Define AuthErr as one of InvalidCreds or Locked.`
- `To login with user: Text, produce Result of User or AuthErr.`

## Statements
- `Let x be 42.`
- `Set x to 3.`
- `Return "hello".`
- `If cond,: ...`
- `Match x: When ...`

## Expressions
- Literals: strings, ints, bools, null
- Names and dotted names: `AuthRepo.verify`
- Calls: `f(x, y)`
- Constructions: `User with id = 1 and name = "a"`
- Ok/Err/Some/None wrappers

## Types
- Primitive: `Text`, `Int`, `Float`, `Bool`
- `maybe T`
- `Option of T`
- `Result of T or E`
- `list of T`, `map K to V`

