import { performance } from 'node:perf_hooks';
import type { Core, TypecheckDiagnostic } from './types.js';
import { type CapabilityManifest, type CapabilityContext, isAllowed } from './capabilities.js';
import type { CapabilityKind } from './config/semantic.js';
import { inferEffects } from './effect_inference.js';
import { DefaultCoreVisitor } from './visitor.js';
// import { DiagnosticBuilder, DiagnosticCode, DiagnosticSeverity } from './diagnostics.js';
import { IO_PREFIXES, CPU_PREFIXES, CAPABILITY_PREFIXES } from './config/effects.js';
import { ENFORCE_CAPABILITIES } from './config/runtime.js';
import { createLogger, logPerformance } from './utils/logger.js';

// Re-export TypecheckDiagnostic for external use
export type { TypecheckDiagnostic };

// Internal representation for types during checking
type UnknownT = { kind: 'Unknown' };
type T = Core.Type | UnknownT;

function isUnknown(x: T): x is UnknownT {
  return (x as { kind: string }).kind === 'Unknown';
}

function tUnknown(): T {
  return { kind: 'Unknown' };
}

const UNKNOWN_TYPENAME: Core.TypeName = { kind: 'TypeName', name: 'Unknown' };
const typecheckLogger = createLogger('typecheck');

function tEquals(a: T, b: T): boolean {
  if (isUnknown(a) || isUnknown(b)) return true;
  const aa = a as Core.Type;
  const bb = b as Core.Type;
  if (aa.kind === 'TypeVar' && bb.kind === 'TypeVar') {
    return aa.name === bb.name;
  }
  if (aa.kind === 'TypeVar' || bb.kind === 'TypeVar') return false;
  if (aa.kind !== bb.kind) return false;
  switch (aa.kind) {
    case 'TypeName': {
      const an = aa.name;
      const bn = (bb as Core.TypeName).name;
      if (an === 'Unknown' || bn === 'Unknown') return true;
      return an === bn;
    }
    case 'TypeApp': {
      const aApp = aa as Core.TypeApp;
      const bApp = bb as Core.TypeApp;
      if (aApp.base !== bApp.base) return false;
      if (aApp.args.length !== bApp.args.length) return false;
      for (let i = 0; i < aApp.args.length; i++)
        if (!tEquals(aApp.args[i] as T, bApp.args[i] as T)) return false;
      return true;
    }
    case 'Maybe':
      return tEquals((aa as Core.Maybe).type as T, (bb as Core.Maybe).type as T);
    case 'Option':
      return tEquals((aa as Core.Option).type as T, (bb as Core.Option).type as T);
    case 'Result': {
      const aRes = aa as Core.Result;
      const bRes = bb as Core.Result;
      return tEquals(aRes.ok as T, bRes.ok as T) && tEquals(aRes.err as T, bRes.err as T);
    }
    case 'List':
      return tEquals((aa as Core.List).type as T, (bb as Core.List).type as T);
    case 'Map': {
      const aMap = aa as Core.Map;
      const bMap = bb as Core.Map;
      return tEquals(aMap.key as T, bMap.key as T) && tEquals(aMap.val as T, bMap.val as T);
    }
    case 'FuncType': {
      const aFunc = aa as unknown as Core.FuncType;
      const bFunc = bb as unknown as Core.FuncType;
      if (aFunc.params.length !== bFunc.params.length) return false;
      for (let i = 0; i < aFunc.params.length; i++)
        if (!tEquals(aFunc.params[i] as T, bFunc.params[i] as T)) return false;
      return tEquals(aFunc.ret as T, bFunc.ret as T);
    }
    case 'PiiType': {
      const aPii = aa as Core.PiiType;
      const bPii = bb as Core.PiiType;
      return aPii.sensitivity === bPii.sensitivity &&
             aPii.category === bPii.category &&
             tEquals(aPii.baseType as T, bPii.baseType as T);
    }
    default: {
      // 穷尽检查：TypeScript 确保所有类型都已处理
      // 如果到达这里，说明类型定义与实现不一致
      const unknownType = aa as Core.Type;
      // const _exhaustiveCheck: never = unknownType as never;
      typecheckLogger.warn('tEquals: 未处理的类型 kind', { value: unknownType });
      return false;
    }
  }
}

