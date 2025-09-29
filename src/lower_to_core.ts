import { Core, Effect } from './core_ir.js';
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
  return Core.Module(ast.name, decls);
}

function lowerDecl(d: Declaration): import('./types.js').Core.Declaration {
  switch (d.kind) {
    case 'Import':
      return Core.Import(d.name, d.asName || null);
    case 'Data':
      return Core.Data(
        d.name,
        d.fields.map(f => ({ name: f.name, type: lowerType(f.type) }))
      );
    case 'Enum':
      return Core.Enum(d.name, [...d.variants]);
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
  return Core.Func(f.name, f.typeParams ?? [], params, ret, effects, body);
}

function lowerBlock(b: Block): import('./types.js').Core.Block {
  return Core.Block(b.statements.map(lowerStmt));
}

function lowerStmt(s: Statement): import('./types.js').Core.Statement {
  switch (s.kind) {
    case 'Let':
      return Core.Let(s.name, lowerExpr(s.expr));
    case 'Set':
      return Core.Set(s.name, lowerExpr(s.expr));
    case 'Return':
      return Core.Return(lowerExpr(s.expr));
    case 'If':
      return Core.If(
        lowerExpr(s.cond),
        lowerBlock(s.thenBlock),
        s.elseBlock ? lowerBlock(s.elseBlock) : null
      );
    // Scope sugar lowering placeholder: if a Block starts with a special marker, we could emit Scope. For now none.

    case 'Match':
      return Core.Match(
        lowerExpr(s.expr),
        s.cases.map(c => Core.Case(lowerPattern(c.pattern), lowerCaseBody(c.body)))
      );
    case 'Start':
      return Core.Start(s.name, lowerExpr(s.expr));
    case 'Wait':
      return Core.Wait(s.names);
    case 'Block':
      return Core.Scope((s as Block).statements.map(lowerStmt));
    default:
      // For bare expressions evaluated for side-effects in v0: wrap as Return(expr) for now
      return Core.Return(lowerExpr(s as Expression));
  }
}

function lowerCaseBody(
  body: import('./types.js').Return | Block
): import('./types.js').Core.Return | import('./types.js').Core.Block {
  // Body can be a Return node or a Block
  if (body.kind === 'Return') return Core.Return(lowerExpr(body.expr));
  return lowerBlock(body);
}

function lowerExpr(e: Expression): import('./types.js').Core.Expression {
  switch (e.kind) {
    case 'Name':
      return Core.Name(e.name);
    case 'Bool':
      return Core.Bool(e.value);
    case 'Int':
      return Core.Int(e.value);
    case 'Long':
      return Core.Long(e.value);
    case 'Double':
      return Core.Double(e.value);
    case 'String':
      return Core.String(e.value);
    case 'Null':
      return Core.Null();
    case 'Call':
      return Core.Call(lowerExpr(e.target), e.args.map(lowerExpr));
    case 'Construct':
      return Core.Construct(
        e.typeName,
        e.fields.map(f => ({ name: f.name, expr: lowerExpr(f.expr) }))
      );
    case 'Ok':
      return Core.Ok(lowerExpr(e.expr));
    case 'Err':
      return Core.Err(lowerExpr(e.expr));
    case 'Some':
      return Core.Some(lowerExpr(e.expr));
    case 'None':
      return Core.None();
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
      return Core.PatNull();
    case 'PatternInt':
      return Core.PatInt(p.value);
    case 'PatternCtor': {
      const ctor = p as Pattern & { args?: readonly Pattern[] };
      const args = ctor.args ? ctor.args.map(pp => lowerPattern(pp)) : undefined;
      return Core.PatCtor(p.typeName, [...(p.names ?? [])], args);
    }
    case 'PatternName':
      return Core.PatName(p.name);
    default:
      throw new Error(`Unknown pattern kind: ${(p as { kind: string }).kind}`);
  }
}

function lowerType(t: Type): import('./types.js').Core.Type {
  switch (t.kind) {
    case 'TypeName':
      return Core.TypeName(t.name);
    case 'TypeVar':
      return Core.TypeVar(t.name);
    case 'TypeApp':
      return Core.TypeApp(t.base, t.args.map(lowerType));
    case 'Maybe':
      return Core.Maybe(lowerType(t.type));
    case 'Option':
      return Core.Option(lowerType(t.type));
    case 'Result':
      return Core.Result(lowerType(t.ok), lowerType(t.err));
    case 'List':
      return Core.List(lowerType(t.type));
    case 'Map':
      return Core.Map(lowerType(t.key), lowerType(t.val));
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
