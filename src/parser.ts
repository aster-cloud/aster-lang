import {KW, TokenKind} from './tokens.js';
import {Node} from './ast.js';
import type {
    Block,
    Case,
    Declaration,
    Expression,
    Field,
    Module,
    Parameter,
    Pattern,
    Statement,
    Token,
    Type,
} from './types.js';
import {Diagnostics} from './diagnostics.js';
import {createLogger} from './utils/logger.js';

export interface ParserContext {
  readonly tokens: readonly Token[];
  index: number;
  moduleName: string | null;
  declaredTypes: Set<string>;
  currentTypeVars: Set<string>;
  collectedEffects: string[] | null;
  effectSnapshots: Array<string[] | null>;
  debug: { enabled: boolean; depth: number; log(message: string): void };
  peek(offset?: number): Token;
  next(): Token;
  at(kind: TokenKind, value?: Token['value']): boolean;
  expect(kind: TokenKind, message: string): Token;
  isKeyword(kw: string): boolean;
  isKeywordSeq(words: string | string[]): boolean;
  nextWord(): Token;
  nextWords(words: string[]): void;
  consumeIndent(): void;
  consumeNewlines(): void;
  pushEffect(effects: string[]): void;
  snapshotEffects(): string[] | null;
  restoreEffects(snapshot: string[] | null): void;
  withTypeScope<T>(names: Iterable<string>, body: () => T): T;
}

const parserLogger = createLogger('parser');

