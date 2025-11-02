/**
 * 资源解析模块
 *
 * 提供统一的资源路径解析逻辑，支持"内置 > 配置 > 默认"的优先级策略。
 * 用于 LSP 服务器、CLI 等扩展资源的路径解析。
 */

import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';

/**
 * 解析扩展内置资源的路径
 *
 * 优先级策略：
 * 1. 内置资源：扩展安装目录下的资源（最高优先级）
 * 2. 用户配置：用户在设置中指定的路径
 * 3. 默认路径：降级到工作区中的默认路径
 *
 * @param context 扩展上下文，用于获取扩展安装路径
 * @param resourcePath 内置资源的相对路径（如 'dist/src/lsp/server.js'）
 * @param configKey 配置项键名（如 'langServer.path'）
 * @param fallbackPath 可选的降级路径（相对于工作区根目录）
 * @returns 解析后的绝对路径
 */
export function resolveBundledResource(
  context: vscode.ExtensionContext,
  resourcePath: string,
  configKey: string,
  fallbackPath?: string
): string {
  // 1. 优先检查扩展内置资源
  const bundledPath = path.join(context.extensionPath, resourcePath);
  if (fs.existsSync(bundledPath)) {
    return bundledPath;
  }

  // 2. 读取用户配置
  const config = vscode.workspace.getConfiguration('aster');
  const customPath = config.get<string>(configKey);

  // 只有在配置非空时才使用自定义路径
  if (customPath && customPath.trim() !== '') {
    // 如果是相对路径，解析为相对于工作区根目录的绝对路径
    const workspaceRoot = getWorkspaceRoot();
    if (workspaceRoot) {
      return path.resolve(workspaceRoot, customPath);
    }
    // 如果没有工作区，尝试作为绝对路径处理
    return path.resolve(customPath);
  }

  // 3. 使用降级路径或原始资源路径
  const finalPath = fallbackPath || resourcePath;
  const workspaceRoot = getWorkspaceRoot();
  if (workspaceRoot) {
    return path.resolve(workspaceRoot, finalPath);
  }

  // 最后的降级：相对于当前工作目录
  return path.resolve(process.cwd(), finalPath);
}

/**
 * 获取工作区根目录
 *
 * @returns 工作区根目录的绝对路径，如果没有打开工作区则返回 null
 */
function getWorkspaceRoot(): string | null {
  const workspaceFolders = vscode.workspace.workspaceFolders;
  return workspaceFolders && workspaceFolders.length > 0
    ? workspaceFolders[0].uri.fsPath
    : null;
}
