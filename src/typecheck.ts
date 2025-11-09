import { performance } from 'node:perf_hooks';
import type { Core, Origin, Span, TypecheckDiagnostic } from './types.js';
import {
  type CapabilityManifest,
  type CapabilityContext,
  isAllowed,
  normalizeManifest,
  parseLegacyCapability,
} from './capabilities.js';
import { CapabilityKind, inferCapabilityFromName } from './config/semantic.js';
import { inferEffects } from './effect_inference.js';
import { DefaultCoreVisitor, createVisitorContext } from './visitor.js';
import { DefaultTypeVisitor } from './ast_visitor.js';
import { getIOPrefixes, getCPUPrefixes } from './config/effect_config.js';
import { ENFORCE_CAPABILITIES } from './config/runtime.js';
import { createLogger, logPerformance } from './utils/logger.js';
import { SymbolTable } from './typecheck/symbol_table.js';
import type { SymbolKind } from './typecheck/symbol_table.js';
import { DiagnosticBuilder } from './typecheck/diagnostics.js';
import { TypeSystem } from './typecheck/type_system.js';
import { ErrorCode } from './error_codes.js';

// 从配置获取效果推断前缀（模块级，避免重复调用）
const IO_PREFIXES = getIOPrefixes();
const CPU_PREFIXES = getCPUPrefixes();

// Re-export TypecheckDiagnostic for external use
export type { TypecheckDiagnostic };

/**
 * 解析导入别名到真实模块前缀
 * @param name 原始名称（如 "H.get"）
 * @param imports 别名映射（如 {H: "Http"}）
 * @returns 解析后的名称（如 "Http.get"）
 */
export function resolveAlias(name: string, imports: Map<string, string>): string {
  if (!name.includes('.')) return name;
  const [prefix, ...rest] = name.split('.');
  const resolved = imports.get(prefix!);
  return resolved ? `${resolved}.${rest.join('.')}` : name;
}

const typecheckLogger = createLogger('typecheck');
const UNKNOWN_TYPE: Core.Type = TypeSystem.unknown();
const UNIT_TYPE: Core.Type = { kind: 'TypeName', name: 'Unit' };
const IO_EFFECT_TYPE: Core.Type = { kind: 'TypeName', name: 'IO' };
const CPU_EFFECT_TYPE: Core.Type = { kind: 'TypeName', name: 'CPU' };
const PURE_EFFECT_TYPE: Core.Type = { kind: 'TypeName', name: 'PURE' };

function isUnknown(type: Core.Type | undefined | null): boolean {
  if (!type) return true;
  return TypeSystem.equals(type, UNKNOWN_TYPE);
}

function unknownType(): Core.Type {
  return UNKNOWN_TYPE;
}

function normalizeType(type: Core.Type | undefined | null): Core.Type {
  return type ?? UNKNOWN_TYPE;
}

function formatType(type: Core.Type | undefined | null): string {
  return TypeSystem.format(normalizeType(type));
}

function typesEqual(a: Core.Type | undefined | null, b: Core.Type | undefined | null, strict = false): boolean {
  return TypeSystem.equals(normalizeType(a), normalizeType(b), strict);
}

function originToSpan(origin: Origin | undefined): Span | undefined {
  if (!origin) return undefined;
  return { start: origin.start, end: origin.end };
}

interface FunctionSignature {
  params: Core.Type[];
  ret: Core.Type;
}

interface ModuleContext {
  datas: Map<string, Core.Data>;
  enums: Map<string, Core.Enum>;
  imports: Map<string, string>; // alias -> module name (or module name -> module name if no alias)
  funcSignatures: Map<string, FunctionSignature>;
}

export function typecheckModule(m: Core.Module): TypecheckDiagnostic[] {
  const moduleName = m.name ?? '<anonymous>';
  const startTime = performance.now();
  typecheckLogger.info('开始类型检查模块', { moduleName });
  try {
    const diagnostics = new DiagnosticBuilder();
    const ctx: ModuleContext = { datas: new Map(), enums: new Map(), imports: new Map(), funcSignatures: new Map() };
    for (const d of m.decls) {
      if (d.kind === 'Func') {
        const params = d.params.map(param => normalizeType(param.type as Core.Type));
        const ret = normalizeType(d.ret as Core.Type);
        ctx.funcSignatures.set(d.name, { params, ret });
      }
    }

    for (const d of m.decls) {
      if (d.kind === 'Import') {
        const alias = d.asName ?? d.name;
        if (ctx.imports.has(alias)) {
          diagnostics.warning(ErrorCode.DUPLICATE_IMPORT_ALIAS, d.span, { alias });
        } else {
          ctx.imports.set(alias, d.name);
        }
      }
    }
    for (const d of m.decls) {
      if (d.kind === 'Data') ctx.datas.set(d.name, d);
      if (d.kind === 'Enum') ctx.enums.set(d.name, d);
    }
    for (const d of m.decls) {
      if (d.kind === 'Func') {
        typecheckFunc(ctx, d, diagnostics);
      }
    }
    const effectDiags = inferEffects(m, ctx.imports);
    const result = [...diagnostics.getDiagnostics(), ...effectDiags];
    const duration = performance.now() - startTime;
    const baseMeta = { moduleName, errorCount: result.length };
    logPerformance({
      component: 'typecheck',
      operation: '模块类型检查',
      duration,
      metadata: baseMeta,
    });
    typecheckLogger.info('类型检查完成', {
      ...baseMeta,
      duration_ms: duration,
    });
    return result;
  } catch (error) {
    typecheckLogger.error('类型检查失败', error as Error, { moduleName });
    throw error;
  }
}

export function typecheckModuleWithCapabilities(
  m: Core.Module,
  manifest: CapabilityManifest | null
): TypecheckDiagnostic[] {
  const normalizedManifest = manifest ? normalizeManifest(manifest) : null;
  const baseDiagnostics = typecheckModule(m);
  if (!normalizedManifest) return baseDiagnostics;

  const builder = new DiagnosticBuilder();
  const capCtx: CapabilityContext = { moduleName: m.name ?? '' };

  for (const d of m.decls) {
    if (d.kind !== 'Func') continue;

    const declaredCaps = new Set<CapabilityKind>();
    for (const eff of d.effects) {
      const effName = String(eff).toLowerCase();
      if (effName === 'io') {
        for (const cap of parseLegacyCapability('io')) declaredCaps.add(cap);
      } else if (effName === 'cpu') {
        declaredCaps.add(CapabilityKind.CPU);
      }
    }

    const meta = d as unknown as { effectCaps: readonly CapabilityKind[]; effectCapsExplicit: boolean };
    if (meta.effectCapsExplicit) {
      for (const cap of meta.effectCaps) declaredCaps.add(cap);
    }

    const usedCaps = collectCapabilities(d.body);
    for (const cap of usedCaps.keys()) declaredCaps.add(cap);

    for (const cap of declaredCaps) {
      if (!isAllowed(cap, d.name, capCtx, normalizedManifest)) {
        builder.error(ErrorCode.CAPABILITY_NOT_ALLOWED, d.span, {
          func: d.name,
          module: m.name ?? '',
          cap,
        });
      }
    }
  }
  return [...baseDiagnostics, ...builder.getDiagnostics()];
}

