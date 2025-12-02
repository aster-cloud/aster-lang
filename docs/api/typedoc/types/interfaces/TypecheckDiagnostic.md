[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: TypecheckDiagnostic

Defined in: [types.ts:129](https://github.com/wontlost-ltd/aster-lang/blob/40cf3fa867a79e63c55441efb659b6a20a4f9fa6/src/types.ts#L129)

## Properties

### severity

> **severity**: `"error"` \| `"warning"` \| `"info"`

Defined in: [types.ts:130](https://github.com/wontlost-ltd/aster-lang/blob/40cf3fa867a79e63c55441efb659b6a20a4f9fa6/src/types.ts#L130)

***

### code

> **code**: [`ErrorCode`](../../error_codes/enumerations/ErrorCode.md)

Defined in: [types.ts:131](https://github.com/wontlost-ltd/aster-lang/blob/40cf3fa867a79e63c55441efb659b6a20a4f9fa6/src/types.ts#L131)

***

### message

> **message**: `string`

Defined in: [types.ts:132](https://github.com/wontlost-ltd/aster-lang/blob/40cf3fa867a79e63c55441efb659b6a20a4f9fa6/src/types.ts#L132)

***

### span?

> `optional` **span**: [`Span`](Span.md)

Defined in: [types.ts:133](https://github.com/wontlost-ltd/aster-lang/blob/40cf3fa867a79e63c55441efb659b6a20a4f9fa6/src/types.ts#L133)

***

### origin?

> `optional` **origin**: [`Origin`](Origin.md)

Defined in: [types.ts:134](https://github.com/wontlost-ltd/aster-lang/blob/40cf3fa867a79e63c55441efb659b6a20a4f9fa6/src/types.ts#L134)

***

### help?

> `optional` **help**: `string`

Defined in: [types.ts:135](https://github.com/wontlost-ltd/aster-lang/blob/40cf3fa867a79e63c55441efb659b6a20a4f9fa6/src/types.ts#L135)

***

### data?

> `optional` **data**: `unknown`

Defined in: [types.ts:136](https://github.com/wontlost-ltd/aster-lang/blob/40cf3fa867a79e63c55441efb659b6a20a4f9fa6/src/types.ts#L136)

***

### source?

> `optional` **source**: `"aster-typecheck"` \| `"aster-pii"`

Defined in: [types.ts:137](https://github.com/wontlost-ltd/aster-lang/blob/40cf3fa867a79e63c55441efb659b6a20a4f9fa6/src/types.ts#L137)
