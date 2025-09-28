#!/usr/bin/env node
import cp from 'node:child_process';
import fs from 'node:fs';

function shCapture(cmd: string): string {
  return cp.execSync(cmd, { stdio: ['ignore', 'pipe', 'pipe'] }).toString('utf8');
}

function findThirdFn(): string {
  const preferred = 'build/jvm-classes/demo/list/third_fn.class';
  if (fs.existsSync(preferred)) return preferred;
  const root = 'build/jvm-classes';
  const stack: string[] = [root];
  while (stack.length) {
    const d = stack.pop()!;
    for (const ent of fs.readdirSync(d)) {
      const p = d + '/' + ent;
      const st = fs.statSync(p);
      if (st.isDirectory()) stack.push(p);
      else if (st.isFile() && p.endsWith('third_fn.class')) return p;
    }
  }
  throw new Error('third_fn.class not found under build/jvm-classes');
}

function main(): void {
  const cls = findThirdFn();
  const out = shCapture(`javap -v '${cls}'`);
  // Expect List.get to use (I)Ljava/lang/Object;
  if (!out.includes('InterfaceMethod java/util/List.get:(I)Ljava/lang/Object;')) {
    console.error('Assertion failed: expected List.get:(I)Ljava/lang/Object; in bytecode');
    process.exit(1);
  }
  console.log('javap assertion passed: List.get uses int overload');
}

main();
