import fs from 'node:fs';
import path from 'node:path';
import type {Core} from '../types.js';
// Note: 代码生成包含具体副作用与输出顺序，此处暂不引入访问器改造，保持原有手写遍历以确保行为稳定。

function ensureDir(p: string): void {
  fs.mkdirSync(p, { recursive: true });
}

function pkgToPath(pkg: string | null): { pkgDecl: string; dir: string } {
  const pkgName = pkg ?? '';
  if (!pkgName) return { pkgDecl: '', dir: '' };
  return { pkgDecl: `package ${pkgName};\n\n`, dir: pkgName.replaceAll('.', path.sep) };
}

function javaType(t: Core.Type): string {
  switch (t.kind) {
    case 'TypeName': {
      const n = t.name;
      if (n === 'Text') return 'String';
      if (n === 'Text?') return 'String'; // Maybe Text

      if (n === 'Int') return 'int';
      if (n === 'Bool') return 'boolean';
      return n; // user types
    }
    case 'Result': {
      const ok = javaType(t.ok);
      const err = javaType(t.err);
      return `aster.runtime.Result<${ok}, ${err}>`;
    }
    case 'TypeVar':
      return 'Object';
    case 'TypeApp': {
      // Basic mapping for unknown generic types: treat as raw type 'Object'
      // Future: map known generic bridges
      return 'Object';
    }
    case 'Maybe': {
      // Represent Maybe<T> as nullable T
      return javaType(t.type);
    }
    case 'Option':
      return javaType(t.type);
    case 'List':
      return `java.util.List<${javaType(t.type)}>`;
    case 'Map':
      return `java.util.Map<${javaType(t.key)}, ${javaType(t.val)}>`;
    case 'FuncType': {
      const ar = t.params.length;
      if (ar === 1) return 'aster.runtime.Fn1';
      if (ar === 2) return 'aster.runtime.Fn2';
      return 'java.lang.Object';
    }
    default:
      return 'Object';
  }
}

function emitData(pkgDecl: string, d: Core.Data): string {
  const fields = d.fields.map(f => `  public final ${javaType(f.type)} ${f.name};`).join('\n');
  const ctorParams = d.fields.map(f => `${javaType(f.type)} ${f.name}`).join(', ');
  const ctorBody = d.fields.map(f => `    this.${f.name} = ${f.name};`).join('\n');
  return `${pkgDecl}public final class ${d.name} {\n${fields}\n  public ${d.name}(${ctorParams}) {\n${ctorBody}\n  }\n}\n`;
}

function emitEnum(pkgDecl: string, e: Core.Enum): string {
  const variants = e.variants.join(', ');
  return `${pkgDecl}public enum ${e.name} { ${variants} }\n`;
}

