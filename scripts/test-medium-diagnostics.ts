#!/usr/bin/env node
/**
 * 专门测试 Medium 项目（40文件）的诊断耗时
 * 目的：准确测量首次诊断和缓存命中的实际耗时
 */

import { performance } from 'node:perf_hooks';
import { TextDocument } from 'vscode-languageserver-textdocument';
import { computeDiagnostics } from '../src/lsp/diagnostics.js';
import { lex } from '../src/lexer.js';
import { parse } from '../src/parser.js';
import { canonicalize } from '../src/canonicalizer.js';
import { promises as fs } from 'node:fs';
import { join } from 'node:path';

// CNL examples 路径
const EXAMPLES_PATH = join(process.cwd(), 'cnl/examples');

// 模拟 getOrParse 函数
function getOrParse(doc: TextDocument): { text: string; tokens: readonly any[]; ast: any } {
  const text = doc.getText();
  const can = canonicalize(text);
  const tokens = lex(can);
  let ast: any;
  try {
    ast = parse(tokens);
  } catch {
    ast = null;
  }
  return { text, tokens, ast };
}

async function loadExampleFiles(): Promise<TextDocument[]> {
  console.log('📂 Loading CNL example files...');
  const files = await fs.readdir(EXAMPLES_PATH);
  const cnlFiles = files.filter(f => f.endsWith('.cnl'));

  const documents: TextDocument[] = [];
  for (const file of cnlFiles) {
    const filePath = join(EXAMPLES_PATH, file);
    const content = await fs.readFile(filePath, 'utf8');
    const uri = `file://${filePath}`;
    const doc = TextDocument.create(uri, 'cnl', 1, content);
    documents.push(doc);
  }

  console.log(`✅ Loaded ${documents.length} files`);
  return documents;
}

async function testDiagnostics(documents: TextDocument[], label: string): Promise<{ total: number; avg: number; p50: number; p95: number; p99: number; results: number[] }> {
  console.log(`\n🧪 Test: ${label}`);
  console.log('─'.repeat(60));

  const results: number[] = [];

  for (let i = 0; i < documents.length; i++) {
    const doc = documents[i];
    if (!doc) continue; // TypeScript guard
    const fileName = doc.uri.split('/').pop();

    const start = performance.now();
    try {
      const diagnostics = await computeDiagnostics(doc, getOrParse);
      const duration = performance.now() - start;
      results.push(duration);

      console.log(
        `  [${(i + 1).toString().padStart(2)}/${documents.length}] ${(fileName ?? 'unknown').padEnd(30)} ` +
        `${duration.toFixed(2).padStart(8)}ms  (${diagnostics.length} diagnostics)`
      );
    } catch (error) {
      const duration = performance.now() - start;
      results.push(duration);
      console.log(
        `  [${(i + 1).toString().padStart(2)}/${documents.length}] ${(fileName ?? 'unknown').padEnd(30)} ` +
        `${duration.toFixed(2).padStart(8)}ms  ❌ ERROR: ${error instanceof Error ? error.message : String(error)}`
      );
    }
  }

  // 计算统计数据
  const total = results.reduce((a, b) => a + b, 0);
  const sorted = [...results].sort((a, b) => a - b);
  const p50 = sorted[Math.floor(sorted.length * 0.5)] ?? 0;
  const p95 = sorted[Math.floor(sorted.length * 0.95)] ?? 0;
  const p99 = sorted[Math.floor(sorted.length * 0.99)] ?? 0;
  const avg = total / results.length;

  console.log('─'.repeat(60));
  console.log(`📊 Statistics:`);
  console.log(`  Total time:    ${total.toFixed(2)}ms`);
  console.log(`  Average:       ${avg.toFixed(2)}ms`);
  console.log(`  p50 (median):  ${p50.toFixed(2)}ms`);
  console.log(`  p95:           ${p95.toFixed(2)}ms`);
  console.log(`  p99:           ${p99.toFixed(2)}ms`);
  console.log(`  Min:           ${Math.min(...results).toFixed(2)}ms`);
  console.log(`  Max:           ${Math.max(...results).toFixed(2)}ms`);

  return { total, avg, p50, p95, p99, results };
}

async function main(): Promise<void> {
  console.log('🚀 CNL Examples Diagnostics Performance Test');
  console.log('='.repeat(60));

  // 加载所有文件
  const documents = await loadExampleFiles();

  // Test 1: 首次诊断（冷启动）
  const coldStart = await testDiagnostics(documents, 'Cold Start (First Run)');

  // Test 2: 第二次诊断（缓存命中）
  const warmStart = await testDiagnostics(documents, 'Warm Start (Cached)');

  // 对比
  console.log('\n📈 Performance Comparison:');
  console.log('='.repeat(60));
  console.log(`Cold Start Total:  ${coldStart.total.toFixed(2)}ms`);
  console.log(`Warm Start Total:  ${warmStart.total.toFixed(2)}ms`);
  console.log(`Speedup:           ${(coldStart.total / (warmStart.total || 1)).toFixed(2)}x`);
  console.log(`\nCold Start p50:    ${coldStart.p50.toFixed(2)}ms`);
  console.log(`Warm Start p50:    ${warmStart.p50.toFixed(2)}ms`);
  console.log(`Speedup:           ${(coldStart.p50 / (warmStart.p50 || 1)).toFixed(2)}x`);

  // 分析缓存效果
  const cacheHitCount = warmStart.results.filter(t => t < 1).length;
  const cacheHitRate = (cacheHitCount / warmStart.results.length) * 100;
  console.log(`\n🎯 Cache Performance:`);
  console.log(`  Cache hits (<1ms): ${cacheHitCount}/${warmStart.results.length} (${cacheHitRate.toFixed(1)}%)`);
}

main().catch(error => {
  console.error('❌ Test failed:', error);
  process.exit(1);
});
