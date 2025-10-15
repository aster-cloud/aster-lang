# Java 25 Compatibility Guide

## Native Access Warnings

When building Aster with Java 25 and Gradle 9.0.0, you may encounter warnings about restricted native access:

```
WARNING: A restricted method in java.lang.System has been called
WARNING: java.lang.System::load has been called by net.rubygrapefruit.platform.internal.NativeLibraryLoader
WARNING: Use --enable-native-access=ALL-UNNAMED to avoid a warning for callers in this module
WARNING: Restricted methods will be blocked in a future release unless native access is enabled
```

## Root Cause

These warnings originate from Gradle's native-platform library (`net.rubygrapefruit.platform`), which uses `System::load` to load native libraries. In Java 25, this is now a restricted method that requires explicit permission.

## Solutions

### Option 1: Use the Wrapper Script (Recommended)

The easiest way is to use the provided `gradlew-java25` wrapper script:

```bash
./gradlew-java25 build
./gradlew-java25 test
./gradlew-java25 clean
```

This script automatically sets `GRADLE_OPTS` to enable native access.

### Option 2: Set GRADLE_OPTS Manually

For individual commands:

```bash
GRADLE_OPTS="--enable-native-access=ALL-UNNAMED" ./gradlew build
```

Or export it for your shell session:

```bash
export GRADLE_OPTS="--enable-native-access=ALL-UNNAMED"
./gradlew build
./gradlew test
```

### Option 3: Source the Configuration File

```bash
source .gradle-jvmargs
./gradlew build
```

### Option 4: Add to Shell Profile (Permanent)

Add to `~/.bashrc`, `~/.zshrc`, or `~/.profile`:

```bash
export GRADLE_OPTS="--enable-native-access=ALL-UNNAMED"
```

Then restart your shell or run `source ~/.bashrc` (or appropriate file).

## IDE Configuration

### IntelliJ IDEA

1. Go to **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Gradle**
2. In **Gradle JVM**, ensure Java 25 is selected
3. Add to **VM options**: `--enable-native-access=ALL-UNNAMED`

Or configure in **Preferences** → **Build, Execution, Deployment** → **Gradle**:
- Set **Gradle JVM** to your Java 25 installation
- In **Gradle VM options**: `--enable-native-access=ALL-UNNAMED`

### VS Code

Add to `.vscode/settings.json`:

```json
{
  "java.jdt.ls.vmargs": "--enable-native-access=ALL-UNNAMED",
  "gradle.javaDebug.tasks": {
    "run": {
      "javaDebug": true,
      "jvmArgs": ["--enable-native-access=ALL-UNNAMED"]
    }
  }
}
```

## Why This Warning Exists

Java 25 introduced stricter controls over native code access as part of Project Panama and JEP 454 (Foreign Function & Memory API). The Java platform aims to:

1. **Improve Security**: Prevent unauthorized native code execution
2. **Enhance Safety**: Make native access explicit and auditable
3. **Prepare for Future**: Restricted methods will eventually be blocked entirely

## Impact on Aster Project

- **Build Time**: Warnings appear but do not affect build success
- **Runtime**: No impact - these warnings are only during Gradle's own startup
- **Future Compatibility**: Gradle will likely update their native-platform library to address this in future versions

## Gradle's Fix Timeline

Gradle is aware of this issue and working on a fix for native-platform:
- Issue tracked at: https://github.com/gradle/native-platform/issues
- Expected in Gradle 9.x or 10.0
- In the meantime, using `--enable-native-access=ALL-UNNAMED` is the recommended workaround

## gradle.properties Configuration

The project already includes configuration in `gradle.properties`:

```properties
# Java 25 native access: Allow Gradle's native-platform to load native libraries
# This suppresses warnings from Gradle 9.0.0's NativeLibraryLoader
# Note: Also affects the Gradle daemon and build processes
org.gradle.jvmargs=-XX:+UnlockExperimentalVMOptions --enable-native-access=ALL-UNNAMED
```

**Note**: This configuration affects the Gradle daemon JVM, but not the initial Gradle launcher process. For complete warning suppression, you still need to set `GRADLE_OPTS`.

## Verification

To verify the warnings are suppressed:

```bash
# Should show no warnings, only BUILD SUCCESSFUL
./gradlew-java25 help 2>&1 | grep -E "WARNING|BUILD"
```

Expected output:
```
BUILD SUCCESSFUL in Xs
```

## Troubleshooting

### Warnings Still Appear

If you still see warnings after setting GRADLE_OPTS:

1. **Check Environment**: Ensure GRADLE_OPTS is actually set
   ```bash
   echo $GRADLE_OPTS
   ```

2. **Restart Gradle Daemon**:
   ```bash
   ./gradlew --stop
   GRADLE_OPTS="--enable-native-access=ALL-UNNAMED" ./gradlew help
   ```

3. **Verify Java Version**:
   ```bash
   java -version
   ```
   Should show Java 25 or later.

### Build Failures

If the `--enable-native-access` flag causes build failures:

1. Ensure you're using Java 25+ (the flag is only available in recent versions)
2. Check for typos in the flag name
3. Try without `-XX:+UnlockExperimentalVMOptions` if you added it

## References

- [JEP 454: Foreign Function & Memory API](https://openjdk.org/jeps/454)
- [JEP 471: Deprecate the Memory-Access Methods in sun.misc.Unsafe](https://openjdk.org/jeps/471)
- [Gradle Native Platform](https://github.com/gradle/native-platform)
- [Java 25 Release Notes](https://openjdk.org/projects/jdk/25/)

## Future Migration

When Gradle releases an update that fully supports Java 25's native access model:

1. Update `gradle/wrapper/gradle-wrapper.properties` to the new version
2. Remove the `--enable-native-access` workaround from configurations
3. Test that builds complete without warnings
4. Update this documentation accordingly
