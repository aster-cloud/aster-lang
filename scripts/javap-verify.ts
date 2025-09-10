#!/usr/bin/env node
import * as cp from 'node:child_process';
import * as fs from 'node:fs';
import * as path from 'node:path';

function sh(cmd: string) {
  console.log(`$ ${cmd}`);
  return cp.execSync(cmd, { stdio: 'inherit' });
}

function main() {
  const classesDir = 'build/jvm-classes';
  if (!fs.existsSync(classesDir)) { console.error('classes not found'); process.exit(2); }
  const files: string[] = [];
  function collect(d: string) {
    for (const e of fs.readdirSync(d)) {
      const p = path.join(d, e);
      const st = fs.statSync(p);
      if (st.isDirectory()) collect(p);
      else if (p.endsWith('.class')) files.push(p);
    }
  }
  collect(classesDir);
  for (const f of files) {
    sh(`javap -v ${f}`);
  }
  console.log('javap verification completed');
}

main();

