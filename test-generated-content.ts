#!/usr/bin/env ts-node
/**
 * 测试生成的 medium 和 large 项目内容，找到正确的 hover position
 */
import { generateMediumProject, generateLargeProgram } from './test/generators.js';

console.log('=== Medium Project Common Module ===');
const mediumModules = generateMediumProject(40, 42);
const commonModule = mediumModules.get('benchmark.medium.common');
if (commonModule) {
  const lines = commonModule.split('\n');
  lines.forEach((line, idx) => {
    if (line.includes('buildRequestId')) {
      console.log(`Line ${idx}: ${line}`);
      // Line 52: "To buildRequestId with prefix: Text and id: Number, produce Text:"
      // 搜索字符串 "buildRequestId" 在第 52 行，offset 2 应该指向 "ildRequestId" 的 "i"
      // 我们需要找到参数 "prefix" 或 "id"
      const prefixPos = line.indexOf('prefix');
      const idPos = line.indexOf('id:');
      console.log(`  - "prefix" at char: ${prefixPos}`);
      console.log(`  - "id:" at char: ${idPos}`);
    }
  });
}

console.log('\n=== Large Project ===');
const largeContent = generateLargeProgram(50);
const largeLines = largeContent.split('\n');
largeLines.slice(0, 20).forEach((line, idx) => {
  if (line.includes('process0')) {
    console.log(`Line ${idx}: ${line}`);
    // Line 5: "To process0 with user: User, produce Status:"
    // 搜索 "process0"，offset 3 应该指向 "cess0" 的第二个 "s"
    // 我们需要找到参数 "user"
    const userPos = line.indexOf('user:');
    console.log(`  - "user:" at char: ${userPos}`);
  }
});
