// Mock for Lit library
export class LitElement extends HTMLElement {
  render() {
    return null;
  }

  firstUpdated() {}
  updated() {}
  disconnectedCallback() {}
}

export const html = (strings: TemplateStringsArray, ...values: any[]) => {
  return strings.join('');
};

export const css = (strings: TemplateStringsArray, ...values: any[]) => {
  return strings.join('');
};

export default { LitElement, html, css };
