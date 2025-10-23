import { readFileSync } from 'fs';
import { lex } from './dist/src/index.js';

const file = 'test/cnl/examples/nested_generic_lambda.aster';
const source = readFileSync(file, 'utf8');
console.log('Source:\n', source);
console.log('\n=== Tokens ===');

const tokens = lex(source, file);
tokens.forEach((tok, i) => {
  console.log(`${i.toString().padStart(3)}: ${tok.kind.padEnd(15)} ${JSON.stringify(tok.value).padEnd(20)} @${tok.start.line}:${tok.start.column}`);
});
