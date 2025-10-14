#!/usr/bin/env node
import fs from 'node:fs';
import { canonicalize } from '../src/canonicalizer.js';
import { lex } from '../src/lexer.js';
import { parse } from '../src/parser.js';
import { lowerModule } from '../src/lower_to_core.js';
import { typecheckModuleWithCapabilities } from '../src/typecheck.js';

function main(): void {
  const src = fs.readFileSync('cnl/examples/capdemo.aster', 'utf8');
  const manifest = JSON.parse(fs.readFileSync('cnl/examples/capabilities.json', 'utf8'));
  const ast = parse(lex(canonicalize(src)));
  const core = lowerModule(ast);
  const diags = typecheckModuleWithCapabilities(core, manifest);
  // Expect exactly one error: badIO should be allowed (module matches), but its body is invalid nullstrict-wise; we only assert capability errors count is zero
  const capErrors = diags.filter(d => d.severity === 'error' && d.message.includes('capability manifest')).length;
  if (capErrors !== 0) {
    console.error('Capability verification failed:', diags);
    process.exit(1);
  }
  console.log('Capability verification passed (no manifest violations)');
}

main();

