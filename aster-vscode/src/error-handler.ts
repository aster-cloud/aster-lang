/**
 * 错误处理模块
 *
 * 提供统一的错误提示和处理逻辑，支持分级错误显示和可操作的修复按钮。
 * 所有错误同时输出到 "Aster" OutputChannel 以便调试。
 */

import * as vscode from 'vscode';

/**
 * 资源类型
 */
export type ResourceType = 'LSP' | 'CLI' | 'Workspace';

/**
 * 错误操作按钮
 */
export interface ErrorAction {
  /** 按钮显示文本 */
  label: string;
  /** 按钮点击处理函数 */
  handler: () => void | Promise<void>;
}

/**
 * 全局输出通道（单例）
 */
let outputChannel: vscode.OutputChannel | undefined;

/**
 * 获取或创建输出通道（单例）
 *
 * 导出此函数供其他模块使用，确保整个扩展只有一个 Aster 输出通道实例。
 */
export function getOutputChannel(): vscode.OutputChannel {
  if (!outputChannel) {
    outputChannel = vscode.window.createOutputChannel('Aster');
  }
  return outputChannel;
}

/**
 * 显示资源缺失错误提示
 *
 * 根据资源类型显示相应的错误消息，并提供可操作的修复按钮。
 * 错误消息同时写入 "Aster" OutputChannel。
 *
 * @param resourceType 资源类型（LSP/CLI/Workspace）
 * @param resourcePath 资源路径（用于详细错误信息）
 * @param actions 可选的操作按钮列表
 * @returns 用户选择的操作按钮标签，如果用户未选择则返回 undefined
 */
export async function showResourceError(
  resourceType: ResourceType,
  resourcePath: string,
  actions?: ErrorAction[]
): Promise<string | undefined> {
  // 构建错误消息
  const message = buildErrorMessage(resourceType, resourcePath);

  // 写入输出通道
  const channel = getOutputChannel();
  channel.appendLine(`[ERROR] ${new Date().toISOString()}`);
  channel.appendLine(`资源类型: ${resourceType}`);
  channel.appendLine(`资源路径: ${resourcePath}`);
  channel.appendLine(`错误消息: ${message}`);
  channel.appendLine('---');

  // 提取按钮标签
  const buttons = actions ? actions.map(a => a.label) : [];

  // 显示错误消息并处理用户选择
  const selection = await vscode.window.showErrorMessage(message, ...buttons);

  if (selection && actions) {
    // 查找对应的操作并执行
    const action = actions.find(a => a.label === selection);
    if (action) {
      try {
        // 执行处理函数并等待结果（支持 Promise 和 VSCode Thenable）
        channel.appendLine(`[ACTION] 执行操作: ${action.label}`);
        await Promise.resolve(action.handler());
        channel.appendLine(`[ACTION] 操作完成: ${action.label}`);
      } catch (error: any) {
        channel.appendLine(`[ERROR] 操作执行失败 (${action.label}): ${error}`);
        vscode.window.showErrorMessage(`操作失败: ${error.message || String(error)}`);
      }
    }
  }

  return selection;
}

/**
 * 构建错误消息
 */
function buildErrorMessage(resourceType: ResourceType, resourcePath: string): string {
  switch (resourceType) {
    case 'LSP':
      return `Aster LSP 服务器未找到: ${resourcePath}。请先构建项目或配置路径。`;
    case 'CLI':
      return `Aster CLI 未找到: ${resourcePath}。扩展已内置 CLI，请检查配置。`;
    case 'Workspace':
      return 'Aster 需要工作区才能运行。请先打开包含 .aster 文件的文件夹。';
    default:
      return `资源未找到: ${resourcePath}`;
  }
}

/** Aster 项目文档和 Release 页面 URL */
const ASTER_DOCS_URL = 'https://github.com/anthropics/aster-lang#readme';
const ASTER_RELEASES_URL = 'https://github.com/anthropics/aster-lang/releases';

/**
 * 预定义的标准操作按钮
 */
export const StandardActions = {
  /**
   * "了解更多"按钮 - 打开文档链接
   */
  learnMore: (url: string): ErrorAction => ({
    label: '了解更多',
    handler: () => {
      vscode.env.openExternal(vscode.Uri.parse(url));
    },
  }),

  /**
   * "查看文档"按钮 - 打开 Aster 项目文档
   */
  openDocumentation: (): ErrorAction => ({
    label: '查看文档',
    handler: () => {
      vscode.env.openExternal(vscode.Uri.parse(ASTER_DOCS_URL));
    },
  }),

  /**
   * "下载最新版本"按钮 - 打开 Release 页面
   */
  openReleases: (): ErrorAction => ({
    label: '下载最新版本',
    handler: () => {
      vscode.env.openExternal(vscode.Uri.parse(ASTER_RELEASES_URL));
    },
  }),

  /**
   * "配置路径"按钮 - 打开设置面板
   */
  configurePath: (settingKey: string): ErrorAction => ({
    label: '配置路径',
    handler: () => {
      vscode.commands.executeCommand('workbench.action.openSettings', settingKey);
    },
  }),

  /**
   * "打开输出"按钮 - 显示输出通道
   */
  showOutput: (): ErrorAction => ({
    label: '查看日志',
    handler: () => {
      const channel = getOutputChannel();
      channel.show();
    },
  }),
};

/**
 * 显示信息消息（同时记录到输出通道）
 */
export function showInfo(message: string): void {
  const channel = getOutputChannel();
  channel.appendLine(`[INFO] ${new Date().toISOString()} - ${message}`);
  vscode.window.showInformationMessage(message);
}

/**
 * 显示警告消息（同时记录到输出通道）
 */
export function showWarning(message: string): void {
  const channel = getOutputChannel();
  channel.appendLine(`[WARN] ${new Date().toISOString()} - ${message}`);
  vscode.window.showWarningMessage(message);
}

/**
 * 清理资源
 */
export function dispose(): void {
  if (outputChannel) {
    outputChannel.dispose();
    outputChannel = undefined;
  }
}
