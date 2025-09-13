// Core type definitions for Aster CNL

export interface Position {
  readonly line: number;
  readonly col: number;
}

export interface Span {
  readonly start: Position;
  readonly end: Position;
}

export interface Token {
  readonly kind: TokenKind;
  readonly value: string | number | boolean | null;
  readonly start: Position;
  readonly end: Position;
}

export enum TokenKind {
  EOF = 'EOF',
  NEWLINE = 'NEWLINE',
  INDENT = 'INDENT',
  DEDENT = 'DEDENT',
  DOT = 'DOT',
  COLON = 'COLON',
  COMMA = 'COMMA',
  LPAREN = 'LPAREN',
  RPAREN = 'RPAREN',
  EQUALS = 'EQUALS',
  PLUS = 'PLUS',
  MINUS = 'MINUS',
  LT = 'LT',
  GT = 'GT',
  IDENT = 'IDENT',
  TYPE_IDENT = 'TYPE_IDENT',
  STRING = 'STRING',
  INT = 'INT',
  BOOL = 'BOOL',
  NULL = 'NULL',
  KEYWORD = 'KEYWORD',
}

export enum Effect {
  IO = 'IO',
  CPU = 'CPU',
}

// CNL AST types
export interface AstNode {
  readonly kind: string;
}

export interface Module extends AstNode {
  readonly kind: 'Module';
  readonly name: string | null;
  readonly decls: readonly Declaration[];
}

export interface Import extends AstNode {
  readonly kind: 'Import';
  readonly name: string;
  readonly asName: string | null;
}

export interface Data extends AstNode {
  readonly kind: 'Data';
  readonly name: string;
  readonly fields: readonly Field[];
}

export interface Field {
  readonly name: string;
  readonly type: Type;
}

export interface Enum extends AstNode {
  readonly kind: 'Enum';
  readonly name: string;
  readonly variants: readonly string[];
}

export interface Func extends AstNode {
  readonly kind: 'Func';
  readonly name: string;
  readonly typeParams: readonly string[];
  readonly params: readonly Parameter[];
  readonly retType: Type;
  readonly effects: readonly string[];
  readonly body: Block | null;
}

export interface Parameter {
  readonly name: string;
  readonly type: Type;
}

export interface Block extends AstNode {
  readonly kind: 'Block';
  readonly statements: readonly Statement[];
}

export type Declaration = Import | Data | Enum | Func;

export type Statement = Let | Set | Return | If | Match | Start | Wait | Expression | Block;

export interface Let extends AstNode {
  readonly kind: 'Let';
  readonly name: string;
  readonly expr: Expression;
}

export interface Set extends AstNode {
  readonly kind: 'Set';
  readonly name: string;
  readonly expr: Expression;
}

export interface Return extends AstNode {
  readonly kind: 'Return';
  readonly expr: Expression;
}

export interface If extends AstNode {
  readonly kind: 'If';
  readonly cond: Expression;
  readonly thenBlock: Block;
  readonly elseBlock: Block | null;
}

export interface Match extends AstNode {
  readonly kind: 'Match';
  readonly expr: Expression;
  readonly cases: readonly Case[];
}

export interface Case extends AstNode {
  readonly kind: 'Case';
  readonly pattern: Pattern;
  readonly body: Return | Block;
}
export interface Start extends AstNode {
  readonly kind: 'Start';
  readonly name: string;
  readonly expr: Expression; // async expr
}

export interface Wait extends AstNode {
  readonly kind: 'Wait';
  readonly names: readonly string[];
}

export type Pattern = PatternNull | PatternCtor | PatternName;

export interface PatternNull extends AstNode {
  readonly kind: 'PatternNull';
}

export interface PatternCtor extends AstNode {
  readonly kind: 'PatternCtor';
  readonly typeName: string;
  readonly names: readonly string[];
  readonly args?: readonly Pattern[];
}

export interface PatternName extends AstNode {
  readonly kind: 'PatternName';
  readonly name: string;
}

export type Expression =
  | Name
  | Bool
  | Int
  | String
  | Null
  | Call
  | Construct
  | Ok
  | Err
  | Some
  | None
  | Lambda
  | Await;

export interface Await extends AstNode {
  readonly kind: 'Await';
  readonly expr: Expression;
}

