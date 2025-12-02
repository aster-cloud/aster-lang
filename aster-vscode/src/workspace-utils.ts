/**
 * 工作区工具模块
 *
 * 提供统一的工作区路径解析逻辑，用于 extension.ts 和 resource-resolver.ts 共享。
 * 确保两个模块的行为一致，避免代码漂移。
 */

import * as vscode from 'vscode';

/**
 * 获取工作区根目录
 *
 * 支持 multi-root 工作区：如果提供了文件 URI，返回该文件所属的工作区根目录；
 * 否则返回第一个工作区根目录。
 *
 * 注意：当文件不属于任何工作区时返回 null，不会回退到文件所在目录或第一个工作区，
 * 以确保用户收到正确的"请打开工作区"提示而非误导性的"CLI 未找到"错误。
 *
 * @param fileUri 可选的文件 URI，用于确定所属工作区
 * @returns 工作区根目录的绝对路径，如果没有打开工作区则返回 null
 */
export function getWorkspaceRoot(fileUri?: vscode.Uri): string | null {
  // 如果提供了文件 URI，查找其所属的工作区
  if (fileUri) {
    const folder = vscode.workspace.getWorkspaceFolder(fileUri);
    if (folder) {
      return folder.uri.fsPath;
    }
    // 文件不属于任何工作区，返回 null 触发正确的错误提示
    // 不回退到 path.dirname 或第一个工作区，避免误导性错误信息
    return null;
  }
  // 仅在未提供 fileUri 时回退到第一个工作区
  const workspaceFolders = vscode.workspace.workspaceFolders;
  return workspaceFolders && workspaceFolders.length > 0
    ? workspaceFolders[0].uri.fsPath
    : null;
}
