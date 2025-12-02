// Monaco Editor API Mock
export const monaco = {
  editor: {
    create: jest.fn().mockReturnValue({
      getValue: jest.fn(() => ''),
      setValue: jest.fn(),
      onDidChangeModelContent: jest.fn(() => ({ dispose: jest.fn() })),
      dispose: jest.fn(),
      updateOptions: jest.fn(),
      focus: jest.fn(),
    }),
    createModel: jest.fn().mockReturnValue({
      dispose: jest.fn(),
      getValue: jest.fn(() => ''),
      setValue: jest.fn(),
    }),
    setTheme: jest.fn(),
    defineTheme: jest.fn(),
    IStandaloneCodeEditor: {},
    ITextModel: {},
  },
  languages: {
    register: jest.fn(),
    setMonarchTokensProvider: jest.fn(),
    setLanguageConfiguration: jest.fn(),
  },
  Uri: {
    parse: jest.fn((uri: string) => ({
      toString: () => uri,
      scheme: 'inmemory',
      path: uri,
    })),
  },
};

export default monaco;
