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

/** LSP 连接状态类型 */
export type LspConnectionStatus = 'connected' | 'connecting' | 'disconnected' | 'error';

/** LSP 健康响应接口（与后端 health.ts 对应） */
export interface LspHealthResponse {
  status: 'ok' | 'starting' | 'error';
  timestamp: string;
  process?: {
    pid: number;
    uptime: number;
    memory: {
      rss: number;
      heapUsed: number;
      heapTotal: number;
    };
    cpu?: {
      percent: number;
    };
  };
  metadata?: {
    restartCount: number;
    lastRestartReason?: string;
  };
}

/** 状态变化事件监听器类型 */
export type StatusChangeListener = (status: LspConnectionStatus) => void;

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
  private _status: LspConnectionStatus = 'disconnected';
  private statusListeners: Set<StatusChangeListener> = new Set();

  constructor(private readonly modelUri: string) {}

  /** 获取当前连接状态 */
  get status(): LspConnectionStatus {
    return this._status;
  }

  /** 设置状态并通知监听器 */
  private setStatus(newStatus: LspConnectionStatus): void {
    if (this._status !== newStatus) {
      this._status = newStatus;
      this.statusListeners.forEach(listener => listener(newStatus));
    }
  }

  /** 添加状态变化监听器 */
  onStatusChange(listener: StatusChangeListener): () => void {
    this.statusListeners.add(listener);
    return () => this.statusListeners.delete(listener);
  }

  /** 发送健康检查请求 */
  async checkHealth(): Promise<LspHealthResponse | null> {
    if (!this.languageClient || this._status !== 'connected') {
      return null;
    }
    try {
      const response = await this.languageClient.sendRequest('aster/health', {});
      return response as LspHealthResponse;
    } catch (error) {
      console.warn('LSP 健康检查失败:', error);
      return null;
    }
  }

  connect(): void {
    if (this.languageClient) {
      return;
    }

    // 重置 disposed 标志，允许重新连接
    this.disposed = false;
    this.setStatus('connecting');

    ensureMonacoServices();

    const languageClient = this.createLanguageClient();
    this.languageClient = languageClient;
    void languageClient.start()
      .then(() => {
        // 身份校验：确保回调对应当前活跃的客户端实例
        if (this.languageClient !== languageClient) {
          return; // 旧连接的回调，忽略
        }
        if (!this.disposed) {
          this.setStatus('connected');
        }
      })
      .catch((error: unknown) => {
        // 身份校验：确保回调对应当前活跃的客户端实例
        if (this.languageClient !== languageClient) {
          // 旧连接的回调，仅清理旧客户端，不影响新连接
          try {
            void languageClient.stop();
          } catch {
            // 忽略停止时的错误
          }
          return;
        }
        console.error('无法启动 Aster LSP 客户端', error);
        this.setStatus('error');
        // 清理失败的客户端实例
        try {
          void languageClient.stop();
        } catch {
          // 忽略停止时的错误
        }
        this.languageClient = null;
        if (this.socket) {
          try {
            this.socket.close();
          } catch {
            // 忽略关闭时的错误
          }
          this.socket = null;
        }
      });
  }

  dispose(): void {
    this.disposed = true;
    this.setStatus('disconnected');
    this.statusListeners.clear();
    if (this.languageClient) {
      void this.languageClient.stop();
      this.languageClient = null;
    }
    // 处理所有可关闭的 WebSocket 状态（CONNECTING 或 OPEN）
    if (this.socket) {
      const state = this.socket.readyState;
      if (state === WebSocket.CONNECTING || state === WebSocket.OPEN) {
        try {
          this.socket.close();
        } catch {
          // 忽略关闭时的错误
        }
      }
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
          socket.addEventListener('open', () => {
            if (!this.disposed && this.socket === socket) {
              this.setStatus('connected');
            }
          });
          socket.addEventListener('close', (event) => {
            if (this.socket === socket) {
              this.socket = null;
              if (!this.disposed) {
                // 如果是被拒绝的连接（TRY_AGAIN_LATER = 1013）
                if (event.code === 1013) {
                  console.warn('LSP 服务器已达连接上限，稍后重试');
                }
                this.setStatus('disconnected');
              }
            }
          });
          socket.addEventListener('error', () => {
            if (!this.disposed && this.socket === socket) {
              this.setStatus('error');
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
