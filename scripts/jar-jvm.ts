#!/usr/bin/env node
import * as cp from 'node:child_process';
import * as fs from 'node:fs';

function sh(cmd: string) { cp.execSync(cmd, { stdio: 'inherit' }); }

function main() {
  const classes = 'build/jvm-classes';
  const outJar = 'build/aster-out/aster.jar';
  if (!fs.existsSync(classes)) { console.error('No classes found:', classes); process.exit(2);} 
  fs.mkdirSync('build/aster-out', { recursive: true });
  sh(`jar --create --file ${outJar} -C ${classes} .`);
  console.log('Wrote', outJar);
}

main();

