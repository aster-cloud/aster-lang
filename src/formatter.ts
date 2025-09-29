import { canonicalize } from './canonicalizer.js';
import { lex } from './lexer.js';
import { parse } from './parser.js';
import { buildCst } from './cst_builder.js';
import type {
  Module,
  Declaration,
  Data,
  Enum,
  Func,
  Block,
  Statement,
  Expression,
  Parameter,
  Type,
  ConstructField,
} from './types.js';

export function formatCNL(text: string): string {
  // Pre-sanitize common broken patterns (e.g., accidental '.:' before earlier formatter fix)
  const input = text
    .replace(/produce([^\n]*?)\.\s*:/g, (_m, p1) => `produce${p1}:`)
    // Replace legacy placeholder return with strict 'none'
    .replace(/^\s*Return\s+<expr>\s*\./gm, match => match.replace(/<expr>/, 'none'))
    .replace(/<expr>\s*\./g, 'none.')
    .replace(/^\s*Return\s+<[^>]+>\s*\./gm, 'Return none.')
    // Collapse accidental double periods from earlier bad formatters
    .replace(/\.{2,}/g, '.');
  const can = canonicalize(input);
  let tokens;
  try {
    tokens = lex(can);
  } catch {
    return text;
  }
  let formatted: string;
  const cst = buildCst(text, tokens);
  try {
    const ast = parse(tokens) as Module;
    formatted = simpleFormatModule(ast);
  } catch {
    // If the source doesn't parse, return it unchanged
    return input;
  }
  // Preserve a trailing newline if the original had one; otherwise leave as-is
  const hadTrailingNewline = /\n$/.test(cst.trailing?.text ?? '') || /\n$/.test(text);
  const out = formatted + (hadTrailingNewline ? '\n' : '');
  // Preserve any byte order mark or leading whitespace prefix (if any)
  const leading = cst.leading?.text ?? '';
  const bom = leading.startsWith('\uFEFF') ? '\uFEFF' : '';
  return bom + out;
}

function indent(n: number): string {
  return '  '.repeat(n);
}

function joinWithCommas(parts: string[]): string {
  return parts.join(', ');
}

 

// No doc-comment preservation in output; we keep formatting deterministic

function simpleFormatModule(m: Module): string {
  const out: string[] = [];
  if (m.name) out.push(`This module is ${m.name}.`);
  for (let i = 0; i < m.decls.length; i++) {
    const d = m.decls[i] as Declaration;
    if (out.length > 0) out.push('');
    out.push(formatDecl(d));
  }
  return out.join('\n');
}

function formatDecl(d: Declaration): string {
  switch (d.kind) {
    case 'Import': {
      const asPart = d.asName ? ` as ${d.asName}` : '';
      return `Use ${d.name}${asPart}.`;
    }
    case 'Data':
      return formatData(d as Data);
    case 'Enum':
      return formatEnum(d as Enum);
    case 'Func':
      return formatFunc(d as Func);
    default:
      return '// Unsupported declaration';
  }
}

function formatData(d: Data): string {
  const fields = d.fields.map(f => `${f.name}: ${formatType(f.type)}`);
  const tail = fields.length ? ` with ${joinWithCommas(fields)}` : '';
  return `Define ${d.name}${tail}.`;
}

function formatEnum(e: Enum): string {
  const vars = e.variants.join(', ');
  return `Define ${e.name} as one of ${vars}.`;
}

function formatFunc(f: Func): string {
  const params = formatParams(f.params);
  const hasEff = !!(f.effects && f.effects.length > 0);
  const effTxt = hasEff ? ` It performs ${formatEffects(f.effects)}` : '';
  if (!f.body) {
    return `To ${f.name}${params}, produce ${formatType(f.retType)}.${effTxt}`.trimEnd();
  }
  const header = hasEff
    ? `To ${f.name}${params}, produce ${formatType(f.retType)}.${effTxt}:`
    : `To ${f.name}${params}, produce ${formatType(f.retType)}:`;
  const body = formatBlock(f.body, 1);
  return `${header}\n${body}`;
}

function formatEffects(effs: readonly string[]): string {
  if (effs.length === 1) return effs[0]!;
  return effs.slice(0, -1).join(' and ') + ' and ' + effs[effs.length - 1];
}

function formatParams(ps: readonly Parameter[]): string {
  if (!ps || ps.length === 0) return '';
  const inner = ps.map(p => `${p.name}: ${formatType(p.type)}`);
  return ` with ${joinWithCommas(inner)}`;
}

function formatBlock(b: Block, lvl: number): string {
  const lines = b.statements.map(s => indent(lvl) + formatStmt(s, lvl));
  return lines.join('\n');
}