function tToString(t: T): string {
  if (isUnknown(t)) return 'Unknown';
  switch (t.kind) {
    case 'TypeName':
      return t.name;
    case 'TypeVar':
      return t.name;
    case 'TypeApp':
      return `${t.base}<${t.args.map(tt => tToString(tt as T)).join(', ')}>`;
    case 'Maybe':
      return `${tToString(t.type as T)}?`;
    case 'Option':
      return `Option<${tToString(t.type as T)}>`;
    case 'Result':
      return `Result<${tToString(t.ok as T)}, ${tToString(t.err as T)}>`;
    case 'List':
      return `List<${tToString(t.type as T)}>`;
    case 'Map':
      return `Map<${tToString(t.key as T)}, ${tToString(t.val as T)}>`;
    case 'FuncType': {
      const ft = t as unknown as Core.FuncType;
      const ps = ft.params.map(tt => tToString(tt as T)).join(', ');
      return `(${ps}) -> ${tToString(ft.ret as T)}`;
    }
    case 'PiiType': {
      const pt = t as Core.PiiType;
      return `@pii(${pt.sensitivity}, ${pt.category}) ${tToString(pt.baseType as T)}`;
    }
    default:
      return 'Unknown';
  }
}

interface Env {
  vars: Map<string, T>;
}

interface ModuleContext {
  datas: Map<string, Core.Data>;
  enums: Map<string, Core.Enum>;
  imports: Map<string, string>; // alias -> module name (or module name -> module name if no alias)
}

export function typecheckModule(m: Core.Module): TypecheckDiagnostic[] {
  const moduleName = m.name ?? '<anonymous>';
  const startTime = performance.now();
  typecheckLogger.info('开始类型检查模块', { moduleName });
  try {
    const diags: TypecheckDiagnostic[] = [];
    const ctx: ModuleContext = { datas: new Map(), enums: new Map(), imports: new Map() };
    for (const d of m.decls) {
      if (d.kind === 'Import') {
        const alias = d.asName ?? d.name;
        if (ctx.imports.has(alias)) {
          diags.push({
            severity: 'warning',
            message: `Duplicate import alias '${alias}'.`,
          });
        } else {
          ctx.imports.set(alias, d.name);
        }
      }
    }
    for (const d of m.decls) {
      if (d.kind === 'Data') ctx.datas.set(d.name, d);
      if (d.kind === 'Enum') ctx.enums.set(d.name, d);
    }
    for (const d of m.decls) {
      if (d.kind === 'Func') {
        diags.push(...typecheckFunc(ctx, d));
      }
    }
    const effectDiags = inferEffects(m);
    diags.push(...effectDiags);
    const duration = performance.now() - startTime;
    const baseMeta = { moduleName, errorCount: diags.length };
    logPerformance({
      component: 'typecheck',
      operation: '模块类型检查',
      duration,
      metadata: baseMeta,
    });
    typecheckLogger.info('类型检查完成', {
      ...baseMeta,
      duration_ms: duration,
    });
    return diags;
  } catch (error) {
    typecheckLogger.error('类型检查失败', error as Error, { moduleName });
    throw error;
  }
}

export function typecheckModuleWithCapabilities(
  m: Core.Module,
  manifest: CapabilityManifest | null
): TypecheckDiagnostic[] {
  const diags = typecheckModule(m);
  const capCtx: CapabilityContext = { moduleName: m.name ?? '' };
  for (const d of m.decls) {
    if (d.kind !== 'Func') continue;
    const effs = new Set(d.effects.map(e => String(e).toLowerCase()));
    if (effs.has('io') && !isAllowed('io', d.name, capCtx, manifest)) {
      diags.push({ severity: 'error', message: `Function '${d.name}' declares @io but capability manifest does not allow it for module '${m.name}'.`, code: 'CAP_IO_NOT_ALLOWED', data: { func: d.name, module: m.name, cap: 'io' } });
    }
    if (effs.has('cpu') && !isAllowed('cpu', d.name, capCtx, manifest)) {
      diags.push({ severity: 'error', message: `Function '${d.name}' declares @cpu but capability manifest does not allow it for module '${m.name}'.`, code: 'CAP_CPU_NOT_ALLOWED', data: { func: d.name, module: m.name, cap: 'cpu' } });
    }
  }
  return diags;
}

