import { LitElement, css, html } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import * as monaco from 'monaco-editor/esm/vs/editor/editor.api';
import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker';
import jsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker';
import tsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker';
// CSS import removed - Monaco styling will be handled by Vaadin theme
import { AsterLspClient, LspConnectionStatus, LspHealthResponse } from '../lsp/lsp-client';

const ASTER_LANGUAGE_ID = 'aster';
let workersRegistered = false;
let languageRegistered = false;

function ensureWorkers(): void {
  if (workersRegistered) {
    return;
  }
  workersRegistered = true;
  (self as any).MonacoEnvironment = {
    getWorker(_: string, label: string) {
      if (label === 'json') {
        return new jsonWorker();
      }
      if (label === 'typescript' || label === 'javascript') {
        return new tsWorker();
      }
      return new editorWorker();
    },
  };
}

function ensureAsterLanguage(): void {
  if (languageRegistered) {
    return;
  }
  languageRegistered = true;
  const keywords = [
    'module',
    'import',
    'from',
    'expose',
    'policy',
    'rule',
    'when',
    'then',
    'else',
    'and',
    'or',
    'not',
    'with',
    'given',
    'where',
    'if',
    'to',
    'as',
    'return',
  ];
  const booleans = ['true', 'false'];
  const builtins = [
    'score',
    'approve',
    'deny',
    'flag',
    'emit',
    'explain',
  ];
  const keywordPattern = new RegExp(`\\b(?:${keywords.join('|')})\\b`, 'i');
  const booleanPattern = new RegExp(`\\b(?:${booleans.join('|')})\\b`, 'i');
  const builtinPattern = new RegExp(`\\b(?:${builtins.join('|')})\\b`, 'i');

  monaco.languages.register({ id: ASTER_LANGUAGE_ID });
  monaco.languages.setMonarchTokensProvider(ASTER_LANGUAGE_ID, {
    ignoreCase: true,
    keywords,
    operators: ['+', '-', '*', '/', '%', '>', '<', '>=', '<=', '==', '!=', '=>'],
    symbols: /[=><!~?:&|+\-*/^%]+/,
    tokenizer: {
      root: [
        [/\@[a-zA-Z_.-]+/, 'annotation'],
        [/"([^"\\]|\\.)*"/, 'string'],
        [/`([^`\\]|\\.)*`/, 'string'],
        [/[{}()\[\]]/, '@brackets'],
        [/[a-zA-Z_][\w.]*/, 'identifier'],
        [/[0-9]+(\.[0-9]+)?/, 'number'],
        [/\s+/, 'white'],
        [/--.*$/, 'comment'],
        [booleanPattern, 'boolean'],
        [keywordPattern, 'keyword'],
        [builtinPattern, 'type.identifier'],
      ],
    },
  });

  monaco.languages.setLanguageConfiguration(ASTER_LANGUAGE_ID, {
    comments: {
      lineComment: '--',
    },
    brackets: [
      ['{', '}'],
      ['[', ']'],
      ['(', ')'],
    ],
    autoClosingPairs: [
      { open: '"', close: '"' },
      { open: '`', close: '`' },
      { open: '(', close: ')' },
      { open: '[', close: ']' },
      { open: '{', close: '}' },
    ],
  });
}

@customElement('monaco-editor-component')
export class MonacoEditorComponent extends LitElement {
  @property({ type: String }) value = '';
  @property({ type: String }) language = ASTER_LANGUAGE_ID;
  @property({ type: String }) theme: 'vs' | 'vs-dark' | 'hc-black' = 'vs-dark';
  @property({ type: Number }) fontSize = 14;
  @property({ type: Boolean }) minimap = true;
  @property({ type: Boolean }) folding = true;
  @property({ type: String, attribute: 'model-uri' }) modelUri = 'inmemory://aster/policy.aster';

  @state() private ready = false;
  @state() private lspStatus: LspConnectionStatus = 'disconnected';
  @state() private lspHealth: LspHealthResponse | null = null;

  private editor: monaco.editor.IStandaloneCodeEditor | null = null;
  private model: monaco.editor.ITextModel | null = null;
  private suppressModelEvent = false;
  private lspClient: AsterLspClient | null = null;
  private healthPollInterval?: number;
  private statusUnsubscribe?: () => void;

  /** 健康检查轮询间隔（毫秒） */
  private static readonly HEALTH_POLL_INTERVAL = 5000;

  static styles = css`
    :host {
      display: block;
      width: 100%;
      height: 100%;
      min-height: 300px;
    }

    .editor-wrapper {
      position: relative;
      width: 100%;
      height: 100%;
    }

    .editor-container {
      width: 100%;
      height: 100%;
    }

    .lsp-status-indicator {
      position: absolute;
      top: 8px;
      right: 8px;
      z-index: 100;
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 4px 8px;
      border-radius: 4px;
      background-color: rgba(30, 30, 30, 0.85);
      font-size: 12px;
      color: #ccc;
      cursor: default;
      user-select: none;
    }

    .status-dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      flex-shrink: 0;
    }

    .status-dot.connected { background-color: #4caf50; }
    .status-dot.connecting { background-color: #ff9800; animation: pulse 1.5s infinite; }
    .status-dot.disconnected { background-color: #9e9e9e; }
    .status-dot.error { background-color: #f44336; }

    @keyframes pulse {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.5; }
    }

    .status-text {
      white-space: nowrap;
    }

    .lsp-status-indicator:hover .tooltip {
      display: block;
    }

    .tooltip {
      display: none;
      position: absolute;
      top: 100%;
      right: 0;
      margin-top: 4px;
      padding: 8px 12px;
      background-color: rgba(30, 30, 30, 0.95);
      border: 1px solid #555;
      border-radius: 4px;
      font-size: 11px;
      line-height: 1.6;
      white-space: pre-line;
      min-width: 180px;
      z-index: 101;
    }
  `;

  render() {
    return html`
      <div class="editor-wrapper">
        ${this.renderLspStatusIndicator()}
        <div id="editor" class="editor-container" part="editor"></div>
      </div>
    `;
  }

  /** 渲染 LSP 状态指示器 */
  private renderLspStatusIndicator() {
    const statusText: Record<LspConnectionStatus, string> = {
      connected: 'LSP 已连接',
      connecting: 'LSP 连接中...',
      disconnected: 'LSP 未连接',
      error: 'LSP 错误',
    };

    return html`
      <div class="lsp-status-indicator">
        <span class="status-dot ${this.lspStatus}"></span>
        <span class="status-text">${statusText[this.lspStatus]}</span>
        <div class="tooltip">${this.getTooltipContent()}</div>
      </div>
    `;
  }

  /** 生成 Tooltip 内容 */
  private getTooltipContent(): string {
    const statusLabels: Record<LspConnectionStatus, string> = {
      connected: '已连接',
      connecting: '连接中',
      disconnected: '未连接',
      error: '错误',
    };

    let content = `状态: ${statusLabels[this.lspStatus]}`;

    if (this.lspHealth && this.lspStatus === 'connected') {
      const { process, metadata } = this.lspHealth;
      if (process) {
        content += `\n运行时间: ${Math.floor(process.uptime || 0)}s`;
        content += `\n内存: ${(process.memory?.rss || 0).toFixed(1)}MB`;
        if (process.cpu?.percent !== undefined) {
          content += `\nCPU: ${process.cpu.percent.toFixed(1)}%`;
        }
      }
      if (metadata) {
        content += `\n重启次数: ${metadata.restartCount || 0}`;
      }
    }

    return content;
  }

  firstUpdated(): void {
    this.initializeEditor();
  }

  connectedCallback(): void {
    super.connectedCallback();
    // 重挂载时恢复编辑器（如果之前已被销毁）
    if (this.ready && !this.editor) {
      this.initializeEditor();
    }
  }

  disconnectedCallback(): void {
    super.disconnectedCallback();
    this.stopHealthPolling();
    this.disposeEditor();
  }

  updated(changed: Map<string, unknown>): void {
    if (changed.has('value') && this.editor && !this.suppressModelEvent) {
      const currentValue = this.editor.getValue();
      if (typeof this.value === 'string' && this.value !== currentValue) {
        this.applyEditorValue(this.value);
      }
    }

    if (changed.has('theme') && this.ready) {
      monaco.editor.setTheme(this.theme);
    }

    if (changed.has('fontSize') && this.editor) {
      this.editor.updateOptions({ fontSize: this.fontSize });
    }

    if (changed.has('minimap') && this.editor) {
      this.editor.updateOptions({ minimap: { enabled: this.minimap } });
    }

    if (changed.has('folding') && this.editor) {
      this.editor.updateOptions({ folding: this.folding });
    }
  }

  public setValue(value: string): void {
    this.value = value ?? '';
    if (this.editor) {
      this.applyEditorValue(this.value);
    }
  }

  public focusEditor(): void {
    this.editor?.focus();
  }

  private initializeEditor(): void {
    if (this.editor) {
      return;
    }

    ensureWorkers();
    ensureAsterLanguage();

    const container = this.renderRoot.querySelector<HTMLDivElement>('#editor');
    if (!container) {
      return;
    }

    const uri = monaco.Uri.parse(this.modelUri);
    this.model = monaco.editor.createModel(this.value ?? '', this.language, uri);

    this.editor = monaco.editor.create(container, {
      model: this.model,
      language: this.language,
      theme: this.theme,
      fontSize: this.fontSize,
      minimap: { enabled: this.minimap },
      folding: this.folding,
      automaticLayout: true,
      smoothScrolling: true,
      scrollBeyondLastLine: false,
      contextmenu: true,
      padding: { top: 12, bottom: 12 },
    });

    this.editor.onDidChangeModelContent(() => this.handleModelChange());

    this.lspClient = new AsterLspClient(uri.toString());

    // 监听 LSP 状态变化
    this.statusUnsubscribe = this.lspClient.onStatusChange((status) => {
      this.lspStatus = status;
      // 状态变化时立即检查健康
      if (status === 'connected') {
        void this.pollHealth();
      } else {
        this.lspHealth = null;
      }
    });

    this.lspClient.connect();

    // 启动健康检查轮询
    this.startHealthPolling();

    this.ready = true;
  }

  /** 启动健康检查轮询 */
  private startHealthPolling(): void {
    this.healthPollInterval = window.setInterval(() => {
      void this.pollHealth();
    }, MonacoEditorComponent.HEALTH_POLL_INTERVAL);
  }

  /** 执行健康检查 */
  private async pollHealth(): Promise<void> {
    if (!this.lspClient || this.lspStatus !== 'connected') {
      return;
    }
    try {
      const health = await this.lspClient.checkHealth();
      if (health) {
        this.lspHealth = health;
      }
    } catch (error) {
      console.warn('健康检查失败:', error);
      this.lspHealth = null;
    }
  }

  /** 停止健康检查轮询 */
  private stopHealthPolling(): void {
    if (this.healthPollInterval !== undefined) {
      clearInterval(this.healthPollInterval);
      this.healthPollInterval = undefined;
    }
  }

  private handleModelChange(): void {
    if (!this.editor) {
      return;
    }
    const newValue = this.editor.getValue();
    this.suppressModelEvent = true;
    this.value = newValue;
    this.dispatchEvent(
      new CustomEvent('value-changed', {
        detail: { value: newValue },
      })
    );
    this.dispatchEvent(
      new CustomEvent('monaco-value-changed', {
        detail: { value: newValue },
        bubbles: true,
        composed: true,
      })
    );
    this.suppressModelEvent = false;
  }

  private applyEditorValue(value: string): void {
    if (!this.editor) {
      return;
    }
    this.suppressModelEvent = true;
    this.editor.setValue(value ?? '');
    this.suppressModelEvent = false;
  }

  private disposeEditor(): void {
    this.statusUnsubscribe?.();
    this.statusUnsubscribe = undefined;
    this.editor?.dispose();
    this.editor = null;
    this.model?.dispose();
    this.model = null;
    this.lspClient?.dispose();
    this.lspClient = null;
    this.lspHealth = null;
    this.lspStatus = 'disconnected';
  }
}