function emitExpr(e: Core.Expression, helpers: EmitHelpers): string {
  switch (e.kind) {
    case 'Name': {
      const en = helpers.enumVariantToEnum.get(e.name);
      if (en) return `${en}.${e.name}`;
      if (e.name === 'UUID.randomUUID') return 'java.util.UUID.randomUUID().toString()';
      return e.name;
    }
    case 'Bool':
      return e.value ? 'true' : 'false';
    case 'Int':
      return String(e.value);
    case 'String':
      return JSON.stringify(e.value);
    case 'Null':
      return 'null';
    case 'Ok':
      return `new aster.runtime.Ok<>(${emitExpr(e.expr, helpers)})`;
    case 'Err':
      return `new aster.runtime.Err<>(${emitExpr(e.expr, helpers)})`;
    case 'Some':
      return emitExpr(e.expr, helpers);
    case 'None':
      return 'null';
    case 'Construct': {
      const args = e.fields.map(f => emitExpr(f.expr, helpers)).join(', ');
      return `new ${e.typeName}(${args})`;
    }
    case 'Call': {
      if (e.target.kind === 'Name' && e.target.name === 'not' && e.args.length === 1) {
        return `!(${emitExpr(e.args[0]!, helpers)})`;
      }
      if (e.target.kind === 'Name') {
        const nm = e.target.name;
        if (nm === 'Text.concat' && e.args.length === 2) {
          const a = emitExpr(e.args[0]!, helpers);
          const b = emitExpr(e.args[1]!, helpers);
          return `(${a} + ${b})`;
        }
        if (nm === 'Text.contains' && e.args.length === 2) {
          const h = emitExpr(e.args[0]!, helpers);
          const n = emitExpr(e.args[1]!, helpers);
          return `${h}.contains(${n})`;
        }
        if (nm === 'Text.equals' && e.args.length === 2) {
          const a = emitExpr(e.args[0]!, helpers);
          const b = emitExpr(e.args[1]!, helpers);
          return `java.util.Objects.equals(${a}, ${b})`;
        }
        if (nm === 'Text.replace' && e.args.length === 3) {
          const h = emitExpr(e.args[0]!, helpers);
          const t = emitExpr(e.args[1]!, helpers);
          const r = emitExpr(e.args[2]!, helpers);
          return `${h}.replace(${t}, ${r})`;
        }
        if (nm === 'Text.split' && e.args.length === 2) {
          const h = emitExpr(e.args[0]!, helpers);
          const s = emitExpr(e.args[1]!, helpers);
          return `java.util.Arrays.asList(${h}.split(${s}))`;
        }
        if (nm === 'Text.indexOf' && e.args.length === 2) {
          const h = emitExpr(e.args[0]!, helpers);
          const n = emitExpr(e.args[1]!, helpers);
          return `${h}.indexOf(${n})`;
        }
        if (nm === 'Text.startsWith' && e.args.length === 2) {
          const h = emitExpr(e.args[0]!, helpers);
          const p = emitExpr(e.args[1]!, helpers);
          return `${h}.startsWith(${p})`;
        }
        if (nm === 'Text.endsWith' && e.args.length === 2) {
          const h = emitExpr(e.args[0]!, helpers);
          const s = emitExpr(e.args[1]!, helpers);
          return `${h}.endsWith(${s})`;
        }
        if (nm === 'Text.toUpper' && e.args.length === 1) {
          const h = emitExpr(e.args[0]!, helpers);
          return `${h}.toUpperCase()`;
        }
        if (nm === 'Text.toLower' && e.args.length === 1) {
          const h = emitExpr(e.args[0]!, helpers);
          return `${h}.toLowerCase()`;
        }
        if (nm === 'Text.length' && e.args.length === 1) {
          const h = emitExpr(e.args[0]!, helpers);
          return `${h}.length()`;
        }
        if (nm === 'List.length' && e.args.length === 1) {
          const xs = emitExpr(e.args[0]!, helpers);
          return `${xs}.size()`;
        }
        if (nm === 'List.get' && e.args.length === 2) {
          const xs = emitExpr(e.args[0]!, helpers);
          const i = emitExpr(e.args[1]!, helpers);
          return `${xs}.get(${i})`;
        }
        if (nm === 'List.isEmpty' && e.args.length === 1) {
          const xs = emitExpr(e.args[0]!, helpers);
          return `${xs}.isEmpty()`;
        }
        if (nm === 'List.head' && e.args.length === 1) {
          const xs = emitExpr(e.args[0]!, helpers);
          return `(${xs}.isEmpty() ? null : ${xs}.get(0))`;
        }
        if (nm === 'Map.get' && e.args.length === 2) {
          const m = emitExpr(e.args[0]!, helpers);
          const k = emitExpr(e.args[1]!, helpers);
          return `${m}.get(${k})`;
        }
      }
      const tgt = emitExpr(e.target, helpers);
      const args = e.args.map(a => emitExpr(a, helpers)).join(', ');
      return `${tgt}(${args})`;
    }
    default:
      return 'null';
  }
}

interface EmitHelpers {
  dataSchema: Map<string, Core.Data>;
  enumVariantToEnum: Map<string, string>;
}