function typecheckFunc(ctx: ModuleContext, f: Core.Func): TypecheckDiagnostic[] {
  // enum member → enum type lookup for expression typing
  const enumMemberOf = new Map<string, string>();
  for (const en of ctx.enums.values()) for (const v of en.variants) enumMemberOf.set(v, en.name);

  const diags: TypecheckDiagnostic[] = [];
  const env: Env = { vars: new Map() };
  for (const p of f.params) env.vars.set(p.name, p.type as T);
  const retT = f.ret as T;
  const bodyRet = typecheckBlock(ctx, env, f.body, diags);
  if (!tEquals(bodyRet, retT)) {
    diags.push({
      severity: 'error',
      message: `Return type mismatch: expected ${tToString(retT)}, got ${tToString(bodyRet)}`,
    });
  }

  // Effect enforcement (minimal lattice: ∅ ⊑ CPU ⊑ IO[*])
  const effs = collectEffects(f.body);
  const hasIO = f.effects.some(e => String(e).toLowerCase() === 'io');
  const hasCPU = f.effects.some(e => String(e).toLowerCase() === 'cpu');
  // Missing IO is an error when IO-like calls are detected
  if (effs.has('io') && !hasIO)
    diags.push({
      severity: 'error',
      message: `Function '${f.name}' may perform I/O but is missing @io effect.`,
      code: 'EFF_MISSING_IO',
      data: { func: f.name },
    });
  // Under lattice, IO subsumes CPU: if CPU work is detected, it is satisfied by either @cpu or @io
  if (effs.has('cpu') && !(hasCPU || hasIO))
    diags.push({
      severity: 'error',
      message: `Function '${f.name}' may perform CPU-bound work but is missing @cpu (or @io) effect.`,
      code: 'EFF_MISSING_CPU',
      data: { func: f.name },
    });
  // Superfluous annotations:
  // - If @io is declared but only CPU-like work is found, emit an info (IO subsumes CPU; not harmful).
  if (!effs.has('io') && hasIO && effs.has('cpu'))
    diags.push({
      severity: 'info',
      message: `Function '${f.name}' declares @io but only CPU-like work found; @io subsumes @cpu and may be unnecessary.`,
      code: 'EFF_SUPERFLUOUS_IO_CPU_ONLY',
      data: { func: f.name },
    });
  // - If @io is declared but no obvious IO/CPU is found, keep a low-severity warning.
  if (!effs.has('io') && hasIO && !effs.has('cpu'))
    diags.push({
      severity: 'warning',
      message: `Function '${f.name}' declares @io but no obvious I/O found.`,
      code: 'EFF_SUPERFLUOUS_IO',
      data: { func: f.name },
    });
  if (!effs.has('cpu') && hasCPU)
    diags.push({
      severity: 'warning',
      message: `Function '${f.name}' declares @cpu but no obvious CPU-bound work found.`,
      code: 'EFF_SUPERFLUOUS_CPU',
      data: { func: f.name },
    });
  // Async discipline: every started task should be waited somewhere in the body

  // Capability-parameterized IO enforcement (feature-gated)
  if (ENFORCE_CAPABILITIES) {
    const meta = f as unknown as { effectCaps?: readonly CapabilityKind[]; effectCapsExplicit?: boolean };
    const declaredCaps = meta.effectCapsExplicit ? meta.effectCaps : undefined;
    if (declaredCaps && declaredCaps.length > 0) {
      const declared = new Set<CapabilityKind>(declaredCaps);
      const used = collectCapabilities(f.body);
      for (const cap of used) {
        if (!declared.has(cap)) {
          diags.push({
            severity: 'error',
            message: `Function '${f.name}' uses IO capability '${cap}' but header declares [${[...declared].join(', ')}].`,
            code: 'EFF_CAP_MISSING',
            data: { func: f.name, cap },
          });
        }
      }
      for (const cap of declared) {
        if (!used.has(cap)) {
          diags.push({
            severity: 'info',
            message: `Function '${f.name}' declares IO capability '${cap}' but it is not used.`,
            code: 'EFF_CAP_SUPERFLUOUS',
            data: { func: f.name, cap },
          });
        }
      }
    }
  }

  const aw = collectAsync(f.body);
  const notWaited = [...aw.started].filter(n => !aw.waited.has(n));
  if (notWaited.length > 0) {
    diags.push({
      severity: 'warning',
      message: `Started async tasks not waited: ${notWaited.join(', ')}`,
    });
  }
  // Generics: infer type variables from return position (preview)
  const typeParams = (f as unknown as { typeParams?: readonly string[] }).typeParams ?? [];
  if (typeParams.length > 0) {
    const subst = new Map<string, Core.Type>();
    unifyTypes(f.ret as T, bodyRet, subst, diags);
  }
  // Generic type parameters: undefined usage (error) and unused (warn)
  {
    const declared = new Set<string>(typeParams);
    const used = new Set<string>();
    const collect = (t: Core.Type): void => {
      switch (t.kind) {
        case 'TypeVar':
          used.add((t as Core.TypeVar).name);
          break;
        case 'Maybe':
          collect((t as Core.Maybe).type);
          break;
        case 'Option':
          collect((t as Core.Option).type);
          break;
        case 'Result':
          collect((t as Core.Result).ok);
          collect((t as Core.Result).err);
          break;
        case 'List':
          collect((t as Core.List).type);
          break;
        case 'Map':
          collect((t as Core.Map).key);
          collect((t as Core.Map).val);
          break;
        case 'TypeApp':
          for (const a of (t as Core.TypeApp).args) collect(a);
          break;
        case 'FuncType': {
          const ft = t as unknown as Core.FuncType;
          ft.params.forEach(collect);
          collect(ft.ret);
          break;
        }
      }
    };
    for (const p of f.params) collect(p.type);
    collect(f.ret);
    for (const u of used)
      if (!declared.has(u))
        diags.push({
          severity: 'error',
          message: `Type variable '${u}' is used in '${f.name}' but not declared in its type parameters.`,
        });
    for (const tv of declared)
      if (!used.has(tv))
        diags.push({
          severity: 'warning',
          message: `Type parameter '${tv}' on '${f.name}' is declared but not used.`,
        });

    // Heuristic: flag capitalized unknown type names that look like type variables but are not declared and not defined data/enum
    const KNOWN_SCALARS = new Set(['Text', 'Int', 'Bool', 'Float']);
    const isMaybeTypeVarLike = (name: string): boolean => /^[A-Z][A-Za-z0-9_]*$/.test(name);
    const unknowns = new Set<string>();
    const findUnknowns = (t: Core.Type): void => {
      switch (t.kind) {
        case 'TypeName': {
          const nm = (t as Core.TypeName).name;
          if (
            !KNOWN_SCALARS.has(nm) &&
            !ctx.datas.has(nm) &&
            !ctx.enums.has(nm) &&
            isMaybeTypeVarLike(nm)
          )
            unknowns.add(nm);
          break;
        }
        case 'Maybe':
          findUnknowns((t as Core.Maybe).type);
          break;
        case 'Option':
          findUnknowns((t as Core.Option).type);
          break;
        case 'Result':
          findUnknowns((t as Core.Result).ok);
          findUnknowns((t as Core.Result).err);
          break;
        case 'List':
          findUnknowns((t as Core.List).type);
          break;
        case 'Map':
          findUnknowns((t as Core.Map).key);
          findUnknowns((t as Core.Map).val);
          break;
        case 'TypeApp':
          for (const a of (t as Core.TypeApp).args) findUnknowns(a);
          break;
        case 'FuncType': {
          const ft = t as unknown as Core.FuncType;
          ft.params.forEach(findUnknowns);
          findUnknowns(ft.ret);
          break;
        }
        case 'PiiType':
          findUnknowns((t as Core.PiiType).baseType);
          break;
      }
    };
    for (const p of f.params) findUnknowns(p.type);
    findUnknowns(f.ret);
    for (const nm of unknowns) {
      if (!declared.has(nm))
        diags.push({
          severity: 'error',
          message: `Type variable-like '${nm}' is used in '${f.name}' but not declared; declare it with 'of ${nm}'.`,
        });
    }
  }
  return diags;
}
function warn(diags: TypecheckDiagnostic[], message: string): void {
  diags.push({ severity: 'warning', message });
}

