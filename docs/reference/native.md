# Native Image (GraalVM)

This project is designed to be reflection‑free by default:

- The ASM emitter generates final classes and static methods.
- Interop helpers (in `aster-runtime`) expose static methods only (no reflection or proxies).
- The Truffle/Java paths avoid dynamic proxies and invokedynamic.

## Requirements

- GraalVM JDK 21 with native‑image installed
- macOS: Xcode toolchain (agree to the license: `sudo xcodebuild -license`)

## Running the native sample (lenient CI mode)

The repository includes a small native sample in `examples/hello-native`.

- Local run (fails if toolchain missing):
  - `npm run native:hello`
- Lenient CI run (logs a warning if toolchain not ready):
  - `npm run native:hello:lenient`

Both commands first emit classes via the ASM emitter (reflection‑free bytecode), then compile a tiny driver to a native executable.

## Reflection‑free policy

- Do:
  - Generate plain classes and static methods.
  - Keep runtime helpers as small, static utilities.
- Don’t:
  - Introduce reflection (e.g., `Class.forName`, `Method.invoke`) in production code.
  - Use dynamic proxies or invokedynamic for critical paths.

If reflection becomes necessary for specific features, provide explicit `reflect-config.json` and template it in build scripts as needed. The current code base does not require any reflection configuration.

