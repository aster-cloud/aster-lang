/** @type {import('jest').Config} */
export default {
  preset: 'ts-jest',
  testEnvironment: 'jsdom',
  roots: ['<rootDir>/src'],
  testMatch: ['**/__tests__/**/*.ts', '**/?(*.)+(spec|test).ts'],
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json'],
  transform: {
    '^.+\\.ts$': ['ts-jest', {
      tsconfig: {
        esModuleInterop: true,
        allowSyntheticDefaultImports: true,
        resolveJsonModule: true,
        moduleResolution: 'node',
        target: 'ES2020',
        module: 'ESNext',
        lib: ['ES2020', 'DOM'],
        skipLibCheck: true,
        strict: true,
      },
    }],
  },
  transformIgnorePatterns: [
    'node_modules/(?!(lit|@lit|lit-element|lit-html)/)',
  ],
  moduleNameMapper: {
    '\\.(css|less|scss|sass)$': '<rootDir>/src/test/__mocks__/styleMock.js',
    '\\?worker$': '<rootDir>/src/test/__mocks__/workerMock.js',
    '^monaco-editor/esm/vs/editor/editor.api$': '<rootDir>/src/test/__mocks__/monacoMock.ts',
    '^monaco-editor/esm/(.*)$': '<rootDir>/src/test/__mocks__/monacoMock.ts',
    '^lit$': '<rootDir>/src/test/__mocks__/litMock.ts',
    '^lit/decorators.js$': '<rootDir>/src/test/__mocks__/litDecoratorsMock.ts',
  },
  setupFilesAfterEnv: ['<rootDir>/src/test/setup.ts'],
  collectCoverageFrom: [
    'src/main/frontend/**/*.{ts,tsx}',
    '!src/main/frontend/**/*.d.ts',
    '!src/main/frontend/**/__tests__/**',
    '!src/main/frontend/**/index.ts',
  ],
  coverageThreshold: {
    global: {
      branches: 70,
      functions: 70,
      lines: 70,
      statements: 70,
    },
  },
  testPathIgnorePatterns: ['/node_modules/', '/target/', '/build/'],
  coveragePathIgnorePatterns: ['/node_modules/', '/target/', '/build/'],
};
