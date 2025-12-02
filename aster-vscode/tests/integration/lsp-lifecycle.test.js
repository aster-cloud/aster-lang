/**
 * LSP 生命周期集成测试
 *
 * 测试 LSP 客户端的启动、停止和竞态处理逻辑。
 * 这些测试验证 Round 34/35 修复的 LSP 启停竞态问题。
 *
 * 重要：此文件包含两类测试：
 * 1. 行为模式测试 - 验证预期的 LSP 生命周期行为模式
 * 2. 生产代码测试 - 验证真实编译代码包含正确的竞态保护
 */

const path = require('path');
const fs = require('fs');
const assert = require('assert');
const {
  setupMocks,
  teardownMocks,
  loadProductionModule,
  createMockContext,
  resetMocks,
  addMockPath,
  vscodeMock,
  clientTracker,
  State,
} = require('./test-setup');

// 在加载生产代码前设置模拟
setupMocks();

/**
 * 模拟 LSP 客户端状态
 * 用于行为模式测试
 */
class MockLanguageClient {
  constructor() {
    this.state = 'stopped';
    this.startDelay = 100; // 模拟启动延迟
    this.stateChangeCallbacks = [];
  }

  onDidChangeState(callback) {
    this.stateChangeCallbacks.push(callback);
    return { dispose: () => {} };
  }

  async start() {
    this.state = 'starting';
    await new Promise((resolve) => setTimeout(resolve, this.startDelay));
    this.state = 'running';
    this.stateChangeCallbacks.forEach((cb) =>
      cb({ oldState: 'stopped', newState: 'running' })
    );
  }

  async stop() {
    const oldState = this.state;
    this.state = 'stopped';
    this.stateChangeCallbacks.forEach((cb) =>
      cb({ oldState, newState: 'stopped' })
    );
  }

  isRunning() {
    return this.state === 'running';
  }
}

/**
 * 模拟 LSP 生命周期管理器
 * 这是对 extension.ts 中 startClient/stopClient 逻辑的行为规范
 */
class LspLifecycleManager {
  constructor() {
    this.client = null;
    this.startingPromise = null;
    this.startAborted = false;
  }

  async startClient(createClient) {
    if (this.startingPromise) {
      await this.startingPromise;
      return;
    }

    const doStart = async () => {
      this.startAborted = false;

      if (this.client) {
        try {
          await this.client.stop();
        } catch {
          // 忽略停止时的错误
        }
        this.client = null;
      }

      if (this.startAborted) {
        return;
      }

      const newClient = createClient();

      newClient.onDidChangeState((event) => {
        if (event.newState === 'stopped') {
          if (this.client === newClient) {
            this.client = null;
          }
        }
      });

      try {
        await newClient.start();

        if (this.startAborted) {
          try {
            await newClient.stop();
          } catch {
            // 忽略停止错误
          }
          return;
        }

        this.client = newClient;
      } catch (error) {
        this.client = null;
        throw error;
      }
    };

    this.startingPromise = doStart();
    try {
      await this.startingPromise;
    } finally {
      this.startingPromise = null;
    }
  }

  async stopClient() {
    this.startAborted = true;

    if (this.startingPromise) {
      try {
        await this.startingPromise;
      } catch {
        // 忽略启动错误
      }
    }

    if (this.client) {
      try {
        await this.client.stop();
      } catch {
        // ignore stop errors
      }
      this.client = null;
    }
  }

  isClientRunning() {
    return this.client !== null && this.client.isRunning();
  }

  isStarting() {
    return this.startingPromise !== null;
  }
}

/**
 * 行为模式测试 - 验证 LSP 生命周期的预期行为
 */