function collectEffects(b: Core.Block): Set<'io' | 'cpu'> {
  const effs = new Set<'io' | 'cpu'>();
  class EffectsVisitor extends DefaultCoreVisitor<void> {
    override visitExpression(e: Core.Expression): void {
      if (e.kind === 'Call' && e.target.kind === 'Name') {
        const n = e.target.name;
        if (IO_PREFIXES.some((p: string) => n.startsWith(p))) effs.add('io');
        if (CPU_PREFIXES.some((p: string) => n.startsWith(p))) effs.add('cpu');
      }
      super.visitExpression(e, undefined as unknown as void);
    }
  }
  new EffectsVisitor().visitBlock(b, undefined as unknown as void);
  return effs;
}


function collectCapabilities(b: Core.Block): Set<CapabilityKind> {
  const caps = new Set<CapabilityKind>();
  class CapVisitor extends DefaultCoreVisitor<void> {
    override visitExpression(e: Core.Expression): void {
      if (e.kind === 'Call' && e.target.kind === 'Name') {
        const n = e.target.name;
        for (const [cap, prefixes] of Object.entries(CAPABILITY_PREFIXES)) {
          if (prefixes.some(p => n.startsWith(p))) caps.add(cap as CapabilityKind);
        }
      }
      super.visitExpression(e, undefined as unknown as void);
    }
  }
  new CapVisitor().visitBlock(b, undefined as unknown as void);
  return caps;
}

class TypecheckVisitor extends DefaultCoreVisitor<{ ctx: ModuleContext; env: Env; diags: TypecheckDiagnostic[] }> {
  public result: T = tUnknown();

  override visitBlock(b: Core.Block, c: { ctx: ModuleContext; env: Env; diags: TypecheckDiagnostic[] }): void {
    let last: T = tUnknown();
    for (const s of b.statements) {
      this.visitStatement(s, c);
      last = this.result;
    }
    this.result = last;
  }

