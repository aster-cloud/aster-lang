import { describe, test, it } from 'node:test';
import assert from 'node:assert/strict';
import { canonicalize } from '../../../src/canonicalizer.js';
import { lex } from '../../../src/lexer.js';
import { parse } from '../../../src/parser.js';
import type { Module, Declaration, Statement } from '../../../src/types.js';
import { CapabilityKind } from '../../../src/config/semantic.js';

function parseSource(source: string): Module {
  const canonical = canonicalize(source);
  const tokens = lex(canonical);
  return parse(tokens);
}

function findDecl<K extends Declaration['kind']>(module: Module, kind: K): Extract<Declaration, { kind: K }> {
  const decl = module.decls.find(
    (candidate): candidate is Extract<Declaration, { kind: K }> => candidate.kind === kind
  );
  assert.ok(decl, `应该找到 ${kind} 声明`);
  return decl;
}

function findFunc(module: Module, name: string): Extract<Declaration, { kind: 'Func' }> {
  const func = module.decls.find(
    (decl): decl is Extract<Declaration, { kind: 'Func' }> => decl.kind === 'Func' && decl.name === name
  );
  assert.ok(func, `应该找到函数 ${name}`);
  return func;
}

describe('语法分析器', () => {
  test('应该解析模块名称', () => {
    const module = parseSource(`
This module is test.parser.module_name.

To ping, produce Text:
  Return "pong".
`);
    assert.equal(module.name, 'test.parser.module_name');
  });

  test('应该解析数据类型字段', () => {
    const module = parseSource(`
This module is test.parser.data_decl.

Define User with id: Text, name: Text, age: Int.
`);
    const data = findDecl(module, 'Data');
    assert.equal(data.fields.length, 3);
    assert.equal(data.fields[0]!.name, 'id');
    assert.equal(data.fields[0]!.type.kind, 'TypeName');
    assert.equal(data.fields[2]!.type.kind, 'TypeName');
  });

  test('应该解析枚举变体列表', () => {
    const module = parseSource(`
This module is test.parser.enum_decl.

Define Status as one of Pending, Success, Failure.
`);
    const en = findDecl(module, 'Enum');
    assert.deepEqual(en.variants, ['Pending', 'Success', 'Failure']);
  });

  test('应该解析函数的参数与返回类型', () => {
    const module = parseSource(`
This module is test.parser.func_signature.

To format with name: Text and times: Int, produce Text:
  Return Text.concat(name, Text.toString(times)).
`);
    const func = findDecl(module, 'Func');
    assert.equal(func.name, 'format');
    assert.equal(func.params.length, 2);
    assert.equal(func.params[0]!.name, 'name');
    assert.equal(func.retType.kind, 'TypeName');
  });

  test('应该解析函数体中的 Return 语句', () => {
    const module = parseSource(`
This module is test.parser.return_stmt.

To identity with value: Text, produce Text:
  Return value.
`);
    const func = findDecl(module, 'Func');
    const statements = func.body?.statements ?? [];
    assert.equal(statements.length, 1);
    assert.equal(statements[0]!.kind, 'Return');
  });

  test('应该解析 Let 语句并构建调用表达式', () => {
    const module = parseSource(`
This module is test.parser.let_stmt.

To greet with name: Text, produce Text:
  Let trimmed be Text.trim(name).
  Return Text.concat("Hi, ", trimmed).
`);
    const func = findDecl(module, 'Func');
    const letStmt = func.body?.statements[0]!;
    assert.equal(letStmt.kind, 'Let');
    assert.equal(letStmt.name, 'trimmed');
    assert.equal(letStmt.expr.kind, 'Call');
  });

  test('应该解析 If 语句并生成 then/else 分支', () => {
    const module = parseSource(`
This module is test.parser.if_stmt.

To classify with score: Int, produce Text:
  If >=(score, 800),:
    Return "Top".
  Otherwise,:
    Return "Regular".
`);
    const func = findDecl(module, 'Func');
    const statements = func.body?.statements ?? [];
    const ifStmt = statements.find(
      (statement): statement is Statement & { kind: 'If' } => statement.kind === 'If'
    );
    assert.ok(ifStmt, '应该找到 If 语句');
    assert.equal(ifStmt!.thenBlock.statements[0]!.kind, 'Return');
    assert.equal(ifStmt!.elseBlock?.statements[0]!.kind, 'Return');
  });

  test('应该解析 Match 表达式及其分支', () => {
    const module = parseSource(`
This module is test.parser.match_stmt.

Define User with id: Text, name: Text.

To welcome with user: User?, produce Text:
  Match user:
    When null, Return "Guest".
    When User(id, name), Return Text.concat("Hi ", name).
`);
    const func = findDecl(module, 'Func');
    const matchStmt = func.body?.statements[0]!;
    assert.equal(matchStmt.kind, 'Match');
    const matchExpr = matchStmt as Extract<Statement, { kind: 'Match' }>;
    assert.equal(matchExpr.cases.length, 2);
    assert.equal(matchExpr.cases[1]!.pattern.kind, 'PatternCtor');
  });

  test('应该解析 Start/Wait 异步语句', () => {
    const module = parseSource(`
This module is test.parser.async_stmt.

To runTasks, produce Text. It performs io:
  Start task as async fetch().
  Wait for task.
  Return "done".

To fetch, produce Text:
  Return "ok".
`);
    const func = findDecl(module, 'Func');
    const statements = func.body?.statements ?? [];
    assert.equal(statements[0]!.kind, 'Start');
    assert.equal(statements[1]!.kind, 'Wait');
  });

  test('应该在语法错误时抛出诊断', () => {
    assert.throws(
      () =>
        parseSource(`
This module is test.parser.error.

Define Broken with x: Int
`),
      /expected '.'/i
    );
  });

  describe('边界场景', () => {
    it('应该解析 import 别名并保持调用目标一致', () => {
      const module = parseSource(`
This module is test.parser.import_alias.

Use Http as H.

To call, produce Text:
  Return H.get().
`);
      const importDecl = findDecl(module, 'Import');
      assert.equal(importDecl.name, 'Http');
      assert.equal(importDecl.asName, 'H');

      const func = findFunc(module, 'call');
      assert.ok(func.body, '函数体不能为空');
      if (!func.body) {
        assert.fail('缺少函数体');
      }
      const statement = func.body.statements[0];
      assert.ok(statement, '应该存在函数体语句');
      if (!statement || statement.kind !== 'Return') {
        assert.fail('第一条语句应为 Return');
      }
      const callExpr = statement.expr;
      if (callExpr.kind !== 'Call') {
        assert.fail('Return 表达式应为函数调用');
      }
      assert.equal(callExpr.target.kind, 'Name');
      assert.equal(callExpr.target.name, 'H.get');
    });

    it('应该解析空效果列表与多基础效果组合', () => {
      const module = parseSource(`
This module is test.parser.effects_basic.

To audit, produce Int. It performs [].

To compute with value: Int, produce Int. It performs io and cpu.
`);
      const audit = findFunc(module, 'audit');
      assert.deepEqual(audit.effects, []);
      assert.equal(audit.effectCaps.length, 0);
      assert.equal(audit.effectCapsExplicit, false);

      const compute = findFunc(module, 'compute');
      assert.deepEqual(compute.effects, ['io', 'cpu']);
      assert.deepEqual(compute.effectCaps, [
        CapabilityKind.HTTP,
        CapabilityKind.SQL,
        CapabilityKind.TIME,
        CapabilityKind.FILES,
        CapabilityKind.SECRETS,
        CapabilityKind.AI_MODEL,
        CapabilityKind.CPU,
      ]);
      assert.equal(compute.effectCapsExplicit, false);
    });

    it('应该解析显式 capability 列表并保留效果体', () => {
      const module = parseSource(`
This module is test.parser.effects_explicit.

To fetch, produce Text. It performs io with Http and Sql:
  Return "ok".
`);
      const func = findFunc(module, 'fetch');
      assert.deepEqual(func.effects, ['io']);
      assert.deepEqual(func.effectCaps, [CapabilityKind.HTTP, CapabilityKind.SQL]);
      assert.equal(func.effectCapsExplicit, true);
      assert.ok(func.body, '显式 capability 应允许生成函数体');
    });

    it('应该解析注解并保留参数信息', () => {
      const module = parseSource(`
This module is test.parser.annotations.

Define User with
  @NotEmpty id: Text,
  @Range(min: 0, max: 120) @Pattern(regexp: "^[0-9]+$") age: Int.

To validate with @NotEmpty input: Text, produce Bool:
  Return true.
`);
      const data = findDecl(module, 'Data');
      const idField = data.fields[0];
      assert.ok(idField, '应该存在第一个字段');
      if (!idField) {
        assert.fail('缺少第一个字段');
      }
      assert.ok(idField.annotations && idField.annotations.length === 1);
      assert.equal(idField.annotations![0]!.name, 'NotEmpty');

      const ageField = data.fields[1];
      assert.ok(ageField, '应该存在第二个字段');
      if (!ageField) {
        assert.fail('缺少第二个字段');
      }
      assert.ok(ageField.annotations && ageField.annotations.length === 2);
      assert.deepEqual(
        ageField.annotations!.map(annotation => annotation.name),
        ['Range', 'Pattern']
      );
      const rangeParams = ageField.annotations![0]!.params;
      const patternParams = ageField.annotations![1]!.params;
      assert.equal(rangeParams.get('min'), 0);
      assert.equal(rangeParams.get('max'), 120);
      assert.equal(patternParams.get('regexp'), '^[0-9]+$');

      const func = findFunc(module, 'validate');
      const param = func.params[0];
      assert.ok(param, '函数参数应该存在');
      if (!param) {
        assert.fail('缺少函数参数');
      }
      assert.ok(param.annotations && param.annotations.length === 1);
      assert.equal(param.annotations![0]!.name, 'NotEmpty');
    });

    it('应该在缺失参数分隔符时报告诊断', () => {
      assert.throws(
        () =>
          parseSource(`
This module is test.parser.error.missing_separator.

To broken with first: Int second: Int, produce Int:
  Return first.
`),
        error => {
          assert.match(String(error), /Expected 'produce' and return type/i);
          return true;
        }
      );
    });

    it('应该在括号不匹配时报告诊断', () => {
      assert.throws(
        () =>
          parseSource(`
This module is test.parser.error.parentheses.

To fail with value: Text, produce Text:
  Return (value.
`),
        error => {
          assert.ok(
            String(error).includes("Expected ')' after expression"),
            '诊断信息应该指出括号缺失'
          );
          return true;
        }
      );
    });
  });
});
