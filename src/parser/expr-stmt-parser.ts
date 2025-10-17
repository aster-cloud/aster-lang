import type { Block, Case, Expression, Parameter, Pattern, Statement, Type } from '../types.js';
import type { ParserContext } from './context.js';
import { kwParts, tokLowerAt } from './context.js';
import { TokenKind, KW } from '../tokens.js';
import { Node } from '../ast.js';
import { parseType, parseEffectList } from './type-parser.js';
import { parseAnnotations } from './annotation-parser.js';

/**
 * 解析代码块（Block）
 * @param ctx Parser 上下文
 * @param error 错误报告函数
 * @returns Block 节点
 */
export function parseBlock(
  ctx: ParserContext,
  error: (msg: string) => never
): Block {
  const statements: Statement[] = [];
  ctx.consumeNewlines();
  // Check if we have an INDENT token (new indented block)
  const hasIndent = ctx.at(TokenKind.INDENT);

  if (hasIndent) {
    // Standard indented block: consume INDENT and parse until DEDENT
    ctx.next();
    while (!ctx.at(TokenKind.DEDENT) && !ctx.at(TokenKind.EOF)) {
      ctx.consumeNewlines();
      if (ctx.at(TokenKind.DEDENT) || ctx.at(TokenKind.EOF)) break;
      statements.push(parseStatement(ctx, error));
      ctx.consumeNewlines();
    }
    if (!ctx.at(TokenKind.DEDENT)) error('Expected dedent');
    const endTok = ctx.peek();
    ctx.next();
    const b = Node.Block(statements);
    const startSpan = (statements[0] as any)?.span?.start || endTok.start;
    (b as any).span = { start: startSpan, end: endTok.end };
    return b;
  } else {
    // Already in an indented context (multi-line parameters case):
    // Parse statements until we hit DEDENT or EOF
    const startTok = ctx.peek();
    while (!ctx.at(TokenKind.DEDENT) && !ctx.at(TokenKind.EOF)) {
      ctx.consumeNewlines();
      if (ctx.at(TokenKind.DEDENT) || ctx.at(TokenKind.EOF)) break;
      statements.push(parseStatement(ctx, error));
      ctx.consumeNewlines();
    }
    if (statements.length === 0) error('Expected at least one statement in function body');
    const endTok = ctx.tokens[ctx.index - 1] || startTok;
    const b = Node.Block(statements);
    const startSpan = (statements[0] as any)?.span?.start || startTok.start;
    (b as any).span = { start: startSpan, end: endTok.end };
    // Don't consume DEDENT here - let the caller handle it
    return b;
  }
}

/**
 * 期望语句以句号结束
 * @param ctx Parser 上下文
 * @param error 错误报告函数
 */
function expectPeriodEnd(
  ctx: ParserContext,
  error: (msg: string) => never
): void {
  if (!ctx.at(TokenKind.DOT)) error("Expected '.' at end of statement");
  ctx.next();
}

/**
 * 期望语句以句号或换行符结束
 * @param ctx Parser 上下文
 * @param error 错误报告函数
 */
function expectPeriodEndOrLine(
  ctx: ParserContext,
  error: (msg: string) => never
): void {
  if (ctx.at(TokenKind.DOT)) {
    ctx.next();
    return;
  }
  // Tolerate newline/dedent/EOF terminators inside blocks for certain statements (e.g., Return)
  if (ctx.at(TokenKind.NEWLINE) || ctx.at(TokenKind.DEDENT) || ctx.at(TokenKind.EOF)) return;
  error("Expected '.' at end of statement");
}

/**
 * 解析语句
 * @param ctx Parser 上下文
 * @param error 错误报告函数
 * @returns Statement 节点
 */
