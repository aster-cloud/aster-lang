#!/usr/bin/env node
import * as cp from 'node:child_process';
import * as fs from 'node:fs';
import * as path from 'node:path';

function sh(cmd: string, options?: cp.ExecSyncOptions): void {
  cp.execSync(cmd, { stdio: 'inherit', ...options });
}

function tryExec(cmd: string, options?: cp.ExecSyncOptions): boolean {
  try {
    cp.execSync(cmd, { stdio: 'inherit', ...options });
    return true;
  } catch {
    return false;
  }
}

function main(): void {
  const classes = 'build/jvm-classes';
  const runtimeJar = 'aster-runtime/build/libs/aster-runtime.jar';
  const outBase = process.env.ASTER_OUT_DIR && process.env.ASTER_OUT_DIR.trim().length > 0
    ? process.env.ASTER_OUT_DIR
    : 'build/aster-out';
  const outJar = path.join(outBase, 'aster.jar');
  const tempDir = path.join(outBase, 'temp-merge');

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
  fs.mkdirSync(outBase, { recursive: true });
  fs.rmSync(tempDir, { recursive: true, force: true });
  fs.mkdirSync(tempDir, { recursive: true });

  // Extract runtime JAR to temp directory
  console.log('Extracting runtime classes from', runtimeJar);
  const absRuntime = path.join(process.cwd(), runtimeJar);
  // 优先使用 jar 工具；若失败（部分 JDK 版本下 jartool 在受限环境会异常），回退到 unzip
  if (!tryExec(`jar --extract --file ${absRuntime}`, { cwd: tempDir })) {
    console.warn('[jar-jvm] jar 工具解压失败，回退到 unzip');
    // -o 覆盖，-q 安静模式（保持输出简洁）
    sh(`unzip -o ${absRuntime}`, { cwd: tempDir });
  }

  // Copy policy classes to temp directory
  console.log('Copying policy classes from', classes);
  sh(`cp -r ${path.join(process.cwd(), classes)}/* ${tempDir}/`);

  // Copy package-map.json to temp directory
  const packageMapSource = 'aster-asm-emitter/build/aster-out/package-map.json';
  const packageMapDest = path.join(tempDir, 'aster-asm-emitter/build/aster-out');
  if (fs.existsSync(packageMapSource)) {
    console.log('Copying package-map.json from', packageMapSource);
    fs.mkdirSync(packageMapDest, { recursive: true });
    fs.copyFileSync(packageMapSource, path.join(packageMapDest, 'package-map.json'));
  } else {
    console.warn('Warning: package-map.json not found at', packageMapSource);
  }

  // Create merged JAR
  console.log('Creating merged JAR:', outJar);
  const absOut = path.join(process.cwd(), outJar);
  if (!tryExec(`jar --create --file ${absOut} -C ${tempDir} .`)) {
    console.warn('[jar-jvm] jar 工具打包失败，回退到 zip');
    // 使用 zip 创建 JAR（ZIP）文件
    const prevCwd = process.cwd();
    try {
      process.chdir(tempDir);
      sh(`zip -r ${absOut} .`);
    } finally {
      process.chdir(prevCwd);
    }
  }

  // Clean up temp directory
  fs.rmSync(tempDir, { recursive: true, force: true });

  console.log('Wrote', outJar);
}

main();
