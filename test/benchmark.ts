#!/usr/bin/env node
import fs from 'node:fs';
import { canonicalize } from '../src/canonicalizer.js';
import { lex } from '../src/lexer.js';
import { parse } from '../src/parser.js';
import { lowerModule } from '../src/lower_to_core.js';

function benchmark<T>(name: string, fn: () => T, iterations = 1000): { result: T; avgMs: number; opsPerSec: number } {
  // Warm up
  for (let i = 0; i < 10; i++) {
    fn();
  }
  
  const start = process.hrtime.bigint();
  let result: T;
  for (let i = 0; i < iterations; i++) {
    result = fn();
  }
  const end = process.hrtime.bigint();
  
  const totalMs = Number(end - start) / 1_000_000;
  const avgMs = totalMs / iterations;
  const opsPerSec = 1000 / avgMs;
  
  console.log(`${name}: ${avgMs.toFixed(3)}ms avg, ${opsPerSec.toFixed(0)} ops/sec`);
  
  return { result: result!, avgMs, opsPerSec };
}

function generateLargeProgram(size: number): string {
  const lines = [
    'This module is benchmark.test.',
    '',
    'Define User with id: Text and name: Text and email: Text.',
    'Define Status as one of Active or Inactive or Pending.',
    '',
  ];
  
  for (let i = 0; i < size; i++) {
    lines.push(`To process${i} with user: User, produce Status:`);
    lines.push(`  Let id be user.id.`);
    lines.push(`  Let name be user.name.`);
    lines.push(`  If name,:`);
    lines.push(`    Return Active.`);
    lines.push(`  Return Inactive.`);
    lines.push('');
  }
  
  return lines.join('\n');
}

/** 生成Medium规模项目（30-50模块，约3000-5000行） */
export function generateMediumProject(moduleCount = 40, baseSeed = 42): Map<string, string> {
  const modules = new Map<string, string>();
  modules.set('benchmark.medium.common', generateCommonModule());
  
  const random = createSeededRandom(baseSeed);
  for (let i = 1; i < moduleCount; i++) {
    const moduleName = `benchmark.medium.module${i}`;
    const needsImport = random() < 0.3;
    modules.set(moduleName, generateBusinessModule(moduleName, baseSeed + i * 17, needsImport));
  }
  
  return modules;
}

/** 生成通用模块，提供共享类型与函数 */
function generateCommonModule(): string {
  const lines = [
    'This module is benchmark.medium.common.',
    '',
    'Define LogLevel as one of Info or Warn or Error or Debug.',
    'Define HttpMethod as one of Get or Post or Put or Delete.',
    '',
    'Define RequestContext with requestId: Text and path: Text and method: Text and retries: Number.',
    'Define ResponseContext with status: Number and payload: Text and success: Boolean.',
    '',
    'To buildRequestId with prefix: Text and id: Number, produce Text:',
    '  Let base be prefix.',
    '  If base,:',
    '    Return base.',
    '  Return "req-default".',
    '',
    'To defaultLogLevel, produce LogLevel:',
    '  Return Info.',
    '',
    'To ensureSuccess with response: ResponseContext, produce Boolean:',
    '  Let flag be response.success.',
    '  If flag,:',
    '    Return true.',
    '  Return false.',
    '',
    'To renderPath with ctx: RequestContext, produce Text:',
    '  Let path be ctx.path.',
    '  If path,:',
    '    Return path.',
    '  Return "/".',
    '',
    'To emitLog with message: Text and level: LogLevel, produce Text. It performs io:',
    '  Let text be message.',
    '  If text,:',
    '    Return text.',
    '  Return "log".',
    '',
  ];
  
  return lines.join('\n');
}

