/**
 * LSP 配置模块（Phase 0 Task 4.1）
 *
 * 从命令行参数读取配置选项，包括：
 * - --strict-pii: 将 PII 泄漏从 Warning 升级为 Error，阻止编译
 */

export interface LspConfig {
  /**
   * 严格 PII 模式
   *
   * 当启用时，PII 数据泄漏诊断从 Warning 升级为 Error，阻止编译。
   * 默认值：false（向后兼容，仅警告）
   */
  strictPiiMode: boolean;
}

/**
 * 全局配置实例
 *
 * 从 process.argv 读取命令行参数
 */
export const config: LspConfig = {
  strictPiiMode: process.argv.includes('--strict-pii'),
};

/**
 * 重置配置（用于测试）
 */
export function resetConfig(newConfig: Partial<LspConfig>): void {
  Object.assign(config, newConfig);
}