  override visitStatement(s: Core.Statement, c: { ctx: ModuleContext; env: Env; diags: TypecheckDiagnostic[] }): void {
    const { ctx, env, diags } = c;
    switch (s.kind) {
      case 'Let': {
        const t = typeOfExpr(ctx, env, s.expr, diags);
        env.vars.set(s.name, t);
        this.result = t;
        return;
      }
      case 'Set': {
        const t = typeOfExpr(ctx, env, s.expr, diags);
        const prev = env.vars.get(s.name) || tUnknown();
        if (!tEquals(prev, t)) {
          diags.push({ severity: 'error', message: `Type mismatch assigning to '${s.name}': ${tToString(prev)} vs ${tToString(t)}` });
        }
        env.vars.set(s.name, t);
        this.result = t;
        return;
      }
      case 'Return': {
        this.result = typeOfExpr(ctx, env, s.expr, diags);
        return;
      }
      case 'If': {
        void typeOfExpr(ctx, env, s.cond, diags);
        const tThen = typecheckBlock(ctx, cloneEnv(env), s.thenBlock, diags);
        const tElse = s.elseBlock ? typecheckBlock(ctx, cloneEnv(env), s.elseBlock, diags) : tUnknown();
        if (isUnknown(tThen)) this.result = tElse;
        else if (isUnknown(tElse)) this.result = tThen;
        else {
          if (!tEquals(tThen, tElse)) diags.push({ severity: 'error', message: `If分支返回类型不一致: then分支 ${tToString(tThen)} vs else分支 ${tToString(tElse)}` });
          this.result = tThen;
        }
        return;
      }
      case 'Match': {
        const et = typeOfExpr(ctx, env, s.expr, diags);
        let out: T | null = null;
        let hasNullCase = false;
        let hasNonNullCase = false;
        const enumDecl = !isUnknown(et) && et.kind === 'TypeName' ? ctx.enums.get(et.name) : undefined;
        const seenEnum = new Set<string>();
        let hasWildcard = false;
        for (const cse of s.cases) {
          const t = typecheckCase(ctx, env, cse, et, diags);
          if (!out) out = t; else if (!tEquals(out, t)) diags.push({ severity: 'error', message: `Match case return types differ: ${tToString(out)} vs ${tToString(t)}` });
          if (cse.pattern.kind === 'PatNull') hasNullCase = true; else hasNonNullCase = true;
          if (enumDecl) {
            if (cse.pattern.kind === 'PatName') {
              if (enumDecl.variants.includes(cse.pattern.name)) {
                if (seenEnum.has(cse.pattern.name)) warn(c.diags, `Duplicate enum case '${cse.pattern.name}' in match on ${enumDecl.name}.`);
                seenEnum.add(cse.pattern.name);
              } else {
                hasWildcard = true;
              }
            } else if (cse.pattern.kind === 'PatCtor') {
              if (enumDecl.variants.includes(cse.pattern.typeName)) {
                if (seenEnum.has(cse.pattern.typeName)) warn(c.diags, `Duplicate enum case '${cse.pattern.typeName}' in match on ${enumDecl.name}.`);
                seenEnum.add(cse.pattern.typeName);
              }
            }
          }
        }
        if (!isUnknown(et) && et.kind === 'Maybe') {
          if (!(hasNullCase && hasNonNullCase)) {
            const missing = hasNullCase ? 'non-null value' : hasNonNullCase ? 'null' : 'null and non-null';
            warn(diags, `Non-exhaustive match on Maybe type; missing ${missing} case.`);
          }
        } else if (enumDecl) {
          if (!hasWildcard) {
            const missing = enumDecl.variants.filter(v => !seenEnum.has(v));
            if (missing.length > 0) warn(diags, `Non-exhaustive match on ${enumDecl.name}; missing: ${missing.join(', ')}`);
          }
        }
        this.result = out ?? tUnknown();
        return;
      }
      case 'Scope': {
        const nested: Core.Block = { kind: 'Block', statements: s.statements };
        this.result = typecheckBlock(ctx, env, nested, diags);
        return;
      }
      case 'Start': {
        void typeOfExpr(ctx, env, s.expr, diags);
        this.result = tUnknown();
        return;
      }
      case 'Wait': {
        this.result = tUnknown();
        return;
      }
    }
  }
}

function typecheckBlock(
  ctx: ModuleContext,
  env: Env,
  b: Core.Block,
  diags: TypecheckDiagnostic[]
): T {
  const v = new TypecheckVisitor();
  v.visitBlock(b, { ctx, env, diags });
  return v.result;
}

