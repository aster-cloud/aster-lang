import { readFileSync } from 'fs';
import { lex, parse, canonicalize, lowerModule } from './dist/src/index.js';

const file = 'test/cnl/examples/pii_type_basic.aster';
const source = readFileSync(file, 'utf8');

console.log('Source:', source);

try {
  const tokens = lex(source, file);
  console.log('✓ Lexed', tokens.length, 'tokens');

  const astMod = parse(tokens, source, file);
  console.log('✓ Parse succeeded');
  console.log('\nAST Module:', JSON.stringify(astMod, null, 2).substring(0, 500));

  // 检查 AST 中是否包含 TypePii
  if (!astMod.decls || astMod.decls.length === 0) {
    console.error('✗ No declarations found in AST');
    process.exit(1);
  }
  const funcDecl = astMod.decls[0];
  console.log('\nFunction:', funcDecl.name);
  console.log('Return type:', JSON.stringify(funcDecl.retType, null, 2));

  if (funcDecl.retType.kind === 'TypePii') {
    console.log('\n✅ TypePii 节点正确生成');
    console.log('  sensitivity:', funcDecl.retType.sensitivity);
    console.log('  category:', funcDecl.retType.category);
    console.log('  baseType:', funcDecl.retType.baseType.kind, funcDecl.retType.baseType.name);
  } else {
    console.error('✗ Expected TypePii but got:', funcDecl.retType.kind);
    process.exit(1);
  }

  const canonMod = canonicalize(astMod);
  console.log('\n✓ Canonicalize succeeded');

  const coreMod = lowerModule(canonMod);
  console.log('✓ Lower succeeded');

  console.log('\n✅ 所有步骤成功！');
} catch (err) {
  console.error('\n✗ Error:', err.message);
  console.error(err.stack);
  process.exit(1);
}
