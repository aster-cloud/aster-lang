import { readFileSync } from 'fs';
import { lex, parse, canonicalize, lowerModule } from './dist/src/index.js';

const files = [
  'cnl/examples/nested_generic_function.cnl',
  'cnl/examples/nested_generic_lambda.cnl'
];

for (const file of files) {
  console.log(`\n=== Testing ${file} ===`);
  try {
    const source = readFileSync(file, 'utf8');
    console.log('Source:\n', source);

    console.log('Step 1: Lexing...');
    const tokens = lex(source, file);
    console.log(`✓ Lexed ${tokens.length} tokens`);

    console.log('Step 2: Parsing...');
    const astMod = parse(tokens, source, file);
    console.log('✓ Parse succeeded');

    console.log('Step 3: Canonicalizing...');
    const canonMod = canonicalize(astMod);
    console.log('✓ Canonicalize succeeded');

    console.log('Step 4: Lowering to Core...');
    const coreMod = lowerModule(canonMod);
    console.log('✓ Lower succeeded');

    console.log(`\n✅ ${file} - All steps succeeded!`);
  } catch (err) {
    console.error(`\n❌ ${file} - Failed`);
    console.error('Error:', err.message);
    if (err.stack) {
      console.error('Stack:', err.stack.split('\n').slice(0, 8).join('\n'));
    }
  }
}