export function parseStatement(
  ctx: ParserContext,
  error: (msg: string) => never
): Statement {
  if (ctx.isKeyword(KW.LET)) {
    const letTok = ctx.peek();
    ctx.nextWord();
    const name = parseIdent(ctx, error);
    expectKeyword(ctx, error, KW.BE, "Use 'be' in bindings: 'Let x be ...'.");
    // Special-case lambda block form to avoid trailing '.'
    if ((ctx.isKeyword('a') && tokLowerAt(ctx, ctx.index + 1) === 'function') || ctx.isKeyword('function')) {
      if (ctx.isKeyword('a')) ctx.nextWord(); // optional 'a'
      ctx.nextWord(); // 'function'
      const params = parseParamList(ctx, error);
      expectCommaOr(ctx);
      expectKeyword(ctx, error, KW.PRODUCE, "Expected 'produce' and return type");
      const retType = parseType(ctx, error);
      if (!ctx.at(TokenKind.COLON)) error("Expected ':' after return type in lambda");
      ctx.next();
      expectNewline(ctx, error);
      const body = parseBlock(ctx, error);
      const nd = Node.Let(name, Node.Lambda(params, retType, body));
      const endTok = ctx.tokens[ctx.index - 1] || ctx.peek();
      (nd as any).span = { start: letTok.start, end: endTok.end };
      return nd;
    }
    const expr = parseExpr(ctx, error);
    expectPeriodEnd(ctx, error);
    const nd = Node.Let(name, expr);
    const endTok = ctx.tokens[ctx.index - 1] || ctx.peek();
    (nd as any).span = { start: letTok.start, end: endTok.end };
    return nd;
  }
  if (ctx.isKeyword(KW.SET)) {
    const setTok = ctx.peek();
    ctx.nextWord();
    const name = parseIdent(ctx, error);
    expectKeyword(ctx, error, KW.TO_WORD, "Use 'to' in assignments: 'Set x to ...'.");
    const expr = parseExpr(ctx, error);
    expectPeriodEnd(ctx, error);
    const nd = Node.Set(name, expr);
    const endTok = ctx.tokens[ctx.index - 1] || ctx.peek();
    (nd as any).span = { start: setTok.start, end: endTok.end };
    return nd;
  }
  if (ctx.isKeyword(KW.RETURN)) {
    const retTok = ctx.peek();
    ctx.nextWord();
    const expr = parseExpr(ctx, error);
    expectPeriodEndOrLine(ctx, error);
    // Allow trailing effect sentence immediately after a Return: 'It performs io.'
    // This attaches to the enclosing function's effects if present.
    if ((tokLowerAt(ctx, ctx.index) === 'it' && tokLowerAt(ctx, ctx.index + 1) === 'performs') || tokLowerAt(ctx, ctx.index) === 'performs') {
      if (tokLowerAt(ctx, ctx.index) === 'it') ctx.nextWord();
      if (tokLowerAt(ctx, ctx.index) === 'performs') {
        ctx.nextWord();
        const effs = parseEffectList(ctx, error);
        expectPeriodEnd(ctx, error);
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
    const args = parseArgList(ctx, error);
    if (args.length !== 1) error('await(expr) takes exactly one argument');
    const aw = Node.Call(Node.Name('await'), args);
    expectPeriodEnd(ctx, error);
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
    const cond = parseExpr(ctx, error);
    const condExpr = negate ? Node.Call(Node.Name('not'), [cond]) : cond;
    if (!ctx.at(TokenKind.COMMA)) error("Expected ',' after condition");
    ctx.next();
    if (!ctx.at(TokenKind.COLON)) error("Expected ':' after ',' in If");
    ctx.next();
    expectNewline(ctx, error);
    const thenBlock = parseBlock(ctx, error);
    let elseBlock: Block | null = null;
    if (ctx.isKeyword(KW.OTHERWISE)) {
      ctx.nextWord();
      if (!ctx.at(TokenKind.COMMA)) error("Expected ',' after 'Otherwise'");
      ctx.next();
      if (!ctx.at(TokenKind.COLON)) error("Expected ':' after ',' in Otherwise");
      ctx.next();
      expectNewline(ctx, error);
      elseBlock = parseBlock(ctx, error);
    }
    const nd = Node.If(condExpr, thenBlock, elseBlock);
    const endTok = ctx.tokens[ctx.index - 1] || ctx.peek();
    (nd as any).span = { start: ifTok.start, end: endTok.end };
    return nd;
  }
  if (ctx.isKeyword(KW.MATCH)) {
    const mTok = ctx.peek();
    ctx.nextWord();
    const expr = parseExpr(ctx, error);
    if (!ctx.at(TokenKind.COLON)) error("Expected ':' after match expression");
    ctx.next();
    expectNewline(ctx, error);
    const cases = parseCases(ctx, error);
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
      const _e = parseExpr(ctx, error);
      expectPeriodEnd(ctx, error);
      return _e as Statement; // Not lowering; in v0, only Return statements are valid side-effects.
    } catch {
      // rewind
      ctx.index = exprStart;
    }
  }
  if (ctx.isKeyword(KW.WITHIN)) {
    ctx.nextWord();
    expectKeyword(ctx, error, KW.SCOPE, "Expected 'scope' after 'Within'");
    if (!ctx.at(TokenKind.COLON)) error("Expected ':' after 'scope'");
    ctx.next();
    expectNewline(ctx, error);
    return parseBlock(ctx, error); // Lowering later
  }
  if (ctx.isKeyword(KW.START)) {
    ctx.nextWord();
    const name = parseIdent(ctx, error);
    expectKeyword(ctx, error, KW.AS, "Expected 'as' after name");
    expectKeyword(ctx, error, KW.ASYNC, "Expected 'async'");
    const expr = parseExpr(ctx, error);
    expectPeriodEnd(ctx, error);
    return Node.Start(name, expr) as Statement;
  }
  if (ctx.isKeywordSeq(KW.WAIT_FOR)) {
    ctx.nextWords(kwParts(KW.WAIT_FOR));
    const names: string[] = [];
    names.push(parseIdent(ctx, error));
    while (ctx.isKeyword(KW.AND) || ctx.at(TokenKind.COMMA)) {
      if (ctx.isKeyword(KW.AND)) {
        ctx.nextWord();
        names.push(parseIdent(ctx, error));
      } else {
        ctx.next();
        names.push(parseIdent(ctx, error));
      }
    }
    expectPeriodEnd(ctx, error);
    return Node.Wait(names) as Statement;
  }

  error('Unknown statement');
}

/**
 * 解析 Match 语句的 Case 列表
 * @param ctx Parser 上下文
 * @param error 错误报告函数
 * @returns Case 节点数组
 */
function parseCases(
  ctx: ParserContext,
  error: (msg: string) => never
): Case[] {
  const cases: Case[] = [];
  if (!ctx.at(TokenKind.INDENT)) error('Expected indent for cases');
  ctx.next();
  while (!ctx.at(TokenKind.DEDENT)) {
    if (!ctx.isKeyword(KW.WHEN)) error("Expected 'When'");
    ctx.nextWord();
    const pat = parsePattern(ctx, error);
    if (!ctx.at(TokenKind.COMMA)) error("Expected ',' after pattern");
    ctx.next();
    const body = parseCaseBody(ctx, error);
    cases.push(Node.Case(pat, body));
    while (ctx.at(TokenKind.NEWLINE)) ctx.next();
  }
  ctx.next();
  return cases;
}

/**
 * 解析 Case 的 Body 部分（可以是 Return 语句或 Block）
 * @param ctx Parser 上下文
 * @param error 错误报告函数
 * @returns Block 或 Return 节点
 */
function parseCaseBody(
  ctx: ParserContext,
  error: (msg: string) => never
): Block | import('../types.js').Return {
  if (ctx.isKeyword(KW.RETURN)) {
    ctx.nextWord();
    const e = parseExpr(ctx, error);
    expectPeriodEnd(ctx, error);
    return Node.Return(e);
  }
  return parseBlock(ctx, error);
}

/**
 * 解析表达式
 * @param ctx Parser 上下文
 * @param error 错误报告函数
 * @returns Expression 节点
 */
export function parseExpr(
  ctx: ParserContext,
  error: (msg: string) => never
): Expression {
  // Operator-name calls like '<(x, y)' or '+(x, y)' or '*(x, y)' or '/(x, y)' or '>(x, y)' or '>=(x, y)' or '<=(x, y)' or '!=(x, y)' or '=(x, y)'
  if (ctx.at(TokenKind.LT) || ctx.at(TokenKind.PLUS) || ctx.at(TokenKind.MINUS) || ctx.at(TokenKind.STAR) || ctx.at(TokenKind.SLASH) || ctx.at(TokenKind.GT) || ctx.at(TokenKind.GTE) || ctx.at(TokenKind.LTE) || ctx.at(TokenKind.NEQ) || ctx.at(TokenKind.EQUALS)) {
    const symTok = ctx.next();
    const sym = symTok.kind === TokenKind.LT ? '<' : symTok.kind === TokenKind.GT ? '>' : symTok.kind === TokenKind.GTE ? '>=' : symTok.kind === TokenKind.LTE ? '<=' : symTok.kind === TokenKind.NEQ ? '!=' : symTok.kind === TokenKind.EQUALS ? '=' : symTok.kind === TokenKind.PLUS ? '+' : symTok.kind === TokenKind.MINUS ? '-' : symTok.kind === TokenKind.STAR ? '*' : '/';
    if (ctx.at(TokenKind.LPAREN)) {
      const target = Node.Name(sym);
      const args = parseArgList(ctx, error);
      return Node.Call(target, args);
    }
    return Node.Name(sym);
  }
  return parseComparison(ctx, error);
}

/**
 * 解析比较表达式
 * @param ctx Parser 上下文
 * @param error 错误报告函数
 * @returns Expression 节点
 */
function parseComparison(
  ctx: ParserContext,
  error: (msg: string) => never
): Expression {
  let left = parseAddition(ctx, error);

  let more = true;
  while (more) {
    if (ctx.at(TokenKind.LT)) {
      ctx.next();
      const right = parseAddition(ctx, error);
      left = Node.Call(Node.Name('<'), [left, right]);
    } else if (ctx.at(TokenKind.GT)) {
      ctx.next();
      const right = parseAddition(ctx, error);
      left = Node.Call(Node.Name('>'), [left, right]);
    } else if (ctx.at(TokenKind.LTE)) {
      ctx.next();
      const right = parseAddition(ctx, error);
      left = Node.Call(Node.Name('<='), [left, right]);
    } else if (ctx.at(TokenKind.GTE)) {
      ctx.next();
      const right = parseAddition(ctx, error);
      left = Node.Call(Node.Name('>='), [left, right]);
    } else if (ctx.at(TokenKind.NEQ)) {
      ctx.next();
      const right = parseAddition(ctx, error);
      left = Node.Call(Node.Name('!='), [left, right]);
    } else if (ctx.isKeyword(KW.LESS_THAN)) {
      ctx.nextWord();
      const right = parseAddition(ctx, error);
      left = Node.Call(Node.Name('<'), [left, right]);
    } else if (ctx.isKeyword(KW.GREATER_THAN)) {
      ctx.nextWord();
      const right = parseAddition(ctx, error);
      left = Node.Call(Node.Name('>'), [left, right]);
    } else if (ctx.isKeyword(KW.EQUALS_TO)) {
      ctx.nextWord();
      const right = parseAddition(ctx, error);
      left = Node.Call(Node.Name('=='), [left, right]);
    } else {
      more = false;
    }
  }
  return left;
}

/**
 * 解析加减法表达式
 * @param ctx Parser 上下文
 * @param error 错误报告函数
 * @returns Expression 节点
 */
function parseAddition(
  ctx: ParserContext,
  error: (msg: string) => never
): Expression {
  let left = parseMultiplication(ctx, error);

  let more = true;
  while (more) {
    if (ctx.at(TokenKind.PLUS)) {
      ctx.next();
      const right = parseMultiplication(ctx, error);
      left = Node.Call(Node.Name('+'), [left, right]);
    } else if (ctx.at(TokenKind.MINUS)) {
      ctx.next();
      const right = parseMultiplication(ctx, error);
      left = Node.Call(Node.Name('-'), [left, right]);
    } else if (ctx.isKeyword(KW.PLUS)) {
      ctx.nextWord();
      const right = parseMultiplication(ctx, error);
      left = Node.Call(Node.Name('+'), [left, right]);
    } else if (ctx.isKeyword(KW.MINUS)) {
      ctx.nextWord();
      const right = parseMultiplication(ctx, error);
      left = Node.Call(Node.Name('-'), [left, right]);
    } else {
      more = false;
    }
  }
  return left;
}

/**
 * 解析乘除法表达式
 * @param ctx Parser 上下文
 * @param error 错误报告函数
 * @returns Expression 节点
 */
function parseMultiplication(
  ctx: ParserContext,
  error: (msg: string) => never
): Expression {
  let left = parsePrimary(ctx, error);

  let more = true;
  while (more) {
    if (ctx.at(TokenKind.STAR)) {
      ctx.next();
      const right = parsePrimary(ctx, error);
      left = Node.Call(Node.Name('*'), [left, right]);
    } else if (ctx.at(TokenKind.SLASH)) {
      ctx.next();
      const right = parsePrimary(ctx, error);
      left = Node.Call(Node.Name('/'), [left, right]);
    } else {
      more = false;
    }
  }
  return left;
}

/**
 * 解析基础表达式
 * @param ctx Parser 上下文
 * @param error 错误报告函数
 * @returns Expression 节点
 */
function parsePrimary(
  ctx: ParserContext,
  error: (msg: string) => never
): Expression {
  // Minimal: construction, literals, names, Ok/Err/Some/None, call with dotted names and parens args
  // Lambda (block form): 'a function' (or 'function') ... 'produce' Type ':' \n Block
  if ((ctx.isKeyword('a') && tokLowerAt(ctx, ctx.index + 1) === 'function') || ctx.isKeyword('function')) {
    if (ctx.isKeyword('a')) ctx.nextWord(); // optional 'a'
    ctx.nextWord(); // 'function'
    const params = parseParamList(ctx, error);
    expectCommaOr(ctx);
    expectKeyword(ctx, error, KW.PRODUCE, "Expected 'produce' and return type");
    const retType = parseType(ctx, error);
    if (!ctx.at(TokenKind.COLON)) error("Expected ':' after return type in lambda");
    ctx.next();
    expectNewline(ctx, error);
    const body = parseBlock(ctx, error);
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
        const pname = parseIdent(ctx, error);
        if (!ctx.at(TokenKind.COLON)) throw new Error('colon');
        ctx.next();
        const ptype = parseType(ctx, error);
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
      const bodyExpr = parseExpr(ctx, error);
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
    return Node.Ok(parseExpr(ctx, error));
  }
  if (ctx.isKeywordSeq(KW.ERR_OF)) {
    ctx.nextWords(kwParts(KW.ERR_OF));
    return Node.Err(parseExpr(ctx, error));
  }
  if (ctx.isKeywordSeq(KW.SOME_OF)) {
    ctx.nextWords(kwParts(KW.SOME_OF));
    return Node.Some(parseExpr(ctx, error));
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
    const args = parseArgList(ctx, error);
    if (args.length !== 1) error('await(expr) takes exactly one argument');
    return Node.Call(Node.Name('await'), args);
  }

  // Parenthesized expressions
  if (ctx.at(TokenKind.LPAREN)) {
    ctx.next();
    const expr = parseExpr(ctx, error);
    if (!ctx.at(TokenKind.RPAREN)) error("Expected ')' after expression");
    ctx.next();
    return expr;
  }

  // Construction: Type with a = expr and b = expr
  if (ctx.at(TokenKind.TYPE_IDENT)) {
    const typeName = ctx.next().value as string;
    if (ctx.isKeyword(KW.WITH)) {
      ctx.nextWord();
      const fields: import('../types.js').ConstructField[] = [];
      let hasMore = true;
      while (hasMore) {
        const nameTok = ctx.peek();
        const name = parseIdent(ctx, error);
        if (!ctx.at(TokenKind.EQUALS)) error("Expected '=' in construction");
        ctx.next();
        const e = parseExpr(ctx, error);
        const fld: import('../types.js').ConstructField = { name, expr: e };
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
      full += '.' + parseIdent(ctx, error);
    }
    if (ctx.at(TokenKind.LPAREN)) {
      const target = Node.Name(full);
      const args = parseArgList(ctx, error);
      return Node.Call(target, args);
    }
    return Node.Name(full);
  }

  if (ctx.at(TokenKind.IDENT)) {
    const name = parseIdent(ctx, error);
    // dotted chain
    let full = name;
    while (
      ctx.at(TokenKind.DOT) &&
      ctx.tokens[ctx.index + 1] &&
      (ctx.tokens[ctx.index + 1]!.kind === TokenKind.IDENT || ctx.tokens[ctx.index + 1]!.kind === TokenKind.TYPE_IDENT)
    ) {
      ctx.next();
      if (ctx.at(TokenKind.IDENT)) {
        full += '.' + parseIdent(ctx, error);
      } else if (ctx.at(TokenKind.TYPE_IDENT)) {
        full += '.' + ctx.next().value;
      }
    }
    const target = Node.Name(full);
    if (ctx.at(TokenKind.LPAREN)) {
      const args = parseArgList(ctx, error);
      return Node.Call(target, args);
    }
    return target;
  }

  error('Unexpected expression');
}

/**
 * 解析参数列表
 * @param ctx Parser 上下文
 * @param error 错误报告函数
 * @returns Expression 数组
 */
export function parseArgList(
  ctx: ParserContext,
  error: (msg: string) => never
): Expression[] {
  if (!ctx.at(TokenKind.LPAREN)) error("Expected '('");
  ctx.next();
  const args: Expression[] = [];
  while (!ctx.at(TokenKind.RPAREN)) {
    args.push(parseExpr(ctx, error));
    if (ctx.at(TokenKind.COMMA)) {
      ctx.next();
      continue;
    } else break;
  }
  if (!ctx.at(TokenKind.RPAREN)) error("Expected ')'");
  ctx.next();
  return args;
}

/**
 * 解析模式（Pattern）
 * @param ctx Parser 上下文
 * @param error 错误报告函数
 * @returns Pattern 节点
 */
export function parsePattern(
  ctx: ParserContext,
  error: (msg: string) => never
): Pattern {
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
        names.push(parseIdent(ctx, error));
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
  const name = parseIdent(ctx, error);
  return Node.PatternName(name);
}

/**
 * 解析参数列表（函数声明）
 * @param ctx Parser 上下文
 * @param error 错误报告函数
 * @returns Parameter 数组
 */
export function parseParamList(
  ctx: ParserContext,
  error: (msg: string, tok?: import('../types.js').Token) => never
): Parameter[] {
  const params: Parameter[] = [];
  // 'with' params
  if (ctx.isKeyword(KW.WITH)) {
    ctx.nextWord();
    let hasMore = true;
    while (hasMore) {
      // 在开始解析参数前，先消费换行和缩进，支持多行格式
      ctx.consumeNewlines();
      ctx.consumeIndent();

      const { annotations, firstToken } = parseAnnotations(ctx, error);
      const nameTok = ctx.peek();
      const name = parseIdent(ctx, error);
      if (!ctx.at(TokenKind.COLON)) error("Expected ':' after parameter name", ctx.peek());
      ctx.next();
      const type = parseType(ctx, error);
      const p: Parameter =
        annotations.length > 0 ? { name, type, annotations } : { name, type };
      const endTok = ctx.tokens[ctx.index - 1] || ctx.peek();
      const spanStart = firstToken?.start ?? nameTok.start;
      (p as any).span = { start: spanStart, end: endTok.end };
      params.push(p);
      if (ctx.at(TokenKind.IDENT) && ((ctx.peek().value as string) || '').toLowerCase() === KW.AND) {
        ctx.nextWord();
        // 'and' 后允许换行和缩进
        ctx.consumeNewlines();
        ctx.consumeIndent();
        continue;
      }
      if (ctx.at(TokenKind.COMMA)) {
        ctx.next();
        // 逗号后允许换行
        ctx.consumeNewlines();
        // 检查当前 token 是否是 'produce'（不需要 INDENT，因为参数行保持同一缩进级别）
        if (tokLowerAt(ctx, ctx.index) === KW.PRODUCE) {
          hasMore = false;
        } else {
          // 不是 'produce'，继续解析参数（可能有 INDENT 表示增加了缩进）
          ctx.consumeIndent();
          continue;
        }
      } else {
        hasMore = false;
      }
    }
    return params;
  }
  // Bare params: name: Type [(and|,) name: Type]*
  if (ctx.at(TokenKind.IDENT) && ctx.tokens[ctx.index + 1] && ctx.tokens[ctx.index + 1]!.kind === TokenKind.COLON) {
    let hasMore = true;
    while (hasMore) {
      // 在开始解析参数前，先消费换行和缩进，支持多行格式
      ctx.consumeNewlines();
      ctx.consumeIndent();

      const { annotations, firstToken } = parseAnnotations(ctx, error);
      const nameTok = ctx.peek();
      const name = parseIdent(ctx, error);
      if (!ctx.at(TokenKind.COLON)) error("Expected ':' after parameter name", ctx.peek());
      ctx.next();
      const type = parseType(ctx, error);
      const p: Parameter =
        annotations.length > 0 ? { name, type, annotations } : { name, type };
      const endTok = ctx.tokens[ctx.index - 1] || ctx.peek();
      const spanStart = firstToken?.start ?? nameTok.start;
      (p as any).span = { start: spanStart, end: endTok.end };
      params.push(p);
      // Accept 'and' or ',' between parameters
      if (ctx.at(TokenKind.IDENT) && ((ctx.peek().value as string) || '').toLowerCase() === KW.AND) {
        ctx.nextWord();
        // 'and' 后允许换行和缩进
        ctx.consumeNewlines();
        ctx.consumeIndent();
        continue;
      }
      if (ctx.at(TokenKind.COMMA)) {
        ctx.next();
        // 逗号后允许换行
        ctx.consumeNewlines();
        // 检查当前 token 是否是 'produce' 或 'with'
        const after = tokLowerAt(ctx, ctx.index);
        if (after === KW.PRODUCE || after === KW.WITH) {
          hasMore = false;
        } else {
          // 不是终止关键字，继续解析参数
          ctx.consumeIndent();
          continue;
        }
      } else {
        hasMore = false;
      }
    }
  }
  return params;
}

/**
 * 推断 Lambda 返回类型（用于短形式 Lambda）
 * @param e Expression
 * @returns Type
 */
export function inferLambdaReturnType(e: Expression): Type {
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
        if (n === '<' || n === '>' || n === '<=' || n === '>=' || n === '==' || n === '!=') return Node.TypeName('Bool');
      }
      return Node.TypeName('Unknown');
    }
    default:
      return Node.TypeName('Unknown');
  }
}

