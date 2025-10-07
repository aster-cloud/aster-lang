import { lex, parse } from './dist/src/index.js';

// 测试简单的泛型函数
const testCases = [
  {
    name: 'Simple generic',
    code: `This module is test.
To identity with x: T, produce T:
  Return x.
`
  },
  {
    name: 'Generic with Maybe',
    code: `This module is test.
To wrap with x: T, produce Maybe of T:
  Return Some of x.
`
  }
];

for (const { name, code } of testCases) {
  console.log(`\n=== ${name} ===`);
  try {
    const tokens = lex(code, 'test.cnl');
    const astMod = parse(tokens, code, 'test.cnl');
    console.log('✓ Succeeded');
  } catch (err) {
    console.error('✗ Failed:', err.message);
  }
}
