import { TokenKind } from '../tokens.js';
import type { Annotation, Token } from '../types.js';
import type { ParserContext } from './context.js';

interface AnnotationParamSpec {
  readonly name: string;
  readonly type: 'number' | 'string' | 'boolean';
}

interface AnnotationSpec {
  readonly params: readonly AnnotationParamSpec[];
}

const ANNOTATION_SPECS: Record<string, AnnotationSpec> = {
  NotEmpty: { params: [] },
  Range: {
    params: [
      { name: 'min', type: 'number' },
      { name: 'max', type: 'number' },
    ],
  },
  Pattern: {
    params: [{ name: 'regexp', type: 'string' }],
  },
};

export interface ParsedAnnotations {
  readonly annotations: readonly Annotation[];
  readonly firstToken?: Token;
}

export function parseAnnotations(
  ctx: ParserContext,
  error: (msg: string, tok?: Token) => never
): ParsedAnnotations {
  const annotations: Annotation[] = [];
  let firstToken: Token | undefined;

  while (ctx.at(TokenKind.AT)) {
    const atTok = ctx.next();
    if (!firstToken) {
      firstToken = atTok;
    }

    const nameTok = ctx.peek();
    if (!ctx.at(TokenKind.IDENT) && !ctx.at(TokenKind.TYPE_IDENT)) {
      error('注解语法错误：@ 后需要注解名称', nameTok);
    }
    const name = ctx.next().value as string;

    const spec = ANNOTATION_SPECS[name];
    if (!spec) {
      error(`未知注解 '${name}'`, nameTok);
    }

    const params = new Map<string, unknown>();
    if (spec.params.length === 0) {
      if (ctx.at(TokenKind.LPAREN)) {
        error(`注解 '${name}' 不接受参数`, ctx.peek());
      }
    } else {
      if (!ctx.at(TokenKind.LPAREN)) {
        error(`注解 '${name}' 需要参数列表`, ctx.peek());
      }
      consumeAnnotationParams(ctx, error, spec, params);
    }

    annotations.push({ name, params });
  }

  if (firstToken) {
    return { annotations, firstToken };
  }
  return { annotations };
}

function consumeAnnotationParams(
  ctx: ParserContext,
  error: (msg: string, tok?: Token) => never,
  spec: AnnotationSpec,
  params: Map<string, unknown>
): void {
  ctx.next(); // consume '('

  let expectParam = true;
  while (!ctx.at(TokenKind.RPAREN)) {
    if (!expectParam) {
      if (!ctx.at(TokenKind.COMMA)) {
        error('注解参数之间需要逗号分隔', ctx.peek());
      }
      ctx.next(); // consume ','
      if (ctx.at(TokenKind.RPAREN)) {
        error('注解参数列表不允许尾随逗号', ctx.peek());
      }
    }

    expectParam = false;

    const keyTok = ctx.peek();
    if (!ctx.at(TokenKind.IDENT)) {
      error('注解参数必须使用标识符作为名称', keyTok);
    }
    const key = ctx.next().value as string;
    if (params.has(key)) {
      error(`注解参数 '${key}' 重复定义`, keyTok);
    }

    if (!ctx.at(TokenKind.COLON)) {
      error("注解参数名后需要 ':'", ctx.peek());
    }
    ctx.next(); // consume ':'

    const valueTok = ctx.peek();
    const value = parseAnnotationValue(ctx, error);
    params.set(key, value);

    validateAnnotationParamType(spec, key, value, valueTok, error);
  }

  const closing = ctx.peek();
  ctx.next(); // consume ')'

  for (const paramSpec of spec.params) {
    if (!params.has(paramSpec.name)) {
      error(`注解缺少必需参数 '${paramSpec.name}'`, closing);
    }
  }

  const allowedKeys = new Set(spec.params.map(p => p.name));
  for (const key of params.keys()) {
    if (!allowedKeys.has(key)) {
      error(`注解参数 '${key}' 未被识别`, closing);
    }
  }
}

function parseAnnotationValue(
  ctx: ParserContext,
  error: (msg: string, tok?: Token) => never
): unknown {
  const tok = ctx.peek();
  switch (tok.kind) {
    case TokenKind.INT:
    case TokenKind.FLOAT:
    case TokenKind.LONG: {
      ctx.next();
      return tok.value as number;
    }
    case TokenKind.STRING: {
      ctx.next();
      return tok.value as string;
    }
    case TokenKind.BOOL: {
      ctx.next();
      return tok.value as boolean;
    }
    default:
      error(`不支持的注解参数值类型 '${tok.kind}'`, tok);
  }
}

function validateAnnotationParamType(
  spec: AnnotationSpec,
  key: string,
  value: unknown,
  tok: Token,
  error: (msg: string, tok?: Token) => never
): void {
  const paramSpec = spec.params.find(p => p.name === key);
  if (!paramSpec) {
    return;
  }

  switch (paramSpec.type) {
    case 'number':
      if (typeof value !== 'number' || Number.isNaN(value)) {
        error(`注解参数 '${key}' 需要数值`, tok);
      }
      break;
    case 'string':
      if (typeof value !== 'string') {
        error(`注解参数 '${key}' 需要字符串`, tok);
      }
      break;
    case 'boolean':
      if (typeof value !== 'boolean') {
        error(`注解参数 '${key}' 需要布尔值`, tok);
      }
      break;
    default:
      error(`注解参数 '${key}' 的类型约束未定义`, tok);
  }
}
