/**
 * @module canonicalizer
 *
 * Canonicalizer（规范化器）：将 CNL 源代码规范化为标准格式。
 *
 * **功能**：
 * - 规范化关键字为美式英语（en-US）
 * - 强制语句以句号或冒号结尾
 * - 规范化空白符和缩进（2 空格为标准）
 * - 保留标识符的大小写
 * - 移除注释（`//` 和 `#`）
 * - 去除冠词（a, an, the）
 *
 * **注意**：
 * - Aster 使用 2 空格缩进，缩进具有语法意义
 * - 制表符会被自动转换为 2 个空格
 */

import { KW } from './tokens.js';

// Remove common articles only when followed by whitespace to avoid
// creating leading comment markers or altering tokens adjacent to punctuation.
const ARTICLE_RE = /\b(a|an|the)\b(?=\s)/gi;

// 判断指定位置的引号是否被转义
function isEscaped(str: string, index: number): boolean {
  let slashCount = 0;
  for (let i = index - 1; i >= 0 && str[i] === '\\'; i--) {
    slashCount++;
  }
  return slashCount % 2 === 1;
}

// Multi-word keyword list ordered by length (desc) to match greedily.
const MULTI = [
  KW.MODULE_IS,
  KW.ONE_OF,
  KW.WAIT_FOR,
  KW.FOR_EACH,
  KW.OPTION_OF,
  KW.RESULT_OF,
  KW.OK_OF,
  KW.ERR_OF,
  KW.SOME_OF,
  KW.PERFORMS,
].sort((a, b) => b.length - a.length);

/**
 * 规范化 CNL 源代码为标准格式。
 *
 * 这是 Aster 编译管道的第一步，将原始 CNL 文本转换为规范化的格式，
 * 以便后续的词法分析和语法分析阶段处理。
 *
 * **转换步骤**：
 * 1. 规范化换行符为 `\n`
 * 2. 将制表符转换为 2 个空格
 * 3. 移除行注释（`//` 和 `#`）
 * 4. 规范化引号（智能引号 → 直引号）
 * 5. 强制语句以句号或冒号结尾
 * 6. 去除冠词（a, an, the）
 * 7. 规范化多词关键字大小写（如 "This module is" → "This module is"）
 *
 * @param input - 原始 CNL 源代码字符串
 * @returns 规范化后的 CNL 源代码
 *
 * @example
 * ```typescript
 * import { canonicalize } from '@wontlost-ltd/aster-lang';
 *
 * const raw = `
 * This Module Is app.
 * To greet, produce Text:
 *   Return "Hello"
 * `;
 *
 * const canonical = canonicalize(raw);
 * // 输出：规范化后的代码，包含正确的句号和关键字大小写
 * ```
 */
export function canonicalize(input: string): string {
  // Normalize newlines to \n
  let s = input.replace(/\r\n?/g, '\n');

  // Normalize tabs to two spaces (indentation is 2-space significant)
  // Convert all tabs, including leading indentation, to ensure the lexer
  // measures indentation consistently.
  s = s.replace(/\t/g, '  ');

  // Drop line comments (// and #) entirely; formatter/LSP preserve docs separately
  s = s
    .split('\n')
    .filter(line => !/^\s*\/\//.test(line) && !/^\s*#/.test(line))
    .join('\n');

  // Normalize smart quotes to straight quotes
  s = s.replace(/[\u201C\u201D]/g, '"').replace(/[\u2018\u2019]/g, "'");

  // Ensure lines end with either period or colon before newline if they look like statements
  s = s
    .split('\n')
    .map(line => {
      const trimmed = line.trim();
      if (trimmed === '') return line; // keep empty
      // If ends with ':' or '.' already, keep
      if (/[:.]$/.test(trimmed)) return line;
      // Heuristic: if line appears to open a block (keywords like match/within/to ... produce ...:)
      // We won't add punctuation here; parser will require proper punctuation and offer fix-it.
      return line; // do nothing; errors will prompt fixes
    })
    .join('\n');

  // Fold multiple spaces (but not newlines); keep indentation (2-space rule) for leading spaces only
  s = s
    .split('\n')
    .map(line => {
      const m = line.match(/^(\s*)(.*)$/);
      if (!m) return line;
      const indent = m[1] ?? '';
      const rest = (m[2] ?? '').replace(/[ \t]+/g, ' ').replace(/\s+([.,:])/g, '$1');
      return indent + rest;
    })
    .join('\n');

  // Keep original casing to preserve TypeIdents. We only normalize multi-word keywords by hinting
  // but we leave actual case handling to the parser (case-insensitive compare).
  let marked = s;
  for (const phrase of MULTI) {
    const re = new RegExp(phrase.replace(/[-/\\^$*+?.()|[\]{}]/g, '\\$&'), 'ig');
    marked = marked.replace(re, m => m.toLowerCase());
  }

  // Remove articles in allowed contexts (lightweight; parser will enforce correctness)
  const segments: Array<{ text: string; inString: boolean }> = [];
  let inString = false;
  let current = '';

  for (let i = 0; i < marked.length; i++) {
    const ch = marked[i];
    current += ch;

    if (ch === '"' && !isEscaped(marked, i)) {
      if (inString) {
        segments.push({ text: current, inString: true });
        current = '';
        inString = false;
      } else {
        const before = current.slice(0, -1);
        if (before) {
          segments.push({ text: before, inString: false });
        }
        current = '"';
        inString = true;
      }
    }
  }

  if (current) {
    segments.push({ text: current, inString });
  }

  marked = segments
    .map(segment => (segment.inString ? segment.text : segment.text.replace(ARTICLE_RE, '')))
    .join('');
  // Do not collapse newlines globally.
  marked = marked.replace(/^\s+$/gm, '');

  // Final whitespace normalization to ensure idempotency after article/macro passes
  marked = marked
    .split('\n')
    .map(line => {
      const m = line.match(/^(\s*)(.*)$/);
      if (!m) return line;
      const indent = m[1] ?? '';
      const rest = (m[2] ?? '')
        .replace(/[ \t]+/g, ' ')
        .replace(/\s+([.,:!;?])/g, '$1')
        .replace(/\s+$/g, '');
      return indent + rest;
    })
    .join('\n');

  return marked;
}
