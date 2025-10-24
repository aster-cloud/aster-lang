import { before, after, describe, it } from 'node:test';
import assert from 'node:assert/strict';
import type { Core, Origin } from '../../../src/types.js';
import { checkPiiFlow } from '../../../src/lsp/pii_diagnostics.js';

let piiDiagnosticsModule: typeof import('../../../src/lsp/pii_diagnostics.js') | null = null;
let originalActiveContext: unknown = null;

before(async () => {
  piiDiagnosticsModule = await import('../../../src/lsp/pii_diagnostics.js');
  if (!piiDiagnosticsModule) return;
  const exportedActive = (piiDiagnosticsModule as Record<string, unknown>).activeContext;
  if (exportedActive !== undefined) {
    originalActiveContext = exportedActive;
    return;
  }
  const resetForTesting = (piiDiagnosticsModule as Record<string, unknown>).resetContextForTesting;
  if (typeof resetForTesting === 'function') {
    originalActiveContext = (resetForTesting as () => unknown)();
  }
});

after(() => {
  if (!piiDiagnosticsModule) return;
  const exportedActive = (piiDiagnosticsModule as Record<string, unknown>).activeContext;
  if (exportedActive !== undefined) {
    (piiDiagnosticsModule as Record<string, unknown>).activeContext = originalActiveContext;
    return;
  }
  const setForTesting = (piiDiagnosticsModule as Record<string, unknown>).setContextForTesting;
  if (typeof setForTesting === 'function') {
    (setForTesting as (value: unknown) => void)(originalActiveContext);
  }
});

function makeOrigin(label: string): Origin {
  return {
    file: `${label}.aster`,
    start: { line: 1, col: 1 },
    end: { line: 1, col: 10 },
  };
}

function makeName(name: string): Core.Name {
  return { kind: 'Name', name, origin: makeOrigin(name) };
}

function makeCall(name: string, args: Core.Expression[] = []): Core.Call {
  return {
    kind: 'Call',
    target: makeName(name),
    args,
    origin: makeOrigin(`call:${name}`),
  };
}

function makeBlock(statements: Core.Statement[]): Core.Block {
  return { kind: 'Block', statements };
}

interface FuncOptions {
  name: string;
  params?: Core.Parameter[];
  effects?: ReadonlyArray<Core.Func['effects'][number]>;
  body?: Core.Block;
  ret?: Core.Type;
}

const UNIT_TYPE: Core.TypeName = { kind: 'TypeName', name: 'Unit' };

function makeFunc({
  name,
  params = [],
  effects = [],
  body = makeBlock([makeReturn(makeName('unit'))]),
  ret = UNIT_TYPE,
}: FuncOptions): Core.Func {
  return {
    kind: 'Func',
    name,
    typeParams: [],
    params: params.map(param => ({ ...param, annotations: param.annotations ?? [] })),
    ret,
    effects,
    effectCaps: [],
    effectCapsExplicit: false,
    body,
  };
}

function makeReturn(expr: Core.Expression): Core.Return {
  return { kind: 'Return', expr };
}

function makeLet(name: string, expr: Core.Expression): Core.Let {
  return { kind: 'Let', name, expr };
}

describe('PII Diagnostics', () => {
  describe('checkPiiFlow', () => {
    it('应该对空模块返回空诊断列表', () => {
      const emptyModule: Core.Module = {
        kind: 'Module',
        name: 'TestModule',
        decls: [],
      };
      const diagnostics = checkPiiFlow(emptyModule);
      assert.strictEqual(diagnostics.length, 0);
    });
  });
});
