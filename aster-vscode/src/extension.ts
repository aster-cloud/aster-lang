/**
 * Aster Language VSCode Extension
 *
 * 提供 Aster CNL 语言支持：
 * - 语法高亮（通过 TextMate grammar）
 * - LSP 集成（代码补全、跳转定义、错误检查等）
 * - 文件监视和自动同步
 * - 编译、调试、构建、打包命令
 */

import * as path from 'node:path';
import * as fs from 'node:fs';
import * as vscode from 'vscode';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import {
  LanguageClient,
  LanguageClientOptions,
  ServerOptions,
  TransportKind,
} from 'vscode-languageclient/node';

const execFileAsync = promisify(execFile);

let client: LanguageClient | null = null;

/**
 * 解析 LSP 服务器路径
 *
 * 从配置中读取服务器路径（相对于工作区根目录），
 * 默认为 'dist/src/lsp/server.js'
 */
function resolveServerPath(): string {
  const cfg = vscode.workspace.getConfiguration('aster');
  const rel = cfg.get<string>('langServer.path', 'dist/src/lsp/server.js');
  const ws = vscode.workspace.workspaceFolders?.[0];
  const root = ws ? ws.uri.fsPath : process.cwd();
  return path.resolve(root, rel);
}

/**
 * 启动 LSP 客户端
 */
function startClient(): void {
  const serverModule = resolveServerPath();

  // 检查服务器文件是否存在
  if (!fs.existsSync(serverModule)) {
    vscode.window.showErrorMessage(
      `Aster LSP 未找到: ${serverModule}。请先构建项目（npm run build）。`
    );
    return;
  }

  // LSP 服务器配置
  const serverOptions: ServerOptions = {
    run: { module: serverModule, transport: TransportKind.stdio },
    debug: { module: serverModule, transport: TransportKind.stdio },
  };

  // LSP 客户端配置
  const clientOptions: LanguageClientOptions = {
    documentSelector: [{ scheme: 'file', language: 'aster' }],
    synchronize: {
      // 监视工作区中所有 .aster 文件的变化
      fileEvents: vscode.workspace.createFileSystemWatcher('**/*.aster'),
    },
  };

  // 创建并启动 LSP 客户端
  client = new LanguageClient(
    'asterLangServer',
    'Aster Language Server',
    serverOptions,
    clientOptions
  );

  // 启动客户端（自动管理生命周期）
  void client.start();

  // 清理函数：停止时由 deactivate() 处理

  vscode.window.showInformationMessage('Aster Language Server 已启动');
}

/**
 * 获取工作区根目录
 */
function getWorkspaceRoot(): string | null {
  const ws = vscode.workspace.workspaceFolders?.[0];
  return ws ? ws.uri.fsPath : null;
}

/**
 * 获取配置值
 */
function getConfig<T>(key: string, defaultValue: T): T {
  const cfg = vscode.workspace.getConfiguration('aster');
  return cfg.get<T>(key, defaultValue);
}

/**
 * 解析 CLI 路径
 */
function resolveCLIPath(): string {
  const root = getWorkspaceRoot();
  if (!root) {
    throw new Error('未找到工作区根目录');
  }
  const relPath = getConfig('cli.path', 'aster-lang-cli/build/install/aster-lang-cli/bin/aster-lang-cli');
  return path.resolve(root, relPath);
}

/**
 * 获取当前激活的 Aster 文件路径
 */
function getActiveAsterFile(): string | null {
  const editor = vscode.window.activeTextEditor;
  if (!editor) {
    return null;
  }
  const doc = editor.document;
  if (doc.languageId !== 'aster') {
    return null;
  }
  return doc.uri.fsPath;
}

/**
 * 执行 Aster CLI 命令
 */
async function runAsterCommand(
  command: string,
  args: string[],
  options: { showOutput?: boolean; cwd?: string } = {}
): Promise<{ stdout: string; stderr: string }> {
  const cliPath = resolveCLIPath();

  // 检查 CLI 是否存在
  if (!fs.existsSync(cliPath)) {
    throw new Error(`Aster CLI 未找到: ${cliPath}。请先构建项目（./gradlew :aster-lang-cli:installDist）。`);
  }

  // 设置环境变量
  const compiler = getConfig('compiler', 'typescript');
  const debugEnabled = getConfig('debug.enabled', false);

  const env = { ...process.env };
  env.ASTER_COMPILER = compiler;
  if (debugEnabled) {
    env.ASTER_DEBUG = 'true';
  }

  // 执行命令
  const cwd = options.cwd || getWorkspaceRoot() || process.cwd();
  const fullArgs = [command, ...args];

  try {
    const result = await execFileAsync(cliPath, fullArgs, { env, cwd, maxBuffer: 10 * 1024 * 1024 });

    if (options.showOutput !== false) {
      const output = result.stdout || result.stderr;
      if (output) {
        vscode.window.showInformationMessage(`Aster ${command}: 执行成功`);
        const outputChannel = vscode.window.createOutputChannel('Aster');
        outputChannel.appendLine(output);
        outputChannel.show();
      }
    }

    return result;
  } catch (error: any) {
    const errorMsg = error.stderr || error.stdout || error.message || String(error);
    vscode.window.showErrorMessage(`Aster ${command} 失败: ${errorMsg}`);

    const outputChannel = vscode.window.createOutputChannel('Aster');
    outputChannel.appendLine(`错误: ${errorMsg}`);
    outputChannel.show();

    throw error;
  }
}

/**
 * Compile 命令：编译当前 Aster 文件
 */
