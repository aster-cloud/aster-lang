// Core type definitions for Aster CNL

import { Effect as EffectEnum } from './config/semantic.js';
import type * as Base from './types/base.js';

export interface Position {
  readonly line: number;
  readonly col: number;
}

export interface Span {
  readonly start: Position;
  readonly end: Position;
}

// Optional file-backed origin info; used for IR provenance and logs
export interface Origin {
  readonly file?: string;
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
  LBRACKET = 'LBRACKET',
  RBRACKET = 'RBRACKET',
  EQUALS = 'EQUALS',
  PLUS = 'PLUS',
  STAR = 'STAR',
  MINUS = 'MINUS',
  LT = 'LT',
  GT = 'GT',
  QUESTION = 'QUESTION',
  AT = 'AT',
  IDENT = 'IDENT',
  TYPE_IDENT = 'TYPE_IDENT',
  STRING = 'STRING',
  INT = 'INT',
  FLOAT = 'FLOAT',
  LONG = 'LONG',
  BOOL = 'BOOL',
  NULL = 'NULL',
  KEYWORD = 'KEYWORD',
}

// Effect 枚举现在从 config/semantic.ts 导出，保持类型定义集中
export { Effect } from './config/semantic.js';

export interface TypecheckDiagnostic {
  severity: 'error' | 'warning' | 'info';
  message: string;
  code?: string;
  data?: unknown;
  location?: Origin;
}

// CNL AST types
export type AstNode = Base.BaseNode<Span>;

export interface Module extends Base.BaseModule<Span, Declaration> {}

export interface Import extends Base.BaseImport<Span> {}

export interface Data extends Base.BaseData<Span, Type> {}

export interface Field extends Base.BaseField<Type> {}

export interface Enum extends Base.BaseEnum<Span> {}

export interface Func extends Base.BaseFunc<Span, readonly string[], Type> {
  readonly retType: Type;
  readonly body: Block | null;
}

export interface Parameter extends Base.BaseParameter<Type> {}

export interface Block extends Base.BaseBlock<Span, Statement> {}

export type Declaration = Import | Data | Enum | Func;

export type Statement = Let | Set | Return | If | Match | Start | Wait | Expression | Block;

export interface Let extends Base.BaseLet<Span, Expression> {}

export interface Set extends Base.BaseSet<Span, Expression> {}

export interface Return extends Base.BaseReturn<Span, Expression> {}

export interface If extends Base.BaseIf<Span, Expression, Block> {}

export interface Match extends Base.BaseMatch<Span, Expression, Case> {}

export interface Case extends Base.BaseCase<Span, Pattern, Return | Block> {}

export interface Start extends Base.BaseStart<Span, Expression> {}

export interface Wait extends Base.BaseWait<Span> {}

export type Pattern = PatternNull | PatternCtor | PatternName | PatternInt;

export interface PatternNull extends Base.BasePatternNull<Span> {}

export interface PatternCtor extends Base.BasePatternCtor<Span, Pattern> {}

export interface PatternName extends Base.BasePatternName<Span> {}

export interface PatternInt extends Base.BasePatternInt<Span> {}

export type Expression =
  | Name
  | Bool
  | Int
  | Long
  | Double
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

export interface Await extends Base.BaseAwait<Span, Expression> {}

export interface Name extends Base.BaseName<Span> {}

export interface Bool extends Base.BaseBool<Span> {}

export interface Int extends Base.BaseInt<Span> {}

export interface Long extends Base.BaseLong<Span> {}

export interface Double extends Base.BaseDouble<Span> {}

export interface String extends Base.BaseString<Span> {}

export interface Null extends Base.BaseNull<Span> {}

export interface Call extends Base.BaseCall<Span, Expression> {}

export interface Lambda extends Base.BaseLambda<Span, Type, Block> {
  readonly retType: Type;
}

export interface Construct extends Base.BaseConstruct<Span, ConstructField> {}

export interface ConstructField extends Base.BaseConstructField<Expression> {}

export interface Ok extends Base.BaseOk<Span, Expression> {}

export interface Err extends Base.BaseErr<Span, Expression> {}

export interface Some extends Base.BaseSome<Span, Expression> {}

export interface None extends Base.BaseNone<Span> {}

export type Type = TypeName | Maybe | Option | Result | List | Map | TypeApp | TypeVar | FuncType | TypePii;

/**
 * PII 敏感级别
 * - L1: 低敏感（如公开的邮箱地址）
 * - L2: 中敏感（如电话号码、地址）
 * - L3: 高敏感（如SSN、金融账户、健康数据）
 */
export type PiiSensitivityLevel = 'L1' | 'L2' | 'L3';

/**
 * PII 数据类别
 */
export type PiiDataCategory =
  | 'email'      // 电子邮件地址
  | 'phone'      // 电话号码
  | 'ssn'        // 社会安全号码
  | 'address'    // 物理地址
  | 'financial'  // 金融信息（银行账户、信用卡等）
  | 'health'     // 健康医疗数据
  | 'name'       // 姓名
  | 'biometric'; // 生物识别信息（指纹、面部识别等）

/**
 * PII 类型标注（AST 层）
 * 语法：@pii(L2, email) Text
 */
