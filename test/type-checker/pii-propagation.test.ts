import { describe, it, before, after } from 'node:test';
import assert from 'node:assert/strict';

import type { Core } from '../../src/types.js';
import { Effect } from '../../src/config/semantic.js';
import { Core as CoreBuilder } from '../../src/core_ir.js';
import { typecheckModule } from '../../src/typecheck.js';
import { ErrorCode } from '../../src/error_codes.js';

const IO_EFFECT: readonly Effect[] = [Effect.IO];
const PURE_EFFECT: readonly Effect[] = [Effect.PURE];

const TEXT = (): Core.Type => CoreBuilder.TypeName('Text');
const piiType = (level: 'L1' | 'L2' | 'L3', category: Core.PiiType['category']): Core.Type =>
  CoreBuilder.Pii(TEXT(), level, category);

const plainParam = (name: string): Core.Parameter => ({ name, type: TEXT(), annotations: [] });
const piiParam = (name: string, level: 'L1' | 'L2' | 'L3'): Core.Parameter => ({
  name,
  type: piiType(level, 'email'),
  annotations: [],
});

function makeFunc(options: {
  name: string;
  params: readonly Core.Parameter[];
  ret: Core.Type;
  body: readonly Core.Statement[];
  effects?: readonly Effect[];
}): Core.Func {
  return CoreBuilder.Func(
    options.name,
    [],
    options.params,
    options.ret,
    options.effects ?? PURE_EFFECT,
    CoreBuilder.Block(options.body),
    [],
    false
  );
}

function runModule(funcs: readonly Core.Func[]) {
  const module: Core.Module = CoreBuilder.Module('tests.pii', funcs);
  return typecheckModule(module);
}

