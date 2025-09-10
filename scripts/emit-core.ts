#!/usr/bin/env node
import fs from 'node:fs';
import { canonicalize } from '../src/canonicalizer.js';
import { lex } from '../src/lexer.js';
import { parse } from '../src/parser.js';
import { lowerModule } from '../src/lower_to_core.js';
import { DiagnosticError, formatDiagnostic } from '../src/diagnostics.js';

function main(): void {
  const file = process.argv[2];
  if (!file) {
    console.error('Usage: emit-core <file.cnl>');
    process.exit(2);
  }
  const input = fs.readFileSync(file, 'utf8');
  try {
    const can = canonicalize(input);
    const toks = lex(can);
    const ast = parse(toks);
    const core = lowerModule(ast);
    console.log(JSON.stringify(core, null, 2));
  } catch (e: unknown) {
    if (e instanceof DiagnosticError) {
      console.error(formatDiagnostic(e.diagnostic, input));
    } else if (typeof e === 'object' && e && 'message' in e) {
      const err = e as { message?: string; pos?: { line: number; col: number } };
      const pos = err.pos ? `:${err.pos.line}:${err.pos.col}` : '';
      console.error(`Error${pos}: ${err.message ?? 'unknown error'}`);
    } else {
      console.error('Unknown error');
    }
    process.exit(1);
  }
}

main();