export interface TypePii extends AstNode {
  readonly kind: 'TypePii';
  readonly baseType: Type;
  readonly sensitivity: PiiSensitivityLevel;
  readonly category: PiiDataCategory;
}

export interface TypeName extends Base.BaseTypeName<Span> {}

export interface TypeVar extends Base.BaseTypeVar<Span> {}

export interface TypeApp extends Base.BaseTypeApp<Span, Type> {}

export interface Maybe extends Base.BaseMaybe<Span, Type> {}

export interface Option extends Base.BaseOption<Span, Type> {}

export interface Result extends Base.BaseResult<Span, Type> {}

export interface List extends Base.BaseList<Span, Type> {}

export interface Map extends Base.BaseMap<Span, Type> {}

export interface FuncType extends Base.BaseFuncType<Span, Type> {}

// Core IR types (distinct from CNL AST)
export namespace Core {
  export type CoreNode = Base.BaseNode<Origin>;

  export interface Module extends Base.BaseModule<Origin, Declaration> {}

  export interface Import extends Base.BaseImport<Origin> {}

  export interface Data extends Base.BaseData<Origin, Type> {}

  export interface Field extends Base.BaseField<Type> {}

  export interface Enum extends Base.BaseEnum<Origin> {}

  export interface Func extends Base.BaseFunc<Origin, readonly EffectEnum[], Type> {
    readonly ret: Type;
    readonly effects: readonly EffectEnum[];
    readonly body: Block;
  }

  export interface Parameter extends Base.BaseParameter<Type> {}

  export interface Block extends Base.BaseBlock<Origin, Statement> {}

  export type Declaration = Import | Data | Enum | Func;

  export interface Start extends Base.BaseStart<Origin, Expression> {}

  export interface Wait extends Base.BaseWait<Origin> {}

  export interface Scope extends Base.BaseScope<Origin, Statement> {}

  export type Statement = Let | Set | Return | If | Match | Scope | Start | Wait;

  export interface Let extends Base.BaseLet<Origin, Expression> {}

  export interface Set extends Base.BaseSet<Origin, Expression> {}

  export interface Return extends Base.BaseReturn<Origin, Expression> {}

  export interface If extends Base.BaseIf<Origin, Expression, Block> {}

  export interface Match extends Base.BaseMatch<Origin, Expression, Case> {}

  export interface Case extends Base.BaseCase<Origin, Pattern, Return | Block> {}

  export type Pattern = PatNull | PatCtor | PatName | PatInt;

  export interface PatNull extends Base.BasePatternNull<Origin> {}

  export interface PatCtor extends Base.BasePatternCtor<Origin, Pattern> {}

  export interface PatName extends Base.BasePatternName<Origin> {}

  export interface PatInt extends Base.BasePatternInt<Origin> {}

  export type Expression =
    | Name
    | Bool
    | Int
    | Long
    | Double
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

  export interface Name extends Base.BaseName<Origin> {}

  export interface Bool extends Base.BaseBool<Origin> {}

  export interface Int extends Base.BaseInt<Origin> {}

  export interface Long extends Base.BaseLong<Origin> {}

  export interface Double extends Base.BaseDouble<Origin> {}

  export interface String extends Base.BaseString<Origin> {}

  export interface Null extends Base.BaseNull<Origin> {}

  export interface Call extends Base.BaseCall<Origin, Expression> {}

  export interface Lambda extends Base.BaseLambda<Origin, Type, Block> {
    readonly ret: Type;
    readonly captures?: readonly string[];
  }

  export interface Construct extends Base.BaseConstruct<Origin, ConstructField> {}

  export interface ConstructField extends Base.BaseConstructField<Expression> {}

  export interface Ok extends Base.BaseOk<Origin, Expression> {}

  export interface Err extends Base.BaseErr<Origin, Expression> {}

  export interface Some extends Base.BaseSome<Origin, Expression> {}

  export interface None extends Base.BaseNone<Origin> {}

  export interface Await extends Base.BaseAwait<Origin, Expression> {}

  // Extended with generics (preview)
  export type Type = TypeName | Maybe | Option | Result | List | Map | TypeApp | TypeVar | FuncType | PiiType;

  /**
   * PII 类型（Core IR 层）
   * 用于运行时 PII 数据流跟踪和污点分析
   */
  export interface PiiType extends CoreNode {
    readonly kind: 'PiiType';
    readonly baseType: Type;
    readonly sensitivity: PiiSensitivityLevel;
    readonly category: PiiDataCategory;
  }

  export interface TypeName extends Base.BaseTypeName<Origin> {}

  export interface TypeVar extends Base.BaseTypeVar<Origin> {}

  export interface TypeApp extends Base.BaseTypeApp<Origin, Type> {}

  export interface Maybe extends Base.BaseMaybe<Origin, Type> {}

  export interface Option extends Base.BaseOption<Origin, Type> {}

  export interface Result extends Base.BaseResult<Origin, Type> {}

  export interface List extends Base.BaseList<Origin, Type> {}

  export interface Map extends Base.BaseMap<Origin, Type> {}

  export interface FuncType extends Base.BaseFuncType<Origin, Type> {}
}
