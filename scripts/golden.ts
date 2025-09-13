#!/usr/bin/env node
import fs from 'node:fs';
import { canonicalize, lex, parse } from '../src';

function runOneAst(inputPath: string, expectPath: string): void {
  try {
    const src = fs.readFileSync(inputPath, 'utf8');
    const can = canonicalize(src);
    const toks = lex(can);
    const ast = parse(toks);
    const actual = prune(ast);
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
    const actual = prune(core);
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

async function runOneTypecheck(inputPath: string, expectPath: string): Promise<void> {
  try {
    const src = fs.readFileSync(inputPath, 'utf8');
    const can = canonicalize(src);
    const toks = lex(can);
    const ast = parse(toks);
    const { lowerModule } = await import('../src/lower_to_core.js');
    const core = lowerModule(ast);
    const { typecheckModule } = await import('../src/typecheck.js');
    const diags = typecheckModule(core);
    const actualLines = Array.from(
      new Set(diags.map(d => `${d.severity.toUpperCase()}: ${d.message}`))
    );
    const expectedLines = Array.from(
      new Set(
        fs
          .readFileSync(expectPath, 'utf8')
          .split(/\r?\n/)
          .map(s => s.trim())
          .filter(s => s.length > 0)
      )
    );
    const actual = actualLines.join('\n') + (actualLines.length ? '\n' : '');
    const expected = expectedLines.join('\n') + (expectedLines.length ? '\n' : '');
    if (actual !== expected) {
      console.error(`FAIL: TYPECHECK ${inputPath}`);
      console.error('--- Actual ---');
      process.stdout.write(actual);
      console.error('--- Expected ---');
      process.stdout.write(expected);
      process.exitCode = 1;
    } else {
      console.log(`OK: TYPECHECK ${inputPath}`);
    }
  } catch (e: unknown) {
    const err = e as { message?: string };
    console.error(`ERROR: TYPECHECK ${inputPath}: ${err.message ?? String(e)}`);
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
  // Stdlib stubs
  runOneAst('cnl/examples/stdlib_text.cnl', 'cnl/examples/expected_stdlib_text.ast.json');
  await runOneCore('cnl/examples/stdlib_text.cnl', 'cnl/examples/expected_stdlib_text_core.json');
  runOneAst(
    'cnl/examples/stdlib_collections.cnl',
    'cnl/examples/expected_stdlib_collections.ast.json'
  );
  await runOneCore(
    'cnl/examples/stdlib_collections.cnl',
    'cnl/examples/expected_stdlib_collections_core.json'
  );
  runOneAst(
    'cnl/examples/stdlib_maybe_result.cnl',
    'cnl/examples/expected_stdlib_maybe_result.ast.json'
  );
  await runOneCore(
    'cnl/examples/stdlib_maybe_result.cnl',
    'cnl/examples/expected_stdlib_maybe_result_core.json'
  );
  runOneAst('cnl/examples/stdlib_io.cnl', 'cnl/examples/expected_stdlib_io.ast.json');
  await runOneCore('cnl/examples/stdlib_io.cnl', 'cnl/examples/expected_stdlib_io_core.json');
  // Text ops demo
  runOneAst('cnl/examples/text_ops.cnl', 'cnl/examples/expected_text_ops.ast.json');
  await runOneCore('cnl/examples/text_ops.cnl', 'cnl/examples/expected_text_ops_core.json');
  runOneAst('cnl/examples/list_ops.cnl', 'cnl/examples/expected_list_ops.ast.json');
  await runOneCore('cnl/examples/list_ops.cnl', 'cnl/examples/expected_list_ops_core.json');
  runOneAst('cnl/examples/if_param.cnl', 'cnl/examples/expected_if_param.ast.json');
  await runOneCore('cnl/examples/if_param.cnl', 'cnl/examples/expected_if_param_core.json');
  runOneAst('cnl/examples/map_ops.cnl', 'cnl/examples/expected_map_ops.ast.json');
  await runOneCore('cnl/examples/map_ops.cnl', 'cnl/examples/expected_map_ops_core.json');
  // Generics: function type parameters
  runOneAst('cnl/examples/id_generic.cnl', 'cnl/examples/expected_id_generic.ast.json');
  await runOneCore('cnl/examples/id_generic.cnl', 'cnl/examples/expected_id_generic_core.json');
  // Typecheck diagnostics
  await runOneTypecheck(
    'cnl/examples/bad_generic.cnl',
    'cnl/examples/expected_bad_generic.diag.txt'
  );
  // Match example
  runOneAst('cnl/examples/match_null.cnl', 'cnl/examples/expected_match_null.ast.json');
  await runOneCore('cnl/examples/match_null.cnl', 'cnl/examples/expected_match_null_core.json');
  runOneAst('cnl/examples/match_enum.cnl', 'cnl/examples/expected_match_enum.ast.json');
  await runOneCore('cnl/examples/match_enum.cnl', 'cnl/examples/expected_match_enum_core.json');
  // CNL lambda example
  runOneAst('cnl/examples/lambda_cnl.cnl', 'cnl/examples/expected_lambda_cnl.ast.json');
  await runOneCore('cnl/examples/lambda_cnl.cnl', 'cnl/examples/expected_lambda_cnl_core.json');
  // CNL short-form lambda example
  runOneAst('cnl/examples/lambda_cnl_short.cnl', 'cnl/examples/expected_lambda_cnl_short.ast.json');
  await runOneCore(
    'cnl/examples/lambda_cnl_short.cnl',
    'cnl/examples/expected_lambda_cnl_short_core.json'
  );
  // CNL short-form math + bool lambda examples
  runOneAst(
    'cnl/examples/lambda_cnl_math_bool.cnl',
    'cnl/examples/expected_lambda_cnl_math_bool.ast.json'
  );
  await runOneCore(
    'cnl/examples/lambda_cnl_math_bool.cnl',
    'cnl/examples/expected_lambda_cnl_math_bool_core.json'
  );
  // CNL mixed lambdas example (block + short form)
  runOneAst('cnl/examples/lambda_cnl_mixed.cnl', 'cnl/examples/expected_lambda_cnl_mixed.ast.json');
  await runOneCore(
    'cnl/examples/lambda_cnl_mixed.cnl',
    'cnl/examples/expected_lambda_cnl_mixed.core.json'
  );
  // CNL lambda example using Text.length
  runOneAst(
    'cnl/examples/lambda_cnl_length.cnl',
    'cnl/examples/expected_lambda_cnl_length.ast.json'
  );
  await runOneCore(
    'cnl/examples/lambda_cnl_length.cnl',
    'cnl/examples/expected_lambda_cnl_length_core.json'
  );
  // CNL lambda example using Text.length with comparison
  runOneAst(
    'cnl/examples/lambda_cnl_length_cmp.cnl',
    'cnl/examples/expected_lambda_cnl_length_cmp.ast.json'
  );
  await runOneCore(
    'cnl/examples/lambda_cnl_length_cmp.cnl',
    'cnl/examples/expected_lambda_cnl_length_cmp_core.json'
  );
  // Lambda block-form match with binding + if/else inside lambda
  runOneAst(
    'cnl/examples/lambda_cnl_match_bind.cnl',
    'cnl/examples/expected_lambda_cnl_match_bind.ast.json'
  );
  await runOneCore(
    'cnl/examples/lambda_cnl_match_bind.cnl',
    'cnl/examples/expected_lambda_cnl_match_bind_core.json'
  );
  // Lambda match on Result (Ok/Err) and binding
  runOneAst(
    'cnl/examples/lambda_cnl_match_result.cnl',
    'cnl/examples/expected_lambda_cnl_match_result.ast.json'
  );
  await runOneCore(
    'cnl/examples/lambda_cnl_match_result.cnl',
    'cnl/examples/expected_lambda_cnl_match_result_core.json'
  );
  // Lambda match on Maybe (null vs value)
  runOneAst(
    'cnl/examples/lambda_cnl_match_maybe.cnl',
    'cnl/examples/expected_lambda_cnl_match_maybe.ast.json'
  );
  await runOneCore(
    'cnl/examples/lambda_cnl_match_maybe.cnl',
    'cnl/examples/expected_lambda_cnl_match_maybe_core.json'
  );
  // Enum wildcard (catch-all)
  runOneAst('cnl/examples/enum_wildcard.cnl', 'cnl/examples/expected_enum_wildcard.ast.json');
  await runOneCore(
    'cnl/examples/enum_wildcard.cnl',
    'cnl/examples/expected_enum_wildcard_core.json'
  );
}

main().catch(e => {
  console.error('Golden test runner failed:', e.message);
  process.exit(1);
});

function prune(obj: unknown): unknown {
  if (Array.isArray(obj)) return obj.map(prune);
  if (obj && typeof obj === 'object') {
    const out: Record<string, unknown> = {};
    for (const [k, v] of Object.entries(obj as Record<string, unknown>)) {
      if (k === 'typeParams' && Array.isArray(v) && v.length === 0) continue;
      out[k] = prune(v as unknown);
    }
    return out;
  }
  return obj;
}
