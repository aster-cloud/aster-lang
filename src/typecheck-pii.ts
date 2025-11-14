import type { Core, Origin, PiiMeta, PiiLevel, Span, TypecheckDiagnostic } from './types.js';
import { ErrorCode, ERROR_METADATA, ERROR_MESSAGES } from './error_codes.js';

type PiiEnv = Map<string, PiiMeta | null>;

interface FuncPiiSignature {
  readonly params: readonly (PiiMeta | null)[];
  readonly ret: PiiMeta | null;
}

interface FunctionContext {
  readonly func: Core.Func;
  readonly signatures: ReadonlyMap<string, FuncPiiSignature>;
  readonly diagnostics: TypecheckDiagnostic[];
  readonly returnMeta: PiiMeta | null;
}

interface AssignmentDecision {
  violation: boolean;
  warning: boolean;
  categoryMismatch: boolean;
}

interface SinkDescriptor {
  readonly label: string;
  readonly kind: 'console' | 'network' | 'database' | 'emit';
  readonly argIndices?: readonly number[] | undefined;
}

const LEVEL_ORDER: Record<PiiLevel, number> = { L1: 1, L2: 2, L3: 3 } as const;
const mergeCache = new Map<string, { level: PiiLevel; categories: readonly string[] }>();
const assignmentCache = new Map<string, AssignmentDecision>();
const SYNTHETIC_ORIGIN: Origin = {
  start: { line: 0, col: 0 },
  end: { line: 0, col: 0 },
};

const CONSOLE_SINKS = new Set(['print', 'IO.print', 'IO.println', 'log', 'Log.info', 'Log.debug', 'Console.log']);
const NETWORK_PREFIXES = ['Http.', 'HTTP.', 'http.'];
const DATABASE_PREFIXES = ['Sql.', 'SQL.', 'sql.', 'Db.', 'DB.'];
const EMIT_SINKS = new Set(['emit', 'workflow.emit']);

export function checkModulePII(funcs: readonly Core.Func[], diagnostics: TypecheckDiagnostic[]): void {
  const signatures = buildFuncSignatures(funcs);
  for (const func of funcs) {
    if (!func.body) continue;
    const env: PiiEnv = new Map();
    seedEnvWithParams(func, env);
    const ctx: FunctionContext = {
      func,
      signatures,
      diagnostics,
      returnMeta: metaFromType(func.ret, func.body.origin),
    };
    traverseBlock(func.body, env, ctx);
  }
}