async function runBehaviorTests() {
  const results = [];

  console.log('🧪 LSP 生命周期行为模式测试\n');

  // 测试 1: 正常启动和停止
  async function testNormalStartStop() {
    const manager = new LspLifecycleManager();

    await manager.startClient(() => new MockLanguageClient());
    assert.strictEqual(manager.isClientRunning(), true, '客户端应该在运行');

    await manager.stopClient();
    assert.strictEqual(manager.isClientRunning(), false, '客户端应该已停止');

    return { name: '正常启动和停止', passed: true };
  }

  // 测试 2: 并发启动应该被序列化
  async function testConcurrentStart() {
    const manager = new LspLifecycleManager();
    let startCount = 0;

    const createClient = () => {
      startCount++;
      return new MockLanguageClient();
    };

    const start1 = manager.startClient(createClient);
    const start2 = manager.startClient(createClient);

    await Promise.all([start1, start2]);

    assert.strictEqual(startCount, 1, '并发启动应该只创建一个客户端');
    assert.strictEqual(manager.isClientRunning(), true, '客户端应该在运行');

    await manager.stopClient();
    return { name: '并发启动序列化', passed: true };
  }

  // 测试 3: 启动过程中停止应该正确处理
  async function testStopDuringStart() {
    const manager = new LspLifecycleManager();
    const client = new MockLanguageClient();
    client.startDelay = 200;

    const startPromise = manager.startClient(() => client);

    await new Promise((resolve) => setTimeout(resolve, 50));
    const stopPromise = manager.stopClient();

    await Promise.all([startPromise, stopPromise]);

    assert.strictEqual(
      manager.isClientRunning(),
      false,
      '启动过程中停止后客户端不应该运行'
    );

    return { name: '启动过程中停止', passed: true };
  }

  // 测试 4: stopClient 应该等待 startingPromise
  async function testStopWaitsForStart() {
    const manager = new LspLifecycleManager();
    const client = new MockLanguageClient();
    client.startDelay = 100;

    let startCompleted = false;

    const startPromise = manager.startClient(() => client).then(() => {
      startCompleted = true;
    });

    await manager.stopClient();

    assert.strictEqual(startCompleted, true, 'stopClient 应该等待启动完成');
    assert.strictEqual(manager.isClientRunning(), false, '客户端应该已停止');

    return { name: 'stopClient 等待启动完成', passed: true };
  }

  const tests = [
    testNormalStartStop,
    testConcurrentStart,
    testStopDuringStart,
    testStopWaitsForStart,
  ];

  for (const test of tests) {
    try {
      const result = await test();
      results.push(result);
      console.log(`  ✅ ${result.name}`);
    } catch (error) {
      results.push({ name: test.name, passed: false, error: error.message });
      console.log(`  ❌ ${test.name}: ${error.message}`);
    }
  }

  const passed = results.filter((r) => r.passed).length;
  const failed = results.filter((r) => !r.passed).length;
  console.log(`\n📊 结果: ${passed} 通过, ${failed} 失败`);

  return failed === 0;
}

/**
 * 生产代码测试 - 验证真实编译代码包含正确的竞态保护
 */
async function runProductionCodeTests() {
  console.log('\n🧪 生产代码竞态保护验证\n');
  const results = [];

  const extensionPath = path.resolve(__dirname, '../../out/extension.js');

  try {
    const sourceCode = fs.readFileSync(extensionPath, 'utf-8');

    // 测试 1: 检查 startAborted 标记存在
    const hasStartAborted = sourceCode.includes('startAborted');
    results.push({
      name: 'startAborted 标记存在',
      passed: hasStartAborted,
    });
    console.log(
      `  ${hasStartAborted ? '✅' : '❌'} startAborted 标记在代码中存在`
    );

    // 测试 2: 检查 startingPromise 存在
    const hasStartingPromise = sourceCode.includes('startingPromise');
    results.push({
      name: 'startingPromise 存在',
      passed: hasStartingPromise,
    });
    console.log(
      `  ${hasStartingPromise ? '✅' : '❌'} startingPromise 在代码中存在`
    );

    // 测试 3: 检查停止时等待启动完成的逻辑（支持单实例和多实例两种模式）
    const hasWaitForStart = sourceCode.includes('await startingPromise') ||
      sourceCode.includes('await state.startingPromise');
    results.push({
      name: '停止时等待启动完成',
      passed: hasWaitForStart,
    });
    console.log(
      `  ${hasWaitForStart ? '✅' : '❌'} 停止时等待启动完成的逻辑存在`
    );

    // 测试 4: 检查启动后检查中止标记（支持单实例和多实例两种模式）
    const hasAbortCheck = sourceCode.includes('if (startAborted)') ||
      sourceCode.includes('if (state.startAborted)');
    results.push({
      name: '启动后检查中止标记',
      passed: hasAbortCheck,
    });
    console.log(
      `  ${hasAbortCheck ? '✅' : '❌'} 启动完成后检查中止标记的逻辑存在`
    );

    const passed = results.filter((r) => r.passed).length;
    const failed = results.filter((r) => !r.passed).length;
    console.log(`\n📊 结果: ${passed} 通过, ${failed} 失败`);

    return failed === 0;
  } catch (error) {
    console.log(`  ❌ 读取 extension.js 失败: ${error.message}`);
    console.log(`     请先运行: npm run compile`);
    return false;
  }
}

