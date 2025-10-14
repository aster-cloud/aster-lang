#!/usr/bin/env node
import fs from 'node:fs';
import { canonicalize } from '../src/canonicalizer.js';
import { lex } from '../src/lexer.js';
import { parse } from '../src/parser.js';
import { lowerModule } from '../src/lower_to_core.js';
import { emitJava } from '../src/jvm/emitter.js';

async function main(): Promise<void> {
  const inputPath = process.argv[2];
  if (!inputPath) {
    console.error('Usage: emit-jvm <file.aster>');
    process.exit(2);
  }
  const src = fs.readFileSync(inputPath, 'utf8');
  // Clean previous Java output to avoid stale sources causing javac failures
  fs.rmSync('build/jvm-src', { recursive: true, force: true });
  const core = lowerModule(parse(lex(canonicalize(src))));
  await emitJava(core);
  console.log('Wrote Java sources to build/jvm-src');
}

main().catch(e => {
  console.error('emit-jvm failed:', e);
  process.exit(1);
});
