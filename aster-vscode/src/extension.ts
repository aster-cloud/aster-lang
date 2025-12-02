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
  State,
  TransportKind,
} from 'vscode-languageclient/node';
import { resolveBundledResource } from './resource-resolver';
import { showResourceError, StandardActions, dispose as disposeErrorHandler, getOutputChannel } from './error-handler';
import { getWorkspaceRoot } from './workspace-utils';

const execFileAsync = promisify(execFile);

/**
 * 简单字符串哈希函数（Java String.hashCode 算法）
 * 用于生成工作区 URI 的唯一标识符，避免路径字符替换导致的冲突
 */
function hashCode(str: string): string {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i);
    hash = ((hash << 5) - hash) + char;
    hash = hash & hash; // 转换为 32 位整数
  }
  // 返回绝对值的十六进制表示，确保是有效标识符
  return Math.abs(hash).toString(16);
}

/**
 * 多工作区 LSP 客户端管理
 *
 * 每个工作区独立拥有一个 LSP 客户端实例，使用工作区 URI 字符串作为键。
 * 这样可以确保不同工作区的 aster.langServer.path 配置相互隔离。
 */
interface WorkspaceLspState {
  client: LanguageClient | null;
  startingPromise: Promise<void> | null;
  startAborted: boolean;
}

/** 工作区 LSP 状态映射表 */
const workspaceLspStates = new Map<string, WorkspaceLspState>();

/** 获取或创建工作区的 LSP 状态 */
function getWorkspaceLspState(workspaceUri: vscode.Uri): WorkspaceLspState {
  const key = workspaceUri.toString();
  let state = workspaceLspStates.get(key);
  if (!state) {
    state = { client: null, startingPromise: null, startAborted: false };
    workspaceLspStates.set(key, state);
  }
  return state;
}

/**
 * LSP 状态管理器
 *
 * 集中管理多个 LSP 客户端的生命周期状态，便于调试和监控。
 * 支持多工作区场景下的状态追踪。
 */
const lspState = {
  /** 获取指定工作区的 LSP 状态描述 */
  getStatus(workspaceUri?: vscode.Uri): string {
    if (!workspaceUri) {
      // 返回所有工作区的汇总状态
      const running = Array.from(workspaceLspStates.values()).filter(s => s.client).length;
      const starting = Array.from(workspaceLspStates.values()).filter(s => s.startingPromise).length;
      return `${running} running, ${starting} starting`;
    }
    const state = workspaceLspStates.get(workspaceUri.toString());
    if (!state) return 'not initialized';
    if (state.startingPromise) return 'starting';
    if (state.client) return 'running';
    return 'stopped';
  },

  /** 获取所有运行中的工作区路径 */
  getActiveWorkspaces(): string[] {
    return Array.from(workspaceLspStates.entries())
      .filter(([, state]) => state.client)
      .map(([uri]) => vscode.Uri.parse(uri).fsPath);
  },

  /** 记录状态变更到输出通道 */
  logStateChange(event: string, details?: Record<string, unknown>): void {
    const channel = getOutputChannel();
    const timestamp = new Date().toISOString();
    const detailStr = details ? ` ${JSON.stringify(details)}` : '';
    channel.appendLine(`[${timestamp}] LSP: ${event}${detailStr}`);
  },
};

/**
 * 解析 LSP 服务器路径
 *
 * 使用统一的资源解析模块，优先级策略：
 * 1. 用户配置路径（最高优先级，允许覆盖内置资源）
 * 2. 扩展内置资源
 * 3. 工作区默认路径
 *
 * @param context 扩展上下文
 * @param workspaceUri 可选的工作区 URI，用于 multi-root 场景读取对应工作区的配置
 */
function resolveServerPath(context: vscode.ExtensionContext, workspaceUri?: vscode.Uri): string {
  // 如果未指定，使用第一个工作区作为配置作用域
  const effectiveUri = workspaceUri ?? vscode.workspace.workspaceFolders?.[0]?.uri;
  return resolveBundledResource(
    context,
    'dist/src/lsp/server.js',
    'langServer.path',
    'dist/src/lsp/server.js',
    effectiveUri
  );
}