function typecheckFunc(ctx: ModuleContext, f: Core.Func, diagnostics: DiagnosticBuilder): void {
  const symbols = new SymbolTable();
  symbols.enterScope('function');
  const functionContext: TypecheckWalkerContext = { module: ctx, symbols, diagnostics };

  for (const param of f.params) {
    const paramType = normalizeType(param.type as Core.Type);
    defineSymbol(functionContext, param.name, paramType, 'param');
  }

  const declaredReturn = normalizeType(f.ret as Core.Type);
  const bodyReturn = f.body ? typecheckBlock(ctx, symbols, f.body, diagnostics) : unknownType();

  if (!typesEqual(bodyReturn, declaredReturn)) {
    diagnostics.error(ErrorCode.RETURN_TYPE_MISMATCH, f.body?.span ?? f.ret?.span ?? f.span, {
      expected: formatType(declaredReturn),
      actual: formatType(bodyReturn),
    });
  }

  // Effect enforcement (minimal lattice: ∅ ⊑ CPU ⊑ IO[*])
  checkEffects(ctx, f, diagnostics);

  // Async discipline: every started task should be waited somewhere in the body
  checkAsyncDiscipline(f, diagnostics);

  // Capability-parameterized IO enforcement (feature-gated)
  checkCapabilities(f, diagnostics);

  // Generics: infer type variables from return position (preview)
  const typeParams = (f as unknown as { typeParams?: readonly string[] }).typeParams ?? [];
  if (typeParams.length > 0) {
    const bindings = new Map<string, Core.Type>();
    unifyTypeParameters(declaredReturn, bodyReturn, bindings, diagnostics, f.ret?.span ?? f.span);
  }

  // Generic type parameters: undefined usage (error) and unused (warn)
  checkGenericTypeParameters(ctx, f, diagnostics);

  symbols.exitScope();
}

function unifyTypeParameters(
  expected: Core.Type,
  actual: Core.Type,
  bindings: Map<string, Core.Type>,
  diagnostics: DiagnosticBuilder,
  span: Span | undefined
): void {
  const inferred = new Map<string, Core.Type>();
  if (!TypeSystem.unify(expected, actual, inferred)) return;

  for (const [name, type] of inferred) {
    const previous = bindings.get(name);
    if (previous) {
      if (!typesEqual(previous, type, true)) {
        diagnostics.error(ErrorCode.TYPEVAR_INCONSISTENT, span, {
          name,
          previous: formatType(previous),
          actual: formatType(type),
        });
      }
    } else {
      bindings.set(name, type);
    }
  }
}

/**
 * 类型参数收集器：收集类型表达式中所有使用的 TypeVar
 */
class TypeParamCollector extends DefaultTypeVisitor<Set<string>> {
  override visitTypeVar(v: Core.TypeVar, ctx: Set<string>): void {
    ctx.add(v.name);
  }
}

/**
 * 未知类型名称查找器：查找可能是类型变量但未声明的 TypeName
 */
class UnknownTypeFinder extends DefaultTypeVisitor<{ unknowns: Set<string>; ctx: ModuleContext }> {
  private static readonly KNOWN_SCALARS = new Set(['Text', 'Int', 'Bool', 'Float']);
  private static readonly isMaybeTypeVarLike = (name: string): boolean => /^[A-Z][A-Za-z0-9_]*$/.test(name);

  override visitTypeName(n: Core.TypeName, context: { unknowns: Set<string>; ctx: ModuleContext }): void {
    const nm = n.name;
    if (
      !UnknownTypeFinder.KNOWN_SCALARS.has(nm) &&
      !context.ctx.datas.has(nm) &&
      !context.ctx.enums.has(nm) &&
      UnknownTypeFinder.isMaybeTypeVarLike(nm)
    ) {
      context.unknowns.add(nm);
    }
  }
}

/**
 * 检查泛型类型参数的使用（Generic type parameters validation）
 * 包括：未声明的类型变量、未使用的类型参数、类型变量命名规范
 */
function checkGenericTypeParameters(
  ctx: ModuleContext,
  f: Core.Func,
  diagnostics: DiagnosticBuilder
): void {
  const typeParams = (f as unknown as { typeParams?: readonly string[] }).typeParams ?? [];

  // 收集已使用的类型变量
  const declared = new Set<string>(typeParams);
  const used = new Set<string>();
  const collector = new TypeParamCollector();
  for (const p of f.params) collector.visitType(p.type, used);
  collector.visitType(f.ret, used);

  // 检查未声明的类型变量使用
  for (const u of used) {
    if (!declared.has(u)) {
      diagnostics.error(ErrorCode.TYPE_VAR_UNDECLARED, f.span, {
        name: u,
        func: f.name,
      });
    }
  }

  // 检查未使用的类型参数
  for (const tv of declared) {
    if (!used.has(tv)) {
      diagnostics.warning(ErrorCode.TYPE_PARAM_UNUSED, f.span, {
        name: tv,
        func: f.name,
      });
    }
  }

  // 查找疑似类型变量的未知类型名
  const unknowns = new Set<string>();
  const finder = new UnknownTypeFinder();
  for (const p of f.params) finder.visitType(p.type, { unknowns, ctx });
  finder.visitType(f.ret, { unknowns, ctx });

  // 报告未声明的疑似类型变量
  for (const nm of unknowns) {
   if (!declared.has(nm)) {
      diagnostics.error(ErrorCode.TYPEVAR_LIKE_UNDECLARED, f.span, {
        name: nm,
        func: f.name,
      });
    }
  }
}

/**
 * 检查异步任务纪律（Async discipline）
 *
 * 完整的异步纪律检查，包括：
 * 1. Start 未 Wait → error
 * 2. Wait 引用不存在的 Start → error
 * 3. 重复 Start → error（控制流敏感）
 * 4. 重复 Wait → warning
 * 5. Wait-before-Start / 作用域违规 → error
 */
function checkAsyncDiscipline(
  f: Core.Func,
  diagnostics: DiagnosticBuilder
): void {
  const asyncInfo = collectAsync(f.body);
  const schedule = scheduleAsync(f.body);
  const isPlaceholderSpan = (span: Span): boolean =>
    span.start.line === 0 &&
    span.start.col === 0 &&
    span.end.line === 0 &&
    span.end.col === 0;
  const pickSpan = (spans: Span[] | undefined): Span | undefined =>
    spans?.find(s => !isPlaceholderSpan(s));
  // 1. 检查 Start 未 Wait
  const notWaited = [...asyncInfo.starts.keys()].filter(n => !asyncInfo.waits.has(n));
  for (const name of notWaited) {
    const spans = asyncInfo.starts.get(name);
    const firstSpan = pickSpan(spans);

    diagnostics.error(ErrorCode.ASYNC_START_NOT_WAITED, firstSpan, { task: name });
  }

  // 2. 检查 Wait 引用不存在的 Start
  const notStarted = [...asyncInfo.waits.keys()].filter(n => !asyncInfo.starts.has(n));
  for (const name of notStarted) {
    const spans = asyncInfo.waits.get(name);
    const firstSpan = pickSpan(spans);

    diagnostics.error(ErrorCode.ASYNC_WAIT_NOT_STARTED, firstSpan, { task: name });
  }

  // 3. 检查重复 Wait
  for (const [name, spans] of asyncInfo.waits) {
    if (spans.length > 1) {
      for (let i = 1; i < spans.length; i++) {
        const span = spans[i]!;
        const targetSpan = isPlaceholderSpan(span) ? undefined : span;
        diagnostics.warning(ErrorCode.ASYNC_DUPLICATE_WAIT, targetSpan, {
          task: name,
          count: spans.length,
        });
      }
    }
  }

  // 4. 控制流敏感的调度验证（Wait-before-Start / 条件分支重复 Start）
  validateSchedule(schedule, asyncInfo, diagnostics);
}

