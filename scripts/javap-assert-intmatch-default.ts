#!/usr/bin/env node
import cp from 'node:child_process';
import fs from 'node:fs';

function shCapture(cmd: string): string {
  return cp.execSync(cmd, { stdio: ['ignore', 'pipe', 'pipe'] }).toString('utf8');
}

function findClass(): string {
  const root = 'build/jvm-classes';
  const prefer = root + '/demo/int_match_default/classifyDefault_fn.class';
  if (fs.existsSync(prefer)) return prefer;
  const stack: string[] = [root];
  while (stack.length) {
    const d = stack.pop()!;
    for (const ent of fs.readdirSync(d)) {
      const p = d + '/' + ent;
      const st = fs.statSync(p);
      if (st.isDirectory()) stack.push(p);
      else if (st.isFile() && p.endsWith('classifyDefault_fn.class')) return p;
    }
  }
  throw new Error('classifyDefault_fn.class not found under ' + root);
}

function main(): void {
  const cls = findClass();
  const out = shCapture(`javap -v '${cls}'`);
  if (!(out.includes('tableswitch') || out.includes('lookupswitch'))) {
    console.error('Assertion failed: expected tableswitch/lookupswitch in classifyDefault()');
    process.exit(1);
  }
  console.log('javap assertion passed: int match (with default) lowered to switch');
}

main();

