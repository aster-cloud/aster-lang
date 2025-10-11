#!/usr/bin/env ts-node
/**
 * 测试greet.cnl的hover - 使用修复后的position
 */
import { spawn } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { performance } from 'node:perf_hooks';

const server = spawn('node', ['dist/src/lsp/server.js', '--stdio'], {
  stdio: ['pipe', 'pipe', 'pipe'],
});

let buffer = '';
let msgId = 0;

function parseMessages(data: string) {
  buffer += data;
  const messages: any[] = [];

  while (true) {
    const match = buffer.match(/^Content-Length: (\d+)\r\n\r\n/);
    if (!match) break;

    const length = parseInt(match[1]!, 10);
    const start = match[0].length;

    if (buffer.length < start + length) break;

    const payload = buffer.slice(start, start + length);
    buffer = buffer.slice(start + length);

    try {
      messages.push(JSON.parse(payload));
    } catch (e) {
      console.error('Parse error:', e);
    }
  }

  return messages;
}

function send(msg: any) {
  const payload = JSON.stringify(msg);
  const header = `Content-Length: ${Buffer.byteLength(payload)}\r\n\r\n`;
  server.stdin.write(header + payload);
}

async function test() {
  console.log('[测试] 使用实际greet.cnl内容测试hover...');

  const greetText = readFileSync('cnl/examples/greet.cnl', 'utf8');
  console.log('[测试] greet.cnl内容:');
  console.log(greetText);
  console.log('[测试] ---');

  const allMessages: any[] = [];
  server.stdout.on('data', (chunk) => {
    const messages = parseMessages(chunk.toString());
    allMessages.push(...messages);
  });

  server.stderr.on('data', (chunk) => {
    console.error('[LSP stderr]', chunk.toString());
  });

  await new Promise(resolve => setTimeout(resolve, 100));

  // Initialize
  send({
    jsonrpc: '2.0',
    id: ++msgId,
    method: 'initialize',
    params: {
      processId: process.pid,
      rootUri: 'file:///tmp',
      capabilities: {
        textDocument: {
          hover: { contentFormat: ['markdown', 'plaintext'] },
        },
      },
    },
  });

  await new Promise(resolve => setTimeout(resolve, 500));

  send({ jsonrpc: '2.0', method: 'initialized', params: {} });

  // Open document
  const uri = 'file:///tmp/greet.cnl';

  send({
    jsonrpc: '2.0',
    method: 'textDocument/didOpen',
    params: {
      textDocument: {
        uri,
        languageId: 'cnl',
        version: 1,
        text: greetText,
      },
    },
  });

  await new Promise(resolve => setTimeout(resolve, 200));

  // Test hover at修复后的position: line 4, character 18
  // Line 5 in editor (0-indexed = line 4): "To greet with user: User?, produce Text:"
  //                                                   ^---- char 18
  console.log('[测试] 测试 hover position: line 4, char 18 (\'user\' 参数)');
  const hoverStart = performance.now();
  const hoverId = ++msgId;
  send({
    jsonrpc: '2.0',
    id: hoverId,
    method: 'textDocument/hover',
    params: {
      textDocument: { uri },
      position: { line: 4, character: 18 },
    },
  });

  let hoverMsg: any = null;
  let waited = 0;
  const maxWait = 2000;

  while (waited < maxWait) {
    await new Promise(resolve => setTimeout(resolve, 100));
    waited += 100;

    hoverMsg = allMessages.find(m => m.id === hoverId);
    if (hoverMsg) break;
  }

  const hoverTime = performance.now() - hoverStart;
  console.log(`[测试] hover 耗时: ${hoverTime.toFixed(2)}ms`);

  if (hoverMsg && hoverMsg.result) {
    console.log('[测试] ✅ hover 成功，返回内容:');
    console.log(JSON.stringify(hoverMsg.result, null, 2));
  } else if (hoverMsg) {
    console.log('[测试] ⚠️  hover 返回 null (position 可能不准确)');
  } else {
    console.log('[测试] ❌ hover 超时');
  }

  // Cleanup
  send({ jsonrpc: '2.0', method: 'shutdown', id: ++msgId });
  await new Promise(resolve => setTimeout(resolve, 200));
  send({ jsonrpc: '2.0', method: 'exit' });

  process.exit(hoverMsg && hoverMsg.result ? 0 : 1);
}

test().catch(err => {
  console.error('[测试] 失败:', err);
  process.exit(1);
});
