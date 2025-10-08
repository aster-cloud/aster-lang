/**
 * 错误ID系统集中管理：
 * - 使用 ErrorCode 枚举统一编号，便于跨模块追踪
 * - createError 用于生成兼容 TypecheckDiagnostic 的结构化错误
 * - timestamp 字段记录发生时间，location 对应源码位置
 */
import type { Origin, TypecheckDiagnostic } from '../types.js';

/**
 * 错误编号约定：
 * - 1xxx：能力 (Capability) 相关
 * - 2xxx：类型系统相关
 * - 3xxx：效应 (Effect) 或推断相关
 * - 4xxx：语法分析阶段
 * - 5xxx：词法分析阶段
 * - 9xxx：内部保留错误
 */
export enum ErrorCode {
  CAPABILITY_VIOLATION = 'E1001',
  CAPABILITY_NOT_ALLOWED = 'E1002',
  CAPABILITY_MISSING = 'E1003',
  CAPABILITY_SUPERFLUOUS = 'E1004',
  TYPE_MISMATCH = 'E2001',
  TYPE_UNDEFINED_SYMBOL = 'E2002',
  TYPE_DUPLICATE_DEFINITION = 'E2003',
  TYPE_NON_EXHAUSTIVE = 'E2004',
  EFFECT_MISSING = 'E3001',
  EFFECT_SUPERFLUOUS = 'E3002',
  EFFECT_INFERENCE_MISSING = 'E3003',
  EFFECT_INFERENCE_REDUNDANT = 'E3004',
  PARSER_UNEXPECTED_TOKEN = 'E4001',
  PARSER_EXPECTED_TOKEN = 'E4002',
  LEXER_UNTERMINATED_STRING = 'E5001',
  LEXER_INVALID_INDENTATION = 'E5002',
  INTERNAL_UNEXPECTED_STATE = 'E9001',
}

export type ErrorSeverity = TypecheckDiagnostic['severity'];

export interface StructuredError extends TypecheckDiagnostic {
  readonly code: ErrorCode;
  readonly timestamp: string;
  location?: Origin;
}

export interface CreateErrorOptions {
  readonly code: ErrorCode;
  readonly message: string;
  readonly severity?: ErrorSeverity;
  readonly data?: unknown;
  readonly location?: Origin;
}

/**
 * createError 示例：
 *
 * ```ts
 * const error = createError({
 *   code: ErrorCode.CAPABILITY_MISSING,
 *   message: '函数 foo 缺少必需的 @io 能力',
 *   location: someOrigin,
 *   data: { func: 'foo', capability: 'io' },
 * });
 * diags.push(error);
 * ```
 */
export function createError(options: CreateErrorOptions): StructuredError {
  const {
    code,
    message,
    severity = 'error',
    data,
    location,
  } = options;
  const timestamp = new Date().toISOString();
  const error = {
    severity,
    message,
    code,
    timestamp,
  } as StructuredError;

  if (data !== undefined) {
    error.data = data;
  }

  if (location) {
    error.location = location;
  }

  return error;
}
