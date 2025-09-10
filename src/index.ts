// Main API exports for Aster CNL

export { canonicalize } from './canonicalizer.js';
export { lex } from './lexer.js';
export { parse } from './parser.js';
export { lowerModule } from './lower_to_core.js';
export { Core, Effect } from './core_ir.js';
export { TokenKind, KW } from './tokens.js';
export { Node } from './ast.js';

// Re-export types
export type * from './types.js';
