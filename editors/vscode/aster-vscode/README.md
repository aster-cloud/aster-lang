# Aster VS Code Extension (Preview)

This extension wraps the Aster Language Server included in this repo and wires it to VS Code.

Prerequisites:

- Build the repo so the LSP server exists at `dist/src/lsp/server.js`:
  - `npm run build`

Install the extension for local development:

- Open this repository in VS Code.
- Run the `Extensions: Install from VSIX...` command after packaging, or use `F5` to start an Extension Host for this folder.
- Alternatively, run `vsce package` inside `editors/vscode/aster-vscode` to create a `.vsix` (requires `npm i -g @vscode/vsce`).

Configuration:

- `aster.langServer.path` (default: `dist/src/lsp/server.js`): override the server entry, relative to workspace root.

## Capabilities Manifest

To enable capability checks in diagnostics, set the `ASTER_CAPS` environment variable to a JSON manifest before launching VS Code or the language server.

Example shell launch:

```
ASTER_CAPS=cnl/examples/capabilities.json code .
```

Manifest format:
- `allow`: per-capability allow-list (e.g., `{ "io": ["demo.capdemo.*"], "cpu": ["*"] }`)
- Optional `deny`: entries that take precedence over allow (e.g., `{ "io": ["demo.capdemo.bad*"] }`)

Patterns: `*`, `module`, `module.*`, `module.func`, and suffix wildcard `module.func*`.

Notes:

- This is a minimal client using `stdio` transport. Features depend on server capabilities as they evolve.
- Language id is `aster` and files with `.cnl` extension are recognized.
