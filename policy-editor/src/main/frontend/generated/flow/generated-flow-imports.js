import { injectGlobalCss } from 'Frontend/generated/jar-resources/theme-util.js';

import { css, unsafeCSS, registerStyles } from '@vaadin/vaadin-themable-mixin';
import $cssFromFile_0 from 'Frontend/styles/json.css?inline';
import '@vaadin/polymer-legacy-adapter/style-modules.js';
import '@vaadin/app-layout/theme/lumo/vaadin-app-layout.js';
import '@vaadin/tabs/theme/lumo/vaadin-tabs.js';
import '@vaadin/tabs/theme/lumo/vaadin-tab.js';
import '@vaadin/tooltip/theme/lumo/vaadin-tooltip.js';
import 'Frontend/components/monaco-editor-component.ts';
import '@vaadin/vertical-layout/theme/lumo/vaadin-vertical-layout.js';
import '@vaadin/text-field/theme/lumo/vaadin-text-field.js';
import '@vaadin/text-area/theme/lumo/vaadin-text-area.js';
import '@vaadin/combo-box/theme/lumo/vaadin-combo-box.js';
import 'Frontend/generated/jar-resources/flow-component-renderer.js';
import 'Frontend/generated/jar-resources/comboBoxConnector.js';
import '@vaadin/multi-select-combo-box/theme/lumo/vaadin-multi-select-combo-box.js';
import '@vaadin/button/theme/lumo/vaadin-button.js';
import 'Frontend/generated/jar-resources/disableOnClickFunctions.js';
import '@vaadin/horizontal-layout/theme/lumo/vaadin-horizontal-layout.js';
import '@vaadin/notification/theme/lumo/vaadin-notification.js';
import '@vaadin/split-layout/theme/lumo/vaadin-split-layout.js';
import '@vaadin/app-layout/theme/lumo/vaadin-drawer-toggle.js';
import '@vaadin/integer-field/theme/lumo/vaadin-integer-field.js';
import '@vaadin/form-layout/theme/lumo/vaadin-form-layout.js';
import '@vaadin/form-layout/theme/lumo/vaadin-form-item.js';
import '@vaadin/form-layout/theme/lumo/vaadin-form-row.js';
import '@vaadin/number-field/theme/lumo/vaadin-number-field.js';
import '@vaadin/checkbox/theme/lumo/vaadin-checkbox.js';
import '@vaadin/grid/theme/lumo/vaadin-grid.js';
import '@vaadin/grid/theme/lumo/vaadin-grid-column.js';
import '@vaadin/grid/theme/lumo/vaadin-grid-sorter.js';
import 'Frontend/generated/jar-resources/gridConnector.ts';
import 'Frontend/generated/jar-resources/vaadin-grid-flow-selection-column.js';
import '@vaadin/grid/theme/lumo/vaadin-grid-column-group.js';
import 'Frontend/generated/jar-resources/lit-renderer.ts';
import '@vaadin/context-menu/theme/lumo/vaadin-context-menu.js';
import 'Frontend/generated/jar-resources/contextMenuConnector.js';
import 'Frontend/generated/jar-resources/contextMenuTargetConnector.js';
import '@vaadin/upload/theme/lumo/vaadin-upload.js';
import '@vaadin/dialog/theme/lumo/vaadin-dialog.js';
import '@vaadin/common-frontend/ConnectionIndicator.js';
import '@vaadin/vaadin-lumo-styles/color-global.js';
import '@vaadin/vaadin-lumo-styles/typography-global.js';
import '@vaadin/vaadin-lumo-styles/sizing.js';
import '@vaadin/vaadin-lumo-styles/spacing.js';
import '@vaadin/vaadin-lumo-styles/style.js';
import '@vaadin/vaadin-lumo-styles/vaadin-iconset.js';
import 'Frontend/generated/jar-resources/ReactRouterOutletElement.tsx';

injectGlobalCss($cssFromFile_0.toString(), 'CSSImport end', document);

const loadOnDemand = (key) => {
  const pending = [];
  if (key === 'feb2495d0cf46ec47977acbfd40ce3899f360649678c84bc65c6bb3e3321b4f1') {
    pending.push(import('./chunks/chunk-1d357ee2696c888b1d727e6cc9a6ac0942efc74a10c4eb8caffa5ff14b72b763.js'));
  }
  return Promise.all(pending);
}

window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.loadOnDemand = loadOnDemand;
window.Vaadin.Flow.resetFocus = () => {
 let ae=document.activeElement;
 while(ae&&ae.shadowRoot) ae = ae.shadowRoot.activeElement;
 return !ae || ae.blur() || ae.focus() || true;
}