#!/usr/bin/env node
import * as cp from 'node:child_process';
import * as fs from 'node:fs';
import * as path from 'node:path';

function shCapture(cmd: string): string {
  return cp.execSync(cmd, { stdio: ['ignore', 'pipe', 'pipe'] }).toString('utf8');
}

function main(): void {
  const classesDir = 'build/jvm-classes';
  if (!fs.existsSync(classesDir)) {
    console.error('classes not found');
    process.exit(2);
  }
  const files: string[] = [];
  function collect(d: string): void {
    for (const e of fs.readdirSync(d)) {
      const p = path.join(d, e);
      const st = fs.statSync(p);
      if (st.isDirectory()) collect(p);
      else if (p.endsWith('.class')) files.push(p);
    }
  }
  collect(classesDir);
  const problems: string[] = [];
  const outputs: string[] = [];
  for (const f of files) {
    const quoted = `'${f.replace(/'/g, "'\\''")}'`;
    const out = shCapture(`javap -v ${quoted}`);
    outputs.push(out);
    // Rule: flag unqualified object descriptors like LAction; (no package slash), excluding JDK/internal and our runtime/aster
    const badDesc = /L(?!java\/|javax\/|jdk\/|org\/|com\/|aster\/)[A-Za-z0-9_\\$]+;/g;
    let m: RegExpExecArray | null;
    while ((m = badDesc.exec(out)) !== null) {
      problems.push(`${f}: unqualified descriptor ${m[0]}`);
    }
  }
  if (problems.length > 0) {
    console.error('javap verification failed:');
    for (const p of problems) console.error(' - ' + p);
    process.exit(1);
  }
  console.log('javap verification completed: no unqualified descriptors found');
}

main();
