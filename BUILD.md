# Build Quick Reference

## Java 25 Compatibility

This project uses Java 25 and requires special configuration to avoid warnings from Gradle's native platform.

### Quick Start (Recommended)

Use the provided wrapper script:

```bash
./gradlew-java25 build
./gradlew-java25 test
./gradlew-java25 clean
```

This automatically sets the required JVM flags to suppress Java 25 native access warnings.

### Alternative: Manual Setup

If you prefer to use `./gradlew` directly:

```bash
# One-time per shell session
export GRADLE_OPTS="--enable-native-access=ALL-UNNAMED"

# Or source the config file
source .gradle-jvmargs

# Then use gradlew normally
./gradlew build
```

### Permanent Setup

Add to your shell profile (`~/.bashrc`, `~/.zshrc`, etc.):

```bash
export GRADLE_OPTS="--enable-native-access=ALL-UNNAMED"
```

## Common Commands

```bash
# Build all modules
./gradlew-java25 build

# Run tests
./gradlew-java25 test
./gradlew-java25 :aster-asm-emitter:test
./gradlew-java25 :quarkus-policy-api:test

# Clean build
./gradlew-java25 clean build

# Run specific test
./gradlew-java25 :quarkus-policy-api:test --tests PolicyGraphQLResourceTest

# Generate Aster class files
npm run emit:class cnl/stdlib/finance/loan.aster
```

## IDE Setup

### IntelliJ IDEA

1. **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Gradle**
2. Set **Gradle JVM** to Java 25
3. Add to **Gradle VM options**: `--enable-native-access=ALL-UNNAMED`

### VS Code

Add to `.vscode/settings.json`:

```json
{
  "java.jdt.ls.vmargs": "--enable-native-access=ALL-UNNAMED"
}
```

## Troubleshooting

### Still Seeing Warnings?

```bash
# Stop Gradle daemon
./gradlew --stop

# Verify environment
echo $GRADLE_OPTS

# Should output: --enable-native-access=ALL-UNNAMED

# If empty, export it:
export GRADLE_OPTS="--enable-native-access=ALL-UNNAMED"
```

### Build Failures?

```bash
# Verify Java version
java -version
# Should show Java 25

# Clean and rebuild
./gradlew-java25 clean build --no-daemon
```

## Documentation

- **Java 25 Compatibility Guide**: [docs/java25-compatibility.md](docs/java25-compatibility.md)
- **Type Inference Documentation**: [aster-asm-emitter/docs/type-inference.md](aster-asm-emitter/docs/type-inference.md)
- **Resolution Report**: [.claude/java25-warnings-resolution.md](.claude/java25-warnings-resolution.md)

## CI/CD

When setting up CI pipelines, ensure `GRADLE_OPTS` is set:

```yaml
env:
  GRADLE_OPTS: "--enable-native-access=ALL-UNNAMED"
```

## Questions?

See the full compatibility guide at [docs/java25-compatibility.md](docs/java25-compatibility.md) or ask the team.