/**
 * 检查函数的能力声明（Capability-parameterized IO enforcement）
 * 仅在 ENFORCE_CAPABILITIES 特性开启时执行
 */
function checkCapabilities(
  f: Core.Func,
  diagnostics: DiagnosticBuilder
): void {
  if (!ENFORCE_CAPABILITIES) return;

  const meta = f as unknown as { effectCaps: readonly CapabilityKind[]; effectCapsExplicit: boolean };
  if (!meta.effectCapsExplicit || meta.effectCaps.length === 0) return;

  const declared = new Set<CapabilityKind>(meta.effectCaps);
  const used = collectCapabilities(f.body);

  for (const [cap, callSites] of used) {
   if (!declared.has(cap)) {
      const declaredList = [...declared].join(', ');
      diagnostics.error(ErrorCode.EFF_CAP_MISSING, f.span, {
        func: f.name,
        cap,
        declared: declaredList,
        calls: callSites,
      });
    }
  }

  for (const cap of declared) {
    if (!used.has(cap)) {
      diagnostics.info(ErrorCode.EFF_CAP_SUPERFLUOUS, f.span, { func: f.name, cap });
    }
  }
}

/**
 * 检查函数的效应声明（Effect enforcement）
 * 使用最小格理论：∅ ⊑ CPU ⊑ IO[*]
 */
function checkEffects(
  ctx: ModuleContext,
  f: Core.Func,
  diagnostics: DiagnosticBuilder
): void {
  const effs = collectEffects(ctx, f.body);
  const hasIO = f.effects.some(e => String(e).toLowerCase() === 'io');
  const hasCPU = f.effects.some(e => String(e).toLowerCase() === 'cpu');

  // Missing IO is an error when IO-like calls are detected
  if (effs.has('io') && !hasIO)
    diagnostics.error(ErrorCode.EFF_MISSING_IO, f.span, { func: f.name });

  // Under lattice, IO subsumes CPU: if CPU work is detected, it is satisfied by either @cpu or @io
  if (effs.has('cpu') && !(hasCPU || hasIO))
    diagnostics.error(ErrorCode.EFF_MISSING_CPU, f.span, { func: f.name });

  // Superfluous annotations:
  // - If @io is declared but only CPU-like work is found, emit an info (IO subsumes CPU; not harmful).
  if (!effs.has('io') && hasIO && effs.has('cpu'))
    diagnostics.info(ErrorCode.EFF_SUPERFLUOUS_IO_CPU_ONLY, f.span, { func: f.name });

  if (!effs.has('cpu') && hasCPU)
    diagnostics.warning(ErrorCode.EFF_SUPERFLUOUS_CPU, f.span, { func: f.name });

  const workflows = collectWorkflows(f.body);
  if (workflows.length > 0) {
    if (!hasIO) {
      diagnostics.error(ErrorCode.WORKFLOW_MISSING_IO_EFFECT, f.span, { func: f.name });
    }
    const funcMeta = f as unknown as { effectCaps?: readonly CapabilityKind[] };
    const declaredCaps = new Set<CapabilityKind>(funcMeta.effectCaps ?? []);
    for (const workflow of workflows) {
      for (const step of workflow.steps) {
        const stepSpan = originToSpan(step.origin);
        const bodyCaps = collectCapabilities(step.body);
        reportWorkflowCapabilityViolation(diagnostics, declaredCaps, bodyCaps, f.name, step.name, stepSpan);
        if (step.compensate) {
          const compensateCaps = collectCapabilities(step.compensate);
          const compensateSpan = originToSpan(step.compensate.origin ?? step.origin);
          reportWorkflowCapabilityViolation(
            diagnostics,
            declaredCaps,
            compensateCaps,
            f.name,
            step.name,
            compensateSpan
          );
          const bodyCapSet = new Set(bodyCaps.keys());
          for (const cap of compensateCaps.keys()) {
            if (!bodyCapSet.has(cap)) {
              diagnostics.error(ErrorCode.COMPENSATE_NEW_CAPABILITY, compensateSpan, {
                func: f.name,
                step: step.name,
                capability: cap,
              });
            }
          }
        }
      }
    }
  }
}

function collectEffects(ctx: ModuleContext, b: Core.Block): Set<'io' | 'cpu'> {
  const effs = new Set<'io' | 'cpu'>();
  class EffectsVisitor extends DefaultCoreVisitor {
    override visitStatement(
      statement: Core.Statement,
      context: import('./visitor.js').VisitorContext
    ): void {
      if (statement.kind === 'workflow') {
        // workflow 自身由运行时驱动，默认需要 IO 效果
        effs.add('io');
        for (const step of statement.steps) {
          this.visitBlock(step.body, context);
          if (step.compensate) this.visitBlock(step.compensate, context);
        }
        return;
      }
      super.visitStatement(statement, context);
    }

    override visitExpression(e: Core.Expression, context: import('./visitor.js').VisitorContext): void {
      if (e.kind === 'Call' && e.target.kind === 'Name') {
        const rawName = e.target.name;
        const resolvedName = resolveAlias(rawName, ctx.imports);
        if (IO_PREFIXES.some((p: string) => resolvedName.startsWith(p))) effs.add('io');
        if (CPU_PREFIXES.some((p: string) => resolvedName.startsWith(p))) effs.add('cpu');
      }
      super.visitExpression(e, context);
    }
  }
  new EffectsVisitor().visitBlock(b, createVisitorContext());
  return effs;
}


function collectCapabilities(b: Core.Block): Map<CapabilityKind, string[]> {
  const caps = new Map<CapabilityKind, string[]>();
  class CapVisitor extends DefaultCoreVisitor {
    override visitExpression(e: Core.Expression, context: import('./visitor.js').VisitorContext): void {
      if (e.kind === 'Call' && e.target.kind === 'Name') {
        const n = e.target.name;
        const inferred = inferCapabilityFromName(n);
        if (inferred) {
          const items = caps.get(inferred);
          if (items) {
            items.push(n);
          } else {
            caps.set(inferred, [n]);
          }
        }
      }
      super.visitExpression(e, context);
    }
  }
  new CapVisitor().visitBlock(b, createVisitorContext());
  return caps;
}

function collectWorkflows(block: Core.Block): Core.Workflow[] {
  const workflows: Core.Workflow[] = [];
  class WorkflowCollector extends DefaultCoreVisitor {
    override visitStatement(statement: Core.Statement, context: import('./visitor.js').VisitorContext): void {
      if (statement.kind === 'workflow') workflows.push(statement);
      super.visitStatement(statement, context);
    }
  }
  new WorkflowCollector().visitBlock(block, createVisitorContext());
  return workflows;
}

function reportWorkflowCapabilityViolation(
  diagnostics: DiagnosticBuilder,
  declaredCaps: Set<CapabilityKind>,
  observedCaps: Map<CapabilityKind, string[]>,
  funcName: string,
  stepName: string,
  span: Span | undefined
): void {
  for (const cap of observedCaps.keys()) {
    if (!declaredCaps.has(cap)) {
      diagnostics.error(ErrorCode.WORKFLOW_UNDECLARED_CAPABILITY, span, {
        func: funcName,
        step: stepName,
        capability: cap,
      });
    }
  }
}

interface TypecheckWalkerContext {
  module: ModuleContext;
  symbols: SymbolTable;
  diagnostics: DiagnosticBuilder;
}