function typecheckCase(
  ctx: ModuleContext,
  env: Env,
  c: Core.Case,
  et: T,
  diags: TypecheckDiagnostic[]
): T {
  const env2 = cloneEnv(env);
  // Bind pattern
  if (c.pattern.kind === 'PatNull') {
    // ok for Maybe T
  } else if (c.pattern.kind === 'PatCtor') {
    bindPatternTypes(ctx, env2, c.pattern, et, diags);
  } else if (c.pattern.kind === 'PatName') {
    env2.vars.set(c.pattern.name, et);
  } else if (c.pattern.kind === 'PatInt') {
    // Scrutinee should be Int; we don't bind names for literal patterns
    const isInt = !isUnknown(et) && et.kind === 'TypeName' && (et as Core.TypeName).name === 'Int';
    if (!isInt) {
      diags.push({ severity: 'error', message: `Integer pattern used on non-Int scrutinee (${tToString(et)})` });
    }
  }
  // Body
  if (c.body.kind === 'Return') return typeOfExpr(ctx, env2, c.body.expr, diags);
  return typecheckBlock(ctx, env2, c.body, diags);
}

function bindPatternTypes(
  ctx: ModuleContext,
  env: Env,
  p: Core.PatCtor,
  et: T,
  diags: TypecheckDiagnostic[]
): void {
  // Special-case Result Ok/Err destructuring
  if (p.typeName === 'Ok' && !isUnknown(et) && et.kind === 'Result') {
    const inner = (et as unknown as Core.Result).ok as T;
    const ctor = p as Core.PatCtor & { args?: readonly Core.Pattern[] };
    const child = ctor.args && ctor.args.length > 0
      ? (ctor.args[0] as Core.Pattern)
      : p.names && p.names[0]
        ? ({ kind: 'PatName', name: p.names[0] } as Core.PatName)
        : null;
    if (child) bindPatternNode(ctx, env, child, inner, diags);
    return;
  }
  if (p.typeName === 'Err' && !isUnknown(et) && et.kind === 'Result') {
    const inner = (et as unknown as Core.Result).err as T;
    const ctor = p as Core.PatCtor & { args?: readonly Core.Pattern[] };
    const child = ctor.args && ctor.args.length > 0
      ? (ctor.args[0] as Core.Pattern)
      : p.names && p.names[0]
        ? ({ kind: 'PatName', name: p.names[0] } as Core.PatName)
        : null;
    if (child) bindPatternNode(ctx, env, child, inner, diags);
    return;
  }
  // Data constructor by schema
  const d = ctx.datas.get(p.typeName);
  if (!d) {
    // unknown ctor; bind any names as Unknown and recurse into args with Unknown
    if (p.names) for (const n of p.names) env.vars.set(n, tUnknown());
    const ctor = p as Core.PatCtor & { args?: readonly Core.Pattern[] };
    if (ctor.args)
      for (const a of ctor.args as readonly Core.Pattern[])
        bindPatternNode(ctx, env, a, tUnknown(), diags);
    return;
  }
  const ar = d.fields.length;
  const ctor2 = p as Core.PatCtor & { args?: readonly Core.Pattern[] };
  const args: readonly Core.Pattern[] = ctor2.args ? (ctor2.args as readonly Core.Pattern[]) : [];
  for (let i = 0; i < ar; i++) {
    const ft = d.fields[i]!.type as T;
    const child =
      i < args.length
        ? args[i]!
        : p.names && i < p.names.length
          ? ({ kind: 'PatName', name: p.names[i]! } as Core.PatName)
          : null;
    if (!child) continue;
    bindPatternNode(ctx, env, child, ft, diags);
  }
}

function bindPatternNode(
  ctx: ModuleContext,
  env: Env,
  pat: Core.Pattern,
  et: T,
  diags: TypecheckDiagnostic[]
): void {
  if (pat.kind === 'PatName') {
    env.vars.set(pat.name, et);
    return;
  }
  if (pat.kind === 'PatNull') return;
  if (pat.kind === 'PatCtor') bindPatternTypes(ctx, env, pat, et, diags);
}

