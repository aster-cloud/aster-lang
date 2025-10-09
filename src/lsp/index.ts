import { promises as fs } from 'node:fs';
import { dirname, join, extname } from 'node:path';
import { pathToFileURL } from 'node:url';
import type { Location, Range } from 'vscode-languageserver-types';
import { canonicalize } from '../canonicalizer.js';
import { lex } from '../lexer.js';
import { parse } from '../parser.js';
import type { Module as AstModule, Span } from '../types.js';

/**
 * 表示索引模块的配置选项。
 */
export interface IndexConfig {
  /**
   * 是否启用索引持久化功能。
   */
  persistEnabled: boolean;
  /**
   * 索引文件的绝对路径，可为空表示使用默认路径。
   */
  indexPath?: string | null;
  /**
   * 自动保存索引的延迟毫秒数。
   */
  autoSaveDelay?: number;
}

/**
 * 描述单个符号的索引信息。
 */
export interface SymbolInfo {
  /**
   * 符号名称。
   */
  name: string;
  /**
   * 符号分类（例如函数、类型、变量等）。
   */
  kind: string;
  /**
   * 符号在文档中的完整范围。
   */
  range: Range;
  /**
   * 可选的精确选择范围（通常对应符号名称）。
   */
  selectionRange?: Range;
  /**
   * 关联引用位置集合，用于重用索引结果。
   */
  references?: Location[];
  /**
   * 额外描述信息，如签名或注释摘要。
   */
  detail?: string;
  /**
   * 符号所属文档的 URI，可用于交叉引用。
   */
  uri?: string;
}

/**
 * 描述单个模块的索引记录。
 */
export interface ModuleIndex {
  /**
   * 文档 URI（通常为 file:// 路径）。
   */
  uri: string;
  /**
   * 模块名称，若无法推断则为 null。
   */
  moduleName: string | null;
  /**
   * 模块内已索引的符号集合。
   */
  symbols: SymbolInfo[];
  /**
   * 该索引最后一次更新的时间戳（毫秒）。
   */
  lastModified: number;
}

const indexByUri = new Map<string, ModuleIndex>();
const indexByModule = new Map<string, ModuleIndex>();

let indexConfig: IndexConfig = {
  persistEnabled: true,
  autoSaveDelay: 1000
};

let indexWriteTimer: NodeJS.Timeout | null = null;

/**
 * 根据文档 URI 获取对应的模块索引。
 * @param uri 目标文档的 URI。
 * @returns 找到时返回索引记录，否则返回 undefined。
 */
export function getModuleIndex(uri: string): ModuleIndex | undefined {
  return indexByUri.get(uri);
}

/**
 * 获取当前工作区内所有模块的索引快照。
 * @returns 模块索引数组。
 */
export function getAllModules(): ModuleIndex[] {
  return Array.from(indexByUri.values());
}

/**
 * 根据符号名称查找其在工作区内的引用位置。
 * @param symbol 需要查找的符号名称。
 * @param excludeUri 可选的排除 URI，用于忽略当前文档。
 * @returns 匹配到的引用列表。
 */
export async function findSymbolReferences(symbol: string, excludeUri?: string): Promise<Location[]> {
  const locations: Location[] = [];

  for (const [uri, moduleIndex] of indexByUri) {
    if (excludeUri && uri === excludeUri) {
      continue;
    }

    for (const symbolInfo of moduleIndex.symbols) {
      if (symbolInfo.name === symbol) {
        locations.push({ uri, range: symbolInfo.range });
      }
    }
  }

  return locations;
}

/**
 * 更新指定文档的索引内容。
 * @param uri 目标文档的 URI。
 * @param content 文档最新内容。
 * @returns 更新完成后的模块索引。
 */
export async function updateDocumentIndex(uri: string, content: string): Promise<ModuleIndex> {
  try {
    const canonical = canonicalize(content);
    const tokens = lex(canonical);
    const ast = parse(tokens) as AstModule;

    const symbols: SymbolInfo[] = [];
    const decls = Array.isArray((ast as any)?.decls) ? ((ast as any).decls as any[]) : [];
    for (const decl of decls) {
      const kind = decl?.kind as string | undefined;
      if (!kind || (kind !== 'Func' && kind !== 'Data' && kind !== 'Enum')) {
        continue;
      }
      const name = (decl as any)?.name as string | undefined;
      if (!name) continue;
      const span = (decl as any)?.span as Span | undefined;
      const nameSpan = (decl as any)?.nameSpan as Span | undefined;

      const symbol: SymbolInfo = {
        name,
        kind: kind === 'Func' ? 'function' : 'type',
        range: ensureRange(span),
      };
      const selectionRange = optionalRange(nameSpan);
      if (selectionRange) {
        symbol.selectionRange = selectionRange;
      }
      symbols.push(symbol);
    }

    const astName = typeof ast?.name === 'string' && ast.name.length > 0 ? ast.name : null;
    const moduleName = astName ?? extractModuleName(content);
    const moduleIndex: ModuleIndex = {
      uri,
      moduleName,
      symbols,
      lastModified: Date.now(),
    };

    const previous = indexByUri.get(uri);
    if (previous?.moduleName) {
      indexByModule.delete(previous.moduleName);
    }

    indexByUri.set(uri, moduleIndex);
    if (moduleIndex.moduleName) {
      indexByModule.set(moduleIndex.moduleName, moduleIndex);
    }

    if (indexConfig.persistEnabled) {
      scheduleSaveIndex();
    }

    return moduleIndex;
  } catch (err) {
    invalidateDocument(uri);
    throw err;
  }
}

