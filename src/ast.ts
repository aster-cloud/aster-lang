// Simple AST node constructors
import type * as AST from './types.js';

export const Node = {
  Module: (name: string | null, decls: readonly AST.Declaration[]): AST.Module => ({ kind: 'Module', name, decls }),
  Import: (name: string, asName: string | null): AST.Import => ({ kind: 'Import', name, asName }),
  Data: (name: string, fields: readonly AST.Field[]): AST.Data => ({ kind: 'Data', name, fields }),
  Enum: (name: string, variants: readonly string[]): AST.Enum => ({ kind: 'Enum', name, variants }),
  Func: (name: string, params: readonly AST.Parameter[], retType: AST.Type, effects: readonly string[], body: AST.Block | null): AST.Func => 
    ({ kind: 'Func', name, params, retType, effects, body }),
  Block: (statements: readonly AST.Statement[]): AST.Block => ({ kind: 'Block', statements }),
  Let: (name: string, expr: AST.Expression): AST.Let => ({ kind: 'Let', name, expr }),
  Set: (name: string, expr: AST.Expression): AST.Set => ({ kind: 'Set', name, expr }),
  Return: (expr: AST.Expression): AST.Return => ({ kind: 'Return', expr }),
  If: (cond: AST.Expression, thenBlock: AST.Block, elseBlock: AST.Block | null): AST.If => 
    ({ kind: 'If', cond, thenBlock, elseBlock }),
  Match: (expr: AST.Expression, cases: readonly AST.Case[]): AST.Match => ({ kind: 'Match', expr, cases }),
  Case: (pattern: AST.Pattern, body: AST.Return | AST.Block): AST.Case => ({ kind: 'Case', pattern, body }),
  Start: (name: string, expr: AST.Expression): AST.AstNode => ({ kind: 'Start', name, expr } as any),
  Wait: (names: readonly string[]): AST.AstNode => ({ kind: 'Wait', names } as any),

  // Expressions
  Name: (name: string): AST.Name => ({ kind: 'Name', name }),
  Bool: (value: boolean): AST.Bool => ({ kind: 'Bool', value }),
  Null: (): AST.Null => ({ kind: 'Null' }),
  Int: (value: number): AST.Int => ({ kind: 'Int', value }),
  String: (value: string): AST.String => ({ kind: 'String', value }),
  Call: (target: AST.Expression, args: readonly AST.Expression[]): AST.Call => ({ kind: 'Call', target, args }),
  Construct: (typeName: string, fields: readonly AST.ConstructField[]): AST.Construct => ({ kind: 'Construct', typeName, fields }),
  Ok: (expr: AST.Expression): AST.Ok => ({ kind: 'Ok', expr }),
  Err: (expr: AST.Expression): AST.Err => ({ kind: 'Err', expr }),
  Some: (expr: AST.Expression): AST.Some => ({ kind: 'Some', expr }),
  None: (): AST.None => ({ kind: 'None' }),

  // Types
  TypeName: (name: string): AST.TypeName => ({ kind: 'TypeName', name }),
  Maybe: (type: AST.Type): AST.Maybe => ({ kind: 'Maybe', type }),
  Option: (type: AST.Type): AST.Option => ({ kind: 'Option', type }),
  Result: (ok: AST.Type, err: AST.Type): AST.Result => ({ kind: 'Result', ok, err }),
  List: (type: AST.Type): AST.List => ({ kind: 'List', type }),
  Map: (key: AST.Type, val: AST.Type): AST.Map => ({ kind: 'Map', key, val }),

  PatternNull: (): AST.PatternNull => ({ kind: 'PatternNull' }),
  PatternCtor: (typeName: string, names: readonly string[]): AST.PatternCtor => ({ kind: 'PatternCtor', typeName, names }),
  PatternName: (name: string): AST.PatternName => ({ kind: 'PatternName', name }),
};
