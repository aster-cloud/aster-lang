#!/usr/bin/env node
/**
 * TextMate 语法快照测试脚本
 */
const fs = require('fs');
const path = require('path');
const tm = require('vscode-textmate');
const oniguruma = require('vscode-oniguruma');

const ROOT = path.join(__dirname, '..');
const FIXTURE_DIR = path.join(ROOT, 'tests', 'syntax', 'fixtures');
const SNAPSHOT_DIR = path.join(ROOT, 'tests', 'syntax', '__snapshots__');
const GRAMMAR_PATH = path.join(ROOT, 'syntaxes', 'aster.tmLanguage.json');

const BASE_GRAMMAR = JSON.parse(fs.readFileSync(GRAMMAR_PATH, 'utf8'));
const BASE_REPO = BASE_GRAMMAR.repository || {};

const GRAMMAR_DEFS = {
  'block-comments.aster': {
    scopeName: 'source.aster.block-comments',
    patterns: [
      { include: '#comments' },
      { include: '#data-type-declaration' }
    ],
    repositories: [
      'comments',
      'data-type-declaration',
      'annotated-type',
      'map-to-syntax',
      'angle-bracket-generics',
      'annotations'
    ]
  },
  'type-alias.aster': {
    scopeName: 'source.aster.type-alias',
    patterns: [{ include: '#type-alias' }],
    repositories: ['type-alias', 'annotated-type', 'map-to-syntax', 'angle-bracket-generics', 'annotations']
  },
  'data-type-generics.aster': {
    scopeName: 'source.aster.data-type',
    patterns: [{ include: '#data-type-declaration' }],
    repositories: ['data-type-declaration', 'annotated-type', 'map-to-syntax', 'angle-bracket-generics', 'annotations']
  },
  'cnl-types.aster': {
    scopeName: 'source.aster.cnl',
    patterns: [
      { include: '#comments' },
      { include: '#function-declaration' },
      { include: '#type-alias' },
      { include: '#data-type-declaration' },
      { include: '#enum-declaration' }
    ],
    repositories: [
      'comments',
      'function-declaration',
      'type-alias',
      'data-type-declaration',
      'enum-declaration',
      'annotated-type',
      'map-to-syntax',
      'angle-bracket-generics',
      'annotations',
      'effects',
      'keywords'
    ]
  }
};

/**
 * Scope 断言配置：定义每个 fixture 的 scope 使用次数预期
 * 用于防止 and 分隔符回归（Round 31）
 */
const SCOPE_ASSERTIONS = {
  'block-comments.aster': {
    // Block comment scope should appear for ### ... ### blocks
    // Round 47: 块注释边界改为行首匹配，实际约 34 个 token
    'comment.block.aster': {
      count: { min: 30, max: 45, description: '块注释 scope 出现次数' }
    },
    // Line comment scope should still work alongside block comments
    'comment.line.number-sign.aster': {
      count: { min: 8, max: 20, description: '行注释 scope 出现次数' }
    }
  },
  'cnl-types.aster': {
    // and 作为参数分隔符只应出现在 with ... and ... 参数列表中
    // Round 33: handleResponse:12, transform:56, compose:60, validateUser:88 = 4处
    'keyword.control.separator.aster': {
      // 统计 and 作为分隔符的次数（不含逗号），若大幅增加说明可能回归
      andCount: { min: 3, max: 6, description: 'and 作为参数分隔符' }
    },
    // and 作为泛型关键字应出现在 Result of Text and Error 等类型表达式中
    // Round 33: 新增 article/capability 场景后增加到约 31 次
    'keyword.control.generic.aster': {
      // 统计 and/of 作为泛型关键字的次数，若大幅减少说明可能被误标为分隔符
      andOfCount: { min: 25, max: 40, description: 'and/of 作为泛型关键字' }
    }
  }
};

async function createRegistry() {
  const wasmPath = require.resolve('vscode-oniguruma/release/onig.wasm');
  const wasm = fs.readFileSync(wasmPath).buffer;
  await oniguruma.loadWASM(wasm);

  return new tm.Registry({
    onigLib: Promise.resolve({
      createOnigScanner(patterns) {
        return new oniguruma.OnigScanner(patterns);
      },
      createOnigString(s) {
        return new oniguruma.OnigString(s);
      }
    })
  });
}

function cloneRepositoryEntry(key) {
  if (!BASE_REPO[key]) {
    throw new Error(`仓库缺少 ${key} 定义`);
  }
  return JSON.parse(JSON.stringify(BASE_REPO[key]));
}

function buildIsolatedGrammar(def) {
  const repository = {};
  def.repositories.forEach((name) => {
    repository[name] = cloneRepositoryEntry(name);
  });
  return {
    scopeName: def.scopeName,
    patterns: def.patterns,
    repository
  };
}

async function loadGrammarForFixture(registry, fileName) {
  const def = GRAMMAR_DEFS[fileName];
  if (!def) {
    throw new Error(`未配置 ${fileName} 对应的语法定义`);
  }
  if (!def._instance) {
    def._instance = await registry.addGrammar(buildIsolatedGrammar(def));
  }
  return def._instance;
}

function normalizeNewlines(value) {
  return value.replace(/\r\n?/g, '\n');
}

