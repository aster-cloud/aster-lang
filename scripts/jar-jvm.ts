#!/usr/bin/env node
import * as cp from 'node:child_process';
import * as fs from 'node:fs';
import * as path from 'node:path';

function sh(cmd: string, options?: cp.ExecSyncOptions): void {
  cp.execSync(cmd, { stdio: 'inherit', ...options });
}

function main(): void {
  const classes = 'build/jvm-classes';
  const runtimeJar = 'aster-runtime/build/libs/aster-runtime.jar';
  const outJar = 'build/aster-out/aster.jar';
  const tempDir = 'build/aster-out/temp-merge';

  if (!fs.existsSync(classes)) {
    console.error('No classes found:', classes);
    process.exit(2);
  }

  if (!fs.existsSync(runtimeJar)) {
    console.error('Runtime JAR not found:', runtimeJar);
    console.error('Please build aster-runtime first: ./gradlew :aster-runtime:jar');
    process.exit(2);
  }

  // Create output and temp directories
  fs.mkdirSync('build/aster-out', { recursive: true });
  fs.rmSync(tempDir, { recursive: true, force: true });
  fs.mkdirSync(tempDir, { recursive: true });

  // Extract runtime JAR to temp directory
  console.log('Extracting runtime classes from', runtimeJar);
  sh(`jar --extract --file ${path.join(process.cwd(), runtimeJar)}`, { cwd: tempDir });

  // Copy policy classes to temp directory
  console.log('Copying policy classes from', classes);
  sh(`cp -r ${path.join(process.cwd(), classes)}/* ${tempDir}/`);

  // Create merged JAR
  console.log('Creating merged JAR:', outJar);
  sh(`jar --create --file ${path.join(process.cwd(), outJar)} -C ${tempDir} .`);

  // Clean up temp directory
  fs.rmSync(tempDir, { recursive: true, force: true });

  console.log('Wrote', outJar);
}

main();
