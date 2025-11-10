[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseRetryPolicy

Defined in: [types/base.ts:241](https://github.com/wontlost-ltd/aster-lang/blob/4bba3cc1455514643f915b45d24b39e31caea9a6/src/types/base.ts#L241)

RetryPolicy 基础接口。

## Extended by

- [`RetryPolicy`](../../interfaces/RetryPolicy.md)
- [`RetryPolicy`](../../namespaces/Core/interfaces/RetryPolicy.md)

## Properties

### maxAttempts

> `readonly` **maxAttempts**: `number`

Defined in: [types/base.ts:242](https://github.com/wontlost-ltd/aster-lang/blob/4bba3cc1455514643f915b45d24b39e31caea9a6/src/types/base.ts#L242)

***

### backoff

> `readonly` **backoff**: `"exponential"` \| `"linear"`

Defined in: [types/base.ts:243](https://github.com/wontlost-ltd/aster-lang/blob/4bba3cc1455514643f915b45d24b39e31caea9a6/src/types/base.ts#L243)
