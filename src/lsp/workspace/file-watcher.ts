/**
 * LSP Workspace 文件监控器
 * 提供文件系统变化监控，支持降级策略（从 native watcher 降级到 polling）
 */

import { promises as fs } from 'node:fs';
import { extname, relative, sep } from 'node:path';
import { updateDocumentIndex, invalidateDocument } from './document-indexer.js';
import { pathToFileURL } from 'node:url';

/**
 * 文件监控配置
 */
export interface FileWatcherConfig {
  /**
   * 是否启用文件监控
   */
  enabled: boolean;
  /**
   * 监控模式：'native'（客户端提供）或 'polling'（服务器轮询）
   */
  mode: 'native' | 'polling';
  /**
   * Polling 模式下的轮询间隔（毫秒）
   */
  pollingInterval: number;
  /**
   * 排除的目录模式
   */
  excludePatterns: string[];
}

/**
 * 文件变更事件
 */
interface FileChangeEvent {
  uri: string;
  type: 'created' | 'changed' | 'deleted';
}

/**
 * 文件元数据快照
 */
interface FileSnapshot {
  mtime: number;
  size: number;
}

const defaultConfig: FileWatcherConfig = {
  enabled: true,
  mode: 'native',
  pollingInterval: 3000, // 3 秒轮询一次
  excludePatterns: ['node_modules', '.git', 'dist', '.asteri'],
};

let currentConfig: FileWatcherConfig = { ...defaultConfig };
let pollingTimer: NodeJS.Timeout | null = null;
const fileSnapshots: Map<string, FileSnapshot> = new Map();
let workspaceFolders: string[] = [];
let isRunning = false;
let isScanning = false; // 单飞行锁：防止并发扫描

/**
 * 配置文件监控器
 */
export function configureFileWatcher(config: Partial<FileWatcherConfig>): void {
  const wasRunning = isRunning;
  if (wasRunning) {
    stopFileWatcher();
  }

  currentConfig = { ...currentConfig, ...config };

  if (wasRunning && currentConfig.enabled) {
    startFileWatcher(workspaceFolders);
  }
}

/**
 * 启动文件监控
 */
export function startFileWatcher(folders: string[]): void {
  if (isRunning) {
    return;
  }

  workspaceFolders = [...folders];
  isRunning = true;

  if (currentConfig.mode === 'polling') {
    startPolling();
  }
  // native 模式下，由客户端负责触发 onDidChangeWatchedFiles
}

/**
 * 停止文件监控
 */
export function stopFileWatcher(): void {
  isRunning = false;
  stopPolling();
  fileSnapshots.clear();
}

/**
 * 获取监控状态
 */
export function getWatcherStatus(): {
  enabled: boolean;
  mode: 'native' | 'polling';
  isRunning: boolean;
  trackedFiles: number;
} {
  return {
    enabled: currentConfig.enabled,
    mode: currentConfig.mode,
    isRunning,
    trackedFiles: fileSnapshots.size,
  };
}

/**
 * 启动轮询机制
 */
function startPolling(): void {
  if (pollingTimer) {
    return;
  }

  // 立即执行一次扫描
  void scanAndUpdate();

  // 设置定时器
  pollingTimer = setInterval(() => {
    void scanAndUpdate();
  }, currentConfig.pollingInterval);
}

/**
 * 停止轮询
 */
function stopPolling(): void {
  if (pollingTimer) {
    clearInterval(pollingTimer);
    pollingTimer = null;
  }
}

/**
 * 扫描并更新文件索引
 */
async function scanAndUpdate(): Promise<void> {
  // 单飞行锁：如果已经在扫描，跳过本次
  if (isScanning) {
    return;
  }

  isScanning = true;
  try {
    const changes: FileChangeEvent[] = [];

    for (const folder of workspaceFolders) {
      const detectedChanges = await detectChanges(folder);
      changes.push(...detectedChanges);
    }

    // 批量处理变更
    await processChanges(changes);
  } finally {
    isScanning = false;
  }
}

/**
 * 检测目录下的文件变更
 */
async function detectChanges(dir: string): Promise<FileChangeEvent[]> {
  const changes: FileChangeEvent[] = [];
  const currentFiles = new Set<string>();

  try {
    await scanDirectory(dir, currentFiles, changes);
  } catch {
    // 目录不存在或无法访问
    return changes;
  }

  // 检查已删除的文件
  for (const [path] of fileSnapshots) {
    // 使用 relative 检查文件是否在目录下，避免前缀碰撞
    // 例如: /foo/bar 不会误匹配 /foo/barista/file.aster
    const rel = relative(dir, path);
    const isInDir = rel && !rel.startsWith('..') && !rel.startsWith(sep);

    if (isInDir && !currentFiles.has(path)) {
      changes.push({
        uri: pathToFileURL(path).href,
        type: 'deleted',
      });
      fileSnapshots.delete(path);
    }
  }

  return changes;
}

/**
 * 递归扫描目录
 */
async function scanDirectory(
  dir: string,
  currentFiles: Set<string>,
  changes: FileChangeEvent[]
): Promise<void> {
  let entries;
  try {
    entries = await fs.readdir(dir, { withFileTypes: true });
  } catch {
    return;
  }

  for (const entry of entries) {
    // 跳过排除的目录
    if (entry.isDirectory() && currentConfig.excludePatterns.includes(entry.name)) {
      continue;
    }

    const fullPath = `${dir}/${entry.name}`;

    if (entry.isDirectory()) {
      await scanDirectory(fullPath, currentFiles, changes);
    } else if (entry.isFile() && extname(entry.name) === '.aster') {
      currentFiles.add(fullPath);

      try {
        const stats = await fs.stat(fullPath);
        const snapshot: FileSnapshot = {
          mtime: stats.mtimeMs,
          size: stats.size,
        };

        const previous = fileSnapshots.get(fullPath);
        if (!previous) {
          // 新文件
          changes.push({
            uri: pathToFileURL(fullPath).href,
            type: 'created',
          });
        } else if (previous.mtime !== snapshot.mtime || previous.size !== snapshot.size) {
          // 已修改
          changes.push({
            uri: pathToFileURL(fullPath).href,
            type: 'changed',
          });
        }

        fileSnapshots.set(fullPath, snapshot);
      } catch {
        // 文件无法访问
      }
    }
  }
}

/**
 * 处理文件变更
 */
async function processChanges(changes: FileChangeEvent[]): Promise<void> {
  if (changes.length === 0) {
    return;
  }

  const BATCH_SIZE = 10;
  for (let i = 0; i < changes.length; i += BATCH_SIZE) {
    const batch = changes.slice(i, i + BATCH_SIZE);
    await Promise.all(
      batch.map(async change => {
        try {
          if (change.type === 'deleted') {
            invalidateDocument(change.uri);
          } else {
            // created 或 changed
            const fsPath = new URL(change.uri).pathname;
            const content = await fs.readFile(fsPath, 'utf8');
            await updateDocumentIndex(change.uri, content);
          }
        } catch {
          // 忽略错误（文件可能已被删除或无法读取）
        }
      })
    );
  }
}

/**
 * 处理客户端提供的文件变更事件（native 模式）
 */
export async function handleNativeFileChanges(
  changes: Array<{ uri: string; type: number }>
): Promise<void> {
  const events: FileChangeEvent[] = changes.map(ch => ({
    uri: ch.uri,
    type: ch.type === 1 ? 'created' : ch.type === 2 ? 'changed' : 'deleted',
  }));

  await processChanges(events);
}
