/**
 * 顶层声明解析器
 * 负责解析数据类型定义（Define）和函数定义（To）
 */

import { KW, TokenKind } from '../tokens.js';
import { Node } from '../ast.js';
import type { Block, Declaration, Token, Type } from '../types.js';
import type { ParserContext } from './context.js';
import { kwParts, tokLowerAt } from './context.js';
import { parseType, parseEffectList, separateEffectsAndCaps } from './type-parser.js';
import { parseBlock, parseParamList } from './expr-stmt-parser.js';
import { parseFieldList, parseVariantList } from './field-variant-parser.js';

/**
 * 解析数据类型定义
 * 语法: Define User with name: Text and age: Int.
 *
 * @param ctx 解析器上下文
 * @param error 错误报告函数
 * @param expectDot 期望点号的辅助函数
 * @param parseTypeIdent 解析类型标识符的辅助函数
 * @returns 数据类型声明
 */
export function parseDataDecl(
  ctx: ParserContext,
  error: (msg: string, tok?: Token) => never,
  expectDot: () => void,
  parseTypeIdent: () => string
): Declaration {
  // 期望: Define
  ctx.nextWord();

  // 解析类型名
  const typeName = parseTypeIdent();

  // 期望: with
  if (!ctx.isKeywordSeq(KW.WITH)) {
    error("Expected 'with' after type name in data definition");
  }
  ctx.nextWord();

  // 解析字段列表
  const fields = parseFieldList(ctx, error);

  // 期望句点结束
  expectDot();

  // 创建 Data 节点并注册类型
  const dataDecl = Node.Data(typeName, fields);
  ctx.declaredTypes.add(typeName);

  return dataDecl;
}

/**
 * 解析枚举类型定义
 * 语法: Define Status as one of Success, Failure, Pending.
 *
 * @param ctx 解析器上下文
 * @param error 错误报告函数
 * @param expectDot 期望点号的辅助函数
 * @param parseTypeIdent 解析类型标识符的辅助函数
 * @returns 枚举类型声明
 */
export function parseEnumDecl(
  ctx: ParserContext,
  error: (msg: string, tok?: Token) => never,
  expectDot: () => void,
  parseTypeIdent: () => string
): Declaration {
  // 期望: Define
  ctx.nextWord();

  // 解析类型名
  const typeName = parseTypeIdent();

  // 期望: as one of
  if (!ctx.isKeywordSeq(KW.ONE_OF)) {
    error("Expected 'as one of' after type name in enum definition");
  }
  ctx.nextWords(kwParts(KW.ONE_OF));

  // 解析变体列表
  const variants = parseVariantList(ctx, error);

  // 期望句点结束
  expectDot();

  // 创建 Enum 节点并附加变体 spans
  const en = Node.Enum(typeName, variants);
  const spans = (parseVariantList as any)._lastSpans as import('../types.js').Span[] | undefined;
  if (spans && Array.isArray(spans)) {
    (en as any).variantSpans = spans;
  }
  ctx.declaredTypes.add(typeName);

  return en;
}

/**
 * 解析函数定义
 * 语法: To greet with name: Text, produce Text. It performs io: ...
 *
 * @param ctx 解析器上下文
 * @param error 错误报告函数
 * @param expectCommaOr 期望逗号或允许省略的辅助函数
 * @param expectKeyword 期望关键字的辅助函数
 * @param expectNewline 期望换行的辅助函数
 * @param parseIdent 解析标识符的辅助函数
 * @returns 函数声明
 */