function formatStmt(s: Statement, lvl: number): string {
  switch (s.kind) {
    case 'Let':
      if ((s as any).expr && (s as any).expr.kind === 'Lambda') {
        const lam = (s as any).expr as any;
        const ps = (lam.params as any[]).map((p: any) => `${p.name}: ${formatType(p.type)}`).join(', ');
        const header = `Let ${s.name} be function with ${ps}, produce ${formatType(lam.retType)}:`;
        const body = formatBlock(lam.body, lvl + 1);
        return `${header}\n${body}`;
      }
      return `Let ${s.name} be ${formatExpr(s.expr)}.`;
    case 'Set':
      return `Set ${s.name} to ${formatExpr(s.expr)}.`;
    case 'Return':
      return `Return ${formatExpr(s.expr)}.`;
    case 'Start':
      return `Start ${s.name} as async ${formatExpr((s as any).expr)}.`;
    case 'Wait': {
      const names = (s as any).names as string[];
      const inner = names.length <= 2 ? names.join(' and ') : names.slice(0, -1).join(', ') + ' and ' + names[names.length - 1];
      return `Wait for ${inner}.`;
    }
    case 'If': {
      const head = `If ${formatExpr(s.cond)},:`;
      const thenB = '\n' + formatBlock(s.thenBlock, lvl + 1);
      const elseB = s.elseBlock ? `\n${indent(lvl)}Otherwise,:\n${formatBlock(s.elseBlock, lvl + 1)}` : '';
      return `${head}${thenB}${elseB}`;
    }
    case 'Match': {
      const head = `Match ${formatExpr(s.expr)}:`;
      const cases = s.cases
        .map(c => {
          const pat = formatPattern(c.pattern);
          if (c.body.kind === 'Return') return `${indent(lvl + 1)}When ${pat}, Return ${formatExpr(c.body.expr)}.`;
          return `${indent(lvl + 1)}When ${pat},:\n${formatBlock(c.body, lvl + 2)}`;
        })
        .join('\n');
      return `${head}\n${cases}`;
    }
    case 'Block':
      return formatBlock(s, lvl);
    default:
      // Allow expression statements (calls)
      return `${formatExpr(s as unknown as Expression)}.`;
  }
}

function formatPattern(p: any): string {
  switch (p.kind) {
    case 'PatternNull':
      return 'null';
    case 'PatternInt':
      return String(p.value);
    case 'PatternName':
      return p.name;
    case 'PatternCtor': {
      if (p.args && p.args.length > 0) return `${p.typeName}(${p.args.map(formatPattern).join(', ')})`;
      if (p.names && p.names.length > 0) return `${p.typeName}(${p.names.join(', ')})`;
      return p.typeName;
    }
    default:
      return '<pattern>';
  }
}

function formatExpr(e: Expression): string {
  switch (e.kind) {
    case 'Name':
      return e.name;
    case 'Bool':
      return e.value ? 'true' : 'false';
    case 'Null':
      return 'null';
    case 'Int':
      return String(e.value);
    case 'Long':
      return String(e.value) + 'L';
    case 'Double': {
      const v = e.value;
      if (Number.isFinite(v) && Math.floor(v) === v) return v.toFixed(1);
      return String(v);
    }
    case 'String':
      return JSON.stringify(e.value);
    case 'None':
      return 'none';
    case 'Ok':
      return `ok of ${formatExpr(e.expr)}`;
    case 'Err':
      return `err of ${formatExpr(e.expr)}`;
    case 'Some':
      return `some of ${formatExpr(e.expr)}`;
    case 'Construct':
      return `${e.typeName} with ${e.fields.map(formatConstructField).join(', ')}`;
    case 'Call': {
      const t = e.target;
      const target = t.kind === 'Name' ? t.name : `(${formatExpr(t)})`;
      const args = e.args.map(formatExpr).join(', ');
      return `${target}(${args})`;
    }
    case 'Lambda': {
      const ps = e.params.map(p => `${p.name}: ${formatType(p.type)}`).join(', ');
      return `function with ${ps}, produce ${formatType(e.retType)}:\n${formatBlock(e.body, 1)}`;
    }
    default:
      return '<expr>';
  }
}

function formatConstructField(f: ConstructField): string {
  return `${f.name} = ${formatExpr(f.expr)}`;
}

function formatType(t: Type): string {
  switch (t.kind) {
    case 'TypeName':
      return t.name;
    case 'Maybe':
      return `${formatType(t.type)}?`;
    case 'Option':
      return `Option of ${formatType(t.type)}`;
    case 'Result':
      return `Result of ${formatType(t.ok)} and ${formatType(t.err)}`;
    case 'List':
      return `List of ${formatType(t.type)}`;
    case 'Map':
      return `Map ${formatType(t.key)} to ${formatType(t.val)}`;
    case 'TypeApp':
      return `${t.base} of ${t.args.map(formatType).join(', ')}`;
    case 'TypeVar':
      return t.name;
    case 'FuncType':
      return `(${t.params.map(formatType).join(', ')}) -> ${formatType(t.ret)}`;
    default:
      return '<type>';
  }
}
