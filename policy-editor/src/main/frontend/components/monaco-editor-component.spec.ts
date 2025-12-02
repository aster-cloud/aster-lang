import { MonacoEditorComponent } from './monaco-editor-component';
import * as monaco from 'monaco-editor/esm/vs/editor/editor.api';

// Mock the LSP client
jest.mock('../lsp/lsp-client', () => ({
  AsterLspClient: jest.fn().mockImplementation(() => ({
    connect: jest.fn(),
    dispose: jest.fn(),
  })),
}));

describe('MonacoEditorComponent', () => {
  let component: MonacoEditorComponent;

  beforeEach(() => {
    // Create component instance
    component = new MonacoEditorComponent();

    // Mock the renderRoot to simulate actual DOM
    Object.defineProperty(component, 'renderRoot', {
      value: {
        querySelector: jest.fn((selector: string) => {
          if (selector === '#editor') {
            return document.createElement('div');
          }
          return null;
        }),
      },
      writable: true,
    });
  });

  afterEach(() => {
    if (component && component['editor']) {
      component.disconnectedCallback();
    }
    jest.clearAllMocks();
  });

  describe('属性绑定 (Property Binding)', () => {
    test('应该有默认的 value 属性', () => {
      expect(component.value).toBe('');
    });

    test('应该能够设置和获取 value 属性', () => {
      const testValue = 'This module is test.';
      component.value = testValue;
      expect(component.value).toBe(testValue);
    });

    test('应该有默认的语言为 aster', () => {
      expect(component.language).toBe('aster');
    });

    test('应该有默认的主题为 vs-dark', () => {
      expect(component.theme).toBe('vs-dark');
    });

    test('应该有默认的字体大小为 14', () => {
      expect(component.fontSize).toBe(14);
    });

    test('应该能够更新属性值', () => {
      component.theme = 'vs';
      component.fontSize = 16;
      component.minimap = false;
      component.folding = false;

      expect(component.theme).toBe('vs');
      expect(component.fontSize).toBe(16);
      expect(component.minimap).toBe(false);
      expect(component.folding).toBe(false);
    });
  });

  describe('编辑器初始化 (Editor Initialization)', () => {
    test('应该在 firstUpdated 时初始化编辑器', () => {
      const createSpy = monaco.editor.create as jest.Mock;
      const createModelSpy = monaco.editor.createModel as jest.Mock;

      component.firstUpdated();

      expect(createModelSpy).toHaveBeenCalledWith(
        component.value,
        'aster',
        expect.objectContaining({
          toString: expect.any(Function),
        })
      );
      expect(createSpy).toHaveBeenCalled();
    });

    test('应该使用配置的属性创建编辑器', () => {
      component.theme = 'vs';
      component.fontSize = 18;
      component.minimap = false;

      component.firstUpdated();

      const createCall = (monaco.editor.create as jest.Mock).mock.calls[0];
      const options = createCall[1];

      expect(options.theme).toBe('vs');
      expect(options.fontSize).toBe(18);
      expect(options.minimap.enabled).toBe(false);
    });

    test('应该注册 onDidChangeModelContent 监听器', () => {
      const mockEditor = (monaco.editor.create as jest.Mock).mock.results[0].value;

      component.firstUpdated();

      expect(mockEditor.onDidChangeModelContent).toHaveBeenCalled();
    });
  });

  describe('事件派发 (Event Dispatching)', () => {
    test('应该在值变化时派发 monaco-value-changed 事件', () => {
      const newValue = 'New policy code';
      const eventListener = jest.fn();

      component.addEventListener('monaco-value-changed', eventListener);
      component.firstUpdated();

      // Simulate editor content change
      const mockEditor = (monaco.editor.create as jest.Mock).mock.results[0].value;
      mockEditor.getValue.mockReturnValue(newValue);

      // Trigger the content change callback
      const onChangeCallback = mockEditor.onDidChangeModelContent.mock.calls[0][0];
      onChangeCallback();

      expect(eventListener).toHaveBeenCalled();
      const event = eventListener.mock.calls[0][0] as CustomEvent;
      expect(event.detail.value).toBe(newValue);
      expect(event.bubbles).toBe(true);
      expect(event.composed).toBe(true);
    });

    test('应该派发 value-changed 事件以支持 Vaadin 双向绑定', () => {
      const newValue = 'Updated code';
      const eventListener = jest.fn();

      component.addEventListener('value-changed', eventListener);
      component.firstUpdated();

      const mockEditor = (monaco.editor.create as jest.Mock).mock.results[0].value;
      mockEditor.getValue.mockReturnValue(newValue);

      const onChangeCallback = mockEditor.onDidChangeModelContent.mock.calls[0][0];
      onChangeCallback();

      expect(eventListener).toHaveBeenCalled();
      const event = eventListener.mock.calls[0][0] as CustomEvent;
      expect(event.detail.value).toBe(newValue);
    });
  });

  describe('setValue 方法 (setValue Method)', () => {
    test('应该更新组件的 value 属性', () => {
      const newValue = 'Test policy';
      component.setValue(newValue);
      expect(component.value).toBe(newValue);
    });

    test('应该更新编辑器的值', () => {
      component.firstUpdated();
      const mockEditor = (monaco.editor.create as jest.Mock).mock.results[0].value;

      const newValue = 'New test value';
      component.setValue(newValue);

      expect(mockEditor.setValue).toHaveBeenCalledWith(newValue);
    });

    test('应该处理 null 值', () => {
      component.firstUpdated();
      const mockEditor = (monaco.editor.create as jest.Mock).mock.results[0].value;

      component.setValue(null as any);

      expect(mockEditor.setValue).toHaveBeenCalledWith('');
    });
  });

  describe('LSP 客户端 (LSP Client)', () => {
    test('应该初始化 LSP 客户端', () => {
      const { AsterLspClient } = require('../lsp/lsp-client');

      component.firstUpdated();

      expect(AsterLspClient).toHaveBeenCalled();
    });

    test('应该使用正确的 URI 初始化 LSP 客户端', () => {
      const { AsterLspClient } = require('../lsp/lsp-client');

      component.modelUri = 'inmemory://test/file.aster';
      component.firstUpdated();

      expect(AsterLspClient).toHaveBeenCalledWith('inmemory://test/file.aster');
    });

    test('应该在初始化后连接 LSP 客户端', () => {
      const { AsterLspClient } = require('../lsp/lsp-client');
      const mockLspClient = AsterLspClient.mock.results[0].value;

      component.firstUpdated();

      expect(mockLspClient.connect).toHaveBeenCalled();
    });
  });

  describe('生命周期管理 (Lifecycle Management)', () => {
    test('应该在 disconnectedCallback 时释放编辑器', () => {
      component.firstUpdated();
      const mockEditor = (monaco.editor.create as jest.Mock).mock.results[0].value;
      const mockModel = (monaco.editor.createModel as jest.Mock).mock.results[0].value;

      component.disconnectedCallback();

      expect(mockEditor.dispose).toHaveBeenCalled();
      expect(mockModel.dispose).toHaveBeenCalled();
    });

    test('应该在 disconnectedCallback 时释放 LSP 客户端', () => {
      const { AsterLspClient } = require('../lsp/lsp-client');
      component.firstUpdated();
      const mockLspClient = AsterLspClient.mock.results[0].value;

      component.disconnectedCallback();

      expect(mockLspClient.dispose).toHaveBeenCalled();
    });

    test('应该防止多次初始化编辑器', () => {
      const createSpy = monaco.editor.create as jest.Mock;

      component.firstUpdated();
      const callCount = createSpy.mock.calls.length;

      component.firstUpdated();

      expect(createSpy.mock.calls.length).toBe(callCount);
    });
  });

  describe('属性更新响应 (Property Update Reactions)', () => {
    test('应该在 theme 变化时更新主题', () => {
      component.firstUpdated();

      const changedMap = new Map([['theme', 'vs-dark']]);
      component.theme = 'vs';
      component.updated(changedMap);

      expect(monaco.editor.setTheme).toHaveBeenCalledWith('vs');
    });

    test('应该在 fontSize 变化时更新字体大小', () => {
      component.firstUpdated();
      const mockEditor = (monaco.editor.create as jest.Mock).mock.results[0].value;

      const changedMap = new Map([['fontSize', 14]]);
      component.fontSize = 16;
      component.updated(changedMap);

      expect(mockEditor.updateOptions).toHaveBeenCalledWith({ fontSize: 16 });
    });

    test('应该在 value 变化时同步编辑器值', () => {
      component.firstUpdated();
      const mockEditor = (monaco.editor.create as jest.Mock).mock.results[0].value;
      mockEditor.getValue.mockReturnValue('old value');

      const newValue = 'new value';
      const changedMap = new Map([['value', 'old value']]);
      component.value = newValue;
      component.updated(changedMap);

      expect(mockEditor.setValue).toHaveBeenCalledWith(newValue);
    });
  });

  describe('焦点管理 (Focus Management)', () => {
    test('应该提供 focusEditor 方法', () => {
      component.firstUpdated();
      const mockEditor = (monaco.editor.create as jest.Mock).mock.results[0].value;

      component.focusEditor();

      expect(mockEditor.focus).toHaveBeenCalled();
    });
  });
});
