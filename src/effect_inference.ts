import type { Core, TypecheckDiagnostic, Origin } from './types.js';
import { Effect } from './types.js';
import { getIOPrefixes, getCPUPrefixes } from './config/effect_config.js';
import { DefaultCoreVisitor, createVisitorContext } from './visitor.js';
import { resolveAlias } from './typecheck.js';
import { ErrorCode } from './error_codes.js';

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
  class CallScanVisitor extends DefaultCoreVisitor {
    override visitExpression(e: Core.Expression, context: import('./visitor.js').VisitorContext): void {
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
      super.visitExpression(e, context);
    }
  }

  if (func.body) new CallScanVisitor().visitBlock(func.body, createVisitorContext());

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
  if (effectMap.size === 0) return;

  const { nodes, adjacency } = buildEffectFlowGraph(constraints, effectMap);
  if (nodes.length === 0) return;

  const { components, componentByNode } = runTarjan(nodes, adjacency);
  const { componentEdges, indegree } = buildComponentGraph(components, componentByNode, adjacency);
  const order = topologicalSort(componentEdges, indegree);

  for (const componentIndex of order) {
    const members = components[componentIndex];
    if (!members || members.length === 0) continue;

    const firstMember = members[0]!;
    if (members.length > 1 || hasSelfLoop(firstMember, adjacency)) {
      let localChanged = true;
      while (localChanged) {
        localChanged = false;
        for (const node of members) {
          const source = effectMap.get(node);
          const neighbors = adjacency.get(node);
          if (!source || !neighbors) continue;
          for (const neighbor of neighbors) {
            if (componentByNode.get(neighbor) !== componentIndex) continue;
            const target = effectMap.get(neighbor);
            if (!target) continue;
            for (const effect of source) {
              if (!target.has(effect)) {
                target.add(effect);
                localChanged = true;
              }
            }
          }
        }
      }
    }

    for (const node of members) {
      const source = effectMap.get(node);
      const neighbors = adjacency.get(node);
      if (!source || !neighbors) continue;
      for (const neighbor of neighbors) {
        if (componentByNode.get(neighbor) === componentIndex) continue;
        const target = effectMap.get(neighbor);
        if (!target) continue;
        for (const effect of source) {
          target.add(effect);
        }
      }
    }
  }
}

function buildEffectFlowGraph(
  constraints: EffectConstraint[],
  effectMap: Map<string, Set<Effect>>
): { nodes: string[]; adjacency: Map<string, Set<string>> } {
  const adjacency = new Map<string, Set<string>>();
  const nodes: string[] = [];

  for (const node of effectMap.keys()) {
    nodes.push(node);
    adjacency.set(node, new Set());
  }

  for (const constraint of constraints) {
    if (!effectMap.has(constraint.caller) || !effectMap.has(constraint.callee)) continue;
    let followers = adjacency.get(constraint.callee);
    if (!followers) {
      followers = new Set();
      adjacency.set(constraint.callee, followers);
    }
    followers.add(constraint.caller);
  }

  return { nodes, adjacency };
}

function runTarjan(
  nodes: string[],
  adjacency: Map<string, Set<string>>
): { components: string[][]; componentByNode: Map<string, number> } {
  let index = 0;
  const indices = new Map<string, number>();
  const lowLinks = new Map<string, number>();
  const stack: string[] = [];
  const onStack = new Set<string>();
  const components: string[][] = [];
  const componentByNode = new Map<string, number>();

  function strongConnect(node: string): void {
    indices.set(node, index);
    lowLinks.set(node, index);
    index += 1;
    stack.push(node);
    onStack.add(node);

    const neighbors = adjacency.get(node);
    if (neighbors) {
      for (const neighbor of neighbors) {
        if (!indices.has(neighbor)) {
          strongConnect(neighbor);
          const currentLow = lowLinks.get(node)!;
          const neighborLow = lowLinks.get(neighbor)!;
          if (neighborLow < currentLow) lowLinks.set(node, neighborLow);
        } else if (onStack.has(neighbor)) {
          const currentLow = lowLinks.get(node)!;
          const neighborIndex = indices.get(neighbor)!;
          if (neighborIndex < currentLow) lowLinks.set(node, neighborIndex);
        }
      }
    }

    if (lowLinks.get(node) === indices.get(node)) {
      const component: string[] = [];
      while (true) {
        const member = stack.pop();
        if (!member) break;
        onStack.delete(member);
        component.push(member);
        componentByNode.set(member, components.length);
        if (member === node) break;
      }
      components.push(component);
    }
  }

  for (const node of nodes) {
    if (!indices.has(node)) strongConnect(node);
  }

  return { components, componentByNode };
}

