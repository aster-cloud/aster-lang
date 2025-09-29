#!/usr/bin/env node
import cp from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { canonicalize } from '../src/canonicalizer.js';
import { lex } from '../src/lexer.js';
import { parse } from '../src/parser.js';
import { lowerModule } from '../src/lower_to_core.js';

function envWithGradle(): Record<string, string | undefined> {
  return {
    GRADLE_USER_HOME: path.resolve('build/.gradle'),
    GRADLE_OPTS: `${process.env.GRADLE_OPTS ?? ''} -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Stack=false`.trim(),
    JAVA_OPTS: `${process.env.JAVA_OPTS ?? ''} -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Stack=false`.trim(),
    ...process.env,
  };
}

async function main(): Promise<void> {
  // Ensure emitter built (use gradle if no wrapper)
  const hasWrapper = fs.existsSync('./gradlew');
  const buildDir = 'aster-asm-emitter/build/libs';
  if (
    !fs.existsSync(buildDir) ||
    fs.readdirSync(buildDir).filter(f => f.endsWith('.jar')).length === 0
  ) {
    const buildCmd = hasWrapper
      ? ['./gradlew', '-g', 'build/.gradle', ':aster-asm-emitter:build']
      : ['gradle', '-g', 'build/.gradle', ':aster-asm-emitter:build'];
    try {
      cp.execFileSync(buildCmd[0]!, buildCmd.slice(1), { stdio: 'inherit', env: envWithGradle() });
    } catch (e) {
      console.error('Failed to build ASM emitter:', e);
      process.exit(1);
    }
  }
  const jars = fs.readdirSync(buildDir).filter(f => f.endsWith('.jar'));
  if (jars.length === 0) {
    console.error('Emitter jar not found in', buildDir);
    process.exit(2);
  }

  const inputs = process.argv.slice(2);
  if (inputs.length === 0) {
    console.error('Usage: emit-classfiles <file.cnl> [more.cnl ...]');
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

  for (const input of inputs) {
    const src = fs.readFileSync(input, 'utf8');
    const core = lowerModule(parse(lex(canonicalize(src))));
    const payload = JSON.stringify(core);
    fs.writeFileSync('build/last-core.json', payload);

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
  console.log('Emitted classes to build/jvm-classes');
}

main().catch(e => {
  console.error('emit-classfiles failed:', e);
  process.exit(1);
});
