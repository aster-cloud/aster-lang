import { readFileSync } from 'fs';
import { lex, parse } from './dist/src/index.js';

const file = 'cnl/examples/nested_generic_lambda.aster';
const source = readFileSync(file, 'utf8');
console.log('Source:\n', source);

try {
  const tokens = lex(source, file);
  console.log('\n✓ Lexed', tokens.length, 'tokens');

  // 显示前 35 个 tokens
  console.log('\nFirst 35 tokens:');
  for (let i = 0; i < Math.min(35, tokens.length); i++) {
    console.log(`${i.toString().padStart(2)}: ${tokens[i].kind.padEnd(15)} ${JSON.stringify(tokens[i].value || '').padEnd(20)}`);
  }

  console.log('\n解析中...');
  const astMod = parse(tokens, source, file);
  console.log('✓ Parse succeeded');
} catch (err) {
  console.error('\n✗ Parse failed:', err.message);
  // 尝试从错误栈中提取位置信息
  if (err.stack) {
    console.error(err.stack.split('\n').slice(0, 10).join('\n'));
  }
}
