/**
 * @module lower_to_core
 *
 * AST 到 Core IR 的降级器：将高级 AST 转换为小型、严格的 Core IR。
 *
 * **功能**：
 * - 将 AST 节点转换为 Core IR 节点
 * - 展开语法糖（如 `User?` → `Maybe of User`）
 * - 规范化表达式和语句结构
 * - 保留源代码位置信息（origin）
 * - 为后续的类型检查和代码生成准备简化的 IR
 *
 * **Core IR 设计**：
 * - 更小的节点集合（相比 AST）
 * - 显式的作用域节点（Scope）
 * - 规范化的模式匹配
 * - 简化的效果标注
 */

import { Core } from './core_ir.js';
import { parseEffect, getAllEffects } from './config/semantic.js';
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
import { DefaultAstVisitor } from './ast_visitor.js';

/**
 * 将 AST Module 降级为 Core IR Module。
 *
 * 这是 Aster 编译管道的第四步，将高级的抽象语法树转换为更简洁、更严格的 Core IR，
 * 以便进行类型检查和后续的代码生成。
 *
 * **降级转换**：
 * - 展开语法糖（`User?` → `Maybe of User`、`Result of A and B` → 标准 Result 类型）
 * - 规范化函数体为 Block 和 Statement 序列
 * - 将模式匹配转换为 Core IR 的 Case 结构
 * - 保留原始位置信息用于错误报告
 *
 * @param ast - AST Module 节点（通过 parser.parse 生成）
 * @returns Core IR Module 节点，包含所有声明的规范化表示
 *
 * @example
 * ```typescript
 * import { canonicalize, lex, parse, lowerModule } from '@wontlost-ltd/aster-lang';
 *
 * const src = `This module is app.
 * To greet, produce Text:
 *   Return "Hello".
 * `;
 *
 * const ast = parse(lex(canonicalize(src)));
 * const core = lowerModule(ast);
 *
 * console.log(core.kind);  // "Module"
 * console.log(core.name);  // "app"
 * // Core IR 是更简洁的表示，适合类型检查和代码生成
 * ```
 */
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
  // 严格校验效果字符串，拒绝未知值
  const effects = (f.effects || []).map(e => {
    const effect = parseEffect(e.toLowerCase());
    if (effect === null) {
      const validEffects = getAllEffects().join(', ');
      throw new Error(`未知的 effect '${e}'，有效值为：${validEffects}`);
    }
    return effect;
  });
  const body = f.body ? lowerBlock(f.body) : Core.Block([]);
  const out = Core.Func(f.name, f.typeParams ?? [], params, ret, effects, body);
  // Pass through capability metadata if present
  const caps = (f as any).effectCaps as readonly import('./config/semantic.js').CapabilityKind[] | undefined;
  if (caps && caps.length > 0) {
    (out as any).effectCaps = [...caps];
    if ((f as any).effectCapsExplicit !== undefined) {
      (out as any).effectCapsExplicit = (f as any).effectCapsExplicit;
    }
  }
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
      // Parser now prohibits bare expressions, so this should never be reached
      throw new Error(`lowerStmt: 未处理的语句类型 '${(s as any).kind}'`);
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
    case 'Await':
      {
        const out = Core.Await(lowerExpr(e.expr));
        const o = spanToOrigin((e as any).span, (e as any).file ?? null);
        if (o) (out as any).origin = o;
        return out;
      }
    case 'Lambda': {
      // 使用统一 AST 访客进行捕获变量收集
      const paramNames = new Set(e.params.map(p => p.name));
      const names = new Set<string>();
      class CaptureVisitor extends DefaultAstVisitor<void> {
        override visitExpression(ex: Expression): void {
          if (ex.kind === 'Name' && !paramNames.has(ex.name) && !ex.name.includes('.')) {
            names.add(ex.name);
          }
          super.visitExpression(ex, undefined as unknown as void);
        }
      }
      new CaptureVisitor().visitBlock(e.body, undefined as unknown as void);
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
    case 'TypePii':
      {
        const out = Core.Pii(lowerType(t.baseType), t.sensitivity, t.category);
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
    case 'TypePii':
      return Core.Pii(lowerTypeWithVars(t.baseType, vars), t.sensitivity, t.category);
    default:
      return lowerType(t);
  }
}
