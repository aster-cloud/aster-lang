import { readFileSync } from 'fs';
import { canonicalize, lex, lowerModule } from './dist/src/index.js';
import { parse as parseAst } from './dist/src/parser.js';
import { typecheckModule } from './dist/src/typecheck.js';

const file = process.argv[2];
const src = readFileSync(file, 'utf-8');
const can = canonicalize(src);
const toks = lex(can);
const ast = parseAst(toks);
const core = lowerModule(ast);
const diags = typecheckModule(core);
const errors = diags.filter(d => d.severity === 'error');
if (errors.length > 0) {
  console.log(`Found ${errors.length} type errors:`);
  errors.forEach(e => console.log(`  - ${e.message}`));
  process.exit(1);
} else {
  console.log('No type errors!');
}