/** 生成业务模块，控制模块内类型与函数数量 */
function generateBusinessModule(name: string, seed: number, needsImport: boolean): string {
  const rand = createSeededRandom(seed);
  const recordTemplates = getRecordTemplates();
  const sumTemplates = getSumTemplates();
  
  const records = [] as { name: string; fields: { name: string; type: string }[] }[];
  const sums = [] as { name: string; variants: string[] }[];
  
  const lines: string[] = [];
  lines.push(`This module is ${name}.`);
  lines.push('');
  
  if (needsImport) {
    lines.push('Use benchmark.medium.common.');
    lines.push('');
  }
  
  const recordCount = 2 + Math.floor(rand() * 2);
  const sumCount = 1 + Math.floor(rand() * 2);
  
  for (let i = 0; i < recordCount; i++) {
    const template = recordTemplates[(seed + i) % recordTemplates.length]!;
    const typeName = `${template.base}${seed}${i}`;
    const fieldParts = template.fields.map(field => `${field.name}: ${field.type}`);
    lines.push(`Define ${typeName} with ${fieldParts.join(' and ')}.`);
    lines.push('');
    records.push({ name: typeName, fields: template.fields });
  }
  
  for (let i = 0; i < sumCount; i++) {
    const template = sumTemplates[(seed + i * 3) % sumTemplates.length]!;
    const typeName = `${template.base}${seed}${i}`;
    lines.push(`Define ${typeName} as one of ${template.variants.join(' or ')}.`);
    lines.push('');
    sums.push({ name: typeName, variants: template.variants });
  }
  
  const functionCount = 8 + Math.floor(rand() * 5);
  let effectCounter = 0;
  
  for (let i = 0; i < functionCount; i++) {
    const effectful = i % 10 === 0;
    if (effectful) {
      effectCounter++;
      lines.push(...generateEffectfulFunction(seed, i));
    } else {
      lines.push(...generateRoutineFunction(records, sums, seed, i));
    }
    lines.push('');
  }
  
  if (effectCounter === 0 && functionCount > 0) {
    lines.splice(lines.length - 1, 0, ...generateEffectfulFunction(seed, functionCount));
    lines.push('');
  }
  
  return lines.join('\n');
}

/** 生成带效果声明的函数，确保比例约为10% */
function generateEffectfulFunction(seed: number, fnIndex: number): string[] {
  const functionName = `fetch${seed}${fnIndex}`;
  return [
    `To ${functionName} with resource: Text, produce Text. It performs io:`,
    '  Let value be resource.',
    '  If value,:',
    '    Return value.',
    '  Return "unavailable".',
  ];
}

/** 生成常规函数，实现控制流与绑定多样性 */
function generateRoutineFunction(
  records: { name: string; fields: { name: string; type: string }[] }[],
  sums: { name: string; variants: string[] }[],
  seed: number,
  fnIndex: number,
): string[] {
  const parts: string[] = [];
  
  if (records.length === 0) {
    records.push({
      name: `TempRecord${seed}${fnIndex}`,
      fields: [
        { name: 'id', type: 'Text' },
        { name: 'name', type: 'Text' },
        { name: 'flag', type: 'Boolean' },
      ],
    });
  }
  if (sums.length === 0) {
    sums.push({
      name: `TempSum${seed}${fnIndex}`,
      variants: ['Alpha', 'Beta', 'Gamma'],
    });
  }
  
  const recordA = records[fnIndex % records.length]!;
  const recordB = records[(fnIndex + 1) % records.length]!;
  const sum = sums[fnIndex % sums.length]!;
  const primaryVariant = sum.variants[fnIndex % sum.variants.length] ?? sum.variants[0] ?? 'Alpha';
  const secondaryVariant = sum.variants[(fnIndex + 1) % sum.variants.length] ?? primaryVariant;
  const textFieldA = getTextField(recordA);
  const textFieldB = getTextField(recordB);
  const booleanFieldA = getBooleanField(recordA);
  
  const signatureVariants = fnIndex % 4;
  switch (signatureVariants) {
    case 0: {
      const functionName = `format${seed}${fnIndex}`;
      parts.push(
        `To ${functionName} with item: ${recordA.name}, produce Text:`,
        `  Let value be item.${textFieldA}.`,
        '  If value,:',
        '    Return value.',
        '  Return "unknown".',
      );
      return parts;
    }
    case 1: {
      const functionName = `evaluate${seed}${fnIndex}`;
      parts.push(
        `To ${functionName} with item: ${recordA.name} and status: ${sum.name}, produce ${sum.name}:`,
        `  Let active be item.${booleanFieldA}.`,
        '  If active,:',
        '    Let current be status.',
        '    If current,:',
        '      Return current.',
        `    Return ${primaryVariant}.`,
        `  Return ${secondaryVariant}.`,
      );
      return parts;
    }
    case 2: {
      const functionName = `compare${seed}${fnIndex}`;
      parts.push(
        `To ${functionName} with left: ${recordA.name} and right: ${recordB.name}, produce Boolean:`,
        `  Let first be left.${textFieldA}.`,
        `  Let second be right.${textFieldB}.`,
        '  If first,:',
        '    If second,:',
        '      Return true.',
        '  Return false.',
      );
      return parts;
    }
    default: {
      const functionName = `current${seed}${fnIndex}`;
      parts.push(
        `To ${functionName}, produce Text:`,
        `  Let mark be "${recordA.name}-${fnIndex}".`,
        '  If mark,:',
        `    Return mark.`,
        '  Return "constant".',
      );
      return parts;
    }
  }
}

