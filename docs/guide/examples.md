# Examples

## CLI Tool (end-to-end)

- Build and run:
  - `npm run cli:run`
- Usage:
  - `cli greet Alice` → prints `Hello, Alice`
  - `cli exclaim wow` → prints `wow!`
  - `cli length hello` → prints `5`

## REST Service (end-to-end)

- Build and run:
  - `npm run rest:run`
- Endpoints:
  - `GET http://localhost:8080/greet?name=Alice` → `Hello, Alice\n`
  - `GET http://localhost:8080/exclaim?text=wow` → `wow!\n`
  - `GET http://localhost:8080/length?text=hello` → `5\n`

## Greet

```text
Define a User with id: Text and name: Text.

To greet user: maybe User, produce Text:
  Match user:
    When null, Return "Hi, guest".
    When User(id, name), Return "Welcome, {name}".
```

## Login

```text
Define a User with id: Text and name: Text.
Define an AuthErr as one of InvalidCreds or Locked.

To login with user: Text and pass: Text, produce Result of User or AuthErr. It performs IO:
  Let ok be AuthRepo.verify(user, pass).
  If not ok,:
    Return Err of InvalidCreds.
  Return Ok of User with id = UUID.randomUUID() and name = user.
```


## Math and Match (IR-driven)

- Arithmetic/Compare (core IR): emits add(a,b) and cmp(a,b)
  - Generate: `./gradlew :aster-asm-emitter:run --args=build/jvm-classes < cnl/examples/arith_compare_core.json`
  - Inspect: `javap -classpath build/jvm-classes -v app.math.add_fn` and `app.math.cmp_fn`

- Match (two-case, core IR): data Color, pick(Color?) returning Text
  - Generate: `./gradlew :aster-asm-emitter:run --args=build/jvm-classes < cnl/examples/match_two_core.json`
  - Inspect: `javap -classpath build/jvm-classes -v app.match.pick_fn`

## Native Login Example

- Build and run a native binary that calls the generated login function
  - `npm run login:native`
  - Binary: `examples/login-native/build/native/nativeCompile/login-aster`

## Truffle Runner

- Minimal AST runner for now (If + Return + Literal)
  - `./gradlew :truffle:run`
