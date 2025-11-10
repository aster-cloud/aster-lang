#!/usr/bin/env node
import cp from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { canonicalize } from '../src/canonicalizer.js';
import { lex } from '../src/lexer.js';
import { parse } from '../src/parser.js';
import { lowerModule } from '../src/lower_to_core.js';
import { emitJava } from '../src/jvm/emitter.js';
import type { Core as CoreIR } from '../src/types.js';

function envWithGradle(): Record<string, string | undefined> {
  return {
    GRADLE_USER_HOME: path.resolve('build/.gradle'),
    GRADLE_OPTS: `${process.env.GRADLE_OPTS ?? ''} -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Stack=false`.trim(),
    JAVA_OPTS: `${process.env.JAVA_OPTS ?? ''} -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Stack=false`.trim(),
    ...process.env,
  };
}

const JVM_SRC_DIR = path.resolve('build/jvm-src');
const JAVA_DEP_SOURCE_DIRS = [path.resolve('aster-runtime/src/main/java')];
const WORKFLOW_RUNTIME_FILES = [
  path.resolve('aster-truffle/src/main/java/aster/truffle/runtime/AsyncTaskRegistry.java'),
  path.resolve('aster-truffle/src/main/java/aster/truffle/runtime/DependencyGraph.java'),
  path.resolve('aster-truffle/src/main/java/aster/truffle/runtime/WorkflowScheduler.java'),
];

function ensureJar(
  hasWrapper: boolean,
  jarDir: string,
  gradleTask: string,
  label: string
): void {
  const hasJar =
    fs.existsSync(jarDir) && fs.readdirSync(jarDir).some(f => f.endsWith('.jar'));
  if (hasJar) return;
  const buildCmd = hasWrapper
    ? ['./gradlew', '-g', 'build/.gradle', gradleTask]
    : ['gradle', '-g', 'build/.gradle', gradleTask];
  try {
    cp.execFileSync(buildCmd[0]!, buildCmd.slice(1), {
      stdio: 'inherit',
      env: envWithGradle(),
    });
  } catch (e) {
    console.error(`Failed to build ${label}:`, e);
    process.exit(1);
  }
  const jars = fs.readdirSync(jarDir).filter(f => f.endsWith('.jar'));
  if (jars.length === 0) {
    console.error(`${label} jar not found in`, jarDir);
    process.exit(2);
  }
}

function containsWorkflow(core: CoreIR.Module): boolean {
  return core.decls.some(
    decl => decl.kind === 'Func' && blockHasWorkflow(decl.body)
  );
}

function blockHasWorkflow(block: CoreIR.Block): boolean {
  return block.statements.some(statementHasWorkflow);
}

function statementHasWorkflow(stmt: CoreIR.Statement): boolean {
  switch (stmt.kind) {
    case 'workflow':
      return true;
    case 'Scope':
      return stmt.statements.some(statementHasWorkflow);
    case 'If':
      return (
        blockHasWorkflow(stmt.thenBlock) ||
        (stmt.elseBlock ? blockHasWorkflow(stmt.elseBlock) : false)
      );
    case 'Match':
      return stmt.cases.some(c =>
        c.body.kind === 'Return' ? false : blockHasWorkflow(c.body)
      );
    default:
      return false;
  }
}

function collectJavaSources(dir: string): string[] {
  if (!fs.existsSync(dir)) return [];
  const result: string[] = [];
  const stack: string[] = [dir];
  while (stack.length > 0) {
    const current = stack.pop()!;
    const stat = fs.statSync(current);
    if (stat.isDirectory()) {
      for (const entry of fs.readdirSync(current)) {
        stack.push(path.join(current, entry));
      }
    } else if (current.endsWith('.java')) {
      result.push(current);
    }
  }
  return result;
}

async function compileWorkflowSources(outDir: string): Promise<void> {
  const dependencySources = JAVA_DEP_SOURCE_DIRS.flatMap(collectJavaSources);
  const generatedSources = collectJavaSources(JVM_SRC_DIR);
  if (generatedSources.length === 0) {
    console.warn('[emit-classfiles] 未找到 workflow Java 源文件，跳过 javac。');
    return;
  }
  const workflowRuntimeSources = WORKFLOW_RUNTIME_FILES.filter(f => fs.existsSync(f));
  const javacArgs = [
    '--release',
    '21',
    '-g',
    '-d',
    outDir,
    ...dependencySources,
    ...workflowRuntimeSources,
    ...generatedSources,
  ];
  await new Promise<void>((resolve, reject) => {
    const proc = cp.spawn('javac', javacArgs, { stdio: 'inherit' });
    proc.on('error', reject);
    proc.on('close', code =>
      code === 0 ? resolve() : reject(new Error(`javac exited ${code}`))
    );
  });
}

async function emitWorkflowModules(
  modules: readonly { core: CoreIR.Module; input: string }[],
  outDir: string
): Promise<void> {
  if (modules.length === 0) return;
  console.log(
    `[emit-classfiles] 检测到 ${modules.length} 个 workflow 模块，切换到 TypeScript JVM emitter`
  );
  fs.rmSync(JVM_SRC_DIR, { recursive: true, force: true });
  fs.mkdirSync(JVM_SRC_DIR, { recursive: true });
  for (const { core, input } of modules) {
    console.log(`[emit-classfiles] 生成 workflow Java 源码：${input}`);
    await emitJava(core, JVM_SRC_DIR);
  }
  await compileWorkflowSources(outDir);
}

async function main(): Promise<void> {
  const hasWrapper = fs.existsSync('./gradlew');
  // 确保 ASM emitter Jar 就绪（legacy 路径仍依赖）
  ensureJar(hasWrapper, 'aster-asm-emitter/build/libs', ':aster-asm-emitter:build', 'ASM emitter');

  const inputs = process.argv.slice(2);
  if (inputs.length === 0) {
    console.error('Usage: emit-classfiles <file.aster> [more.aster ...]');
    process.exit(2);
  }

  // Prefer running via Gradle run to get classpath deps available
  const runCmd = hasWrapper ? './gradlew' : 'gradle';
  const outDir = path.resolve('build/jvm-classes');

  fs.mkdirSync('build', { recursive: true });
  // Clean output dir once to avoid stale classes
  if (fs.existsSync(outDir)) {
    fs.rmSync(outDir, { recursive: true, force: true });
  }

  const workflowModules: { core: CoreIR.Module; input: string }[] = [];

  for (const input of inputs) {
    const src = fs.readFileSync(input, 'utf8');
    const core = lowerModule(parse(lex(canonicalize(src))));
    const payload = JSON.stringify(core);
    fs.writeFileSync('build/last-core.json', payload);

    if (containsWorkflow(core)) {
      workflowModules.push({ core, input });
      continue;
    }

    await new Promise<void>((resolve, reject) => {
      const proc = cp.spawn(
        runCmd,
        ['-g', 'build/.gradle', ':aster-asm-emitter:run', `--args=${outDir}`],
        {
          stdio: ['pipe', 'inherit', 'inherit'],
          env: { ...envWithGradle(), ASTER_ROOT: process.cwd() },
        }
      );
      proc.on('error', reject);
      proc.on('close', code =>
        code === 0 ? resolve() : reject(new Error(`emitter exited ${code}`))
      );
      proc.stdin.write(payload);
      proc.stdin.end();
    });
  }
  if (workflowModules.length > 0) {
    await emitWorkflowModules(workflowModules, outDir);
  }
  console.log('Emitted classes to build/jvm-classes');
}

main().catch(e => {
  console.error('emit-classfiles failed:', e);
  process.exit(1);
});