function traverseBlock(block: Core.Block, env: PiiEnv, ctx: FunctionContext): void {
  for (const stmt of block.statements) {
    switch (stmt.kind) {
      case 'Let': {
        const rhsMeta = inferExprPii(stmt.expr, env, ctx);
        const lhsMeta = env.has(stmt.name) ? env.get(stmt.name)! : undefined;
        if (lhsMeta !== undefined) {
          handleAssignment(lhsMeta, rhsMeta, originToSpan(stmt.origin), ctx, 'variable');
        }
        env.set(stmt.name, cloneMeta(rhsMeta));
        break;
      }
      case 'Set': {
        const rhsMeta = inferExprPii(stmt.expr, env, ctx);
        const lhsMeta = env.has(stmt.name) ? env.get(stmt.name)! : undefined;
        if (lhsMeta !== undefined) {
          handleAssignment(lhsMeta, rhsMeta, originToSpan(stmt.origin), ctx, 'variable');
        }
        const nextMeta = mergePiiMeta(lhsMeta ?? null, rhsMeta);
        env.set(stmt.name, cloneMeta(nextMeta));
        break;
      }
      case 'Return': {
        const valueMeta = inferExprPii(stmt.expr, env, ctx);
        handleAssignment(ctx.returnMeta ?? null, valueMeta, originToSpan(stmt.origin), ctx, 'return');
        break;
      }
      case 'If': {
        void inferExprPii(stmt.cond, env, ctx);
        const thenEnv = cloneEnv(env);
        traverseBlock(stmt.thenBlock, thenEnv, ctx);
        const elseEnv = stmt.elseBlock ? cloneEnv(env) : cloneEnv(env);
        if (stmt.elseBlock) traverseBlock(stmt.elseBlock, elseEnv, ctx);
        replaceEnv(env, mergeEnv(thenEnv, elseEnv));
        break;
      }
      case 'Match': {
        const matchedMeta = inferExprPii(stmt.expr, env, ctx);
        let accumulatedEnv: PiiEnv | null = null;
        for (const kase of stmt.cases) {
          const branchEnv = cloneEnv(env);
          bindPattern(kase.pattern, matchedMeta, branchEnv);
          if (kase.body.kind === 'Return') {
            const meta = inferExprPii(kase.body.expr, branchEnv, ctx);
            handleAssignment(ctx.returnMeta ?? null, meta, originToSpan(kase.body.origin), ctx, 'return');
          } else {
            traverseBlock(kase.body, branchEnv, ctx);
          }
          accumulatedEnv = accumulatedEnv ? mergeEnv(accumulatedEnv, branchEnv) : branchEnv;
        }
        if (accumulatedEnv) replaceEnv(env, accumulatedEnv);
        break;
      }
      case 'Scope': {
        const scopeBlock: Core.Block = {
          kind: 'Block',
          statements: stmt.statements,
          origin: ensureOrigin(stmt.origin),
        };
        traverseBlock(scopeBlock, env, ctx);
        break;
      }
      case 'workflow': {
        for (const step of stmt.steps) {
          const stepEnv = cloneEnv(env);
          traverseBlock(step.body, stepEnv, ctx);
        }
        break;
      }
      case 'Start': {
        void inferExprPii(stmt.expr, env, ctx);
        break;
      }
      case 'Wait':
        break;
    }
  }
}

function inferExprPii(expr: Core.Expression, env: PiiEnv, ctx: FunctionContext): PiiMeta | null {
  switch (expr.kind) {
    case 'Name':
      return cloneMeta(env.get(expr.name) ?? null);
    case 'Bool':
    case 'Int':
    case 'Long':
    case 'Double':
    case 'String':
    case 'Null':
      return null;
    case 'Call': {
      const argMetas = expr.args.map(arg => inferExprPii(arg, env, ctx));
      const targetName = resolveCallName(expr.target);
      if (targetName) {
        const sink = classifySink(targetName);
        if (sink) {
          const indices = sink.argIndices ?? expr.args.map((_, idx) => idx);
          for (const index of indices) {
            const argMeta = argMetas[index] ?? null;
            const argSpan = expr.args[index] ? originToSpan(expr.args[index]!.origin) : originToSpan(expr.origin);
            checkSinkAllowed(sink.label, sink.kind, argMeta, expr.args[index], argSpan, env, ctx);
          }
        }

        const signature = ctx.signatures.get(targetName);
        if (signature) {
          validateCallArgs(targetName, signature, argMetas, expr.args, ctx);
          return cloneMeta(signature.ret);
        }

        const sanitized = handleSanitizer(targetName, argMetas, expr);
        if (sanitized !== undefined) return sanitized;
      }
      return argMetas.reduce<PiiMeta | null>((acc, meta) => mergePiiMeta(acc, meta), null);
    }
    case 'Construct': {
      let merged: PiiMeta | null = null;
      for (const field of expr.fields) {
        merged = mergePiiMeta(merged, inferExprPii(field.expr, env, ctx));
      }
      return merged;
    }
    case 'Ok':
    case 'Err':
    case 'Some':
      return inferExprPii(expr.expr, env, ctx);
    case 'Await':
      return inferExprPii(expr.expr, env, ctx);
    case 'Lambda': {
      const lambdaEnv: PiiEnv = new Map();
      seedLambdaParams(expr, lambdaEnv);
      const lambdaCtx: FunctionContext = {
        func: ctx.func,
        signatures: ctx.signatures,
        diagnostics: ctx.diagnostics,
        returnMeta: metaFromType(expr.ret, expr.body.origin),
      };
      traverseBlock(expr.body, lambdaEnv, lambdaCtx);
      return null;
    }
    default:
      return null;
  }
}