/**
 * 启动指定工作区的 LSP 客户端
 *
 * 每个工作区拥有独立的 LSP 客户端实例，使用各自的 aster.langServer.path 配置。
 * 实现状态机管理：监听客户端状态变化，在失败或停止时自动清理。
 * 使用互斥锁防止同一工作区的并发启动导致进程泄漏。
 *
 * @param context 扩展上下文
 * @param options.silent 静默模式，为 true 时不显示启动成功提示（用于自动启动场景）
 * @param options.workspaceUri 工作区 URI，必须指定以确定使用哪个工作区的配置
 */
async function startClient(context: vscode.ExtensionContext, options: { silent?: boolean; workspaceUri?: vscode.Uri } = {}): Promise<void> {
  // 确定目标工作区
  const targetWorkspaceUri = options.workspaceUri ?? vscode.workspace.workspaceFolders?.[0]?.uri;
  if (!targetWorkspaceUri) {
    lspState.logStateChange('start skipped - no workspace');
    return;
  }

  // 获取该工作区的状态
  const state = getWorkspaceLspState(targetWorkspaceUri);

  // 如果该工作区正在启动中，等待当前启动完成后返回
  if (state.startingPromise) {
    await state.startingPromise;
    return;
  }

  // 内部启动逻辑
  const doStart = async (): Promise<void> => {
    // 重置取消标志
    state.startAborted = false;

    lspState.logStateChange('start requested', {
      workspace: targetWorkspaceUri.fsPath,
      silent: options.silent,
      activeClients: lspState.getActiveWorkspaces(),
    });

    // 如果该工作区已有客户端在运行，先停止
    if (state.client) {
      lspState.logStateChange('stopping existing client', { workspace: targetWorkspaceUri.fsPath });
      try {
        await state.client.stop();
      } catch {
        // 忽略停止时的错误
      }
      state.client = null;
    }

    // 检查启动前是否已被取消
    if (state.startAborted) {
      lspState.logStateChange('start aborted before initialization', { workspace: targetWorkspaceUri.fsPath });
      return;
    }

    const serverModule = resolveServerPath(context, targetWorkspaceUri);

    // 检查服务器文件是否存在
    if (!fs.existsSync(serverModule)) {
      void showResourceError('LSP', serverModule, [
        StandardActions.openDocumentation(),
        StandardActions.configurePath('aster.langServer.path'),
        StandardActions.showOutput(),
      ]);
      return;
    }

    // LSP 服务器配置
    const serverOptions: ServerOptions = {
      run: { module: serverModule, transport: TransportKind.stdio },
      debug: { module: serverModule, transport: TransportKind.stdio },
    };

    // 创建工作区专属的文件过滤器
    const workspaceFolder = vscode.workspace.getWorkspaceFolder(targetWorkspaceUri);
    const workspacePattern = workspaceFolder
      ? new vscode.RelativePattern(workspaceFolder, '**/*.{aster,astr}')
      : '**/*.{aster,astr}';
    const asterfilePattern = workspaceFolder
      ? new vscode.RelativePattern(workspaceFolder, '**/Asterfile')
      : '**/Asterfile';

    // LSP 客户端配置 - 仅处理该工作区的文件
    const clientOptions: LanguageClientOptions = {
      documentSelector: [
        { scheme: 'file', language: 'aster', pattern: workspaceFolder ? `${workspaceFolder.uri.fsPath}/**/*` : undefined },
        { scheme: 'untitled', language: 'aster' },
        { scheme: 'vscode-remote', language: 'aster' },
        { scheme: 'vscode-vfs', language: 'aster' },
      ],
      workspaceFolder: workspaceFolder,
      synchronize: {
        fileEvents: [
          vscode.workspace.createFileSystemWatcher(workspacePattern),
          vscode.workspace.createFileSystemWatcher(asterfilePattern),
        ],
      },
    };

    // 创建工作区专属的 LSP 客户端（使用 URI 哈希确保唯一性）
    // 使用 URI toString() 的哈希值避免 /foo-bar 与 /foo_bar 等路径冲突
    const uriHash = hashCode(targetWorkspaceUri.toString());
    const clientId = `asterLangServer-${uriHash}`;
    const newClient = new LanguageClient(
      clientId,
      `Aster Language Server (${workspaceFolder?.name ?? 'default'})`,
      serverOptions,
      clientOptions
    );

    // 监听状态变化，在失败或停止时清理 client 引用
    const workspaceKey = targetWorkspaceUri.toString();
    newClient.onDidChangeState((event) => {
      if (event.newState === State.Stopped) {
        const currentState = workspaceLspStates.get(workspaceKey);
        if (currentState && currentState.client === newClient) {
          currentState.client = null;
        }
      }
    });

    // 启动客户端
    try {
      lspState.logStateChange('client starting', { workspace: targetWorkspaceUri.fsPath, serverModule });
      await newClient.start();

      // 启动完成后检查是否在启动过程中被取消
      if (state.startAborted) {
        lspState.logStateChange('start aborted after client.start()', { workspace: targetWorkspaceUri.fsPath });
        try {
          await newClient.stop();
        } catch {
          // 忽略停止错误
        }
        return;
      }

      state.client = newClient;
      lspState.logStateChange('client started successfully', {
        workspace: targetWorkspaceUri.fsPath,
        totalClients: lspState.getActiveWorkspaces().length,
      });

      // 仅在非静默模式下显示启动成功提示
      if (!options.silent) {
        vscode.window.showInformationMessage(`Aster Language Server 已启动 (${workspaceFolder?.name ?? 'default'})`);
      }
    } catch (error: any) {
      state.client = null;
      const errorMsg = error.message || String(error);
      lspState.logStateChange('client start failed', { workspace: targetWorkspaceUri.fsPath, error: errorMsg });
      vscode.window.showErrorMessage(`Aster Language Server 启动失败: ${errorMsg}`);
    }
  };

  // 设置互斥锁并执行启动
  state.startingPromise = doStart();
  try {
    await state.startingPromise;
  } finally {
    state.startingPromise = null;
  }
}

