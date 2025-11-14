import path from 'node:path';
import { fileURLToPath } from 'node:url';

export type JsonValue = null | boolean | number | string | JsonValue[] | { [key: string]: JsonValue };

export interface DiagnosticSpan {
  readonly file?: string;
  readonly startLine: number;
  readonly startCol: number;
  readonly endLine: number;
  readonly endCol: number;
}

export interface DiagnosticCommon {
  readonly severity: 'error' | 'warning' | 'info';
  readonly code: string;
  readonly message: string;
  readonly span?: DiagnosticSpan;
  readonly help?: string;
}

type ErrorCodeMeta = { code?: string } & Record<string, JsonValue>;

const SELF_PATH = fileURLToPath(import.meta.url);
const CURRENT_DIR = path.dirname(SELF_PATH);
export const ERROR_CODES_PATH = path.resolve(CURRENT_DIR, '../shared/error_codes.json');

export const COLORS = {
  green: '\u001b[32m',
  red: '\u001b[31m',
  yellow: '\u001b[33m',
  cyan: '\u001b[36m',
  magenta: '\u001b[35m',
  gray: '\u001b[90m',
  reset: '\u001b[0m',
};

export const SUCCESS_PREFIX = `${COLORS.green}✅${COLORS.reset}`;
export const FAILURE_PREFIX = `${COLORS.red}❌${COLORS.reset}`;
export const BULLET = `${COLORS.gray}•${COLORS.reset}`;
export const OUTPUT_LIMIT = 40;

export function colorText(value: string, color: string): string {
  return `${color}${value}${COLORS.reset}`;
}

export function reportDifferences(label: string, differences: string[], limit = OUTPUT_LIMIT): boolean {
  if (differences.length === 0) {
    console.log(`${SUCCESS_PREFIX} ${label}一致，两侧输出完全匹配。`);
    return false;
  }

  console.error(`${FAILURE_PREFIX} ${label}不一致:`);
  for (const item of differences.slice(0, limit)) {
    console.error(`  ${BULLET} ${item}`);
  }
  if (differences.length > limit) {
    console.error(`  ${COLORS.yellow}… 还有 ${differences.length - limit} 条差异未列出${COLORS.reset}`);
  }
  return true;
}

export function buildErrorCodeMap(raw: JsonValue): Map<string, string> {
  const map = new Map<string, string>();
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) {
    return map;
  }

  for (const [name, value] of Object.entries(raw)) {
    if (!value || typeof value !== 'object' || Array.isArray(value)) continue;
    const meta = value as ErrorCodeMeta;
    const codeValue = typeof meta.code === 'string' ? meta.code : undefined;
    if (typeof codeValue !== 'string' || codeValue.trim().length === 0) continue;
    const canonical = codeValue.trim();
    map.set(name.toUpperCase(), canonical);
    map.set(canonical.toUpperCase(), canonical);
  }
  return map;
}

export function normalizeDiagnostics(payload: JsonValue, label: string, codes: Map<string, string>): DiagnosticCommon[] {
  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
    return [];
  }
  const diagnostics = (payload as Record<string, JsonValue>).diagnostics;
  if (!Array.isArray(diagnostics)) {
    return [];
  }

  return diagnostics.map((item, index) => normalizeDiagnostic(item, `${label}#${index}`, codes));
}

function normalizeDiagnostic(
  value: JsonValue,
  label: string,
  codes: Map<string, string>,
): DiagnosticCommon {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`${label} 诊断必须是对象`);
  }
  const record = value as Record<string, JsonValue>;

  const severity = normalizeSeverity(record, label);
  const code = normalizeCode(record, codes);
  const message = normalizeMessage(record);
  const help = normalizeHelp(record);
  const span = normalizeSpan(record);

  return {
    severity,
    code,
    message,
    ...(help !== undefined ? { help } : {}),
    ...(span ? { span } : {}),
  };
}

function normalizeSeverity(record: Record<string, JsonValue>, label: string): DiagnosticCommon['severity'] {
  const raw = record.severity ?? record.Severity ?? record.level ?? record.Level;
  if (typeof raw === 'string') {
    const normalized = raw.trim().toLowerCase();
    if (normalized === 'error' || normalized === 'warning' || normalized === 'info') {
      return normalized;
    }
  }
  throw new Error(`${label} 无法解析 severity: ${JSON.stringify(raw)}`);
}