function defineSymbol(
  context: TypecheckWalkerContext,
  name: string,
  type: Core.Type,
  kind: SymbolKind,
  span?: Span
): void {
  const existing = context.symbols.lookupInCurrentScope(name);
  if (existing) {
    existing.type = type;
    if (!existing.span && span) existing.span = span;
    return;
  }
  const options: { span?: Span; mutable?: boolean } = {};
  if (span) options.span = span;
  options.mutable = kind !== 'param';
  context.symbols.define(name, type, kind, options);
}

function assignSymbol(context: TypecheckWalkerContext, name: string, type: Core.Type, span?: Span): void {
  const symbol = context.symbols.lookup(name);
  if (!symbol) {
    context.diagnostics.undefinedVariable(name, span);
    return;
  }
  if (!typesEqual(symbol.type, type) && !TypeSystem.isSubtype(type, symbol.type)) {
    context.diagnostics.error(ErrorCode.TYPE_MISMATCH_ASSIGN, span, {
      name,
      expected: formatType(symbol.type),
      actual: formatType(type),
    });
  }
  symbol.type = type;
}

class TypecheckVisitor extends DefaultCoreVisitor<TypecheckWalkerContext> {
  public result: Core.Type = unknownType();

  override visitBlock(block: Core.Block, context: TypecheckWalkerContext): void {
    let last: Core.Type = unknownType();
    for (const stmt of block.statements) {
      this.visitStatement(stmt, context);
      last = this.result;
    }
    this.result = last;
  }

  override visitStatement(statement: Core.Statement, context: TypecheckWalkerContext): void {
    const { module, symbols, diagnostics } = context;
    switch (statement.kind) {
      case 'Let': {
        const valueType = typeOfExpr(module, symbols, statement.expr, diagnostics);
        defineSymbol(context, statement.name, valueType, 'var', statement.span);
        this.result = valueType;
        return;
      }
      case 'Set': {
        const valueType = typeOfExpr(module, symbols, statement.expr, diagnostics);
        assignSymbol(context, statement.name, valueType, statement.span);
        this.result = valueType;
        return;
      }
      case 'Return': {
        this.result = typeOfExpr(module, symbols, statement.expr, diagnostics);
        return;
      }
      case 'If': {
        void typeOfExpr(module, symbols, statement.cond, diagnostics);
        const thenType = typecheckBlock(module, symbols, statement.thenBlock, diagnostics);
        const elseType = statement.elseBlock
          ? typecheckBlock(module, symbols, statement.elseBlock, diagnostics)
          : unknownType();
        if (isUnknown(thenType)) {
          this.result = elseType;
        } else if (isUnknown(elseType)) {
          this.result = thenType;
        } else {
          if (!typesEqual(thenType, elseType)) {
            diagnostics.error(ErrorCode.IF_BRANCH_MISMATCH, statement.span, {
              thenType: formatType(thenType),
              elseType: formatType(elseType),
            });
          }
          this.result = thenType;
        }
        return;
      }
      case 'Match': {
        const scrutineeType = typeOfExpr(module, symbols, statement.expr, diagnostics);
        let aggregated: Core.Type | null = null;
        let hasNullCase = false;
        let hasNonNullCase = false;
        const enumDecl =
          !isUnknown(scrutineeType) && scrutineeType.kind === 'TypeName'
            ? module.enums.get(scrutineeType.name)
            : undefined;
        const seenEnum = new Set<string>();
        let hasWildcard = false;
        for (const caseClause of statement.cases) {
          const caseType = typecheckCase(module, symbols, caseClause, scrutineeType, diagnostics);
          if (!aggregated) {
            aggregated = caseType;
          } else if (!typesEqual(aggregated, caseType)) {
            diagnostics.error(ErrorCode.MATCH_BRANCH_MISMATCH, caseClause.body.span ?? statement.span, {
              expected: formatType(aggregated),
              actual: formatType(caseType),
            });
          }
          if (caseClause.pattern.kind === 'PatNull') hasNullCase = true;
          else hasNonNullCase = true;
          if (enumDecl) {
            if (caseClause.pattern.kind === 'PatName') {
              if (enumDecl.variants.includes(caseClause.pattern.name)) {
                if (seenEnum.has(caseClause.pattern.name)) {
                  diagnostics.warning(
                    ErrorCode.DUPLICATE_ENUM_CASE,
                    caseClause.pattern.span ?? statement.span,
                    { case: caseClause.pattern.name, type: enumDecl.name }
                  );
                }
                seenEnum.add(caseClause.pattern.name);
              } else {
                hasWildcard = true;
              }
            } else if (caseClause.pattern.kind === 'PatCtor') {
              if (enumDecl.variants.includes(caseClause.pattern.typeName)) {
                if (seenEnum.has(caseClause.pattern.typeName)) {
                  diagnostics.warning(
                    ErrorCode.DUPLICATE_ENUM_CASE,
                    caseClause.pattern.span ?? statement.span,
                    { case: caseClause.pattern.typeName, type: enumDecl.name }
                  );
                }
                seenEnum.add(caseClause.pattern.typeName);
              }
            } else {
              hasWildcard = true;
            }
          }
        }
        if (!isUnknown(scrutineeType) && scrutineeType.kind === 'Maybe') {
          if (!(hasNullCase && hasNonNullCase)) {
            const missing = hasNullCase ? 'non-null value' : hasNonNullCase ? 'null' : 'null and non-null';
            diagnostics.warning(ErrorCode.NON_EXHAUSTIVE_MAYBE, statement.span, {
              missing,
            });
          }
        } else if (enumDecl && !hasWildcard) {
          const missing = enumDecl.variants.filter(v => !seenEnum.has(v));
          if (missing.length > 0) {
            diagnostics.warning(ErrorCode.NON_EXHAUSTIVE_ENUM, statement.span, {
              type: enumDecl.name,
              missing: missing.join(', '),
            });
          }
        }
        this.result = aggregated ?? unknownType();
        return;
      }
      case 'Scope': {
        const nested: Core.Block = { kind: 'Block', statements: statement.statements, span: statement.span } as Core.Block;
        this.result = typecheckBlock(module, symbols, nested, diagnostics);
        return;
      }
      case 'Start': {
        void typeOfExpr(module, symbols, statement.expr, diagnostics);
        this.result = unknownType();
        return;
      }
      case 'Wait': {
        this.result = unknownType();
        return;
      }
      case 'workflow': {
        this.result = typecheckWorkflow(context, statement as Core.Workflow);
        return;
      }
    }
  }
}

function typecheckWorkflow(context: TypecheckWalkerContext, workflow: Core.Workflow): Core.Type {
  let resultType: Core.Type = unknownType();
  const stepEffects = new Map<Core.Step, Set<'io' | 'cpu'>>();
  for (const step of workflow.steps) {
    resultType = typecheckStep(context, step, stepEffects);
  }
  validateWorkflowMetadata(workflow, context.diagnostics);
  const effectType = workflowEffectType(context, workflow, stepEffects);
  return {
    kind: 'TypeApp',
    base: 'Workflow',
    args: [normalizeType(resultType), effectType],
  };
}

