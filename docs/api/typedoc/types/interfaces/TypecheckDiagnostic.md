[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: TypecheckDiagnostic

Defined in: [types.ts:129](https://github.com/wontlost-ltd/aster-lang/blob/ab6eefadc7906826e24e3d67ad63e9619e1502fe/src/types.ts#L129)

## Properties

### severity

> **severity**: `"error"` \| `"warning"` \| `"info"`

Defined in: [types.ts:130](https://github.com/wontlost-ltd/aster-lang/blob/ab6eefadc7906826e24e3d67ad63e9619e1502fe/src/types.ts#L130)

***

### code

> **code**: [`ErrorCode`](../../error_codes/enumerations/ErrorCode.md)

Defined in: [types.ts:131](https://github.com/wontlost-ltd/aster-lang/blob/ab6eefadc7906826e24e3d67ad63e9619e1502fe/src/types.ts#L131)

***

### message

> **message**: `string`

Defined in: [types.ts:132](https://github.com/wontlost-ltd/aster-lang/blob/ab6eefadc7906826e24e3d67ad63e9619e1502fe/src/types.ts#L132)

***

### span?

> `optional` **span**: [`Span`](Span.md)

Defined in: [types.ts:133](https://github.com/wontlost-ltd/aster-lang/blob/ab6eefadc7906826e24e3d67ad63e9619e1502fe/src/types.ts#L133)

***

### origin?

> `optional` **origin**: [`Origin`](Origin.md)

Defined in: [types.ts:134](https://github.com/wontlost-ltd/aster-lang/blob/ab6eefadc7906826e24e3d67ad63e9619e1502fe/src/types.ts#L134)

***

### help?

> `optional` **help**: `string`

Defined in: [types.ts:135](https://github.com/wontlost-ltd/aster-lang/blob/ab6eefadc7906826e24e3d67ad63e9619e1502fe/src/types.ts#L135)

***

### data?

> `optional` **data**: `unknown`

Defined in: [types.ts:136](https://github.com/wontlost-ltd/aster-lang/blob/ab6eefadc7906826e24e3d67ad63e9619e1502fe/src/types.ts#L136)
