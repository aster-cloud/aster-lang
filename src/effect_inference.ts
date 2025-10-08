import type { Core, TypecheckDiagnostic, Origin } from './types.js';
import { Effect } from './types.js';
import { getIOPrefixes, getCPUPrefixes } from './config/effect_config.js';
import { DefaultCoreVisitor } from './visitor.js';
import { resolveAlias } from './typecheck.js';

// 从配置获取效果推断前缀（模块级，避免重复调用）
const IO_PREFIXES = getIOPrefixes();
const CPU_PREFIXES = getCPUPrefixes();

export interface EffectConstraint {
  caller: string;
  callee: string;
  location?: Origin;
}

interface FunctionAnalysis {
  constraints: EffectConstraint[];
  localEffects: Set<Effect>;
}

export function inferEffects(core: Core.Module, imports?: Map<string, string>): TypecheckDiagnostic[] {
  const funcIndex = new Map<string, Core.Func>();
  for (const decl of core.decls) {
    if (decl.kind === 'Func') funcIndex.set(decl.name, decl);
  }

  const constraints: EffectConstraint[] = [];
  const declaredEffects = new Map<string, Set<Effect>>();
  const inferredEffects = new Map<string, Set<Effect>>();
  const requiredEffects = new Map<string, Set<Effect>>();

  // 第一遍：收集局部效果和约束
  for (const func of funcIndex.values()) {
    const analysis = analyzeFunction(func, funcIndex, imports);
    constraints.push(...analysis.constraints);

    const declared = new Set<Effect>(func.effects);
    const inferred = new Set<Effect>([...func.effects, ...analysis.localEffects]);
    const required = new Set<Effect>(analysis.localEffects);

    declaredEffects.set(func.name, declared);
    inferredEffects.set(func.name, inferred);
    requiredEffects.set(func.name, required);
  }

  // 第二遍：将被调函数的声明效果添加到 requiredEffects
  for (const constraint of constraints) {
    const callerRequired = requiredEffects.get(constraint.caller);
    const calleeDeclared = declaredEffects.get(constraint.callee);
    if (callerRequired && calleeDeclared) {
      for (const eff of calleeDeclared) {
        callerRequired.add(eff);
      }
    }
  }

  propagateEffects(constraints, inferredEffects);
  propagateEffects(constraints, requiredEffects);

  return buildDiagnostics(funcIndex, declaredEffects, inferredEffects, requiredEffects);
}

function analyzeFunction(func: Core.Func, index: Map<string, Core.Func>, imports?: Map<string, string>): FunctionAnalysis {
  const constraints: EffectConstraint[] = [];
  const localEffects = new Set<Effect>();

  // 使用统一的 Core 访客遍历函数体，收集调用与内建效果
  class CallScanVisitor extends DefaultCoreVisitor<void> {
    override visitExpression(e: Core.Expression): void {
      if (e.kind === 'Call') {
        const calleeName = extractFunctionName(e.target);
        if (calleeName) {
          const resolvedName = imports ? resolveAlias(calleeName, imports) : calleeName;
          if (!index.has(resolvedName)) recordBuiltinEffect(resolvedName, localEffects);
          if (index.has(resolvedName)) {
            const constraint: EffectConstraint = { caller: func.name, callee: resolvedName };
            const call = e as Core.Call;
            if (call.origin) constraint.location = call.origin as Origin;
            constraints.push(constraint);
          }
        }
      }
      // 继续默认递归
      super.visitExpression(e, undefined as unknown as void);
    }
  }

  if (func.body) new CallScanVisitor().visitBlock(func.body, undefined as unknown as void);

  return { constraints, localEffects };
}

function extractFunctionName(expr: Core.Expression): string | null {
  return expr.kind === 'Name' ? expr.name : null;
}

function recordBuiltinEffect(name: string, effects: Set<Effect>): void {
  if (IO_PREFIXES.some(prefix => name.startsWith(prefix))) effects.add(Effect.IO);
  if (CPU_PREFIXES.some(prefix => name.startsWith(prefix))) effects.add(Effect.CPU);
}