function typecheckStep(
  context: TypecheckWalkerContext,
  step: Core.Step,
  effectCache?: Map<Core.Step, Set<'io' | 'cpu'>>
): Core.Type {
  const bodyType = typecheckBlock(context.module, context.symbols, step.body, context.diagnostics);
  const effects = collectEffects(context.module, step.body);
  if (effectCache) {
    effectCache.set(step, effects);
  }
  if (step.compensate) {
    const compensateType = typecheckBlock(context.module, context.symbols, step.compensate, context.diagnostics);
    validateCompensateBlock(context, step, bodyType, compensateType);
  } else if (stepHasSideEffects(step, effects)) {
    context.diagnostics.warning(ErrorCode.WORKFLOW_COMPENSATE_MISSING, originToSpan(step.origin), {
      step: step.name,
    });
  }
  return bodyType;
}

function validateCompensateBlock(
  context: TypecheckWalkerContext,
  step: Core.Step,
  bodyType: Core.Type,
  compensateType: Core.Type
): void {
  const expected = normalizeType(bodyType);
  if (expected.kind !== 'Result') return;

  const expectedErr = normalizeType(expected.err as Core.Type);
  const targetSpan = originToSpan(step.compensate?.origin ?? step.origin);

  if (compensateType.kind !== 'Result') {
    context.diagnostics.error(ErrorCode.WORKFLOW_COMPENSATE_TYPE, targetSpan, {
      step: step.name,
      expectedErr: formatType(expectedErr),
      actual: formatType(compensateType),
    });
    return;
  }

  const okMatchesUnit =
    TypeSystem.equals(compensateType.ok as Core.Type, UNIT_TYPE) || isUnknown(compensateType.ok as Core.Type);
  const errMatches =
    TypeSystem.equals(normalizeType(compensateType.err as Core.Type), expectedErr) ||
    TypeSystem.isSubtype(normalizeType(compensateType.err as Core.Type), expectedErr);

  if (!okMatchesUnit || !errMatches) {
    context.diagnostics.error(ErrorCode.WORKFLOW_COMPENSATE_TYPE, targetSpan, {
      step: step.name,
      expectedErr: formatType(expectedErr),
      actual: formatType(compensateType),
    });
  }
}

function workflowEffectType(
  context: TypecheckWalkerContext,
  workflow: Core.Workflow,
  cachedEffects?: Map<Core.Step, Set<'io' | 'cpu'>>
): Core.Type {
  let hasIOCap = workflow.effectCaps.some(cap => cap !== CapabilityKind.CPU);
  let hasCpuCap = workflow.effectCaps.some(cap => cap === CapabilityKind.CPU);

  if (!hasIOCap) {
    for (const step of workflow.steps) {
      const effects = cachedEffects?.get(step) ?? collectEffects(context.module, step.body);
      if (effects.has('io')) {
        hasIOCap = true;
        break;
      }
      if (effects.has('cpu')) {
        hasCpuCap = true;
      }
    }
  }

  if (hasIOCap) return IO_EFFECT_TYPE;
  if (hasCpuCap) return CPU_EFFECT_TYPE;
  return PURE_EFFECT_TYPE;
}

function validateWorkflowMetadata(workflow: Core.Workflow, diagnostics: DiagnosticBuilder): void {
  if (workflow.retry && workflow.retry.maxAttempts <= 0) {
    diagnostics.error(ErrorCode.WORKFLOW_RETRY_INVALID, originToSpan(workflow.origin), {
      maxAttempts: workflow.retry.maxAttempts,
    });
  }
  if (workflow.timeout && workflow.timeout.milliseconds <= 0) {
    diagnostics.error(ErrorCode.WORKFLOW_TIMEOUT_INVALID, originToSpan(workflow.origin), {
      milliseconds: workflow.timeout.milliseconds,
    });
  }
}

function stepHasSideEffects(step: Core.Step, effects: Set<'io' | 'cpu'>): boolean {
  if (effects.has('io')) return true;
  return step.effectCaps.some(cap => cap !== CapabilityKind.CPU);
}

function typecheckBlock(
  ctx: ModuleContext,
  symbols: SymbolTable,
  block: Core.Block,
  diagnostics: DiagnosticBuilder
): Core.Type {
  symbols.enterScope('block');
  try {
    const visitor = new TypecheckVisitor();
    visitor.visitBlock(block, { module: ctx, symbols, diagnostics });
    return visitor.result;
  } finally {
    symbols.exitScope();
  }
}

function typecheckCase(
  ctx: ModuleContext,
  symbols: SymbolTable,
  caseClause: Core.Case,
  scrutineeType: Core.Type,
  diagnostics: DiagnosticBuilder
): Core.Type {
  symbols.enterScope('block');
  try {
    bindPattern(ctx, symbols, caseClause.pattern, scrutineeType, diagnostics);
    if (caseClause.body.kind === 'Return') {
      return typeOfExpr(ctx, symbols, caseClause.body.expr, diagnostics);
    }
    return typecheckBlock(ctx, symbols, caseClause.body, diagnostics);
  } finally {
    symbols.exitScope();
  }
}

function bindPattern(
  ctx: ModuleContext,
  symbols: SymbolTable,
  pattern: Core.Pattern,
  scrutineeType: Core.Type,
  diagnostics: DiagnosticBuilder
): void {
  if (pattern.kind === 'PatName') {
    defineSymbol({ module: ctx, symbols, diagnostics }, pattern.name, scrutineeType, 'var', pattern.span);
    return;
  }
  if (pattern.kind === 'PatNull') return;
  if (pattern.kind === 'PatInt') {
    const isInt =
      !isUnknown(scrutineeType) &&
      scrutineeType.kind === 'TypeName' &&
      scrutineeType.name === 'Int';
    if (!isInt) {
      diagnostics.error(ErrorCode.INTEGER_PATTERN_TYPE, pattern.span, {
        scrutineeType: formatType(scrutineeType),
      });
    }
    return;
  }
  if (pattern.kind !== 'PatCtor') return;
  bindPatternCtor(ctx, symbols, pattern, scrutineeType, diagnostics);
}

function bindPatternCtor(
  ctx: ModuleContext,
  symbols: SymbolTable,
  pattern: Core.PatCtor,
  scrutineeType: Core.Type,
  diagnostics: DiagnosticBuilder
): void {
  // Special-case Result Ok/Err destructuring
  if (pattern.typeName === 'Ok' && !isUnknown(scrutineeType) && scrutineeType.kind === 'Result') {
    const inner = (scrutineeType as Core.Result).ok as Core.Type;
    const ctor = pattern as Core.PatCtor & { args?: readonly Core.Pattern[] };
    const child = ctor.args && ctor.args.length > 0
      ? (ctor.args[0] as Core.Pattern)
      : pattern.names && pattern.names[0]
        ? ({ kind: 'PatName', name: pattern.names[0] } as Core.PatName)
        : null;
    if (child) bindPattern(ctx, symbols, child, inner, diagnostics);
    return;
  }
  if (pattern.typeName === 'Err' && !isUnknown(scrutineeType) && scrutineeType.kind === 'Result') {
    const inner = (scrutineeType as Core.Result).err as Core.Type;
    const ctor = pattern as Core.PatCtor & { args?: readonly Core.Pattern[] };
    const child = ctor.args && ctor.args.length > 0
      ? (ctor.args[0] as Core.Pattern)
      : pattern.names && pattern.names[0]
        ? ({ kind: 'PatName', name: pattern.names[0] } as Core.PatName)
        : null;
    if (child) bindPattern(ctx, symbols, child, inner, diagnostics);
    return;
  }
  // Data constructor by schema
  const d = ctx.datas.get(pattern.typeName);
  if (!d) {
    // unknown ctor; bind any names as Unknown and recurse into args with Unknown
    if (pattern.names) {
      for (const name of pattern.names) {
        defineSymbol({ module: ctx, symbols, diagnostics }, name, unknownType(), 'var', pattern.span);
      }
    }
    const ctor = pattern as Core.PatCtor & { args?: readonly Core.Pattern[] };
    if (ctor.args) {
      for (const arg of ctor.args as readonly Core.Pattern[]) {
        bindPattern(ctx, symbols, arg, unknownType(), diagnostics);
      }
    }
    return;
  }
  const ar = d.fields.length;
  const ctor2 = pattern as Core.PatCtor & { args?: readonly Core.Pattern[] };
  const args: readonly Core.Pattern[] = ctor2.args ? (ctor2.args as readonly Core.Pattern[]) : [];
  for (let i = 0; i < ar; i++) {
    const ft = d.fields[i]!.type as Core.Type;
    const child =
      i < args.length
        ? args[i]!
        : pattern.names && i < pattern.names.length
          ? ({ kind: 'PatName', name: pattern.names[i]! } as Core.PatName)
          : null;
    if (!child) continue;
    bindPattern(ctx, symbols, child, ft, diagnostics);
  }
}

