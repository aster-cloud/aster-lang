#!/usr/bin/env node
import fs from 'node:fs';
import * as cp from 'node:child_process';
import * as path from 'node:path';
import { canonicalize, lex, lowerModule } from '../src/index.js';
import { parse as parseAst } from '../src/parser.js';
import { emitJava } from '../src/jvm/emitter.js';
import { DiagnosticError, formatDiagnostic } from '../src/diagnostics.js';

type Cmd = 'parse' | 'core' | 'jvm' | 'class' | 'jar' | 'truffle' | 'help';

function usage(code = 2): never {
  console.error(
    `Usage: aster <command> [options]\n\nCommands:\n` +
      `  parse <file.aster> [--watch]     Parse CNL → AST (JSON)\n` +
      `  core <file.aster> [--watch]      Lower AST → Core IR (JSON)\n` +
      `  jvm <file.aster> [--out DIR] [--watch]    Emit Java sources (default build/jvm-src)\n` +
      `  class <file.aster> [--out DIR] [--watch]  Emit .class files (default build/jvm-classes)\n` +
      `  jar [<file.aster>] [--out FILE]  Create JAR from classes; if file given, build classes first\n` +
      `  truffle <file.(aster|json)> [-- args...]  Run Core IR on Truffle (auto-lower .aster to JSON)\n` +
      `  help                     Show this help\n`
  );
  process.exit(code);
}

function readFileStrict(file: string): string {
  return fs.readFileSync(file, 'utf8');
}

function sh(cmd: string, opts: cp.ExecSyncOptions = {}): void {
  cp.execSync(cmd, { stdio: 'inherit', ...opts });
}

async function cmdParse(file: string): Promise<void> {
  const input = readFileStrict(file);
  const can = canonicalize(input);
  const toks = lex(can);
  const ast = parseAst(toks);
  console.log(JSON.stringify(ast, null, 2));
}

async function cmdCore(file: string): Promise<void> {
  const input = readFileStrict(file);
  const core = lowerModule(parseAst(lex(canonicalize(input))));
  console.log(JSON.stringify(core, null, 2));
}

async function cmdJvm(file: string, outDir = 'build/jvm-src'): Promise<void> {
  const input = readFileStrict(file);
  const core = lowerModule(parseAst(lex(canonicalize(input))));
  fs.rmSync(outDir, { recursive: true, force: true });
  await emitJava(core, outDir);
  console.log('Wrote Java sources to', outDir);
}

async function ensureAsmEmitterBuilt(): Promise<void> {
  const hasWrapper = fs.existsSync('./gradlew');
  const buildDir = 'aster-asm-emitter/build/libs';
  const hasJar = fs.existsSync(buildDir) && fs.readdirSync(buildDir).some(f => f.endsWith('.jar'));
  if (!hasJar) {
    const buildCmd = hasWrapper
      ? './gradlew :aster-asm-emitter:build'
      : 'gradle :aster-asm-emitter:build';
    try {
      sh(buildCmd, {
        env: {
          GRADLE_USER_HOME: path.resolve('build/.gradle'),
          GRADLE_OPTS: `${process.env.GRADLE_OPTS ?? ''} -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Stack=false`.trim(),
          JAVA_OPTS: `${process.env.JAVA_OPTS ?? ''} -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Stack=false`.trim(),
          ...process.env,
        },
      });
    } catch (e) {
      console.error('Failed to build ASM emitter');
      throw e;
    }
  }
}

async function cmdClass(file: string, outDir = 'build/jvm-classes'): Promise<void> {
  await ensureAsmEmitterBuilt();
  const input = readFileStrict(file);
  const core = lowerModule(parseAst(lex(canonicalize(input))));
  const payload = JSON.stringify(core);
  fs.mkdirSync('build', { recursive: true });
  fs.writeFileSync('build/last-core.json', payload);
  const runCmd = fs.existsSync('./gradlew') ? './gradlew' : 'gradle';
  await new Promise<void>((resolve, reject) => {
    const env = {
      GRADLE_USER_HOME: path.resolve('build/.gradle'),
      GRADLE_OPTS: `${process.env.GRADLE_OPTS ?? ''} -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Stack=false`.trim(),
      JAVA_OPTS: `${process.env.JAVA_OPTS ?? ''} -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Stack=false`.trim(),
      ...process.env,
    };
    const proc = cp.spawn(runCmd, [':aster-asm-emitter:run', `--args=${path.resolve(outDir)}`], {
      stdio: ['pipe', 'inherit', 'inherit'],
      env,
    });
    proc.on('error', reject);
    proc.on('close', code =>
      code === 0 ? resolve() : reject(new Error(`emitter exited ${code}`))
    );
    proc.stdin.write(payload);
    proc.stdin.end();
  });
  console.log('Emitted classes to', outDir);
}

async function cmdJar(
  optionalFile: string | undefined,
  outFile = 'build/aster-out/aster.jar'
): Promise<void> {
  if (optionalFile) {
    await cmdClass(optionalFile);
  }
  const classes = 'build/jvm-classes';
  if (!fs.existsSync(classes)) {
    console.error('No classes found:', classes);
    process.exit(2);
  }
  fs.mkdirSync(path.dirname(outFile), { recursive: true });
  sh(`jar --create --file ${outFile} -C ${classes} .`);
  console.log('Wrote', outFile);
}

