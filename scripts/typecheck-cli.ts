#!/usr/bin/env node
import fs from 'node:fs';
import { canonicalize } from '../src/canonicalizer.js';
import { lex } from '../src/lexer.js';
import { parse } from '../src/parser.js';
import { lowerModule } from '../src/lower_to_core.js';
import { typecheckModule } from '../src/typecheck.js';

function main(): void {
  const file = process.argv[2];
  if (!file) {
    console.error('Usage: typecheck <file.cnl>');
    process.exit(2);
  }
  const input = fs.readFileSync(file, 'utf8');
  const can = canonicalize(input);
  const toks = lex(can);
  const ast = parse(toks);
  const core = lowerModule(ast);
  const diags = typecheckModule(core);
  if (diags.length === 0) {
    console.log('Typecheck OK');
  } else {
    for (const d of diags) {
      const tag = d.severity === 'error' ? 'ERROR' : 'WARN';
      console.log(`${tag}: ${d.message}`);
    }
  }
}

main();