function mergePiiMeta(left: PiiMeta | null | undefined, right: PiiMeta | null | undefined): PiiMeta | null {
  if (!left && !right) return null;
  if (!left) return cloneMeta(right ?? null);
  if (!right) return cloneMeta(left);
  const key = `${metaKey(left)}|${metaKey(right)}`;
  let cached = mergeCache.get(key);
  if (!cached) {
    const level = LEVEL_ORDER[left.level] >= LEVEL_ORDER[right.level] ? left.level : right.level;
    const categories = normalizeCategories([...left.categories, ...right.categories]);
    cached = { level, categories };
    mergeCache.set(key, cached);
  }
  return { level: cached.level, categories: cached.categories, sourceSpan: left.sourceSpan ?? right.sourceSpan };
}

function evaluateAssignment(
  targetMeta: PiiMeta | null | undefined,
  valueMeta: PiiMeta | null
): AssignmentDecision {
  const key = `${metaKey(targetMeta, true)}->${metaKey(valueMeta)}`;
  const cached = assignmentCache.get(key);
  if (cached) return cached;
  const decision: AssignmentDecision = { violation: false, warning: false, categoryMismatch: false };
  if (targetMeta === undefined) {
    assignmentCache.set(key, decision);
    return decision;
  }
  if (!targetMeta && valueMeta) {
    decision.violation = true;
  } else if (targetMeta && !valueMeta) {
    decision.warning = true;
  } else if (targetMeta && valueMeta) {
    if (!categoriesEqual(targetMeta.categories, valueMeta.categories)) {
      decision.violation = true;
      decision.categoryMismatch = true;
    } else {
      const lhsRank = LEVEL_ORDER[targetMeta.level];
      const rhsRank = LEVEL_ORDER[valueMeta.level];
      if (rhsRank > lhsRank) {
        decision.violation = true;
      } else if (rhsRank < lhsRank) {
        decision.warning = true;
      }
    }
  }
  assignmentCache.set(key, decision);
  return decision;
}

function violatesAssignment(lhs: PiiMeta | null | undefined, rhs: PiiMeta | null): boolean {
  return evaluateAssignment(lhs, rhs).violation;
}

function handleAssignment(
  targetMeta: PiiMeta | null | undefined,
  valueMeta: PiiMeta | null,
  span: Span | undefined,
  ctx: FunctionContext,
  targetLabel: 'variable' | 'return'
): void {
  const decision = evaluateAssignment(targetMeta, valueMeta);
  if (decision.violation) {
    const code = targetLabel === 'return' ? ErrorCode.PII_ASSIGN_DOWNGRADE : ErrorCode.PII_ASSIGN_DOWNGRADE;
    emitDiagnostic(ctx.diagnostics, code, span, {
      source: describeMeta(valueMeta),
      target: describeMeta(targetMeta ?? null),
    });
  } else if (decision.warning) {
    emitDiagnostic(ctx.diagnostics, ErrorCode.PII_IMPLICIT_UPLEVEL, span, {
      source: describeMeta(valueMeta),
      target: describeMeta(targetMeta ?? null),
    });
  }
}

