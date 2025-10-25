[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseNode\<S\>

Defined in: [types/base.ts:32](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L32)

## Extended by

- [`BaseImport`](BaseImport.md)
- [`BaseData`](BaseData.md)
- [`BaseEnum`](BaseEnum.md)
- [`BaseModule`](BaseModule.md)
- [`BaseFunc`](BaseFunc.md)
- [`BaseLet`](BaseLet.md)
- [`BaseSet`](BaseSet.md)
- [`BaseReturn`](BaseReturn.md)
- [`BaseBlock`](BaseBlock.md)
- [`BaseScope`](BaseScope.md)
- [`BaseIf`](BaseIf.md)
- [`BaseMatch`](BaseMatch.md)
- [`BaseCase`](BaseCase.md)
- [`BaseStart`](BaseStart.md)
- [`BaseWait`](BaseWait.md)
- [`BasePatternNull`](BasePatternNull.md)
- [`BasePatternCtor`](BasePatternCtor.md)
- [`BasePatternName`](BasePatternName.md)
- [`BasePatternInt`](BasePatternInt.md)
- [`BaseName`](BaseName.md)
- [`BaseBool`](BaseBool.md)
- [`BaseInt`](BaseInt.md)
- [`BaseLong`](BaseLong.md)
- [`BaseDouble`](BaseDouble.md)
- [`BaseString`](BaseString.md)
- [`BaseNull`](BaseNull.md)
- [`BaseCall`](BaseCall.md)
- [`BaseLambda`](BaseLambda.md)
- [`BaseConstruct`](BaseConstruct.md)
- [`BaseOk`](BaseOk.md)
- [`BaseErr`](BaseErr.md)
- [`BaseSome`](BaseSome.md)
- [`BaseNone`](BaseNone.md)
- [`BaseAwait`](BaseAwait.md)
- [`BaseTypeName`](BaseTypeName.md)
- [`BaseTypeVar`](BaseTypeVar.md)
- [`BaseTypeApp`](BaseTypeApp.md)
- [`BaseMaybe`](BaseMaybe.md)
- [`BaseOption`](BaseOption.md)
- [`BaseResult`](BaseResult.md)
- [`BaseList`](BaseList.md)
- [`BaseMap`](BaseMap.md)
- [`BaseFuncType`](BaseFuncType.md)

## Type Parameters

### S

`S` = [`Span`](../../interfaces/Span.md) \| [`Origin`](../../interfaces/Origin.md)

## Properties

### kind

> `readonly` **kind**: `string`

Defined in: [types/base.ts:33](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L33)

***

### span?

> `readonly` `optional` **span**: `HasFileProp`\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:34](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L34)

***

### origin?

> `readonly` `optional` **origin**: `HasFileProp`\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:35](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L35)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:36](https://github.com/wontlost-ltd/aster-lang/blob/8b88510e1ccceb11c9859896707837644101a436/src/types/base.ts#L36)