describe('PII propagation diagnostics', () => {
  const prevAster = process.env.ASTER_ENFORCE_PII;
  const prevEnforce = process.env.ENFORCE_PII;
  const prevDisable = process.env.DISABLE_PII;

  before(() => {
    // 确保 PII 检查启用（显式设置启用标志）
    delete process.env.DISABLE_PII;
    process.env.ENFORCE_PII = 'true'; // 显式启用 PII 检查（渐进式策略）
  });

  after(() => {
    if (prevAster === undefined) delete process.env.ASTER_ENFORCE_PII;
    else process.env.ASTER_ENFORCE_PII = prevAster;
    if (prevEnforce === undefined) delete process.env.ENFORCE_PII;
    else process.env.ENFORCE_PII = prevEnforce;
    if (prevDisable === undefined) delete process.env.DISABLE_PII;
    else process.env.DISABLE_PII = prevDisable;
  });

  it('should reject assigning PII to plain variable', () => {
    const fn = makeFunc({
      name: 'assign_plain',
      params: [piiParam('email', 'L2')],
      ret: TEXT(),
      body: [
        CoreBuilder.Let('plain', CoreBuilder.String('safe')),
        CoreBuilder.Set('plain', CoreBuilder.Name('email')),
        CoreBuilder.Return(CoreBuilder.Name('plain')),
      ],
    });
    const diagnostics = runModule([fn]);
    assert.ok(diagnostics.some(diag => diag.code === ErrorCode.PII_ASSIGN_DOWNGRADE));
  });

  it('should warn on implicit upgrade from plain to L1', () => {
    const fn = makeFunc({
      name: 'implicit_upgrade',
      params: [piiParam('emailLow', 'L1')],
      ret: piiType('L1', 'email'),
      body: [
        CoreBuilder.Let('low', CoreBuilder.Name('emailLow')),
        CoreBuilder.Set('low', CoreBuilder.String('anon')),
        CoreBuilder.Return(CoreBuilder.Name('low')),
      ],
    });
    const diagnostics = runModule([fn]);
    assert.ok(diagnostics.some(diag => diag.code === ErrorCode.PII_IMPLICIT_UPLEVEL));
  });

  it('should forbid assigning L2 data into L1 slot', () => {
    const fn = makeFunc({
      name: 'downgrade_level',
      params: [piiParam('low', 'L1'), piiParam('high', 'L2')],
      ret: piiType('L1', 'email'),
      body: [
        CoreBuilder.Let('alias', CoreBuilder.Name('low')),
        CoreBuilder.Set('alias', CoreBuilder.Name('high')),
        CoreBuilder.Return(CoreBuilder.Name('alias')),
      ],
    });
    const diagnostics = runModule([fn]);
    assert.ok(diagnostics.some(diag => diag.code === ErrorCode.PII_ASSIGN_DOWNGRADE));
  });

  it('should treat merged branches with L3 as L3 when printing', () => {
    const fn = makeFunc({
      name: 'merge_highest',
      params: [piiParam('low', 'L1'), piiParam('top', 'L3')],
      ret: TEXT(),
      effects: IO_EFFECT,
      body: [
        CoreBuilder.Let('final', CoreBuilder.Name('low')),
        CoreBuilder.If(
          CoreBuilder.Bool(true),
          CoreBuilder.Block([CoreBuilder.Set('final', CoreBuilder.Name('top'))]),
          CoreBuilder.Block([CoreBuilder.Set('final', CoreBuilder.Name('low'))])
        ),
        CoreBuilder.Return(CoreBuilder.Call(CoreBuilder.Name('IO.print'), [CoreBuilder.Name('final')])),
      ],
    });
    const diagnostics = runModule([fn]);
    const sinkDiag = diagnostics.find(diag => diag.code === ErrorCode.PII_SINK_UNSANITIZED);
    assert.ok(sinkDiag, '应存在 sink 违规诊断');
    assert.equal((sinkDiag!.data as { level?: string }).level, 'L3');
  });

  it('should propagate L2 level across plain branch', () => {
    const fn = makeFunc({
      name: 'merge_mid',
      params: [piiParam('mid', 'L2')],
      ret: TEXT(),
      effects: IO_EFFECT,
      body: [
        CoreBuilder.Let('final', CoreBuilder.String('plain')),
        CoreBuilder.If(
          CoreBuilder.Bool(true),
          CoreBuilder.Block([CoreBuilder.Set('final', CoreBuilder.Name('mid'))]),
          CoreBuilder.Block([CoreBuilder.Set('final', CoreBuilder.String('plain'))])
        ),
        CoreBuilder.Return(CoreBuilder.Call(CoreBuilder.Name('IO.print'), [CoreBuilder.Name('final')])),
      ],
    });
    const diagnostics = runModule([fn]);
    const sinkDiag = diagnostics.find(diag => diag.code === ErrorCode.PII_SINK_UNSANITIZED);
    assert.ok(sinkDiag);
    assert.equal((sinkDiag!.data as { level?: string }).level, 'L2');
  });

  it('should block direct print of L2 data', () => {
    const fn = makeFunc({
      name: 'print_pii',
      params: [piiParam('email', 'L2')],
      ret: TEXT(),
      effects: IO_EFFECT,
      body: [CoreBuilder.Return(CoreBuilder.Call(CoreBuilder.Name('IO.print'), [CoreBuilder.Name('email')]))],
    });
    const diagnostics = runModule([fn]);
    assert.ok(diagnostics.some(diag => diag.code === ErrorCode.PII_SINK_UNSANITIZED));
  });

  it('should allow printing plain data without diagnostics', () => {
    const fn = makeFunc({
      name: 'print_plain',
      params: [plainParam('msg')],
      ret: TEXT(),
      effects: IO_EFFECT,
      body: [CoreBuilder.Return(CoreBuilder.Call(CoreBuilder.Name('IO.print'), [CoreBuilder.Name('msg')]))],
    });
    const diagnostics = runModule([fn]);
    assert.equal(diagnostics.length, 0);
  });

  it('should forbid logging L3 payloads', () => {
    const fn = makeFunc({
      name: 'log_secret',
      params: [piiParam('secret', 'L3')],
      ret: TEXT(),
      effects: IO_EFFECT,
      body: [CoreBuilder.Return(CoreBuilder.Call(CoreBuilder.Name('Log.info'), [CoreBuilder.Name('secret')]))],
    });
    const diagnostics = runModule([fn]);
    assert.ok(diagnostics.some(diag => diag.code === ErrorCode.PII_SINK_UNSANITIZED));
  });

  it('should accept matching PII argument in function call', () => {
    const callee = makeFunc({
      name: 'handle_email',
      params: [piiParam('email', 'L2')],
      ret: piiType('L2', 'email'),
      body: [CoreBuilder.Return(CoreBuilder.Name('email'))],
    });
    const caller = makeFunc({
      name: 'forward_email',
      params: [piiParam('email', 'L2')],
      ret: piiType('L2', 'email'),
      body: [
        CoreBuilder.Return(
          CoreBuilder.Call(CoreBuilder.Name('handle_email'), [CoreBuilder.Name('email')])
        ),
      ],
    });
    const diagnostics = runModule([callee, caller]);
    assert.equal(diagnostics.length, 0);
  });

  it('should report argument mismatch when passing plain data to PII param', () => {
    const callee = makeFunc({
      name: 'handle_email',
      params: [piiParam('email', 'L2')],
      ret: piiType('L2', 'email'),
      body: [CoreBuilder.Return(CoreBuilder.Name('email'))],
    });
    const caller = makeFunc({
      name: 'send_plain',
      params: [plainParam('email')],
      ret: piiType('L2', 'email'),
      body: [
        CoreBuilder.Return(
          CoreBuilder.Call(CoreBuilder.Name('handle_email'), [CoreBuilder.Name('email')])
        ),
      ],
    });
    const diagnostics = runModule([callee, caller]);
    assert.ok(diagnostics.some(diag => diag.code === ErrorCode.PII_ARG_VIOLATION));
  });

  // Workflow Start/Wait PII 传播测试
  it('should propagate PII across workflow steps via Start/Wait', () => {
    const fn = makeFunc({
      name: 'workflow_start_wait_pii',
      params: [piiParam('piiData', 'L2')],
      ret: TEXT(),
      effects: IO_EFFECT,
      body: [
        CoreBuilder.Let('result', CoreBuilder.String('plain')),
        CoreBuilder.Workflow(
          [
            CoreBuilder.Step(
              'load',
              CoreBuilder.Block([
                CoreBuilder.Set('result', CoreBuilder.Name('piiData')),
                CoreBuilder.Start('task', CoreBuilder.Name('result')),
              ]),
              []
            ),
            CoreBuilder.Step(
              'process',
              CoreBuilder.Block([
                CoreBuilder.Wait(['task']),
                // Wait 后，result 应该保持 L2 PII（从 Start 步骤传播）
                CoreBuilder.Return(
                  CoreBuilder.Call(CoreBuilder.Name('IO.print'), [CoreBuilder.Name('result')])
                ),
              ]),
              []
            ),
          ],
          []
        ),
        CoreBuilder.Return(CoreBuilder.String('done')),
      ],
    });
    const diagnostics = runModule([fn]);
    // 应该检测到 L2 PII sink 违规
    const sinkDiag = diagnostics.find(diag => diag.code === ErrorCode.PII_SINK_UNSANITIZED);
    assert.ok(sinkDiag, '应检测到 PII sink 违规');
    assert.equal((sinkDiag!.data as { level?: string }).level, 'L2');
  });

  it('should merge PII levels across parallel workflow branches', () => {
    const fn = makeFunc({
      name: 'workflow_parallel_merge',
      params: [piiParam('low', 'L1'), piiParam('high', 'L3')],
      ret: TEXT(),
      effects: IO_EFFECT,
      body: [
        CoreBuilder.Let('result', CoreBuilder.String('plain')),
        CoreBuilder.Workflow(
          [
            // 分支 1: 设置为 L1
            CoreBuilder.Step(
              'branch1',
              CoreBuilder.Block([CoreBuilder.Set('result', CoreBuilder.Name('low'))]),
              []
            ),
            // 分支 2: 设置为 L3
            CoreBuilder.Step(
              'branch2',
              CoreBuilder.Block([CoreBuilder.Set('result', CoreBuilder.Name('high'))]),
              []
            ),
          ],
          []
        ),
        // Workflow 结束后，result 应该是 L3（取最高等级）
        CoreBuilder.Return(
          CoreBuilder.Call(CoreBuilder.Name('IO.print'), [CoreBuilder.Name('result')])
        ),
      ],
    });
    const diagnostics = runModule([fn]);
    const sinkDiag = diagnostics.find(diag => diag.code === ErrorCode.PII_SINK_UNSANITIZED);
    assert.ok(sinkDiag, '应存在 sink 违规诊断');
    assert.equal((sinkDiag!.data as { level?: string }).level, 'L3');
  });

  it('should detect PII sink violation across workflow steps', () => {
    const fn = makeFunc({
      name: 'workflow_cross_step_sink',
      params: [piiParam('secret', 'L3')],
      ret: TEXT(),
      effects: IO_EFFECT,
      body: [
        CoreBuilder.Let('data', CoreBuilder.String('plain')),
        CoreBuilder.Workflow(
          [
            CoreBuilder.Step(
              'load',
              CoreBuilder.Block([
                CoreBuilder.Set('data', CoreBuilder.Name('secret')),
                CoreBuilder.Start('processing', CoreBuilder.Name('data')),
              ]),
              []
            ),
            CoreBuilder.Step(
              'output',
              CoreBuilder.Block([
                CoreBuilder.Wait(['processing']),
                // 打印 L3 数据应该被阻止
                CoreBuilder.Return(
                  CoreBuilder.Call(CoreBuilder.Name('IO.print'), [CoreBuilder.Name('data')])
                ),
              ]),
              []
            ),
          ],
          []
        ),
        CoreBuilder.Return(CoreBuilder.String('done')),
      ],
    });
    const diagnostics = runModule([fn]);
    const sinkDiag = diagnostics.find(diag => diag.code === ErrorCode.PII_SINK_UNSANITIZED);
    assert.ok(sinkDiag, '应检测到 L3 PII sink 违规');
    assert.equal((sinkDiag!.data as { level?: string }).level, 'L3');
  });

  it('should allow plain data operations in workflow without diagnostics', () => {
    const fn = makeFunc({
      name: 'workflow_plain_data',
      params: [plainParam('msg')],
      ret: TEXT(),
      effects: IO_EFFECT,
      body: [
        CoreBuilder.Let('output', CoreBuilder.String('init')),
        CoreBuilder.Workflow(
          [
            CoreBuilder.Step(
              'step1',
              CoreBuilder.Block([
                CoreBuilder.Set('output', CoreBuilder.Name('msg')),
                CoreBuilder.Start('task', CoreBuilder.Name('output')),
              ]),
              []
            ),
            CoreBuilder.Step(
              'step2',
              CoreBuilder.Block([
                CoreBuilder.Wait(['task']),
                CoreBuilder.Return(
                  CoreBuilder.Call(CoreBuilder.Name('IO.print'), [CoreBuilder.Name('output')])
                ),
              ]),
              []
            ),
          ],
          []
        ),
        CoreBuilder.Return(CoreBuilder.String('done')),
      ],
    });
    const diagnostics = runModule([fn]);
    // 不应有任何 PII 诊断（可能有 workflow 相关诊断，但不应有 PII 诊断）
    const piiDiags = diagnostics.filter(d =>
      d.code === ErrorCode.PII_SINK_UNSANITIZED ||
      d.code === ErrorCode.PII_ASSIGN_DOWNGRADE ||
      d.code === ErrorCode.PII_ARG_VIOLATION ||
      d.code === ErrorCode.PII_IMPLICIT_UPLEVEL
    );
    assert.equal(piiDiags.length, 0);
  });

  // 回归测试：Lambda 内 Start/Wait（修复 Issue 1）
  // 简化版本：直接在 workflow 内测试 PII 传播，确保 stepEnvs 正确工作
  it('should allow Start/Wait in workflow without stepEnvs crash', () => {
    const fn = makeFunc({
      name: 'workflow_with_start_wait',
      params: [piiParam('data', 'L2')],
      ret: TEXT(),
      effects: IO_EFFECT,
      body: [
        CoreBuilder.Let('result', CoreBuilder.String('plain')),
        CoreBuilder.Workflow(
          [
            CoreBuilder.Step(
              'task1',
              CoreBuilder.Block([
                CoreBuilder.Set('result', CoreBuilder.Name('data')),
                CoreBuilder.Start('process', CoreBuilder.Name('result')),
              ]),
              []
            ),
            CoreBuilder.Step(
              'task2',
              CoreBuilder.Block([
                CoreBuilder.Wait(['process']),
                CoreBuilder.Return(CoreBuilder.Name('result')),
              ]),
              []
            ),
          ],
          []
        ),
        CoreBuilder.Return(CoreBuilder.Name('result')),
      ],
    });
    const diagnostics = runModule([fn]);
    // 应该不会崩溃，可能会有 PII 诊断但不应有致命错误
    assert.ok(Array.isArray(diagnostics), '应返回诊断数组而非崩溃');
    // 验证没有崩溃相关的错误
    const crashErrors = diagnostics.filter(d => d.message && d.message.includes('undefined'));
    assert.equal(crashErrors.length, 0, '不应有 undefined 相关错误');
  });

  // 回归测试：多个 workflow 不应相互污染（修复 Issue 2）
  it('should not pollute stepEnvs across multiple workflows', () => {
    const fn = makeFunc({
      name: 'multiple_workflows',
      params: [piiParam('pii1', 'L1'), piiParam('pii2', 'L2')],
      ret: TEXT(),
      effects: IO_EFFECT,
      body: [
        CoreBuilder.Let('data', CoreBuilder.String('plain')),
        // 第一个 workflow: 使用 pii1 (L1)
        CoreBuilder.Workflow(
          [
            CoreBuilder.Step(
              'step1',
              CoreBuilder.Block([
                CoreBuilder.Set('data', CoreBuilder.Name('pii1')),
                CoreBuilder.Start('task1', CoreBuilder.Name('data')),
              ]),
              []
            ),
            CoreBuilder.Step(
              'step2',
              CoreBuilder.Block([CoreBuilder.Wait(['task1'])]),
              []
            ),
          ],
          []
        ),
        CoreBuilder.Set('data', CoreBuilder.String('plain')), // 重置
        // 第二个 workflow: 使用 pii2 (L2)，不应看到 task1
        CoreBuilder.Workflow(
          [
            CoreBuilder.Step(
              'step3',
              CoreBuilder.Block([
                CoreBuilder.Set('data', CoreBuilder.Name('pii2')),
                CoreBuilder.Start('task2', CoreBuilder.Name('data')),
              ]),
              []
            ),
            CoreBuilder.Step(
              'step4',
              CoreBuilder.Block([
                CoreBuilder.Wait(['task2']),
                // 打印 data，应该是 L2，不应受第一个 workflow 影响
                CoreBuilder.Return(
                  CoreBuilder.Call(CoreBuilder.Name('IO.print'), [CoreBuilder.Name('data')])
                ),
              ]),
              []
            ),
          ],
          []
        ),
        CoreBuilder.Return(CoreBuilder.String('done')),
      ],
    });
    const diagnostics = runModule([fn]);
    const sinkDiag = diagnostics.find(diag => diag.code === ErrorCode.PII_SINK_UNSANITIZED);
    assert.ok(sinkDiag, '应检测到 PII sink 违规');
    // 应该是 L2，不应因第一个 workflow 的 L1 而混淆
    assert.equal((sinkDiag!.data as { level?: string }).level, 'L2');
  });

  // 回归测试：HTTP 别名调用检测（修复 Issue 3）
  it('should detect PII sink via HTTP alias', () => {
    // 创建带 import 别名的模块，测试 HTTP 别名解析
    const httpImport = CoreBuilder.Import('Http', 'H');
    const fn = makeFunc({
      name: 'http_alias_test',
      params: [piiParam('secret', 'L3')],
      ret: TEXT(),
      effects: IO_EFFECT,
      body: [
        // 使用别名 H.post 而非 Http.post
        CoreBuilder.Return(
          CoreBuilder.Call(CoreBuilder.Name('H.post'), [
            CoreBuilder.String('url'),
            CoreBuilder.Name('secret'),
          ])
        ),
      ],
    });
    // 创建包含 import 声明的模块
    const module: Core.Module = CoreBuilder.Module('tests.pii', [httpImport, fn]);
    const diagnostics = typecheckModule(module);
    // 应检测到 HTTP sink 违规（通过别名 H.post）
    const sinkDiag = diagnostics.find(diag => diag.code === ErrorCode.PII_SINK_UNSANITIZED);
    assert.ok(sinkDiag, '应检测到通过别名的 HTTP sink 违规');
    assert.equal((sinkDiag!.data as { level?: string }).level, 'L3');
  });
});
