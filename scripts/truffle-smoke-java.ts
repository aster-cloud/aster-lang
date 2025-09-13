#!/usr/bin/env node
import cp from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

function usage(): never {
  console.error('Usage: truffle-smoke-java <core.json> <expected>');
  process.exit(2);
}

const [, , corePath, expectedRaw] = process.argv;
if (!corePath || !expectedRaw) usage();
const expected = String(expectedRaw).trim();
const absCore = path.resolve(corePath);
if (!fs.existsSync(absCore)) {
  console.error(`File not found: ${absCore}`);
  process.exit(2);
}

// Build classpath from compiled classes and Gradle cache jars
const repoRoot = process.cwd();
const trufBuild = path.join(repoRoot, 'truffle', 'build');
const classes = path.join(trufBuild, 'classes', 'java', 'main');
const resources = path.join(trufBuild, 'resources', 'main');

function findJarAnywhere(base: string, namePrefix: string): string | null {
  // Search breadth-first to keep it quick
  const q: string[] = [base];
  while (q.length) {
    const d = q.shift()!;
    let ents: fs.Dirent[] = [];
    try { ents = fs.readdirSync(d, { withFileTypes: true }); } catch { continue; }
    for (const e of ents) {
      const p = path.join(d, e.name);
      if (e.isDirectory()) q.push(p);
      else if (e.isFile() && e.name.startsWith(namePrefix) && e.name.endsWith('.jar')) return p;
    }
  }
  return null;
}

const gradleHome = process.env.GRADLE_USER_HOME || path.join(repoRoot, 'build', '.gradle') || path.join(os.homedir(), '.gradle');
const cacheRoot = path.join(gradleHome, 'caches', 'modules-2', 'files-2.1');

const required = [
  { prefix: 'truffle-api-24.1.1' },
  { prefix: 'jackson-annotations-2.17.2' },
  { prefix: 'jackson-core-2.17.2' },
  { prefix: 'jackson-databind-2.17.2' },
  { prefix: 'polyglot-24.1.1' },
  { prefix: 'collections-24.1.1' },
  { prefix: 'nativeimage-24.1.1' },
  { prefix: 'word-24.1.1' },
];

function buildIfNeeded(): void {
  const hasWrapper = fs.existsSync(path.join(repoRoot, 'gradlew'));
  const runCmd = hasWrapper ? (process.platform === 'win32' ? 'gradlew.bat' : './gradlew') : 'gradle';
  const env: Record<string, string | undefined> = {
    GRADLE_USER_HOME: gradleHome,
    GRADLE_OPTS: `${process.env.GRADLE_OPTS ?? ''} -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Stack=false`.trim(),
    JAVA_OPTS: `${process.env.JAVA_OPTS ?? ''} -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Stack=false`.trim(),
    ...process.env,
  };
  try {
    // Build classes and resolve dependencies; force re-run to pick up source changes
    cp.execFileSync(runCmd, [':truffle:classes', '--rerun-tasks', '--no-daemon', '--console=plain'], { stdio: 'inherit', env });
  } catch (e) {
    console.error('Failed to run Gradle to build Truffle classes.');
  }
}

if (!fs.existsSync(classes)) {
  buildIfNeeded();
}

const jars: string[] = [];
for (const r of required) {
  let p = findJarAnywhere(cacheRoot, r.prefix);
  if (!p) {
    buildIfNeeded();
    p = findJarAnywhere(cacheRoot, r.prefix);
  }
  if (!p) {
    console.error(`Missing dependency jar '${r.prefix}*.jar' in Gradle cache at ${cacheRoot}.`);
    console.error('Run once: ./gradlew :truffle:run to download dependencies.');
    process.exit(2);
  }
  jars.push(p);
}

const cpSep = process.platform === 'win32' ? ';' : ':';
const cpEntries = [classes, resources, ...jars];
const classpath = cpEntries.join(cpSep);

const java = process.env.JAVA_HOME
  ? path.join(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'java.exe' : 'java')
  : 'java';

const env = {
  GRADLE_USER_HOME: gradleHome,
  GRADLE_OPTS: `${process.env.GRADLE_OPTS ?? ''} -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Stack=false`.trim(),
  JAVA_OPTS: `${process.env.JAVA_OPTS ?? ''} -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Stack=false`.trim(),
  ASTER_TRUFFLE_DEBUG: process.env.ASTER_TRUFFLE_DEBUG ?? '1',
  ...process.env,
};

const args = ['-cp', classpath, 'aster.truffle.Runner', absCore];
const proc = cp.spawn(java, args, { stdio: ['ignore', 'pipe', 'pipe'], env });
let out = '';
let err = '';
proc.stdout.setEncoding('utf8');
proc.stderr.setEncoding('utf8');
proc.stdout.on('data', c => (out += c));
proc.stderr.on('data', c => (err += c));
proc.on('close', code => {
  if (code !== 0) {
    console.error(`Java exited with code ${code}`);
    if (err) process.stderr.write(err);
    process.exit(code ?? 1);
  }
  const merged = (out + '\n' + err).trim();
  const lines = merged.split(/\r?\n/).map(s => s.trim()).filter(Boolean);
  const lastNumeric = [...lines].reverse().find(l => /^-?\d+$/.test(l));
  const actual = (lastNumeric ?? lines[lines.length - 1] ?? '').trim();
  if (actual === expected) {
    console.log(`OK: ${path.basename(absCore)} => ${expected}`);
    process.exit(0);
  } else {
    console.error('FAIL: Java smoke output did not match');
    console.error('Expected:', expected);
    console.error('Actual  :', actual);
    if (err) {
      console.error('--- stderr ---');
      process.stderr.write(err);
    }
    process.exit(1);
  }
});