// 访问器优先的表达式类型推断（第一阶段：Name/常量/Construct/Ok/Err/Some/None）
class TypeOfExprVisitor extends DefaultCoreVisitor<{ ctx: ModuleContext; env: Env; diags: TypecheckDiagnostic[] }> {
  public handled = false;
  public result: T = tUnknown();
  override visitExpression(e: Core.Expression, c: { ctx: ModuleContext; env: Env; diags: TypecheckDiagnostic[] }): void {
    const { ctx, env, diags } = c;
    switch (e.kind) {
      case 'Name': {
        const t = env.vars.get(e.name);
        if (t) { this.result = t; this.handled = true; return; }
        for (const en of ctx.enums.values()) {
          if (en.variants.includes(e.name)) { this.result = { kind: 'TypeName', name: en.name } as Core.TypeName; this.handled = true; return; }
        }
        this.result = tUnknown(); this.handled = true; return;
      }
      case 'Bool': this.result = { kind: 'TypeName', name: 'Bool' } as Core.TypeName; this.handled = true; return;
      case 'Int': this.result = { kind: 'TypeName', name: 'Int' } as Core.TypeName; this.handled = true; return;
      case 'Long': this.result = { kind: 'TypeName', name: 'Long' } as Core.TypeName; this.handled = true; return;
      case 'Double': this.result = { kind: 'TypeName', name: 'Double' } as Core.TypeName; this.handled = true; return;
      case 'String': this.result = { kind: 'TypeName', name: 'Text' } as Core.TypeName; this.handled = true; return;
      case 'Null': this.result = { kind: 'Maybe', type: UNKNOWN_TYPENAME } as T; this.handled = true; return;
      case 'Ok': {
        const inner = typeOfExpr(ctx, env, e.expr, diags);
        this.result = { kind: 'Result', ok: isUnknown(inner) ? UNKNOWN_TYPENAME : (inner as Core.Type), err: UNKNOWN_TYPENAME } as T;
        this.handled = true; return;
      }
      case 'Err': {
        const inner = typeOfExpr(ctx, env, e.expr, diags);
        this.result = { kind: 'Result', ok: UNKNOWN_TYPENAME, err: isUnknown(inner) ? UNKNOWN_TYPENAME : (inner as Core.Type) } as T;
        this.handled = true; return;
      }
      case 'Some': {
        const inner = typeOfExpr(ctx, env, e.expr, diags);
        this.result = { kind: 'Option', type: isUnknown(inner) ? UNKNOWN_TYPENAME : (inner as Core.Type) } as T;
        this.handled = true; return;
      }
      case 'None': this.result = { kind: 'Option', type: UNKNOWN_TYPENAME } as T; this.handled = true; return;
      case 'Construct': {
        const d = ctx.datas.get(e.typeName);
        if (!d) { this.result = tUnknown(); this.handled = true; return; }
        const providedFields = new Set<string>();
        for (const f of e.fields) {
          providedFields.add(f.name);
          const field = d.fields.find(ff => ff.name === f.name);
          if (!field) { diags.push({ severity: 'error', message: `Unknown field '${f.name}' for ${d.name}` }); continue; }
          const ft = typeOfExpr(ctx, env, f.expr, diags);
          if (!tEquals(field.type as T, ft)) diags.push({ severity: 'error', message: `Field '${f.name}' expects ${tToString(field.type as T)}, got ${tToString(ft)}` });
        }
        for (const field of d.fields) if (!providedFields.has(field.name)) diags.push({ severity: 'error', message: `构造 ${d.name} 缺少必需字段 '${field.name}'` });
        this.result = { kind: 'TypeName', name: e.typeName } as Core.TypeName; this.handled = true; return;
      }
      case 'Await': {
        // Await<T> 返回 T，即解包 Future/Promise 类型
        // 当前简化实现：直接返回内部表达式的类型
        const t = typeOfExpr(ctx, env, e.expr, diags);
        this.result = t;
        this.handled = true; return;
      }
      case 'Lambda': {
        const pt = e.params.map(p => p.type) as readonly Core.Type[];
        const ft: Core.FuncType = { kind: 'FuncType', params: pt, ret: e.ret };
        this.result = ft as unknown as T;
        this.handled = true; return;
      }
      case 'Call': {
        // not(x): 简化布尔运算
        if (e.target.kind === 'Name' && e.target.name === 'not') {
          if (e.args.length !== 1) diags.push({ severity: 'error', message: `not(...) expects 1 argument` });
          else void typeOfExpr(ctx, env, e.args[0]!, diags);
          this.result = { kind: 'TypeName', name: 'Bool' } as Core.TypeName;
          this.handled = true; return;
        }
        // 处理参数类型（副作用：产生必要诊断）
        for (const a of e.args) void typeOfExpr(ctx, env, a, diags);
        // 互操作静态调用的数字混用告警
        if (e.target.kind === 'Name' && e.target.name.includes('.')) {
          let hasInt = false, hasLong = false, hasDouble = false;
          for (const a of e.args) {
            switch (a.kind) {
              case 'Int': hasInt = true; break;
              case 'Long': hasLong = true; break;
              case 'Double': hasDouble = true; break;
            }
          }
          const kinds = (hasInt ? 1 : 0) + (hasLong ? 1 : 0) + (hasDouble ? 1 : 0);
          if (kinds > 1) diags.push({ severity: 'warning', message: `Ambiguous interop call '${e.target.name}': mixing numeric kinds (Int=${hasInt}, Long=${hasLong}, Double=${hasDouble}). Overload resolution may widen/box implicitly.` });
        }
        // await(expr): 类型规则
        if (e.target.kind === 'Name' && e.target.name === 'await' && e.args.length === 1) {
          const at = typeOfExpr(ctx, env, e.args[0]!, diags);
          if (!isUnknown(at) && at.kind === 'Maybe') { this.result = (at.type as T); this.handled = true; return; }
          if (!isUnknown(at) && at.kind === 'Result') { this.result = (at.ok as T); this.handled = true; return; }
          diags.push({ severity: 'warning', message: `await expects Maybe<T> or Result<T,E>, got ${tToString(at)}` });
          this.result = tUnknown(); this.handled = true; return;
        }
        this.result = tUnknown(); this.handled = true; return;
      }
    }
    // 未处理的表达式保持 handled=false，用旧实现兜底
  }
}

