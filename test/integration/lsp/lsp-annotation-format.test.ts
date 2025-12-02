#!/usr/bin/env node
/**
 * Annotation 格式化函数单元测试
 * 验证 formatAnnotation, formatAnnotations, formatFieldDetail 函数的正确性
 */

import { formatAnnotation, formatAnnotations, formatFieldDetail } from '../../../src/lsp/completion.js';
import type { Annotation, Span, Type as AstType } from '../../../src/types.js';

function assert(condition: boolean, message: string): void {
  if (!condition) throw new Error(message);
}

function createAnnotation(name: string, params?: [string, unknown][]): Annotation {
  return {
    name,
    params: new Map(params || []),
  };
}

function createSpan(): Span {
  return {
    start: { line: 0, col: 0 },
    end: { line: 0, col: 0 },
  };
}

function createTypeName(name: string): AstType {
  return { kind: 'TypeName', name, annotations: [], span: createSpan() };
}

// ============================================================================
// formatAnnotation 测试
// ============================================================================

async function testFormatAnnotationNoParams(): Promise<void> {
  const annotation = createAnnotation('NotEmpty');
  const result = formatAnnotation(annotation);
  assert(result === '@NotEmpty', `应格式化为 "@NotEmpty"，实际: "${result}"`);
  console.log('✓ formatAnnotation - 无参数注解');
}

async function testFormatAnnotationRangeParams(): Promise<void> {
  const annotation = createAnnotation('Range', [['min', 0], ['max', 100]]);
  const result = formatAnnotation(annotation);
  assert(result === '@Range(min: 0, max: 100)', `应格式化为 "@Range(min: 0, max: 100)"，实际: "${result}"`);
  console.log('✓ formatAnnotation - Range 注解（数字参数）');
}

async function testFormatAnnotationPatternParams(): Promise<void> {
  const annotation = createAnnotation('Pattern', [['regexp', '^[a-z]+$']]);
  const result = formatAnnotation(annotation);
  // 字符串参数应该用单引号包裹
  assert(result === "@Pattern(regexp: '^[a-z]+$')", `应格式化为 "@Pattern(regexp: '^[a-z]+\$')"，实际: "${result}"`);
  console.log('✓ formatAnnotation - Pattern 注解（字符串参数）');
}

async function testFormatAnnotationStringEscaping(): Promise<void> {
  const annotation = createAnnotation('Pattern', [['regexp', "it's"]]);
  const result = formatAnnotation(annotation);
  // 内部单引号应该被转义
  assert(result === "@Pattern(regexp: 'it\\'s')", `应正确转义单引号，实际: "${result}"`);
  console.log('✓ formatAnnotation - 字符串参数转义');
}

async function testFormatAnnotationMultipleParams(): Promise<void> {
  const annotation = createAnnotation('Custom', [['min', 0], ['max', 100], ['message', 'error']]);
  const result = formatAnnotation(annotation);
  // 参数顺序保持不变（Map 保持插入顺序）
  assert(result.includes('min: 0'), '应包含 min 参数');
  assert(result.includes('max: 100'), '应包含 max 参数');
  assert(result.includes("message: 'error'"), '应包含 message 参数');
  console.log('✓ formatAnnotation - 多个参数');
}

// ============================================================================
// formatAnnotations 测试
// ============================================================================

async function testFormatAnnotationsEmpty(): Promise<void> {
  const result1 = formatAnnotations(undefined);
  const result2 = formatAnnotations([]);
  assert(result1 === '', '未定义应返回空字符串');
  assert(result2 === '', '空数组应返回空字符串');
  console.log('✓ formatAnnotations - 空输入');
}

async function testFormatAnnotationsSingle(): Promise<void> {
  const annotations = [createAnnotation('NotEmpty')];
  const result = formatAnnotations(annotations);
  assert(result === '@NotEmpty', `应格式化为 "@NotEmpty"，实际: "${result}"`);
  console.log('✓ formatAnnotations - 单个注解');
}

async function testFormatAnnotationsMultiple(): Promise<void> {
  const annotations = [
    createAnnotation('NotEmpty'),
    createAnnotation('Range', [['min', 0], ['max', 100]]),
  ];
  const result = formatAnnotations(annotations);
  assert(result === '@NotEmpty @Range(min: 0, max: 100)', `应用空格分隔多个注解，实际: "${result}"`);
  console.log('✓ formatAnnotations - 多个注解');
}

async function testFormatAnnotationsMixed(): Promise<void> {
  const annotations = [
    createAnnotation('NotEmpty'),
    createAnnotation('Pattern', [['regexp', '^[a-z]+@[a-z]+\\.[a-z]+$']]),
  ];
  const result = formatAnnotations(annotations);
  assert(result.includes('@NotEmpty'), '应包含 @NotEmpty');
  assert(result.includes('@Pattern'), '应包含 @Pattern');
  assert(result.includes('^[a-z]+@[a-z]+\\.[a-z]+$'), '应包含正则表达式');
  console.log('✓ formatAnnotations - 混合注解');
}

// ============================================================================
// formatFieldDetail 测试
// ============================================================================

async function testFormatFieldDetailNoAnnotations(): Promise<void> {
  const field = { type: createTypeName('Text') };
  const result = formatFieldDetail(field);
  assert(result === 'Text', `无注解应只返回类型，实际: "${result}"`);
  console.log('✓ formatFieldDetail - 无注解');
}

async function testFormatFieldDetailWithAnnotation(): Promise<void> {
  const field = {
    type: createTypeName('Text'),
    annotations: [createAnnotation('NotEmpty')],
  };
  const result = formatFieldDetail(field);
  assert(result === '@NotEmpty Text', `应格式化为 "@NotEmpty Text"，实际: "${result}"`);
  console.log('✓ formatFieldDetail - 带注解');
}

