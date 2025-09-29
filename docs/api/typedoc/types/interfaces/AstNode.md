[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: AstNode

Defined in: [types.ts:60](https://github.com/wontlost-ltd/aster-lang/blob/f053c53a14827c4a30ae5d1a128f8c19cddebf8a/src/types.ts#L60)

## Extended by

- [`Module`](Module.md)
- [`Import`](Import.md)
- [`Data`](Data.md)
- [`Enum`](Enum.md)
- [`Func`](Func.md)
- [`Block`](Block.md)
- [`Let`](Let.md)
- [`Set`](Set.md)
- [`Return`](Return.md)
- [`If`](If.md)
- [`Match`](Match.md)
- [`Case`](Case.md)
- [`Start`](Start.md)
- [`Wait`](Wait.md)
- [`PatternNull`](PatternNull.md)
- [`PatternCtor`](PatternCtor.md)
- [`PatternName`](PatternName.md)
- [`PatternInt`](PatternInt.md)
- [`Await`](Await.md)
- [`Name`](Name.md)
- [`Bool`](Bool.md)
- [`Int`](Int.md)
- [`Long`](Long.md)
- [`Double`](Double.md)
- [`String`](String.md)
- [`Null`](Null.md)
- [`Call`](Call.md)
- [`Lambda`](Lambda.md)
- [`Construct`](Construct.md)
- [`Ok`](Ok.md)
- [`Err`](Err.md)
- [`Some`](Some.md)
- [`None`](None.md)
- [`TypeName`](TypeName.md)
- [`TypeVar`](TypeVar.md)
- [`TypeApp`](TypeApp.md)
- [`Maybe`](Maybe.md)
- [`Option`](Option.md)
- [`Result`](Result.md)
- [`List`](List.md)
- [`Map`](Map.md)
- [`FuncType`](FuncType.md)

## Properties

### kind

> `readonly` **kind**: `string`

Defined in: [types.ts:61](https://github.com/wontlost-ltd/aster-lang/blob/f053c53a14827c4a30ae5d1a128f8c19cddebf8a/src/types.ts#L61)

***

### span?

> `readonly` `optional` **span**: [`Span`](Span.md)

Defined in: [types.ts:62](https://github.com/wontlost-ltd/aster-lang/blob/f053c53a14827c4a30ae5d1a128f8c19cddebf8a/src/types.ts#L62)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types.ts:63](https://github.com/wontlost-ltd/aster-lang/blob/f053c53a14827c4a30ae5d1a128f8c19cddebf8a/src/types.ts#L63)
