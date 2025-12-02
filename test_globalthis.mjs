import { typecheckModule, shouldEnforcePii } from './dist/src/typecheck.js';
import { Core as CoreBuilder } from './dist/src/core_ir.js';

// 设置 globalThis.lspConfig 启用 PII 检查
globalThis.lspConfig = { enforcePiiChecks: true };

// Error codes (compiled from ErrorCode enum)
const ErrorCode = {
  PII_SINK_UNSANITIZED: 'E072',
};

const IO_EFFECT = [{ kind: 'IO' }];
const PURE_EFFECT = [{ kind: 'PURE' }];

const TEXT = () => CoreBuilder.TypeName('Text');
const piiType = (level, category) => CoreBuilder.Pii(TEXT(), level, category);

const piiParam = (name, level) => ({
  name,
  type: piiType(level, 'email'),
  annotations: [],
});

function makeFunc(options) {
  return CoreBuilder.Func(
    options.name,
    [],
    options.params,
    options.ret,
    options.effects ?? PURE_EFFECT,
    CoreBuilder.Block(options.body),
    [],
    false
  );
}

const httpImport = CoreBuilder.Import('Http', 'Http');
const fn = makeFunc({
  name: 'type_layer_source_test',
  params: [piiParam('email', 'L2')],
  ret: TEXT(),
  effects: IO_EFFECT,
  body: [
    CoreBuilder.Return(
      CoreBuilder.Call(CoreBuilder.Name('Http.post'), [
        CoreBuilder.String('https://api.example.com'),
        CoreBuilder.Name('email'),
      ])
    ),
  ],
});

const module = CoreBuilder.Module('tests.pii.type_layer_source', [httpImport, fn]);

console.log('globalThis.lspConfig:', globalThis.lspConfig);
console.log('shouldEnforcePii() returns:', shouldEnforcePii());
console.log('Module structure:', JSON.stringify(module, null, 2));

const diagnostics = typecheckModule(module);
console.log('Total diagnostics:', diagnostics.length);
console.log('Diagnostics:', JSON.stringify(diagnostics, null, 2));

const sinkDiag = diagnostics.find(diag => diag.code === ErrorCode.PII_SINK_UNSANITIZED);
console.log('Sink diagnostic found:', Boolean(sinkDiag));
if (sinkDiag) {
  console.log('Sink diagnostic:', JSON.stringify(sinkDiag, null, 2));
}
