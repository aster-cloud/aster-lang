import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { canonicalize } from '../../../src/canonicalizer.js';

describe('canonicalizer', () => {
  describe('注释处理', () => {
    it('应该删除行注释并保留空行占位', () => {
      const input = ['first line', '# comment', '  // inline comment', 'second line'].join('\n');
      const result = canonicalize(input);

      assert.strictEqual(result, 'first line\n\nsecond line');
      assert.strictEqual(result.includes('comment'), false);
    });
  });

  describe('冠词移除', () => {
    it('应该在字符串外移除冠词', () => {
      const result = canonicalize('Return the answer.');

      assert.strictEqual(result, 'Return answer.');
    });

    it('应该在字符串内保留冠词', () => {
      const result = canonicalize('Return "the answer".');

      assert.strictEqual(result, 'Return "the answer".');
    });
  });

  describe('多词关键字替换', () => {
    it('应该将多词关键字统一为小写', () => {
      const input = 'THIS MODULE IS Example.\nWAIT FOR OPTION OF value.';
      const result = canonicalize(input);

      assert.strictEqual(result, 'this module is Example.\nwait for option of value.');
    });

    it('应该避免误匹配紧凑单词', () => {
      const input = 'Return WaitFor result and Module island is scenic.';
      const result = canonicalize(input);

      assert.strictEqual(result, 'Return WaitFor result and Module island is scenic.');
    });
  });

  describe('缩进与空白规范', () => {
    it('应该将制表符统一为两个空格缩进', () => {
      const input = ['Line1', '\tIndented line', '\t  Mixed tab spaces', '  Already spaced'].join('\n');
      const result = canonicalize(input);

      assert.strictEqual(
        result,
        ['Line1', '  Indented line', '    Mixed tab spaces', '  Already spaced'].join('\n')
      );
      assert.strictEqual(result.includes('\t'), false);
    });

    it('应该移除行尾多余空格同时保留缩进', () => {
      const input = ['Return value   ', '  Next line   '].join('\n');
      const result = canonicalize(input);

      assert.strictEqual(result, ['Return value', '  Next line'].join('\n'));
    });
  });

  describe('标点与空格规范', () => {
    it('应该移除标点前多余空格', () => {
      const input = 'Return  value ,  next : item !  Should we ?';
      const result = canonicalize(input);

      assert.strictEqual(result, 'Return value, next: item! Should we?');
    });
  });

  describe('引号处理', () => {
    it('应该将智能引号转换为直引号并保留转义', () => {
      const input = 'Return “smart” and ‘single’ plus "escaped \\"quote\\"".';
      const result = canonicalize(input);

      assert.strictEqual(result, 'Return "smart" and \'single\' plus "escaped \\"quote\\"".');
    });
  });

  describe('字符串分段保护', () => {
    it('应该避免字符串内部空白被规范化', () => {
      const input = 'Return " spaced , punctuation " and the value , please.';
      const result = canonicalize(input);

      assert.strictEqual(result, 'Return " spaced , punctuation " and value, please.');
      assert.strictEqual(result.includes('" spaced , punctuation "'), true);
    });
  });

  describe('幂等性', () => {
    it('应该在重复规范化后保持不变', () => {
      const input = ['THIS MODULE IS Example.', 'Return  value ,  next.', '  Next line   '].join('\n');
      const once = canonicalize(input);
      const twice = canonicalize(once);

      assert.strictEqual(twice, once);
    });
  });
});