/**
 * 将指定文档从索引中移除或标记为失效。
 * @param uri 目标文档的 URI。
 */
export function invalidateDocument(uri: string): void {
  const existing = indexByUri.get(uri);
  if (existing?.moduleName) {
    indexByModule.delete(existing.moduleName);
  }
  indexByUri.delete(uri);
}

/**
 * 递归扫描目录下所有 .cnl 文件。
 * @param dir 目录路径。
 * @returns 所有 .cnl 文件的绝对路径列表。
 */
async function scanCnlFiles(dir: string): Promise<string[]> {
  const results: string[] = [];
  try {
    const entries = await fs.readdir(dir, { withFileTypes: true });
    for (const entry of entries) {
      const fullPath = join(dir, entry.name);
      if (entry.isDirectory()) {
        // 跳过常见的排除目录
        if (entry.name === 'node_modules' || entry.name === '.git' || entry.name === 'dist') {
          continue;
        }
        const subFiles = await scanCnlFiles(fullPath);
        results.push(...subFiles);
      } else if (entry.isFile() && extname(entry.name) === '.cnl') {
        results.push(fullPath);
      }
    }
  } catch {
    // 忽略无法读取的目录
  }
  return results;
}

/**
 * 重新构建工作区内给定文件夹的索引信息。
 * @param folders 工作区根文件夹路径列表。
 */
export async function rebuildWorkspaceIndex(folders: string[]): Promise<void> {
  // 1. 扫描所有 .cnl 文件
  const allFiles: string[] = [];
  for (const folder of folders) {
    const files = await scanCnlFiles(folder);
    allFiles.push(...files);
  }

  if (allFiles.length === 0) {
    return; // 没有找到任何文件
  }

  // 2. 批量异步索引（避免一次性加载过多文件）
  const BATCH_SIZE = 20;
  for (let i = 0; i < allFiles.length; i += BATCH_SIZE) {
    const batch = allFiles.slice(i, i + BATCH_SIZE);
    await Promise.all(
      batch.map(async (filePath) => {
        try {
          const content = await fs.readFile(filePath, 'utf8');
          const uri = pathToFileURL(filePath).href;
          await updateDocumentIndex(uri, content);
        } catch {
          // 忽略无法解析的文件
        }
      })
    );
  }
}

function scheduleSaveIndex(): void {
  if (indexWriteTimer) clearTimeout(indexWriteTimer);

  indexWriteTimer = setTimeout(() => {
    if (indexConfig.indexPath) {
      saveIndex(indexConfig.indexPath).catch(err => {
        console.error('Failed to save index:', err);
      });
    }
    indexWriteTimer = null;
  }, indexConfig.autoSaveDelay || 1000);
}

/**
 * 从持久化存储加载索引数据。
 * @param indexPath 索引文件路径。
 * @returns 成功加载时返回 true，失败或未找到时返回 false。
 */
export async function loadIndex(indexPath: string): Promise<boolean> {
  try {
    const content = await fs.readFile(indexPath, 'utf-8');
    const data = JSON.parse(content) as {
      indexByUri: Array<[string, ModuleIndex]>;
      indexByModule: Array<[string, ModuleIndex]>;
    };

    indexByUri.clear();
    indexByModule.clear();

    for (const [uri, index] of data.indexByUri) {
      indexByUri.set(uri, index);
    }
    for (const [moduleName, index] of data.indexByModule) {
      indexByModule.set(moduleName, index);
    }

    return true;
  } catch (error) {
    console.log(error);
    return false;
  }
}

/**
 * 将当前索引数据写入持久化存储。
 * @param indexPath 索引文件路径。
 */
export async function saveIndex(indexPath: string): Promise<void> {
  const data = {
    version: 1,
    timestamp: Date.now(),
    indexByUri: Array.from(indexByUri.entries()),
    indexByModule: Array.from(indexByModule.entries()),
  };

  await fs.mkdir(dirname(indexPath), { recursive: true });
  await fs.writeFile(indexPath, JSON.stringify(data, null, 2), 'utf-8');
}

/**
 * 更新索引模块的运行配置。
 * @param config 新配置对象。
 */
export function setIndexConfig(config: Partial<IndexConfig>): void {
  indexConfig = { ...indexConfig, ...config };
}

/**
 * 清空当前索引缓存并取消未完成的写入定时器。
 */
export function clearIndex(): void {
  indexByUri.clear();
  indexByModule.clear();
  if (indexWriteTimer) {
    clearTimeout(indexWriteTimer);
    indexWriteTimer = null;
  }
}

function ensureRange(span: Span | undefined): Range {
  if (span) {
    return {
      start: { line: Math.max(0, span.start.line - 1), character: Math.max(0, span.start.col - 1) },
      end: { line: Math.max(0, span.end.line - 1), character: Math.max(0, span.end.col - 1) },
    };
  }
  return { start: { line: 0, character: 0 }, end: { line: 0, character: 0 } };
}

function optionalRange(span: Span | undefined): Range | undefined {
  if (!span) return undefined;
  return ensureRange(span);
}

function extractModuleName(text: string): string | null {
  const match = text.match(/This module is ([A-Za-z][A-Za-z0-9_.]*)\./);
  return match?.[1] ?? null;
}
