[**@wontlost-ltd/aster-lang**](../../../README.md)

***

# Interface: BaseRetryPolicy

Defined in: [types/base.ts:240](https://github.com/wontlost-ltd/aster-lang/blob/ec56dc5865274099ba9065d4f0b63ed821e2967c/src/types/base.ts#L240)

RetryPolicy 基础接口。

## Extended by

- [`RetryPolicy`](../../interfaces/RetryPolicy.md)
- [`RetryPolicy`](../../namespaces/Core/interfaces/RetryPolicy.md)

## Properties

### maxAttempts

> `readonly` **maxAttempts**: `number`

Defined in: [types/base.ts:241](https://github.com/wontlost-ltd/aster-lang/blob/ec56dc5865274099ba9065d4f0b63ed821e2967c/src/types/base.ts#L241)

***

### backoff

> `readonly` **backoff**: `"exponential"` \| `"linear"`

Defined in: [types/base.ts:242](https://github.com/wontlost-ltd/aster-lang/blob/ec56dc5865274099ba9065d4f0b63ed821e2967c/src/types/base.ts#L242)
