[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseNode\<S\>

Defined in: [types/base.ts:39](https://github.com/wontlost-ltd/aster-lang/blob/3fcb2e3cbadc78c9b553141ad532d86964542285/src/types/base.ts#L39)

所有 AST 和 Core IR 节点的根接口。

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
- [`BaseWorkflow`](BaseWorkflow.md)
- [`BaseStep`](BaseStep.md)
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

Span 类型（AST: Span, Core: Origin）

## Properties

### kind

> `readonly` **kind**: `string`

Defined in: [types/base.ts:40](https://github.com/wontlost-ltd/aster-lang/blob/3fcb2e3cbadc78c9b553141ad532d86964542285/src/types/base.ts#L40)

***

### span?

> `readonly` `optional` **span**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? `never` : [`Span`](../../interfaces/Span.md)

Defined in: [types/base.ts:41](https://github.com/wontlost-ltd/aster-lang/blob/3fcb2e3cbadc78c9b553141ad532d86964542285/src/types/base.ts#L41)

***

### origin?

> `readonly` `optional` **origin**: [`HasFileProp`](../type-aliases/HasFileProp.md)\<`S`\> *extends* `true` ? [`Origin`](../../interfaces/Origin.md) : `never`

Defined in: [types/base.ts:42](https://github.com/wontlost-ltd/aster-lang/blob/3fcb2e3cbadc78c9b553141ad532d86964542285/src/types/base.ts#L42)

***

### file?

> `readonly` `optional` **file**: `null` \| `string`

Defined in: [types/base.ts:43](https://github.com/wontlost-ltd/aster-lang/blob/3fcb2e3cbadc78c9b553141ad532d86964542285/src/types/base.ts#L43)
