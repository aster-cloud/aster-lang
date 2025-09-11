#!/usr/bin/env node
import * as cp from 'node:child_process';
import * as fs from 'node:fs';
import * as path from 'node:path';

function sh(cmd: string, opts: cp.ExecSyncOptions = {}): void {
  console.log(`$ ${cmd}`);
  cp.execSync(cmd, { stdio: 'inherit', ...opts });
}

function main(): void {
  const srcDir = 'build/jvm-src';
  const outDir = 'build/jvm-classes';
  const rtSrc = 'aster-runtime/src/main/java';
  if (!fs.existsSync(srcDir)) { console.error(`${srcDir} not found; run emit:jvm first`); process.exit(2); }
  fs.rmSync(outDir, { recursive: true, force: true });
  fs.mkdirSync(outDir, { recursive: true });
  // Compile runtime and emitted sources
  const srcs: string[] = [];
  function collect(d: string): void {
    for (const e of fs.readdirSync(d)) {
      const p = path.join(d, e);
      const st = fs.statSync(p);
      if (st.isDirectory()) collect(p);
      else if (p.endsWith('.java')) srcs.push(p);
    }
  }
  collect(rtSrc);
  collect(srcDir);
  sh(`javac -source 17 -target 17 -g -d ${outDir} ${srcs.join(' ')}`);
  console.log(`Compiled classes into ${outDir}`);
}

main();