// 保持原始代码生成逻辑：不使用访问器以免影响输出行为
function emitStatement(
  s: Core.Statement,
  locals: string[],
  helpers: EmitHelpers,
  indent = '    '
): string {
  switch (s.kind) {
    case 'Let':
      return `${indent}${javaLocalDecl(s.name)} = ${emitExpr(s.expr, helpers)};\n`;
    case 'Set':
      return `${indent}${s.name} = ${emitExpr(s.expr, helpers)};\n`;
    case 'Return':
      return `${indent}return ${emitExpr(s.expr, helpers)};\n`;
    case 'If': {
      const cond = emitExpr(s.cond, helpers);
      const thenB = emitBlock(s.thenBlock, locals, helpers, indent + '  ');
      const elseB = s.elseBlock
        ? ` else {\n${emitBlock(s.elseBlock, locals, helpers, indent + '  ')}${indent}}\n`
        : '\n';
      return `${indent}if (${cond}) {\n${thenB}${indent}}${elseB}`;
    }
    case 'Match': {
      // 尝试优化为 enum switch 或 int switch（只读分析）
      const enName = analyzeMatchForEnumSwitch(s, helpers);
      const allPatInt = analyzeMatchForIntSwitch(s);
      if (enName) {
          const scrut = emitExpr(s.expr, helpers);
          const lines: string[] = [];
          lines.push(`${indent}{`);
          lines.push(`${indent}  var __scrut = ${scrut};`);
          lines.push(`${indent}  switch((${enName})__scrut) {`);
          for (const c of s.cases) {
            const variant = (c.pattern as Core.PatName).name;
            lines.push(`${indent}    case ${enName}.${variant}: {`);
            const bodyStr = emitCaseBody(c.body, locals, helpers, indent + '      ');
            lines.push(bodyStr);
            if (c.body.kind !== 'Return') lines.push(`${indent}      break;`);
            lines.push(`${indent}    }`);
          }
          lines.push(`${indent}  }`);
          lines.push(`${indent}}\n`);
          return lines.join('\n');
      }
      // Integers: emit a simple switch (string-emitter only)
      if (allPatInt && s.cases.length > 0) {
        const scrut = emitExpr(s.expr, helpers);
        const lines: string[] = [];
        lines.push(`${indent}{`);
        lines.push(`${indent}  switch (${scrut}) {`);
        for (const c of s.cases) {
          const v = (c.pattern as any).value as number;
          lines.push(`${indent}    case ${v}: {`);
          const bodyStr = emitCaseBody(c.body, locals, helpers, indent + '      ');
          lines.push(bodyStr);
          if (c.body.kind !== 'Return') lines.push(`${indent}      break;`);
          lines.push(`${indent}    }`);
        }
        lines.push(`${indent}    default: break;`);
        lines.push(`${indent}  }`);
        lines.push(`${indent}}\n`);
        return lines.join('\n');
      }
      // Fallback: handle nullable, data ctor name pattern, and basic PatName
      const scrut = emitExpr(s.expr, helpers);
      const lines: string[] = [];
      lines.push(`${indent}{`);
      lines.push(`${indent}  var __scrut = ${scrut};`);
      for (const c of s.cases) {
        if (c.pattern.kind === 'PatNull') {
          lines.push(`${indent}  if (__scrut == null) {`);
          lines.push(emitCaseBody(c.body, locals, helpers, indent + '    '));
          lines.push(`${indent}  }`);
        } else if (c.pattern.kind === 'PatCtor') {
          const p = c.pattern as Core.PatCtor;
          lines.push(`${indent}  if (__scrut instanceof ${p.typeName}) {`);
          lines.push(`${indent}    var __tmp = (${p.typeName})__scrut;`);
          const nb = emitNestedPatBinds(p, '__tmp', helpers, indent + '    ');
          lines.push(...nb.prefix);
          lines.push(emitCaseBody(c.body, locals, helpers, indent + '    '));
          lines.push(...nb.suffix);
          lines.push(`${indent}  }`);
        } else if (c.pattern.kind === 'PatName') {
          lines.push(`${indent}  if (__scrut != null) {`);
          lines.push(emitCaseBody(c.body, locals, helpers, indent + '    '));
          lines.push(`${indent}  }`);
        }
      }
      lines.push(`${indent}}\n`);
      return lines.join('\n');
    }
    case 'Scope': {
        return s.statements.map(st => emitStatement(st, locals, helpers, indent)).join('');
    }
    case 'Start':
    case 'Wait':
      // Async not handled in MVP
      return `${indent}// async not implemented in MVP\n`;
    }
  }

function emitCaseBody(
  b: Core.Return | Core.Block,
  locals: string[],
  helpers: EmitHelpers,
  indent: string
): string {
  if (b.kind === 'Return') return `${indent}return ${emitExpr(b.expr, helpers)};\n`;
  return emitBlock(b, locals, helpers, indent);
}
 

function emitBlock(b: Core.Block, locals: string[], helpers: EmitHelpers, indent = '    '): string {
  return b.statements.map(s => emitStatement(s, locals, helpers, indent)).join('');
}

function javaLocalDecl(name: string): string {
  return `var ${name}`;
}

function fieldByIndexName(index: number): string {
  // Fallback field name f0,f1,... for MVP; will refine with schema later
  return `f${index}`;
}

function fieldNameByIndex(typeName: string, helpers: EmitHelpers, idx: number): string {
  const d = helpers.dataSchema.get(typeName);
  if (!d) return fieldByIndexName(idx);
  if (idx < 0 || idx >= d.fields.length) return fieldByIndexName(idx);
  return d.fields[idx]!.name;
}

