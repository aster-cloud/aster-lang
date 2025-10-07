# Capabilities & Effects

This guide explains how to enforce declared effects against a capability manifest at typecheck-time and in the LSP.

## Effects in function headers

Use the header clause to declare effects:

```
To hello with name: Text, produce Text. It performs IO:
  Return Text.concat("Hello, ", name).
```

Supported effects:
- `IO`
- `CPU`

## Capability manifest

Enable checks by setting `ASTER_CAPS` to a JSON manifest file.

Environment examples:
- CLI: `ASTER_CAPS=cnl/examples/capabilities.json node dist/scripts/typecheck-cli.js cnl/examples/capdemo.cnl`
- LSP: `ASTER_CAPS=cnl/examples/capabilities.json npm run lsp`

Manifest schema (JSON):

```
{
  "allow": { "io": ["module.*", "module.func"], "cpu": ["*"] },
  "deny":  { "io": ["module.bad*"], "cpu": [] }
}
```

Rules:
- `deny` takes precedence over `allow` when specified.
- Patterns supported: `*`, `module`, `module.*`, `module.func`, and suffix wildcard `module.func*`.

Examples:
- Allow all IO in a module: `{ "allow": { "io": ["demo.capdemo.*"] } }`
- Allow all but a specific function (deny precedence):
  `{ "allow": { "io": ["demo.capdemo.*"] }, "deny": { "io": ["demo.capdemo.hello"] } }`

## Diagnostics

### Effect enforcement note

- Effect declarations are enforced at compile-time.
- Minimal lattice: ∅ ⊑ CPU ⊑ IO[*] (declaring @io satisfies CPU work).
- Superfluous @io when only CPU-like work is detected is reported as info; superfluous @cpu with no CPU-like work is a warning.


When a function declares an effect that is not allowed by the manifest, the typechecker emits an error like:

```
ERROR: Function 'hello' declares @io but capability manifest does not allow it for module 'demo.capdemo'.
```

These diagnostics also appear in the LSP if `ASTER_CAPS` is set.

## Quick Fixes (LSP)

When using the VS Code extension with the language server:
- Missing effect on a function header shows a quick-fix “Add It performs IO/CPU …”.
- Superfluous effect shows “Remove It performs IO/CPU …”.
- Capability violation offers manifest edits (when `ASTER_CAPS` is set):
  - Allow for the specific function FQN (e.g., `demo.capdemo.hello`).
  - Allow for the entire module (`demo.capdemo.*`).
  - If `module.*` is already present, “Narrow” to `module.func`.
  - If `module.func` is present, “Broaden” to `module.*`.

### Debugging code actions

You can use the smoke scripts to exercise the server and print available actions:
- `node dist/scripts/lsp-codeaction-smoke.js --debug`
- `node dist/scripts/lsp-capmanifest-codeaction-smoke.js --debug`

These will dump diagnostics and code action titles to the console to help debug.

## Golden tests

Two example goldens are included to validate behavior:
- `capabilities_deny.json` denies all IO/CPU: both IO functions in `capdemo.cnl` report errors.
- `capabilities_mixed.json` allows the module but denies a specific function: only that function reports an error.

Run: `npm run test:golden`

## Editor support

When launching the VS Code extension, set `ASTER_CAPS=...` in the environment so the language server reads the manifest and produces capability diagnostics.

> Screenshot placeholders:
> - Diagnostics panel showing capability errors for `hello`.
> - Hover or Problems list highlighting denied effect usage.
