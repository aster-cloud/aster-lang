#!/usr/bin/env node
import fs from 'node:fs';
import { canonicalize } from '../src/canonicalizer.js';
import { lex } from '../src/lexer.js';
import { parse } from '../src/parser.js';

function runOneAst(inputPath: string, expectPath: string): void {
  try {
    const src = fs.readFileSync(inputPath, 'utf8');
    const can = canonicalize(src);
    const toks = lex(can);
    const ast = parse(toks);
    const actual = ast;
    const expected = JSON.parse(fs.readFileSync(expectPath, 'utf8'));
    if (JSON.stringify(actual) !== JSON.stringify(expected)) {
      console.error(`FAIL: AST ${inputPath}`);
      console.error('--- Actual ---');
      console.error(JSON.stringify(actual, null, 2));
      console.error('--- Expected ---');
      console.error(JSON.stringify(expected, null, 2));
      process.exitCode = 1;
    } else {
      console.log(`OK: AST ${inputPath}`);
    }
  } catch (e: unknown) {
    const err = e as { message?: string };
    console.error(`ERROR: AST ${inputPath}: ${err.message ?? String(e)}`);
    process.exitCode = 1;
  }
}

async function runOneCore(inputPath: string, expectPath: string): Promise<void> {
  try {
    const src = fs.readFileSync(inputPath, 'utf8');
    const can = canonicalize(src);
    const toks = lex(can);
    const ast = parse(toks);
    const { lowerModule } = await import('../src/lower_to_core.js');
    const core = lowerModule(ast);
    const actual = core;
    const expected = JSON.parse(fs.readFileSync(expectPath, 'utf8'));
    if (JSON.stringify(actual) !== JSON.stringify(expected)) {
      console.error(`FAIL: CORE ${inputPath}`);
      console.error('--- Actual ---');
      console.error(JSON.stringify(actual, null, 2));
      console.error('--- Expected ---');
      console.error(JSON.stringify(expected, null, 2));
      process.exitCode = 1;
    } else {
      console.log(`OK: CORE ${inputPath}`);
    }
  } catch (e: unknown) {
    const err = e as { message?: string };
    console.error(`ERROR: CORE ${inputPath}: ${err.message ?? String(e)}`);
    process.exitCode = 1;
  }
}

async function main(): Promise<void> {
  runOneAst('cnl/examples/greet.cnl', 'cnl/examples/expected_greet.ast.json');
  runOneAst('cnl/examples/login.cnl', 'cnl/examples/expected_login.ast.json');
  await runOneCore('cnl/examples/greet.cnl', 'cnl/examples/expected_greet_core.json');
  await runOneCore('cnl/examples/login.cnl', 'cnl/examples/expected_login_core.json');
  runOneAst('cnl/examples/fetch_dashboard.cnl', 'cnl/examples/expected_fetch_dashboard.ast.json');
  await runOneCore(
    'cnl/examples/fetch_dashboard.cnl',
    'cnl/examples/expected_fetch_dashboard_core.json'
  );
  runOneAst(
    'cnl/examples/enum_exhaustiveness.cnl',
    'cnl/examples/expected_enum_exhaustiveness.ast.json'
  );
  await runOneCore(
    'cnl/examples/enum_exhaustiveness.cnl',
    'cnl/examples/expected_enum_exhaustiveness_core.json'
  );
  runOneAst('cnl/examples/arith_compare.cnl', 'cnl/examples/expected_arith_compare.ast.json');
  await runOneCore(
    'cnl/examples/arith_compare.cnl',
    'cnl/examples/expected_arith_compare_core.json'
  );
}

main().catch(e => {
  console.error('Golden test runner failed:', e.message);
  process.exit(1);
});
