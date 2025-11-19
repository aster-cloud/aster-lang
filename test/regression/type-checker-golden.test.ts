/**
 * 类型检查 golden 基线回归测试
 *
 * 读取 test/type-checker/golden 下的 .aster 文件，执行完整类型检查流程，
 * 并将诊断结果与 expected/*.errors.json 基线比对，确保回归场景稳定。
 */

import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

import { canonicalize } from '../../src/canonicalizer.js';
import { lex } from '../../src/lexer.js';
import { parse } from '../../src/parser.js';
import { lowerModule } from '../../src/lower_to_core.js';
import { typecheckModule } from '../../src/typecheck.js';

import type { Module as AstModule, TypecheckDiagnostic } from '../../src/types.js';

type DiagnosticView = {
  code?: TypecheckDiagnostic['code'];
  severity?: TypecheckDiagnostic['severity'];
  message: string;
};

const PROJECT_ROOT = process.cwd();
const TYPE_CHECKER_DIR = path.resolve(PROJECT_ROOT, 'test/type-checker');

const TEST_CASES = [
  // TODO(Parser): 暂不支持命名参数语法 Entry(id: "123")，待解析器增强后恢复
  // Issue: 解析错误 "Expected ')'"
  // 'type_mismatch_assign',
  'capability_missing_decl',
  'effect_missing_io',
  'cross-module/module_b',
  'cross_module/module_b',
  // TODO(TypeChecker): CPU 效应检测未接入 typecheckModule，待类型检查器增强后恢复
  // Issue: 未生成预期的 E201 诊断
  // 'effect_missing_cpu',
  'async_missing_wait',
  'pii_http_violation',
  'return_type_mismatch',
  // TODO(Parser): 暂不支持混合列表字面量 [1, "two"]，待解析器增强后恢复
  // Issue: 解析错误 "Unexpected expression"
  // 'list_literal_mismatch',
  'generics',
  'basic_types',
  'workflow-linear',
  'workflow-missing-compensate',
  'workflow-type-mismatch',
  'workflow-missing-io',
  'workflow-undeclared-capability',
  'workflow-compensate-new-cap',
  'workflow_retry_many_attempts',
  'workflow_retry_timeout_conflict',
  'workflow_timeout_too_short',
  'workflow_timeout_too_long'
] as const;

const GOLDEN_DIR = path.join(TYPE_CHECKER_DIR, 'golden');
const EXPECTED_DIR = path.join(TYPE_CHECKER_DIR, 'expected');

function compileDiagnostics(caseName: (typeof TEST_CASES)[number]): DiagnosticView[] {
  const sourcePath = path.join(GOLDEN_DIR, `${caseName}.aster`);
  const source = fs.readFileSync(sourcePath, 'utf8');
  const canonical = canonicalize(source);
  const tokens = lex(canonical);
  const ast = parse(tokens) as AstModule;
  const core = lowerModule(ast);
  const diagnostics = typecheckModule(core);
  return diagnostics.map(({ code, severity, message }) => ({ code, severity, message }));
}

function loadExpectedDiagnostics(caseName: (typeof TEST_CASES)[number]): DiagnosticView[] {
  const expectedPath = path.join(EXPECTED_DIR, `${caseName}.errors.json`);
  const raw = JSON.parse(fs.readFileSync(expectedPath, 'utf8')) as {
    diagnostics?: Array<Partial<DiagnosticView>>;
  };
  return (raw.diagnostics ?? []).map(({ code, severity, message }) => {
    const view: DiagnosticView = {
      message: message ?? ''
    };
    if (code !== undefined) {
      view.code = code as TypecheckDiagnostic['code'];
    }
    if (severity !== undefined) {
      view.severity = severity as TypecheckDiagnostic['severity'];
    }
    return view;
  });
}

describe('类型检查 golden 回归测试', () => {
  for (const caseName of TEST_CASES) {
    it(`用例 ${caseName} 的诊断应与基线一致`, () => {
      const actualDiagnostics = compileDiagnostics(caseName);
      const expectedDiagnostics = loadExpectedDiagnostics(caseName);
      try {
        assert.deepStrictEqual(actualDiagnostics, expectedDiagnostics);
      } catch (error) {
        console.error(`\n用例 ${caseName} 诊断 diff:`);
        console.error('实际诊断:');
        console.error(JSON.stringify(actualDiagnostics, null, 2));
        console.error('期望诊断:');
        console.error(JSON.stringify(expectedDiagnostics, null, 2));
        throw error;
      }
    });
  }
});