export function parse(tokens: readonly Token[]): Module {
  const ctx: ParserContext = {
    tokens,
    index: 0,
    moduleName: null,
    declaredTypes: new Set<string>(),
    currentTypeVars: new Set<string>(),
    collectedEffects: null,
    effectSnapshots: [],
    debug: {
      enabled: process.env.ASTER_DEBUG_TYPES === '1',
      depth: 0,
      log: (message: string): void => {
        if (!ctx.debug.enabled) return;
        parserLogger.debug(`[parseType] ${message}`, { depth: ctx.debug.depth });
      },
    },
    peek: (offset: number = 0): Token => {
      const idx = ctx.index + offset;
      if (idx < ctx.tokens.length) return ctx.tokens[idx]!;
      return ctx.tokens[ctx.tokens.length - 1]!;
    },
    next: (): Token => {
      const tok = ctx.peek();
      if (ctx.index < ctx.tokens.length) ctx.index += 1;
      return tok;
    },
    at: (kind: TokenKind, value?: Token['value']): boolean => {
      const t = ctx.peek();
      if (!t) return false;
      if (t.kind !== kind) return false;
      if (value === undefined) return true;
      return t.value === value;
    },
    expect: (kind: TokenKind, message: string): Token => {
      if (!ctx.at(kind)) error(message + `, got ${ctx.peek().kind}`);
      return ctx.next();
    },
    isKeyword: (kw: string): boolean => {
      const v = tokLowerAt(ctx.index);
      return v === kw;
    },
    isKeywordSeq: (words: string | string[]): boolean => {
      const ws = Array.isArray(words) ? words : kwParts(words);
      for (let k = 0; k < ws.length; k++) {
        const v = tokLowerAt(ctx.index + k);
        if (v !== ws[k]) return false;
      }
      return true;
    },
    nextWord: (): Token => {
      if (!(ctx.at(TokenKind.IDENT) || ctx.at(TokenKind.TYPE_IDENT))) error('Expected keyword/identifier');
      return ctx.next();
    },
    nextWords: (words: string[]): void => {
      words.forEach(() => ctx.nextWord());
    },
    consumeIndent: (): void => {
      while (ctx.at(TokenKind.INDENT)) ctx.next();
    },
    consumeNewlines: (): void => {
      while (ctx.at(TokenKind.NEWLINE)) ctx.next();
    },
    pushEffect: (effects: string[]): void => {
      if (!Array.isArray(ctx.collectedEffects)) ctx.collectedEffects = [];
      ctx.collectedEffects!.push(...effects);
    },
    snapshotEffects: (): string[] | null => {
      const snapshot = ctx.collectedEffects ? [...ctx.collectedEffects] : null;
      ctx.effectSnapshots.push(snapshot);
      return snapshot;
    },
    restoreEffects: (snapshot: string[] | null): void => {
      ctx.collectedEffects = snapshot ? [...snapshot] : null;
    },
    withTypeScope: <T>(names: Iterable<string>, body: () => T): T => {
      const saved = new Set(ctx.currentTypeVars);
      for (const name of names) ctx.currentTypeVars.add(name);
      try {
        return body();
      } finally {
        ctx.currentTypeVars = saved;
      }
    },
  };

  function error(msg: string, tok: Token = ctx.peek()): never {
    Diagnostics.unexpectedToken(msg, tok.start).withMessage(msg).throw();
    throw new Error('unreachable');
  }

  // 局部变量：收集解析结果
  const decls: Declaration[] = [];
  ctx.consumeNewlines();
  while (!ctx.at(TokenKind.EOF)) {
    ctx.consumeNewlines();
    while (ctx.at(TokenKind.DEDENT)) ctx.next();
    while (ctx.at(TokenKind.INDENT)) ctx.next();
    ctx.consumeNewlines();
    if (ctx.at(TokenKind.EOF)) break;
    if (ctx.isKeywordSeq(KW.MODULE_IS)) {
      ctx.nextWords(kwParts(KW.MODULE_IS));
      const name = parseDottedIdent();
      ctx.moduleName = name;
      expectDot();
    } else if (ctx.isKeyword(KW.USE)) {
      ctx.nextWord();
      const name = parseDottedIdent();
      let asName: string | null = null;
      if (ctx.isKeyword(KW.AS)) {
        ctx.nextWord();
        asName = parseIdent();
      }
      expectDot();
      decls.push(Node.Import(name, asName));
    } else if (ctx.isKeyword(KW.DEFINE)) {
      ctx.nextWord();
      const typeName = parseTypeIdent();
      if (ctx.isKeywordSeq(KW.WITH)) {
        ctx.nextWord();
        const fields = parseFieldList();
        expectDot();
        decls.push(Node.Data(typeName, fields));
        ctx.declaredTypes.add(typeName);
      } else if (ctx.isKeywordSeq(KW.ONE_OF)) {
        ctx.nextWords(kwParts(KW.ONE_OF));
        const variants = parseVariantList();
        expectDot();
        const en = Node.Enum(typeName, variants);
        const spans = (parseVariantList as any)._lastSpans as import('./types.js').Span[] | undefined;
        if (spans && Array.isArray(spans)) (en as any).variantSpans = spans;
        decls.push(en);
        ctx.declaredTypes.add(typeName);
      } else {
        error("Expected 'with' or 'as one of' after type name");
      }
    } else if (ctx.isKeyword(KW.TO)) {
      // Function
      const toTok = ctx.peek();
      ctx.nextWord();
      const nameTok = ctx.peek();
      const name = parseIdent();
      // Optional type parameters: 'of' TypeId ('and' TypeId)*
      let typeParams: string[] = [];
      if (ctx.isKeyword('of')) {
        ctx.nextWord();
        let more = true;
        while (more) {
          // Stop if we ran into parameter list or produce clause
          if (ctx.isKeyword(KW.WITH) || ctx.isKeyword(KW.PRODUCE) || ctx.at(TokenKind.COLON)) break;
          // Parse a type variable name (prefer TYPE_IDENT; fall back to IDENT)
          const tv = ctx.at(TokenKind.TYPE_IDENT) ? (ctx.next().value as string) : parseIdent();
          typeParams.push(tv);
          if (ctx.isKeyword(KW.AND)) {
            ctx.nextWord();
            continue;
          }
          if (ctx.at(TokenKind.COMMA)) {
            ctx.next();
            // If comma is followed by 'with' or produce, stop
            if (ctx.isKeyword(KW.WITH) || ctx.isKeyword(KW.PRODUCE)) {
              more = false;
              break;
            }
            continue;
          }
          more = false;
        }
      }
      // Capture generic type parameters: 'of T and U'
      const savedTypeVars = new Set(ctx.currentTypeVars);
      ctx.currentTypeVars = new Set(typeParams);
      const params = parseParamList();
      if (params.length > 0) expectCommaOr();
      else if (ctx.at(TokenKind.COMMA)) ctx.next();
      expectKeyword(KW.PRODUCE, "Expected 'produce' and return type");
      const retType = parseType();
      let effects: string[] = [];
      // Prepare to collect any trailing effect sentences inside the body
      const prevCollected: string[] | null = ctx.collectedEffects;
      ctx.collectedEffects = [];
      let body: Block | null = null;

      // After return type, we can see '.' ending the sentence, then an optional effect sentence
      // Or we can see an inline effect ending with ':'
      if (ctx.at(TokenKind.DOT)) {
        ctx.next();
        ctx.consumeNewlines();
        if (
          ctx.isKeywordSeq(KW.PERFORMS) ||
          (tokLowerAt(ctx.index) === 'it' && tokLowerAt(ctx.index + 1) === 'performs')
        ) {
          if (!ctx.isKeywordSeq(KW.PERFORMS)) ctx.nextWord();
          ctx.nextWords(kwParts(KW.PERFORMS));
          effects = parseEffectList();
          if (ctx.at(TokenKind.DOT)) {
            ctx.next();
          } else if (ctx.at(TokenKind.COLON)) {
            ctx.next();
            expectNewline();
            body = parseBlock();
          } else {
            error("Expected '.' or ':' after effect clause");
          }
        }
      } else if (
        ctx.isKeywordSeq(KW.PERFORMS) ||
        (tokLowerAt(ctx.index) === 'it' && tokLowerAt(ctx.index + 1) === 'performs')
      ) {
        if (!ctx.isKeywordSeq(KW.PERFORMS)) ctx.nextWord();
        ctx.nextWords(kwParts(KW.PERFORMS));
        effects = parseEffectList();
        if (ctx.at(TokenKind.DOT)) {
          ctx.next();
        } else if (ctx.at(TokenKind.COLON)) {
          ctx.next();
          expectNewline();
          body = parseBlock();
        } else {
          error("Expected '.' or ':' after effect clause");
        }
      } else if (ctx.at(TokenKind.COLON)) {
        ctx.next();
        expectNewline();
        body = parseBlock();
      } else {
        error("Expected '.' or ':' after return type");
      }

      // Infer missing generic type parameters from usage if none declared explicitly
      if (typeParams.length === 0) {
        const BUILTINS = new Set(['Int', 'Bool', 'Text', 'Double', 'Float', 'Option', 'Result', 'List', 'Map']);
        const found = new Set<string>();
        const visitType = (t: Type): void => {
          switch (t.kind) {
            case 'TypeName':
              if (
                typeof t.name === 'string' &&
                /^[A-Z][A-Za-z0-9_]*$/.test(t.name) &&
                !BUILTINS.has(t.name) &&
                !ctx.declaredTypes.has(t.name)
              ) {
                found.add(t.name);
              }
              break;
            case 'TypeApp':
              t.args.forEach(visitType);
              break;
            case 'Maybe':
            case 'Option':
              visitType((t as any).type);
              break;
            case 'Result':
              visitType((t as any).ok);
              visitType((t as any).err);
              break;
            case 'List':
              visitType((t as any).type);
              break;
            case 'Map':
              visitType((t as any).key);
              visitType((t as any).val);
              break;
            case 'FuncType':
              (t as any).params.forEach(visitType);
              visitType((t as any).ret);
              break;
            default:
              break;
          }
        };
        for (const p of params) visitType(p.type);
        visitType(retType);
        if (found.size > 0) {
          typeParams = Array.from(found);
        }
      }

      const endTok = ctx.tokens[ctx.index - 1] || ctx.peek();
      // Merge any trailing effect sentences collected inside the body
      if (Array.isArray(ctx.collectedEffects) && ctx.collectedEffects.length > 0) {
        effects = effects.concat(ctx.collectedEffects);
      }
      // restore collector and type params scope
      ctx.collectedEffects = prevCollected;
      // restore type params scope
      ctx.currentTypeVars = savedTypeVars;

      // Separate base effects from capabilities
      const { baseEffects, effectCaps } = separateEffectsAndCaps(effects);

      const fn = Node.Func(name, typeParams, params, retType, baseEffects, body);
      (fn as any).span = { start: toTok.start, end: endTok.end };
      // Record function name span for precise navigation/highlighting
      ;(fn as any).nameSpan = { start: nameTok.start, end: (ctx.tokens[ctx.index - 1] || nameTok).end };
      // Attach capability metadata if present
      if (Object.keys(effectCaps).length > 0) {
        (fn as any).effectCaps = effectCaps;
      }
      decls.push(fn);
    } else if (ctx.at(TokenKind.NEWLINE) || ctx.at(TokenKind.DEDENT) || ctx.at(TokenKind.INDENT)) {
      // Tolerate stray whitespace/dedent/indent at top-level
      ctx.next();
    } else {
      error('Unexpected token at top level');
    }
    ctx.consumeNewlines();
  }

  return Node.Module(ctx.moduleName, decls);

  // Helpers
  function kwParts(phrase: string): string[] {
    return phrase.split(' ');
  }
  function tokLowerAt(idx: number): string | null {
    const t = ctx.tokens[idx];
    if (!t) return null;
    if (t.kind !== TokenKind.IDENT && t.kind !== TokenKind.TYPE_IDENT) return null;
    return ((t.value as string) || '').toLowerCase();
  }

  function expectKeyword(kw: string, msg: string): void {
    if (!ctx.isKeyword(kw)) Diagnostics.expectedKeyword(kw, ctx.peek().start).withMessage(msg).throw();
    ctx.nextWord();
  }
  function expectDot(): void {
    if (!ctx.at(TokenKind.DOT)) Diagnostics.expectedPunctuation('.', ctx.peek().start).throw();
    ctx.next();
  }
  function expectCommaOr(): void {
    if (ctx.at(TokenKind.COMMA)) {
      ctx.next();
    }
  }
  function expectNewline(): void {
    if (!ctx.at(TokenKind.NEWLINE))
      Diagnostics.expectedToken('newline', ctx.peek().kind, ctx.peek().start).throw();
    ctx.next();
  }

  function parseDottedIdent(): string {
    const parts = [parseIdent()];
    while (
      ctx.at(TokenKind.DOT) &&
      ctx.tokens[ctx.index + 1] &&
      (ctx.tokens[ctx.index + 1]!.kind === TokenKind.IDENT || ctx.tokens[ctx.index + 1]!.kind === TokenKind.TYPE_IDENT)
    ) {
      ctx.next();
      if (ctx.at(TokenKind.IDENT)) {
        parts.push(parseIdent());
      } else if (ctx.at(TokenKind.TYPE_IDENT)) {
        parts.push(ctx.next().value as string);
      }
    }
    return parts.join('.');
  }
  function parseIdent(): string {
    if (!ctx.at(TokenKind.IDENT)) Diagnostics.expectedIdentifier(ctx.peek().start).throw();
    return ctx.next().value as string;
  }
  function parseTypeIdent(): string {
    if (!ctx.at(TokenKind.TYPE_IDENT))
      Diagnostics.expectedToken('Type identifier', ctx.peek().kind, ctx.peek().start).throw();
    return ctx.next().value as string;
  }

  function parseFieldList(): Field[] {
    const fields: Field[] = [];
    let hasMore = true;
    while (hasMore) {
      const nameTok = ctx.peek();
      const name = parseIdent();
      if (!ctx.at(TokenKind.COLON)) error("Expected ':' after field name");
      ctx.next();
      const t = parseType();
      const f: Field = { name, type: t };
      const endTok = ctx.tokens[ctx.index - 1] || ctx.peek();
      (f as any).span = { start: nameTok.start, end: endTok.end };
      fields.push(f);
      if (ctx.at(TokenKind.COMMA)) {
        ctx.next();
        continue;
      }
      if (ctx.isKeyword(KW.AND)) {
        ctx.nextWord();
        continue;
      }
      hasMore = false;
    }
    return fields;
  }
  function parseVariantList(): string[] {
    const vars: string[] = [];
    const spans: import('./types.js').Span[] = [] as any;
    let hasMore = true;
    while (hasMore) {
      const vTok = ctx.peek();
      const v = parseTypeIdent();
      spans.push({ start: vTok.start, end: (ctx.tokens[ctx.index - 1] || vTok).end } as any);
      vars.push(v);
      if (ctx.at(TokenKind.IDENT) && ((ctx.peek().value as string) || '').toLowerCase() === KW.OR) {
        ctx.nextWord();
        continue;
      }
      if (ctx.at(TokenKind.COMMA)) {
        ctx.next();
        continue;
      }
      hasMore = false;
    }
    // Attach variant spans to the last created Enum node by caller using a side-channel is messy,
    // so return vars here and the caller will attach spans after constructing the node.
    // We stash spans on a temporary property on the parser function object for retrieval.
    (parseVariantList as any)._lastSpans = spans;
    return vars;
  }

  function parseParamList(): Parameter[] {
    const params: Parameter[] = [];
    // 'with' params
    if (ctx.isKeyword(KW.WITH)) {
      ctx.nextWord();
      let hasMore = true;
      while (hasMore) {
        const nameTok = ctx.peek();
        const name = parseIdent();
        if (!ctx.at(TokenKind.COLON)) error("Expected ':' after parameter name");
        ctx.next();
        const type = parseType();
        const p: Parameter = { name, type };
        const endTok = ctx.tokens[ctx.index - 1] || ctx.peek();
        (p as any).span = { start: nameTok.start, end: endTok.end };
        params.push(p);
        if (ctx.at(TokenKind.IDENT) && ((ctx.peek().value as string) || '').toLowerCase() === KW.AND) {
          ctx.nextWord();
          continue;
        }
        if (ctx.at(TokenKind.COMMA)) {
          // If a trailing comma appears before 'produce', stop params
          if (tokLowerAt(ctx.index + 1) === KW.PRODUCE) {
            hasMore = false;
          } else {
            ctx.next();
            continue;
          }
        }
        hasMore = false;
      }
      return params;
    }
    // Bare params: name: Type [(and|,) name: Type]*
    if (ctx.at(TokenKind.IDENT) && ctx.tokens[ctx.index + 1] && ctx.tokens[ctx.index + 1]!.kind === TokenKind.COLON) {
      let hasMore = true;
      while (hasMore) {
        const nameTok = ctx.peek();
        const name = parseIdent();
        if (!ctx.at(TokenKind.COLON)) error("Expected ':' after parameter name");
        ctx.next();
        const type = parseType();
        const p: Parameter = { name, type };
        const endTok = ctx.tokens[ctx.index - 1] || ctx.peek();
        (p as any).span = { start: nameTok.start, end: endTok.end };
        params.push(p);
        // Accept 'and' or ',' between parameters
        if (ctx.at(TokenKind.IDENT) && ((ctx.peek().value as string) || '').toLowerCase() === KW.AND) {
          ctx.nextWord();
          continue;
        }
        if (ctx.at(TokenKind.COMMA)) {
          // If a trailing comma appears before 'produce' or 'with', stop params
          const after = tokLowerAt(ctx.index + 1);
          if (after === KW.PRODUCE || after === KW.WITH) {
            hasMore = false;
          } else {
            ctx.next();
            continue;
          }
        }
        hasMore = false;
      }
    }
    return params;
  }

  function parseEffectList(): string[] {
    const effs: string[] = [];

    // Parse base effect (io or cpu)
    if (ctx.isKeyword(KW.IO)) {
      ctx.nextWord();
      effs.push('io');
    }
    if (ctx.isKeyword(KW.CPU)) {
      ctx.nextWord();
      effs.push('cpu');
    }

    // Parse optional capability list with 'with' keyword
    // Example: io with Http and Sql and Time
    if (ctx.isKeyword(KW.WITH)) {
      ctx.nextWord(); // consume 'with'

      // First capability after 'with'
      if (!ctx.at(TokenKind.TYPE_IDENT)) {
        error("Expected capability name (capitalized identifier) after 'with'");
      }
      const cap = ctx.next().value as string;
      effs.push(cap);

      // Additional capabilities with 'and' separator
      while (ctx.isKeyword(KW.AND)) {
        ctx.nextWord(); // consume 'and'
        if (!ctx.at(TokenKind.TYPE_IDENT)) {
          error("Expected capability name (capitalized identifier) after 'and'");
        }
        const cap2 = ctx.next().value as string;
        effs.push(cap2);
      }

      return effs;
    }

    // Parse optional capability list with 'and' separator
    // Example: io and Http and Sql and Time
    while (ctx.isKeyword(KW.AND)) {
      ctx.nextWord(); // consume 'and'

      // Capabilities are TYPE_IDENT (capitalized names like Http, Sql, Time)
      if (!ctx.at(TokenKind.TYPE_IDENT)) {
        // Could also be another effect keyword (io/cpu)
        if (ctx.isKeyword(KW.IO)) {
          ctx.nextWord();
          effs.push('io');
          continue;
        }
        if (ctx.isKeyword(KW.CPU)) {
          ctx.nextWord();
          effs.push('cpu');
          continue;
        }
        error("Expected capability name (capitalized identifier) after 'and'");
      }
      const cap = ctx.next().value as string;
      effs.push(cap);
    }

    // Parse optional capability brackets [Cap1, Cap2, Cap3]
    if (ctx.at(TokenKind.LBRACKET)) {
      ctx.next(); // consume '['
      while (!ctx.at(TokenKind.RBRACKET) && !ctx.at(TokenKind.EOF)) {
        // Capabilities are TYPE_IDENT (capitalized names like Http, Sql, Time)
        if (!ctx.at(TokenKind.TYPE_IDENT)) {
          error("Expected capability name (capitalized identifier)");
        }
        const cap = ctx.next().value as string;
        effs.push(cap);

        if (ctx.at(TokenKind.COMMA)) {
          ctx.next();
          continue;
        } else {
          break;
        }
      }
      if (!ctx.at(TokenKind.RBRACKET)) error("Expected ']' after capability list");
      ctx.next(); // consume ']'
    }

    return effs;
  }

  function separateEffectsAndCaps(effects: string[]): { baseEffects: string[], effectCaps: Record<string, string[]> } {
    const baseEffects: string[] = [];
    const capabilities: string[] = [];
    const baseEffectSet = new Set(['io', 'cpu', 'pure']);

    for (const eff of effects) {
      if (baseEffectSet.has(eff.toLowerCase())) {
        baseEffects.push(eff.toLowerCase());
      } else {
        capabilities.push(eff);
      }
    }

    // Group capabilities under base effects
    const effectCaps: Record<string, string[]> = {};
    if (baseEffects.length > 0 && capabilities.length > 0) {
      // Associate capabilities with the first base effect
      const firstEffect = baseEffects[0];
      if (firstEffect) {
        effectCaps[firstEffect] = capabilities;
      }
    }

    return { baseEffects, effectCaps };
  }

  function parseType(): Type {
    // Check for @pii annotation: @pii(level, category) Type
    let piiAnnotation: { level: string; category: string } | null = null;
    if (ctx.at(TokenKind.AT)) {
      ctx.next(); // consume '@'
      if (!ctx.isKeyword('pii')) {
        error("Expected 'pii' after '@'");
      }
      ctx.nextWord(); // consume 'pii'
      if (!ctx.at(TokenKind.LPAREN)) {
        error("Expected '(' after '@pii'");
      }
      ctx.next(); // consume '('

      // Parse level (L1, L2, L3, etc.)
      if (!ctx.at(TokenKind.TYPE_IDENT) && !ctx.at(TokenKind.IDENT)) {
        error("Expected PII level (e.g., L1, L2, L3)");
      }
      const level = ctx.next().value as string;

      if (!ctx.at(TokenKind.COMMA)) {
        error("Expected ',' after PII level");
      }
      ctx.next(); // consume ','

      // Parse category (email, phone, ssn, name, etc.)
      if (!ctx.at(TokenKind.IDENT)) {
        error("Expected PII category (e.g., email, phone, ssn)");
      }
      const category = ctx.next().value as string;

      if (!ctx.at(TokenKind.RPAREN)) {
        error("Expected ')' after PII category");
      }
      ctx.next(); // consume ')'

      piiAnnotation = { level, category };
    }

    const applyAnnotations = (t: Type): Type => {
      let result = t;

      // First apply nullable wrapper if present
      if (ctx.at(TokenKind.QUESTION)) {
        ctx.next();
        result = Node.Maybe(result);
      }

      // Then wrap in TypePii if annotation present
      // This ensures @pii(L3, phone) Text? becomes TypePii(Maybe(Text), L3, phone)
      if (piiAnnotation) {
        result = Node.TypePii(result, piiAnnotation.level as any, piiAnnotation.category as any);
      }

      return result;
    };

    // maybe T | Option of T | Result of T or E | list of T | map Text to Int | Text/Int/Float/Bool | TypeIdent
    if (ctx.isKeyword(KW.MAYBE)) {
      ctx.nextWord();
      return applyAnnotations(Node.Maybe(parseType()));
    }
    if (ctx.isKeywordSeq(KW.OPTION_OF)) {
      ctx.nextWords(kwParts(KW.OPTION_OF));
      return applyAnnotations(Node.Option(parseType()));
    }
    if (ctx.isKeywordSeq(KW.RESULT_OF)) {
      ctx.nextWords(kwParts(KW.RESULT_OF));
      const ok = parseType();
      // Accept 'or' or 'and' between ok and err
      if (ctx.isKeyword(KW.OR) || ctx.isKeyword(KW.AND)) ctx.nextWord();
      else Diagnostics.expectedKeyword('or/and', ctx.peek().start).withMessage("Expected 'or'/'and' in Result of").throw();
      const err = parseType();
      return applyAnnotations(Node.Result(ok, err));
    }
    if (ctx.isKeywordSeq(KW.FOR_EACH)) {
      /* not a type; handled elsewhere */
    }
    if (ctx.isKeywordSeq(KW.WITHIN)) {
      /* not a type */
    }

    if (ctx.isKeywordSeq(['list', 'of'])) {
      ctx.nextWord();
      ctx.nextWord();
      return applyAnnotations(Node.List(parseType()));
    }
    if (ctx.isKeyword('map')) {
      ctx.nextWord();
      const k = parseType();
      expectKeyword(KW.TO_WORD, "Expected 'to' in map type");
      const v = parseType();
      return applyAnnotations(Node.Map(k, v));
    }

    if (ctx.isKeyword(KW.TEXT)) {
      ctx.nextWord();
      return applyAnnotations(Node.TypeName('Text'));
    }
    if (ctx.isKeyword(KW.INT)) {
      ctx.nextWord();
      return applyAnnotations(Node.TypeName('Int'));
    }
    if (ctx.isKeyword(KW.FLOAT)) {
      ctx.nextWord();
      return applyAnnotations(Node.TypeName('Double'));
    }
    if (ctx.isKeyword(KW.BOOL_TYPE)) {
      ctx.nextWord();
      return applyAnnotations(Node.TypeName('Bool'));
    }

    // Handle capitalized type keywords (Int, Bool, etc.) that are tokenized as IDENT
    if (ctx.at(TokenKind.IDENT)) {
      const value = ctx.peek().value as string;
      if (value === 'Int') {
        ctx.nextWord();
        return applyAnnotations(Node.TypeName('Int'));
      }
      if (value === 'Bool') {
        ctx.nextWord();
        return applyAnnotations(Node.TypeName('Bool'));
      }
      if (value === 'Text') {
        ctx.nextWord();
        return applyAnnotations(Node.TypeName('Text'));
      }
      if (value === 'Float') {
        ctx.nextWord();
        return applyAnnotations(Node.TypeName('Float'));
      }
    }

    if (ctx.at(TokenKind.TYPE_IDENT)) {
      const name = ctx.next().value as string;
      // Generic application: TypeName of T [and U]*
      if (ctx.isKeyword('of')) {
        ctx.nextWord();
        const args: Type[] = [];
        let more = true;
        while (more) {
          args.push(parseType());
          if (ctx.isKeyword(KW.AND)) {
            ctx.nextWord();
            continue;
          }
          if (ctx.at(TokenKind.COMMA)) {
            ctx.next();
            continue;
          }
          more = false;
        }
        return applyAnnotations(Node.TypeApp(name, args));
      }
      // Recognize type variables in current function scope
      if (ctx.currentTypeVars.has(name)) {
        return applyAnnotations(Node.TypeVar(name));
      }
      return applyAnnotations(Node.TypeName(name));
    }

    error('Expected type');
  }

  function parseBlock(): Block {
    const statements: Statement[] = [];
    ctx.consumeNewlines();
    // Allow either an indented block or a single-line block without INDENT.
    if (!ctx.at(TokenKind.INDENT)) {
      // Parse a single statement at current indentation level
      const startTok = ctx.peek();
      const stmt = parseStatement();
      const endTok = ctx.tokens[ctx.index - 1] || startTok;
      const b = Node.Block([stmt]);
      (b as any).span = { start: startTok.start, end: endTok.end };
      return b;
    }
    ctx.next();
    while (!ctx.at(TokenKind.DEDENT) && !ctx.at(TokenKind.EOF)) {
      ctx.consumeNewlines();
      if (ctx.at(TokenKind.DEDENT) || ctx.at(TokenKind.EOF)) break;
      statements.push(parseStatement());
      ctx.consumeNewlines();
    }
    if (!ctx.at(TokenKind.DEDENT)) error('Expected dedent');
    const endTok = ctx.peek();
    ctx.next();
    const b = Node.Block(statements);
    const startSpan = (statements[0] as any)?.span?.start || endTok.start;
    (b as any).span = { start: startSpan, end: endTok.end };
    return b;
  }

  function expectPeriodEnd(): void {
    if (!ctx.at(TokenKind.DOT)) error("Expected '.' at end of statement");
    ctx.next();
  }
  function expectPeriodEndOrLine(): void {
    if (ctx.at(TokenKind.DOT)) {
      ctx.next();
      return;
    }
    // Tolerate newline/dedent/EOF terminators inside blocks for certain statements (e.g., Return)
    if (ctx.at(TokenKind.NEWLINE) || ctx.at(TokenKind.DEDENT) || ctx.at(TokenKind.EOF)) return;
    error("Expected '.' at end of statement");
  }

  function parseStatement(): Statement {
    if (ctx.isKeyword(KW.LET)) {
      const letTok = ctx.peek();
      ctx.nextWord();
      const name = parseIdent();
      expectKeyword(KW.BE, "Use 'be' in bindings: 'Let x be ...'.");
      // Special-case lambda block form to avoid trailing '.'
      if ((ctx.isKeyword('a') && tokLowerAt(ctx.index + 1) === 'function') || ctx.isKeyword('function')) {
        if (ctx.isKeyword('a')) ctx.nextWord(); // optional 'a'
        ctx.nextWord(); // 'function'
        const params = parseParamList();
        expectCommaOr();
        expectKeyword(KW.PRODUCE, "Expected 'produce' and return type");
        const retType = parseType();
        if (!ctx.at(TokenKind.COLON)) error("Expected ':' after return type in lambda");
        ctx.next();
        expectNewline();
        const body = parseBlock();
        const nd = Node.Let(name, Node.Lambda(params, retType, body));
        const endTok = ctx.tokens[ctx.index - 1] || ctx.peek();
        (nd as any).span = { start: letTok.start, end: endTok.end };
        return nd;
      }
      const expr = parseExpr();
      expectPeriodEnd();
      const nd = Node.Let(name, expr);
      const endTok = ctx.tokens[ctx.index - 1] || ctx.peek();
      (nd as any).span = { start: letTok.start, end: endTok.end };
      return nd;
    }
    if (ctx.isKeyword(KW.SET)) {
      const setTok = ctx.peek();
      ctx.nextWord();
      const name = parseIdent();
      expectKeyword(KW.TO_WORD, "Use 'to' in assignments: 'Set x to ...'.");
      const expr = parseExpr();
      expectPeriodEnd();
      const nd = Node.Set(name, expr);
      const endTok = ctx.tokens[ctx.index - 1] || ctx.peek();
      (nd as any).span = { start: setTok.start, end: endTok.end };
      return nd;
    }
    if (ctx.isKeyword(KW.RETURN)) {
      const retTok = ctx.peek();
      ctx.nextWord();
      const expr = parseExpr();
      expectPeriodEndOrLine();
      // Allow trailing effect sentence immediately after a Return: 'It performs io.'
      // This attaches to the enclosing function's effects if present.
      if ((tokLowerAt(ctx.index) === 'it' && tokLowerAt(ctx.index + 1) === 'performs') || tokLowerAt(ctx.index) === 'performs') {
        if (tokLowerAt(ctx.index) === 'it') ctx.nextWord();
        if (tokLowerAt(ctx.index) === 'performs') {
          ctx.nextWord();
          const effs = parseEffectList();
          expectPeriodEnd();
          if (Array.isArray(ctx.collectedEffects)) ctx.collectedEffects.push(...effs);
        }
      }
      const nd = Node.Return(expr);
      const endTok = ctx.tokens[ctx.index - 1] || ctx.peek();
      (nd as any).span = { start: retTok.start, end: endTok.end };
      return nd;
    }
    if (ctx.isKeyword(KW.AWAIT)) {
      ctx.nextWord();
      if (!ctx.at(TokenKind.LPAREN)) error("Expected '(' after await");
      const args = parseArgList();
      if (args.length !== 1) error('await(expr) takes exactly one argument');
      const aw = Node.Call(Node.Name('await'), args);
      expectPeriodEnd();
      return aw as unknown as Statement;
    }
    if (ctx.isKeyword(KW.IF)) {
      const ifTok = ctx.peek();
      ctx.nextWord();
      let negate = false;
      if (ctx.isKeyword(KW.NOT)) {
        ctx.nextWord();
        negate = true;
      }
      const cond = parseExpr();
      const condExpr = negate ? Node.Call(Node.Name('not'), [cond]) : cond;
      if (!ctx.at(TokenKind.COMMA)) error("Expected ',' after condition");
      ctx.next();
      if (!ctx.at(TokenKind.COLON)) error("Expected ':' after ',' in If");
      ctx.next();
      expectNewline();
      const thenBlock = parseBlock();
      let elseBlock: Block | null = null;
      if (ctx.isKeyword(KW.OTHERWISE)) {
        ctx.nextWord();
        if (!ctx.at(TokenKind.COMMA)) error("Expected ',' after 'Otherwise'");
        ctx.next();
        if (!ctx.at(TokenKind.COLON)) error("Expected ':' after ',' in Otherwise");
        ctx.next();
        expectNewline();
        elseBlock = parseBlock();
      }
      const nd = Node.If(condExpr, thenBlock, elseBlock);
      const endTok = ctx.tokens[ctx.index - 1] || ctx.peek();
      (nd as any).span = { start: ifTok.start, end: endTok.end };
      return nd;
    }
    if (ctx.isKeyword(KW.MATCH)) {
      const mTok = ctx.peek();
      ctx.nextWord();
      const expr = parseExpr();
      if (!ctx.at(TokenKind.COLON)) error("Expected ':' after match expression");
      ctx.next();
      expectNewline();
      const cases = parseCases();
      const nd = Node.Match(expr, cases);
      const endTok = ctx.tokens[ctx.index - 1] || ctx.peek();
      (nd as any).span = { start: mTok.start, end: endTok.end };
      return nd;
    }
    // Plain bare expression as statement (allow method calls, constructions) ending with '.'
    if (
      ctx.at(TokenKind.IDENT) ||
      ctx.at(TokenKind.TYPE_IDENT) ||
      ctx.at(TokenKind.STRING) ||
      ctx.at(TokenKind.INT) ||
      ctx.at(TokenKind.BOOL) ||
      ctx.at(TokenKind.NULL) ||
      ctx.at(TokenKind.LPAREN)
    ) {
      const exprStart = ctx.index;
      try {
        const _e = parseExpr();
        expectPeriodEnd();
        return _e as Statement; // Not lowering; in v0, only Return statements are valid side-effects.
      } catch {
        // rewind
        ctx.index = exprStart;
      }
    }
    if (ctx.isKeyword(KW.WITHIN)) {
      ctx.nextWord();
      expectKeyword(KW.SCOPE, "Expected 'scope' after 'Within'");
      if (!ctx.at(TokenKind.COLON)) error("Expected ':' after 'scope'");
      ctx.next();
      expectNewline();
      return parseBlock(); // Lowering later
    }
    if (ctx.isKeyword(KW.START)) {
      ctx.nextWord();
      const name = parseIdent();
      expectKeyword(KW.AS, "Expected 'as' after name");
      expectKeyword(KW.ASYNC, "Expected 'async'");
      const expr = parseExpr();
      expectPeriodEnd();
      return Node.Start(name, expr) as Statement;
    }
    if (ctx.isKeywordSeq(KW.WAIT_FOR)) {
      ctx.nextWords(kwParts(KW.WAIT_FOR));
      const names: string[] = [];
      names.push(parseIdent());
      while (ctx.isKeyword(KW.AND) || ctx.at(TokenKind.COMMA)) {
        if (ctx.isKeyword(KW.AND)) {
          ctx.nextWord();
          names.push(parseIdent());
        } else {
          ctx.next();
          names.push(parseIdent());
        }
      }
      expectPeriodEnd();
      return Node.Wait(names) as Statement;
    }

    error('Unknown statement');
  }

  function parseCases(): Case[] {
    const cases: Case[] = [];
    if (!ctx.at(TokenKind.INDENT)) error('Expected indent for cases');
    ctx.next();
    while (!ctx.at(TokenKind.DEDENT)) {
      if (!ctx.isKeyword(KW.WHEN)) error("Expected 'When'");
      ctx.nextWord();
      const pat = parsePattern();
      if (!ctx.at(TokenKind.COMMA)) error("Expected ',' after pattern");
      ctx.next();
      const body = parseCaseBody();
      cases.push(Node.Case(pat, body));
      while (ctx.at(TokenKind.NEWLINE)) ctx.next();
    }
    ctx.next();
    return cases;
  }
  function parseCaseBody(): Block | import('./types.js').Return {
    if (ctx.isKeyword(KW.RETURN)) {
      ctx.nextWord();
      const e = parseExpr();
      expectPeriodEnd();
      return Node.Return(e);
    }
    return parseBlock();
  }

  function parseExpr(): Expression {
    // Operator-name calls like '<(x, y)' or '+(x, y)' or '>(x, y)'
    if (ctx.at(TokenKind.LT) || ctx.at(TokenKind.PLUS) || ctx.at(TokenKind.MINUS) || ctx.at(TokenKind.GT)) {
      const symTok = ctx.next();
      const sym = symTok.kind === TokenKind.LT ? '<' : symTok.kind === TokenKind.GT ? '>' : symTok.kind === TokenKind.PLUS ? '+' : '-';
      if (ctx.at(TokenKind.LPAREN)) {
        const target = Node.Name(sym);
        const args = parseArgList();
        return Node.Call(target, args);
      }
      return Node.Name(sym);
    }
    return parseComparison();
  }

  function parseComparison(): Expression {
    let left = parseAddition();

    let more = true;
    while (more) {
      if (ctx.at(TokenKind.LT)) {
        ctx.next();
        const right = parseAddition();
        left = Node.Call(Node.Name('<'), [left, right]);
      } else if (ctx.at(TokenKind.GT)) {
        ctx.next();
        const right = parseAddition();
        left = Node.Call(Node.Name('>'), [left, right]);
      } else if (ctx.isKeyword(KW.LESS_THAN)) {
        ctx.nextWord();
        const right = parseAddition();
        left = Node.Call(Node.Name('<'), [left, right]);
      } else if (ctx.isKeyword(KW.GREATER_THAN)) {
        ctx.nextWord();
        const right = parseAddition();
        left = Node.Call(Node.Name('>'), [left, right]);
      } else if (ctx.isKeyword(KW.EQUALS_TO)) {
        ctx.nextWord();
        const right = parseAddition();
        left = Node.Call(Node.Name('=='), [left, right]);
      } else {
        more = false;
      }
    }
    return left;
  }

  function parseAddition(): Expression {
    let left = parsePrimary();

    let more = true;
    while (more) {
      if (ctx.at(TokenKind.PLUS)) {
        ctx.next();
        const right = parsePrimary();
        left = Node.Call(Node.Name('+'), [left, right]);
      } else if (ctx.at(TokenKind.MINUS)) {
        ctx.next();
        const right = parsePrimary();
        left = Node.Call(Node.Name('-'), [left, right]);
      } else if (ctx.isKeyword(KW.PLUS)) {
        ctx.nextWord();
        const right = parsePrimary();
        left = Node.Call(Node.Name('+'), [left, right]);
      } else if (ctx.isKeyword(KW.MINUS)) {
        ctx.nextWord();
        const right = parsePrimary();
        left = Node.Call(Node.Name('-'), [left, right]);
      } else {
        more = false;
      }
    }
    return left;
  }

  function parsePrimary(): Expression {
    // Minimal: construction, literals, names, Ok/Err/Some/None, call with dotted names and parens args
    // Lambda (block form): 'a function' (or 'function') ... 'produce' Type ':' \n Block
    if ((ctx.isKeyword('a') && tokLowerAt(ctx.index + 1) === 'function') || ctx.isKeyword('function')) {
      if (ctx.isKeyword('a')) ctx.nextWord(); // optional 'a'
      ctx.nextWord(); // 'function'
      const params = parseParamList();
      expectCommaOr();
      expectKeyword(KW.PRODUCE, "Expected 'produce' and return type");
      const retType = parseType();
      if (!ctx.at(TokenKind.COLON)) error("Expected ':' after return type in lambda");
      ctx.next();
      expectNewline();
      const body = parseBlock();
      return Node.Lambda(params, retType, body);
    }
    // Lambda (short form): (x: Text, y: Int) => expr
    if (ctx.at(TokenKind.LPAREN)) {
      const save = ctx.index;
      try {
        ctx.next();
        const params: Parameter[] = [];
        let first = true;
        while (!ctx.at(TokenKind.RPAREN)) {
          if (!first) {
            if (ctx.at(TokenKind.COMMA)) {
              ctx.next();
            } else {
              throw new Error('comma');
            }
          }
          const pname = parseIdent();
          if (!ctx.at(TokenKind.COLON)) throw new Error('colon');
          ctx.next();
          const ptype = parseType();
          params.push({ name: pname, type: ptype });
          first = false;
        }
        ctx.next(); // consume ')'
        if (!(ctx.at(TokenKind.EQUALS) && ctx.tokens[ctx.index + 1] && ctx.tokens[ctx.index + 1]!.kind === TokenKind.GT)) {
          throw new Error('arrow');
        }
        ctx.next(); // '='
        ctx.next(); // '>'
        // Expression body; infer return type when possible
        const bodyExpr = parseExpr();
        const body = Node.Block([Node.Return(bodyExpr)]);
        const retType = inferLambdaReturnType(bodyExpr);
        return Node.Lambda(params, retType, body);
      } catch {
        // rewind and treat as parenthesized expression
        ctx.index = save;
      }
    }
    if (ctx.isKeywordSeq(KW.OK_OF)) {
      ctx.nextWords(kwParts(KW.OK_OF));
      return Node.Ok(parseExpr());
    }
    if (ctx.isKeywordSeq(KW.ERR_OF)) {
      ctx.nextWords(kwParts(KW.ERR_OF));
      return Node.Err(parseExpr());
    }
    if (ctx.isKeywordSeq(KW.SOME_OF)) {
      ctx.nextWords(kwParts(KW.SOME_OF));
      return Node.Some(parseExpr());
    }
    if (ctx.isKeyword(KW.NONE)) {
      ctx.nextWord();
      return Node.None();
    }
    if (ctx.at(TokenKind.STRING)) return Node.String(ctx.next().value as string);
    if (ctx.at(TokenKind.BOOL)) return Node.Bool(ctx.next().value as boolean);
    if (ctx.at(TokenKind.NULL)) return Node.Null();
    if (ctx.at(TokenKind.INT)) return Node.Int(ctx.next().value as number);
    if (ctx.at(TokenKind.LONG)) return Node.Long(ctx.next().value as number);
    if (ctx.at(TokenKind.FLOAT)) return Node.Double(ctx.next().value as number);
    if (ctx.isKeyword(KW.AWAIT)) {
      ctx.nextWord();
      const args = parseArgList();
      if (args.length !== 1) error('await(expr) takes exactly one argument');
      return Node.Call(Node.Name('await'), args);
    }

    // Parenthesized expressions
    if (ctx.at(TokenKind.LPAREN)) {
      ctx.next();
      const expr = parseExpr();
      if (!ctx.at(TokenKind.RPAREN)) error("Expected ')' after expression");
      ctx.next();
      return expr;
    }

    // Construction: Type with a = expr and b = expr
    if (ctx.at(TokenKind.TYPE_IDENT)) {
      const typeName = ctx.next().value as string;
      if (ctx.isKeyword(KW.WITH)) {
        ctx.nextWord();
        const fields: import('./types.js').ConstructField[] = [];
        let hasMore = true;
        while (hasMore) {
          const nameTok = ctx.peek();
          const name = parseIdent();
          if (!ctx.at(TokenKind.EQUALS)) error("Expected '=' in construction");
          ctx.next();
          const e = parseExpr();
          const fld: import('./types.js').ConstructField = { name, expr: e };
          const endTok = ctx.tokens[ctx.index - 1] || ctx.peek();
          (fld as any).span = { start: nameTok.start, end: endTok.end };
          fields.push(fld);
          if (ctx.isKeyword(KW.AND)) {
            ctx.nextWord();
            continue;
          }
          if (ctx.at(TokenKind.COMMA)) {
            ctx.next();
            continue;
          }
          hasMore = false;
        }
        return Node.Construct(typeName, fields);
      }
      // Dotted chain after TypeIdent (e.g., AuthRepo.verify)
      let full = typeName;
      while (
        ctx.at(TokenKind.DOT) &&
        ctx.tokens[ctx.index + 1] &&
        (ctx.tokens[ctx.index + 1]!.kind === TokenKind.IDENT || ctx.tokens[ctx.index + 1]!.kind === TokenKind.TYPE_IDENT)
      ) {
        ctx.next();
        full += '.' + parseIdent();
      }
      if (ctx.at(TokenKind.LPAREN)) {
        const target = Node.Name(full);
        const args = parseArgList();
        return Node.Call(target, args);
      }
      return Node.Name(full);
    }

    if (ctx.at(TokenKind.IDENT)) {
      const name = parseIdent();
      // dotted chain
      let full = name;
      while (
        ctx.at(TokenKind.DOT) &&
        ctx.tokens[ctx.index + 1] &&
        (ctx.tokens[ctx.index + 1]!.kind === TokenKind.IDENT || ctx.tokens[ctx.index + 1]!.kind === TokenKind.TYPE_IDENT)
      ) {
        ctx.next();
        if (ctx.at(TokenKind.IDENT)) {
          full += '.' + parseIdent();
        } else if (ctx.at(TokenKind.TYPE_IDENT)) {
          full += '.' + ctx.next().value;
        }
      }
      const target = Node.Name(full);
      if (ctx.at(TokenKind.LPAREN)) {
        const args = parseArgList();
        return Node.Call(target, args);
      }
      return target;
    }

    error('Unexpected expression');
  }

  function parseArgList(): Expression[] {
    if (!ctx.at(TokenKind.LPAREN)) error("Expected '('");
    ctx.next();
    const args: Expression[] = [];
    while (!ctx.at(TokenKind.RPAREN)) {
      args.push(parseExpr());
      if (ctx.at(TokenKind.COMMA)) {
        ctx.next();
        continue;
      } else break;
    }
    if (!ctx.at(TokenKind.RPAREN)) error("Expected ')'");
    ctx.next();
    return args;
  }

  function parsePattern(): Pattern {
    if (ctx.isKeyword(KW.NULL) || ctx.at(TokenKind.NULL)) {
      if (ctx.at(TokenKind.NULL)) ctx.next();
      else ctx.nextWord();
      return Node.PatternNull();
    }
    if (ctx.at(TokenKind.INT)) {
      const v = ctx.next().value as number;
      return Node.PatternInt(v);
    }
    if (ctx.at(TokenKind.TYPE_IDENT)) {
      const typeName = ctx.next().value as string;
      if (ctx.at(TokenKind.LPAREN)) {
        ctx.next();
        const names: string[] = [];
        while (!ctx.at(TokenKind.RPAREN)) {
          names.push(parseIdent());
          if (ctx.at(TokenKind.COMMA)) {
            ctx.next();
            continue;
          } else break;
        }
        if (!ctx.at(TokenKind.RPAREN)) error("Expected ')' in pattern");
        ctx.next();
        return Node.PatternCtor(typeName, names);
      }
      // No LPAREN: treat bare TypeIdent as a variant name pattern (enum member)
      return Node.PatternName(typeName);
    }
    const name = parseIdent();
    return Node.PatternName(name);
  }
}
function inferLambdaReturnType(e: Expression): Type {
  switch (e.kind) {
    case 'String':
      return Node.TypeName('Text');
    case 'Int':
      return Node.TypeName('Int');
    case 'Bool':
      return Node.TypeName('Bool');
    case 'Call': {
      if (e.target.kind === 'Name') {
        const n = e.target.name;
        if (n === 'Text.concat') return Node.TypeName('Text');
        if (n === 'Text.length') return Node.TypeName('Int');
        if (n === '+') return Node.TypeName('Int');
        if (n === 'not') return Node.TypeName('Bool');
        if (n === '<' || n === '>' || n === '==') return Node.TypeName('Bool');
      }
      return Node.TypeName('Unknown');
    }
    default:
      return Node.TypeName('Unknown');
  }
}
