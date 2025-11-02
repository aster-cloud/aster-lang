#!/usr/bin/env node
/**
 * 资源打包复制脚本
 *
 * 在扩展打包前，从主项目复制预编译的 LSP 服务器和 CLI 到扩展目录。
 * 用途：确保 VSIX 包包含内置的 LSP 和 CLI，实现开箱即用。
 */

const fs = require('fs-extra');
const path = require('path');

// 路径配置
const projectRoot = path.resolve(__dirname, '../..');  // 仓库根目录
const extensionRoot = path.resolve(__dirname, '..');   // aster-vscode 目录

console.log('📦 开始复制内置资源...');
console.log(`项目根目录: ${projectRoot}`);
console.log(`扩展根目录: ${extensionRoot}`);
console.log('');

// 定义需要复制的资源
const resources = [
  {
    name: 'LSP 服务器',
    src: path.join(projectRoot, 'dist/src/lsp'),
    dest: path.join(extensionRoot, 'dist/src/lsp'),
  },
  {
    name: 'Node CLI',
    src: path.join(projectRoot, 'dist/scripts'),
    dest: path.join(extensionRoot, 'dist/scripts'),
  },
  {
    name: '其他 dist 文件',
    src: path.join(projectRoot, 'dist/src'),
    dest: path.join(extensionRoot, 'dist/src'),
    exclude: ['lsp'], // 排除 lsp 目录（已单独复制）
  },
  {
    name: '运行时依赖 (node_modules)',
    src: path.join(projectRoot, 'node_modules'),
    dest: path.join(extensionRoot, 'node_modules'),
    production: true, // 仅复制生产依赖
  },
];

// 获取生产依赖列表
function getProductionDependencies() {
  const pkgPath = path.join(projectRoot, 'package.json');
  if (!fs.existsSync(pkgPath)) {
    return [];
  }
  const pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf8'));
  return Object.keys(pkg.dependencies || {});
}

// 文件过滤器
function createFilter(options = {}) {
  const { exclude = [], production = false } = options;
  const prodDeps = production ? getProductionDependencies() : [];

  return (src) => {
    const basename = path.basename(src);
    const relativePath = path.relative(options.baseSrc || '', src);

    // 排除 .map 文件
    if (src.endsWith('.map')) {
      return false;
    }

    // 排除指定目录
    if (exclude.some(ex => relativePath.startsWith(ex))) {
      return false;
    }

    // 仅复制生产依赖
    if (production && src.includes('node_modules')) {
      const match = src.match(/node_modules[/\\]([^/\\]+)/);
      if (match) {
        const pkgName = match[1].startsWith('@')
          ? `${match[1]}/${src.match(/node_modules[/\\][^/\\]+[/\\]([^/\\]+)/)?.[1] || ''}`
          : match[1];
        return prodDeps.includes(pkgName);
      }
    }

    return true;
  };
}

// 复制资源
let successCount = 0;
let warningCount = 0;

resources.forEach(({ name, src, dest, exclude, production }) => {
  console.log(`📂 复制 ${name}...`);
  console.log(`  源: ${src}`);
  console.log(`  目标: ${dest}`);

  if (fs.existsSync(src)) {
    try {
      // 确保目标目录存在
      fs.ensureDirSync(path.dirname(dest));

      // 创建过滤器
      const filter = createFilter({ exclude, production, baseSrc: src });

      // 复制目录
      fs.copySync(src, dest, {
        filter,
        overwrite: true,
      });

      // 统计复制的文件（递归）
      let fileCount = 0;
      if (fs.existsSync(dest)) {
        const countFiles = (dir) => {
          const entries = fs.readdirSync(dir, { withFileTypes: true });
          entries.forEach(entry => {
            if (entry.isDirectory()) {
              countFiles(path.join(dir, entry.name));
            } else {
              fileCount++;
            }
          });
        };
        countFiles(dest);
      }

      console.log(`  ✅ 成功！复制了 ${fileCount} 个文件`);
      successCount++;
    } catch (error) {
      console.error(`  ❌ 失败: ${error.message}`);
      process.exit(1);
    }
  } else {
    console.warn(`  ⚠️  警告: 源路径不存在，跳过复制`);
    console.warn(`  提示: 请先构建主项目 (npm run build)`);
    warningCount++;
  }

  console.log('');
});

// 总结
console.log('📊 复制总结:');
console.log(`  成功: ${successCount} 项`);
console.log(`  警告: ${warningCount} 项`);

if (warningCount > 0) {
  console.log('');
  console.log('⚠️  部分资源未复制，扩展可能无法开箱即用。');
  console.log('💡 请运行以下命令构建主项目:');
  console.log('   cd .. && npm run build');
  process.exit(0);  // 不中断打包流程，仅警告
}

console.log('');
console.log('✨ 所有资源复制完成！');