class TypeOfExprVisitor extends DefaultCoreVisitor<TypecheckWalkerContext> {
  public handled = false;
  public result: Core.Type = unknownType();

  override visitExpression(expression: Core.Expression, context: TypecheckWalkerContext): void {
    const { module, symbols, diagnostics } = context;
    switch (expression.kind) {
      case 'Name': {
        // Check if this is a field access (e.g., "applicant.creditScore")
        if (expression.name.includes('.')) {
          const parts = expression.name.split('.');
          const baseName = parts[0]!;
          const fieldPath = parts.slice(1);

          // Look up the base variable
          const baseSymbol = symbols.lookup(baseName);
          if (!baseSymbol) {
            diagnostics.undefinedVariable(baseName, expression.span);
            this.result = unknownType();
            this.handled = true;
            return;
          }

          // Resolve field access through the type chain
          let currentType = baseSymbol.type;
          for (const fieldName of fieldPath) {
            // Expand type aliases
            const expanded = TypeSystem.expand(currentType, symbols.getTypeAliases());

            // Check if current type is a custom data type
            if (expanded.kind === 'TypeName') {
              const dataDecl = module.datas.get(expanded.name);
              if (!dataDecl) {
                diagnostics.error(ErrorCode.UNKNOWN_FIELD, expression.span, {
                  field: fieldName,
                  type: formatType(currentType),
                });
                this.result = unknownType();
                this.handled = true;
                return;
              }

              // Find the field in the data declaration
              const field = dataDecl.fields.find(f => f.name === fieldName);
              if (!field) {
                diagnostics.error(ErrorCode.UNKNOWN_FIELD, expression.span, {
                  field: fieldName,
                  type: dataDecl.name,
                });
                this.result = unknownType();
                this.handled = true;
                return;
              }

              // Move to the field's type
              currentType = field.type as Core.Type;
            } else {
              // Not a data type, can't access fields
              diagnostics.error(ErrorCode.UNKNOWN_FIELD, expression.span, {
                field: fieldName,
                type: formatType(currentType),
              });
              this.result = unknownType();
              this.handled = true;
              return;
            }
          }

          this.result = currentType;
          this.handled = true;
          return;
        }

        // Regular variable lookup
        const symbol = symbols.lookup(expression.name);
        if (symbol) {
          this.result = symbol.type;
        } else {
          let matched: Core.Enum | undefined;
          for (const en of module.enums.values()) {
            if (en.variants.includes(expression.name)) {
              matched = en;
              break;
            }
          }
          if (matched) {
            this.result = { kind: 'TypeName', name: matched.name } as Core.TypeName;
          } else {
            diagnostics.undefinedVariable(expression.name, expression.span);
            this.result = unknownType();
          }
        }
        this.handled = true;
        return;
      }
      case 'Bool':
        this.result = { kind: 'TypeName', name: 'Bool' } as Core.TypeName;
        this.handled = true;
        return;
      case 'Int':
        this.result = { kind: 'TypeName', name: 'Int' } as Core.TypeName;
        this.handled = true;
        return;
      case 'Long':
        this.result = { kind: 'TypeName', name: 'Long' } as Core.TypeName;
        this.handled = true;
        return;
      case 'Double':
        this.result = { kind: 'TypeName', name: 'Double' } as Core.TypeName;
        this.handled = true;
        return;
      case 'String':
        this.result = { kind: 'TypeName', name: 'Text' } as Core.TypeName;
        this.handled = true;
        return;
      case 'Null':
        this.result = { kind: 'Maybe', type: unknownType() } as Core.Maybe;
        this.handled = true;
        return;
      case 'Ok': {
        const inner = typeOfExpr(module, symbols, expression.expr, diagnostics);
        this.result = {
          kind: 'Result',
          ok: isUnknown(inner) ? unknownType() : inner,
          err: unknownType(),
        } as Core.Result;
        this.handled = true;
        return;
      }
      case 'Err': {
        const inner = typeOfExpr(module, symbols, expression.expr, diagnostics);
        this.result = {
          kind: 'Result',
          ok: unknownType(),
          err: isUnknown(inner) ? unknownType() : inner,
        } as Core.Result;
        this.handled = true;
        return;
      }
      case 'Some': {
        const inner = typeOfExpr(module, symbols, expression.expr, diagnostics);
        this.result = {
          kind: 'Option',
          type: isUnknown(inner) ? unknownType() : inner,
        } as Core.Option;
        this.handled = true;
        return;
      }
      case 'None':
        this.result = { kind: 'Option', type: unknownType() } as Core.Option;
        this.handled = true;
        return;
      case 'Construct': {
        const dataDecl = module.datas.get(expression.typeName);
        if (!dataDecl) {
          this.result = unknownType();
          this.handled = true;
          return;
        }
        const provided = new Set<string>();
        for (const field of expression.fields) {
          provided.add(field.name);
          const schemaField = dataDecl.fields.find(item => item.name === field.name);
          if (!schemaField) {
            diagnostics.error(ErrorCode.UNKNOWN_FIELD, expression.span, {
              field: field.name,
              type: dataDecl.name,
            });
            continue;
          }
          const valueType = typeOfExpr(module, symbols, field.expr, diagnostics);
          if (!typesEqual(schemaField.type as Core.Type, valueType)) {
            diagnostics.error(ErrorCode.FIELD_TYPE_MISMATCH, (field.expr as { span?: Span }).span ?? expression.span, {
              field: field.name,
              expected: formatType(schemaField.type as Core.Type),
              actual: formatType(valueType),
            });
          }
        }
        for (const field of dataDecl.fields) {
          if (!provided.has(field.name)) {
            diagnostics.error(ErrorCode.MISSING_REQUIRED_FIELD, expression.span, {
              type: dataDecl.name,
              field: field.name,
            });
          }
        }
        this.result = { kind: 'TypeName', name: expression.typeName } as Core.TypeName;
        this.handled = true;
        return;
      }
      case 'Await': {
        const awaited = typeOfExpr(module, symbols, expression.expr, diagnostics);
        if (!isUnknown(awaited) && awaited.kind === 'Maybe') {
          this.result = awaited.type as Core.Type;
        } else if (!isUnknown(awaited) && awaited.kind === 'Result') {
          this.result = awaited.ok as Core.Type;
        } else {
          diagnostics.warning(ErrorCode.AWAIT_TYPE, expression.span, { type: formatType(awaited) });
          this.result = unknownType();
        }
        this.handled = true;
        return;
      }
      case 'Lambda': {
        const params = expression.params.map(param => normalizeType(param.type as Core.Type)) as readonly Core.Type[];
        const funcType: Core.FuncType = {
          kind: 'FuncType',
          params,
          ret: normalizeType(expression.ret as Core.Type),
        };
        this.result = funcType;
        this.handled = true;
        return;
      }
      case 'Call': {
        // not(x): 简化布尔运算
        if (expression.target.kind === 'Name' && expression.target.name === 'not') {
          if (expression.args.length !== 1) {
            diagnostics.error(ErrorCode.NOT_CALL_ARITY, expression.span ?? expression.target.span, {});
          } else {
            void typeOfExpr(module, symbols, expression.args[0]!, diagnostics);
          }
          this.result = { kind: 'TypeName', name: 'Bool' } as Core.TypeName;
          this.handled = true;
          return;
        }

        for (const arg of expression.args) {
          void typeOfExpr(module, symbols, arg, diagnostics);
        }

        if (expression.target.kind === 'Name' && expression.target.name.includes('.')) {
          let hasInt = false;
          let hasLong = false;
          let hasDouble = false;
          for (const arg of expression.args) {
            switch (arg.kind) {
              case 'Int':
                hasInt = true;
                break;
              case 'Long':
                hasLong = true;
                break;
              case 'Double':
                hasDouble = true;
                break;
            }
          }
          const kindCount = (hasInt ? 1 : 0) + (hasLong ? 1 : 0) + (hasDouble ? 1 : 0);
          if (kindCount > 1) {
            diagnostics.warning(ErrorCode.AMBIGUOUS_INTEROP_NUMERIC, expression.span ?? expression.target.span, {
              target: expression.target.name,
              hasInt,
              hasLong,
              hasDouble,
            });
          }
        }

        if (expression.target.kind === 'Name' && expression.target.name === 'await' && expression.args.length === 1) {
          const awaitedType = typeOfExpr(module, symbols, expression.args[0]!, diagnostics);
          if (!isUnknown(awaitedType) && awaitedType.kind === 'Maybe') {
            this.result = awaitedType.type as Core.Type;
          } else if (!isUnknown(awaitedType) && awaitedType.kind === 'Result') {
            this.result = awaitedType.ok as Core.Type;
          } else {
            diagnostics.warning(ErrorCode.AWAIT_TYPE, expression.span, { type: formatType(awaitedType) });
            this.result = unknownType();
          }
          this.handled = true;
          return;
        }

        if (expression.target.kind === 'Name') {
          const signature = module.funcSignatures.get(expression.target.name);
          if (signature) {
            this.result = signature.ret;
            this.handled = true;
            return;
          }
        }

        this.result = unknownType();
        this.handled = true;
        return;
      }
      default:
        break;
    }
  }
}