export function parseFuncDecl(
  ctx: ParserContext,
  error: (msg: string, tok?: Token) => never,
  expectCommaOr: () => void,
  expectKeyword: (kw: string, msg: string) => void,
  expectNewline: () => void,
  parseIdent: () => string
): Declaration {
  // 记录函数起始位置
  const toTok = ctx.peek();
  ctx.nextWord(); // 消费 'To'

  // 记录函数名位置
  const nameTok = ctx.peek();
  const name = parseIdent();

  // 解析可选的类型参数: 'of' TypeId ('and' TypeId)*
  let typeParams: string[] = [];
  if (ctx.isKeyword('of')) {
    ctx.nextWord();
    let more = true;
    while (more) {
      // 如果遇到参数列表或 produce 子句，停止
      if (ctx.isKeyword(KW.WITH) || ctx.isKeyword(KW.PRODUCE) || ctx.at(TokenKind.COLON)) {
        break;
      }
      // 解析类型变量名（优先 TYPE_IDENT，回退到 IDENT）
      const tv = ctx.at(TokenKind.TYPE_IDENT)
        ? (ctx.next().value as string)
        : parseIdent();
      typeParams.push(tv);

      if (ctx.isKeyword(KW.AND)) {
        ctx.nextWord();
        continue;
      }
      if (ctx.at(TokenKind.COMMA)) {
        ctx.next();
        // 如果逗号后面跟 'with' 或 produce，停止
        if (ctx.isKeyword(KW.WITH) || ctx.isKeyword(KW.PRODUCE)) {
          more = false;
          break;
        }
        continue;
      }
      more = false;
    }
  }

  // 保存当前类型变量作用域，设置新的作用域
  const savedTypeVars = new Set(ctx.currentTypeVars);
  ctx.currentTypeVars = new Set(typeParams);

  // 解析参数列表
  const params = parseParamList(ctx, error);
  if (params.length > 0) expectCommaOr();
  else if (ctx.at(TokenKind.COMMA)) ctx.next();

  // 期望 'produce' 和返回类型
  expectKeyword(KW.PRODUCE, "Expected 'produce' and return type");
  const retType = parseType(ctx, error);

  let effects: string[] = [];
  // 准备收集函数体内的效果声明
  const prevCollected: string[] | null = ctx.collectedEffects;
  ctx.collectedEffects = [];
  let body: Block | null = null;

  // 解析效果声明和函数体
  // 场景1: produce Type. It performs io: ...
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
        // 如果 parseBlock 没有消费 DEDENT（多行参数情况），这里消费它
        if (ctx.at(TokenKind.DEDENT)) {
          ctx.next();
        }
      } else {
        error("Expected '.' or ':' after effect clause");
      }
    }
  }
  // 场景2: produce Type. It performs io: ...（内联效果）
  else if (
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
      // 如果 parseBlock 没有消费 DEDENT（多行参数情况），这里消费它
      if (ctx.at(TokenKind.DEDENT)) {
        ctx.next();
      }
    } else {
      error("Expected '.' or ':' after effect clause");
    }
  }
  // 场景3: produce Type: ...（直接进入函数体）
  else if (ctx.at(TokenKind.COLON)) {
    ctx.next();
    expectNewline();
    body = parseBlock(ctx, error);
    // 如果 parseBlock 没有消费 DEDENT（多行参数情况），这里消费它
    if (ctx.at(TokenKind.DEDENT)) {
      ctx.next();
    }
  } else {
    error("Expected '.' or ':' after return type");
  }

  // 如果没有显式声明类型参数，尝试从类型使用中推断
  if (typeParams.length === 0) {
    const BUILTINS = new Set(['Int', 'Bool', 'Text', 'Long', 'Double', 'Number', 'Float', 'Option', 'Result', 'List', 'Map']);
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

  // 合并函数体内收集的效果
  if (Array.isArray(ctx.collectedEffects) && ctx.collectedEffects.length > 0) {
    effects = effects.concat(ctx.collectedEffects);
  }

  // 恢复效果收集器和类型参数作用域
  ctx.collectedEffects = prevCollected;
  ctx.currentTypeVars = savedTypeVars;

  // 分离基本效果和能力约束
  const { baseEffects, effectCaps, hasExplicitCaps } = separateEffectsAndCaps(effects, error);

  // 创建函数节点并附加元数据
  const fn = Node.Func(name, typeParams, params, retType, baseEffects, body);
  (fn as any).span = { start: toTok.start, end: endTok.end };
  // 记录函数名 span 用于精确导航/高亮
  (fn as any).nameSpan = { start: nameTok.start, end: (ctx.tokens[ctx.index - 1] || nameTok).end };
  // 附加能力元数据（如果存在）
  if (effectCaps.length > 0) {
    (fn as any).effectCaps = effectCaps;
    (fn as any).effectCapsExplicit = hasExplicitCaps;
  }

  return fn;
}
