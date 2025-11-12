import { LitElement, css, html } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';
import * as monaco from 'monaco-editor/esm/vs/editor/editor.api';
import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker';
import jsonWorker from 'monaco-editor/esm/vs/language/json/json.worker?worker';
import tsWorker from 'monaco-editor/esm/vs/language/typescript/ts.worker?worker';
import 'monaco-editor/esm/vs/editor/editor.main.css';
import { AsterLspClient } from '../lsp/lsp-client.js';

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

  private editor: monaco.editor.IStandaloneCodeEditor | null = null;
  private model: monaco.editor.ITextModel | null = null;
  private suppressModelEvent = false;
  private lspClient: AsterLspClient | null = null;

  static styles = css`
    :host {
      display: block;
      width: 100%;
      height: 100%;
      min-height: 300px;
    }

    .editor-container {
      width: 100%;
      height: 100%;
    }
  `;

  render() {
    return html`<div id="editor" class="editor-container" part="editor"></div>`;
  }

  firstUpdated(): void {
    this.initializeEditor();
  }

  disconnectedCallback(): void {
    super.disconnectedCallback();
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
    this.lspClient.connect();

    this.ready = true;
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
    this.editor?.dispose();
    this.editor = null;
    this.model?.dispose();
    this.model = null;
    this.lspClient?.dispose();
    this.lspClient = null;
  }
}