/**
 * 停止 LSP 客户端
 *
 * @param workspaceUri 如果指定，仅停止该工作区的客户端；否则停止所有客户端
 */
async function stopClient(workspaceUri?: vscode.Uri): Promise<void> {
  if (workspaceUri) {
    // 停止指定工作区的客户端
    await stopWorkspaceClient(workspaceUri);
  } else {
    // 停止所有工作区的客户端
    lspState.logStateChange('stopping all clients', {
      activeClients: lspState.getActiveWorkspaces(),
    });

    const stopPromises = Array.from(workspaceLspStates.keys()).map(key =>
      stopWorkspaceClient(vscode.Uri.parse(key))
    );
    await Promise.all(stopPromises);
    workspaceLspStates.clear();

    lspState.logStateChange('all clients stopped');
  }
}

/**
 * 停止指定工作区的 LSP 客户端
 */
async function stopWorkspaceClient(workspaceUri: vscode.Uri): Promise<void> {
  const state = workspaceLspStates.get(workspaceUri.toString());
  if (!state) {
    return;
  }

  lspState.logStateChange('stop requested', {
    workspace: workspaceUri.fsPath,
    currentStatus: lspState.getStatus(workspaceUri),
  });

  // 设置取消标志，中止正在进行的启动
  state.startAborted = true;

  // 等待任何正在进行的启动完成
  if (state.startingPromise) {
    lspState.logStateChange('waiting for pending start to complete', { workspace: workspaceUri.fsPath });
    try {
      await state.startingPromise;
    } catch {
      // 忽略启动错误
    }
  }

  // 停止已运行的客户端
  if (state.client) {
    lspState.logStateChange('stopping running client', { workspace: workspaceUri.fsPath });
    try {
      await state.client.stop();
    } catch {
      // ignore stop errors
    }
    state.client = null;
  }

  // 从映射表中移除该工作区的状态
  workspaceLspStates.delete(workspaceUri.toString());
  lspState.logStateChange('client stopped', { workspace: workspaceUri.fsPath });
}

