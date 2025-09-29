#!/usr/bin/env node
import cp from 'node:child_process';
import fs from 'node:fs';

function shCapture(cmd: string): string {
  return cp.execSync(cmd, { stdio: ['ignore', 'pipe', 'pipe'] }).toString('utf8');
}

function findBySuffix(suffix: string): string {
  const root = 'build/jvm-classes';
  const stack: string[] = [root];
  while (stack.length) {
    const d = stack.pop()!;
    for (const ent of fs.readdirSync(d)) {
      const p = d + '/' + ent;
      const st = fs.statSync(p);
      if (st.isDirectory()) stack.push(p);
      else if (st.isFile() && p.endsWith(suffix)) return p;
    }
  }
  throw new Error('Class not found: *' + suffix);
}

function assertContainsBySuffix(suffix: string, needle: string): void {
  const cls = findBySuffix(suffix);
  const out = shCapture(`javap -v '${cls}'`);
  if (!out.includes(needle)) {
    console.error(`Assertion failed in ${cls}: missing '${needle}'`);
    process.exit(1);
  }
}

function main(): void {
  assertContainsBySuffix('sumInt_fn.class', 'Interop.sum:(II)Ljava/lang/String;');
  assertContainsBySuffix('sumLong_fn.class', 'Interop.sum:(JJ)Ljava/lang/String;');
  assertContainsBySuffix('sumDouble_fn.class', 'Interop.sum:(DD)Ljava/lang/String;');
  assertContainsBySuffix('sumIntLong_fn.class', 'Interop.sum:(JJ)Ljava/lang/String;');
  assertContainsBySuffix('sumIntDouble_fn.class', 'Interop.sum:(DD)Ljava/lang/String;');
  console.log('javap sum assertion passed');
}

main();
