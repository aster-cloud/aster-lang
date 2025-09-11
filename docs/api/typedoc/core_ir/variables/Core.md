[**aster-lang**](../../README.md)

***

# Variable: Core

> `const` **Core**: `object`

Defined in: [core\_ir.ts:8](https://github.com/wontlost-ltd/aster-lang/blob/b53f24492721cfed9e6a591422966205af1dad8e/src/core_ir.ts#L8)

## Type Declaration

### Module()

> **Module**: (`name`, `decls`) => [`Module`](../../types/namespaces/Core/interfaces/Module.md)

#### Parameters

##### name

`null` | `string`

##### decls

readonly [`Declaration`](../../types/namespaces/Core/type-aliases/Declaration.md)[]

#### Returns

[`Module`](../../types/namespaces/Core/interfaces/Module.md)

### Import()

> **Import**: (`name`, `asName`) => [`Import`](../../types/namespaces/Core/interfaces/Import.md)

#### Parameters

##### name

`string`

##### asName

`null` | `string`

#### Returns

[`Import`](../../types/namespaces/Core/interfaces/Import.md)

### Data()

> **Data**: (`name`, `fields`) => [`Data`](../../types/namespaces/Core/interfaces/Data.md)

#### Parameters

##### name

`string`

##### fields

readonly [`Field`](../../types/namespaces/Core/interfaces/Field.md)[]

#### Returns

[`Data`](../../types/namespaces/Core/interfaces/Data.md)

### Enum()

> **Enum**: (`name`, `variants`) => [`Enum`](../../types/namespaces/Core/interfaces/Enum.md)

#### Parameters

##### name

`string`

##### variants

readonly `string`[]

#### Returns

[`Enum`](../../types/namespaces/Core/interfaces/Enum.md)

### Func()

> **Func**: (`name`, `params`, `ret`, `effects`, `body`) => [`Func`](../../types/namespaces/Core/interfaces/Func.md)

#### Parameters

##### name

`string`

##### params

readonly [`Parameter`](../../types/namespaces/Core/interfaces/Parameter.md)[]

##### ret

[`Type`](../../types/namespaces/Core/type-aliases/Type.md)

##### effects

readonly [`Effect`](../../types/enumerations/Effect.md)[]

##### body

[`Block`](../../types/namespaces/Core/interfaces/Block.md)

#### Returns

[`Func`](../../types/namespaces/Core/interfaces/Func.md)

### Block()

> **Block**: (`statements`) => [`Block`](../../types/namespaces/Core/interfaces/Block.md)

#### Parameters

##### statements

readonly [`Statement`](../../types/namespaces/Core/type-aliases/Statement.md)[]

#### Returns

[`Block`](../../types/namespaces/Core/interfaces/Block.md)

### Scope()

> **Scope**: (`statements`) => [`Scope`](../../types/namespaces/Core/interfaces/Scope.md)

#### Parameters

##### statements

readonly [`Statement`](../../types/namespaces/Core/type-aliases/Statement.md)[]

#### Returns

[`Scope`](../../types/namespaces/Core/interfaces/Scope.md)

### Let()

> **Let**: (`name`, `expr`) => [`Let`](../../types/namespaces/Core/interfaces/Let.md)

#### Parameters

##### name

`string`

##### expr

[`Expression`](../../types/namespaces/Core/type-aliases/Expression.md)

#### Returns

[`Let`](../../types/namespaces/Core/interfaces/Let.md)

### Set()

> **Set**: (`name`, `expr`) => [`Set`](../../types/namespaces/Core/interfaces/Set.md)

#### Parameters

##### name

`string`

##### expr

[`Expression`](../../types/namespaces/Core/type-aliases/Expression.md)

#### Returns

[`Set`](../../types/namespaces/Core/interfaces/Set.md)

### Return()

> **Return**: (`expr`) => [`Return`](../../types/namespaces/Core/interfaces/Return.md)

#### Parameters

##### expr

[`Expression`](../../types/namespaces/Core/type-aliases/Expression.md)

#### Returns

[`Return`](../../types/namespaces/Core/interfaces/Return.md)

### If()

> **If**: (`cond`, `thenBlock`, `elseBlock`) => [`If`](../../types/namespaces/Core/interfaces/If.md)

#### Parameters

##### cond

[`Expression`](../../types/namespaces/Core/type-aliases/Expression.md)

##### thenBlock

[`Block`](../../types/namespaces/Core/interfaces/Block.md)

##### elseBlock

`null` | [`Block`](../../types/namespaces/Core/interfaces/Block.md)

#### Returns

[`If`](../../types/namespaces/Core/interfaces/If.md)

### Match()

> **Match**: (`expr`, `cases`) => [`Match`](../../types/namespaces/Core/interfaces/Match.md)

#### Parameters

##### expr

[`Expression`](../../types/namespaces/Core/type-aliases/Expression.md)

##### cases

readonly [`Case`](../../types/namespaces/Core/interfaces/Case.md)[]

#### Returns

[`Match`](../../types/namespaces/Core/interfaces/Match.md)

### Case()

> **Case**: (`pattern`, `body`) => [`Case`](../../types/namespaces/Core/interfaces/Case.md)

#### Parameters

##### pattern

[`Pattern`](../../types/namespaces/Core/type-aliases/Pattern.md)

##### body

[`Block`](../../types/namespaces/Core/interfaces/Block.md) | [`Return`](../../types/namespaces/Core/interfaces/Return.md)

#### Returns

[`Case`](../../types/namespaces/Core/interfaces/Case.md)

### Start()

> **Start**: (`name`, `expr`) => [`Start`](../../types/namespaces/Core/interfaces/Start.md)

#### Parameters

##### name

`string`

##### expr

[`Expression`](../../types/namespaces/Core/type-aliases/Expression.md)

#### Returns

[`Start`](../../types/namespaces/Core/interfaces/Start.md)

### Wait()

> **Wait**: (`names`) => [`Wait`](../../types/namespaces/Core/interfaces/Wait.md)

#### Parameters

##### names

readonly `string`[]

#### Returns

[`Wait`](../../types/namespaces/Core/interfaces/Wait.md)

### Name()

> **Name**: (`name`) => [`Name`](../../types/namespaces/Core/interfaces/Name.md)

#### Parameters

##### name

`string`

#### Returns

[`Name`](../../types/namespaces/Core/interfaces/Name.md)

### Bool()

> **Bool**: (`value`) => [`Bool`](../../types/namespaces/Core/interfaces/Bool.md)

#### Parameters

##### value

`boolean`

#### Returns

[`Bool`](../../types/namespaces/Core/interfaces/Bool.md)

### Int()

> **Int**: (`value`) => [`Int`](../../types/namespaces/Core/interfaces/Int.md)

#### Parameters

##### value

`number`

#### Returns

[`Int`](../../types/namespaces/Core/interfaces/Int.md)

### String()

> **String**: (`value`) => [`String`](../../types/namespaces/Core/interfaces/String.md)

#### Parameters

##### value

`string`

#### Returns

[`String`](../../types/namespaces/Core/interfaces/String.md)

### Null()

> **Null**: () => [`Null`](../../types/namespaces/Core/interfaces/Null.md)

#### Returns

[`Null`](../../types/namespaces/Core/interfaces/Null.md)

### Call()

> **Call**: (`target`, `args`) => [`Call`](../../types/namespaces/Core/interfaces/Call.md)

#### Parameters

##### target

[`Expression`](../../types/namespaces/Core/type-aliases/Expression.md)

##### args

readonly [`Expression`](../../types/namespaces/Core/type-aliases/Expression.md)[]

#### Returns

[`Call`](../../types/namespaces/Core/interfaces/Call.md)

### Construct()

> **Construct**: (`typeName`, `fields`) => [`Construct`](../../types/namespaces/Core/interfaces/Construct.md)

#### Parameters

##### typeName

`string`

##### fields

readonly [`ConstructField`](../../types/namespaces/Core/interfaces/ConstructField.md)[]

#### Returns

[`Construct`](../../types/namespaces/Core/interfaces/Construct.md)

### Ok()

> **Ok**: (`expr`) => [`Ok`](../../types/namespaces/Core/interfaces/Ok.md)

#### Parameters

##### expr

[`Expression`](../../types/namespaces/Core/type-aliases/Expression.md)

#### Returns

[`Ok`](../../types/namespaces/Core/interfaces/Ok.md)

### Err()

> **Err**: (`expr`) => [`Err`](../../types/namespaces/Core/interfaces/Err.md)

#### Parameters

##### expr

[`Expression`](../../types/namespaces/Core/type-aliases/Expression.md)

#### Returns

[`Err`](../../types/namespaces/Core/interfaces/Err.md)

### Some()

> **Some**: (`expr`) => [`Some`](../../types/namespaces/Core/interfaces/Some.md)

#### Parameters

##### expr

[`Expression`](../../types/namespaces/Core/type-aliases/Expression.md)

#### Returns

[`Some`](../../types/namespaces/Core/interfaces/Some.md)

### None()

> **None**: () => [`None`](../../types/namespaces/Core/interfaces/None.md)

#### Returns

[`None`](../../types/namespaces/Core/interfaces/None.md)

### TypeName()

> **TypeName**: (`name`) => [`TypeName`](../../types/namespaces/Core/interfaces/TypeName.md)

#### Parameters

##### name

`string`

#### Returns

[`TypeName`](../../types/namespaces/Core/interfaces/TypeName.md)

### Maybe()

> **Maybe**: (`type`) => [`Maybe`](../../types/namespaces/Core/interfaces/Maybe.md)

#### Parameters

##### type

[`Type`](../../types/namespaces/Core/type-aliases/Type.md)

#### Returns

[`Maybe`](../../types/namespaces/Core/interfaces/Maybe.md)

### Option()

> **Option**: (`type`) => [`Option`](../../types/namespaces/Core/interfaces/Option.md)

#### Parameters

##### type

[`Type`](../../types/namespaces/Core/type-aliases/Type.md)

#### Returns

[`Option`](../../types/namespaces/Core/interfaces/Option.md)

### Result()

> **Result**: (`ok`, `err`) => [`Result`](../../types/namespaces/Core/interfaces/Result.md)

#### Parameters

##### ok

[`Type`](../../types/namespaces/Core/type-aliases/Type.md)

##### err

[`Type`](../../types/namespaces/Core/type-aliases/Type.md)

#### Returns

[`Result`](../../types/namespaces/Core/interfaces/Result.md)

### List()

> **List**: (`type`) => [`List`](../../types/namespaces/Core/interfaces/List.md)

#### Parameters

##### type

[`Type`](../../types/namespaces/Core/type-aliases/Type.md)

#### Returns

[`List`](../../types/namespaces/Core/interfaces/List.md)

### Map()

> **Map**: (`key`, `val`) => [`Map`](../../types/namespaces/Core/interfaces/Map.md)

#### Parameters

##### key

[`Type`](../../types/namespaces/Core/type-aliases/Type.md)

##### val

[`Type`](../../types/namespaces/Core/type-aliases/Type.md)

#### Returns

[`Map`](../../types/namespaces/Core/interfaces/Map.md)

### PatNull()

> **PatNull**: () => [`PatNull`](../../types/namespaces/Core/interfaces/PatNull.md)

#### Returns

[`PatNull`](../../types/namespaces/Core/interfaces/PatNull.md)

### Await()

> **Await**: (`expr`) => [`Expression`](../../types/namespaces/Core/type-aliases/Expression.md)

#### Parameters

##### expr

[`Expression`](../../types/namespaces/Core/type-aliases/Expression.md)

#### Returns

[`Expression`](../../types/namespaces/Core/type-aliases/Expression.md)

### PatCtor()

> **PatCtor**: (`typeName`, `names`) => [`PatCtor`](../../types/namespaces/Core/interfaces/PatCtor.md)

#### Parameters

##### typeName

`string`

##### names

readonly `string`[]

#### Returns

[`PatCtor`](../../types/namespaces/Core/interfaces/PatCtor.md)

### PatName()

> **PatName**: (`name`) => [`PatName`](../../types/namespaces/Core/interfaces/PatName.md)

#### Parameters

##### name

`string`

#### Returns

[`PatName`](../../types/namespaces/Core/interfaces/PatName.md)