// getWorkspaceRoot 已移至 workspace-utils.ts 共享模块，避免重复实现

/**
 * 获取配置值
 *
 * @param key 配置键名
 * @param defaultValue 默认值
 * @param scope 可选的作用域 URI，用于 multi-root 场景读取对应工作区的配置
 */
function getConfig<T>(key: string, defaultValue: T, scope?: vscode.Uri): T {
  const cfg = vscode.workspace.getConfiguration('aster', scope);
  return cfg.get<T>(key, defaultValue);
}

/**
 * 解析 CLI 路径
 *
 * 使用统一的资源解析模块，优先级策略：
 * 1. 用户配置路径（最高优先级，允许覆盖内置资源）
 * 2. 扩展内置 Node 版 CLI
 * 3. 工作区默认路径（Gradle 构建的 Java 版 CLI）
 *
 * @param context 扩展上下文
 * @param fileUri 可选的文件 URI，用于 multi-root 场景确定正确的工作区
 */
function resolveCLIPath(context: vscode.ExtensionContext, fileUri?: vscode.Uri): string {
  const gradleCliName = process.platform === 'win32' ? 'aster-lang-cli.bat' : 'aster-lang-cli';
  const fallbackPath = path.join(
    'aster-lang-cli',
    'build',
    'install',
    'aster-lang-cli',
    'bin',
    gradleCliName
  );
  return resolveBundledResource(
    context,
    'dist/scripts/aster.js',
    'cli.path',
    fallbackPath,
    fileUri
  );
}

/**
 * 当前 Aster 文件信息
 */
interface ActiveAsterFile {
  /** 文件路径 */
  path: string;
  /** 文件 URI */
  uri: vscode.Uri;
}

/**
 * 获取当前激活的 Aster 文件信息
 *
 * @returns 文件路径和 URI，如果没有激活的 Aster 文件则返回 null
 */
function getActiveAsterFile(): ActiveAsterFile | null {
  const editor = vscode.window.activeTextEditor;
  if (!editor) {
    return null;
  }
  const doc = editor.document;
  if (doc.languageId !== 'aster') {
    return null;
  }
  return {
    path: doc.uri.fsPath,
    uri: doc.uri,
  };
}

async function ensureLatestFileInfo(file: ActiveAsterFile): Promise<ActiveAsterFile | null> {
  const targetDoc = vscode.workspace.textDocuments.find((doc) => doc.uri.toString() === file.uri.toString());
  if (targetDoc) {
    if (targetDoc.isDirty) {
      const saved = await targetDoc.save();
      if (!saved) {
        vscode.window.showWarningMessage('请先保存文件后再运行 Aster 命令。');
        return null;
      }
    }
    return {
      path: targetDoc.uri.fsPath,
      uri: targetDoc.uri,
    };
  }
  return file;
}

/**
 * 执行 Aster CLI 命令
 *
 * @param context 扩展上下文
 * @param command CLI 命令名称
 * @param args 命令参数
 * @param options 执行选项
 * @param options.showOutput 是否显示输出（默认 true）
 * @param options.fileUri 当前文件的 URI，用于确定工作区
 */
