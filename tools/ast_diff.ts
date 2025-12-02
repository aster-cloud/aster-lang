#!/usr/bin/env ts-node
/**
 * 对比 Java 与 TypeScript 类型检查器生成的 AST 或诊断 JSON，确保字段、类型与取值一致。
 */
import { promises as fs } from 'node:fs';
import process from 'node:process';
import {
  COLORS,
  ERROR_CODES_PATH,
  FAILURE_PREFIX,
  OUTPUT_LIMIT,
  SUCCESS_PREFIX,
  buildErrorCodeMap,
  colorText,
  diffDiagnostics,
  normalizeDiagnostics,
  reportDifferences,
  type JsonValue,
} from './diagnostic_diff_core.js';

type Mode = 'ast' | 'diagnostics';

interface CliOptions {
  mode: Mode;
  showHelp: boolean;
}

interface ParsedArgs {
  readonly options: CliOptions;
  readonly positional: string[];
}

async function main(): Promise<void> {
  let parsed: ParsedArgs;
  try {
    parsed = parseArgs(process.argv.slice(2));
  } catch (error) {
    console.error(`${FAILURE_PREFIX} 参数错误: ${(error as Error).message}`);
    process.exitCode = 2;
    return;
  }
  const [leftPath, rightPath] = parsed.positional;

  if (parsed.options.showHelp) {
    printUsage();
    return;
  }
  if (!leftPath || !rightPath) {
    printUsage();
    process.exitCode = 2;
    return;
  }

  try {
    if (parsed.options.mode === 'diagnostics') {
      await runDiagnosticsMode(leftPath, rightPath);
    } else {
      await runAstMode(leftPath, rightPath);
    }
  } catch (error) {
    console.error(`${FAILURE_PREFIX} 比对失败:`, error);
    process.exitCode = 1;
  }
}

function parseArgs(argv: string[]): ParsedArgs {
  const options: CliOptions = { mode: 'ast', showHelp: false };
  const positional: string[] = [];

  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]!;
    if (arg === '-h' || arg === '--help') {
      options.showHelp = true;
      continue;
    }
    if (arg === '-m' || arg === '--mode') {
      const value = argv[++i];
      if (!value) {
        throw new Error('--mode 需要值: ast 或 diagnostics');
      }
      options.mode = parseMode(value);
      continue;
    }
    if (arg.startsWith('--mode=')) {
      const [, value] = arg.split('=', 2);
      options.mode = parseMode(value ?? '');
      continue;
    }
    if (arg === '--') {
      positional.push(...argv.slice(i + 1));
      break;
    }
    if (arg.startsWith('-')) {
      throw new Error(`未知参数: ${arg}`);
    }
    positional.push(arg);
  }

  return { options, positional };
}

function parseMode(value: string): Mode {
  const normalized = value.trim().toLowerCase();
  if (normalized === 'diagnostics' || normalized === 'diagnostic') {
    return 'diagnostics';
  }
  if (normalized === 'ast') {
    return 'ast';
  }
  throw new Error(`不支持的 mode: ${value}`);
}

async function runAstMode(leftPath: string, rightPath: string): Promise<void> {
  const [leftJson, rightJson] = await Promise.all([readJson(leftPath), readJson(rightPath)]);
  const differences = diffAst(normalizeAst(leftJson), normalizeAst(rightJson));
  if (reportDifferences('AST', differences)) {
    process.exitCode = 1;
  }
}

async function runDiagnosticsMode(leftPath: string, rightPath: string): Promise<void> {
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

  const differences = diffDiagnostics(leftDiagnostics, rightDiagnostics);
  if (reportDifferences('诊断', differences)) {
    process.exitCode = 1;
  }
}

async function readJson(filePath: string): Promise<JsonValue> {
  const raw = await fs.readFile(filePath, 'utf8');
  try {
    return JSON.parse(raw) as JsonValue;
  } catch (error) {
    throw new Error(`${filePath} 解析 JSON 失败: ${(error as Error).message}`);
  }
}

function normalizeAst(value: JsonValue): JsonValue {
  if (Array.isArray(value)) {
    return value.map(normalizeAst);
  }
  if (value && typeof value === 'object') {
    const entries = Object.entries(value)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([k, v]) => [k, normalizeAst(v)] as const);
    return Object.fromEntries(entries);
  }
  return value;
}

function diffAst(left: JsonValue, right: JsonValue, path = '$'): string[] {
  if (Object.is(left, right)) return [];

  if (Array.isArray(left) && Array.isArray(right)) {
    const len = Math.max(left.length, right.length);
    const result: string[] = [];
    for (let i = 0; i < len; i += 1) {
      const nextPath = `${path}[${i}]`;
      if (i >= left.length) {
        result.push(`${highlightPath(nextPath)} 仅存在于右侧: ${highlightRight(formatValue(right[i]))}`);
        continue;
      }
      if (i >= right.length) {
        result.push(`${highlightPath(nextPath)} 仅存在于左侧: ${highlightLeft(formatValue(left[i]))}`);
        continue;
      }
      result.push(...diffAst(left[i]!, right[i]!, nextPath));
    }
    return result;
  }

  if (isRecord(left) && isRecord(right)) {
    const result: string[] = [];
    const keys = new Set([...Object.keys(left), ...Object.keys(right)]);
    for (const key of Array.from(keys).sort()) {
      const nextPath = `${path}.${key}`;
      if (!(key in left)) {
        result.push(`${highlightPath(nextPath)} 缺失于左侧，右侧值为 ${highlightRight(formatValue(right[key]))}`);
        continue;
      }
      if (!(key in right)) {
        result.push(`${highlightPath(nextPath)} 缺失于右侧，左侧值为 ${highlightLeft(formatValue(left[key]))}`);
        continue;
      }
      result.push(...diffAst(left[key]!, right[key]!, nextPath));
    }
    return result;
  }

  return [
    `${highlightPath(path)} 左右值不一致 -> 左 ${highlightLeft(formatValue(left))} | 右 ${highlightRight(formatValue(right))}`,
  ];
}

function isRecord(value: JsonValue): value is Record<string, JsonValue> {
  return !!value && typeof value === 'object' && !Array.isArray(value);
}

function formatValue(value: JsonValue | undefined): string {
  if (value === null) return 'null';
  if (value === undefined) return 'undefined';
  if (typeof value === 'string') {
    const truncated = value.length > 120 ? `${value.slice(0, 117)}…` : value;
    return JSON.stringify(truncated);
  }
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value);
  }
  if (Array.isArray(value)) return `[数组(${value.length})]`;
  return '{对象}';
}

function highlightPath(value: string): string {
  return colorText(value, COLORS.cyan);
}

function highlightLeft(value: string): string {
  return colorText(value, COLORS.red);
}

function highlightRight(value: string): string {
  return colorText(value, COLORS.green);
}

function printUsage(): void {
  console.log('用法: tools/ast_diff.ts [--mode=ast|diagnostics] <java-output.json> <ts-output.json>');
  console.log('默认模式为 AST。使用 --mode=diagnostics 时，会复用诊断对齐逻辑并输出高亮差异。');
}

void main().catch(error => {
  console.error(`${FAILURE_PREFIX} AST 比对失败:`, error);
  process.exitCode = 1;
});
