import ReconnectingWebSocket from 'reconnecting-websocket';
import { listen, type MessageConnection } from '@codingame/monaco-jsonrpc';
import {
  CloseAction,
  ErrorAction,
  MonacoLanguageClient,
  MonacoServices,
  createConnection,
} from 'monaco-languageclient';
import * as monaco from 'monaco-editor/esm/vs/editor/editor.api';

let servicesInstalled = false;

function ensureMonacoServices(): void {
  if (!servicesInstalled) {
    MonacoServices.install(monaco);
    servicesInstalled = true;
  }
}

function toWebSocketUrl(path: string): string {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
  return `${protocol}://${window.location.host}${path}`;
}

export class AsterLspClient {
  private languageClient: MonacoLanguageClient | null = null;
  private socket: ReconnectingWebSocket | null = null;
  private disposed = false;

  constructor(private readonly modelUri: string) {}

  connect(): void {
    if (this.languageClient || this.disposed) {
      return;
    }

    ensureMonacoServices();

    const url = toWebSocketUrl('/ws/lsp');
    this.socket = new ReconnectingWebSocket(url, [], {
      maxRetries: Infinity,
      reconnectInterval: 3000,
    });

    listen({
      webSocket: this.socket as unknown as WebSocket,
      onConnection: (connection: MessageConnection) => {
        if (this.disposed) {
          connection.dispose();
          return;
        }
        if (this.languageClient) {
          void this.languageClient.stop();
          this.languageClient = null;
        }
        const languageClient = this.createLanguageClient(connection);
        const disposable = languageClient.start();
        connection.onClose(() => disposable.dispose());
        this.languageClient = languageClient;
      },
    });
  }

  dispose(): void {
    this.disposed = true;
    if (this.languageClient) {
      void this.languageClient.stop();
      this.languageClient = null;
    }
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
  }

  private createLanguageClient(connection: MessageConnection): MonacoLanguageClient {
    return new MonacoLanguageClient({
      name: 'Aster Language Client',
      clientOptions: {
        documentSelector: [{ language: 'aster' }],
        initializationOptions: {
          documentUri: this.modelUri,
        },
        errorHandler: {
          error: () => ErrorAction.Continue,
          closed: () => CloseAction.Restart,
        },
      },
      connectionProvider: {
        get: (errorHandler, closeHandler) => {
          return Promise.resolve(createConnection(connection, errorHandler, closeHandler));
        },
      },
    });
  }
}