async function runAsterCommand(
  context: vscode.ExtensionContext,
  command: string,
  args: string[],
  options: { showOutput?: boolean; fileUri?: vscode.Uri } = {}
): Promise<{ stdout: string; stderr: string }> {
  // 检查工作区是否存在
  const workspaceRoot = getWorkspaceRoot(options.fileUri);
  if (!workspaceRoot) {
    await showResourceError('Workspace', '', [
      StandardActions.openDocumentation(),
      StandardActions.showOutput(),
    ]);
    throw new Error('需要打开工作区才能执行此命令');
  }

  const cliPath = resolveCLIPath(context, options.fileUri);

  // 检查 CLI 是否存在
  if (!fs.existsSync(cliPath)) {
    // 使用增强的错误提示，提供可操作的修复按钮
    await showResourceError('CLI', cliPath, [
      StandardActions.openDocumentation(),
      StandardActions.openReleases(),
      StandardActions.configurePath('aster.cli.path'),
      StandardActions.showOutput(),
    ]);
    throw new Error(`Aster CLI 未找到: ${cliPath}`);
  }

  // 设置环境变量（使用 fileUri scope 支持 multi-root 独立配置）
  const compiler = getConfig('compiler', 'typescript', options.fileUri);
  const debugEnabled = getConfig('debug.enabled', false, options.fileUri);

  const env = { ...process.env };
  env.ASTER_COMPILER = compiler;
  if (debugEnabled) {
    env.ASTER_DEBUG = 'true';
  }

  // 如果 CLI 路径是 .js 文件，使用 VSCode 内置的 Node.js 执行（process.execPath）
  // 这避免了依赖系统安装的 node，确保在干净环境下也能正常工作
  const isJsFile = cliPath.endsWith('.js');
  const isBatchFile =
    process.platform === 'win32' && (cliPath.endsWith('.bat') || cliPath.endsWith('.cmd'));

  let execCommand: string;
  let fullArgs: string[];
  if (isJsFile) {
    execCommand = process.execPath;
    fullArgs = [cliPath, command, ...args];
  } else if (isBatchFile) {
    execCommand = process.env.ComSpec || 'cmd.exe';
    fullArgs = ['/c', cliPath, command, ...args];
  } else {
    execCommand = cliPath;
    fullArgs = [command, ...args];
  }

  try {
    const result = await execFileAsync(execCommand, fullArgs, { env, cwd: workspaceRoot, maxBuffer: 10 * 1024 * 1024 });

    if (options.showOutput !== false) {
      const output = result.stdout || result.stderr;
      if (output) {
        vscode.window.showInformationMessage(`Aster ${command}: 执行成功`);
        const channel = getOutputChannel();
        channel.appendLine(`[${new Date().toISOString()}] ${command} 执行成功`);
        channel.appendLine(output);
        channel.show();
      }
    }

    return result;
  } catch (error: any) {
    const errorMsg = error.stderr || error.stdout || error.message || String(error);
    vscode.window.showErrorMessage(`Aster ${command} 失败: ${errorMsg}`);

    const channel = getOutputChannel();
    channel.appendLine(`[${new Date().toISOString()}] ${command} 执行失败`);
    channel.appendLine(`错误: ${errorMsg}`);
    channel.show();

    throw error;
  }
}

/**
 * Compile 命令：编译当前 Aster 文件
 *
 * @param context 扩展上下文
 * @param targetFile 可选的目标文件，如果提供则编译指定文件，否则编译当前激活的文件
 * @returns 编译是否成功
 */
async function compileCommand(context: vscode.ExtensionContext, targetFile?: ActiveAsterFile): Promise<boolean> {
  let activeFile = targetFile || getActiveAsterFile();
  if (!activeFile) {
    vscode.window.showWarningMessage('请打开一个 .aster 文件');
    return false;
  }

  const refreshed = await ensureLatestFileInfo(activeFile);
  if (!refreshed) {
    return false;
  }
  activeFile = refreshed;

  const outputDir = getConfig('output.directory', 'build/aster-out', activeFile.uri);
  const root = getWorkspaceRoot(activeFile.uri);
  const outputPath = root ? path.resolve(root, outputDir) : outputDir;

  try {
    await vscode.window.withProgress(
      {
        location: vscode.ProgressLocation.Notification,
        title: `编译 ${path.basename(activeFile.path)}...`,
        cancellable: false,
      },
      async () => {
        await runAsterCommand(context, 'compile', [activeFile.path, '--output', outputPath], { fileUri: activeFile.uri });
      }
    );
    vscode.window.showInformationMessage(`编译成功: ${path.basename(activeFile.path)}`);
    return true;
  } catch (error) {
    // Error already shown by runAsterCommand
    return false;
  }
}

