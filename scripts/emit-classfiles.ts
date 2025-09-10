#!/usr/bin/env node
import cp from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { canonicalize } from '../src/canonicalizer.js';
import { lex } from '../src/lexer.js';
import { parse } from '../src/parser.js';
import { lowerModule } from '../src/lower_to_core.js';


function sh(cmd: string, opts: cp.ExecSyncOptions = {}): void { cp.execSync(cmd, { stdio: 'inherit', ...opts }); }

async function main(): Promise<void> {
  // Ensure emitter built (use gradle if no wrapper)
  const hasWrapper = fs.existsSync('./gradlew');
  const buildDir = 'aster-asm-emitter/build/libs';
  if (!fs.existsSync(buildDir) || fs.readdirSync(buildDir).filter(f => f.endsWith('.jar')).length === 0) {
    const buildCmd = hasWrapper ? './gradlew :aster-asm-emitter:build' : 'gradle :aster-asm-emitter:build';
    try { sh(buildCmd); } catch (e) { console.error('Failed to build ASM emitter:', e); process.exit(1); }
  }
  const jars = fs.readdirSync(buildDir).filter(f => f.endsWith('.jar'));
  if (jars.length === 0) { console.error('Emitter jar not found in', buildDir); process.exit(2); }

  const input = process.argv[2];
  if (!input) { console.error('Usage: emit-classfiles <file.cnl>'); process.exit(2); }
  const src = fs.readFileSync(input, 'utf8');
  const core = lowerModule(parse(lex(canonicalize(src))));
  const payload = JSON.stringify(core);

  fs.mkdirSync('build', { recursive: true });
  fs.writeFileSync('build/last-core.json', payload);

  // Prefer running via Gradle run to get classpath deps available
  const runCmd = hasWrapper ? './gradlew' : 'gradle';
  const outDir = path.resolve('build/jvm-classes');
  await new Promise<void>((resolve, reject) => {
    const proc = cp.spawn(runCmd, [':aster-asm-emitter:run', `--args=${outDir}`], { stdio: ['pipe', 'inherit', 'inherit'] });
    proc.on('error', reject);
    proc.on('close', code => code === 0 ? resolve() : reject(new Error(`emitter exited ${code}`)));
    proc.stdin.write(payload);
    proc.stdin.end();
  });
  console.log('Emitted classes to build/jvm-classes');
}

main().catch(e => { console.error('emit-classfiles failed:', e); process.exit(1); });