function typeOfExpr(
  ctx: ModuleContext,
  symbols: SymbolTable,
  expr: Core.Expression,
  diagnostics: DiagnosticBuilder
): Core.Type {
  const visitor = new TypeOfExprVisitor();
  visitor.visitExpression(expr, { module: ctx, symbols, diagnostics });
  return visitor.result;
}

/**
 * 异步任务分析结果
 *
 * 包含所有 Start 和 Wait 语句的位置信息，用于生成精确的诊断消息。
 */
export interface AsyncAnalysis {
  starts: Map<string, Span[]>; // 任务名 -> Start 位置列表
  waits: Map<string, Span[]>;  // 任务名 -> Wait 位置列表
}

/**
 * 表示单个异步调度节点，记录语句类型与控制流上下文。
 */
export interface ScheduleNode {
  kind: 'Start' | 'Wait';
  name: string;
  index: number;
  blockDepth: number;
  conditionalDepth: number;
  origin: Span | undefined;
}

/**
 * 异步调度图，按执行顺序收集所有调度节点。
 */
export interface AsyncSchedule {
  nodes: ScheduleNode[];
  taskNames: Set<string>;
  conditionalPaths: Map<number, string>;
  conditionalBranches: Map<number, Set<string>>;
}

function collectAsync(b: Core.Block): AsyncAnalysis {
  const starts = new Map<string, Span[]>();
  const waits = new Map<string, Span[]>();
  const fallbackSpan: Span = {
    start: { line: 0, col: 0 },
    end: { line: 0, col: 0 },
  };

  const ensureEntry = (map: Map<string, Span[]>, name: string): Span[] => {
    let bucket = map.get(name);
    if (!bucket) {
      bucket = [];
      map.set(name, bucket);
    }
    return bucket;
  };

  const toSpan = (origin: Origin | undefined): Span | undefined =>
    origin ? { start: origin.start, end: origin.end } : undefined;

  const record = (map: Map<string, Span[]>, name: string, span: Span | undefined): void => {
    const bucket = ensureEntry(map, name);
    bucket.push(span ?? fallbackSpan);
  };

  class AsyncVisitor extends DefaultCoreVisitor {
    override visitStatement(s: Core.Statement, context: import('./visitor.js').VisitorContext): void {
      if (s.kind === 'Start') {
        record(starts, s.name, toSpan(s.origin));
      } else if (s.kind === 'Wait') {
        for (const name of s.names) {
          record(waits, name, toSpan(s.origin));
        }
      }
      super.visitStatement(s, context);
    }
  }

  new AsyncVisitor().visitBlock(b, createVisitorContext());
  return { starts, waits };
}