/** 提供可复用的记录类型模板 */
function getRecordTemplates(): { base: string; fields: { name: string; type: string }[] }[] {
  return [
    {
      base: 'User',
      fields: [
        { name: 'id', type: 'Text' },
        { name: 'name', type: 'Text' },
        { name: 'email', type: 'Text' },
        { name: 'isActive', type: 'Boolean' },
      ],
    },
    {
      base: 'Config',
      fields: [
        { name: 'endpoint', type: 'Text' },
        { name: 'timeout', type: 'Number' },
        { name: 'retries', type: 'Number' },
        { name: 'enabled', type: 'Boolean' },
      ],
    },
    {
      base: 'Request',
      fields: [
        { name: 'path', type: 'Text' },
        { name: 'method', type: 'Text' },
        { name: 'payload', type: 'Text' },
        { name: 'attempts', type: 'Number' },
      ],
    },
    {
      base: 'Profile',
      fields: [
        { name: 'nickname', type: 'Text' },
        { name: 'createdAt', type: 'Text' },
        { name: 'score', type: 'Number' },
        { name: 'verified', type: 'Boolean' },
      ],
    },
  ];
}

/** 提供可复用的和类型模板 */
function getSumTemplates(): { base: string; variants: string[] }[] {
  return [
    { base: 'Status', variants: ['Ready', 'Busy', 'Error', 'Pending'] },
    { base: 'ErrorKind', variants: ['Network', 'Timeout', 'Invalid', 'Unknown'] },
    { base: 'ResultFlag', variants: ['Ok', 'Retry', 'Fail'] },
    { base: 'Mode', variants: ['Live', 'Test', 'Maintenance'] },
  ];
}

/** 返回记录类型中的文本字段名称 */
function getTextField(record: { fields: { name: string; type: string }[] }): string {
  const item = record.fields.find(field => field.type === 'Text');
  return item ? item.name : record.fields[0]?.name ?? 'id';
}

/** 返回记录类型中的布尔字段名称 */
function getBooleanField(record: { fields: { name: string; type: string }[] }): string {
  const item = record.fields.find(field => field.type === 'Boolean');
  return item ? item.name : record.fields[record.fields.length - 1]?.name ?? 'isActive';
}

/** 构建可复用的确定性随机数生成器 */
function createSeededRandom(seed: number): () => number {
  let state = Math.abs(seed) % 233280;
  return () => {
    state = (state * 9301 + 49297) % 233280;
    return state / 233280;
  };
}

