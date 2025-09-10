#!/usr/bin/env node
import { spawn } from 'node:child_process';

function send(server: any, msg: any): void {
  const payload = JSON.stringify(msg);
  const header = `Content-Length: ${Buffer.byteLength(payload, 'utf8')}\r\n\r\n`;
  server.stdin.write(header + payload);
}

async function main(): Promise<void> {
  const server = spawn('node', ['dist/src/lsp/server.js', '--stdio'], { stdio: ['pipe', 'pipe', 'inherit'] });
  let gotInitialize = false;
  server.stdout.setEncoding('utf8');
  server.stdout.on('data', chunk => {
    const s = String(chunk);
    if (s.includes('Content-Length')) {
      const jsonStart = s.indexOf('{');
      if (jsonStart >= 0) {
        const obj = JSON.parse(s.slice(jsonStart));
        if (obj.id === 1) gotInitialize = true;
      }
    }
  });

  send(server, { jsonrpc: '2.0', id: 1, method: 'initialize', params: { processId: null, rootUri: null, capabilities: {} } });
  send(server, { jsonrpc: '2.0', method: 'initialized', params: {} });
  setTimeout(() => {
    send(server, { jsonrpc: '2.0', id: 2, method: 'shutdown' });
    send(server, { jsonrpc: '2.0', method: 'exit' });
    setTimeout(() => process.exit(gotInitialize ? 0 : 1), 200);
  }, 250);
}

main().catch(e => { console.error('lsp-smoke failed:', e); process.exit(1); });