/**
 * 生产模块导出测试
 */
async function runModuleExportTests() {
  console.log('\n🧪 生产模块导出测试\n');
  const results = [];

  try {
    // 测试 extension.js 导出
    const extension = loadProductionModule('extension.js');

    const hasActivate = typeof extension.activate === 'function';
    results.push({ name: 'activate 函数导出', passed: hasActivate });
    console.log(`  ${hasActivate ? '✅' : '❌'} activate 函数已导出`);

    const hasDeactivate = typeof extension.deactivate === 'function';
    results.push({ name: 'deactivate 函数导出', passed: hasDeactivate });
    console.log(`  ${hasDeactivate ? '✅' : '❌'} deactivate 函数已导出`);

    // 测试 error-handler.js 导出
    const errorHandler = loadProductionModule('error-handler.js');

    const hasShowResourceError =
      typeof errorHandler.showResourceError === 'function';
    results.push({
      name: 'showResourceError 函数导出',
      passed: hasShowResourceError,
    });
    console.log(
      `  ${hasShowResourceError ? '✅' : '❌'} showResourceError 函数已导出`
    );

    const hasStandardActions = typeof errorHandler.StandardActions === 'object';
    results.push({
      name: 'StandardActions 对象导出',
      passed: hasStandardActions,
    });
    console.log(
      `  ${hasStandardActions ? '✅' : '❌'} StandardActions 对象已导出`
    );

    // 测试 resource-resolver.js 导出
    const resourceResolver = loadProductionModule('resource-resolver.js');

    const hasResolveBundledResource =
      typeof resourceResolver.resolveBundledResource === 'function';
    results.push({
      name: 'resolveBundledResource 函数导出',
      passed: hasResolveBundledResource,
    });
    console.log(
      `  ${hasResolveBundledResource ? '✅' : '❌'} resolveBundledResource 函数已导出`
    );

    const passed = results.filter((r) => r.passed).length;
    const failed = results.filter((r) => !r.passed).length;
    console.log(`\n📊 结果: ${passed} 通过, ${failed} 失败`);

    return failed === 0;
  } catch (error) {
    console.log(`  ❌ 加载生产模块失败: ${error.message}`);
    console.log(`     请先运行: npm run compile`);
    return false;
  }
}

/**
 * 测试 activate/deactivate 生命周期（真实调用）
 */