function main(): void {
  console.log('Running performance benchmarks...\n');
  
  // Load test files
  const greetProgram = fs.readFileSync('cnl/examples/greet.cnl', 'utf8');
  const loginProgram = fs.readFileSync('cnl/examples/login.cnl', 'utf8');
  const largeProgram = generateLargeProgram(50); // 50 functions
  
  console.log('=== Small Programs ===');
  
  // Benchmark canonicalizer
  benchmark('Canonicalize (greet)', () => canonicalize(greetProgram), 5000);
  benchmark('Canonicalize (login)', () => canonicalize(loginProgram), 5000);
  
  // Benchmark lexer
  const greetCan = canonicalize(greetProgram);
  const loginCan = canonicalize(loginProgram);
  
  benchmark('Lex (greet)', () => lex(greetCan), 3000);
  benchmark('Lex (login)', () => lex(loginCan), 3000);
  
  // Benchmark parser
  const greetTokens = lex(greetCan);
  const loginTokens = lex(loginCan);
  
  benchmark('Parse (greet)', () => parse(greetTokens), 2000);
  benchmark('Parse (login)', () => parse(loginTokens), 2000);
  
  // Benchmark lowering
  const greetAst = parse(greetTokens);
  const loginAst = parse(loginTokens);
  
  benchmark('Lower (greet)', () => lowerModule(greetAst), 3000);
  benchmark('Lower (login)', () => lowerModule(loginAst), 3000);
  
  // Full pipeline benchmarks
  benchmark('Full pipeline (greet)', () => {
    const can = canonicalize(greetProgram);
    const tokens = lex(can);
    const ast = parse(tokens);
    return lowerModule(ast);
  }, 1000);
  
  benchmark('Full pipeline (login)', () => {
    const can = canonicalize(loginProgram);
    const tokens = lex(can);
    const ast = parse(tokens);
    return lowerModule(ast);
  }, 1000);
  
  console.log('\n=== Large Program (50 functions) ===');
  
  const largeCan = canonicalize(largeProgram);
  const largeTokens = lex(largeCan);
  const largeAst = parse(largeTokens);
  
  console.log(`Program size: ${largeProgram.length} chars, ${largeTokens.length} tokens`);
  
  benchmark('Canonicalize (large)', () => canonicalize(largeProgram), 100);
  benchmark('Lex (large)', () => lex(largeCan), 100);
  benchmark('Parse (large)', () => parse(largeTokens), 50);
  benchmark('Lower (large)', () => lowerModule(largeAst), 100);
  
  benchmark('Full pipeline (large)', () => {
    const can = canonicalize(largeProgram);
    const tokens = lex(can);
    const ast = parse(tokens);
    return lowerModule(ast);
  }, 20);
  
  console.log('\n=== Memory Usage ===');
  
  const memBefore = process.memoryUsage();
  
  // Process large program multiple times
  for (let i = 0; i < 100; i++) {
    const can = canonicalize(largeProgram);
    const tokens = lex(can);
    const ast = parse(tokens);
    lowerModule(ast);
  }
  
  const memAfter = process.memoryUsage();
  
  console.log(`Heap used: ${((memAfter.heapUsed - memBefore.heapUsed) / 1024 / 1024).toFixed(2)} MB`);
  console.log(`Heap total: ${((memAfter.heapTotal - memBefore.heapTotal) / 1024 / 1024).toFixed(2)} MB`);
  console.log(`RSS: ${((memAfter.rss - memBefore.rss) / 1024 / 1024).toFixed(2)} MB`);
  
  // Performance thresholds (fail if too slow)
  const greetPipelineResult = benchmark('Performance check (greet)', () => {
    const can = canonicalize(greetProgram);
    const tokens = lex(can);
    const ast = parse(tokens);
    return lowerModule(ast);
  }, 100);
  
  if (greetPipelineResult.avgMs > 5.0) {
    console.error(`❌ Performance regression: greet pipeline took ${greetPipelineResult.avgMs.toFixed(3)}ms (threshold: 5.0ms)`);
    process.exit(1);
  }
  
  console.log('\n✅ All benchmarks completed successfully!');
  console.log(`Greet pipeline: ${greetPipelineResult.opsPerSec.toFixed(0)} ops/sec (${greetPipelineResult.avgMs.toFixed(3)}ms avg)`);
}

main();
