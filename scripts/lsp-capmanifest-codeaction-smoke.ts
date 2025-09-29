#!/usr/bin/env node
import { spawn, type ChildProcessWithoutNullStreams } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

type Json = Record<string, any>;

function send(server: ChildProcessWithoutNullStreams, msg: Json): void {
  const payload = JSON.stringify(msg);
  const header = `Content-Length: ${Buffer.byteLength(payload, 'utf8')}\r\n\r\n`;
  server.stdin.write(header + payload);
}

async function main(): Promise<void> {
  // Create a temp manifest that denies IO by leaving allow empty
  const outDir = 'build';
  if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });
  const capsPath = path.resolve(outDir, 'tmp_caps.json');
  fs.writeFileSync(capsPath, JSON.stringify({ allow: { io: [], cpu: [] } }, null, 2) + '\n', 'utf8');

  const server = spawn('node', ['dist/src/lsp/server.js', '--stdio'], {
    stdio: ['pipe', 'pipe', 'inherit'],
    env: { ...process.env, ASTER_CAPS: capsPath },
  }) as unknown as ChildProcessWithoutNullStreams;

  server.stdout.setEncoding('utf8');
  let buffer = '';
  let diags: any[] = [];
  const DEBUG = process.argv.includes('--debug');

  server.stdout.on('data', (chunk: string | Buffer) => {
    buffer += String(chunk);
    for (;;) {
      const match = buffer.match(/^Content-Length: (\d+)\r\n\r\n/);
      if (!match) break;
      const len = Number(match[1]);
      const start = match[0].length;
      if (buffer.length < start + len) break;
      const jsonText = buffer.slice(start, start + len);
      buffer = buffer.slice(start + len);
      const obj = JSON.parse(jsonText);
      if (obj.method === 'textDocument/publishDiagnostics' && obj.params?.uri === 'file:///cap-smoke.cnl') {
        diags = obj.params.diagnostics || [];
        if (DEBUG) {
          console.log('Diagnostics:', JSON.stringify(diags, null, 2));
        }
      }
      if (obj.id === 2) {
        const actions: any[] = obj.result || [];
        if (DEBUG) {
          console.log('CodeActions:', JSON.stringify(actions.map(a => a.title), null, 2));
        }
        const hasAllowFqn = actions.some(a => typeof a.title === 'string' && a.title.includes('Allow IO for demo.capdemo.hello'));
        const hasAllowMod = actions.some(a => typeof a.title === 'string' && a.title.includes('Allow IO for demo.capdemo.*'));
        if (!hasAllowFqn || !hasAllowMod) {
          console.error('lsp-capmanifest-codeaction-smoke: expected allow actions not found');
          if (!DEBUG) {
            console.error('Titles:', actions.map(a => a.title));
            console.error('Diags:', diags);
          }
          process.exit(1);
        }
        // shutdown
        send(server, { jsonrpc: '2.0', id: 3, method: 'shutdown' });
        send(server, { jsonrpc: '2.0', method: 'exit' });
        setTimeout(() => process.exit(0), 100);
      }
    }
  });

  // Initialize
  send(server, {
    jsonrpc: '2.0',
    id: 1,
    method: 'initialize',
    params: { processId: null, rootUri: null, capabilities: {} },
  });
  send(server, { jsonrpc: '2.0', method: 'initialized', params: {} });

  // Open a document that declares IO and should trigger capability violation
  const content = [
    'This module is demo.capdemo.',
    '',
    'To hello, produce Text. It performs IO:',
    '  Return "x".',
    '',
  ].join('\n');
  send(server, {
    jsonrpc: '2.0',
    method: 'textDocument/didOpen',
    params: {
      textDocument: { uri: 'file:///cap-smoke.cnl', languageId: 'cnl', version: 1, text: content },
    },
  });

  // Request code actions after diagnostics arrive
  setTimeout(() => {
    send(server, {
      jsonrpc: '2.0',
      id: 2,
      method: 'textDocument/codeAction',
      params: {
        textDocument: { uri: 'file:///cap-smoke.cnl' },
        range: { start: { line: 2, character: 0 }, end: { line: 2, character: 1 } },
        context: { diagnostics: diags },
      },
    });
  }, 700);
}

main().catch(e => {
  console.error('lsp-capmanifest-codeaction-smoke failed:', e);
  process.exit(1);
});