/**
 * Debug 命令：启动调试配置
 *
 * 首先编译文件，只有编译成功后才启动调试会话。
 *
 * 注意：调试功能目前为实验性功能，需要用户在配置中指定 mainClass。
 */
async function debugCommand(context: vscode.ExtensionContext): Promise<void> {
  let activeFile = getActiveAsterFile();
  if (!activeFile) {
    vscode.window.showWarningMessage('请打开一个 .aster 文件');
    return;
  }

  activeFile = (await ensureLatestFileInfo(activeFile)) ?? null;
  if (!activeFile) {
    return;
  }

  // 获取调试配置（使用 fileUri scope 支持 multi-root 独立配置）
  const mainClass = getConfig('debug.mainClass', '', activeFile.uri);
  const rawClassPaths = getConfig<unknown>('debug.classPaths', [], activeFile.uri);

  // 检查 mainClass 配置
  if (!mainClass) {
    const action = await vscode.window.showWarningMessage(
      '调试功能需要配置 aster.debug.mainClass。请在设置中指定编译后的 Java 主类名称。',
      '打开设置',
      '查看文档'
    );
    if (action === '打开设置') {
      vscode.commands.executeCommand('workbench.action.openSettings', 'aster.debug.mainClass');
    } else if (action === '查看文档') {
      vscode.env.openExternal(vscode.Uri.parse('https://github.com/anthropics/aster-lang#debugging'));
    }
    return;
  }

  // 验证 classPaths 配置类型
  let classPaths: string[] = [];
  if (rawClassPaths !== undefined && rawClassPaths !== null) {
    if (!Array.isArray(rawClassPaths)) {
      // 用户可能误写成字符串而非数组
      const action = await vscode.window.showErrorMessage(
        'aster.debug.classPaths 配置格式错误：应为字符串数组，如 ["build/classes", "lib/deps.jar"]。',
        '打开设置'
      );
      if (action === '打开设置') {
        vscode.commands.executeCommand('workbench.action.openSettings', 'aster.debug.classPaths');
      }
      return;
    }
    // 验证数组元素均为非空字符串
    const invalidItems = rawClassPaths.filter(
      (item) => typeof item !== 'string' || item.trim() === ''
    );
    if (invalidItems.length > 0) {
      const action = await vscode.window.showErrorMessage(
        'aster.debug.classPaths 配置包含无效元素：所有元素必须为非空字符串。',
        '打开设置'
      );
      if (action === '打开设置') {
        vscode.commands.executeCommand('workbench.action.openSettings', 'aster.debug.classPaths');
      }
      return;
    }
    classPaths = rawClassPaths as string[];
  }

  // 首先编译文件，传递已捕获的文件信息以确保编译和调试目标一致
  const compileSuccess = await compileCommand(context, activeFile);
  if (!compileSuccess) {
    // 编译失败时不启动调试器，错误信息已由 compileCommand 显示
    vscode.window.showErrorMessage('编译失败，无法启动调试会话。请修复编译错误后重试。');
    return;
  }

  activeFile = (await ensureLatestFileInfo(activeFile)) ?? null;
  if (!activeFile) {
    return;
  }

  // 获取输出目录用于 classpath
  const outputDir = getConfig('output.directory', 'build/aster-out', activeFile.uri);
  const root = getWorkspaceRoot(activeFile.uri);
  const outputPath = root ? path.resolve(root, outputDir) : outputDir;

  // 构建 classpath 列表，解析相对路径为绝对路径
  let effectiveClassPaths: string[];
  if (classPaths.length > 0) {
    effectiveClassPaths = classPaths.map((cp) => {
      if (path.isAbsolute(cp)) {
        return cp;
      }
      return root ? path.resolve(root, cp) : path.resolve(cp);
    });
  } else {
    effectiveClassPaths = [outputPath];
  }

  // 创建调试配置
  const debugConfig: vscode.DebugConfiguration = {
    type: 'java',
    request: 'launch',
    name: `Debug ${path.basename(activeFile.path)}`,
    mainClass: mainClass,
    classPaths: effectiveClassPaths,
    projectName: getConfig('debug.projectName', 'aster-lang', activeFile.uri),
  };

  // 启动调试会话（传入正确的工作区文件夹，支持 multi-root 工作区）
  const workspaceFolder = vscode.workspace.getWorkspaceFolder(activeFile.uri);
  const started = await vscode.debug.startDebugging(workspaceFolder, debugConfig);
  if (!started) {
    vscode.window.showErrorMessage('无法启动调试会话。请确保已安装 Java 调试扩展。');
  }
}