// ===== 辅助函数 =====

/**
 * 解析标识符
 * @param ctx Parser 上下文
 * @param error 错误报告函数
 * @returns 标识符字符串
 */
function parseIdent(
  ctx: ParserContext,
  error: (msg: string) => never
): string {
  if (!ctx.at(TokenKind.IDENT)) {
    error('Expected identifier');
  }
  return ctx.next().value as string;
}

/**
 * 期望关键字
 * @param ctx Parser 上下文
 * @param error 错误报告函数
 * @param kw 关键字
 * @param msg 错误消息
 */
function expectKeyword(
  ctx: ParserContext,
  error: (msg: string) => never,
  kw: string,
  msg: string
): void {
  if (!ctx.isKeyword(kw)) error(msg);
  ctx.nextWord();
}

/**
 * 期望逗号或允许省略
 * @param ctx Parser 上下文
 */
function expectCommaOr(ctx: ParserContext): void {
  if (ctx.at(TokenKind.COMMA)) {
    ctx.next();
  }
}

/**
 * 期望换行符
 * @param ctx Parser 上下文
 * @param error 错误报告函数
 */
function expectNewline(
  ctx: ParserContext,
  error: (msg: string) => never
): void {
  if (!ctx.at(TokenKind.NEWLINE)) error('Expected newline');
  ctx.next();
}
