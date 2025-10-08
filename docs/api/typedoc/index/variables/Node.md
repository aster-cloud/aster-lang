[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Variable: Node

> `const` **Node**: `object`

Defined in: [ast.ts:4](https://github.com/wontlost-ltd/aster-lang/blob/f2b08094a4f9f64a0177a5c0498280a96fec6f93/src/ast.ts#L4)

## Type Declaration

### Module()

> **Module**: (`name`, `decls`) => [`Module`](../../types/interfaces/Module.md)

#### Parameters

##### name

`null` | `string`

##### decls

readonly [`Declaration`](../../types/type-aliases/Declaration.md)[]

#### Returns

[`Module`](../../types/interfaces/Module.md)

### Import()

> **Import**: (`name`, `asName`) => [`Import`](../../types/interfaces/Import.md)

#### Parameters

##### name

`string`

##### asName

`null` | `string`

#### Returns

[`Import`](../../types/interfaces/Import.md)

### Data()

> **Data**: (`name`, `fields`) => [`Data`](../../types/interfaces/Data.md)

#### Parameters

##### name

`string`

##### fields

readonly [`Field`](../../types/interfaces/Field.md)[]

#### Returns

[`Data`](../../types/interfaces/Data.md)

### Enum()

> **Enum**: (`name`, `variants`) => [`Enum`](../../types/interfaces/Enum.md)

#### Parameters

##### name

`string`

##### variants

readonly `string`[]

#### Returns

[`Enum`](../../types/interfaces/Enum.md)

### Func()

> **Func**: (`name`, `typeParams`, `params`, `retType`, `effects`, `body`) => [`Func`](../../types/interfaces/Func.md)

#### Parameters

##### name

`string`

##### typeParams

readonly `string`[]

##### params

readonly [`Parameter`](../../types/interfaces/Parameter.md)[]

##### retType

[`Type`](../../types/type-aliases/Type.md)

##### effects

readonly `string`[]

##### body

`null` | [`Block`](../../types/interfaces/Block.md)

#### Returns

[`Func`](../../types/interfaces/Func.md)

### Block()

> **Block**: (`statements`) => [`Block`](../../types/interfaces/Block.md)

#### Parameters

##### statements

readonly [`Statement`](../../types/type-aliases/Statement.md)[]

#### Returns

[`Block`](../../types/interfaces/Block.md)

### Let()

> **Let**: (`name`, `expr`) => [`Let`](../../types/interfaces/Let.md)

#### Parameters

##### name

`string`

##### expr

[`Expression`](../../types/type-aliases/Expression.md)

#### Returns

[`Let`](../../types/interfaces/Let.md)

### Set()

> **Set**: (`name`, `expr`) => [`Set`](../../types/interfaces/Set.md)

#### Parameters

##### name

`string`

##### expr

[`Expression`](../../types/type-aliases/Expression.md)

#### Returns

[`Set`](../../types/interfaces/Set.md)

### Return()

> **Return**: (`expr`) => [`Return`](../../types/interfaces/Return.md)

#### Parameters

##### expr

[`Expression`](../../types/type-aliases/Expression.md)

#### Returns

[`Return`](../../types/interfaces/Return.md)

### If()

> **If**: (`cond`, `thenBlock`, `elseBlock`) => [`If`](../../types/interfaces/If.md)

#### Parameters

##### cond

[`Expression`](../../types/type-aliases/Expression.md)

##### thenBlock

[`Block`](../../types/interfaces/Block.md)

##### elseBlock

`null` | [`Block`](../../types/interfaces/Block.md)

#### Returns

[`If`](../../types/interfaces/If.md)

### Match()

> **Match**: (`expr`, `cases`) => [`Match`](../../types/interfaces/Match.md)

#### Parameters

##### expr

[`Expression`](../../types/type-aliases/Expression.md)

##### cases

readonly [`Case`](../../types/interfaces/Case.md)[]

#### Returns

[`Match`](../../types/interfaces/Match.md)

### Case()

> **Case**: (`pattern`, `body`) => [`Case`](../../types/interfaces/Case.md)

#### Parameters

##### pattern

[`Pattern`](../../types/type-aliases/Pattern.md)

##### body

[`Block`](../../types/interfaces/Block.md) | [`Return`](../../types/interfaces/Return.md)

#### Returns

[`Case`](../../types/interfaces/Case.md)

### Start()

> **Start**: (`name`, `expr`) => [`Start`](../../types/interfaces/Start.md)

#### Parameters

##### name

`string`

##### expr

[`Expression`](../../types/type-aliases/Expression.md)

#### Returns

[`Start`](../../types/interfaces/Start.md)

### Wait()

> **Wait**: (`names`) => [`Wait`](../../types/interfaces/Wait.md)

#### Parameters

##### names

readonly `string`[]

#### Returns

[`Wait`](../../types/interfaces/Wait.md)

### Name()

> **Name**: (`name`) => [`Name`](../../types/interfaces/Name.md)

#### Parameters

##### name

`string`

#### Returns

[`Name`](../../types/interfaces/Name.md)

### Bool()

> **Bool**: (`value`) => [`Bool`](../../types/interfaces/Bool.md)

#### Parameters

##### value

`boolean`

#### Returns

[`Bool`](../../types/interfaces/Bool.md)

### Null()

> **Null**: () => [`Null`](../../types/interfaces/Null.md)

#### Returns

[`Null`](../../types/interfaces/Null.md)

### Int()

> **Int**: (`value`) => [`Int`](../../types/interfaces/Int.md)

#### Parameters

##### value

`number`

#### Returns

[`Int`](../../types/interfaces/Int.md)

### Long()

> **Long**: (`value`) => [`Long`](../../types/interfaces/Long.md)

#### Parameters

##### value

`number`

#### Returns

[`Long`](../../types/interfaces/Long.md)

### Double()

> **Double**: (`value`) => [`Double`](../../types/interfaces/Double.md)

#### Parameters

##### value

`number`

#### Returns

[`Double`](../../types/interfaces/Double.md)

### String()

> **String**: (`value`) => [`String`](../../types/interfaces/String.md)

#### Parameters

##### value

`string`

#### Returns

[`String`](../../types/interfaces/String.md)

### Call()

> **Call**: (`target`, `args`) => [`Call`](../../types/interfaces/Call.md)

#### Parameters

##### target

[`Expression`](../../types/type-aliases/Expression.md)

##### args

readonly [`Expression`](../../types/type-aliases/Expression.md)[]

#### Returns

[`Call`](../../types/interfaces/Call.md)

### Construct()

> **Construct**: (`typeName`, `fields`) => [`Construct`](../../types/interfaces/Construct.md)

#### Parameters

##### typeName

`string`

##### fields

readonly [`ConstructField`](../../types/interfaces/ConstructField.md)[]

#### Returns

[`Construct`](../../types/interfaces/Construct.md)

### Ok()

> **Ok**: (`expr`) => [`Ok`](../../types/interfaces/Ok.md)

#### Parameters

##### expr

[`Expression`](../../types/type-aliases/Expression.md)

#### Returns

[`Ok`](../../types/interfaces/Ok.md)

### Err()

> **Err**: (`expr`) => [`Err`](../../types/interfaces/Err.md)

#### Parameters

##### expr

[`Expression`](../../types/type-aliases/Expression.md)

#### Returns

[`Err`](../../types/interfaces/Err.md)

### Some()

> **Some**: (`expr`) => [`Some`](../../types/interfaces/Some.md)

#### Parameters

##### expr

[`Expression`](../../types/type-aliases/Expression.md)

#### Returns

[`Some`](../../types/interfaces/Some.md)

### None()

> **None**: () => [`None`](../../types/interfaces/None.md)

#### Returns

[`None`](../../types/interfaces/None.md)

### Lambda()

> **Lambda**: (`params`, `retType`, `body`) => [`Lambda`](../../types/interfaces/Lambda.md)

#### Parameters

##### params

readonly [`Parameter`](../../types/interfaces/Parameter.md)[]

##### retType

[`Type`](../../types/type-aliases/Type.md)

##### body

[`Block`](../../types/interfaces/Block.md)

#### Returns

[`Lambda`](../../types/interfaces/Lambda.md)

### Await()

> **Await**: (`expr`) => [`Await`](../../types/interfaces/Await.md)

#### Parameters

##### expr

[`Expression`](../../types/type-aliases/Expression.md)

#### Returns

[`Await`](../../types/interfaces/Await.md)

### TypeName()

> **TypeName**: (`name`) => [`TypeName`](../../types/interfaces/TypeName.md)

#### Parameters

##### name

`string`

#### Returns

[`TypeName`](../../types/interfaces/TypeName.md)

### Maybe()

> **Maybe**: (`type`) => [`Maybe`](../../types/interfaces/Maybe.md)

#### Parameters

##### type

[`Type`](../../types/type-aliases/Type.md)

#### Returns

[`Maybe`](../../types/interfaces/Maybe.md)

### Option()

> **Option**: (`type`) => [`Option`](../../types/interfaces/Option.md)

#### Parameters

##### type

[`Type`](../../types/type-aliases/Type.md)

#### Returns

[`Option`](../../types/interfaces/Option.md)

### Result()

> **Result**: (`ok`, `err`) => [`Result`](../../types/interfaces/Result.md)

#### Parameters

##### ok

[`Type`](../../types/type-aliases/Type.md)

##### err

[`Type`](../../types/type-aliases/Type.md)

#### Returns

[`Result`](../../types/interfaces/Result.md)

### List()

> **List**: (`type`) => [`List`](../../types/interfaces/List.md)

#### Parameters

##### type

[`Type`](../../types/type-aliases/Type.md)

#### Returns

[`List`](../../types/interfaces/List.md)

### Map()

> **Map**: (`key`, `val`) => [`Map`](../../types/interfaces/Map.md)

#### Parameters

##### key

[`Type`](../../types/type-aliases/Type.md)

##### val

[`Type`](../../types/type-aliases/Type.md)

#### Returns

[`Map`](../../types/interfaces/Map.md)

### TypeApp()

> **TypeApp**: (`base`, `args`) => [`TypeApp`](../../types/interfaces/TypeApp.md)

#### Parameters

##### base

`string`

##### args

readonly [`Type`](../../types/type-aliases/Type.md)[]

#### Returns

[`TypeApp`](../../types/interfaces/TypeApp.md)

### TypeVar()

> **TypeVar**: (`name`) => [`TypeVar`](../../types/interfaces/TypeVar.md)

#### Parameters

##### name

`string`

#### Returns

[`TypeVar`](../../types/interfaces/TypeVar.md)

### TypePii()

> **TypePii**: (`baseType`, `sensitivity`, `category`) => [`TypePii`](../../types/interfaces/TypePii.md)

#### Parameters

##### baseType

[`Type`](../../types/type-aliases/Type.md)

##### sensitivity

[`PiiSensitivityLevel`](../../types/type-aliases/PiiSensitivityLevel.md)

##### category

[`PiiDataCategory`](../../types/type-aliases/PiiDataCategory.md)

#### Returns

[`TypePii`](../../types/interfaces/TypePii.md)

### PatternNull()

> **PatternNull**: () => [`PatternNull`](../../types/interfaces/PatternNull.md)

#### Returns

[`PatternNull`](../../types/interfaces/PatternNull.md)

### PatternCtor()

> **PatternCtor**: (`typeName`, `names`, `args?`) => [`PatternCtor`](../../types/interfaces/PatternCtor.md)

#### Parameters

##### typeName

`string`

##### names

readonly `string`[]

##### args?

readonly [`Pattern`](../../types/type-aliases/Pattern.md)[]

#### Returns

[`PatternCtor`](../../types/interfaces/PatternCtor.md)

### PatternName()

> **PatternName**: (`name`) => [`PatternName`](../../types/interfaces/PatternName.md)

#### Parameters

##### name

`string`

#### Returns

[`PatternName`](../../types/interfaces/PatternName.md)

### PatternInt()

> **PatternInt**: (`value`) => [`PatternInt`](../../types/interfaces/PatternInt.md)

#### Parameters

##### value

`number`

#### Returns

[`PatternInt`](../../types/interfaces/PatternInt.md)
