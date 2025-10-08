#!/usr/bin/env node

/**
 * 性能统计工具函数
 * 提供百分位数计算，用于性能测试脚本
 */

/**
 * 计算数组的百分位数
 * @param values 数值数组
 * @param p 百分位 (0.0-1.0，例如 0.50 表示中位数)
 * @returns 百分位数值，空数组返回 0
 */
export function percentile(values: number[], p: number): number {
  if (values.length === 0) return 0;
  const sorted = [...values].sort((a, b) => a - b);
  const index = Math.ceil(sorted.length * p) - 1;
  return sorted[Math.max(0, index)]!;
}

/**
 * 计算中位数 (50th percentile)
 */
export function p50(values: number[]): number {
  if (values.length === 0) return 0;
  const sorted = [...values].sort((a, b) => a - b);
  const mid = Math.floor(sorted.length / 2);
  return sorted.length % 2 ? sorted[mid]! : (sorted[mid - 1]! + sorted[mid]!) / 2;
}

/**
 * 计算 95th percentile
 */
export const p95 = (values: number[]) => percentile(values, 0.95);

/**
 * 计算 99th percentile
 */
export const p99 = (values: number[]) => percentile(values, 0.99);

