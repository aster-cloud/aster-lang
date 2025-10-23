#!/usr/bin/env ts-node
/**
 * 对比 Java 与 TypeScript 类型检查器生成的 AST JSON，确保字段、类型与取值一致。
 */
import { promises as fs } from 'node:fs';
import process from 'node:process';

type JsonValue = null | boolean | number | string | JsonValue[] | { [key: string]: JsonValue };

async function main(): Promise<void> {
  const [leftPath, rightPath] = process.argv.slice(2);
  if (!leftPath || !rightPath) {
    console.error('用法: tools/ast_diff.ts <java-output.json> <ts-output.json>');
    process.exitCode = 2;
    return;
  }

  const leftRaw = await fs.readFile(leftPath, 'utf8');
  const rightRaw = await fs.readFile(rightPath, 'utf8');

  let leftJson: JsonValue;
  let rightJson: JsonValue;
  try {
    leftJson = JSON.parse(leftRaw) as JsonValue;
    rightJson = JSON.parse(rightRaw) as JsonValue;
  } catch (error) {
    console.error('解析 JSON 失败:', error);
    process.exitCode = 1;
    return;
  }

  const differences = diff(normalize(leftJson), normalize(rightJson));
  if (differences.length === 0) {
    console.log('✅ AST 一致，两侧输出完全匹配。');
    return;
  }

  console.error('❌ AST 不一致:');
  for (const item of differences.slice(0, 20)) {
    console.error(`  • ${item}`);
  }
  if (differences.length > 20) {
    console.error(`  … 还有 ${differences.length - 20} 处差异未列出`);
  }
  process.exitCode = 1;
}

function normalize(value: JsonValue): JsonValue {
  if (Array.isArray(value)) {
    return value.map(normalize);
  }
  if (value && typeof value === 'object') {
    const entries = Object.entries(value)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([k, v]) => [k, normalize(v)] as const);
    return Object.fromEntries(entries);
  }
  return value;
}

function diff(left: JsonValue, right: JsonValue, path = '$'): string[] {
  if (left === right) return [];

  if (Array.isArray(left) && Array.isArray(right)) {
    const len = Math.max(left.length, right.length);
    const result: string[] = [];
    for (let i = 0; i < len; i += 1) {
      if (i >= left.length) {
        result.push(`${path}[${i}] 仅存在于右侧: ${formatValue(right[i])}`);
        continue;
      }
      if (i >= right.length) {
        result.push(`${path}[${i}] 仅存在于左侧: ${formatValue(left[i])}`);
        continue;
      }
      result.push(...diff(left[i]!, right[i]!, `${path}[${i}]`));
    }
    return result;
  }

  if (isRecord(left) && isRecord(right)) {
    const result: string[] = [];
    const keys = new Set([...Object.keys(left), ...Object.keys(right)]);
    for (const key of Array.from(keys).sort()) {
      if (!(key in left)) {
        result.push(`${path}.${key} 缺失于左侧，右侧值为 ${formatValue(right[key])}`);
        continue;
      }
      if (!(key in right)) {
        result.push(`${path}.${key} 缺失于右侧，左侧值为 ${formatValue(left[key])}`);
        continue;
      }
      result.push(...diff(left[key]!, right[key]!, `${path}.${key}`));
    }
    return result;
  }

  return [`${path} 左右值不一致: ${formatValue(left)} vs ${formatValue(right)}`];
}

function isRecord(value: JsonValue): value is Record<string, JsonValue> {
  return !!value && typeof value === 'object' && !Array.isArray(value);
}

function formatValue(value: JsonValue): string {
  if (value === null) return 'null';
  if (typeof value === 'string') return JSON.stringify(value);
  if (typeof value === 'object') return Array.isArray(value) ? '[...]' : '{...}';
  return String(value);
}

void main().catch(error => {
  console.error('AST 比对失败:', error);
  process.exitCode = 1;
});
