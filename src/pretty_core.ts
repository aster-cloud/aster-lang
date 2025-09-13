import type { Core } from './types.js';

export function formatModule(m: Core.Module): string {
  const parts: string[] = [];
  if (m.name) parts.push(`// module ${m.name}`);
  for (const d of m.decls) {
    parts.push(formatDecl(d));
  }
  return parts.join('\n');
}

export function formatDecl(d: Core.Declaration): string {
  switch (d.kind) {
    case 'Import':
      return `use ${d.name}${d.asName ? ` as ${d.asName}` : ''}`;
    case 'Data': {
      const fields = d.fields.map(f => `${f.name}: ${formatType(f.type)}`).join(', ');
      return `data ${d.name}(${fields})`;
    }
    case 'Enum': {
      return `enum ${d.name} { ${d.variants.join(', ')} }`;
    }
    case 'Func':
      return formatFunc(d);
  }
}

export function formatFunc(f: Core.Func): string {
  const params = f.params.map(p => `${p.name}: ${formatType(p.type)}`).join(', ');
  const eff =
    f.effects && f.effects.length
      ? ` @${f.effects.map(e => String(e).toLowerCase()).join(',')}`
      : '';
  const body = formatBlock(f.body, 0);
  return `func ${f.name}(${params}): ${formatType(f.ret)}${eff} = ${body}`;
}

function indent(n: number): string {
  return '  '.repeat(n);
}

function formatBlock(b: Core.Block, lvl: number): string {
  if (!b.statements.length) return '{}';
  const lines = b.statements.map(s => indent(lvl + 1) + formatStmt(s, lvl + 1));
  return `{
${lines.join('\n')}
${indent(lvl)}}`;
}

function formatStmt(s: Core.Statement, lvl: number): string {
  switch (s.kind) {
    case 'Let':
      return `val ${s.name} = ${formatExpr(s.expr)}`;
    case 'Set':
      return `${s.name} = ${formatExpr(s.expr)}`;
    case 'Return':
      return `return ${formatExpr(s.expr)}`;
    case 'If': {
      const thenB = formatBlock(s.thenBlock, lvl);
      const elseB = s.elseBlock ? ` else ${formatBlock(s.elseBlock, lvl)}` : '';
      return `if (${formatExpr(s.cond)}) ${thenB}${elseB}`;
    }
    case 'Match': {
      const cases = s.cases
        .map(c => `${indent(lvl)}${formatPattern(c.pattern)} -> ${formatCaseBody(c.body, lvl)}`)
        .join('\n');
      return `match (${formatExpr(s.expr)}) {
${cases}
${indent(lvl - 1)}}`;
    }
    case 'Scope': {
      const inner: Core.Block = { kind: 'Block', statements: s.statements };
      return `scope ${formatBlock(inner, lvl)}`;
    }
    case 'Start':
      return `val ${s.name} = async { ${formatExpr(s.expr)} }`;
    case 'Wait':
      return `awaitAll(${s.names.join(', ')})`;
  }
}

function formatPattern(p: Core.Pattern): string {
  switch (p.kind) {
    case 'PatNull':
      return 'null';
    case 'PatCtor': {
      const pat = p as Core.PatCtor & { args?: readonly Core.Pattern[] };
      if (pat.args && pat.args.length > 0) {
        const parts = pat.args.map(pp => formatPattern(pp));
        return `${pat.typeName}(${parts.join(', ')})`;
      }
      return `${pat.typeName}(${pat.names.join(', ')})`;
    }
    case 'PatName':
      return p.name;
  }
}

function formatCaseBody(body: Core.Return | Core.Block, lvl: number): string {
  if (body.kind === 'Return') return formatExpr(body.expr);
  return formatBlock(body, lvl);
}

function formatExpr(e: Core.Expression): string {
  switch (e.kind) {
    case 'Name':
      return e.name;
    case 'Bool':
      return String(e.value);
    case 'Int':
      return String(e.value);
    case 'String':
      return JSON.stringify(e.value);
    case 'Null':
      return 'null';
    case 'Call': {
      if (e.target.kind === 'Name' && e.target.name === 'await') {
        return `await(${e.args.map(formatExpr).join(', ')})`;
      }
      return `${formatExpr(e.target)}(${e.args.map(formatExpr).join(', ')})`;
    }
    case 'Construct': {
      const fs = e.fields.map(f => `${f.name} = ${formatExpr(f.expr)}`).join(', ');
      return `${e.typeName}(${fs})`;
    }
    case 'Ok':
      return `Ok(${formatExpr(e.expr)})`;
    case 'Err':
      return `Err(${formatExpr(e.expr)})`;
    case 'Some':
      return `Some(${formatExpr(e.expr)})`;
    case 'None':
      return 'None';
    case 'Lambda': {
      const ps = e.params.map(p => `${p.name}: ${formatType(p.type)}`).join(', ');
      const body = formatBlock(e.body, 0);
      return `(${ps}) => ${body}`;
    }
  }
}

function formatType(t: Core.Type): string {
  switch (t.kind) {
    case 'TypeName':
      return t.name;
    case 'TypeVar':
      return t.name;
    case 'TypeApp':
      return `${t.base}<${t.args.map(formatType).join(', ')}>`;
    case 'Maybe':
      return `${formatType(t.type)}?`;
    case 'Option':
      return `Option<${formatType(t.type)}>`;
    case 'Result':
      return `Result<${formatType(t.ok)}, ${formatType(t.err)}>`;
    case 'List':
      return `List<${formatType(t.type)}>`;
    case 'Map':
      return `Map<${formatType(t.key)}, ${formatType(t.val)}>`;
    case 'FuncType': {
      const ps = t.params.map(formatType).join(', ');
      return `(${ps}) -> ${formatType(t.ret)}`;
    }
  }
}
