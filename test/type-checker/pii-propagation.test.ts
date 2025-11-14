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

  before(() => {
    process.env.ASTER_ENFORCE_PII = '1';
    process.env.ENFORCE_PII = 'false';
  });

  after(() => {
    if (prevAster === undefined) delete process.env.ASTER_ENFORCE_PII;
    else process.env.ASTER_ENFORCE_PII = prevAster;
    if (prevEnforce === undefined) delete process.env.ENFORCE_PII;
    else process.env.ENFORCE_PII = prevEnforce;
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
});
