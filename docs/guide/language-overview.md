# Language Overview

Aster’s CNL syntax aims to be readable and precise, compiling to a small Core IR.

- Non-null by default
- Maybe vs Option distinction
- Pattern matching
- Effects (IO, CPU)

```text
Define a User with id: Text and name: Text.

To greet with user: maybe User, produce Text:
  Match user:
    When null, Return "Hi, guest".
    When User(id, name), Return "Welcome, {name}".
```