function propagateEffects(
  constraints: EffectConstraint[],
  effectMap: Map<string, Set<Effect>>
): void {
  let changed = true;
  while (changed) {
    changed = false;
    for (const constraint of constraints) {
      const callerEffects = effectMap.get(constraint.caller);
      const calleeEffects = effectMap.get(constraint.callee);
      if (!callerEffects || !calleeEffects) continue;
      for (const eff of calleeEffects) {
        if (!callerEffects.has(eff)) {
          callerEffects.add(eff);
          changed = true;
        }
      }
    }
  }
}

function buildDiagnostics(
  funcIndex: Map<string, Core.Func>,
  declared: Map<string, Set<Effect>>,
  inferred: Map<string, Set<Effect>>,
  required: Map<string, Set<Effect>>
): TypecheckDiagnostic[] {
  const diagnostics: TypecheckDiagnostic[] = [];

  for (const [name, func] of funcIndex) {
    const declaredSet = declared.get(name) ?? new Set<Effect>();
    const inferredSet = inferred.get(name) ?? new Set<Effect>();
    const requiredSet = required.get(name) ?? new Set<Effect>();

    const inferredHasIO = inferredSet.has(Effect.IO);
    const inferredHasCPU = inferredSet.has(Effect.CPU);
    const declaredHasIO = declaredSet.has(Effect.IO);
    const declaredHasCPU = declaredSet.has(Effect.CPU);

    if (inferredHasIO && !declaredHasIO) {
      const diag: TypecheckDiagnostic = {
        severity: 'error',
        message: `函数 '${name}' 缺少 @io 效果声明，推断要求 IO。`,
        code: 'EFF_INFER_MISSING_IO',
        data: { func: name, effect: 'io' },
      };
      if (func.origin) diag.location = func.origin;
      diagnostics.push(diag);
    }

    if (inferredHasCPU && !(declaredHasCPU || declaredHasIO)) {
      const diag: TypecheckDiagnostic = {
        severity: 'error',
        message: `函数 '${name}' 缺少 @cpu 效果声明，推断要求 CPU（或 @io）。`,
        code: 'EFF_INFER_MISSING_CPU',
        data: { func: name, effect: 'cpu' },
      };
      if (func.origin) diag.location = func.origin;
      diagnostics.push(diag);
    }

    const requiredHasIO = requiredSet.has(Effect.IO);
    const requiredHasCPU = requiredSet.has(Effect.CPU);

    if (declaredHasIO && !requiredHasIO) {
      const diag: TypecheckDiagnostic = {
        severity: 'warning',
        message: `函数 '${name}' 声明了 @io，但推断未发现 IO 副作用。`,
        code: 'EFF_INFER_REDUNDANT_IO',
        data: { func: name, effect: 'io' },
      };
      if (func.origin) diag.location = func.origin;
      diagnostics.push(diag);
    }

    if (declaredHasCPU) {
      if (!requiredHasCPU && !requiredHasIO) {
        const diag: TypecheckDiagnostic = {
          severity: 'warning',
          message: `函数 '${name}' 声明了 @cpu，但推断未发现 CPU 副作用。`,
          code: 'EFF_INFER_REDUNDANT_CPU',
          data: { func: name, effect: 'cpu' },
        };
        if (func.origin) diag.location = func.origin;
        diagnostics.push(diag);
      } else if (!requiredHasCPU && requiredHasIO) {
        const diag: TypecheckDiagnostic = {
          severity: 'warning',
          message: `函数 '${name}' 同时声明 @cpu 和 @io；由于需要 @io，@cpu 可移除。`,
          code: 'EFF_INFER_REDUNDANT_CPU_WITH_IO',
          data: { func: name, effect: 'cpu' },
        };
        if (func.origin) diag.location = func.origin;
        diagnostics.push(diag);
      }
    }
  }

  return diagnostics;
}
