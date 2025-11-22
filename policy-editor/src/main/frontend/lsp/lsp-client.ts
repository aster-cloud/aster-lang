import {
  toSocket,
  WebSocketMessageReader,
  WebSocketMessageWriter,
} from '@codingame/monaco-jsonrpc';
import {
  CloseAction,
  ErrorAction,
  MonacoLanguageClient,
  MonacoServices,
} from 'monaco-languageclient';
let servicesInstalled = false;

function ensureMonacoServices(): void {
  if (!servicesInstalled) {
    MonacoServices.install();
    servicesInstalled = true;
  }
}

function toWebSocketUrl(path: string): string {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
  return `${protocol}://${window.location.host}${path}`;
}

export class AsterLspClient {
  private languageClient: MonacoLanguageClient | null = null;
  private socket: WebSocket | null = null;
  private disposed = false;

  constructor(private readonly modelUri: string) {}

  connect(): void {
    if (this.languageClient) {
      return;
    }

    // 重置 disposed 标志，允许重新连接
    this.disposed = false;

    ensureMonacoServices();

    const languageClient = this.createLanguageClient();
    this.languageClient = languageClient;
    void languageClient.start().catch((error: unknown) => {
      // 仅记录日志，错误会由 Vaadin DevTools 捕获
      console.error('无法启动 Aster LSP 客户端', error);
    });
  }

  dispose(): void {
    this.disposed = true;
    if (this.languageClient) {
      void this.languageClient.stop();
      this.languageClient = null;
    }
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.socket.close();
      this.socket = null;
    }
  }

  private createLanguageClient(): MonacoLanguageClient {
    const url = toWebSocketUrl('/ws/lsp');
    return new MonacoLanguageClient({
      name: 'Aster Language Client',
      clientOptions: {
        documentSelector: [{ language: 'aster' }],
        initializationOptions: {
          documentUri: this.modelUri,
        },
        errorHandler: {
          error: () => ({ action: ErrorAction.Continue }),
          closed: () => ({ action: CloseAction.Restart }),
        },
      },
      connectionProvider: {
        get: async (_encoding) => {
          // 不检查 disposed 状态，避免与 connect() 的竞态条件
          // 如果客户端已释放，languageClient.stop() 会处理清理
          if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            this.socket.close();
          }
          const socket = new WebSocket(url);
          this.socket = socket;
          socket.addEventListener('close', () => {
            if (this.socket === socket) {
              this.socket = null;
            }
          });
          const wrappedSocket = toSocket(socket);
          const reader = new WebSocketMessageReader(wrappedSocket);
          const writer = new WebSocketMessageWriter(wrappedSocket);
          return { reader, writer };
        },
      },
    });
  }
}