function tokenize(grammar, text) {
  const tokens = [];
  const lines = normalizeNewlines(text).split('\n');
  let ruleStack = null;

  lines.forEach((line, lineIndex) => {
    const result = grammar.tokenizeLine(line, ruleStack);
    ruleStack = result.ruleStack;
    result.tokens.forEach((token) => {
      tokens.push({
        line: lineIndex + 1,
        startIndex: token.startIndex,
        endIndex: token.endIndex,
        text: line.slice(token.startIndex, token.endIndex),
        scopes: token.scopes
      });
    });
  });

  return tokens;
}

function readFixtureFiles() {
  if (!fs.existsSync(FIXTURE_DIR)) {
    return [];
  }
  return fs
    .readdirSync(FIXTURE_DIR)
    .filter((file) => file.endsWith('.aster'))
    .sort()
    .map((file) => ({
      name: file,
      path: path.join(FIXTURE_DIR, file)
    }));
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function writeSnapshot(fileName, tokens) {
  ensureDir(SNAPSHOT_DIR);
  const snapshotPath = path.join(SNAPSHOT_DIR, `${fileName}.snapshot.json`);
  fs.writeFileSync(snapshotPath, JSON.stringify(tokens, null, 2) + '\n');
  console.log(`写入快照: ${path.relative(ROOT, snapshotPath)}`);
}

function compareSnapshot(fileName, tokens) {
  const snapshotPath = path.join(SNAPSHOT_DIR, `${fileName}.snapshot.json`);
  if (!fs.existsSync(snapshotPath)) {
    throw new Error(`缺少快照文件：${path.relative(ROOT, snapshotPath)}，请运行 --update 生成`);
  }
  const expected = JSON.parse(fs.readFileSync(snapshotPath, 'utf8'));
  const actualSerialized = JSON.stringify(tokens, null, 2);
  const expectedSerialized = JSON.stringify(expected, null, 2);
  if (actualSerialized !== expectedSerialized) {
    throw new Error(`语法快照不匹配：${fileName}，请审查差异或使用 --update 更新`);
  }
}

/**
 * 验证 scope 使用次数断言（Round 31：防止分隔符回归，Round 46：块注释验证）
 */
function verifyScopeAssertions(fileName, tokens) {
  const assertions = SCOPE_ASSERTIONS[fileName];
  if (!assertions) return [];

  const errors = [];

  // 遍历所有断言
  for (const [scopeName, scopeAssertions] of Object.entries(assertions)) {
    // 通用 count 断言：统计 scope 出现次数
    if (scopeAssertions.count) {
      const scopeCount = tokens.filter(
        (t) => t.scopes.includes(scopeName)
      ).length;

      const { min, max, description } = scopeAssertions.count;
      if (scopeCount < min || scopeCount > max) {
        errors.push(
          `${description}：实际 ${scopeCount} 次，预期 ${min}-${max} 次`
        );
      }
    }

    // andCount 断言：统计特定 scope 中 and 的数量
    if (scopeAssertions.andCount) {
      const andCount = tokens.filter(
        (t) =>
          t.scopes.includes(scopeName) &&
          t.text.toLowerCase() === 'and'
      ).length;

      const { min, max, description } = scopeAssertions.andCount;
      if (andCount < min || andCount > max) {
        errors.push(
          `${description}：实际 ${andCount} 次，预期 ${min}-${max} 次`
        );
      }
    }

    // andOfCount 断言：统计特定 scope 中 and/of 的数量
    if (scopeAssertions.andOfCount) {
      const andOfCount = tokens.filter(
        (t) =>
          t.scopes.includes(scopeName) &&
          ['and', 'of'].includes(t.text.toLowerCase())
      ).length;

      const { min, max, description } = scopeAssertions.andOfCount;
      if (andOfCount < min || andOfCount > max) {
        errors.push(
          `${description}：实际 ${andOfCount} 次，预期 ${min}-${max} 次`
        );
      }
    }
  }

  return errors;
}

async function main() {
  const update = process.argv.includes('--update');
  const registry = await createRegistry();

  const fixtures = readFixtureFiles();
  if (fixtures.length === 0) {
    throw new Error('tests/syntax/fixtures 目录为空，无法执行语法测试');
  }

  let allAssertionErrors = [];

  for (const fixture of fixtures) {
    const grammar = await loadGrammarForFixture(registry, fixture.name);
    const contents = fs.readFileSync(fixture.path, 'utf8');
    const tokens = tokenize(grammar, contents);
    if (update) {
      writeSnapshot(fixture.name, tokens);
    } else {
      compareSnapshot(fixture.name, tokens);
      console.log(`快照验证通过：${fixture.name}`);
    }

    // 验证 scope 断言（无论是否 update 都执行）
    const assertionErrors = verifyScopeAssertions(fixture.name, tokens);
    if (assertionErrors.length > 0) {
      allAssertionErrors.push({ fixture: fixture.name, errors: assertionErrors });
    }
  }

  // 报告所有断言错误
  if (allAssertionErrors.length > 0) {
    console.error('\n⚠️  Scope 断言失败：');
    for (const { fixture, errors } of allAssertionErrors) {
      console.error(`  ${fixture}:`);
      errors.forEach((err) => console.error(`    - ${err}`));
    }
    throw new Error('Scope 断言验证失败');
  }

  console.log('\n✅ 所有 scope 断言通过');
}

main().catch((err) => {
  console.error(err.message || err);
  process.exitCode = 1;
});
