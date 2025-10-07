import type {
  Module,
  Declaration,
  Block,
  Statement,
  Expression,
  Pattern,
  Type,
} from './types.js';

/**
 * 统一的 AST 遍历器接口与默认实现（只读遍历）。
 *
 * - 入口：visitModule/visitDeclaration/visitBlock/visitStatement/visitExpression
 * - 默认实现执行深度优先递归；子类可覆写特定 visit 方法并调用 super 继续遍历
 */
export interface AstVisitor<Ctx, R = void> {
  visitModule(m: Module, ctx: Ctx): R;
  visitDeclaration(d: Declaration, ctx: Ctx): R;
  visitBlock(b: Block, ctx: Ctx): R;
  visitStatement(s: Statement, ctx: Ctx): R;
  visitExpression(e: Expression, ctx: Ctx): R;
  visitPattern?(p: Pattern, ctx: Ctx): R;
  visitType?(t: Type, ctx: Ctx): R;
}

export class DefaultAstVisitor<Ctx> implements AstVisitor<Ctx, void> {
  // 可选钩子默认不实现，由子类按需覆写
  public visitType?(t: Type, ctx: Ctx): void;
  public visitPattern?(p: Pattern, ctx: Ctx): void;
  visitModule(m: Module, ctx: Ctx): void {
    for (const d of m.decls) this.visitDeclaration(d, ctx);
  }

  visitDeclaration(d: Declaration, ctx: Ctx): void {
    switch (d.kind) {
      case 'Import':
        return;
      case 'Data':
        for (const f of d.fields ?? []) this.visitType?.(f.type, ctx);
        return;
      case 'Enum':
        return;
      case 'Func':
        for (const p of d.params) this.visitType?.(p.type, ctx);
        this.visitType?.(d.retType, ctx);
        if (d.body) this.visitBlock(d.body, ctx);
        return;
    }
  }

  visitBlock(b: Block, ctx: Ctx): void {
    for (const s of b.statements) this.visitStatement(s, ctx);
  }

  visitStatement(s: Statement, ctx: Ctx): void {
    switch (s.kind) {
      case 'Let':
      case 'Set':
      case 'Return':
        this.visitExpression(s.expr, ctx);
        return;
      case 'If':
        this.visitExpression(s.cond, ctx);
        this.visitBlock(s.thenBlock, ctx);
        if (s.elseBlock) this.visitBlock(s.elseBlock, ctx);
        return;
      case 'Match':
        this.visitExpression(s.expr, ctx);
        for (const c of s.cases) {
          if (c.pattern) this.visitPattern?.(c.pattern, ctx);
          if (c.body.kind === 'Return') this.visitExpression(c.body.expr, ctx);
          else this.visitBlock(c.body, ctx);
        }
        return;
      case 'Start':
        this.visitExpression(s.expr, ctx);
        return;
      case 'Wait':
        return;
      case 'Block':
        this.visitBlock(s, ctx);
        return;
      default:
        // AST 的 Statement 联合包含 Expression，直接下派
        this.visitExpression(s as unknown as Expression, ctx);
        return;
    }
  }

  visitExpression(e: Expression, ctx: Ctx): void {
    switch (e.kind) {
      case 'Name':
      case 'Bool':
      case 'Int':
      case 'Long':
      case 'Double':
      case 'String':
      case 'Null':
      case 'None':
        return;
      case 'Call':
        this.visitExpression(e.target, ctx);
        for (const a of e.args) this.visitExpression(a, ctx);
        return;
      case 'Construct':
        for (const f of e.fields) this.visitExpression(f.expr, ctx);
        return;
      case 'Ok':
      case 'Err':
      case 'Some':
        this.visitExpression(e.expr, ctx);
        return;
      case 'Lambda':
        this.visitBlock(e.body, ctx);
        return;
      case 'Await':
        this.visitExpression(e.expr, ctx);
        return;
    }
  }
}
