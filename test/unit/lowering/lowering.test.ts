import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { canonicalize } from '../../../src/canonicalizer.js';
import { lex } from '../../../src/lexer.js';
import { parse } from '../../../src/parser.js';
import { lowerModule } from '../../../src/lower_to_core.js';
import { Node } from '../../../src/ast.js';
import type { Core } from '../../../src/types.js';
import type {
  Module as AstModule,
  Statement as AstStatement,
  Parameter as AstParameter,
  Type as AstType,
} from '../../../src/types.js';

function lower(source: string): Core.Module {
  const canonical = canonicalize(source);
  const tokens = lex(canonical);
  const ast = parse(tokens);
  return lowerModule(ast);
}

function lowerAst(module: AstModule): Core.Module {
  return lowerModule(module);
}

function freshSpan() {
  return {
    start: { line: 0, col: 0 },
    end: { line: 0, col: 0 },
  };
}

function makeParam(name: string, type: AstType): AstParameter {
  return { name, type, span: freshSpan() };
}

function makeFunc(options: {
  name: string;
  params?: readonly AstParameter[];
  retType: AstType;
  statements: readonly AstStatement[];
}): AstModule['decls'][number] {
  const body = Node.Block([...options.statements]);
  return Node.Func(
    options.name,
    [],
    options.params ?? [],
    options.retType,
    [],
    [],
    false,
    body
  );
}