/**
 * 编译到 JVM 命令
 *
 * 当前功能：编译为 JVM 字节码
 * 未来计划：支持 GraalVM Native Image 原生构建
 */
async function buildNativeCommand(context: vscode.ExtensionContext): Promise<void> {
  let activeFile = getActiveAsterFile();
  if (!activeFile) {
    vscode.window.showWarningMessage('请打开一个 .aster 文件');
    return;
  }

  const refreshed = await ensureLatestFileInfo(activeFile);
  if (!refreshed) {
    return;
  }
  activeFile = refreshed;

  try {
    await vscode.window.withProgress(
      {
        location: vscode.ProgressLocation.Notification,
        title: `编译 ${path.basename(activeFile.path)} 到 JVM...`,
        cancellable: false,
      },
      async () => {
        const outputDir = getConfig('output.directory', 'build/aster-out', activeFile.uri);
        const root = getWorkspaceRoot(activeFile.uri);
        const outputPath = root ? path.resolve(root, outputDir) : outputDir;

        await runAsterCommand(context, 'compile', [activeFile.path, '--output', outputPath], { fileUri: activeFile.uri });
      }
    );
    vscode.window.showInformationMessage(
      `已编译为 JVM 字节码。GraalVM Native Image 原生构建支持开发中。`
    );
  } catch (error) {
    // Error already shown by runAsterCommand
  }
}

/**
 * Package 命令：打包为 JAR
 */