function checkSinkAllowed(
  sinkLabel: string,
  kind: SinkDescriptor['kind'],
  meta: PiiMeta | null,
  argExpr: Core.Expression | undefined,
  span: Span | undefined,
  env: PiiEnv,
  ctx: FunctionContext
): void {
  if (!meta) {
    if (argExpr?.kind === 'Name' && !env.has(argExpr.name)) {
      emitDiagnostic(ctx.diagnostics, ErrorCode.PII_SINK_UNKNOWN, span, { sinkKind: sinkLabel });
    }
    return;
  }
  if (meta.level === 'L3') {
    emitDiagnostic(ctx.diagnostics, ErrorCode.PII_SINK_UNSANITIZED, span, {
      level: meta.level,
      sinkKind: sinkLabel,
    });
    return;
  }
  if (kind === 'console' && meta.level === 'L2') {
    emitDiagnostic(ctx.diagnostics, ErrorCode.PII_SINK_UNSANITIZED, span, {
      level: meta.level,
      sinkKind: sinkLabel,
    });
  }
}

function validateCallArgs(
  callee: string,
  signature: FuncPiiSignature,
  argMetas: readonly (PiiMeta | null)[],
  argNodes: readonly Core.Expression[],
  ctx: FunctionContext
): void {
  const count = Math.min(signature.params.length, argMetas.length);
  for (let i = 0; i < count; i++) {
    const expected = signature.params[i];
    const actual = argMetas[i] ?? null;
    if (violatesAssignment(expected, actual)) {
      emitDiagnostic(ctx.diagnostics, ErrorCode.PII_ARG_VIOLATION, originToSpan(argNodes[i]?.origin), {
        expected: describeMeta(expected ?? null),
        actual: describeMeta(actual),
        func: callee,
      });
    } else if (expected && !actual) {
      emitDiagnostic(ctx.diagnostics, ErrorCode.PII_ARG_VIOLATION, originToSpan(argNodes[i]?.origin), {
        expected: describeMeta(expected),
        actual: describeMeta(actual),
        func: callee,
      });
    }
  }
}

function handleSanitizer(
  callee: string,
  argMetas: readonly (PiiMeta | null)[],
  expr: Core.Call
): PiiMeta | null | undefined {
  if (callee === 'redact' || callee === 'tokenize') {
    const source = argMetas[0];
    if (!source) return null;
    return {
      level: 'L1',
      categories: source.categories,
      sourceSpan: originToSpan(expr.origin),
    };
  }
  return undefined;
}

function seedEnvWithParams(func: Core.Func, env: PiiEnv): void {
  for (const param of func.params) {
    const meta = metaFromType(param.type as Core.Type, func.body.origin);
    if (meta) {
      env.set(param.name, meta);
    } else {
      env.set(param.name, null);
    }
  }
}

function seedLambdaParams(lambda: Core.Lambda, env: PiiEnv): void {
  for (const param of lambda.params) {
    const meta = metaFromType(param.type as Core.Type, lambda.body.origin);
    env.set(param.name, meta ?? null);
  }
}

function buildFuncSignatures(funcs: readonly Core.Func[]): Map<string, FuncPiiSignature> {
  const map = new Map<string, FuncPiiSignature>();
  for (const func of funcs) {
    const params = func.params.map(param => metaFromType(param.type as Core.Type, func.body.origin));
    const ret = metaFromType(func.ret, func.body.origin);
    map.set(func.name, { params, ret });
  }
  return map;
}

function bindPattern(pattern: Core.Pattern, meta: PiiMeta | null, env: PiiEnv): void {
  switch (pattern.kind) {
    case 'PatName':
      env.set(pattern.name, cloneMeta(meta));
      break;
    case 'PatCtor':
      if (pattern.names) {
        for (const name of pattern.names) env.set(name, cloneMeta(meta));
      }
      if (pattern.args) {
        for (const arg of pattern.args) bindPattern(arg, meta, env);
      }
      break;
    default:
      break;
  }
}

function classifySink(targetName: string): SinkDescriptor | null {
  if (CONSOLE_SINKS.has(targetName)) return { label: targetName, kind: 'console' };
  if (EMIT_SINKS.has(targetName)) return { label: targetName, kind: 'emit' };
  if (NETWORK_PREFIXES.some(prefix => targetName.startsWith(prefix))) {
    const indices = targetName.startsWith('Http.') ? ([1] as const) : undefined;
    return { label: targetName, kind: 'network', argIndices: indices };
  }
  if (DATABASE_PREFIXES.some(prefix => targetName.startsWith(prefix))) {
    return { label: targetName, kind: 'database' };
  }
  return null;
}

