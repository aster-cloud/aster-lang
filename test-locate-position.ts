#!/usr/bin/env ts-node
/**
 * 测试改进后的 locatePosition 函数
 */
import fs from 'node:fs/promises';
import { generateMediumProject, generateLargeProgram } from './test/generators.js';

type Position = { line: number; character: number };

function locatePosition(text: string, search: string, offset = 0, context?: 'parameter'): Position {
  let targetIndex: number;

  if (context === 'parameter') {
    const paramPattern = new RegExp(`\\b(with|and)\\s+(${search})\\s*:`, 'g');
    const match = paramPattern.exec(text);
    if (!match) throw new Error(`在文本中找不到参数：${search}`);
    targetIndex = match.index + match[1]!.length + 1;
  } else {
    const index = text.indexOf(search);
    if (index === -1) throw new Error(`在文本中找不到片段：${search}`);
    targetIndex = index + offset;
  }

  const untilTarget = text.slice(0, targetIndex);
  const line = untilTarget.split(/\r?\n/).length - 1;
  const lastLineBreak = untilTarget.lastIndexOf('\n');
  const character = targetIndex - (lastLineBreak + 1);
  return { line, character };
}

async function test() {
  console.log('=== Testing Small Project (greet.cnl) ===');
  const greetText = await fs.readFile('cnl/examples/greet.cnl', 'utf8');
  const smallPos = locatePosition(greetText, 'user', 0, 'parameter');
  console.log(`Position for 'user' parameter: line ${smallPos.line}, char ${smallPos.character}`);
  console.log(`Expected: line 4, char 18`);
  console.log(`Match: ${smallPos.line === 4 && smallPos.character === 18 ? '✅' : '❌'}`);

  console.log('\n=== Testing Medium Project ===');
  const mediumModules = generateMediumProject(40, 42);
  const mediumText = mediumModules.get('benchmark.medium.common')!;
  const mediumPos = locatePosition(mediumText, 'prefix', 0, 'parameter');
  console.log(`Position for 'prefix' parameter: line ${mediumPos.line}, char ${mediumPos.character}`);
  console.log(`Expected: line 8, char 23`);
  console.log(`Match: ${mediumPos.line === 8 && mediumPos.character === 23 ? '✅' : '❌'}`);

  console.log('\n=== Testing Large Project ===');
  const largeText = generateLargeProgram(50);
  const largePos = locatePosition(largeText, 'user', 0, 'parameter');
  console.log(`Position for 'user' parameter: line ${largePos.line}, char ${largePos.character}`);
  console.log(`Expected: line 5, char 17`);
  console.log(`Match: ${largePos.line === 5 && largePos.character === 17 ? '✅' : '❌'}`);
}

test().catch(console.error);
