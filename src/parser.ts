import {KW, TokenKind} from './tokens.js';
import {Node} from './ast.js';
import {ConfigService} from './config/config-service.js';
import type {
    Block,
    Declaration,
    Field,
    Module,
    Token,
    Type,
} from './types.js';
import {Diagnostics} from './diagnostics.js';
import {createLogger} from './utils/logger.js';
import type {ParserContext} from './parser/context.js';
import {kwParts, tokLowerAt} from './parser/context.js';
import {parseType, parseEffectList, separateEffectsAndCaps} from './parser/type-parser.js';
import {
  parseBlock,
  parseParamList as parseParamListExternal,
} from './parser/expr-stmt-parser.js';

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
      enabled: ConfigService.getInstance().debugTypes,
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
      const v = tokLowerAt(ctx, ctx.index);
      return v === kw;
    },
    isKeywordSeq: (words: string | string[]): boolean => {
      const ws = Array.isArray(words) ? words : kwParts(words);
      for (let k = 0; k < ws.length; k++) {
        const v = tokLowerAt(ctx, ctx.index + k);
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
      ctx.moduleName = parseDottedIdent();;
      expectDot();
    } else if (ctx.isKeyword(KW.USE)) {
      ctx.nextWord();
      const name = parseDottedIdent();
      let asName: string | null = null;
      if (ctx.isKeyword(KW.AS)) {
        ctx.nextWord();
        // 允许别名为普通标识符或类型标识符（如：use Http as H.）
        if (ctx.at(TokenKind.TYPE_IDENT)) {
          asName = ctx.next().value as string;
        } else {
          asName = parseIdent();
        }
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
      const params = parseParamListExternal(ctx, error);
      if (params.length > 0) expectCommaOr();
      else if (ctx.at(TokenKind.COMMA)) ctx.next();
      expectKeyword(KW.PRODUCE, "Expected 'produce' and return type");
      const retType = parseType(ctx, error);
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
          (tokLowerAt(ctx, ctx.index) === 'it' && tokLowerAt(ctx, ctx.index + 1) === 'performs')
        ) {
          if (!ctx.isKeywordSeq(KW.PERFORMS)) ctx.nextWord();
          ctx.nextWords(kwParts(KW.PERFORMS));
          effects = parseEffectList(ctx, error);
          if (ctx.at(TokenKind.DOT)) {
            ctx.next();
          } else if (ctx.at(TokenKind.COLON)) {
            ctx.next();
            expectNewline();
            body = parseBlock(ctx, error);
          } else {
            error("Expected '.' or ':' after effect clause");
          }
        }
      } else if (
        ctx.isKeywordSeq(KW.PERFORMS) ||
        (tokLowerAt(ctx, ctx.index) === 'it' && tokLowerAt(ctx, ctx.index + 1) === 'performs')
      ) {
        if (!ctx.isKeywordSeq(KW.PERFORMS)) ctx.nextWord();
        ctx.nextWords(kwParts(KW.PERFORMS));
        effects = parseEffectList(ctx, error);
        if (ctx.at(TokenKind.DOT)) {
          ctx.next();
        } else if (ctx.at(TokenKind.COLON)) {
          ctx.next();
          expectNewline();
          body = parseBlock(ctx, error);
        } else {
          error("Expected '.' or ':' after effect clause");
        }
      } else if (ctx.at(TokenKind.COLON)) {
        ctx.next();
        expectNewline();
        body = parseBlock(ctx, error);
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
      const { baseEffects, effectCaps, hasExplicitCaps } = separateEffectsAndCaps(effects, error);

      const fn = Node.Func(name, typeParams, params, retType, baseEffects, body);
      (fn as any).span = { start: toTok.start, end: endTok.end };
      // Record function name span for precise navigation/highlighting
      ;(fn as any).nameSpan = { start: nameTok.start, end: (ctx.tokens[ctx.index - 1] || nameTok).end };
      // Attach capability metadata if present
      if (effectCaps.length > 0) {
        (fn as any).effectCaps = effectCaps;
        (fn as any).effectCapsExplicit = hasExplicitCaps;
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
    // 允许点号分隔的标识符首段为普通标识符或类型标识符
    const parts: string[] = [];
    if (ctx.at(TokenKind.IDENT)) {
      parts.push(parseIdent());
    } else if (ctx.at(TokenKind.TYPE_IDENT)) {
      parts.push(ctx.next().value as string);
    } else {
      Diagnostics.expectedIdentifier(ctx.peek().start).throw();
    }
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
      const t = parseType(ctx, error);
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
}