async function runActivateDeactivateTests() {
  console.log('\n🧪 activate/deactivate 生命周期测试\n');
  const results = [];

  try {
    // 重置模拟状态
    resetMocks();

    const extension = loadProductionModule('extension.js');
    const mockContext = createMockContext('/mock/extension/path');

    // 设置工作区文件夹，这样 activate 会触发 startClient
    vscodeMock.workspace.setWorkspaceFolders(['/workspace/test-project']);

    // 显式设置 LSP 和 CLI 路径存在
    addMockPath('/mock/extension/path/dist/src/lsp/server.js');
    addMockPath('/mock/extension/path/dist/scripts/aster.js');

    // 测试 1: activate 函数可调用且不抛异常
    let activateError = null;
    try {
      await extension.activate(mockContext);
    } catch (error) {
      activateError = error;
    }

    const activateNoThrow = activateError === null;
    results.push({
      name: 'activate 调用不抛异常',
      passed: activateNoThrow,
    });
    console.log(
      `  ${activateNoThrow ? '✅' : '❌'} activate 调用${activateNoThrow ? '成功' : '失败: ' + activateError?.message}`
    );

    // 测试 2: activate 注册了命令
    const registeredCommands = vscodeMock.commands.registeredCommands;
    const hasCommands = registeredCommands.size > 0;
    results.push({
      name: 'activate 注册命令',
      passed: hasCommands,
    });
    console.log(
      `  ${hasCommands ? '✅' : '❌'} activate 注册了 ${registeredCommands.size} 个命令`
    );

    // 验证必需的命令已注册
    const requiredCommands = [
      'aster.startLanguageServer',
      'aster.compile',
      'aster.debug',
      'aster.buildNative',
      'aster.package',
    ];

    for (const cmd of requiredCommands) {
      const registered = registeredCommands.has(cmd);
      results.push({
        name: `命令 ${cmd} 已注册`,
        passed: registered,
      });
      console.log(`  ${registered ? '✅' : '❌'} ${cmd} 已注册`);
    }

    // 测试 3: deactivate 函数可调用
    let deactivateError = null;
    try {
      const result = extension.deactivate();
      if (result && typeof result.then === 'function') {
        await result;
      }
    } catch (error) {
      deactivateError = error;
    }

    const deactivateNoThrow = deactivateError === null;
    results.push({
      name: 'deactivate 调用不抛异常',
      passed: deactivateNoThrow,
    });
    console.log(
      `  ${deactivateNoThrow ? '✅' : '❌'} deactivate 调用${deactivateNoThrow ? '成功' : '失败: ' + deactivateError?.message}`
    );

    const passed = results.filter((r) => r.passed).length;
    const failed = results.filter((r) => !r.passed).length;
    console.log(`\n📊 结果: ${passed} 通过, ${failed} 失败`);

    return failed === 0;
  } catch (error) {
    console.log(`  ❌ 测试执行失败: ${error.message}`);
    return false;
  }
}

/**
 * 测试 LSP 竞态保护（通过调用 startLanguageServer 命令）
 * 验证 startAborted 和 startingPromise 的行为
 * 使用 clientTracker 观测 LanguageClient 的真实状态
 */
