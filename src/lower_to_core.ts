import { Core, Effect } from './core_ir.js';
import type { Span } from './types.js';
import type {
  Module,
  Declaration,
  Func,
  Block,
  Statement,
  Expression,
  Pattern,
  Type,
} from './types.js';

export function lowerModule(ast: Module): import('./types.js').Core.Module {
  const decls = ast.decls.map(lowerDecl);
  const m = Core.Module(ast.name, decls);
  const o = spanToOrigin((ast as any).span, (ast as any).file ?? null);
  if (o) (m as any).origin = o;
  return m;
}

function lowerDecl(d: Declaration): import('./types.js').Core.Declaration {
  switch (d.kind) {
    case 'Import':
      {
        const out = Core.Import(d.name, d.asName || null);
        const o = spanToOrigin((d as any).span, (d as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Data':
      {
        const out = Core.Data(
          d.name,
          d.fields.map(f => ({ name: f.name, type: lowerType(f.type) }))
        );
        const o = spanToOrigin((d as any).span, (d as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Enum':
      {
        const out = Core.Enum(d.name, [...d.variants]);
        const o = spanToOrigin((d as any).span, (d as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Func':
      return lowerFunc(d);
    default:
      throw new Error(`Unknown decl kind: ${(d as { kind: string }).kind}`);
  }
}

function lowerFunc(f: Func): import('./types.js').Core.Func {
  const tvars = new Set<string>(f.typeParams ?? []);
  const params = f.params.map(p => ({ name: p.name, type: lowerTypeWithVars(p.type, tvars) }));
  const ret = lowerTypeWithVars(f.retType, tvars);
  const effects = (f.effects || []).map(e => (e === 'io' ? Effect.IO : Effect.CPU));
  const body = f.body ? lowerBlock(f.body) : Core.Block([]);
  const out = Core.Func(f.name, f.typeParams ?? [], params, ret, effects, body);
  const o = spanToOrigin((f as any).span, (f as any).file ?? null);
  if (o) (out as any).origin = o;
  return out;
}

function lowerBlock(b: Block): import('./types.js').Core.Block {
  const out = Core.Block(b.statements.map(lowerStmt));
  const o = spanToOrigin((b as any).span, (b as any).file ?? null);
  if (o) (out as any).origin = o;
  return out;
}

function lowerStmt(s: Statement): import('./types.js').Core.Statement {
  switch (s.kind) {
    case 'Let':
      {
        const out = Core.Let(s.name, lowerExpr(s.expr));
        const o = spanToOrigin((s as any).span, (s as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Set':
      {
        const out = Core.Set(s.name, lowerExpr(s.expr));
        const o = spanToOrigin((s as any).span, (s as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Return':
      {
        const out = Core.Return(lowerExpr(s.expr));
        const o = spanToOrigin((s as any).span, (s as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'If':
      {
        const out = Core.If(
          lowerExpr(s.cond),
          lowerBlock(s.thenBlock),
          s.elseBlock ? lowerBlock(s.elseBlock) : null
        );
        const o = spanToOrigin((s as any).span, (s as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    // Scope sugar lowering placeholder: if a Block starts with a special marker, we could emit Scope. For now none.

    case 'Match':
      {
        const out = Core.Match(
          lowerExpr(s.expr),
          s.cases.map(c => {
            const cc = Core.Case(lowerPattern(c.pattern), lowerCaseBody(c.body));
            const co = spanToOrigin((c as any).span, (c as any).file ?? null);
            if (co) (cc as any).origin = co;
            return cc;
          })
        );
        const o = spanToOrigin((s as any).span, (s as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Start':
      {
        const out = Core.Start(s.name, lowerExpr(s.expr));
        const o = spanToOrigin((s as any).span, (s as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Wait':
      {
        const out = Core.Wait(s.names);
        const o = spanToOrigin((s as any).span, (s as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Block':
      {
        const out = Core.Scope((s as Block).statements.map(lowerStmt));
        const o = spanToOrigin((s as any).span, (s as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    default:
      // For bare expressions evaluated for side-effects in v0: wrap as Return(expr) for now
      return Core.Return(lowerExpr(s as Expression));
  }
}

function lowerCaseBody(
  body: import('./types.js').Return | Block
): import('./types.js').Core.Return | import('./types.js').Core.Block {
  // Body can be a Return node or a Block
  if (body.kind === 'Return') {
    const out = Core.Return(lowerExpr(body.expr));
    const o = spanToOrigin((body as any).span, (body as any).file ?? null);
    if (o) (out as any).origin = o;
    return out;
  }
  return lowerBlock(body);
}

function lowerExpr(e: Expression): import('./types.js').Core.Expression {
  switch (e.kind) {
    case 'Name':
      {
        const out = Core.Name(e.name);
        const o = spanToOrigin((e as any).span, (e as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Bool':
      {
        const out = Core.Bool(e.value);
        const o = spanToOrigin((e as any).span, (e as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Int':
      {
        const out = Core.Int(e.value);
        const o = spanToOrigin((e as any).span, (e as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Long':
      {
        const out = Core.Long(e.value);
        const o = spanToOrigin((e as any).span, (e as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Double':
      {
        const out = Core.Double(e.value);
        const o = spanToOrigin((e as any).span, (e as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'String':
      {
        const out = Core.String(e.value);
        const o = spanToOrigin((e as any).span, (e as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Null':
      {
        const out = Core.Null();
        const o = spanToOrigin((e as any).span, (e as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Call':
      {
        const out = Core.Call(lowerExpr(e.target), e.args.map(lowerExpr));
        const o = spanToOrigin((e as any).span, (e as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Construct':
      {
        const out = Core.Construct(
          e.typeName,
          e.fields.map(f => ({ name: f.name, expr: lowerExpr(f.expr) }))
        );
        const o = spanToOrigin((e as any).span, (e as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Ok':
      {
        const out = Core.Ok(lowerExpr(e.expr));
        const o = spanToOrigin((e as any).span, (e as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Err':
      {
        const out = Core.Err(lowerExpr(e.expr));
        const o = spanToOrigin((e as any).span, (e as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Some':
      {
        const out = Core.Some(lowerExpr(e.expr));
        const o = spanToOrigin((e as any).span, (e as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'None':
      {
        const out = Core.None();
        const o = spanToOrigin((e as any).span, (e as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Lambda': {
      // Naive capture analysis: collect Name identifiers in body that are not params and not dotted
      const paramNames = new Set(e.params.map(p => p.name));
      const names = new Set<string>();
      const visitExpr = (ex: Expression): void => {
        switch (ex.kind) {
          case 'Name':
            if (!paramNames.has(ex.name) && !ex.name.includes('.')) names.add(ex.name);
            break;
          case 'Call':
            visitExpr(ex.target);
            ex.args.forEach(visitExpr);
            break;
          case 'Construct':
            ex.fields.forEach(f => visitExpr(f.expr));
            break;
          case 'Ok':
          case 'Err':
          case 'Some':
            visitExpr((ex as any).expr);
            break;
          default:
            break;
        }
      };
      const visitStmt = (s: Statement): void => {
        switch (s.kind) {
          case 'Let':
            visitExpr(s.expr);
            break;
          case 'Set':
            visitExpr(s.expr);
            break;
          case 'Return':
            visitExpr(s.expr);
            break;
          case 'If':
            visitExpr(s.cond);
            (s.thenBlock.statements || []).forEach(visitStmt);
            if (s.elseBlock) (s.elseBlock.statements || []).forEach(visitStmt);
            break;
          case 'Match':
            visitExpr(s.expr);
            s.cases.forEach(c => {
              if (c.body.kind === 'Return') visitExpr(c.body.expr);
              else (c.body.statements || []).forEach(visitStmt);
            });
            break;
          default:
            break;
        }
      };
      (e.body.statements || []).forEach(visitStmt);
      const captures = Array.from(names);
      const coreParams = e.params.map(p => ({ name: p.name, type: lowerType(p.type) }));
      const ret = lowerType(e.retType);
      return { kind: 'Lambda', params: coreParams, ret, body: lowerBlock(e.body), captures } as any;
    }
    default:
      throw new Error(`Unknown expr kind: ${(e as { kind: string }).kind}`);
  }
}

function lowerPattern(p: Pattern): import('./types.js').Core.Pattern {
  switch (p.kind) {
    case 'PatternNull':
      {
        const out = Core.PatNull();
        const o = spanToOrigin((p as any).span, (p as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'PatternInt':
      {
        const out = Core.PatInt(p.value);
        const o = spanToOrigin((p as any).span, (p as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'PatternCtor': {
      const ctor = p as Pattern & { args?: readonly Pattern[] };
      const args = ctor.args ? ctor.args.map(pp => lowerPattern(pp)) : undefined;
      const out = Core.PatCtor(p.typeName, [...(p.names ?? [])], args);
      const o = spanToOrigin((p as any).span, (p as any).file ?? null);
      if (o) (out as any).origin = o;
      return out;
    }
    case 'PatternName':
      {
        const out = Core.PatName(p.name);
        const o = spanToOrigin((p as any).span, (p as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    default:
      throw new Error(`Unknown pattern kind: ${(p as { kind: string }).kind}`);
  }
}

function spanToOrigin(span: Span | undefined, file: string | null): import('./types.js').Origin | null {
  if (!span) return null;
  return { file: file ?? undefined, start: span.start, end: span.end } as any;
}

function lowerType(t: Type): import('./types.js').Core.Type {
  switch (t.kind) {
    case 'TypeName': {
      const out = Core.TypeName(t.name);
      const o = spanToOrigin((t as any).span, (t as any).file ?? null);
      if (o) (out as any).origin = o;
      return out;
    }
    case 'TypeVar': {
      const out = Core.TypeVar(t.name);
      const o = spanToOrigin((t as any).span, (t as any).file ?? null);
      if (o) (out as any).origin = o;
      return out;
    }
    case 'TypeApp':
      {
        const out = Core.TypeApp(t.base, t.args.map(lowerType));
        const o = spanToOrigin((t as any).span, (t as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Maybe':
      {
        const out = Core.Maybe(lowerType(t.type));
        const o = spanToOrigin((t as any).span, (t as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Option':
      {
        const out = Core.Option(lowerType(t.type));
        const o = spanToOrigin((t as any).span, (t as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Result':
      {
        const out = Core.Result(lowerType(t.ok), lowerType(t.err));
        const o = spanToOrigin((t as any).span, (t as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'List':
      {
        const out = Core.List(lowerType(t.type));
        const o = spanToOrigin((t as any).span, (t as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Map':
      {
        const out = Core.Map(lowerType(t.key), lowerType(t.val));
        const o = spanToOrigin((t as any).span, (t as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    default:
      throw new Error(`Unknown type kind: ${(t as { kind: string }).kind}`);
  }
}

function lowerTypeWithVars(t: Type, vars: Set<string>): import('./types.js').Core.Type {
  switch (t.kind) {
    case 'TypeName':
      return vars.has(t.name)
        ? ({ kind: 'TypeVar', name: t.name } as import('./types.js').Core.TypeVar)
        : Core.TypeName(t.name);
    case 'Maybe':
      return Core.Maybe(lowerTypeWithVars(t.type, vars));
    case 'Option':
      return Core.Option(lowerTypeWithVars(t.type, vars));
    case 'Result':
      return Core.Result(lowerTypeWithVars(t.ok, vars), lowerTypeWithVars(t.err, vars));
    case 'List':
      return Core.List(lowerTypeWithVars(t.type, vars));
    case 'Map':
      return Core.Map(lowerTypeWithVars(t.key, vars), lowerTypeWithVars(t.val, vars));
    case 'TypeApp':
      return {
        kind: 'TypeApp',
        base: t.base,
        args: t.args.map(tt => lowerTypeWithVars(tt, vars)),
      } as import('./types.js').Core.Type;
    case 'TypeVar':
      return { kind: 'TypeVar', name: t.name } as import('./types.js').Core.TypeVar;
    default:
      return lowerType(t);
  }
}
