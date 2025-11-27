// Jest setup file

// Mock global objects if needed
global.ResizeObserver = jest.fn().mockImplementation(() => ({
  observe: jest.fn(),
  unobserve: jest.fn(),
  disconnect: jest.fn(),
}));

// Create a proper custom elements registry for JSDOM
if (!global.customElements) {
  const elementsRegistry = new Map();

  global.customElements = {
    define: jest.fn((name: string, constructor: any) => {
      elementsRegistry.set(name, constructor);
    }),
    get: jest.fn((name: string) => {
      return elementsRegistry.get(name);
    }),
    whenDefined: jest.fn(() => Promise.resolve()),
  } as any;
}

// Mock CSS.registerProperty for @property usage in Vaadin components
if (!global.CSS) {
  global.CSS = {
    supports: jest.fn(() => false),
    escape: jest.fn((str: string) => str),
  } as any;
}

// Mock document.createElement for custom elements
const originalCreateElement = document.createElement.bind(document);
document.createElement = jest.fn((tagName: string, options?: any) => {
  const constructor = (global.customElements as any).get(tagName);
  if (constructor) {
    const element = Object.create(constructor.prototype);
    constructor.call(element);
    return element;
  }
  return originalCreateElement(tagName, options);
}) as any;
