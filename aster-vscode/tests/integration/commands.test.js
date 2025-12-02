/**
 * 命令集成测试
 *
 * 测试 Package 和 BuildNative 命令的核心逻辑。
 * 这些测试验证 Round 34/35 修复的命令问题。
 *
 * 重要：此文件测试真实的生产代码，而非重新实现的逻辑。
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
  cliTracker,
  clientTracker,
} = require('./test-setup');

// 在加载生产代码前设置模拟
setupMocks();

/**
 * 测试 JAR 文件名生成逻辑（验证生产代码）
 * 验证 M2 修复：.astr 文件不再生成 foo.astr.jar
 */
function testJarNaming() {
  console.log('🧪 JAR 命名测试（生产代码验证）\n');
  const results = [];

  // 读取生产代码验证实际实现
  const extensionPath = path.resolve(__dirname, '../../out/extension.js');
  const sourceCode = fs.readFileSync(extensionPath, 'utf-8');

  // 测试 1: 验证生产代码使用 path.parse().name 而非 path.basename
  const usesPathParse = sourceCode.includes('path.parse(') && sourceCode.includes('.name');
  const notUsesBasenameForJar = !sourceCode.includes("path.basename(activeFile.path, '.aster')");

  results.push({
    name: '生产代码使用 path.parse().name',
    passed: usesPathParse,
  });
  console.log(
    `  ${usesPathParse ? '✅' : '❌'} 生产代码使用 path.parse().name 获取文件名`
  );

  results.push({
    name: '生产代码不使用有 bug 的 basename 方式',
    passed: notUsesBasenameForJar,
  });
  console.log(
    `  ${notUsesBasenameForJar ? '✅' : '❌'} 生产代码不使用 path.basename(path, '.aster')`
  );

  // 测试 2: 验证 path.parse().name 的行为（确认我们的理解正确）
  const testCases = [
    { input: '/workspace/src/main.aster', expected: 'main' },
    { input: '/workspace/src/app.astr', expected: 'app' },
    { input: '/workspace/src/module.ASTER', expected: 'module' },
    { input: 'simple.aster', expected: 'simple' },
    { input: 'simple.astr', expected: 'simple' },
  ];

  console.log('\n  📌 path.parse().name 行为验证:');
  for (const tc of testCases) {
    const actual = path.parse(tc.input).name;
    const passed = actual === tc.expected;
    results.push({ input: tc.input, expected: tc.expected, actual, passed });
    console.log(
      `  ${passed ? '✅' : '❌'} path.parse("${tc.input}").name = "${actual}"`
    );
  }

  // 测试 3: 对比旧实现的 bug
  console.log('\n  📌 对比有 bug 的旧实现:');
  const buggyResult = path.basename('/workspace/app.astr', '.aster');
  const hasBug = buggyResult === 'app.astr'; // 旧实现无法正确处理 .astr
  results.push({
    name: '确认旧实现有 bug',
    passed: hasBug,
  });
  console.log(
    `  ${hasBug ? '✅' : '❌'} path.basename("app.astr", ".aster") = "${buggyResult}" (bug: 不移除 .astr 后缀)`
  );

  const passed = results.filter((r) => r.passed).length;
  const failed = results.filter((r) => !r.passed).length;
  console.log(`\n📊 结果: ${passed} 通过, ${failed} 失败`);

  return failed === 0;
}

/**
 * 测试 package 命令的实际行为
 * 通过调用注册的命令验证文件检测逻辑
 */