export interface Name extends AstNode {
  readonly kind: 'Name';
  readonly name: string;
}

export interface Bool extends AstNode {
  readonly kind: 'Bool';
  readonly value: boolean;
}

export interface Int extends AstNode {
  readonly kind: 'Int';
  readonly value: number;
}

export interface String extends AstNode {
  readonly kind: 'String';
  readonly value: string;
}

export interface Null extends AstNode {
  readonly kind: 'Null';
}

export interface Call extends AstNode {
  readonly kind: 'Call';
  readonly target: Expression;
  readonly args: readonly Expression[];
}

export interface Lambda extends AstNode {
  readonly kind: 'Lambda';
  readonly params: readonly Parameter[];
  readonly retType: Type;
  readonly body: Block;
}

export interface Construct extends AstNode {
  readonly kind: 'Construct';
  readonly typeName: string;
  readonly fields: readonly ConstructField[];
}

export interface ConstructField {
  readonly name: string;
  readonly expr: Expression;
}

export interface Ok extends AstNode {
  readonly kind: 'Ok';
  readonly expr: Expression;
}

export interface Err extends AstNode {
  readonly kind: 'Err';
  readonly expr: Expression;
}

export interface Some extends AstNode {
  readonly kind: 'Some';
  readonly expr: Expression;
}

export interface None extends AstNode {
  readonly kind: 'None';
}

export type Type = TypeName | Maybe | Option | Result | List | Map | TypeApp | TypeVar | FuncType;

export interface TypeName extends AstNode {
  readonly kind: 'TypeName';
  readonly name: string;
}

export interface TypeVar extends AstNode {
  readonly kind: 'TypeVar';
  readonly name: string;
}

export interface TypeApp extends AstNode {
  readonly kind: 'TypeApp';
  readonly base: string; // base type name
  readonly args: readonly Type[];
}

export interface Maybe extends AstNode {
  readonly kind: 'Maybe';
  readonly type: Type;
}

export interface Option extends AstNode {
  readonly kind: 'Option';
  readonly type: Type;
}

export interface Result extends AstNode {
  readonly kind: 'Result';
  readonly ok: Type;
  readonly err: Type;
}

export interface List extends AstNode {
  readonly kind: 'List';
  readonly type: Type;
}

export interface Map extends AstNode {
  readonly kind: 'Map';
  readonly key: Type;
  readonly val: Type;
}

export interface FuncType extends AstNode {
  readonly kind: 'FuncType';
  readonly params: readonly Type[];
  readonly ret: Type;
}

// Core IR types (distinct from CNL AST)
export namespace Core {
  export interface CoreNode {
    readonly kind: string;
  }

  export interface Module extends CoreNode {
    readonly kind: 'Module';
    readonly name: string | null;
    readonly decls: readonly Declaration[];
  }

  export interface Import extends CoreNode {
    readonly kind: 'Import';
    readonly name: string;
    readonly asName: string | null;
  }

  export interface Data extends CoreNode {
    readonly kind: 'Data';
    readonly name: string;
    readonly fields: readonly Field[];
  }

  export interface Field {
    readonly name: string;
    readonly type: Type;
  }

  export interface Enum extends CoreNode {
    readonly kind: 'Enum';
    readonly name: string;
    readonly variants: readonly string[];
  }

  export interface Func extends CoreNode {
    readonly kind: 'Func';
    readonly name: string;
    readonly typeParams: readonly string[];
    readonly params: readonly Parameter[];
    readonly ret: Type;
    readonly effects: readonly Effect[];
    readonly body: Block;
  }

  export interface Parameter {
    readonly name: string;
    readonly type: Type;
  }

  export interface Block extends CoreNode {
    readonly kind: 'Block';
    readonly statements: readonly Statement[];
  }

  export type Declaration = Import | Data | Enum | Func;

  export interface Start extends CoreNode {
    readonly kind: 'Start';
    readonly name: string;
    readonly expr: Expression;
  }
  export interface Wait extends CoreNode {
    readonly kind: 'Wait';
    readonly names: readonly string[];
  }
  export interface Scope extends CoreNode {
    readonly kind: 'Scope';
    readonly statements: readonly Statement[];
  }

  export type Statement = Let | Set | Return | If | Match | Scope | Start | Wait;