async function compileCommand(): Promise<void> {
  const filePath = getActiveAsterFile();
  if (!filePath) {
    vscode.window.showWarningMessage('请打开一个 .aster 文件');
    return;
  }

  const outputDir = getConfig('output.directory', 'build/aster-out');
  const root = getWorkspaceRoot();
  const outputPath = root ? path.resolve(root, outputDir) : outputDir;

  try {
    await vscode.window.withProgress(
      {
        location: vscode.ProgressLocation.Notification,
        title: `编译 ${path.basename(filePath)}...`,
        cancellable: false,
      },
      async () => {
        await runAsterCommand('compile', [filePath, '--output', outputPath]);
      }
    );
    vscode.window.showInformationMessage(`编译成功: ${path.basename(filePath)}`);
  } catch (error) {
    // Error already shown by runAsterCommand
  }
}

/**
 * Debug 命令：启动调试配置
 */
async function debugCommand(): Promise<void> {
  const filePath = getActiveAsterFile();
  if (!filePath) {
    vscode.window.showWarningMessage('请打开一个 .aster 文件');
    return;
  }

  // 首先编译文件
  await compileCommand();

  // 创建调试配置
  const debugConfig: vscode.DebugConfiguration = {
    type: 'java',
    request: 'launch',
    name: `Debug ${path.basename(filePath)}`,
    mainClass: '${file}', // 需要根据实际编译输出调整
    projectName: 'aster-lang',
  };

  // 启动调试会话
  const started = await vscode.debug.startDebugging(undefined, debugConfig);
  if (!started) {
    vscode.window.showErrorMessage('无法启动调试会话。请确保已安装 Java 调试扩展。');
  }
}

/**
 * Build Native 命令：构建原生可执行文件
 */
async function buildNativeCommand(): Promise<void> {
  const filePath = getActiveAsterFile();
  if (!filePath) {
    vscode.window.showWarningMessage('请打开一个 .aster 文件');
    return;
  }

  try {
    await vscode.window.withProgress(
      {
        location: vscode.ProgressLocation.Notification,
        title: `构建原生可执行文件 ${path.basename(filePath)}...`,
        cancellable: false,
      },
      async () => {
        // 目前 CLI 不支持 native 命令，这里先编译
        const outputDir = getConfig('output.directory', 'build/aster-out');
        const root = getWorkspaceRoot();
        const outputPath = root ? path.resolve(root, outputDir) : outputDir;

        await runAsterCommand('compile', [filePath, '--output', outputPath]);

        vscode.window.showInformationMessage(
          '原生构建功能即将推出。当前已编译为 JVM 字节码。'
        );
      }
    );
  } catch (error) {
    // Error already shown by runAsterCommand
  }
}

/**
 * Package 命令：打包为 JAR
 */
async function packageCommand(): Promise<void> {
  const filePath = getActiveAsterFile();
  if (!filePath) {
    vscode.window.showWarningMessage('请打开一个 .aster 文件');
    return;
  }

  const outputDir = getConfig('output.directory', 'build/aster-out');
  const root = getWorkspaceRoot();
  const outputPath = root ? path.resolve(root, outputDir) : outputDir;
  const jarPath = path.join(outputPath, `${path.basename(filePath, '.aster')}.jar`);

  try {
    await vscode.window.withProgress(
      {
        location: vscode.ProgressLocation.Notification,
        title: `打包 ${path.basename(filePath)} 为 JAR...`,
        cancellable: false,
      },
      async () => {
        // 首先编译
        await runAsterCommand('compile', [filePath, '--output', outputPath], { showOutput: false });

        // 然后生成 JAR
        await runAsterCommand('jar', [filePath, '--output', jarPath]);
      }
    );
    vscode.window.showInformationMessage(`JAR 已生成: ${jarPath}`);
  } catch (error) {
    // Error already shown by runAsterCommand
  }
}

/**
 * 扩展激活入口
 *
 * 当 VSCode 激活此扩展时调用（打开 .aster 文件或执行扩展命令时）
 */
export function activate(context: vscode.ExtensionContext): void {
  // 注册命令：手动启动语言服务器
  context.subscriptions.push(
    vscode.commands.registerCommand('aster.startLanguageServer', () => {
      if (client) {
        vscode.window.showInformationMessage('Aster Language Server 已在运行中');
        return;
      }
      startClient();
    })
  );

  // 注册编译命令
  context.subscriptions.push(
    vscode.commands.registerCommand('aster.compile', () => {
      void compileCommand();
    })
  );

  // 注册调试命令
  context.subscriptions.push(
    vscode.commands.registerCommand('aster.debug', () => {
      void debugCommand();
    })
  );

  // 注册原生构建命令
  context.subscriptions.push(
    vscode.commands.registerCommand('aster.buildNative', () => {
      void buildNativeCommand();
    })
  );

  // 注册打包命令
  context.subscriptions.push(
    vscode.commands.registerCommand('aster.package', () => {
      void packageCommand();
    })
  );

  // 自动启动：如果存在工作区，则在激活时自动启动服务器
  if (vscode.workspace.workspaceFolders && vscode.workspace.workspaceFolders.length > 0) {
    startClient();
  } else {
    vscode.window.showInformationMessage(
      'Aster: 未检测到工作区。打开包含 .aster 文件的文件夹后，使用 "Aster: Start Language Server" 命令启动。'
    );
  }
}

/**
 * 扩展停用清理
 *
 * 停止 LSP 客户端并释放资源
 */
export async function deactivate(): Promise<void> {
  if (client) {
    await client.stop();
    client = null;
  }
}
