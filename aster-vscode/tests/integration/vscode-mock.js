/**
 * VSCode API Mock
 *
 * 提供 VSCode API 的模拟实现，用于单元测试和集成测试。
 * 这些 mock 允许在不启动 VSCode 实例的情况下测试扩展逻辑。
 *
 * 注意：使用原生 JavaScript 实现，不依赖 Jest 或其他测试框架。
 */

const path = require('path');
const EventEmitter = require('events');

/**
 * 创建可追踪调用的 stub 函数
 */
function createStub(defaultReturn = undefined) {
  const calls = [];
  const stub = function (...args) {
    calls.push(args);
    if (stub._returnValue !== undefined) {
      return stub._returnValue;
    }
    if (stub._asyncReturnValue !== undefined) {
      return Promise.resolve(stub._asyncReturnValue);
    }
    return defaultReturn;
  };
  stub.calls = calls;
  stub.callCount = () => calls.length;
  stub.lastCall = () => calls[calls.length - 1];
  stub.mockClear = () => { calls.length = 0; };
  stub.mockReturnValue = (value) => { stub._returnValue = value; return stub; };
  stub.mockResolvedValue = (value) => { stub._asyncReturnValue = value; return stub; };
  stub.wasCalled = () => calls.length > 0;
  stub.wasCalledWith = (...expectedArgs) => {
    return calls.some(callArgs =>
      expectedArgs.every((arg, i) => callArgs[i] === arg)
    );
  };
  return stub;
}

/**
 * 模拟 VSCode Uri
 */
class MockUri {
  constructor(fsPath) {
    this.fsPath = fsPath;
    this.scheme = 'file';
    this.path = fsPath;
  }

  static file(fsPath) {
    return new MockUri(fsPath);
  }

  static parse(uriString) {
    if (uriString.startsWith('file://')) {
      return new MockUri(uriString.replace('file://', ''));
    }
    return new MockUri(uriString);
  }

  toString() {
    return `file://${this.fsPath}`;
  }
}

/**
 * 模拟 OutputChannel
 */
class MockOutputChannel {
  constructor(name) {
    this.name = name;
    this.lines = [];
    this.isShown = false;
  }

  appendLine(line) {
    this.lines.push(line);
  }

  append(text) {
    if (this.lines.length === 0) {
      this.lines.push(text);
    } else {
      this.lines[this.lines.length - 1] += text;
    }
  }

  show() {
    this.isShown = true;
  }

  hide() {
    this.isShown = false;
  }

  clear() {
    this.lines = [];
  }

  dispose() {
    this.lines = [];
  }

  getContent() {
    return this.lines.join('\n');
  }
}

/**
 * 模拟 ExtensionContext
 */
class MockExtensionContext {
  constructor(extensionPath) {
    this.extensionPath = extensionPath;
    this.subscriptions = [];
    this.globalState = new Map();
    this.workspaceState = new Map();
    this.extensionUri = MockUri.file(extensionPath);
  }

  asAbsolutePath(relativePath) {
    return path.join(this.extensionPath, relativePath);
  }
}

/**
 * 模拟 WorkspaceFolder
 */
class MockWorkspaceFolder {
  constructor(fsPath, name = path.basename(fsPath), index = 0) {
    this.uri = MockUri.file(fsPath);
    this.name = name;
    this.index = index;
  }
}

/**
 * 全局配置存储（跨 getConfiguration 调用持久化）
 */
const globalConfigStore = new Map();

/**
 * 工作区变更事件监听器列表
 */
const workspaceFoldersChangeListeners = [];

/**
 * 配置变更事件监听器列表
 */
const configurationChangeListeners = [];

/**
 * 模拟 VSCode workspace
 */
