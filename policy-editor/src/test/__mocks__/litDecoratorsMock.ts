// Mock for Lit decorators
export const customElement = (tagName: string) => {
  return (target: any) => target;
};

export const property = (options?: any) => {
  return (target: any, propertyKey: string) => {};
};

export const state = (options?: any) => {
  return (target: any, propertyKey: string) => {};
};

export const query = (selector: string) => {
  return (target: any, propertyKey: string) => {};
};

export default { customElement, property, state, query };
