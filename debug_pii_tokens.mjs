import { readFileSync } from 'fs';
import { lex } from './dist/src/index.js';

const file = 'cnl/examples/pii_type_basic.aster';
const source = readFileSync(file, 'utf8');

console.log('Source:', source);
console.log('\n=== All Tokens ===');

const tokens = lex(source, file);
tokens.forEach((tok, i) => {
  console.log(`${i.toString().padStart(3)}: ${tok.kind.padEnd(15)} ${JSON.stringify(tok.value || '').padEnd(20)}`);
});

console.log('\n=== Looking for @pii sequence ===');
for (let i = 0; i < tokens.length; i++) {
  if (tokens[i].kind === 'AT') {
    console.log(`Found @ at position ${i}`);
    for (let j = 0; j < 10 && i + j < tokens.length; j++) {
      console.log(`  ${i + j}: ${tokens[i + j].kind.padEnd(15)} ${JSON.stringify(tokens[i + j].value || '')}`);
    }
    break;
  }
}
