#!/usr/bin/env node
import cp from 'node:child_process';
import fs from 'node:fs';

function shCapture(cmd: string): string {
  return cp.execSync(cmd, { stdio: ['ignore', 'pipe', 'pipe'] }).toString('utf8');
}

function findClass(): string {
  const root = 'build/jvm-classes';
  const prefer = root + '/demo/int_match_default/classifyDefault_fn.class';
  if (fs.existsSync(prefer)) return prefer;
  const stack: string[] = [root];
  while (stack.length) {
    const d = stack.pop()!;
    for (const ent of fs.readdirSync(d)) {
      const p = d + '/' + ent;
      const st = fs.statSync(p);
      if (st.isDirectory()) stack.push(p);
      else if (st.isFile() && p.endsWith('classifyDefault_fn.class')) return p;
    }
  }
  throw new Error('classifyDefault_fn.class not found under ' + root);
}

function extractMethod(body: string, methodName: string): string | null {
  const lines = body.split(/\r?\n/);
  const startIdx = lines.findIndex(l => (l ?? '').includes('static') && (l ?? '').includes(methodName + '('));
  if (startIdx === -1) return null;
  // Find next likely method signature or end of file
  let endIdx = lines.length;
  for (let i = startIdx + 1; i < lines.length; i++) {
    const li = lines[i] ?? '';
    if (/^(\s)*\w.*\(/.test(li) && li.includes('static') && i - startIdx > 3) { endIdx = i; break; }
  }
  return lines.slice(startIdx, endIdx).join('\n');
}

function main(): void {
  const cls = findClass();
  const out = shCapture(`javap -c -p '${cls}'`);
  const method = extractMethod(out, 'classifyDefault');
  if (!method) {
    console.error('Could not locate method classifyDefault in javap output');
    process.exit(1);
  }
  if (!(method.includes('tableswitch') || method.includes('lookupswitch'))) {
    console.error('Expected tableswitch/lookupswitch in classifyDefault');
    process.exit(1);
  }
  const full = process.argv.includes('--full');
  if (full) {
    console.log('--- classifyDefault (full javap) ---');
    console.log(method);
    console.log('------------------------------------');
    console.log('javap scan passed (full): switch present and default inline heuristic will be checked below');
  }
  // Heuristic: ensure default block is not immediately an unconditional goto
  const mLines = method.split(/\r?\n/);
  let okDefaultInline = true;
  let idxSwitch = -1;
  for (let i = 0; i < mLines.length; i++) {
    const line = (mLines[i] ?? '').trim();
    if (idxSwitch === -1 && (line.includes('tableswitch') || line.includes('lookupswitch'))) idxSwitch = i;
    if (line.startsWith('default:')) {
      // Check next few non-empty lines
      for (let j = i + 1; j < Math.min(i + 5, mLines.length); j++) {
        const t = (mLines[j] ?? '').trim();
        if (!t) continue;
        if (/^goto\s+/.test(t)) okDefaultInline = false;
        break;
      }
    }
  }
  if (!okDefaultInline) {
    console.error('Default block appears to immediately jump to a shared label (unexpected)');
    console.error(method);
    process.exit(1);
  }
  if (!full) {
    // Print a concise snippet around the switch and default for visual confirmation
    let idxDefault = -1;
    for (let i = 0; i < mLines.length; i++) {
      const line = (mLines[i] ?? '').trim();
      if (line.startsWith('default:')) { idxDefault = i; break; }
    }
    const start = Math.max(0, (idxSwitch !== -1 ? idxSwitch : 0) - 5);
    const end = Math.min(mLines.length, (idxDefault !== -1 ? idxDefault + 8 : (idxSwitch !== -1 ? idxSwitch + 25 : start + 25)));
    const snippet = mLines.slice(start, end).join('\n');
    console.log('--- classifyDefault (javap snippet) ---');
    console.log(snippet);
    console.log('---------------------------------------');
  }
  console.log('javap scan passed: classifyDefault uses switch with inline default body');
}

main();
