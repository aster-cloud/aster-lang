#!/usr/bin/env node
/**
 * 验证 shouldEnforcePii 渐进式启用策略
 *
 * 测试场景：
 * 1. 无环境变量 → 应返回 false (默认禁用，渐进式策略)
 * 2. ENFORCE_PII=false → 应返回 false
 * 3. ENFORCE_PII=true → 应返回 true (显式启用)
 * 4. ASTER_ENFORCE_PII=true → 应返回 true (显式启用)
 * 5. ASTER_ENFORCE_PII=false → 应返回 false
 * 6-8. 大小写变体 → 应返回 true (大小写无关匹配，与 Java 一致)
 */

import { shouldEnforcePii } from './dist/src/typecheck.js';

const scenarios = [
  {
    name: '场景1: 无环境变量(默认禁用)',
    env: {},
    expected: false
  },
  {
    name: '场景2: ENFORCE_PII=false',
    env: { ENFORCE_PII: 'false' },
    expected: false
  },
  {
    name: '场景3: ENFORCE_PII=true (显式启用)',
    env: { ENFORCE_PII: 'true' },
    expected: true
  },
  {
    name: '场景4: ASTER_ENFORCE_PII=true (显式启用)',
    env: { ASTER_ENFORCE_PII: 'true' },
    expected: true
  },
  {
    name: '场景5: ASTER_ENFORCE_PII=false',
    env: { ASTER_ENFORCE_PII: 'false' },
    expected: false
  },
  {
    name: '场景6: ENFORCE_PII=True (大小写无关)',
    env: { ENFORCE_PII: 'True' },
    expected: true
  },
  {
    name: '场景7: ENFORCE_PII=TRUE (大小写无关)',
    env: { ENFORCE_PII: 'TRUE' },
    expected: true
  },
  {
    name: '场景8: ASTER_ENFORCE_PII=TrUe (大小写无关)',
    env: { ASTER_ENFORCE_PII: 'TrUe' },
    expected: true
  }
];

let passed = 0;
let failed = 0;

console.log('验证 shouldEnforcePii 默认行为...\n');

for (const scenario of scenarios) {
  // 清除相关环境变量
  delete process.env.ENFORCE_PII;
  delete process.env.ASTER_ENFORCE_PII;
  delete process.env.DISABLE_PII;

  // 设置场景环境变量
  Object.assign(process.env, scenario.env);

  const result = shouldEnforcePii();
  const status = result === scenario.expected ? '✅ PASS' : '❌ FAIL';

  if (result === scenario.expected) {
    passed++;
  } else {
    failed++;
  }

  console.log(`${status} - ${scenario.name}`);
  console.log(`  期望: ${scenario.expected}, 实际: ${result}`);
  if (Object.keys(scenario.env).length > 0) {
    console.log(`  环境变量: ${JSON.stringify(scenario.env)}`);
  }
  console.log();
}

// 清理环境变量
delete process.env.ENFORCE_PII;
delete process.env.ASTER_ENFORCE_PII;
delete process.env.DISABLE_PII;

console.log(`\n总计: ${passed + failed} 测试`);
console.log(`✅ 通过: ${passed}`);
console.log(`❌ 失败: ${failed}`);

process.exit(failed === 0 ? 0 : 1);