function normalizeCode(record: Record<string, JsonValue>, codes: Map<string, string>): string {
  const raw = record.code ?? record.Code ?? record.errorCode ?? record.ErrorCode;
  return canonicalizeCode(raw ?? null, codes);
}

function canonicalizeCode(value: JsonValue, codes: Map<string, string>): string {
  if (value === null || value === undefined) {
    return 'UNKNOWN';
  }
  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (trimmed.length === 0) return 'UNKNOWN';
    const upper = trimmed.toUpperCase();
    if (codes.has(upper)) {
      return codes.get(upper)!;
    }
    return trimmed;
  }
  if (typeof value === 'number' && Number.isFinite(value)) {
    return String(value);
  }
  if (typeof value === 'object') {
    if (Array.isArray(value)) {
      return canonicalizeCode(value[0] ?? null, codes);
    }
    const nested = (value as Record<string, JsonValue>).code
      ?? (value as Record<string, JsonValue>).name
      ?? (value as Record<string, JsonValue>).value;
    return canonicalizeCode(nested ?? null, codes);
  }
  return 'UNKNOWN';
}

function normalizeMessage(record: Record<string, JsonValue>): string {
  const raw = record.message ?? record.Message ?? record.msg ?? record.Msg;
  if (typeof raw === 'string') {
    return raw;
  }
  if (raw === undefined || raw === null) {
    return '';
  }
  return JSON.stringify(raw);
}

function normalizeHelp(record: Record<string, JsonValue>): string | undefined {
  const raw = record.help ?? record.Help;
  if (typeof raw === 'string' && raw.trim().length > 0) {
    return raw;
  }
  return undefined;
}

function normalizeSpan(record: Record<string, JsonValue>): DiagnosticSpan | undefined {
  const candidates = [
    record.origin ?? record.Origin,
    record.span ?? record.Span,
    record.location ?? record.Location,
  ];

  for (const candidate of candidates) {
    const span = extractSpan(candidate ?? null);
    if (span) {
      return span;
    }
  }
  return undefined;
}

function extractSpan(value: JsonValue): DiagnosticSpan | undefined {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return undefined;
  }
  const record = value as Record<string, JsonValue>;
  const start = extractPosition(record.start ?? record.Start ?? null);
  const end = extractPosition(record.end ?? record.End ?? null);
  if (!start || !end) {
    return undefined;
  }
  const file = typeof record.file === 'string' && record.file.trim().length > 0
    ? record.file
    : undefined;
  return {
    ...(file ? { file } : {}),
    startLine: start.line,
    startCol: start.col,
    endLine: end.line,
    endCol: end.col,
  };
}

function extractPosition(value: JsonValue): { line: number; col: number } | undefined {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return undefined;
  }
  const record = value as Record<string, JsonValue>;
  const line = normalizeNumber(record.line ?? record.Line ?? record.row ?? record.Row ?? null);
  const col = normalizeNumber(record.col ?? record.Col ?? record.column ?? record.Column ?? null);
  if (line === undefined || col === undefined) {
    return undefined;
  }
  return { line, col };
}

function normalizeNumber(value: JsonValue): number | undefined {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === 'string' && value.trim().length > 0) {
    const parsed = Number(value);
    if (Number.isFinite(parsed)) {
      return parsed;
    }
  }
  return undefined;
}

export interface DiffOptions {
  readonly ignoreSpan?: boolean;
}