async function packageCommand(context: vscode.ExtensionContext): Promise<void> {
  let activeFile = getActiveAsterFile();
  if (!activeFile) {
    vscode.window.showWarningMessage('请打开一个 .aster 文件');
    return;
  }

  const refreshed = await ensureLatestFileInfo(activeFile);
  if (!refreshed) {
    return;
  }
  activeFile = refreshed;

  const outputDir = getConfig('output.directory', 'build/aster-out', activeFile.uri);
  const root = getWorkspaceRoot(activeFile.uri);
  const outputPath = root ? path.resolve(root, outputDir) : outputDir;
  // 使用 path.parse().name 正确处理 .aster 和 .astr 两种后缀
  const jarPath = path.join(outputPath, `${path.parse(activeFile.path).name}.jar`);

  try {
    await vscode.window.withProgress(
      {
        location: vscode.ProgressLocation.Notification,
        title: `打包 ${path.basename(activeFile.path)} 为 JAR...`,
        cancellable: false,
      },
      async () => {
        // 首先编译
        await runAsterCommand(context, 'compile', [activeFile.path, '--output', outputPath], { showOutput: false, fileUri: activeFile.uri });

        // 然后生成 JAR
        await runAsterCommand(context, 'jar', [activeFile.path, '--output', jarPath], { fileUri: activeFile.uri });
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
  // 注册命令：手动启动/重启语言服务器（针对当前活动编辑器的工作区）
  context.subscriptions.push(
    vscode.commands.registerCommand('aster.startLanguageServer', () => {
      // 优先使用当前活动编辑器所属的工作区
      const activeUri = vscode.window.activeTextEditor?.document.uri;
      const workspaceFolder = activeUri
        ? vscode.workspace.getWorkspaceFolder(activeUri)
        : undefined;
      const targetUri = workspaceFolder?.uri ?? vscode.workspace.workspaceFolders?.[0]?.uri;
      void startClient(context, { workspaceUri: targetUri });
    })
  );

  // 注册编译命令
  context.subscriptions.push(
    vscode.commands.registerCommand('aster.compile', () => {
      void compileCommand(context);
    })
  );

  // 注册调试命令
  context.subscriptions.push(
    vscode.commands.registerCommand('aster.debug', () => {
      void debugCommand(context);
    })
  );

  // 注册原生构建命令
  context.subscriptions.push(
    vscode.commands.registerCommand('aster.buildNative', () => {
      void buildNativeCommand(context);
    })
  );

  // 注册打包命令
  context.subscriptions.push(
    vscode.commands.registerCommand('aster.package', () => {
      void packageCommand(context);
    })
  );

  // 自动启动：为每个工作区启动独立的 LSP 客户端
  if (vscode.workspace.workspaceFolders && vscode.workspace.workspaceFolders.length > 0) {
    // 为所有工作区并行启动 LSP 客户端
    const startPromises = vscode.workspace.workspaceFolders.map(folder =>
      startClient(context, { silent: true, workspaceUri: folder.uri })
    );
    Promise.all(startPromises).catch(() => {
      // 忽略启动错误，各工作区独立处理
    });
  } else {
    vscode.window.showInformationMessage(
      'Aster: 未检测到工作区。打开包含 .aster 文件的文件夹后，使用 "Aster: Start Language Server" 命令启动。'
    );
  }

  // 监听工作区变化：为新增工作区启动 LSP，为移除的工作区停止 LSP
  context.subscriptions.push(
    vscode.workspace.onDidChangeWorkspaceFolders((event) => {
      lspState.logStateChange('workspace folders changed', {
        added: event.added.map(f => f.uri.fsPath),
        removed: event.removed.map(f => f.uri.fsPath),
        total: vscode.workspace.workspaceFolders?.length ?? 0,
        activeClients: lspState.getActiveWorkspaces(),
      });

      // 停止被移除工作区的 LSP 客户端
      for (const folder of event.removed) {
        void stopClient(folder.uri);
      }

      // 为新增的工作区启动 LSP 客户端
      for (const folder of event.added) {
        void startClient(context, { silent: true, workspaceUri: folder.uri });
      }
    })
  );

  // 监听配置变化：当 LSP 配置变更时提示用户重启
  context.subscriptions.push(
    vscode.workspace.onDidChangeConfiguration((event) => {
      if (event.affectsConfiguration('aster.langServer.path')) {
        lspState.logStateChange('LSP configuration changed');
        const activeWorkspaces = lspState.getActiveWorkspaces();
        if (activeWorkspaces.length > 0) {
          vscode.window.showInformationMessage(
            'Aster LSP 配置已更改。重新启动 Language Server 以应用新配置？',
            '重启全部'
          ).then(action => {
            if (action === '重启全部') {
              // 重启所有工作区的 LSP 客户端
              const folders = vscode.workspace.workspaceFolders ?? [];
              for (const folder of folders) {
                void startClient(context, { workspaceUri: folder.uri });
              }
            }
          });
        }
      }
    })
  );
}

/**
 * 扩展停用清理
 *
 * 停止 LSP 客户端并释放所有资源
 */
export async function deactivate(): Promise<void> {
  await stopClient();

  // 释放错误处理模块的 OutputChannel
  disposeErrorHandler();
}