async function testFormatFieldDetailMultipleAnnotations(): Promise<void> {
  const field = {
    type: createTypeName('Int'),
    annotations: [
      createAnnotation('NotEmpty'),
      createAnnotation('Range', [['min', 18], ['max', 120]]),
    ],
  };
  const result = formatFieldDetail(field);
  assert(result === '@NotEmpty @Range(min: 18, max: 120) Int', `应格式化所有注解和类型，实际: "${result}"`);
  console.log('✓ formatFieldDetail - 多个注解');
}

async function testFormatFieldDetailEmptyAnnotations(): Promise<void> {
  const field = { type: createTypeName('Bool'), annotations: [] };
  const result = formatFieldDetail(field);
  assert(result === 'Bool', `空注解数组应只返回类型，实际: "${result}"`);
  console.log('✓ formatFieldDetail - 空注解数组');
}

// ============================================================================
// 复杂类型测试
// ============================================================================

async function testFormatFieldDetailComplexType(): Promise<void> {
  const field = {
    type: {
      kind: 'Maybe' as const,
      type: createTypeName('Text'),
      span: createSpan(),
    },
    annotations: [createAnnotation('NotEmpty')],
  };
  // 注意：这个测试验证 formatFieldDetail 能处理复杂类型
  // typeText 函数会将 Maybe<Text> 格式化
  const result = formatFieldDetail(field);
  assert(result.includes('@NotEmpty'), '应包含注解');
  assert(result.includes('Maybe'), '应包含类型');
  console.log('✓ formatFieldDetail - 复杂类型（Maybe<T>）');
}

// ============================================================================
// 边界情况测试
// ============================================================================

async function testEdgeCases(): Promise<void> {
  // 测试空参数名
  const annotation1 = createAnnotation('Custom', [['', 'value']]);
  const result1 = formatAnnotation(annotation1);
  assert(result1.includes('value'), '应处理空参数名');

  // 测试特殊字符
  const annotation2 = createAnnotation('Pattern', [['regexp', '^.*$']]);
  const result2 = formatAnnotation(annotation2);
  assert(result2.includes('^.*$'), '应保留正则表达式特殊字符');

  // 测试数字类型（整数、浮点数）
  const annotation3 = createAnnotation('Range', [['min', 0.5], ['max', 99.9]]);
  const result3 = formatAnnotation(annotation3);
  assert(result3.includes('0.5'), '应正确处理浮点数');
  assert(result3.includes('99.9'), '应正确处理浮点数');

  // 测试布尔值
  const annotation4 = createAnnotation('Custom', [['enabled', true], ['strict', false]]);
  const result4 = formatAnnotation(annotation4);
  assert(result4.includes('true'), '应正确处理布尔值');
  assert(result4.includes('false'), '应正确处理布尔值');

  console.log('✓ 边界情况测试通过');
}

// ============================================================================
// 实际示例测试（模拟真实使用场景）
// ============================================================================

async function testRealWorldExamples(): Promise<void> {
  // 示例 1: UserProfile 数据类型
  const userIdField = {
    type: createTypeName('Text'),
    annotations: [createAnnotation('NotEmpty')],
  };
  assert(formatFieldDetail(userIdField) === '@NotEmpty Text', 'userId 字段格式化错误');

  const ageField = {
    type: createTypeName('Int'),
    annotations: [createAnnotation('Range', [['min', 18], ['max', 120]])],
  };
  assert(formatFieldDetail(ageField) === '@Range(min: 18, max: 120) Int', 'age 字段格式化错误');

  const emailField = {
    type: createTypeName('Text'),
    annotations: [createAnnotation('Pattern', [['regexp', '^[a-z]+@[a-z]+\\.[a-z]+$']])],
  };
  const emailResult = formatFieldDetail(emailField);
  assert(emailResult.includes('@Pattern'), 'email 字段应包含 Pattern 注解');
  assert(emailResult.includes('Text'), 'email 字段应包含 Text 类型');

  // 示例 2: 多个注解组合
  const complexField = {
    type: createTypeName('Text'),
    annotations: [
      createAnnotation('NotEmpty'),
      createAnnotation('Pattern', [['regexp', '^[A-Z]']]),
    ],
  };
  const complexResult = formatFieldDetail(complexField);
  assert(complexResult.includes('@NotEmpty'), '应包含 NotEmpty 注解');
  assert(complexResult.includes('@Pattern'), '应包含 Pattern 注解');
  assert(complexResult.endsWith('Text'), '应以类型结尾');

  console.log('✓ 真实示例测试通过');
}

// ============================================================================
// 主测试函数
// ============================================================================

async function main(): Promise<void> {
  console.log('Running LSP annotation format tests...\n');

  try {
    // formatAnnotation 测试
    await testFormatAnnotationNoParams();
    await testFormatAnnotationRangeParams();
    await testFormatAnnotationPatternParams();
    await testFormatAnnotationStringEscaping();
    await testFormatAnnotationMultipleParams();

    // formatAnnotations 测试
    await testFormatAnnotationsEmpty();
    await testFormatAnnotationsSingle();
    await testFormatAnnotationsMultiple();
    await testFormatAnnotationsMixed();

    // formatFieldDetail 测试
    await testFormatFieldDetailNoAnnotations();
    await testFormatFieldDetailWithAnnotation();
    await testFormatFieldDetailMultipleAnnotations();
    await testFormatFieldDetailEmptyAnnotations();
    await testFormatFieldDetailComplexType();

    // 边界情况和真实示例
    await testEdgeCases();
    await testRealWorldExamples();

    console.log('\n✅ All annotation format tests passed (22 tests).');
  } catch (error) {
    console.error('\n❌ Test failed:', error);
    process.exit(1);
  }
}

main();
