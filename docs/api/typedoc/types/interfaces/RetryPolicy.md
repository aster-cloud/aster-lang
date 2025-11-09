[**@wontlost-ltd/aster-lang**](../../README.md)

***

# Interface: RetryPolicy

Defined in: [types.ts:241](https://github.com/wontlost-ltd/aster-lang/blob/ec56dc5865274099ba9065d4f0b63ed821e2967c/src/types.ts#L241)

RetryPolicy 基础接口。

## Extends

- [`BaseRetryPolicy`](../base/interfaces/BaseRetryPolicy.md)

## Properties

### maxAttempts

> `readonly` **maxAttempts**: `number`

Defined in: [types/base.ts:241](https://github.com/wontlost-ltd/aster-lang/blob/ec56dc5865274099ba9065d4f0b63ed821e2967c/src/types/base.ts#L241)

#### Inherited from

[`BaseRetryPolicy`](../base/interfaces/BaseRetryPolicy.md).[`maxAttempts`](../base/interfaces/BaseRetryPolicy.md#maxattempts)

***

### backoff

> `readonly` **backoff**: `"exponential"` \| `"linear"`

Defined in: [types/base.ts:242](https://github.com/wontlost-ltd/aster-lang/blob/ec56dc5865274099ba9065d4f0b63ed821e2967c/src/types/base.ts#L242)

#### Inherited from

[`BaseRetryPolicy`](../base/interfaces/BaseRetryPolicy.md).[`backoff`](../base/interfaces/BaseRetryPolicy.md#backoff)
