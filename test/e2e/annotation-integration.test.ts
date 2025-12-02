import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

type DiagnosticLike = {
  readonly code?: string;
  readonly severity?: string;
  readonly message?: string;
};

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, '..', '..', '..');
const sourceFile = path.join(repoRoot, 'test/e2e/annotation-integration.aster');
const tsCli = path.join(repoRoot, 'dist/scripts/typecheck-cli.js');
const emitCoreScript = path.join(repoRoot, 'dist/scripts/emit-core.js');
const gradleExecutable = process.platform === 'win32' ? 'gradlew.bat' : './gradlew';
const javaCliBinary = path.join(
  repoRoot,
  'aster-core',
  'build',
  'install',
  'aster-core',
  'bin',
  process.platform === 'win32' ? 'aster-core.bat' : 'aster-core'
);
const javaHomeBin = 'java';
const moduleName = 'test.e2e.annotation_integration';

describe('端到端注解集成测试', () => {
  it('TypeScript和Java诊断一致性', () => {
    const tsDiagnostics = runTypescriptTypecheck(sourceFile);
    const javaDiagnostics = runJavaTypecheck(sourceFile);

    assert.deepEqual(
      normalizeDiagnostics(tsDiagnostics),
      normalizeDiagnostics(javaDiagnostics),
      'TypeScript 与 Java 类型检查诊断必须一致'
    );
  });

  it('Core IR包含正确的PII与Capability元数据', () => {
    const module = emitCoreModule(sourceFile);

    const getUserEmail = module.decls.find((decl: any) => decl.name === 'get_user_email');
    assert.ok(getUserEmail, '应存在 get_user_email 声明');
    assert.equal(getUserEmail.piiLevel, 'L2');
    assert.deepEqual(new Set(getUserEmail.piiCategories ?? []), new Set(['name', 'email']));
    assert.deepEqual(getUserEmail.effectCaps ?? [], ['Http']);

    const processSensitive = module.decls.find((decl: any) => decl.name === 'process_sensitive_data');
    assert.ok(processSensitive, '应存在 process_sensitive_data 声明');
    assert.equal(processSensitive.piiLevel, 'L3');
    assert.deepEqual(new Set(processSensitive.piiCategories ?? []), new Set(['ssn', 'email']));
    assert.deepEqual(new Set(processSensitive.effectCaps ?? []), new Set(['Http', 'Sql']));
  });

  it('生成的.class文件包含JVM注解', () => {
    compileAsterSource(sourceFile);
    const verification = runAnnotationVerifier(moduleName);

    assert.match(
      verification,
      /@AsterPii\(level="L2", categories=\[name,\s*email\]\)/,
      'get_user_email 应携带 L2 PII 注解'
    );
    assert.match(
      verification,
      /@AsterCapability\(effects=\[io\], capabilities=\[Http\]\)/,
      'get_user_email 应声明 Http 能力'
    );
    assert.match(
      verification,
      /@AsterPii\(level="L3", categories=\[ssn,\s*email\]\)/,
      'process_sensitive_data 应聚合 email 与 ssn'
    );
    assert.match(
      verification,
      /@AsterCapability\(effects=\[io\], capabilities=\[Http,\s*Sql\]\)/,
      'process_sensitive_data 应声明 Http/Sql 能力'
    );
  });

  it('PII诊断跨栈一致', () => {
    const tsDiagnostics = runTypescriptPiiCheck(sourceFile);
    const javaDiagnostics = runJavaPiiCheck(sourceFile);

    assert.deepEqual(
      normalizeDiagnostics(tsDiagnostics),
      normalizeDiagnostics(javaDiagnostics),
      'PII 诊断必须保持跨栈一致'
    );
  });

  it('Capability诊断跨栈一致', () => {
    const tsDiagnostics = runTypescriptCapabilityCheck(sourceFile);
    const javaDiagnostics = runJavaCapabilityCheck(sourceFile);

    const expectedErrors = ['E303', 'E302'];
    for (const code of expectedErrors) {
      assert.ok(
        tsDiagnostics.some((diag: DiagnosticLike) => diag.code === code),
        `TypeScript 应产出 ${code}`
      );
      assert.ok(
        javaDiagnostics.some((diag: DiagnosticLike) => diag.code === code),
        `Java 应产出 ${code}`
      );
    }
  });
});

