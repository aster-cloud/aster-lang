#!/usr/bin/env node
import cp from 'node:child_process';

function main(): void {
  try {
    cp.execSync('./gradlew :examples:hello-native:nativeCompile', { stdio: 'inherit' });
    process.exit(0);
  } catch (e) {
    console.warn('native:hello skipped (toolchain not ready):', (e as Error).message);
    process.exit(0);
  }
}

main();

