import type { EffectSignature } from '../effect_signature.js';

type EffectCache = Map<string, EffectSignature>;

const effectSignaturesByModule = new Map<string, EffectCache>();
const moduleByUri = new Map<string, string>();
const uriByModule = new Map<string, string>();
const importsByModule = new Map<string, Set<string>>();
const dependentsByModule = new Map<string, Set<string>>();

export interface CacheEffectOptions {
  moduleName: string;
  uri?: string | null;
  signatures: Map<string, EffectSignature>;
  imports?: readonly string[];
}

export function cacheModuleEffectSignatures(options: CacheEffectOptions): void {
  const { moduleName, uri, signatures, imports = [] } = options;
  if (!moduleName || moduleName === '<anonymous>') return;

  effectSignaturesByModule.set(moduleName, new Map(signatures));

  if (uri) {
    moduleByUri.set(uri, moduleName);
    uriByModule.set(moduleName, uri);
  }

  updateDependencies(moduleName, imports);
}

export function getModuleEffectSignatures(moduleName: string): ReadonlyMap<string, EffectSignature> | undefined {
  return effectSignaturesByModule.get(moduleName);
}

export function invalidateModuleEffectsByUri(uri: string): void {
  const moduleName = moduleByUri.get(uri);
  if (!moduleName) return;
  invalidateModuleEffects(moduleName);
}

export function invalidateModuleEffects(moduleName: string): void {
  const visited = new Set<string>();
  invalidateRecursive(moduleName, visited);
}

export function clearModuleEffectCache(): void {
  effectSignaturesByModule.clear();
  moduleByUri.clear();
  uriByModule.clear();
  importsByModule.clear();
  dependentsByModule.clear();
}

function updateDependencies(moduleName: string, imports: readonly string[]): void {
  const normalized = new Set(imports.filter(value => typeof value === 'string' && value.length > 0));
  const previous = importsByModule.get(moduleName);
  if (previous) {
    for (const dep of previous) {
      dependentsByModule.get(dep)?.delete(moduleName);
      if (dependentsByModule.get(dep)?.size === 0) {
        dependentsByModule.delete(dep);
      }
    }
  }

  importsByModule.set(moduleName, normalized);
  for (const dep of normalized) {
    let dependents = dependentsByModule.get(dep);
    if (!dependents) {
      dependents = new Set();
      dependentsByModule.set(dep, dependents);
    }
    dependents.add(moduleName);
  }
}

function invalidateRecursive(moduleName: string, visited: Set<string>): void {
  if (visited.has(moduleName)) return;
  visited.add(moduleName);

  effectSignaturesByModule.delete(moduleName);

  const imports = importsByModule.get(moduleName);
  if (imports) {
    for (const dep of imports) {
      dependentsByModule.get(dep)?.delete(moduleName);
      if (dependentsByModule.get(dep)?.size === 0) {
        dependentsByModule.delete(dep);
      }
    }
  }
  importsByModule.delete(moduleName);

  const uri = uriByModule.get(moduleName);
  if (uri) {
    uriByModule.delete(moduleName);
    moduleByUri.delete(uri);
  }

  const dependents = dependentsByModule.get(moduleName);
  dependentsByModule.delete(moduleName);

  if (!dependents) return;
  for (const dependent of dependents) {
    invalidateRecursive(dependent, visited);
  }
}
