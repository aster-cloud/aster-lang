---
layout: home
home: true
hero:
  name: Aster Language
  text: A pragmatic, safe, fast language with a human CNL surface
  tagline: Human-readable inputs, strict core semantics
  actions:
    - theme: brand
      text: Get Started
      link: /guide/getting-started
    - theme: alt
      text: GitHub
      link: https://github.com/wontlost-ltd/aster-lang
---

Aster is a programming language designed with a human-friendly Controlled Natural Language (CNL) that compiles to a strict, safe core IR, targeting the JVM (with GraalVM/Truffle planned).

- [![Latest Release](https://img.shields.io/github/v/release/wontlost-ltd/aster-lang?display_name=tag)](https://github.com/wontlost-ltd/aster-lang/releases)
- [![GitHub Stars](https://img.shields.io/github/stars/wontlost-ltd/aster-lang?style=social)](https://github.com/wontlost-ltd/aster-lang)

- Human-feeling syntax with deterministic semantics
- Non-null by default, Maybe/Option distinction
- First-class pattern matching and algebraic data types
- Effect system (IO/CPU) from day one

```text
To greet user: maybe User, produce Text:
  Match user:
    When null, Return "Hi, guest".
    When User(id, name), Return "Welcome, {name}".
```

```bash
# Try it
npm run build
node dist/scripts/cli.js cnl/examples/greet.cnl
node dist/scripts/emit-core.js cnl/examples/greet.cnl
```