async function testPackageCommandBehavior() {
  console.log('\n🧪 Package 命令行为测试（真实调用）\n');
  const results = [];

  try {
    // 重置模拟状态
    resetMocks();

    const extension = loadProductionModule('extension.js');
    const mockContext = createMockContext('/mock/extension/path');

    // 设置工作区
    vscodeMock.workspace.setWorkspaceFolders(['/workspace/test-project']);

    // 显式设置 LSP 和 CLI 路径存在（测试需要这些路径可用）
    addMockPath('/mock/extension/path/dist/src/lsp/server.js');
    addMockPath('/mock/extension/path/dist/scripts/aster.js');

    // 激活扩展
    await extension.activate(mockContext);

    // 测试 1: package 命令已注册
    const packageCmd = vscodeMock.commands.registeredCommands.get('aster.package');
    const cmdRegistered = typeof packageCmd === 'function';
    results.push({
      name: 'package 命令已注册',
      passed: cmdRegistered,
    });
    console.log(`  ${cmdRegistered ? '✅' : '❌'} aster.package 命令已注册`);

    if (!cmdRegistered) {
      console.log(`\n📊 结果: ${results.filter(r => r.passed).length} 通过, ${results.filter(r => !r.passed).length} 失败`);
      return false;
    }

    // 测试 2: 没有活动文件时显示警告
    vscodeMock.window.setActiveEditor(null);
    vscodeMock.window.showWarningMessage.mockClear();

    await packageCmd();

    const warningShownNoFile = vscodeMock.window.showWarningMessage.wasCalled();
    results.push({
      name: '无活动文件时显示警告',
      passed: warningShownNoFile,
    });
    console.log(`  ${warningShownNoFile ? '✅' : '❌'} 没有活动文件时 showWarningMessage 被调用`);

    // 测试 3: 非 .aster/.astr 文件时显示警告
    vscodeMock.window.setActiveEditor({
      path: '/workspace/test-project/readme.md',
      languageId: 'markdown',
    });
    vscodeMock.window.showWarningMessage.mockClear();

    await packageCmd();

    const warningShownWrongType = vscodeMock.window.showWarningMessage.wasCalled();
    results.push({
      name: '非 Aster 文件时显示警告',
      passed: warningShownWrongType,
    });
    console.log(`  ${warningShownWrongType ? '✅' : '❌'} 非 Aster 文件时 showWarningMessage 被调用`);

    // 测试 4: 有 .astr 文件时尝试执行（不显示文件类型警告）
    vscodeMock.window.setActiveEditor({
      path: '/workspace/test-project/app.astr',
      languageId: 'aster',
    });
    vscodeMock.window.showWarningMessage.mockClear();

    // 命令会因为 CLI 不存在而失败，但不应该显示"请打开一个 .aster 文件"的警告
    await packageCmd();

    // 检查是否没有显示"请打开一个 .aster 文件"警告
    // 如果显示了其他错误（如 CLI 不存在），那是预期的
    const lastWarningCall = vscodeMock.window.showWarningMessage.lastCall();
    const noFileTypeWarning = !lastWarningCall ||
      !lastWarningCall[0]?.includes('请打开一个 .aster 文件');

    results.push({
      name: '.astr 文件被正确识别',
      passed: noFileTypeWarning,
    });
    console.log(`  ${noFileTypeWarning ? '✅' : '❌'} .astr 文件被正确识别为 Aster 文件`);

    // 清理
    await extension.deactivate();

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
 * 测试 JAR 命名的运行时验证
 * 通过捕获 CLI 调用参数，验证 .astr 文件生成正确的 JAR 名称
 */
async function testJarNamingRuntime() {
  console.log('\n🧪 JAR 命名运行时验证测试\n');
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

    // 激活扩展
    await extension.activate(mockContext);

    const packageCmd = vscodeMock.commands.registeredCommands.get('aster.package');
    if (!packageCmd) {
      console.log('  ❌ package 命令未注册');
      return false;
    }

    // 测试用例：不同后缀的文件
    const testCases = [
      { file: 'app.astr', expectedJarName: 'app.jar' },
      { file: 'main.aster', expectedJarName: 'main.jar' },
      { file: 'module.ASTER', expectedJarName: 'module.jar' },
    ];

    for (const tc of testCases) {
      // 设置活动编辑器
      vscodeMock.window.setActiveEditor({
        path: `/workspace/test-project/${tc.file}`,
        languageId: 'aster',
      });

      // 清除之前的 CLI 调用记录
      cliTracker.reset();

      // 执行 package 命令
      await packageCmd();

      // 等待异步操作完成（CLI 调用通过 setImmediate 异步执行）
      await new Promise(resolve => setTimeout(resolve, 100));

      // 检查 CLI 调用
      const jarCalls = cliTracker.getCallsByCommand('jar');
      const passed = jarCalls.length > 0;

      if (passed) {
        // 验证 JAR 路径
        const jarCall = jarCalls[0];
        const outputArg = jarCall.args.find((arg, i, arr) =>
          i > 0 && arr[i - 1] === '--output'
        );

        const jarPathCorrect = outputArg && outputArg.endsWith(tc.expectedJarName);
        results.push({
          name: `${tc.file} -> ${tc.expectedJarName}`,
          passed: jarPathCorrect,
        });
        console.log(
          `  ${jarPathCorrect ? '✅' : '❌'} ${tc.file} -> ${outputArg ? path.basename(outputArg) : 'undefined'} (预期: ${tc.expectedJarName})`
        );
      } else {
        results.push({
          name: `${tc.file} CLI 调用`,
          passed: false,
        });
        console.log(`  ❌ ${tc.file}: jar 命令未被调用`);
      }
    }

    // 清理
    await extension.deactivate();

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
 * 测试 debugCommand 多工作区绑定
 * 验证 Round 41 修复：startDebugging 收到正确的 workspaceFolder
 */
async function testDebugCommandMultiWorkspace() {
  console.log('\n🧪 Debug 命令多工作区绑定测试\n');
  const results = [];

  try {
    // 重置模拟状态
    resetMocks();

    const extension = loadProductionModule('extension.js');
    const mockContext = createMockContext('/mock/extension/path');

    // 设置多工作区环境
    vscodeMock.workspace.setWorkspaceFolders([
      '/workspace/project1',
      '/workspace/project2',
    ]);

    // 显式设置 LSP 和 CLI 路径存在
    addMockPath('/mock/extension/path/dist/src/lsp/server.js');
    addMockPath('/mock/extension/path/dist/scripts/aster.js');

    // 配置调试所需的 mainClass
    vscodeMock.workspace.setConfig('aster.debug.mainClass', 'com.example.Main');

    // 激活扩展
    await extension.activate(mockContext);

    const debugCmd = vscodeMock.commands.registeredCommands.get('aster.debug');
    if (!debugCmd) {
      console.log('  ❌ debug 命令未注册');
      return false;
    }

    // 测试 1: 设置活动文件为第二个工作区的文件
    vscodeMock.window.setActiveEditor({
      path: '/workspace/project2/src/app.aster',
      languageId: 'aster',
    });

    // 清除之前的 startDebugging 调用
    vscodeMock.debug.startDebugging.mockClear();

    // 执行 debug 命令
    await debugCmd();

    // 等待异步操作完成
    await new Promise(resolve => setTimeout(resolve, 150));

    // 检查 startDebugging 是否被调用
    const debugCalled = vscodeMock.debug.startDebugging.wasCalled();
    results.push({
      name: 'startDebugging 被调用',
      passed: debugCalled,
    });
    console.log(`  ${debugCalled ? '✅' : '❌'} startDebugging 被调用`);

    if (debugCalled) {
      // 验证传入的 workspaceFolder 是正确的（第二个工作区）
      const callArgs = vscodeMock.debug.startDebugging.lastCall();
      const workspaceFolder = callArgs[0];

      // workspaceFolder 应该是 project2，因为活动文件在 project2 中
      const correctFolder = workspaceFolder &&
        workspaceFolder.uri &&
        workspaceFolder.uri.fsPath === '/workspace/project2';

      results.push({
        name: 'workspaceFolder 正确绑定到文件所属工作区',
        passed: correctFolder,
      });
      console.log(
        `  ${correctFolder ? '✅' : '❌'} workspaceFolder: ${workspaceFolder?.uri?.fsPath || 'undefined'} (预期: /workspace/project2)`
      );

      // 验证 debugConfig 包含正确的信息
      const debugConfig = callArgs[1];
      const hasMainClass = debugConfig && debugConfig.mainClass === 'com.example.Main';
      results.push({
        name: 'debugConfig 包含 mainClass',
        passed: hasMainClass,
      });
      console.log(
        `  ${hasMainClass ? '✅' : '❌'} debugConfig.mainClass: ${debugConfig?.mainClass || 'undefined'}`
      );
    }

    // 清理
    await extension.deactivate();

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
 * 测试真实的 resource-resolver 模块
 * 验证 L5 修复：workspace URI 传递和资源解析优先级
 */
function testResourceResolverProduction() {
  console.log('\n🧪 生产代码 resource-resolver 测试\n');
  const results = [];

  try {
    // 加载真实的生产代码模块
    const resourceResolver = loadProductionModule('resource-resolver.js');

    // 创建模拟上下文
    const mockExtensionPath = '/mock/extension/path';
    const mockContext = createMockContext(mockExtensionPath);

    // 设置工作区
    vscodeMock.workspace.setWorkspaceFolders(['/workspace/project1']);

    // 测试 1: 当内置资源不存在时，使用用户配置或降级路径
    const result1 = resourceResolver.resolveBundledResource(
      mockContext,
      'nonexistent/path.js',
      'langServer.path',
      'fallback/path.js'
    );

    // 由于内置资源不存在且配置为空，应该使用降级路径
    const expectedPath1 = path.resolve('/workspace/project1', 'fallback/path.js');
    const passed1 = result1 === expectedPath1;
    results.push({
      name: '降级路径解析',
      passed: passed1,
    });
    console.log(
      `  ${passed1 ? '✅' : '❌'} 降级路径: "${result1}" (预期: "${expectedPath1}")`
    );

    // 测试 2: 验证模块导出正确
    const hasExport = typeof resourceResolver.resolveBundledResource === 'function';
    results.push({
      name: '模块导出 resolveBundledResource 函数',
      passed: hasExport,
    });
    console.log(
      `  ${hasExport ? '✅' : '❌'} resolveBundledResource 函数已导出`
    );

    const passed = results.filter((r) => r.passed).length;
    const failed = results.filter((r) => !r.passed).length;
    console.log(`\n📊 结果: ${passed} 通过, ${failed} 失败`);

    return failed === 0;
  } catch (error) {
    console.log(`  ❌ 加载生产模块失败: ${error.message}`);
    console.log(`     这可能是因为尚未编译 TypeScript 代码。`);
    console.log(`     请先运行: npm run compile`);
    return false;
  }
}

/**
 * 测试 BuildNative 命令的功能状态标记
 * 验证 P1-2 修复：命令标题准确描述当前功能（编译到 JVM）和未来计划（原生构建开发中）
 */
function testBuildNativeExperimental() {
  console.log('\n🧪 BuildNative 命令标题验证测试\n');
  const results = [];

  // 读取真实的 package.json 验证命令标题
  const packageJsonPath = path.resolve(__dirname, '../../package.json');
  let packageJson;

  try {
    packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf-8'));
  } catch (error) {
    console.log(`  ❌ 无法读取 package.json: ${error.message}`);
    return false;
  }

  // 查找 buildNative 命令
  const commands = packageJson.contributes?.commands || [];
  const buildNativeCmd = commands.find((cmd) => cmd.command === 'aster.buildNative');

  // 测试 1: 命令存在
  const cmdExists = !!buildNativeCmd;
  results.push({ name: 'buildNative 命令存在', passed: cmdExists });
  console.log(`  ${cmdExists ? '✅' : '❌'} buildNative 命令在 package.json 中定义`);

  if (buildNativeCmd) {
    // 测试 2: 命令标题包含 JVM（描述当前功能）和开发中（描述未来计划）
    const titleHasJvm = buildNativeCmd.title.includes('JVM');
    const titleHasDevStatus = buildNativeCmd.title.includes('开发中');
    const titleCorrect = titleHasJvm && titleHasDevStatus;
    results.push({
      name: '命令标题准确描述功能状态',
      passed: titleCorrect,
    });
    console.log(
      `  ${titleCorrect ? '✅' : '❌'} 命令标题: "${buildNativeCmd.title}" (应包含 JVM 和 开发中)`
    );
  }

  // 测试 3: 验证激活事件包含 buildNative
  const activationEvents = packageJson.activationEvents || [];
  const hasActivation = activationEvents.includes('onCommand:aster.buildNative');
  results.push({ name: '激活事件包含 buildNative', passed: hasActivation });
  console.log(`  ${hasActivation ? '✅' : '❌'} 激活事件已配置`);

  const passed = results.filter((r) => r.passed).length;
  const failed = results.filter((r) => !r.passed).length;
  console.log(`\n📊 结果: ${passed} 通过, ${failed} 失败`);

  return failed === 0;
}

/**
 * 测试 Thenable 处理逻辑（使用真实生产代码）
 * 验证 M4 修复：使用 Promise.resolve 统一处理
 */
async function testThenableHandling() {
  console.log('\n🧪 Thenable 处理测试（生产代码）\n');
  const results = [];

  try {
    // 加载真实的 error-handler 模块
    const errorHandler = loadProductionModule('error-handler.js');

    // 模拟 VSCode Thenable（不是 Promise 但有 then 方法）
    class MockThenable {
      constructor(value) {
        this.value = value;
      }

      then(onFulfilled, onRejected) {
        try {
          const result = onFulfilled(this.value);
          return new MockThenable(result);
        } catch (error) {
          if (onRejected) {
            return new MockThenable(onRejected(error));
          }
          throw error;
        }
      }
    }

    // 测试 1: 验证生产代码使用 Promise.resolve
    // 读取源代码验证实现
    const fs = require('fs');
    const errorHandlerPath = path.resolve(__dirname, '../../out/error-handler.js');
    const sourceCode = fs.readFileSync(errorHandlerPath, 'utf-8');

    const usesPromiseResolve = sourceCode.includes('await Promise.resolve(action.handler())');
    results.push({
      name: '生产代码使用 Promise.resolve 处理 handler',
      passed: usesPromiseResolve,
    });
    console.log(
      `  ${usesPromiseResolve ? '✅' : '❌'} 生产代码使用 await Promise.resolve(action.handler())`
    );

    // 测试 2: 实际调用 showResourceError 并验证 Thenable 被正确处理
    let handlerExecuted = false;
    let handlerValue = null;

    // 配置 mock 返回用户选择
    vscodeMock.window.showErrorMessage.mockResolvedValue('测试操作');

    const testAction = {
      label: '测试操作',
      handler: () => {
        handlerExecuted = true;
        // 返回一个 Thenable，不是 Promise
        return new MockThenable('thenable-result');
      },
    };

    // 调用真实的 showResourceError 函数
    await errorHandler.showResourceError('LSP', '/test/path', [testAction]);

    results.push({
      name: 'showResourceError 执行 Thenable handler',
      passed: handlerExecuted,
    });
    console.log(
      `  ${handlerExecuted ? '✅' : '❌'} showResourceError 成功执行了返回 Thenable 的 handler`
    );

    // 测试 3: 验证没有因为 Thenable 抛出异常
    const outputChannel = vscodeMock.window.outputChannels.get('Aster');
    const hasError = outputChannel?.getContent().includes('[ERROR] 操作执行失败');
    results.push({
      name: 'Thenable handler 没有导致错误',
      passed: !hasError,
    });
    console.log(
      `  ${!hasError ? '✅' : '❌'} Thenable handler 执行没有抛出错误`
    );

    const passed = results.filter((r) => r.passed).length;
    const failed = results.filter((r) => !r.passed).length;
    console.log(`\n📊 结果: ${passed} 通过, ${failed} 失败`);

    return failed === 0;
  } catch (error) {
    console.log(`  ❌ 测试执行失败: ${error.message}`);
    console.log(`     请先运行: npm run compile`);
    return false;
  }
}

/**
 * 测试扩展模块导出
 * 验证生产代码正确导出 activate 和 deactivate
 */
function testExtensionExports() {
  console.log('\n🧪 扩展模块导出测试\n');
  const results = [];

  try {
    const extension = loadProductionModule('extension.js');

    // 测试 1: activate 函数导出
    const hasActivate = typeof extension.activate === 'function';
    results.push({ name: 'activate 函数导出', passed: hasActivate });
    console.log(`  ${hasActivate ? '✅' : '❌'} activate 函数已导出`);

    // 测试 2: deactivate 函数导出
    const hasDeactivate = typeof extension.deactivate === 'function';
    results.push({ name: 'deactivate 函数导出', passed: hasDeactivate });
    console.log(`  ${hasDeactivate ? '✅' : '❌'} deactivate 函数已导出`);

    const passed = results.filter((r) => r.passed).length;
    const failed = results.filter((r) => !r.passed).length;
    console.log(`\n📊 结果: ${passed} 通过, ${failed} 失败`);

    return failed === 0;
  } catch (error) {
    console.log(`  ❌ 加载 extension.js 失败: ${error.message}`);
    console.log(`     请先运行: npm run compile`);
    return false;
  }
}

/**
 * 测试工作区变更事件处理
 * 验证 Round 42 修复：移除最后一个工作区时 LSP 停止
 * Round 44 优化：使用 clientTracker 直接断言 stop/start 调用计数
 */
async function testWorkspaceFolderChange() {
  console.log('\n🧪 工作区变更事件处理测试\n');
  const results = [];

  try {
    // 重置模拟状态
    resetMocks();

    const extension = loadProductionModule('extension.js');
    const mockContext = createMockContext('/mock/extension/path');

    // 设置初始工作区
    vscodeMock.workspace.setWorkspaceFolders(['/workspace/project1']);

    // 显式设置 LSP 和 CLI 路径存在
    addMockPath('/mock/extension/path/dist/src/lsp/server.js');
    addMockPath('/mock/extension/path/dist/scripts/aster.js');

    // 激活扩展（会注册 workspace 变更监听器）
    await extension.activate(mockContext);

    // 等待 LSP 启动完成
    await new Promise(resolve => setTimeout(resolve, 100));

    // 记录激活后的初始计数
    const initialStartCount = clientTracker.startCallCount;
    const initialStopCount = clientTracker.stopCallCount;

    console.log(`  📊 激活后状态: startCount=${initialStartCount}, stopCount=${initialStopCount}`);

    // 测试 1: 移除所有工作区时应调用 stopClient
    // 模拟移除所有工作区
    vscodeMock.workspace.clearWorkspaceFolders();

    // 触发工作区变更事件
    vscodeMock.workspace.triggerWorkspaceFoldersChange({
      removed: ['/workspace/project1'],
    });

    // 等待事件处理完成
    await new Promise(resolve => setTimeout(resolve, 100));

    // 验证：stopCallCount 应该增加（stopClient 被调用）
    const stopCountAfterRemove = clientTracker.stopCallCount;
    const stopWasCalled = stopCountAfterRemove > initialStopCount;

    results.push({
      name: '移除工作区时 stopClient 被调用',
      passed: stopWasCalled,
    });
    console.log(
      `  ${stopWasCalled ? '✅' : '❌'} 移除工作区: stopCount ${initialStopCount} -> ${stopCountAfterRemove} (增量: ${stopCountAfterRemove - initialStopCount})`
    );

    // 测试 2: 添加新工作区时应启动 LSP
    const startCountBeforeAdd = clientTracker.startCallCount;
    vscodeMock.workspace.setWorkspaceFolders(['/workspace/project2']);

    // 触发工作区变更事件
    vscodeMock.workspace.triggerWorkspaceFoldersChange({
      added: ['/workspace/project2'],
    });

    // 等待事件处理完成
    await new Promise(resolve => setTimeout(resolve, 100));

    // 验证：startCallCount 应该增加（startClient 被调用）
    const startCountAfterAdd = clientTracker.startCallCount;
    const startWasCalled = startCountAfterAdd > startCountBeforeAdd;

    results.push({
      name: '添加工作区时 startClient 被调用',
      passed: startWasCalled,
    });
    console.log(
      `  ${startWasCalled ? '✅' : '❌'} 添加工作区: startCount ${startCountBeforeAdd} -> ${startCountAfterAdd} (增量: ${startCountAfterAdd - startCountBeforeAdd})`
    );

    // 清理
    await extension.deactivate();

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
 * 测试非工作区文件处理
 * 验证 Round 41/42 修复：文件不属于任何工作区时返回正确错误
 */
async function testNonWorkspaceFile() {
  console.log('\n🧪 非工作区文件处理测试\n');
  const results = [];

  try {
    // 重置模拟状态
    resetMocks();

    const extension = loadProductionModule('extension.js');
    const mockContext = createMockContext('/mock/extension/path');

    // 设置工作区为 /workspace/project1
    vscodeMock.workspace.setWorkspaceFolders(['/workspace/project1']);

    // 显式设置 LSP 和 CLI 路径存在
    addMockPath('/mock/extension/path/dist/src/lsp/server.js');
    addMockPath('/mock/extension/path/dist/scripts/aster.js');

    // 激活扩展
    await extension.activate(mockContext);

    const compileCmd = vscodeMock.commands.registeredCommands.get('aster.compile');
    if (!compileCmd) {
      console.log('  ❌ compile 命令未注册');
      return false;
    }

    // 测试: 设置活动文件为不在工作区内的文件
    // /external/file.aster 不在 /workspace/project1 下
    vscodeMock.window.setActiveEditor({
      path: '/external/project/file.aster',
      languageId: 'aster',
    });

    // 清除之前的消息
    vscodeMock.window.showErrorMessage.mockClear();

    // 执行 compile 命令
    await compileCmd();

    // 等待异步操作完成
    await new Promise(resolve => setTimeout(resolve, 100));

    // 验证：应该显示工作区相关的错误，而不是 CLI 未找到的错误
    const errorCalled = vscodeMock.window.showErrorMessage.wasCalled();
    const lastErrorCall = vscodeMock.window.showErrorMessage.lastCall();

    // 检查错误消息是否包含"工作区"或"Workspace"
    // 而不是误导性的"CLI 未找到"
    let correctError = false;
    if (lastErrorCall && lastErrorCall[0]) {
      const errorMsg = lastErrorCall[0];
      // 如果是工作区错误，说明 getWorkspaceRoot 正确返回了 null
      // 如果是 CLI 错误，说明存在回退到其他目录的问题
      correctError = !errorMsg.includes('CLI 未找到') || errorMsg.includes('Workspace');
    }

    results.push({
      name: '非工作区文件显示正确错误',
      passed: errorCalled && correctError,
    });
    console.log(
      `  ${errorCalled && correctError ? '✅' : '❌'} 非工作区文件显示工作区相关错误 (非误导性 CLI 错误)`
    );

    // 清理
    await extension.deactivate();

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
 * 测试 workspace-utils 共享模块
 * 验证 Round 43 修复：getWorkspaceRoot 已提取到共享模块
 */
function testWorkspaceUtilsModule() {
  console.log('\n🧪 workspace-utils 共享模块测试\n');
  const results = [];

  try {
    // 加载共享模块
    const workspaceUtils = loadProductionModule('workspace-utils.js');

    // 测试 1: 模块导出 getWorkspaceRoot 函数
    const hasExport = typeof workspaceUtils.getWorkspaceRoot === 'function';
    results.push({
      name: 'getWorkspaceRoot 函数已导出',
      passed: hasExport,
    });
    console.log(
      `  ${hasExport ? '✅' : '❌'} workspace-utils 导出 getWorkspaceRoot 函数`
    );

    if (!hasExport) {
      console.log(`\n📊 结果: 0 通过, 1 失败`);
      return false;
    }

    // 测试 2: 无工作区时返回 null
    vscodeMock.workspace.clearWorkspaceFolders();
    const resultNoWorkspace = workspaceUtils.getWorkspaceRoot();
    const noWorkspaceCorrect = resultNoWorkspace === null;
    results.push({
      name: '无工作区时返回 null',
      passed: noWorkspaceCorrect,
    });
    console.log(
      `  ${noWorkspaceCorrect ? '✅' : '❌'} 无工作区时返回 null (实际: ${resultNoWorkspace})`
    );

    // 测试 3: 有工作区时返回第一个工作区路径
    vscodeMock.workspace.setWorkspaceFolders(['/workspace/project1', '/workspace/project2']);
    const resultWithWorkspace = workspaceUtils.getWorkspaceRoot();
    const withWorkspaceCorrect = resultWithWorkspace === '/workspace/project1';
    results.push({
      name: '有工作区时返回第一个工作区',
      passed: withWorkspaceCorrect,
    });
    console.log(
      `  ${withWorkspaceCorrect ? '✅' : '❌'} 有工作区时返回第一个工作区 (实际: ${resultWithWorkspace})`
    );

    // 测试 4: 传入 fileUri 时返回对应工作区
    const fileUri = vscodeMock.Uri.file('/workspace/project2/src/app.aster');
    const resultWithUri = workspaceUtils.getWorkspaceRoot(fileUri);
    const withUriCorrect = resultWithUri === '/workspace/project2';
    results.push({
      name: 'fileUri 返回对应工作区',
      passed: withUriCorrect,
    });
    console.log(
      `  ${withUriCorrect ? '✅' : '❌'} fileUri 返回对应工作区 (实际: ${resultWithUri})`
    );

    // 测试 5: fileUri 不属于任何工作区时返回 null
    const externalUri = vscodeMock.Uri.file('/external/file.aster');
    const resultExternal = workspaceUtils.getWorkspaceRoot(externalUri);
    const externalCorrect = resultExternal === null;
    results.push({
      name: '外部文件 URI 返回 null',
      passed: externalCorrect,
    });
    console.log(
      `  ${externalCorrect ? '✅' : '❌'} 外部文件 URI 返回 null (实际: ${resultExternal})`
    );

    const passed = results.filter((r) => r.passed).length;
    const failed = results.filter((r) => !r.passed).length;
    console.log(`\n📊 结果: ${passed} 通过, ${failed} 失败`);

    return failed === 0;
  } catch (error) {
    console.log(`  ❌ 加载 workspace-utils.js 失败: ${error.message}`);
    console.log(`     请先运行: npm run compile`);
    return false;
  }
}

/**
 * 运行所有测试
 */
async function runAllTests() {
  console.log('═'.repeat(60));
  console.log('  命令集成测试 - Round 34/35/41/42/43 修复验证');
  console.log('  测试真实生产代码（非重新实现）');
  console.log('═'.repeat(60));

  let allPassed = true;

  // 重置模拟状态
  resetMocks();

  allPassed = testJarNaming() && allPassed;
  allPassed = (await testPackageCommandBehavior()) && allPassed;
  allPassed = (await testJarNamingRuntime()) && allPassed;
  allPassed = (await testDebugCommandMultiWorkspace()) && allPassed;
  allPassed = testBuildNativeExperimental() && allPassed;
  allPassed = (await testThenableHandling()) && allPassed;
  allPassed = testResourceResolverProduction() && allPassed;
  allPassed = testExtensionExports() && allPassed;
  // Round 43 新增测试
  allPassed = (await testWorkspaceFolderChange()) && allPassed;
  allPassed = (await testNonWorkspaceFile()) && allPassed;
  allPassed = testWorkspaceUtilsModule() && allPassed;

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
