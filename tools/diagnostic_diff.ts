#!/usr/bin/env ts-node
/**
 * 对比 Java 与 TypeScript 类型检查诊断输出，确保 severity/code/message/span 等字段一致。
 */
import { promises as fs } from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';
import {
  COLORS,
  ERROR_CODES_PATH,
  FAILURE_PREFIX,
  OUTPUT_LIMIT,
  buildErrorCodeMap,
  diffDiagnostics,
  normalizeDiagnostics,
  reportDifferences,
  type DiffOptions,
  type JsonValue,
} from './diagnostic_diff_core.js';

async function main(): Promise<void> {
  let ignoreSpan = false;
  const positional: string[] = [];
  for (const arg of process.argv.slice(2)) {
    if (arg === '--ignore-span') {
      ignoreSpan = true;
      continue;
    }
    positional.push(arg);
  }
  const [leftPath, rightPath] = positional;
  if (!leftPath || !rightPath) {
    console.error(`${FAILURE_PREFIX} 用法: tools/diagnostic_diff.ts [--ignore-span] <java-output.json> <ts-output.json>`);
    process.exitCode = 2;
    return;
  }

  try {
    const [leftRaw, rightRaw, errorCodeRaw] = await Promise.all([
      fs.readFile(leftPath, 'utf8'),
      fs.readFile(rightPath, 'utf8'),
      fs.readFile(ERROR_CODES_PATH, 'utf8'),
    ]);

    const errorCodeMap = buildErrorCodeMap(JSON.parse(errorCodeRaw) as JsonValue);
    const leftDiagnostics = normalizeDiagnostics(JSON.parse(leftRaw) as JsonValue, '左侧', errorCodeMap);
    const rightDiagnostics = normalizeDiagnostics(JSON.parse(rightRaw) as JsonValue, '右侧', errorCodeMap);

    const totalDiagnostics = Math.max(leftDiagnostics.length, rightDiagnostics.length);
    if (totalDiagnostics > 1000) {
      console.error(`${COLORS.yellow}⏳ 检测到 ${totalDiagnostics} 条诊断，输出将自动截断。${COLORS.reset}`);
    }

    const differences = diffDiagnostics(leftDiagnostics, rightDiagnostics, ignoreSpan ? { ignoreSpan: true } : {});
    const hasDifference = reportDifferences('诊断', differences, OUTPUT_LIMIT);
    if (hasDifference) {
      process.exitCode = 1;
    }
  } catch (error) {
    console.error(`${FAILURE_PREFIX} 诊断比对失败:`, error);
    process.exitCode = 1;
  }
}

const selfPath = fileURLToPath(import.meta.url);
const entryPoint = process.argv[1] ? path.resolve(process.argv[1]!) : undefined;

if (entryPoint === selfPath) {
  void main().catch(error => {
    console.error(`${FAILURE_PREFIX} 诊断比对失败:`, error);
    process.exitCode = 1;
  });
}

// 供其他模块复用核心类型与函数
export {
  COLORS,
  FAILURE_PREFIX,
  OUTPUT_LIMIT,
  buildErrorCodeMap,
  diffDiagnostics,
  normalizeDiagnostics,
  reportDifferences,
} from './diagnostic_diff_core.js';