function resolveCallName(target: Core.Expression): string | null {
  return target.kind === 'Name' ? target.name : null;
}

function cloneMeta(meta: PiiMeta | null | undefined): PiiMeta | null {
  if (!meta) return null;
  return { level: meta.level, categories: [...meta.categories], sourceSpan: meta.sourceSpan };
}

function cloneEnv(env: PiiEnv): PiiEnv {
  const next: PiiEnv = new Map();
  for (const [key, value] of env.entries()) {
    next.set(key, cloneMeta(value));
  }
  return next;
}

function mergeEnv(left: PiiEnv, right: PiiEnv): PiiEnv {
  const merged: PiiEnv = new Map();
  const keys = new Set([...left.keys(), ...right.keys()]);
  for (const key of keys) {
    const leftMeta = left.has(key) ? left.get(key)! : undefined;
    const rightMeta = right.has(key) ? right.get(key)! : undefined;
    merged.set(key, cloneMeta(mergePiiMeta(leftMeta ?? null, rightMeta ?? null)) ?? null);
  }
  return merged;
}

function replaceEnv(target: PiiEnv, source: PiiEnv): void {
  target.clear();
  for (const [key, value] of source.entries()) {
    target.set(key, cloneMeta(value));
  }
}

function categoriesEqual(a: readonly string[], b: readonly string[]): boolean {
  if (a.length !== b.length) return false;
  const normalizedA = normalizeCategories(a);
  const normalizedB = normalizeCategories(b);
  return normalizedA.every((cat, idx) => cat === normalizedB[idx]);
}

function normalizeCategories(values: readonly string[]): readonly string[] {
  return [...new Set(values)].sort();
}

function metaKey(meta: PiiMeta | null | undefined, allowUnset = false): string {
  if (meta === undefined) return allowUnset ? 'unset' : 'plain';
  if (!meta) return 'plain';
  return `${meta.level}[${normalizeCategories(meta.categories).join('|')}]`;
}

function describeMeta(meta: PiiMeta | null): string {
  if (!meta) return 'Plain';
  return `${meta.level}[${normalizeCategories(meta.categories).join(', ')}]`;
}

function metaFromType(type: Core.Type | undefined, origin?: Origin): PiiMeta | null {
  if (!type) return null;
  if (type.kind === 'PiiType') {
    return {
      level: type.sensitivity,
      categories: [type.category],
      sourceSpan: originToSpan(origin),
    };
  }
  return null;
}

function originToSpan(origin: Origin | undefined): Span | undefined {
  if (!origin) return undefined;
  return { start: origin.start, end: origin.end };
}

function ensureOrigin(origin: Origin | undefined): Origin {
  return origin ?? SYNTHETIC_ORIGIN;
}

function emitDiagnostic(
  diagnostics: TypecheckDiagnostic[],
  code: ErrorCode,
  span: Span | undefined,
  params: Record<string, unknown>
): void {
  const metadata = ERROR_METADATA[code];
  const template = ERROR_MESSAGES[code] ?? metadata.message;
  const diagnostic: TypecheckDiagnostic = {
    code,
    severity: metadata.severity,
    message: formatMessage(template, params),
    help: metadata.help,
  };
  if (span) diagnostic.span = span;
  if (Object.keys(params).length > 0) diagnostic.data = params;
  diagnostics.push(diagnostic);
}

function formatMessage(template: string, params: Record<string, unknown>): string {
  return template.replace(/\{(\w+)\}/g, (_, key: string) => {
    const value = params[key];
    if (value === undefined || value === null) return `{${key}}`;
    if (Array.isArray(value)) return value.join(', ');
    return String(value);
  });
}
