[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: TypecheckDiagnostic

Defined in: [types.ts:126](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types.ts#L126)

## Properties

### severity

> **severity**: `"error"` \| `"warning"` \| `"info"`

Defined in: [types.ts:127](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types.ts#L127)

***

### code

> **code**: [`ErrorCode`](../../error_codes/enumerations/ErrorCode.md)

Defined in: [types.ts:128](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types.ts#L128)

***

### message

> **message**: `string`

Defined in: [types.ts:129](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types.ts#L129)

***

### span?

> `optional` **span**: [`Span`](Span.md)

Defined in: [types.ts:130](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types.ts#L130)

***

### origin?

> `optional` **origin**: [`Origin`](Origin.md)

Defined in: [types.ts:131](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types.ts#L131)

***

### help?

> `optional` **help**: `string`

Defined in: [types.ts:132](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types.ts#L132)

***

### data?

> `optional` **data**: `unknown`

Defined in: [types.ts:133](https://github.com/wontlost-ltd/aster-lang/blob/5c134b7830ecb54926b0e82a270cbd2b8e3b9761/src/types.ts#L133)
