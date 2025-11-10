[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseRetryPolicy

Defined in: [types/base.ts:240](https://github.com/wontlost-ltd/aster-lang/blob/8e7741ab305219c4b2df3c1661d37343186d5d42/src/types/base.ts#L240)

RetryPolicy 基础接口。

## Extended by

- [`RetryPolicy`](../../interfaces/RetryPolicy.md)
- [`RetryPolicy`](../../namespaces/Core/interfaces/RetryPolicy.md)

## Properties

### maxAttempts

> `readonly` **maxAttempts**: `number`

Defined in: [types/base.ts:241](https://github.com/wontlost-ltd/aster-lang/blob/8e7741ab305219c4b2df3c1661d37343186d5d42/src/types/base.ts#L241)

***

### backoff

> `readonly` **backoff**: `"exponential"` \| `"linear"`

Defined in: [types/base.ts:242](https://github.com/wontlost-ltd/aster-lang/blob/8e7741ab305219c4b2df3c1661d37343186d5d42/src/types/base.ts#L242)