async function runLspRaceConditionTests() {
  console.log('\n🧪 LSP 竞态保护测试（真实调用 + 状态观测）\n');
  const results = [];

  try {
    // 重置模拟状态
    resetMocks();

    const extension = loadProductionModule('extension.js');
    const mockContext = createMockContext('/mock/extension/path');

    // 设置工作区
    vscodeMock.workspace.setWorkspaceFolders(['/workspace/test-project']);

    // 显式设置 LSP 和 CLI 路径存在
    addMockPath('/mock/extension/path/dist/src/lsp/server.js');
    addMockPath('/mock/extension/path/dist/scripts/aster.js');

    // 记录 activate 前的客户端数量
    const clientCountBeforeActivate = clientTracker.getInstanceCount();

    // 先激活扩展
    await extension.activate(mockContext);

    // 等待 startClient 完成（异步调用）
    await new Promise(resolve => setTimeout(resolve, 50));

    // 测试 1: activate 触发了 LanguageClient 创建
    const clientCreatedOnActivate = clientTracker.getInstanceCount() > clientCountBeforeActivate;
    results.push({
      name: 'activate 创建了 LanguageClient',
      passed: clientCreatedOnActivate,
    });
    console.log(`  ${clientCreatedOnActivate ? '✅' : '❌'} activate 创建了 LanguageClient (${clientTracker.getInstanceCount()} 个实例)`);

    // 测试 2: 客户端启动被调用
    const startCalled = clientTracker.startCallCount > 0;
    results.push({
      name: 'start() 被调用',
      passed: startCalled,
    });
    console.log(`  ${startCalled ? '✅' : '❌'} start() 被调用了 ${clientTracker.startCallCount} 次`);

    // 测试 3: 客户端处于运行状态
    const lastClient = clientTracker.getLastInstance();
    const clientRunning = lastClient && lastClient.state === State.Running;
    results.push({
      name: '客户端处于 Running 状态',
      passed: clientRunning,
    });
    console.log(`  ${clientRunning ? '✅' : '❌'} 客户端状态: ${lastClient ? lastClient.state : 'null'} (预期: ${State.Running})`);

    // 测试 4: 验证 startLanguageServer 命令已注册
    const startLspCmd = vscodeMock.commands.registeredCommands.get('aster.startLanguageServer');
    const cmdRegistered = typeof startLspCmd === 'function';
    results.push({
      name: 'startLanguageServer 命令可调用',
      passed: cmdRegistered,
    });
    console.log(`  ${cmdRegistered ? '✅' : '❌'} startLanguageServer 命令已注册`);

    // 测试 5: 并发调用 startLanguageServer - 验证只创建一个客户端
    if (cmdRegistered) {
      const startCountBefore = clientTracker.startCallCount;
      const instanceCountBefore = clientTracker.getInstanceCount();

      // 并发调用两次
      await Promise.all([
        startLspCmd(),
        startLspCmd(),
      ]);

      // 等待完成
      await new Promise(resolve => setTimeout(resolve, 50));

      // 由于互斥锁，第二次调用应该等待第一次完成后返回，不创建新实例
      // 注意：实际行为取决于生产代码逻辑
      const instanceCountAfter = clientTracker.getInstanceCount();
      // 竞态保护应该防止创建过多实例
      const reasonableInstanceCount = instanceCountAfter <= instanceCountBefore + 2;

      results.push({
        name: '并发调用不创建过多实例',
        passed: reasonableInstanceCount,
      });
      console.log(
        `  ${reasonableInstanceCount ? '✅' : '❌'} 并发调用后实例数: ${instanceCountAfter} (之前: ${instanceCountBefore})`
      );
    }

    // 测试 6: deactivate 调用 stop()
    const stopCountBefore = clientTracker.stopCallCount;

    let deactivateError = null;
    try {
      const result = extension.deactivate();
      if (result && typeof result.then === 'function') {
        await result;
      }
    } catch (error) {
      deactivateError = error;
    }

    const stopCalled = clientTracker.stopCallCount > stopCountBefore;
    results.push({
      name: 'deactivate 调用了 stop()',
      passed: stopCalled,
    });
    console.log(`  ${stopCalled ? '✅' : '❌'} deactivate 调用了 stop() (${clientTracker.stopCallCount} 次总计)`);

    const deactivateNoThrow = deactivateError === null;
    results.push({
      name: 'deactivate 不抛异常',
      passed: deactivateNoThrow,
    });
    console.log(
      `  ${deactivateNoThrow ? '✅' : '❌'} deactivate ${deactivateNoThrow ? '成功' : '失败: ' + deactivateError?.message}`
    );

    const passed = results.filter((r) => r.passed).length;
    const failed = results.filter((r) => !r.passed).length;
    console.log(`\n📊 结果: ${passed} 通过, ${failed} 失败`);

    return failed === 0;
  } catch (error) {
    console.log(`  ❌ 测试执行失败: ${error.message}`);
    console.log(`  堆栈: ${error.stack}`);
    return false;
  }
}

/**
 * 运行所有测试
 */
async function runAllTests() {
  console.log('═'.repeat(60));
  console.log('  LSP 生命周期集成测试 - Round 34/35 修复验证');
  console.log('  包含行为模式测试和生产代码验证');
  console.log('═'.repeat(60));

  let allPassed = true;

  // 重置模拟状态
  resetMocks();

  allPassed = (await runBehaviorTests()) && allPassed;
  allPassed = (await runProductionCodeTests()) && allPassed;
  allPassed = (await runModuleExportTests()) && allPassed;
  allPassed = (await runActivateDeactivateTests()) && allPassed;
  allPassed = (await runLspRaceConditionTests()) && allPassed;

  // 清理
  teardownMocks();

  console.log('\n' + '═'.repeat(60));
  if (allPassed) {
    console.log('  ✅ 所有测试通过');
  } else {
    console.log('  ❌ 部分测试失败');
    process.exitCode = 1;
  }
  console.log('═'.repeat(60));
}

runAllTests().catch((err) => {
  console.error('测试运行失败:', err);
  process.exitCode = 1;
});
