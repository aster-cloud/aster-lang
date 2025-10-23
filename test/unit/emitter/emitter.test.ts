import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { emitJava } from '../../../src/jvm/emitter.js';
import { Core } from '../../../src/core_ir.js';
import type { Core as CoreTypes } from '../../../src/types.js';

function createTempDir(): string {
  const base = fs.mkdtempSync(path.join(os.tmpdir(), 'aster-emitter-'));
  return base;
}

function readFile(filePath: string): string {
  return fs.readFileSync(filePath, 'utf8');
}

async function emitToTemp(module: CoreTypes.Module): Promise<{ outDir: string; cleanup: () => void }> {
  const outDir = createTempDir();
  let cleaned = false;
  return {
    outDir,
    cleanup: () => {
      if (!cleaned) {
        fs.rmSync(outDir, { recursive: true, force: true });
        cleaned = true;
      }
    },
  };
}

describe('JVM 代码生成器', () => {
  it('应生成数据类型对应的 Java 类', async () => {
    const module = Core.Module('test.emitter.data', [
      Core.Data('User', [
        { name: 'id', type: Core.TypeName('Text'), annotations: [] },
        { name: 'age', type: Core.TypeName('Int'), annotations: [] },
      ]),
    ]);
    const { outDir, cleanup } = await emitToTemp(module);
    try {
      await emitJava(module, outDir);
      const filePath = path.join(outDir, 'test', 'emitter', 'data', 'User.java');
      assert.equal(fs.existsSync(filePath), true);
      const content = readFile(filePath);
      assert.equal(content.includes('public final class User'), true);
      assert.equal(content.includes('public final String id;'), true);
    } finally {
      cleanup();
    }
  });

  it('应生成枚举声明', async () => {
    const module = Core.Module('test.emitter.enum', [Core.Enum('Status', ['Pending', 'Success'])]);
    const { outDir, cleanup } = await emitToTemp(module);
    try {
      await emitJava(module, outDir);
      const filePath = path.join(outDir, 'test', 'emitter', 'enum', 'Status.java');
      const content = readFile(filePath);
      assert.equal(content.includes('public enum Status'), true);
      assert.equal(content.includes('Pending'), true);
    } finally {
      cleanup();
    }
  });

  it('应生成函数包装类并渲染主体', async () => {
    const funcBody = Core.Block([Core.Return(Core.String('pong'))]);
    const funcDecl: CoreTypes.Func = Core.Func(
      'ping',
      [],
      [{ name: 'input', type: Core.TypeName('Text'), annotations: [] }],
      Core.TypeName('Text'),
      [],
      funcBody
    );
    const module = Core.Module('test.emitter.func', [funcDecl]);
    const { outDir, cleanup } = await emitToTemp(module);
    try {
      await emitJava(module, outDir);
      const filePath = path.join(outDir, 'test', 'emitter', 'func', 'ping_fn.java');
      const content = readFile(filePath);
      assert.equal(content.includes('public static String ping'), true);
      assert.equal(content.includes('return "pong";'), true);
    } finally {
      cleanup();
    }
  });

  it('应为枚举匹配生成 switch 结构', async () => {
    const enumDecl = Core.Enum('Status', ['Ok', 'Err']);
    const matcher = Core.Func(
      'classify',
      [],
      [{ name: 'status', type: Core.TypeName('Status'), annotations: [] }],
      Core.TypeName('int'),
      [],
      Core.Block([
        Core.Match(Core.Name('status'), [
          Core.Case(Core.PatName('Ok'), Core.Return(Core.Int(1))),
          Core.Case(Core.PatName('Err'), Core.Return(Core.Int(0))),
        ]),
      ])
    );
    const module = Core.Module('test.emitter.match', [enumDecl, matcher]);
    const { outDir, cleanup } = await emitToTemp(module);
    try {
      await emitJava(module, outDir);
      const filePath = path.join(outDir, 'test', 'emitter', 'match', 'classify_fn.java');
      const content = readFile(filePath);
      assert.equal(content.includes('switch((Status)__scrut)'), true);
      assert.equal(content.includes('case Status.Ok'), true);
      assert.equal(content.includes('return 1;'), true);
    } finally {
      cleanup();
    }
  });

  it('嵌套数据匹配应生成 instanceof 守卫与解构', async () => {
    const pairData = Core.Data('Pair', [
      { name: 'left', type: Core.TypeName('Object'), annotations: [] },
      { name: 'right', type: Core.TypeName('Object'), annotations: [] },
    ]);
    const matcher = Core.Func(
      'unwrap',
      [],
      [{ name: 'value', type: Core.TypeName('Pair'), annotations: [] }],
      Core.TypeName('Object'),
      [],
      Core.Block([
        Core.Match(Core.Name('value'), [
          Core.Case(
            Core.PatCtor('Pair', [], [
              Core.PatName('a'),
              Core.PatCtor('Pair', ['innerLeft']),
            ]),
            Core.Return(Core.Name('innerLeft'))
          ),
        ]),
      ])
    );
    const module = Core.Module('test.emitter.nested_match', [pairData, matcher]);
    const { outDir, cleanup } = await emitToTemp(module);
    try {
      await emitJava(module, outDir);
      const filePath = path.join(outDir, 'test', 'emitter', 'nested_match', 'unwrap_fn.java');
      const content = readFile(filePath);
      assert.equal(content.includes('if (__scrut instanceof Pair)'), true);
      assert.equal(content.includes('var __tmp = (Pair)__scrut;'), true);
      assert.equal(content.includes('__tmp_1'), true);
    } finally {
      cleanup();
    }
  });

  it('数据类应对 List 与 Map 字段生成泛型类型', async () => {
    const module = Core.Module('test.emitter.collections', [
      Core.Data('Catalog', [
        { name: 'items', type: Core.List(Core.TypeName('Int')), annotations: [] },
        {
          name: 'metadata',
          type: Core.Map(Core.TypeName('Text'), Core.TypeName('Text')),
          annotations: [],
        },
      ]),
    ]);
    const { outDir, cleanup } = await emitToTemp(module);
    try {
      await emitJava(module, outDir);
      const filePath = path.join(outDir, 'test', 'emitter', 'collections', 'Catalog.java');
      const content = readFile(filePath);
      assert.equal(content.includes('java.util.List<int> items;'), true);
      assert.equal(content.includes('java.util.Map<String, String> metadata;'), true);
    } finally {
      cleanup();
    }
  });

  it('Scope 语句应串联内部语句输出', async () => {
    const scopedBody = Core.Block([
      Core.Scope([
        Core.Let('temp', Core.Int(1)),
        Core.Set('result', Core.Int(2)),
      ]),
      Core.Return(Core.Name('result')),
    ]);
    const funcDecl: CoreTypes.Func = Core.Func(
      'withScope',
      [],
      [{ name: 'result', type: Core.TypeName('int'), annotations: [] }],
      Core.TypeName('int'),
      [],
      scopedBody
    );
    const module = Core.Module('test.emitter.scope', [funcDecl]);
    const { outDir, cleanup } = await emitToTemp(module);
    try {
      await emitJava(module, outDir);
      const filePath = path.join(outDir, 'test', 'emitter', 'scope', 'withScope_fn.java');
      const content = readFile(filePath);
      assert.equal(content.includes('var temp = 1;'), true);
      assert.equal(content.includes('result = 2;'), true);
    } finally {
      cleanup();
    }
  });

  it('Start 与 Wait 语句暂未实现时应输出占位注释', async () => {
    const funcDecl: CoreTypes.Func = Core.Func(
      'asyncStub',
      [],
      [],
      Core.TypeName('void'),
      [],
      Core.Block([
        Core.Start('task', Core.Name('producer')),
        Core.Wait(['task']),
      ])
    );
    const module = Core.Module('test.emitter.async', [funcDecl]);
    const { outDir, cleanup } = await emitToTemp(module);
    try {
      await emitJava(module, outDir);
      const filePath = path.join(outDir, 'test', 'emitter', 'async', 'asyncStub_fn.java');
      const content = readFile(filePath);
      assert.equal(content.includes('// async not implemented in MVP'), true);
    } finally {
      cleanup();
    }
  });

  it('Await 表达式当前回退为 null 占位', async () => {
    const funcDecl: CoreTypes.Func = Core.Func(
      'awaitFallback',
      [],
      [],
      Core.TypeName('Object'),
      [],
      Core.Block([Core.Return(Core.Await(Core.Name('future')))])
    );
    const module = Core.Module('test.emitter.await', [funcDecl]);
    const { outDir, cleanup } = await emitToTemp(module);
    try {
      await emitJava(module, outDir);
      const filePath = path.join(outDir, 'test', 'emitter', 'await', 'awaitFallback_fn.java');
      const content = readFile(filePath);
      assert.equal(content.includes('return null;'), true);
    } finally {
      cleanup();
    }
  });
});