async function cmdTruffle(input: string, passthrough: string[]): Promise<void> {
  // Prepare Core JSON path
  let corePath = input;
  if (input.endsWith('.aster')) {
    const src = readFileStrict(input);
    const core = lowerModule(parseAst(lex(canonicalize(src))));
    fs.mkdirSync('build', { recursive: true });
    corePath = path.join('build', `${path.basename(input, '.aster')}_core.json`);
    fs.writeFileSync(corePath, JSON.stringify(core));
  }
  // Run Truffle runner via Gradle
  const hasWrapper = fs.existsSync('./gradlew');
  const runCmd = hasWrapper ? './gradlew' : 'gradle';
  // Handle --profile flag: set env var
  const pass = [...passthrough];
  const profileIdx = pass.indexOf('--profile');
  const env: Record<string, string | undefined> = { ...process.env };
  if (profileIdx >= 0) {
    pass.splice(profileIdx, 1);
    env.ASTER_TRUFFLE_PROFILE = '1';
  }
  const argsStr = [corePath, ...pass].join(' ');
  await new Promise<void>((resolve, reject) => {
    const env2 = {
      GRADLE_USER_HOME: path.resolve('build/.gradle'),
      GRADLE_OPTS: `${process.env.GRADLE_OPTS ?? ''} -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Stack=false`.trim(),
      ...env,
    };
    const proc = cp.spawn(runCmd, [':aster-truffle:run', `--args=${argsStr}`], {
      stdio: 'inherit',
      env: env2,
    });
    proc.on('error', reject);
    proc.on('close', code =>
      code === 0 ? resolve() : reject(new Error(`truffle exited ${code}`))
    );
  });
}

function parseArgs(argv: string[]): {
  cmd: Cmd;
  args: string[];
  opts: Record<string, string | boolean>;
} {
  const [cmdRaw, ...rest] = argv;
  const cmd: Cmd = (cmdRaw as Cmd) ?? 'help';
  const args: string[] = [];
  const opts: Record<string, string | boolean> = {};
  for (let i = 0; i < rest.length; i++) {
    const t = rest[i]!;
    if (t === '--') {
      args.push(...rest.slice(i + 1));
      break;
    }
    if (t.startsWith('--')) {
      const key = t.slice(2);
      const nxt = rest[i + 1];
      if (nxt && !nxt.startsWith('--')) {
        opts[key] = nxt;
        i++;
      } else {
        opts[key] = true;
      }
      continue;
    }
    args.push(t);
  }
  return { cmd, args, opts };
}

async function main(): Promise<void> {
  const { cmd, args, opts } = parseArgs(process.argv.slice(2));
  const watch = Boolean(opts['watch']);
  try {
    switch (cmd) {
      case 'parse': {
        const file = args[0];
        if (!file) usage();
        const run: () => Promise<void> = async () => {
          await cmdParse(file).catch(err => console.error(err));
        };
        await run();
        if (watch) await watchFile(file, run);
        break;
      }
      case 'core': {
        const file = args[0];
        if (!file) usage();
        const run: () => Promise<void> = async () => {
          await cmdCore(file).catch(err => console.error(err));
        };
        await run();
        if (watch) await watchFile(file, run);
        break;
      }
      case 'jvm': {
        const file = args[0];
        if (!file) usage();
        const out = (opts['out'] as string) || 'build/jvm-src';
        const run: () => Promise<void> = async () => {
          await cmdJvm(file, out).catch(err => console.error(err));
        };
        await run();
        if (watch) await watchFile(file, run);
        break;
      }
      case 'class': {
        const file = args[0];
        if (!file) usage();
        const out = (opts['out'] as string) || 'build/jvm-classes';
        const run: () => Promise<void> = async () => {
          await cmdClass(file, out).catch(err => console.error(err));
        };
        await run();
        if (watch) await watchFile(file, run);
        break;
      }
      case 'jar': {
        const file = args[0];
        const out = (opts['out'] as string) || 'build/aster-out/aster.jar';
        await cmdJar(file, out);
        break;
      }
      case 'truffle': {
        const file = args[0];
        if (!file) usage();
        const pass = args.slice(1);
        await cmdTruffle(file, pass);
        break;
      }
      case 'help':
      default:
        usage(0);
    }
  } catch (e: unknown) {
    if (e instanceof DiagnosticError) {
      const msg = formatDiagnostic(e.diagnostic, '');
      console.error(msg);
    } else if (typeof e === 'object' && e && 'message' in e) {
      const emsg = (e as { message?: unknown }).message;
      console.error(String(emsg));
    } else {
      console.error('Unknown error');
    }
    process.exit(1);
  }
}

main();

async function watchFile(file: string, run: () => void | Promise<void>): Promise<void> {
  console.log('Watching', file, '(Ctrl+C to exit)');
  let timeout: ReturnType<typeof setTimeout> | null = null;
  const trigger: () => void = () => {
    if (timeout) clearTimeout(timeout);
    timeout = setTimeout(() => {
      console.log('— change detected —');
      Promise.resolve(run()).catch(err => console.error(err));
    }, 100);
  };
  try {
    fs.watch(file, { persistent: true }, trigger);
  } catch (e) {
    console.error('Failed to watch file:', file, e);
    process.exit(1);
  }
  // Keep process alive indefinitely
  await new Promise<void>(resolve => void resolve);
}
