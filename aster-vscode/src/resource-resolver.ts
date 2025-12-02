/**
 * 资源解析模块
 *
 * 提供统一的资源路径解析逻辑，支持"配置 > 内置 > 默认"的优先级策略。
 * 用户配置拥有最高优先级，允许覆盖扩展内置资源。
 * 用于 LSP 服务器、CLI 等扩展资源的路径解析。
 */

import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';
import * as os from 'os';
import { getWorkspaceRoot } from './workspace-utils';

/**
 * 展开路径中的 ~ 为用户 home 目录
 *
 * 支持 Unix 风格 (~/) 和 Windows 风格 (~\) 的路径分隔符。
 *
 * @param inputPath 可能包含 ~ 的路径
 * @returns 展开后的路径
 */
function expandHomePath(inputPath: string): string {
  // 支持 ~/ (Unix) 和 ~\ (Windows) 两种路径分隔符
  if (inputPath.startsWith('~/') || inputPath.startsWith('~\\')) {
    return path.join(os.homedir(), inputPath.slice(2));
  }
  if (inputPath === '~') {
    return os.homedir();
  }
  return inputPath;
}

/**
 * 解析扩展内置资源的路径
 *
 * 优先级策略：
 * 1. 用户配置：用户在设置中显式指定的路径（最高优先级，允许覆盖内置资源）
 * 2. 内置资源：扩展安装目录下的资源
 * 3. 默认路径：降级到工作区中的默认路径
 *
 * @param context 扩展上下文，用于获取扩展安装路径
 * @param resourcePath 内置资源的相对路径（如 'dist/src/lsp/server.js'）
 * @param configKey 配置项键名（如 'langServer.path'）
 * @param fallbackPath 可选的降级路径（相对于工作区根目录）
 * @param workspaceUri 可选的工作区 URI，用于 multi-root 场景确定正确的工作区
 * @returns 解析后的绝对路径
 */
export function resolveBundledResource(
  context: vscode.ExtensionContext,
  resourcePath: string,
  configKey: string,
  fallbackPath?: string,
  workspaceUri?: vscode.Uri
): string {
  // 1. 优先读取用户配置（传入 workspaceUri 以支持 multi-root 独立配置）
  // 允许用户通过配置覆盖内置资源
  const config = vscode.workspace.getConfiguration('aster', workspaceUri);
  const customPath = config.get<string>(configKey);

  // 只有在配置非空时才使用自定义路径
  if (customPath && customPath.trim() !== '') {
    // 首先展开 ~ 为 home 目录
    const expandedPath = expandHomePath(customPath.trim());

    // 如果是绝对路径（包括展开后的 ~ 路径），直接使用
    if (path.isAbsolute(expandedPath)) {
      return expandedPath;
    }

    // 如果是相对路径，解析为相对于工作区根目录的绝对路径
    const workspaceRoot = getWorkspaceRoot(workspaceUri);
    if (workspaceRoot) {
      return path.resolve(workspaceRoot, expandedPath);
    }
    // 如果没有工作区，尝试作为绝对路径处理
    return path.resolve(expandedPath);
  }

  // 2. 检查扩展内置资源
  const bundledPath = path.join(context.extensionPath, resourcePath);
  if (fs.existsSync(bundledPath)) {
    return bundledPath;
  }

  // 3. 使用降级路径或原始资源路径
  const finalPath = fallbackPath || resourcePath;
  const workspaceRoot = getWorkspaceRoot(workspaceUri);
  if (workspaceRoot) {
    return path.resolve(workspaceRoot, finalPath);
  }

  // 没有工作区时返回内置资源路径（避免误指向其他仓库）
  return bundledPath;
}

// getWorkspaceRoot 已移至 workspace-utils.ts 共享模块，避免重复实现