describe('降级至 Core IR', () => {
  it('模块降级后应保留名称与声明数量', () => {
    const core = lower(`
This module is test.lowering.basic.

Define User with id: Text.

To ping, produce Text:
  Return "pong".
`);
    assert.equal(core.kind, 'Module');
    assert.equal(core.name, 'test.lowering.basic');
    assert.equal(core.decls.length, 2);
  });

  it('函数降级后应保留参数与返回类型', () => {
    const core = lower(`
This module is test.lowering.func_types.

To repeat with text: Text and times: Int, produce Text:
  Return text.
`);
    const func = core.decls.find(d => d.kind === 'Func') as Core.Func | undefined;
    assert.ok(func, '应该存在函数声明');
    assert.equal(func!.params.length, 2);
    assert.equal(func!.params[0]!.name, 'text');
    assert.equal(func!.ret.kind, 'TypeName');
    assert.equal(func!.effects.length, 0);
  });

  it('Return 语句应降级为 Core.Return', () => {
    const core = lower(`
This module is test.lowering.return_stmt.

To identity with value: Int, produce Int:
  Return value.
`);
    const func = core.decls.find(d => d.kind === 'Func') as Core.Func;
    const body = func.body.statements;
    assert.equal(body.length, 1);
    assert.equal(body[0]!.kind, 'Return');
  });

  it('Match 语句应降级并保留所有分支', () => {
    const core = lower(`
This module is test.lowering.match_stmt.

Define Result as one of Ok, Err.

To handle with result: Result, produce Int:
  Match result:
    When Ok, Return 1.
    When Err, Return 0.
`);
    const func = core.decls.find(d => d.kind === 'Func') as Core.Func;
    const matchStmt = func.body.statements[0]!;
    assert.equal(matchStmt.kind, 'Match');
    const matchCore = matchStmt as Core.Match;
    assert.equal(matchCore.cases.length, 2);
    assert.equal(matchCore.cases[0]!.pattern.kind, 'PatName');
  });

  it('Maybe 类型应降级为 Core.Maybe 包装类型', () => {
    const core = lower(`
This module is test.lowering.maybe_type.

To safeHead with items: List of Int, produce Int?:
  Return None.
`);
    const func = core.decls.find(d => d.kind === 'Func') as Core.Func;
    assert.equal(func.ret.kind, 'Maybe');
    assert.equal(func.ret.type.kind, 'TypeName');
    const returnStmt = func.body.statements[0]!;
    assert.equal(returnStmt.kind, 'Return');
  });

  it('Lambda 表达式应降级为 Core.Lambda 并保留参数信息', () => {
    const core = lower(`
This module is test.lowering.lambda_arrows.

To makeIdentity, produce Fn1:
  Return (value: Text) => value.
`);
    const func = core.decls.find(d => d.kind === 'Func') as Core.Func;
    const lambda = (func.body.statements[0] as Core.Return).expr as Core.Lambda;
    assert.equal(lambda.kind, 'Lambda');
    assert.equal(lambda.params.length, 1);
    assert.equal(lambda.params[0]!.name, 'value');
    assert.equal(lambda.ret.kind, 'TypeName');
  });

  it('Lambda 闭包应记录外部变量捕获列表', () => {
    const paramValue = makeParam('value', Node.TypeName('Int'));
    const funcParam = makeParam('base', Node.TypeName('Int'));
    const lambdaExpr = Node.Lambda(
      [paramValue],
      Node.TypeName('Int'),
      Node.Block([Node.Return(Node.Name('base'))])
    );
    const funcDecl = makeFunc({
      name: 'makeAdder',
      params: [funcParam],
      retType: Node.TypeName('Fn1'),
      statements: [Node.Return(lambdaExpr)],
    });
    const moduleAst = Node.Module('test.lowering.lambda_capture', [funcDecl]);
    const core = lowerAst(moduleAst);
    const func = core.decls.find(d => d.kind === 'Func') as Core.Func;
    const lambda = (func.body.statements[0] as Core.Return).expr as Core.Lambda;
    const captures = lambda.captures ? [...lambda.captures].sort() : [];
    assert.deepEqual(captures, ['base']);
  });

  it('Await 表达式应降级为 Core.Await', () => {
    const awaitExpr = Node.Await(Node.Name('taskResult'));
    const funcDecl = makeFunc({
      name: 'awaiter',
      retType: Node.TypeName('Text'),
      statements: [Node.Return(awaitExpr)],
    });
    const moduleAst = Node.Module('test.lowering.await_expr', [funcDecl]);
    const core = lowerAst(moduleAst);
    const func = core.decls[0] as Core.Func;
    const retExpr = (func.body.statements[0] as Core.Return).expr;
    assert.equal(retExpr.kind, 'Await');
    assert.equal((retExpr as Core.Await).expr.kind, 'Name');
  });

  it('Block 语句应降级为 Core.Scope 并保持嵌套顺序', () => {
    const inner = Node.Block([Node.Let('temp', Node.Int(1)), Node.Return(Node.Name('temp'))]);
    const funcDecl = makeFunc({
      name: 'scoped',
      retType: Node.TypeName('Int'),
      statements: [Node.Block([Node.Let('inner', Node.Int(2))]), inner],
    });
    const moduleAst = Node.Module('test.lowering.scope_block', [funcDecl]);
    const core = lowerAst(moduleAst);
    const func = core.decls[0] as Core.Func;
    assert.equal(func.body.statements[0]!.kind, 'Scope');
    const scope = func.body.statements[1]!;
    assert.equal(scope.kind, 'Scope');
    const scopeBody = scope as Core.Scope;
    assert.equal(scopeBody.statements[1]!.kind, 'Return');
  });

  it('Result/List map 调用应降级并保持 Lambda 参数信息', () => {
    const resultLambda = Node.Lambda(
      [makeParam('value', Node.TypeName('Int'))],
      Node.TypeName('Int'),
      Node.Block([Node.Return(Node.Name('value'))])
    );
    const resultCall = Node.Call(Node.Name('Result.map'), [Node.Name('res'), resultLambda]);
    const listLambda = Node.Lambda(
      [makeParam('item', Node.TypeName('Text'))],
      Node.TypeName('Text'),
      Node.Block([Node.Return(Node.Name('prefix'))])
    );
    const listCall = Node.Call(Node.Name('List.map'), [Node.Name('items'), listLambda]);
    const funcDecl = makeFunc({
      name: 'mapBoth',
      params: [makeParam('prefix', Node.TypeName('Text'))],
      retType: Node.TypeName('List'),
      statements: [Node.Return(resultCall), Node.Return(listCall)],
    });
    const moduleAst = Node.Module('test.lowering.map_calls', [funcDecl]);
    const core = lowerAst(moduleAst);
    const body = (core.decls[0] as Core.Func).body.statements;
    const resultExpr = (body[0] as Core.Return).expr as Core.Call;
    const listExpr = (body[1] as Core.Return).expr as Core.Call;
    assert.equal(resultExpr.kind, 'Call');
    assert.equal(resultExpr.args[1]?.kind, 'Lambda');
    const listLambdaCore = listExpr.args[1] as Core.Lambda;
    assert.deepEqual(listLambdaCore.captures ? [...listLambdaCore.captures] : [], ['prefix']);
  });
});