function runTypescriptTypecheck(file: string, extraEnv?: NodeJS.ProcessEnv) {
  ensureTypescriptCli();
  const payload = execNodeScript(tsCli, [file], extraEnv);
  return payload.diagnostics ?? [];
}

function runJavaTypecheck(file: string, extraEnv?: NodeJS.ProcessEnv) {
  ensureJavaCli();
  const payload = execFileSync(javaCliBinary, ['typecheck', file], {
    cwd: repoRoot,
    env: { ...process.env, ...extraEnv },
    encoding: 'utf8',
  });
  return JSON.parse(payload).diagnostics ?? [];
}

function runTypescriptPiiCheck(file: string) {
  return runTypescriptTypecheck(file, { ENFORCE_PII: 'true' });
}

function runJavaPiiCheck(file: string) {
  return runJavaTypecheck(file, { ENFORCE_PII: 'true' });
}

function runTypescriptCapabilityCheck(file: string) {
  return runTypescriptTypecheck(file);
}

function runJavaCapabilityCheck(file: string) {
  return runJavaTypecheck(file);
}

function emitCoreModule(file: string) {
  ensureTypescriptCli();
  const output = execNodeScript(emitCoreScript, [file]);
  return output;
}

function compileAsterSource(file: string) {
  const args = ['--no-configuration-cache', ':aster-asm-emitter:compileAster', `-Paster.source=${file}`];
  execFileSync(path.join(repoRoot, gradleExecutable), args, {
    cwd: repoRoot,
    stdio: 'inherit',
  });
}

function runAnnotationVerifier(module: string) {
  const classDir = path.join(repoRoot, 'aster-asm-emitter/build/jvm-classes');
  const emitterMain = path.join(repoRoot, 'aster-asm-emitter/build/classes/java/main');
  const emitterTest = path.join(repoRoot, 'aster-asm-emitter/build/classes/java/test');
  const runtimeJar = resolveRuntimeJar();
  const classpath = [classDir, emitterTest, emitterMain, runtimeJar]
    .filter(Boolean)
    .join(path.delimiter);

  const output = execFileSync(
    javaHomeBin,
    ['-cp', classpath, 'aster.emitter.test.AnnotationVerifier', module, classDir],
    { cwd: repoRoot, encoding: 'utf8' }
  );
  return output;
}

function resolveRuntimeJar(): string {
  const runtimeLibDir = path.join(repoRoot, 'aster-runtime/build/libs');
  if (!fs.existsSync(runtimeLibDir)) {
    execFileSync(path.join(repoRoot, gradleExecutable), [':aster-runtime:jar'], {
      cwd: repoRoot,
      stdio: 'inherit',
    });
  }
  const files = fs.readdirSync(runtimeLibDir).filter(name => name.endsWith('.jar'));
  if (files.length === 0) {
    throw new Error('未找到 aster-runtime JAR，请先执行 ./gradlew :aster-runtime:jar');
  }
  return path.join(runtimeLibDir, files[0]!);
}

function ensureTypescriptCli(): void {
  if (fs.existsSync(tsCli)) {
    return;
  }
  execFileSync('npm', ['run', 'build'], { cwd: repoRoot, stdio: 'inherit' });
}

function ensureJavaCli(): void {
  if (fs.existsSync(javaCliBinary)) {
    return;
  }
  execFileSync(path.join(repoRoot, gradleExecutable), [':aster-core:installDist'], {
    cwd: repoRoot,
    stdio: 'inherit',
  });
}

function execNodeScript(script: string, args: string[], extraEnv?: NodeJS.ProcessEnv) {
  const output = execFileSync(process.execPath, [script, ...args], {
    cwd: repoRoot,
    env: { ...process.env, ...extraEnv },
    encoding: 'utf8',
  });
  return JSON.parse(output);
}

function normalizeDiagnostics(diags: readonly DiagnosticLike[]) {
  const ignored = new Set(['E205', 'E207']);
  return diags
    .filter(diag => !ignored.has((diag.code ?? '').toUpperCase()))
    .map(diag => ({
      code: (diag.code ?? '').toUpperCase(),
      severity: (diag.severity ?? '').toLowerCase(),
    }))
    .sort((a, b) => a.code.localeCompare(b.code));
}