export function diffDiagnostics(
  left: DiagnosticCommon[],
  right: DiagnosticCommon[],
  options: DiffOptions = {},
): string[] {
  const result: string[] = [];
  const comparator = createDiagnosticComparator(options.ignoreSpan ?? false);
  const leftSorted = [...left].sort(comparator);
  const rightSorted = [...right].sort(comparator);
  const len = Math.max(leftSorted.length, rightSorted.length);

  for (let i = 0; i < len; i += 1) {
    const l = leftSorted[i];
    const r = rightSorted[i];
    if (!l) {
      result.push(formatMissing('左侧缺失', i, r!));
      continue;
    }
    if (!r) {
      result.push(formatMissing('右侧缺失', i, l));
      continue;
    }
    if (l.code !== r.code) {
      result.push(formatFieldDiff(i, 'code', l.code, r.code, '检查 ErrorCode 映射是否一致'));
    }
    if (l.severity !== r.severity) {
      result.push(formatFieldDiff(i, 'severity', l.severity, r.severity, '确认 severity 归一化逻辑'));
    }
    if (l.message !== r.message) {
      result.push(formatFieldDiff(
        i,
        'message',
        truncateMessage(l.message),
        truncateMessage(r.message),
        '检查诊断消息模板与占位符',
      ));
    }
    if ((l.help ?? '') !== (r.help ?? '')) {
      result.push(formatFieldDiff(
        i,
        'help',
        l.help ?? '无',
        r.help ?? '无',
        '同步 help 提示源',
      ));
    }
    if (!(options.ignoreSpan ?? false) && !spanEquals(l.span, r.span)) {
      result.push(formatFieldDiff(
        i,
        'span',
        formatSpan(l.span),
        formatSpan(r.span),
        '确认 span 计算逻辑与源映射',
      ));
    }
  }

  return result;
}

function createDiagnosticComparator(ignoreSpan: boolean) {
  return (a: DiagnosticCommon, b: DiagnosticCommon): number =>
    diagKey(a, ignoreSpan).localeCompare(diagKey(b, ignoreSpan), 'en');
}

function diagKey(diag: DiagnosticCommon, ignoreSpan: boolean): string {
  const spanKey = ignoreSpan || !diag.span
    ? ''
    : [
        diag.span.file ?? '',
        diag.span.startLine,
        diag.span.startCol,
        diag.span.endLine,
        diag.span.endCol,
      ].join(':');
  return [
    diag.code,
    diag.severity,
    diag.message,
    diag.help ?? '',
    spanKey,
  ].join('|');
}

function formatFieldDiff(index: number, field: string, leftValue: string, rightValue: string, suggestion?: string): string {
  const label = colorText(field, COLORS.cyan);
  const hint = suggestion ? `（建议: ${suggestion}）` : '';
  return `索引 ${index}: ${label} 不一致 -> 左 ${colorText(leftValue, COLORS.red)} | 右 ${colorText(rightValue, COLORS.green)} ${hint}`.trim();
}

function formatMissing(side: string, index: number, diag: DiagnosticCommon): string {
  const hint = side === '左侧缺失' || side === '右侧缺失'
    ? '确认两侧是否都生成相同数量诊断'
    : undefined;
  return `索引 ${index}: ${colorText(side, COLORS.magenta)} ${formatDiagnostic(diag)}${hint ? `（建议: ${hint}）` : ''}`;
}

function formatDiagnostic(diag: DiagnosticCommon): string {
  const severityColor = diag.severity === 'error'
    ? COLORS.red
    : diag.severity === 'warning'
      ? COLORS.yellow
      : COLORS.cyan;
  const parts = [
    `${severityColor}[${diag.severity}]${COLORS.reset} ${colorText(diag.code, COLORS.magenta)}`,
    truncateMessage(diag.message),
    diag.span ? formatSpan(diag.span) : '',
  ].filter(Boolean);
  return parts.join(' ');
}

export function formatSpan(span?: DiagnosticSpan): string {
  if (!span) return colorText('<无位置信息>', COLORS.gray);
  const file = span.file ?? '<unknown>';
  return colorText(`${file}:${span.startLine}:${span.startCol}-${span.endLine}:${span.endCol}`, COLORS.cyan);
}

function truncateMessage(message: string, limit = 160): string {
  const normalized = message.replace(/\s+/g, ' ').trim();
  if (normalized.length <= limit) return normalized;
  return `${normalized.slice(0, limit - 1)}…`;
}

function spanEquals(a?: DiagnosticSpan, b?: DiagnosticSpan): boolean {
  if (!a && !b) return true;
  if (!a || !b) return false;
  return (a.file ?? '') === (b.file ?? '')
    && a.startLine === b.startLine
    && a.startCol === b.startCol
    && a.endLine === b.endLine
    && a.endCol === b.endCol;
}
