#!/usr/bin/env node
import cp from 'node:child_process';

function sh(cmd: string): void {
  cp.execSync(cmd, { stdio: 'inherit', env: { ...process.env, ASTER_ROOT: process.cwd() } });
}

function main(): void {
  // Ensure some classes emitted to produce package-map.json for module packages
  try {
    sh('node dist/scripts/emit-classfiles.js cnl/examples/greet.cnl');
  } catch {
    // non-fatal; examples run shouldn't depend on this
  }
  // Compile+run JVM examples against package map
  const targets = [
    ':examples:login-jvm:build',
    ':examples:text-jvm:build',
    ':examples:list-jvm:build',
    ':examples:map-jvm:build',
  ];
  try {
    sh(`./gradlew ${targets.join(' ')}`);
  } catch (e) {
    console.error('Examples build failed:', (e as Error).message);
    process.exit(1);
  }
  console.log('Examples compile against package map: OK');
}

main();

