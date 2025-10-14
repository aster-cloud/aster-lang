/**
 * LSP Health 模块
 * 提供服务健康检查和状态报告功能
 */

import type { Connection } from 'vscode-languageserver/node.js';

/**
 * Health 模块返回的状态接口
 */
export interface HealthStatus {
  watchers: {
    capability: boolean;
    registered: boolean;
    mode?: 'native' | 'polling';
    isRunning?: boolean;
    trackedFiles?: number;
  };
  index: {
    files: number;
    modules: number;
  };
  queue?: {
    pending: number;
    running: number;
    completed: number;
    failed: number;
    total: number;
  };
}

/**
 * 注册 Health 相关的 LSP 处理器
 * @param connection LSP 连接对象
 * @param hasWatchedFilesCapability 客户端是否支持文件监视
 * @param watcherRegistered 文件监视器是否已注册
 * @param getAllModules 获取所有模块的函数
 * @param getWatcherStatus 获取文件监控状态的函数（可选）
 * @param getQueueStats 获取任务队列统计的函数（可选）
 */
export function registerHealthHandlers(
  connection: Connection,
  hasWatchedFilesCapability: boolean,
  watcherRegistered: boolean,
  getAllModules: () => Array<{ moduleName: string | null }>,
  getWatcherStatus?: () => {
    enabled: boolean;
    mode: 'native' | 'polling';
    isRunning: boolean;
    trackedFiles: number;
  },
  getQueueStats?: () => {
    pending: number;
    running: number;
    completed: number;
    failed: number;
    cancelled: number;
    total: number;
  }
): void {
  const HEALTH_METHOD = 'aster/health';

  connection.onRequest(HEALTH_METHOD, (): HealthStatus => {
    const modules = getAllModules();
    const moduleNames = new Set<string>();
    for (const m of modules) if (m.moduleName) moduleNames.add(m.moduleName);

    const watcherStatus = getWatcherStatus?.();
    const queueStats = getQueueStats?.();

    const result: HealthStatus = {
      watchers: {
        capability: hasWatchedFilesCapability,
        registered: watcherRegistered,
      },
      index: {
        files: modules.length,
        modules: moduleNames.size,
      },
    };

    // 仅在 watcherStatus 存在时添加可选字段
    if (watcherStatus) {
      result.watchers.mode = watcherStatus.mode;
      result.watchers.isRunning = watcherStatus.isRunning;
      result.watchers.trackedFiles = watcherStatus.trackedFiles;
    }

    // 添加队列统计
    if (queueStats) {
      result.queue = {
        pending: queueStats.pending,
        running: queueStats.running,
        completed: queueStats.completed,
        failed: queueStats.failed,
        total: queueStats.total,
      };
    }

    return result;
  });
}
