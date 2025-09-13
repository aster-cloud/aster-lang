#!/usr/bin/env node
import * as fc from 'fast-check';
import { canonicalize, lex, parse, lowerModule, TokenKind } from '../src';

// Property: Canonicalizer should be idempotent
const testCanonicalizerIdempotent = (): void => {
  fc.assert(
    fc.property(
      fc.string({ minLength: 0, maxLength: 100 }),
      (input: string) => {
        const first = canonicalize(input);
        const second = canonicalize(first);
        return first === second;
      }
    ),
    { numRuns: 100 }
  );
  console.log('✓ Canonicalizer is idempotent');
};

// Property: Lexer should handle empty input
const testLexerEmptyInput = (): void => {
  const tokens = lex('');
  if (tokens.length !== 1 || tokens[0]?.kind !== TokenKind.EOF) {
    throw new Error('Lexer should produce exactly one EOF token for empty input');
  }
  console.log('✓ Lexer handles empty input correctly');
};

// Property: Lexer should always end with EOF
const testLexerAlwaysEOF = (): void => {
  fc.assert(
    fc.property(
      fc.string({ minLength: 0, maxLength: 50 }),
      (input: string) => {
        try {
          const tokens = lex(input);
          return tokens.length > 0 && tokens[tokens.length - 1]?.kind === TokenKind.EOF;
        } catch {
          return true; // Lexer errors are acceptable
        }
      }
    ),
    { numRuns: 100 }
  );
  console.log('✓ Lexer always ends with EOF token');
};

// Property: Valid identifiers should lex correctly
const testValidIdentifiers = (): void => {
  const validIdent = fc.string({ minLength: 1, maxLength: 20 })
    .filter(s => /^[a-zA-Z][a-zA-Z0-9_]*$/.test(s));
  
  fc.assert(
    fc.property(validIdent, (ident: string) => {
      const tokens = lex(ident);
      return tokens.length >= 2 && 
             (tokens[0]?.kind === TokenKind.IDENT || tokens[0]?.kind === TokenKind.TYPE_IDENT) &&
             tokens[tokens.length - 1]?.kind === TokenKind.EOF;
    }),
    { numRuns: 50 }
  );
  console.log('✓ Valid identifiers lex correctly');
};

// Property: Valid integers should lex correctly
const testValidIntegers = (): void => {
  fc.assert(
    fc.property(
      fc.integer({ min: 0, max: 999999 }),
      (num: number) => {
        const tokens = lex(num.toString());
        return tokens.length >= 2 && 
               tokens[0]?.kind === TokenKind.INT &&
               tokens[0]?.value === num &&
               tokens[tokens.length - 1]?.kind === TokenKind.EOF;
      }
    ),
    { numRuns: 50 }
  );
  console.log('✓ Valid integers lex correctly');
};

// Property: String literals should lex correctly
const testStringLiterals = (): void => {
  const validString = fc.string({ minLength: 0, maxLength: 30 })
    .filter(s => !s.includes('"') && !s.includes('\\'));
  
  fc.assert(
    fc.property(validString, (str: string) => {
      const input = `"${str}"`;
      const tokens = lex(input);
      return tokens.length >= 2 && 
             tokens[0]?.kind === TokenKind.STRING &&
             tokens[0]?.value === str &&
             tokens[tokens.length - 1]?.kind === TokenKind.EOF;
    }),
    { numRuns: 50 }
  );
  console.log('✓ String literals lex correctly');
};

// Property: Round-trip test for simple valid programs
const testRoundTrip = (): void => {
  const validPrograms = [
    'This module is test.',
    'Define User with name: Text.',
    'To greet, produce Text.',
    'To test, produce Text:\n  Let x be 42.\n  Return "hello".',
  ];

  for (const program of validPrograms) {
    try {
      const can = canonicalize(program);
      const tokens = lex(can);
      const ast = parse(tokens);
      const core = lowerModule(ast);
      
      // Basic sanity checks
      if (!ast || !core) {
        throw new Error(`Round-trip failed for: ${program}`);
      }
      if (ast.kind !== 'Module' || core.kind !== 'Module') {
        throw new Error(`Expected Module, got ${ast.kind} -> ${core.kind}`);
      }
    } catch (e) {
      throw new Error(`Round-trip failed for "${program}": ${(e as Error).message}`);
    }
  }
  console.log('✓ Round-trip test passes for valid programs');
};

// Property: Parser should handle malformed input gracefully
const testParserErrorHandling = (): void => {
  const malformedInputs = [
    'This module is',  // Missing module name
    'Define User with',  // Incomplete data definition
    'To greet produce',  // Missing comma
    'Let x be',  // Missing expression
    'Return',  // Missing expression
  ];

  for (const input of malformedInputs) {
    try {
      const can = canonicalize(input);
      const tokens = lex(can);
      parse(tokens);
      throw new Error(`Expected parser error for: ${input}`);
    } catch (e) {
      // Expected to throw
      if (!(e as any).pos) {
        throw new Error(`Parser error should include position for: ${input}`);
      }
    }
  }
  console.log('✓ Parser handles malformed input gracefully with position info');
};

function main(): void {
  console.log('Running property tests...\n');
  
  try {
    testCanonicalizerIdempotent();
    testLexerEmptyInput();
    testLexerAlwaysEOF();
    testValidIdentifiers();
    testValidIntegers();
    testStringLiterals();
    testRoundTrip();
    // Generics sanity check
    testGenericsBasic();
    testParserErrorHandling();
    
    console.log('\n✅ All property tests passed!');
  } catch (e) {
    console.error('\n❌ Property test failed:', (e as Error).message);
    process.exit(1);
  }
}

main();

// Generics: basic sanity
function testGenericsBasic(): void {
  const src = [
    'This module is demo.generic.',
    '',
    'To identity of T, with x: T, produce T:',
    '  Return x.',
    '',
  ].join('\n');
  const can = canonicalize(src);
  const toks = lex(can);
  const ast = parse(toks);
  const core = lowerModule(ast);
  if (core.kind !== 'Module') throw new Error('Expected Module');
  const fn = core.decls.find(d => (d as any).kind === 'Func') as any;
  if (!fn) throw new Error('Expected a function decl');
  const tp = (fn.typeParams ?? []) as string[];
  if (!(tp.length === 1 && tp[0] === 'T')) throw new Error('Expected type param T');
  const paramType = fn.params[0]?.type;
  const retType = fn.ret;
  if (!paramType || paramType.kind !== 'TypeVar' || paramType.name !== 'T') throw new Error('Param type should be TypeVar T');
  if (!retType || retType.kind !== 'TypeVar' || (retType as any).name !== 'T') throw new Error('Return type should be TypeVar T');
  console.log('✓ Generics basic parse/lower');
}
