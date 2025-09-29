export type Capability = 'io' | 'cpu';

export interface CapabilityManifest {
  readonly allow: { readonly [K in Capability]?: readonly string[] };
  readonly deny?: { readonly [K in Capability]?: readonly string[] };
}

export interface CapabilityContext {
  readonly moduleName: string; // e.g., demo.login
}

export function isAllowed(
  cap: Capability,
  funcName: string,
  ctx: CapabilityContext,
  man: CapabilityManifest | null
): boolean {
  if (!man) return true; // no manifest -> permissive
  // Deny entries take precedence if present
  const deny = man.deny?.[cap] ?? [];
  const fqn = `${ctx.moduleName}.${funcName}`;
  for (const pat of deny) if (matches(pat, ctx.moduleName, fqn)) return false;
  const patterns = man.allow[cap];
  if (!patterns || patterns.length === 0) return false; // manifest present but no allow-list for cap
  for (const pat of patterns) if (matches(pat, ctx.moduleName, fqn)) return true;
  return false;
}

function matches(pat: string, moduleName: string, fqn: string): boolean {
  if (pat === '*') return true;
  // Simple suffix wildcard: 'module.func*' → startsWith on fqn
  if (pat.endsWith('*')) {
    const base = pat.slice(0, -1);
    return fqn.startsWith(base) || moduleName.startsWith(base);
  }
  if (pat.endsWith('.*')) {
    const pref = pat.slice(0, -2);
    return fqn.startsWith(pref + '.') || moduleName === pref;
  }
  return fqn === pat || moduleName === pat;
}
