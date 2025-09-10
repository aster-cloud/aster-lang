import fs from 'node:fs';
import path from 'node:path';
import type { Core } from '../types.js';

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
      // MVP: handle match on nullable and on data ctor name pattern
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
          // bind names in order to fields with same order
          p.names.forEach((n, idx) => {
            lines.push(
              `${indent}    var ${n} = __tmp.${fieldNameByIndex(p.typeName, helpers, idx)};`
            );
          });
          lines.push(emitCaseBody(c.body, locals, helpers, indent + '    '));
          lines.push(`${indent}  }`);
        } else if (c.pattern.kind === 'PatName') {
          // enum variant name or catch-all: treat as else if not null
          lines.push(`${indent}  if (__scrut != null) {`);
          lines.push(emitCaseBody(c.body, locals, helpers, indent + '    '));
          lines.push(`${indent}  }`);
        }
      }
      lines.push(`${indent}}\n`);
      return lines.join('\n');
    }
    case 'Scope': {
      const body = s.statements.map(st => emitStatement(st, locals, helpers, indent)).join('');
      return body;
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

function emitFunc(pkgDecl: string, f: Core.Func, helpers: EmitHelpers): string {
  const ret = javaType(f.ret);
  const params = f.params.map(p => `${javaType(p.type)} ${p.name}`).join(', ');
  const body = emitBlock(f.body, [], helpers, '    ');
  const fallback = ret === 'int' ? '0' : ret === 'boolean' ? 'false' : 'null';
  return `${pkgDecl}public final class ${f.name}_fn {\n  private ${f.name}_fn(){}\n  public static ${ret} ${f.name}(${params}) {\n${body}    return ${fallback};\n  }\n}\n`;
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