// 只读分析：是否可用 enum switch 优化（全部 PatName 且来自同一个 Enum）
function analyzeMatchForEnumSwitch(s: Core.Match, helpers: EmitHelpers): string | null {
  if (s.cases.length === 0) return null;
  if (!s.cases.every(c => c.pattern.kind === 'PatName')) return null;
  const enums = new Set<string>();
  for (const c of s.cases) {
    const variant = (c.pattern as Core.PatName).name;
    const en = helpers.enumVariantToEnum.get(variant);
    if (!en) return null;
    enums.add(en);
  }
  return enums.size === 1 ? [...enums][0]! : null;
}

// 只读分析：是否全部是整数模式
function analyzeMatchForIntSwitch(s: Core.Match): boolean {
  return s.cases.length > 0 && s.cases.every(c => c.pattern.kind === 'PatInt');
}

function emitNestedPatBinds(
  p: Core.PatCtor,
  baseVar: string,
  helpers: EmitHelpers,
  indent = '    '
): { prefix: string[]; suffix: string[] } {
  const prefix: string[] = [];
  const suffix: string[] = [];
  const patWithArgs = p as Core.PatCtor & { args?: readonly Core.Pattern[] };
  const args = patWithArgs.args as undefined | Core.Pattern[];
  if (args && args.length > 0) {
    args.forEach((child, idx) => {
      const field = fieldNameByIndex(p.typeName, helpers, idx);
      if (child.kind === 'PatName') {
        prefix.push(`${indent}var ${child.name} = ${baseVar}.${field};`);
      } else if (child.kind === 'PatCtor') {
        // open guard and bind child object
        const tmpVar = `${baseVar}_${idx}`;
        prefix.push(
          `${indent}if (${baseVar}.${field} instanceof ${(child as Core.PatCtor).typeName}) {`
        );
        prefix.push(
          `${indent}  var ${tmpVar} = ( ${(child as Core.PatCtor).typeName} )${baseVar}.${field};`
        );
        const rec = emitNestedPatBinds(child as Core.PatCtor, tmpVar, helpers, indent + '  ');
        prefix.push(...rec.prefix);
        // close nested guards after body
        suffix.unshift(...rec.suffix);
        suffix.unshift(`${indent}}`);
      }
    });
  } else {
    // Legacy names support
    (p.names || []).forEach((n, idx) => {
      prefix.push(`${indent}var ${n} = ${baseVar}.${fieldNameByIndex(p.typeName, helpers, idx)};`);
    });
  }
  return { prefix, suffix };
}

function emitFunc(pkgDecl: string, f: Core.Func, helpers: EmitHelpers): string {
  const ret = javaType(f.ret);
  const params = f.params.map(p => `${javaType(p.type)} ${p.name}`).join(', ');
  const body = emitBlock(f.body, [], helpers, '    ');
  const fallback = `    return ${ret === 'int' ? '0' : ret === 'boolean' ? 'false' : 'null'};\n`;
  return `${pkgDecl}public final class ${f.name}_fn {\n  private ${f.name}_fn(){}\n  public static ${ret} ${f.name}(${params}) {\n${body}${fallback}  }\n}\n`;
}

export async function emitJava(core: Core.Module, outRoot = 'build/jvm-src'): Promise<void> {
  const { pkgDecl, dir } = pkgToPath(core.name);
  const baseDir = path.join(outRoot, dir);
  ensureDir(baseDir);

  // Collect data decls for field mapping
  const dataSchema = new Map<string, Core.Data>();
  for (const d of core.decls) {
    if (d.kind === 'Data') dataSchema.set(d.name, d);
  }
  const helpers: EmitHelpers = { dataSchema, enumVariantToEnum: collectEnums(core) };

  for (const d of core.decls) {
    if (d.kind === 'Data') {
      const content = emitData(pkgDecl, d);
      fs.writeFileSync(path.join(baseDir, `${d.name}.java`), content, 'utf8');
    } else if (d.kind === 'Enum') {
      const content = emitEnum(pkgDecl, d);
      fs.writeFileSync(path.join(baseDir, `${d.name}.java`), content, 'utf8');
    } else if (d.kind === 'Func') {
      const content = emitFunc(pkgDecl, d, helpers);
      fs.writeFileSync(path.join(baseDir, `${d.name}_fn.java`), content, 'utf8');
    }
  }
}

function collectEnums(core: Core.Module): Map<string, string> {
  const map = new Map<string, string>();
  for (const d of core.decls) {
    if (d.kind === 'Enum') {
      for (const v of d.variants) map.set(v, d.name);
    }
  }
  return map;
}