function scheduleAsync(b: Core.Block): AsyncSchedule {
  const nodes: ScheduleNode[] = [];
  const taskNames = new Set<string>();

  const toSpan = (origin: Origin | undefined): Span | undefined =>
    origin ? { start: origin.start, end: origin.end } : undefined;

  class ScheduleBuilder extends DefaultCoreVisitor {
    private index = 0;
    private blockDepth = 0;
    private readonly conditionalStack: Array<{ id: number; value: string }> = [];
    private readonly pathRegistry = new Map<string, number>();
    private readonly pathLookup = new Map<number, string>();
    private readonly branchRegistry = new Map<number, Set<string>>();
    private nextConditionalId = 1;
    private nextPathId = 1;
    private isRootBlock = true;

    private currentPathId(): number {
      if (this.conditionalStack.length === 0) return 0;
      const key = this.conditionalStack.map(entry => `${entry.id}:${entry.value}`).join('|');
      let id = this.pathRegistry.get(key);
      if (id === undefined) {
        id = this.nextPathId++;
        this.pathRegistry.set(key, id);
        this.pathLookup.set(id, key);
      }
      return id;
    }

    private withConditional(condId: number, value: string, fn: () => void): void {
      this.registerBranch(condId, value);
      this.conditionalStack.push({ id: condId, value });
      try {
        fn();
      } finally {
        this.conditionalStack.pop();
      }
    }

    private registerBranch(condId: number, value: string): void {
      let branches = this.branchRegistry.get(condId);
      if (!branches) {
        branches = new Set();
        this.branchRegistry.set(condId, branches);
      }
      branches.add(value);
    }

    override visitBlock(block: Core.Block, ctx: import('./visitor.js').VisitorContext): void {
      const isRoot = this.isRootBlock;
      if (isRoot) {
        this.isRootBlock = false;
      } else {
        this.blockDepth++;
      }

      for (const statement of block.statements) {
        this.visitStatement(statement, ctx);
      }

      if (!isRoot) {
        this.blockDepth--;
      }
    }

    override visitStatement(s: Core.Statement, ctx: import('./visitor.js').VisitorContext): void {
      const currentIndex = this.index++;
      const pathId = this.currentPathId();

      if (s.kind === 'Start') {
        nodes.push({
          kind: 'Start',
          name: s.name,
          index: currentIndex,
          blockDepth: this.blockDepth,
          conditionalDepth: pathId,
          origin: toSpan(s.origin),
        });
        taskNames.add(s.name);
      } else if (s.kind === 'Wait') {
        for (const name of s.names) {
          nodes.push({
            kind: 'Wait',
            name,
            index: currentIndex,
            blockDepth: this.blockDepth,
            conditionalDepth: pathId,
            origin: toSpan(s.origin),
          });
          taskNames.add(name);
        }
      }

      switch (s.kind) {
        case 'Let':
        case 'Set':
        case 'Return':
          this.visitExpression(s.expr, ctx);
          return;
        case 'If': {
          this.visitExpression(s.cond, ctx);
          const condId = this.nextConditionalId++;
          this.registerBranch(condId, 'then');
          this.withConditional(condId, 'then', () => {
            this.visitBlock(s.thenBlock, ctx);
          });
          this.withConditional(condId, 'else', () => {
            if (s.elseBlock) {
              this.visitBlock(s.elseBlock, ctx);
            }
          });
          return;
        }
        case 'Match': {
          this.visitExpression(s.expr, ctx);
          const condId = this.nextConditionalId++;
          let branchIndex = 0;
          for (const kase of s.cases) {
            if (kase.pattern) this.visitPattern?.(kase.pattern, ctx);
            const branchLabel = `case#${branchIndex++}`;
            this.withConditional(condId, branchLabel, () => {
              if (kase.body.kind === 'Return') {
                this.visitExpression(kase.body.expr, ctx);
              } else {
                this.visitBlock(kase.body, ctx);
              }
            });
          }
          return;
        }
        case 'Scope':
          this.visitBlock({ kind: 'Block', statements: s.statements }, ctx);
          return;
        case 'Start':
          this.visitExpression(s.expr, ctx);
          return;
        case 'Wait':
          return;
      }
    }

    getConditionalPaths(): Map<number, string> {
      const result = new Map<number, string>();
      result.set(0, 'root');
      for (const [id, key] of this.pathLookup) {
        result.set(id, key);
      }
      return result;
    }

    getConditionalBranches(): Map<number, Set<string>> {
      return this.branchRegistry;
    }
  }

  const builder = new ScheduleBuilder();
  builder.visitBlock(b, createVisitorContext());
  return {
    nodes,
    taskNames,
    conditionalPaths: builder.getConditionalPaths(),
    conditionalBranches: builder.getConditionalBranches(),
  };
}

function validateSchedule(
  schedule: AsyncSchedule,
  analysis: AsyncAnalysis,
  diagnostics: DiagnosticBuilder
): void {
  const startsByTask = new Map<string, ScheduleNode[]>();
  const waitsByTask = new Map<string, ScheduleNode[]>();

  for (const node of schedule.nodes) {
    if (node.kind === 'Start') {
      const bucket = startsByTask.get(node.name);
      if (bucket) {
        bucket.push(node);
      } else {
        startsByTask.set(node.name, [node]);
      }
    } else {
      const bucket = waitsByTask.get(node.name);
      if (bucket) {
        bucket.push(node);
      } else {
        waitsByTask.set(node.name, [node]);
      }
    }
  }

  const assignmentCache = new Map<number, Map<number, string>>();
  const parseAssignments = (node: ScheduleNode): Map<number, string> => {
    const cached = assignmentCache.get(node.conditionalDepth);
    if (cached) return cached;
    const signature = schedule.conditionalPaths.get(node.conditionalDepth);
    const assignments = new Map<number, string>();
    if (signature && signature !== 'root') {
      for (const part of signature.split('|')) {
        if (!part) continue;
        const [condIdStr, value] = part.split(':', 2);
        if (!condIdStr || value === undefined) continue;
        const idNum = Number(condIdStr);
        if (!Number.isNaN(idNum)) {
          assignments.set(idNum, value);
        }
      }
    }
    assignmentCache.set(node.conditionalDepth, assignments);
    return assignments;
  };

  const pathsCompatible = (a: ScheduleNode, b: ScheduleNode): boolean => {
    const aAssignments = parseAssignments(a);
    const bAssignments = parseAssignments(b);
    for (const [condId, value] of aAssignments) {
      const other = bAssignments.get(condId);
      if (other !== undefined && other !== value) {
        return false;
      }
    }
    for (const [condId, value] of bAssignments) {
      const other = aAssignments.get(condId);
      if (other !== undefined && other !== value) {
        return false;
      }
    }
    return true;
  };

  const isPlaceholderSpan = (span: Span | undefined): boolean =>
    !span || (
      span.start.line === 0 &&
      span.start.col === 0 &&
      span.end.line === 0 &&
      span.end.col === 0
    );

  const pickFirstRealSpan = (spans: Span[] | undefined): Span | undefined =>
    spans?.find(span => !isPlaceholderSpan(span));

  const resolveSpan = (node: ScheduleNode, fallbacks: Span[] | undefined): Span | undefined =>
    isPlaceholderSpan(node.origin) ? pickFirstRealSpan(fallbacks) : node.origin;

  const hasBranchCoverage = (candidates: ScheduleNode[]): boolean => {
    if (candidates.length === 0) return false;
    const coverage = new Map<number, Set<string>>();
    for (const candidate of candidates) {
      const assignments = parseAssignments(candidate);
      if (assignments.size === 0) return false;
      for (const [condId, value] of assignments) {
        let bucket = coverage.get(condId);
        if (!bucket) {
          bucket = new Set();
          coverage.set(condId, bucket);
        }
        bucket.add(value);
      }
    }

    for (const [condId, observed] of coverage) {
      const possible = schedule.conditionalBranches.get(condId);
      if (!possible || possible.size === 0) return false;
      for (const value of possible) {
        if (!observed.has(value)) return false;
      }
    }
    return coverage.size > 0;
  };

  for (const [taskName, waitNodes] of waitsByTask) {
    const startNodes = startsByTask.get(taskName) ?? [];
    for (const wait of waitNodes) {
      const candidates = startNodes.filter(
        candidate => candidate.index < wait.index && pathsCompatible(candidate, wait)
      );

      let validStart = candidates.find(candidate => candidate.blockDepth <= wait.blockDepth);

      if (!validStart) {
        const deeperCandidates = candidates.filter(candidate => candidate.blockDepth > wait.blockDepth);
        if (deeperCandidates.length > 0 && hasBranchCoverage(deeperCandidates)) {
          validStart = deeperCandidates[0];
        }
      }

      if (!validStart) {
        diagnostics.error(ErrorCode.ASYNC_WAIT_BEFORE_START, resolveSpan(wait, analysis.waits.get(taskName)), {
          task: taskName,
        });
      }
    }
  }

  for (const [taskName, startNodes] of startsByTask) {
    startNodes.sort((a, b) => a.index - b.index);
    const observed: ScheduleNode[] = [];
    const count = analysis.starts.get(taskName)?.length ?? startNodes.length;

    for (const start of startNodes) {
      const conflicting = observed.find(prev => pathsCompatible(prev, start));
      if (conflicting) {
        diagnostics.error(ErrorCode.ASYNC_DUPLICATE_START, resolveSpan(start, analysis.starts.get(taskName)), {
          task: taskName,
          count,
        });
      } else {
        observed.push(start);
      }
    }
  }
}