const mockWorkspace = {
  workspaceFolders: [],
  textDocuments: [],

  getConfiguration(section, scope) {
    return {
      get(key, defaultValue) {
        const fullKey = section ? `${section}.${key}` : key;
        return globalConfigStore.has(fullKey) ? globalConfigStore.get(fullKey) : defaultValue;
      },
      has(key) {
        const fullKey = section ? `${section}.${key}` : key;
        return globalConfigStore.has(fullKey);
      },
      update(key, value) {
        const fullKey = section ? `${section}.${key}` : key;
        globalConfigStore.set(fullKey, value);
        return Promise.resolve();
      },
    };
  },

  /**
   * 设置配置值（测试辅助方法）
   */
  setConfig(key, value) {
    globalConfigStore.set(key, value);
  },

  /**
   * 清除所有配置
   */
  clearConfig() {
    globalConfigStore.clear();
  },

  getWorkspaceFolder(uri) {
    return this.workspaceFolders.find(
      (f) => uri.fsPath.startsWith(f.uri.fsPath)
    );
  },

  createFileSystemWatcher(pattern) {
    return {
      onDidCreate: () => ({ dispose: () => {} }),
      onDidChange: () => ({ dispose: () => {} }),
      onDidDelete: () => ({ dispose: () => {} }),
      dispose: () => {},
    };
  },

  /**
   * 注册工作区变更监听器
   * @param {Function} listener 监听器函数
   * @returns {{ dispose: Function }} 可销毁对象
   */
  onDidChangeWorkspaceFolders(listener) {
    workspaceFoldersChangeListeners.push(listener);
    return {
      dispose: () => {
        const idx = workspaceFoldersChangeListeners.indexOf(listener);
        if (idx >= 0) {
          workspaceFoldersChangeListeners.splice(idx, 1);
        }
      },
    };
  },

  /**
   * 触发工作区变更事件（测试辅助方法）
   * @param {{ added: string[], removed: string[] }} changes 变更信息
   */
  triggerWorkspaceFoldersChange(changes = {}) {
    const event = {
      added: (changes.added || []).map((f, i) => new MockWorkspaceFolder(f, path.basename(f), i)),
      removed: (changes.removed || []).map((f, i) => new MockWorkspaceFolder(f, path.basename(f), i)),
    };
    workspaceFoldersChangeListeners.forEach((listener) => {
      try {
        listener(event);
      } catch (err) {
        // 忽略监听器错误，继续触发其他监听器
      }
    });
  },

  /**
   * 清除所有工作区变更监听器（测试辅助方法）
   */
  clearWorkspaceFoldersChangeListeners() {
    workspaceFoldersChangeListeners.length = 0;
  },

  setWorkspaceFolders(folders) {
    this.workspaceFolders = folders.map(
      (f, i) => new MockWorkspaceFolder(f, path.basename(f), i)
    );
  },

  clearWorkspaceFolders() {
    this.workspaceFolders = [];
  },

  /**
   * 注册配置变更监听器
   * @param {Function} listener 监听器函数
   * @returns {{ dispose: Function }} 可销毁对象
   */
  onDidChangeConfiguration(listener) {
    configurationChangeListeners.push(listener);
    return {
      dispose: () => {
        const idx = configurationChangeListeners.indexOf(listener);
        if (idx >= 0) {
          configurationChangeListeners.splice(idx, 1);
        }
      },
    };
  },

  /**
   * 触发配置变更事件（测试辅助方法）
   * @param {{ affectsConfiguration: Function }} event 配置变更事件
   */
  triggerConfigurationChange(changedSections = []) {
    const event = {
      affectsConfiguration: (section) => changedSections.includes(section),
    };
    configurationChangeListeners.forEach((listener) => {
      try {
        listener(event);
      } catch (err) {
        // 忽略监听器错误，继续触发其他监听器
      }
    });
  },

  /**
   * 清除所有配置变更监听器（测试辅助方法）
   */
  clearConfigurationChangeListeners() {
    configurationChangeListeners.length = 0;
  },
};

/**
 * 模拟 VSCode window
 */
const mockWindow = {
  activeTextEditor: null,
  outputChannels: new Map(),

  showInformationMessage: createStub().mockResolvedValue(undefined),
  showWarningMessage: createStub().mockResolvedValue(undefined),
  showErrorMessage: createStub().mockResolvedValue(undefined),

  createOutputChannel(name) {
    if (!this.outputChannels.has(name)) {
      this.outputChannels.set(name, new MockOutputChannel(name));
    }
    return this.outputChannels.get(name);
  },

  withProgress(options, task) {
    const progress = {
      report: () => {},
    };
    const token = {
      isCancellationRequested: false,
      onCancellationRequested: () => ({ dispose: () => {} }),
    };
    return task(progress, token);
  },

  setActiveEditor(document) {
    this.activeTextEditor = document
      ? {
          document: {
            uri: MockUri.file(document.path),
            languageId: document.languageId || 'aster',
            isDirty: document.isDirty || false,
            save: createStub().mockResolvedValue(true),
          },
        }
      : null;
  },

  reset() {
    this.activeTextEditor = null;
    this.showInformationMessage = createStub().mockResolvedValue(undefined);
    this.showWarningMessage = createStub().mockResolvedValue(undefined);
    this.showErrorMessage = createStub().mockResolvedValue(undefined);
    this.outputChannels.clear();
  },
};

/**
 * 模拟 VSCode commands
 */
const mockCommands = {
  registeredCommands: new Map(),

  registerCommand(command, callback) {
    this.registeredCommands.set(command, callback);
    return { dispose: () => this.registeredCommands.delete(command) };
  },

  executeCommand: createStub().mockResolvedValue(undefined),

  reset() {
    this.registeredCommands.clear();
    this.executeCommand = createStub().mockResolvedValue(undefined);
  },
};

/**
 * 模拟 VSCode env
 */
const mockEnv = {
  openExternal: createStub().mockResolvedValue(true),
};

/**
 * 模拟 VSCode debug
 */
const mockDebug = {
  startDebugging: createStub().mockResolvedValue(true),
};

/**
 * 模拟 ProgressLocation
 */
const ProgressLocation = {
  Notification: 15,
  Window: 10,
  SourceControl: 1,
};

/**
 * 模拟 RelativePattern
 *
 * 用于多工作区场景下创建相对于工作区的文件匹配模式
 */
class MockRelativePattern {
  constructor(base, pattern) {
    this.base = base;
    this.pattern = pattern;
    // 如果 base 是 WorkspaceFolder，提取其 URI
    this.baseUri = base.uri ? base.uri : (base instanceof MockUri ? base : MockUri.file(String(base)));
  }
}

module.exports = {
  Uri: MockUri,
  RelativePattern: MockRelativePattern,
  MockOutputChannel,
  MockExtensionContext,
  MockWorkspaceFolder,
  workspace: mockWorkspace,
  window: mockWindow,
  commands: mockCommands,
  env: mockEnv,
  debug: mockDebug,
  ProgressLocation,
  createStub,

  // 重置所有 mock
  resetAllMocks() {
    mockWindow.reset();
    mockCommands.reset();
    mockWorkspace.clearWorkspaceFolders();
    mockWorkspace.clearConfig();
    mockWorkspace.clearWorkspaceFoldersChangeListeners();
    mockWorkspace.clearConfigurationChangeListeners();
    mockEnv.openExternal = createStub().mockResolvedValue(true);
    mockDebug.startDebugging = createStub().mockResolvedValue(true);
  },
};
