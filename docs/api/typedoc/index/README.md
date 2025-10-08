[**@wontlost-ltd/aster-lang**](../README.md)

***

# index

## Example

```typescript
import { canonicalize, lex, parse, lowerModule } from '@wontlost-ltd/aster-lang';

const src = `This module is app. To id, produce Int: Return 1.`;
const canonical = canonicalize(src);  // 规范化源代码
const tokens = lex(canonical);         // 词法分析
const ast = parse(tokens);             // 语法分析
const core = lowerModule(ast);         // 降级到 Core IR
console.log(core);
```

## Variables

- [Node](variables/Node.md)

## References

### canonicalize

Re-exports [canonicalize](../canonicalizer/functions/canonicalize.md)

***

### lex

Re-exports [lex](../lexer/functions/lex.md)

***

### parse

Re-exports [parse](../parser/functions/parse.md)

***

### lowerModule

Re-exports [lowerModule](../lower_to_core/functions/lowerModule.md)

***

### Core

Re-exports [Core](../core_ir/variables/Core.md)

***

### Effect

Re-exports [Effect](../config/semantic/enumerations/Effect.md)

***

### TokenKind

Re-exports [TokenKind](../types/enumerations/TokenKind.md)

***

### KW

Re-exports [KW](../config/semantic/variables/KW.md)

***

### Position

Re-exports [Position](../types/interfaces/Position.md)

***

### Span

Re-exports [Span](../types/interfaces/Span.md)

***

### Origin

Re-exports [Origin](../types/interfaces/Origin.md)

***

### Token

Re-exports [Token](../types/interfaces/Token.md)

***

### TypecheckDiagnostic

Re-exports [TypecheckDiagnostic](../types/interfaces/TypecheckDiagnostic.md)

***

### AstNode

Re-exports [AstNode](../types/type-aliases/AstNode.md)

***

### Module

Re-exports [Module](../types/interfaces/Module.md)

***

### Import

Re-exports [Import](../types/interfaces/Import.md)

***

### Data

Re-exports [Data](../types/interfaces/Data.md)

***

### Field

Re-exports [Field](../types/interfaces/Field.md)

***

### Enum

Re-exports [Enum](../types/interfaces/Enum.md)

***

### Func

Re-exports [Func](../types/interfaces/Func.md)

***

### Parameter

Re-exports [Parameter](../types/interfaces/Parameter.md)

***

### Block

Re-exports [Block](../types/interfaces/Block.md)

***

### Declaration

Re-exports [Declaration](../types/type-aliases/Declaration.md)

***

### Statement

Re-exports [Statement](../types/type-aliases/Statement.md)

***

### Let

Re-exports [Let](../types/interfaces/Let.md)

***

### Set

Re-exports [Set](../types/interfaces/Set.md)

***

### Return

Re-exports [Return](../types/interfaces/Return.md)

***

### If

Re-exports [If](../types/interfaces/If.md)

***

### Match

Re-exports [Match](../types/interfaces/Match.md)

***

### Case

Re-exports [Case](../types/interfaces/Case.md)

***

### Start

Re-exports [Start](../types/interfaces/Start.md)

***

### Wait

Re-exports [Wait](../types/interfaces/Wait.md)

***

### Pattern

Re-exports [Pattern](../types/type-aliases/Pattern.md)

***

### PatternNull

Re-exports [PatternNull](../types/interfaces/PatternNull.md)

***

### PatternCtor

Re-exports [PatternCtor](../types/interfaces/PatternCtor.md)

***

### PatternName

Re-exports [PatternName](../types/interfaces/PatternName.md)

***

### PatternInt

Re-exports [PatternInt](../types/interfaces/PatternInt.md)

***

### Expression

Re-exports [Expression](../types/type-aliases/Expression.md)

***

### Await

Re-exports [Await](../types/interfaces/Await.md)

***

### Name

Re-exports [Name](../types/interfaces/Name.md)

***

### Bool

Re-exports [Bool](../types/interfaces/Bool.md)

***

### Int

Re-exports [Int](../types/interfaces/Int.md)

***

### Long

Re-exports [Long](../types/interfaces/Long.md)

***

### Double

Re-exports [Double](../types/interfaces/Double.md)

***

### String

Re-exports [String](../types/interfaces/String.md)

***

### Null

Re-exports [Null](../types/interfaces/Null.md)

***

### Call

Re-exports [Call](../types/interfaces/Call.md)

***

### Lambda

Re-exports [Lambda](../types/interfaces/Lambda.md)

***

### Construct

Re-exports [Construct](../types/interfaces/Construct.md)

***

### ConstructField

Re-exports [ConstructField](../types/interfaces/ConstructField.md)

***

### Ok

Re-exports [Ok](../types/interfaces/Ok.md)

***

### Err

Re-exports [Err](../types/interfaces/Err.md)

***

### Some

Re-exports [Some](../types/interfaces/Some.md)

***

### None

Re-exports [None](../types/interfaces/None.md)

***

### Type

Re-exports [Type](../types/type-aliases/Type.md)

***

### PiiSensitivityLevel

Re-exports [PiiSensitivityLevel](../types/type-aliases/PiiSensitivityLevel.md)

***

### PiiDataCategory

Re-exports [PiiDataCategory](../types/type-aliases/PiiDataCategory.md)

***

### TypePii

Re-exports [TypePii](../types/interfaces/TypePii.md)

***

### TypeName

Re-exports [TypeName](../types/interfaces/TypeName.md)

***

### TypeVar

Re-exports [TypeVar](../types/interfaces/TypeVar.md)

***

### TypeApp

Re-exports [TypeApp](../types/interfaces/TypeApp.md)

***

### Maybe

Re-exports [Maybe](../types/interfaces/Maybe.md)

***

### Option

Re-exports [Option](../types/interfaces/Option.md)

***

### Result

Re-exports [Result](../types/interfaces/Result.md)

***

### List

Re-exports [List](../types/interfaces/List.md)

***

### Map

Re-exports [Map](../types/interfaces/Map.md)

***

### FuncType

Re-exports [FuncType](../types/interfaces/FuncType.md)
