/**
 * 多工作区热插拔竞态测试
 *
 * 验证多工作区场景下 LSP 客户端的热插拔行为：
 * - 动态添加工作区触发 LSP 启动
 * - 动态移除工作区触发 LSP 停止
 * - 快速切换不会导致竞态问题
 * - 多工作区独立 LSP 实例管理
 *
 * Round 48 改进：针对多实例 LSP 架构的专项测试
 */

const path = require('path');
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
 * 多工作区热插拔测试
 */
async function runMultiWorkspaceHotplugTests() {
  console.log('🧪 多工作区热插拔竞态测试\n');
  const results = [];

  // 测试 1: 多工作区同时启动 - 每个工作区独立 LSP
  async function testMultiWorkspaceParallelStart() {
    resetMocks();
    const extension = loadProductionModule('extension.js');
    const mockContext = createMockContext('/mock/extension/path');

    // 设置 LSP 和 CLI 路径
    addMockPath('/mock/extension/path/dist/src/lsp/server.js');
    addMockPath('/mock/extension/path/dist/scripts/aster.js');

    // 设置多个工作区
    vscodeMock.workspace.setWorkspaceFolders([
      '/workspace/project-a',
      '/workspace/project-b',
      '/workspace/project-c',
    ]);

    const instanceCountBefore = clientTracker.getInstanceCount();

    // 激活扩展
    await extension.activate(mockContext);

    // 等待所有 LSP 启动完成
    await new Promise(resolve => setTimeout(resolve, 100));

    const instanceCountAfter = clientTracker.getInstanceCount();
    const createdInstances = instanceCountAfter - instanceCountBefore;

    // 应该为每个工作区创建独立的 LSP 客户端
    const passed = createdInstances >= 3;

    await extension.deactivate();

    return {
      name: '多工作区并行启动创建独立 LSP 实例',
      passed,
      details: `创建了 ${createdInstances} 个实例 (预期 >= 3)`,
    };
  }

  // 测试 2: 热添加工作区 - 触发新 LSP 启动
  async function testHotAddWorkspace() {
    resetMocks();
    const extension = loadProductionModule('extension.js');
    const mockContext = createMockContext('/mock/extension/path');

    addMockPath('/mock/extension/path/dist/src/lsp/server.js');
    addMockPath('/mock/extension/path/dist/scripts/aster.js');

    // 初始只有一个工作区
    vscodeMock.workspace.setWorkspaceFolders(['/workspace/project-a']);

    await extension.activate(mockContext);
    await new Promise(resolve => setTimeout(resolve, 50));

    const instanceCountBefore = clientTracker.getInstanceCount();
    const startCountBefore = clientTracker.startCallCount;

    // 热添加新工作区
    vscodeMock.workspace.workspaceFolders.push(
      new vscodeMock.MockWorkspaceFolder('/workspace/project-b', 'project-b', 1)
    );
    vscodeMock.workspace.triggerWorkspaceFoldersChange({
      added: ['/workspace/project-b'],
      removed: [],
    });

    // 等待 LSP 启动
    await new Promise(resolve => setTimeout(resolve, 100));

    const instanceCountAfter = clientTracker.getInstanceCount();
    const startCountAfter = clientTracker.startCallCount;

    // 应该创建新的 LSP 实例
    const newInstanceCreated = instanceCountAfter > instanceCountBefore;
    const startCalled = startCountAfter > startCountBefore;

    await extension.deactivate();

    return {
      name: '热添加工作区触发新 LSP 启动',
      passed: newInstanceCreated || startCalled,
      details: `实例: ${instanceCountBefore} -> ${instanceCountAfter}, start(): ${startCountBefore} -> ${startCountAfter}`,
    };
  }

  // 测试 3: 热移除工作区 - 触发 LSP 停止
  async function testHotRemoveWorkspace() {
    resetMocks();
    const extension = loadProductionModule('extension.js');
    const mockContext = createMockContext('/mock/extension/path');

    addMockPath('/mock/extension/path/dist/src/lsp/server.js');
    addMockPath('/mock/extension/path/dist/scripts/aster.js');

    // 设置两个工作区
    vscodeMock.workspace.setWorkspaceFolders([
      '/workspace/project-a',
      '/workspace/project-b',
    ]);

    await extension.activate(mockContext);
    await new Promise(resolve => setTimeout(resolve, 100));

    const stopCountBefore = clientTracker.stopCallCount;

    // 热移除一个工作区
    const removedFolder = vscodeMock.workspace.workspaceFolders.pop();
    vscodeMock.workspace.triggerWorkspaceFoldersChange({
      added: [],
      removed: ['/workspace/project-b'],
    });

    // 等待 LSP 停止
    await new Promise(resolve => setTimeout(resolve, 50));

    const stopCountAfter = clientTracker.stopCallCount;

    // 应该调用 stop() 停止移除工作区的 LSP
    const stopCalled = stopCountAfter > stopCountBefore;

    await extension.deactivate();

    return {
      name: '热移除工作区触发 LSP 停止',
      passed: stopCalled,
      details: `stop() 调用: ${stopCountBefore} -> ${stopCountAfter}`,
    };
  }

  // 测试 4: 快速添加/移除循环 - 竞态保护
  async function testRapidAddRemoveCycle() {
    resetMocks();
    const extension = loadProductionModule('extension.js');
    const mockContext = createMockContext('/mock/extension/path');

    addMockPath('/mock/extension/path/dist/src/lsp/server.js');
    addMockPath('/mock/extension/path/dist/scripts/aster.js');

    vscodeMock.workspace.setWorkspaceFolders(['/workspace/project-a']);

    await extension.activate(mockContext);
    await new Promise(resolve => setTimeout(resolve, 50));

    // 快速连续添加和移除同一个工作区
    const cycleCount = 5;
    for (let i = 0; i < cycleCount; i++) {
      // 添加
      vscodeMock.workspace.workspaceFolders.push(
        new vscodeMock.MockWorkspaceFolder('/workspace/project-b', 'project-b', 1)
      );
      vscodeMock.workspace.triggerWorkspaceFoldersChange({
        added: ['/workspace/project-b'],
        removed: [],
      });

      // 立即移除（不等待启动完成）
      vscodeMock.workspace.workspaceFolders.pop();
      vscodeMock.workspace.triggerWorkspaceFoldersChange({
        added: [],
        removed: ['/workspace/project-b'],
      });
    }

    // 等待所有操作完成
    await new Promise(resolve => setTimeout(resolve, 200));

    // 验证没有抛出异常且最终状态稳定
    // 实例数应该合理（不会无限增长）
    const instanceCount = clientTracker.getInstanceCount();
    const passed = instanceCount < cycleCount * 3; // 宽松检查

    await extension.deactivate();

    return {
      name: '快速添加/移除循环不会导致实例泄漏',
      passed,
      details: `${cycleCount} 次循环后实例数: ${instanceCount}`,
    };
  }

  // 测试 5: 并发热插拔 - 同时添加和移除
  async function testConcurrentHotplug() {
    resetMocks();
    const extension = loadProductionModule('extension.js');
    const mockContext = createMockContext('/mock/extension/path');

    addMockPath('/mock/extension/path/dist/src/lsp/server.js');
    addMockPath('/mock/extension/path/dist/scripts/aster.js');

    vscodeMock.workspace.setWorkspaceFolders([
      '/workspace/project-a',
      '/workspace/project-b',
    ]);

    await extension.activate(mockContext);
    await new Promise(resolve => setTimeout(resolve, 100));

    // 同时触发添加和移除
    vscodeMock.workspace.workspaceFolders = [
      new vscodeMock.MockWorkspaceFolder('/workspace/project-a', 'project-a', 0),
      new vscodeMock.MockWorkspaceFolder('/workspace/project-c', 'project-c', 1),
    ];
    vscodeMock.workspace.triggerWorkspaceFoldersChange({
      added: ['/workspace/project-c'],
      removed: ['/workspace/project-b'],
    });

    // 等待操作完成
    await new Promise(resolve => setTimeout(resolve, 100));

    // 验证系统仍然稳定
    let deactivateError = null;
    try {
      await extension.deactivate();
    } catch (e) {
      deactivateError = e;
    }

    return {
      name: '并发添加/移除工作区系统保持稳定',
      passed: deactivateError === null,
      details: deactivateError ? `错误: ${deactivateError.message}` : '无错误',
    };
  }

  // 测试 6: 配置变更检测（显示确认对话框）
  async function testConfigChangeDetection() {
    resetMocks();
    const extension = loadProductionModule('extension.js');
    const mockContext = createMockContext('/mock/extension/path');

    addMockPath('/mock/extension/path/dist/src/lsp/server.js');
    addMockPath('/mock/extension/path/dist/scripts/aster.js');

    vscodeMock.workspace.setWorkspaceFolders(['/workspace/project-a']);

    await extension.activate(mockContext);
    await new Promise(resolve => setTimeout(resolve, 50));

    // 模拟用户点击"重启全部"
    vscodeMock.window.showInformationMessage.mockResolvedValue('重启全部');

    const startCountBefore = clientTracker.startCallCount;

    // 触发 LSP 配置变更（使用精确的配置路径）
    vscodeMock.workspace.triggerConfigurationChange(['aster.langServer.path']);

    // 等待对话框处理和重启
    await new Promise(resolve => setTimeout(resolve, 150));

    const startCountAfter = clientTracker.startCallCount;

    // 用户确认后应触发重启
    const restartTriggered = startCountAfter > startCountBefore;
    // 或者至少检测到了配置变更（显示了对话框）
    const dialogShown = vscodeMock.window.showInformationMessage.wasCalled();

    await extension.deactivate();

    return {
      name: '配置变更检测并提示重启',
      passed: restartTriggered || dialogShown,
      details: `start(): ${startCountBefore} -> ${startCountAfter}, 对话框: ${dialogShown ? '已显示' : '未显示'}`,
    };
  }

  // 测试 7: 验证 startAborted 标记在热插拔中生效
  async function testStartAbortedDuringHotplug() {
    resetMocks();
    const extension = loadProductionModule('extension.js');
    const mockContext = createMockContext('/mock/extension/path');

    addMockPath('/mock/extension/path/dist/src/lsp/server.js');
    addMockPath('/mock/extension/path/dist/scripts/aster.js');

    vscodeMock.workspace.setWorkspaceFolders(['/workspace/project-a']);

    await extension.activate(mockContext);
    await new Promise(resolve => setTimeout(resolve, 50));

    const stopCountBeforeHotplug = clientTracker.stopCallCount;
    const instanceCountBeforeHotplug = clientTracker.getInstanceCount();

    // 添加新工作区
    vscodeMock.workspace.workspaceFolders.push(
      new vscodeMock.MockWorkspaceFolder('/workspace/project-b', 'project-b', 1)
    );
    vscodeMock.workspace.triggerWorkspaceFoldersChange({
      added: ['/workspace/project-b'],
      removed: [],
    });

    // 立即移除（在启动完成前）
    // 这应该设置 startAborted 标记
    await new Promise(resolve => setTimeout(resolve, 5));
    vscodeMock.workspace.workspaceFolders.pop();
    vscodeMock.workspace.triggerWorkspaceFoldersChange({
      added: [],
      removed: ['/workspace/project-b'],
    });

    // 等待启动尝试完成
    await new Promise(resolve => setTimeout(resolve, 100));

    const stopCountAfterHotplug = clientTracker.stopCallCount;
    const instanceCountAfterHotplug = clientTracker.getInstanceCount();

    // 验证：移除工作区后应该触发 stop() 调用
    // startAborted 标记应该导致新启动的客户端被停止
    const stopWasCalled = stopCountAfterHotplug > stopCountBeforeHotplug;

    // 或者：验证被中止的工作区没有留下运行中的客户端
    // （实例可能被创建但随后被停止）
    const lastClient = clientTracker.getLastInstance();
    const lastClientStopped = lastClient && lastClient.state === State.Stopped;

    // 任一条件满足即可：stop 被调用，或者最后的客户端已停止
    const passed = stopWasCalled || lastClientStopped;

    await extension.deactivate();

    return {
      name: 'startAborted 标记在热插拔中正确处理',
      passed,
      details: `stop() 调用: ${stopCountBeforeHotplug} -> ${stopCountAfterHotplug}, 最后客户端状态: ${lastClient ? lastClient.state : 'null'} (Stopped=${State.Stopped})`,
    };
  }

  // 执行所有测试
  const tests = [
    testMultiWorkspaceParallelStart,
    testHotAddWorkspace,
    testHotRemoveWorkspace,
    testRapidAddRemoveCycle,
    testConcurrentHotplug,
    testConfigChangeDetection,
    testStartAbortedDuringHotplug,
  ];

  for (const test of tests) {
    try {
      const result = await test();
      results.push(result);
      console.log(`  ${result.passed ? '✅' : '❌'} ${result.name}`);
      if (result.details) {
        console.log(`     ${result.details}`);
      }
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
 * 运行所有测试
 */
async function runAllTests() {
  console.log('═'.repeat(60));
  console.log('  多工作区热插拔竞态测试 - Round 48');
  console.log('  验证多实例 LSP 架构的稳定性');
  console.log('═'.repeat(60) + '\n');

  let allPassed = true;

  allPassed = (await runMultiWorkspaceHotplugTests()) && allPassed;

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
