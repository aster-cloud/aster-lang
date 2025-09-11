import type { Core } from './types.js';
// import { DiagnosticBuilder, DiagnosticCode, DiagnosticSeverity } from './diagnostics.js';
import { IO_PREFIXES, CPU_PREFIXES } from './config/effects.js';

export interface TypecheckDiagnostic {
  severity: 'error' | 'warning';
  message: string;
}

// Internal representation for types during checking
type UnknownT = { kind: 'Unknown' };
type T = Core.Type | UnknownT;

function isUnknown(x: T): x is UnknownT {
  return (x as { kind: string }).kind === 'Unknown';
}

function tUnknown(): T { return { kind: 'Unknown' }; }

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
  }
}

function tToString(t: T): string {
  if (isUnknown(t)) return 'Unknown';
  switch (t.kind) {
    case 'TypeName':
      return t.name;
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
  }
}

interface Env {
  vars: Map<string, T>;
}

interface ModuleContext {
  datas: Map<string, Core.Data>;
  enums: Map<string, Core.Enum>;
}

export function typecheckModule(m: Core.Module): TypecheckDiagnostic[] {
  const diags: TypecheckDiagnostic[] = [];
  const ctx: ModuleContext = { datas: new Map(), enums: new Map() };
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
    });
  if (!effs.has('io') && hasIO)
    diags.push({
      severity: 'warning',
      message: `Function '${f.name}' declares @io but no obvious I/O found.`,
    });
  if (effs.has('cpu') && !hasCPU)
    diags.push({
      severity: 'warning',
      message: `Function '${f.name}' may perform CPU-bound work but is missing @cpu effect.`,
    });
  if (!effs.has('cpu') && hasCPU)
    diags.push({
      severity: 'warning',
      message: `Function '${f.name}' declares @cpu but no obvious CPU-bound work found.`,
    });
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
      default:
        break;
    }
  }
  return effs;
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars

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
      const tElse = s.elseBlock ? typecheckBlock(ctx, cloneEnv(env), s.elseBlock, diags) : tUnknown();
      return tThen || tElse;
    }
    case 'Match': {
      const et = typeOfExpr(ctx, env, s.expr, diags);
      // Evaluate each case body type; require all equal (when known)
      let out: T | null = null;
      let sawNull = false;
      let sawCtor = false;
      for (const c of s.cases) {
        if (c.pattern.kind === 'PatNull') sawNull = true;
        if (c.pattern.kind === 'PatCtor') sawCtor = true;
        const t = typecheckCase(ctx, env, c, et, diags);
        if (!out) out = t;
        else if (!tEquals(out, t)) {
          diags.push({
            severity: 'error',
            message: `Match case return types differ: ${tToString(out)} vs ${tToString(t)}`,
          });
        }
      }
      // Exhaustiveness
      if (!isUnknown(et) && et.kind === 'Maybe') {
        if (!(sawNull && sawCtor)) {
          warn(diags, `Non-exhaustive match on Maybe type; handle both null and value.`);
        }
      } else if (!isUnknown(et) && et.kind === 'TypeName') {
        const enumDecl = ctx.enums.get(et.name);
        if (enumDecl) {
          const seen = new Set<string>();
          for (const c of s.cases) {
            if (c.pattern.kind === 'PatName') seen.add(c.pattern.name);
            if (c.pattern.kind === 'PatCtor') seen.add(c.pattern.typeName);
          }
          const missing = enumDecl.variants.filter(v => !seen.has(v));
          if (missing.length > 0) {
            warn(
              diags,
              `Non-exhaustive match on ${enumDecl.name}; missing cases: ${missing.join(', ')}`
            );
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
    const d = ctx.datas.get(c.pattern.typeName);
    if (d) {
      if (d.fields.length !== c.pattern.names.length) {
        diags.push({
          severity: 'error',
          message: `Constructor ${d.name} arity ${d.fields.length} does not match pattern (${c.pattern.names.length}).`,
        });
      }
      for (let i = 0; i < Math.min(d.fields.length, c.pattern.names.length); i++) {
        const nm = c.pattern.names[i]!;
        const ft = d.fields[i]!.type as T;
        env2.vars.set(nm, ft);
      }
    } else {
      // unknown ctor; bind names as Unknown
      for (const n of c.pattern.names) env2.vars.set(n, tUnknown());
    }
  } else if (c.pattern.kind === 'PatName') {
    env2.vars.set(c.pattern.name, et);
  }
  // Body
  if (c.body.kind === 'Return') return typeOfExpr(ctx, env2, c.body.expr, diags);
  return typecheckBlock(ctx, env2, c.body, diags);
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
    case 'String':
      return { kind: 'TypeName', name: 'Text' } as Core.TypeName;
    case 'Null':
      return { kind: 'Maybe', type: UNKNOWN_TYPENAME } as T;
    case 'Ok': {
      const inner = typeOfExpr(ctx, env, e.expr, diags);
      return { kind: 'Result', ok: isUnknown(inner) ? UNKNOWN_TYPENAME : (inner as Core.Type), err: UNKNOWN_TYPENAME } as T;
    }
    case 'Err': {
      const inner = typeOfExpr(ctx, env, e.expr, diags);
      return { kind: 'Result', ok: UNKNOWN_TYPENAME, err: isUnknown(inner) ? UNKNOWN_TYPENAME : (inner as Core.Type) } as T;
    }
    case 'Some': {
      const inner = typeOfExpr(ctx, env, e.expr, diags);
      return { kind: 'Option', type: isUnknown(inner) ? UNKNOWN_TYPENAME : (inner as Core.Type) } as T;
    }
    case 'None':
      return { kind: 'Option', type: UNKNOWN_TYPENAME } as T;
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