function typeOfExpr(
  ctx: ModuleContext,
  env: Env,
  e: Core.Expression,
  diags: TypecheckDiagnostic[]
): T {
  const v = new TypeOfExprVisitor();
  v.visitExpression(e, { ctx, env, diags });
  return v.result;
}

function cloneEnv(env: Env): Env {
  return { vars: new Map(env.vars) };
}

// Unify expected vs actual types, binding TypeVar names in 'subst'.
function unifyTypes(
  expected: T,
  actual: T,
  subst: Map<string, Core.Type>,
  diags: TypecheckDiagnostic[]
): void {
  if (isUnknown(expected) || isUnknown(actual)) return;
  // If expected is a type variable, bind it
  if (expected.kind === 'TypeVar') {
    const tv = (expected as Core.TypeVar).name;
    const bound = subst.get(tv);
    if (!bound) {
      subst.set(tv, actual as Core.Type);
    } else if (!tEquals(bound as T, actual)) {
      diags.push({
        severity: 'error',
        message: `Type variable '${tv}' inferred inconsistently: ${tToString(bound as T)} vs ${tToString(actual)}`,
      });
    }
    return;
  }
  // Recurse structurally when kinds match
  if (expected.kind !== actual.kind) return;
  switch (expected.kind) {
    case 'Maybe':
      unifyTypes(
        (expected as Core.Maybe).type as T,
        (actual as Core.Maybe).type as T,
        subst,
        diags
      );
      break;
    case 'Option':
      unifyTypes(
        (expected as Core.Option).type as T,
        (actual as Core.Option).type as T,
        subst,
        diags
      );
      break;
    case 'Result': {
      const ee = expected as Core.Result,
        aa = actual as Core.Result;
      unifyTypes(ee.ok as T, aa.ok as T, subst, diags);
      unifyTypes(ee.err as T, aa.err as T, subst, diags);
      break;
    }
    case 'List':
      unifyTypes((expected as Core.List).type as T, (actual as Core.List).type as T, subst, diags);
      break;
    case 'Map': {
      const ee = expected as Core.Map,
        aa = actual as Core.Map;
      unifyTypes(ee.key as T, aa.key as T, subst, diags);
      unifyTypes(ee.val as T, aa.val as T, subst, diags);
      break;
    }
    case 'TypeApp': {
      const ee = expected as Core.TypeApp,
        aa = actual as Core.TypeApp;
      const n = Math.min(ee.args.length, aa.args.length);
      for (let i = 0; i < n; i++) unifyTypes(ee.args[i] as T, aa.args[i] as T, subst, diags);
      break;
    }
    case 'FuncType': {
      const ee = expected as unknown as Core.FuncType,
        aa = actual as unknown as Core.FuncType;
      const n = Math.min(ee.params.length, aa.params.length);
      for (let i = 0; i < n; i++) unifyTypes(ee.params[i] as T, aa.params[i] as T, subst, diags);
      unifyTypes(ee.ret as T, aa.ret as T, subst, diags);
      break;
    }
    default:
      // Ground kinds: TypeName, etc. — nothing further
      break;
  }
}

function collectAsync(b: Core.Block): { started: Set<string>; waited: Set<string> } {
  const started = new Set<string>();
  const waited = new Set<string>();
  class AsyncVisitor extends DefaultCoreVisitor<void> {
    override visitStatement(s: Core.Statement): void {
      if (s.kind === 'Start') started.add(s.name);
      if (s.kind === 'Wait') s.names.forEach(n => waited.add(n));
      super.visitStatement(s, undefined as unknown as void);
    }
  }
  new AsyncVisitor().visitBlock(b, undefined as unknown as void);
  return { started, waited };
}