  export interface Let extends CoreNode {
    readonly kind: 'Let';
    readonly name: string;
    readonly expr: Expression;
  }

  export interface Set extends CoreNode {
    readonly kind: 'Set';
    readonly name: string;
    readonly expr: Expression;
  }

  export interface Return extends CoreNode {
    readonly kind: 'Return';
    readonly expr: Expression;
  }

  export interface If extends CoreNode {
    readonly kind: 'If';
    readonly cond: Expression;
    readonly thenBlock: Block;
    readonly elseBlock: Block | null;
  }

  export interface Match extends CoreNode {
    readonly kind: 'Match';
    readonly expr: Expression;
    readonly cases: readonly Case[];
  }

  export interface Case extends CoreNode {
    readonly kind: 'Case';
    readonly pattern: Pattern;
    readonly body: Return | Block;
  }

  export type Pattern = PatNull | PatCtor | PatName;

  export interface PatNull extends CoreNode {
    readonly kind: 'PatNull';
  }

  export interface PatCtor extends CoreNode {
    readonly kind: 'PatCtor';
    readonly typeName: string;
    readonly names: readonly string[];
    readonly args?: readonly Pattern[];
  }

  export interface PatName extends CoreNode {
    readonly kind: 'PatName';
    readonly name: string;
  }

  export type Expression =
    | Name
    | Bool
    | Int
    | String
    | Null
    | Call
    | Construct
    | Ok
    | Err
    | Some
    | None
    | Lambda;

  export interface Name extends CoreNode {
    readonly kind: 'Name';
    readonly name: string;
  }

  export interface Bool extends CoreNode {
    readonly kind: 'Bool';
    readonly value: boolean;
  }

  export interface Int extends CoreNode {
    readonly kind: 'Int';
    readonly value: number;
  }

  export interface String extends CoreNode {
    readonly kind: 'String';
    readonly value: string;
  }

  export interface Null extends CoreNode {
    readonly kind: 'Null';
  }

  export interface Call extends CoreNode {
    readonly kind: 'Call';
    readonly target: Expression;
    readonly args: readonly Expression[];
  }

  export interface Lambda extends CoreNode {
    readonly kind: 'Lambda';
    readonly params: readonly Parameter[];
    readonly ret: Type;
    readonly body: Block;
    readonly captures?: readonly string[];
  }

  export interface Construct extends CoreNode {
    readonly kind: 'Construct';
    readonly typeName: string;
    readonly fields: readonly ConstructField[];
  }

  export interface ConstructField {
    readonly name: string;
    readonly expr: Expression;
  }

  export interface Ok extends CoreNode {
    readonly kind: 'Ok';
    readonly expr: Expression;
  }

  export interface Err extends CoreNode {
    readonly kind: 'Err';
    readonly expr: Expression;
  }

  export interface Some extends CoreNode {
    readonly kind: 'Some';
    readonly expr: Expression;
  }

  export interface None extends CoreNode {
    readonly kind: 'None';
  }

  // Extended with generics (preview)
  export type Type = TypeName | Maybe | Option | Result | List | Map | TypeApp | TypeVar | FuncType;

  export interface TypeName extends CoreNode {
    readonly kind: 'TypeName';
    readonly name: string;
  }

  export interface TypeVar extends CoreNode {
    readonly kind: 'TypeVar';
    readonly name: string;
  }

  export interface TypeApp extends CoreNode {
    readonly kind: 'TypeApp';
    readonly base: string;
    readonly args: readonly Type[];
  }

  export interface Maybe extends CoreNode {
    readonly kind: 'Maybe';
    readonly type: Type;
  }

  export interface Option extends CoreNode {
    readonly kind: 'Option';
    readonly type: Type;
  }

  export interface Result extends CoreNode {
    readonly kind: 'Result';
    readonly ok: Type;
    readonly err: Type;
  }

  export interface List extends CoreNode {
    readonly kind: 'List';
    readonly type: Type;
  }

  export interface Map extends CoreNode {
    readonly kind: 'Map';
    readonly key: Type;
    readonly val: Type;
  }

  export interface FuncType extends CoreNode {
    readonly kind: 'FuncType';
    readonly params: readonly Type[];
    readonly ret: Type;
  }
}