function buildComponentGraph(
  components: string[][],
  componentByNode: Map<string, number>,
  adjacency: Map<string, Set<string>>
): { componentEdges: Map<number, Set<number>>; indegree: number[] } {
  const componentEdges = new Map<number, Set<number>>();
  const indegree = Array.from({ length: components.length }, () => 0);

  for (let componentIndex = 0; componentIndex < components.length; componentIndex += 1) {
    const componentMembers = components[componentIndex];
    if (!componentMembers) continue;
    for (const node of componentMembers) {
      const neighbors = adjacency.get(node);
      if (!neighbors) continue;
      for (const neighbor of neighbors) {
        const neighborComponent = componentByNode.get(neighbor);
        if (neighborComponent === undefined || neighborComponent === componentIndex) continue;
        let edges = componentEdges.get(componentIndex);
        if (!edges) {
          edges = new Set();
          componentEdges.set(componentIndex, edges);
        }
        if (!edges.has(neighborComponent)) {
          edges.add(neighborComponent);
          indegree[neighborComponent] = (indegree[neighborComponent] ?? 0) + 1;
        }
      }
    }
  }

  return { componentEdges, indegree };
}

function topologicalSort(
  componentEdges: Map<number, Set<number>>,
  indegree: number[]
): number[] {
  const order: number[] = [];
  const queue: number[] = [];
  const visited = Array.from({ length: indegree.length }, () => false);

  for (let i = 0; i < indegree.length; i += 1) {
    if (indegree[i] === 0) {
      queue.push(i);
    }
  }

  while (queue.length > 0) {
    const index = queue.shift()!;
    if (visited[index]) continue;
    visited[index] = true;
    order.push(index);
    const edges = componentEdges.get(index);
    if (!edges) continue;
    for (const next of edges) {
      if (next < 0 || next >= indegree.length) continue;
      const current = indegree[next];
      if (current === undefined) continue;
      const updated = current - 1;
      indegree[next] = updated;
      if (updated === 0 && !visited[next]) {
        queue.push(next);
      }
    }
  }

  if (order.length !== indegree.length) {
    // 理论上组件图无环，此处仅保证顺序覆盖全部节点
    for (let i = 0; i < indegree.length; i += 1) {
      if (!visited[i]) order.push(i);
    }
  }

  return order;
}

function hasSelfLoop(
  node: string,
  adjacency: Map<string, Set<string>>
): boolean {
  const neighbors = adjacency.get(node);
  return neighbors ? neighbors.has(node) : false;
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
        code: ErrorCode.EFF_INFER_MISSING_IO,
        help: '根据推断结果为函数添加 @io 效果。',
        ...(func.span ? { span: func.span } : {}),
        data: { func: name, effect: 'io' },
      };
      diagnostics.push(diag);
    }

    if (inferredHasCPU && !(declaredHasCPU || declaredHasIO)) {
      const diag: TypecheckDiagnostic = {
        severity: 'error',
        message: `函数 '${name}' 缺少 @cpu 效果声明，推断要求 CPU（或 @io）。`,
        code: ErrorCode.EFF_INFER_MISSING_CPU,
        help: '根据推断结果补齐 @cpu 或 @io 效果。',
        ...(func.span ? { span: func.span } : {}),
        data: { func: name, effect: 'cpu' },
      };
      diagnostics.push(diag);
    }

    const requiredHasIO = requiredSet.has(Effect.IO);
    const requiredHasCPU = requiredSet.has(Effect.CPU);

    if (declaredHasIO && !requiredHasIO) {
      const diag: TypecheckDiagnostic = {
        severity: 'warning',
        message: `函数 '${name}' 声明了 @io，但推断未发现 IO 副作用。`,
        code: ErrorCode.EFF_INFER_REDUNDANT_IO,
        help: '确认是否需要保留 @io 声明。',
        ...(func.span ? { span: func.span } : {}),
        data: { func: name, effect: 'io' },
      };
      diagnostics.push(diag);
    }

    if (declaredHasCPU) {
      if (!requiredHasCPU && !requiredHasIO) {
        const diag: TypecheckDiagnostic = {
          severity: 'warning',
          message: `函数 '${name}' 声明了 @cpu，但推断未发现 CPU 副作用。`,
          code: ErrorCode.EFF_INFER_REDUNDANT_CPU,
          help: '若无 CPU 副作用，可删除 @cpu 声明。',
          ...(func.span ? { span: func.span } : {}),
          data: { func: name, effect: 'cpu' },
        };
        diagnostics.push(diag);
      } else if (!requiredHasCPU && requiredHasIO) {
        const diag: TypecheckDiagnostic = {
          severity: 'warning',
          message: `函数 '${name}' 同时声明 @cpu 和 @io；由于需要 @io，@cpu 可移除。`,
          code: ErrorCode.EFF_INFER_REDUNDANT_CPU_WITH_IO,
          help: '保留 @io 即可满足需求，移除多余的 @cpu。',
          ...(func.span ? { span: func.span } : {}),
          data: { func: name, effect: 'cpu' },
        };
        diagnostics.push(diag);
      }
    }
  }

  return diagnostics;
}
