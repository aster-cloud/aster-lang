[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseRetryPolicy

Defined in: [types/base.ts:241](https://github.com/wontlost-ltd/aster-lang/blob/b6ce5257cbcdd765132b6b1613adb34added24b2/src/types/base.ts#L241)

RetryPolicy 基础接口。

## Extended by

- [`RetryPolicy`](../../interfaces/RetryPolicy.md)
- [`RetryPolicy`](../../namespaces/Core/interfaces/RetryPolicy.md)

## Properties

### maxAttempts

> `readonly` **maxAttempts**: `number`

Defined in: [types/base.ts:242](https://github.com/wontlost-ltd/aster-lang/blob/b6ce5257cbcdd765132b6b1613adb34added24b2/src/types/base.ts#L242)

***

### backoff

> `readonly` **backoff**: `"exponential"` \| `"linear"`

Defined in: [types/base.ts:243](https://github.com/wontlost-ltd/aster-lang/blob/b6ce5257cbcdd765132b6b1613adb34added24b2/src/types/base.ts#L243)
