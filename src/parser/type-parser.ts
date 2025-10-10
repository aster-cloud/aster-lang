import type { Type } from '../types.js';
import type { ParserContext } from './context.js';
import { kwParts } from './context.js';
import { TokenKind, KW } from '../tokens.js';
import { Node } from '../ast.js';
import { Diagnostics } from '../diagnostics.js';
import { isCapabilityKind, parseLegacyCapability } from '../capabilities.js';
import type { CapabilityKind } from '../config/semantic.js';

/**
 * 解析效果列表（io, cpu, 能力等）
 * @param ctx Parser 上下文
 * @param error 错误报告函数
 * @returns 效果字符串数组
 */
export function parseEffectList(
  ctx: ParserContext,
  error: (msg: string) => never
): string[] {
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
      } else {
        break;
      }
    }
    if (!ctx.at(TokenKind.RBRACKET)) error("Expected ']' after capability list");
    ctx.next(); // consume ']'
  }

  return effs;
}

/**
 * 分离基础效果和能力类型
 * @param effects 效果字符串数组
 * @param error 错误报告函数
 * @returns 基础效果、能力类型和是否有显式能力声明
 */
export function separateEffectsAndCaps(
  effects: string[],
  error: (msg: string) => never
): { baseEffects: string[]; effectCaps: CapabilityKind[]; hasExplicitCaps: boolean } {
  const baseEffects: string[] = [];
  const rawCaps: string[] = [];
  const baseEffectSet = new Set(['io', 'cpu', 'pure']);

  for (const eff of effects) {
    const lower = eff.toLowerCase();
    if (baseEffectSet.has(lower)) {
      baseEffects.push(lower);
      continue;
    }
    rawCaps.push(eff);
  }

  const effectCaps: CapabilityKind[] = [];
  const seenCaps = new Set<CapabilityKind>();
  const appendCaps = (caps: readonly CapabilityKind[]): void => {
    for (const cap of caps) {
      if (seenCaps.has(cap)) continue;
      seenCaps.add(cap);
      effectCaps.push(cap);
    }
  };

  if (rawCaps.length > 0) {
    for (const capText of rawCaps) {
      if (isCapabilityKind(capText)) {
        appendCaps([capText as CapabilityKind]);
        continue;
      }
      error(`Unknown capability '${capText}'`);
    }
  } else {
    for (const eff of baseEffects) {
      if (eff === 'io' || eff === 'cpu') {
        appendCaps(parseLegacyCapability(eff));
      }
    }
  }

  return { baseEffects, effectCaps, hasExplicitCaps: rawCaps.length > 0 };
}

/**
 * 解析类型表达式
 * @param ctx Parser 上下文
 * @param error 错误报告函数
 * @returns 类型节点
 */
export function parseType(
  ctx: ParserContext,
  error: (msg: string) => never
): Type {
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
    return applyAnnotations(Node.Maybe(parseType(ctx, error)));
  }
  if (ctx.isKeywordSeq(KW.OPTION_OF)) {
    ctx.nextWords(kwParts(KW.OPTION_OF));
    return applyAnnotations(Node.Option(parseType(ctx, error)));
  }
  if (ctx.isKeywordSeq(KW.RESULT_OF)) {
    ctx.nextWords(kwParts(KW.RESULT_OF));
    const ok = parseType(ctx, error);
    // Accept 'or' or 'and' between ok and err
    if (ctx.isKeyword(KW.OR) || ctx.isKeyword(KW.AND)) ctx.nextWord();
    else Diagnostics.expectedKeyword('or/and', ctx.peek().start).withMessage("Expected 'or'/'and' in Result of").throw();
    const err = parseType(ctx, error);
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
    return applyAnnotations(Node.List(parseType(ctx, error)));
  }
  if (ctx.isKeyword('map')) {
    ctx.nextWord();
    const k = parseType(ctx, error);
    if (!ctx.isKeyword(KW.TO_WORD)) {
      Diagnostics.expectedKeyword(KW.TO_WORD, ctx.peek().start).withMessage("Expected 'to' in map type").throw();
    }
    ctx.nextWord();
    const v = parseType(ctx, error);
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
        args.push(parseType(ctx, error));
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
