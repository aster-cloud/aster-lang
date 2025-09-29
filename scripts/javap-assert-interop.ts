#!/usr/bin/env node
import cp from 'node:child_process';
import fs from 'node:fs';

function shCapture(cmd: string): string {
  return cp.execSync(cmd, { stdio: ['ignore', 'pipe', 'pipe'] }).toString('utf8');
}

function assertContains(cls: string, needle: string): void {
  if (!fs.existsSync(cls)) throw new Error('Class not found: ' + cls);
  const out = shCapture(`javap -v '${cls}'`);
  if (!out.includes(needle)) {
    console.error(`Assertion failed in ${cls}: missing '${needle}'`);
    process.exit(1);
  }
}

function main(): void {
  // Allow either CP comment or instruction comment; match on signature fragment
  assertContains('build/jvm-classes/demo/interop/pickInt_fn.class', 'Interop.pick:(I)Ljava/lang/String;');
  assertContains('build/jvm-classes/demo/interop/pickObj_fn.class', 'Interop.pick:(Ljava/lang/Object;)Ljava/lang/String;');
  assertContains('build/jvm-classes/demo/interop/pickBool_fn.class', 'Interop.pick:(Z)Ljava/lang/String;');
  assertContains('build/jvm-classes/demo/interop/pickStr_fn.class', 'Interop.pick:(Ljava/lang/String;)Ljava/lang/String;');
  assertContains('build/jvm-classes/demo/interop/pickLong_fn.class', 'Interop.pick:(J)Ljava/lang/String;');
  assertContains('build/jvm-classes/demo/interop/pickDouble_fn.class', 'Interop.pick:(D)Ljava/lang/String;');
  console.log('javap interop assertion passed');
}

main();
