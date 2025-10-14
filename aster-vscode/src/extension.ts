/**
 * Aster Language VSCode Extension
 *
 * 提供 Aster CNL 语言支持：
 * - 语法高亮（通过 TextMate grammar）
 * - LSP 集成（代码补全、跳转定义、错误检查等）
 * - 文件监视和自动同步
 */

import * as path from 'node:path';
import * as fs from 'node:fs';
import * as vscode from 'vscode';
import {
  LanguageClient,
  LanguageClientOptions,
  ServerOptions,
  TransportKind,
} from 'vscode-languageclient/node';

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
      // 监视工作区中所有 .cnl 文件的变化
      fileEvents: vscode.workspace.createFileSystemWatcher('**/*.cnl'),
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
 * 扩展激活入口
 *
 * 当 VSCode 激活此扩展时调用（打开 .cnl 文件或执行扩展命令时）
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

  // 自动启动：如果存在工作区，则在激活时自动启动服务器
  if (vscode.workspace.workspaceFolders && vscode.workspace.workspaceFolders.length > 0) {
    startClient();
  } else {
    vscode.window.showInformationMessage(
      'Aster: 未检测到工作区。打开包含 .cnl 文件的文件夹后，使用 "Aster: Start Language Server" 命令启动。'
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
