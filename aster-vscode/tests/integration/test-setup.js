/**
 * 集成测试设置模块
 *
 * 在导入编译后的生产代码之前，注入 VSCode API 模拟。
 * 这允许我们测试真实的生产代码，而不是重新实现逻辑。
 */

const path = require('path');
const Module = require('module');
const realFs = require('fs');

// 导入我们的 VSCode mock
const vscodeMock = require('./vscode-mock');

/**
 * 模拟路径列表
 * 这些路径在 existsSync 中会返回 true
 */
const mockExistingPaths = new Set();

/**
 * CLI 调用跟踪器
 * 用于捕获和验证 CLI 命令参数
 */
const cliTracker = {
  calls: [],

  reset() {
    this.calls = [];
  },

  getLastCall() {
    return this.calls[this.calls.length - 1] || null;
  },

  getCallCount() {
    return this.calls.length;
  },

  getCallsByCommand(cmd) {
    return this.calls.filter(c => c.args.includes(cmd));
  },
};

// 保存原始的 require 函数
const originalRequire = Module.prototype.require;

/**
 * 模拟 LanguageClient 状态枚举
 */
const State = {
  Stopped: 1,
  Starting: 2,
  Running: 3,
};

/**
 * 客户端实例跟踪器
 * 用于测试时观测 LanguageClient 的创建和状态
 */
const clientTracker = {
  instances: [],
  startCallCount: 0,
  stopCallCount: 0,

  reset() {
    this.instances = [];
    this.startCallCount = 0;
    this.stopCallCount = 0;
  },

  getLastInstance() {
    return this.instances[this.instances.length - 1] || null;
  },

  getInstanceCount() {
    return this.instances.length;
  },
};

/**
 * 完整的 LanguageClient Mock
 * 支持状态机、事件和生命周期
 */
class MockLanguageClient {
  constructor(id, name, serverOptions, clientOptions) {
    this.id = id;
    this.name = name;
    this.serverOptions = serverOptions;
    this.clientOptions = clientOptions;
    this._state = State.Stopped;
    this._stateChangeCallbacks = [];
    this._startDelay = 10; // 模拟启动延迟 (ms)

    // 注册到跟踪器
    clientTracker.instances.push(this);
  }

  get state() {
    return this._state;
  }

  /**
   * 注册状态变化监听器
   * 生产代码通过此方法监控客户端状态
   */
  onDidChangeState(callback) {
    this._stateChangeCallbacks.push(callback);
    return { dispose: () => {
      const idx = this._stateChangeCallbacks.indexOf(callback);
      if (idx >= 0) this._stateChangeCallbacks.splice(idx, 1);
    }};
  }

  /**
   * 触发状态变化事件
   */
  _emitStateChange(oldState, newState) {
    this._stateChangeCallbacks.forEach(cb => {
      cb({ oldState, newState });
    });
  }

  async start() {
    clientTracker.startCallCount++;
    const oldState = this._state;
    this._state = State.Starting;
    this._emitStateChange(oldState, State.Starting);

    // 模拟异步启动
    await new Promise(resolve => setTimeout(resolve, this._startDelay));

    this._state = State.Running;
    this._emitStateChange(State.Starting, State.Running);
    return Promise.resolve();
  }

  async stop() {
    clientTracker.stopCallCount++;
    const oldState = this._state;
    this._state = State.Stopped;
    this._emitStateChange(oldState, State.Stopped);
    return Promise.resolve();
  }

  /**
   * 测试辅助方法：设置启动延迟
   */
  setStartDelay(ms) {
    this._startDelay = ms;
  }
}

/**
 * 模拟的 fs 模块
 * 支持 existsSync 返回可控结果，其他方法委托给真实 fs
 *
 * 注意：测试应通过 addMockPath() 显式设置需要存在的路径，
 * 不再有默认放行逻辑，以确保测试能捕获资源缺失等场景。
 */
const mockFs = {
  ...realFs,
  existsSync(filePath) {
    // 如果路径在模拟列表中，返回 true
    if (mockExistingPaths.has(filePath)) {
      return true;
    }
    // 其他情况委托给真实 fs（不再有默认放行逻辑）
    return realFs.existsSync(filePath);
  },
};

