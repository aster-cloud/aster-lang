/**
 * 异步纪律检查测试
 *
 * 测试 collectAsync 的错误检查逻辑：
 * 1. Start 未 Wait - 应该产生 error
 * 2. Wait 未 Start - 应该产生 error
 * 3. 重复 Start - 应该产生 error
 * 4. 重复 Wait - 应该产生 warning
 * 5. 正常场景 - 不应该有错误
 */

import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { canonicalize } from '../src/canonicalizer.js';
import { lex } from '../src/lexer.js';
import { parse } from '../src/parser.js';
import { lowerModule } from '../src/lower_to_core.js';
import { typecheckModule } from '../src/typecheck.js';
import type { Module as AstModule } from '../src/types.js';

function compileAndGetDiagnostics(source: string): Array<{ severity: string; message: string; code?: string }> {
  try {
    const canonical = canonicalize(source);
    const tokens = lex(canonical);
    const ast = parse(tokens) as AstModule;
    const core = lowerModule(ast);
    return typecheckModule(core);
  } catch (error) {
    return [{ severity: 'error', message: (error as Error).message }];
  }
}

describe('异步纪律检查', () => {
  describe('Start 未 Wait 场景', () => {
    it('应该检测单个 Start 未 Wait', () => {
      const source = `
This module is test.async.start_not_waited.

Define User with id: Text.

To fetchData with u: User, produce Text. It performs io:
  Start profile as async fetchProfile(u.id).
  Return "Done".

To fetchProfile with id: Text, produce Text. It performs io:
  Return "Profile".
`;

      const diagnostics = compileAndGetDiagnostics(source);
      const errors = diagnostics.filter(d => d.severity === 'error' && d.code === 'ASYNC_START_NOT_WAITED');

      assert.equal(errors.length, 1, '应该有1个 Start 未 Wait 错误');
      assert.equal(errors[0]!.message.includes('profile'), true, '错误消息应该包含任务名 profile');
      assert.equal(errors[0]!.message.includes('not waited'), true, '错误消息应该说明未等待');
    });

    it('应该检测多个 Start 未 Wait', () => {
      const source = `
This module is test.async.multiple_not_waited.

Define User with id: Text.

To fetchData with u: User, produce Text. It performs io:
  Start profile as async fetchProfile(u.id).
  Start timeline as async fetchTimeline(u.id).
  Start settings as async fetchSettings(u.id).
  Return "Done".

To fetchProfile with id: Text, produce Text. It performs io:
  Return "Profile".

To fetchTimeline with id: Text, produce Text. It performs io:
  Return "Timeline".

To fetchSettings with id: Text, produce Text. It performs io:
  Return "Settings".
`;

      const diagnostics = compileAndGetDiagnostics(source);
      const errors = diagnostics.filter(d => d.severity === 'error' && d.code === 'ASYNC_START_NOT_WAITED');

      assert.equal(errors.length, 3, '应该有3个 Start 未 Wait 错误');
      const errorNames = errors.map(e => e.message).join(' ');
      assert.equal(errorNames.includes('profile'), true, '应该包含 profile');
      assert.equal(errorNames.includes('timeline'), true, '应该包含 timeline');
      assert.equal(errorNames.includes('settings'), true, '应该包含 settings');
    });
  });

  describe('Wait 未 Start 场景', () => {
    it('应该检测单个 Wait 未 Start', () => {
      const source = `
This module is test.async.wait_not_started.

Define User with id: Text.

To fetchData with u: User, produce Text. It performs io:
  Wait for profile.
  Return "Done".
`;

      const diagnostics = compileAndGetDiagnostics(source);
      const errors = diagnostics.filter(d => d.severity === 'error' && d.code === 'ASYNC_WAIT_NOT_STARTED');

      assert.equal(errors.length, 1, '应该有1个 Wait 未 Start 错误');
      assert.equal(errors[0]!.message.includes('profile'), true, '错误消息应该包含任务名 profile');
      assert.equal(errors[0]!.message.includes('never started'), true, '错误消息应该说明从未启动');
    });

    it('应该检测多个 Wait 未 Start', () => {
      const source = `
This module is test.async.multiple_wait_not_started.

Define User with id: Text.

To fetchData with u: User, produce Text. It performs io:
  Wait for profile and timeline and settings.
  Return "Done".
`;

      const diagnostics = compileAndGetDiagnostics(source);
      const errors = diagnostics.filter(d => d.severity === 'error' && d.code === 'ASYNC_WAIT_NOT_STARTED');

      assert.equal(errors.length, 3, '应该有3个 Wait 未 Start 错误');
      const errorNames = errors.map(e => e.message).join(' ');
      assert.equal(errorNames.includes('profile'), true, '应该包含 profile');
      assert.equal(errorNames.includes('timeline'), true, '应该包含 timeline');
      assert.equal(errorNames.includes('settings'), true, '应该包含 settings');
    });
  });

  describe('重复 Start 场景', () => {
    it('应该检测重复 Start 同一任务', () => {
      const source = `
This module is test.async.duplicate_start.

Define User with id: Text.

To fetchData with u: User, produce Text. It performs io:
  Start profile as async fetchProfile(u.id).
  Start profile as async fetchProfile(u.id).
  Wait for profile.
  Return "Done".

To fetchProfile with id: Text, produce Text. It performs io:
  Return "Profile".
`;

      const diagnostics = compileAndGetDiagnostics(source);
      const errors = diagnostics.filter(d => d.severity === 'error' && d.code === 'ASYNC_DUPLICATE_START');

      assert.equal(errors.length, 1, '应该有1个重复 Start 错误');
      assert.equal(errors[0]!.message.includes('profile'), true, '错误消息应该包含任务名 profile');
      assert.equal(errors[0]!.message.includes('multiple times'), true, '错误消息应该说明多次启动');
      assert.equal(errors[0]!.message.includes('2'), true, '错误消息应该包含出现次数 2');
    });

    it('应该检测三次 Start 同一任务', () => {
      const source = `
This module is test.async.triple_start.

Define User with id: Text.

To fetchData with u: User, produce Text. It performs io:
  Start profile as async fetchProfile(u.id).
  Start profile as async fetchProfile(u.id).
  Start profile as async fetchProfile(u.id).
  Wait for profile.
  Return "Done".

To fetchProfile with id: Text, produce Text. It performs io:
  Return "Profile".
`;

      const diagnostics = compileAndGetDiagnostics(source);
      const errors = diagnostics.filter(d => d.severity === 'error' && d.code === 'ASYNC_DUPLICATE_START');

      // 应该有2个错误（第2次和第3次启动）
      assert.equal(errors.length, 2, '应该有2个重复 Start 错误（第2次和第3次启动）');
      assert.equal(errors[0]!.message.includes('3'), true, '错误消息应该包含总出现次数 3');
    });
  });

  describe('重复 Wait 场景', () => {
    it('应该检测重复 Wait 同一任务（warning）', () => {
      const source = `
This module is test.async.duplicate_wait.

Define User with id: Text.

To fetchData with u: User, produce Text. It performs io:
  Start profile as async fetchProfile(u.id).
  Wait for profile.
  Wait for profile.
  Return "Done".

To fetchProfile with id: Text, produce Text. It performs io:
  Return "Profile".
`;

      const diagnostics = compileAndGetDiagnostics(source);
      const warnings = diagnostics.filter(d => d.severity === 'warning' && d.code === 'ASYNC_DUPLICATE_WAIT');

      assert.equal(warnings.length, 1, '应该有1个重复 Wait 警告');
      assert.equal(warnings[0]!.message.includes('profile'), true, '警告消息应该包含任务名 profile');
      assert.equal(warnings[0]!.message.includes('multiple times'), true, '警告消息应该说明多次等待');
      assert.equal(warnings[0]!.message.includes('2'), true, '警告消息应该包含出现次数 2');
    });

    it('应该检测三次 Wait 同一任务', () => {
      const source = `
This module is test.async.triple_wait.

Define User with id: Text.

To fetchData with u: User, produce Text. It performs io:
  Start profile as async fetchProfile(u.id).
  Wait for profile.
  Wait for profile.
  Wait for profile.
  Return "Done".

To fetchProfile with id: Text, produce Text. It performs io:
  Return "Profile".
`;

      const diagnostics = compileAndGetDiagnostics(source);
      const warnings = diagnostics.filter(d => d.severity === 'warning' && d.code === 'ASYNC_DUPLICATE_WAIT');

      // 应该有2个警告（第2次和第3次等待）
      assert.equal(warnings.length, 2, '应该有2个重复 Wait 警告（第2次和第3次等待）');
      assert.equal(warnings[0]!.message.includes('3'), true, '警告消息应该包含总出现次数 3');
    });
  });

  describe('正常场景', () => {
    it('单个 Start-Wait 对应该无错误', () => {
      const source = `
This module is test.async.normal_single.

Define User with id: Text.

To fetchData with u: User, produce Text. It performs io:
  Start profile as async fetchProfile(u.id).
  Wait for profile.
  Return "Done".

To fetchProfile with id: Text, produce Text. It performs io:
  Return "Profile".
`;

      const diagnostics = compileAndGetDiagnostics(source);
      const asyncErrors = diagnostics.filter(d =>
        d.code?.startsWith('ASYNC_')
      );

      assert.equal(asyncErrors.length, 0, '正常场景不应该有异步纪律错误');
    });

    it('多个 Start-Wait 对应该无错误', () => {
      const source = `
This module is test.async.normal_multiple.

Define User with id: Text.

To fetchData with u: User, produce Text. It performs io:
  Start profile as async fetchProfile(u.id).
  Start timeline as async fetchTimeline(u.id).
  Start settings as async fetchSettings(u.id).
  Wait for profile and timeline and settings.
  Return "Done".

To fetchProfile with id: Text, produce Text. It performs io:
  Return "Profile".

To fetchTimeline with id: Text, produce Text. It performs io:
  Return "Timeline".

To fetchSettings with id: Text, produce Text. It performs io:
  Return "Settings".
`;

      const diagnostics = compileAndGetDiagnostics(source);
      const asyncErrors = diagnostics.filter(d =>
        d.code?.startsWith('ASYNC_')
      );

      assert.equal(asyncErrors.length, 0, '正常场景不应该有异步纪律错误');
    });

    it('分批 Wait 应该无错误', () => {
      const source = `
This module is test.async.normal_batched.

Define User with id: Text.

To fetchData with u: User, produce Text. It performs io:
  Start profile as async fetchProfile(u.id).
  Start timeline as async fetchTimeline(u.id).
  Wait for profile.
  Start settings as async fetchSettings(u.id).
  Wait for timeline and settings.
  Return "Done".

To fetchProfile with id: Text, produce Text. It performs io:
  Return "Profile".

To fetchTimeline with id: Text, produce Text. It performs io:
  Return "Timeline".

To fetchSettings with id: Text, produce Text. It performs io:
  Return "Settings".
`;

      const diagnostics = compileAndGetDiagnostics(source);
      const asyncErrors = diagnostics.filter(d =>
        d.code?.startsWith('ASYNC_')
      );

      assert.equal(asyncErrors.length, 0, '分批等待场景不应该有异步纪律错误');
    });
  });

  describe('混合场景', () => {
    it('应该同时检测多种错误', () => {
      const source = `
This module is test.async.mixed_errors.

Define User with id: Text.

To fetchData with u: User, produce Text. It performs io:
  Start profile as async fetchProfile(u.id).
  Start profile as async fetchProfile(u.id).
  Start timeline as async fetchTimeline(u.id).
  Wait for profile.
  Wait for profile.
  Wait for settings.
  Return "Done".

To fetchProfile with id: Text, produce Text. It performs io:
  Return "Profile".

To fetchTimeline with id: Text, produce Text. It performs io:
  Return "Timeline".
`;

      const diagnostics = compileAndGetDiagnostics(source);

      // 应该有1个 Start 未 Wait 错误（timeline）
      const startNotWaited = diagnostics.filter(d => d.code === 'ASYNC_START_NOT_WAITED');
      assert.equal(startNotWaited.length, 1, '应该有1个 Start 未 Wait 错误');
      assert.equal(startNotWaited[0]!.message.includes('timeline'), true, '应该是 timeline');

      // 应该有1个 Wait 未 Start 错误（settings）
      const waitNotStarted = diagnostics.filter(d => d.code === 'ASYNC_WAIT_NOT_STARTED');
      assert.equal(waitNotStarted.length, 1, '应该有1个 Wait 未 Start 错误');
      assert.equal(waitNotStarted[0]!.message.includes('settings'), true, '应该是 settings');

      // 应该有1个重复 Start 错误（profile）
      const duplicateStart = diagnostics.filter(d => d.code === 'ASYNC_DUPLICATE_START');
      assert.equal(duplicateStart.length, 1, '应该有1个重复 Start 错误');
      assert.equal(duplicateStart[0]!.message.includes('profile'), true, '应该是 profile');

      // 应该有1个重复 Wait 警告（profile）
      const duplicateWait = diagnostics.filter(d => d.code === 'ASYNC_DUPLICATE_WAIT');
      assert.equal(duplicateWait.length, 1, '应该有1个重复 Wait 警告');
      assert.equal(duplicateWait[0]!.message.includes('profile'), true, '应该是 profile');
    });
  });
});
