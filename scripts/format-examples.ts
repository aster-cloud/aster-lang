#!/usr/bin/env node
import * as fs from 'node:fs';
import * as path from 'node:path';
import { formatCNL } from '../src/formatter.js';

const mode = process.argv.includes('--write') ? 'write' : process.argv.includes('--check') ? 'check' : 'check';

function run(): void {
  const dir = path.join(process.cwd(), 'cnl', 'examples');
  const files = fs.readdirSync(dir).filter(f => f.endsWith('.aster')).map(f => path.join(dir, f));
  let changed = 0;
  let ok = 0;
  for (const file of files) {
    const src = fs.readFileSync(file, 'utf8');
    const out = formatCNL(src);
    if (out !== src) {
      if (mode === 'write') {
        fs.writeFileSync(file, out, 'utf8');
        changed++;
        continue;
      } else {
        console.log(`[diff] ${path.relative(process.cwd(), file)} would be reformatted`);
      }
    } else {
      ok++;
    }
  }
  if (mode === 'write') {
    console.log(`Formatted ${changed} files; ${ok} already formatted.`);
  } else {
    if (changed > 0) {
      console.error(`\n${changed} file(s) would be reformatted. Run with --write to apply.`);
      process.exit(2);
    }
    console.log(`All ${ok} example files are properly formatted.`);
  }
}

run();