/**
 * 模拟的 child_process 模块
 * 捕获 execFile 调用并返回成功结果
 * 支持回调模式和 promisify 模式
 */
const realChildProcess = require('child_process');
const { promisify } = require('util');

// 创建支持 promisify 的 execFile mock
function mockExecFile(command, args, options, callback) {
  // 处理参数重载：execFile(cmd, args, callback) 或 execFile(cmd, args, options, callback)
  let actualOptions = options;
  let actualCallback = callback;

  if (typeof options === 'function') {
    actualCallback = options;
    actualOptions = {};
  }

  // 记录调用
  cliTracker.calls.push({
    command,
    args: args || [],
    options: actualOptions || {},
    timestamp: Date.now(),
  });

  // 如果传入了 callback，模拟成功回调
  if (typeof actualCallback === 'function') {
    setImmediate(() => actualCallback(null, '', ''));
  }

  // 返回一个带有 stdout/stderr 事件的假进程（用于流式处理）
  const EventEmitter = require('events');
  const fakeProcess = new EventEmitter();
  fakeProcess.stdout = new EventEmitter();
  fakeProcess.stderr = new EventEmitter();
  setImmediate(() => {
    fakeProcess.emit('close', 0);
  });
  return fakeProcess;
}

// 添加 promisify 自定义实现，返回 { stdout, stderr } 对象
mockExecFile[promisify.custom] = (command, args, options) => {
  return new Promise((resolve, reject) => {
    mockExecFile(command, args, options, (error, stdout, stderr) => {
      if (error) {
        reject(error);
      } else {
        resolve({ stdout, stderr });
      }
    });
  });
};

const mockChildProcess = {
  ...realChildProcess,
  execFile: mockExecFile,
};

// 模拟的模块映射
const mockModules = {
  vscode: vscodeMock,
  fs: mockFs,
  'node:fs': mockFs,  // Node.js 16+ 使用 node: 前缀
  'child_process': mockChildProcess,
  'node:child_process': mockChildProcess,  // Node.js 16+ 使用 node: 前缀
  'vscode-languageclient/node': {
    LanguageClient: MockLanguageClient,
    State: State,
    TransportKind: {
      stdio: 0,
      ipc: 1,
      pipe: 2,
      socket: 3,
    },
  },
};

/**
 * 设置模块模拟
 * 在测试运行前调用此函数
 */
function setupMocks() {
  Module.prototype.require = function (id) {
    if (mockModules[id]) {
      return mockModules[id];
    }
    return originalRequire.apply(this, arguments);
  };
}

/**
 * 清理模块模拟
 * 在测试完成后调用
 */
function teardownMocks() {
  Module.prototype.require = originalRequire;
}

/**
 * 加载生产模块（带模拟）
 * @param {string} moduleName 模块名称（相对于 out/ 目录）
 */
function loadProductionModule(moduleName) {
  // 清除模块缓存以确保使用最新的模拟
  const modulePath = path.resolve(__dirname, '../../out', moduleName);
  delete require.cache[require.resolve(modulePath)];

  // 加载模块
  return require(modulePath);
}

/**
 * 创建模拟的扩展上下文
 */
function createMockContext(extensionPath) {
  return new vscodeMock.MockExtensionContext(extensionPath);
}

/**
 * 重置所有模拟状态
 */
function resetMocks() {
  vscodeMock.resetAllMocks();
  clientTracker.reset();
  cliTracker.reset();
  mockExistingPaths.clear();
}

/**
 * 添加模拟存在的路径
 * @param {string} filePath 要模拟存在的文件路径
 */
function addMockPath(filePath) {
  mockExistingPaths.add(filePath);
}

/**
 * 清除模拟路径列表
 */
function clearMockPaths() {
  mockExistingPaths.clear();
}

module.exports = {
  setupMocks,
  teardownMocks,
  loadProductionModule,
  createMockContext,
  resetMocks,
  addMockPath,
  clearMockPaths,
  mockFs,
  mockChildProcess,
  vscodeMock,
  clientTracker,
  cliTracker,
  State,
};
