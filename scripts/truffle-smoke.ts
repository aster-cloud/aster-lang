#!/usr/bin/env node
import cp from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

function usage(): never {
  console.error('Usage: truffle-smoke <core.json> <expected>');
  process.exit(2);
}

const [, , corePath, expectedRaw] = process.argv;
if (!corePath || !expectedRaw) usage();

const expected = String(expectedRaw).trim();
const cwd = process.cwd();
const abs = path.resolve(corePath);
if (!fs.existsSync(abs)) {
  console.error(`File not found: ${abs}`);
  process.exit(2);
}

const hasWrapper = fs.existsSync(path.join(cwd, 'gradlew'));
const runCmd = hasWrapper ? (process.platform === 'win32' ? 'gradlew.bat' : './gradlew') : 'gradle';
const offlineArgs = ['--no-daemon', '--offline', '--console=plain', ':truffle:run', `--args=${abs}`];
const onlineArgs = ['--no-daemon', '--console=plain', ':truffle:run', `--args=${abs}`];

const env: Record<string, string | undefined> = {
  GRADLE_USER_HOME: path.resolve('build/.gradle'),
  GRADLE_OPTS: `${process.env.GRADLE_OPTS ?? ''} -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Stack=false`.trim(),
  JAVA_OPTS: `${process.env.JAVA_OPTS ?? ''} -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Stack=false`.trim(),
  ...process.env,
};
async function runOnce(gradleArgs: string[]): Promise<{ code: number; out: string; err: string }>{
  return new Promise(resolve => {
    const p = cp.spawn(runCmd, gradleArgs, { stdio: ['ignore', 'pipe', 'pipe'], env });
    let _out = '';
    let _err = '';
    p.stdout.setEncoding('utf8');
    p.stderr.setEncoding('utf8');
    p.stdout.on('data', chunk => { _out += chunk; });
    p.stderr.on('data', chunk => { _err += chunk; });
    p.on('error', e => resolve({ code: 1, out: _out, err: _err + String((e as Error).message || '') }));
    p.on('close', code => resolve({ code: code ?? 1, out: _out, err: _err }));
  });
}

async function main(): Promise<void> {
  let res = await runOnce(offlineArgs);
  if (res.code !== 0) {
    res = await runOnce(onlineArgs);
  }
  if (res.code !== 0) {
    console.error(`Gradle exited with code ${res.code}`);
    if (res.err) process.stderr.write(res.err);
    process.exit(res.code);
  }
  const merged = (res.out + '\n' + res.err).toString();
  const lines = merged.trim().split(/\r?\n/).map(s => s.trim()).filter(Boolean);
  const lastNumeric = [...lines].reverse().find(l => /^-?\d+(\.\d+)?$/.test(l));
  const actual = (lastNumeric ?? lines[lines.length - 1] ?? '').trim();
  if (actual === expected) {
    console.log(`OK: ${path.basename(corePath as string)} => ${expected}`);
    process.exit(0);
  } else {
    console.error('FAIL: Truffle output did not match');
    console.error('Expected:', expected);
    console.error('Actual  :', actual);
    if (res.err) {
      console.error('--- stderr ---');
      process.stderr.write(res.err);
    }
    process.exit(1);
  }
}

main().catch(e => {
  console.error('Smoke harness error:', (e as Error).message);
  process.exit(1);
});
