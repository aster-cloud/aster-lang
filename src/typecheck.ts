import type { Core } from './types.js';
import { type CapabilityManifest, type CapabilityContext, isAllowed } from './capabilities.js';
// import { DiagnosticBuilder, DiagnosticCode, DiagnosticSeverity } from './diagnostics.js';
import { IO_PREFIXES, CPU_PREFIXES } from './config/effects.js';

export interface TypecheckDiagnostic {
  severity: 'error' | 'warning';
  message: string;
  code?: string;
  data?: unknown;
}

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

function tEquals(a: T, b: T): boolean {
  if (isUnknown(a) || isUnknown(b)) return true;
  if (a.kind !== b.kind) return false;
  switch (a.kind) {
    case 'TypeName': {
      const an = (a as Core.TypeName).name;
      const bn = (b as Core.TypeName).name;
      if (an === 'Unknown' || bn === 'Unknown') return true;
      return an === bn;
    }
    case 'TypeVar':
      // Permissive equality for preview; assume compatible
      return true;
    case 'TypeApp': {
      const aa = a as Core.TypeApp,
        bb = b as Core.TypeApp;
      if (aa.base !== bb.base) return false;
      if (aa.args.length !== bb.args.length) return false;
      for (let i = 0; i < aa.args.length; i++)
        if (!tEquals(aa.args[i] as T, bb.args[i] as T)) return false;
      return true;
    }
    case 'Maybe':
      return tEquals((a as Core.Maybe).type as T, (b as Core.Maybe).type as T);
    case 'Option':
      return tEquals((a as Core.Option).type as T, (b as Core.Option).type as T);
    case 'Result': {
      const aa = a as Core.Result,
        bb = b as Core.Result;
      return tEquals(aa.ok as T, bb.ok as T) && tEquals(aa.err as T, bb.err as T);
    }
    case 'List':
      return tEquals((a as Core.List).type as T, (b as Core.List).type as T);
    case 'Map': {
      const aa = a as Core.Map,
        bb = b as Core.Map;
      return tEquals(aa.key as T, bb.key as T) && tEquals(aa.val as T, bb.val as T);
    }
    case 'FuncType': {
      const aa = a as unknown as Core.FuncType,
        bb = b as unknown as Core.FuncType;
      if (aa.params.length !== bb.params.length) return false;
      for (let i = 0; i < aa.params.length; i++)
        if (!tEquals(aa.params[i] as T, bb.params[i] as T)) return false;
      return tEquals(aa.ret as T, bb.ret as T);
    }
    default:
      return true;
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
  const diags: TypecheckDiagnostic[] = [];
  const ctx: ModuleContext = { datas: new Map(), enums: new Map(), imports: new Map() };
  // Collect imports (MVP: warn on duplicate aliases)
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
  return diags;
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

  // Effect lints with small registry
  const effs = collectEffects(f.body);
  const hasIO = f.effects.some(e => String(e).toLowerCase() === 'io');
  const hasCPU = f.effects.some(e => String(e).toLowerCase() === 'cpu');
  if (effs.has('io') && !hasIO)
    diags.push({
      severity: 'warning',
      message: `Function '${f.name}' may perform I/O but is missing @io effect.`,
      code: 'EFF_MISSING_IO',
      data: { func: f.name },
    });
  if (!effs.has('io') && hasIO)
    diags.push({
      severity: 'warning',
      message: `Function '${f.name}' declares @io but no obvious I/O found.`,
      code: 'EFF_SUPERFLUOUS_IO',
      data: { func: f.name },
    });
  if (effs.has('cpu') && !hasCPU)
    diags.push({
      severity: 'warning',
      message: `Function '${f.name}' may perform CPU-bound work but is missing @cpu effect.`,
      code: 'EFF_MISSING_CPU',
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
  const markFromExpr = (e: Core.Expression): void => {
    if (e.kind === 'Call') {
      if (e.target.kind === 'Name') {
        const n = e.target.name;
        // simple registry of known IO and CPU calls
        // configurable prefixes
        if (IO_PREFIXES.some((p: string) => n.startsWith(p))) effs.add('io');
        if (CPU_PREFIXES.some((p: string) => n.startsWith(p))) effs.add('cpu');
      }
      markFromExpr(e.target);
      e.args.forEach(markFromExpr);
    } else if (e.kind === 'Construct') {
      e.fields.forEach(f => markFromExpr(f.expr));
    } else if (e.kind === 'Ok' || e.kind === 'Err' || e.kind === 'Some') {
      markFromExpr(e.expr);
    }
  };
  for (const s of b.statements) {
    switch (s.kind) {
      case 'Let':
        markFromExpr(s.expr);
        break;
      case 'Set':
        markFromExpr(s.expr);
        break;
      case 'Return':
        markFromExpr(s.expr);
        break;
      case 'If':
        markFromExpr(s.cond);
        collectEffects(s.thenBlock).forEach(e => effs.add(e));
        if (s.elseBlock) collectEffects(s.elseBlock).forEach(e => effs.add(e));
        break;
      case 'Match':
        markFromExpr(s.expr);
        for (const c of s.cases) {
          if (c.body.kind === 'Return') markFromExpr(c.body.expr);
          else collectEffects(c.body).forEach(e => effs.add(e));
        }
        break;
      case 'Scope': {
        const nested: Core.Block = { kind: 'Block', statements: s.statements };
        collectEffects(nested).forEach(e => effs.add(e));
        break;
      }
      case 'Start':
        markFromExpr(s.expr);
        break;
      case 'Wait':
        // waiting itself isn't IO; no-op
        break;
      default:
        break;
    }
  }
  return effs;
}

function typecheckBlock(
  ctx: ModuleContext,
  env: Env,
  b: Core.Block,
  diags: TypecheckDiagnostic[]
): T {
  let last: T = tUnknown();
  for (const s of b.statements) {
    last = typecheckStmt(ctx, env, s, diags);
  }
  return last;
}

function typecheckStmt(
  ctx: ModuleContext,
  env: Env,
  s: Core.Statement,
  diags: TypecheckDiagnostic[]
): T {
  switch (s.kind) {
    case 'Let': {
      const t = typeOfExpr(ctx, env, s.expr, diags);
      env.vars.set(s.name, t);
      return t;
    }
    case 'Set': {
      const t = typeOfExpr(ctx, env, s.expr, diags);
      const prev = env.vars.get(s.name) || tUnknown();
      if (!tEquals(prev, t)) {
        diags.push({
          severity: 'error',
          message: `Type mismatch assigning to '${s.name}': ${tToString(prev)} vs ${tToString(t)}`,
        });
      }
      return prev;
    }
    case 'Return': {
      return typeOfExpr(ctx, env, s.expr, diags);
    }
    case 'If': {
      // Evaluate condition (currently allows any type)
      void typeOfExpr(ctx, env, s.cond, diags);
      // Allow any condition; in future require Bool
      const tThen = typecheckBlock(ctx, cloneEnv(env), s.thenBlock, diags);
      const tElse = s.elseBlock
        ? typecheckBlock(ctx, cloneEnv(env), s.elseBlock, diags)
        : tUnknown();
      return tThen || tElse;
    }
    case 'Match': {
      const et = typeOfExpr(ctx, env, s.expr, diags);
      // Evaluate each case body type; require all equal (when known)
      let out: T | null = null;

      // Tracking for exhaustiveness
      let hasNullCase = false;
      let hasNonNullCase = false; // any pattern that implies non-null (ctor or variable catch-all)

      // Enum tracking
      const enumDecl =
        !isUnknown(et) && et.kind === 'TypeName' ? ctx.enums.get(et.name) : undefined;
      const seenEnum = new Set<string>();
      let hasWildcard = false; // variable catch-all for enums

      for (const c of s.cases) {
        // Case typing
        const t = typecheckCase(ctx, env, c, et, diags);
        if (!out) out = t;
        else if (!tEquals(out, t)) {
          diags.push({
            severity: 'error',
            message: `Match case return types differ: ${tToString(out)} vs ${tToString(t)}`,
          });
        }

        // Exhaustiveness bookkeeping
        if (c.pattern.kind === 'PatNull') hasNullCase = true;
        else hasNonNullCase = true;

        if (enumDecl) {
          if (c.pattern.kind === 'PatName') {
            if (enumDecl.variants.includes(c.pattern.name)) {
              if (seenEnum.has(c.pattern.name)) {
                warn(
                  diags,
                  `Duplicate enum case '${c.pattern.name}' in match on ${enumDecl.name}.`
                );
              }
              seenEnum.add(c.pattern.name);
            } else {
              // variable catch-all
              hasWildcard = true;
            }
          } else if (c.pattern.kind === 'PatCtor') {
            // Treat bare ctor name as variant name if it matches one
            if (enumDecl.variants.includes(c.pattern.typeName)) {
              if (seenEnum.has(c.pattern.typeName)) {
                warn(
                  diags,
                  `Duplicate enum case '${c.pattern.typeName}' in match on ${enumDecl.name}.`
                );
              }
              seenEnum.add(c.pattern.typeName);
            } else {
              // Not a variant of this enum; does not contribute to coverage
            }
          }
        }
      }

      // Exhaustiveness diagnostics
      if (!isUnknown(et) && et.kind === 'Maybe') {
        if (!(hasNullCase && hasNonNullCase)) {
          const missing = hasNullCase
            ? 'non-null value'
            : hasNonNullCase
              ? 'null'
              : 'null and non-null';
          warn(diags, `Non-exhaustive match on Maybe type; missing ${missing} case.`);
        }
      } else if (enumDecl) {
        if (!hasWildcard) {
          const missing = enumDecl.variants.filter(v => !seenEnum.has(v));
          if (missing.length > 0) {
            warn(diags, `Non-exhaustive match on ${enumDecl.name}; missing: ${missing.join(', ')}`);
          }
        }
      }
      return out ?? tUnknown();
    }
    default:
      return tUnknown();
  }
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

function typeOfExpr(
  ctx: ModuleContext,
  env: Env,
  e: Core.Expression,
  diags: TypecheckDiagnostic[]
): T {
  switch (e.kind) {
    case 'Name': {
      // Variable, or enum member reference -> its enum type
      const t = env.vars.get(e.name);
      if (t) return t;
      // enum member lookup across known enums
      for (const en of ctx.enums.values()) {
        if (en.variants.includes(e.name)) {
          return { kind: 'TypeName', name: en.name } as Core.TypeName;
        }
      }
      return tUnknown();
    }
    case 'Bool':
      return { kind: 'TypeName', name: 'Bool' } as Core.TypeName;
    case 'Int':
      return { kind: 'TypeName', name: 'Int' } as Core.TypeName;
    case 'Long':
      return { kind: 'TypeName', name: 'Long' } as Core.TypeName;
    case 'Double':
      return { kind: 'TypeName', name: 'Double' } as Core.TypeName;
    case 'String':
      return { kind: 'TypeName', name: 'Text' } as Core.TypeName;
    case 'Null':
      return { kind: 'Maybe', type: UNKNOWN_TYPENAME } as T;
    case 'Ok': {
      const inner = typeOfExpr(ctx, env, e.expr, diags);
      return {
        kind: 'Result',
        ok: isUnknown(inner) ? UNKNOWN_TYPENAME : (inner as Core.Type),
        err: UNKNOWN_TYPENAME,
      } as T;
    }
    case 'Err': {
      const inner = typeOfExpr(ctx, env, e.expr, diags);
      return {
        kind: 'Result',
        ok: UNKNOWN_TYPENAME,
        err: isUnknown(inner) ? UNKNOWN_TYPENAME : (inner as Core.Type),
      } as T;
    }
    case 'Some': {
      const inner = typeOfExpr(ctx, env, e.expr, diags);
      return {
        kind: 'Option',
        type: isUnknown(inner) ? UNKNOWN_TYPENAME : (inner as Core.Type),
      } as T;
    }
    case 'None':
      return { kind: 'Option', type: UNKNOWN_TYPENAME } as T;
    case 'Lambda': {
      const pt = e.params.map(p => p.type) as readonly Core.Type[];
      const ft: Core.FuncType = { kind: 'FuncType', params: pt, ret: e.ret };
      return ft as unknown as T;
    }
    case 'Construct': {
      const d = ctx.datas.get(e.typeName);
      if (!d) return tUnknown();
      // Validate fields by name
      for (const f of e.fields) {
        const field = d.fields.find(ff => ff.name === f.name);
        if (!field) {
          diags.push({ severity: 'error', message: `Unknown field '${f.name}' for ${d.name}` });
          continue;
        }
        const ft = typeOfExpr(ctx, env, f.expr, diags);
        if (!tEquals(field.type as T, ft)) {
          diags.push({
            severity: 'error',
            message: `Field '${f.name}' expects ${tToString(field.type as T)}, got ${tToString(ft)}`,
          });
        }
      }
      return { kind: 'TypeName', name: e.typeName } as Core.TypeName;
    }
    case 'Call': {
      // Unknown calls: assume Bool condition or Unknown; prefer Bool
      // If call target name is 'not', ensure arg is Bool and return Bool
      if (e.target.kind === 'Name' && e.target.name === 'not') {
        if (e.args.length !== 1) {
          diags.push({ severity: 'error', message: `not(...) expects 1 argument` });
        } else {
          void typeOfExpr(ctx, env, e.args[0]!, diags);
          // Accept any; future: require Bool
        }
        return { kind: 'TypeName', name: 'Bool' } as Core.TypeName;
      }
      // Otherwise, infer from usage minimally: for now return Unknown
      for (const a of e.args) {
        void typeOfExpr(ctx, env, a, diags);
      }
      // Interop static call ambiguity diagnostic (numeric mix without clear target info)
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
        if (kinds > 1) {
          diags.push({
            severity: 'warning',
            message: `Ambiguous interop call '${e.target.name}': mixing numeric kinds (Int=${hasInt}, Long=${hasLong}, Double=${hasDouble}). Overload resolution may widen/box implicitly.`,
          });
        }
      }
      // await(expr) typing: await Maybe<T> => T; await Result<T,E> => T; else Unknown
      if (e.target.kind === 'Name' && e.target.name === 'await' && e.args.length === 1) {
        const at = typeOfExpr(ctx, env, e.args[0]!, diags);
        if (!isUnknown(at) && at.kind === 'Maybe') return at.type as T;
        if (!isUnknown(at) && at.kind === 'Result') return at.ok as T;
        // Warn on unsafe await usage
        diags.push({
          severity: 'warning',
          message: `await expects Maybe<T> or Result<T,E>, got ${tToString(at)}`,
        });
      }
      return tUnknown();
    }
  }
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
        severity: 'warning',
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
  const walk = (blk: Core.Block): void => {
    for (const s of blk.statements) {
      switch (s.kind) {
        case 'Start':
          started.add(s.name);
          break;
        case 'Wait':
          s.names.forEach(n => waited.add(n));
          break;
        case 'If':
          walk(s.thenBlock);
          if (s.elseBlock) walk(s.elseBlock);
          break;
        case 'Match':
          for (const c of s.cases) {
            if (c.body.kind !== 'Return') walk(c.body);
          }
          break;
        case 'Scope':
          walk({ kind: 'Block', statements: s.statements });
          break;
        default:
          break;
      }
    }
  };
  walk(b);
  return { started, waited };
}
